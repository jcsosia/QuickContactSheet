package com.quickcontactsheet.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ContactsRepository(
    private val context: Context,
) {
    suspend fun loadContacts(): LoadResult<List<ContactSummary>> =
        runCatching {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return LoadResult.Error("Contacts permission has not been granted.")
            }

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
            )

            val grouped = linkedMapOf<Long, MutableContact>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val groupedContact = grouped.getOrPut(id) {
                        MutableContact(
                            contactId = id,
                            lookupKey = cursor.getString(lookupIndex).orEmpty(),
                            displayName = cursor.getString(nameIndex).orEmpty().ifBlank { "Contact" },
                            photoUri = cursor.getString(photoIndex),
                        )
                    }
                    val rawNumber = cursor.getString(numberIndex).orEmpty().trim()
                    if (rawNumber.isNotBlank()) {
                        val type = cursor.getInt(typeIndex)
                        val customLabel = cursor.getString(labelIndex)
                        val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, customLabel).toString()
                        groupedContact.phoneNumbers += ContactPhoneNumber(
                            number = rawNumber,
                            label = label,
                        )
                    }
                }
            }

            grouped.values
                .mapNotNull { candidate ->
                    val numbers = candidate.phoneNumbers.distinctBy { phone ->
                        phone.number.filter { it.isDigit() || it == '+' }.ifEmpty { phone.number }
                    }
                    if (candidate.lookupKey.isBlank() || numbers.isEmpty()) {
                        null
                    } else {
                        ContactSummary(
                            contactId = candidate.contactId,
                            lookupKey = candidate.lookupKey,
                            displayName = candidate.displayName,
                            phoneNumbers = numbers,
                            photoUri = candidate.photoUri,
                        )
                    }
                }
        }.fold(
            onSuccess = { LoadResult.Success(it) },
            onFailure = { LoadResult.Error("Unable to load contacts.", it) },
        )

    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?,
        val phoneNumbers: MutableList<ContactPhoneNumber> = mutableListOf(),
    )
}
