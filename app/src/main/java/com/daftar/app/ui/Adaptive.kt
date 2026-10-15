package com.daftar.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

/**
 * Where the fold is, in window pixels, and how the device is bent.
 * Read from androidx.window's FoldingFeature, so it is live: it changes as the device opens.
 */
class Hinge(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    /** The hinge runs top to bottom (book posture) rather than left to right (tabletop). */
    val vertical: Boolean,
    val halfOpen: Boolean,
)

/**
 * The one decision the whole app takes from the live window: the cover experience (compact width,
 * under 600 dp) or the inner experience (medium and expanded width). It never looks at the device
 * model, so a tablet or a phone in split-screen gets the same treatment as a foldable.
 */
class WindowLayout(val widthClass: WindowWidthSizeClass, val hinge: Hinge?) {
    val expanded: Boolean get() = widthClass != WindowWidthSizeClass.COMPACT

    /** Half-open with the hinge across the middle: content goes above and below it, never across. */
    val tabletop: Boolean get() = hinge != null && hinge.halfOpen && !hinge.vertical

    /** A hinge running top to bottom: two-pane layouts put their divider on it. */
    val bookHinge: Hinge? get() = hinge?.takeIf { it.vertical }
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout(WindowWidthSizeClass.COMPACT, null) }

private fun Context.activityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Call once at the top of the app; everything below reads [LocalWindowLayout]. */
@Composable
fun rememberWindowLayout(): WindowLayout {
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val activity = LocalContext.current.activityOrNull()
    val layoutInfo by remember(activity) {
        if (activity == null) {
            kotlinx.coroutines.flow.emptyFlow()
        } else {
            WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
        }
    }.collectAsState(initial = null as WindowLayoutInfo?)
    val fold = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()
    val hinge = fold?.let {
        Hinge(
            left = it.bounds.left.toFloat(),
            top = it.bounds.top.toFloat(),
            right = it.bounds.right.toFloat(),
            bottom = it.bounds.bottom.toFloat(),
            vertical = it.orientation == FoldingFeature.Orientation.VERTICAL,
            halfOpen = it.state == FoldingFeature.State.HALF_OPENED,
        )
    }
    return remember(widthClass, hinge?.left, hinge?.top, hinge?.right, hinge?.bottom, hinge?.vertical, hinge?.halfOpen) {
        WindowLayout(widthClass, hinge)
    }
}

/**
 * Two panes side by side. The divider sits on the fold when there is a vertical hinge, otherwise
 * the first pane takes [fraction] of the width. In Arabic the first pane is on the right.
 */
@Composable
fun TwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    fraction: Float = 0.4f,
    minFirst: Dp = 320.dp,
    maxFirst: Dp = 440.dp,
    gap: Dp = 16.dp,
) {
    val layout = LocalWindowLayout.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var windowX by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { windowX = it.positionInWindow().x }) {
        val total = maxWidth
        val hinge = layout.bookHinge
        var firstWidth = (total * fraction).coerceIn(minFirst, maxOf(minFirst, minOf(maxFirst, total - minFirst)))
        var divider = gap
        if (hinge != null) {
            val totalPx = with(density) { total.toPx() }
            // the pane that starts the row ends where the hinge begins
            val startEdgePx = if (rtl) (windowX + totalPx) - hinge.right else hinge.left - windowX
            val hingePx = hinge.right - hinge.left
            if (startEdgePx > 0f && startEdgePx < totalPx) {
                firstWidth = with(density) { startEdgePx.toDp() }
                divider = with(density) { hingePx.toDp() } + gap
            }
        }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(firstWidth).fillMaxHeight()) { first() }
            Spacer(Modifier.width(divider))
            Box(Modifier.weight(1f).fillMaxHeight()) { second() }
        }
    }
}

/**
 * On a half-open device lying flat (tabletop), [top] goes above the hinge and [bottom] below it;
 * nothing is drawn on the hinge itself. In any other posture only [whole] is shown.
 */
@Composable
fun TabletopSplit(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    whole: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = LocalWindowLayout.current
    val hinge = layout.hinge
    if (!layout.tabletop || hinge == null) {
        whole()
        return
    }
    val density = LocalDensity.current
    var windowY by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { windowY = it.positionInWindow().y }) {
        val totalPx = with(density) { maxHeight.toPx() }
        val topPx = (hinge.top - windowY).coerceIn(0f, totalPx)
        val belowPx = (hinge.bottom - windowY).coerceIn(0f, totalPx)
        if (topPx < 120f || totalPx - belowPx < 120f) {
            // the hinge is at the very edge of this area: nothing sensible to split
            whole()
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(with(density) { topPx.toDp() })) { top() }
                Spacer(Modifier.height(with(density) { (belowPx - topPx).toDp() }))
                Box(Modifier.fillMaxWidth().weight(1f)) { bottom() }
            }
        }
    }
}

/** Whether the inner (unfolded, tablet) experience is on. */
@Composable
fun isExpanded(): Boolean = LocalWindowLayout.current.expanded

/** Keeps wide screens readable: content narrower than the window, centred. */
@Composable
fun contentMaxWidth(): Dp = if (isExpanded()) 720.dp else Dp.Unspecified

@Composable
fun paneBackground() = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.35f)
