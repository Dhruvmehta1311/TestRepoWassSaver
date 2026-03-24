package com.pbhadoo.wassaver.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class SplitterUiState(
    val selectedUri: Uri? = null,
    val selectedName: String? = null,
    val durationMs: Long = 0L,
    val estimatedParts: Int = 0,
    val outputFiles: List<File> = emptyList(),
    val progress: Float = 0f,
    val isSplitting: Boolean = false,
    val isDone: Boolean = false,
    val message: String? = null
)

class StatusSplitterViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(SplitterUiState())
    val state: StateFlow<SplitterUiState> = _state.asStateFlow()

    private val SEGMENT_DURATION_US = 90_000_000L // 90 seconds in microseconds

    fun onVideoSelected(uri: Uri, name: String) {
        viewModelScope.launch {
            val duration = getVideoDuration(uri)
            val parts = ((duration / 90_000) + 1).toInt().coerceAtLeast(1)
            _state.update {
                it.copy(
                    selectedUri = uri,
                    selectedName = name,
                    durationMs = duration,
                    estimatedParts = parts,
                    outputFiles = emptyList(),
                    isDone = false,
                    progress = 0f
                )
            }
        }
    }

    private suspend fun getVideoDuration(uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(getApplication(), uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    fun splitVideo() {
        val uri = _state.value.selectedUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSplitting = true, isDone = false, outputFiles = emptyList()) }
            val files = mutableListOf<File>()
            try {
                val outputDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "WASSaver/Split"
                )
                outputDir.mkdirs()
                val baseName = _state.value.selectedName?.substringBeforeLast('.') ?: "split"
                val totalDuration = _state.value.durationMs * 1000L // convert to microseconds
                var partIndex = 0
                var startUs = 0L

                while (startUs < totalDuration) {
                    val endUs = (startUs + SEGMENT_DURATION_US).coerceAtMost(totalDuration)
                    val outputFile = File(outputDir, "${baseName}_part${partIndex + 1}.mp4")
                    extractSegment(getApplication(), uri, startUs, endUs, outputFile)
                    files.add(outputFile)
                    startUs = endUs
                    partIndex++
                    val progress = startUs.toFloat() / totalDuration.toFloat()
                    _state.update { it.copy(progress = progress.coerceIn(0f, 1f)) }
                }

                _state.update {
                    it.copy(
                        isSplitting = false,
                        isDone = true,
                        outputFiles = files,
                        progress = 1f,
                        message = "Split into ${files.size} parts"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSplitting = false,
                        message = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun extractSegment(
        context: Context,
        inputUri: Uri,
        startUs: Long,
        endUs: Long,
        outputFile: File
    ) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, inputUri, null)

        val trackCount = extractor.trackCount
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackIndexMap = mutableMapOf<Int, Int>()

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                val muxerTrackIndex = muxer.addTrack(format)
                trackIndexMap[i] = muxerTrackIndex
            }
        }

        muxer.start()
        val bufferInfo = MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(1024 * 1024)
        var firstVideoSample = true

        // Seek and copy each track
        trackIndexMap.forEach { (extractorTrack, muxerTrack) ->
            extractor.selectTrack(extractorTrack)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break
                if (sampleTime < startUs) {
                    extractor.advance()
                    continue
                }

                bufferInfo.presentationTimeUs = sampleTime - startUs
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                extractor.advance()
            }
            extractor.unselectTrack(extractorTrack)
        }

        muxer.stop()
        muxer.release()
        extractor.release()
    }

    fun sharePartToWhatsApp(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.update { it.copy(message = "WhatsApp not installed") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
