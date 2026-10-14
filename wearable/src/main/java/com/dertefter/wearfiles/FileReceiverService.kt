package com.dertefter.wearfiles

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class FileReceiverService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val activeTransfers = AtomicInteger(0)

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val powerManager by lazy {
        getSystemService(POWER_SERVICE) as PowerManager
    }

    companion object {
        private const val TAG = "FileReceiverService"
        private const val CHANNEL_ID = "file_receiver_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand called with flags=$flags, startId=$startId")
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

    private fun startForegroundServiceIfNeeded(fileName: String) {
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

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        Log.d(TAG, "onChannelOpened: path=${channel.path}, nodeId=${channel.nodeId}")
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
            Log.d(TAG, "Parsed transfer info: fileName='$fileName', expectedSize=$expectedSize bytes, senderNodeId=$nodeId")

            try {
                val intent = Intent(this, FileReceiverService::class.java)
                startForegroundService(intent)
                Log.d(TAG, "Started FileReceiverService intent for foreground service")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start service intent: ${e.message}", e)
            }

            val currentActive = activeTransfers.incrementAndGet()
            Log.d(TAG, "Active transfers count: $currentActive")
            if (currentActive == 1) {
                startForegroundServiceIfNeeded(fileName)
            } else {
                updateProgressNotification(fileName, 0)
            }

            scope.launch {
                receiveFileFromChannel(channel, fileName, nodeId, expectedSize)
            }
        } else {
            Log.w(TAG, "Ignored channel opening with unsupported path: ${channel.path}")
        }
    }

    private suspend fun receiveFileFromChannel(
        channel: ChannelClient.Channel,
        fileName: String,
        nodeId: String,
        expectedSize: Long
    ) {
        Log.d(TAG, "receiveFileFromChannel starting for $fileName from node $nodeId")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearFiles:FileReceiverWakeLock"
        )
        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            Log.d(TAG, "Wake lock acquired for receiving $fileName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}", e)
        }

        val channelClient = Wearable.getChannelClient(this)
        val receivedDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Received")

        try {
            if (!receivedDir.exists()) {
                val created = receivedDir.mkdirs()
                Log.d(TAG, "Directory ${receivedDir.absolutePath} exists=false, mkdirs result=$created")
            }

            val file = File(receivedDir, fileName)
            Log.d(TAG, "Receiving file '$fileName' -> destination path: ${file.absolutePath}")

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
                FileOutputStream(file).use { outputStream ->
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

            if (expectedSize > 0 && bytesReceived != expectedSize) {
                Log.e(TAG, "Size mismatch for $fileName: received $bytesReceived bytes, expected $expectedSize bytes")
            }

            Log.d(TAG, "File '$fileName' saved successfully to ${file.absolutePath}")

            try {
                Log.d(TAG, "Sending success status back to node $nodeId for file $fileName...")
                Wearable.getMessageClient(this)
                    .sendMessage(nodeId, "/file-transfer-status", "success:$fileName".toByteArray())
                    .await()
                Log.d(TAG, "Success status message sent successfully to node $nodeId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send success status to node $nodeId: ${e.message}", e)
            }

        } catch (e: CancellationException) {
            Log.w(TAG, "Receive cancelled for $fileName: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving file $fileName: ${e.message}", e)
            val file = File(receivedDir, fileName)
            if (file.exists()) {
                val deleted = file.delete()
                Log.w(TAG, "Deleted incomplete file ${file.absolutePath}, result=$deleted")
            }

            try {
                Log.d(TAG, "Sending error status back to node $nodeId for file $fileName...")
                Wearable.getMessageClient(this)
                    .sendMessage(nodeId, "/file-transfer-status", "error:$fileName".toByteArray())
                    .await()
                Log.d(TAG, "Error status message sent successfully to node $nodeId")
            } catch (sendException: Exception) {
                Log.w(TAG, "Failed to send error status to node $nodeId: ${sendException.message}", sendException)
            }
        } finally {
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.d(TAG, "Wake lock released")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to release wake lock: ${e.message}")
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
                Log.d(TAG, "No more active transfers. Stopping foreground service.")
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stop foreground: ${e.message}")
                }
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroying, cancelling supervisor job")
        job.cancel()
    }
}