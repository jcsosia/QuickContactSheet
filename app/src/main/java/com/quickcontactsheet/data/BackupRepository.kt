package com.quickcontactsheet.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupRepository(
    private val context: Context? = null,
) {

    fun encodeBackup(presets: List<ContactPreset>): String {
        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put(
                "presets",
                JSONArray().apply {
                    presets.forEach { preset ->
                        put(
                            JSONObject().apply {
                                put("displayName", preset.displayName)
                                put("contactLookupKey", preset.contactLookupKey)
                                put("lastUpdated", preset.lastUpdated)
                                put(
                                    "phoneNumbers",
                                    JSONArray().apply {
                                        preset.phoneNumbers.forEach { phone ->
                                            put(
                                                JSONObject().apply {
                                                    put("number", phone.number)
                                                    put("label", phone.label)
                                                },
                                            )
                                        }
                                    },
                                )
                                put(
                                    "messages",
                                    JSONArray().apply {
                                        preset.messages.forEach { message ->
                                            put(
                                                JSONObject().apply {
                                                    put("id", message.id)
                                                    put("text", message.text)
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
        return root.toString(2)
    }

    fun decodeBackup(jsonString: String): Result<BackupData> = runCatching {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val presetsArray = root.optJSONArray("presets")

        if (presetsArray != null) {
            val presets = buildList {
                for (i in 0 until presetsArray.length()) {
                    val obj = presetsArray.optJSONObject(i) ?: continue
                    val displayName = obj.optString("displayName").trim()
                    val lookupKey = obj.optString("contactLookupKey").ifBlank { null }
                    val lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())

                    val phoneArray = obj.optJSONArray("phoneNumbers") ?: JSONArray()
                    val phoneNumbers = buildList {
                        for (p in 0 until phoneArray.length()) {
                            val phoneObj = phoneArray.optJSONObject(p)
                            if (phoneObj != null) {
                                val number = phoneObj.optString("number")
                                val label = phoneObj.optString("label")
                                if (number.isNotBlank()) {
                                    add(ContactPhoneNumber(number = number, label = label))
                                }
                            } else {
                                val number = phoneArray.optString(p)
                                if (number.isNotBlank()) {
                                    add(ContactPhoneNumber(number = number, label = ""))
                                }
                            }
                        }
                    }

                    val messageArray = obj.optJSONArray("messages") ?: JSONArray()
                    val messages = parseMessagesArray(messageArray)

                    if (displayName.isNotBlank() || phoneNumbers.isNotEmpty() || messages.isNotEmpty()) {
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
            }
            BackupData(version = version, exportedAt = exportedAt, presets = presets)
        } else if (root.has("messages")) {
            // Single contact export format: { "displayName": "Mom", "messages": [...] }
            val displayName = root.optString("displayName").trim()
            val messageArray = root.optJSONArray("messages") ?: JSONArray()
            val messages = parseMessagesArray(messageArray)
            val preset = ContactPreset(
                displayName = displayName.ifBlank { "Imported Contact" },
                messages = messages,
            )
            BackupData(version = version, exportedAt = exportedAt, presets = listOf(preset))
        } else {
            throw IllegalArgumentException("Unrecognized backup format: missing 'presets' or 'messages'")
        }
    }

    private fun parseMessagesArray(messageArray: JSONArray): List<WidgetMessage> = buildList {
        var nextId = System.currentTimeMillis()
        for (m in 0 until messageArray.length()) {
            val msgObj = messageArray.optJSONObject(m)
            if (msgObj != null) {
                val text = msgObj.optString("text").trim()
                val id = msgObj.optLong("id", nextId++)
                if (text.isNotBlank()) {
                    add(WidgetMessage(id = id, text = text))
                }
            } else {
                val text = messageArray.optString(m).trim()
                if (text.isNotBlank()) {
                    add(WidgetMessage(id = nextId++, text = text))
                }
            }
        }
    }

    suspend fun exportToUri(uri: Uri, presets: List<ContactPreset>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = requireNotNull(context) { "Context is required for I/O operations" }
                val json = encodeBackup(presets)
                ctx.contentResolver.openOutputStream(uri)?.use { output ->
                    OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                        writer.write(json)
                    }
                } ?: throw IllegalStateException("Unable to open output stream for $uri")
                presets.size
            }
        }

    suspend fun importFromUri(uri: Uri): Result<BackupData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = requireNotNull(context) { "Context is required for I/O operations" }
                val content = ctx.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                } ?: throw IllegalStateException("Unable to open input stream for $uri")
                decodeBackup(content).getOrThrow()
            }
        }


    companion object {
        const val BACKUP_VERSION = 1

        @Volatile
        private var instance: BackupRepository? = null

        fun get(context: Context): BackupRepository =
            instance ?: synchronized(this) {
                instance ?: BackupRepository(context.applicationContext).also { instance = it }
            }
    }
}
