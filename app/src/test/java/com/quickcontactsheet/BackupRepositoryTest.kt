package com.quickcontactsheet

import com.quickcontactsheet.data.BackupData
import com.quickcontactsheet.data.BackupRepository
import com.quickcontactsheet.data.ContactPhoneNumber
import com.quickcontactsheet.data.ContactPreset
import com.quickcontactsheet.data.WidgetMessage
import com.quickcontactsheet.data.normalizePhoneNumber
import com.quickcontactsheet.data.phoneNumbersMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    private val backupRepo = BackupRepository()


    @Test
    fun `normalizePhoneNumber removes symbols except digits and plus`() {
        assertEquals("+15551234567", normalizePhoneNumber("+1 (555) 123-4567"))
        assertEquals("5551234567", normalizePhoneNumber("(555) 123-4567"))
        assertEquals("12345", normalizePhoneNumber("123-45"))
    }

    @Test
    fun `phoneNumbersMatch compares numbers correctly across formats`() {
        assertTrue(phoneNumbersMatch("+1 555-123-4567", "(555) 123-4567"))
        assertTrue(phoneNumbersMatch("5551234567", "555-123-4567"))
        assertTrue(phoneNumbersMatch("+15551234567", "5551234567"))
        assertFalse(phoneNumbersMatch("555-123-4567", "555-999-9999"))
        assertFalse(phoneNumbersMatch("", "555-123-4567"))
    }

    @Test
    fun `matchesContact matches by phone number even if name differs slightly`() {
        val preset = ContactPreset(
            displayName = "Mom",
            phoneNumbers = listOf(ContactPhoneNumber(number = "555-123-4567", label = "Mobile")),
            messages = listOf(WidgetMessage(1, "Love you")),
        )

        assertTrue(
            preset.matchesContact(
                targetLookupKey = null,
                targetDisplayName = "Mother",
                targetPhoneNumbers = listOf(ContactPhoneNumber(number = "(555) 123-4567", label = "Cell")),
            )
        )
    }

    @Test
    fun `matchesContact matches by name if phone numbers unavailable`() {
        val preset = ContactPreset(
            displayName = "Dr. Smith",
            phoneNumbers = emptyList(),
            messages = listOf(WidgetMessage(1, "Confirming appointment")),
        )

        assertTrue(
            preset.matchesContact(
                targetLookupKey = null,
                targetDisplayName = "dr. smith ",
                targetPhoneNumbers = emptyList(),
            )
        )
        assertFalse(
            preset.matchesContact(
                targetLookupKey = null,
                targetDisplayName = "Dr. Jones",
                targetPhoneNumbers = emptyList(),
            )
        )
    }

    @Test
    fun `encodeBackup and decodeBackup preserves all data`() {
        val presets = listOf(
            ContactPreset(
                contactLookupKey = "lookup_123",
                displayName = "Sarah Connor",
                phoneNumbers = listOf(
                    ContactPhoneNumber("555-0100", "Mobile"),
                    ContactPhoneNumber("555-0101", "Work"),
                ),
                messages = listOf(
                    WidgetMessage(101L, "Come with me if you want to live"),
                    WidgetMessage(102L, "Be right there"),
                ),
                lastUpdated = 1725800000000L,
            ),
            ContactPreset(
                contactLookupKey = null,
                displayName = "John Doe",
                phoneNumbers = listOf(ContactPhoneNumber("555-0200", "Home")),
                messages = listOf(WidgetMessage(201L, "Hey there")),
                lastUpdated = 1725800001000L,
            ),
        )

        val json = backupRepo.encodeBackup(presets)
        val decodedResult = backupRepo.decodeBackup(json)

        assertTrue(decodedResult.isSuccess)
        val backupData = decodedResult.getOrThrow()
        assertEquals(BackupRepository.BACKUP_VERSION, backupData.version)
        assertEquals(2, backupData.presets.size)

        val first = backupData.presets[0]
        assertEquals("Sarah Connor", first.displayName)
        assertEquals("lookup_123", first.contactLookupKey)
        assertEquals(2, first.phoneNumbers.size)
        assertEquals("555-0100", first.phoneNumbers[0].number)
        assertEquals("Mobile", first.phoneNumbers[0].label)
        assertEquals(2, first.messages.size)
        assertEquals("Come with me if you want to live", first.messages[0].text)

        val second = backupData.presets[1]
        assertEquals("John Doe", second.displayName)
        assertEquals(1, second.phoneNumbers.size)
        assertEquals("Hey there", second.messages[0].text)
    }

    @Test
    fun `decodeBackup handles single contact messages export format`() {
        val json = """
            {
              "displayName": "Dad",
              "messages": [
                "Call you soon",
                "On my way!"
              ]
            }
        """.trimIndent()

        val decodedResult = backupRepo.decodeBackup(json)
        assertTrue(decodedResult.isSuccess)
        val backupData = decodedResult.getOrThrow()
        assertEquals(1, backupData.presets.size)
        assertEquals("Dad", backupData.presets[0].displayName)
        assertEquals(2, backupData.presets[0].messages.size)
        assertEquals("Call you soon", backupData.presets[0].messages[0].text)
        assertEquals("On my way!", backupData.presets[0].messages[1].text)
    }

    @Test
    fun `decodeBackup fails gracefully on invalid json`() {
        val result = backupRepo.decodeBackup("{ invalid json content }")
        assertTrue(result.isFailure)
    }

    @Test
    fun `decodeBackup handles empty presets list`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1725800000000,
              "presets": []
            }
        """.trimIndent()

        val decodedResult = backupRepo.decodeBackup(json)
        assertTrue(decodedResult.isSuccess)
        val backupData = decodedResult.getOrThrow()
        assertTrue(backupData.presets.isEmpty())
    }

    @Test
    fun `phoneNumbersMatch matches local and international prefixes`() {
        assertTrue(phoneNumbersMatch("011-1-555-123-4567", "555-123-4567"))
        assertTrue(phoneNumbersMatch("+44 20 7946 0991", "020 7946 0991"))
        assertFalse(phoneNumbersMatch("123", "456"))
    }

    @Test
    fun `preset removal selectively removes matching preset and keeps others`() {
        val carmen = ContactPreset(
            displayName = "Carmen",
            phoneNumbers = listOf(ContactPhoneNumber("555-0100", "Mobile")),
            messages = listOf(WidgetMessage(1, "Testing")),
        )
        val dad = ContactPreset(
            displayName = "Dad",
            phoneNumbers = listOf(ContactPhoneNumber("555-0200", "Mobile")),
            messages = listOf(WidgetMessage(2, "On my way")),
        )
        val list = mutableListOf(carmen, dad)

        // Simulate deleting Carmen
        list.removeAll { it.matchesContact(carmen.contactLookupKey, carmen.displayName, carmen.phoneNumbers) }

        assertEquals(1, list.size)
        assertEquals("Dad", list[0].displayName)
    }
}


