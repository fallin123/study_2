package com.voyah.tvad.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PCM -> AAC 转码 + AAC 切片工具
 *
 * 编码参数（按设计稿要求）：
 * - 采样率 16000 Hz
 * - 声道数 单声道
 * - 采样位深 16bit PCM
 * - AAC Profile: AAC-LC
 * - 码率 16kbps
 *
 * 转码输出：带 ADTS 头的 .aac 文件（可直接播放）
 * 切片输出：按文件大小(KB)在 AAC 帧边界切割，每个切片自带 ADTS 头，可独立播放
 *
 * 健壮性/性能约束：
 * - 所有 MediaCodec / 流资源均在 finally 中释放，异常路径不泄漏
 * - 复用缓冲，避免每帧创建对象（数万~数十万帧场景下的 GC 压力）
 * - 所有 MediaCodec dequeue 返回值分支显式处理，避免未知负值导致死循环
 * - 切片采用单遍流式处理，不在内存保存帧索引，避免大文件 OOM
 * - 所有解析做边界校验，避免数组越界
 */
object PcmAacProcessor {

    private const val TAG = "PcmAacProcessor"
    private const val TIMEOUT_US = 10_000L

    /** 每帧采样数（AAC 固定 1024） */
    private const val SAMPLES_PER_FRAME = 1024

    /** AAC-LC 对应的 Audio Object Type = 2，ADTS 里写 (AOT - 1) = 1 */
    private const val AOT_LC_MINUS_1 = 1

    /** 空转保护上限：防止异常情况下的死循环 */
    private const val MAX_IDLE_LOOP = 10_000

    /** 切片结果回调（主线程） */
    interface SliceCallback {
        fun onSliceCompleted(slicePaths: List<String>)
        fun onError(msg: String)

        /** 切片进度（已处理字节数 / 已完成切片数），默认空实现，可选重写 */
        fun onProgress(processedBytes: Long, sliceCount: Int) {}
    }

    interface ConvertCallback {
        fun onConvertSuccess(outputFile: File, durationSec: Float)
        fun onError(msg: String)
        /** 转码进度（已处理字节数 / 已处理音频秒数），默认空实现，可选重写 */
        fun onProgress(processedBytes: Long, processedSec: Float) {}

        /** 编码器实际输出的采样率/声道数（可能与请求值不同），用于核对 */
        fun onActualFormat(sampleRate: Int, channelCount: Int) {}
    }

    // ------------------------------------------------------------------
    // 多声道 PCM 降混
    //
    // Android 的 AAC 编码器只支持 1~2 声道，而麦克风阵列采集的源 PCM 可能是 8 声道。
    // 必须先降混为单声道再编码。
    // ------------------------------------------------------------------

    /** 降混策略 */
    enum class DownmixMode {
        /** 各声道取平均：保留所有麦克风信息，兼有一定降噪效果（推荐） */
        AVERAGE,

        /** 只取第 1 声道：麦克风阵列中若只有某一路是目标信号时使用 */
        FIRST
    }

    /**
     * 逐块读取多声道 interleaved PCM，降混为单声道。
     *
     * 源数据排列（每声道 2 字节 little-endian）：
     * ```
     * [ch0 ch1 ... chN-1] [ch0 ch1 ... chN-1] ...
     * └──── 第 1 帧 ────┘ └──── 第 2 帧 ────┘
     * ```
     * 若把这种数据直接当单声道读，N 路信号会被串成一路，
     * 听起来就是尖锐的"吱吱"噪声 —— 这正是必须降混的原因。
     *
     * 内部自动处理跨块帧对齐：尾部不足一帧的字节结转到下一次读取，不会丢样例。
     */
    private class MonoPcmReader(
        private val input: InputStream,
        private val srcChannels: Int,
        private val mode: DownmixMode
    ) {
        private val src = ByteArray(READ_CHUNK)
        /** src[0..pending) 为上次结转的残余字节 */
        private var pending = 0

        /** 已从底层读取的源 PCM 字节数（用于进度显示，对应源文件大小） */
        var totalReadBytes = 0L
            private set

        /** 读取一块并降混，返回写入 dst 的有效字节数；<=0 表示已读完 */
        fun read(dst: ByteArray): Int {
            val bytesPerFrame = srcChannels * BYTES_PER_SAMPLE
            while (true) {
                val n = input.read(src, pending, src.size - pending)
                if (n <= 0) return 0
                totalReadBytes += n
                pending += n
                if (pending < bytesPerFrame) continue // 不足一帧，继续读

                // 受 dst 容量约束，保证不越界
                val frames = minOf(pending / bytesPerFrame, dst.size / BYTES_PER_SAMPLE)
                val used = frames * bytesPerFrame
                if (srcChannels == 1) {
                    System.arraycopy(src, 0, dst, 0, used)
                } else {
                    downmix(src, 0, used, srcChannels, dst, mode)
                }

                // 结转尾部不足一帧的残余字节到下一次
                val remain = pending - used
                if (remain > 0) System.arraycopy(src, used, src, 0, remain)
                pending = remain
                return frames * BYTES_PER_SAMPLE
            }
        }

        companion object {
            const val READ_CHUNK = 65536
            const val BYTES_PER_SAMPLE = 2 // 16bit

            /** 读 little-endian 有符号 16bit */
            fun readS16LE(arr: ByteArray, off: Int): Int {
                val lo = arr[off].toInt() and 0xFF
                val hi = arr[off + 1].toInt() // 有符号，左移后自然得到补码扩展
                return (hi shl 8) or lo
            }

            /** 写 little-endian 有符号 16bit */
            fun writeS16LE(arr: ByteArray, off: Int, v: Int) {
                arr[off] = (v and 0xFF).toByte()
                arr[off + 1] = ((v shr 8) and 0xFF).toByte()
            }

            private fun downmix(
                srcArr: ByteArray, off: Int, len: Int, ch: Int,
                dstArr: ByteArray, mode: DownmixMode
            ) {
                val bytesPerFrame = ch * BYTES_PER_SAMPLE
                val frames = len / bytesPerFrame
                for (f in 0 until frames) {
                    val b = off + f * bytesPerFrame
                    val v = if (mode == DownmixMode.FIRST) {
                        readS16LE(srcArr, b)
                    } else {
                        var sum = 0
                        for (c in 0 until ch) {
                            sum += readS16LE(srcArr, b + c * BYTES_PER_SAMPLE)
                        }
                        // 累加值最大 8×32767，仍在 Int 范围内；除以声道数后必然落回 short
                        sum / ch
                    }
                    writeS16LE(dstArr, f * BYTES_PER_SAMPLE, v)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // PCM -> AAC 转码
    // ------------------------------------------------------------------

    /**
     * 将 PCM 流编码为带 ADTS 头的 .aac 文件
     *
     * 源 PCM 支持多声道（如麦克风阵列 8 声道），会先按 [downmixMode] 降混为单声道，
     * 因为 Android 的 AAC 编码器不支持 2 声道以上。
     * 声道数填错（例如把 8 声道源当 1 声道读）会转出"吱吱"的尖锐噪声。
     *
     * @param pcmInput           PCM 输入流（16bit little-endian interleaved），方法内部关闭
     * @param outputFile         输出 .aac 文件
     * @param sampleRate         采样率，默认 16000
     * @param channelCount       编码输出声道数，默认 1（单声道）
     * @param bitrate            码率，默认 16000 (16kbps)
     * @param sourceChannelCount 源 PCM 声道数，默认 1；>1 时先降混
     * @param downmixMode        降混策略，默认各声道取平均
     */
    fun convertPcmToAac(
        pcmInput: InputStream,
        outputFile: File,
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitrate: Int = 16000,
        sourceChannelCount: Int = 1,
        downmixMode: DownmixMode = DownmixMode.AVERAGE,
        callback: ConvertCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var encoder: MediaCodec? = null
        var fos: OutputStream? = null

        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            // 关键：部分设备 AAC 编码器不支持 16000Hz，会悄悄改用其他采样率输出。
            // ADTS 头必须写【编码器实际输出】的采样率，否则播放器按头里的采样率解码
            // 实际是另一种采样率的数据 → 时长错误、播放一顿一顿甚至无法播放。
            var actualSampleRate = sampleRate
            var actualChannels = channelCount
            fun readActualFormat(of: MediaFormat?) {
                if (of == null) return
                if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    actualSampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    actualChannels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                Log.d(
                    TAG, "编码器实际输出: ${actualSampleRate}Hz / ${actualChannels}ch " +
                            "(请求: ${sampleRate}Hz / ${channelCount}ch)"
                )
            }
            readActualFormat(encoder?.outputFormat)

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()
            // 256KB 写缓冲：16kbps 下每帧仅约 135 字节，避免逐帧小写入
            fos = FileOutputStream(outputFile).buffered(256 * 1024)

            var totalPcmBytes = 0L
            // 64KB 分块 + 1MB 读缓冲：逐次小量读会反复触发 IO，大文件(数百MB)下极慢
            // 源 PCM 可能是多声道（如麦克风阵列 8 声道），必须先降混为单声道再编码
            val monoReader = MonoPcmReader(
                pcmInput.buffered(1024 * 1024), sourceChannelCount, downmixMode
            )
            val monoChunk = ByteArray(MonoPcmReader.READ_CHUNK)
            val bufferInfo = MediaCodec.BufferInfo()
            // 降混后是单声道：字节速率 = 采样率 × 2（16bit）
            val bytesPerSec = sampleRate * MonoPcmReader.BYTES_PER_SAMPLE
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            // 复用输出帧缓冲，避免每帧 new ByteArray（数十万帧时 GC 压力巨大）
            var frameBuf = ByteArray(1024)

            /**
             * 排空编码器输出并写入文件。
             * 显式处理所有 dequeue 返回值，未覆盖的负值安全退出，避免死循环。
             * @return 是否收到 EOS
             */
            fun drainEncoder(): Boolean {
                var sawEos = false
                var idle = 0
                while (idle++ < MAX_IDLE_LOOP) {
                    val outIndex = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        ?: MediaCodec.INFO_TRY_AGAIN_LATER
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            readActualFormat(encoder?.outputFormat)
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 已废弃事件，忽略即可（必须显式处理，否则会空转）
                        }
                        outIndex >= 0 -> {
                            val size = bufferInfo.size
                            // 编码器配置数据(AudioSpecificConfig)不是音频帧，
                            // 给它加 ADTS 头会写出 9 字节坏帧，手机播放器解析到就会卡住/杂音
                            val isCodecConfig =
                                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val outBuffer = encoder?.getOutputBuffer(outIndex)
                            if (outBuffer != null && size > 0 && !isCodecConfig) {
                                val total = size + 7
                                if (frameBuf.size < total) frameBuf = ByteArray(total * 2)
                                // 用编码器实际输出的采样率/声道写 ADTS 头
                                writeAdtsHeader(frameBuf, total, actualSampleRate, actualChannels)
                                try {
                                    outBuffer.position(bufferInfo.offset)
                                    outBuffer.limit(bufferInfo.offset + size)
                                    outBuffer.get(frameBuf, 7, size) // 越界会抛异常，下面兜住
                                    fos?.write(frameBuf, 0, total)
                                } catch (e: Exception) {
                                    Log.w(TAG, "写输出帧异常，跳过该帧", e)
                                }
                            }
                            encoder?.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawEos = true
                        }
                        else -> return sawEos // 其余未知负值，安全退出
                    }
                }
                Log.w(TAG, "drainEncoder 达到空转上限，提前退出")
                return sawEos
            }

            // 喂 PCM 数据
            while (true) {
                val read = monoReader.read(monoChunk)
                if (read <= 0) break
                totalPcmBytes += read

                // 每处理 4MB 或每 1 秒回调一次进度，避免主线程刷新过频
                if (monoReader.totalReadBytes - lastProgressBytes >= 4 * 1024 * 1024 ||
                    System.currentTimeMillis() - lastProgressMs >= 1000
                ) {
                    lastProgressBytes = monoReader.totalReadBytes
                    lastProgressMs = System.currentTimeMillis()
                    // 进度显示源 PCM 读取量（含所有声道），便于对照源文件大小
                    val bytesSnapshot = monoReader.totalReadBytes
                    val secSnapshot = totalPcmBytes.toFloat() / bytesPerSec
                    main.post { callback.onProgress(bytesSnapshot, secSnapshot) }
                }

                var offset = 0
                var idle = 0
                while (offset < read && idle++ < MAX_IDLE_LOOP) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex < 0) {
                        drainEncoder()
                        continue
                    }
                    val inBuffer = encoder?.getInputBuffer(inIndex)
                    if (inBuffer == null) {
                        drainEncoder()
                        continue
                    }
                    idle = 0
                    inBuffer.clear()
                    val len = minOf(inBuffer.capacity(), read - offset)
                    inBuffer.put(monoChunk, offset, len)
                    encoder?.queueInputBuffer(inIndex, 0, len, 0, 0)
                    offset += len
                    drainEncoder()
                }
            }

            // 发送 EOS 并排空
            var eosQueued = false
            var eosDrained = false
            var idle = 0
            while (!eosDrained && idle++ < MAX_IDLE_LOOP) {
                if (!eosQueued) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex >= 0) {
                        encoder?.queueInputBuffer(
                            inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosQueued = true
                    }
                }
                if (drainEncoder()) eosDrained = true
            }
            if (!eosDrained) Log.w(TAG, "EOS 排空超时，输出可能不完整")

            val durationSec = totalPcmBytes.toFloat() / bytesPerSec
            Log.d(
                TAG, "转码完成: ${outputFile.absolutePath}, 时长=${durationSec}s, " +
                        "实际=${actualSampleRate}Hz/${actualChannels}ch"
            )
            val sr = actualSampleRate
            val ch = actualChannels
            main.post {
                callback.onActualFormat(sr, ch)
                callback.onConvertSuccess(outputFile, durationSec)
            }
        } catch (e: Exception) {
            Log.e(TAG, "PCM转AAC失败", e)
            main.post { callback.onError("转码失败: ${e.message}") }
        } finally {
            // 无论成功/异常，统一释放，避免 MediaCodec 与文件句柄泄漏
            try {
                fos?.close()
            } catch (ignored: Exception) {
            }
            try {
                pcmInput.close()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.stop()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * 写 7 字节 ADTS 头（无 CRC）
     */
    private fun writeAdtsHeader(dest: ByteArray, aacFrameLength: Int, sampleRate: Int, channelCount: Int) {
        val sampleRateIndex = sampleRateToIndex(sampleRate)
        val frameLen = aacFrameLength // 含 ADTS 头的总长度
        dest[0] = 0xFF.toByte()
        dest[1] = 0xF1.toByte()                        // MPEG-4, Layer 0, protection_absent=1
        dest[2] = (((AOT_LC_MINUS_1 and 0x3) shl 6)
                or ((sampleRateIndex and 0xF) shl 2)
                or ((channelCount shr 2) and 0x1)).toByte()
        dest[3] = (((channelCount and 0x3) shl 6)
                or ((frameLen shr 11) and 0x3)).toByte()
        dest[4] = ((frameLen shr 3) and 0xFF).toByte()
        // byte[5] 位布局：bit7~5 = frameLen[2:0]，bit4~0 = adts_buffer_fullness[10:6]
        // 只能用 0x1F：0x7FF(VBR) 的高 5 位 = 0b11111。
        // 若填 0x7F(0b0111_1111) 会把 bit6/bit5 也置 1，而这两位属于 frame_length，
        // 导致头里声明的帧长比实际大 → 播放器按错长度跳，吃进下一帧的同步头 →
        // 丢帧、重新同步失败，表现为时长不对、语速变快、卡顿。
        dest[5] = (((frameLen and 0x7) shl 5) or 0x1F).toByte()
        dest[6] = 0xFC.toByte()                        // fullness 低 6 位 = 0x3F, 一帧
    }

    private fun sampleRateToIndex(sampleRate: Int): Int = when (sampleRate) {
        96000 -> 0; 88200 -> 1; 64000 -> 2; 48000 -> 3
        44100 -> 4; 32000 -> 5; 24000 -> 6; 22050 -> 7
        16000 -> 8; 12000 -> 9; 11025 -> 10; 8000 -> 11
        else -> 8 // 默认 16k
    }

    // ------------------------------------------------------------------
    // AAC 切片（单遍流式，内存占用恒定）
    // ------------------------------------------------------------------

    /**
     * 按文件大小(KB)切片（当前需求：以大小为主，严格控制上传文件大小；按时长切片暂不启用）
     *
     * 实现要点：
     * - 单遍流式扫描，边解析边写，**不在内存保存帧索引**，大文件也不会 OOM
     * - 用 RandomAccessFile.seek 精确定位，避免 skip() 部分跳过导致的数据错位
     * - 每帧都写入完整 ADTS 头，切片可独立播放
     *
     * @param aacFile     已转码的 .aac 文件
     * @param outputDir   切片输出目录
     * @param sizeKb      单个切片大小上限（KB），<=0 表示不限制
     */
    fun sliceAac(
        aacFile: File,
        outputDir: File,
        sizeKb: Int,
        callback: SliceCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var out: OutputStream? = null

        try {
            if (!aacFile.exists() || aacFile.length() <= 0) {
                main.post { callback.onError("AAC 文件不存在或为空") }
                return
            }

            outputDir.mkdirs()
            // 只清理旧切片文件（切片目录与转码目录已分离，转码结果不会被误删）
            outputDir.listFiles()
                ?.filter { it.name.startsWith("slice_") }
                ?.forEach { it.delete() }

            val sizeLimitBytes = if (sizeKb > 0) sizeKb * 1024L else Long.MAX_VALUE
            val fileLen = aacFile.length()
            // 单帧时长（微秒）= 1024 / sampleRate * 1_000_000，16kHz 下 = 64ms（仅用于统计日志）
            val frameDurationUs = SAMPLES_PER_FRAME * 1_000_000L / 16_000L

            val slicePaths = mutableListOf<String>()
            var sliceIndex = 1
            var sliceBytes = 0L
            var sliceFrames = 0
            var sliceDurationUs = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            fun openSlice() {
                val f = File(outputDir, "slice_%03d.aac".format(sliceIndex))
                slicePaths.add(f.absolutePath)
                out = FileOutputStream(f).buffered(64 * 1024)
                sliceIndex++
                sliceBytes = 0L
                sliceFrames = 0
                sliceDurationUs = 0L
            }

            fun closeSlice() {
                try {
                    out?.close()
                } catch (ignored: Exception) {
                }
                out = null
            }

            RandomAccessFile(aacFile, "r").use { raf ->
                val header = ByteArray(7)
                val body = ByteArray(64 * 1024)
                var offset = 0L

                while (offset + 7 <= fileLen) {
                    raf.seek(offset)
                    if (raf.read(header, 0, 7) < 7) break

                    // syncword 校验
                    if ((header[0].toInt() and 0xFF) != 0xFF || (header[1].toInt() and 0xF0) != 0xF0) {
                        offset++ // 非同步位置，前进 1 字节重新同步
                        continue
                    }

                    val frameLen = ((header[3].toInt() and 0x3) shl 11) or
                            ((header[4].toInt() and 0xFF) shl 3) or
                            ((header[5].toInt() and 0xE0) shr 5)

                    // 边界校验：帧长非法或超出文件末尾则重新同步，避免越界/脏数据
                    if (frameLen < 7 || offset + frameLen > fileLen) {
                        offset++
                        continue
                    }

                    if (out == null) openSlice()

                    // 写 ADTS 头（7 字节）
                    out?.write(header, 0, 7)

                    // 写帧体：raf 位置已停在 offset+7，顺序读即可，无需再次 seek
                    var remaining = frameLen - 7
                    while (remaining > 0) {
                        val n = raf.read(body, 0, minOf(body.size, remaining))
                        if (n <= 0) break
                        out?.write(body, 0, n)
                        remaining -= n
                    }

                    sliceBytes += frameLen
                    sliceFrames++
                    sliceDurationUs += frameDurationUs
                    offset += frameLen

                    // 进度：每 2MB 或每 1 秒回调一次
                    if (offset - lastProgressBytes >= 2 * 1024 * 1024 ||
                        System.currentTimeMillis() - lastProgressMs >= 1000
                    ) {
                        lastProgressBytes = offset
                        lastProgressMs = System.currentTimeMillis()
                        val bytesSnapshot = offset
                        val countSnapshot = sliceIndex - 1
                        main.post { callback.onProgress(bytesSnapshot, countSnapshot) }
                    }

                    // 按需求：以文件大小为主切片，时长条件暂不启用
                    if (sliceBytes >= sizeLimitBytes) {
                        Log.d(
                            TAG, "切片${sliceIndex - 1}: ${sliceFrames}帧, " +
                                    "${sliceBytes / 1024}KB, ${sliceDurationUs / 1000}ms"
                        )
                        closeSlice()
                    }
                }
            }
            closeSlice() // 收尾，关闭最后一个切片

            if (slicePaths.isEmpty()) {
                main.post { callback.onError("未解析到有效 AAC 帧，文件可能损坏") }
                return
            }

            Log.d(TAG, "切片完成，共 ${slicePaths.size} 个")
            main.post { callback.onSliceCompleted(slicePaths) }
        } catch (e: Exception) {
            Log.e(TAG, "AAC切片失败", e)
            main.post { callback.onError("切片失败: ${e.message}") }
        } finally {
            try {
                out?.close()
            } catch (ignored: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------
    // MP4A(M4A 容器) 转码 + 切片
    //
    // 与 ADTS 裸流方案的区别：
    // - ADTS(.aac) 无容器，没有时长元数据，手机播放器时长常显示不准
    // - M4A(MP4 容器) 写入 moov box，自带精确时长，手机播放器可正常显示与拖动
    // 编码参数完全一致：16000Hz / 单声道 / 16bit / AAC-LC / 16kbps
    // ------------------------------------------------------------------

    /**
     * 将 PCM 编码并封装为 M4A（MP4 容器 / AAC-LC），自带时长信息
     *
     * 与 ADTS 版的关键差异：容器依赖**递增的时间戳**计算时长，
     * 因此 queueInputBuffer 必须传入递增的 presentationTimeUs，否则时长/播放异常。
     */
    fun convertPcmToM4a(
        pcmInput: InputStream,
        outputFile: File,
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitrate: Int = 16000,
        sourceChannelCount: Int = 1,
        downmixMode: DownmixMode = DownmixMode.AVERAGE,
        callback: ConvertCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            Log.i(TAG, "convertPcmToM4a 使用编码器: ${encoder.name}")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            // 源 PCM 可能是多声道（如麦克风阵列 8 声道），必须先降混为单声道再编码
            val monoReader = MonoPcmReader(
                pcmInput.buffered(1024 * 1024), sourceChannelCount, downmixMode
            )
            val monoChunk = ByteArray(MonoPcmReader.READ_CHUNK)
            val bufferInfo = MediaCodec.BufferInfo()
            // 降混后是单声道：字节速率 = 采样率 × 2（16bit）
            val bytesPerSec = sampleRate * MonoPcmReader.BYTES_PER_SAMPLE
            var totalPcmBytes = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            var actualSampleRate = sampleRate
            var actualChannels = channelCount
            var trackIndex = -1
            var muxerStarted = false

            /** 拿到编码器真实输出格式后建立轨道（MP4 需要含 csd-0 的 MediaFormat 才能 addTrack） */
            fun ensureMuxer() {
                if (muxerStarted) return
                val of = encoder?.outputFormat ?: return
                if (!of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) return
                actualSampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    actualChannels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                val m = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                muxer = m
                trackIndex = m.addTrack(of)
                m.start()
                muxerStarted = true
                Log.d(TAG, "M4A 轨道就绪: ${actualSampleRate}Hz / ${actualChannels}ch")
            }

            fun drainEncoder(): Boolean {
                var sawEos = false
                var idle = 0
                while (idle++ < MAX_IDLE_LOOP) {
                    val outIndex = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        ?: MediaCodec.INFO_TRY_AGAIN_LATER
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            Log.d(TAG, "M4A encoder format: ${encoder?.outputFormat}")
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 已废弃事件，显式忽略避免空转
                        }
                        outIndex >= 0 -> {
                            val size = bufferInfo.size
                            val isCodecConfig =
                                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val outBuffer = encoder?.getOutputBuffer(outIndex)
                            if (outBuffer != null && size > 0 && !isCodecConfig) {
                                ensureMuxer()
                                if (muxerStarted) {
                                    outBuffer.position(bufferInfo.offset)
                                    outBuffer.limit(bufferInfo.offset + size)
                                    try {
                                        muxer?.writeSampleData(trackIndex, outBuffer, bufferInfo)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "写 muxer 样本失败", e)
                                    }
                                }
                            }
                            encoder?.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawEos = true
                        }
                        else -> return sawEos
                    }
                }
                Log.w(TAG, "drainEncoder 达到空转上限(M4A)")
                return sawEos
            }

            // 喂 PCM：必须传递增时间戳，否则容器里时长为 0 或播放异常
            var ptsUs = 0L
            while (true) {
                val read = monoReader.read(monoChunk)
                if (read <= 0) break
                totalPcmBytes += read

                if (monoReader.totalReadBytes - lastProgressBytes >= 4 * 1024 * 1024 ||
                    System.currentTimeMillis() - lastProgressMs >= 1000
                ) {
                    lastProgressBytes = monoReader.totalReadBytes
                    lastProgressMs = System.currentTimeMillis()
                    // 进度显示源 PCM 读取量（含所有声道），便于对照源文件大小
                    val bytesSnapshot = monoReader.totalReadBytes
                    val secSnapshot = totalPcmBytes.toFloat() / bytesPerSec
                    main.post { callback.onProgress(bytesSnapshot, secSnapshot) }
                }

                var offset = 0
                var idle = 0
                while (offset < read && idle++ < MAX_IDLE_LOOP) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex < 0) {
                        drainEncoder()
                        continue
                    }
                    val inBuffer = encoder?.getInputBuffer(inIndex)
                    if (inBuffer == null) {
                        drainEncoder()
                        continue
                    }
                    idle = 0
                    inBuffer.clear()
                    val len = minOf(inBuffer.capacity(), read - offset)
                    inBuffer.put(monoChunk, offset, len)
                    encoder?.queueInputBuffer(inIndex, 0, len, ptsUs, 0)
                    ptsUs += len.toLong() * 1_000_000L / bytesPerSec
                    offset += len
                    drainEncoder()
                }
            }

            // EOS 并排空
            var eosQueued = false
            var eosDrained = false
            var idle = 0
            while (!eosDrained && idle++ < MAX_IDLE_LOOP) {
                if (!eosQueued) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex >= 0) {
                        encoder?.queueInputBuffer(
                            inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosQueued = true
                    }
                }
                if (drainEncoder()) eosDrained = true
            }
            if (!eosDrained) Log.w(TAG, "M4A EOS 排空超时，输出可能不完整")

            val durationSec = totalPcmBytes.toFloat() / bytesPerSec
            Log.d(TAG, "M4A 转码完成: ${outputFile.absolutePath}, 时长=${durationSec}s")
            val sr = actualSampleRate
            val ch = actualChannels
            main.post {
                callback.onActualFormat(sr, ch)
                callback.onConvertSuccess(outputFile, durationSec)
            }
        } catch (e: Exception) {
            Log.e(TAG, "PCM转M4A失败", e)
            main.post { callback.onError("转码失败(M4A): ${e.message}") }
        } finally {
            try {
                muxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                muxer?.release()
            } catch (ignored: Exception) {
            }
            try {
                pcmInput.close()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.stop()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------
    // 诊断：AAC 编码器实现
    // ------------------------------------------------------------------

    /**
     * 枚举本机所有 AAC 编码器，输出「硬件 / 软件」实现与能力区间。
     *
     * 为什么需要它：25 万帧 AAC 编码是整个转码链路里最可能吃掉大部分 CPU 的一环。
     * 如果 createEncoderByType 选中的是软编（isSoftwareOnly = true），
     * 那么降混再怎么优化，CPU 也降不下来 —— 优化方向必须改成「换编码器或换编码格式」。
     *
     * 结果同时写 logcat（TAG = PcmAacProcessor），并作为字符串返回给 UI。
     */
    fun dumpAacEncoderInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("==== AAC 编码器清单 ====")
        try {
            var count = 0
            for (info in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
                if (!info.isEncoder) continue
                val supportsAac = info.supportedTypes.any {
                    it.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true)
                }
                if (!supportsAac) continue

                count++
                sb.appendLine("#$count ${info.name}")
                // isHardwareAccelerated / isSoftwareOnly / isAlias 是 API 29 才有的
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    sb.appendLine(
                        "   硬件加速=${info.isHardwareAccelerated} " +
                                "纯软件=${info.isSoftwareOnly} " +
                                "厂商实现=${info.isVendor} 别名=${info.isAlias}"
                    )
                } else {
                    sb.appendLine("   API ${Build.VERSION.SDK_INT} 不支持读取硬编/软编标记")
                }
                try {
                    val caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC)
                    val audio = caps.audioCapabilities
                    if (audio != null) {
                        sb.appendLine("   码率区间=${audio.bitrateRange}")
                        sb.appendLine("   支持 16kHz=${audio.isSampleRateSupported(16000)}")
                        val rates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000)
                            .filter { audio.isSampleRateSupported(it) }
                        sb.appendLine("   支持采样率(Hz)=$rates")
                    }
                    val lc = caps.profileLevels?.any {
                        it.profile == MediaCodecInfo.CodecProfileLevel.AACObjectLC
                    } ?: false
                    sb.appendLine("   支持 AAC-LC=$lc")
                } catch (e: Exception) {
                    sb.appendLine("   读取能力失败: ${e.message}")
                }
            }
            if (count == 0) sb.appendLine("未找到任何 AAC 编码器（异常）")
        } catch (e: Exception) {
            sb.appendLine("枚举失败: ${e.message}")
        }

        sb.appendLine("---- createEncoderByType 实际选中 ----")
        sb.appendLine(
            try {
                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                val name = codec.name
                codec.release()
                name
            } catch (e: Exception) {
                "创建失败: ${e.message}"
            }
        )
        sb.appendLine("========================")

        val text = sb.toString()
        Log.i(TAG, text)
        return text
    }

    // ------------------------------------------------------------------
    // PCM -> M4A 高性能版
    //
    // 输出格式与 convertPcmToM4a 完全一致（M4A / AAC-LC / 单声道），
    // 只替换内部数据处理方式，共 5 处优化：
    //
    // 1. 降混不再逐字节做位运算，改为 ShortBuffer 批量搬运 + ShortArray 纯数组求和。
    //    原实现每帧 9 次函数调用、约 18 次带边界检查的字节访问；
    //    2.59 亿帧的源文件意味着 20 多亿次字节操作。
    // 2. 降混结果直接写进 MediaCodec 的输入 ByteBuffer，省掉中间 mono 缓冲
    //    以及 inBuffer.put(monoChunk, ...) 这次整段拷贝。
    // 3. 源文件用 256KB 大块直接读，不再套 BufferedInputStream，省掉一次全量内存拷贝。
    // 4. 输出轮询改非阻塞（timeout=0）：原实现每处理一块，drainEncoder 末尾必然
    //    阻塞满 10ms 才返回 TRY_AGAIN，8100 块累计约 80 秒纯等待。
    // 5. 复用 BufferInfo / ShortArray / ByteBuffer 视图，减少 GC 抖动。
    // ------------------------------------------------------------------

    /**
     * 把 interleaved 多声道 16bit 采样压成单声道（ShortArray 版）。
     *
     * 与 [MonoPcmReader] 的 downmix 结果完全等价，只是把「字节→short→字节」
     * 的位运算换成了纯 short 数组运算，JIT 更容易优化成紧凑循环。
     */
    private fun downmixToShorts(
        src: ShortArray,
        frames: Int,
        channels: Int,
        dst: ShortArray,
        mode: DownmixMode
    ) {
        when {
            channels == 1 -> System.arraycopy(src, 0, dst, 0, frames)

            mode == DownmixMode.FIRST -> {
                var i = 0
                var o = 0
                while (o < frames) {
                    dst[o++] = src[i]
                    i += channels
                }
            }

            // 8 声道展开：省掉内层循环，最常见的麦克风阵列场景
            channels == 8 -> {
                var i = 0
                var o = 0
                while (o < frames) {
                    val s = src[i] + src[i + 1] + src[i + 2] + src[i + 3] +
                            src[i + 4] + src[i + 5] + src[i + 6] + src[i + 7]
                    dst[o] = (s / 8).toShort()
                    i += 8
                    o++
                }
            }

            else -> {
                var i = 0
                var o = 0
                while (o < frames) {
                    var s = 0
                    var c = 0
                    while (c < channels) {
                        s += src[i + c]
                        c++
                    }
                    dst[o] = (s / channels).toShort()
                    i += channels
                    o++
                }
            }
        }
    }

    /**
     * PCM → M4A 高性能实现（与 [convertPcmToM4a] 行为一致，仅内部优化）
     *
     * @param pcmInput           PCM 输入流（16bit little-endian interleaved），方法内部关闭
     * @param outputFile         输出 .m4a 文件
     * @param sampleRate         采样率，默认 16000
     * @param channelCount       编码输出声道数，默认 1（本实现固定按单声道喂数据）
     * @param bitrate            码率，默认 16000（与 [convertPcmToM4a] 保持一致）
     * @param sourceChannelCount 源 PCM 声道数，默认 1；>1 时先降混
     * @param downmixMode        降混策略，默认各声道取平均
     * @param throttleMsPerBlock 每处理完一个数据块后的主动让出时长（ms），0 = 不节流。
     *                           这是「占用率 ↔ 总耗时」的旋钮：占用率 ≈ 16% × 6.6/(6.6+该值)。
     *                           需求允许拉长时间换低占用时调大它，见 Activity 侧的换算表。
     */
    fun convertPcmToM4aFast(
        pcmInput: InputStream,
        outputFile: File,
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitrate: Int = 16000,
        sourceChannelCount: Int = 1,
        downmixMode: DownmixMode = DownmixMode.AVERAGE,
        throttleMsPerBlock: Int = 0,
        callback: ConvertCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        // 源数据块：16384 帧。8 声道时 256KB，按帧对齐（不会产生跨块残帧）
        val framesPerBlock = 16384
        val srcChannels = if (sourceChannelCount < 1) 1 else sourceChannelCount
        val frameBytes = srcChannels * 2

        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            // 打印实际选中的编码器实现：软编 vs 硬编决定了 CPU 优化的上限
            Log.i(TAG, "高性能版使用编码器: ${encoder.name}")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            // 大块源缓冲 + 一次性的 short 视图：整条链路只有这一份源内存
            val srcBytes = ByteArray(framesPerBlock * frameBytes)
            val srcShorts = ByteBuffer.wrap(srcBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
            val samples = ShortArray(framesPerBlock * srcChannels)
            val mono = ShortArray(framesPerBlock)
            val bufferInfo = MediaCodec.BufferInfo()

            var valid = 0          // srcBytes[0, valid) 为待处理数据
            var eof = false
            var totalSrcBytes = 0L
            var totalMonoBytes = 0L
            var ptsFrames = 0L
            var ptsUs = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            var actualSampleRate = sampleRate
            var actualChannels = channelCount
            var trackIndex = -1
            var muxerStarted = false

            /** 拿到含 csd-0 的真实输出格式后建轨道（与旧实现一致，只是提前到 FORMAT_CHANGED） */
            fun ensureMuxer() {
                if (muxerStarted) return
                val of = encoder?.outputFormat ?: return
                if (!of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) return
                actualSampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    actualChannels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                val m = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                muxer = m
                trackIndex = m.addTrack(of)
                m.start()
                muxerStarted = true
                Log.d(TAG, "高性能版轨道就绪: ${actualSampleRate}Hz / ${actualChannels}ch")
            }

            // ---- 排空环节的细分计时（定位 9 秒花在哪一步）----
            var nsDequeueOut = 0L   // dequeueOutputBuffer
            var nsWriteSample = 0L  // muxer.writeSampleData
            var nsReleaseOut = 0L   // releaseOutputBuffer
            var muxWriteCount = 0L  // writeSampleData 调用次数
            var muxWriteBytes = 0L  // 写入的样本总字节

            /**
             * 排空编码器输出。
             * @param timeoutUs 0 = 非阻塞（常规喂数据时用），EOS 收尾时才用 TIMEOUT_US
             */
            fun drainEncoder(timeoutUs: Long): Boolean {
                var sawEos = false
                var idle = 0
                while (idle++ < MAX_IDLE_LOOP) {
                    val tDq = System.nanoTime()
                    val outIndex = encoder?.dequeueOutputBuffer(bufferInfo, timeoutUs)
                        ?: MediaCodec.INFO_TRY_AGAIN_LATER
                    nsDequeueOut += System.nanoTime() - tDq
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> ensureMuxer()
                        outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 已废弃事件，显式忽略
                        }
                        outIndex >= 0 -> {
                            val size = bufferInfo.size
                            val isCodecConfig =
                                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val outBuffer = encoder?.getOutputBuffer(outIndex)
                            if (outBuffer != null && size > 0 && !isCodecConfig) {
                                ensureMuxer()
                                if (muxerStarted) {
                                    // 直接交给 muxer，不做任何拷贝
                                    outBuffer.position(bufferInfo.offset)
                                    outBuffer.limit(bufferInfo.offset + size)
                                    val tW = System.nanoTime()
                                    try {
                                        muxer?.writeSampleData(trackIndex, outBuffer, bufferInfo)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "高性能版写样本失败", e)
                                    }
                                    nsWriteSample += System.nanoTime() - tW
                                    muxWriteCount++
                                    muxWriteBytes += size
                                }
                            }
                            val tR = System.nanoTime()
                            encoder?.releaseOutputBuffer(outIndex, false)
                            nsReleaseOut += System.nanoTime() - tR
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawEos = true
                        }
                        else -> return sawEos
                    }
                }
                return sawEos
            }

            val startMs = System.currentTimeMillis()
            var stall = 0 // 连续拿不到输入缓冲的次数，异常时兜底退出，避免死循环

            // ---- 性能分解计时（块级采样，每块 1 秒音频，开销可忽略）----
            var nsWaitInput = 0L // 等编码器输入缓冲（说明编码器是瓶颈）
            var nsDownmix = 0L   // 降混（含 ShortBuffer 搬运）
            var nsQueue = 0L     // queueInputBuffer 的 JNI 开销
            var nsDrain = 0L     // 排空输出 + muxer 写盘
            var nsRead = 0L      // 读源文件（磁盘 / FUSE）

            fun timedDrain(): Boolean {
                val t = System.nanoTime()
                val r = drainEncoder(0L)
                nsDrain += System.nanoTime() - t
                return r
            }

            while (true) {
                // 1) 有整帧数据 → 直接编码，直到块内数据耗尽
                if (valid >= frameBytes) {
                    val tWait = System.nanoTime()
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    nsWaitInput += System.nanoTime() - tWait
                    if (inIndex < 0) {
                        if (++stall > MAX_IDLE_LOOP) {
                            Log.w(TAG, "高性能版长时间拿不到输入缓冲，提前结束")
                            break
                        }
                        timedDrain() // 编码器忙：先把已完成输出排空，不空等
                        continue
                    }
                    val inBuffer = encoder?.getInputBuffer(inIndex)
                    if (inBuffer == null) {
                        if (++stall > MAX_IDLE_LOOP) {
                            Log.w(TAG, "高性能版输入缓冲为空，提前结束")
                            break
                        }
                        timedDrain()
                        continue
                    }
                    stall = 0

                    inBuffer.clear()
                    inBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val inShorts = inBuffer.asShortBuffer()
                    val capFrames = inShorts.capacity()
                    if (capFrames <= 0) {
                        // 理论上不会发生，兜底避免死循环
                        encoder?.queueInputBuffer(inIndex, 0, 0, ptsUs, 0)
                        drainEncoder(0L)
                        continue
                    }

                    val availFrames = valid / frameBytes
                    val frames = if (availFrames < capFrames) availFrames else capFrames

                    // 源数据整体搬到 ShortArray（一次批量搬运），再做纯数组求和
                    val tMix = System.nanoTime()
                    srcShorts.position(0)
                    srcShorts.get(samples, 0, frames * srcChannels)
                    downmixToShorts(samples, frames, srcChannels, mono, downmixMode)
                    // 降混结果直接落进编码器的输入缓冲，省掉中间 byte 数组与 put 拷贝
                    inShorts.put(mono, 0, frames)
                    nsDownmix += System.nanoTime() - tMix

                    val tQueue = System.nanoTime()
                    encoder?.queueInputBuffer(inIndex, 0, frames * 2, ptsUs, 0)
                    nsQueue += System.nanoTime() - tQueue
                    ptsFrames += frames
                    ptsUs = ptsFrames * 1_000_000L / sampleRate

                    // 消费并前移剩余数据（arraycopy 支持重叠，等价 memmove）
                    val consumed = frames * frameBytes
                    valid -= consumed
                    if (valid > 0) System.arraycopy(srcBytes, consumed, srcBytes, 0, valid)
                    totalMonoBytes += frames.toLong() * 2

                    timedDrain()

                    if (totalSrcBytes - lastProgressBytes >= 4 * 1024 * 1024 ||
                        System.currentTimeMillis() - lastProgressMs >= 1000
                    ) {
                        lastProgressBytes = totalSrcBytes
                        lastProgressMs = System.currentTimeMillis()
                        val bytesSnapshot = totalSrcBytes
                        val secSnapshot = totalMonoBytes / (sampleRate * 2f)
                        main.post { callback.onProgress(bytesSnapshot, secSnapshot) }
                    }

                    // 节流：每块处理完主动让出 CPU。睡眠期间本线程和编码器线程都空闲，
                    // 占用率随之下降，代价是总耗时按比例拉长（需求允许时间换占用）。
                    if (throttleMsPerBlock > 0) {
                        try {
                            Thread.sleep(throttleMsPerBlock.toLong())
                        } catch (ignored: InterruptedException) {
                        }
                    }
                    continue
                }

                // 2) 数据不够一帧 → 补数据
                if (eof) break
                val tRead = System.nanoTime()
                val n = pcmInput.read(srcBytes, valid, srcBytes.size - valid)
                nsRead += System.nanoTime() - tRead
                if (n <= 0) {
                    eof = true
                    continue
                }
                valid += n
                totalSrcBytes += n
            }

            // 3) EOS：送空包后阻塞排空
            var eosQueued = false
            var eosDrained = false
            var idle = 0
            while (!eosDrained && idle++ < MAX_IDLE_LOOP) {
                if (!eosQueued) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex >= 0) {
                        encoder?.queueInputBuffer(
                            inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosQueued = true
                    }
                }
                if (drainEncoder(if (eosQueued) TIMEOUT_US else 0L)) eosDrained = true
            }
            if (!eosDrained) Log.w(TAG, "高性能版 EOS 排空超时，输出可能不完整")

            val durationSec = totalMonoBytes / (sampleRate * 2f)
            val elapsedMs = System.currentTimeMillis() - startMs
            val speed = if (elapsedMs > 0) durationSec * 1000f / elapsedMs else 0f
            Log.d(
                TAG, "高性能版完成: ${outputFile.name}, 时长=${durationSec}s, " +
                        "耗时=${elapsedMs}ms(${speed}x 实时), " +
                        "源=${totalSrcBytes / 1024 / 1024}MB, " +
                        "格式=${actualSampleRate}Hz/${actualChannels}ch"
            )

            // 耗时分解：哪一个环节占大头，就用这份数据说话，不再靠猜
            val totalNs = elapsedMs * 1_000_000L
            fun pct(v: Long) = if (totalNs > 0) v * 100f / totalNs else 0f
            Log.i(
                TAG, buildString {
                    appendLine("==== 高性能版耗时分解（总 ${elapsedMs}ms）====")
                    appendLine("  等待编码器输入 ${nsWaitInput / 1_000_000}ms (${"%.1f".format(pct(nsWaitInput))}%)")
                    appendLine("  降混+搬运      ${nsDownmix / 1_000_000}ms (${"%.1f".format(pct(nsDownmix))}%)")
                    appendLine("  queueInput     ${nsQueue / 1_000_000}ms (${"%.1f".format(pct(nsQueue))}%)")
                    appendLine("  排空输出+写盘  ${nsDrain / 1_000_000}ms (${"%.1f".format(pct(nsDrain))}%)")
                    appendLine("  读源文件       ${nsRead / 1_000_000}ms (${"%.1f".format(pct(nsRead))}%)")
                    appendLine("  未统计(编码器内部异步)/其他 ${(totalNs - nsWaitInput - nsDownmix - nsQueue - nsDrain - nsRead) / 1_000_000}ms")
                    appendLine("---- 排空环节细分 ----")
                    appendLine("  输出路径       ${outputFile.absolutePath}")
                    appendLine("  dequeueOutput  ${nsDequeueOut / 1_000_000}ms")
                    appendLine("  writeSampleData ${nsWriteSample / 1_000_000}ms（${muxWriteCount} 次, 共 ${muxWriteBytes / 1024}KB, " +
                            "平均 ${if (muxWriteCount > 0) nsWriteSample / 1000 / muxWriteCount else 0}µs/次）")
                    appendLine("  releaseOutput  ${nsReleaseOut / 1_000_000}ms")
                    append("  输出采样帧数    约 ${durationSec * sampleRate / 1024} 个 AAC 帧")
                }
            )

            val sr = actualSampleRate
            val ch = actualChannels
            main.post {
                callback.onActualFormat(sr, ch)
                callback.onConvertSuccess(outputFile, durationSec)
            }
        } catch (e: Exception) {
            Log.e(TAG, "高性能版 PCM转M4A失败", e)
            main.post { callback.onError("转码失败(高性能版): ${e.message}") }
        } finally {
            try {
                muxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                muxer?.release()
            } catch (ignored: Exception) {
            }
            try {
                pcmInput.close()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.stop()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * 将 M4A 按大小(KB)切成多个独立可播的 M4A
     *
     * 用 MediaExtractor 读出原始 AAC 帧与时间戳，再用 MediaMuxer 逐片封装，
     * 每个切片都自带 moov（时长精确），时间戳归一化到从 0 开始 → 可独立播放与拖动。
     *
     * @param sourceFile 已转码的 .m4a 文件
     * @param outputDir  切片输出目录
     * @param sizeKb     单个切片大小上限（KB），<=0 表示不限制
     */
    fun sliceM4a(
        sourceFile: File,
        outputDir: File,
        sizeKb: Int,
        callback: SliceCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            if (!sourceFile.exists() || sourceFile.length() <= 0) {
                main.post { callback.onError("源文件不存在或为空") }
                return
            }
            outputDir.mkdirs()
            // 只清理旧 M4A 切片（不影响 .aac 切片，也不动转码结果）
            outputDir.listFiles()
                ?.filter { it.name.startsWith("slice_") && it.name.endsWith(".m4a") }
                ?.forEach { it.delete() }

            val ex = MediaExtractor()
            extractor = ex
            ex.setDataSource(sourceFile.absolutePath)

            var audioTrack = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    break
                }
            }
            if (audioTrack < 0) {
                main.post { callback.onError("源文件中没有音频轨") }
                return
            }
            ex.selectTrack(audioTrack)
            val format = ex.getTrackFormat(audioTrack)

            val sizeLimitBytes = if (sizeKb > 0) sizeKb * 1024L else Long.MAX_VALUE
            val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocateDirect(maxInput)
            val bufferInfo = MediaCodec.BufferInfo()

            val slicePaths = mutableListOf<String>()
            var sliceIndex = 1
            var sliceBytes = 0L
            var sliceStartUs = -1L
            var trackIndex = -1

            fun openSlice() {
                val f = File(outputDir, "slice_%03d.m4a".format(sliceIndex))
                val m = MediaMuxer(
                    f.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                muxer = m
                trackIndex = m.addTrack(format)
                m.start()
                slicePaths.add(f.absolutePath)
                sliceIndex++
                sliceBytes = 0L
                sliceStartUs = -1L
            }

            fun closeSlice() {
                try {
                    muxer?.stop()
                } catch (ignored: Exception) {
                }
                try {
                    muxer?.release()
                } catch (ignored: Exception) {
                }
                muxer = null
            }

            var totalBytes = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            while (true) {
                // writeSampleData 会移动 buffer 的 position，下次读取前必须复位
                buffer.clear()
                val size = ex.readSampleData(buffer, 0)
                if (size < 0) break

                if (muxer == null) openSlice()
                val timeUs = ex.sampleTime
                if (sliceStartUs < 0) sliceStartUs = timeUs // 归一化：每片从 0 开始

                buffer.position(0)
                buffer.limit(size)
                bufferInfo.set(0, size, timeUs - sliceStartUs, ex.sampleFlags)
                try {
                    muxer?.writeSampleData(trackIndex, buffer, bufferInfo)
                } catch (e: Exception) {
                    Log.w(TAG, "写切片样本失败", e)
                }
                sliceBytes += size
                totalBytes += size
                ex.advance()

                // 进度：每 2MB 或每 1 秒回调一次
                // （M4A 逐帧封装比 ADTS 字节复制慢很多，必须让使用者看到在推进）
                if (totalBytes - lastProgressBytes >= 2 * 1024 * 1024 ||
                    System.currentTimeMillis() - lastProgressMs >= 1000
                ) {
                    lastProgressBytes = totalBytes
                    lastProgressMs = System.currentTimeMillis()
                    val bytesSnapshot = totalBytes
                    val countSnapshot = sliceIndex - 1
                    main.post { callback.onProgress(bytesSnapshot, countSnapshot) }
                }

                // 以文件大小为主切片
                if (sliceBytes >= sizeLimitBytes) {
                    Log.d(TAG, "M4A 切片${sliceIndex - 1}: ${sliceBytes / 1024}KB")
                    closeSlice()
                }
            }
            closeSlice() // 收尾

            if (slicePaths.isEmpty()) {
                main.post { callback.onError("未读取到有效音频帧") }
                return
            }

            Log.d(TAG, "M4A 切片完成，共 ${slicePaths.size} 个")
            main.post { callback.onSliceCompleted(slicePaths) }
        } catch (e: Exception) {
            Log.e(TAG, "M4A切片失败", e)
            main.post { callback.onError("切片失败(M4A): ${e.message}") }
        } finally {
            try {
                muxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                muxer?.release()
            } catch (ignored: Exception) {
            }
            try {
                extractor?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------
    // 一步到位：转码 + 切片一次完成（M4A）
    // ------------------------------------------------------------------

    /** 一步到位回调 */
    interface OneStepCallback {
        fun onSuccess(outputFile: File, durationSec: Float, slicePaths: List<String>)
        fun onError(msg: String)

        /** 进度（已处理 PCM 字节 / 音频秒数 / 已完成切片数） */
        fun onProgress(processedBytes: Long, processedSec: Float, sliceCount: Int) {}
    }

    /**
     * 一步到位：PCM → M4A 转码，并在转码过程中按大小(KB)直接分片输出
     *
     * 相比「先转码再切片」省掉了二次读取(MediaExtractor)与逐帧重新封装的开销，
     * 4.5 小时音频约 25 万帧的 JNI 往返被完全消除，总耗时基本等于只转码一次。
     *
     * 产出：
     * - [outputFile] 完整转码结果（converted.m4a，仍保留，便于核对）
     * - [sliceDir]   下 slice_001.m4a、slice_002.m4a ...（各自带 moov，独立可播，时长精确）
     *
     * @param sizeKb 单个切片大小上限（KB），<=0 表示不限制
     */
    fun convertAndSliceM4a(
        pcmInput: InputStream,
        outputFile: File,
        sliceDir: File,
        sizeKb: Int,
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitrate: Int = 16000,
        sourceChannelCount: Int = 1,
        downmixMode: DownmixMode = DownmixMode.AVERAGE,
        callback: OneStepCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var encoder: MediaCodec? = null
        var fullMuxer: MediaMuxer? = null
        var sliceMuxer: MediaMuxer? = null
        var outFormat: MediaFormat? = null

        try {
            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()
            sliceDir.mkdirs()
            // 只清理旧 M4A 切片，不动完整转码结果
            sliceDir.listFiles()
                ?.filter { it.name.startsWith("slice_") && it.name.endsWith(".m4a") }
                ?.forEach { it.delete() }

            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            // 源 PCM 可能是多声道（如麦克风阵列 8 声道），必须先降混为单声道再编码
            val monoReader = MonoPcmReader(
                pcmInput.buffered(1024 * 1024), sourceChannelCount, downmixMode
            )
            val monoChunk = ByteArray(MonoPcmReader.READ_CHUNK)
            val bufferInfo = MediaCodec.BufferInfo()
            // 降混后是单声道：字节速率 = 采样率 × 2（16bit）
            val bytesPerSec = sampleRate * MonoPcmReader.BYTES_PER_SAMPLE
            var totalPcmBytes = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            var actualSampleRate = sampleRate
            var actualChannels = channelCount
            var fullTrack = -1
            var sliceTrack = -1
            var muxerStarted = false

            val sizeLimitBytes = if (sizeKb > 0) sizeKb * 1024L else Long.MAX_VALUE
            val slicePaths = mutableListOf<String>()
            var sliceIndex = 1
            var sliceBytes = 0L
            var sliceStartUs = -1L

            fun openSliceMuxer(of: MediaFormat) {
                val f = File(sliceDir, "slice_%03d.m4a".format(sliceIndex))
                val m = MediaMuxer(f.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                sliceMuxer = m
                sliceTrack = m.addTrack(of)
                m.start()
                slicePaths.add(f.absolutePath)
                sliceIndex++
                sliceBytes = 0L
                sliceStartUs = -1L
                Log.d(TAG, "一步到位: 开始写切片 ${f.name}")
            }

            fun closeSliceMuxer() {
                try {
                    sliceMuxer?.stop()
                } catch (ignored: Exception) {
                }
                try {
                    sliceMuxer?.release()
                } catch (ignored: Exception) {
                }
                sliceMuxer = null
            }

            /** 首次拿到真实输出格式时建立完整文件轨道；切片写满后自动开下一片 */
            fun ensureMuxers() {
                if (muxerStarted) {
                    if (sliceMuxer == null) {
                        val of = outFormat
                        if (of != null) openSliceMuxer(of)
                    }
                    return
                }
                val of = encoder?.outputFormat ?: return
                if (!of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) return
                outFormat = of
                actualSampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    actualChannels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                val fm = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                fullMuxer = fm
                fullTrack = fm.addTrack(of)
                fm.start()
                openSliceMuxer(of)
                muxerStarted = true
                Log.d(TAG, "一步到位: 轨道就绪 ${actualSampleRate}Hz / ${actualChannels}ch")
            }

            fun drainEncoder(): Boolean {
                var sawEos = false
                var idle = 0
                while (idle++ < MAX_IDLE_LOOP) {
                    val outIndex = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        ?: MediaCodec.INFO_TRY_AGAIN_LATER
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            Log.d(TAG, "一步到位 format: ${encoder?.outputFormat}")
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 已废弃事件，显式忽略避免空转
                        }
                        outIndex >= 0 -> {
                            val size = bufferInfo.size
                            val isCodecConfig =
                                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val outBuffer = encoder?.getOutputBuffer(outIndex)
                            if (outBuffer != null && size > 0 && !isCodecConfig) {
                                ensureMuxers()
                                if (muxerStarted) {
                                    val pts = bufferInfo.presentationTimeUs
                                    if (sliceStartUs < 0) sliceStartUs = pts // 本片时间戳归一化到 0
                                    try {
                                        // 1) 写完整文件
                                        outBuffer.position(bufferInfo.offset)
                                        outBuffer.limit(bufferInfo.offset + size)
                                        fullMuxer?.writeSampleData(fullTrack, outBuffer, bufferInfo)

                                        // 2) 写当前切片（writeSampleData 会移动 position，需复位）
                                        outBuffer.position(bufferInfo.offset)
                                        outBuffer.limit(bufferInfo.offset + size)
                                        val sliceInfo = MediaCodec.BufferInfo()
                                        sliceInfo.set(0, size, pts - sliceStartUs, bufferInfo.flags)
                                        sliceMuxer?.writeSampleData(sliceTrack, outBuffer, sliceInfo)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "一步到位: 写样本失败", e)
                                    }
                                    sliceBytes += size
                                    if (sliceBytes >= sizeLimitBytes) {
                                        Log.d(TAG, "一步到位: 切片${sliceIndex - 1} 完成 ${sliceBytes / 1024}KB")
                                        closeSliceMuxer() // 下一帧 ensureMuxers 会自动开新片
                                    }
                                }
                            }
                            encoder?.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawEos = true
                        }
                        else -> return sawEos
                    }
                }
                Log.w(TAG, "一步到位: drainEncoder 达到空转上限")
                return sawEos
            }

            // 喂 PCM（必须传递增时间戳，容器靠它算时长）
            var ptsUs = 0L
            while (true) {
                val read = monoReader.read(monoChunk)
                if (read <= 0) break
                totalPcmBytes += read

                if (monoReader.totalReadBytes - lastProgressBytes >= 4 * 1024 * 1024 ||
                    System.currentTimeMillis() - lastProgressMs >= 1000
                ) {
                    lastProgressBytes = monoReader.totalReadBytes
                    lastProgressMs = System.currentTimeMillis()
                    // 进度显示源 PCM 读取量（含所有声道），便于对照源文件大小
                    val bytesSnapshot = monoReader.totalReadBytes
                    val secSnapshot = totalPcmBytes.toFloat() / bytesPerSec
                    val countSnapshot = sliceIndex - 1
                    main.post {
                        callback.onProgress(bytesSnapshot, secSnapshot, countSnapshot)
                    }
                }

                var offset = 0
                var idle = 0
                while (offset < read && idle++ < MAX_IDLE_LOOP) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex < 0) {
                        drainEncoder()
                        continue
                    }
                    val inBuffer = encoder?.getInputBuffer(inIndex)
                    if (inBuffer == null) {
                        drainEncoder()
                        continue
                    }
                    idle = 0
                    inBuffer.clear()
                    val len = minOf(inBuffer.capacity(), read - offset)
                    inBuffer.put(monoChunk, offset, len)
                    encoder?.queueInputBuffer(inIndex, 0, len, ptsUs, 0)
                    ptsUs += len.toLong() * 1_000_000L / bytesPerSec
                    offset += len
                    drainEncoder()
                }
            }

            // EOS 并排空
            var eosQueued = false
            var eosDrained = false
            var idle = 0
            while (!eosDrained && idle++ < MAX_IDLE_LOOP) {
                if (!eosQueued) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex >= 0) {
                        encoder?.queueInputBuffer(
                            inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosQueued = true
                    }
                }
                if (drainEncoder()) eosDrained = true
            }
            if (!eosDrained) Log.w(TAG, "一步到位: EOS 排空超时，输出可能不完整")

            // 收尾：先关最后一个切片，再关完整文件
            closeSliceMuxer()
            try {
                fullMuxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                fullMuxer?.release()
            } catch (ignored: Exception) {
            }
            fullMuxer = null

            val durationSec = totalPcmBytes.toFloat() / bytesPerSec
            Log.d(
                TAG, "一步到位完成: 时长=${durationSec}s, 切片=${slicePaths.size} 个, " +
                        "实际=${actualSampleRate}Hz/${actualChannels}ch"
            )
            main.post { callback.onSuccess(outputFile, durationSec, slicePaths) }
        } catch (e: Exception) {
            Log.e(TAG, "一步到位失败", e)
            main.post { callback.onError("一步到位失败: ${e.message}") }
        } finally {
            try {
                sliceMuxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                sliceMuxer?.release()
            } catch (ignored: Exception) {
            }
            try {
                fullMuxer?.stop()
            } catch (ignored: Exception) {
            }
            try {
                fullMuxer?.release()
            } catch (ignored: Exception) {
            }
            try {
                pcmInput.close()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.stop()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * 一步到位（ADTS 版）：PCM → AAC 转码，并在转码过程中按大小(KB)直接分片输出
     *
     * 产出：
     * - [outputFile] 完整转码结果（converted.aac）
     * - [sliceDir]   下 slice_001.aac、slice_002.aac ...
     *
     * 每帧写独立 ADTS 头，切割落在帧边界，切片可独立播放。
     *
     * @param sizeKb 单个切片大小上限（KB），<=0 表示不限制
     */
    fun convertAndSliceAac(
        pcmInput: InputStream,
        outputFile: File,
        sliceDir: File,
        sizeKb: Int,
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitrate: Int = 16000,
        sourceChannelCount: Int = 1,
        downmixMode: DownmixMode = DownmixMode.AVERAGE,
        callback: OneStepCallback
    ) {
        val main = Handler(Looper.getMainLooper())
        var encoder: MediaCodec? = null
        var fullOut: BufferedOutputStream? = null
        var sliceOut: BufferedOutputStream? = null

        try {
            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()
            sliceDir.mkdirs()
            sliceDir.listFiles()
                ?.filter { it.name.startsWith("slice_") && it.name.endsWith(".aac") }
                ?.forEach { it.delete() }

            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            fullOut = FileOutputStream(outputFile).buffered(256 * 1024)

            // 源 PCM 可能是多声道（如麦克风阵列 8 声道），必须先降混为单声道再编码
            val monoReader = MonoPcmReader(
                pcmInput.buffered(1024 * 1024), sourceChannelCount, downmixMode
            )
            val monoChunk = ByteArray(MonoPcmReader.READ_CHUNK)
            val bufferInfo = MediaCodec.BufferInfo()
            // 降混后是单声道：字节速率 = 采样率 × 2（16bit）
            val bytesPerSec = sampleRate * MonoPcmReader.BYTES_PER_SAMPLE
            var totalPcmBytes = 0L
            var lastProgressBytes = 0L
            var lastProgressMs = System.currentTimeMillis()

            var actualSampleRate = sampleRate
            var actualChannels = channelCount

            val sizeLimitBytes = if (sizeKb > 0) sizeKb * 1024L else Long.MAX_VALUE
            val slicePaths = mutableListOf<String>()
            var sliceIndex = 1
            var sliceBytes = 0L
            var frameBuf = ByteArray(1024)

            fun openSlice() {
                val f = File(sliceDir, "slice_%03d.aac".format(sliceIndex))
                sliceOut = FileOutputStream(f).buffered(256 * 1024)
                slicePaths.add(f.absolutePath)
                sliceIndex++
                sliceBytes = 0L
                Log.d(TAG, "一步到位(AAC): 开始写切片 ${f.name}")
            }

            fun closeSlice() {
                try {
                    sliceOut?.flush()
                } catch (ignored: Exception) {
                }
                try {
                    sliceOut?.close()
                } catch (ignored: Exception) {
                }
                sliceOut = null
            }

            /** ADTS 头必须写编码器实际输出的采样率，否则手机播放器时长/播放异常 */
            fun readActualFormat() {
                val of = encoder?.outputFormat ?: return
                if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    actualSampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    actualChannels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            readActualFormat()
            openSlice()

            fun drainEncoder(): Boolean {
                var sawEos = false
                var idle = 0
                while (idle++ < MAX_IDLE_LOOP) {
                    val outIndex = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        ?: MediaCodec.INFO_TRY_AGAIN_LATER
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return sawEos
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            readActualFormat()
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 已废弃事件，显式忽略避免空转
                        }
                        outIndex >= 0 -> {
                            val size = bufferInfo.size
                            val isCodecConfig =
                                bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val outBuffer = encoder?.getOutputBuffer(outIndex)
                            if (outBuffer != null && size > 0 && !isCodecConfig) {
                                val total = size + 7
                                if (frameBuf.size < total) frameBuf = ByteArray(total * 2)
                                writeAdtsHeader(frameBuf, total, actualSampleRate, actualChannels)
                                try {
                                    outBuffer.position(bufferInfo.offset)
                                    outBuffer.limit(bufferInfo.offset + size)
                                    outBuffer.get(frameBuf, 7, size)
                                    // 完整文件与当前切片各写一份
                                    fullOut?.write(frameBuf, 0, total)
                                    if (sliceOut == null) openSlice()
                                    sliceOut?.write(frameBuf, 0, total)
                                    sliceBytes += total
                                    if (sliceBytes >= sizeLimitBytes) {
                                        Log.d(
                                            TAG, "一步到位(AAC): 切片${sliceIndex - 1} 完成 " +
                                                    "${sliceBytes / 1024}KB"
                                        )
                                        closeSlice() // 下一帧自动开新片
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "一步到位(AAC): 写帧异常，跳过", e)
                                }
                            }
                            encoder?.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawEos = true
                        }
                        else -> return sawEos
                    }
                }
                Log.w(TAG, "一步到位(AAC): drainEncoder 达到空转上限")
                return sawEos
            }

            // 喂 PCM（ADTS 裸流不依赖时间戳，传 0 即可）
            while (true) {
                val read = monoReader.read(monoChunk)
                if (read <= 0) break
                totalPcmBytes += read

                if (monoReader.totalReadBytes - lastProgressBytes >= 4 * 1024 * 1024 ||
                    System.currentTimeMillis() - lastProgressMs >= 1000
                ) {
                    lastProgressBytes = monoReader.totalReadBytes
                    lastProgressMs = System.currentTimeMillis()
                    // 进度显示源 PCM 读取量（含所有声道），便于对照源文件大小
                    val bytesSnapshot = monoReader.totalReadBytes
                    val secSnapshot = totalPcmBytes.toFloat() / bytesPerSec
                    val countSnapshot = sliceIndex - 1
                    main.post {
                        callback.onProgress(bytesSnapshot, secSnapshot, countSnapshot)
                    }
                }

                var offset = 0
                var idle = 0
                while (offset < read && idle++ < MAX_IDLE_LOOP) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex < 0) {
                        drainEncoder()
                        continue
                    }
                    val inBuffer = encoder?.getInputBuffer(inIndex)
                    if (inBuffer == null) {
                        drainEncoder()
                        continue
                    }
                    idle = 0
                    inBuffer.clear()
                    val len = minOf(inBuffer.capacity(), read - offset)
                    inBuffer.put(monoChunk, offset, len)
                    encoder?.queueInputBuffer(inIndex, 0, len, 0, 0)
                    offset += len
                    drainEncoder()
                }
            }

            // EOS 并排空
            var eosQueued = false
            var eosDrained = false
            var idle = 0
            while (!eosDrained && idle++ < MAX_IDLE_LOOP) {
                if (!eosQueued) {
                    val inIndex = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inIndex >= 0) {
                        encoder?.queueInputBuffer(
                            inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        eosQueued = true
                    }
                }
                if (drainEncoder()) eosDrained = true
            }
            if (!eosDrained) Log.w(TAG, "一步到位(AAC): EOS 排空超时，输出可能不完整")

            closeSlice()
            try {
                fullOut?.flush()
            } catch (ignored: Exception) {
            }
            try {
                fullOut?.close()
            } catch (ignored: Exception) {
            }
            fullOut = null

            val durationSec = totalPcmBytes.toFloat() / bytesPerSec
            Log.d(
                TAG, "一步到位(AAC)完成: 时长=${durationSec}s, 切片=${slicePaths.size} 个, " +
                        "实际=${actualSampleRate}Hz/${actualChannels}ch"
            )
            main.post { callback.onSuccess(outputFile, durationSec, slicePaths) }
        } catch (e: Exception) {
            Log.e(TAG, "一步到位(AAC)失败", e)
            main.post { callback.onError("一步到位失败(AAC): ${e.message}") }
        } finally {
            try {
                sliceOut?.close()
            } catch (ignored: Exception) {
            }
            try {
                fullOut?.close()
            } catch (ignored: Exception) {
            }
            try {
                pcmInput.close()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.stop()
            } catch (ignored: Exception) {
            }
            try {
                encoder?.release()
            } catch (ignored: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------
    // 结果文件删除
    // ------------------------------------------------------------------

    /** 删除结果 */
    data class DeleteResult(
        /** 成功删除的文件数 */
        val deletedCount: Int,
        /** 删除失败的文件路径 */
        val failedPaths: List<String>
    )

    /**
     * 删除转码与切片的结果文件
     *
     * @param urls       指定要删除的文件路径列表；
     *                   传入空列表时，删除 [convertDir] 与 [sliceDir] 下【所有】结果文件
     * @param convertDir 转码结果存储目录
     * @param sliceDir   切片结果存储目录
     * @return DeleteResult(成功删除数, 失败路径列表)
     */
    fun deleteResults(urls: ArrayList<String>, convertDir: File, sliceDir: File): DeleteResult {
        var deleted = 0
        val failed = mutableListOf<String>()

        fun deleteOne(f: File) {
            try {
                if (!f.exists()) return            // 已不存在，跳过不计数
                if (f.delete()) deleted++ else failed.add(f.absolutePath)
            } catch (e: Exception) {
                failed.add(f.absolutePath)
            }
        }

        val validUrls = urls.filter { it.isNotBlank() }
        if (validUrls.isEmpty()) {
            // 传空 → 删除两个目录下所有结果文件
            // 用 FileTreeWalk 惰性遍历，不 toList，避免文件极多时占用内存
            deleteFilesIn(convertDir) { deleteOne(it) }
            deleteFilesIn(sliceDir) { deleteOne(it) }
        } else {
            validUrls.forEach { deleteOne(File(it)) }
        }

        Log.d(TAG, "deleteResults: 删除 $deleted 个，失败 ${failed.size} 个")
        return DeleteResult(deleted, failed)
    }

    /** 惰性递归遍历目录下的文件并交给 [action] 处理（不构建完整列表，避免内存堆积） */
    private fun deleteFilesIn(dir: File, action: (File) -> Unit) {
        if (!dir.exists()) return
        try {
            dir.walkTopDown().forEach { f ->
                if (f.isFile) action(f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "遍历目录失败: ${dir.absolutePath}", e)
        }
    }
}
