package com.quickcontactsheet.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.R
import com.quickcontactsheet.data.BackupRepository
import com.quickcontactsheet.data.ContactPreset
import com.quickcontactsheet.data.WidgetMessage
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageEditorSheet(
    initialMessages: List<WidgetMessage>,
    onDismiss: () -> Unit,
    onSave: (List<WidgetMessage>) -> Unit,
    contactName: String? = null,
    savedPresets: List<WidgetMessage> = emptyList(),
) {
    val context = LocalContext.current
    val backupRepo = remember(context) { BackupRepository.get(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages = remember(initialMessages) { initialMessages.toMutableStateList() }
    var draftText by remember { mutableStateOf("") }
    var editingId by remember { mutableLongStateOf(0L) }
    var menuExpanded by remember { mutableStateOf(false) }

    val exportMessagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val currentList = messages.toList()
                val preset = ContactPreset(
                    displayName = contactName?.ifBlank { null } ?: "Quick Messages",
                    messages = currentList,
                )
                val result = backupRepo.exportToUri(uri, listOf(preset))
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.export_success, 1), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importMessagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = backupRepo.importFromUri(uri)
                val backupData = result.getOrNull()
                val importedMsgs = backupData?.presets?.flatMap { it.messages }
                if (!importedMsgs.isNullOrEmpty()) {
                    messages.addAll(importedMsgs)
                    Toast.makeText(context, context.getString(R.string.messages_saved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.invalid_backup_file), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun commitDraft() {
        val cleaned = draftText.trim()
        if (cleaned.isBlank()) return
        if (editingId == 0L) {
            messages += WidgetMessage(
                id = System.currentTimeMillis(),
                text = cleaned,
            )
        } else {
            val index = messages.indexOfFirst { it.id == editingId }
            if (index >= 0) {
                messages[index] = messages[index].copy(text = cleaned)
            }
        }
        editingId = 0L
        draftText = ""
    }

    fun handleSave() {
        val finalMessages = messages.toMutableList()
        val cleaned = draftText.trim()
        if (cleaned.isNotBlank()) {
            if (editingId == 0L) {
                finalMessages.add(
                    WidgetMessage(
                        id = System.currentTimeMillis(),
                        text = cleaned,
                    )
                )
            } else {
                val index = finalMessages.indexOfFirst { it.id == editingId }
                if (index >= 0) {
                    finalMessages[index] = finalMessages[index].copy(text = cleaned)
                }
            }
        }
        onSave(finalMessages.filter { it.text.isNotBlank() })
    }

    val preventSheetDismissScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y > 0f) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                return if (available.y > 0f) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        ),
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .nestedScroll(preventSheetDismissScrollConnection)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                    }
                },
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Drag to reorder • Tap to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (savedPresets.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.restore_from_saved)) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Restore, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        messages.clear()
                                        messages.addAll(savedPresets)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_messages)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    val safeName = contactName?.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        ?.ifBlank { "quick_messages" } ?: "quick_messages"
                                    exportMessagesLauncher.launch("${safeName}_messages.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_messages)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    importMessagesLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                            )
                        }
                    }

                    Button(
                        onClick = { handleSave() },
                    ) {
                        Text("Done")
                    }
                }
            }

            // Message List / Empty State
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "No quick messages yet.\nAdd presets like \"On my way!\" or \"Call you in 5\" for 1-tap texting from your widget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (savedPresets.isNotEmpty()) {
                        val nameStr = contactName?.ifBlank { null } ?: "contact"
                        AssistChip(
                            onClick = {
                                messages.clear()
                                messages.addAll(savedPresets)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(
                                        R.string.restored_messages_for,
                                        savedPresets.size,
                                        nameStr,
                                    ),
                                )
                            },
                        )
                    }
                }
            } else {

                ReorderableMessageList(
                    messages = messages,
                    onEdit = { message ->
                        editingId = message.id
                        draftText = message.text
                    },
                    onDelete = { message ->
                        messages.removeAll { it.id == message.id }
                        if (editingId == message.id) {
                            editingId = 0L
                            draftText = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(horizontal = 20.dp),
                )
            }

            // Docked Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (editingId != 0L) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = "Editing message",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable {
                                    editingId = 0L
                                    draftText = ""
                                },
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = {
                                Text(if (editingId == 0L) "Add a quick message…" else "Edit message…")
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { commitDraft() },
                            ),
                            trailingIcon = if (draftText.isNotBlank()) {
                                {
                                    IconButton(onClick = { draftText = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear",
                                        )
                                    }
                                }
                            } else null,
                        )

                        FilledIconButton(
                            onClick = { commitDraft() },
                            enabled = draftText.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (editingId == 0L) Icons.Rounded.Add else Icons.Rounded.Check,
                                contentDescription = if (editingId == 0L) "Add message" else "Update message",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableMessageList(
    messages: SnapshotStateList<WidgetMessage>,
    onEdit: (WidgetMessage) -> Unit,
    onDelete: (WidgetMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        messages.move(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
        modifier = modifier,
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            ReorderableItem(reorderableLazyListState, key = message.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
                val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragScale")

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isDragging) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Reorder",
                                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = message.text,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEdit(message) },
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        IconButton(onClick = { onEdit(message) }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit",
                            )
                        }
                        IconButton(onClick = { onDelete(message) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun <T> SnapshotStateList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) {
        return
    }
    val item = removeAt(fromIndex)
    add(toIndex, item)
}
