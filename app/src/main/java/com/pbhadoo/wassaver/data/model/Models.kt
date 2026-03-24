package com.pbhadoo.wassaver.data.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val size: Long = 0L,
    val dateModified: Long = 0L
)

data class DeletedMessage(
    val id: Long = System.currentTimeMillis(),
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val packageName: String = "com.whatsapp"
)

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

enum class MediaFilter { ALL, PHOTOS, VIDEOS }
enum class WaApp { WHATSAPP, BUSINESS }
