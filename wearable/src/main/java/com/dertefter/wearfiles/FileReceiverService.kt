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
        private const val CHANNEL_ID = "file_receiver_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
            Log.e("FileReceiverService", "Failed to start foreground service: ${e.message}")
        }
    }

    private fun updateProgressNotification(fileName: String, progress: Int) {
        val notification = buildNotification(fileName, progress)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w("FileReceiverService", "Missing notification permission: ${e.message}")
        } catch (e: Exception) {
            Log.w("FileReceiverService", "Failed to update notification: ${e.message}")
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
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

            try {
                val intent = Intent(this, FileReceiverService::class.java)
                startForegroundService(intent)
            } catch (e: Exception) {
                Log.w("FileReceiverService", "Failed to start service intent: ${e.message}")
            }

            val currentActive = activeTransfers.incrementAndGet()
            if (currentActive == 1) {
                startForegroundServiceIfNeeded(fileName)
            } else {
                updateProgressNotification(fileName, 0)
            }

            scope.launch {
                receiveFileFromChannel(channel, fileName, nodeId, expectedSize)
            }
        }
    }

    private suspend fun receiveFileFromChannel(
        channel: ChannelClient.Channel,
        fileName: String,
        nodeId: String,
        expectedSize: Long
    ) {
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearFiles:FileReceiverWakeLock"
        )
        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.w("FileReceiverService", "Failed to acquire wake lock: ${e.message}")
        }

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
            var lastNotificationTime = 0L

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

        } catch (e: CancellationException) {
            Log.w("FileReceiverService", "Receive cancelled for $fileName", e)
            throw e
        } catch (e: Exception) {
            Log.e("FileReceiverService", "Error receiving file: " + e.stackTraceToString())
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
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                } catch (e: Exception) {
                    Log.w("FileReceiverService", "Failed to release wake lock: ${e.message}")
                }
            }

            try {
                channelClient.close(channel).await()
            } catch (closeException: Exception) {
                Log.w("FileReceiverService", "Failed to close channel: ${closeException.message}")
            }

            if (activeTransfers.decrementAndGet() <= 0) {
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (e: Exception) {
                    Log.w("FileReceiverService", "Failed to stop foreground: ${e.message}")
                }
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}