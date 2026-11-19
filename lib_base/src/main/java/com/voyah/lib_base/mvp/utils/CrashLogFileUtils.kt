package com.voyah.lib_base.mvp.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.voyah.lib_base.mvp.base.BaseApplication
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat


object CrashLogFileUtils {
    @JvmStatic
    private val TAG: String = "岚图汽车"

    @JvmStatic
    private var PATH: String? = null

    @JvmStatic
    private val FILE_NAME = "Crash"

    @JvmStatic
    private val FILE_NAME_SUFFIX = ".txt"

    @JvmStatic
    private val VOYAH_LOG = "voyah_log"

    fun writerNetworkCrashInfo(content: String?) {
        try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val documentFileDir: File? =
                    BaseApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                if (documentFileDir != null) {
                    PATH = documentFileDir.absolutePath + File.separator + VOYAH_LOG
                } else {
                }
            } else {
                PATH =
                    BaseApplication.getAppContext().filesDir.absolutePath + File.separator + VOYAH_LOG
            }

        }catch (e: Exception){
            e.printStackTrace()
        }
        GlobalScope.launch {
            try {
                val dir = File(PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val currentTimeMillis = System.currentTimeMillis()
                val time: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentTimeMillis)
                val file = File(PATH + File.separator +FILE_NAME  + FILE_NAME_SUFFIX)
                val printWriter = PrintWriter(BufferedWriter(FileWriter(file,true)))
                printWriter.println(time)
                printWriter.println(content)
                printWriter.close()
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}