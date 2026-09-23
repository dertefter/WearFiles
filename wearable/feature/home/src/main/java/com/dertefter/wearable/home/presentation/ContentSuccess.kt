package com.dertefter.wearable.home.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dertefter.wearable.data.model.PinnedItem
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.FileItem
import com.dertefter.wearable.design.components.items.FileItemType
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.home.R
import com.dertefter.wearable.home.model.HomeItem
import com.dertefter.wearable.home.model.HomeItemType
import com.dertefter.wearable.navigation.Routes
import java.io.File

@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun ContentSuccess(
    homeItems: List<HomeItem>,
    pinnedItems: List<PinnedItem>,
    onEvent: (Event) -> Unit,
) {
    NotificationPermissionRequest()

    val columnState = rememberTransformingLazyColumnState()

    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = PaddingValues(
            top = 28.dp, start = 10.dp, end = 10.dp, bottom = 12.dp
        ),
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
                ){
                    Text(
                        text = stringResource(R.string.home_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            for (item in homeItems) {

                item {

                    val title = when (item.type){
                        HomeItemType.AUDIO -> stringResource(R.string.music)
                        HomeItemType.IMAGES -> stringResource(R.string.photo)
                        HomeItemType.VIDEOS -> stringResource(R.string.video)
                        HomeItemType.STORAGE -> stringResource(R.string.storage)
                        HomeItemType.SETTINGS -> stringResource(R.string.settings)
                        HomeItemType.RECEIVED -> stringResource(R.string.received)

                    }

                    val icon = when (item.type){
                        HomeItemType.IMAGES -> Icons.Wallpaper
                        HomeItemType.VIDEOS -> Icons.Video
                        HomeItemType.AUDIO -> Icons.Music
                        HomeItemType.STORAGE -> Icons.Watch
                        HomeItemType.SETTINGS -> Icons.Settings
                        HomeItemType.RECEIVED -> Icons.DevicesWearables
                    }

                    val type = if (item.type == HomeItemType.STORAGE) {
                        FileItemType.PRIMARY
                    } else {
                        FileItemType.DEFAULT
                    }

                    FileItem(
                        transformationSpec,
                        text = title,
                        icon = icon,
                        onClick = {
                            onEvent(Event.OnNavigateTo(item.routes))
                        },
                        onLongClick = {},
                        type = type

                    )
                }
            }

            if (pinnedItems.isNotEmpty()){
                item {
                    ListSubHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ){
                        Text(
                            text = stringResource(R.string.pinned),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                for (item in pinnedItems) {

                    item {

                        FileItem(
                            transformationSpec,
                            text = item.name,
                            thumbnailUrl = item.path,
                            file = File(item.path),
                            onClick = {
                                if (item.isFile) {
                                    onEvent(Event.OnFileClick(item.path))
                                }  else {
                                    onEvent(Event.OnDirectoryClick(item.path))
                                }
                            },
                            onLongClick = {
                                onEvent(Event.OnShowMenuFor(listOf(item.path)))
                            },
                            type = FileItemType.DEFAULT

                        )
                    }
                }

            }

            item {
                FilledIconButton(
                    onClick = {
                        onEvent(Event.OnNavigateTo(Routes.Settings))
                    },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Settings,
                        contentDescription = ""
                    )
                }
            }

        }

    }

}

@Composable
@Preview(device = "id:wearos_small_round", showBackground = false)
fun ContentFailedPreview() {
    ContentSuccess(
        emptyList(),emptyList(),{}
    )
}
