package com.dertefter.wearable.file_list.presentation.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.dertefter.wearable.data.model.PrettyPath
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.FileItem
import com.dertefter.wearable.design.components.items.PathItem
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.file_list.R
import com.dertefter.wearable.file_list.presentation.Action
import com.dertefter.wearable.file_list.presentation.Event
import com.dertefter.wearable.menu.MenuMode
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import java.io.File

@Composable
fun ContentSuccess(
    path: PrettyPath,
    files: List<File>,
    onEvent: (Event) -> Unit,
    actions: List<Action> = emptyList()
) {

    val columnState = rememberTransformingLazyColumnState()

    val transformationSpec = rememberTransformationSpec()

    val selectedFilePaths = remember { mutableStateListOf<String>() }


    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
    )

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding,
        edgeButton = {
            if (actions.contains(Action.MORE)){
                EdgeButton(
                    onClick = {
                        onEvent(
                            Event.OnShowMenuFor(
                                paths = listOf(path.path),
                                menuMode = MenuMode.INSIDE
                            )
                        )
                    }
                ){
                    Icon(
                        imageVector = Icons.MoreHorizontal,
                        contentDescription = null
                    )
                }
            }
        }
    ) { contentPadding ->

        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        )
        {

            item(key = "path") {

                val fullPath = path.path
                val homePath = path.homePath


                PathItem(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 14.dp),
                    transformationSpec = transformationSpec,
                    fullPath = fullPath,
                    homePath = homePath,
                    homeIcon = Icons.Watch,
                )
            }

            items(files, key = { it.absolutePath }) { file ->

                FileItem(
                    transformationSpec,
                    text = file.name,
                    thumbnailUrl = file.absolutePath,
                    isSelected = selectedFilePaths.contains(file.absolutePath),
                    onIconClick = {
                        if (selectedFilePaths.contains(file.absolutePath)){
                            selectedFilePaths.remove(file.absolutePath)
                        }else{
                            selectedFilePaths.add(file.absolutePath)
                        }
                    },
                    file = file,
                    onClick = {
                        if (selectedFilePaths.isNotEmpty()){
                            if (selectedFilePaths.contains(file.absolutePath)){
                                selectedFilePaths.remove(file.absolutePath)
                            }else{
                                selectedFilePaths.add(file.absolutePath)
                            }

                        } else {
                            if (file.isFile) {
                                onEvent(Event.OnFileClick(file))
                            } else if (file.isDirectory) {
                                onEvent(Event.OnDirectoryClick(file.absolutePath))
                            }
                        }
                    },

                    onLongClick = {
                        if (selectedFilePaths.isNotEmpty()){
                            if (selectedFilePaths.contains(file.absolutePath)){
                                selectedFilePaths.remove(file.absolutePath)
                            }else{
                                selectedFilePaths.add(file.absolutePath)
                            }
                        } else {
                            onEvent(
                                Event.OnShowMenuFor(
                                    listOf(file.absolutePath),
                                    menuMode = MenuMode.OUTSIDE
                                )
                            )
                        }

                    }

                )
            }
        }


        AnimatedVisibility(
            visible = selectedFilePaths.isNotEmpty(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            )
            {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(
                            bottom = 12.dp,
                            start = 30.dp,
                            end = 30.dp
                        )
                ){

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        val allSelected = selectedFilePaths.size == files.size
                        Button(
                            onClick = {
                                if (allSelected) {
                                    selectedFilePaths.clear()
                                } else {
                                    selectedFilePaths.clear()
                                    selectedFilePaths.addAll(files.map { it.absolutePath })
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ){
                            Text(
                                if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 10.sp
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier
                                .size(28.dp),
                            onClick = {
                                selectedFilePaths.clear()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Close,
                                modifier = Modifier.size(20.dp),
                                contentDescription = null
                            )
                        }
                    }


                    FilledTonalIconButton(
                        modifier = Modifier
                            .size(34.dp),
                        onClick = {
                            onEvent(
                                Event.OnShowMenuFor(
                                    paths = selectedFilePaths.toList(),
                                    menuMode = MenuMode.OUTSIDE
                                )
                            )
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.MoreHorizontal,
                            modifier = Modifier.size(20.dp),
                            contentDescription = null
                        )
                    }

                }
            }
        }



    }

}

@Composable
@Preview(device = "id:wearos_small_round", showBackground = true)
fun ContentFailedPreview() {
    ContentSuccess(
        PrettyPath("", ""), files = emptyList(), onEvent = {})
}
