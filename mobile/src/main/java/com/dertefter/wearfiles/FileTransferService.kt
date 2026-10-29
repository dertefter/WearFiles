package com.dertefter.wearfiles

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import com.dertefter.wearfiles.data.TransferAck
import com.dertefter.wearfiles.data.TransferAckRegistry
import com.dertefter.wearfiles.data.TransferItem
import com.dertefter.wearfiles.data.TransferProtocol
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.TransferStatus
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class FileTransferService : Service() {

    companion object {
        private const val TAG = "FileTransferService"
        private const val FOREGROUND_NOTIFICATION_ID = 1


        private const val ACK_TIMEOUT_MS = 120_000L
        private const val ACK_POLL_INTERVAL_MS = 150L
        private const val CHANNEL_CLOSED_ACK_GRACE_MS = 3_000L
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var notificationHelper: NotificationHelper
    private val processing = AtomicBoolean(false)

    private class TransferCancelledException(message: String) : Exception(message)

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
        if (!processing.compareAndSet(false, true)) {
            Log.d(TAG, "startProcessing called, but already processing transfers")
            return
        }
        Log.d(TAG, "startProcessing: Starting transfer loop in foreground service")

        val notification = notificationHelper.getNotification(getString(R.string.notification_file_queue), TransferStatus.SENDING)
        startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        scope.launch {
            try {
                while (true) {
                    val nextItem = TransferRepository.queue.firstOrNull { it.status == TransferStatus.PENDING } ?: break

                    Log.d(TAG, "Processing next queue item: id=${nextItem.id}, fileName=${nextItem.fileName}")
                    try {
                        processItem(nextItem)
                    } catch (e: TransferCancelledException) {
                        Log.d(TAG, "Item ${nextItem.id} (${nextItem.fileName}) cancelled: ${e.message}")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing item ${nextItem.id} (${nextItem.fileName}): ${e.message}", e)
                        TransferRepository.updateItem(nextItem.id) { it.copy(status = TransferStatus.ERROR) }
                        notificationHelper.showTransferNotification(nextItem.fileName, TransferStatus.ERROR)
                    }
                }
            } finally {
                processing.set(false)
            }

            if (!isActive) return@launch

            // An item can be enqueued between the last queue check and this point, so the
            // queue is checked again before the service stops.
            if (TransferRepository.queue.any { it.status == TransferStatus.PENDING }) {
                Log.d(TAG, "Pending transfers appeared after the loop finished, continuing")
                startProcessing()
            } else {
                Log.d(TAG, "No more pending transfers in queue. Stopping foreground service.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun processItem(item: TransferItem) {
        if (TransferRepository.queue.none { it.id == item.id }) {
            Log.w(TAG, "processItem: Item ${item.id} no longer exists in queue, skipping")
            return
        }

        Log.d(TAG, "processItem starting for item ${item.id} (${item.fileName}) -> targetNode=${item.targetNodeId}")

        TransferRepository.updateItem(item.id) { it.copy(status = TransferStatus.SENDING) }
        
        val nodeId = item.targetNodeId
        val channelClient = Wearable.getChannelClient(this)
        val totalSize = queryFileSize(item.uri)

        Log.d(TAG, "File size for ${item.fileName}: $totalSize bytes")

        // The transfer id keeps the channel path unique, so files with the same name and size
        // can never collide on the same path.
        val channelPath = "${TransferProtocol.CHANNEL_PATH_PREFIX}/${item.id}/$totalSize/${Uri.encode(item.fileName)}"
        Log.d(TAG, "Opening channel to node $nodeId with path $channelPath")
        
        val ack = TransferAckRegistry.register(item.id)
        val channelClosed = AtomicBoolean(false)
        // The receiver may die mid-transfer: then no confirmation will ever arrive.
        val channelCallback = object : ChannelClient.ChannelCallback() {
            override fun onChannelClosed(channel: ChannelClient.Channel, closeReason: Int, appSpecificErrorCode: Int) {
                Log.d(TAG, "Channel closed while transferring ${item.fileName}: reason=$closeReason, errorCode=$appSpecificErrorCode")
                channelClosed.set(true)
            }
        }
        var channel: ChannelClient.Channel? = null
        var cancelled = false

        try {
            val openedChannel = channelClient.openChannel(nodeId, channelPath).await()
                ?: throw IOException("Channel $channelPath to node $nodeId was not opened")
            channel = openedChannel
            Log.d(TAG, "Channel opened successfully: ${openedChannel.path}")
            channelClient.registerChannelCallback(openedChannel, channelCallback).await()

            Log.d(TAG, "Getting channel output stream and content input stream...")
            val outputStream = try {
                channelClient.getOutputStream(openedChannel).await()
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
                            throw TransferCancelledException(getString(R.string.error_canceled))
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

            // The channel has to stay open until the watch confirms that the file is stored:
            // closing it right after the last write can drop the data that is still in flight.
            val ackResult = awaitAck(item.id, ack, channelClosed)
            cancelled = ackResult == TransferAck.CANCELLED
            val status = when (ackResult) {
                TransferAck.SUCCESS -> TransferStatus.SUCCESS
                TransferAck.ERROR -> TransferStatus.ERROR
                TransferAck.CANCELLED -> null
                // No confirmation from the watch: report what could be verified locally.
                null -> if (totalSize <= 0 || bytesWritten == totalSize) TransferStatus.SUCCESS else TransferStatus.ERROR
            }
            Log.d(TAG, "Transfer ${item.id} finished with ack=$ackResult, status=$status")

            if (status != null && TransferRepository.queue.any { it.id == item.id }) {
                TransferRepository.updateItem(item.id) {
                    it.copy(progress = if (status == TransferStatus.SUCCESS) 100 else it.progress, status = status)
                }
                notificationHelper.showTransferNotification(item.fileName, status)
            }

        } catch (e: TransferCancelledException) {
            cancelled = true
            Log.d(TAG, "processItem cancelled for ${item.fileName} (id=${item.id}): ${e.message}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception during processItem for ${item.fileName} (id=${item.id}): ${e.message}", e)
            throw e
        } finally {
            if (channel != null) {
                try {
                    channelClient.unregisterChannelCallback(channel, channelCallback).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unregister channel callback: ${e.message}")
                }
            }
            TransferAckRegistry.unregister(item.id)
            if (channel != null) {
                try {
                    if (cancelled) {
                        // Tells the watch to drop the partial file instead of keeping it.
                        channelClient.close(channel, TransferProtocol.CLOSE_CODE_CANCELLED).await()
                    } else {
                        channelClient.close(channel).await()
                    }
                    Log.d(TAG, "Channel closed successfully (cancelled=$cancelled)")
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing channel: ${e.message}")
                }
            }
        }
    }

    private suspend fun awaitAck(
        itemId: String,
        ack: CompletableDeferred<TransferAck>,
        channelClosed: AtomicBoolean
    ): TransferAck? {
        val deadline = System.currentTimeMillis() + ACK_TIMEOUT_MS
        var closedAt: Long? = null
        while (System.currentTimeMillis() < deadline) {
            if (ack.isCompleted) return ack.await()
            if (TransferRepository.queue.none { it.id == itemId }) return TransferAck.CANCELLED
            if (channelClosed.get()) {
                // Give a confirmation that is already in flight a moment to arrive.
                val sinceClosed = closedAt ?: System.currentTimeMillis().also { closedAt = it }
                if (System.currentTimeMillis() - sinceClosed > CHANNEL_CLOSED_ACK_GRACE_MS) return TransferAck.ERROR
            }
            delay(ACK_POLL_INTERVAL_MS.milliseconds)
        }
        return if (ack.isCompleted) ack.await() else null
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else -1L
            } ?: -1L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query file size for uri $uri: ${e.message}")
            -1L
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroying, cancelling supervisor job")
        job.cancel()
    }
}
