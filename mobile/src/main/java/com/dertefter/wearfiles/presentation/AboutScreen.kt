package com.dertefter.wearfiles.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.ui.theme.WearFilesTheme
import com.github.droibit.oss_licenses.ui.compose.material3.OssLicensesActivity


data class AboutItem(
    val text: String,
    val icon: Painter? = null,
    val onClick: () -> Unit = {}

)

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {

    val context = LocalContext.current

    val items = listOf(
        AboutItem(
            text = stringResource(R.string.about_oss_licenses),
            icon = painterResource(R.drawable.ic_article),
            onClick = {
                context.startActivity(OssLicensesActivity.createIntent(context))
            }
        ),
        AboutItem(
            text = stringResource(R.string.about_telegram_chat),
            icon = painterResource(R.drawable.ic_tg),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://t.me/wearfiles_app".toUri())
                context.startActivity(intent)
            }
        ),
        AboutItem(
            text = stringResource(R.string.about_github),
            icon = painterResource(R.drawable.ic_github),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://github.com/dertefter/WearFiles".toUri())
                context.startActivity(intent)
            }
        ),
        AboutItem(
            text = stringResource(R.string.about_donate),
            icon = painterResource(R.drawable.ic_attach_money),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://www.donationalerts.com/r/dertefter".toUri())
                context.startActivity(intent)
            }
        )
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.mobile_app_name))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->

        val colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )

        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(items) { index, item ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = items.count()),
                    colors = colors,
                    onClick = item.onClick,
                    leadingContent = {
                        item.icon?.let { icon ->
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    },
                    content = { Text(item.text) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    WearFilesTheme {
        AboutScreen()
    }
}