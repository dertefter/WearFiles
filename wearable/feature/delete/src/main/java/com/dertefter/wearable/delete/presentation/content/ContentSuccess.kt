package com.dertefter.wearable.delete.presentation.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dertefter.wearable.design.R
import com.dertefter.wearable.delete.presentation.Event
import com.dertefter.wearable.design.components.basic_screens.DialogDefaultScreen
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.design.theme.WearFilesTheme

@Composable
fun ContentSuccess(
    paths: List<String>,
    onEvent: (Event) -> Unit,
) {

    DialogDefaultScreen(
        title = stringResource(R.string.delete_title),
        onCancel = {
            onEvent(Event.OnNavigateBack)
        },
        onOk = {
            onEvent(Event.OnDelete(paths))
        },
        okIcon = Icons.Delete,
        cancelIcon = Icons.ArrowBack
    )

}

@Composable
@Preview(device = "id:wearos_small_round", showSystemUi = false, showBackground = true)
fun ContentFailedPreview(){
    WearFilesTheme() {
        ContentSuccess(
            emptyList(),{}
        )
    }
}