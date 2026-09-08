package com.quickcontactsheet

import com.quickcontactsheet.data.ContactPhoneNumber
import com.quickcontactsheet.data.WidgetSettings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSettingsTest {

    @Test
    fun `primaryPhoneNumber defaults to first number when none explicitly selected`() {
        val settings = WidgetSettings(
            widgetId = 1,
            displayName = "Carmen",
            phoneNumbers = listOf(
                ContactPhoneNumber(number = "349-555-0192", label = "Mobile"),
                ContactPhoneNumber(number = "212-555-0144", label = "Work"),
            ),
        )

        assertEquals("349-555-0192", settings.primaryPhoneNumber)
        assertEquals("Mobile", settings.primaryPhoneLabel)
    }

    @Test
    fun `primaryPhoneNumber uses selectedPhoneNumber when specified`() {
        val settings = WidgetSettings(
            widgetId = 1,
            displayName = "Carmen",
            phoneNumbers = listOf(
                ContactPhoneNumber(number = "212-555-0144", label = "Work"),
                ContactPhoneNumber(number = "349-555-0192", label = "Mobile"),
            ),
            selectedPhoneNumber = "349-555-0192",
        )

        assertEquals("349-555-0192", settings.primaryPhoneNumber)
        assertEquals("Mobile", settings.primaryPhoneLabel)
    }

    @Test
    fun `primaryPhoneNumber falls back to first number if selected is not in list`() {
        val settings = WidgetSettings(
            widgetId = 1,
            displayName = "Carmen",
            phoneNumbers = listOf(
                ContactPhoneNumber(number = "349-555-0192", label = "Mobile"),
            ),
            selectedPhoneNumber = "999-999-9999",
        )

        assertEquals("349-555-0192", settings.primaryPhoneNumber)
    }

    @Test
    fun `decodes legacy json with plain string phone numbers correctly`() {
        val legacyJson = JSONObject().apply {
            put("widgetId", 42)
            put("displayName", "Carmen")
            put("phoneNumbers", JSONArray().apply {
                put("349-555-0192")
                put("212-555-0144")
            })
        }.toString()

        val decoded = decodeWidgetSettingsForTest(legacyJson)
        assertEquals(42, decoded?.widgetId)
        assertEquals("Carmen", decoded?.displayName)
        assertEquals(2, decoded?.phoneNumbers?.size)
        assertEquals("349-555-0192", decoded?.phoneNumbers?.get(0)?.number)
        assertEquals("", decoded?.phoneNumbers?.get(0)?.label)
        assertEquals("349-555-0192", decoded?.primaryPhoneNumber)
    }

    @Test
    fun `decodes new json with labeled phone numbers and selectedPhoneNumber`() {
        val newJson = JSONObject().apply {
            put("widgetId", 42)
            put("displayName", "Carmen")
            put("selectedPhoneNumber", "349-555-0192")
            put("phoneNumbers", JSONArray().apply {
                put(JSONObject().put("number", "212-555-0144").put("label", "Work"))
                put(JSONObject().put("number", "349-555-0192").put("label", "Mobile"))
            })
        }.toString()

        val decoded = decodeWidgetSettingsForTest(newJson)
        assertEquals(42, decoded?.widgetId)
        assertEquals("349-555-0192", decoded?.selectedPhoneNumber)
        assertEquals("349-555-0192", decoded?.primaryPhoneNumber)
        assertEquals("Mobile", decoded?.primaryPhoneLabel)
    }

    private fun decodeWidgetSettingsForTest(raw: String): WidgetSettings? =
        runCatching {
            val json = JSONObject(raw)
            val phoneArray = json.optJSONArray("phoneNumbers") ?: JSONArray()
            WidgetSettings(
                widgetId = json.getInt("widgetId"),
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
            )
        }.getOrNull()
}
