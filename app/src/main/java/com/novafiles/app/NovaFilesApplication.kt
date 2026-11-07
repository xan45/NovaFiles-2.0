package com.novafiles.app

import android.app.Application
import android.content.Intent
import com.novafiles.core.media.NovaMusicPlaybackService

class NovaFilesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val serviceIntent = Intent(this, NovaMusicPlaybackService::class.java)
            startService(serviceIntent)
        } catch (_: Exception) {}
    }
}
