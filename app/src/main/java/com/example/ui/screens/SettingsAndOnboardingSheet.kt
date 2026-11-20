package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppPreferencesState
import com.example.model.EarbudsDeviceState
import com.example.ui.components.DotMatrixDivider
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NothingRed
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSetupSheet(
    deviceState: EarbudsDeviceState,
    hapticsEnabled: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var stepIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HARDIK SETUP · ${stepIndex + 1}/3",
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingRed
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close setup", tint = SilverText)
                }
            }

            Image(
                painter = painterResource(id = R.drawable.img_onboarding_sound),
                contentDescription = "Hardik Buds Pro acoustic waveform art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            )

            when (stepIndex) {
                0 -> {
                    Text(
                        text = "WELCOME TO HARDIK BUDS PRO ULTRA MAX",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "Zero-account local companion engineered for OnePlus Nord Buds 3 Pro and Bluetooth audio. Enjoy real 10-band DynamicsProcessing DSP, Soft Limiter protection, BLE telemetry, and SHA-256 verified GitHub Releases updates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                }
                1 -> {
                    Text(
                        text = "CONNECT BLUETOOTH TELEMETRY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = if (deviceState.hasBluetoothPermissions) {
                            "Bluetooth permissions are granted. Pair your earbuds in Android Bluetooth settings to monitor battery, codecs, and BLE signal strength."
                        } else {
                            "Grant Android 12+ Bluetooth Connect & Scan permissions so Hardik Buds Pro can inspect your connected earbuds without requiring location tracking."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!deviceState.hasBluetoothPermissions) {
                            NothingPillButton(
                                text = "GRANT BLUETOOTH",
                                onClick = onRequestPermissions,
                                modifier = Modifier.weight(1f),
                                hapticsEnabled = hapticsEnabled
                            )
                        }
                        NothingPillButton(
                            text = "PAIR IN SETTINGS",
                            onClick = onOpenBluetoothSettings,
                            isPrimary = deviceState.hasBluetoothPermissions,
                            modifier = Modifier.weight(1f),
                            hapticsEnabled = hapticsEnabled
                        )
                    }
                }
                else -> {
                    Text(
                        text = "HONEST HARDWARE & ULTRA SOUND",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Text(
                        text = "All sound tuning (10-band EQ, Bass, Treble, Vocal, Clarity, Soft Limiter) runs natively via Android DSP. Encrypted vendor ANC controls on OnePlus Nord Buds 3 Pro are clearly bridged to the official HeyMelody companion—never faked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stepIndex > 0) {
                    NothingPillButton(
                        text = "BACK",
                        onClick = { stepIndex-- },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        hapticsEnabled = hapticsEnabled
                    )
                }
                NothingPillButton(
                    text = if (stepIndex < 2) "NEXT" else "FINISH SETUP",
                    onClick = {
                        if (stepIndex < 2) stepIndex++ else onComplete()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_next_button"),
                    hapticsEnabled = hapticsEnabled
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    prefsState: AppPreferencesState,
    currentVersion: String,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onToggleAutoAiHeal: (Boolean) -> Unit,
    onSafeThresholdChange: (Int) -> Unit,
    onOpenOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotMatrixSectionHeader(
                    title = "SYSTEM SETTINGS",
                    subtitle = "HARDIK BUDS PRO v$currentVersion"
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close settings", tint = SilverText)
                }
            }

            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = "Guest mode privacy",
                        tint = StatusEmerald
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ZERO-ACCOUNT PRIVACY MODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite
                        )
                        Text(
                            text = "No Google Sign-In, no cloud tracking. Presets and preferences are stored strictly locally in Android DataStore.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverText
                        )
                    }
                }
            }

            GlassCard {
                SettingToggleRow(
                    title = "HAPTIC FEEDBACK",
                    subtitle = "Tactile click response on ANC & EQ controls",
                    checked = prefsState.hapticsEnabled,
                    onCheckedChange = onToggleHaptics,
                    testTag = "setting_haptics_switch"
                )
                DotMatrixDivider()
                SettingToggleRow(
                    title = "BLUETOOTH AUTO-RECONNECT",
                    subtitle = "Attempt profile reconnection if earbuds drop",
                    checked = prefsState.autoReconnectEnabled,
                    onCheckedChange = onToggleAutoReconnect,
                    testTag = "setting_reconnect_switch"
                )
                DotMatrixDivider()
                SettingToggleRow(
                    title = "AUTO CHECK GITHUB UPDATES",
                    subtitle = "Check on launch & every 12h via WorkManager",
                    checked = prefsState.autoUpdateCheckEnabled,
                    onCheckedChange = onToggleAutoUpdate,
                    testTag = "setting_autoupdate_switch"
                )
                DotMatrixDivider()
                SettingToggleRow(
                    title = "AI AUTO-FIX ON ERRORS",
                    subtitle = "Automatically diagnose & self-heal errors via Gemini AI",
                    checked = prefsState.autoAiHealEnabled,
                    onCheckedChange = onToggleAutoAiHeal,
                    testTag = "setting_ai_heal_switch"
                )
                DotMatrixDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SAFE LISTENING ALERT THRESHOLD: ${prefsState.safeListeningThresholdPct}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite
                )
                Slider(
                    value = prefsState.safeListeningThresholdPct.toFloat(),
                    onValueChange = { onSafeThresholdChange(it.roundToInt()) },
                    valueRange = 60f..95f,
                    colors = SliderDefaults.colors(
                        thumbColor = PureWhite,
                        activeTrackColor = NothingRed,
                        inactiveTrackColor = GlassBorder
                    ),
                    modifier = Modifier.testTag("setting_safe_threshold_slider")
                )
            }

            NothingPillButton(
                text = "REOPEN SETUP GUIDE",
                onClick = {
                    onDismiss()
                    onOpenOnboarding()
                },
                isPrimary = false,
                modifier = Modifier.fillMaxWidth(),
                hapticsEnabled = prefsState.hapticsEnabled
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = PureWhite)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MutedSteel)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = NothingRed,
                uncheckedThumbColor = SilverText,
                uncheckedTrackColor = GlassCardElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
