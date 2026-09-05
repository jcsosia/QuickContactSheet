package com.quickcontactsheet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

suspend fun Context.refreshQuickContactSheetWidgets() {
    QuickContactSheetWidget().updateAll(this)
}

suspend fun Context.refreshQuickContactSheetWidget(widgetId: Int) {
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        runCatching {
            val glanceManager = GlanceAppWidgetManager(this)
            val glanceId = glanceManager.getGlanceIdBy(widgetId)
            QuickContactSheetWidget().update(this, glanceId)
        }
    }
    runCatching {
        QuickContactSheetWidget().updateAll(this)
    }
}
