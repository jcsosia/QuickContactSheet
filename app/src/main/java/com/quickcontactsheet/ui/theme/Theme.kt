package com.quickcontactsheet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val QuickContactSheetColorScheme =
    darkColorScheme(
        primary = Blue300,
        onPrimary = Slate900,
        secondary = Blue200,
        background = Slate900,
        onBackground = TextPrimary,
        surface = Slate800,
        onSurface = TextPrimary,
        surfaceVariant = Slate700,
        onSurfaceVariant = TextSecondary,
        tertiary = SuccessBlue,
    )

@Composable
fun QuickContactSheetTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = QuickContactSheetColorScheme,
        content = content,
    )
}
