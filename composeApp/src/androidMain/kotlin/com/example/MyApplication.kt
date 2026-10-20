package com.example

import android.app.Application
import com.example.platform.AppContextHolder
import java.io.File
import java.io.PrintWriter

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val file = File(getExternalFilesDir(null), "crash_log.txt")
                val writer = PrintWriter(file)
                exception.printStackTrace(writer)
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
