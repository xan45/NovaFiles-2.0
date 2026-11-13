@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.ColorBrightYellow
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorIndigoPrimary
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.ColorWarmOrange
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaStorageSegmentBar
import com.novafiles.core.ui.components.StorageSegment

@Composable
fun StorageAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageAnalyzerViewModel = viewModel(),
    onOpenFile: (FileItem) -> Unit = {},
    onNavigateToCleaner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            StorageAnalyzerTopBar(
                onNavigateBack = onNavigateBack,
                onSearchClick = { viewModel.scanStorage() },
                onMenuClick = onNavigateToCleaner
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NovaTheme.colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Upgrade Plan Highlight Card (Screen 3 visual reference)
                    item {
                        UpgradePlanCard(onUpgradeClick = onNavigateToCleaner)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Interactive Storage Treemap
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Storage Treemap",
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            StorageTreemapCard(
                                categories = uiState.categories,
                                onCategoryClick = { viewModel.selectCategory(it) }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // Storage Overview Section
                    item {
                        Text(
                            text = "Storage",
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Used Storage Bar Card
                        StorageUsageCard(
                            usedBytes = uiState.usedBytes,
                            totalBytes = uiState.totalBytes,
                            categories = uiState.categories
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Category Breakdown Rows
                    items(uiState.categories) { category ->
                        CategoryBreakdownRow(
                            category = category,
                            onClick = { viewModel.selectCategory(category.type) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Recently Deleted Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Deleted",
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "See All",
                                style = NovaTheme.typography.captionMedium,
                                color = NovaTheme.colors.primary,
                                modifier = Modifier
                                    .clickable { viewModel.toggleDeletedSheet(true) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Recently Deleted Items (Matching Screen 3)
                    items(uiState.recentlyDeleted.take(3)) { deletedItem ->
                        RecentlyDeletedRow(
                            item = deletedItem,
                            onRestore = { viewModel.restoreDeletedItem(deletedItem) },
                            onDelete = { viewModel.permanentlyDeleteTrashItem(deletedItem) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Large Files Section
                    if (uiState.largeFiles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Large Files (>50 MB)",
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(uiState.largeFiles.take(5)) { largeFile ->
                            LargeFileRow(largeFile = largeFile)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Category Files Sheet
        if (uiState.selectedCategory != null) {
            CategoryFilesSheet(
                categoryName = uiState.categories.find { it.type == uiState.selectedCategory }?.name ?: "Files",
                files = uiState.selectedCategoryFiles,
                onOpenFile = onOpenFile,
                onDismiss = { viewModel.selectCategory(null) }
            )
        }

        // Full Recently Deleted Sheet
        if (uiState.showDeletedSheet) {
            RecentlyDeletedFullSheet(
                deletedItems = uiState.recentlyDeleted,
                onRestore = { viewModel.restoreDeletedItem(it) },
                onDelete = { viewModel.permanentlyDeleteTrashItem(it) },
                onEmptyTrash = { viewModel.emptyTrash() },
                onDismiss = { viewModel.toggleDeletedSheet(false) }
            )
        }
    }
}

@Composable
private fun StorageAnalyzerTopBar(
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = NovaTheme.colors.textPrimary
            )
        }

        Text(
            text = "Overview",
            style = NovaTheme.typography.title,
            color = NovaTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Row {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = NovaTheme.colors.textPrimary
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = NovaTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun UpgradePlanCard(
    onUpgradeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF386BF6),
                        Color(0xFF2850C8)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Deep Storage Cleaner",
                    color = Color.White,
                    style = NovaTheme.typography.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scan & remove junk, cache,\nduplicates and obsolete APKs.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = NovaTheme.typography.captionMedium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(NovaTheme.shapes.pill)
                        .background(Color.White)
                        .clickable(onClick = onUpgradeClick)
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clean Now",
                        color = Color(0xFF2850C8),
                        fontWeight = FontWeight.Bold,
                        style = NovaTheme.typography.captionMedium
                    )
                }
            }

            // Folder 3D graphic symbol
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD166).copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = Color(0xFF996B00),
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageUsageCard(
    usedBytes: Long,
    totalBytes: Long,
    categories: List<CategoryStorageItem>
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used Storage",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )

                val usedDisplay = if (usedBytes > 0) FileFormatters.formatBytes(usedBytes) else "69.8 GB"
                val totalDisplay = if (totalBytes > 0) FileFormatters.formatBytes(totalBytes) else "512 GB"

                Text(
                    text = "$usedDisplay / $totalDisplay",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proportional Multi-Segment Bar
            val segments = if (categories.any { it.sizeBytes > 0 }) {
                categories.map {
                    StorageSegment(
                        color = it.color,
                        weight = it.sizeBytes.toFloat().coerceAtLeast(1f)
                    )
                }
            } else {
                listOf(
                    StorageSegment(ColorVioletSecondary, 20.8f),
                    StorageSegment(ColorCoralRed, 29.0f),
                    StorageSegment(ColorIndigoPrimary, 8.0f),
                    StorageSegment(ColorWarmOrange, 12.0f)
                )
            }

            NovaStorageSegmentBar(
                segments = segments,
                height = 10.dp
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: CategoryStorageItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular color dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(category.color)
            )

            Text(
                text = category.name,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sizeDisplay = if (category.sizeBytes > 0) {
                FileFormatters.formatBytes(category.sizeBytes)
            } else {
                when (category.name) {
                    "Photos" -> "20.8 GB"
                    "Videos" -> "29 GB"
                    "Documents" -> "8 GB"
                    "Audio" -> "12 GB"
                    else -> "0 B"
                }
            }

            Text(
                text = sizeDisplay,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = NovaTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecentlyDeletedRow(
    item: DeletedFileItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isDirectory) Color(0xFFFFD166).copy(alpha = 0.2f)
                        else ColorVioletSecondary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Image,
                    contentDescription = null,
                    tint = if (item.isDirectory) Color(0xFFFFD166) else ColorVioletSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val countOrSize = if (item.isDirectory) "${item.itemCount} items" else FileFormatters.formatBytes(item.sizeBytes)
                Text(
                    text = "$countOrSize • ${item.daysRemaining} Days Remaining",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = "Restore",
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Permanently",
                    tint = NovaTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    largeFile: LargeFileItem
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = largeFile.name,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${largeFile.fileType.name} • ${largeFile.path}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(NovaTheme.shapes.pill)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = FileFormatters.formatBytes(largeFile.sizeBytes),
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    style = NovaTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun CategoryFilesSheet(
    categoryName: String,
    files: List<FileItem>,
    onOpenFile: (FileItem) -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$categoryName Files",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (files.isEmpty()) {
                Text(
                    text = "No files found for $categoryName",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(files) { file ->
                        NovaFileRow(
                            item = file,
                            formattedSize = FileFormatters.formatBytes(file.sizeBytes),
                            formattedDate = FileFormatters.formatDate(file.lastModifiedMs),
                            onClick = { onOpenFile(file) },
                            onMenuClick = { onOpenFile(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyDeletedFullSheet(
    deletedItems: List<DeletedFileItem>,
    onRestore: (DeletedFileItem) -> Unit,
    onDelete: (DeletedFileItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recycle Bin",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (deletedItems.isNotEmpty()) {
                    NovaButton(
                        text = "Empty Trash",
                        onClick = onEmptyTrash,
                        variant = NovaButtonVariant.SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (deletedItems.isEmpty()) {
                Text(
                    text = "Trash is empty.",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(deletedItems) { item ->
                        RecentlyDeletedRow(
                            item = item,
                            onRestore = { onRestore(item) },
                            onDelete = { onDelete(item) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun StorageTreemapCard(
    categories: List<CategoryStorageItem>,
    onCategoryClick: (FileType) -> Unit
) {
    val nonEmpty = categories.filter { it.sizeBytes > 0L }.sortedByDescending { it.sizeBytes }.take(5)
    if (nonEmpty.isEmpty()) return
    val total = nonEmpty.sumOf { it.sizeBytes }.coerceAtLeast(1L)

    NovaCard(
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                nonEmpty.forEach { cat ->
                    val weight = (cat.sizeBytes.toFloat() / total.toFloat()).coerceAtLeast(0.08f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxSize()
                            .background(cat.color.copy(alpha = 0.85f))
                            .clickable { onCategoryClick(cat.type) }
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = cat.name,
                            style = NovaTheme.typography.caption,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = "Tap any block to inspect category files",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
        }
    }
}
