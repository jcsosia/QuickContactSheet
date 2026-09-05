package com.quickcontactsheet.data

data class WidgetMessage(
    val id: Long,
    val text: String,
)

data class WidgetSettings(
    val widgetId: Int,
    val contactId: Long? = null,
    val contactLookupKey: String? = null,
    val contactLookupUri: String? = null,
    val displayName: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val photoUri: String? = null,
    val messages: List<WidgetMessage> = emptyList(),
) {
    val primaryPhoneNumber: String?
        get() = phoneNumbers.firstOrNull()

    val isConfigured: Boolean
        get() = !displayName.isBlank() && !primaryPhoneNumber.isNullOrBlank()
}

data class ContactSummary(
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val photoUri: String?,
) {
    val primaryPhoneNumber: String?
        get() = phoneNumbers.firstOrNull()
}

sealed interface LoadResult<out T> {
    data class Success<T>(val value: T) : LoadResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : LoadResult<Nothing>
}
