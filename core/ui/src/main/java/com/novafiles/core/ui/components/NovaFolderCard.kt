package com.novafiles.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

@Composable
fun NovaFolderCard(
    name: String,
    itemCount: Int,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = NovaTheme.colors.categoryVideos.copy(alpha = 0.25f),
    folderIconColor: Color = NovaTheme.colors.categoryVideos,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null
) {
    NovaCard(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = NovaTheme.shapes.large,
        backgroundColor = cardColor,
        borderColor = if (isSelected) NovaTheme.colors.primary else cardColor.copy(alpha = 0.6f)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = folderIconColor,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NovaTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$itemCount items • $dateText",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                maxLines = 1
            )
        }
    }
}
