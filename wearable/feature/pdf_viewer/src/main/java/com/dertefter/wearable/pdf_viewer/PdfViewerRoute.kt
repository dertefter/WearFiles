package com.dertefter.wearable.pdf_viewer

import android.graphics.pdf.PdfRenderer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import dev.zt64.compose.pdf.component.PdfPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun PdfViewerRoute(
    uriString: String
) {
    val context = LocalContext.current
    var isPdfValid by remember(uriString) { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember(uriString) { mutableStateOf<String?>(null) }

    val couldNotOpenFdError = stringResource(R.string.pdf_viewer_could_not_open_file_descriptor)
    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use {
                        isPdfValid = it.pageCount > 0
                    }
                } ?: run {
                    isPdfValid = false
                    errorMessage = couldNotOpenFdError
                }
            } catch (e: Exception) {
                isPdfValid = false
                errorMessage = e.message
            }
        }
    }

    if (isPdfValid == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pdf_viewer_loading))
        }
        return
    }

    if (isPdfValid == false) {
        val errorString = stringResource(R.string.pdf_viewer_invalid_pdf)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.pdf_viewer_error_with_message, errorMessage ?: errorString))
        }
        return
    }


    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.BodyText,
        last = ColumnItemType.Button,
    )

    val pdfState = rememberWearPdfState(uri = uriString.toUri())

    var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }

    ScreenScaffold(
        scrollIndicator = {
            if (zoomedPageIndex == null) {
                ScrollIndicator(state = columnState)
            }
        },
        contentPadding = contentPadding,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.padding(12.dp),
            userScrollEnabled = zoomedPageIndex == null,
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        ) {

            items(pdfState.pageCount, key = { it }) { i ->
                val zoomState = rememberZoomState()
                LaunchedEffect(zoomState.scale) {
                    if (zoomState.scale > 1f) {
                        zoomedPageIndex = i
                    } else if (zoomedPageIndex == i) {
                        zoomedPageIndex = null
                    }
                }

                AnimatedVisibility(
                    visible = zoomedPageIndex == null || zoomedPageIndex == i,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PdfPage(
                        pdfState,
                        index = i,
                        modifier = Modifier
                            .zoomable(zoomState)
                    )
                }
            }
        }
    }

}