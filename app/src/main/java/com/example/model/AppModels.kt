package com.example.model

import com.squareup.moshi.JsonClass

enum class AppDestination(val route: String, val label: String) {
    HOME("home", "HUB"),
    ANC("anc", "ANC"),
    STUDIO("studio", "STUDIO"),
    DEVICES("devices", "DEVICES"),
    UPDATE("update", "SYSTEM")
}

enum class AncMode(val title: String, val code: String, val description: String) {
    ANC_ON("ANC ON", "NOISE CANCEL", "Active Noise Cancellation"),
    TRANSPARENCY("TRANSPARENCY", "AMBIENT PASS", "External environmental passthrough"),
    ADAPTIVE("ADAPTIVE", "SMART ANC", "Auto-adjusts to ambient noise"),
    ANC_OFF("ANC OFF", "PASSIVE SEAL", "Noise processing bypassed")
}

enum class AncControlCapability(val badge: String, val detail: String) {
    NATIVE_BLE_GATT(
        "DIRECT GATT CONTROL",
        "Connected earbuds expose a compatible BLE GATT ANC characteristic."
    ),
    COMPANION_REQUIRED(
        "OFFICIAL COMPANION REQUIRED",
        "OnePlus Nord Buds 3 Pro uses a proprietary encrypted HeyMelody/OPlus protocol for hardware ANC, Touch Gestures, and Wear Detection. Direct third-party hardware switching is blocked by the manufacturer; use the official HeyMelody companion or device Bluetooth panel."
    ),
    NO_DEVICE_CONNECTED(
        "NO EARBUDS CONNECTED",
        "Connect Bluetooth earbuds to inspect ANC protocol support and battery telemetry."
    )
}

@JsonClass(generateAdapter = true)
data class EqPreset(
    val id: String,
    val name: String,
    val bandsDb: List<Float>, // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz (-12f..+12f)
    val bassBoost: Float = 0f,       // 0f..1f
    val trebleBoost: Float = 0f,     // 0f..1f
    val vocalBoost: Float = 0f,      // 0f..1f
    val clarity: Float = 0f,         // 0f..1f
    val loudnessGainDb: Float = 0f,  // 0f..10f
    val spatialAmount: Float = 0f,   // 0f..1f
    val isCustom: Boolean = false
)

data class AudioEngineState(
    val isEngineEnabled: Boolean = true,
    val activeSessionId: Int = 0,
    val usingGlobalMix: Boolean = true,
    val isDynamicsProcessingActive: Boolean = false,
    val isLimiterActive: Boolean = true,
    val isSpatializerAvailable: Boolean = false,
    val isSpatializerEnabled: Boolean = false,
    val isTestTonePlaying: Boolean = false,
    val activePresetId: String = "original_plus",
    val bandsDb: List<Float> = List(10) { 0f },
    val bassBoost: Float = 0.15f,
    val trebleBoost: Float = 0.10f,
    val vocalBoost: Float = 0.10f,
    val clarity: Float = 0.20f,
    val volumeEnhanceDb: Float = 2.0f, // 0f..10f dB software compensation
    val stereoBalance: Float = 0f,     // -1f (Left) .. +1f (Right)
    val spatialVirtualizer: Float = 0.25f,
    val soundProfileMode: SoundProfileMode = SoundProfileMode.MUSIC,
    val currentStreamVolumePct: Int = 50,
    val isSafeListeningWarning: Boolean = false,
    val engineStatusMessage: String = "ULTRA SOUND READY (SOFT LIMITER ARMED)"
)

enum class SoundProfileMode(val label: String) {
    MUSIC("MUSIC"),
    CINEMA("CINEMA"),
    GAMING("GAMING")
}

data class ScannedBluetoothDevice(
    val name: String,
    val address: String,
    val rssiDbm: Int?,
    val isBonded: Boolean,
    val isConnected: Boolean,
    val isAudioDevice: Boolean,
    val isOnePlusTarget: Boolean
)

data class EarbudsDeviceState(
    val hasBluetoothPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDeviceName: String = "No Earbuds Connected",
    val connectedAddress: String = "--:--:--:--:--:--",
    val isOnePlusNordBuds3Pro: Boolean = false,
    val leftBatteryPct: Int? = null,
    val rightBatteryPct: Int? = null,
    val caseBatteryPct: Int? = null,
    val overallBatteryPct: Int? = null,
    val rssiDbm: Int? = null,
    val codecName: String = "Unavailable",
    val sampleRateHz: Int? = null,
    val firmwareVersion: String = "Not exposed by device",
    val hardwareVersion: String = "Standard Bluetooth Audio",
    val ancMode: AncMode = AncMode.ANC_OFF,
    val ancLevel: Int = 2, // 0=Mild, 1=Moderate, 2=Max
    val ancCapability: AncControlCapability = AncControlCapability.NO_DEVICE_CONNECTED,
    val dualDeviceSupported: Boolean = false,
    val wearDetectionSupported: Boolean = false,
    val touchControlSupported: Boolean = false,
    val gattImmediateAlertSupported: Boolean = false,
    val isFindToneActive: Boolean = false,
    val isHeyMelodyInstalled: Boolean = false,
    val pairedAudioDevices: List<ScannedBluetoothDevice> = emptyList(),
    val scannedDevices: List<ScannedBluetoothDevice> = emptyList(),
    val activeAudioRoutes: List<String> = emptyList(),
    val statusLog: String = "Bluetooth listener active"
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpToDate(val currentVersion: String, val checkedAt: String) : UpdateStatus
    data class Available(
        val currentVersion: String,
        val latestVersion: String,
        val releaseTitle: String,
        val releaseNotes: String,
        val apkDownloadUrl: String,
        val apkFileName: String,
        val apkSizeBytes: Long,
        val expectedSha256: String?,
        val htmlUrl: String
    ) : UpdateStatus

    data class Downloading(
        val latestVersion: String,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateStatus

    data class Verifying(val latestVersion: String) : UpdateStatus

    data class ReadyToInstall(
        val latestVersion: String,
        val apkPath: String,
        val verifiedSha256: String,
        val checksumMatched: Boolean?,
        val releaseNotes: String
    ) : UpdateStatus

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UpdateStatus
}

@JsonClass(generateAdapter = true)
data class UpdateHistoryEntry(
    val timestamp: String,
    val version: String,
    val status: String,
    val sha256Short: String
)

data class AppPreferencesState(
    val onboardingCompleted: Boolean = false,
    val autoReconnectEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoUpdateCheckEnabled: Boolean = true,
    val autoAiHealEnabled: Boolean = true,
    val safeListeningThresholdPct: Int = 80,
    val githubRepoUrl: String = "https://github.com/hardik-audio/hardik-buds-pro-ultra-max",
    val customPresets: List<EqPreset> = emptyList(),
    val updateHistory: List<UpdateHistoryEntry> = emptyList()
)

object PresetCatalog {
    val freqLabels = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    val builtInPresets: List<EqPreset> = listOf(
        EqPreset(
            id = "ultra_loud",
            name = "ULTRA LOUD",
            bandsDb = listOf(4.5f, 4.0f, 3.0f, 1.5f, 1.0f, 2.5f, 4.0f, 5.0f, 4.5f, 3.5f),
            bassBoost = 0.45f,
            trebleBoost = 0.40f,
            vocalBoost = 0.35f,
            clarity = 0.50f,
            loudnessGainDb = 8.0f,
            spatialAmount = 0.25f
        ),
        EqPreset(
            id = "original_plus",
            name = "ORIGINAL PLUS",
            bandsDb = listOf(1.5f, 1.0f, 0.5f, 0.0f, 0.0f, 0.5f, 1.5f, 2.0f, 1.5f, 1.0f),
            bassBoost = 0.20f,
            trebleBoost = 0.15f,
            vocalBoost = 0.15f,
            clarity = 0.25f,
            loudnessGainDb = 2.5f,
            spatialAmount = 0.20f
        ),
        EqPreset(
            id = "deep_bass",
            name = "DEEP BASS",
            bandsDb = listOf(8.5f, 7.5f, 5.5f, 2.5f, 0.0f, -0.5f, 0.5f, 1.5f, 2.0f, 1.5f),
            bassBoost = 0.85f,
            trebleBoost = 0.15f,
            vocalBoost = 0.10f,
            clarity = 0.20f,
            loudnessGainDb = 3.0f,
            spatialAmount = 0.15f
        ),
        EqPreset(
            id = "vocal_clear",
            name = "VOCAL CLEAR",
            bandsDb = listOf(-2.0f, -1.5f, -1.0f, 0.5f, 2.5f, 5.0f, 6.0f, 4.5f, 2.5f, 1.5f),
            bassBoost = 0.05f,
            trebleBoost = 0.30f,
            vocalBoost = 0.80f,
            clarity = 0.70f,
            loudnessGainDb = 2.5f,
            spatialAmount = 0.10f
        ),
        EqPreset(
            id = "cinema",
            name = "CINEMA",
            bandsDb = listOf(6.0f, 5.0f, 3.0f, 1.0f, -0.5f, 1.5f, 3.5f, 4.0f, 3.5f, 2.5f),
            bassBoost = 0.65f,
            trebleBoost = 0.35f,
            vocalBoost = 0.45f,
            clarity = 0.45f,
            loudnessGainDb = 4.0f,
            spatialAmount = 0.85f
        ),
        EqPreset(
            id = "gaming",
            name = "GAMING",
            bandsDb = listOf(2.0f, 1.0f, -1.5f, -2.0f, 1.0f, 3.5f, 5.5f, 6.5f, 5.0f, 3.0f),
            bassBoost = 0.25f,
            trebleBoost = 0.55f,
            vocalBoost = 0.40f,
            clarity = 0.75f,
            loudnessGainDb = 3.5f,
            spatialAmount = 0.75f
        ),
        EqPreset(
            id = "rock",
            name = "ROCK",
            bandsDb = listOf(5.0f, 4.0f, 2.5f, 0.5f, -1.0f, 0.5f, 2.5f, 4.5f, 5.5f, 4.0f),
            bassBoost = 0.50f,
            trebleBoost = 0.45f,
            vocalBoost = 0.25f,
            clarity = 0.40f,
            loudnessGainDb = 3.0f,
            spatialAmount = 0.30f
        ),
        EqPreset(
            id = "pop",
            name = "POP",
            bandsDb = listOf(3.0f, 2.5f, 1.0f, 0.0f, 1.5f, 3.0f, 3.5f, 2.5f, 3.0f, 2.5f),
            bassBoost = 0.35f,
            trebleBoost = 0.30f,
            vocalBoost = 0.45f,
            clarity = 0.40f,
            loudnessGainDb = 2.5f,
            spatialAmount = 0.25f
        ),
        EqPreset(
            id = "edm",
            name = "EDM",
            bandsDb = listOf(8.0f, 7.0f, 4.0f, 0.5f, -1.5f, -0.5f, 2.5f, 5.0f, 6.5f, 5.5f),
            bassBoost = 0.80f,
            trebleBoost = 0.55f,
            vocalBoost = 0.15f,
            clarity = 0.50f,
            loudnessGainDb = 4.5f,
            spatialAmount = 0.55f
        )
    )
}
