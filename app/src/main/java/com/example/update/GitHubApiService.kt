package com.example.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "prerelease") val prerelease: Boolean = false,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "assets") val assets: List<GitHubAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubAssetDto(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "size") val size: Long,
    @Json(name = "content_type") val contentType: String?
)

interface GitHubApiService {
    @Headers(
        "Accept: application/vnd.github+json",
        "User-Agent: HardikBudsProUltraMax-Android"
    )
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubReleaseDto>
}
