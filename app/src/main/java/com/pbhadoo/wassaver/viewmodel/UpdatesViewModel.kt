package com.pbhadoo.wassaver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pbhadoo.wassaver.BuildConfig
import com.pbhadoo.wassaver.data.model.GitHubRelease
import com.pbhadoo.wassaver.data.model.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class UpdatesUiState(
    val isLoading: Boolean = false,
    val currentVersion: String = "v${BuildConfig.VERSION_NAME}",
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false,
    val releases: List<GitHubRelease> = emptyList(),
    val error: String? = null
)

class UpdatesViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UpdatesUiState())
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()

    private val GITHUB_API = "https://api.github.com/repos/PBhadoo/WASSaver-Android/releases"

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val releases = fetchReleases()
                val latest = releases.firstOrNull()?.tagName
                val current = _state.value.currentVersion
                val updateAvailable = latest != null && latest != current

                _state.update {
                    it.copy(
                        isLoading = false,
                        releases = releases,
                        latestVersion = latest,
                        updateAvailable = updateAvailable
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
            }
        }
    }

    private suspend fun fetchReleases(): List<GitHubRelease> = withContext(Dispatchers.IO) {
        val url = URL(GITHUB_API)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("User-Agent", "WASSaver-Android")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val arr = JSONArray(response)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val assetsArr = obj.getJSONArray("assets")
            val assets = (0 until assetsArr.length()).map { j ->
                val a = assetsArr.getJSONObject(j)
                ReleaseAsset(
                    name = a.getString("name"),
                    downloadUrl = a.getString("browser_download_url"),
                    size = a.getLong("size")
                )
            }
            GitHubRelease(
                tagName = obj.getString("tag_name"),
                name = obj.getString("name"),
                body = obj.optString("body", ""),
                publishedAt = obj.getString("published_at").take(10),
                assets = assets
            )
        }
    }
}
