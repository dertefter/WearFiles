package com.dertefter.wearfiles

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.dertefter.wearfiles.data.ConnectionStatus
import com.dertefter.wearfiles.data.TransferRepository
import com.dertefter.wearfiles.data.WearNode
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearableFileSender(private val context: Context) {

    companion object {
        private const val TAG = "WearableFileSender"
    }

    suspend fun checkConnection() {
        try {
            Log.d(TAG, "checkConnection: Querying connected nodes and capabilities...")
            val nodeClient = Wearable.getNodeClient(context)
            val capabilityClient = Wearable.getCapabilityClient(context)

            val allNodes = nodeClient.connectedNodes.await()
            Log.d(TAG, "Found ${allNodes.size} connected node(s): ${allNodes.map { "${it.displayName} (${it.id})" }}")

            val capabilityInfo = capabilityClient
                .getCapability("wear_files_app", CapabilityClient.FILTER_ALL)
                .await()
            val reachableNodes = capabilityClient
                .getCapability("wear_files_app", CapabilityClient.FILTER_REACHABLE)
                .await().nodes

            val newNodes = allNodes.map { node ->
                val status = when {
                    !capabilityInfo.nodes.any { it.id == node.id } -> ConnectionStatus.APP_NOT_INSTALLED
                    !reachableNodes.any { it.id == node.id } -> ConnectionStatus.NOT_NEARBY
                    else -> ConnectionStatus.READY
                }
                Log.d(TAG, "Node ${node.displayName} (${node.id}) status: $status")
                WearNode(node.id, node.displayName, status)
            }

            TransferRepository.updateNodes(newNodes)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking node connections: ${e.message}", e)
            TransferRepository.updateNodes(emptyList())
        }
    }

    fun sendFileToWear(uri: Uri) {
        val nodeId = TransferRepository.selectedNodeId
        Log.d(TAG, "sendFileToWear called for uri=$uri, selectedNodeId=$nodeId")
        if (nodeId == null) {
            Log.w(TAG, "Cannot send file: selectedNodeId is null")
            return
        }
        val fileName = getFileName(uri) ?: context.getString(R.string.file_default_name)
        Log.d(TAG, "Resolved file name: '$fileName' for uri=$uri")
        val intent = Intent(context, FileTransferService::class.java).apply {
            action = "ADD_TRANSFER"
            putExtra("file_uri", uri)
            putExtra("file_name", fileName)
            putExtra("target_node_id", nodeId)
        }
        try {
            context.startForegroundService(intent)
            Log.d(TAG, "Started FileTransferService with ADD_TRANSFER for $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FileTransferService: ${e.message}", e)
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor.use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying file name from content resolver for $uri: ${e.message}")
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
