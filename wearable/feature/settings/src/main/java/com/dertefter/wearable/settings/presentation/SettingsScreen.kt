package com.dertefter.wearable.settings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
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
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.github.droibit.oss_licenses.ui.wear.compose.material3.WearableOssLicensesActivity

@Composable
fun SettingsScreen(
    onEvent: (Event) -> Unit, dialogState: DialogState = DialogState.CLOSED
) {


    Dialog(
        showDialog = dialogState != DialogState.CLOSED, onDismissRequest = {
            onEvent(Event.CloseDialog)
        }) {
        Alert(
            icon = {
                if (dialogState == DialogState.SUCCESS) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = stringResource(R.string.send_success)
                    )
                } else if (dialogState == DialogState.FAILED) {
                    Icon(
                        imageVector = Icons.Error,
                        contentDescription = stringResource(R.string.send_git_failed)
                    )
                }

            },
            title = {

                Text(
                    text = when (dialogState) {
                        DialogState.SUCCESS -> {
                            stringResource(R.string.send_success)
                        }

                        DialogState.FAILED -> {
                            stringResource(R.string.send_git_failed)
                        }

                        else -> ""
                    }, textAlign = TextAlign.Center
                )
            },

            ) {

        }
    }


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


        val context = LocalContext.current

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
                        text = stringResource(R.string.title),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                FileItem(
                    transformationSpec = transformationSpec,
                    text = stringResource(R.string.theming),
                    icon = Icons.Palette,
                    onClick = {
                        onEvent(Event.OnNavigateToTheming)
                    },
                )
            }

            item {
                FileItem(
                    transformationSpec = transformationSpec,
                    text = stringResource(R.string.github),
                    icon = Icons.Github,
                    onClick = {
                        onEvent(Event.OnNavigateToGitRepo)
                    },
                )
            }

            item {
                FileItem(
                    transformationSpec = transformationSpec,
                    text = stringResource(R.string.licences),
                    icon = Icons.Docs,
                    onClick = {
                        context.startActivity(WearableOssLicensesActivity.createIntent(context))
                    },
                )
            }

        }


    }


}

@Preview(device = "id:wearos_square")
@Composable
fun SettingsScreenPrev() {
    SettingsScreen(
        onEvent = {})
}