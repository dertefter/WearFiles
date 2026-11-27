package com.dertefter.wearable.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.R
import com.dertefter.wearable.onboarding.presentation.content.OnBoardingContent
import com.dertefter.wearable.onboarding.presentation.content.DialogState

@Composable
fun OnBoardingScreen(
    onEvent: (Event) -> Unit,
    dialogState: DialogState = DialogState.CLOSED
) {

    Dialog(
        showDialog = dialogState != DialogState.CLOSED,
        onDismissRequest = {
            onEvent(Event.CloseDialog)
        }
    )
    {
        Alert(
            icon = {

                val iconRes = if (dialogState == DialogState.SUCCESS) {
                    Icons.Check
                } else {
                    Icons.Error
                }

                val contentDescription = if (dialogState == DialogState.SUCCESS) {
                    stringResource(R.string.success)
                } else {
                    stringResource(R.string.failed)
                }

                Icon(
                    imageVector = iconRes,
                    contentDescription = contentDescription
                )
            },
            title = {

                Text(
                    text = when (dialogState) {
                        DialogState.SUCCESS -> {
                            stringResource(R.string.send_success)
                        }

                        DialogState.FAILED -> {
                            stringResource(R.string.send_failed)
                        }

                        else -> ""
                    },
                    textAlign = TextAlign.Center
                )
            },
            content = {}
            )
    }

    OnBoardingContent(onEvent = onEvent)

}

@Composable
@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = false)
fun OnBoardingScreenPreview() {
    OnBoardingScreen({})
}


