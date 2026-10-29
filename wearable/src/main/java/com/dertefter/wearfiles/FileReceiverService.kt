package com.dertefter.wearfiles

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService


class FileReceiverService : WearableListenerService() {

    companion object {
        private const val TAG = "FileReceiverService"
        private const val CHANNEL_PATH_PREFIX = "/file-transfer"
        private const val CLOSE_CODE_CANCELLED = 1001
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        Log.d(TAG, "onChannelOpened: path=${channel.path}, nodeId=${channel.nodeId}")
        val transferId = transferIdOf(channel.path)
        if (transferId == null) {
            Log.w(TAG, "Ignored channel opening with unsupported path: ${channel.path}")
            return
        }

        // Path layout: /file-transfer/<transferId>/<size>/<encodedFileName>
        val pathParts = channel.path.removePrefix("$CHANNEL_PATH_PREFIX/").split("/", limit = 3)
        if (pathParts.size < 3) {
            Log.w(TAG, "Ignored channel opening with incomplete path: ${channel.path}")
            return
        }
        val expectedSize = pathParts[1].toLongOrNull() ?: -1L
        val fileName = Uri.decode(pathParts[2])
        Log.d(TAG, "Parsed transfer info: transferId=$transferId, fileName='$fileName', expectedSize=$expectedSize bytes")

        ChannelRegistry.put(transferId, channel)

        val intent = Intent(this, FileReceiveService::class.java).apply {
            putExtra(FileReceiveService.EXTRA_TRANSFER_ID, transferId)
            putExtra(FileReceiveService.EXTRA_FILE_NAME, fileName)
            putExtra(FileReceiveService.EXTRA_EXPECTED_SIZE, expectedSize)
            putExtra(FileReceiveService.EXTRA_NODE_ID, channel.nodeId)
        }

        try {
            ContextCompat.startForegroundService(this, intent)
            Log.d(TAG, "Handed transfer $transferId over to FileReceiveService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FileReceiveService for $fileName: ${e.message}", e)
            ChannelRegistry.clear(transferId)
            // Closing the channel makes the phone fail immediately instead of waiting for
            // a confirmation that will never come.
            Wearable.getChannelClient(this).close(channel)
        }
    }

    override fun onChannelClosed(channel: ChannelClient.Channel, closeReason: Int, appSpecificErrorCode: Int) {
        Log.d(TAG, "onChannelClosed: path=${channel.path}, reason=$closeReason, errorCode=$appSpecificErrorCode")
        if (appSpecificErrorCode == CLOSE_CODE_CANCELLED) {
            transferIdOf(channel.path)?.let { transferId ->
                ChannelRegistry.markCancelled(transferId)
                Log.d(TAG, "Transfer $transferId marked as cancelled by the sender")
            }
        }
    }

    private fun transferIdOf(path: String): String? {
        val prefix = "$CHANNEL_PATH_PREFIX/"
        if (!path.startsWith(prefix)) return null
        return path.removePrefix(prefix).substringBefore("/").takeIf { it.isNotEmpty() }
    }
}
