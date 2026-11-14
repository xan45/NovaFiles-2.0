package com.xan.novafiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.xan.novafiles.MainActivity
import com.xan.novafiles.R
import com.novafiles.core.media.MusicPlaybackManager
import com.novafiles.core.media.MusicState
import java.io.File
import java.util.Locale

abstract class BaseMusicWidgetProvider(private val isExpanded: Boolean) : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.xan.novafiles.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.xan.novafiles.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.xan.novafiles.widget.ACTION_PREVIOUS"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val state = MusicPlaybackManager.state.value
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, state)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> MusicPlaybackManager.togglePlayPause()
            ACTION_NEXT -> MusicPlaybackManager.skipToNext()
            ACTION_PREVIOUS -> MusicPlaybackManager.skipToPrevious()
            MusicPlaybackManager.ACTION_WIDGET_UPDATE,
            Intent.ACTION_BOOT_COMPLETED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, this.javaClass)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                val state = MusicPlaybackManager.state.value
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id, state)
                }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: MusicState
    ) {
        val layoutId = if (isExpanded) R.layout.widget_music_expanded else R.layout.widget_music_compact
        val views = RemoteViews(context.packageName, layoutId)

        // Launch app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

        // Play/Pause intent
        val playPauseIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

        // Next intent
        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

        // Track info
        val track = state.currentTrack
        if (track != null) {
            views.setTextViewText(R.id.widget_track_title, track.title)
            views.setTextViewText(R.id.widget_artist_name, track.artist)

            // Album art
            val artPath = track.albumArtPath
            if (artPath != null && File(artPath).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(artPath)
                    views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                } catch (_: Exception) {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music_default)
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music_default)
            }
        } else {
            views.setTextViewText(R.id.widget_track_title, "No track playing")
            views.setTextViewText(R.id.widget_artist_name, "NovaFiles")
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music_default)
        }

        // Play/Pause icon
        val playPauseIcon = if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

        // Expanded-specific elements
        if (isExpanded) {
            // Previous intent
            val prevIntent = Intent(context, this.javaClass).apply {
                action = ACTION_PREVIOUS
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                prevIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_previous, prevPendingIntent)

            views.setTextViewText(R.id.widget_album_name, track?.album ?: "Library")

            val currentSec = (state.currentPositionMs / 1000)
            val durSec = (state.durationMs / 1000)
            val timeText = String.format(
                Locale.US,
                "%02d:%02d / %02d:%02d",
                currentSec / 60, currentSec % 60,
                durSec / 60, durSec % 60
            )
            views.setTextViewText(R.id.widget_time_text, timeText)

            val progressPercent = if (state.durationMs > 0) {
                ((state.currentPositionMs.toFloat() / state.durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

class NovaMusicCompactWidgetProvider : BaseMusicWidgetProvider(isExpanded = false)
class NovaMusicExpandedWidgetProvider : BaseMusicWidgetProvider(isExpanded = true)
