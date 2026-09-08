package com.quickcontactsheet.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val APP_SETTINGS_DATASTORE = "app_settings"

private val Context.appSettingsDataStore by preferencesDataStore(name = APP_SETTINGS_DATASTORE)

class AppSettingsRepository private constructor(
    private val context: Context,
) {
    val appSettingsFlow: Flow<AppSettings> =
        context.appSettingsDataStore.data.map { preferences ->
            AppSettings(
                hapticFeedbackEnabled = preferences[KEY_HAPTIC_FEEDBACK] ?: true,
            )
        }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }

    companion object {
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")

        @Volatile
        private var instance: AppSettingsRepository? = null

        fun get(context: Context): AppSettingsRepository =
            instance ?: synchronized(this) {
                instance ?: AppSettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
