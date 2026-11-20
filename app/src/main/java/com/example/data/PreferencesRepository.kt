package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppPreferencesState
import com.example.model.AudioEngineState
import com.example.model.EqPreset
import com.example.model.PresetCatalog
import com.example.model.SoundProfileMode
import com.example.model.UpdateHistoryEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hardik_buds_prefs")

class PreferencesRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val presetListType = Types.newParameterizedType(List::class.java, EqPreset::class.java)
    private val presetAdapter = moshi.adapter<List<EqPreset>>(presetListType)

    private val historyListType = Types.newParameterizedType(List::class.java, UpdateHistoryEntry::class.java)
    private val historyAdapter = moshi.adapter<List<UpdateHistoryEntry>>(historyListType)

    private val floatListType = Types.newParameterizedType(List::class.java, java.lang.Float::class.java)
    private val floatListAdapter = moshi.adapter<List<Float>>(floatListType)

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val AUTO_UPDATE_CHECK = booleanPreferencesKey("auto_update_check")
        val SAFE_LISTENING_THRESHOLD = intPreferencesKey("safe_listening_threshold")
        val GITHUB_REPO_URL = stringPreferencesKey("github_repo_url")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        val UPDATE_HISTORY_JSON = stringPreferencesKey("update_history_json")

        val ENGINE_ENABLED = booleanPreferencesKey("engine_enabled")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        val EQ_BANDS_JSON = stringPreferencesKey("eq_bands_json")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val TREBLE_BOOST = floatPreferencesKey("treble_boost")
        val VOCAL_BOOST = floatPreferencesKey("vocal_boost")
        val CLARITY = floatPreferencesKey("clarity")
        val VOLUME_ENHANCE_DB = floatPreferencesKey("volume_enhance_db")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val SPATIAL_VIRTUALIZER = floatPreferencesKey("spatial_virtualizer")
        val SOUND_PROFILE_MODE = stringPreferencesKey("sound_profile_mode")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val customPresets = prefs[Keys.CUSTOM_PRESETS_JSON]?.let { json ->
            runCatching { presetAdapter.fromJson(json) }.getOrNull()
        } ?: emptyList()

        val history = prefs[Keys.UPDATE_HISTORY_JSON]?.let { json ->
            runCatching { historyAdapter.fromJson(json) }.getOrNull()
        } ?: emptyList()

        AppPreferencesState(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            autoReconnectEnabled = prefs[Keys.AUTO_RECONNECT] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            autoUpdateCheckEnabled = prefs[Keys.AUTO_UPDATE_CHECK] ?: true,
            safeListeningThresholdPct = prefs[Keys.SAFE_LISTENING_THRESHOLD] ?: 80,
            githubRepoUrl = prefs[Keys.GITHUB_REPO_URL]
                ?: "https://github.com/hardik-audio/hardik-buds-pro-ultra-max",
            customPresets = customPresets,
            updateHistory = history
        )
    }

    val savedAudioStateFlow: Flow<AudioEngineState> = context.dataStore.data.map { prefs ->
        val defaultPreset = PresetCatalog.builtInPresets[1] // ORIGINAL PLUS
        val savedBands = prefs[Keys.EQ_BANDS_JSON]?.let { json ->
            runCatching { floatListAdapter.fromJson(json) }.getOrNull()
        }?.takeIf { it.size == 10 } ?: defaultPreset.bandsDb

        val modeName = prefs[Keys.SOUND_PROFILE_MODE] ?: SoundProfileMode.MUSIC.name
        val profileMode = SoundProfileMode.entries.find { it.name == modeName } ?: SoundProfileMode.MUSIC

        AudioEngineState(
            isEngineEnabled = prefs[Keys.ENGINE_ENABLED] ?: true,
            isLimiterActive = prefs[Keys.LIMITER_ENABLED] ?: true,
            activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: defaultPreset.id,
            bandsDb = savedBands,
            bassBoost = prefs[Keys.BASS_BOOST] ?: defaultPreset.bassBoost,
            trebleBoost = prefs[Keys.TREBLE_BOOST] ?: defaultPreset.trebleBoost,
            vocalBoost = prefs[Keys.VOCAL_BOOST] ?: defaultPreset.vocalBoost,
            clarity = prefs[Keys.CLARITY] ?: defaultPreset.clarity,
            volumeEnhanceDb = prefs[Keys.VOLUME_ENHANCE_DB] ?: defaultPreset.loudnessGainDb,
            stereoBalance = prefs[Keys.STEREO_BALANCE] ?: 0f,
            spatialVirtualizer = prefs[Keys.SPATIAL_VIRTUALIZER] ?: defaultPreset.spatialAmount,
            soundProfileMode = profileMode
        )
    }

    suspend fun saveAudioState(state: AudioEngineState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENGINE_ENABLED] = state.isEngineEnabled
            prefs[Keys.LIMITER_ENABLED] = state.isLimiterActive
            prefs[Keys.ACTIVE_PRESET_ID] = state.activePresetId
            prefs[Keys.EQ_BANDS_JSON] = floatListAdapter.toJson(state.bandsDb)
            prefs[Keys.BASS_BOOST] = state.bassBoost
            prefs[Keys.TREBLE_BOOST] = state.trebleBoost
            prefs[Keys.VOCAL_BOOST] = state.vocalBoost
            prefs[Keys.CLARITY] = state.clarity
            prefs[Keys.VOLUME_ENHANCE_DB] = state.volumeEnhanceDb
            prefs[Keys.STEREO_BALANCE] = state.stereoBalance
            prefs[Keys.SPATIAL_VIRTUALIZER] = state.spatialVirtualizer
            prefs[Keys.SOUND_PROFILE_MODE] = state.soundProfileMode.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setAutoUpdateCheck(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_UPDATE_CHECK] = enabled }
    }

    suspend fun setSafeListeningThreshold(pct: Int) {
        context.dataStore.edit { it[Keys.SAFE_LISTENING_THRESHOLD] = pct.coerceIn(60, 95) }
    }

    suspend fun setGithubRepoUrl(url: String) {
        context.dataStore.edit { it[Keys.GITHUB_REPO_URL] = url.trim() }
    }

    suspend fun saveCustomPreset(preset: EqPreset) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PRESETS_JSON]?.let {
                runCatching { presetAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val updated = (current.filterNot { it.id == preset.id } + preset)
            prefs[Keys.CUSTOM_PRESETS_JSON] = presetAdapter.toJson(updated)
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PRESETS_JSON]?.let {
                runCatching { presetAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.CUSTOM_PRESETS_JSON] = presetAdapter.toJson(current.filterNot { it.id == presetId })
        }
    }

    suspend fun appendUpdateHistory(entry: UpdateHistoryEntry) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UPDATE_HISTORY_JSON]?.let {
                runCatching { historyAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(entry) + current).take(20)
            prefs[Keys.UPDATE_HISTORY_JSON] = historyAdapter.toJson(updated)
        }
    }
}
