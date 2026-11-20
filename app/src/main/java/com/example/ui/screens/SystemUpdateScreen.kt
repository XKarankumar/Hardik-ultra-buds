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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ai.AiDoctorUiState
import com.example.ai.AutoFixAction
import com.example.model.AppPreferencesState
import com.example.model.UpdateStatus
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
import kotlin.math.roundToInt

@Composable
fun SystemUpdateScreen(
    currentVersion: String,
    updateStatus: UpdateStatus,
    aiDoctorState: AiDoctorUiState,
    prefsState: AppPreferencesState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    onSaveRepoUrl: (String) -> Unit,
    onDiagnoseError: (String) -> Unit,
    onRequestBtPermissions: () -> Unit,
    onOpenBtSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var repoInput by remember(prefsState.githubRepoUrl) { mutableStateOf(prefsState.githubRepoUrl) }
    var customErrorInput by remember { mutableStateOf("") }

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
            // HARDIK SYSTEM Status Card (matches requested Nothing-inspired update UI)
            item {
                GlassCard(
                    highlighted = updateStatus is UpdateStatus.Available ||
                        updateStatus is UpdateStatus.ReadyToInstall
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HARDIK SYSTEM",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SYSTEM STATUS: ONLINE",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusEmerald
                            )
                        }
                        StatusDotBadge(
                            label = "HTTPS OTA",
                            isActive = true,
                            activeColor = NothingRed
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    DotMatrixDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    val latestVersionDisplay = when (updateStatus) {
                        is UpdateStatus.Available -> updateStatus.latestVersion
                        is UpdateStatus.Downloading -> updateStatus.latestVersion
                        is UpdateStatus.Verifying -> updateStatus.latestVersion
                        is UpdateStatus.ReadyToInstall -> updateStatus.latestVersion
                        is UpdateStatus.UpToDate -> updateStatus.currentVersion
                        else -> "Check GitHub"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                            Text(
                                text = currentVersion,
                                style = MaterialTheme.typography.displayMedium,
                                color = PureWhite
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Latest Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                            Text(
                                text = latestVersionDisplay,
                                style = MaterialTheme.typography.displayMedium,
                                color = if (updateStatus is UpdateStatus.Available ||
                                    updateStatus is UpdateStatus.ReadyToInstall
                                ) {
                                    NothingRed
                                } else {
                                    PureWhite
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (updateStatus) {
                        is UpdateStatus.Idle -> {
                            Text(
                                text = "Ready to check GitHub Releases for signed APK updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NothingPillButton(
                                text = "CHECK FOR UPDATES",
                                onClick = onCheckForUpdates,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("check_updates_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }

                        is UpdateStatus.Checking -> {
                            Text(
                                text = "Querying GitHub Releases API over HTTPS...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                color = NothingRed,
                                trackColor = GlassBorder,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is UpdateStatus.UpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Up to date",
                                    tint = StatusEmerald
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "System is up to date (checked ${updateStatus.checkedAt}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            NothingPillButton(
                                text = "CHECK AGAIN",
                                onClick = onCheckForUpdates,
                                isPrimary = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("check_updates_again_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }

                        is UpdateStatus.Available -> {
                            Text(
                                text = "Update Available · ${updateStatus.releaseTitle}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NothingRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = updateStatus.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlassCardElevated)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ASSET: ${updateStatus.apkFileName} (${updateStatus.apkSizeBytes / 1024} KB)" +
                                    (updateStatus.expectedSha256?.let { "\nSHA-256: ${it.take(24)}..." }
                                        ?: "\nSHA-256: Will be computed locally upon download"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            NothingPillButton(
                                text = "DOWNLOAD UPDATE",
                                onClick = onDownloadUpdate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_update_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                        }

                        is UpdateStatus.Downloading -> {
                            val pct = (updateStatus.progress * 100f).roundToInt()
                            Text(
                                text = "Downloading v${updateStatus.latestVersion} — $pct%",
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { updateStatus.progress },
                                color = NothingRed,
                                trackColor = GlassBorder,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${updateStatus.downloadedBytes / 1024} KB / ${updateStatus.totalBytes / 1024} KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }

                        is UpdateStatus.Verifying -> {
                            Text(
                                text = "Verifying APK SHA-256 checksum...",
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusAmber
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                color = StatusAmber,
                                trackColor = GlassBorder,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is UpdateStatus.ReadyToInstall -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = "Verified APK",
                                    tint = StatusEmerald
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (updateStatus.checksumMatched) {
                                        true -> "SHA-256 CHECKSUM VERIFIED"
                                        null -> "APK DOWNLOADED (NO UPSTREAM HASH PUBLISHED)"
                                        false -> "CHECKSUM MISMATCH"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StatusEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SHA-256: ${updateStatus.verifiedSha256}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            NothingPillButton(
                                text = "INSTALL UPDATE",
                                onClick = { onInstallUpdate(updateStatus.apkPath) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("install_update_button"),
                                hapticsEnabled = prefsState.hapticsEnabled
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android system installer will open for your explicit confirmation.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                        }

                        is UpdateStatus.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Update error",
                                    tint = NothingRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = updateStatus.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                            if (updateStatus.canRetry) {
                                Spacer(modifier = Modifier.height(12.dp))
                                NothingPillButton(
                                    text = "RETRY UPDATE CHECK",
                                    onClick = onCheckForUpdates,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("retry_update_button"),
                                    hapticsEnabled = prefsState.hapticsEnabled
                                )
                            }
                        }
                    }
                }
            }

            // Configurable GitHub Repository Card
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "GITHUB RELEASE SOURCE",
                        subtitle = "HTTPS ONLY · NO GOOGLE ACCOUNT REQUIRED"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("GitHub Repository URL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = NothingRed,
                            unfocusedLabelColor = SilverText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NothingPillButton(
                            text = "SAVE SOURCE",
                            onClick = { onSaveRepoUrl(repoInput) },
                            isPrimary = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_repo_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                        NothingPillButton(
                            text = "CHECK NOW",
                            onClick = {
                                onSaveRepoUrl(repoInput)
                                onCheckForUpdates()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_now_button"),
                            hapticsEnabled = prefsState.hapticsEnabled
                        )
                    }
                }
            }

            // AI Error Doctor & Automatic Recovery Panel (Firebase AI Logic - Gemini)
            item {
                GlassCard(highlighted = aiDoctorState is AiDoctorUiState.Diagnosed || aiDoctorState is AiDoctorUiState.Analyzing) {
                    DotMatrixSectionHeader(
                        title = "AI ERROR AUTO-DOCTOR",
                        subtitle = "GEMINI AUTO-HEALING · ZERO LOGIN",
                        trailing = {
                            StatusDotBadge(
                                label = if (prefsState.autoAiHealEnabled) "AUTO-FIX ON" else "MANUAL",
                                isActive = prefsState.autoAiHealEnabled,
                                activeColor = StatusEmerald
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (aiDoctorState) {
                        is AiDoctorUiState.Idle -> {
                            Text(
                                text = "Automatically diagnoses GitHub, Bluetooth, and Audio DSP errors. You can also paste any GitHub Actions or Gradle build error log below for an instant fix.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverText
                            )
                        }
                        is AiDoctorUiState.Analyzing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "AI analyzing",
                                    tint = NothingRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing error with Gemini AI...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                color = NothingRed,
                                trackColor = GlassBorder,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is AiDoctorUiState.Diagnosed -> {
                            val res = aiDoctorState.result
                            Text(
                                text = "ROOT CAUSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NothingRed
                            )
                            Text(
                                text = res.rootCause,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite
                            )
                            if (!res.autoAppliedNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "AUTO-FIXED: ${res.autoAppliedNote}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusEmerald,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassCardElevated)
                                        .border(1.dp, StatusEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                )
                            }
                            if (res.fixSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "RECOMMENDED STEPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteel
                                )
                                res.fixSteps.forEachIndexed { idx, step ->
                                    Text(
                                        text = "${idx + 1}. $step",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SilverText
                                    )
                                }
                            }
                            if (!res.patchSnippet.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = res.patchSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AmoledBlack)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                )
                            }
                            when (res.autoFixAction) {
                                AutoFixAction.REQUEST_BT_PERMISSIONS -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    NothingPillButton(
                                        text = "GRANT BT PERMISSIONS",
                                        onClick = onRequestBtPermissions,
                                        modifier = Modifier.fillMaxWidth(),
                                        hapticsEnabled = prefsState.hapticsEnabled
                                    )
                                }
                                AutoFixAction.OPEN_BT_SETTINGS -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    NothingPillButton(
                                        text = "OPEN BLUETOOTH SETTINGS",
                                        onClick = onOpenBtSettings,
                                        modifier = Modifier.fillMaxWidth(),
                                        hapticsEnabled = prefsState.hapticsEnabled
                                    )
                                }
                                else -> Unit
                            }
                        }
                        is AiDoctorUiState.Error -> {
                            Text(
                                text = "AI Doctor Error: ${aiDoctorState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NothingRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customErrorInput,
                        onValueChange = { customErrorInput = it },
                        label = { Text("Paste GitHub / Gradle / App Error Log") },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = NothingRed,
                            unfocusedLabelColor = SilverText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_error_log_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NothingPillButton(
                        text = "AI DIAGNOSE & AUTO-FIX",
                        onClick = {
                            val toDiagnose = customErrorInput.ifBlank {
                                (updateStatus as? UpdateStatus.Error)?.message
                                    ?: "GitHub Actions build failed or no APK release detected"
                            }
                            onDiagnoseError(toDiagnose)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_diagnose_button"),
                        hapticsEnabled = prefsState.hapticsEnabled
                    )
                }
            }

            // Update History Log
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "UPDATE HISTORY",
                        subtitle = "LOCAL VERIFICATION LOG"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (prefsState.updateHistory.isEmpty()) {
                        Text(
                            text = "No update checks logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSteel
                        )
                    }
                }
            }

            items(prefsState.updateHistory) { entry ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "v${entry.version} · ${entry.status}",
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite
                            )
                            Text(
                                text = entry.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                        }
                        Text(
                            text = entry.sha256Short,
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverText
                        )
                    }
                }
            }
        }
    }
}
