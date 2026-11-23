package com.study.pcmprovider

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.study.aidl.IPcmStreamProvider
import java.io.File
import java.io.FileInputStream

/**
 * PCM 提供端服务：运行在独立进程 [:pcm_provider]，通过 AIDL + 管道向转码端提供 PCM 数据流。
 *
 * 工作方式（AIDL + FileDescriptor + Pipe）：
 * 1. 转码端 bindService 后调用 [IPcmStreamProvider.open]
 * 2. 本端 createPipe() 拿到一对 fd，把【读端】作为 Binder 返回值跨进程传给转码端
 *    （框架在传输返回值时会转移 fd 所有权并 dup 到对方进程，本端无需也无法再使用）
 * 3. 本端起后台线程「边读 PCM 文件边写管道写端」——pipe 容量有限（~64KB），
 *    客户端读慢时 write() 阻塞，背压天然成立；转码端的节流旋钮照常生效
 * 4. 收尾即 pipe 语义：客户端关读端 → 本端 write() 抛 EPIPE → 线程退出并关写端；
 *    文件读尽 → 关写端 → 客户端读到 EOF。无需显式 close 会话
 *
 * 与旧的「逐 64KB byte[] 拉流」相比：数据不再过 Binder 事务通道，
 * open() 一次往返之后全部走内核管道，Binder 负载接近于零。
 */
class PcmProviderService : Service() {

    /** 最近一次 open 的源大小，供 getSize 返回（同一时刻只支持一个活动流，够用） */
    @Volatile
    private var streamSize: Long = 0L

    /** PCM 源路径：与转码端 PCM_SOURCE_PATH 语义一致（"/" 开头 = 绝对路径，否则相对本应用私有外部存储） */
    private fun resolvePcmFile(): File {
        val p = PCM_SOURCE_PATH
        return if (p.startsWith("/")) File(p) else File(getExternalFilesDir(null), p)
    }

    private val binder = object : IPcmStreamProvider.Stub() {

        override fun open(): ParcelFileDescriptor {
            val f = resolvePcmFile()
            if (!f.exists() || f.length() <= 0) {
                throw RuntimeException("PCM 源不存在: ${f.absolutePath}")
            }
            streamSize = f.length()

            // 创建内核管道：reader 跨进程给转码端，writer 由本线程写入
            val (reader, writer) = ParcelFileDescriptor.createPipe()
            val fis = FileInputStream(f)

            // 边读文件边写管道；读写两侧节奏解耦，背压由管道天然实现
            Thread {
                // 提升本线程优先级：避免与进程其他低优先级线程互相拖累
                // （注意：这只调整线程 nice 值；若整个进程被系统降为后台 cgroup，
                //   还需要看下面的速率日志确认是否被 cgroup 限速）
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT)
                try {
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    var markBytes = 0L
                    var markMs = System.currentTimeMillis()
                    ParcelFileDescriptor.AutoCloseOutputStream(writer).use { pipeOut ->
                        fis.use { input ->
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                // 管道满时阻塞在这里 —— 转码端（节流睡眠）读得慢，本线程就写得慢
                                pipeOut.write(buf, 0, n)
                                total += n
                                // 每 32MB 打一次实际写管道速率：区分「被 cgroup 限速」和「客户端节流」
                                if (total - markBytes >= 32L * 1024 * 1024) {
                                    val now = System.currentTimeMillis()
                                    val rate = (total - markBytes) * 1000f / (now - markMs) / 1024f / 1024f
                                    Log.i(TAG, "管道写入进度: ${total / 1024 / 1024}MB, 速率=${"%.1f".format(rate)}MB/s")
                                    markBytes = total
                                    markMs = now
                                }
                            }
                        }
                    }
                    Log.i(TAG, "PCM 已全部写入管道: $total 字节")
                } catch (e: Exception) {
                    // 正常收尾路径：客户端读完关掉读端（或转码结束/进程退出）→ write 报 EPIPE
                    Log.i(TAG, "管道写入终止（客户端关闭读端或断开）: ${e.message}")
                } finally {
                    try {
                        writer.close() // 正常 EOF / 异常断开都必须关写端，否则客户端永远读不到 EOF
                    } catch (ignored: Exception) {
                    }
                }
            }.apply { name = "pcm-pipe-writer" }.start()

            Log.i(TAG, "管道已建立: ${f.absolutePath} (${f.length() / 1024 / 1024}MB)，" +
                    "调用方=${packageManager.getNameForUid(Binder.getCallingUid()) ?: "unknown"}")
            return reader // Binder 返回值传输时框架转移 fd 所有权，本端此后不得再碰
        }

        override fun getSize(): Long = streamSize
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "转码端已绑定")
        return binder
    }

    companion object {
        private const val TAG = "PcmProvider"

        /**
         * 本端持有的 PCM 源文件：
         * - "/xxx.pcm"      → 绝对路径（如车机录音模块直接落盘的路径）
         * - "xxx.pcm"       → 本应用私有外部存储 Android/data/com.sgm.pcmprovider/files/ 下
         *
         * 演示时可把 PCM 文件推到本应用外部目录：
         * adb push out_vad_asr.pcm /sdcard/Android/data/com.sgm.pcmprovider/files/
         */
        const val PCM_SOURCE_PATH = "out_vad_asr_10-14_16-12-46-308.pcm"
    }
}
