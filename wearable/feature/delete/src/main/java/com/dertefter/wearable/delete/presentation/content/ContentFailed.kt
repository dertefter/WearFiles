package com.dertefter.wearable.delete.presentation.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dertefter.wearable.design.R
import com.dertefter.wearable.delete.presentation.Event
import com.dertefter.wearable.design.components.basic_screens.ContentFailedDefaultScreen

@Composable
fun ContentFailed(
    onEvent: (Event) -> Unit,
){
    ContentFailedDefaultScreen(
        title = stringResource(id = R.string.delete_failed),
        onBackClick = {onEvent(Event.OnNavigateBack)}
    )
}