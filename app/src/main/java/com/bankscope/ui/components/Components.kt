package com.bankscope.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-style "Large Title" header that sits above screen content: bold oversized title
 * with a muted subtitle directly beneath, matching UINavigationBar's large-title mode.
 */
@Composable
fun LargeTitleHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** An iOS "grouped" inset card: rounded container holding a column of [GroupedRow]s. */
@Composable
fun GroupedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

/** A single row inside a [GroupedCard], with an optional bottom divider inset from the edge. */
@Composable
fun GroupedRow(
    showDivider: Boolean = true,
    verticalPadding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = verticalPadding)) { content() }
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/** Circular initials avatar used for institutions/accounts, Apple-contacts style. */
@Composable
fun InitialsAvatar(text: String, tint: Color = MaterialTheme.colorScheme.primary, size: Dp = 44.dp) {
    Surface(shape = CircleShape, color = tint.copy(alpha = 0.16f), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = tint)
        }
    }
}

/**
 * A pill-shaped segmented control with a sliding selection highlight — the Compose
 * equivalent of UISegmentedControl, used in place of Material's flat tab row.
 */
@Composable
fun PillSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Applies a gentle spring-based press scale (iOS-style "squishy" button feedback)
 * without needing a ripple; pair with `clickable(interactionSource = ...)`.
 */
@Composable
fun Modifier.springPressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Soft, low-opacity shadow that reads closer to Apple's card elevation than Material's default. */
fun Modifier.appleCardShadow(radius: Dp = 14.dp): Modifier =
    this.shadow(
        elevation = radius,
        shape = RoundedCornerShape(18.dp),
        ambientColor = Color.Black.copy(alpha = 0.06f),
        spotColor = Color.Black.copy(alpha = 0.10f)
    )
