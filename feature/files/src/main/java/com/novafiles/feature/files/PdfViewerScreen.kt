package com.novafiles.feature.files

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = remember(filePath) { File(filePath) }
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState()
    val renderMutex = remember { Mutex() }

    val rendererHolder = remember(filePath) {
        try {
            if (!file.exists() || !file.canRead()) {
                errorMessage = "PDF file cannot be read."
                null
            } else {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                pfd to renderer
            }
        } catch (e: Exception) {
            errorMessage = "Failed to open PDF: ${e.message}"
            null
        }
    }

    DisposableEffect(rendererHolder) {
        onDispose {
            try {
                rendererHolder?.second?.close()
                rendererHolder?.first?.close()
            } catch (_: Exception) {}
        }
    }

    val currentPage by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount.coerceAtLeast(1))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pageCount > 0) {
                        Text(
                            text = "Page $currentPage of $pageCount • ${String.format("%.0f%%", zoomScale * 100)}",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f) }) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
                IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3f) }) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }
        }

        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unable to render PDF",
                        style = NovaTheme.typography.body,
                        color = NovaTheme.colors.error
                    )
                }
            }
            rendererHolder == null || pageCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NovaTheme.colors.primary)
                }
            }
            else -> {
                val renderer = rendererHolder.second
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1f, 3f)
                            }
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale
                            ),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pageCount) { index ->
                            PdfPageItem(
                                renderer = renderer,
                                pageIndex = index,
                                renderMutex = renderMutex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    renderMutex: Mutex
) {
    val bitmapState by produceState<Bitmap?>(initialValue = null, key1 = pageIndex) {
        value = withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    renderer.openPage(pageIndex).use { page ->
                        val targetWidth = 1080
                        val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                        val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(AndroidColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    val bmp = bitmapState
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "PDF Page ${pageIndex + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(NovaTheme.colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NovaTheme.colors.primary)
        }
    }
}
