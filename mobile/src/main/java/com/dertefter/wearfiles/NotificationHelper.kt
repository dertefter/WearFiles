package com.dertefter.wearfiles

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dertefter.wearfiles.data.TransferStatus

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "file_transfer_channel"
        private const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_desc)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showTransferNotification(fileName: String, status: TransferStatus, progress: Int = 0) {
        val title = when (status) {
            TransferStatus.SENDING -> context.getString(R.string.notification_sending_title)
            TransferStatus.SUCCESS -> context.getString(R.string.notification_success_title)
            TransferStatus.ERROR -> context.getString(R.string.notification_error_title)
            else -> return
        }
        val text = when (status) {
            TransferStatus.SENDING -> context.getString(R.string.notification_sending_desc, fileName, progress)
            TransferStatus.SUCCESS -> context.getString(R.string.notification_success_desc, fileName)
            TransferStatus.ERROR -> context.getString(R.string.notification_error_desc, fileName)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (status == TransferStatus.SENDING) android.R.drawable.stat_sys_upload else android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setOngoing(status == TransferStatus.SENDING)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)

        if (status == TransferStatus.SENDING) {
            builder.setProgress(100, progress, false)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun getNotification(fileName: String, status: TransferStatus, progress: Int = 0): android.app.Notification {
        val title = when (status) {
            TransferStatus.SENDING -> context.getString(R.string.notification_sending_title)
            TransferStatus.SUCCESS -> context.getString(R.string.notification_success_title)
            TransferStatus.ERROR -> context.getString(R.string.notification_error_title)
            else -> context.getString(R.string.notification_preparing)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, status == TransferStatus.PENDING)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .build()
    }
}
