package com.dertefter.wearfiles

import android.util.Log
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.TransferStatus
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class TransferStatusListener : WearableListenerService() {

    companion object {
        private const val TAG = "TransferStatusListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}, sourceNodeId=${messageEvent.sourceNodeId}")
        if (messageEvent.path == "/file-transfer-status") {
            val message = String(messageEvent.data)
            Log.d(TAG, "Received file transfer status message: '$message' from ${messageEvent.sourceNodeId}")
            val parts = message.split(":", limit = 2)
            if (parts.size == 2) {
                val status = parts[0]
                val fileName = parts[1]
                val sourceNodeId = messageEvent.sourceNodeId
                
                val notificationHelper = NotificationHelper(this)
                if (status == "success") {
                    val item = TransferRepository.queue.find { it.fileName == fileName && it.targetNodeId == sourceNodeId && it.status == TransferStatus.SENDING }
                    if (item != null) {
                        Log.d(TAG, "Transfer success confirmation received for item id=${item.id}, fileName=$fileName")
                        TransferRepository.updateItem(item.id) { it.copy(status = TransferStatus.SUCCESS, progress = 100) }
                    } else {
                        Log.w(TAG, "Transfer success received for fileName=$fileName from node=$sourceNodeId, but no matching item sending in queue")
                    }
                    notificationHelper.showTransferNotification(fileName, TransferStatus.SUCCESS)
                } else {
                    val item = TransferRepository.queue.find { it.fileName == fileName && it.targetNodeId == sourceNodeId && it.status == TransferStatus.SENDING }
                    if (item != null) {
                        Log.e(TAG, "Transfer error reported from receiver for item id=${item.id}, fileName=$fileName")
                        TransferRepository.updateItem(item.id) { it.copy(status = TransferStatus.ERROR) }
                    } else {
                        Log.w(TAG, "Transfer error received for fileName=$fileName from node=$sourceNodeId, but no matching item sending in queue")
                    }
                    notificationHelper.showTransferNotification(fileName, TransferStatus.ERROR)
                }
            } else {
                Log.w(TAG, "Malformed transfer status message format: '$message'")
            }
        }
    }
}
