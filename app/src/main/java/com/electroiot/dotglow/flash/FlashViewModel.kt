package com.electroiot.dotglow.flash

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What happens after flashing: shown on the Done screen. */
enum class NextSteps { DotGlow, Esp8266, Custom }

sealed interface FlashState {
    data object Idle : FlashState
    data class Busy(val title: String, val detail: String = "", val progress: Float? = null) : FlashState
    /** Board found and waiting in its bootloader: confirm what gets written. */
    data class Ready(val chip: EspChip, val adapter: String, val firmware: FirmwareFile?, val problem: String?) : FlashState
    data class Done(val chip: EspChip, val firmware: FirmwareFile, val next: NextSteps, val seconds: Long) : FlashState
    data class Failed(val message: String) : FlashState
}

/** Flashing a new board over USB-OTG. */
class FlashViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<FlashState>(FlashState.Idle)
    val state: StateFlow<FlashState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _catalog = MutableStateFlow<FirmwareCatalog?>(null)
    val catalog: StateFlow<FirmwareCatalog?> = _catalog.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    private var link: UsbSerialLink? = null
    private var flasher: EspFlasher? = null
    private var job: Job? = null

    /** A .bin picked from the phone instead of the online firmware. */
    private var customImage: ByteArray? = null
    private var customName = ""

    private val usb get() = getApplication<Application>().getSystemService(Context.USB_SERVICE) as UsbManager

    init { loadCatalog() }

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogError.value = null
            runCatching { FirmwareCatalog.load() }
                .onSuccess { _catalog.value = it }
                .onFailure { _catalogError.value = "Could not load the firmware list. Check the internet connection." }
        }
    }

    private fun log(line: String) { _log.value = (_log.value + line).takeLast(60) }

    /** Use a file from the phone (null = back to the online firmware). */
    fun useFile(uri: Uri?) {
        if (uri == null) { customImage = null; customName = ""; return }
        val ctx = getApplication<Application>()
        customImage = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        customName = uri.lastPathSegment?.substringAfterLast('/') ?: "file.bin"
        if (customImage == null) _state.value = FlashState.Failed("Could not read that file")
    }

    val customFileName get() = customName.takeIf { customImage != null }

    /** Step 1: find the board on USB, get permission, put it in its bootloader, detect the chip. */
    fun detect() {
        job?.cancel()
        _log.value = emptyList()
        job = viewModelScope.launch {
            try {
                closeLink()
                _state.value = FlashState.Busy("Looking for the board...")
                val driver = UsbSerialLink.findDrivers(usb).firstOrNull()
                    ?: throw FlashException("No board found. Plug it in with a USB-OTG adapter (or a USB-C to USB cable) and try again.")
                _state.value = FlashState.Busy("Allow USB access", "Tap OK on the request from Android")
                if (!UsbSerialLink.requestPermission(getApplication(), usb, driver.device)) throw FlashException("USB access was not allowed")
                val adapter = UsbSerialLink.describe(driver.device)
                log("USB: $adapter")
                _state.value = FlashState.Busy("Connecting to the board...", adapter)
                val chip = withContext(Dispatchers.IO) {
                    val l = UsbSerialLink.open(usb, driver)
                    link = l
                    val f = EspFlasher(l) { log(it) }
                    flasher = f
                    f.connect()
                }
                _state.value = choose(chip, adapter)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun choose(chip: EspChip, adapter: String): FlashState.Ready {
        val custom = customImage
        if (custom != null) {
            val imgChip = FirmwareCatalog.imageChip(custom, 0)
            val boardKey = FirmwareCatalog.chipKey(chip)
            val problem = when {
                imgChip == null -> "This file is not ESP firmware."
                boardKey == null -> "${chip.label} boards are not supported."
                imgChip != boardKey -> "This file is for ${if (imgChip == "other") "another chip" else FirmwareCatalog.label(imgChip)}, but the board is ${chip.label}."
                else -> null
            }
            val offset = customOffset(custom, chip)
            return FlashState.Ready(chip, adapter, FirmwareFile(customName, "", "", custom.size.toLong(), "", offset), problem)
        }
        val key = FirmwareCatalog.chipKey(chip)
            ?: return FlashState.Ready(chip, adapter, null, "${chip.label} boards are not supported. Use an ESP32, ESP32-S3 or ESP8266 board.")
        val cat = _catalog.value ?: return FlashState.Ready(chip, adapter, null, _catalogError.value ?: "The firmware list is still loading.")
        val fw = cat.full(key) ?: return FlashState.Ready(chip, adapter, null, "No firmware for ${chip.label} yet.")
        return FlashState.Ready(chip, adapter, fw, null)
    }

    /** Full images (with a partition table at 0x8000) go to 0x0; ESP32 app images to 0x10000. */
    private fun customOffset(b: ByteArray, chip: EspChip): Int {
        if (chip == EspChip.ESP8266) return 0
        val full = b.size > 0x8002 && b[0x8000] == 0xAA.toByte() && b[0x8001] == 0x50.toByte()
        return if (full) 0 else 0x10000
    }

    /** Step 2: download (if needed) and write. */
    fun install() {
        val ready = _state.value as? FlashState.Ready ?: return
        val fw = ready.firmware ?: return
        val f = flasher ?: return
        job = viewModelScope.launch {
            val start = System.currentTimeMillis()
            try {
                val image = customImage ?: FirmwareCatalog.download(getApplication(), fw) { done, total ->
                    _state.value = FlashState.Busy("Downloading ${fw.title}", "${done / 1024} of ${total / 1024} KB",
                        if (total > 0) done.toFloat() / total else null)
                }
                _state.value = FlashState.Busy("Installing", "Keep the board plugged in", 0f)
                withContext(Dispatchers.IO) {
                    f.flash(image, fw.offset) { done, total ->
                        _state.value = FlashState.Busy("Installing", "${done / 1024} of ${total / 1024} KB  -  keep the board plugged in",
                            done.toFloat() / total)
                    }
                }
                val next = when {
                    customImage != null -> NextSteps.Custom
                    ready.chip == EspChip.ESP8266 -> NextSteps.Esp8266
                    else -> NextSteps.DotGlow
                }
                _state.value = FlashState.Done(ready.chip, fw, next, (System.currentTimeMillis() - start) / 1000)
            } catch (e: Exception) {
                fail(e)
            } finally {
                closeLink()
            }
        }
    }

    private fun fail(e: Exception) {
        closeLink()
        if (e is kotlinx.coroutines.CancellationException) return
        log("Error: ${e.message}")
        _state.value = FlashState.Failed(
            when (e) {
                is FlashException -> e.message ?: "Flashing failed"
                is java.io.IOException -> "The USB connection was lost (${e.message}). Check the cable and try again."
                else -> e.message ?: "Flashing failed"
            },
        )
    }

    fun reset() {
        job?.cancel()
        closeLink()
        _state.value = FlashState.Idle
    }

    private fun closeLink() {
        link?.let { l -> runCatching { flasher?.hardReset() }; l.close() }
        link = null
        flasher = null
    }

    override fun onCleared() = closeLink()
}
