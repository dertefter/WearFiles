package com.dertefter.wearable.rename.presentation.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dertefter.wearable.design.components.basic_screens.ContentFailedDefaultScreen
import com.dertefter.wearable.design.R
import com.dertefter.wearable.rename.presentation.Event

@Composable
fun ContentFailed(
    onEvent: (Event) -> Unit,
){
    ContentFailedDefaultScreen(
        title = stringResource(id = R.string.rename_failed),
        onBackClick = {onEvent(Event.OnNavigateBack)}
    )
}