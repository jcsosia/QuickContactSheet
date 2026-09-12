package com.quickcontactsheet.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackEmailHelperTest {

    @Test
    fun supportEmail_isCorrectAddress() {
        assertEquals("support@sosiacollective.com", FeedbackEmailHelper.SUPPORT_EMAIL)
    }

    @Test
    fun buildFeedbackSubject_formatsCorrectly() {
        val subject = FeedbackEmailHelper.buildFeedbackSubject(
            versionName = "1.0",
            versionCode = 1,
        )
        assertEquals("[Quick Contact Sheet Feedback] v1.0 (1)", subject)
    }

    @Test
    fun buildFeedbackBody_containsGreetingAndDiagnostics() {
        val body = FeedbackEmailHelper.buildFeedbackBody(
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8",
            osRelease = "14",
            sdkInt = 34,
            versionName = "1.0",
            versionCode = 1,
            buildType = "release",
        )

        assertTrue("Should contain greeting", body.contains("Hi JC!"))
        assertTrue("Should contain prompt placeholder", body.contains("[Please share your feedback, idea, or issue here]"))
        assertTrue("Should contain app version info", body.contains("App: Quick Contact Sheet v1.0 (1, release)"))
        assertTrue("Should contain device model", body.contains("Device: Google Pixel 8"))
        assertTrue("Should contain OS version", body.contains("OS: Android 14 (API 34)"))
    }
}
