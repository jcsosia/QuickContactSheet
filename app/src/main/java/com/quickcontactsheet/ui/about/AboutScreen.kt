package com.quickcontactsheet.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.BuildConfig
import com.quickcontactsheet.QuickContactIntents
import com.quickcontactsheet.R
import com.quickcontactsheet.tryLaunchIntent
import com.quickcontactsheet.ui.components.SettingsDivider
import com.quickcontactsheet.ui.components.SettingsGroup
import com.quickcontactsheet.ui.components.SettingsNavigationTile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLicensesDialog by remember { mutableStateOf(false) }

    fun launchUrl(url: String) {
        val intent = QuickContactIntents.createWebIntent(url)
        val launched = context.tryLaunchIntent(intent)
        if (!launched) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.unable_to_open_link))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // App Identity Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(56.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.about_version_build,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = stringResource(R.string.about_app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Privacy & Security
            SettingsGroup(
                title = stringResource(R.string.about_privacy_title),
            ) {
                Text(
                    text = stringResource(R.string.about_privacy_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                SettingsDivider()
                SettingsNavigationTile(
                    title = stringResource(R.string.about_privacy_policy_title),
                    subtitle = stringResource(R.string.about_privacy_policy_subtitle),
                    icon = Icons.Rounded.PrivacyTip,
                    onClick = { launchUrl(context.getString(R.string.about_privacy_policy_url)) },
                )
                SettingsDivider()
                SettingsNavigationTile(
                    title = stringResource(R.string.about_terms_title),
                    subtitle = stringResource(R.string.about_terms_subtitle),
                    icon = Icons.Rounded.Description,
                    onClick = { launchUrl(context.getString(R.string.about_terms_url)) },
                )
            }

            // Legal & Open Source
            SettingsGroup(
                title = stringResource(R.string.about_open_source_licenses_title),
            ) {
                SettingsNavigationTile(
                    title = stringResource(R.string.about_open_source_licenses_title),
                    subtitle = stringResource(R.string.about_open_source_licenses_subtitle),
                    icon = Icons.Rounded.Code,
                    onClick = { showLicensesDialog = true },
                )
            }

            // Project & Support
            SettingsGroup(
                title = stringResource(R.string.about_project_title),
            ) {
                SettingsNavigationTile(
                    title = stringResource(R.string.about_github_title),
                    subtitle = stringResource(R.string.about_github_subtitle),
                    icon = Icons.Rounded.Public,
                    onClick = { launchUrl(context.getString(R.string.about_github_url)) },
                )
                SettingsDivider()
                SettingsNavigationTile(
                    title = stringResource(R.string.about_issues_title),
                    subtitle = stringResource(R.string.about_issues_subtitle),
                    icon = Icons.Rounded.BugReport,
                    onClick = { launchUrl(context.getString(R.string.about_issues_url)) },
                )
            }
        }
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.about_licenses_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    LicenseItem(
                        name = "Jetpack Compose & Material 3",
                        license = "Apache License 2.0",
                        copyright = "The Android Open Source Project",
                    )
                    LicenseItem(
                        name = "Jetpack Glance (AppWidgets)",
                        license = "Apache License 2.0",
                        copyright = "The Android Open Source Project",
                    )
                    LicenseItem(
                        name = "AndroidX (Core, Lifecycle, Navigation, DataStore)",
                        license = "Apache License 2.0",
                        copyright = "The Android Open Source Project",
                    )
                    LicenseItem(
                        name = "Material Components for Android",
                        license = "Apache License 2.0",
                        copyright = "Google LLC",
                    )
                    LicenseItem(
                        name = "Reorderable (Jetpack Compose Drag & Drop)",
                        license = "Apache License 2.0",
                        copyright = "Calvin Liang",
                    )
                    LicenseItem(
                        name = "Kotlin & Kotlinx Coroutines",
                        license = "Apache License 2.0",
                        copyright = "JetBrains s.r.o. and Kotlin project contributors",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text(text = stringResource(R.string.about_close))
                }
            },
        )
    }
}

@Composable
private fun LicenseItem(
    name: String,
    license: String,
    copyright: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$license • $copyright",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
