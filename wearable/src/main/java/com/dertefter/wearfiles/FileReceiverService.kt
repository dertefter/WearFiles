package com.dertefter.wearfiles

import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileReceiverService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onChannelOpened(channel: com.google.android.gms.wearable.ChannelClient.Channel) {
        Log.d("FileReceiverService", "Channel opened: ${channel.path}")
        if (channel.path.startsWith("/file-transfer/")) {
            val pathData = channel.path.substringAfter("/file-transfer/")
            val expectedSize: Long
            val fileName: String

            if (pathData.contains("/")) {
                expectedSize = pathData.substringBefore("/").toLongOrNull() ?: -1L
                fileName = Uri.decode(pathData.substringAfter("/"))
            } else {
                expectedSize = -1L
                fileName = Uri.decode(pathData)
            }

            val nodeId = channel.nodeId
            scope.launch {
                receiveFileFromChannel(channel, fileName, nodeId, expectedSize)
            }
        }
    }

    private suspend fun receiveFileFromChannel(
        channel: com.google.android.gms.wearable.ChannelClient.Channel,
        fileName: String,
        nodeId: String,
        expectedSize: Long
    ) {
        val channelClient = Wearable.getChannelClient(this)
        val receivedDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Received")
        try {
            if (!receivedDir.exists()) {
                receivedDir.mkdirs()
            }
            
            val file = File(receivedDir, fileName)
            Log.d("FileReceiverService", "Receiving file: $fileName to ${file.absolutePath}")
            
            val inputStream = channelClient.getInputStream(channel).await()
            var bytesReceived = 0L
            
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { outputStream ->
                    inputStream.use { input ->
                        val buffer = ByteArray(32768)
                        var bytes = input.read(buffer)
                        while (bytes != -1) {
                            outputStream.write(buffer, 0, bytes)
                            bytesReceived += bytes
                            bytes = input.read(buffer)
                        }
                        outputStream.flush()
                    }
                }
            }

            if (expectedSize != -1L && bytesReceived != expectedSize) {
                throw Exception("Incomplete file: received $bytesReceived of $expectedSize")
            }
            
            Log.d("FileReceiverService", "File saved successfully!")
            
            try {
                Wearable.getMessageClient(this)
                    .sendMessage(nodeId, "/file-transfer-status", "success:$fileName".toByteArray())
                    .await()
            } catch (e: Exception) {
                Log.w("FileReceiverService", "Failed to send success status: ${e.message}")
            }
                
        } catch (e: Exception) {
            Log.e("FileReceiverService", "Error receiving file", e)
            val file = File(receivedDir, fileName)
            if (file.exists()) {
                file.delete()
            }

            try {
                Wearable.getMessageClient(this)
                    .sendMessage(nodeId, "/file-transfer-status", "error:$fileName".toByteArray())
                    .await()
            } catch (sendException: Exception) {
                Log.w("FileReceiverService", "Failed to send error status: ${sendException.message}")
            }
        } finally {
            try {
                channelClient.close(channel).await()
            } catch (closeException: Exception) {
                Log.w("FileReceiverService", "Failed to close channel: ${closeException.message}")
            }
        }
    }
}
