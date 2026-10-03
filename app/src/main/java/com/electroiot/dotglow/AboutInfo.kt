package com.electroiot.dotglow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.electroiot.dotglow.ble.DotGlowProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class LinkItem(val title: String, val url: String, val subtitle: String = "")

@Serializable
data class AboutInfo(
    val appGithub: String = "https://github.com/ElectroIoT",
    val appWebsite: String = "https://mtpcode.com/apps/dotglow.html",
    val appEmail: String = "admin@mtpcode.com",
    val privacy: String = "https://mtpcode.com/legal/dotglow/privacy.html",
    val terms: String = "https://mtpcode.com/legal/dotglow/terms.html",
    val devName: String = "Manoranjan",
    val devTagline: String = "Full-stack developer, IoT engineer and PCB designer",
    val devPhoto: String = "https://github.com/manoranjan2050.png?size=256",
    val devWebsite: String = "https://manoranjan.dev",
    val devEmail: String = "electroiot.in@gmail.com",
    val devGithub: String = "https://github.com/manoranjan2050",
    val socials: List<LinkItem> = listOf(
        LinkItem("GitHub", "https://github.com/manoranjan2050"),
        LinkItem("LinkedIn", "https://linkedin.com/in/manoranjan2050"),
        LinkItem("Google Developer", "https://me.developers.google.com/u/manoranjan2050"),
        LinkItem("ElectroIoT", "https://github.com/ElectroIoT"),
    ),
    val projects: List<LinkItem> = listOf(
        LinkItem("ESP8266 Matrix Clock", "https://github.com/ElectroIoT/ESP8266-Matrix-Clock", "The WiFi clock DotGlow grew from"),
        LinkItem("All projects", "https://manoranjan.dev/projects.php", "manoranjan.dev"),
        LinkItem("ElectroIoT", "https://electroiot.in", "Electronics and IoT guides"),
    ),
    val apps: List<LinkItem> = listOf(
        LinkItem("MTP Code apps", "https://mtpcode.in", "Android apps by the same developer"),
    ),
)

/**
 * About-page links, read from https://manoranjan.dev/dotglow/about.json (same fields as
 * [AboutInfo]) so they can change without an app update; the last good copy is cached.
 */
class AboutRepo(private val context: Context) {
    private val prefs = context.getSharedPreferences("about", Context.MODE_PRIVATE)
    private val json = DotGlowProtocol.json

    val cached: AboutInfo
        get() = prefs.getString("json", null)?.let { runCatching { json.decodeFromString<AboutInfo>(it) }.getOrNull() } ?: AboutInfo()

    suspend fun refresh(): AboutInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val text = (URL(URL_ABOUT).openConnection() as HttpURLConnection)
                .apply { connectTimeout = 8000; readTimeout = 8000 }
                .inputStream.bufferedReader().use { it.readText() }
            val info = json.decodeFromString<AboutInfo>(text)
            prefs.edit().putString("json", json.encodeToString(info)).apply()
            info
        }.getOrNull()
    }

    /** The developer's photo, downloaded once and kept in the cache folder. */
    suspend fun photo(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "dev_photo_${url.hashCode()}.img")
        runCatching {
            if (!file.exists()) {
                if (!url.startsWith("https://")) return@runCatching null
                val bytes = (URL(url).openConnection() as HttpURLConnection)
                    .apply { connectTimeout = 8000; readTimeout = 8000; instanceFollowRedirects = true }
                    .inputStream.use { it.readBytes() }
                file.writeBytes(bytes)
            }
            BitmapFactory.decodeFile(file.path)
        }.getOrNull()
    }

    companion object {
        const val URL_ABOUT = "https://manoranjan.dev/dotglow/about.json"
    }
}
