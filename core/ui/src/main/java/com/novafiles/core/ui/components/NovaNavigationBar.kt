package com.novafiles.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

enum class NovaNavTab(val icon: ImageVector, val label: String) {
    HOME(Icons.Default.Home, "Home"),
    FILES(Icons.Default.Folder, "Files"),
    DUAL(Icons.Default.SwapHoriz, "Dual"),
    VAULT(Icons.Default.Cloud, "Vault"),
    SETTINGS(Icons.Default.Person, "Settings")
}

@Composable
fun NovaNavigationBar(
    currentTab: NovaNavTab,
    onTabSelected: (NovaNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(NovaTheme.elevation.level3, NovaTheme.shapes.pill)
                .clip(NovaTheme.shapes.pill)
                .background(NovaTheme.colors.surface)
                .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovaNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val tint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else NovaTheme.colors.textSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "tabTint"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(NovaTheme.colors.primary)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
