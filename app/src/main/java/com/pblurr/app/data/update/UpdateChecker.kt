package com.pblurr.app.data.update

import com.pblurr.app.data.logging.AppLogger
import com.pblurr.app.domain.model.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String?
)

class UpdateChecker {

    companion object {
        private const val GITHUB_RELEASE_API = "https://api.github.com/repos/APPROX4/P.blurr/releases/latest"
        const val CURRENT_VERSION_NAME = "1.0.1"
    }

    suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "PBlurr-Android-App")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/APPROX4/P.blurr/releases/latest")
                val releaseNotes = json.optString("body", "")

                if (tagName.isNotEmpty() && isNewerVersion(tagName, CURRENT_VERSION_NAME)) {
                    AppLogger.i(LogCategory.APP, "New version detected on GitHub: v$tagName (Installed: v$CURRENT_VERSION_NAME)")
                    return@withContext AppUpdateInfo(
                        latestVersion = tagName,
                        releaseUrl = htmlUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.d(LogCategory.APP, "GitHub update check note: ${e.message}")
        }
        return@withContext null
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = Math.max(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
