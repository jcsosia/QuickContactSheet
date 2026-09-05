package com.quickcontactsheet.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

suspend fun Context.refreshQuickContactSheetWidgets() {
    QuickContactSheetWidget().updateAll(this)
}
