package com.dertefter.wearfiles

import android.util.Log
import com.dertefter.wearfiles.data.TransferAck
import com.dertefter.wearfiles.data.TransferAckRegistry
import com.dertefter.wearfiles.data.TransferProtocol
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class TransferStatusListener : WearableListenerService() {

    companion object {
        private const val TAG = "TransferStatusListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}, sourceNodeId=${messageEvent.sourceNodeId}")
        if (messageEvent.path != TransferProtocol.STATUS_PATH) return

        val message = String(messageEvent.data)
        Log.d(TAG, "Received file transfer status message: '$message' from ${messageEvent.sourceNodeId}")

        // Payload is "<status>:<transferId>": the id keeps transfers of equally named files apart.
        val parts = message.split(":", limit = 2)
        if (parts.size != 2) {
            Log.w(TAG, "Malformed transfer status message format: '$message'")
            return
        }

        val ack = TransferAck.fromProtocolValue(parts[0])
        if (ack == null) {
            Log.w(TAG, "Unknown transfer status '${parts[0]}'")
            return
        }

        val transferId = parts[1]
        if (TransferAckRegistry.complete(transferId, ack)) {
            Log.d(TAG, "Delivered status '$ack' for transfer id=$transferId")
        } else {
            Log.w(TAG, "No transfer waiting for status '$ack' (id=$transferId)")
        }
    }
}
