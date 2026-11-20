package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Spatializer
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.example.model.AudioEngineState
import com.example.model.EqPreset
import com.example.model.PresetCatalog
import com.example.model.SoundProfileMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class UltraSoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(AudioEngineState())
    val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var legacyEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var testToneJob: Job? = null
    private var testAudioTrack: AudioTrack? = null
    private var attachedSessionId: Int = -1
    private var safeThresholdPct: Int = 80

    private val bandCenterFreqs = floatArrayOf(
        31.25f, 62.5f, 125f, 250f, 500f,
        1000f, 2000f, 4000f, 8000f, 16000f
    )

    init {
        refreshSystemAudioTelemetry()
        attachEffectsToSession(0)
        scope.launch {
            while (isActive) {
                refreshSystemAudioTelemetry()
                delay(1500L)
            }
        }
    }

    fun updateSafeListeningThreshold(pct: Int) {
        safeThresholdPct = pct.coerceIn(60, 95)
        refreshSystemAudioTelemetry()
    }

    fun restoreSavedState(saved: AudioEngineState) {
        _state.update {
            saved.copy(
                isSpatializerAvailable = it.isSpatializerAvailable,
                isSpatializerEnabled = it.isSpatializerEnabled,
                currentStreamVolumePct = it.currentStreamVolumePct,
                isTestTonePlaying = it.isTestTonePlaying,
                activeSessionId = attachedSessionId.coerceAtLeast(0),
                usingGlobalMix = attachedSessionId <= 0,
                isDynamicsProcessingActive = dynamicsProcessing != null
            )
        }
        applyAllEffects()
    }

    fun setEngineEnabled(enabled: Boolean) {
        _state.update { it.copy(isEngineEnabled = enabled) }
        applyAllEffects()
    }

    fun setLimiterActive(enabled: Boolean) {
        _state.update { it.copy(isLimiterActive = enabled) }
        applyAllEffects()
    }

    fun applyPreset(preset: EqPreset) {
        _state.update {
            it.copy(
                activePresetId = preset.id,
                bandsDb = preset.bandsDb.take(10).let { list ->
                    if (list.size == 10) list else List(10) { idx -> list.getOrElse(idx) { 0f } }
                },
                bassBoost = preset.bassBoost,
                trebleBoost = preset.trebleBoost,
                vocalBoost = preset.vocalBoost,
                clarity = preset.clarity,
                volumeEnhanceDb = preset.loudnessGainDb,
                spatialVirtualizer = preset.spatialAmount
            )
        }
        applyAllEffects()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..9) return
        val clamped = gainDb.coerceIn(-12f, 12f)
        _state.update { current ->
            val updated = current.bandsDb.toMutableList()
            updated[bandIndex] = clamped
            current.copy(bandsDb = updated, activePresetId = "custom")
        }
        applyAllEffects()
    }

    fun setBassBoost(amount: Float) {
        _state.update { it.copy(bassBoost = amount.coerceIn(0f, 1f), activePresetId = "custom") }
        applyAllEffects()
    }

    fun setTrebleBoost(amount: Float) {
        _state.update { it.copy(trebleBoost = amount.coerceIn(0f, 1f), activePresetId = "custom") }
        applyAllEffects()
    }

    fun setVocalBoost(amount: Float) {
        _state.update { it.copy(vocalBoost = amount.coerceIn(0f, 1f), activePresetId = "custom") }
        applyAllEffects()
    }

    fun setClarity(amount: Float) {
        _state.update { it.copy(clarity = amount.coerceIn(0f, 1f), activePresetId = "custom") }
        applyAllEffects()
    }

    fun setVolumeEnhanceDb(db: Float) {
        _state.update { it.copy(volumeEnhanceDb = db.coerceIn(0f, 10f)) }
        applyAllEffects()
        refreshSystemAudioTelemetry()
    }

    fun setStereoBalance(balance: Float) {
        _state.update { it.copy(stereoBalance = balance.coerceIn(-1f, 1f)) }
        applyAllEffects()
    }

    fun setSpatialVirtualizer(amount: Float) {
        _state.update { it.copy(spatialVirtualizer = amount.coerceIn(0f, 1f)) }
        applyAllEffects()
    }

    fun setSoundProfileMode(mode: SoundProfileMode) {
        val targetPreset = when (mode) {
            SoundProfileMode.MUSIC -> PresetCatalog.builtInPresets.first { it.id == "original_plus" }
            SoundProfileMode.CINEMA -> PresetCatalog.builtInPresets.first { it.id == "cinema" }
            SoundProfileMode.GAMING -> PresetCatalog.builtInPresets.first { it.id == "gaming" }
        }
        _state.update { it.copy(soundProfileMode = mode) }
        applyPreset(targetPreset)
    }

    fun resetToFlat() {
        _state.update {
            it.copy(
                activePresetId = "flat",
                bandsDb = List(10) { 0f },
                bassBoost = 0f,
                trebleBoost = 0f,
                vocalBoost = 0f,
                clarity = 0f,
                volumeEnhanceDb = 0f,
                stereoBalance = 0f,
                spatialVirtualizer = 0f,
                isLimiterActive = true
            )
        }
        applyAllEffects()
    }

    fun toggleAcousticTestSignal() {
        if (_state.value.isTestTonePlaying) {
            stopAcousticTestSignal()
        } else {
            startAcousticTestSignal()
        }
    }

    private fun startAcousticTestSignal() {
        stopAcousticTestSignal()
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val sessionId = track.audioSessionId
            testAudioTrack = track
            attachEffectsToSession(sessionId)
            track.play()
            _state.update {
                it.copy(
                    isTestTonePlaying = true,
                    activeSessionId = sessionId,
                    usingGlobalMix = false
                )
            }

            testToneJob = scope.launch(Dispatchers.IO) {
                val frames = 1024
                val buffer = ShortArray(frames * 2)
                var phaseBass = 0.0
                var phaseMid = 0.0
                var phaseAir = 0.0
                val twoPi = 2.0 * PI

                while (isActive && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val balance = _state.value.stereoBalance
                    val leftGain = if (balance > 0f) (1f - balance).coerceIn(0f, 1f) else 1f
                    val rightGain = if (balance < 0f) (1f + balance).coerceIn(0f, 1f) else 1f

                    for (i in 0 until frames) {
                        // Warm multi-band acoustic reference chord (65Hz sub + 440Hz mid + 3520Hz air)
                        val sample = (
                            0.35 * sin(phaseBass) +
                            0.28 * sin(phaseMid) +
                            0.12 * sin(phaseAir)
                        )
                        phaseBass = (phaseBass + twoPi * 65.41 / sampleRate) % twoPi
                        phaseMid = (phaseMid + twoPi * 440.0 / sampleRate) % twoPi
                        phaseAir = (phaseAir + twoPi * 3520.0 / sampleRate) % twoPi

                        val pcmLeft = (sample * leftGain * 14000.0).roundToInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        val pcmRight = (sample * rightGain * 14000.0).roundToInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        buffer[i * 2] = pcmLeft
                        buffer[i * 2 + 1] = pcmRight
                    }
                    track.write(buffer, 0, buffer.size)
                }
            }
        } catch (e: Exception) {
            Log.e("UltraSoundEngine", "Failed to start test tone", e)
            stopAcousticTestSignal()
        }
    }

    fun stopAcousticTestSignal() {
        testToneJob?.cancel()
        testToneJob = null
        try {
            testAudioTrack?.stop()
            testAudioTrack?.release()
        } catch (_: Exception) {
        }
        testAudioTrack = null
        _state.update { it.copy(isTestTonePlaying = false) }
        attachEffectsToSession(0)
    }

    private fun attachEffectsToSession(sessionId: Int) {
        if (attachedSessionId == sessionId && (dynamicsProcessing != null || legacyEqualizer != null)) {
            applyAllEffects()
            return
        }
        releaseEffects()
        attachedSessionId = sessionId

        var dpActive = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true, 10,
                    false, 0,
                    false, 0,
                    true
                ).build()
                val dp = DynamicsProcessing(0, sessionId, config)
                val preEq = dp.getPreEqByChannelIndex(0)
                for (i in 0 until 10) {
                    val band = preEq.getBand(i)
                    band.cutoffFrequency = bandCenterFreqs[i]
                    band.isEnabled = true
                }
                dp.setPreEqAllChannelsTo(preEq)
                dp.enabled = _state.value.isEngineEnabled
                dynamicsProcessing = dp
                dpActive = true
            } catch (e: Exception) {
                Log.w("UltraSoundEngine", "DynamicsProcessing unavailable on session $sessionId: ${e.message}")
            }
        }

        if (!dpActive) {
            try {
                legacyEqualizer = Equalizer(0, sessionId).apply {
                    enabled = _state.value.isEngineEnabled
                }
            } catch (e: Exception) {
                Log.w("UltraSoundEngine", "Legacy Equalizer unavailable on session $sessionId: ${e.message}")
            }
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _state.value.isEngineEnabled
            }
        } catch (_: Exception) {
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _state.value.isEngineEnabled
            }
        } catch (_: Exception) {
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = _state.value.isEngineEnabled
            }
        } catch (_: Exception) {
        }

        val status = when {
            dpActive -> "10-BAND DYNAMICS DSP + LIMITER ACTIVE (${if (sessionId == 0) "GLOBAL MIX" else "SESSION $sessionId"})"
            legacyEqualizer != null -> "5-BAND FALLBACK DSP ACTIVE (${if (sessionId == 0) "GLOBAL MIX" else "SESSION $sessionId"})"
            else -> "AUDIO FX RESTRICTED ON GLOBAL MIX — USE STUDIO TEST SIGNAL"
        }

        _state.update {
            it.copy(
                activeSessionId = sessionId,
                usingGlobalMix = sessionId == 0,
                isDynamicsProcessingActive = dpActive,
                engineStatusMessage = status
            )
        }
        applyAllEffects()
    }

    private fun applyAllEffects() {
        val s = _state.value
        val active = s.isEngineEnabled

        // Compute combined 10-band gain curve from manual EQ + Vocal/Treble/Clarity harmonic shelves
        val effectiveBands = FloatArray(10) { idx ->
            val base = s.bandsDb.getOrElse(idx) { 0f }
            val vocalAdd = when (idx) {
                4 -> s.vocalBoost * 2.0f
                5 -> s.vocalBoost * 4.0f
                6 -> s.vocalBoost * 4.5f
                7 -> s.vocalBoost * 2.5f
                else -> 0f
            }
            val trebleAdd = when (idx) {
                7 -> s.trebleBoost * 2.5f
                8 -> s.trebleBoost * 4.5f
                9 -> s.trebleBoost * 5.0f
                else -> 0f
            }
            val clarityAdd = when (idx) {
                2, 3 -> -s.clarity * 1.5f
                6, 7 -> s.clarity * 3.0f
                8 -> s.clarity * 2.5f
                else -> 0f
            }
            (base + vocalAdd + trebleAdd + clarityAdd).coerceIn(-12f, 12f)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                try {
                    dp.enabled = active
                    // Apply Stereo Balance via per-channel input gain (0 dB nominal, up to -18 dB attenuation on opposite channel)
                    val leftBalanceDb = if (s.stereoBalance > 0f) -18f * s.stereoBalance else 0f
                    val rightBalanceDb = if (s.stereoBalance < 0f) 18f * s.stereoBalance else 0f
                    dp.setInputGainbyChannel(0, leftBalanceDb)
                    dp.setInputGainbyChannel(1, rightBalanceDb)

                    for (bandIdx in 0 until 10) {
                        val band = DynamicsProcessing.EqBand(true, bandCenterFreqs[bandIdx], effectiveBands[bandIdx])
                        dp.setPreEqBandAllChannelsTo(bandIdx, band)
                    }

                    // Soft Limiter to prevent digital clipping & distortion
                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        s.isLimiterActive && active,
                        0,
                        1.0f,    // attack ms
                        60.0f,   // release ms
                        4.0f,    // ratio
                        -1.0f,   // threshold dBFS
                        0.0f     // postGain dB
                    )
                    dp.setLimiterAllChannelsTo(limiter)
                } catch (e: Exception) {
                    Log.w("UltraSoundEngine", "Failed updating DynamicsProcessing: ${e.message}")
                }
            }
        }

        legacyEqualizer?.let { eq ->
            try {
                eq.enabled = active
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minMb = range[0].toFloat()
                val maxMb = range[1].toFloat()
                for (b in 0 until numBands) {
                    val centerHz = eq.getCenterFreq(b.toShort()) / 1000f
                    val nearestIdx = bandCenterFreqs.indices.minByOrNull { kotlin.math.abs(bandCenterFreqs[it] - centerHz) } ?: 0
                    val targetMb = (effectiveBands[nearestIdx] * 100f).coerceIn(minMb, maxMb)
                    eq.setBandLevel(b.toShort(), targetMb.toInt().toShort())
                }
            } catch (_: Exception) {
            }
        }

        bassBoost?.let { bb ->
            try {
                bb.enabled = active && s.bassBoost > 0.01f
                if (bb.strengthSupported) {
                    bb.setStrength((s.bassBoost * 1000f).roundToInt().coerceIn(0, 1000).toShort())
                }
            } catch (_: Exception) {
            }
        }

        virtualizer?.let { virt ->
            try {
                virt.enabled = active && s.spatialVirtualizer > 0.01f
                if (virt.strengthSupported) {
                    virt.setStrength((s.spatialVirtualizer * 1000f).roundToInt().coerceIn(0, 1000).toShort())
                }
            } catch (_: Exception) {
            }
        }

        loudnessEnhancer?.let { le ->
            try {
                le.enabled = active && s.volumeEnhanceDb > 0.1f
                // Soft-capped at 1000 mB (+10 dB) with limiter protection
                val gainMb = (s.volumeEnhanceDb * 100f).roundToInt().coerceIn(0, 1000)
                le.setTargetGain(gainMb)
            } catch (_: Exception) {
            }
        }
    }

    private fun refreshSystemAudioTelemetry() {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = max(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        val volPct = ((currentVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        var spatialAvail = false
        var spatialEnabled = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val spatializer = audioManager.spatializer
                spatialAvail = spatializer.isAvailable &&
                    spatializer.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
                spatialEnabled = spatializer.isEnabled
            } catch (_: Exception) {
            }
        }

        val effectiveSplIndex = min(100, volPct + (_state.value.volumeEnhanceDb * 2.5f).roundToInt())
        val warning = effectiveSplIndex >= safeThresholdPct

        _state.update {
            it.copy(
                currentStreamVolumePct = volPct,
                isSpatializerAvailable = spatialAvail,
                isSpatializerEnabled = spatialEnabled,
                isSafeListeningWarning = warning
            )
        }
    }

    fun release() {
        stopAcousticTestSignal()
        releaseEffects()
    }

    private fun releaseEffects() {
        try { dynamicsProcessing?.release() } catch (_: Exception) {}
        try { legacyEqualizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        dynamicsProcessing = null
        legacyEqualizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
