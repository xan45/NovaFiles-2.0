package com.novafiles.feature.files

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import java.io.File
import java.security.MessageDigest

private data class ApkDetails(
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val signingSha256: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInspectorSheet(
    apkItem: FileItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val details = remember(apkItem.path) {
        runCatching {
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES
                }
            val pkgInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkItem.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkItem.path, flags)
            }
            pkgInfo?.let { info ->
                info.applicationInfo?.sourceDir = apkItem.path
                info.applicationInfo?.publicSourceDir = apkItem.path
                val label = info.applicationInfo?.loadLabel(pm)?.toString() ?: apkItem.name
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
                val sigBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
                } else {
                    @Suppress("DEPRECATION")
                    info.signatures?.firstOrNull()?.toByteArray()
                }
                val sha256 = sigBytes?.let { bytes ->
                    MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString(":") { "%02X".format(it) }
                } ?: "Unavailable"

                ApkDetails(
                    appLabel = label,
                    packageName = info.packageName ?: "Unknown",
                    versionName = info.versionName ?: "1.0",
                    versionCode = vCode,
                    minSdk = info.applicationInfo?.minSdkVersion ?: 0,
                    targetSdk = info.applicationInfo?.targetSdkVersion ?: 0,
                    permissions = info.requestedPermissions?.toList() ?: emptyList(),
                    signingSha256 = sha256
                )
            }
        }.getOrNull()
    }

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    tint = NovaTheme.colors.categoryApks,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details?.appLabel ?: apkItem.name,
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${details?.packageName ?: "Invalid APK"} • ${FileFormatters.formatBytes(apkItem.sizeBytes)}",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (details == null) {
                Text(
                    text = "Could not parse APK manifest metadata.",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.error
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NovaTheme.colors.surfaceElevated)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Version: ${details.versionName} (${details.versionCode})", style = NovaTheme.typography.captionMedium, color = NovaTheme.colors.textPrimary)
                    Text("SDK: Min ${details.minSdk} • Target ${details.targetSdk}", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    Text("Permissions: ${details.permissions.size} requested", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    Text(
                        text = "Signer SHA-256: ${details.signingSha256}",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (details.permissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(details.permissions) { perm ->
                            Text(
                                text = "• ${perm.substringAfterLast('.')}",
                                style = NovaTheme.typography.caption,
                                color = NovaTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NovaButton(
                    text = "Close",
                    onClick = onDismiss,
                    variant = NovaButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
                NovaButton(
                    text = "Install APK",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.fromFile(File(apkItem.path)), "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        onDismiss()
                    },
                    variant = NovaButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
