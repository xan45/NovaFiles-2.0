@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    images: List<GalleryItem>,
    initialIndex: Int,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit,
    onDeleteImage: (GalleryItem) -> Unit,
    onRenameImage: (GalleryItem, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        onNavigateBack()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.size - 1)
    ) { images.size }

    var areControlsVisible by remember { mutableStateOf(true) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val currentItem = images.getOrNull(pagerState.currentPage) ?: images.first()
    val thumbListState = rememberLazyListState()

    var metadata by remember { mutableStateOf<ImageExifMetadata?>(null) }

    LaunchedEffect(pagerState.currentPage, currentItem.id) {
        rotationDegrees = 0f
        thumbListState.animateScrollToItem(
            (pagerState.currentPage - 2).coerceAtLeast(0)
        )
        metadata = withContext(Dispatchers.IO) {
            extractExifMetadata(currentItem.file)
        }
    }

    BackHandler {
        when {
            showInfoSheet -> showInfoSheet = false
            showDeleteDialog -> showDeleteDialog = false
            showRenameDialog -> showRenameDialog = false
            else -> onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val item = images[page]
            ZoomableImage(
                imageFile = item.file,
                rotationDegrees = if (page == pagerState.currentPage) rotationDegrees else 0f,
                onTap = { areControlsVisible = !areControlsVisible }
            )
        }

        // Overlay Controls Layer
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Top Floating Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    FrostedCircleButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Index indicator: "12 / 44"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Right action: Favorite toggle
                    val favorite = isFavorite(currentItem.id)
                    FrostedCircleButton(onClick = { onToggleFavorite(currentItem.id) }) {
                        Icon(
                            imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (favorite) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Controls: Actions + Thumbnail Scrubber Strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Action Buttons Row (Share, Rotate, Rename, Delete, Info)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share
                        FrostedCircleButton(onClick = { shareSingleImage(context, currentItem.file) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Rotate 90°
                        FrostedCircleButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                            Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        // Rename
                        FrostedCircleButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Delete
                        FrostedCircleButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        }

                        // Info
                        FrostedCircleButton(onClick = { showInfoSheet = true }) {
                            Icon(Icons.Rounded.Info, contentDescription = "Info", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Thumbnail Scrubber Strip
                    LazyRow(
                        state = thumbListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(images) { index, item ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF5B6EF5) else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = item.file,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Info / Details Sheet
        if (showInfoSheet) {
            val meta = metadata
            val dimText = if (meta?.width != null && meta.height != null) {
                "${meta.width} × ${meta.height} px"
            } else if (currentItem.width != null && currentItem.height != null) {
                "${currentItem.width} × ${currentItem.height} px"
            } else {
                "Unknown"
            }

            val formatText = currentItem.file.extension.uppercase().ifEmpty { "Image" }
            val dateTakenFormatted = meta?.dateTaken?.let {
                SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
            } ?: currentItem.dateTaken?.let {
                SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
            } ?: "Not available"

            val dateModFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(currentItem.lastModified))

            val gpsText = if (meta?.latitude != null && meta.longitude != null) {
                String.format(Locale.US, "%.5f, %.5f", meta.latitude, meta.longitude)
            } else {
                "No GPS coordinates"
            }

            NovaBottomSheet(
                onDismissRequest = { showInfoSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Image Details",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = NovaTheme.colors.cardBorder)

                    DetailRow("Filename", currentItem.name)
                    DetailRow("Dimensions", dimText)
                    DetailRow("Format", formatText)
                    DetailRow("Size", "${FileFormatters.formatBytes(currentItem.sizeBytes)} (${currentItem.sizeBytes} bytes)")
                    DetailRow("Date Taken", dateTakenFormatted)
                    DetailRow("Date Modified", dateModFormatted)
                    DetailRow("Location", gpsText)

                    if (meta?.cameraModel != null) {
                        DetailRow("Camera", meta.cameraModel)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Path",
                                color = NovaTheme.colors.textSecondary,
                                style = NovaTheme.typography.caption
                            )
                            Text(
                                text = currentItem.file.absolutePath,
                                color = NovaTheme.colors.textPrimary,
                                style = NovaTheme.typography.body,
                                maxLines = 2
                            )
                        }
                        FrostedCircleButton(onClick = {
                            val clip = ClipData.newPlainText("File Path", currentItem.file.absolutePath)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Path",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            val baseName = currentItem.name.substringBeforeLast(".")
            NovaInputDialog(
                title = "Rename Image",
                initialValue = baseName,
                placeholder = "New name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName ->
                    showRenameDialog = false
                    onRenameImage(currentItem, newName)
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteDialog) {
            NovaConfirmDialog(
                title = "Delete Image?",
                message = "Are you sure you want to permanently delete \"${currentItem.name}\"?",
                confirmText = "Delete",
                cancelText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    showDeleteDialog = false
                    onDeleteImage(currentItem)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    imageFile: File,
    rotationDegrees: Float,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    this.rotationZ = rotationDegrees
                }
        )
    }
}

@Composable
private fun FrostedCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NovaTheme.colors.textSecondary,
            style = NovaTheme.typography.captionMedium
        )
        Text(
            text = value,
            color = NovaTheme.colors.textPrimary,
            style = NovaTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun extractExifMetadata(file: File): ImageExifMetadata {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, options)
    val width = options.outWidth
    val height = options.outHeight

    var lat: Double? = null
    var lon: Double? = null
    var dateTakenMs: Long? = null
    var model: String? = null
    var iso: String? = null
    var fNumber: String? = null
    var exposure: String? = null
    var orientation = 0

    try {
        val exif = ExifInterface(file.absolutePath)
        val latLong = FloatArray(2)
        if (exif.getLatLong(latLong)) {
            lat = latLong[0].toDouble()
            lon = latLong[1].toDouble()
        }
        val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        if (dateStr != null) {
            try {
                dateTakenMs = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateStr)?.time
            } catch (_: Exception) {}
        }
        model = exif.getAttribute(ExifInterface.TAG_MODEL)
        iso = exif.getAttribute("PhotographicSensitivity") ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: Exception) {}

    return ImageExifMetadata(
        width = if (width > 0) width else null,
        height = if (height > 0) height else null,
        dateTaken = dateTakenMs,
        latitude = lat,
        longitude = lon,
        cameraModel = model,
        iso = iso,
        fNumber = fNumber,
        exposureTime = exposure,
        orientation = orientation
    )
}

private fun shareSingleImage(context: Context, file: File) {
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        Uri.fromFile(file)
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Share Image").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
