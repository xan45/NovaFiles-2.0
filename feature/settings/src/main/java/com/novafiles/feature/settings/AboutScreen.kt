@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.novafiles.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.branding.AboutHeroBanner
import com.novafiles.core.ui.branding.ContactSupportCard
import com.novafiles.core.ui.branding.CreditsCard
import com.novafiles.core.ui.branding.DeveloperInformationCard
import com.novafiles.core.ui.branding.OpenSourceLibrary
import com.novafiles.core.ui.branding.ThankYouBanner
import com.novafiles.core.ui.components.NovaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Dynamically retrieve application and package metadata
    val packageInfo = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 9L
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong() ?: 9L
    }
    val minSdk = context.applicationInfo.minSdkVersion
    val minAndroidName = when (minSdk) {
        26 -> "Android 8.0 (API 26)"
        24 -> "Android 7.0 (API 24)"
        21 -> "Android 5.0 (API 21)"
        else -> "Android API $minSdk"
    }
    val lastUpdateMillis = packageInfo?.lastUpdateTime ?: System.currentTimeMillis()
    val lastUpdatedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(lastUpdateMillis))
    val packageName = context.packageName

    val supportEmail = "xandronhq@gmail.com"

    val handleEmailClick = {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$supportEmail")
                putExtra(Intent.EXTRA_SUBJECT, "NovaFiles Inquiries & Feedback")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("NovaFiles Support Email", supportEmail))
            Toast.makeText(context, "Email copied: $supportEmail", Toast.LENGTH_SHORT).show()
        }
    }

    val handleCopyEmail = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaFiles Support Email", supportEmail))
        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Verified dependencies from libs.versions.toml
    val verifiedLibraries = remember {
        listOf(
            OpenSourceLibrary(
                name = "AndroidX",
                role = "Jetpack Libraries",
                icon = Icons.Rounded.Android,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Media3",
                role = "Media Playback",
                icon = Icons.Rounded.PlayCircle,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Compose",
                role = "Declarative UI",
                icon = Icons.Rounded.Layers,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Coil",
                role = "Image Loading",
                icon = Icons.Rounded.Image,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Kotlin",
                role = "Programming Language",
                icon = Icons.Rounded.Code,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Coroutines",
                role = "Concurrency Engine",
                icon = Icons.Rounded.Storage,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Commons Compress",
                role = "Archive Extraction",
                icon = Icons.Rounded.FolderZip,
                license = "Apache 2.0"
            ),
            OpenSourceLibrary(
                name = "Biometric",
                role = "Hardware Security",
                icon = Icons.Rounded.Fingerprint,
                license = "Apache 2.0"
            )
        )
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "About NovaFiles",
                        style = NovaTheme.typography.title.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = NovaTheme.colors.textPrimary
                    )
                }

                Box {
                    IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy System Info") },
                            onClick = {
                                showOverflowMenu = false
                                val info = "NovaFiles v$versionName (Build $versionCode)\n$packageName\n$minAndroidName"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("NovaFiles Info", info))
                                Toast.makeText(context, "System info copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Visit GitHub Repository") },
                            onClick = {
                                showOverflowMenu = false
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xan45/NovaFiles"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // Main Content Scroll
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Banner
                item {
                    AboutHeroBanner()
                }

                // 2. Application Information Card (Split 2-column layout with 3D illustration)
                item {
                    ApplicationInfoCard(
                        version = "$versionName (Build $versionCode)",
                        minAndroid = minAndroidName,
                        lastUpdated = lastUpdatedDate,
                        packageName = packageName
                    )
                }

                // 3. Developer & Contact Card
                item {
                    DeveloperInformationCard(
                        developerName = "Abhinav Kishore (Xan45)",
                        role = "Android Developer & App Creator",
                        tool = "Made using Google Antigravity"
                    )
                }

                item {
                    ContactSupportCard(
                        email = supportEmail,
                        onEmailClick = handleEmailClick,
                        onCopyClick = handleCopyEmail
                    )
                }

                // 4. Credits & Acknowledgements
                item {
                    CreditsCard(libraries = verifiedLibraries)
                }

                // 5. Thank You Banner
                item {
                    ThankYouBanner()
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Split 2-column Application Information Card matching Image 2 reference.
 */
@Composable
private fun ApplicationInfoCard(
    version: String,
    minAndroid: String,
    lastUpdated: String,
    packageName: String,
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Metadata List
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetadataRow(
                    icon = Icons.Rounded.Info,
                    label = "Version",
                    value = version
                )
                MetadataRow(
                    icon = Icons.Rounded.Android,
                    label = "Minimum Android",
                    value = minAndroid
                )
                MetadataRow(
                    icon = Icons.Rounded.CalendarToday,
                    label = "Last Updated",
                    value = lastUpdated
                )
                MetadataRow(
                    icon = Icons.Rounded.Security,
                    label = "Package Name",
                    value = packageName
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vertical Divider
            Box(
                modifier = Modifier
                    .height(130.dp)
                    .width(1.dp)
                    .background(NovaTheme.colors.cardBorder.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: 3D Folder & Media Illustration
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_about_folder_illustration),
                    contentDescription = "NovaFiles Media & Security Suite",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaTheme.colors.primary,
            modifier = Modifier.size(17.dp)
        )

        Column {
            Text(
                text = label,
                style = NovaTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = NovaTheme.colors.textSecondary
            )
            Text(
                text = value,
                style = NovaTheme.typography.body.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = NovaTheme.colors.textPrimary
            )
        }
    }
}
