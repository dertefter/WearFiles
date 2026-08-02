package com.dertefter.wearable.pdf_viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    val pageCount = renderer.pageCount

    // Cache for page sizes to avoid repeated renderer access during composition
    private val _pageSizes = mutableStateMapOf<Int, IntSize>()
    val pageSizes: Map<Int, IntSize> = _pageSizes

    suspend fun getPageSize(index: Int): IntSize = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cached = _pageSizes[index]
            if (cached != null) return@withLock cached

            if (index !in 0 until pageCount) return@withLock IntSize(0, 0)

            val page = renderer.openPage(index)
            val size = IntSize(page.width, page.height)
            page.close()
            _pageSizes[index] = size
            size
        }
    }

    suspend fun renderPage(index: Int, zoom: Float = 1.2f): ImageBitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (index !in 0 until pageCount) return@withLock null

            try {
                val page = renderer.openPage(index)
                val width = (page.width * zoom).toInt()
                val height = (page.height * zoom).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun close() {
        try {
            renderer.close()
            pfd.close()
        } catch (_: Exception) {}
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
@Composable
fun PdfPage(
    state: WearPdfState,
    index: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(state, index) { mutableStateOf<ImageBitmap?>(null) }
    val pageSize = state.pageSizes[index]

    LaunchedEffect(state, index) {
        if (pageSize == null) {
            state.getPageSize(index)
        }
        bitmap = state.renderPage(index)
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
