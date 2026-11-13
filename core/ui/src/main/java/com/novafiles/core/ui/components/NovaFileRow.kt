package com.novafiles.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.ui.icons.NovaIcons
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaFileRow(
    item: FileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    formattedSize: String = "",
    formattedDate: String = ""
) {
    val (icon, iconTint, iconBg) = getFileTypeAttributes(item.fileType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.medium)
            .background(if (isSelected) NovaTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(1.dp, NovaTheme.colors.primary.copy(alpha = 0.5f), NovaTheme.shapes.medium)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NovaTheme.colors.primary,
                    uncheckedColor = NovaTheme.colors.cardBorder
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (!item.isDirectory && (item.fileType == FileType.IMAGE || item.fileType == FileType.VIDEO)) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(item.path))
                        .size(160)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                if (item.fileType == FileType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val metaText = when {
                item.isDirectory -> "${item.itemCount ?: 0} items • $formattedDate"
                else -> "$formattedSize • $formattedDate"
            }
            Text(
                text = metaText,
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                maxLines = 1
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = NovaTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaFileGridItem(
    item: FileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    formattedSize: String = "",
    formattedDate: String = ""
) {
    val (icon, iconTint, iconBg) = getFileTypeAttributes(item.fileType)

    Box(
        modifier = modifier
            .clip(NovaTheme.shapes.large)
            .background(if (isSelected) NovaTheme.colors.primary.copy(alpha = 0.15f) else NovaTheme.colors.surfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                shape = NovaTheme.shapes.large
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Preview / Icon Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (!item.isDirectory && (item.fileType == FileType.IMAGE || item.fileType == FileType.VIDEO)) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(item.path))
                            .size(240)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    if (item.fileType == FileType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Three-dot menu button in top right of grid tile
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Checkbox in top left if selection mode
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NovaTheme.colors.primary,
                            uncheckedColor = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            val metaText = when {
                item.isDirectory -> "${item.itemCount ?: 0} items"
                else -> formattedSize
            }
            Text(
                text = metaText,
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun getFileTypeAttributes(fileType: FileType): Triple<ImageVector, Color, Color> {
    val colors = NovaTheme.colors
    return when (fileType) {
        FileType.IMAGE -> Triple(NovaIcons.Photos, colors.categoryPhotos, colors.categoryPhotos.copy(alpha = 0.2f))
        FileType.VIDEO -> Triple(NovaIcons.Videos, colors.categoryVideos, colors.categoryVideos.copy(alpha = 0.2f))
        FileType.AUDIO -> Triple(NovaIcons.AudioWave, colors.categoryAudio, colors.categoryAudio.copy(alpha = 0.2f))
        FileType.DOCUMENT -> Triple(NovaIcons.Documents, colors.categoryDocuments, colors.categoryDocuments.copy(alpha = 0.2f))
        FileType.FOLDER -> Triple(NovaIcons.Folder, colors.categoryFolders, colors.categoryFolders.copy(alpha = 0.2f))
        FileType.APK -> Triple(NovaIcons.ApkCube, colors.categoryApks, colors.categoryApks.copy(alpha = 0.2f))
        FileType.ARCHIVE -> Triple(NovaIcons.ArchiveZip, colors.categoryArchives, colors.categoryArchives.copy(alpha = 0.2f))
        else -> Triple(NovaIcons.Documents, colors.textSecondary, colors.surfaceElevated)
    }
}
