package com.dertefter.wearable.images.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.dertefter.wearable.design.components.basic_screens.AskDefaultScreen
import com.dertefter.wearable.design.components.basic_screens.ContentLoadingDefaultScreen
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.images.R
import com.dertefter.wearable.images.presentation.content.ContentSuccess
import com.dertefter.wearable.images.presentation.content.PermissionDialogState
import com.dertefter.wearable.images.presentation.content.UiState

@Composable
fun GalleyScreen(onEvent: (Event) -> Unit, uiState: UiState, dialogState: PermissionDialogState) {


    val context = LocalContext.current
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->

        onEvent(Event.OnLoad)
    }

    if (dialogState == PermissionDialogState.SHOW) {
        AskDefaultScreen(
            title = stringResource(R.string.perm_title),
            content = listOf({
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    permissionsLauncher.launch(permissions)
                },
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.grant_permissions)
                )
            }
        },
            {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ){
                    FilledTonalIconButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }, modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Settings,
                            contentDescription = null,
                        )
                    }
                }

        }

        ))
    } else {
        when (uiState) {
            is UiState.Loading -> ContentLoadingDefaultScreen()
            is UiState.Success -> ContentSuccess(
                uiState.mediaItems, onEvent
            )

            is UiState.Failed -> {
            }
        }

    }


}

@Preview(device = "id:wearos_square")
@Composable
fun GalleyScreenPrev() {
    GalleyScreen(
        onEvent = {}, uiState = UiState.Loading, dialogState = PermissionDialogState.SHOW
    )
}