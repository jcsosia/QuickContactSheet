package com.quickcontactsheet

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.ui.theme.QuickContactSheetTheme
import com.quickcontactsheet.widget.QuickContactSheetWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickContactSheetTheme {
                Surface {
                    MainRoute(activity = this)
                }
            }
        }
    }
}

@Composable
private fun MainRoute(activity: ComponentActivity) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val appWidgetManager = activity.getSystemService(AppWidgetManager::class.java)
                    val message = when {
                        appWidgetManager == null || !appWidgetManager.isRequestPinAppWidgetSupported ->
                            context.getString(R.string.widget_picker_not_supported)
                        !appWidgetManager.requestPinAppWidget(
                            ComponentName(activity, QuickContactSheetWidgetReceiver::class.java),
                            null,
                            null,
                        ) -> context.getString(R.string.pinning_unavailable)
                        else -> context.getString(R.string.widget_picker_requested)
                    }
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                    )
                },
                text = { Text(text = context.getString(R.string.add_widget)) },
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    top = innerPadding.calculateTopPadding() + 24.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = context.getString(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = context.getString(R.string.welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

