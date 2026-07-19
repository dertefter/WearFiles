package com.dertefter.wearfiles

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.dertefter.wearfiles.data.TransferItem
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.TransferStatus
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class FileTransferService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var notificationHelper: NotificationHelper
    private var isProcessing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CANCEL_TRANSFER" -> {
                val id = intent.getStringExtra("item_id")
                if (id != null) {
                    TransferRepository.removeItem(id)
                }
                return START_NOT_STICKY
            }
            "ADD_TRANSFER" -> {
                @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>("file_uri")
                val fileName = intent.getStringExtra("file_name") ?: getString(R.string.file_default_name)
                val nodeId = intent.getStringExtra("target_node_id")
                if (uri != null && nodeId != null) {
                    val newItem = TransferItem(
                        id = UUID.randomUUID().toString(),
                        targetNodeId = nodeId,
                        uri = uri,
                        fileName = fileName
                    )
                    TransferRepository.addItem(newItem)
                    startProcessing()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessing() {
        if (isProcessing) return
        isProcessing = true
        
        val notification = notificationHelper.getNotification(getString(R.string.notification_file_queue), TransferStatus.SENDING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        scope.launch {
            while (isActive) {
                val nextItem = TransferRepository.queue.firstOrNull { it.status == TransferStatus.PENDING }
                if (nextItem == null) {
                    isProcessing = false
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }

                try {
                    processItem(nextItem)
                } catch (e: Exception) {
                    Log.e("FileTransferService", "Error processing item ${nextItem.id}", e)
                    TransferRepository.updateItem(nextItem.id) { it.copy(status = TransferStatus.ERROR) }
                    notificationHelper.showTransferNotification(nextItem.fileName, TransferStatus.ERROR)
                }
            }
        }
    }

    private suspend fun processItem(item: TransferItem) {
        if (TransferRepository.queue.none { it.id == item.id }) return

        TransferRepository.updateItem(item.id) { it.copy(status = TransferStatus.SENDING) }
        
        val nodeId = item.targetNodeId
        val channelClient = Wearable.getChannelClient(this)

        val totalSize = try {
            contentResolver.query(item.uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }

        val channelPath = "/file-transfer/$totalSize/${Uri.encode(item.fileName)}"
        
        val channel = channelClient.openChannel(nodeId, channelPath).await()
        
        try {
            delay(500.milliseconds)
            val outputStream = channelClient.getOutputStream(channel).await()
            val inputStream = contentResolver.openInputStream(item.uri) ?: throw Exception(getString(R.string.error_input_stream))

            val buffer = ByteArray(32768)
            var bytesWritten = 0L
            var lastUpdate = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    var bytes = input.read(buffer)
                    while (bytes != -1) {
                        if (TransferRepository.queue.none { it.id == item.id }) {
                            throw CancellationException(getString(R.string.error_canceled))
                        }

                        output.write(buffer, 0, bytes)
                        bytesWritten += bytes
                        
                        val progress = if (totalSize > 0) (bytesWritten * 100 / totalSize).toInt() else 0
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdate > 300) { 
                            TransferRepository.updateItem(item.id) { it.copy(progress = progress) }
                            notificationHelper.showTransferNotification(item.fileName, TransferStatus.SENDING, progress)
                            lastUpdate = currentTime
                        }
                        bytes = input.read(buffer)
                    }
                    output.flush()
                }
            }
            TransferRepository.updateItem(item.id) { it.copy(progress = 100) }

        } finally {
            try {
                channelClient.close(channel).await()
            } catch (e: Exception) {
                Log.w("FileTransferService", "Error closing channel: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
