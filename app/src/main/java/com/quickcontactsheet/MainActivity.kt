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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.data.AppSettings
import com.quickcontactsheet.data.AppSettingsRepository
import com.quickcontactsheet.data.BackupData
import com.quickcontactsheet.data.BackupRepository
import com.quickcontactsheet.data.WidgetSettingsRepository
import com.quickcontactsheet.ui.components.ManagePresetsSheet
import com.quickcontactsheet.ui.components.SettingsDivider
import com.quickcontactsheet.ui.components.SettingsGroup
import com.quickcontactsheet.ui.components.SettingsNavigationTile
import com.quickcontactsheet.ui.components.SettingsSwitchTile
import com.quickcontactsheet.ui.components.SettingsTile
import com.quickcontactsheet.ui.theme.QuickContactSheetTheme
import com.quickcontactsheet.widget.QuickContactSheetWidgetReceiver
import com.quickcontactsheet.widget.refreshQuickContactSheetWidgets
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val appSettingsRepo = remember(context) { AppSettingsRepository.get(context) }
    val appSettings by appSettingsRepo.appSettingsFlow.collectAsState(initial = AppSettings())
    val repository = remember(context) { WidgetSettingsRepository.get(context) }
    val backupRepo = remember(context) { BackupRepository.get(context) }
    val allPresets by repository.allPresetsFlow.collectAsState(initial = emptyList())

    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var managePresetsVisible by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val presets = repository.getAllPresets()
                if (presets.isEmpty()) {
                    snackbarHostState.showSnackbar(context.getString(R.string.no_presets_to_export))
                    return@launch
                }
                val result = backupRepo.exportToUri(uri, presets)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.export_success, result.getOrNull() ?: presets.size),
                    )
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_failed))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = backupRepo.importFromUri(uri)
                val backupData = result.getOrNull()
                if (backupData != null && backupData.presets.isNotEmpty()) {
                    pendingRestoreData = backupData
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.invalid_backup_file))
                }
            }
        }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    top = innerPadding.calculateTopPadding() + 24.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
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

            SettingsGroup(
                title = context.getString(R.string.settings_title),
            ) {
                SettingsNavigationTile(
                    title = context.getString(R.string.settings_permissions_title),
                    subtitle = context.getString(R.string.settings_permissions_subtitle),
                    icon = Icons.Rounded.Security,
                    onClick = {
                        QuickContactIntents.createAppSettingsIntent(context).let(context::tryLaunchIntent)
                    },
                )
                SettingsDivider()
                SettingsSwitchTile(
                    title = context.getString(R.string.settings_haptic_title),
                    subtitle = context.getString(R.string.settings_haptic_subtitle),
                    icon = Icons.Rounded.Vibration,
                    checked = appSettings.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            appSettingsRepo.setHapticFeedbackEnabled(enabled)
                        }
                    },
                )
                SettingsDivider()
                SettingsTile(
                    title = context.getString(R.string.settings_about_title),
                    subtitle = context.getString(R.string.settings_version, BuildConfig.VERSION_NAME),
                    icon = Icons.Rounded.Info,
                )
            }

            SettingsGroup(
                title = context.getString(R.string.backup_restore_title),
            ) {
                SettingsNavigationTile(
                    title = context.getString(R.string.export_backup_title),
                    subtitle = context.getString(R.string.export_backup_subtitle),
                    icon = Icons.Rounded.UploadFile,
                    onClick = {
                        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val fileName = "quick_contact_sheet_backup_${dateFormat.format(Date())}.json"
                        exportLauncher.launch(fileName)
                    },
                )
                SettingsDivider()
                SettingsNavigationTile(
                    title = context.getString(R.string.restore_backup_title),
                    subtitle = context.getString(R.string.restore_backup_subtitle),
                    icon = Icons.Rounded.FileDownload,
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                )
                SettingsDivider()
                SettingsNavigationTile(
                    title = context.getString(R.string.manage_presets_title),
                    subtitle = if (allPresets.isEmpty()) {
                        context.getString(R.string.manage_presets_subtitle_empty)
                    } else {
                        context.getString(R.string.manage_presets_subtitle_format, allPresets.size)
                    },
                    icon = Icons.Rounded.Bookmarks,
                    onClick = { managePresetsVisible = true },
                )
            }
        }
    }

    if (managePresetsVisible) {
        ManagePresetsSheet(
            presets = allPresets,
            onDismiss = { managePresetsVisible = false },
            onDeletePreset = { preset ->
                scope.launch {
                    repository.deletePreset(preset)
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.preset_deleted, preset.displayName),
                    )
                }
            },
            onClearAll = {
                scope.launch {
                    repository.clearAllPresets()
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.all_presets_cleared),
                    )
                }
            },
        )
    }

    pendingRestoreData?.let { data ->

        val totalMessages = data.presets.sumOf { it.messages.size }
        AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            title = { Text(text = stringResource(R.string.restore_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.restore_dialog_message,
                        data.presets.size,
                        totalMessages,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRestore = data.presets
                        pendingRestoreData = null
                        scope.launch {
                            val restoredCount = repository.restorePresets(toRestore)
                            val appContext = context.applicationContext
                            appContext.refreshQuickContactSheetWidgets()
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.restore_success, restoredCount),
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreData = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

