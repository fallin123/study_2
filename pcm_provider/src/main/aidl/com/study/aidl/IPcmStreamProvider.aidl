package com.study.aidl;

import android.os.ParcelFileDescriptor;

// PCM 跨进程提供端接口（pipe 模型：AIDL 只传 fd，数据走内核管道）。
//
// 与逐块 byte[] 拉流相比的优势：
// - open() 之后数据流与 Binder 通道无关：客户端用 AutoCloseInputStream 直读内核 pipe，
//   没有"每 64KB 一次 binder 事务"的 JNI 往返
// - 管道容量有限（~64KB），客户端读慢时写端自然阻塞 → 背压天然成立，
//   转码端的节流旋钮（throttleMsPerBlock）仍然原样生效
// - 关闭语义即 pipe 语义：客户端关掉读端，写端写入时报 EPIPE，提供端线程自动收尾，
//   无需显式 close 会话
interface IPcmStreamProvider {
    /**
     * 打开 PCM 源并建立管道，返回管道【读端】fd（跨进程传给转码端）。
     * 提供端同时在后台线程"边读文件边写管道"。失败抛 RuntimeException。
     *
     * 注意：返回值作为 Binder 事务返回值传输时框架会转移 fd 所有权，
     * 提供端不需要（也不应该）再碰这个 reader。
     */
    ParcelFileDescriptor open();

    /** 源总字节数（未知返回 0），供转码端展示进度用 */
    long getSize();
}
