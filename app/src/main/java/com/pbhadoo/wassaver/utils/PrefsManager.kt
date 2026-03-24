package com.pbhadoo.wassaver.utils

import android.content.Context
import android.net.Uri
import com.pbhadoo.wassaver.data.model.WaApp

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("wassaver_prefs", Context.MODE_PRIVATE)

    fun getStatusTreeUri(waApp: WaApp): Uri? {
        val key = if (waApp == WaApp.WHATSAPP) "wa_tree_uri" else "wab_tree_uri"
        return prefs.getString(key, null)?.let { Uri.parse(it) }
    }

    fun setStatusTreeUri(waApp: WaApp, uri: Uri) {
        val key = if (waApp == WaApp.WHATSAPP) "wa_tree_uri" else "wab_tree_uri"
        prefs.edit().putString(key, uri.toString()).apply()
    }
}
