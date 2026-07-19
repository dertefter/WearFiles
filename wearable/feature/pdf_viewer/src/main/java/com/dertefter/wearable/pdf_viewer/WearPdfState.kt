package com.dertefter.wearable.pdf_viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import dev.zt64.compose.pdf.PdfState
import androidx.core.graphics.createBitmap

@Stable
class WearPdfState(private val pfd: ParcelFileDescriptor) : PdfState {
    private val renderer = PdfRenderer(pfd)
    override val pageCount: Int = renderer.pageCount

    override fun renderPage(index: Int): Painter {
        if (index !in 0..<pageCount) {
            return BitmapPainter(createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImageBitmap())
        }

        return try {
            renderer.openPage(index).use { page ->
                val scale = 1.2f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                val bmp = createBitmap(width, height)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                BitmapPainter(bmp.asImageBitmap())
            }
        } catch (_: Exception) {
            BitmapPainter(createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImageBitmap())
        }
    }

    override fun close() {
        try {
            renderer.close()
            pfd.close()
        } catch (_: Exception) {}
    }
}

@Composable
fun rememberWearPdfState(uri: Uri): PdfState {
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

    return state ?: EmptyPdfState
}

private object EmptyPdfState : PdfState {
    override val pageCount: Int = 0
    override fun renderPage(index: Int): Painter = 
        BitmapPainter(createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImageBitmap())
    override fun close() {}
}
