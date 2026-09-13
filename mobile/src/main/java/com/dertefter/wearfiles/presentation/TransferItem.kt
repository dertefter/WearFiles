package com.dertefter.wearfiles.presentation

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.data.TransferItem
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.TransferStatus
import com.dertefter.wearfiles.ui.theme.WearFilesTheme
import com.materialkolor.ktx.harmonize
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransferItem(
    modifier: Modifier = Modifier,
    item: TransferItem,
    onCancel: () -> Unit,
    index: Int = 1,
    count: Int = 1,
) {

    val successColor = colorResource(R.color.success_container).harmonize(MaterialTheme.colorScheme.primary)
    val errorColor = MaterialTheme.colorScheme.errorContainer
    val defaultColor = MaterialTheme.colorScheme.surfaceContainer

    val onSuccessColor = colorResource(R.color.on_success_container).harmonize(MaterialTheme.colorScheme.primary)
    val onErrorColor = MaterialTheme.colorScheme.onErrorContainer
    val onDefaultColor = MaterialTheme.colorScheme.onSurface

    val progress by animateFloatAsState(
        item.progress / 100f
    )

    val bgColor by animateColorAsState(
        when (item.status) {
            TransferStatus.PENDING -> defaultColor
            TransferStatus.SENDING -> defaultColor
            TransferStatus.SUCCESS -> successColor
            TransferStatus.ERROR -> errorColor
        }
    )

    val contentColor by animateColorAsState(
        when (item.status) {
            TransferStatus.PENDING -> onDefaultColor
            TransferStatus.SENDING -> onDefaultColor
            TransferStatus.SUCCESS -> onSuccessColor
            TransferStatus.ERROR -> onErrorColor
        }
    )

    val colors =
        ListItemDefaults.colors(containerColor = bgColor)

    val dismissState = rememberSwipeToDismissBoxState()
    var isVisible by remember { mutableStateOf(true) }
    if (isVisible) {

        val density = LocalDensity.current

        val offset = try { dismissState.requireOffset() } catch (_: Exception) { 0f }

        val dynamicWidth = with(density) { abs(offset).toDp() }

        val thresholdPx = with(density) { 80.dp.toPx() }
        val fraction = (abs(offset) / thresholdPx).coerceIn(0f, 1f)

        val direction = dismissState.dismissDirection
        val isToEnd = direction == SwipeToDismissBoxValue.StartToEnd

        SwipeToDismissBox(
            modifier = modifier,
            state = dismissState,
            backgroundContent = {
                Box(Modifier.fillMaxSize()) {
                    if (direction != SwipeToDismissBoxValue.Settled) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .align(if (isToEnd) Alignment.CenterStart else Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(dynamicWidth)
                                .background( MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ){
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .alpha(fraction)
                            )
                        }
                    }
                }
            },
            onDismiss = { _ ->
                isVisible = false
                TransferRepository.removeItem(item.id)
            },
        ) {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                colors = colors,
                contentPadding = PaddingValues(),
                content = {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 10.dp)
                            .padding(
                                vertical = 12.dp
                            )
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ){

                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = contentColor
                            )
                            Text(
                                text = when (item.status) {
                                    TransferStatus.PENDING -> stringResource(R.string.status_pending)
                                    TransferStatus.SENDING -> if (item.progress < 100) stringResource(R.string.status_sending) else stringResource(R.string.status_waiting_watch)
                                    TransferStatus.SUCCESS -> stringResource(R.string.status_success)
                                    TransferStatus.ERROR -> stringResource(R.string.status_error)
                                },
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color = contentColor
                            )
                        }

                        Crossfade(
                            targetState = item.status,

                            ) {
                            when (it) {
                                TransferStatus.PENDING -> {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier
                                            .size(48.dp),
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                onClick = onCancel
                                            )
                                            .padding(12.dp)
                                    )

                                }
                                TransferStatus.SENDING -> {
                                    CircularWavyProgressIndicator(
                                        progress = {progress},
                                        stroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                                        modifier = Modifier
                                            .size(48.dp)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                onClick = onCancel
                                            )
                                            .padding(12.dp)
                                    )
                                }
                                TransferStatus.SUCCESS -> {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        tint = bgColor,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(MaterialShapes.Pill.toShape())
                                            .background(contentColor)
                                            .padding(12.dp)
                                    )
                                }
                                TransferStatus.ERROR -> {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(MaterialShapes.Cookie9Sided.toShape())
                                            .clickable(
                                                onClick = { TransferRepository.removeItem(item.id) }
                                            )
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }





                },
            )
        }
    }


}

@Preview(showBackground = true)
@Composable
fun TransferItemPreview() {
    WearFilesTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransferItem(
                item = TransferItem(
                    id = "1",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "movie.mp4",
                    progress = 0,
                    status = TransferStatus.PENDING
                ),
                onCancel = {},
                count = 4,
                index = 0
            )
            TransferItem(
                item = TransferItem(
                    id = "2",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "imagecccccccccccccccccccccccccccccccccccccccccccccccccc.jpg",
                    progress = 45,
                    status = TransferStatus.SENDING
                ),
                onCancel = {},
                count = 4,
                index = 1
            )
            TransferItem(
                item = TransferItem(
                    id = "3",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "document.pdf",
                    progress = 100,
                    status = TransferStatus.SUCCESS
                ),
                onCancel = {},
                count = 4,
                index = 2
            )
            TransferItem(
                item = TransferItem(
                    id = "4",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "large_file.zip",
                    progress = 10,
                    status = TransferStatus.ERROR
                ),
                onCancel = {},
                count = 4,
                index = 3
            )
        }
    }
}

