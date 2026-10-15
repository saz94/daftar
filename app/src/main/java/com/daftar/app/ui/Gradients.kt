package com.daftar.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Every screen sits on this: the theme background with a warm glow spilling from the top. */
@Composable
fun GlowBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val money = LocalMoneyColors.current
    Box(
        modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(money.glow, Color.Transparent),
                        center = Offset(size.width * 0.15f, -size.height * 0.02f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(money.glow2, Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.05f),
                        radius = size.width * 0.7f,
                    ),
                )
            },
    ) {
        // text drawn straight on this background must use the theme's text colour, not black
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { content() }
    }
}

/** Small rounded pill: gradient when chosen, quiet when not. Like the day pills in the reference. */
@Composable
fun GradientChip(label: String, selected: Boolean, onClick: () -> Unit, warm: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    if (warm) WarmGradient else BrandGradient
                } else {
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                },
            )
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) OnGradient else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** A filled gradient circle with a soft halo, used for the one headline figure on a screen. */
@Composable
fun GlowOrb(size: Dp, modifier: Modifier = Modifier, warm: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(size + 36.dp), contentAlignment = Alignment.Center) {
        // the halo: a soft radial light in the gradient's colours, fading out past the circle's edge
        val haloColor = if (warm) Pink else Teal
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    0.55f to haloColor.copy(alpha = 0.45f),
                    1f to Color.Transparent,
                    center = center,
                    radius = this.size.minDimension / 2,
                ),
            )
        }
        Box(
            Modifier.size(size).clip(CircleShape).background(
                Brush.linearGradient(
                    colors = if (warm) listOf(Pink, Orange) else listOf(Lime, Teal),
                    start = Offset(0f, 0f),
                    end = Offset(size.value * 3f, size.value * 3f),
                ),
            ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Progress ring drawn in the gradient, with a faint track. [fraction] 0..1.
 * Like the day rings in the reference's calendar.
 */
@Composable
fun GradientRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    warm: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val colors = if (warm) listOf(Pink, Orange, Pink) else listOf(Teal, Lime, Teal)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = thickness.toPx()
            val arc = Size(size.width - t, size.height - t)
            val tl = Offset(t / 2, t / 2)
            drawArc(track, 0f, 360f, false, tl, arc, style = Stroke(t))
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) {
                rotate(-90f) {
                    drawArc(Brush.sweepGradient(colors), 0f, 360f * f, false, tl, arc, style = Stroke(t, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** A wide gradient card, like the "Read" and "Go for a walk" cards in the reference. */
@Composable
fun GradientCard(
    title: String,
    detail: String,
    amount: String,
    icon: ImageVector,
    warm: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Row(
            Modifier
                .background(if (warm) WarmGradient else BrandGradient)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = OnGradient, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = OnGradient.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(6.dp))
                Text(LTR_MARK + amount, style = MaterialTheme.typography.titleLarge.tabular(), color = OnGradient, maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            // the translucent square from the reference's cards, holding the icon
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = OnGradient, modifier = Modifier.size(22.dp))
            }
        }
    }
}
