package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.model.AncControlCapability
import com.example.model.AncMode
import com.example.model.EarbudsDeviceState
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusDotBadge
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedMuted
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCyan
import com.example.ui.theme.StatusEmerald
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AncControlScreen(
    deviceState: EarbudsDeviceState,
    hapticsEnabled: Boolean,
    onSelectMode: (AncMode, Int) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenCompanionApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val supportsDirectControl = deviceState.ancCapability == AncControlCapability.NATIVE_BLE_GATT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DotMatrixSectionHeader(
                    title = "NOISE CONTROL",
                    subtitle = deviceState.connectedDeviceName.uppercase(),
                    trailing = {
                        StatusDotBadge(
                            label = if (supportsDirectControl) "DIRECT GATT" else "COMPANION LINK",
                            isActive = deviceState.isConnected,
                            activeColor = if (supportsDirectControl) StatusEmerald else StatusAmber
                        )
                    }
                )
            }

            // Large Circular Nothing-style ANC Visualizer
            item {
                GlassCard(highlighted = deviceState.ancMode != AncMode.ANC_OFF) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AncCircularRadar(
                            mode = deviceState.ancMode,
                            level = deviceState.ancLevel,
                            isConnected = deviceState.isConnected
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = deviceState.ancMode.code,
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (supportsDirectControl) {
                                    "HARDWARE ACTIVE"
                                } else if (deviceState.isConnected) {
                                    "PROTOCOL GATED"
                                } else {
                                    "DISCONNECTED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (deviceState.ancMode == AncMode.ANC_ON) NothingRed else SilverText
                            )
                        }
                    }
                }
            }

            // Honest Hardware Protocol Disclosure Card
            item {
                GlassCard(highlighted = !supportsDirectControl && deviceState.isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Hardware protocol notice",
                            tint = if (supportsDirectControl) StatusEmerald else NothingRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = deviceState.ancCapability.badge,
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = deviceState.ancCapability.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (deviceState.isHeyMelodyInstalled) {
                            NothingPillButton(
                                text = "OPEN HEYMELODY",
                                onClick = onOpenCompanionApp,
                                isPrimary = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_companion_button"),
                                hapticsEnabled = hapticsEnabled
                            )
                        }
                        NothingPillButton(
                            text = "BT DEVICE SETTINGS",
                            onClick = onOpenBluetoothSettings,
                            isPrimary = !deviceState.isHeyMelodyInstalled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anc_bt_settings_button"),
                            hapticsEnabled = hapticsEnabled
                        )
                    }
                }
            }

            // ANC Mode Selector Matrix
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "MODE SELECTION",
                        subtitle = if (supportsDirectControl) {
                            "DIRECT HARDWARE SWITCHING"
                        } else {
                            "PREVIEW / REQUIRES OFFICIAL COMPANION"
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val modes = listOf(
                        AncMode.ANC_ON to Icons.Rounded.NoiseAware,
                        AncMode.TRANSPARENCY to Icons.Rounded.Hearing,
                        AncMode.ADAPTIVE to Icons.Rounded.AutoAwesome,
                        AncMode.ANC_OFF to Icons.Rounded.NoiseControlOff
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        modes.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (mode, icon) ->
                                    AncModeTile(
                                        mode = mode,
                                        icon = icon,
                                        selected = deviceState.ancMode == mode,
                                        onClick = {
                                            if (hapticsEnabled) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            onSelectMode(mode, deviceState.ancLevel)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("anc_mode_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // Noise control depth slider — shown only when hardware GATT ANC is supported and ANC_ON is active
                    if (supportsDirectControl && deviceState.ancMode == AncMode.ANC_ON) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val levelLabels = listOf("MILD", "MODERATE", "MAX")
                        Text(
                            text = "CANCELLATION DEPTH: ${levelLabels.getOrElse(deviceState.ancLevel) { "MAX" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                        Slider(
                            value = deviceState.ancLevel.toFloat(),
                            onValueChange = { raw ->
                                onSelectMode(deviceState.ancMode, raw.roundToInt().coerceIn(0, 2))
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = NothingRed,
                                activeTrackColor = NothingRed,
                                inactiveTrackColor = GlassBorder
                            ),
                            modifier = Modifier.testTag("anc_depth_slider")
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlassCardElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MutedSteel,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Noise depth slider is hidden because OnePlus Nord Buds 3 Pro does not expose open GATT ANC depth characteristics.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AncModeTile(
    mode: AncMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) NothingRed else GlassBorder,
        animationSpec = tween(220),
        label = "ancTileBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) NothingRedMuted else GlassCardSurface,
        animationSpec = tween(220),
        label = "ancTileBg"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.title,
                tint = if (selected) NothingRed else SilverText
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) NothingRed else GlassBorder)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = mode.title,
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = mode.description,
            style = MaterialTheme.typography.labelSmall,
            color = SilverText
        )
    }
}

@Composable
private fun AncCircularRadar(
    mode: AncMode,
    level: Int,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ancRadar")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val targetSweep by animateFloatAsState(
        targetValue = when (mode) {
            AncMode.ANC_ON -> 360f
            AncMode.ADAPTIVE -> 270f
            AncMode.TRANSPARENCY -> 180f
            AncMode.ANC_OFF -> 60f
        },
        animationSpec = spring(dampingRatio = 0.72f),
        label = "sweepAngle"
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            !isConnected -> MutedSteel
            mode == AncMode.ANC_ON -> NothingRed
            mode == AncMode.TRANSPARENCY -> StatusCyan
            mode == AncMode.ADAPTIVE -> StatusEmerald
            else -> SilverText
        },
        animationSpec = tween(300),
        label = "radarColor"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.36f

        // Dot-matrix outer ring
        val dotCount = 36
        for (i in 0 until dotCount) {
            val angleRad = Math.toRadians((i * (360.0 / dotCount)) - 90.0)
            val r = baseRadius * 1.24f
            val x = center.x + (r * cos(angleRad)).toFloat()
            val y = center.y + (r * sin(angleRad)).toFloat()
            val activeDot = (i.toFloat() / dotCount) * 360f <= targetSweep
            drawCircle(
                color = if (activeDot) ringColor else GlassBorder,
                radius = if (activeDot) 2.5.dp.toPx() else 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Inner breathing acoustic wave when active
        if (mode != AncMode.ANC_OFF) {
            val pulseRadius = baseRadius * (0.78f + 0.36f * wavePhase)
            drawCircle(
                color = ringColor.copy(alpha = (1f - wavePhase) * 0.55f),
                radius = pulseRadius,
                center = center,
                style = Stroke(width = (1.5f + level).dp.toPx())
            )
        }

        drawCircle(
            color = GlassCardElevated,
            radius = baseRadius * 0.74f,
            center = center
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.7f),
            radius = baseRadius * 0.74f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
