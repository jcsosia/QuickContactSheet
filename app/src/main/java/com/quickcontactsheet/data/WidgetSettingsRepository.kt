package com.quickcontactsheet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.provider.ContactsContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val DATASTORE_NAME = "quick_contact_sheet"
private const val WIDGET_KEY_PREFIX = "widget_"

private val Context.widgetDataStore by preferencesDataStore(name = DATASTORE_NAME)

class WidgetSettingsRepository private constructor(
    private val context: Context,
) {
    val allSettingsFlow: Flow<List<WidgetSettings>> =
        context.widgetDataStore.data.map { preferences ->
            preferences.asMap()
                .mapNotNull { (key, value) ->
                    if (key.name.startsWith(WIDGET_KEY_PREFIX) && value is String) {
                        decodeWidgetSettings(value)
                    } else {
                        null
                    }
                }
                .sortedBy { it.displayName.lowercase() }
        }

    fun widgetSettingsFlow(widgetId: Int): Flow<WidgetSettings?> =
        context.widgetDataStore.data.map { preferences ->
            preferences[widgetKey(widgetId)]?.let(::decodeWidgetSettings)
        }

    suspend fun getWidgetSettings(widgetId: Int): WidgetSettings? {
        val settings = widgetSettingsFlow(widgetId).first() ?: return null
        val currentPhoto = settings.photoUri
        // Auto-migrate legacy content:// URI to local file if not yet saved locally
        if (!currentPhoto.isNullOrBlank() && !java.io.File(currentPhoto).exists()) {
            val photoBitmap = context.loadContactPhoto(
                contactId = settings.contactId,
                lookupUriString = settings.contactLookupUri,
                photoUriString = currentPhoto,
                maxDimensionPx = 768,
            )
            if (photoBitmap != null) {
                val savedPath = context.saveWidgetPhoto(widgetId, photoBitmap)
                val migrated = settings.copy(photoUri = savedPath)
                saveWidgetSettings(migrated)
                return migrated
            }
        }
        return settings
    }

    suspend fun saveSelectedContact(
        widgetId: Int,
        contact: ContactSummary,
    ) {
        val current = getWidgetSettings(widgetId)
        val lookupUri = buildLookupUri(contact)
        val photoBitmap = context.loadContactPhoto(
            contactId = contact.contactId,
            lookupUriString = lookupUri,
            photoUriString = contact.photoUri,
            maxDimensionPx = 768,
        )
        val savedPhotoPath = if (photoBitmap != null) {
            context.saveWidgetPhoto(widgetId, photoBitmap)
        } else {
            contact.photoUri
        }

        val updated = WidgetSettings(
            widgetId = widgetId,
            contactId = contact.contactId,
            contactLookupKey = contact.lookupKey,
            contactLookupUri = lookupUri,
            displayName = contact.displayName,
            phoneNumbers = contact.phoneNumbers.filterNot { it.isBlank() }.distinct(),
            photoUri = savedPhotoPath,
            messages = current?.messages ?: emptyList(),
        )
        saveWidgetSettings(updated)
    }

    suspend fun saveMessages(
        widgetId: Int,
        messages: List<WidgetMessage>,
    ) {
        val current = getWidgetSettings(widgetId) ?: WidgetSettings(widgetId = widgetId)
        saveWidgetSettings(
            current.copy(
                messages = messages.filter { it.text.isNotBlank() },
            ),
        )
    }

    suspend fun saveWidgetSettings(settings: WidgetSettings) {
        context.widgetDataStore.edit { preferences ->
            preferences[widgetKey(settings.widgetId)] = encodeWidgetSettings(settings)
        }
    }

    suspend fun deleteWidget(widgetId: Int) {
        context.deleteWidgetPhoto(widgetId)
        context.widgetDataStore.edit { preferences ->
            preferences.remove(widgetKey(widgetId))
        }
    }

    private fun widgetKey(widgetId: Int): Preferences.Key<String> =
        stringPreferencesKey("$WIDGET_KEY_PREFIX$widgetId")

    private fun encodeWidgetSettings(settings: WidgetSettings): String {
        val json = JSONObject()
            .put("widgetId", settings.widgetId)
            .put("contactId", settings.contactId)
            .put("contactLookupKey", settings.contactLookupKey)
            .put("contactLookupUri", settings.contactLookupUri)
            .put("displayName", settings.displayName)
            .put("photoUri", settings.photoUri)
            .put(
                "phoneNumbers",
                JSONArray(settings.phoneNumbers),
            )
            .put(
                "messages",
                JSONArray().apply {
                    settings.messages.forEach { message ->
                        put(
                            JSONObject()
                                .put("id", message.id)
                                .put("text", message.text),
                        )
                    }
                },
            )
        return json.toString()
    }

    private fun decodeWidgetSettings(raw: String): WidgetSettings? =
        runCatching {
            val json = JSONObject(raw)
            val messageArray = json.optJSONArray("messages") ?: JSONArray()
            val phoneArray = json.optJSONArray("phoneNumbers") ?: JSONArray()
            WidgetSettings(
                widgetId = json.getInt("widgetId"),
                contactId = json.takeIf { !it.isNull("contactId") }?.optLong("contactId"),
                contactLookupKey = json.optString("contactLookupKey").ifBlank { null },
                contactLookupUri = json.optString("contactLookupUri").ifBlank { null },
                displayName = json.optString("displayName"),
                phoneNumbers = buildList {
                    for (index in 0 until phoneArray.length()) {
                        val number = phoneArray.optString(index)
                        if (number.isNotBlank()) {
                            add(number)
                        }
                    }
                },
                photoUri = json.optString("photoUri").ifBlank { null },
                messages = buildList {
                    for (index in 0 until messageArray.length()) {
                        val messageObject = messageArray.optJSONObject(index) ?: continue
                        val text = messageObject.optString("text")
                        if (text.isNotBlank()) {
                            add(
                                WidgetMessage(
                                    id = messageObject.optLong("id"),
                                    text = text,
                                ),
                            )
                        }
                    }
                },
            )
        }.getOrNull()

    private fun buildLookupUri(contact: ContactSummary): String =
        ContactsContract.Contacts.getLookupUri(contact.contactId, contact.lookupKey).toString()

    companion object {
        @Volatile
        private var instance: WidgetSettingsRepository? = null

        fun get(context: Context): WidgetSettingsRepository =
            instance ?: synchronized(this) {
                instance ?: WidgetSettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
