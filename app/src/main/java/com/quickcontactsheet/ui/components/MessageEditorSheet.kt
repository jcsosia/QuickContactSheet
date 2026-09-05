package com.quickcontactsheet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.quickcontactsheet.data.WidgetMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageEditorSheet(
    initialMessages: List<WidgetMessage>,
    onDismiss: () -> Unit,
    onSave: (List<WidgetMessage>) -> Unit,
) {
    val messages = remember(initialMessages) { initialMessages.toMutableStateList() }
    var draftText by remember { mutableStateOf("") }
    var editingId by remember { mutableLongStateOf(0L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Edit messages",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Drag messages to reorder them. Tap edit to change the text or delete to remove it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (messages.isEmpty()) {
                Text(
                    text = "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                )
            }

            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message text") },
                minLines = 2,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledTonalButton(
                    onClick = {
                        editingId = 0L
                        draftText = ""
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        val cleaned = draftText.trim()
                        if (cleaned.isBlank()) {
                            return@Button
                        }
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
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (editingId == 0L) "Add message" else "Update message")
                }
            }

            Button(
                onClick = { onSave(messages.filter { it.text.isNotBlank() }) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableMessageList(
    messages: SnapshotStateList<WidgetMessage>,
    onEdit: (WidgetMessage) -> Unit,
    onDelete: (WidgetMessage) -> Unit,
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        state = listState,
        modifier = Modifier.heightIn(max = 320.dp),
    ) {
        itemsIndexed(
            items = messages,
            key = { _, message -> message.id },
        ) { index, message ->
            val isDragging = draggingIndex == index
            val elevation by animateFloatAsState(if (isDragging) 12f else 0f, label = "dragElevation")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .graphicsLayer {
                        translationY = if (isDragging) draggingOffset else 0f
                        shadowElevation = elevation
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.pointerInput(messages.size, draggingIndex) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    draggingOffset = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    draggingOffset = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    draggingOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val activeIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    draggingOffset += dragAmount.y
                                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                                    val activeItem = visibleItems.firstOrNull { it.index == activeIndex }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val middle = activeItem.offset + draggingOffset + (activeItem.size / 2f)
                                    val target = visibleItems.firstOrNull { itemInfo ->
                                        itemInfo.index != activeIndex &&
                                            middle >= itemInfo.offset &&
                                            middle <= itemInfo.offset + itemInfo.size
                                    } ?: return@detectDragGesturesAfterLongPress

                                    messages.move(activeIndex, target.index)
                                    draggingIndex = target.index
                                    draggingOffset += activeItem.offset - target.offset
                                },
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Reorder",
                        )
                    }

                    Text(
                        text = message.text,
                        modifier = Modifier.weight(1f),
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

private fun <T> SnapshotStateList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) {
        return
    }
    val item = removeAt(fromIndex)
    add(toIndex, item)
}
