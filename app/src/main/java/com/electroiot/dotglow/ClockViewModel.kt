package com.electroiot.dotglow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.electroiot.dotglow.ble.ClockConnection
import com.electroiot.dotglow.ble.ClockScanner
import com.electroiot.dotglow.ble.ConnState
import com.electroiot.dotglow.ble.FoundClock
import com.electroiot.dotglow.ble.RpcResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** Live state of the clock, from the "status" request. */
data class ClockStatus(
    val time: String,
    val ampm: String,
    val date: String,
    val synced: Boolean,
    val ssid: String,
    val ip: String,
    val rssi: Int,
    val uptimeS: Long,
    val version: String,
    val board: String,
    val mode: String,         // "clock", "setup" (no WiFi yet) or "safe"
    val latest: String,
    val newer: Boolean,
    val brightnessNow: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClockViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = ClockScanner(app)
    val bluetoothOn: Boolean get() = scanner.bluetoothOn

    // ---- scanning ------------------------------------------------------------------
    private val _found = MutableStateFlow<List<FoundClock>>(emptyList())
    val found: StateFlow<List<FoundClock>> = _found.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _found.value = emptyList()
        _scanning.value = true
        scanJob = viewModelScope.launch {
            scanner.scan()
                .catch { e -> message(e.message ?: "Scan failed") }
                .collect { clock ->
                    _found.value = (_found.value.filterNot { it.address == clock.address } + clock)
                        .sortedByDescending { it.rssi }
                }
            _scanning.value = false
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _scanning.value = false
    }

    // ---- connection ------------------------------------------------------------------
    private val connection = MutableStateFlow<ClockConnection?>(null)

    val connState: StateFlow<ConnState?> = connection
        .flatMapLatest { it?.state ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _status = MutableStateFlow<ClockStatus?>(null)
    val status: StateFlow<ClockStatus?> = _status.asStateFlow()

    private val _brightness = MutableStateFlow(4)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    private fun message(text: String) { _messages.tryEmit(text) }

    private var pollJob: Job? = null

    fun connect(clock: FoundClock) {
        stopScan()
        disconnect()
        val c = ClockConnection(getApplication(), clock.device)
        connection.value = c
        viewModelScope.launch {
            c.connect()
            if (c.state.value is ConnState.Ready) {
                loadConfig()
                startPolling()
            }
        }
    }

    fun disconnect() {
        pollJob?.cancel()
        connection.value?.disconnect()
        connection.value = null
        _status.value = null
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                runCatching { request("status") }.getOrNull()?.let { r ->
                    if (r.ok) _status.value = parseStatus(r.body.jsonObject)
                }
                delay(2000)
            }
        }
    }

    private suspend fun loadConfig() {
        runCatching { request("config") }.getOrNull()?.takeIf { it.ok }?.let { r ->
            (r.body.jsonObject["br"] as? JsonPrimitive)?.intOrNull?.let { _brightness.value = it }
        }
    }

    // ---- commands ------------------------------------------------------------------------
    private suspend fun request(op: String, args: Map<String, Any?> = emptyMap()): RpcResult {
        val c = connection.value ?: throw IllegalStateException("Not connected")
        return c.call(op, args)
    }

    /** Fire-and-report: runs a command and shows its error, if any. */
    private fun command(op: String, args: Map<String, Any?> = emptyMap(), done: String? = null) {
        viewModelScope.launch {
            runCatching { request(op, args) }
                .onSuccess { r -> if (!r.ok) message(r.message) else if (done != null) message(done) }
                .onFailure { e -> message(e.message ?: "Request failed") }
        }
    }

    /** live = while dragging (no flash write); save = on release. */
    fun setBrightness(value: Int, save: Boolean) {
        _brightness.value = value
        command("set", if (save) mapOf("k" to "br", "v" to value) else mapOf("k" to "br", "v" to value, "live" to 1))
    }

    fun playAnimation(n: Int) = command("anim", mapOf("n" to n), "Watch the clock")
    fun previewDigits() = command("digits", done = "Watch the clock")
    fun showIp() = command("show", done = "Watch the clock")

    override fun onCleared() {
        disconnect()
    }

    // ---- parsing ---------------------------------------------------------------------------
    private fun parseStatus(o: JsonObject): ClockStatus {
        fun s(k: String) = (o[k] as? JsonPrimitive)?.content ?: ""
        fun i(k: String) = (o[k] as? JsonPrimitive)?.intOrNull ?: 0
        fun b(k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: false
        return ClockStatus(
            time = s("time"), ampm = s("ampm"), date = s("date"), synced = b("synced"),
            ssid = s("ssid"), ip = s("ip"), rssi = i("rssi"), uptimeS = s("up").toLongOrNull() ?: 0,
            version = s("version"), board = s("board"), mode = s("mode"),
            latest = s("latest"), newer = b("newer"), brightnessNow = i("brNow"),
        )
    }
}
