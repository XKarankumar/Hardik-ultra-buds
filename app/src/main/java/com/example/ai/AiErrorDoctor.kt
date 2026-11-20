package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AutoFixAction {
    NORMALIZE_GITHUB_URL,
    SWITCH_TO_TEST_SIGNAL_DSP,
    ENABLE_SOFT_LIMITER,
    TRIGGER_BLE_GATT_PROBE,
    REQUEST_BT_PERMISSIONS,
    OPEN_BT_SETTINGS,
    SHOW_CI_PATCH,
    NONE
}

data class AiDiagnosisResult(
    val rootCause: String,
    val autoFixAction: AutoFixAction,
    val suggestedRepoUrl: String?,
    val fixSteps: List<String>,
    val patchSnippet: String?,
    val autoAppliedNote: String? = null
)

sealed interface AiDoctorUiState {
    data object Idle : AiDoctorUiState
    data class Analyzing(val errorSummary: String) : AiDoctorUiState
    data class Diagnosed(val result: AiDiagnosisResult) : AiDoctorUiState
    data class Error(val message: String) : AiDoctorUiState
}

class AiErrorDoctor {

    private val diagnosticSchema = Schema.obj(
        properties = mapOf(
            "rootCause" to Schema.string(
                description = "Clear explanation of the root cause of the GitHub, Bluetooth, Audio DSP, or Gradle error."
            ),
            "autoFixAction" to Schema.enumeration(
                values = AutoFixAction.entries.map { it.name },
                description = "Exact safe in-app recovery action to execute automatically or recommend."
            ),
            "suggestedRepoUrl" to Schema.string(
                description = "Clean https://github.com/owner/repo URL if the error involves a malformed GitHub repo URL, else empty string.",
                nullable = true
            ),
            "fixSteps" to Schema.array(
                items = Schema.string(),
                description = "Ordered actionable steps to fix the issue."
            ),
            "patchSnippet" to Schema.string(
                description = "Optional YAML/Kotlin/Gradle fix snippet if the error is a GitHub Actions or build error.",
                nullable = true
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosticSchema
            }
        )
    }

    suspend fun diagnoseError(
        errorSource: String,
        errorDetail: String,
        currentRepoUrl: String,
        appContextSummary: String
    ): AiDiagnosisResult {
        val prompt = """
            You are HARDIK AI AUTO-DOCTOR embedded inside the Android app "Hardik Buds Pro Ultra Max".
            Diagnose the error below and choose the best structured autoFixAction so the app can self-heal safely.

            Available autoFixAction meanings:
            - NORMALIZE_GITHUB_URL: when the user's GitHub URL is malformed (e.g. contains /releases, .git, http://, extra spaces, or wrong format) and can be normalized to https://github.com/owner/repo.
            - SWITCH_TO_TEST_SIGNAL_DSP: when Android global mix audio session 0 is blocked by OEM ROM and the app should switch Ultra Sound DSP to its internal AudioTrack test session.
            - ENABLE_SOFT_LIMITER: when safe listening alert or clipping/distortion risk is detected.
            - TRIGGER_BLE_GATT_PROBE: when Bluetooth telemetry/RSSI/firmware is missing on an active connection.
            - REQUEST_BT_PERMISSIONS: when Android Bluetooth permissions are denied.
            - OPEN_BT_SETTINGS: when Bluetooth is disabled or pairing needs system Bluetooth panel.
            - SHOW_CI_PATCH: when the user pasted a GitHub Actions / Gradle / SDK / keystore build log error.
            - NONE: when the repository simply has no published release tag yet or needs an external manual step.

            Context:
            - Source: $errorSource
            - Current GitHub Repo URL: $currentRepoUrl
            - Device/Audio Summary: $appContextSummary
            - Error / Log:
            $errorDetail

            Keep rootCause and fixSteps concise, technical, and helpful. Never invent fake hardware ANC controls.
        """.trimIndent()

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty diagnostic payload.")
            val obj = JSONObject(jsonText)
            val rootCause = obj.optString("rootCause", "Diagnostic complete.")
            val actionRaw = obj.optString("autoFixAction", AutoFixAction.NONE.name)
            val action = AutoFixAction.entries.find { it.name == actionRaw } ?: AutoFixAction.NONE
            val suggestedRepo = obj.optString("suggestedRepoUrl", "").trim().takeIf { it.isNotEmpty() }
            val patchSnippet = obj.optString("patchSnippet", "").trim().takeIf { it.isNotEmpty() }
            val stepsArr = obj.optJSONArray("fixSteps")
            val steps = if (stepsArr != null) {
                (0 until stepsArr.length()).mapNotNull { idx ->
                    stepsArr.optString(idx).takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }

            return AiDiagnosisResult(
                rootCause = rootCause,
                autoFixAction = action,
                suggestedRepoUrl = suggestedRepo,
                fixSteps = steps,
                patchSnippet = patchSnippet
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI diagnosis timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI diagnosis failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AiErrorDoctor"
    }
}
