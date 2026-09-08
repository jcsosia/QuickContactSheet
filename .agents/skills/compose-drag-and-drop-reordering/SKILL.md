---
name: compose-drag-and-drop-reordering
description: >-
  Best practices and reference guide for implementing drag-to-reorder in Jetpack Compose
  using the sh.calvin.reorderable library. Use whenever building or debugging reorderable
  LazyColumns, LazyRows, or LazyGrids with auto-scrolling and drag handles.
---

# Jetpack Compose Drag-and-Drop Reordering Guidelines

This skill provides the standard implementation pattern for drag-and-drop list reordering in Jetpack Compose using the battle-tested **`sh.calvin.reorderable`** library.

---

## 1. Why `sh.calvin.reorderable`?

Custom/DIY drag-to-reorder implementations in Jetpack Compose encounter severe edge cases:
- **Item Disposal**: When `LazyColumn` scrolls, items whose layout slots leave the viewport are disposed, cancelling item-level `pointerInput` and prematurely dropping the drag.
- **Viewport Flickering**: Manual offset compensation (`draggingOffset += consumed`) desynchronizes with Compose's frame layout pass, causing frame-by-frame visual jitter.
- **Edge Stalls**: Items outside `visibleItemsInfo` cause manual boundary swaps to fail when auto-scrolling at viewport edges.

`sh.calvin.reorderable` natively resolves these by pinning active items in memory, synchronizing auto-scrolling with Compose's layout pipeline, and providing smooth item placement animations.

---

## 2. Dependency Setup

### `gradle/libs.versions.toml`
```toml
[versions]
reorderable = "2.4.3"

[libraries]
reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }
```

### `app/build.gradle.kts`
```kotlin
dependencies {
    implementation(libs.reorderable)
}
```

---

## 3. Standard Implementation Pattern

```kotlin
@Composable
fun ReorderableList(
    items: SnapshotStateList<MyItem>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        items.move(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(
            items = items,
            key = { it.id }, // Stable unique key is mandatory
        ) { item ->
            ReorderableItem(reorderableState, key = item.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
                val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragScale")

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        // Immediate Drag Handle
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
                            text = item.title,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun <T> SnapshotStateList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}
```

---

## 4. Key Rules & Best Practices

1. **Stable Keys**: Always use stable, unique keys (`key = { it.id }`) in both `items(...)` and `ReorderableItem(..., key = ...)`.
2. **Immediate vs Long-Press Drag**:
   - Use `Modifier.draggableHandle(...)` on a dedicated handle icon for immediate 1-touch drag response.
   - Use `Modifier.longPressDraggableHandle(...)` if the entire card surface is used to initiate drag, avoiding conflicts with tap-to-select or scrolling.
3. **Haptic Feedback**: Trigger haptic feedback on `onDragStarted` inside `draggableHandle` and on item swaps inside `onMove`.
