package com.dertefter.wearfiles

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class FileReceiveService : Service() {

    companion object {
        private const val TAG = "FileReceiveService"
        private const val CHANNEL_ID = "file_receiver_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_TRANSFER_ID = "transfer_id"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_EXPECTED_SIZE = "expected_size"
        const val EXTRA_NODE_ID = "node_id"

        // Wire format shared with the phone app (FileTransferService).
        private const val STATUS_PATH = "/file-transfer-status"
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_ERROR = "error"
        private const val STATUS_CANCELLED = "cancelled"

        private const val CANCEL_SIGNAL_GRACE_MS = 2_000L
        private const val CANCEL_POLL_INTERVAL_MS = 50L
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val activeTransfers = AtomicInteger(0)

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val transferId = intent?.getStringExtra(EXTRA_TRANSFER_ID)
        val fileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: getString(R.string.notification_receiving_title)
        val expectedSize = intent?.getLongExtra(EXTRA_EXPECTED_SIZE, -1L) ?: -1L
        val nodeId = intent?.getStringExtra(EXTRA_NODE_ID)
        Log.d(TAG, "onStartCommand: transferId=$transferId, fileName=$fileName, size=$expectedSize, nodeId=$nodeId")

        // A foreground service must show its notification right away.
        startForegroundSafely(fileName)

        if (transferId == null || nodeId == null) {
            Log.e(TAG, "Missing transfer data in start intent (transferId=$transferId, nodeId=$nodeId)")
            stopIfIdle()
            return START_NOT_STICKY
        }

        val channel = ChannelRegistry.take(transferId)
        if (channel == null) {
            Log.e(TAG, "No pending channel for transfer $transferId")
            // Let the phone fail fast instead of waiting for a confirmation.
            scope.launch {
                sendTransferStatus(nodeId, transferId, STATUS_ERROR)
                stopIfIdle()
            }
            return START_NOT_STICKY
        }

        activeTransfers.incrementAndGet()
        scope.launch {
            receiveFileFromChannel(channel, transferId, fileName, nodeId, expectedSize)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(fileName: String, progress: Int): Notification {
        val title = getString(R.string.notification_receiving_title)
        val contentText = if (progress >= 0) {
            getString(R.string.notification_receiving_desc, fileName, progress)
        } else {
            fileName
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun startForegroundSafely(fileName: String) {
        val notification = buildNotification(fileName, 0)
        try {
            Log.d(TAG, "Starting foreground service for $fileName")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }
    }

    private fun updateProgressNotification(fileName: String, progress: Int) {
        val notification = buildNotification(fileName, progress)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing notification permission: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification: ${e.message}", e)
        }
    }

    private fun stopIfIdle() {
        if (activeTransfers.get() > 0) return
        Log.d(TAG, "No active transfers. Stopping foreground service.")
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop foreground: ${e.message}")
        }
        stopSelf()
    }

    private suspend fun receiveFileFromChannel(
        channel: ChannelClient.Channel,
        transferId: String,
        fileName: String,
        nodeId: String,
        expectedSize: Long
    ) {
        Log.d(TAG, "receiveFileFromChannel starting for $fileName (transferId=$transferId) from node $nodeId")
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "WearFiles:FileReceiverWifiLock"
        ).apply {
            setReferenceCounted(false)
        }
        try {
            wifiLock.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wifi lock: ${e.message}")
        }

        val channelClient = Wearable.getChannelClient(this)
        val receivedDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Received")
        // Incomplete data goes to a unique ".part" file: an interrupted or cancelled transfer
        // can never look like a saved file, and same-named transfers don't clobber each other.
        val partFile = File(receivedDir, "$fileName.$transferId.part")

        try {
            if (!receivedDir.exists()) {
                val created = receivedDir.mkdirs()
                Log.d(TAG, "Directory ${receivedDir.absolutePath} exists=false, mkdirs result=$created")
            }

            Log.d(TAG, "Receiving file '$fileName' (transferId=$transferId) into ${partFile.absolutePath}")

            Log.d(TAG, "Opening channel InputStream...")
            val inputStream = try {
                channelClient.getInputStream(channel).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get InputStream from channel: ${e.message}", e)
                throw e
            }

            var bytesReceived = 0L
            var lastNotificationTime = 0L

            Log.d(TAG, "Beginning byte read loop for $fileName...")
            withContext(Dispatchers.IO) {
                FileOutputStream(partFile).use { outputStream ->
                    inputStream.use { input ->
                        val buffer = ByteArray(32768)
                        var bytes = input.read(buffer)
                        while (bytes != -1) {
                            outputStream.write(buffer, 0, bytes)
                            bytesReceived += bytes

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastNotificationTime > 500) {
                                val progress = if (expectedSize > 0) {
                                    (bytesReceived * 100 / expectedSize).toInt()
                                } else {
                                    -1
                                }
                                updateProgressNotification(fileName, progress)
                                lastNotificationTime = currentTime
                            }

                            bytes = input.read(buffer)
                        }
                        outputStream.flush()
                    }
                }
            }

            Log.d(TAG, "Finished writing file $fileName. Total bytes received: $bytesReceived (expected: $expectedSize)")

            if (ChannelRegistry.isCancelled(transferId)) {
                Log.d(TAG, "Transfer $transferId was cancelled by the sender, discarding ${partFile.name}")
                partFile.delete()
                sendTransferStatus(nodeId, transferId, STATUS_CANCELLED)
                return
            }

            if (expectedSize > 0 && bytesReceived != expectedSize) {
                Log.e(TAG, "Size mismatch for $fileName: received $bytesReceived bytes, expected $expectedSize bytes")
            }

            val targetFile = uniqueTargetFile(receivedDir, fileName)
            if (!partFile.renameTo(targetFile)) {
                throw IOException("Failed to move ${partFile.name} to ${targetFile.name}")
            }
            Log.d(TAG, "File '$fileName' saved successfully to ${targetFile.absolutePath}")

            sendTransferStatus(nodeId, transferId, STATUS_SUCCESS)

        } catch (e: CancellationException) {
            Log.w(TAG, "Receive cancelled for $fileName: ${e.message}", e)
            val deleted = partFile.delete()
            Log.w(TAG, "Deleted incomplete file ${partFile.absolutePath}, result=$deleted")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving file $fileName: ${e.message}", e)
            val deleted = partFile.delete()
            Log.w(TAG, "Deleted incomplete file ${partFile.absolutePath}, result=$deleted")

            // A cancelled transfer must not be reported as a failed one.
            val cancelled = waitForCancellation(transferId)
            sendTransferStatus(nodeId, transferId, if (cancelled) STATUS_CANCELLED else STATUS_ERROR)
        } finally {
            ChannelRegistry.clear(transferId)
            if (wifiLock.isHeld) {
                try {
                    wifiLock.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to release wifi lock: ${e.message}")
                }
            }

            try {
                Log.d(TAG, "Closing channel ${channel.path}...")
                channelClient.close(channel).await()
                Log.d(TAG, "Channel closed successfully")
            } catch (closeException: Exception) {
                Log.w(TAG, "Failed to close channel: ${closeException.message}")
            }

            val remaining = activeTransfers.decrementAndGet()
            Log.d(TAG, "Remaining active transfers: $remaining")
            if (remaining <= 0) {
                stopIfIdle()
            }
        }
    }

    private fun isTransferCancelled(transferId: String): Boolean = ChannelRegistry.isCancelled(transferId)

    private suspend fun waitForCancellation(transferId: String, timeoutMs: Long = CANCEL_SIGNAL_GRACE_MS): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isTransferCancelled(transferId)) return true
            delay(CANCEL_POLL_INTERVAL_MS.milliseconds)
        }
        return isTransferCancelled(transferId)
    }

    private fun uniqueTargetFile(dir: File, fileName: String): File {
        val candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate

        val extension = candidate.extension
        val baseName = if (extension.isEmpty()) candidate.name else candidate.name.removeSuffix(".$extension")
        var index = 1
        while (true) {
            val next = File(dir, if (extension.isEmpty()) "$baseName ($index)" else "$baseName ($index).$extension")
            if (!next.exists()) {
                Log.d(TAG, "${candidate.name} already exists, saving as ${next.name}")
                return next
            }
            index++
        }
    }

    private suspend fun sendTransferStatus(nodeId: String, transferId: String, status: String) {
        try {
            Wearable.getMessageClient(this)
                .sendMessage(nodeId, STATUS_PATH, "$status:$transferId".toByteArray())
                .await()
            Log.d(TAG, "Sent '$status' status to node $nodeId for transfer $transferId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send '$status' status to node $nodeId: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroying, cancelling supervisor job")
        job.cancel()
    }
}
