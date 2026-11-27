package com.dertefter.wearable.video.presentation.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.CircleThumb
import com.dertefter.wearable.design.R
import com.dertefter.wearable.video.data.MediaItem
import com.dertefter.wearable.video.presentation.Event
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Suppress("UNUSED_PARAMETER")
@Composable
fun ContentSuccess(
    media: List<MediaItem>,
    onEvent: (Event) -> Unit,
) {

    val columnState = rememberTransformingLazyColumnState()

    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )

    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding,
    )
    { contentPadding ->

        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        ) {


            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ){
                    Text(
                        text = stringResource(R.string.videos_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (media.isEmpty()){
                item {
                    ListSubHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ){
                        Text(
                            text = stringResource(R.string.nothing_here),
                            modifier = Modifier
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            media.chunked(3).forEach { rowItems ->
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                CircleThumb(
                                    uri = item.uri,
                                    contentDescription = item.displayName,
                                    onClick = {
                                        onEvent(Event.OnOpenFile(item.uri, item.isVideo, item.displayName))
                                    },
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }


                }
            }
        }


    }

}

@Composable
@Preview(device = "id:wearos_square", showBackground = true)
fun ContentFailedPreview(){
    ContentSuccess(
        emptyList(),
        {  }
    )
}
