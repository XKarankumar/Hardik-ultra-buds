package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppPreferencesState
import com.example.model.EarbudsDeviceState
import com.example.model.ScannedBluetoothDevice
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
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

@Composable
fun DevicesScreen(
    deviceState: EarbudsDeviceState,
    prefsState: AppPreferencesState,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPairOrConnect: (String) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleFindRing: () -> Unit,
    onProbeGatt: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenCompanionApp: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Header + Scan Controls
            item {
                GlassCard(highlighted = deviceState.isScanning) {
                    DotMatrixSectionHeader(
                        title = "BLUETOOTH CONTROL",
                        subtitle = deviceState.statusLog,
                        trailing = {
                            StatusDotBadge(
                                label = if (deviceState.isBluetoothEnabled) "BT ON" else "BT OFF",
                                isActive = deviceState.isBluetoothEnabled,
                                activeColor = if (deviceState.isBluetoothEnabled) StatusEmerald else StatusAmber
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!deviceState.hasBluetoothPermissions) {
                        NothingPillButton(
                            text = "GRANT BLUETOOTH PERMISSIONS",
                            onClick = onRequestPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("devices_grant_permission_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NothingPillButton(
                                text = if (deviceState.isScanning) "STOP SCAN" else "SCAN BLE DEVICES",
                                onClick = if (deviceState.isScanning) onStopScan else onStartScan,
                                isPrimary = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ble_scan_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                            NothingPillButton(
                                text = "SYS BT PANEL",
                                onClick = onOpenBluetoothSettings,
                                isPrimary = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_sys_bt_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }
                    }

                    if (deviceState.isScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = NothingRed,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Listening for BLE audio advertisements & RSSI telemetry...",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                    }
                }
            }

            // Reconnect, Find Earbuds, and Active Routing
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "DEVICE OPERATIONS",
                        subtitle = "ACTIVE OUTPUTS: ${deviceState.activeAudioRoutes.size}"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTO RECONNECT",
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite
                            )
                            Text(
                                text = "Automatically attempt profile reconnect on unexpected ACL disconnect.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                        Switch(
                            checked = prefsState.autoReconnectEnabled,
                            onCheckedChange = onToggleAutoReconnect,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = NothingRed,
                                uncheckedThumbColor = SilverText,
                                uncheckedTrackColor = GlassCardElevated
                            ),
                            modifier = Modifier.testTag("auto_reconnect_switch")
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
                            text = if (deviceState.isFindToneActive) "STOP LOCATE RING" else "FIND EARBUDS RING",
                            onClick = onToggleFindRing,
                            isPrimary = deviceState.isFindToneActive,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("devices_find_ring_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                        NothingPillButton(
                            text = "PROBE GATT",
                            onClick = onProbeGatt,
                            isPrimary = false,
                            enabled = deviceState.isConnected,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("devices_probe_gatt_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                    }

                    if (deviceState.activeAudioRoutes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ROUTED ANDROID AUDIO OUTPUTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        deviceState.activeAudioRoutes.forEach { route ->
                            Text(
                                text = "• $route",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusEmerald
                            )
                        }
                    }
                }
            }

            // Compatibility & Protocol Support Matrix (Honest hardware boundary)
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "HARDWARE COMPATIBILITY MATRIX",
                        subtitle = if (deviceState.isOnePlusNordBuds3Pro) {
                            "ONEPLUS NORD BUDS 3 PRO PROFILE"
                        } else {
                            "GENERIC BLUETOOTH AUDIO PROFILE"
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CompatibilityRow(
                        feature = "ULTRA SOUND 10-BAND EQ & LIMITER",
                        status = "SUPPORTED",
                        isSupported = true
                    )
                    CompatibilityRow(
                        feature = "FIND EARBUDS ACOUSTIC / GATT ALERT",
                        status = if (deviceState.gattImmediateAlertSupported) "GATT + AUDIO" else "AUDIO CHIME",
                        isSupported = true
                    )
                    CompatibilityRow(
                        feature = "DUAL-DEVICE SWITCHING",
                        status = if (deviceState.dualDeviceSupported) "COMPANION / SYS BT" else "SYS BT SWITCH",
                        isSupported = false
                    )
                    CompatibilityRow(
                        feature = "TOUCH CONTROLS & WEAR DETECTION",
                        status = if (deviceState.wearDetectionSupported) "HEYMELODY PROTOCOL" else "NOT EXPOSED",
                        isSupported = false
                    )
                    if (deviceState.isHeyMelodyInstalled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NothingPillButton(
                            text = "OPEN OFFICIAL HEYMELODY APP",
                            onClick = onOpenCompanionApp,
                            isPrimary = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("devices_open_companion_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                    }
                }
            }

            // Paired Bluetooth Audio Devices
            if (deviceState.pairedAudioDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "PAIRED AUDIO DEVICES",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(deviceState.pairedAudioDevices, key = { "paired_${it.address}" }) { dev ->
                    DeviceListRow(
                        device = dev,
                        hapticsEnabled = prefsState.hapticsEnabled,
                        onAction = { onPairOrConnect(dev.address) }
                    )
                }
            }

            // Scanned BLE Devices
            if (deviceState.scannedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "NEARBY BLE DISCOVERIES",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(deviceState.scannedDevices, key = { "scan_${it.address}" }) { dev ->
                    DeviceListRow(
                        device = dev,
                        hapticsEnabled = prefsState.hapticsEnabled,
                        onAction = { onPairOrConnect(dev.address) }
                    )
                }
            } else if (!deviceState.isScanning) {
                item {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.BluetoothSearching,
                                contentDescription = null,
                                tint = MutedSteel
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tap SCAN BLE DEVICES above to discover nearby OnePlus Nord Buds 3 Pro or Bluetooth audio peripherals with live dBm signal strength.",
                                style = MaterialTheme.typography.bodyMedium,
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
private fun DeviceListRow(
    device: ScannedBluetoothDevice,
    hapticsEnabled: Boolean,
    onAction: () -> Unit
) {
    GlassCard(highlighted = device.isConnected || device.isOnePlusTarget) {
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
                    imageVector = if (device.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Headphones,
                    contentDescription = device.name,
                    tint = when {
                        device.isConnected -> StatusEmerald
                        device.isOnePlusTarget -> NothingRed
                        else -> PureWhite
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(device.address)
                            device.rssiDbm?.let { append(" · $it dBm") }
                            if (device.isOnePlusTarget) append(" · TARGET")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.isOnePlusTarget) NothingRed else SilverText
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            NothingPillButton(
                text = when {
                    device.isConnected -> "ACTIVE"
                    device.isBonded -> "CONNECT"
                    else -> "PAIR"
                },
                onClick = onAction,
                isPrimary = !device.isConnected,
                enabled = !device.isConnected,
                hapticsEnabled = hapticsEnabled
            )
        }
    }
}

@Composable
private fun CompatibilityRow(
    feature: String,
    status: String,
    isSupported: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.labelSmall,
            color = SilverText,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSupported) StatusEmerald else StatusAmber,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GlassCardElevated)
                .border(1.dp, GlassBorder, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
