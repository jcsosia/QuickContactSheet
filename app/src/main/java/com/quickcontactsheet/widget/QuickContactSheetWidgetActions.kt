package com.quickcontactsheet.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.quickcontactsheet.QuickContactIntents
import com.quickcontactsheet.data.AppSettingsRepository
import com.quickcontactsheet.data.WidgetSettingsRepository
import com.quickcontactsheet.tryLaunchIntent
import com.quickcontactsheet.util.HapticFeedbackHelper

class OpenQuickActionsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        if (AppSettingsRepository.get(context).isHapticFeedbackEnabled()) {
            HapticFeedbackHelper.performClick(context)
        }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val settings = WidgetSettingsRepository.get(context).getWidgetSettings(appWidgetId)
        val intent = if (settings?.isConfigured == true) {
            QuickContactIntents.createQuickActionsIntent(context, appWidgetId)
        } else {
            QuickContactIntents.createConfigurationIntent(context, appWidgetId)
        }
        context.startActivity(intent)
    }
}

class DialContactAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        if (AppSettingsRepository.get(context).isHapticFeedbackEnabled()) {
            HapticFeedbackHelper.performClick(context)
        }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val settings = WidgetSettingsRepository.get(context).getWidgetSettings(appWidgetId)
        val launched = settings?.let(QuickContactIntents::createDialIntent)?.let(context::tryLaunchIntent) == true
        if (!launched) {
            context.startActivity(
                QuickContactIntents.createQuickActionsIntent(context, appWidgetId),
            )
        }
    }
}

class TextContactAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        if (AppSettingsRepository.get(context).isHapticFeedbackEnabled()) {
            HapticFeedbackHelper.performClick(context)
        }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val settings = WidgetSettingsRepository.get(context).getWidgetSettings(appWidgetId)
        val launched = settings?.let(QuickContactIntents::createSmsIntent)?.let(context::tryLaunchIntent) == true
        if (!launched) {
            context.startActivity(
                QuickContactIntents.createQuickActionsIntent(context, appWidgetId),
            )
        }
    }
}
