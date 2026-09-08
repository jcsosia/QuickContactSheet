package com.quickcontactsheet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val DATASTORE_NAME = "quick_contact_sheet"
private const val WIDGET_KEY_PREFIX = "widget_"
private const val CONTACT_PRESETS_KEY = "all_contact_presets"
private val PRESETS_PREFERENCE_KEY = stringPreferencesKey(CONTACT_PRESETS_KEY)

private val Context.widgetDataStore by preferencesDataStore(name = DATASTORE_NAME)

class WidgetSettingsRepository private constructor(
    private val context: Context,
) {
    val allPresetsFlow: Flow<List<ContactPreset>> =
        context.widgetDataStore.data.map { preferences ->
            preferences[PRESETS_PREFERENCE_KEY]?.let(::decodePresetsList) ?: emptyList()
        }

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
        selectedPhoneNumber: String? = null,
    ) = withContext(Dispatchers.IO) {
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
            context.deleteWidgetPhoto(widgetId)
            contact.photoUri
        }

        val chosenNumber = selectedPhoneNumber
            ?: contact.phoneNumbers.firstOrNull()?.number

        val savedPreset = if (current?.messages.isNullOrEmpty()) {
            getPresetForContact(contact.lookupKey, contact.displayName, contact.phoneNumbers)
        } else null

        val updated = WidgetSettings(
            widgetId = widgetId,
            contactId = contact.contactId,
            contactLookupKey = contact.lookupKey,
            contactLookupUri = lookupUri,
            displayName = contact.displayName,
            phoneNumbers = contact.phoneNumbers,
            selectedPhoneNumber = chosenNumber,
            photoUri = savedPhotoPath,
            messages = current?.messages?.takeIf { it.isNotEmpty() } ?: savedPreset?.messages.orEmpty(),
        )
        saveWidgetSettings(updated)
    }

    suspend fun saveSelectedPhoneNumber(
        widgetId: Int,
        phoneNumber: String,
    ) = withContext(Dispatchers.IO) {
        val current = getWidgetSettings(widgetId) ?: return@withContext
        saveWidgetSettings(current.copy(selectedPhoneNumber = phoneNumber))
    }

    suspend fun saveMessages(
        widgetId: Int,
        messages: List<WidgetMessage>,
    ) = withContext(Dispatchers.IO) {
        val current = getWidgetSettings(widgetId) ?: WidgetSettings(widgetId = widgetId)
        val validMessages = messages.filter { it.text.isNotBlank() }
        val updated = current.copy(
            messages = validMessages,
        )
        saveWidgetSettings(updated)
        if (updated.displayName.isNotBlank() || updated.phoneNumbers.isNotEmpty()) {
            saveContactPresetFromSettings(updated)
        }
    }


    suspend fun saveCustomPhoto(
        widgetId: Int,
        imageUri: Uri,
    ): Boolean = withContext(Dispatchers.IO) {
        val current = getWidgetSettings(widgetId) ?: return@withContext false
        val photoBitmap = context.loadBitmapFromImageUri(imageUri, maxDimensionPx = 768) ?: return@withContext false
        val savedPhotoPath = context.saveWidgetPhoto(widgetId, photoBitmap)
        saveWidgetSettings(current.copy(photoUri = savedPhotoPath))
        true
    }

    suspend fun resetToContactPhoto(
        widgetId: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        val current = getWidgetSettings(widgetId) ?: return@withContext false
        val lookupUri = current.contactLookupUri
        val photoBitmap = context.loadContactPhoto(
            contactId = current.contactId,
            lookupUriString = lookupUri,
            photoUriString = null,
            maxDimensionPx = 768,
        )
        val savedPhotoPath = if (photoBitmap != null) {
            context.saveWidgetPhoto(widgetId, photoBitmap)
        } else {
            context.deleteWidgetPhoto(widgetId)
            null
        }
        saveWidgetSettings(current.copy(photoUri = savedPhotoPath))
        true
    }

    suspend fun removePhoto(
        widgetId: Int,
    ) = withContext(Dispatchers.IO) {
        val current = getWidgetSettings(widgetId) ?: return@withContext
        context.deleteWidgetPhoto(widgetId)
        saveWidgetSettings(current.copy(photoUri = null))
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
            .put("selectedPhoneNumber", settings.selectedPhoneNumber)
            .put(
                "phoneNumbers",
                JSONArray().apply {
                    settings.phoneNumbers.forEach { phone ->
                        put(
                            JSONObject()
                                .put("number", phone.number)
                                .put("label", phone.label),
                        )
                    }
                },
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
                selectedPhoneNumber = json.optString("selectedPhoneNumber").ifBlank { null },
                phoneNumbers = buildList {
                    for (index in 0 until phoneArray.length()) {
                        val optObj = phoneArray.optJSONObject(index)
                        if (optObj != null) {
                            val number = optObj.optString("number")
                            val label = optObj.optString("label")
                            if (number.isNotBlank()) {
                                add(ContactPhoneNumber(number = number, label = label))
                            }
                        } else {
                            val number = phoneArray.optString(index)
                            if (number.isNotBlank()) {
                                add(ContactPhoneNumber(number = number, label = ""))
                            }
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

    suspend fun getAllPresets(): List<ContactPreset> =
        allPresetsFlow.first()

    suspend fun getPresetForContact(
        lookupKey: String?,
        displayName: String,
        phoneNumbers: List<ContactPhoneNumber>,
    ): ContactPreset? = withContext(Dispatchers.IO) {
        val presets = getAllPresets()
        presets.firstOrNull { it.matchesContact(lookupKey, displayName, phoneNumbers) }
    }

    suspend fun saveContactPreset(preset: ContactPreset) = withContext(Dispatchers.IO) {
        val currentPresets = getAllPresets().toMutableList()
        val index = currentPresets.indexOfFirst {
            it.matchesContact(preset.contactLookupKey, preset.displayName, preset.phoneNumbers)
        }
        if (index >= 0) {
            currentPresets[index] = preset
        } else {
            currentPresets.add(preset)
        }
        saveAllPresets(currentPresets)
    }

    suspend fun saveContactPresetFromSettings(settings: WidgetSettings) = withContext(Dispatchers.IO) {
        if (settings.displayName.isBlank() && settings.phoneNumbers.isEmpty()) return@withContext
        val preset = ContactPreset(
            contactLookupKey = settings.contactLookupKey,
            displayName = settings.displayName,
            phoneNumbers = settings.phoneNumbers,
            messages = settings.messages,
            lastUpdated = System.currentTimeMillis(),
        )
        saveContactPreset(preset)
    }

    suspend fun restorePresets(newPresets: List<ContactPreset>): Int = withContext(Dispatchers.IO) {
        if (newPresets.isEmpty()) return@withContext 0
        val currentPresets = getAllPresets().toMutableList()
        var updatedCount = 0
        newPresets.forEach { newPreset ->
            val index = currentPresets.indexOfFirst {
                it.matchesContact(newPreset.contactLookupKey, newPreset.displayName, newPreset.phoneNumbers)
            }
            if (index >= 0) {
                currentPresets[index] = newPreset
            } else {
                currentPresets.add(newPreset)
            }
            updatedCount++
        }
        saveAllPresets(currentPresets)

        // Sync with active widgets if they currently have no messages or match
        val allWidgets = allSettingsFlow.first()
        allWidgets.forEach { widget ->
            if (widget.messages.isEmpty()) {
                val matchingPreset = newPresets.firstOrNull {
                    it.matchesContact(widget.contactLookupKey, widget.displayName, widget.phoneNumbers)
                }
                if (matchingPreset != null && matchingPreset.messages.isNotEmpty()) {
                    saveWidgetSettings(widget.copy(messages = matchingPreset.messages))
                }
            }
        }
        updatedCount
    }

    suspend fun deletePreset(preset: ContactPreset) = withContext(Dispatchers.IO) {
        val currentPresets = getAllPresets().toMutableList()
        currentPresets.removeAll {
            it.matchesContact(preset.contactLookupKey, preset.displayName, preset.phoneNumbers)
        }
        saveAllPresets(currentPresets)
    }

    suspend fun clearAllPresets() = withContext(Dispatchers.IO) {
        context.widgetDataStore.edit { preferences ->
            preferences.remove(PRESETS_PREFERENCE_KEY)
        }
    }

    private suspend fun saveAllPresets(presets: List<ContactPreset>) {
        context.widgetDataStore.edit { preferences ->
            preferences[PRESETS_PREFERENCE_KEY] = encodePresetsList(presets)
        }
    }


    private fun encodePresetsList(presets: List<ContactPreset>): String {
        val array = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject()
                .put("displayName", preset.displayName)
                .put("contactLookupKey", preset.contactLookupKey)
                .put("lastUpdated", preset.lastUpdated)
                .put(
                    "phoneNumbers",
                    JSONArray().apply {
                        preset.phoneNumbers.forEach { phone ->
                            put(
                                JSONObject()
                                    .put("number", phone.number)
                                    .put("label", phone.label),
                            )
                        }
                    },
                )
                .put(
                    "messages",
                    JSONArray().apply {
                        preset.messages.forEach { message ->
                            put(
                                JSONObject()
                                    .put("id", message.id)
                                    .put("text", message.text),
                            )
                        }
                    },
                )
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodePresetsList(raw: String): List<ContactPreset> =
        runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val displayName = obj.optString("displayName")
                    val lookupKey = obj.optString("contactLookupKey").ifBlank { null }
                    val lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())

                    val phoneArray = obj.optJSONArray("phoneNumbers") ?: JSONArray()
                    val phoneNumbers = buildList {
                        for (p in 0 until phoneArray.length()) {
                            val phoneObj = phoneArray.optJSONObject(p)
                            if (phoneObj != null) {
                                val num = phoneObj.optString("number")
                                val lbl = phoneObj.optString("label")
                                if (num.isNotBlank()) add(ContactPhoneNumber(number = num, label = lbl))
                            }
                        }
                    }

                    val messageArray = obj.optJSONArray("messages") ?: JSONArray()
                    val messages = buildList {
                        for (m in 0 until messageArray.length()) {
                            val msgObj = messageArray.optJSONObject(m)
                            if (msgObj != null) {
                                val text = msgObj.optString("text")
                                val id = msgObj.optLong("id")
                                if (text.isNotBlank()) add(WidgetMessage(id = id, text = text))
                            }
                        }
                    }

                    add(
                        ContactPreset(
                            contactLookupKey = lookupKey,
                            displayName = displayName,
                            phoneNumbers = phoneNumbers,
                            messages = messages,
                            lastUpdated = lastUpdated,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())

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
