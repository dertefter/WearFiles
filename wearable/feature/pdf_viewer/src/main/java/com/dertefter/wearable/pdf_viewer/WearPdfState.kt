package com.dertefter.wearable.pdf_viewer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thread-safe PDF state management for Wear OS.
 * Uses a Mutex to ensure only one page is opened in the PdfRenderer at a time.
 */
@Stable
class WearPdfState(
    private val pfd: ParcelFileDescriptor
) {
    private val renderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    private var isClosed = false
    val pageCount = renderer.pageCount

    // Cache for page sizes to avoid repeated renderer access during composition
    private val _pageSizes = mutableStateMapOf<Int, IntSize>()
    val pageSizes: Map<Int, IntSize> = _pageSizes

    suspend fun getPageSize(index: Int): IntSize = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isClosed) return@withLock IntSize(0, 0)
            val cached = _pageSizes[index]
            if (cached != null) return@withLock cached

            if (index !in 0 until pageCount) return@withLock IntSize(0, 0)

            try {
                val page = renderer.openPage(index)
                val size = IntSize(page.width, page.height)
                page.close()
                _pageSizes[index] = size
                size
            } catch (_: Exception) {
                IntSize(0, 0)
            }
        }
    }

    suspend fun renderPage(index: Int, zoom: Float = 1.2f): ImageBitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isClosed || index !in 0 until pageCount) return@withLock null

            try {
                val page = renderer.openPage(index)
                val width = (page.width * zoom).toInt()
                val height = (page.height * zoom).toInt()
                
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun close() {
        runBlocking {
            mutex.withLock {
                if (isClosed) return@withLock
                isClosed = true
                try {
                    renderer.close()
                    pfd.close()
                } catch (_: Exception) {}
            }
        }
    }
}

@Composable
fun rememberWearPdfState(uri: Uri): WearPdfState? {
    val context = LocalContext.current
    val state = remember(uri) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.let {
                WearPdfState(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(state) {
        onDispose {
            state?.close()
        }
    }

    return state
}

/**
 * A custom PDF page component that safely handles rendering using WearPdfState.
 */
@OptIn(FlowPreview::class)
@Composable
fun PdfPage(
    state: WearPdfState,
    index: Int,
    modifier: Modifier = Modifier,
    zoom: Float = 1f
) {
    val currentZoom = rememberUpdatedState(zoom)
    var bitmap by remember(state, index) { mutableStateOf<ImageBitmap?>(null) }
    var lastRenderedZoom by remember { mutableFloatStateOf(0f) }
    val pageSize = state.pageSizes[index]

    LaunchedEffect(state, index) {
        if (pageSize == null) {
            state.getPageSize(index)
        }
        
        snapshotFlow { currentZoom.value }
            .debounce(300.milliseconds)
            .collectLatest { z ->
                val targetZoom = (2.0f * z).coerceIn(2.0f, 4.0f)
                if (abs(targetZoom - lastRenderedZoom) > 0.5f || bitmap == null) {
                    bitmap = state.renderPage(index, targetZoom)
                    lastRenderedZoom = targetZoom
                }
            }
    }

    if (bitmap != null && pageSize != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Page $index",
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(pageSize.width.toFloat() / pageSize.height.toFloat()),
            contentScale = ContentScale.FillWidth
        )
    } else {
        // Placeholder with standard aspect ratio
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        )
    }
}
