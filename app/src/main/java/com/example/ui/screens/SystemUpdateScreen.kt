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
    prefsState: AppPreferencesState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    onSaveRepoUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var repoInput by remember(prefsState.githubRepoUrl) { mutableStateOf(prefsState.githubRepoUrl) }

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
