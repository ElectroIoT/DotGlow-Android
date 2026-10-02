package com.electroiot.dotglow.flash

import android.content.Context
import com.electroiot.dotglow.ble.DotGlowProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** One downloadable image. [offset]: where it goes in flash (0 for full images). */
data class FirmwareFile(val title: String, val version: String, val url: String, val size: Long, val md5: String, val offset: Int = 0)

/**
 * The firmware the app can install, read from a small JSON file on the web
 * (written by tools/release.py in DotGlow-ESP32):
 *
 *   {"dotglow": {"version": "0.2.0", "boards": {"esp32s3": {"app", "appSize", "appMd5", "full", ...}}},
 *    "esp8266": {"name", "version", "full", "fullSize", "fullMd5"}}
 */
class FirmwareCatalog(private val dotglowVersion: String, private val boards: JsonObject, private val esp8266: JsonObject?) {

    val latestVersion get() = dotglowVersion

    /** Full image (bootloader + app) for a new board. chipKey: "esp32", "esp32s3", "esp8266". */
    fun full(chipKey: String): FirmwareFile? =
        if (chipKey == "esp8266") esp8266?.let { file(it.s("name").ifEmpty { "Matrix Clock (ESP8266)" }, it.s("version"), it, "full") }
        else (boards[chipKey] as? JsonObject)?.let { file("DotGlow for ${label(chipKey)}", dotglowVersion, it, "full") }

    /** App-only image, for updating a running DotGlow clock over Bluetooth. */
    fun app(chipKey: String): FirmwareFile? =
        (boards[chipKey] as? JsonObject)?.let { file("DotGlow $dotglowVersion", dotglowVersion, it, "app", 0x10000) }

    private fun file(title: String, version: String, o: JsonObject, kind: String, offset: Int = 0): FirmwareFile? {
        val url = o.s(kind)
        if (!url.startsWith("https://")) return null
        return FirmwareFile(title, version, url, (o["${kind}Size"] as? JsonPrimitive)?.longOrNull ?: 0, o.s("${kind}Md5"), offset)
    }

    companion object {
        const val URL_CATALOG = "https://manoranjan.dev/dotglow/firmware.json"

        fun label(chipKey: String) = when (chipKey) { "esp32s3" -> "ESP32-S3"; "esp32" -> "ESP32"; else -> "ESP8266" }

        /** "firmware-esp32s3.bin" (what a clock reports) or a board name -> catalog key. */
        fun chipKey(asset: String, board: String): String = when {
            asset.startsWith("firmware-") -> asset.removePrefix("firmware-").removeSuffix(".bin")
            board.equals("ESP32-S3", true) -> "esp32s3"
            else -> "esp32"
        }

        fun chipKey(chip: EspChip): String? = when (chip) {
            EspChip.ESP32 -> "esp32"; EspChip.ESP32S3 -> "esp32s3"; EspChip.ESP8266 -> "esp8266"; else -> null
        }

        private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.content ?: ""

        suspend fun load(): FirmwareCatalog = withContext(Dispatchers.IO) {
            val text = get(URL_CATALOG).inputStream.bufferedReader().use { it.readText() }
            val o = DotGlowProtocol.json.parseToJsonElement(text).jsonObject
            val d = o["dotglow"]?.jsonObject ?: JsonObject(emptyMap())
            FirmwareCatalog(d.s("version"), d["boards"] as? JsonObject ?: JsonObject(emptyMap()), o["esp8266"] as? JsonObject)
        }

        private fun get(url: String): HttpURLConnection {
            var u = URL(url)
            repeat(5) {   // GitHub release links redirect (https -> https only)
                val c = (u.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 20_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "DotGlow-Android")
                }
                when (c.responseCode) {
                    in 200..299 -> return c
                    301, 302, 303, 307, 308 -> {
                        u = URL(u, c.getHeaderField("Location") ?: throw FlashException("Bad download link"))
                        if (u.protocol != "https") throw FlashException("Insecure download link")
                    }
                    else -> throw FlashException("Download failed (HTTP ${c.responseCode})")
                }
            }
            throw FlashException("Too many redirects")
        }

        /** Downloads (or reuses a cached copy of) [f], checking its size and MD5. */
        suspend fun download(context: Context, f: FirmwareFile, onProgress: (Long, Long) -> Unit): ByteArray = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "firmware").apply { mkdirs() }
            val cached = File(dir, f.md5.ifEmpty { f.url.hashCode().toString() } + ".bin")
            if (cached.exists() && f.md5.isNotEmpty() && md5(cached.readBytes()) == f.md5) return@withContext cached.readBytes()
            val c = get(f.url)
            val total = c.contentLengthLong.takeIf { it > 0 } ?: f.size
            val out = java.io.ByteArrayOutputStream()
            c.inputStream.use { input ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    onProgress(out.size().toLong(), total)
                }
            }
            val bytes = out.toByteArray()
            if (f.size > 0 && bytes.size.toLong() != f.size) throw FlashException("Download incomplete, please try again")
            if (f.md5.isNotEmpty() && md5(bytes) != f.md5) throw FlashException("Download damaged (MD5), please try again")
            dir.listFiles()?.forEach { it.delete() }   // keep only the newest download
            cached.writeBytes(bytes)
            bytes
        }

        fun md5(b: ByteArray): String = MessageDigest.getInstance("MD5").digest(b).joinToString("") { "%02x".format(it) }

        /**
         * Which chip an ESP image file was built for, from its header (null = not an ESP image).
         * ESP32-family images carry a chip id; ESP8266 images don't (and start at 0x0 with 0xE9 too).
         */
        fun imageChip(b: ByteArray, offset: Int): String? {
            // a full image starts with the bootloader: at 0x1000 on the ESP32, at 0x0 on the S3 / ESP8266
            val at = if (offset == 0 && b.size > 0x1010 && b[0] == 0xFF.toByte() && b[0x1000] == 0xE9.toByte()) 0x1000 else 0
            if (b.size < at + 16 || b[at] != 0xE9.toByte()) return null
            val chipId = (b[at + 12].toInt() and 0xFF) or ((b[at + 13].toInt() and 0xFF) shl 8)
            val wpPin = b[at + 8].toInt() and 0xFF   // first byte of the extended header
            // the extended header (wp pin 0xEE etc.) only exists on ESP32-family images
            return when {
                wpPin == 0xEE && chipId == 9 -> "esp32s3"
                wpPin == 0xEE && chipId == 0 -> "esp32"
                wpPin == 0xEE -> "other"
                else -> "esp8266"
            }
        }
    }
}
