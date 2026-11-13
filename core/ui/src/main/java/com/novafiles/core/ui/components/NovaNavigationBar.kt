package com.novafiles.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.icons.NovaIcons

enum class NovaNavTab(val icon: ImageVector, val label: String) {
    HOME(NovaIcons.Home, "Home"),
    FILES(NovaIcons.Folder, "Files"),
    DUAL(NovaIcons.DualPane, "Dual"),
    VAULT(NovaIcons.VaultShield, "Vault"),
    SETTINGS(NovaIcons.SettingsSliders, "Settings")
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(NovaTheme.elevation.level3, NovaTheme.shapes.pill)
                .clip(NovaTheme.shapes.pill)
                .background(NovaTheme.colors.surface)
                .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovaNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val tint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else NovaTheme.colors.textSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "tabTint"
                )

                Row(
                    modifier = Modifier
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
                        )
                        .padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(visible = isSelected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = NovaTheme.typography.captionMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
