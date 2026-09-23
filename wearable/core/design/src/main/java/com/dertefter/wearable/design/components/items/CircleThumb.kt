package com.dertefter.wearable.design.components.items

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.theme.WearFilesTheme
import java.io.File

@Composable
fun CircleThumb(
    uri: Uri?,
    modifier: Modifier = Modifier,
    file: File? = null,
    contentDescription: String? = null,
    onClick: () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
    shape: RoundedCornerShape = CircleShape,
    isSelected: Boolean = false
) {
    val context = LocalContext.current

    var previewLoaded by remember { mutableStateOf(false) }

    val animatedSelectedAlpha  by animateFloatAsState(
        if (isSelected) 1f else 0f
    )

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {

        val resolvedIcon = icon ?: file?.resolveIcon()

        if (!previewLoaded) {
            Icon(
                contentDescription = null,
                imageVector = resolvedIcon ?: Icons.Draft,
                modifier = Modifier,
                tint = iconColor
            )
        }

        val isVideo = remember(uri, file, context) {
            file?.let {
                val mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(it.extension.lowercase())
                mime?.startsWith("video/") == true
            } ?: uri?.let { u ->
                context.contentResolver.getType(u)?.startsWith("video/") == true
            } ?: false
        }

        val imageLoader = remember(context, isVideo) {
            if (isVideo) {
                ImageLoader.Builder(context)
                    .components { add(VideoFrameDecoder.Factory()) }
                    .build()
            } else {
                ImageLoader.Builder(context).build()
            }
        }

        val request = remember(uri, isVideo) {
            ImageRequest.Builder(context)
                .data(uri)
                .apply { if (isVideo) videoFrameMillis(0) }
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state ->
                previewLoaded = state is AsyncImagePainter.State.Success
            }
        )

        Icon(
            imageVector = Icons.Check,
            modifier = Modifier
                .alpha(animatedSelectedAlpha)
                .background(MaterialTheme.colorScheme.primary)
                .padding(8.dp)
                .fillMaxSize(),
            tint = MaterialTheme.colorScheme.onPrimary,
            contentDescription = null
        )

    }
}

@Composable
fun File.resolveIcon(): ImageVector {
    if (isDirectory) return Icons.Folder

    val mime = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?: return Icons.Draft

    return when {
        mime.startsWith("image/") -> Icons.Image
        mime.startsWith("video/") -> Icons.Video
        mime.startsWith("audio/") -> Icons.Music
        mime.contains("zip") || mime.contains("x-rar") || mime.contains("x-7z") -> Icons.Zip
        mime.endsWith("vnd.android.package-archive") -> Icons.Apk
        mime.startsWith("text/") || mime.endsWith("json") || mime.endsWith("xml") || mime.endsWith("csv") -> Icons.Docs
        else -> Icons.Draft
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true)
@Composable
private fun CircleThumbPreview() {
    WearFilesTheme(
        seedColor = Color.Green
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleThumb(
                    uri = null,
                    modifier = Modifier.size(48.dp),
                    icon = Icons.Folder
                )
                CircleThumb(
                    uri = null,
                    modifier = Modifier.size(48.dp),
                    icon = Icons.Image,
                    isSelected = true
                )
            }
        }
    }
}
