package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedGlow
import com.example.ui.theme.NothingRedMuted
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusEmerald

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) NothingRed.copy(alpha = 0.65f) else GlassBorder,
        animationSpec = tween(250),
        label = "cardBorder"
    )
    val shape = RoundedCornerShape(24.dp)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            if (highlighted) GlassCardElevated else GlassCardSurface,
            GlassCardSurface.copy(alpha = 0.85f)
        )
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun DotMatrixSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteel
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusDotBadge(
    label: String,
    isActive: Boolean,
    activeColor: Color = NothingRed,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isActive) activeColor.copy(alpha = 0.14f) else GlassCardElevated)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.45f) else GlassBorder,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = pulseAlpha) else MutedSteel)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) PureWhite else SilverText
        )
    }
}

@Composable
fun BatteryRingWidget(
    label: String,
    percentage: Int?,
    modifier: Modifier = Modifier,
    ringSize: Dp = 68.dp
) {
    val clamped = percentage?.coerceIn(0, 100)
    val accent = when {
        clamped == null -> MutedSteel
        clamped <= 20 -> NothingRedGlow
        else -> StatusEmerald
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidth = 5.dp.toPx()
                drawArc(
                    color = GlassBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (clamped != null) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * (clamped / 100f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = if (clamped != null) "$clamped%" else "--",
                style = MaterialTheme.typography.labelLarge,
                color = if (clamped != null) PureWhite else MutedSteel
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SilverText
        )
    }
}

@Composable
fun NothingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    hapticsEnabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(50)
    val bgColor = when {
        !enabled -> GlassCardElevated.copy(alpha = 0.5f)
        isPrimary -> NothingRed
        else -> GlassCardElevated
    }
    val borderColor = when {
        !enabled -> GlassBorder.copy(alpha = 0.4f)
        isPrimary -> NothingRedGlow
        else -> GlassBorder
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Button) {
                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) PureWhite else MutedSteel
        )
    }
}

@Composable
fun DotMatrixDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val spacing = 10.dp.toPx()
        val y = size.height / 2f
        var x = 0f
        while (x < size.width) {
            drawCircle(
                color = GlassBorder,
                radius = 1.2.dp.toPx(),
                center = Offset(x, y)
            )
            x += spacing
        }
    }
}

@Composable
fun NothingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(if (selected) NothingRedMuted else GlassCardSurface)
            .border(1.dp, if (selected) NothingRed else GlassBorder, shape)
            .clickable(role = Role.Button) {
                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PureWhite else SilverText
        )
    }
}
