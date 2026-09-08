package com.quickcontactsheet

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcontactsheet.data.ContactSummary
import com.quickcontactsheet.data.ContactsRepository
import com.quickcontactsheet.data.LoadResult
import com.quickcontactsheet.data.WidgetMessage
import com.quickcontactsheet.data.WidgetSettings
import com.quickcontactsheet.data.WidgetSettingsRepository
import com.quickcontactsheet.ui.components.ContactAvatar
import com.quickcontactsheet.ui.components.MessageEditorSheet
import com.quickcontactsheet.ui.theme.QuickContactSheetTheme
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.quickcontactsheet.widget.QuickContactSheetWidget
import com.quickcontactsheet.widget.QuickContactSheetWidgetReceiver
import com.quickcontactsheet.widget.refreshQuickContactSheetWidget
import com.quickcontactsheet.widget.refreshQuickContactSheetWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val widgetId = resolveWidgetId(intent)
        setResult(RESULT_CANCELED)

        setContent {
            QuickContactSheetTheme {
                Surface {
                    ConfigurationRoute(
                        widgetId = widgetId,
                        openMessageEditor = intent.getBooleanExtra(QuickContactIntents.EXTRA_OPEN_MESSAGE_EDITOR, false),
                        onClose = { finish() },
                        onComplete = {
                            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                                )
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun resolveWidgetId(intent: Intent): Int =
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            intent.getIntExtra(
                QuickContactIntents.EXTRA_WIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationRoute(
    widgetId: Int,
    openMessageEditor: Boolean,
    onClose: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { WidgetSettingsRepository.get(context) }
    val contactsRepository = remember(context) { ContactsRepository(context) }
    val currentSettings by repository.widgetSettingsFlow(widgetId).collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasContactsPermission by remember { mutableStateOf(context.hasContactsPermission()) }
    var refreshContactsToken by remember { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var shouldOpenEditor by rememberSaveable { mutableStateOf(openMessageEditor) }
    var pendingSelectedContact by remember { mutableStateOf<ContactSummary?>(null) }
    var numberPickerContact by remember { mutableStateOf<ContactSummary?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasContactsPermission = context.hasContactsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasContactsPermission = granted
        if (granted) {
            refreshContactsToken++
        }
    }

    val contactsResult by androidx.compose.runtime.produceState<LoadResult<List<ContactSummary>>?>(
        initialValue = null,
        hasContactsPermission,
        refreshContactsToken,
    ) {
        value = if (hasContactsPermission) {
            contactsRepository.loadContacts()
        } else {
            null
        }
    }

    LaunchedEffect(currentSettings?.isConfigured, shouldOpenEditor) {
        if (shouldOpenEditor && currentSettings?.isConfigured == true) {
            editorVisible = true
            shouldOpenEditor = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(R.string.choose_contact)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = context.getString(R.string.cancel),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val contactToSave = pendingSelectedContact
                            val isConfigured = currentSettings?.isConfigured == true || contactToSave != null
                            if (isConfigured) {
                                val appContext = context.applicationContext
                                scope.launch {
                                    withContext(NonCancellable + Dispatchers.IO) {
                                        if (contactToSave != null && currentSettings?.contactId != contactToSave.contactId) {
                                            repository.saveSelectedContact(widgetId, contactToSave)
                                        }
                                        val glanceManager = GlanceAppWidgetManager(appContext)
                                        val glanceId = runCatching { glanceManager.getGlanceIdBy(widgetId) }.getOrNull()
                                        if (glanceId != null) {
                                            QuickContactSheetWidget().update(appContext, glanceId)
                                        } else {
                                            appContext.refreshQuickContactSheetWidgets()
                                        }
                                    }
                                    val refreshIntent = Intent(appContext, QuickContactSheetWidgetReceiver::class.java).apply {
                                        action = "com.quickcontactsheet.action.REFRESH_WIDGET"
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    }
                                    appContext.sendBroadcast(refreshIntent)
                                    onComplete()
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.no_contact_selected),
                                    )
                                }
                            }
                        },
                    ) {
                        Text(text = context.getString(R.string.done))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            InvalidWidgetContent(innerPadding = innerPadding, onClose = onClose)
            return@Scaffold
        }

        val contacts = (contactsResult as? LoadResult.Success)?.value.orEmpty()
        val filteredContacts = contacts.filter {
            searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ConfigurationHeader(
                    widgetId = widgetId,
                    currentSettings = currentSettings,
                    onEditMessages = { editorVisible = true },
                    onSelectNumber = {
                        val configuredContact = contacts.firstOrNull { it.contactId == currentSettings?.contactId }
                            ?: currentSettings?.let { settings ->
                                ContactSummary(
                                    contactId = settings.contactId ?: 0L,
                                    lookupKey = settings.contactLookupKey.orEmpty(),
                                    displayName = settings.displayName,
                                    phoneNumbers = settings.phoneNumbers,
                                    photoUri = settings.photoUri,
                                )
                            }
                        if (configuredContact != null && configuredContact.phoneNumbers.size > 1) {
                            numberPickerContact = configuredContact
                        }
                    },
                )
            }

            if (!hasContactsPermission) {
                item {
                    PermissionCard(
                        onGrant = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        onOpenSettings = { context.startActivity(QuickContactIntents.createAppSettingsIntent(context)) },
                    )
                }
            } else {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.search_contacts)) },
                        singleLine = true,
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear search",
                                    )
                                }
                            }
                        } else null,
                    )
                }

                when (val result = contactsResult) {
                    null -> item {
                        LoadingCard()
                    }

                    is LoadResult.Error -> item {
                        ErrorCard(
                            message = result.message,
                            onRetry = { refreshContactsToken++ },
                        )
                    }

                    is LoadResult.Success -> {
                        items(filteredContacts, key = { it.contactId }) { contact ->
                            ContactRow(
                                contact = contact,
                                isSelected = (pendingSelectedContact?.contactId ?: currentSettings?.contactId) == contact.contactId,
                                onClick = {
                                    if (contact.phoneNumbers.size > 1) {
                                        numberPickerContact = contact
                                    } else {
                                        pendingSelectedContact = contact
                                        scope.launch {
                                            withContext(NonCancellable + Dispatchers.IO) {
                                                repository.saveSelectedContact(widgetId, contact)
                                            }
                                            if (shouldOpenEditor) {
                                                editorVisible = true
                                                shouldOpenEditor = false
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (editorVisible && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        MessageEditorSheet(
            initialMessages = currentSettings?.messages.orEmpty(),
            onDismiss = { editorVisible = false },
            onSave = { messages ->
                editorVisible = false
                scope.launch {
                    withContext(NonCancellable + Dispatchers.IO) {
                        repository.saveMessages(widgetId, messages)
                        context.refreshQuickContactSheetWidget(widgetId)
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.messages_saved))
                }
            },
        )
    }

    numberPickerContact?.let { contactToPick ->
        val currentSelectedNumber = if (contactToPick.contactId == (pendingSelectedContact?.contactId ?: currentSettings?.contactId)) {
            currentSettings?.primaryPhoneNumber
        } else {
            contactToPick.primaryPhoneNumber
        }

        SelectNumberSheet(
            contact = contactToPick,
            currentSelectedNumber = currentSelectedNumber,
            onDismiss = { numberPickerContact = null },
            onNumberSelected = { chosenNumber ->
                numberPickerContact = null
                pendingSelectedContact = contactToPick
                scope.launch {
                    withContext(NonCancellable + Dispatchers.IO) {
                        repository.saveSelectedContact(
                            widgetId = widgetId,
                            contact = contactToPick,
                            selectedPhoneNumber = chosenNumber,
                        )
                    }
                    if (shouldOpenEditor) {
                        editorVisible = true
                        shouldOpenEditor = false
                    }
                }
            },
        )
    }
}

@Composable
private fun InvalidWidgetContent(
    innerPadding: PaddingValues,
    onClose: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
    ) {
        Text(
            text = "This widget could not be configured.",
            style = MaterialTheme.typography.titleLarge,
        )
        Button(
            onClick = onClose,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = "Close")
        }
    }
}

@Composable
private fun ConfigurationHeader(
    widgetId: Int,
    currentSettings: WidgetSettings?,
    onEditMessages: () -> Unit,
    onSelectNumber: () -> Unit = {},
) {
    Card {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_settings),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (currentSettings?.isConfigured == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ContactAvatar(
                        photoUri = currentSettings.photoUri,
                        contentDescription = currentSettings.displayName,
                        modifier = Modifier.size(72.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = currentSettings.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val primaryNumber = currentSettings.primaryPhoneNumber.orEmpty()
                        val primaryLabel = currentSettings.primaryPhoneLabel
                        val displayPhone = if (!primaryLabel.isNullOrBlank()) {
                            "$primaryNumber ($primaryLabel)"
                        } else {
                            primaryNumber
                        }

                        if (currentSettings.phoneNumbers.size > 1) {
                            Surface(
                                onClick = onSelectNumber,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = displayPhone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = stringResource(R.string.change_default_number),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = displayPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    FilledIconButton(onClick = onEditMessages) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit messages",
                        )
                    }
                }
                if (currentSettings.messages.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_messages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        currentSettings.messages.forEach { message ->
                            AssistChip(
                                onClick = onEditMessages,
                                label = { Text(text = message.text) },
                            )
                        }
                        AssistChip(
                            onClick = onEditMessages,
                            label = { Text(text = stringResource(R.string.message_editor)) },
                        )
                    }
                }
            } else {
                Text(
                    text = "Choose a contact to preview the widget and set up reusable messages.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "Contacts permission needed",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Quick Contact Sheet needs access to your contacts so each widget can be linked to a person, name, photo, and phone number.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onGrant) {
                    Text(text = "Grant access")
                }
                Button(onClick = onOpenSettings) {
                    Text(text = "Open Settings")
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            CircularProgressIndicator()
            Text(text = "Loading contacts…")
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Card {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ContactAvatar(
                photoUri = contact.photoUri,
                contentDescription = contact.displayName,
                modifier = Modifier.size(56.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                val primaryPhone = contact.phoneNumbers.firstOrNull()
                val basePhoneText = if (primaryPhone != null) {
                    if (primaryPhone.label.isNotBlank()) "${primaryPhone.number} (${primaryPhone.label})" else primaryPhone.number
                } else ""
                val subtitleText = if (contact.phoneNumbers.size > 1) {
                    val countText = stringResource(
                        R.string.multiple_numbers_format,
                        contact.phoneNumbers.size,
                    )
                    "$basePhoneText • $countText"
                } else {
                    basePhoneText
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectNumberSheet(
    contact: ContactSummary,
    currentSelectedNumber: String?,
    onDismiss: () -> Unit,
    onNumberSelected: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.select_default_number),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.select_default_number_desc, contact.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(contact.phoneNumbers, key = { it.number }) { phone ->
                    val isSelected = phone.number == currentSelectedNumber ||
                        (currentSelectedNumber == null && phone == contact.phoneNumbers.firstOrNull())
                    Surface(
                        onClick = { onNumberSelected(phone.number) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onNumberSelected(phone.number) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = phone.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (phone.label.isNotBlank()) {
                                    Text(
                                        text = phone.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Context.hasContactsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
