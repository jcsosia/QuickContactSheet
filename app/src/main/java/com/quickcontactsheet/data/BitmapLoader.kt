package com.quickcontactsheet.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

fun Context.loadBitmapFromFileOrUri(
    pathOrUri: String?,
    maxDimensionPx: Int = 512,
): Bitmap? {
    if (pathOrUri.isNullOrBlank()) return null

    // Check if it's a direct file path
    val directFile = File(pathOrUri)
    if (directFile.exists() && directFile.isFile) {
        return decodeSampledBitmapFromFile(directFile, maxDimensionPx)
    }

    // Check if it's a file:// URI
    if (pathOrUri.startsWith("file://")) {
        val fileUri = Uri.parse(pathOrUri)
        val file = fileUri.path?.let { File(it) }
        if (file?.exists() == true) {
            return decodeSampledBitmapFromFile(file, maxDimensionPx)
        }
    }

    // Check if it's a content:// URI
    if (pathOrUri.startsWith("content://")) {
        val uri = Uri.parse(pathOrUri)
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                decodeSampledBitmapFromBytes(stream.readBytes(), maxDimensionPx)
            }
        }.getOrNull()
    }

    return null
}

fun Context.loadContactPhoto(
    contactId: Long?,
    lookupUriString: String?,
    photoUriString: String?,
    maxDimensionPx: Int = 512,
): Bitmap? {
    // 1. Try loading from photoUriString if already local file or accessible
    val existing = loadBitmapFromFileOrUri(photoUriString, maxDimensionPx)
    if (existing != null) return existing

    // 2. Try ContactsContract.Contacts.openContactPhotoInputStream with lookup URI (high-res)
    if (!lookupUriString.isNullOrBlank()) {
        runCatching {
            val lookupUri = Uri.parse(lookupUriString)
            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, lookupUri, true)?.use { stream ->
                decodeSampledBitmapFromBytes(stream.readBytes(), maxDimensionPx)
            }
        }.getOrNull()?.let { return it }
    }

    // 3. Try with contact ID (high-res)
    if (contactId != null && contactId > 0) {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        runCatching {
            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, true)?.use { stream ->
                decodeSampledBitmapFromBytes(stream.readBytes(), maxDimensionPx)
            }
        }.getOrNull()?.let { return it }
    }

    // 4. Try standard thumbnail via openContactPhotoInputStream (non-highres)
    if (!lookupUriString.isNullOrBlank()) {
        runCatching {
            val lookupUri = Uri.parse(lookupUriString)
            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, lookupUri, false)?.use { stream ->
                decodeSampledBitmapFromBytes(stream.readBytes(), maxDimensionPx)
            }
        }.getOrNull()?.let { return it }
    }

    if (contactId != null && contactId > 0) {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        runCatching {
            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, false)?.use { stream ->
                decodeSampledBitmapFromBytes(stream.readBytes(), maxDimensionPx)
            }
        }.getOrNull()?.let { return it }
    }

    // 5. Query ContactsContract.Data directly for PHOTO blob (data15)
    if (contactId != null && contactId > 0) {
        runCatching {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val blob = cursor.getBlob(0)
                    if (blob != null && blob.isNotEmpty()) {
                        decodeSampledBitmapFromBytes(blob, maxDimensionPx)
                    } else null
                } else null
            }
        }.getOrNull()?.let { return it }
    }

    return null
}

fun Context.saveWidgetPhoto(widgetId: Int, bitmap: Bitmap): String {
    val dir = File(filesDir, "widget_photos").apply { mkdirs() }
    val file = File(dir, "widget_$widgetId.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file.absolutePath
}

fun Context.deleteWidgetPhoto(widgetId: Int) {
    val dir = File(filesDir, "widget_photos")
    File(dir, "widget_$widgetId.jpg").delete()
    File(dir, "widget_$widgetId.png").delete()
}

private fun decodeSampledBitmapFromFile(file: File, maxDimensionPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val longestSide = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    val sampleSize = generateSequence(1) { it * 2 }
        .first { longestSide / it <= maxDimensionPx }

    return runCatching {
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }.getOrNull()
}

private fun decodeSampledBitmapFromBytes(bytes: ByteArray, maxDimensionPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val longestSide = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    val sampleSize = generateSequence(1) { it * 2 }
        .first { longestSide / it <= maxDimensionPx }

    return runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }.getOrNull()
}

// Backward-compatibility alias
fun Context.loadBitmapFromUri(uriString: String?, maxDimensionPx: Int = 512): Bitmap? =
    loadBitmapFromFileOrUri(uriString, maxDimensionPx)
