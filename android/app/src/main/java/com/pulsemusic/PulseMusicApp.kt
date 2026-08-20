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

        database = AppDatabase.getInstance(this)
        repository = MusicRepository(database)

        initializePython()
    }

    private fun initializePython() {
        try {
            System.loadLibrary("python")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.w("PulseMusicApp", "Python library not yet loaded (first run)", e)
        }
    }

    companion object {
        lateinit var instance: PulseMusicApp
            private set
    }
}
