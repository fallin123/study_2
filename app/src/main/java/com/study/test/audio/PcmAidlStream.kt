package com.voyah.tvad.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.study.aidl.IPcmStreamProvider
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 把 AIDL 传过来的【管道读端 fd】包装成 [InputStream]，供 [PcmAacProcessor.convertPcmToM4aFast] 直接消费。
 *
 * 架构（AIDL + FileDescriptor + Pipe）：
 * - AIDL 只传一次 fd（open() 的返回值），之后数据全部走内核管道，
 *   没有"每 64KB 一次 binder 事务"的 JNI 往返
 * - 提供端在另一进程"边读文件边写管道"；管道容量 ~64KB，本端读慢时对方 write 自然阻塞
 *   → 背压成立，节流旋钮（throttleMsPerBlock）直接作用于读取节奏，占用率逻辑不变
 * - 处理器的签名只认 InputStream，所以跨进程改造收敛在这一个类里，
 *   处理器内部（降混、编码、节流、埋点）一行都不用动
 *
 * 生命周期（都在转码线程上调用，同步调用没有主线程限制）：
 * 1. [connectBlocking]  绑定提供端服务，open() 拿管道读端，包成 AutoCloseInputStream
 * 2. read(...)          处理器按节奏从管道读
 * 3. [close]            处理器 finally 里会关流 → 读端关闭 → 提供端写端收到 EPIPE 自动收尾
 */
class PcmAidlStream(
    private val context: Context,
    private val providerPkg: String,
    private val providerCls: String
) : InputStream() {

    @Volatile
    private var remote: IPcmStreamProvider? = null
    private var stream: InputStream? = null
    private var totalSizeBytes: Long = 0L
    private var bound = false
    private val connected = CountDownLatch(1)

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote = IPcmStreamProvider.Stub.asInterface(service)
            Log.i(TAG, "PCM 提供端已连接: $name")
            connected.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote = null
            Log.w(TAG, "PCM 提供端连接断开: $name")
        }
    }

    /**
     * 绑定提供端并打开 PCM 管道流。阻塞直到连接成功或超时。
     * @throws IOException 绑定失败 / 超时 / 提供端打开 PCM 失败（含源文件不存在）
     */
    fun connectBlocking(timeoutMs: Long = 8_000L) {
        val intent = Intent().setClassName(providerPkg, providerCls)
        bound = try {
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            throw IOException("无法绑定 PCM 提供端（$providerPkg/$providerCls）: ${e.message}", e)
        }
        if (!bound) throw IOException("无法绑定 PCM 提供端（$providerPkg/$providerCls）")

        if (!connected.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw IOException("连接 PCM 提供端超时(${timeoutMs}ms)，请确认应用 $providerPkg 已安装")
        }

        val pfd: ParcelFileDescriptor = try {
            remote!!.open() // 提供端此刻起后台线程写管道；读端 fd 已跨进程转移到本进程
        } catch (e: Exception) {
            throw IOException("提供端打开 PCM 失败: ${e.message}", e)
        }
        totalSizeBytes = runCatching { remote!!.getSize() }.getOrDefault(0L)
        // AutoCloseInputStream：close 时自动关闭 fd → 对方 write 收到 EPIPE 自动收尾
        stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        Log.i(TAG, "PCM 管道已建立, 总大小=${totalSizeBytes / 1024 / 1024}MB")
    }

    /** 源总字节数（提供端未知时为 0） */
    fun totalSize(): Long = totalSizeBytes

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val s = stream ?: throw IOException("PCM 管道未建立")
        if (len == 0) return 0
        return try {
            // 关键：管道一次 read 最多返回 ~64KB（管道容量），而转码器按 256KB 大块
            // 组织处理与节流。若直接把 64KB 返回给它，节流会变成「每 64KB 睡一次」，
            // 睡眠次数翻 4 倍（485MB 文件多出 ~13 分钟），queueInput 调用也翻 4 倍。
            // 所以这里循环累积到满足 len（或 EOF）再返回，等价于文件流的大块读语义。
            var total = 0
            while (total < len) {
                val n = s.read(b, off + total, len - total)
                if (n <= 0) break // EOF（对方关写端）
                total += n
            }
            if (total == 0) -1 else total
        } catch (e: Exception) {
            throw IOException("读取 PCM 失败: ${e.message}", e)
        }
    }

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) <= 0) -1 else one[0].toInt() and 0xFF
    }

    override fun available(): Int = 0

    override fun close() {
        runCatching { stream?.close() } // 关读端 → 提供端写端 EPIPE 收尾
        stream = null
        remote = null
        if (bound) {
            runCatching { context.unbindService(conn) }
            bound = false
        }
        Log.d(TAG, "PCM 管道已关闭并解绑")
    }

    companion object {
        private const val TAG = "PcmAidlStream"
    }
}
