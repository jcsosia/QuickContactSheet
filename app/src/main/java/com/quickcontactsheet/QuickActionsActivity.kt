package com.quickcontactsheet

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.data.AppSettings
import com.quickcontactsheet.data.AppSettingsRepository
import com.quickcontactsheet.data.WidgetSettings
import com.quickcontactsheet.data.WidgetSettingsRepository
import com.quickcontactsheet.ui.components.ContactAvatar
import com.quickcontactsheet.ui.theme.QuickContactSheetTheme
import com.quickcontactsheet.util.HapticFeedbackHelper
import kotlinx.coroutines.launch

class QuickActionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        setContent {
            QuickContactSheetTheme {
                QuickActionsRoute(
                    widgetId = intent.getIntExtra(
                        QuickContactIntents.EXTRA_WIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    ),
                    onClose = { finish() },
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsRoute(
    widgetId: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { WidgetSettingsRepository.get(context) }
    val settings by repository.widgetSettingsFlow(widgetId).collectAsState(initial = null)
    val appSettingsRepo = remember(context) { AppSettingsRepository.get(context) }
    val appSettings by appSettingsRepo.appSettingsFlow.collectAsState(initial = AppSettings())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val current = settings
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || (current != null && !current.isConfigured)) {
            ModalBottomSheet(
                onDismissRequest = onClose,
                scrimColor = Color.Black.copy(alpha = 0.5f),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "This widget could not be configured.",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Button(onClick = onClose) {
                        Text(text = "Close")
                    }
                }
            }
        } else if (current != null) {
            ModalBottomSheet(
                onDismissRequest = onClose,
                scrimColor = Color.Black.copy(alpha = 0.5f),
                containerColor = Color(0xFF13171C),
                dragHandle = null,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                QuickActionsSheet(
                    settings = current,
                    hapticFeedbackEnabled = appSettings.hapticFeedbackEnabled,
                    quickMessagesOnTop = appSettings.quickMessagesOnTop,
                    onClose = onClose,
                    onMessageError = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSheet(
    settings: WidgetSettings,
    hapticFeedbackEnabled: Boolean,
    quickMessagesOnTop: Boolean,
    onClose: () -> Unit,
    onMessageError: (String) -> Unit,
) {
    val context = LocalContext.current

    fun performHaptic() {
        if (hapticFeedbackEnabled) {
            HapticFeedbackHelper.performClick(context)
        }
    }

    fun launchOrNotify(intent: android.content.Intent?, fallbackMessage: String) {
        when {
            intent == null -> onMessageError(fallbackMessage)
            context.tryLaunchIntent(intent) -> onClose()
            else -> onMessageError(context.getString(R.string.action_not_supported))
        }
    }

    val contactRow: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ContactAvatar(
                photoUri = settings.photoUri,
                contentDescription = settings.displayName,
                modifier = Modifier.size(96.dp),
                onClick = {
                    performHaptic()
                    launchOrNotify(
                        QuickContactIntents.createContactIntent(settings),
                        context.getString(R.string.launch_contact_failed),
                    )
                },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    onClick = {
                        performHaptic()
                        launchOrNotify(
                            QuickContactIntents.createDialIntent(settings),
                            context.getString(R.string.no_phone_number),
                        )
                    },
                    shape = CircleShape,
                    color = Color(0xFF2C3138),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = context.getString(R.string.call),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Surface(
                    onClick = {
                        performHaptic()
                        launchOrNotify(
                            QuickContactIntents.createSmsIntent(settings),
                            context.getString(R.string.no_phone_number),
                        )
                    },
                    shape = CircleShape,
                    color = Color(0xFF2C3138),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Message,
                            contentDescription = context.getString(R.string.text),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }

    val messagesRow: @Composable () -> Unit = {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(settings.messages, key = { it.id }) { message ->
                Surface(
                    onClick = {
                        performHaptic()
                        launchOrNotify(
                            QuickContactIntents.createSmsIntent(settings, message.text),
                            context.getString(R.string.no_phone_number),
                        )
                    },
                    shape = CircleShape,
                    color = Color(0xFF13171C),
                    border = BorderStroke(1.5.dp, Color(0xFFE2E2E6)),
                    modifier = Modifier.height(44.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                        )
                    }
                }
            }
            item {
                Surface(
                    onClick = {
                        performHaptic()
                        context.startActivity(
                            QuickContactIntents.createConfigurationIntent(
                                context = context,
                                widgetId = settings.widgetId,
                                openMessageEditor = true,
                            ),
                        )
                        onClose()
                    },
                    shape = CircleShape,
                    color = Color(0xFF13171C),
                    border = BorderStroke(1.5.dp, Color(0xFF8EC5FF)),
                    modifier = Modifier.height(44.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color(0xFF8EC5FF),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = context.getString(R.string.add_or_edit_messages),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF8EC5FF),
                        )
                    }
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
    ) {
        if (quickMessagesOnTop) {
            messagesRow()
            contactRow()
        } else {
            contactRow()
            messagesRow()
        }
    }
}
