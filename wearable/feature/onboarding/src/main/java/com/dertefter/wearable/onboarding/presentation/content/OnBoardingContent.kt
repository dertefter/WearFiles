package com.dertefter.wearable.onboarding.presentation.content

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dertefter.wearable.design.components.common.rememberSafeRotaryScrollableBehavior
import com.dertefter.wearable.design.components.items.BadgedTextItem
import com.dertefter.wearable.design.components.items.CodeItem
import com.dertefter.wearable.design.components.items.TextItem
import com.dertefter.wearable.design.icons.Icons
import com.dertefter.wearable.onboarding.R
import com.dertefter.wearable.onboarding.presentation.Event
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun OnBoardingContent(onEvent: (Event) -> Unit){

    var showInstructions by rememberSaveable { mutableStateOf(false) }
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(

    )

    val transformationSpec = rememberTransformationSpec()
    val context = LocalContext.current
    val errorSettingsText = stringResource(R.string.error_opening_settings)

    ScreenScaffold(
        scrollState = columnState, contentPadding = contentPadding
    )
    { contentPadding ->

        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding + PaddingValues(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            rotaryScrollableBehavior = rememberSafeRotaryScrollableBehavior(columnState)
        ) {

            item {
                TextItem(
                    transformationSpec,
                    text = stringResource(R.string.permission_required),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Button(
                    onClick = {
                        context.openStoragePermissionSettings {
                            Toast.makeText(context, errorSettingsText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                    )
                }
            }

            if (showInstructions) {
                item {
                    TextItem(
                        modifier = Modifier.animateItem(),
                        transformationSpec = transformationSpec,
                        text = stringResource(R.string.permission_alert)
                    )
                }

                item {
                    BadgedTextItem(
                        modifier = Modifier.animateItem(),
                        transformationSpec = transformationSpec,
                        text = stringResource(R.string.pt1),
                        badgeText = "1"
                    )
                }

                item {
                    BadgedTextItem(
                        modifier = Modifier.animateItem(),
                        transformationSpec = transformationSpec,
                        text = stringResource(R.string.pt2),
                        badgeText = "2"
                    )
                }

                item {
                    CodeItem(
                        modifier = Modifier.animateItem(),
                        transformationSpec = transformationSpec,
                        text = stringResource(R.string.pt2_command)
                    )
                }

                item {
                    BadgedTextItem(
                        modifier = Modifier.animateItem(),
                        transformationSpec = transformationSpec,
                        text = stringResource(R.string.pt3),
                        badgeText = "3"
                    )
                }

                item {
                    FilledTonalButton(
                        onClick = { onEvent(Event.OnOpenLinkOnPhone) },
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .animateItem(),
                        transformation = SurfaceTransformation(transformationSpec),
                        icon = {
                            Icon(
                                imageVector = Icons.MobileArrowRight,
                                contentDescription = "",
                            )
                        }) {
                        Text(
                            text = stringResource(R.string.info),
                            modifier = Modifier.padding(4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (!showInstructions){
                item {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            showInstructions = !showInstructions
                        },
                        modifier = Modifier
                            .animateItem()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(
                            text = stringResource(R.string.having_trouble),
                        )
                    }
                }
            }

        }

    }

}

@Composable
@Preview(device = "id:wearos_small_round", showSystemUi = false)
@Preview(widthDp = 200, showSystemUi = false)
fun OnBoardingContentPreview(){
    OnBoardingContent {}
}