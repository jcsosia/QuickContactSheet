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
