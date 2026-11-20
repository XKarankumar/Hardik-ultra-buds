package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppDestination
import com.example.model.AppPreferencesState
import com.example.model.AudioEngineState
import com.example.model.EarbudsDeviceState
import com.example.model.UpdateStatus
import com.example.ui.components.BatteryRingWidget
import com.example.ui.components.DotMatrixDivider
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusDotBadge
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedMuted
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

@Composable
fun HomeDashboardScreen(
    deviceState: EarbudsDeviceState,
    audioState: AudioEngineState,
    prefsState: AppPreferencesState,
    updateStatus: UpdateStatus,
    onRequestPermissions: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onProbeGatt: () -> Unit,
    onToggleEngine: (Boolean) -> Unit,
    onToggleFindRing: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

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
            // Top Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HARDIK BUDS PRO",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ULTRA MAX EDITION · GUEST MODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenOnboarding,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("setup_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = "Open setup guide",
                                tint = SilverText
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Open settings",
                                tint = PureWhite
                            )
                        }
                    }
                }
            }

            // First-launch / permission setup banner
            if (!deviceState.hasBluetoothPermissions || !prefsState.onboardingCompleted) {
                item {
                    GlassCard(highlighted = true) {
                        DotMatrixSectionHeader(
                            title = if (!deviceState.hasBluetoothPermissions) {
                                "BLUETOOTH ACCESS REQUIRED"
                            } else {
                                "SETUP & CALIBRATION GUIDE"
                            },
                            subtitle = "Zero-account local mode · Android 10–16 compatible"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (!deviceState.hasBluetoothPermissions) {
                                "Grant Bluetooth Connect & Scan permissions to read real earbuds telemetry, codecs, and BLE signal strength."
                            } else {
                                "Pair your OnePlus Nord Buds 3 Pro or any wireless earbuds and calibrate Ultra Sound DSP."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!deviceState.hasBluetoothPermissions) {
                                NothingPillButton(
                                    text = "GRANT ACCESS",
                                    onClick = onRequestPermissions,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grant_bt_permission_button"),
                                    hapticsEnabled = prefsState.hapticsEnabled
                                )
                            }
                            NothingPillButton(
                                text = "SETUP GUIDE",
                                onClick = onOpenOnboarding,
                                isPrimary = deviceState.hasBluetoothPermissions,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_onboarding_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }
                    }
                }
            }

            // Live GitHub Release Update Alert
            item {
                AnimatedVisibility(visible = updateStatus is UpdateStatus.Available || updateStatus is UpdateStatus.ReadyToInstall) {
                    val versionLabel = when (updateStatus) {
                        is UpdateStatus.Available -> updateStatus.latestVersion
                        is UpdateStatus.ReadyToInstall -> updateStatus.latestVersion
                        else -> ""
                    }
                    GlassCard(
                        highlighted = true,
                        onClick = { onNavigate(AppDestination.UPDATE) },
                        modifier = Modifier.testTag("update_alert_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = "Update available",
                                    tint = NothingRed
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "HARDIK SYSTEM v$versionLabel READY",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "Tap to inspect release notes & SHA-256 checksum",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SilverText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hero Earbuds Card with Connection Animation
            item {
                HeroEarbudsCard(
                    deviceState = deviceState,
                    hapticsEnabled = prefsState.hapticsEnabled,
                    onRefresh = onRefreshBluetooth,
                    onProbeGatt = onProbeGatt,
                    onManageDevices = { onNavigate(AppDestination.DEVICES) }
                )
            }

            // Battery Telemetry Card (Left, Right, Case)
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "POWER TELEMETRY",
                        subtitle = if (deviceState.overallBatteryPct != null) {
                            "OVERALL LINKED BATTERY: ${deviceState.overallBatteryPct}%"
                        } else {
                            "AWAITING DEVICE BATTERY METADATA"
                        },
                        trailing = {
                            StatusDotBadge(
                                label = if (deviceState.isConnected) "LIVE" else "IDLE",
                                isActive = deviceState.isConnected,
                                activeColor = StatusEmerald
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BatteryRingWidget(
                            label = "LEFT BUD",
                            percentage = deviceState.leftBatteryPct ?: deviceState.overallBatteryPct
                        )
                        BatteryRingWidget(
                            label = "RIGHT BUD",
                            percentage = deviceState.rightBatteryPct ?: deviceState.overallBatteryPct
                        )
                        BatteryRingWidget(
                            label = "CASE",
                            percentage = deviceState.caseBatteryPct
                        )
                    }
                    if (deviceState.isConnected &&
                        deviceState.leftBatteryPct == null &&
                        deviceState.caseBatteryPct == null
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Note: Individual L/R/Case percentages are only shown if Android Bluetooth metadata or BLE GATT Battery Service exposes them. Overall battery is synced when broadcast by the headset.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteel
                        )
                    }
                }
            }

            // Quick Controls Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick ANC Status Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_anc_card"),
                        onClick = { onNavigate(AppDestination.ANC) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NoiseControlOff,
                            contentDescription = "ANC control",
                            tint = NothingRed,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NOISE CONTROL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = deviceState.ancMode.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (deviceState.isConnected) "Inspect Protocol" else "No Buds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverText
                        )
                    }

                    // Quick Sound Profile Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_studio_card"),
                        onClick = { onNavigate(AppDestination.STUDIO) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Sound studio",
                            tint = StatusEmerald,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SOUND PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = audioState.activePresetId.replace("_", " ").uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${audioState.soundProfileMode.label} · 10-Band",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverText
                        )
                    }
                }
            }

            // Ultra Sound Quick Toggle + Find Earbuds
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Ultra Sound DSP",
                                tint = if (audioState.isEngineEnabled) NothingRed else MutedSteel
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ULTRA SOUND DSP",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = audioState.engineStatusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                            modifier = Modifier.testTag("ultra_sound_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    DotMatrixDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NothingPillButton(
                            text = if (deviceState.isFindToneActive) "STOP RING" else "FIND EARBUDS",
                            onClick = onToggleFindRing,
                            isPrimary = deviceState.isFindToneActive,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("find_earbuds_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                        NothingPillButton(
                            text = "OPEN STUDIO",
                            onClick = { onNavigate(AppDestination.STUDIO) },
                            isPrimary = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_studio_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                    }
                }
            }

            // Firmware & Hardware Telemetry Card
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "HARDWARE & FIRMWARE",
                        subtitle = if (deviceState.isOnePlusNordBuds3Pro) {
                            "ONEPLUS NORD BUDS 3 PRO DETECTED"
                        } else {
                            "BLUETOOTH AUDIO TELEMETRY"
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TelemetryRow("HARDWARE", deviceState.hardwareVersion)
                    TelemetryRow("FIRMWARE", deviceState.firmwareVersion)
                    TelemetryRow("MAC ADDRESS", deviceState.connectedAddress)
                    TelemetryRow(
                        "AUDIO CODEC",
                        buildString {
                            append(deviceState.codecName)
                            deviceState.sampleRateHz?.let { append(" · ${it / 1000}kHz") }
                        }
                    )
                    TelemetryRow(
                        "RSSI SIGNAL",
                        deviceState.rssiDbm?.let { "$it dBm" } ?: "Probe BLE GATT to measure"
                    )
                    TelemetryRow(
                        "STREAM VOL",
                        "${audioState.currentStreamVolumePct}% (+${"%.1f".format(audioState.volumeEnhanceDb)} dB DSP)"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroEarbudsCard(
    deviceState: EarbudsDeviceState,
    hapticsEnabled: Boolean,
    onRefresh: () -> Unit,
    onProbeGatt: () -> Unit,
    onManageDevices: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroConnectionPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    GlassCard(highlighted = deviceState.isConnected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDotBadge(
                label = when {
                    deviceState.isConnected -> "CONNECTED"
                    deviceState.isBluetoothEnabled -> "BLUETOOTH READY"
                    else -> "BLUETOOTH OFF"
                },
                isActive = deviceState.isConnected,
                activeColor = if (deviceState.isConnected) StatusEmerald else StatusAmber
            )

            Text(
                text = deviceState.rssiDbm?.let { "$it dBm" } ?: deviceState.codecName,
                style = MaterialTheme.typography.labelMedium,
                color = SilverText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(AmoledBlack)
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_earbuds_hero),
                contentDescription = "Hardik Buds Pro Ultra Max hardware showcase",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.85f))
                        )
                    )
            )
            if (deviceState.isConnected) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2.2f) * ringScale
                    drawCircle(
                        color = NothingRed.copy(alpha = ringAlpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = deviceState.connectedDeviceName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (deviceState.isOnePlusNordBuds3Pro) {
                        "TARGET HARDWARE PROFILE · ONEPLUS NORD BUDS 3 PRO"
                    } else if (deviceState.isConnected) {
                        "ACTIVE BLUETOOTH AUDIO LINK"
                    } else {
                        "TAP DEVICES TO SCAN OR PAIR EARBUDS"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (deviceState.isOnePlusNordBuds3Pro) NothingRed else SilverText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NothingPillButton(
                text = if (deviceState.isConnected) "PROBE BLE" else "DEVICES",
                onClick = if (deviceState.isConnected) onProbeGatt else onManageDevices,
                isPrimary = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hero_primary_action"),
                hapticsEnabled = hapticsEnabled
            )
            NothingPillButton(
                text = "REFRESH",
                onClick = onRefresh,
                isPrimary = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("hero_refresh_action"),
                hapticsEnabled = hapticsEnabled
            )
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedSteel
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
