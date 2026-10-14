package com.dertefter.wearfiles

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
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

    companion object {
        private const val TAG = "FileTransferService"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var notificationHelper: NotificationHelper
    private var isProcessing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")
        when (action) {
            "CANCEL_TRANSFER" -> {
                val id = intent.getStringExtra("item_id")
                Log.d(TAG, "CANCEL_TRANSFER requested for item_id=$id")
                if (id != null) {
                    TransferRepository.removeItem(id)
                } else {
                    Log.w(TAG, "CANCEL_TRANSFER received with null item_id")
                }
                return START_NOT_STICKY
            }
            "ADD_TRANSFER" -> {
                @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>("file_uri")
                val fileName = intent.getStringExtra("file_name") ?: getString(R.string.file_default_name)
                val nodeId = intent.getStringExtra("target_node_id")
                Log.d(TAG, "ADD_TRANSFER received: uri=$uri, fileName=$fileName, nodeId=$nodeId")
                if (uri != null && nodeId != null) {
                    val newItem = TransferItem(
                        id = UUID.randomUUID().toString(),
                        targetNodeId = nodeId,
                        uri = uri,
                        fileName = fileName
                    )
                    TransferRepository.addItem(newItem)
                    Log.d(TAG, "Added item to transfer queue: id=${newItem.id}")
                    startProcessing()
                } else {
                    Log.e(TAG, "Failed to add transfer: uri or target_node_id is null (uri=$uri, nodeId=$nodeId)")
                }
            }
            else -> {
                Log.w(TAG, "onStartCommand received unknown or null action: $action")
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessing() {
        if (isProcessing) {
            Log.d(TAG, "startProcessing called, but already processing transfers")
            return
        }
        isProcessing = true
        Log.d(TAG, "startProcessing: Starting transfer loop in foreground service")
        
        val notification = notificationHelper.getNotification(getString(R.string.notification_file_queue), TransferStatus.SENDING)
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        scope.launch {
            while (isActive) {
                val nextItem = TransferRepository.queue.firstOrNull { it.status == TransferStatus.PENDING }
                if (nextItem == null) {
                    Log.d(TAG, "No more pending transfers in queue. Stopping foreground service.")
                    isProcessing = false
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }

                Log.d(TAG, "Processing next queue item: id=${nextItem.id}, fileName=${nextItem.fileName}")
                try {
                    processItem(nextItem)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing item ${nextItem.id} (${nextItem.fileName}): ${e.message}", e)
                    TransferRepository.updateItem(nextItem.id) { it.copy(status = TransferStatus.ERROR) }
                    notificationHelper.showTransferNotification(nextItem.fileName, TransferStatus.ERROR)
                }
            }
        }
    }

    private suspend fun processItem(item: TransferItem) {
        if (TransferRepository.queue.none { it.id == item.id }) {
            Log.w(TAG, "processItem: Item ${item.id} no longer exists in queue, skipping")
            return
        }

        Log.d(TAG, "processItem starting for item ${item.id} (${item.fileName}) -> targetNode=${item.targetNodeId}")

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearFiles:FileTransferWakeLock"
        )
        try {
            wakeLock.acquire(15 * 60 * 1000L)
            Log.d(TAG, "Wake lock acquired for transfer of ${item.fileName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}", e)
        }

        TransferRepository.updateItem(item.id) { it.copy(status = TransferStatus.SENDING) }
        
        val nodeId = item.targetNodeId
        val channelClient = Wearable.getChannelClient(this)

        val totalSize = try {
            contentResolver.query(item.uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else -1L
            } ?: -1L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query file size for uri ${item.uri}: ${e.message}", e)
            -1L
        }

        Log.d(TAG, "File size for ${item.fileName}: $totalSize bytes")

        val channelPath = "/file-transfer/$totalSize/${Uri.encode(item.fileName)}"
        Log.d(TAG, "Opening channel to node $nodeId with path $channelPath")
        
        val channel = try {
            channelClient.openChannel(nodeId, channelPath).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open channel to node $nodeId with path $channelPath: ${e.message}", e)
            throw e
        }

        Log.d(TAG, "Channel opened successfully: ${channel.path}")

        try {
            delay(500.milliseconds)
            Log.d(TAG, "Getting channel output stream and content input stream...")
            val outputStream = try {
                channelClient.getOutputStream(channel).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get channel output stream: ${e.message}", e)
                throw e
            }

            val inputStream = contentResolver.openInputStream(item.uri)
            if (inputStream == null) {
                val errorMsg = getString(R.string.error_input_stream)
                Log.e(TAG, "ContentResolver returned null InputStream for uri=${item.uri}")
                throw Exception(errorMsg)
            }

            val buffer = ByteArray(32768)
            var bytesWritten = 0L
            var lastUpdate = 0L

            Log.d(TAG, "Beginning file streaming to node $nodeId...")
            inputStream.use { input ->
                outputStream.use { output ->
                    var bytes = input.read(buffer)
                    while (bytes != -1) {
                        if (TransferRepository.queue.none { it.id == item.id }) {
                            Log.w(TAG, "Transfer cancelled for item ${item.id} during write loop")
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
            Log.d(TAG, "File stream successfully completed for ${item.fileName}. Total bytes written: $bytesWritten")
            TransferRepository.updateItem(item.id) { it.copy(progress = 100) }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during processItem for ${item.fileName} (id=${item.id}): ${e.message}", e)
            throw e
        } finally {
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.d(TAG, "Wake lock released")
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing wake lock: ${e.message}")
                }
            }
            try {
                Log.d(TAG, "Closing channel ${channel.path}")
                channelClient.close(channel).await()
                Log.d(TAG, "Channel closed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error closing channel: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroying, cancelling supervisor job")
        job.cancel()
    }
}
