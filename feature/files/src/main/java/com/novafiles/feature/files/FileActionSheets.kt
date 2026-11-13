package com.novafiles.feature.files

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import kotlinx.coroutines.launch
import java.io.File

/**
 * Clean, context-appropriate file and folder context menu sheet.
 * Fixes BUG-004: Three-dot tap opens this sheet instead of triggering selection mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItemContextMenuSheet(
    item: FileItem,
    clipboard: ClipboardState?,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPasteInto: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onProperties: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onSelect: () -> Unit
) {
    val isArchive = item.fileType == FileType.ARCHIVE ||
            item.name.endsWith(".zip", true) ||
            item.name.endsWith(".tar", true) ||
            item.name.endsWith(".tar.gz", true) ||
            item.name.endsWith(".tgz", true)

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (item.isDirectory) NovaTheme.colors.categoryFolders.copy(alpha = 0.2f) else NovaTheme.colors.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = if (item.isDirectory) NovaTheme.colors.categoryFolders else NovaTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
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
                    val meta = if (item.isDirectory) {
                        "${item.itemCount ?: 0} items • ${FileFormatters.formatDate(item.lastModifiedMs)}"
                    } else {
                        "${FileFormatters.formatBytes(item.sizeBytes)} • ${FileFormatters.formatDate(item.lastModifiedMs)}"
                    }
                    Text(
                        text = meta,
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NovaTheme.colors.cardBorder)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            ContextMenuItem(icon = Icons.Default.Folder, label = "Open", onClick = onOpen)

            if (!item.isDirectory) {
                ContextMenuItem(icon = Icons.AutoMirrored.Filled.OpenInNew, label = "Open with...", onClick = onOpenWith)
            }

            ContextMenuItem(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Cut", onClick = onCut)
            ContextMenuItem(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)

            if (item.isDirectory && clipboard != null && clipboard.files.isNotEmpty()) {
                ContextMenuItem(
                    icon = Icons.Default.ContentPaste,
                    label = "Paste here (${clipboard.files.size} items)",
                    tint = NovaTheme.colors.primary,
                    onClick = onPasteInto
                )
            }

            ContextMenuItem(icon = Icons.Default.Edit, label = "Rename", onClick = onRename)
            ContextMenuItem(icon = Icons.Default.Delete, label = "Delete", tint = NovaTheme.colors.error, onClick = onDelete)

            if (!item.isDirectory) {
                ContextMenuItem(icon = Icons.Default.Share, label = "Share", onClick = onShare)
            }

            ContextMenuItem(icon = Icons.Default.Info, label = "Properties & Checksum", onClick = onProperties)

            ContextMenuItem(
                icon = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (isBookmarked) "Remove Favorite" else "Add to Favorites",
                onClick = onToggleBookmark
            )

            ContextMenuItem(icon = Icons.Default.FolderZip, label = "Compress (ZIP)", onClick = onCompress)

            if (isArchive) {
                ContextMenuItem(icon = Icons.Default.Archive, label = "Extract Here", tint = NovaTheme.colors.primary, onClick = onExtract)
            }

            ContextMenuItem(icon = Icons.Default.Check, label = "Select", onClick = onSelect)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = NovaTheme.colors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = NovaTheme.typography.body,
            color = tint
        )
    }
}

/**
 * In-context browsing options bottom sheet (Sort By, Sort Direction, Layout View, Show Hidden).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingSettingsSheet(
    isGridView: Boolean,
    showHiddenFiles: Boolean,
    sortConfig: FileSortConfig,
    onToggleLayout: () -> Unit,
    onToggleShowHidden: () -> Unit,
    onSelectSortBy: (SortBy) -> Unit,
    onToggleSortDirection: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Browsing Options",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Layout Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaTheme.colors.surfaceElevated)
                    .clickable { onToggleLayout() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Layout View", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                        Text(if (isGridView) "Grid View" else "List View", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                }
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle",
                    tint = NovaTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Show Hidden Files Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaTheme.colors.surfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = NovaTheme.colors.categoryPhotos,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Show Hidden Files", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                        Text("Display dotfiles and .nomedia", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                }
                Switch(
                    checked = showHiddenFiles,
                    onCheckedChange = { onToggleShowHidden() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NovaTheme.colors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Sort By",
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            SortBy.entries.forEach { sortBy ->
                val isSelected = sortConfig.sortBy == sortBy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectSortBy(sortBy) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sortBy.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = NovaTheme.typography.body,
                        color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectSortBy(sortBy) },
                        colors = RadioButtonDefaults.colors(selectedColor = NovaTheme.colors.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Direction Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaTheme.colors.surfaceElevated)
                    .clickable { onToggleSortDirection() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sortConfig.direction == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (sortConfig.direction == SortDirection.ASCENDING) "Order: Ascending (A-Z)" else "Order: Descending (Z-A)",
                        style = NovaTheme.typography.body,
                        color = NovaTheme.colors.textPrimary
                    )
                }
                Text("Toggle", style = NovaTheme.typography.captionMedium, color = NovaTheme.colors.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * File Details & SHA-256 Checksum Verifier Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesSheet(
    item: FileItem,
    onComputeSha256: suspend (String) -> String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()
    var sha256Hash by remember(item.path) { mutableStateOf<String?>(null) }
    var expectedHash by remember(item.path) { mutableStateOf("") }

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Text(
                text = "Path: ${item.path}",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
            Text(
                text = "Size: ${FileFormatters.formatBytes(item.sizeBytes)} (${item.sizeBytes} bytes)",
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.textPrimary
            )
            Text(
                text = "Modified: ${FileFormatters.formatDate(item.lastModifiedMs)}",
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.textSecondary
            )
            if (sha256Hash != null) {
                Text(
                    text = "SHA-256: $sha256Hash",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.primary
                )
                OutlinedTextField(
                    value = expectedHash,
                    onValueChange = { expectedHash = it },
                    placeholder = { Text("Paste expected SHA-256 to verify", color = NovaTheme.colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (expectedHash.isNotBlank()) {
                    val matches = sha256Hash.equals(expectedHash.trim(), ignoreCase = true)
                    Text(
                        text = if (matches) "Checksum Match" else "Checksum Mismatch",
                        style = NovaTheme.typography.captionMedium,
                        color = if (matches) NovaTheme.colors.success else NovaTheme.colors.error
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NovaButton(
                    text = if (sha256Hash == null) "Compute SHA-256" else "Copy Hash",
                    onClick = {
                        if (sha256Hash == null) {
                            uiScope.launch { sha256Hash = onComputeSha256(item.path) }
                        } else {
                            clipboardManager.setText(AnnotatedString(sha256Hash!!))
                            Toast.makeText(context, "SHA-256 copied", Toast.LENGTH_SHORT).show()
                        }
                    },
                    variant = NovaButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
                NovaButton(
                    text = "Close",
                    onClick = onDismiss,
                    variant = NovaButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Top bar header during multi-selection mode.
 */
@Composable
fun MultiSelectionHeader(
    selectedCount: Int,
    isAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Selection",
                    tint = NovaTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$selectedCount Selected",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
        }

        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = Icons.Default.SelectAll,
                contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                tint = if (isAllSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary
            )
        }
    }
}

/**
 * Floating bottom dock for batch actions on selected files.
 */
@Composable
fun MultiSelectionBottomDock(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(NovaTheme.elevation.level4, NovaTheme.shapes.pill)
            .clip(NovaTheme.shapes.pill)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockActionButton(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
        DockActionButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Cut", onClick = onMove)
        DockActionButton(icon = Icons.Default.Delete, label = "Delete", tint = NovaTheme.colors.error, onClick = onDelete)
        DockActionButton(icon = Icons.Default.Share, label = "Share", onClick = onShare)
        DockActionButton(icon = Icons.Default.FolderZip, label = "Zip", onClick = onCompress)
        DockActionButton(icon = Icons.Default.MoreHoriz, label = "More", onClick = onMore)
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = NovaTheme.colors.textPrimary
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = NovaTheme.typography.caption,
            color = tint
        )
    }
}

/**
 * Floating clipboard dock showing pending paste operation.
 */
@Composable
fun SharedClipboardDock(
    clipboard: ClipboardState,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(NovaTheme.elevation.level4, NovaTheme.shapes.pill)
            .clip(NovaTheme.shapes.pill)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.primary.copy(alpha = 0.5f), NovaTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (clipboard.op == ClipboardOp.COPY) Icons.Default.ContentCopy else Icons.AutoMirrored.Filled.DriveFileMove,
            contentDescription = null,
            tint = NovaTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "${clipboard.files.size} items ready to ${if (clipboard.op == ClipboardOp.COPY) "copy" else "move"}",
            style = NovaTheme.typography.captionMedium,
            color = NovaTheme.colors.textPrimary
        )
        NovaButton(
            text = "Paste",
            onClick = onPaste,
            variant = NovaButtonVariant.PRIMARY,
            modifier = Modifier.height(32.dp)
        )
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel clipboard",
                tint = NovaTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
