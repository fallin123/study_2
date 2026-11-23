package com.voyah.tvad.audio

import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.study.test.R
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 音频处理 Demo：PCM → AAC 转码 + AAC 切片
 *
 * 两个输入框：
 * - etSliceSize     切片大小（KB），1024 = 1024KB
 * - etSliceSize 切片大小（KB）；按时长切片已移除，界面只保留大小
 *
 * 六个按钮：
 * - btnConvert 开始转格式（PCM → AAC/M4A），转成功后 btnSlice 才可用
 * - btnSlice   开始切片，完成后通过 SliceCallback 回调所有切片本地路径
 * - btnOneStep 一步到位（转码 + 切片一次完成）
 * - btnM4aFast 高性能版 PCM → M4A（不切片），与 btnConvert 的 M4A 结果等价，仅内部实现优化
 * - btnDiag    诊断：打印本机 AAC 编码器是硬编还是软编
 * - btnClear   清理结果文件（调用 PcmAacProcessor.deleteResults）
 *
 * 健壮性：后台任务回调统一经 safeUi 分发，Activity 销毁后不再触碰 View/Context，
 * 避免回调持有已销毁 UI 造成泄漏或空指针。
 */
class AudioProcessDemoActivity : AppCompatActivity() {

    // ======================= 可配置区域（开发者直接改这里） =======================
    companion object {
        /**
         * 源 PCM 文件路径。
         * 以 "/" 开头 = 绝对路径；否则相对 App 私有外部存储目录(getExternalFilesDir)
         *
         * out_vad_asr_10-14_16-12-46-308
         * out_vad_asr_10-09_15-14-25-222
         */
        const val PCM_SOURCE_PATH = "out_vad_asr_10-14_16-12-46-308.pcm"//out_vad_asr_10-09_15-14-25-222

        /**
         * 转码结果存储目录。
         * {ts} 会替换为本次任务时间戳(yyyyMMdd_HHmmss)；不想要时间戳子目录就写 "convert"
         */
        const val CONVERT_OUTPUT_DIR = "audioConvert/out_vad_asr_{ts}"

        /**
         * 切片结果存储目录。
         * {ts} 与转码同批次一致，便于按批次对应核对
         */
        const val SLICE_OUTPUT_DIR = "audioConvert/out_vad_asr_{ts}/slice"

        // ---------------- 源 PCM 格式（务必与采集端一致，填错会转出噪音） ----------------

        /** 源 PCM 采样率(Hz) */
        const val PCM_SAMPLE_RATE = 16000

        /** 源 PCM 位深(bit)，当前实现按 16bit(每采样 2 字节)处理 */
        const val PCM_BIT_DEPTH = 16

        /** 源 PCM 声道数。麦克风阵列常见 8 声道 */
        const val PCM_CHANNEL_COUNT = 8

        // ---------------- 输出编码格式 ----------------

        /** 输出采样率(Hz)，与源保持一致（当前未做重采样，如需不同采样率要另加重采样） */
        const val OUTPUT_SAMPLE_RATE = 16000

        /**
         * 输出声道数。Android AAC 编码器只支持 1~2 声道，
         * 因此 8 声道源文件会先降混到该声道数再编码
         */
        const val OUTPUT_CHANNEL_COUNT = 1

        /** 输出码率(bps) */
        const val OUTPUT_BITRATE = 16000

        /**
         * 多声道降混策略：
         * - [PcmAacProcessor.DownmixMode.AVERAGE] 各声道取平均（推荐，保留全部麦克风信息）
         * - [PcmAacProcessor.DownmixMode.FIRST]   只取第 1 声道
         */
        val DOWNMIX_MODE: PcmAacProcessor.DownmixMode = PcmAacProcessor.DownmixMode.AVERAGE

        // ---------------- 输出文件名 ----------------

        /** 转码输出文件名（ADTS 封装） */
        const val CONVERT_FILE_NAME = "converted.aac"

        /** 转码输出文件名（M4A 容器） */
        const val CONVERT_FILE_NAME_M4A = "converted.m4a"

        /** 高性能版 M4A 输出目录，与常规转码分开，便于对比耗时 */
        const val FAST_M4A_OUTPUT_DIR = "audioConvert/fast_m4a_{ts}"

        /** 高性能版 M4A 输出文件名 */
        const val FAST_M4A_FILE_NAME = "fast.m4a"

        /**
         * 高性能版节流参数：每累计消费满一个大块源数据（256KB，约 1.02 秒音频）后主动让出的毫秒数。
         *
         * 「占用率 ↔ 耗时」换算表（HUAWEI CET-AL00 实测校准，占用率 ≈ 16% × 31 / (31 + 该值)）：
         *   0   → ~16%，总耗时最短
         *   135 → ~3%（实测，AIDL 管道 + 后台优先级下的工作块 ≈ 31ms）
         *   220 → ~2%（当前默认）
         *   460 → ~1%
         * 校准注意：工作块 W 受两因素影响——线程后台优先级（小核变慢）与数据源返回粒度
         * （管道 64KB/次 vs 文件 256KB/次），换设备或改数据源后需重新实测校准。
         */
        const val FAST_THROTTLE_MS = 135

        /**
         * PCM 数据来源开关（仅影响「高性能转 M4A」）：
         * - true  通过 AIDL 从提供端进程拉流，转码端不再读本地文件
         * - false 旧行为：读 PCM_SOURCE_PATH 指向的本地文件
         */
        const val USE_AIDL_PCM_SOURCE = false

        /** PCM 提供端：独立进程应用（:pcm_provider 模块）的包名与服务类名 */
        const val PCM_PROVIDER_PACKAGE = "com.sgm.pcmprovider"
        const val PCM_PROVIDER_SERVICE = "com.study.pcmprovider.PcmProviderService"

        private const val TAG = "AudioProcessDemo"
    }
    // =============================================================================

    private lateinit var etSliceSize: EditText
    private lateinit var rgFormat: RadioGroup
    private lateinit var btnConvert: Button
    private lateinit var btnSlice: Button
    private lateinit var btnOneStep: Button
    private lateinit var btnM4aFast: Button
    private lateinit var btnDiag: Button
    private lateinit var btnRecv: Button
    private lateinit var btnClear: Button
    private lateinit var rbM4a: RadioButton
    private lateinit var tvStatus: TextView
    private lateinit var tvParams: TextView
    private lateinit var tvResult: TextView

    /**
     * 转码/切片专用后台线程。
     *
     * 设为 THREAD_PRIORITY_BACKGROUND：转码是批量任务，不要求实时性，
     * 低优先级会让调度器把它挪到小核、并在与导航/语音等前台任务争抢时让路。
     * 注意：总 CPU 时间和占用率数字不会变，变的是「影响」——
     * 转码期间 UI、语音链路的卡顿会明显减轻。
     */
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }.apply { name = "audio-convert-bg" }
    }
    private var convertedFile: File? = null

    /** 编码器实际输出的格式（可能与请求的 16kHz 不同），转码成功后显示用于核对 */
    private var actualFormatDesc = ""

    /** 本次任务时间戳，转码与切片共用，保证同批次目录可对应 */
    private var sessionStamp: String = ""
    private var convertDir: File? = null
    private var sliceDir: File? = null

    /*private fun pcmSourceFile(): File {
        return File(filesDir, PCM_SOURCE_PATH)
    }*/


    /*private fun resolvePath(path: String, stamp: String): File {
        val p = path.replace("{ts}", stamp)
        return if (p.startsWith("/")) File(p) else File(filesDir, p)
    }*/

    /** 解析路径：以 "/" 开头视为绝对路径，否则相对 App 私有外部存储目录 */
    private fun resolvePath(path: String, stamp: String): File {
        val p = path.replace("{ts}", stamp)
        return if (p.startsWith("/")) File(p) else File(getExternalFilesDir(null), p)
    }

    private fun nowStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    /** 源 PCM 文件（仅本地文件，assets 逻辑已移除） */
    private fun pcmSourceFile(): File = resolvePath(PCM_SOURCE_PATH, sessionStamp)

    /** 打开源 PCM 流，文件不存在返回 null */
    private fun openPcmStream(): InputStream? {
        val f = pcmSourceFile()
        if (!f.exists() || f.length() <= 0) return null
        return try {
            FileInputStream(f)
        } catch (e: Exception) {
            Log.w(TAG, "打开 PCM 失败", e)
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_process)

        etSliceSize = findViewById(R.id.etSliceSize)
        rgFormat = findViewById(R.id.rgFormat)
        btnConvert = findViewById(R.id.btnConvert)
        btnSlice = findViewById(R.id.btnSlice)
        btnOneStep = findViewById(R.id.btnOneStep)
        btnM4aFast = findViewById(R.id.btnM4aFast)
        btnDiag = findViewById(R.id.btnDiag)
        btnRecv = findViewById(R.id.btnRecv)
        btnClear = findViewById(R.id.btnClear)
        rbM4a = findViewById(R.id.rbM4a)
        tvStatus = findViewById(R.id.tvStatus)
        tvParams = findViewById(R.id.tvParams)
        tvResult = findViewById(R.id.tvResult)

        // 参数说明跟随顶部配置自动生成，改了配置这里自动同步，不会出现"配置与文案不符"
        tvParams.text = "源 PCM：${PCM_SAMPLE_RATE}Hz / ${PCM_CHANNEL_COUNT}声道 / ${PCM_BIT_DEPTH}bit\n" +
                "降混：${DOWNMIX_MODE.name} → 输出：${OUTPUT_SAMPLE_RATE}Hz / " +
                "${OUTPUT_CHANNEL_COUNT}声道 / AAC-LC / ${OUTPUT_BITRATE / 1000}kbps"

        // 先按当前时间解析出目录，保证未转码时清理按钮也可用
        sessionStamp = nowStamp()
        convertDir = resolvePath(CONVERT_OUTPUT_DIR, sessionStamp)
        sliceDir = resolvePath(SLICE_OUTPUT_DIR, sessionStamp)

        btnConvert.setOnClickListener { startConvert() }
        btnSlice.setOnClickListener { startSlice() }
        btnOneStep.setOnClickListener { startOneStep() }
        btnM4aFast.setOnClickListener { startFastM4a() }
        btnDiag.setOnClickListener { runDiagnostics() }
        btnRecv.setOnClickListener { startReceiveOnly() }
        btnClear.setOnClickListener { clearResults() }

        // 封装格式切换 → 按钮文案与实际调用的方法同步联动
        rgFormat.setOnCheckedChangeListener { _, _ -> updateButtonTexts() }
        updateButtonTexts()

        tvStatus.text = "状态：待操作\n源文件：${pcmSourceFile().absolutePath}"
    }

    /**
     * 按钮文案跟随封装格式联动：ADTS(.aac) / M4A(.m4a)
     * 文案与实际调用的方法保持一致，避免"按钮写着 AAC 却输出 M4A"
     */
    private fun updateButtonTexts() {
        val label = if (rbM4a.isChecked) "M4A" else "AAC"
        btnConvert.text = "开始转格式（PCM → $label）"
        btnSlice.text = "开始切片（$label）"
        btnOneStep.text = "一步到位（转码+切片 $label）"
    }

    /** 校验输入：切片大小必须为纯正整数（按时长切片已移除） */
    private fun readSizeKb(): Int? {
        val sizeText = etSliceSize.text.toString().trim()
        if (sizeText.isEmpty()) {
            toast("请填写切片大小(KB)")
            return null
        }
        val size = sizeText.toIntOrNull()
        if (size == null || size <= 0) {
            toast("请输入合法的正整数")
            return null
        }
        return size
    }

    // ------------------------------------------------------------------
    // 转格式
    // ------------------------------------------------------------------

    private fun startConvert() {
        val sizeKb = readSizeKb() ?: return
        Log.d(TAG, "切片参数: sizeKb=$sizeKb")

        // 新批次：更新时间戳与输出目录
        sessionStamp = nowStamp()
        val cDir = resolvePath(CONVERT_OUTPUT_DIR, sessionStamp).apply { mkdirs() }
        val sDir = resolvePath(SLICE_OUTPUT_DIR, sessionStamp).apply { mkdirs() }
        convertDir = cDir
        sliceDir = sDir
        val useM4a = rbM4a.isChecked
        val outFile = File(cDir, if (useM4a) CONVERT_FILE_NAME_M4A else CONVERT_FILE_NAME)
        convertedFile = outFile

        btnConvert.isEnabled = false
        btnSlice.isEnabled = false
        tvResult.text = ""

        val src = pcmSourceFile()
        val sourceDesc = if (src.exists() && src.length() > 0) {
            "本地文件(${"%.0f".format(src.length() / 1024f / 1024f)}MB)"
        } else {
            "文件不存在"
        }
        val label = if (useM4a) "M4A" else "AAC"
        tvStatus.text = "状态：正在转码 PCM → $label ...\n来源：$sourceDesc\n" +
                "转码目录：${cDir.absolutePath}\n切片目录：${sDir.absolutePath}"

        executor.execute {
            val pcmStream = openPcmStream()
            if (pcmStream == null) {
                safeUi {
                    toast("找不到 PCM 文件：${src.absolutePath}")
                    btnConvert.isEnabled = true
                    tvStatus.text = "状态：找不到 PCM 文件\n请检查 PCM_SOURCE_PATH 配置"
                }
                return@execute
            }
            Log.d(TAG, "PCM 来源: ${src.absolutePath}")

            val startMs = System.currentTimeMillis()
            val convertCallback = object : PcmAacProcessor.ConvertCallback {
                    override fun onProgress(processedBytes: Long, processedSec: Float) {
                        safeUi {
                            tvStatus.text = "状态：正在转码 PCM → $label ...\n" +
                                    "已处理 ${"%.1f".format(processedBytes / 1024f / 1024f)}MB " +
                                    "(${processedSec.toInt()}秒音频)\n" +
                                    "转码目录：${cDir.absolutePath}"
                        }
                    }

                    override fun onActualFormat(sampleRate: Int, channelCount: Int) {
                        actualFormatDesc = "${sampleRate}Hz / ${channelCount}ch"
                        Log.d(TAG, "编码器实际输出: $actualFormatDesc")
                    }

                    override fun onConvertSuccess(outputFile: File, totalSec: Float) {
                        val elapsedMs = System.currentTimeMillis() - startMs
                        val speed = if (elapsedMs > 0) totalSec * 1000f / elapsedMs else 0f
                        Log.d(TAG, "转码完成: 耗时=${elapsedMs}ms, 音频=${totalSec.toInt()}s")
                        safeUi {
                            tvStatus.text = "状态：转码完成 ✓ 音频时长 ${"%.1f".format(totalSec)}秒\n" +
                                    "耗时 ${elapsedMs}ms\n可开始切片"
                            tvResult.text = "AAC 文件：${outputFile.absolutePath}\n" +
                                    "实际编码格式：$actualFormatDesc\n" +
                                    "耗时：${elapsedMs}ms（约 ${"%.2f".format(speed)}x 实时）"
                            btnSlice.isEnabled = true
                            btnConvert.isEnabled = true
                            toast("转码成功，耗时 ${elapsedMs}ms")
                        }
                    }

                    override fun onError(msg: String) {
                        safeUi {
                            tvStatus.text = "状态：转码失败 - $msg"
                            btnConvert.isEnabled = true
                            toast(msg)
                        }
                    }
            }

            if (useM4a) {
                PcmAacProcessor.convertPcmToM4a(
                    pcmStream, outFile,
                    sampleRate = OUTPUT_SAMPLE_RATE,
                    channelCount = OUTPUT_CHANNEL_COUNT,
                    bitrate = OUTPUT_BITRATE,
                    sourceChannelCount = PCM_CHANNEL_COUNT,
                    downmixMode = DOWNMIX_MODE,
                    callback = convertCallback
                )
            } else {
                PcmAacProcessor.convertPcmToAac(
                    pcmStream, outFile,
                    sampleRate = OUTPUT_SAMPLE_RATE,
                    channelCount = OUTPUT_CHANNEL_COUNT,
                    bitrate = OUTPUT_BITRATE,
                    sourceChannelCount = PCM_CHANNEL_COUNT,
                    downmixMode = DOWNMIX_MODE,
                    callback = convertCallback
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // 切片
    // ------------------------------------------------------------------

    private fun startSlice() {
        val sizeKb = readSizeKb() ?: return
        val useM4a = rbM4a.isChecked

        val file = convertedFile
        val sDir = sliceDir
        if (file == null || sDir == null) {
            toast("请先完成转码")
            return
        }
        if (!file.exists()) {
            toast("转码文件不存在，请先转码")
            return
        }

        btnSlice.isEnabled = false
        btnConvert.isEnabled = false
        tvStatus.text = "状态：正在切片 ...\n切片目录：${sDir.absolutePath}"

        executor.execute {
            val sliceCallback = object : PcmAacProcessor.SliceCallback {
                    override fun onProgress(processedBytes: Long, sliceCount: Int) {
                        safeUi {
                            tvStatus.text = "状态：正在切片 ...\n" +
                                    "已处理 ${"%.1f".format(processedBytes / 1024f / 1024f)}MB" +
                                    "（已完成 $sliceCount 片）\n" +
                                    "切片目录：${sDir.absolutePath}"
                        }
                    }

                    override fun onSliceCompleted(slicePaths: List<String>) {
                        safeUi {
                            btnSlice.isEnabled = true
                            btnConvert.isEnabled = true
                            tvStatus.text = "状态：切片完成 ✓ 共 ${slicePaths.size} 个切片"
                            // 回调所有切片的本地地址
                            tvResult.text = slicePaths.joinToString("\n") { it }
                            Log.d(TAG, "切片路径回调: $slicePaths")
                            toast("切片完成，共 ${slicePaths.size} 个")
                        }
                    }

                    override fun onError(msg: String) {
                        safeUi {
                            btnSlice.isEnabled = true
                            btnConvert.isEnabled = true
                            tvStatus.text = "状态：切片失败 - $msg"
                            toast(msg)
                        }
                    }
            }

            if (useM4a) {
                PcmAacProcessor.sliceM4a(file, sDir, sizeKb, sliceCallback)
            } else {
                PcmAacProcessor.sliceAac(file, sDir, sizeKb, sliceCallback)
            }
        }
    }

    // ------------------------------------------------------------------
    // 一步到位：转码 + 切片一次完成（M4A）
    // ------------------------------------------------------------------

    /**
     * 一步到位：转码的同时按大小直接分片输出 M4A。
     * 省掉「先转码再切片」的二次读取（MediaExtractor）与逐帧重新封装开销。
     * 完整文件 converted.m4a 仍会保留，便于核对。
     */
    private fun startOneStep() {
        val sizeKb = readSizeKb() ?: return
        val useM4a = rbM4a.isChecked
        val label = if (useM4a) "M4A" else "AAC"

        sessionStamp = nowStamp()
        val cDir = resolvePath(CONVERT_OUTPUT_DIR, sessionStamp).apply { mkdirs() }
        val sDir = resolvePath(SLICE_OUTPUT_DIR, sessionStamp).apply { mkdirs() }
        convertDir = cDir
        sliceDir = sDir
        val outFile = File(cDir, if (useM4a) CONVERT_FILE_NAME_M4A else CONVERT_FILE_NAME)
        convertedFile = outFile

        btnConvert.isEnabled = false
        btnSlice.isEnabled = false
        btnOneStep.isEnabled = false
        tvResult.text = ""

        val src = pcmSourceFile()
        tvStatus.text = "状态：一步到位（转码 + 切片 $label）...\n" +
                "转码目录：${cDir.absolutePath}\n切片目录：${sDir.absolutePath}"

        executor.execute {
            val pcmStream = openPcmStream()
            if (pcmStream == null) {
                safeUi {
                    toast("找不到 PCM 文件：${src.absolutePath}")
                    btnConvert.isEnabled = true
                    btnOneStep.isEnabled = true
                    tvStatus.text = "状态：找不到 PCM 文件\n请检查 PCM_SOURCE_PATH 配置"
                }
                return@execute
            }

            val oneStepCallback = object : PcmAacProcessor.OneStepCallback {
                    override fun onProgress(
                        processedBytes: Long,
                        processedSec: Float,
                        sliceCount: Int
                    ) {
                        safeUi {
                            tvStatus.text = "状态：一步到位（转码 + 切片 $label）...\n" +
                                    "已处理 ${"%.1f".format(processedBytes / 1024f / 1024f)}MB " +
                                    "(${processedSec.toInt()}秒音频，已完成 $sliceCount 片)\n" +
                                    "切片目录：${sDir.absolutePath}"
                        }
                    }

                    override fun onSuccess(
                        outputFile: File,
                        durationSec: Float,
                        slicePaths: List<String>
                    ) {
                        safeUi {
                            tvStatus.text = "状态：一步到位完成 ✓ 音频时长 " +
                                    "${"%.1f".format(durationSec)}秒，共 ${slicePaths.size} 个切片"
                            tvResult.text = "完整文件：${outputFile.absolutePath}\n" +
                                    slicePaths.joinToString("\n") { it }
                            btnConvert.isEnabled = true
                            btnSlice.isEnabled = true
                            btnOneStep.isEnabled = true
                            toast("一步到位完成，共 ${slicePaths.size} 个切片")
                        }
                    }

                    override fun onError(msg: String) {
                        safeUi {
                            tvStatus.text = "状态：一步到位失败 - $msg"
                            btnConvert.isEnabled = true
                            btnOneStep.isEnabled = true
                            toast(msg)
                        }
                    }
            }

            // 按封装格式选择对应实现，保证与按钮文案一致
            if (useM4a) {
                PcmAacProcessor.convertAndSliceM4a(
                    pcmStream, outFile, sDir, sizeKb,
                    sampleRate = OUTPUT_SAMPLE_RATE,
                    channelCount = OUTPUT_CHANNEL_COUNT,
                    bitrate = OUTPUT_BITRATE,
                    sourceChannelCount = PCM_CHANNEL_COUNT,
                    downmixMode = DOWNMIX_MODE,
                    callback = oneStepCallback
                )
            } else {
                PcmAacProcessor.convertAndSliceAac(
                    pcmStream, outFile, sDir, sizeKb,
                    sampleRate = OUTPUT_SAMPLE_RATE,
                    channelCount = OUTPUT_CHANNEL_COUNT,
                    bitrate = OUTPUT_BITRATE,
                    sourceChannelCount = PCM_CHANNEL_COUNT,
                    downmixMode = DOWNMIX_MODE,
                    callback = oneStepCallback
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // 高性能版：PCM → M4A（不切片）
    // ------------------------------------------------------------------

    /**
     * 调用 [PcmAacProcessor.convertPcmToM4aFast]，与「开始转格式（M4A）」输出格式一致，
     * 只替换内部数据处理方式，用于对比耗时：
     * - 降混 → ShortBuffer 批量搬运 + 纯数组求和
     * - 降混结果直接写编码器输入缓冲，不产生中间 mono 数组
     * - 源文件 256KB 大块直读，不套 BufferedInputStream
     * - 输出轮询非阻塞，消除每块末尾固定 10ms 空等
     * - **muxer 先写内部存储再整体搬迁**（下面单独说明）
     */
    private fun startFastM4a() {
        sessionStamp = nowStamp()
        val cDir = resolvePath(FAST_M4A_OUTPUT_DIR, sessionStamp).apply { mkdirs() }
        val outFile = File(cDir, FAST_M4A_FILE_NAME)

        // 关键优化：MediaMuxer 先写内部存储 /data/data（无 FUSE 开销），转完再整体拷到外部目标。
        // 实测把 muxer 输出直接写 /storage/emulated 时，每帧 writeSampleData 约 300µs
        // （每次小写入都要穿透 FUSE），占总转码耗时的 67%，是真正的瓶颈。
        // 而 File.copyTo 是 256KB 顺序大块写，同样数据量只需几百毫秒。
        val tmpDir = File(filesDir, "audioConvert/tmp_$sessionStamp").apply { mkdirs() }
        val tmpFile = File(tmpDir, FAST_M4A_FILE_NAME)

        convertDir = cDir
        convertedFile = outFile

        btnConvert.isEnabled = false
        btnSlice.isEnabled = false
        btnOneStep.isEnabled = false
        btnM4aFast.isEnabled = false
        tvResult.text = ""

        val src = pcmSourceFile()
        val sourceDesc = if (USE_AIDL_PCM_SOURCE) "AIDL($PCM_PROVIDER_PACKAGE)" else src.absolutePath
        tvStatus.text = "状态：转 M4A（高性能版）...\n" +
                "来源：$sourceDesc\n输出：${outFile.absolutePath}\n" +
                "转码临时目录（内部存储）：${tmpDir.absolutePath}"

        executor.execute {
            // PCM 来源：AIDL 跨进程拉流 / 本地文件（开关 USE_AIDL_PCM_SOURCE）
            val pcmStream: InputStream
            if (USE_AIDL_PCM_SOURCE) {
                try {
                    val aidl = PcmAidlStream(this, PCM_PROVIDER_PACKAGE, PCM_PROVIDER_SERVICE)
                    aidl.connectBlocking()
                    pcmStream = aidl
                } catch (e: Exception) {
                    Log.e(TAG, "AIDL PCM 源连接失败", e)
                    safeUi {
                        toast("AIDL 连接失败：${e.message}")
                        btnConvert.isEnabled = true
                        btnM4aFast.isEnabled = true
                        tvStatus.text = "状态：AIDL 连接失败\n${e.message}\n" +
                                "请确认提供端应用（$PCM_PROVIDER_PACKAGE）已安装且其 PCM_SOURCE_PATH 有文件"
                    }
                    return@execute
                }
            } else {
                val local = openPcmStream()
                if (local == null) {
                    safeUi {
                        toast("找不到 PCM 文件：${src.absolutePath}")
                        btnConvert.isEnabled = true
                        btnM4aFast.isEnabled = true
                        tvStatus.text = "状态：找不到 PCM 文件\n请检查 PCM_SOURCE_PATH 配置"
                    }
                    return@execute
                }
                pcmStream = local
            }
            Log.d(TAG, "高性能版 PCM 来源: $sourceDesc")

            val startMs = System.currentTimeMillis()
            // 转码前快照各线程累计 CPU 时间，结束后做差 → 定位不受节流控制的 CPU 地板在哪个线程
            val threadCpuStart = snapshotThreadCpu()
            val callback = object : PcmAacProcessor.ConvertCallback {
                override fun onProgress(processedBytes: Long, processedSec: Float) {
                    safeUi {
                        tvStatus.text = "状态：转 M4A（高性能版）...\n" +
                                "已处理 ${processedBytes / 1024 / 1024}MB " +
                                "(${processedSec.toInt()}秒音频)\n" +
                                "输出：${outFile.absolutePath}"
                    }
                }

                override fun onActualFormat(sampleRate: Int, channelCount: Int) {
                    actualFormatDesc = "${sampleRate}Hz / ${channelCount}ch"
                    Log.d(TAG, "高性能版实际输出: $actualFormatDesc")
                }

                override fun onConvertSuccess(outputFile: File, totalSec: Float) {
                    // Activity 已销毁：没必要再搬，直接清掉内部临时产物
                    if (isFinishing || isDestroyed) {
                        outputFile.delete()
                        tmpDir.delete()
                        return
                    }
                    val convertMs = System.currentTimeMillis() - startMs
                    // 线程级 CPU 归因：节流后仍降不下去的那部分 CPU 在哪个线程上
                    val cpuNow = snapshotThreadCpu()
                    val topThreads = cpuNow.mapNotNull { (name, now) ->
                        threadCpuStart[name]?.let { start -> (now - start) to name }
                    }.filter { it.first > 0 }.sortedByDescending { it.first }.take(8)
                    Log.i(
                        TAG, "高性能版线程 CPU 归因（总 ${convertMs}ms, 节流=${FAST_THROTTLE_MS}ms/块）: " +
                                topThreads.joinToString { "${it.second}=${it.first}ms" }
                    )
                    // 回调在主线程，搬迁放回后台线程，避免大文件拷贝卡 UI
                    val submitted = runCatching {
                        executor.execute { copyBackAndFinish(outputFile, outFile, tmpDir, totalSec, convertMs) }
                    }.isSuccess
                    if (!submitted) {
                        Log.w(TAG, "搬迁任务提交失败（executor 已关闭）")
                    }
                }

                override fun onError(msg: String) {
                    // 清理内部存储里的临时产物
                    if (tmpFile.exists()) tmpFile.delete()
                    tmpDir.delete()
                    safeUi {
                        tvStatus.text = "状态：高性能版转码失败 - $msg"
                        btnConvert.isEnabled = true
                        btnM4aFast.isEnabled = true
                        toast(msg)
                    }
                }
            }

            PcmAacProcessor.convertPcmToM4aFast(
                pcmStream, tmpFile,
                sampleRate = OUTPUT_SAMPLE_RATE,
                channelCount = OUTPUT_CHANNEL_COUNT,
                bitrate = OUTPUT_BITRATE,
                sourceChannelCount = PCM_CHANNEL_COUNT,
                downmixMode = DOWNMIX_MODE,
                throttleMsPerBlock = FAST_THROTTLE_MS,
                callback = callback
            )
        }
    }

    /**
     * 把内部存储里的临时 M4A 搬到外部目标路径，然后清理临时产物并刷新 UI。
     *
     * 之所以「先写内部存储再拷贝」：MediaMuxer.writeSampleData 是逐帧小写入，
     * 直接写 /storage/emulated（FUSE）时每帧约 300µs，占总耗时 67%；
     * 而 File.copyTo 是 256KB 顺序大块写，同样数据量只需几百毫秒。
     *
     * 运行在后台线程（executor），UI 更新经 [safeUi] 回主线程。
     */
    private fun copyBackAndFinish(
        tmpFile: File,
        target: File,
        tmpDir: File,
        totalSec: Float,
        convertMs: Long
    ) {
        val copyStart = System.currentTimeMillis()
        var copyError: String? = null
        try {
            target.parentFile?.mkdirs()
            tmpFile.copyTo(target, overwrite = true, bufferSize = 256 * 1024)
        } catch (e: Exception) {
            copyError = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "临时文件搬迁失败", e)
        }
        val copyMs = System.currentTimeMillis() - copyStart
        if (copyError == null) {
            if (!tmpFile.delete()) Log.w(TAG, "临时文件删除失败: ${tmpFile.absolutePath}")
            tmpDir.delete()
        }
        Log.d(TAG, "高性能版搬迁完成: 转码=${convertMs}ms, 拷贝=${copyMs}ms, 失败=$copyError")

        val elapsedMs = convertMs + copyMs
        val speed = if (elapsedMs > 0) totalSec * 1000f / elapsedMs else 0f
        safeUi {
            if (copyError == null) {
                tvStatus.text = "状态：高性能版转码完成 ✓ " +
                        "音频时长 ${totalSec.toInt()}秒，耗时 ${elapsedMs}ms"
                tvResult.text = "M4A 文件：${target.absolutePath}\n" +
                        "实际编码格式：$actualFormatDesc\n" +
                        "转码耗时：${convertMs}ms（内部存储直写）\n" +
                        "搬迁耗时：${copyMs}ms\n" +
                        "合计：${elapsedMs}ms（约 ${"%.2f".format(speed)}x 实时）\n" +
                        "节流：${FAST_THROTTLE_MS}ms/块（占用率换耗时的旋钮）"
                toast("转码成功，耗时 ${elapsedMs}ms")
            } else {
                tvStatus.text = "状态：转码成功但搬迁失败 - $copyError\n" +
                        "临时文件仍在：${tmpFile.absolutePath}"
                toast("搬迁失败：$copyError")
            }
            btnConvert.isEnabled = true
            btnSlice.isEnabled = true
            btnOneStep.isEnabled = true
            btnM4aFast.isEnabled = true
        }
    }

    // ------------------------------------------------------------------
    // 测速：仅接收 PCM（AIDL 管道），不转码
    // ------------------------------------------------------------------

    /**
     * 只做「从提供端经 AIDL 管道完整接收 PCM」这一件事并计时，用于分离
     * 「跨进程传输耗时」和「转码耗时」：转码总耗时 = 传输 + 降混 + 编码 + 写盘。
     *
     * 与真实转码的唯一差别是省掉编码链路；读取节奏不受节流旋钮影响（全速拉）。
     */
    private fun startReceiveOnly() {
        btnConvert.isEnabled = false
        btnSlice.isEnabled = false
        btnOneStep.isEnabled = false
        btnM4aFast.isEnabled = false
        btnRecv.isEnabled = false
        tvResult.text = ""
        tvStatus.text = "状态：仅接收 PCM 测速...\n来源：AIDL($PCM_PROVIDER_PACKAGE)"

        executor.execute {
            val stream = PcmAidlStream(this, PCM_PROVIDER_PACKAGE, PCM_PROVIDER_SERVICE)
            try {
                stream.connectBlocking()
            } catch (e: Exception) {
                Log.e(TAG, "AIDL PCM 源连接失败", e)
                safeUi {
                    toast("AIDL 连接失败：${e.message}")
                    tvStatus.text = "状态：AIDL 连接失败\n${e.message}"
                    enableAllButtons()
                }
                return@execute
            }

            val srcSize = stream.totalSize()
            val startMs = System.currentTimeMillis()
            val buf = ByteArray(256 * 1024)
            var received = 0L
            var lastUiMs = 0L
            var readError: String? = null
            try {
                while (true) {
                    val n = stream.read(buf, 0, buf.size)
                    if (n <= 0) break // EOF
                    received += n
                    // 每 500ms 刷一次进度
                    if (System.currentTimeMillis() - lastUiMs >= 500) {
                        lastUiMs = System.currentTimeMillis()
                        val snap = received
                        safeUi {
                            tvStatus.text = "状态：仅接收 PCM 测速...\n" +
                                    "已接收 ${snap / 1024 / 1024}MB" +
                                    (if (srcSize > 0) " / ${srcSize / 1024 / 1024}MB" else "")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "接收 PCM 中断", e)
                readError = e.message ?: e.javaClass.simpleName
            }
            val elapsedMs = System.currentTimeMillis() - startMs
            stream.close()

            val mb = received / 1024f / 1024f
            val mBs = if (elapsedMs > 0) mb * 1000f / elapsedMs else 0f
            val sizeMatch = srcSize <= 0 || received == srcSize
            Log.i(TAG, "仅接收 PCM 完成: ${received}字节 / ${elapsedMs}ms = ${"%.1f".format(mBs)}MB/s，" +
                    "对账=${if (sizeMatch) "一致" else "不一致(应为$srcSize)"}，错误=$readError")
            safeUi {
                if (readError == null) {
                    tvStatus.text = "状态：接收完成 ✓ 共 ${"%.1f".format(mb)}MB，耗时 ${elapsedMs}ms"
                    tvResult.text = "仅接收 PCM（AIDL 管道，全速）\n" +
                            "接收字节：$received（${"%.1f".format(mb)}MB）\n" +
                            "对账：${if (sizeMatch) "与提供端报告大小一致" else "不一致（应为 $srcSize 字节）"}\n" +
                            "耗时：${elapsedMs}ms\n" +
                            "吞吐：${"%.1f".format(mBs)}MB/s"
                    toast("接收完成，耗时 ${elapsedMs}ms")
                } else {
                    tvStatus.text = "状态：接收中断 - $readError\n已接收 ${"%.1f".format(mb)}MB"
                    toast("接收中断：$readError")
                }
                enableAllButtons()
            }
        }
    }

    private fun enableAllButtons() {
        btnConvert.isEnabled = true
        btnSlice.isEnabled = true
        btnOneStep.isEnabled = true
        btnM4aFast.isEnabled = true
        btnRecv.isEnabled = true
    }

    // ------------------------------------------------------------------
    // 诊断：AAC 编码器实现
    // ------------------------------------------------------------------

    /**
     * 打印本机所有 AAC 编码器（硬件/软件、支持采样率与码率区间）以及平台默认选中的那个。
     *
     * 判读方法：
     * - 默认选中的是「纯软件 = true」→ 编码开销基本无解，降混优化的收益会被淹没，
     *   此时应换编码格式（Opus）或换硬编实例，而不是继续抠降混。
     * - 默认选中的是硬件 → CPU 主要花在降混与 I/O，
     *   转码后在 logcat 里看「耗时分解」即可定位到具体环节。
     */
    private fun runDiagnostics() {
        tvStatus.text = "状态：正在采集编码器信息..."
        executor.execute {
            val info = PcmAacProcessor.dumpAacEncoderInfo()
            safeUi {
                tvResult.text = info
                tvStatus.text = "状态：诊断完成（完整内容见 logcat，TAG=PcmAacProcessor）"
                toast("诊断信息已输出到日志")
            }
        }
    }

    // ------------------------------------------------------------------
    // 清理结果文件
    // ------------------------------------------------------------------

    /**
     * 删除结果文件（演示 deleteResults）：
     * 传空 urls = 删除转码目录与切片目录下的所有结果文件
     */
    private fun clearResults() {
        val cDir = convertDir ?: return
        val sDir = sliceDir ?: return

        executor.execute {
            val result = PcmAacProcessor.deleteResults(arrayListOf(), cDir, sDir)
            safeUi {
                tvStatus.text = "已清理 ${result.deletedCount} 个结果文件"
                tvResult.text = ""
                toast("已清理 ${result.deletedCount} 个文件")
                if (result.failedPaths.isNotEmpty()) {
                    Log.w(TAG, "删除失败: ${result.failedPaths}")
                }
            }
        }
    }

    /**
     * 读取本进程所有线程的累计 CPU 时间（utime + stime，单位约 10ms）。
     *
     * 背景：进程级 CPU% 只能看到总量。节流后出现「不随睡眠值下降的 CPU 地板」时，
     * 只有按线程归因才能确定它在谁身上（我们的转码线程 / C2 编码器线程 / MPEG4Writer...）。
     * 编码器的 native 线程同属本进程，都会出现在 /proc/self/task 下。
     */
    private fun snapshotThreadCpu(): Map<String, Long> {
        val result = HashMap<String, Long>()
        val tasks = File("/proc/self/task").listFiles() ?: return result
        for (t in tasks) {
            try {
                val stat = File(t, "stat").readText()
                // 第 2 字段是可能含空格的线程名（带括号），先按括号截出来
                val name = stat.substring(stat.indexOf('(') + 1, stat.lastIndexOf(')'))
                val fields = stat.substring(stat.lastIndexOf(')') + 2).trim().split(' ')
                // utime = 第 14 字段、stime = 第 15 字段 → 去掉 comm 后的下标 11、12
                result[name] = (fields[11].toLong() + fields[12].toLong()) * 10L
            } catch (_: Exception) {
                // 单个线程读取失败（如线程已退出）不影响整体
            }
        }
        return result
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 安全主线程更新：Activity 已销毁/正在销毁时不执行。
     * 后台任务可能跨 Activity 生命周期返回，避免回调触碰已销毁的 View 或 Context。
     */
    private fun safeUi(block: () -> Unit) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            if (!isFinishing && !isDestroyed) block()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
