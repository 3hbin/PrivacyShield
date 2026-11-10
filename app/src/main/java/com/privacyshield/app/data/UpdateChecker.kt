package com.privacyshield.app.data

import android.os.Build
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val body: String,
    val apk: ReleaseAsset?,
    val checksum: String?
)

sealed interface UpdateCheck {
    data object NoRepo : UpdateCheck
    data class UpToDate(val current: String) : UpdateCheck
    data class Available(val current: String, val release: ReleaseInfo) : UpdateCheck
    data class Failed(val message: String) : UpdateCheck
}

class UpdateChecker {
    fun check(owner: String, repo: String, currentVersion: String): UpdateCheck {
        if (owner.isBlank() || repo.isBlank()) return UpdateCheck.NoRepo
        return try {
            val release = fetchLatest(owner.trim(), repo.trim())
                ?: return UpdateCheck.Failed("Không tìm thấy bản phát hành trên GitHub.")
            if (VersionComparator.isNewer(currentVersion, release.tag)) {
                UpdateCheck.Available(currentVersion, release)
            } else {
                UpdateCheck.UpToDate(currentVersion)
            }
        } catch (error: Exception) {
            UpdateCheck.Failed(error.message ?: "Không kiểm tra được cập nhật.")
        }
    }

    fun fetchLatest(owner: String, repo: String): ReleaseInfo? {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PrivacyShield")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("GitHub trả về mã ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val assets = json.optJSONArray("assets")
            val parsed = mutableListOf<ReleaseAsset>()
            var checksum: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.getJSONObject(index)
                    val name = asset.optString("name")
                    val download = asset.optString("browser_download_url")
                    parsed += ReleaseAsset(name, download, asset.optLong("size"))
                    if (name.endsWith(".sha256") || name.equals("SHA256SUMS", true)) {
                        checksum = readChecksum(download)
                    }
                }
            }
            val apk = parsed.firstOrNull { it.name.endsWith(".apk", true) }
            return ReleaseInfo(
                tag = json.optString("tag_name"),
                name = json.optString("name"),
                body = json.optString("body"),
                apk = apk,
                checksum = checksum
            )
        } finally {
            connection.disconnect()
        }
    }

    fun download(url: String, onProgress: (Int) -> Unit): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("User-Agent", "PrivacyShield")
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Không tải được file, mã ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            val input = BufferedInputStream(connection.inputStream)
            val buffer = ByteArray(16 * 1024)
            val out = java.io.ByteArrayOutputStream()
            var readTotal = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                out.write(buffer, 0, count)
                readTotal += count
                if (total > 0) onProgress(((readTotal * 100) / total).toInt().coerceIn(0, 100))
            }
            onProgress(100)
            return out.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun readChecksum(url: String): String? {
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "PrivacyShield")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
                .lineSequence()
                .firstOrNull()
                ?.trim()
                ?.substringBefore(" ")
                ?.lowercase()
        }.getOrNull()
    }

    companion object {
        fun androidLimitNote(): String {
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}): máy đời cũ không có chỉ báo quyền riêng tư của hệ thống và không cho ứng dụng bên thứ ba chặn nút nguồn."
            } else {
                "Android ${Build.VERSION.RELEASE}: vẫn không cho ứng dụng thường chặn nút nguồn hoặc đọc thời điểm cảm biến đang được dùng."
            }
        }
    }
}
