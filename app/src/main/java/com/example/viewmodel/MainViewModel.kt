package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HardikBudsApp
import com.example.model.AncMode
import com.example.model.AppPreferencesState
import com.example.model.AudioEngineState
import com.example.model.EarbudsDeviceState
import com.example.model.EqPreset
import com.example.model.SoundProfileMode
import com.example.model.UpdateStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HardikBudsApp
    private val prefsRepo = app.preferencesRepository
    private val audioEngine = app.audioEngine
    private val btManager = app.bluetoothManager
    private val updateManager = app.updateManager

    val audioState: StateFlow<AudioEngineState> = audioEngine.state
    val deviceState: StateFlow<EarbudsDeviceState> = btManager.state
    val updateStatus: StateFlow<UpdateStatus> = updateManager.status

    val preferencesState: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    private val _showOnboardingSheet = MutableStateFlow(false)
    val showOnboardingSheet: StateFlow<Boolean> = _showOnboardingSheet.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    init {
        viewModelScope.launch {
            val savedAudio = prefsRepo.savedAudioStateFlow.first()
            audioEngine.restoreSavedState(savedAudio)

            val prefs = prefsRepo.preferencesFlow.first()
            btManager.setAutoReconnectEnabled(prefs.autoReconnectEnabled)
            audioEngine.updateSafeListeningThreshold(prefs.safeListeningThresholdPct)

            if (prefs.autoUpdateCheckEnabled) {
                updateManager.checkForUpdates(prefs.githubRepoUrl, recordHistory = false)
            }
        }

        // Persist audio state changes with debounce so 60/120fps slider drags remain fluid
        viewModelScope.launch {
            audioEngine.state
                .drop(1)
                .debounce(350L)
                .collect { state ->
                    prefsRepo.saveAudioState(state)
                }
        }
    }

    fun openOnboarding() {
        _showOnboardingSheet.value = true
    }

    fun dismissOnboarding(markCompleted: Boolean) {
        _showOnboardingSheet.value = false
        if (markCompleted) {
            viewModelScope.launch { prefsRepo.setOnboardingCompleted(true) }
        }
    }

    fun openSettings() {
        _showSettingsSheet.value = true
    }

    fun closeSettings() {
        _showSettingsSheet.value = false
    }

    // --- Audio Engine Actions ---
    fun setEngineEnabled(enabled: Boolean) = audioEngine.setEngineEnabled(enabled)
    fun setLimiterActive(enabled: Boolean) = audioEngine.setLimiterActive(enabled)
    fun applyPreset(preset: EqPreset) = audioEngine.applyPreset(preset)
    fun setBandGain(index: Int, db: Float) = audioEngine.setBandGain(index, db)
    fun setBassBoost(amount: Float) = audioEngine.setBassBoost(amount)
    fun setTrebleBoost(amount: Float) = audioEngine.setTrebleBoost(amount)
    fun setVocalBoost(amount: Float) = audioEngine.setVocalBoost(amount)
    fun setClarity(amount: Float) = audioEngine.setClarity(amount)
    fun setVolumeEnhanceDb(db: Float) = audioEngine.setVolumeEnhanceDb(db)
    fun setStereoBalance(balance: Float) = audioEngine.setStereoBalance(balance)
    fun setSpatialVirtualizer(amount: Float) = audioEngine.setSpatialVirtualizer(amount)
    fun setSoundProfileMode(mode: SoundProfileMode) = audioEngine.setSoundProfileMode(mode)
    fun resetAudioToFlat() = audioEngine.resetToFlat()
    fun toggleAcousticTestSignal() = audioEngine.toggleAcousticTestSignal()

    fun saveCurrentAsCustomPreset(name: String) {
        val cleanName = name.trim().uppercase().take(24)
        if (cleanName.isEmpty()) return
        val s = audioState.value
        val preset = EqPreset(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            bandsDb = s.bandsDb,
            bassBoost = s.bassBoost,
            trebleBoost = s.trebleBoost,
            vocalBoost = s.vocalBoost,
            clarity = s.clarity,
            loudnessGainDb = s.volumeEnhanceDb,
            spatialAmount = s.spatialVirtualizer,
            isCustom = true
        )
        viewModelScope.launch {
            prefsRepo.saveCustomPreset(preset)
            audioEngine.applyPreset(preset)
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            prefsRepo.deleteCustomPreset(presetId)
        }
    }

    // --- Bluetooth & ANC Actions ---
    fun requiredBluetoothPermissions(): Array<String> = btManager.requiredRuntimePermissions()
    fun onPermissionsResult() = btManager.refreshPermissionsAndProfiles()
    fun refreshBluetooth() = btManager.refreshConnectedDevices()
    fun probeConnectedGatt() = btManager.probeConnectedDeviceGatt()
    fun startBleScan() = btManager.startBleScan()
    fun stopBleScan() = btManager.stopBleScan()
    fun pairOrConnect(address: String) = btManager.requestPairOrConnect(address)
    fun selectAncMode(mode: AncMode, level: Int = deviceState.value.ancLevel) =
        btManager.selectAncModeUiOnlyWarning(mode, level)

    fun toggleFindEarbudsRing() = btManager.triggerFindEarbudsRing()
    fun bluetoothSettingsIntent(): Intent = btManager.openBluetoothSettingsIntent()
    fun companionAppIntent(): Intent? = btManager.openCompanionAppIntent()

    // --- Preferences & Settings ---
    fun setAutoReconnect(enabled: Boolean) {
        btManager.setAutoReconnectEnabled(enabled)
        viewModelScope.launch { prefsRepo.setAutoReconnect(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoUpdateCheck(enabled) }
    }

    fun setSafeListeningThreshold(pct: Int) {
        audioEngine.updateSafeListeningThreshold(pct)
        viewModelScope.launch { prefsRepo.setSafeListeningThreshold(pct) }
    }

    fun updateGithubRepoUrl(url: String) {
        viewModelScope.launch {
            prefsRepo.setGithubRepoUrl(url)
        }
    }

    // --- GitHub Releases Updater ---
    fun currentAppVersion(): String = updateManager.currentVersionName()

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(preferencesState.value.githubRepoUrl, recordHistory = true)
        }
    }

    fun downloadUpdate() {
        val current = updateStatus.value as? UpdateStatus.Available ?: return
        viewModelScope.launch {
            updateManager.downloadAndVerifyUpdate(current)
        }
    }

    fun canInstallPackages(): Boolean = updateManager.canInstallPackages()
    fun unknownSourcesSettingsIntent(): Intent = updateManager.createUnknownSourcesSettingsIntent()
    fun packageInstallerIntent(apkPath: String): Intent? = updateManager.createPackageInstallerIntent(apkPath)
}
