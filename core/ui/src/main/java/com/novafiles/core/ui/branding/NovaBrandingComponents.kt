package com.novafiles.core.ui.branding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novafiles.core.designsystem.R
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaCard

/**
 * 3D NovaFiles Application Icon with optional ambient glow shadow.
 */
@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "icon_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (showGlow) {
                    Modifier.drawBehind {
                        // Luminous ambient violet-cyan halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x665B6EF5),
                                    Color(0x338A6CFF),
                                    Color.Transparent
                                ),
                                center = Offset(size.toPx() / 2f, size.toPx() / 2f),
                                radius = size.toPx() * 0.75f
                            ),
                            radius = size.toPx() * 0.75f
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_novafiles_3d),
            contentDescription = "NovaFiles Logo",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
        )
    }
}

/**
 * Reusable NovaFiles Wordmark with optional "2.0" gradient badge.
 */
@Composable
fun AppWordmark(
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 24.sp,
    showBadge: Boolean = false,
    badgeText: String = "Alpha 1.0"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NovaFiles",
            style = NovaTheme.typography.display.copy(
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            ),
            color = NovaTheme.colors.textPrimary
        )

        if (showBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF5B6EF5),
                                Color(0xFF8A6CFF)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = NovaTheme.typography.caption.copy(
                        fontSize = (titleSize.value * 0.45f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Master AppLogo combining AppIcon, AppWordmark, and optional tagline.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,
    showWordmark: Boolean = true,
    showBadge: Boolean = false,
    showTagline: Boolean = false,
    tagline: String = "Advanced Storage & File Management Suite"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(size = iconSize, showGlow = true)

        if (showWordmark) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                AppWordmark(
                    titleSize = (iconSize.value * 0.52f).sp,
                    showBadge = showBadge
                )
                if (showTagline) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tagline,
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Pill-style category feature chip matching the About & Credits reference mockup.
 */
@Composable
fun AppFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NovaTheme.colors.primary
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NovaTheme.colors.surface.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = NovaTheme.colors.cardBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = NovaTheme.typography.caption.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp
            ),
            color = NovaTheme.colors.textPrimary
        )
    }
}

/**
 * Prominent branded Hero Banner matching the design reference.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutHeroBanner(
    modifier: Modifier = Modifier,
    onChipClick: ((String) -> Unit)? = null
) {
    NovaCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x335B6EF5),
                spotColor = Color(0x4D8A6CFF)
            ),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Fluid gradient wave mesh decoration across top/right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x402DD4BF), // Cyan wave
                                Color(0x305B6EF5), // Indigo
                                Color(0x258A6CFF), // Violet
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.95f, size.height * 0.05f),
                            radius = size.width * 0.85f
                        )
                    )
                }
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(size = 80.dp, showGlow = true)

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        AppWordmark(titleSize = 26.sp, showBadge = true)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Advanced Storage & File Management Suite",
                            style = NovaTheme.typography.caption.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Feature Chips in a row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFeatureChip(
                        icon = Icons.Rounded.Folder,
                        label = "Files",
                        accentColor = Color(0xFF60A5FA),
                        modifier = Modifier.clickable { onChipClick?.invoke("Files") }
                    )
                    AppFeatureChip(
                        icon = Icons.Rounded.PlayCircle,
                        label = "Media",
                        accentColor = ColorVioletSecondary,
                        modifier = Modifier.clickable { onChipClick?.invoke("Media") }
                    )
                    AppFeatureChip(
                        icon = Icons.Rounded.Storage,
                        label = "Storage",
                        accentColor = ColorNeonCyan,
                        modifier = Modifier.clickable { onChipClick?.invoke("Storage") }
                    )
                    AppFeatureChip(
                        icon = Icons.Rounded.Security,
                        label = "Secure",
                        accentColor = Color(0xFF818CF8),
                        modifier = Modifier.clickable { onChipClick?.invoke("Secure") }
                    )
                }
            }
        }
    }
}

/**
 * Developer attribution and tool identification card.
 */
@Composable
fun DeveloperInformationCard(
    developerName: String = "Abhinav Kishore (Xan45)",
    role: String = "Lead Developer & App Creator",
    tool: String = "Made using Google Antigravity",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = 1.dp,
                color = NovaTheme.colors.cardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with gradient border
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(ColorNeonCyan, ColorVioletSecondary)
                    ),
                    shape = CircleShape
                )
                .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = NovaTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Developed by",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                fontSize = 11.5.sp
            )
            Text(
                text = developerName,
                style = NovaTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                color = NovaTheme.colors.textPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool,
                style = NovaTheme.typography.caption.copy(
                    fontWeight = FontWeight.Medium,
                    color = ColorNeonCyan
                ),
                fontSize = 11.5.sp
            )
        }
    }
}

/**
 * Clickable contact and support card with mailto and clipboard copy.
 */
@Composable
fun ContactSupportCard(
    email: String = "xandronhq@gmail.com",
    onEmailClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = 1.dp,
                color = NovaTheme.colors.cardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Contact & Support",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    fontSize = 11.5.sp
                )
                Text(
                    text = email,
                    style = NovaTheme.typography.body.copy(fontWeight = FontWeight.Bold),
                    color = NovaTheme.colors.primary,
                    fontSize = 14.5.sp,
                    modifier = Modifier.clickable(onClick = onEmailClick)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NovaTheme.colors.surface)
                .clickable(onClick = onCopyClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy Email",
                tint = NovaTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class OpenSourceLibrary(
    val name: String,
    val role: String,
    val icon: ImageVector,
    val license: String = "Apache-2.0"
)

/**
 * Grid section acknowledging verified open-source libraries.
 */
@Composable
fun CreditsCard(
    libraries: List<OpenSourceLibrary>,
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Credits & Acknowledgements",
                        style = NovaTheme.typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = NovaTheme.colors.textPrimary
                    )
                    Text(
                        text = "Special thanks to the open-source projects that make NovaFiles possible.",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column Library Tiles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                libraries.chunked(2).forEach { rowLibs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowLibs.forEach { lib ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NovaTheme.colors.surface)
                                    .border(
                                        width = 1.dp,
                                        color = NovaTheme.colors.cardBorder.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NovaTheme.colors.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = lib.icon,
                                            contentDescription = null,
                                            tint = NovaTheme.colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = lib.name,
                                        style = NovaTheme.typography.body.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = NovaTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = lib.role,
                                        style = NovaTheme.typography.caption.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                        if (rowLibs.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thank You Banner concluding the About page.
 */
@Composable
fun ThankYouBanner(
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle purple-magenta bottom-right glow
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x358A6CFF),
                                Color(0x25F472B6),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.95f, size.height * 0.85f),
                            radius = size.width * 0.7f
                        )
                    )
                }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF472B6).copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFF472B6).copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Thank You",
                        style = NovaTheme.typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "To all users for your support, feedback and trust in NovaFiles.",
                        style = NovaTheme.typography.caption.copy(
                            fontSize = 12.sp
                        ),
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
