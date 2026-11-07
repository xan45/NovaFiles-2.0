package com.novafiles.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    cancelText: String = "Cancel",
    isDestructive: Boolean = true,
    icon: ImageVector = Icons.Default.Delete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NovaTheme.shapes.extraLarge)
                .background(NovaTheme.colors.surfaceElevated)
                .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.extraLarge)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) NovaTheme.colors.error else NovaTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                NovaButton(
                    text = cancelText,
                    onClick = onDismiss,
                    variant = NovaButtonVariant.SECONDARY
                )
                Spacer(modifier = Modifier.width(12.dp))
                NovaButton(
                    text = confirmText,
                    onClick = onConfirm,
                    variant = NovaButtonVariant.PRIMARY,
                    modifier = if (isDestructive) {
                        Modifier.background(NovaTheme.colors.error, NovaTheme.shapes.pill)
                    } else Modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = NovaTheme.colors.surface,
        shape = NovaTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            content()
        }
    }
}
