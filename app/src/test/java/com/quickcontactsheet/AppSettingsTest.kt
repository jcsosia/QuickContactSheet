package com.quickcontactsheet

import com.quickcontactsheet.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default app settings has expected defaults`() {
        val settings = AppSettings()
        assertTrue(settings.hapticFeedbackEnabled)
    }

    @Test
    fun `settings can be copied with modified values`() {
        val original = AppSettings(hapticFeedbackEnabled = true)
        val modified = original.copy(hapticFeedbackEnabled = false)

        assertTrue(original.hapticFeedbackEnabled)
        assertFalse(modified.hapticFeedbackEnabled)
    }
}
