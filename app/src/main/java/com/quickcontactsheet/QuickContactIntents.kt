package com.quickcontactsheet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.quickcontactsheet.data.WidgetSettings

object QuickContactIntents {
    const val EXTRA_WIDGET_ID = "extra_widget_id"
    const val EXTRA_OPEN_MESSAGE_EDITOR = "extra_open_message_editor"

    fun createDialIntent(settings: WidgetSettings): Intent? {
        val number = settings.primaryPhoneNumber ?: return null
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
    }

    fun createSmsIntent(
        settings: WidgetSettings,
        message: String? = null,
    ): Intent? {
        val number = settings.primaryPhoneNumber ?: return null
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
            if (!message.isNullOrBlank()) {
                putExtra("sms_body", message)
            }
        }
    }

    fun createContactIntent(settings: WidgetSettings): Intent? {
        val lookupUri = settings.contactLookupUri ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(lookupUri))
    }

    fun createConfigurationIntent(
        context: Context,
        widgetId: Int,
        openMessageEditor: Boolean = false,
    ): Intent =
        Intent(context, WidgetConfigurationActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_OPEN_MESSAGE_EDITOR, openMessageEditor)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun createQuickActionsIntent(
        context: Context,
        widgetId: Int,
    ): Intent =
        Intent(context, QuickActionsActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, widgetId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
        }

    fun createAppSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
}

fun Context.tryLaunchIntent(intent: Intent): Boolean =
    runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
