package com.dertefter.wearfiles.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dertefter.wearable.data.model.PinnedItem
import com.dertefter.wearable.data.repository.PinnedRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PinnedRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PinnedRepository {

    val Context.settingsDataStore by preferencesDataStore(
        name = "pinned"
    )

    object PinnedKeys {
        val PINNED_ITEMS = stringPreferencesKey("pinned_items")
    }

    private val dataStore = context.settingsDataStore
    private val gson = Gson()

    override suspend fun getPinnedItems(): List<PinnedItem> {
        val prefs = dataStore.data.first()
        return decode(prefs[PinnedKeys.PINNED_ITEMS])
    }

    override fun getPinnedFlow(): Flow<List<PinnedItem>> {
        return dataStore.data.map { prefs ->
            decode(prefs[PinnedKeys.PINNED_ITEMS])
        }
    }

    override suspend fun isPinned(path: String): Boolean {
        return getPinnedItems().any { ((it.path as Any?) as? String) == path }
    }

    override suspend fun pinItem(item: PinnedItem) {
        val itemPath = (item.path as Any?) as? String
        if (itemPath.isNullOrEmpty()) return

        dataStore.edit { prefs ->
            val current = decode(prefs[PinnedKeys.PINNED_ITEMS]).toMutableList()

            if (current.none { ((it.path as Any?) as? String) == itemPath }) {
                current.add(item)
                prefs[PinnedKeys.PINNED_ITEMS] = encode(current)
            }
        }
    }

    override suspend fun unpinItem(item: PinnedItem) {
        val itemPath = (item.path as Any?) as? String
        if (itemPath.isNullOrEmpty()) return

        dataStore.edit { prefs ->
            val current = decode(prefs[PinnedKeys.PINNED_ITEMS])
                .filterNot { ((it.path as Any?) as? String) == itemPath }

            prefs[PinnedKeys.PINNED_ITEMS] = encode(current)
        }
    }

    private fun encode(list: List<PinnedItem>): String =
        gson.toJson(list)

    private fun decode(json: String?): List<PinnedItem> {
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<PinnedItem?>>() {}.type
            val items: List<PinnedItem?>? = gson.fromJson(json, type)
            items?.filterNotNull()?.filter { item ->
                val itemPath = (item.path as Any?) as? String
                val itemName = (item.name as Any?) as? String
                return@filter !itemPath.isNullOrEmpty() && !itemName.isNullOrEmpty()
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
