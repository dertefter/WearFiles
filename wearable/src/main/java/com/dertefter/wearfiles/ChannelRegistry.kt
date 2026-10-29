package com.dertefter.wearfiles

import com.google.android.gms.wearable.ChannelClient
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object ChannelRegistry {

    private val channels = ConcurrentHashMap<String, ChannelClient.Channel>()

    private val cancelledIds: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun put(transferId: String, channel: ChannelClient.Channel) {
        channels[transferId] = channel
    }

    fun take(transferId: String): ChannelClient.Channel? = channels.remove(transferId)

    fun markCancelled(transferId: String) {
        cancelledIds.add(transferId)
    }

    fun isCancelled(transferId: String): Boolean = cancelledIds.contains(transferId)

    fun clear(transferId: String) {
        channels.remove(transferId)
        cancelledIds.remove(transferId)
    }
}
