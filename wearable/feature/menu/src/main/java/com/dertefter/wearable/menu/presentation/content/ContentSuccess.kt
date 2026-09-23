package com.dertefter.wearable.menu.presentation.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.FileItem
import com.dertefter.wearable.design.components.items.FileItemType
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.menu.R
import com.dertefter.wearable.menu.presentation.Event
import com.dertefter.wearable.menu.presentation.MenuAction
import com.dertefter.wearable.menu.presentation.MenuActionType
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun ContentSuccess(
    name: String,
    actions: List<MenuAction> = emptyList(),
    onEvent: (Event) -> Unit,
) {

    val columnState = rememberTransformingLazyColumnState()

    val transformationSpec = rememberTransformationSpec()

    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.Card,
    )

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding
    )
    { contentPadding ->

        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        ) {

            items(actions) { action ->

                val text = when (action.type) {
                    MenuActionType.DELETE -> stringResource(R.string.delete)
                    MenuActionType.RENAME -> stringResource(R.string.rename)
                    MenuActionType.NEW_DIR -> stringResource(R.string.new_dir)
                    MenuActionType.OPEN -> name
                    MenuActionType.PIN -> stringResource(R.string.pin)
                    MenuActionType.UNPIN -> stringResource(R.string.unpin)
                    MenuActionType.PASTE -> stringResource(R.string.paste)
                    MenuActionType.CUT -> stringResource(R.string.cut)
                    MenuActionType.COPY -> stringResource(R.string.copy)
                    MenuActionType.CANCEL_PASTE -> stringResource(R.string.cancel_paste)
                }

                val icon = when (action.type) {
                    MenuActionType.DELETE -> Icons.Delete
                    MenuActionType.RENAME -> Icons.Edit
                    MenuActionType.NEW_DIR -> Icons.CreateNewFolder
                    MenuActionType.OPEN -> Icons.OpenInNew
                    MenuActionType.PIN -> Icons.Keep
                    MenuActionType.UNPIN -> Icons.KeepOff
                    MenuActionType.PASTE -> Icons.ContentPaste
                    MenuActionType.CUT -> Icons.ContentCut
                    MenuActionType.COPY -> Icons.ContentCopy
                    MenuActionType.CANCEL_PASTE -> Icons.ContentPasteOff
                }

                val event: Event = when (action.type) {
                    MenuActionType.DELETE -> Event.OnNavigateToDelete(action.paths)
                    MenuActionType.RENAME -> Event.OnNavigateToRename(action.paths.first())
                    MenuActionType.NEW_DIR -> Event.OnNavigateToNewDirectory(action.paths.first())
                    MenuActionType.OPEN -> Event.OnHeaderClick(action.paths.first())
                    MenuActionType.PIN -> Event.OnPin(action.paths)
                    MenuActionType.UNPIN -> Event.OnUnpin(action.paths)
                    MenuActionType.PASTE -> Event.OnPaste(action.paths.first())
                    MenuActionType.CUT -> Event.OnCut(action.paths)
                    MenuActionType.COPY -> Event.OnCopy(action.paths)
                    MenuActionType.CANCEL_PASTE -> Event.OnCancelPaste(action.paths.first())
                }

                val type = when (action.type) {
                    MenuActionType.DELETE -> FileItemType.ERROR
                    MenuActionType.OPEN -> FileItemType.PRIMARY
                    else -> FileItemType.DEFAULT
                }

                FileItem(
                    transformationSpec = transformationSpec,
                    text = text,
                    icon = icon,
                    onClick = { onEvent(event) },
                    type = type
                )
            }
        }


    }

}

@Composable
@Preview(device = "id:wearos_small_round", showSystemUi = false, showBackground = true)
fun ContentFailedPreview(){
    ContentSuccess(
"really",
        emptyList(),
        onEvent = {}
    )
}
