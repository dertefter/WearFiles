package com.dertefter.wearable.text_viewer.presentation.content


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.yazantarifi.compose.library.MarkdownConfig
import com.yazantarifi.compose.library.MarkdownViewComposable

@Composable
fun ContentSuccess(
    fileName: String,
    content: String?
) {
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.BodyText,
        last = ColumnItemType.Button,
    )

    val markdownContent: String = if (content.isNullOrEmpty()){
        stringResource(R.string.empty)
    }else {
        content
    }

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding,
    ) { contentPadding ->

        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.padding(4.dp),
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        ) {
            item {

                ListHeader(
                ){
                    Text(fileName)
                }

            }



            item {
                MarkdownViewComposable(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    content = markdownContent,
                    config = MarkdownConfig(
                        isLinksClickable = true,
                        isImagesClickable = false,
                        isScrollEnabled = false,
                        colors = HashMap<String, Color>().apply {
                            this[MarkdownConfig.CHECKBOX_COLOR] = MaterialTheme.colorScheme.secondary
                            this[MarkdownConfig.LINKS_COLOR] = MaterialTheme.colorScheme.primary
                            this[MarkdownConfig.TEXT_COLOR] = MaterialTheme.colorScheme.onSurface
                            this[MarkdownConfig.HASH_TEXT_COLOR] = MaterialTheme.colorScheme.onSurfaceVariant
                            this[MarkdownConfig.CODE_BACKGROUND_COLOR] = MaterialTheme.colorScheme.surfaceContainerLow
                            this[MarkdownConfig.CODE_BLOCK_TEXT_COLOR] = MaterialTheme.colorScheme.onSurface
                        }
                    )
                ) { _, type ->
                    when (type) {
                        MarkdownConfig.IMAGE_TYPE -> {}
                        MarkdownConfig.LINK_TYPE -> {}
                    }
                }
            }
        }
    }
}

@Composable
@WearPreviewDevices
fun ContentFailedPreview() {
    ContentSuccess("", "")
}
