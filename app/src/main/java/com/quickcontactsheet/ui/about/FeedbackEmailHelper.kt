package com.quickcontactsheet.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.quickcontactsheet.BuildConfig

object FeedbackEmailHelper {
    const val SUPPORT_EMAIL = "support@sosiacollective.com"

    fun buildFeedbackSubject(
        versionName: String = BuildConfig.VERSION_NAME,
        versionCode: Int = BuildConfig.VERSION_CODE,
    ): String {
        return "[Quick Contact Sheet Feedback] v$versionName ($versionCode)"
    }

    fun buildFeedbackBody(
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL,
        osRelease: String = Build.VERSION.RELEASE,
        sdkInt: Int = Build.VERSION.SDK_INT,
        versionName: String = BuildConfig.VERSION_NAME,
        versionCode: Int = BuildConfig.VERSION_CODE,
        buildType: String = BuildConfig.BUILD_TYPE,
    ): String {
        val model = "$deviceManufacturer $deviceModel".trim()
        val os = "Android $osRelease (API $sdkInt)"
        val app = "v$versionName ($versionCode, $buildType)"

        return """
            Hi JC!

            [Please share your feedback, idea, or issue here]


            --- Diagnostics (Helps us fix issues faster) ---
            App: Quick Contact Sheet $app
            Device: $model
            OS: $os
            ------------------------------------------------
        """.trimIndent()
    }

    fun createFeedbackEmailIntent(
        recipient: String = SUPPORT_EMAIL,
        subject: String = buildFeedbackSubject(),
        body: String = buildFeedbackBody(),
    ): Intent {
        val uriText = "mailto:$recipient" +
            "?subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body)

        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(uriText)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
}
