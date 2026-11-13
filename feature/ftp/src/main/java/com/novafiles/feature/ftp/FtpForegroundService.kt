package com.novafiles.feature.ftp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class FtpForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "novafiles_ftp_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.novafiles.action.START_FTP"
        const val ACTION_STOP = "com.novafiles.action.STOP_FTP"

        const val EXTRA_PORT = "extra_port"
        const val EXTRA_USER = "extra_user"
        const val EXTRA_PASS = "extra_pass"
        const val EXTRA_ANON = "extra_anon"
        const val EXTRA_READ_ONLY = "extra_read_only"
        const val EXTRA_ROOT = "extra_root"

        fun start(context: Context, config: FtpServerConfig) {
            val intent = Intent(context, FtpForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, config.port)
                putExtra(EXTRA_USER, config.username)
                putExtra(EXTRA_PASS, config.password)
                putExtra(EXTRA_ANON, config.isAnonymousAllowed)
                putExtra(EXTRA_READ_ONLY, config.isReadOnly)
                putExtra(EXTRA_ROOT, config.rootPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FtpForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                FtpServerEngine.stop()
                HttpFileShareServer.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 2121)
                val user = intent.getStringExtra(EXTRA_USER) ?: "novafiles"
                val pass = intent.getStringExtra(EXTRA_PASS) ?: FtpServerConfig.generateSecurePassword()
                val anon = intent.getBooleanExtra(EXTRA_ANON, false)
                val readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, true)
                val root = intent.getStringExtra(EXTRA_ROOT) ?: "/storage/emulated/0"

                val config = FtpServerConfig(
                    port = port,
                    username = user,
                    password = pass,
                    isAnonymousAllowed = anon,
                    rootPath = root,
                    isReadOnly = readOnly
                )

                FtpServerEngine.start(config, serviceScope)
                HttpFileShareServer.start(root, port + 1, serviceScope)

                val ip = FtpServerEngine.getLocalIpAddress()
                val notification = buildNotification(ip, port)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(ip: String, port: Int): Notification {
        val stopIntent = Intent(this, FtpForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("NovaFiles FTP Server Running")
            .setContentText("ftp://$ip:$port (Active)")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FTP File Transfer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status of NovaFiles Wi-Fi FTP file sharing server"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        FtpServerEngine.stop()
        HttpFileShareServer.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
