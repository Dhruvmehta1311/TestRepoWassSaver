package com.pbhadoo.wassaver.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pbhadoo.wassaver.data.model.MediaFilter
import com.pbhadoo.wassaver.data.model.MediaItem
import com.pbhadoo.wassaver.data.model.WaApp
import com.pbhadoo.wassaver.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class StatusViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val filter: MediaFilter = MediaFilter.ALL,
    val waApp: WaApp = WaApp.WHATSAPP,
    val isLoading: Boolean = false,
    val message: String? = null,
    val needsPermission: Boolean = false,
    val permissionUri: Uri? = null
)

class StatusViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PrefsManager(application)
    private val _uiState = MutableStateFlow(StatusViewerUiState())
    val uiState: StateFlow<StatusViewerUiState> = _uiState.asStateFlow()

    // WA status paths (Android 11+ requires SAF)
    private fun getStatusPath(waApp: WaApp): String {
        val base = Environment.getExternalStorageDirectory().absolutePath
        return when (waApp) {
            WaApp.WHATSAPP -> "$base/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            WaApp.BUSINESS -> "$base/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val waApp = _uiState.value.waApp

            // Try SAF URI first (Android 11+)
            val safUri = prefs.getStatusTreeUri(waApp)
            val items = if (safUri != null) {
                loadFromSaf(safUri)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                loadFromPath(getStatusPath(waApp))
            } else {
                _uiState.update { it.copy(isLoading = false, needsPermission = true) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    items = items,
                    isLoading = false,
                    needsPermission = items.isEmpty() && safUri == null
                )
            }
        }
    }

    private suspend fun loadFromSaf(treeUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        doc.listFiles()
            .filter { it.name?.startsWith(".") == false && (it.isImage() || it.isVideo()) }
            .map { file ->
                MediaItem(
                    uri = file.uri,
                    name = file.name ?: "unknown",
                    isVideo = file.isVideo(),
                    size = file.length(),
                    dateModified = file.lastModified()
                )
            }
            .sortedByDescending { it.dateModified }
    }

    private suspend fun loadFromPath(path: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.map { file ->
                val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov")
                MediaItem(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    isVideo = isVideo,
                    size = file.length(),
                    dateModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.dateModified }
            ?: emptyList()
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setWaApp(app: WaApp) {
        _uiState.update { it.copy(waApp = app) }
        loadStatuses()
    }

    fun onSafUriGranted(uri: Uri, waApp: WaApp) {
        prefs.setStatusTreeUri(waApp, uri)
        _uiState.update { it.copy(needsPermission = false) }
        loadStatuses()
    }

    fun saveMedia(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "WASSaver"
                )
                dir.mkdirs()
                val destFile = File(dir, item.name)
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                _uiState.update { it.copy(message = "Saved to Pictures/WASSaver") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Save failed: ${e.message}") }
            }
        }
    }

    fun shareMedia(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun repostToWhatsApp(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "WhatsApp not installed") }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    val filteredItems: StateFlow<List<MediaItem>> = uiState.map { state ->
        when (state.filter) {
            MediaFilter.ALL -> state.items
            MediaFilter.PHOTOS -> state.items.filter { !it.isVideo }
            MediaFilter.VIDEOS -> state.items.filter { it.isVideo }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

private fun DocumentFile.isImage(): Boolean =
    type?.startsWith("image/") == true || name?.lowercase()?.let {
        it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".gif")
    } == true

private fun DocumentFile.isVideo(): Boolean =
    type?.startsWith("video/") == true || name?.lowercase()?.let {
        it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".3gp")
    } == true
