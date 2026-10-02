package com.electroiot.dotglow

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import com.electroiot.dotglow.ble.DotGlowProtocol
import java.net.HttpURLConnection
import java.net.URL

/**
 * Links for the setup guide (the YouTube video), read from a small JSON file on the web so
 * they can change without an app update:
 *
 *   https://manoranjan.dev/dotglow/guide.json   {"videoUrl": "https://youtu.be/...", "videoTitle": "..."}
 *
 * The last good copy is cached, so the guide also works offline.
 */
class GuideLinks(context: Context) {
    private val prefs = context.getSharedPreferences("guide", Context.MODE_PRIVATE)

    val videoUrl: String get() = prefs.getString("videoUrl", "") ?: ""
    val videoTitle: String get() = prefs.getString("videoTitle", "") ?: ""

    /** Refreshes the cached links; returns true if they were updated. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(GUIDE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.inputStream.bufferedReader().use { it.readText() }.let { text ->
                val o = DotGlowProtocol.json.parseToJsonElement(text).jsonObject
                fun s(k: String) = (o[k] as? JsonPrimitive)?.content ?: ""
                val url = s("videoUrl")
                // only accept https links (YouTube etc.)
                if (url.isNotEmpty() && !url.startsWith("https://")) return@runCatching false
                prefs.edit().putString("videoUrl", url).putString("videoTitle", s("videoTitle")).apply()
                true
            }
        }.getOrDefault(false)
    }

    companion object {
        const val GUIDE_URL = "https://manoranjan.dev/dotglow/guide.json"
    }
}
