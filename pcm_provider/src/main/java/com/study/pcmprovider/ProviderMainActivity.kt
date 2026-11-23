package com.study.pcmprovider

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import java.io.File

/**
 * 提供端演示主页：显示 PCM 源文件状态，方便确认「提供端有货可发」。
 * 真正的传输由 [PcmProviderService] 在独立进程完成，本页不做任何传输逻辑。
 */
class ProviderMainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_main)

        val f: File = if (PcmProviderService.PCM_SOURCE_PATH.startsWith("/")) {
            File(PcmProviderService.PCM_SOURCE_PATH)
        } else {
            File(getExternalFilesDir(null), PcmProviderService.PCM_SOURCE_PATH)
        }

        val info = buildString {
            appendLine("PCM 提供端（进程 :pcm_provider）")
            appendLine()
            appendLine("源文件：${f.absolutePath}")
            appendLine("存在：${f.exists() && f.length() > 0}")
            if (f.exists()) {
                appendLine("大小：${"%.1f".format(f.length() / 1024f / 1024f)}MB")
            }
            appendLine()
            appendLine("如需放置演示文件：")
            appendLine("adb push xxx.pcm /sdcard/Android/data/${packageName}/files/")
            appendLine()
            appendLine("本页仅做状态展示；转码端点击「高性能转 M4A」时，")
            appendLine("会 bindService 到 PcmProviderService 并按节奏拉取 PCM 数据。")
        }
        findViewById<TextView>(R.id.tvInfo).text = info
    }
}
