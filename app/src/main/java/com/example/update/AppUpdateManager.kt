package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.PreferencesRepository
import com.example.model.UpdateHistoryEntry
import com.example.model.UpdateStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val appContext = context.applicationContext

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val apiService: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private var lastAvailableRelease: UpdateStatus.Available? = null

    fun currentVersionName(): String = BuildConfig.VERSION_NAME

    fun parseOwnerRepo(repoUrl: String): Pair<String, String>? {
        val cleaned = repoUrl.trim().removeSuffix("/").removeSuffix(".git")
        val regex = Regex("""^(?:https://github\.com/)?([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
        val match = regex.matchEntire(cleaned) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    suspend fun checkForUpdates(repoUrl: String, recordHistory: Boolean = true): UpdateStatus {
        val parsed = parseOwnerRepo(repoUrl)
        if (parsed == null) {
            val err = UpdateStatus.Error("Invalid GitHub repository URL. Use https://github.com/owner/repo")
            _status.value = err
            return err
        }

        _status.value = UpdateStatus.Checking
        val currentVersion = currentVersionName()

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLatestRelease(parsed.first, parsed.second)
                if (!response.isSuccessful) {
                    val msg = when (response.code()) {
                        404 -> "No published GitHub Releases found at ${parsed.first}/${parsed.second}."
                        403 -> "GitHub API rate limit reached. Try again later."
                        else -> "GitHub API error (HTTP ${response.code()})."
                    }
                    val err = UpdateStatus.Error(msg)
                    _status.value = err
                    return@withContext err
                }

                val release = response.body() ?: run {
                    val err = UpdateStatus.Error("Empty response from GitHub Releases API.")
                    _status.value = err
                    return@withContext err
                }

                val latestVer = release.tagName.trim().removePrefix("v").removePrefix("V")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

                if (compareSemVer(latestVer, currentVersion) <= 0) {
                    val upToDate = UpdateStatus.UpToDate(
                        currentVersion = currentVersion,
                        checkedAt = timestamp
                    )
                    _status.value = upToDate
                    if (recordHistory) {
                        preferencesRepository.appendUpdateHistory(
                            UpdateHistoryEntry(
                                timestamp = timestamp,
                                version = currentVersion,
                                status = "UP TO DATE",
                                sha256Short = "LOCAL BUILD"
                            )
                        )
                    }
                    return@withContext upToDate
                }

                val apkAsset = release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true) &&
                        asset.browserDownloadUrl.startsWith("https://", ignoreCase = true)
                }

                if (apkAsset == null) {
                    val err = UpdateStatus.Error(
                        "Release $latestVer found, but no HTTPS .apk asset is attached yet."
                    )
                    _status.value = err
                    return@withContext err
                }

                val expectedSha = resolveExpectedSha256(release)
                val available = UpdateStatus.Available(
                    currentVersion = currentVersion,
                    latestVersion = latestVer,
                    releaseTitle = release.name?.ifBlank { "Release $latestVer" } ?: "Release $latestVer",
                    releaseNotes = release.body?.ifBlank { "No release notes provided." }
                        ?: "No release notes provided.",
                    apkDownloadUrl = apkAsset.browserDownloadUrl,
                    apkFileName = apkAsset.name,
                    apkSizeBytes = apkAsset.size,
                    expectedSha256 = expectedSha,
                    htmlUrl = release.htmlUrl
                )
                lastAvailableRelease = available
                _status.value = available
                if (recordHistory) {
                    preferencesRepository.appendUpdateHistory(
                        UpdateHistoryEntry(
                            timestamp = timestamp,
                            version = latestVer,
                            status = "AVAILABLE",
                            sha256Short = expectedSha?.take(12) ?: "UNLISTED"
                        )
                    )
                }
                available
            } catch (e: Exception) {
                val err = UpdateStatus.Error(
                    "Network error checking GitHub Releases: ${e.localizedMessage ?: "Offline"}"
                )
                _status.value = err
                err
            }
        }
    }

    suspend fun downloadAndVerifyUpdate(available: UpdateStatus.Available? = lastAvailableRelease) {
        val target = available ?: run {
            _status.value = UpdateStatus.Error("No update release selected.")
            return
        }

        if (!target.apkDownloadUrl.startsWith("https://", ignoreCase = true)) {
            _status.value = UpdateStatus.Error("Blocked non-HTTPS download URL for security.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _status.value = UpdateStatus.Downloading(
                    latestVersion = target.latestVersion,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = target.apkSizeBytes
                )

                val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
                updatesDir.listFiles()?.forEach { it.delete() }
                val safeFileName = target.apkFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val outFile = File(updatesDir, safeFileName)

                val request = Request.Builder()
                    .url(target.apkDownloadUrl)
                    .header("User-Agent", "HardikBudsProUltraMax-Android")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _status.value = UpdateStatus.Error("Download failed (HTTP ${response.code})")
                        return@withContext
                    }

                    val body = response.body ?: run {
                        _status.value = UpdateStatus.Error("Empty APK download stream")
                        return@withContext
                    }

                    val contentLen = body.contentLength().takeIf { it > 0L } ?: target.apkSizeBytes.coerceAtLeast(1L)
                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloaded = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(outFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                downloaded += read
                                val pct = (downloaded.toFloat() / contentLen.toFloat()).coerceIn(0f, 1f)
                                _status.value = UpdateStatus.Downloading(
                                    latestVersion = target.latestVersion,
                                    progress = pct,
                                    downloadedBytes = downloaded,
                                    totalBytes = contentLen
                                )
                            }
                        }
                    }

                    _status.value = UpdateStatus.Verifying(target.latestVersion)
                    val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    val expected = target.expectedSha256?.lowercase()
                    val matched = expected?.let { it == actualSha256 }

                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                    if (matched == false) {
                        outFile.delete()
                        preferencesRepository.appendUpdateHistory(
                            UpdateHistoryEntry(
                                timestamp = timestamp,
                                version = target.latestVersion,
                                status = "SHA-256 MISMATCH",
                                sha256Short = actualSha256.take(12)
                            )
                        )
                        _status.value = UpdateStatus.Error(
                            "Security verification failed: SHA-256 mismatch! File deleted."
                        )
                        return@withContext
                    }

                    preferencesRepository.appendUpdateHistory(
                        UpdateHistoryEntry(
                            timestamp = timestamp,
                            version = target.latestVersion,
                            status = if (matched == true) "VERIFIED READY" else "DOWNLOADED",
                            sha256Short = actualSha256.take(12)
                        )
                    )

                    _status.value = UpdateStatus.ReadyToInstall(
                        latestVersion = target.latestVersion,
                        apkPath = outFile.absolutePath,
                        verifiedSha256 = actualSha256,
                        checksumMatched = matched,
                        releaseNotes = target.releaseNotes
                    )
                }
            } catch (e: Exception) {
                _status.value = UpdateStatus.Error(
                    "Download error: ${e.localizedMessage ?: "Network interrupted"}"
                )
            }
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createUnknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun createPackageInstallerIntent(apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.exists()) return null
        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun resolveExpectedSha256(release: GitHubReleaseDto): String? {
        // 1. Check .sha256 companion asset
        val shaAsset = release.assets.firstOrNull {
            (it.name.endsWith(".sha256", ignoreCase = true) ||
                it.name.contains("checksum", ignoreCase = true)) &&
                it.browserDownloadUrl.startsWith("https://", ignoreCase = true)
        }
        if (shaAsset != null) {
            runCatching {
                val req = Request.Builder()
                    .url(shaAsset.browserDownloadUrl)
                    .header("User-Agent", "HardikBudsProUltraMax-Android")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val text = resp.body?.string().orEmpty()
                        val match = Regex("""\b([a-fA-F0-9]{64})\b""").find(text)
                        if (match != null) return match.groupValues[1].lowercase()
                    }
                }
            }
        }

        // 2. Fallback: extract 64-char SHA-256 hex digest from release notes body
        val body = release.body.orEmpty()
        val bodyMatch = Regex("""\b([a-fA-F0-9]{64})\b""").find(body)
        return bodyMatch?.groupValues?.get(1)?.lowercase()
    }

    companion object {
        fun compareSemVer(remote: String, local: String): Int {
            val rParts = remote.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
            val lParts = local.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(rParts.size, lParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r != l) return r.compareTo(l)
            }
            return 0
        }
    }
}
