package com.quickcontactsheet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.quickcontactsheet.data.WidgetSettingsRepository
import kotlinx.coroutines.runBlocking

class QuickContactSheetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickContactSheetWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.quickcontactsheet.action.REFRESH_WIDGET") {
            runBlocking {
                context.refreshQuickContactSheetWidgets()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        runBlocking {
            val repository = WidgetSettingsRepository.get(context)
            appWidgetIds.forEach { repository.deleteWidget(it) }
        }
    }
}
