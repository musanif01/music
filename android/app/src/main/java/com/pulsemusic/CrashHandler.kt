package com.pulsemusic

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {

    private const val TAG = "PulseCrash"
    private val logFile = File(
        PulseMusicApp.instance.filesDir,
        "crash.log"
    )

    private var lastHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        if (lastHandler != null) return
        lastHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val text = buildString {
                    appendLine("=== Crash @ " +
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    appendLine("Thread: ${thread.name}")
                    appendLine(sw.toString())
                }
                Log.e(TAG, text)
                try {
                    logFile.parentFile?.mkdirs()
                    logFile.appendText(text)
                } catch (_: Throwable) {
                }
            }
            lastHandler?.uncaughtException(thread, throwable)
        }
    }

    fun readCrashLog(): String {
        return runCatching {
            if (logFile.exists()) logFile.readText() else ""
        }.getOrDefault("")
    }

    fun clearCrashLog() {
        runCatching { if (logFile.exists()) logFile.delete() }
    }
}
