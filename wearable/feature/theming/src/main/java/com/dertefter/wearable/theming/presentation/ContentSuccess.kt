package com.dertefter.wearable.theming.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.FileItem
import com.dertefter.wearable.design.components.items.FileItemType
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.theme.WearFilesTheme
import com.dertefter.wearable.design.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun ContentSuccess(
    colors: List<Color>,
    selectColor: Color?,
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
    ) { contentPadding ->

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
                ) {
                    Text(
                        text = stringResource(R.string.theme_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                FileItem(
                    transformationSpec = transformationSpec,
                    icon = Icons.History,
                    text = stringResource(R.string.default_theme),
                    onClick = {
                        onEvent(Event.OnSelectColor(null))
                    },
                    type = if (selectColor == null) {
                        FileItemType.PRIMARY
                    } else {
                        FileItemType.DEFAULT
                    }
                )
            }

            colors.chunked(3).forEach { rowItems ->
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
                            rowItems.forEach { color ->
                                WearFilesTheme(
                                    seedColor = color
                                ) {

                                    val isSelected = color == selectColor

                                    if (!isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }

                                    val iconColor = if (!isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }

                                    val bgColor = if (!isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                onEvent(Event.OnSelectColor(color))
                                            }

                                    ) {

                                        Column(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.onTertiaryContainer)
                                            )
                                        }


                                        if (color == selectColor) {
                                            Icon(
                                                imageVector = Icons.Check,
                                                contentDescription = "",
                                                modifier = Modifier
                                                    .background(iconColor.copy( alpha = 0.5f))
                                                    .fillMaxSize(),
                                                tint = bgColor,
                                            )
                                        }
                                    }
                                }

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
fun ContentFailedPreview() {
    ContentSuccess(
        listOf(
            Color.Green, Color.Red
        ), Color.Red, {})
}
