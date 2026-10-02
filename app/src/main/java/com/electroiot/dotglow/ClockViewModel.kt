package com.electroiot.dotglow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.electroiot.dotglow.ble.ClockConnection
import com.electroiot.dotglow.ble.ClockLink
import com.electroiot.dotglow.ble.DemoClock
import com.electroiot.dotglow.ble.ClockScanner
import com.electroiot.dotglow.ble.ConnState
import com.electroiot.dotglow.ble.FoundClock
import com.electroiot.dotglow.ble.RpcException
import com.electroiot.dotglow.ble.RpcResult
import com.electroiot.dotglow.flash.FirmwareCatalog
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
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
    val otaMessage: String,
    val otaBusy: Boolean,
    val light: Int,           // light sensor 0..100, -1 = off
    val brightnessNow: Int,
    val stopwatch: Stopwatch = Stopwatch(),
)

/** The clock's stopwatch; [ms] as reported at [at] (phone SystemClock.elapsedRealtime). */
data class Stopwatch(val on: Boolean = false, val running: Boolean = false, val ms: Long = 0, val at: Long = 0) {
    fun now(): Long = ms + if (running) android.os.SystemClock.elapsedRealtime() - at else 0
}

/** All settings, from the "config" request; keys match the clock's `set` keys. */
data class ClockConfig(private val o: JsonObject) {
    fun int(key: String, default: Int = 0) = (o[key] as? JsonPrimitive)?.intOrNull ?: default
    fun flag(key: String) = int(key) != 0 || (o[key] as? JsonPrimitive)?.booleanOrNull == true
    fun str(key: String) = (o[key] as? JsonPrimitive)?.content ?: ""
    val events: List<SpecialDay>
        get() = (o["events"] as? JsonArray)?.map { e ->
            val j = e.jsonObject
            fun i(k: String) = (j[k] as? JsonPrimitive)?.intOrNull ?: 0
            SpecialDay(i("i"), i("m"), i("d"), i("a"), (j["t"] as? JsonPrimitive)?.content ?: "")
        }?.sortedWith(compareBy({ it.month }, { it.day })) ?: emptyList()

    /** A copy with one setting changed (shown at once, before the clock confirms). */
    fun with(key: String, value: Any): ClockConfig {
        val v: JsonElement = when (value) {
            is Boolean -> JsonPrimitive(if (value) 1 else 0)
            is Number -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
        return ClockConfig(JsonObject(o + (key to v)))
    }
}

data class SpecialDay(val index: Int, val month: Int, val day: Int, val anim: Int, val text: String)
data class WifiNetwork(val ssid: String, val rssi: Int, val locked: Boolean)

/** A firmware update sent from the phone over Bluetooth. */
sealed interface BtUpdate {
    data class Busy(val title: String, val progress: Float? = null) : BtUpdate
    data object Done : BtUpdate
    data class Failed(val message: String) : BtUpdate
}

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
    private val connection = MutableStateFlow<ClockLink?>(null)
    private var lastClock: FoundClock? = null
    private var reconnectJob: Job? = null

    val connState: StateFlow<ConnState?> = connection
        .flatMapLatest { it?.state ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True while the app is waiting for a restarting clock to come back. */
    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting.asStateFlow()

    private val _status = MutableStateFlow<ClockStatus?>(null)
    val status: StateFlow<ClockStatus?> = _status.asStateFlow()

    private val _config = MutableStateFlow<ClockConfig?>(null)
    val config: StateFlow<ClockConfig?> = _config.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    private fun message(text: String) { _messages.tryEmit(text) }

    private var pollJob: Job? = null

    /**
     * Connects, retrying a couple of times: right after the app was closed the clock may
     * still be dropping the old link. A failed pairing (wrong PIN) is not retried.
     */
    fun connect(clock: FoundClock) {
        stopScan()
        disconnect()
        lastClock = clock
        viewModelScope.launch {
            // short first try: right after the app was killed Android's first connect can hang
            listOf(6_000L, 10_000L, 15_000L).forEachIndexed { attempt, timeout ->
                val c = open(clock, timeout)
                val end = c.state.first { it is ConnState.Ready || it is ConnState.Failed }
                if (end is ConnState.Ready || connection.value !== c) return@launch
                if ((end as ConnState.Failed).reason.contains("pairing", ignoreCase = true)) return@launch
                if (attempt < 2) delay(800)
            }
        }
    }

    fun retry() {
        lastClock?.let(::connect)
    }

    /** A pretend clock in the app: every screen works without hardware. */
    fun openDemo() {
        stopScan()
        disconnect()
        lastClock = null
        start(DemoClock(), 0)
    }

    private fun open(clock: FoundClock, timeoutMs: Long = 15_000): ClockLink = start(ClockConnection(getApplication(), clock.device), timeoutMs)

    private fun start(c: ClockLink, timeoutMs: Long): ClockLink {
        connection.value = c
        viewModelScope.launch {
            c.connect(timeoutMs)
            if (c.state.value is ConnState.Ready) {
                _reconnecting.value = false
                loadConfig()
                startPolling()
                watchForDrop(c)
            }
        }
        return c
    }

    /** If a ready connection drops (clock restarted, out of range), try to get it back for a minute. */
    private fun watchForDrop(c: ClockLink) {
        viewModelScope.launch {
            c.state.first { it is ConnState.Disconnected }
            if (connection.value !== c) return@launch   // the user disconnected on purpose
            reconnect()
        }
    }

    private fun reconnect() {
        val clock = lastClock ?: return
        reconnectJob?.cancel()
        _reconnecting.value = true
        pollJob?.cancel()
        reconnectJob = viewModelScope.launch {
            delay(4000)   // give a restarting clock time to boot
            repeat(10) {
                val c = open(clock)
                val ready = withTimeoutOrNull(20_000) { c.state.first { it is ConnState.Ready || it is ConnState.Failed } }
                if (ready is ConnState.Ready) return@launch
                delay(3000)
            }
            _reconnecting.value = false
            message("Lost the clock. Make sure it is powered on and close by.")
        }
    }

    fun disconnect() {
        btJob?.cancel()
        _btUpdate.value = null
        reconnectJob?.cancel()
        _reconnecting.value = false
        pollJob?.cancel()
        val c = connection.value
        connection.value = null
        c?.disconnect()
        _status.value = null
        _config.value = null
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

    fun loadConfig() {
        viewModelScope.launch {
            runCatching { request("config") }.getOrNull()?.takeIf { it.ok }?.let { r ->
                _config.value = ClockConfig(r.body.jsonObject)
            }
        }
    }

    // ---- requests ------------------------------------------------------------------------
    private suspend fun request(op: String, args: Map<String, Any?> = emptyMap(), timeoutMs: Long = 15_000): RpcResult {
        val c = connection.value ?: throw IllegalStateException("Not connected")
        return c.call(op, args, timeoutMs)
    }

    /** Runs a command, shows its error (or [done]) and returns whether it worked. */
    private suspend fun run(op: String, args: Map<String, Any?> = emptyMap(), done: String? = null, timeoutMs: Long = 15_000): RpcResult? =
        runCatching { request(op, args, timeoutMs) }
            .onSuccess { r -> if (!r.ok) message(r.message) else if (done != null) message(done) }
            .onFailure { e -> message(e.message ?: "Request failed") }
            .getOrNull()?.takeIf { it.ok }

    private fun command(op: String, args: Map<String, Any?> = emptyMap(), done: String? = null) {
        viewModelScope.launch { run(op, args, done) }
    }

    // ---- settings ----------------------------------------------------------------------------

    /** Changes one setting. live = while a slider is moving (the clock doesn't save it yet). */
    fun set(key: String, value: Any, live: Boolean = false) {
        _config.value = _config.value?.with(key, value)
        val wire = if (value is Boolean) (if (value) 1 else 0) else value   // the clock expects 1 / 0
        val args = mutableMapOf<String, Any?>("k" to key, "v" to wire)
        if (live) args["live"] = 1
        viewModelScope.launch {
            val r = run("set", args)
            if (r != null && r.message.startsWith("Restarting")) message("The clock is restarting to apply this")
        }
    }

    fun setBrightness(value: Int, save: Boolean) = set("br", value, live = !save)

    // ---- WiFi --------------------------------------------------------------------------------
    private val _networks = MutableStateFlow<List<WifiNetwork>?>(null)
    val networks: StateFlow<List<WifiNetwork>?> = _networks.asStateFlow()
    private val _wifiScanning = MutableStateFlow(false)
    val wifiScanning: StateFlow<Boolean> = _wifiScanning.asStateFlow()

    fun scanWifi() {
        viewModelScope.launch {
            _wifiScanning.value = true
            val r = run("scan", timeoutMs = 25_000)
            _wifiScanning.value = false
            _networks.value = r?.body?.jsonArray?.map { e ->
                val o = e.jsonObject
                WifiNetwork(
                    (o["s"] as? JsonPrimitive)?.content ?: "",
                    (o["r"] as? JsonPrimitive)?.intOrNull ?: -100,
                    (o["l"] as? JsonPrimitive)?.intOrNull == 1,
                )
            }?.sortedByDescending { it.rssi } ?: emptyList()
        }
    }

    fun joinWifi(ssid: String, password: String) {
        viewModelScope.launch {
            if (run("wifi", mapOf("ssid" to ssid, "pass" to password)) != null)
                message("Saved. The clock is restarting and joining $ssid...")
        }
    }

    // ---- messages, special days ---------------------------------------------------------------
    fun saveMessage(text: String, every: Int, showNow: Boolean) {
        viewModelScope.launch {
            if (run("message", mapOf("t" to text, "every" to every, "show" to if (showNow) 1 else 0),
                    if (showNow) "Watch the clock" else "Saved") != null) loadConfig()
        }
    }

    fun addSpecialDay(month: Int, day: Int, anim: Int, text: String) {
        viewModelScope.launch {
            if (run("event/add", mapOf("m" to month, "d" to day, "a" to anim, "t" to text), "Added") != null) loadConfig()
        }
    }

    fun deleteSpecialDay(index: Int) {
        viewModelScope.launch { if (run("event/del", mapOf("i" to index), "Deleted") != null) loadConfig() }
    }

    fun showSpecialDay(index: Int) = command("event/show", mapOf("i" to index), "Watch the clock")

    // ---- password, phones -------------------------------------------------------------------
    fun setPassword(current: String, new: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val r = run("password", mapOf("cur" to current, "new" to new), if (new.isEmpty()) "Password removed" else "Password saved")
            if (r != null) { loadConfig(); onDone() }
        }
    }

    fun forgetPhones() {
        viewModelScope.launch {
            run("phones/forget", done = "All phones removed. Pair again to connect.")
            disconnect()
            startScan()
        }
    }

    // ---- actions ---------------------------------------------------------------------------------
    fun playAnimation(n: Int) = command("anim", mapOf("n" to n), "Watch the clock")
    fun previewDigits() = command("digits", done = "Watch the clock")
    fun showModuleNumbers() = command("modules", done = "Watch the clock")
    fun showIp() = command("show", done = "Watch the clock")
    fun syncTime() = command("sync", done = "Re-syncing the time")
    fun checkUpdate() = command("ota/check", done = "Checking for updates...")
    fun installUpdate() = command("ota/install", done = "Installing - the clock restarts when it's done")
    fun restartClock() = command("restart", done = "Restarting...")
    fun factoryReset() = command("factory", done = "Resetting the clock...")

    // ---- firmware update over Bluetooth --------------------------------------------------------

    private val _catalog = MutableStateFlow<FirmwareCatalog?>(null)
    val catalog: StateFlow<FirmwareCatalog?> = _catalog.asStateFlow()
    private val _btUpdate = MutableStateFlow<BtUpdate?>(null)
    val btUpdate: StateFlow<BtUpdate?> = _btUpdate.asStateFlow()
    private var btJob: Job? = null

    fun loadCatalog() {
        viewModelScope.launch { runCatching { FirmwareCatalog.load() }.onSuccess { _catalog.value = it } }
    }

    /** Sends new firmware to the clock: the latest release, or [file] picked on the phone. */
    fun bluetoothUpdate(file: Uri? = null) {
        val c = connection.value ?: return
        val info = (c.state.value as? ConnState.Ready)?.info ?: return
        if (btJob?.isActive == true) return
        btJob = viewModelScope.launch {
            pollJob?.cancel()   // keep the link free for the firmware
            try {
                if (!info.bluetoothUpdates) throw RpcException("This clock's firmware is too old for Bluetooth updates. Update it once over WiFi or USB.")
                val key = FirmwareCatalog.chipKey(info.asset, info.board)
                val image = if (file != null) {
                    withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openInputStream(file)?.use { it.readBytes() } }
                        ?: throw RpcException("Could not read that file")
                } else {
                    _btUpdate.value = BtUpdate.Busy("Getting the latest version...")
                    val cat = _catalog.value ?: FirmwareCatalog.load().also { _catalog.value = it }
                    val fw = cat.app(key) ?: throw RpcException("No firmware for ${info.board}")
                    FirmwareCatalog.download(getApplication(), fw) { done, total ->
                        _btUpdate.value = BtUpdate.Busy("Downloading", if (total > 0) done.toFloat() / total else null)
                    }
                }
                if (image.size > 0x8002 && image[0x8000] == 0xAA.toByte() && image[0x8001] == 0x50.toByte())
                    throw RpcException("That is a full image for USB. Pick the app file (firmware-$key.bin).")
                if (FirmwareCatalog.imageChip(image, 0x10000) != key)
                    throw RpcException("That file is not firmware for ${info.board}.")

                _btUpdate.value = BtUpdate.Busy("Sending to the clock", 0f)
                val begin = c.call("ota/begin", mapOf("size" to image.size, "md5" to FirmwareCatalog.md5(image)), 30_000)
                if (!begin.ok) throw RpcException(begin.message)
                val chunk = (begin.body as? JsonObject)?.get("chunk")?.let { (it as? JsonPrimitive)?.intOrNull } ?: 180
                var shown = -1
                c.sendFirmware(image, chunk) { sent ->
                    val pct = (sent * 100L / image.size).toInt()
                    if (pct != shown) { shown = pct; _btUpdate.value = BtUpdate.Busy("Sending to the clock", pct / 100f) }
                }
                _btUpdate.value = BtUpdate.Busy("Checking the firmware...")
                val end = c.call("ota/end", timeoutMs = 30_000)
                if (!end.ok) throw RpcException(end.message)
                _btUpdate.value = BtUpdate.Done   // the clock restarts; watchForDrop reconnects
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                runCatching { c.call("ota/abort", timeoutMs = 3000) }
                _btUpdate.value = BtUpdate.Failed(e.message ?: "Update failed")
                if (c.state.value is ConnState.Ready) startPolling()
            }
        }
    }

    fun clearBtUpdate() {
        if (btJob?.isActive != true) _btUpdate.value = null
    }

    override fun onCleared() {
        disconnect()
    }

    // ---- stopwatch ---------------------------------------------------------------------------

    /** start, pause, reset, exit: the clock answers with the new state, shown at once. */
    fun stopwatch(action: String) {
        viewModelScope.launch {
            val r = run("stopwatch", mapOf("a" to action)) ?: return@launch
            val sw = (r.body as? JsonObject)?.let(::parseStopwatch) ?: return@launch
            _status.value = _status.value?.copy(stopwatch = sw)
        }
    }

    private fun parseStopwatch(o: JsonObject) = Stopwatch(
        on = (o["on"] as? JsonPrimitive)?.booleanOrNull ?: false,
        running = (o["run"] as? JsonPrimitive)?.booleanOrNull ?: false,
        ms = (o["ms"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0,
        at = android.os.SystemClock.elapsedRealtime(),
    )

    // ---- parsing ---------------------------------------------------------------------------
    private fun parseStatus(o: JsonObject): ClockStatus {
        fun s(k: String) = (o[k] as? JsonPrimitive)?.content ?: ""
        fun i(k: String) = (o[k] as? JsonPrimitive)?.intOrNull ?: 0
        fun b(k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: false
        return ClockStatus(
            time = s("time"), ampm = s("ampm"), date = s("date"), synced = b("synced"),
            ssid = s("ssid"), ip = s("ip"), rssi = i("rssi"), uptimeS = s("up").toLongOrNull() ?: 0,
            version = s("version"), board = s("board"), mode = s("mode"),
            latest = s("latest"), newer = b("newer"), otaMessage = s("otaMsg"), otaBusy = b("otaBusy"),
            light = (o["light"] as? JsonPrimitive)?.intOrNull ?: -1, brightnessNow = i("brNow"),
            stopwatch = (o["sw"] as? JsonObject)?.let(::parseStopwatch) ?: Stopwatch(),
        )
    }
}
