package com.quickcontactsheet.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quickcontactsheet.R
import com.quickcontactsheet.data.WidgetSettings
import com.quickcontactsheet.data.WidgetSettingsRepository
import com.quickcontactsheet.data.loadBitmapFromFileOrUri

private val WidgetSurface = ColorProvider(Color(0xFF1E2328))
private val WidgetSurfaceAlt = ColorProvider(Color(0xFF262C33))
private val WidgetOnSurface = ColorProvider(Color(0xFFF2F5F7))
private val WidgetButtonTranslucent = ColorProvider(Color(0x9920252B))
private val WidgetAccent = ColorProvider(Color(0xFF8EC5FF))

class QuickContactSheetWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: androidx.glance.GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val settings = WidgetSettingsRepository.get(context).getWidgetSettings(appWidgetId)
        val photoProvider = settings?.photoUri
            ?.let { context.loadBitmapFromFileOrUri(it, maxDimensionPx = 768) }
            ?.let(::ImageProvider)

        provideContent {
            val sizeBucket = WidgetSizeBucket.from(LocalSize.current)
            when {
                settings?.isConfigured != true -> {
                    EmptyWidgetState()
                }

                sizeBucket == WidgetSizeBucket.SingleColumn -> {
                    PhotoOnlyWidget(
                        title = settings.displayName,
                        photoProvider = photoProvider,
                    )
                }

                sizeBucket == WidgetSizeBucket.TwoByOne -> {
                    PillWidget(
                        settings = settings,
                        photoProvider = photoProvider,
                        showName = false,
                        singleLineName = false,
                    )
                }

                sizeBucket == WidgetSizeBucket.ThreeByOne -> {
                    PillWidget(
                        settings = settings,
                        photoProvider = photoProvider,
                        showName = true,
                        singleLineName = false,
                    )
                }

                sizeBucket == WidgetSizeBucket.FourPlusByOne -> {
                    PillWidget(
                        settings = settings,
                        photoProvider = photoProvider,
                        showName = true,
                        singleLineName = true,
                    )
                }

                sizeBucket == WidgetSizeBucket.TwoByTwo || sizeBucket == WidgetSizeBucket.Large -> {
                    LargePhotoWidget(
                        settings = settings,
                        photoProvider = photoProvider,
                    )
                }

                else -> {
                    LargePhotoWidget(
                        settings = settings,
                        photoProvider = photoProvider,
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyWidgetState() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(WidgetSurface)
            .clickable(actionRunCallback<OpenQuickActionsAction>())
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tap to set up",
                style = TextStyle(
                    color = WidgetOnSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                ),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "Choose a contact",
                style = TextStyle(
                    color = WidgetAccent,
                    fontSize = 13.sp,
                ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun PhotoOnlyWidget(
    title: String,
    photoProvider: ImageProvider?,
) {
    val size = LocalSize.current
    val circleSize = minOf(size.width, size.height)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<OpenQuickActionsAction>()),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ContactCircle(
            title = title,
            photoProvider = photoProvider,
            modifier = GlanceModifier.size(circleSize),
        )
    }
}

@androidx.compose.runtime.Composable
private fun PillWidget(
    settings: WidgetSettings,
    photoProvider: ImageProvider?,
    showName: Boolean,
    singleLineName: Boolean,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(32.dp)
            .background(WidgetSurface)
            .clickable(actionRunCallback<OpenQuickActionsAction>())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .clickable(actionRunCallback<OpenQuickActionsAction>()),
        ) {
            ContactCircle(
                title = settings.displayName,
                photoProvider = photoProvider,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }

        if (showName) {
            val horizontalGap = if (singleLineName) 12.dp else 10.dp
            Spacer(modifier = GlanceModifier.width(horizontalGap))
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionRunCallback<OpenQuickActionsAction>()),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (singleLineName) {
                    Text(
                        text = settings.displayName,
                        maxLines = 1,
                        style = TextStyle(
                            color = WidgetOnSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        ),
                    )
                } else {
                    val nameText = if (settings.displayName.contains(" ")) {
                        settings.displayName.replaceFirst(" ", "\n")
                    } else {
                        settings.displayName
                    }
                    Text(
                        text = nameText,
                        maxLines = 2,
                        style = TextStyle(
                            color = WidgetOnSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        val buttonGap = if (singleLineName) 16.dp else if (showName) 12.dp else 16.dp
        val endGap = if (singleLineName) 8.dp else if (showName) 4.dp else 6.dp

        Spacer(modifier = GlanceModifier.width(8.dp))
        PillActionButton(
            iconRes = R.drawable.ic_call,
            action = actionRunCallback<DialContactAction>(),
        )
        Spacer(modifier = GlanceModifier.width(buttonGap))
        PillActionButton(
            iconRes = R.drawable.ic_chat,
            action = actionRunCallback<TextContactAction>(),
        )
        Spacer(modifier = GlanceModifier.width(endGap))
    }
}

@androidx.compose.runtime.Composable
private fun LargePhotoWidget(
    settings: WidgetSettings,
    photoProvider: ImageProvider?,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(WidgetSurface)
            .clickable(actionRunCallback<OpenQuickActionsAction>()),
    ) {
        if (photoProvider != null) {
            Image(
                provider = photoProvider,
                contentDescription = settings.displayName,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = settings.displayName.firstOrNull()?.uppercase() ?: "C",
                    style = TextStyle(
                        color = WidgetOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                    ),
                )
            }
        }

        // Subtle dark gradient scrim at the top for clean text legibility
        Image(
            provider = ImageProvider(R.drawable.widget_gradient_scrim_top),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(90.dp),
        )

        // Contact Name in top-left
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = settings.displayName,
                maxLines = 2,
                style = TextStyle(
                    color = WidgetOnSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                ),
            )
        }

        // Action buttons in bottom-left
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 14.dp, bottom = 14.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LargeActionButton(
                    iconRes = R.drawable.ic_call,
                    action = actionRunCallback<DialContactAction>(),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                LargeActionButton(
                    iconRes = R.drawable.ic_chat,
                    action = actionRunCallback<TextContactAction>(),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ContactCircle(
    title: String,
    photoProvider: ImageProvider?,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .cornerRadius(999.dp)
            .background(WidgetSurfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        if (photoProvider != null) {
            Image(
                provider = photoProvider,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "C",
                style = TextStyle(
                    color = WidgetOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun PillActionButton(
    iconRes: Int,
    action: Action,
) {
    Box(
        modifier = GlanceModifier
            .size(42.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun LargeActionButton(
    iconRes: Int,
    action: Action,
) {
    Box(
        modifier = GlanceModifier
            .size(46.dp)
            .cornerRadius(999.dp)
            .background(WidgetButtonTranslucent)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(22.dp),
        )
    }
}
