package com.quickcontactsheet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.quickcontactsheet.data.WidgetSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickContactSheetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickContactSheetWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.quickcontactsheet.action.REFRESH_WIDGET") {
            val pendingResult = goAsync()
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Small delay to allow the launcher activity transition to complete and
                    // LauncherAppWidgetHostView to be bound on the home screen
                    kotlinx.coroutines.delay(400)
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        context.refreshQuickContactSheetWidget(widgetId)
                    } else {
                        context.refreshQuickContactSheetWidgets()
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetSettingsRepository.get(context)
                appWidgetIds.forEach { repository.deleteWidget(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
