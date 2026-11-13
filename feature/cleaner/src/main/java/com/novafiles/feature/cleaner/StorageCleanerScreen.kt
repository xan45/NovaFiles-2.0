@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.cleaner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorMintGreen
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.ColorWarmOrange
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaStorageRing

@Composable
fun StorageCleanerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageCleanerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        onNavigateBack()
    }

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
            CleanerTopBar(
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.startScan() }
            )

            when (uiState.state) {
                CleanerState.SCANNING -> {
                    CleanerScanningView(currentPath = uiState.currentScanningPath)
                }
                CleanerState.READY_TO_CLEAN -> {
                    CleanerReadyView(
                        uiState = uiState,
                        onToggleCategory = { viewModel.toggleCategorySelection(it) },
                        onToggleExpanded = { viewModel.toggleCategoryExpanded(it) },
                        onCleanNow = { viewModel.cleanSelected() }
                    )
                }
                CleanerState.CLEANING -> {
                    CleanerInProgressView()
                }
                CleanerState.FINISHED -> {
                    CleanerFinishedView(
                        cleanedBytes = uiState.cleanedBytes,
                        cleanedItemsCount = uiState.cleanedItemsCount,
                        onDone = onNavigateBack
                    )
                }
                CleanerState.IDLE -> {}
            }
        }
    }
}

@Composable
private fun CleanerTopBar(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
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
            text = "Storage Cleaner",
            style = NovaTheme.typography.title,
            color = NovaTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Rescan",
                tint = NovaTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun CleanerScanningView(currentPath: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = NovaTheme.colors.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = "Scanning for Junk Files...",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = currentPath,
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CleanerReadyView(
    uiState: StorageCleanerUiState,
    onToggleCategory: (JunkCategoryType) -> Unit,
    onToggleExpanded: (JunkCategoryType) -> Unit,
    onCleanNow: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp)
        ) {
            // Hero Space Ring Banner
            item {
                CleanerHeroRingCard(cleanableBytes = uiState.selectedCleanableBytes)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Categories Section Title
            item {
                Text(
                    text = "Cleanable Categories",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Category Checklist Items
            items(uiState.categories) { category ->
                JunkCategoryCard(
                    category = category,
                    onToggle = { onToggleCategory(category.type) },
                    onExpand = { onToggleExpanded(category.type) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Clean Now Floating Bottom Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(NovaTheme.colors.background.copy(alpha = 0.95f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val hasSelection = uiState.selectedCleanableBytes > 0L
            val buttonText = if (hasSelection) {
                "Clean Now (${FileFormatters.formatBytes(uiState.selectedCleanableBytes)})"
            } else {
                "Select Items to Clean"
            }

            NovaButton(
                text = buttonText,
                onClick = onCleanNow,
                variant = NovaButtonVariant.GRADIENT,
                enabled = hasSelection,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CleanerHeroRingCard(cleanableBytes: Long) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .border(2.dp, NovaTheme.colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CleaningServices,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = FileFormatters.formatBytes(cleanableBytes),
                style = NovaTheme.typography.display,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Safe to clean without losing personal files",
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun JunkCategoryCard(
    category: JunkCategory,
    onToggle: () -> Unit,
    onExpand: () -> Unit
) {
    val (icon, iconTint) = getCategoryMeta(category.type)

    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = category.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NovaTheme.colors.primary,
                        uncheckedColor = NovaTheme.colors.cardBorder,
                        checkmarkColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = NovaTheme.typography.body,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${category.items.size} items • ${category.description}",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = FileFormatters.formatBytes(category.totalSizeBytes),
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (category.isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Expand",
                        tint = NovaTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Item List
            AnimatedVisibility(
                visible = category.isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                ) {
                    category.items.take(8).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = NovaTheme.typography.caption,
                                color = NovaTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = FileFormatters.formatBytes(item.sizeBytes),
                                style = NovaTheme.typography.caption,
                                color = NovaTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryMeta(type: JunkCategoryType): Pair<ImageVector, Color> {
    return when (type) {
        JunkCategoryType.CACHE -> Icons.Rounded.Cached to ColorVioletSecondary
        JunkCategoryType.TEMP_LOGS -> Icons.Rounded.Description to ColorWarmOrange
        JunkCategoryType.OBSOLETE_APKS -> Icons.Rounded.Android to ColorMintGreen
        JunkCategoryType.EMPTY_FOLDERS -> Icons.Rounded.FolderOpen to ColorNeonCyan
        JunkCategoryType.DUPLICATES -> Icons.Rounded.ContentCopy to ColorCoralRed
    }
}

@Composable
private fun CleanerInProgressView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = NovaTheme.colors.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Cleaning Selected Files...",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Removing residual cache and temporary files safely.",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CleanerFinishedView(
    cleanedBytes: Long,
    cleanedItemsCount: Int,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ColorMintGreen.copy(alpha = 0.15f))
                    .border(2.dp, ColorMintGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = ColorMintGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Cleanup Complete!",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Text(
                text = "Freed ${FileFormatters.formatBytes(cleanedBytes)} across $cleanedItemsCount items.",
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            NovaButton(
                text = "Done",
                onClick = onDone,
                variant = NovaButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}
