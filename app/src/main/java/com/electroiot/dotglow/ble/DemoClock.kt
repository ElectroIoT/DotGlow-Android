package com.electroiot.dotglow.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** What the app needs from a clock: the real one over Bluetooth, or [DemoClock]. */
interface ClockLink {
    val state: StateFlow<ConnState>
    suspend fun connect(timeoutMs: Long = 15_000)
    suspend fun call(op: String, args: Map<String, Any?> = emptyMap(), timeoutMs: Long = 15_000): RpcResult
    suspend fun sendFirmware(data: ByteArray, chunk: Int, onProgress: (Int) -> Unit)
    fun disconnect()
}

/**
 * A pretend clock that lives in the app: try every screen without hardware (and lets app
 * reviewers see the app working). Settings are kept in memory and answered like the firmware.
 */
class DemoClock : ClockLink {
    private val _state = MutableStateFlow<ConnState>(ConnState.Connecting)
    override val state: StateFlow<ConnState> = _state.asStateFlow()

    private val cfg = mutableMapOf<String, JsonElement>(
        "br" to JsonPrimitive(6), "fmt24" to JsonPrimitive(0), "tz" to JsonPrimitive("IST-5:30"),
        "blinkColon" to JsonPrimitive(1), "secondsBar" to JsonPrimitive(0), "leadingZero" to JsonPrimitive(0),
        "showDate" to JsonPrimitive(1), "showSecs" to JsonPrimitive(0), "autoDim" to JsonPrimitive(1),
        "nightFrom" to JsonPrimitive(23), "nightTo" to JsonPrimitive(6), "nightBr" to JsonPrimitive(0),
        "ldrOn" to JsonPrimitive(0), "ldrMin" to JsonPrimitive(0), "ldrMax" to JsonPrimitive(15), "ldrInvert" to JsonPrimitive(0),
        "modules" to JsonPrimitive(4), "hwType" to JsonPrimitive(0), "flipH" to JsonPrimitive(0), "flipV" to JsonPrimitive(0),
        "roll" to JsonPrimitive(0), "hourlyAnim" to JsonPrimitive(5), "bootAnim" to JsonPrimitive(1),
        "msg" to JsonPrimitive("Happy coding!"), "msgEvery" to JsonPrimitive(0), "autoUpdate" to JsonPrimitive(1),
        "hasPassword" to JsonPrimitive(0), "ssid" to JsonPrimitive("Home WiFi"),
    )
    private val events = mutableListOf(Triple(1, 1, "Happy New Year") to 1, Triple(8, 15, "Independence Day") to 4)
    private val started = System.currentTimeMillis()

    override suspend fun connect(timeoutMs: Long) {
        delay(600)
        _state.value = ConnState.Ready(ClockInfo("DotGlow Demo", "0.2.0", "Demo", 1, "firmware-esp32s3.bin", true))
    }

    override suspend fun call(op: String, args: Map<String, Any?>, timeoutMs: Long): RpcResult {
        delay(if (op == "scan") 1500 else 60)
        fun ok(body: JsonElement = JsonPrimitive("OK")) = RpcResult(200, body)
        return when (op) {
            "status" -> ok(status())
            "config" -> ok(JsonObject(cfg + ("events" to eventsJson())))
            "set" -> {
                val k = args["k"].toString()
                val v = args["v"].toString()
                cfg[k] = v.toIntOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(v)
                ok()
            }
            "scan" -> ok(buildJsonArray {
                listOf("Home WiFi" to -48, "Office" to -63, "Guest" to -77).forEach { (s, r) ->
                    add(buildJsonObject { put("s", s); put("r", r); put("l", 1) })
                }
            })
            "message" -> { cfg["msg"] = JsonPrimitive(args["t"].toString()); ok() }
            "event/add" -> {
                events += Triple(args["m"].toString().toInt(), args["d"].toString().toInt(), args["t"].toString()) to
                    args["a"].toString().toInt()
                ok()
            }
            "event/del" -> { args["i"].toString().toIntOrNull()?.let { if (it in events.indices) events.removeAt(it) }; ok() }
            "ota/begin" -> RpcResult(400, JsonPrimitive("The demo clock can't be updated"))
            "wifi", "password", "phones/forget", "factory", "restart" -> RpcResult(400, JsonPrimitive("Not available on the demo clock"))
            else -> ok()   // anim, digits, show, sync ... : nothing to show without a display
        }
    }

    private fun eventsJson() = JsonArray(events.mapIndexed { i, (e, a) ->
        buildJsonObject { put("i", i); put("m", e.first); put("d", e.second); put("a", a); put("t", e.third) }
    })

    private fun status(): JsonObject {
        val now = LocalDateTime.now()
        val fmt24 = (cfg["fmt24"] as? JsonPrimitive)?.content == "1"
        return buildJsonObject {
            put("time", now.format(DateTimeFormatter.ofPattern(if (fmt24) "HH:mm:ss" else "hh:mm:ss")))
            put("ampm", if (fmt24) "" else now.format(DateTimeFormatter.ofPattern("a")).uppercase())
            put("date", now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")))
            put("synced", true); put("ssid", "Home WiFi"); put("ip", "192.168.1.42"); put("rssi", -52)
            put("up", ((System.currentTimeMillis() - started) / 1000).toString())
            put("version", "0.2.0"); put("board", "Demo"); put("mode", "clock")
            put("latest", "0.2.0"); put("newer", false); put("otaMsg", ""); put("otaBusy", false)
            put("light", -1); put("brNow", (cfg["br"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 6)
        }
    }

    override suspend fun sendFirmware(data: ByteArray, chunk: Int, onProgress: (Int) -> Unit) =
        throw RpcException("The demo clock can't be updated")

    override fun disconnect() { _state.value = ConnState.Disconnected }
}
