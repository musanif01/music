package com.pulsemusic

import android.app.Application
import com.pulsemusic.data.db.AppDatabase
import com.pulsemusic.data.repository.MusicRepository

class PulseMusicApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.install()

        database = AppDatabase.getInstance(this)
        repository = MusicRepository(database)

        initializePython()
    }

    private fun initializePython() {
        try {
            if (!com.chaquo.python.Python.isStarted()) {
                com.chaquo.python.Python.start(com.chaquo.python.android.AndroidPlatform(this))
            }
        } catch (e: Throwable) {
            android.util.Log.w("PulseMusicApp", "Python init failed", e)
        }
    }

    companion object {
        lateinit var instance: PulseMusicApp
            private set
    }
}
