package com.novafiles.feature.files

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.storage.ArchiveEngine
import com.novafiles.core.storage.ArchiveEntryInfo
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveBrowserSheet(
    archiveItem: FileItem,
    onExtractAll: (FileItem) -> Unit,
    onExtractEntry: (FileItem, String) -> Unit,
    onDismiss: () -> Unit
) {
    val entries by produceState<List<ArchiveEntryInfo>>(initialValue = emptyList(), key1 = archiveItem.path) {
        value = withContext(Dispatchers.IO) {
            runCatching { ArchiveEngine().listArchiveEntries(File(archiveItem.path)) }.getOrDefault(emptyList())
        }
    }

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderZip,
                        contentDescription = null,
                        tint = NovaTheme.colors.categoryArchives,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = archiveItem.name,
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entries.size} entries • ${FileFormatters.formatBytes(archiveItem.sizeBytes)}",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }

                NovaButton(
                    text = "Extract All",
                    onClick = {
                        onExtractAll(archiveItem)
                        onDismiss()
                    },
                    variant = NovaButtonVariant.PRIMARY
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (entries.isEmpty()) {
                Text(
                    text = "Reading archive contents or archive is empty...",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NovaTheme.colors.surfaceElevated)
                                .clickable(enabled = !entry.isDirectory) {
                                    onExtractEntry(archiveItem, entry.name)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = if (entry.isDirectory) NovaTheme.colors.categoryFolders else NovaTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = NovaTheme.typography.captionMedium,
                                        color = NovaTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!entry.isDirectory) {
                                        Text(
                                            text = FileFormatters.formatBytes(entry.sizeBytes),
                                            style = NovaTheme.typography.caption,
                                            color = NovaTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                            if (!entry.isDirectory) {
                                IconButton(
                                    onClick = { onExtractEntry(archiveItem, entry.name) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Extract entry",
                                        tint = NovaTheme.colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
