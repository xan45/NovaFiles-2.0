package com.novafiles.feature.dualpane

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaLinearProgress
import com.novafiles.core.ui.components.NovaLoadingState
import com.novafiles.feature.files.FilesUiState

@Composable
fun DualPaneScreen(
    uiState: DualPaneUiState,
    onSetActivePane: (ActivePane) -> Unit,
    onFolderClickA: (String) -> Unit,
    onFolderClickB: (String) -> Unit,
    onToggleSelectA: (String) -> Unit,
    onToggleSelectB: (String) -> Unit,
    onCopyAcross: () -> Unit,
    onMoveAcross: () -> Unit,
    onSyncPanes: () -> Unit,
    onSwapPanes: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Screen Header & Bridge Controls
            DualPaneHeader(
                activePane = uiState.activePane,
                onCopyAcross = onCopyAcross,
                onMoveAcross = onMoveAcross,
                onSyncPanes = onSyncPanes,
                onSwapPanes = onSwapPanes
            )

            // Progress banner if operation is running
            AnimatedVisibility(visible = uiState.isOperationInProgress) {
                val progress = uiState.operationProgress?.bytesPercentage ?: 0f
                val fileName = uiState.operationProgress?.currentFileName ?: "Processing..."
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NovaLinearProgress(progress = progress, title = fileName)
                }
            }

            // Adaptive Panes: Horizontal split if wide screen (landscape/tablet), vertical stack otherwise
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val isWide = maxWidth > 600.dp

                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        PaneView(
                            title = "Pane A",
                            uiState = uiState.paneA,
                            isActive = uiState.activePane == ActivePane.PANE_A,
                            onPaneClick = { onSetActivePane(ActivePane.PANE_A) },
                            onFolderClick = onFolderClickA,
                            onToggleSelect = onToggleSelectA,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PaneView(
                            title = "Pane B",
                            uiState = uiState.paneB,
                            isActive = uiState.activePane == ActivePane.PANE_B,
                            onPaneClick = { onSetActivePane(ActivePane.PANE_B) },
                            onFolderClick = onFolderClickB,
                            onToggleSelect = onToggleSelectB,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PaneView(
                            title = "Pane A",
                            uiState = uiState.paneA,
                            isActive = uiState.activePane == ActivePane.PANE_A,
                            onPaneClick = { onSetActivePane(ActivePane.PANE_A) },
                            onFolderClick = onFolderClickA,
                            onToggleSelect = onToggleSelectA,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PaneView(
                            title = "Pane B",
                            uiState = uiState.paneB,
                            isActive = uiState.activePane == ActivePane.PANE_B,
                            onPaneClick = { onSetActivePane(ActivePane.PANE_B) },
                            onFolderClick = onFolderClickB,
                            onToggleSelect = onToggleSelectB,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualPaneHeader(
    activePane: ActivePane,
    onCopyAcross: () -> Unit,
    onMoveAcross: () -> Unit,
    onSyncPanes: () -> Unit,
    onSwapPanes: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dual-Pane Manager",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Text(
                text = "Active: ${if (activePane == ActivePane.PANE_A) "Pane A" else "Pane B"}",
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.primary
            )
        }

        // Bridge Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onCopyAcross,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy to Other Pane",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onMoveAcross,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = "Move to Other Pane",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSyncPanes,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Synchronize Directory",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSwapPanes,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Panes",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PaneView(
    title: String,
    uiState: FilesUiState,
    isActive: Boolean,
    onPaneClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.cardBorder

    Box(
        modifier = modifier
            .clip(NovaTheme.shapes.large)
            .background(NovaTheme.colors.surface)
            .border(BorderStroke(if (isActive) 2.dp else 1.dp, borderColor), NovaTheme.shapes.large)
            .clickable(onClick = onPaneClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pane Sub-header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = NovaTheme.typography.subtitle,
                    color = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.textPrimary
                )

                Text(
                    text = "${uiState.folders.size + uiState.files.size} items",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }

            // Path Breadcrumbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.breadcrumbs.forEachIndexed { index, crumb ->
                    val isLast = index == uiState.breadcrumbs.size - 1
                    Text(
                        text = crumb.name,
                        style = NovaTheme.typography.caption,
                        color = if (isLast) NovaTheme.colors.textPrimary else NovaTheme.colors.textSecondary,
                        modifier = Modifier.clickable { onFolderClick(crumb.path) }
                    )
                    if (!isLast) {
                        Text(
                            text = " > ",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.isLoading) {
                NovaLoadingState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val combinedList = uiState.folders + uiState.files
                    items(combinedList, key = { it.id }) { item ->
                        val isSelected = uiState.selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NovaTheme.shapes.small)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                                    } else Modifier
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                NovaFileRow(
                                    item = item,
                                    formattedSize = FileFormatters.formatBytes(item.sizeBytes),
                                    formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                                    onClick = {
                                        if (item.isDirectory) {
                                            onFolderClick(item.path)
                                        } else {
                                            onToggleSelect(item.id)
                                        }
                                    },
                                    onMenuClick = { onToggleSelect(item.id) }
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelect(item.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NovaTheme.colors.primary,
                                    uncheckedColor = NovaTheme.colors.cardBorder
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
