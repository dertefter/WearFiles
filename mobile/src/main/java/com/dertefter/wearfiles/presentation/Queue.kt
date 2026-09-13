package com.dertefter.wearfiles.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.wearfiles.FileTransferService
import com.dertefter.wearfiles.R
import com.dertefter.wearfiles.WearableFileSender
import com.dertefter.wearfiles.data.TransferItem
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.TransferStatus
import com.dertefter.wearfiles.ui.theme.WearFilesTheme

@Composable
fun Queue(
    modifier: Modifier = Modifier,
    initialUris: List<Uri> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val fileSender = remember { WearableFileSender(context) }
    
    val queue = TransferRepository.queue.filter { it.targetNodeId == TransferRepository.selectedNodeId }

    LaunchedEffect(initialUris) {
        if (initialUris.isNotEmpty()) {
            initialUris.forEach { uri ->
                fileSender.sendFileToWear(uri)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            fileSender.sendFileToWear(uri)
        }
    }

    QueueContent(
        modifier = modifier
            .padding(horizontal = 14.dp),
        queue = queue,
        onSelectFiles = { launcher.launch("*/*") },
        onCancelTransfer = { item ->
            val intent = Intent(context, FileTransferService::class.java).apply {
                action = "CANCEL_TRANSFER"
                putExtra("item_id", item.id)
            }
            context.startService(intent)
        },
        contentPadding = contentPadding
    )
}

@Composable
fun QueueContent(
    modifier: Modifier = Modifier,
    queue: List<TransferItem> = emptyList(),
    onSelectFiles: () -> Unit = {},
    onCancelTransfer: (TransferItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    Crossfade(
        targetState = queue.isEmpty(),
        modifier = modifier.fillMaxSize()
    )
    { isEmpty ->
        if (isEmpty){
            Box(contentAlignment = Alignment.Center,modifier = Modifier.fillMaxSize()) {
                Text(stringResource(R.string.queue_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                    TransferItem(
                        item = item,
                        onCancel = {
                            onCancelTransfer(item)
                        },
                        index = index,
                        count = queue.count(),
                        modifier = Modifier.animateItem()
                    )

                }
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewQueueScreen() {
    WearFilesTheme {
        QueueContent(
            queue = listOf(
                TransferItem(
                    id = "1",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "test_file.txt",
                    status = TransferStatus.PENDING
                ),
                TransferItem(
                    id = "2",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "image.png",
                    progress = 50,
                    status = TransferStatus.SENDING
                ),
                TransferItem(
                    id = "3",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "vidddddddddddddddddddddddddddddddddddddddddddeo.mp4",
                    status = TransferStatus.SUCCESS
                ),
                TransferItem(
                    id = "4",
                    targetNodeId = "node1",
                    uri = Uri.EMPTY,
                    fileName = "large_file.zip",
                    status = TransferStatus.ERROR
                )
            )
        )
    }
}
