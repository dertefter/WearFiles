package com.dertefter.wearable.music.presentation.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.dertefter.wearable.design.components.items.FileItem
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.R
import com.dertefter.wearable.music.data.MusicItem
import com.dertefter.wearable.music.presentation.Event
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun ContentSuccess(
    media: List<MusicItem>,
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
                        text = stringResource(R.string.music_title),
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

            for (item in media){
                item {
                    FileItem(
                        transformationSpec = transformationSpec,
                        text = item.displayName,
                        thumbnailUrl = item.uri.toString(),
                        onClick = {
                            onEvent(Event.OnMediaClick(item))
                        },
                        icon = Icons.Music,
                    )
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
