package com.dertefter.wearfiles.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.data.ConnectionStatus
import com.dertefter.wearfiles.data.WearNode
import com.dertefter.wearfiles.ui.theme.WearFilesTheme
import com.materialkolor.ktx.harmonize

@Composable
fun NodeSelectionPager(
    modifier: Modifier = Modifier,
    nodes: List<WearNode>,
    selectedNodeId: String?,
    onNodeSelected: (String) -> Unit
) {
    if (nodes.isEmpty()) {
        NodeConnectionCard(
            modifier = modifier
                .padding( horizontal = 14.dp,
                    vertical = 4.dp),
            name = "",
            status = ConnectionStatus.NOT_CONNECTED,
            isSelected = false
        )
        return
    }

    val initialPage = nodes.indexOfFirst { it.id == selectedNodeId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { nodes.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onNodeSelected(nodes[pagerState.currentPage].id)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 4.dp
        ),
        pageSpacing = 4.dp
    ) { page ->
        val node = nodes[page]
        NodeConnectionCard(
            name = node.name,
            status = node.status,
            isSelected = node.id == selectedNodeId
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NodeConnectionCard(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean,
    status: ConnectionStatus
) {
    val isReady = status == ConnectionStatus.READY

    val targetContainerColor = if (isReady) {
        if (isSelected) colorResource(R.color.on_success_container) else colorResource(R.color.success_container)
    } else {
        if (isSelected) colorResource(R.color.on_warn_container) else colorResource(R.color.warn_container)
    }.harmonize(MaterialTheme.colorScheme.primary, false)

    val targetContentColor = if (isReady) {
        if (isSelected) colorResource(R.color.success_container) else colorResource(R.color.on_success_container)
    } else {
        if (isSelected) colorResource(R.color.warn_container) else colorResource(R.color.on_warn_container)
    }.harmonize(MaterialTheme.colorScheme.primary, false)

    val containerColor by animateColorAsState(targetContainerColor, label = "containerColor")
    val contentColor by animateColorAsState(targetContentColor, label = "contentColor")

    val iconBgColor by animateColorAsState(
        if (!isSelected) Color.Transparent else contentColor,
        label = "iconBgColor"
    )

    val iconTintColor by animateColorAsState(
        if (isReady) colorResource(R.color.on_success_container) else colorResource(R.color.on_warn_container),
        label = "iconTintColor"
    )

    val iconScaleState = animateFloatAsState(
        if (isSelected) 1f else 1.3f
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angleState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val iconRes = if (status == ConnectionStatus.READY) {
        R.drawable.ic_watch_check
    } else {
        R.drawable.ic_watch_off
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)

            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier
                        .graphicsLayer {
                            val s = iconScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                        .clip(MaterialShapes.Cookie12Sided.toShape(angleState.value.toInt()))
                        .background(iconBgColor)
                        .padding(8.dp)
                        .fillMaxSize()
                )
            }


            Spacer(modifier = Modifier.width(16.dp))

            Column {

                AnimatedVisibility(
                    name.isNotEmpty()
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = when (status) {
                        ConnectionStatus.READY -> stringResource(R.string.watch_connected)
                        ConnectionStatus.NOT_CONNECTED -> stringResource(R.string.watch_not_found)
                        ConnectionStatus.NOT_NEARBY -> stringResource(R.string.watch_not_nearby)
                        ConnectionStatus.APP_NOT_INSTALLED -> stringResource(R.string.app_not_installed)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (status) {
                        ConnectionStatus.READY -> stringResource(R.string.ready_to_send)
                        ConnectionStatus.NOT_CONNECTED -> stringResource(R.string.check_connection)
                        ConnectionStatus.NOT_NEARBY -> stringResource(R.string.bring_watch_closer)
                        ConnectionStatus.APP_NOT_INSTALLED -> stringResource(R.string.install_app_on_watch)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NodeSelectionPagerPreview() {
    val nodes = listOf(
        WearNode("1", "Galaxy Watch 4", ConnectionStatus.READY),
        WearNode("2", "Pixel Watch", ConnectionStatus.NOT_CONNECTED),
        WearNode("3", "TicWatch Pro", ConnectionStatus.APP_NOT_INSTALLED)
    )
    WearFilesTheme {
        NodeSelectionPager(
            nodes = nodes,
            selectedNodeId = "1",
            onNodeSelected = {}
        )
    }

}

@Preview(showBackground = true)
@Composable
fun NodeSelectionPagerPreview2() {
    WearFilesTheme {
        NodeSelectionPager(
            nodes = emptyList(),
            selectedNodeId = "1",
            onNodeSelected = {}
        )
    }
}

