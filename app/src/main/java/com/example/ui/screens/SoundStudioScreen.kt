package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.model.AppPreferencesState
import com.example.model.AudioEngineState
import com.example.model.EqPreset
import com.example.model.PresetCatalog
import com.example.model.SoundProfileMode
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.NothingChip
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusDotBadge
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedGlow
import com.example.ui.theme.NothingRedMuted
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@Composable
fun SoundStudioScreen(
    audioState: AudioEngineState,
    prefsState: AppPreferencesState,
    onToggleEngine: (Boolean) -> Unit,
    onToggleLimiter: (Boolean) -> Unit,
    onToggleTestSignal: () -> Unit,
    onSelectMode: (SoundProfileMode) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBassChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    onVocalChange: (Float) -> Unit,
    onClarityChange: (Float) -> Unit,
    onVolumeEnhanceChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onSpatialChange: (Float) -> Unit,
    onResetFlat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var customPresetName by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = GlassCardElevated,
            titleContentColor = PureWhite,
            textContentColor = SilverText,
            title = { Text("SAVE CUSTOM PRESET", style = MaterialTheme.typography.labelLarge) },
            text = {
                OutlinedTextField(
                    value = customPresetName,
                    onValueChange = { customPresetName = it.take(24) },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedLabelColor = NothingRed,
                        unfocusedLabelColor = SilverText
                    ),
                    modifier = Modifier.testTag("custom_preset_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPresetName.isNotBlank()) {
                            onSaveCustomPreset(customPresetName)
                            customPresetName = ""
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_preset_button")
                ) {
                    Text("SAVE", color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = SilverText)
                }
            }
        )
    }

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
            // Header + Master Toggle
            item {
                GlassCard(highlighted = audioState.isEngineEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ULTRA SOUND STUDIO",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                            Text(
                                text = audioState.engineStatusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                        Switch(
                            checked = audioState.isEngineEnabled,
                            onCheckedChange = {
                                if (prefsState.hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onToggleEngine(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = NothingRed,
                                uncheckedThumbColor = SilverText,
                                uncheckedTrackColor = GlassCardElevated
                            ),
                            modifier = Modifier.testTag("studio_engine_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingPillButton(
                            text = if (audioState.isTestTonePlaying) "STOP TEST SIGNAL" else "AUDITION TEST TONE",
                            onClick = onToggleTestSignal,
                            isPrimary = audioState.isTestTonePlaying,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audition_test_tone_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                        IconButton(
                            onClick = {
                                if (prefsState.hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onResetFlat()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .background(GlassCardElevated)
                                .border(1.dp, GlassBorder, RoundedCornerShape(50))
                                .testTag("reset_eq_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset equalizer to flat",
                                tint = PureWhite
                            )
                        }
                    }
                }
            }

            // Safe Listening Warning Banner
            if (audioState.isSafeListeningWarning) {
                item {
                    GlassCard(highlighted = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Safe listening alert",
                                tint = StatusAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SAFE LISTENING ALERT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StatusAmber
                                )
                                Text(
                                    text = "System volume (${audioState.currentStreamVolumePct}%) + DSP gain (+${"%.1f".format(audioState.volumeEnhanceDb)} dB) exceeds your ${prefsState.safeListeningThresholdPct}% threshold. Prolonged high SPL may cause hearing fatigue.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }

            // Acoustic Mode (Music / Cinema / Gaming) + Built-in & Custom Presets
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "LISTENING MODES & PRESETS",
                        trailing = {
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_save_preset_dialog")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Save custom preset",
                                    tint = NothingRed
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundProfileMode.entries.forEach { mode ->
                            NothingChip(
                                label = mode.label,
                                selected = audioState.soundProfileMode == mode,
                                onClick = { onSelectMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sound_mode_${mode.name.lowercase()}"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PRO TUNING PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteel
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetCatalog.builtInPresets.forEach { preset ->
                            NothingChip(
                                label = preset.name,
                                selected = audioState.activePresetId == preset.id,
                                onClick = { onSelectPreset(preset) },
                                modifier = Modifier.testTag("preset_${preset.id}"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }
                    }

                    if (prefsState.customPresets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SAVED CUSTOM PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            prefsState.customPresets.forEach { custom ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (audioState.activePresetId == custom.id) NothingRedMuted else GlassCardElevated
                                        )
                                        .border(
                                            1.dp,
                                            if (audioState.activePresetId == custom.id) NothingRed else GlassBorder,
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { onSelectPreset(custom) }
                                        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = custom.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PureWhite
                                    )
                                    IconButton(
                                        onClick = { onDeleteCustomPreset(custom.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete preset ${custom.name}",
                                            tint = SilverText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 10-Band Equalizer + Live Frequency Curve
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "10-BAND PARAMETRIC EQ",
                        subtitle = "-12 dB TO +12 dB · REAL-TIME DSP",
                        trailing = {
                            StatusDotBadge(
                                label = if (audioState.isLimiterActive) "LIMITER ON" else "LIMITER OFF",
                                isActive = audioState.isLimiterActive,
                                activeColor = StatusEmerald
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live frequency spline preview
                    EqFrequencyCurveCanvas(bandsDb = audioState.bandsDb)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 10 Vertical EQ Sliders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetCatalog.freqLabels.forEachIndexed { idx, freq ->
                            val gain = audioState.bandsDb.getOrElse(idx) { 0f }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(34.dp)
                            ) {
                                Text(
                                    text = "${if (gain > 0f) "+" else ""}${gain.roundToInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (gain != 0f) NothingRed else SilverText
                                )
                                VerticalSlider(
                                    value = gain,
                                    onValueChange = { onBandChange(idx, it) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier
                                        .height(140.dp)
                                        .testTag("eq_band_$idx")
                                )
                                Text(
                                    text = freq,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteel
                                )
                            }
                        }
                    }
                }
            }

            // Acoustic Enhancement & Limiter Sliders
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "ACOUSTIC ENHANCEMENT",
                        subtitle = "REAL ANDROID AUDIOFX PROCESSING"
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    EnhancementSliderRow(
                        label = "VOLUME ENHANCEMENT",
                        valueLabel = "+${"%.1f".format(audioState.volumeEnhanceDb)} dB",
                        value = audioState.volumeEnhanceDb,
                        valueRange = 0f..10f,
                        onValueChange = onVolumeEnhanceChange,
                        helperText = "Software LoudnessEnhancer gain protected by Soft Limiter. Cannot exceed physical hardware DAC ceiling.",
                        testTag = "volume_enhance_slider"
                    )
                    EnhancementSliderRow(
                        label = "DEEP BASS BOOST",
                        valueLabel = "${(audioState.bassBoost * 100).roundToInt()}%",
                        value = audioState.bassBoost,
                        valueRange = 0f..1f,
                        onValueChange = onBassChange,
                        testTag = "bass_boost_slider"
                    )
                    EnhancementSliderRow(
                        label = "TREBLE ENHANCEMENT",
                        valueLabel = "${(audioState.trebleBoost * 100).roundToInt()}%",
                        value = audioState.trebleBoost,
                        valueRange = 0f..1f,
                        onValueChange = onTrebleChange,
                        testTag = "treble_boost_slider"
                    )
                    EnhancementSliderRow(
                        label = "VOCAL BOOST",
                        valueLabel = "${(audioState.vocalBoost * 100).roundToInt()}%",
                        value = audioState.vocalBoost,
                        valueRange = 0f..1f,
                        onValueChange = onVocalChange,
                        testTag = "vocal_boost_slider"
                    )
                    EnhancementSliderRow(
                        label = "AUDIO CLARITY",
                        valueLabel = "${(audioState.clarity * 100).roundToInt()}%",
                        value = audioState.clarity,
                        valueRange = 0f..1f,
                        onValueChange = onClarityChange,
                        testTag = "clarity_slider"
                    )
                    EnhancementSliderRow(
                        label = "SPATIAL / VIRTUAL SURROUND",
                        valueLabel = "${(audioState.spatialVirtualizer * 100).roundToInt()}%" +
                            if (audioState.isSpatializerAvailable) " · HW READY" else "",
                        value = audioState.spatialVirtualizer,
                        valueRange = 0f..1f,
                        onValueChange = onSpatialChange,
                        testTag = "spatial_slider"
                    )
                    EnhancementSliderRow(
                        label = "STEREO BALANCE",
                        valueLabel = when {
                            audioState.stereoBalance < -0.05f -> "L ${(-audioState.stereoBalance * 100).roundToInt()}%"
                            audioState.stereoBalance > 0.05f -> "R ${(audioState.stereoBalance * 100).roundToInt()}%"
                            else -> "CENTER"
                        },
                        value = audioState.stereoBalance,
                        valueRange = -1f..1f,
                        onValueChange = onBalanceChange,
                        testTag = "stereo_balance_slider"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOFT LIMITER (ANTI-CLIPPING)",
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite
                            )
                            Text(
                                text = "Limits peaks at -1.0 dBFS to prevent distortion at high gain.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                        Switch(
                            checked = audioState.isLimiterActive,
                            onCheckedChange = onToggleLimiter,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = StatusEmerald,
                                uncheckedThumbColor = SilverText,
                                uncheckedTrackColor = GlassCardElevated
                            ),
                            modifier = Modifier.testTag("soft_limiter_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqFrequencyCurveCanvas(bandsDb: List<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AmoledBlack)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val midY = size.height / 2f
        // Zero dB dashed baseline
        var xDot = 0f
        while (xDot < size.width) {
            drawCircle(
                color = GlassBorder,
                radius = 1.dp.toPx(),
                center = Offset(xDot, midY)
            )
            xDot += 8.dp.toPx()
        }

        if (bandsDb.size < 2) return@Canvas
        val stepX = size.width / (bandsDb.size - 1)
        val path = Path()
        val points = bandsDb.mapIndexed { i, db ->
            val x = i * stepX
            val normalized = (db / 12f).coerceIn(-1f, 1f)
            val y = midY - (normalized * (size.height * 0.42f))
            Offset(x, y)
        }

        path.moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val ctrlX = (p0.x + p1.x) / 2f
            path.cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(NothingRed, NothingRedGlow, PureWhite)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(color = NothingRed, radius = 3.5.dp.toPx(), center = pt)
            drawCircle(color = PureWhite, radius = 1.5.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun EnhancementSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    helperText: String? = null,
    testTag: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = NothingRed
            )
        }
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSteel
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PureWhite,
                activeTrackColor = NothingRed,
                inactiveTrackColor = GlassBorder
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = PureWhite,
            activeTrackColor = NothingRed,
            inactiveTrackColor = GlassBorder
        ),
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}
