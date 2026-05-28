




package iad1tya.echo.music.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.App
import iad1tya.echo.music.constants.GitHubReleasesEtagKey
import iad1tya.echo.music.constants.GitHubReleasesFingerprintKey
import iad1tya.echo.music.constants.GitHubReleasesJsonKey
import iad1tya.echo.music.constants.GitHubReleasesLastCheckedAtKey
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String,
    val downloadUrl: String? = null,
    val downloadAssetName: String? = null,
    val downloadAssetSize: Long? = null,
)

private data class ReleasesNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

object Updater {
    private val client = HttpClient()
    private const val ReleaseCacheCheckIntervalMs: Long = 6 * 60 * 60 * 1000L
    private const val LatestReleasePageUrl = "https://github.com/EchoMusicApp/Echo-Music/releases/latest"
    private const val ApkMimeType = "application/vnd.android.package-archive"
    private const val MaxRedirects = 5
    private const val MinApkBytes = 1L * 1024L * 1024L
    private const val MaxApkBytes = 250L * 1024L * 1024L
    private val TrustedDownloadHosts =
        setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "github-releases.githubusercontent.com",
        )
    var lastCheckTime = -1L
        private set

    private data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: List<PreReleaseIdentifier>,
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            val majorCompare = major.compareTo(other.major)
            if (majorCompare != 0) return majorCompare
            val minorCompare = minor.compareTo(other.minor)
            if (minorCompare != 0) return minorCompare
            val patchCompare = patch.compareTo(other.patch)
            if (patchCompare != 0) return patchCompare

            val thisIsStable = preRelease.isEmpty()
            val otherIsStable = other.preRelease.isEmpty()
            if (thisIsStable && !otherIsStable) return 1
            if (!thisIsStable && otherIsStable) return -1

            val maxIndex = minOf(preRelease.size, other.preRelease.size)
            for (i in 0 until maxIndex) {
                val c = preRelease[i].compareTo(other.preRelease[i])
                if (c != 0) return c
            }
            return preRelease.size.compareTo(other.preRelease.size)
        }

        fun normalizedName(): String =
            if (preRelease.isEmpty()) {
                "$major.$minor.$patch"
            } else {
                "$major.$minor.$patch-" + preRelease.joinToString(".") { it.raw }
            }
    }

    private sealed interface PreReleaseIdentifier : Comparable<PreReleaseIdentifier> {
        val raw: String
    }

    private data class NumericIdentifier(
        override val raw: String,
        val value: Long,
    ) : PreReleaseIdentifier {
        override fun compareTo(other: PreReleaseIdentifier): Int =
            when (other) {
                is NumericIdentifier -> value.compareTo(other.value)
                is AlphaIdentifier -> -1
            }
    }

    private data class AlphaIdentifier(
        override val raw: String,
    ) : PreReleaseIdentifier {
        override fun compareTo(other: PreReleaseIdentifier): Int =
            when (other) {
                is NumericIdentifier -> 1
                is AlphaIdentifier -> raw.compareTo(other.raw)
            }
    }

    private val semVerRegex =
        Regex("""(?i)\bv?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?\b""")

    private fun parseSemVerOrNull(text: String): SemVer? {
        val match = semVerRegex.find(text) ?: return null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return null
        val preReleaseText = match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }
        val preRelease =
            preReleaseText
                ?.split('.')
                ?.filter { it.isNotBlank() }
                ?.map { identifier ->
                    if (identifier.all { it.isDigit() }) {
                        NumericIdentifier(raw = identifier, value = identifier.toLong())
                    } else {
                        AlphaIdentifier(raw = identifier)
                    }
                }
                ?: emptyList()
        return SemVer(
            major = major,
            minor = minor,
            patch = patch,
            preRelease = preRelease,
        )
    }

    private fun parseReleaseSemVerOrNull(release: ReleaseInfo): SemVer? =
        parseSemVerOrNull(release.tagName) ?: parseSemVerOrNull(release.name)

    internal fun isSameVersion(a: String, b: String): Boolean {
        val aSemVer = parseSemVerOrNull(a)
        val bSemVer = parseSemVerOrNull(b)
        return if (aSemVer != null && bSemVer != null) {
            aSemVer.major == bSemVer.major &&
                aSemVer.minor == bSemVer.minor &&
                aSemVer.patch == bSemVer.patch &&
                aSemVer.preRelease == bSemVer.preRelease
        } else {
            a.trim() == b.trim()
        }
    }

    internal fun isNewerVersion(available: String, current: String): Boolean {
        val availableSemVer = parseSemVerOrNull(available)
        val currentSemVer = parseSemVerOrNull(current)
        return if (availableSemVer != null && currentSemVer != null) {
            availableSemVer > currentSemVer
        } else {
            !isSameVersion(available, current)
        }
    }

    internal fun findLatestRelease(releases: List<ReleaseInfo>): ReleaseInfo? {
        if (releases.isEmpty()) return null
        val parsed =
            releases.mapNotNull { release ->
                parseReleaseSemVerOrNull(release)?.let { version -> version to release }
            }

        if (parsed.isEmpty()) return releases.firstOrNull()

        val stable = parsed.filter { it.first.preRelease.isEmpty() }
        val candidates = stable.ifEmpty { parsed }
        return candidates.maxWithOrNull(compareBy({ it.first }, { it.second.publishedAt }))?.second
    }

    private fun preferredReleaseVersionNameOrNull(release: ReleaseInfo): String? =
        parseReleaseSemVerOrNull(release)?.normalizedName()

    fun getReleaseVersionName(release: ReleaseInfo): String =
        preferredReleaseVersionNameOrNull(release) ?: release.name.ifBlank { release.tagName }

    private fun isApkAssetName(nameOrUrl: String): Boolean =
        nameOrUrl.lowercase(Locale.US).endsWith(".apk")

    private fun apkAssetScore(name: String, url: String, architecture: String): Int {
        val normalized = "$name $url".lowercase(Locale.US)
        if (!isApkAssetName(name) && !isApkAssetName(url)) return -1

        val isUniversal = "universal" in normalized || "all" in normalized
        val architectureMatch =
            when (architecture.lowercase(Locale.US)) {
                "universal" -> isUniversal
                "arm64" -> "arm64" in normalized || "arm64-v8a" in normalized || "aarch64" in normalized
                "armeabi" -> "armeabi" in normalized || "armv7" in normalized || "armeabi-v7a" in normalized
                "x86" -> "x86" in normalized && "x86_64" !in normalized && "x86-64" !in normalized
                "x86_64" -> "x86_64" in normalized || "x86-64" in normalized || "x64" in normalized
                else -> false
            }

        return when {
            architectureMatch -> 300
            isUniversal -> 200
            "release" in normalized -> 100
            else -> 10
        }
    }

    private fun selectPreferredApkAsset(assets: JSONArray): JSONObject? {
        var selected: JSONObject? = null
        var selectedScore = -1
        var selectedSize = -1L

        for (j in 0 until assets.length()) {
            val asset = assets.getJSONObject(j)
            val name = asset.optString("name", "")
            val url = asset.optString("browser_download_url", "")
            val score = apkAssetScore(name = name, url = url, architecture = BuildConfig.ARCHITECTURE)
            if (score < 0) continue

            val size = asset.optLong("size", 0L)
            if (score > selectedScore || (score == selectedScore && size > selectedSize)) {
                selected = asset
                selectedScore = score
                selectedSize = size
            }
        }

        return selected
    }

    private fun parseReleasesJson(
        json: String,
    ): List<ReleaseInfo> {
        val jsonArray = JSONArray(json)
        val releases = ArrayList<ReleaseInfo>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            
            val apkAsset =
                if (item.has("assets")) {
                    selectPreferredApkAsset(item.getJSONArray("assets"))
                } else {
                    null
                }

            releases.add(
                ReleaseInfo(
                    tagName = item.optString("tag_name", ""),
                    name = item.optString("name", ""),
                    body = if (item.has("body")) item.optString("body") else null,
                    publishedAt = item.optString("published_at", ""),
                    htmlUrl = item.optString("html_url", ""),
                    downloadUrl = apkAsset?.optString("browser_download_url", "")?.takeIf { it.isNotBlank() },
                    downloadAssetName = apkAsset?.optString("name", "")?.takeIf { it.isNotBlank() },
                    downloadAssetSize = apkAsset?.optLong("size", 0L)?.takeIf { it > 0L },
                )
            )
        }
        return releases
    }

    private fun getTopReleaseFingerprint(releases: List<ReleaseInfo>): String {
        val latest = findLatestRelease(releases) ?: return ""
        return listOf(
            latest.tagName,
            latest.name,
            latest.publishedAt,
            latest.body.orEmpty(),
            latest.htmlUrl,
        ).joinToString("||")
    }

    private suspend fun fetchReleasesNetwork(
        perPage: Int,
        cachedEtag: String?,
    ): ReleasesNetworkResult {
        val response: HttpResponse =
            client.get("https://api.github.com/repos/EchoMusicApp/Echo-Music/releases?per_page=$perPage") {
                headers {
                    append("Accept", "application/vnd.github+json")
                    append("User-Agent", "Echo Music")
                    if (!cachedEtag.isNullOrBlank()) {
                        append("If-None-Match", cachedEtag)
                    }
                }
            }
        val etag = response.headers["ETag"]
        return when (response.status) {
            HttpStatusCode.NotModified ->
                ReleasesNetworkResult(
                    status = response.status,
                    body = null,
                    etag = cachedEtag ?: etag,
                )

            else ->
                ReleasesNetworkResult(
                    status = response.status,
                    body = response.bodyAsText(),
                    etag = etag,
                )
        }
    }

    suspend fun getCachedReleases(): List<ReleaseInfo> {
        val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
        return cachedJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestReleaseInfo().map { latest ->
            getReleaseVersionName(latest)
        }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        getLatestReleaseInfo().map { it.body }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> =
        runCatching {
            val releases = getAllReleases().getOrThrow()
            val latest = findLatestRelease(releases)
                ?: throw IllegalStateException("No releases found")
            lastCheckTime = System.currentTimeMillis()
            latest
        }

    suspend fun getCommitHistory(count: Int = 20, branch: String = "main"): Result<List<GitCommit>> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/EchoMusicApp/Echo-Music/commits?sha=$branch&per_page=$count")
                    .bodyAsText()
            val jsonArray = JSONArray(response)
            val commits = mutableListOf<GitCommit>()
            for (i in 0 until jsonArray.length()) {
                val commitObj = jsonArray.getJSONObject(i)
                val commit = commitObj.getJSONObject("commit")
                val authorObj = commit.optJSONObject("author")
                commits.add(
                    GitCommit(
                        sha = commitObj.optString("sha", "").take(7),
                        message = commit.optString("message", "").lines().firstOrNull() ?: "",
                        author = authorObj?.optString("name", "Unknown") ?: "Unknown",
                        date = authorObj?.optString("date", "") ?: "",
                        url = commitObj.optString("html_url", "")
                    )
                )
            }
            commits
        }

    fun getLatestDownloadUrl(): String {
        return LatestReleasePageUrl
    }

    suspend fun downloadLatestApk(onProgress: (Float) -> Unit): Result<File> =
        runCatching {
            withContext(Dispatchers.IO) {
                val latest = getLatestReleaseInfo().getOrThrow()
                val urlString =
                    latest.downloadUrl
                        ?: throw IllegalStateException("The latest release has no APK asset")
                val fileName =
                    sanitizeApkFileName(
                        latest.downloadAssetName
                            ?: "Echo-${getReleaseVersionName(latest)}-${BuildConfig.ARCHITECTURE}.apk"
                    )
                val outputDir = File(App.instance.cacheDir, "updates").apply { mkdirs() }
                val tempFile = File(outputDir, "$fileName.tmp")
                val apkFile = File(outputDir, fileName)

                tempFile.delete()
                downloadToFile(urlString, tempFile, onProgress)
                validateDownloadedApk(App.instance, tempFile)

                if (apkFile.exists() && !apkFile.delete()) {
                    throw IllegalStateException("Could not replace previous update APK")
                }
                if (!tempFile.renameTo(apkFile)) {
                    tempFile.copyTo(apkFile, overwrite = true)
                    tempFile.delete()
                }
                reportProgress(onProgress, 1f)
                apkFile
            }
        }

    suspend fun getAllReleases(
        perPage: Int = 30,
        forceRefresh: Boolean = false,
    ): Result<List<ReleaseInfo>> =
        runCatching {
            val now = System.currentTimeMillis()
            val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
            val cachedEtag = App.instance.dataStore.getAsync(GitHubReleasesEtagKey)
            val lastCheckedAt = App.instance.dataStore.getAsync(GitHubReleasesLastCheckedAtKey, 0L)
            val cachedFingerprint = App.instance.dataStore.getAsync(GitHubReleasesFingerprintKey)

            val cachedReleases =
                cachedJson
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }

            val shouldCheckNetwork =
                forceRefresh || cachedJson.isNullOrBlank() || (now - lastCheckedAt) >= ReleaseCacheCheckIntervalMs

            if (!shouldCheckNetwork) {
                lastCheckTime = now
                return@runCatching cachedReleases ?: emptyList()
            }

            val networkResult = runCatching {
                fetchReleasesNetwork(
                    perPage = perPage,
                    cachedEtag = cachedEtag,
                )
            }.getOrNull()

            if (networkResult == null) {
                val fallback = cachedReleases
                if (fallback != null) {
                    lastCheckTime = now
                    return@runCatching fallback
                }
                throw IllegalStateException("Failed to fetch releases")
            }

            when {
                networkResult.status == HttpStatusCode.NotModified -> {
                    App.instance.dataStore.edit { settings ->
                        settings[GitHubReleasesLastCheckedAtKey] = now
                        networkResult.etag?.let { settings[GitHubReleasesEtagKey] = it }
                    }
                    val fallback = cachedReleases
                    if (fallback != null) {
                        lastCheckTime = now
                        return@runCatching fallback
                    }
                    throw IllegalStateException("Release cache is empty")
                }

                networkResult.status.value in 200..299 && !networkResult.body.isNullOrBlank() -> {
                    val networkBody = networkResult.body
                    val releases = parseReleasesJson(networkBody)
                    val newFingerprint = getTopReleaseFingerprint(releases)
                    val hasPayloadChanged = cachedJson != networkBody
                    val hasTopReleaseChanged = cachedFingerprint != newFingerprint

                    App.instance.dataStore.edit { settings ->
                        settings[GitHubReleasesLastCheckedAtKey] = now
                        networkResult.etag?.let { settings[GitHubReleasesEtagKey] = it }
                        if (hasPayloadChanged || hasTopReleaseChanged || cachedJson.isNullOrBlank()) {
                            settings[GitHubReleasesJsonKey] = networkBody
                            settings[GitHubReleasesFingerprintKey] = newFingerprint
                        }
                    }
                    lastCheckTime = now
                    releases
                }

                else -> {
                    val fallback = cachedReleases
                    if (fallback != null) {
                        lastCheckTime = now
                        fallback
                    } else {
                        throw IllegalStateException("Failed to fetch releases: HTTP ${networkResult.status.value}")
                    }
                }
            }
        }
        
    private suspend fun downloadToFile(
        urlString: String,
        outputFile: File,
        onProgress: (Float) -> Unit,
    ) {
        var url = URL(urlString)
        var redirects = 0

        while (true) {
            validateDownloadUrl(url)
            val connection =
                url.openConnection() as? HttpURLConnection
                    ?: throw IllegalStateException("Unsupported download connection")
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "$ApkMimeType, application/octet-stream")
            connection.setRequestProperty("User-Agent", "Echo Music")

            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    if (redirects >= MaxRedirects) {
                        throw IllegalStateException("Too many redirects while downloading update")
                    }
                    val location =
                        connection.getHeaderField("Location")
                            ?: throw IllegalStateException("Update download redirect did not include Location")
                    url = URL(url, location)
                    redirects += 1
                    continue
                }

                if (status !in 200..299) {
                    throw IllegalStateException("Update download failed: HTTP $status")
                }

                val contentType = connection.contentType.orEmpty().lowercase(Locale.US)
                if ("text/html" in contentType || "text/plain" in contentType) {
                    throw IllegalStateException("Update download returned $contentType instead of an APK")
                }

                val contentLength = connection.contentLengthLong
                if (contentLength > MaxApkBytes) {
                    throw IllegalStateException("Update APK is too large")
                }

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val data = ByteArray(32 * 1024)
                        var total = 0L
                        var lastReportedProgress = 0f
                        while (true) {
                            val count = input.read(data)
                            if (count == -1) break
                            total += count
                            if (total > MaxApkBytes) {
                                throw IllegalStateException("Update APK exceeded the maximum allowed size")
                            }
                            output.write(data, 0, count)

                            if (contentLength > 0) {
                                val progress = (total.toFloat() / contentLength).coerceIn(0f, 1f)
                                if (progress - lastReportedProgress >= 0.01f || progress >= 1f) {
                                    lastReportedProgress = progress
                                    reportProgress(onProgress, progress)
                                }
                            }
                        }
                    }
                }

                if (outputFile.length() < MinApkBytes) {
                    throw IllegalStateException("Downloaded update is too small to be a valid APK")
                }
                return
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun validateDownloadUrl(url: URL) {
        if (!url.protocol.equals("https", ignoreCase = true)) {
            throw IllegalStateException("Update downloads must use HTTPS")
        }
        val host = url.host.lowercase(Locale.US)
        val trusted = host in TrustedDownloadHosts || host.endsWith(".githubusercontent.com")
        if (!trusted) {
            throw IllegalStateException("Untrusted update download host: $host")
        }
    }

    private suspend fun reportProgress(onProgress: (Float) -> Unit, progress: Float) {
        withContext(Dispatchers.Main.immediate) {
            onProgress(progress.coerceIn(0f, 1f))
        }
    }

    private fun sanitizeApkFileName(name: String): String {
        val sanitized =
            name
                .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
                .trim('_')
                .ifBlank { "update.apk" }
        return if (sanitized.endsWith(".apk", ignoreCase = true)) sanitized else "$sanitized.apk"
    }

    private fun validateDownloadedApk(context: Context, apkFile: File) {
        val packageManager = context.packageManager
        val archiveInfo =
            getArchivePackageInfo(packageManager, apkFile)
                ?: throw IllegalStateException("Downloaded update is not a valid APK")
        if (archiveInfo.packageName != context.packageName) {
            throw IllegalStateException("Downloaded APK package does not match this app")
        }

        val installedInfo = getInstalledPackageInfo(packageManager, context.packageName)
        val archiveVersionCode = versionCodeOf(archiveInfo)
        val installedVersionCode = versionCodeOf(installedInfo)
        if (archiveVersionCode <= installedVersionCode) {
            throw IllegalStateException("Downloaded APK is not newer than the installed app")
        }

        val archiveCertificates = signingCertificatesOf(archiveInfo)
        val installedCertificates = signingCertificatesOf(installedInfo)
        if (archiveCertificates.isNotEmpty() &&
            installedCertificates.isNotEmpty() &&
            archiveCertificates.intersect(installedCertificates).isEmpty()
        ) {
            throw IllegalStateException("Downloaded APK signature does not match this app")
        }
    }

    private fun getPackageInfoFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun getArchivePackageInfo(packageManager: PackageManager, apkFile: File): PackageInfo? {
        val flags = getPackageInfoFlags()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        }
    }

    private fun getInstalledPackageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
        val flags = getPackageInfoFlags()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }
    }

    private fun versionCodeOf(packageInfo: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

    private fun signingCertificatesOf(packageInfo: PackageInfo): Set<String> {
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

        return signatures
            ?.map { Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP) }
            ?.toSet()
            .orEmpty()
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> =
        runCatching {
            require(apkFile.isFile) { "Update APK does not exist" }
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.applicationContext.packageName}.FileProvider",
                    apkFile,
                )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, ApkMimeType)
                clipData = ClipData.newUri(context.contentResolver, apkFile.name, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = uri
                    clipData = ClipData.newUri(context.contentResolver, apkFile.name, uri)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.startActivity(fallbackIntent)
            }
        }
}
