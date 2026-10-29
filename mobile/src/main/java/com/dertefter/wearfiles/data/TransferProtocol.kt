package com.dertefter.wearfiles.data

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

object TransferProtocol {

    const val CHANNEL_PATH_PREFIX = "/file-transfer"

    const val STATUS_PATH = "/file-transfer-status"

    const val CLOSE_CODE_CANCELLED = 1001
}

enum class TransferAck {
    SUCCESS,
    ERROR,
    CANCELLED;

    companion object {
        fun fromProtocolValue(value: String): TransferAck? = when (value) {
            "success" -> SUCCESS
            "error" -> ERROR
            "cancelled" -> CANCELLED
            else -> null
        }
    }
}


object TransferAckRegistry {

    private val pending = ConcurrentHashMap<String, CompletableDeferred<TransferAck>>()

    fun register(transferId: String): CompletableDeferred<TransferAck> =
        CompletableDeferred<TransferAck>().also { pending[transferId] = it }

    fun unregister(transferId: String) {
        pending.remove(transferId)
    }
    fun complete(transferId: String, ack: TransferAck): Boolean {
        val deferred = pending.remove(transferId) ?: return false
        return deferred.complete(ack)
    }
}
