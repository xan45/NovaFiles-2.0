package com.novafiles.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.media.MusicState
import java.io.File

@Composable
fun NovaMiniMusicPlayer(
    musicState: MusicState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = musicState.currentTrack ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xE6111B3A))
            .border(1.dp, Color(0x335B6EF5), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album artwork or Music icon
                val artPath = track.albumArtPath
                if (artPath != null && File(artPath).exists()) {
                    AsyncImage(
                        model = File(artPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NovaTheme.colors.categoryAudio.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NovaTheme.colors.categoryAudio,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Continuous Progress Bar along bottom edge of mini player
            LinearProgressIndicator(
                progress = { musicState.progressFraction },
                color = NovaTheme.colors.primary,
                trackColor = Color(0x26FFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}
