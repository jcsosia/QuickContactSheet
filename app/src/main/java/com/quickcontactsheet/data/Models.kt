package com.quickcontactsheet.data

data class WidgetMessage(
    val id: Long,
    val text: String,
)

data class ContactPhoneNumber(
    val number: String,
    val label: String = "",
)

data class WidgetSettings(
    val widgetId: Int,
    val contactId: Long? = null,
    val contactLookupKey: String? = null,
    val contactLookupUri: String? = null,
    val displayName: String = "",
    val phoneNumbers: List<ContactPhoneNumber> = emptyList(),
    val selectedPhoneNumber: String? = null,
    val photoUri: String? = null,
    val messages: List<WidgetMessage> = emptyList(),
) {
    val primaryPhoneNumber: String?
        get() = selectedPhoneNumber?.takeIf { selected -> phoneNumbers.any { it.number == selected } }
            ?: phoneNumbers.firstOrNull()?.number

    val primaryPhoneLabel: String?
        get() = phoneNumbers.firstOrNull { it.number == primaryPhoneNumber }?.label?.ifBlank { null }

    val isConfigured: Boolean
        get() = !displayName.isBlank() && !primaryPhoneNumber.isNullOrBlank()
}

data class ContactSummary(
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<ContactPhoneNumber>,
    val photoUri: String?,
) {
    val primaryPhoneNumber: String?
        get() = phoneNumbers.firstOrNull()?.number

    val primaryPhoneLabel: String?
        get() = phoneNumbers.firstOrNull()?.label?.ifBlank { null }
}

sealed interface LoadResult<out T> {
    data class Success<T>(val value: T) : LoadResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : LoadResult<Nothing>
}

data class ContactPreset(
    val contactLookupKey: String? = null,
    val displayName: String,
    val phoneNumbers: List<ContactPhoneNumber> = emptyList(),
    val messages: List<WidgetMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    fun matchesContact(
        targetLookupKey: String?,
        targetDisplayName: String,
        targetPhoneNumbers: List<ContactPhoneNumber>,
    ): Boolean {
        if (!this.contactLookupKey.isNullOrBlank() && !targetLookupKey.isNullOrBlank() && this.contactLookupKey == targetLookupKey) {
            return true
        }
        for (p1 in this.phoneNumbers) {
            for (p2 in targetPhoneNumbers) {
                if (phoneNumbersMatch(p1.number, p2.number)) {
                    return true
                }
            }
        }
        if (this.displayName.isNotBlank() && targetDisplayName.isNotBlank() &&
            this.displayName.trim().equals(targetDisplayName.trim(), ignoreCase = true)
        ) {
            return true
        }
        return false
    }
}

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val presets: List<ContactPreset> = emptyList(),
)

fun normalizePhoneNumber(number: String): String =
    number.filter { it.isDigit() || it == '+' }

fun phoneNumbersMatch(num1: String, num2: String): Boolean {
    val norm1 = normalizePhoneNumber(num1)
    val norm2 = normalizePhoneNumber(num2)
    if (norm1.isBlank() || norm2.isBlank()) return false
    if (norm1 == norm2) return true
    val digits1 = norm1.filter { it.isDigit() }
    val digits2 = norm2.filter { it.isDigit() }
    if (digits1.isBlank() || digits2.isBlank()) return false
    if (digits1 == digits2) return true
    if (digits1.length >= 7 && digits2.length >= 7) {
        val minLen = minOf(digits1.length, digits2.length, 10)
        return digits1.takeLast(minLen) == digits2.takeLast(minLen)
    }
    return false
}

