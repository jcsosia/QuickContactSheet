---
name: android-glance-widgets
description: >-
  Best practices and architectural guidelines for building Android AppWidgets using Jetpack Glance.
  Use when creating, debugging, or optimizing Glance app widgets, widget configuration activities,
  reactive state observation, and immediate widget refreshes.
---

# Android Jetpack Glance Widget Guidelines

This skill provides essential architectural patterns and runbooks for building fast, responsive Android home screen widgets using **Jetpack Glance**.

## 1. Reactive State Observation inside `provideContent`

### The Problem
`provideGlance(context, id)` is called when the Glance session is created (e.g. when first dropped on the home screen). It does **not** re-run on subsequent `update()` calls. If widget data is loaded once outside `provideContent`, `provideContent` will capture the initial/unconfigured state in its closure and fail to update.

### The Pattern
Always observe repository flows or Glance state **inside** the `provideContent` composable block:

```kotlin
class MyGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repository = MySettingsRepository.get(context)
        val initialSettings = repository.getWidgetSettings(appWidgetId)

        provideContent {
            // Reactively observe state so any DataStore/Room mutation immediately recomposes the widget UI
            val settings by repository.widgetSettingsFlow(appWidgetId).collectAsState(initial = initialSettings)
            val currentSettings = settings

            // Load heavy resources (e.g. Bitmaps) remembering keys to avoid re-decoding
            val photoProvider = remember(currentSettings?.photoUri) {
                currentSettings?.photoUri
                    ?.let { context.loadBitmapFromFileOrUri(it, maxDimensionPx = 768) }
                    ?.let(::ImageProvider)
            }

            when {
                currentSettings?.isConfigured != true -> {
                    EmptyWidgetState()
                }
                else -> {
                    ConfiguredWidgetContent(
                        settings = currentSettings,
                        photoProvider = photoProvider,
                    )
                }
            }
        }
    }
}
```

---

## 2. Widget Configuration Activity Flow

When a user finishes configuring a widget in a `WidgetConfigurationActivity`, follow this strict 4-step sequence:

```kotlin
// In your Configuration Activity / Done action:
scope.launch {
    // 1. Save configuration data to DataStore / DB and await completion
    withContext(NonCancellable + Dispatchers.IO) {
        repository.saveWidgetConfiguration(widgetId, selectedData)

        // 2. Retrieve the GlanceId mapped to this platform widgetId
        val glanceManager = GlanceAppWidgetManager(appContext)
        val glanceId = runCatching { glanceManager.getGlanceIdBy(widgetId) }.getOrNull()

        // 3. Explicitly trigger and await the widget update BEFORE finish()
        if (glanceId != null) {
            MyGlanceWidget().update(appContext, glanceId)
        } else {
            MyGlanceWidget().updateAll(appContext)
        }
    }

    // 4. Return RESULT_OK with EXTRA_APPWIDGET_ID and finish
    val resultValue = Intent().apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    }
    setResult(Activity.RESULT_OK, resultValue)
    finish()
}
```

---

## 3. Common Pitfalls & Anti-Patterns

1. **Do NOT spawn naked background coroutine delays after `finish()`**:
   When an Activity calls `finish()`, Android immediately moves the app process to a cached/frozen state. Naked coroutine delays (e.g., `CoroutineScope(Dispatchers.Default).launch { delay(...) }`) will be frozen by the OS and fail to execute until much later.
2. **Do NOT call `updateAll()` repeatedly in rapid bursts**:
   Jetpack Glance uses WorkManager `SessionWorker` under the hood. Firing multiple rapid `updateAll()` calls can cause internal session lock collisions where subsequent updates are dropped. Target the specific `glanceId` whenever `widgetId` is known.
3. **Handle Kotlin delegated property smart casting**:
   When using Compose `val settings by flow.collectAsState()`, Kotlin will prevent direct smart casting (e.g., `settings != null`). Create a local copy `val currentSettings = settings` before branching.
