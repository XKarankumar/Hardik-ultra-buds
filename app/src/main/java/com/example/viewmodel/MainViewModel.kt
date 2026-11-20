package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HardikBudsApp
import com.example.ai.AiDoctorUiState
import com.example.ai.AiErrorDoctor
import com.example.ai.AutoFixAction
import com.example.model.AncMode
import com.example.model.AppPreferencesState
import com.example.model.AudioEngineState
import com.example.model.EarbudsDeviceState
import com.example.model.EqPreset
import com.example.model.SoundProfileMode
import com.example.model.UpdateStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
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
    private val aiErrorDoctor = AiErrorDoctor()

    val audioState: StateFlow<AudioEngineState> = audioEngine.state
    val deviceState: StateFlow<EarbudsDeviceState> = btManager.state
    val updateStatus: StateFlow<UpdateStatus> = updateManager.status

    val preferencesState: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    private val _showOnboardingSheet = MutableStateFlow(false)
    val showOnboardingSheet: StateFlow<Boolean> = _showOnboardingSheet.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _aiDoctorState = MutableStateFlow<AiDoctorUiState>(AiDoctorUiState.Idle)
    val aiDoctorState: StateFlow<AiDoctorUiState> = _aiDoctorState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedAudio = prefsRepo.savedAudioStateFlow.first()
            audioEngine.restoreSavedState(savedAudio)

            val prefs = prefsRepo.preferencesFlow.first()
            btManager.setAutoReconnectEnabled(prefs.autoReconnectEnabled)
            audioEngine.updateSafeListeningThreshold(prefs.safeListeningThresholdPct)

            if (prefs.autoUpdateCheckEnabled &&
                prefs.githubRepoUrl != AppPreferencesState().githubRepoUrl
            ) {
                val res = updateManager.checkForUpdates(prefs.githubRepoUrl, recordHistory = false)
                if (res is UpdateStatus.Error && prefs.autoAiHealEnabled) {
                    diagnoseAndAutoHeal("GitHub Update Launch Check", res.message)
                }
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

    fun setAutoAiHeal(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoAiHeal(enabled) }
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

    // --- GitHub Releases Updater + AI Auto-Fix ---
    fun currentAppVersion(): String = updateManager.currentVersionName()

    fun checkForUpdates() {
        viewModelScope.launch {
            val prefs = preferencesState.value
            val result = updateManager.checkForUpdates(prefs.githubRepoUrl, recordHistory = true)
            if (result is UpdateStatus.Error && prefs.autoAiHealEnabled) {
                diagnoseAndAutoHeal("GitHub Release Check", result.message)
            }
        }
    }

    fun downloadUpdate() {
        val current = updateStatus.value as? UpdateStatus.Available ?: return
        viewModelScope.launch {
            updateManager.downloadAndVerifyUpdate(current)
            val after = updateStatus.value
            if (after is UpdateStatus.Error && preferencesState.value.autoAiHealEnabled) {
                diagnoseAndAutoHeal("GitHub APK Download / SHA-256", after.message)
            }
        }
    }

    fun diagnoseCustomError(errorLog: String) {
        val clean = errorLog.trim()
        if (clean.isEmpty()) return
        diagnoseAndAutoHeal("Manual / GitHub CI Log", clean)
    }

    fun clearAiDiagnosis() {
        _aiDoctorState.value = AiDoctorUiState.Idle
    }

    fun diagnoseAndAutoHeal(errorSource: String, errorDetail: String) {
        viewModelScope.launch {
            _aiDoctorState.value = AiDoctorUiState.Analyzing(errorDetail.take(120))
            val prefs = preferencesState.value
            val d = deviceState.value
            val a = audioState.value
            val contextSummary = "BT=${d.isBluetoothEnabled}, Perms=${d.hasBluetoothPermissions}, " +
                "Connected=${d.isConnected} (${d.connectedDeviceName}), DSP=${a.engineStatusMessage}, " +
                "Limiter=${a.isLimiterActive}, SafeAlert=${a.isSafeListeningWarning}"

            try {
                val diagnosis = aiErrorDoctor.diagnoseError(
                    errorSource = errorSource,
                    errorDetail = errorDetail,
                    currentRepoUrl = prefs.githubRepoUrl,
                    appContextSummary = contextSummary
                )

                var autoAppliedNote: String? = null
                when (diagnosis.autoFixAction) {
                    AutoFixAction.NORMALIZE_GITHUB_URL -> {
                        val candidate = diagnosis.suggestedRepoUrl
                        if (!candidate.isNullOrBlank() &&
                            updateManager.parseOwnerRepo(candidate) != null &&
                            candidate != prefs.githubRepoUrl
                        ) {
                            prefsRepo.setGithubRepoUrl(candidate)
                            autoAppliedNote = "Auto-updated GitHub URL to $candidate"
                        }
                    }
                    AutoFixAction.SWITCH_TO_TEST_SIGNAL_DSP -> {
                        if (!a.isTestTonePlaying) {
                            audioEngine.toggleAcousticTestSignal()
                            autoAppliedNote = "Auto-switched DSP to dedicated AudioTrack session."
                        }
                    }
                    AutoFixAction.ENABLE_SOFT_LIMITER -> {
                        audioEngine.setLimiterActive(true)
                        if (a.volumeEnhanceDb > 6f) {
                            audioEngine.setVolumeEnhanceDb(5.0f)
                        }
                        autoAppliedNote = "Auto-armed -1.0 dBFS Soft Limiter and stabilized gain."
                    }
                    AutoFixAction.TRIGGER_BLE_GATT_PROBE -> {
                        btManager.refreshConnectedDevices()
                        if (d.isConnected) {
                            btManager.probeConnectedDeviceGatt()
                            autoAppliedNote = "Auto-refreshed Bluetooth profiles and started BLE GATT probe."
                        }
                    }
                    AutoFixAction.REQUEST_BT_PERMISSIONS,
                    AutoFixAction.OPEN_BT_SETTINGS,
                    AutoFixAction.SHOW_CI_PATCH,
                    AutoFixAction.NONE -> Unit
                }

                _aiDoctorState.value = AiDoctorUiState.Diagnosed(
                    diagnosis.copy(autoAppliedNote = autoAppliedNote)
                )
            } catch (e: TimeoutCancellationException) {
                _aiDoctorState.value = AiDoctorUiState.Error(
                    "AI Error Doctor timed out after 60s. Check internet connection and retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiDoctorState.value = AiDoctorUiState.Error(
                    e.localizedMessage ?: "AI Error Doctor failed to analyze error."
                )
            }
        }
    }

    fun canInstallPackages(): Boolean = updateManager.canInstallPackages()
    fun unknownSourcesSettingsIntent(): Intent = updateManager.createUnknownSourcesSettingsIntent()
    fun packageInstallerIntent(apkPath: String): Intent? = updateManager.createPackageInstallerIntent(apkPath)
}
