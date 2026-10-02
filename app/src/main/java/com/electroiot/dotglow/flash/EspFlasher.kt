package com.electroiot.dotglow.flash

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Deflater

/** The serial port the flasher talks through (USB on the phone; a desktop port in tests). */
interface SerialLink {
    fun write(data: ByteArray)
    /** Reads what is available, waiting up to [timeoutMs]; returns 0 on timeout. */
    fun read(buffer: ByteArray, timeoutMs: Int): Int
    fun setBaudRate(baud: Int)
    fun setDtr(on: Boolean)
    fun setRts(on: Boolean)
    /** True for the ESP32-S3's own USB port (USB-Serial-JTAG): it needs a different reset sequence. */
    val isUsbJtag: Boolean
}

enum class EspChip(val label: String) {
    ESP8266("ESP8266"), ESP32("ESP32"), ESP32S3("ESP32-S3"),
    ESP32S2("ESP32-S2"), ESP32C3("ESP32-C3"), UNKNOWN("unknown chip"),
}

class FlashException(message: String) : Exception(message)

/**
 * Writes firmware through the chip's ROM bootloader (the same serial protocol as esptool.py,
 * without its RAM stub): reset into download mode, sync, detect the chip, write, verify, reset.
 *
 * ESP32 / ESP32-S3: compressed writes (zlib), MD5 check of the written flash.
 * ESP8266: its ROM has neither, so plain 1 KB blocks at 115200 baud.
 */
class EspFlasher(private val link: SerialLink, private val log: (String) -> Unit = {}) {

    var chip = EspChip.UNKNOWN
        private set

    // ---- SLIP framing --------------------------------------------------------------------

    private val rx = ByteArray(4096)
    private var rxLen = 0
    private var rxPos = 0

    private fun nextByte(deadline: Long): Int {
        while (rxPos >= rxLen) {
            val left = deadline - System.currentTimeMillis()
            if (left <= 0) return -1
            rxLen = link.read(rx, left.coerceAtMost(100).toInt())
            rxPos = 0
        }
        return rx[rxPos++].toInt() and 0xFF
    }

    private fun flushInput() {
        rxPos = 0
        rxLen = 0
        while (link.read(rx, 20) > 0) Unit
    }

    /** One SLIP frame, or null on timeout. */
    private fun readFrame(timeoutMs: Long): ByteArray? {
        val deadline = System.currentTimeMillis() + timeoutMs
        var b: Int
        do {
            b = nextByte(deadline)
            if (b < 0) return null
        } while (b != 0xC0)
        val out = ByteArrayOutputStream()
        while (true) {
            b = nextByte(deadline)
            when {
                b < 0 -> return null
                b == 0xC0 -> if (out.size() > 0) return out.toByteArray()   // else: back-to-back delimiters
                b == 0xDB -> when (nextByte(deadline)) {
                    0xDC -> out.write(0xC0)
                    0xDD -> out.write(0xDB)
                    else -> return null
                }
                else -> out.write(b)
            }
        }
    }

    private fun writeFrame(packet: ByteArray) {
        val out = ByteArrayOutputStream(packet.size + 16)
        out.write(0xC0)
        for (x in packet) when (val b = x.toInt() and 0xFF) {
            0xC0 -> { out.write(0xDB); out.write(0xDC) }
            0xDB -> { out.write(0xDB); out.write(0xDD) }
            else -> out.write(b)
        }
        out.write(0xC0)
        link.write(out.toByteArray())
    }

    // ---- commands --------------------------------------------------------------------------

    private class Reply(val value: Int, val data: ByteArray)

    private fun command(op: Int, data: ByteArray = ByteArray(0), checksum: Int = 0, timeoutMs: Long = 3000): Reply {
        val p = ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        p.put(0).put(op.toByte()).putShort(data.size.toShort()).putInt(checksum).put(data)
        writeFrame(p.array())
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val left = deadline - System.currentTimeMillis()
            val f = (if (left > 0) readFrame(left) else null) ?: throw FlashException("The board stopped answering")
            if (f.size < 8 || f[0] != 1.toByte() || f[1] != op.toByte()) continue   // stale reply to an earlier command
            val b = ByteBuffer.wrap(f).order(ByteOrder.LITTLE_ENDIAN)
            return Reply(b.getInt(4), f.copyOfRange(8, f.size))
        }
    }

    /** Runs a command and checks the status bytes at the end of the reply (2 on ESP8266, 4 on ESP32 ROMs). */
    private fun check(what: String, op: Int, data: ByteArray = ByteArray(0), checksum: Int = 0, timeoutMs: Long = 3000): Reply {
        val r = command(op, data, checksum, timeoutMs)
        val statusLen = if (chip == EspChip.ESP8266 || r.data.size == 2) 2 else 4
        if (r.data.size < statusLen) throw FlashException("Bad reply to $what")
        val status = r.data[r.data.size - statusLen].toInt()
        val error = r.data[r.data.size - statusLen + 1].toInt() and 0xFF
        if (status != 0) throw FlashException("$what failed (error 0x${error.toString(16)})")
        return r
    }

    private fun le(vararg words: Int): ByteArray {
        val b = ByteBuffer.allocate(words.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        words.forEach { b.putInt(it) }
        return b.array()
    }

    private fun readReg(address: Int) = check("read register", READ_REG, le(address)).value

    // ---- connect ------------------------------------------------------------------------

    private fun sleep(ms: Long) = Thread.sleep(ms)

    /** EN and GPIO0 through the USB adapter's RTS / DTR lines (NodeMCU, DevKit auto-reset circuit). */
    private fun classicReset(holdMs: Long) {
        link.setDtr(false); link.setRts(true)    // EN low
        sleep(100)
        link.setDtr(true); link.setRts(false)    // GPIO0 low, EN high: boot into download mode
        sleep(holdMs)
        link.setDtr(false)                       // release GPIO0
    }

    /** The ESP32-S3's built-in USB-Serial-JTAG maps DTR/RTS to the reset logic directly. */
    private fun usbJtagReset() {
        link.setRts(false); link.setDtr(false)
        sleep(100)
        link.setDtr(true); link.setRts(false)
        sleep(100)
        link.setRts(true); link.setDtr(false); link.setRts(true)
        sleep(100)
        link.setDtr(false); link.setRts(false)
    }

    private fun sync(): Boolean {
        val data = byteArrayOf(0x07, 0x07, 0x12, 0x20) + ByteArray(32) { 0x55 }
        repeat(5) {
            flushInput()
            try {
                command(SYNC, data, timeoutMs = 120)
                repeat(7) { readFrame(60) }   // the ROM answers a SYNC several times
                flushInput()
                return true
            } catch (_: FlashException) {
            }
        }
        return false
    }

    /** Resets the board into its bootloader and finds out which chip it is. */
    fun connect(): EspChip {
        link.setBaudRate(ROM_BAUD)
        for (attempt in 0 until 6) {
            log(if (attempt == 0) "Starting the board's bootloader..." else "Retrying (${attempt + 1}/6)...")
            when {
                link.isUsbJtag -> usbJtagReset()
                attempt % 2 == 0 -> classicReset(50)
                else -> classicReset(550)   // slower boards need GPIO0 held longer
            }
            if (sync()) {
                val magic = readReg(CHIP_MAGIC_REG)
                chip = when (magic.toLong() and 0xFFFFFFFFL) {
                    0xFFF0C101L -> EspChip.ESP8266
                    0x00F01D83L -> EspChip.ESP32
                    0x9L -> EspChip.ESP32S3
                    0x7C6L -> EspChip.ESP32S2
                    0x6921506FL, 0x1B31506FL, 0x4881606FL, 0x4361606FL -> EspChip.ESP32C3
                    else -> EspChip.UNKNOWN
                }
                log("Found ${chip.label}")
                return chip
            }
        }
        throw FlashException(
            "The board did not answer. Try another cable, or hold BOOT, tap RESET (EN), release BOOT, then try again.",
        )
    }

    // ---- write -------------------------------------------------------------------------------

    /**
     * Writes [image] at [offset], verifies it and restarts the board.
     * [onProgress] gets (bytes of the image written, image size).
     */
    fun flash(image: ByteArray, offset: Int, onProgress: (Int, Int) -> Unit) {
        when (chip) {
            EspChip.ESP8266 -> writePlain(image, offset, onProgress)
            EspChip.ESP32, EspChip.ESP32S3 -> {
                check("attach flash", SPI_ATTACH, ByteArray(8))
                check("set flash size", SPI_SET_PARAMS, le(0, 4 * MB, 64 * 1024, 4096, 256, 0xFFFF))
                if (!link.isUsbJtag) changeBaud(FAST_BAUD)
                writeCompressed(image, offset, onProgress)
                verify(image, offset)
            }
            else -> throw FlashException("${chip.label} is not supported")
        }
        log("Restarting the board")
        hardReset()
    }

    private fun changeBaud(baud: Int) {
        try {
            check("change speed", CHANGE_BAUD, le(baud, 0))
            link.setBaudRate(baud)
            sleep(50)
            flushInput()
            log("Speed: $baud baud")
        } catch (e: FlashException) {
            log("Staying at $ROM_BAUD baud")   // still works, only slower
            link.setBaudRate(ROM_BAUD)
        }
    }

    private fun checksum(data: ByteArray, from: Int = 0, to: Int = data.size): Int {
        var c = 0xEF
        for (i in from until to) c = c xor (data[i].toInt() and 0xFF)
        return c
    }

    private fun blockTimeout(bytes: Int) = (3000L + bytes / 1024 * 30).coerceAtLeast(3000)
    private fun eraseTimeout(bytes: Int) = (bytes.toLong() * 30_000 / MB).coerceAtLeast(10_000)

    private fun writeCompressed(image: ByteArray, offset: Int, onProgress: (Int, Int) -> Unit) {
        val packed = Deflater(Deflater.BEST_COMPRESSION).run {
            setInput(image)
            finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(65536)
            while (!finished()) out.write(buf, 0, deflate(buf))
            end()
            out.toByteArray()
        }
        val blocks = (packed.size + BLOCK - 1) / BLOCK
        val eraseSize = (image.size + BLOCK - 1) / BLOCK * BLOCK
        log("Erasing ${eraseSize / 1024} KB...")
        val begin = if (chip == EspChip.ESP32S3) le(eraseSize, blocks, BLOCK, offset, 0) else le(eraseSize, blocks, BLOCK, offset)
        check("erase", FLASH_DEFL_BEGIN, begin, timeoutMs = eraseTimeout(eraseSize))
        log("Writing ${image.size / 1024} KB (${packed.size / 1024} KB compressed)...")
        for (seq in 0 until blocks) {
            val from = seq * BLOCK
            val to = minOf(from + BLOCK, packed.size)
            val pkt = le(to - from, seq, 0, 0) + packed.copyOfRange(from, to)
            check("write block $seq", FLASH_DEFL_DATA, pkt, checksum(packed, from, to), timeoutMs = blockTimeout(BLOCK * 4))
            onProgress((image.size.toLong() * to / packed.size).toInt(), image.size)
        }
    }

    /** ESP8266 ROM: its erase command erases too much (a ROM bug), esptool's correction. */
    private fun esp8266EraseSize(offset: Int, size: Int): Int {
        val sectorsPerBlock = 16
        val numSectors = (size + SECTOR - 1) / SECTOR
        val startSector = offset / SECTOR
        var head = sectorsPerBlock - (startSector % sectorsPerBlock)
        if (numSectors < head) head = numSectors
        return if (numSectors < 2 * head) (numSectors + 1) / 2 * SECTOR else (numSectors - head) * SECTOR
    }

    private fun writePlain(image: ByteArray, offset: Int, onProgress: (Int, Int) -> Unit) {
        val blocks = (image.size + BLOCK - 1) / BLOCK
        val erase = esp8266EraseSize(offset, image.size)
        log("Erasing ${image.size / 1024} KB...")
        check("erase", FLASH_BEGIN, le(erase, blocks, BLOCK, offset), timeoutMs = eraseTimeout(image.size))
        log("Writing ${image.size / 1024} KB...")
        for (seq in 0 until blocks) {
            val block = ByteArray(BLOCK) { 0xFF.toByte() }
            val from = seq * BLOCK
            val n = minOf(BLOCK, image.size - from)
            System.arraycopy(image, from, block, 0, n)
            check("write block $seq", FLASH_DATA, le(BLOCK, seq, 0, 0) + block, checksum(block), timeoutMs = blockTimeout(BLOCK))
            onProgress(from + n, image.size)
        }
    }

    private fun verify(image: ByteArray, offset: Int) {
        log("Checking...")
        val r = check("check", SPI_FLASH_MD5, le(offset, image.size, 0, 0), timeoutMs = eraseTimeout(image.size))
        val got = String(r.data, 0, 32, Charsets.US_ASCII).lowercase()
        val want = MessageDigest.getInstance("MD5").digest(image).joinToString("") { "%02x".format(it) }
        if (got != want) throw FlashException("The written firmware does not match (MD5). Please try again.")
        log("Firmware verified")
    }

    fun hardReset() {
        link.setDtr(false)
        link.setRts(true)
        sleep(if (link.isUsbJtag) 200 else 100)
        link.setRts(false)
        if (link.isUsbJtag) sleep(200)
    }

    companion object {
        private const val SYNC = 0x08
        private const val READ_REG = 0x0A
        private const val FLASH_BEGIN = 0x02
        private const val FLASH_DATA = 0x03
        private const val SPI_SET_PARAMS = 0x0B
        private const val SPI_ATTACH = 0x0D
        private const val CHANGE_BAUD = 0x0F
        private const val FLASH_DEFL_BEGIN = 0x10
        private const val FLASH_DEFL_DATA = 0x11
        private const val SPI_FLASH_MD5 = 0x13

        private const val CHIP_MAGIC_REG = 0x40001000
        private const val BLOCK = 0x400        // ROM write block size
        private const val SECTOR = 4096
        private const val MB = 1024 * 1024
        const val ROM_BAUD = 115200
        const val FAST_BAUD = 460800
    }
}
