package com.electroiot.dotglow.flash

import com.fazecast.jSerialComm.SerialPort
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Runs the app's flasher against a real board on this computer (skipped unless asked):
 *
 *   ./gradlew testDebugUnitTest --tests '*FlasherHardwareTest*' -PflashPort=COM5 -PflashImage=dotglow-esp32s3-full.bin
 *
 * Without flashImage it only connects and reports the chip.
 */
class FlasherHardwareTest {

    private class DesktopLink(private val port: SerialPort) : SerialLink {
        override val isUsbJtag = port.vendorID == 0x303A
        override fun write(data: ByteArray) { port.writeBytes(data, data.size) }
        override fun read(buffer: ByteArray, timeoutMs: Int): Int {
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0)
            return port.readBytes(buffer, buffer.size).coerceAtLeast(0)
        }
        override fun setBaudRate(baud: Int) { port.baudRate = baud }
        override fun setDtr(on: Boolean) { if (on) port.setDTR() else port.clearDTR() }
        override fun setRts(on: Boolean) { if (on) port.setRTS() else port.clearRTS() }
    }

    @Test
    fun flashBoard() {
        val name = System.getProperty("flashPort").orEmpty()
        assumeTrue("set -PflashPort to run", name.isNotEmpty())
        val port = SerialPort.getCommPort(name)
        port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
        port.clearDTR(); port.clearRTS()
        check(port.openPort()) { "cannot open $name" }
        try {
            val flasher = EspFlasher(DesktopLink(port)) { println("flasher: $it") }
            val chip = flasher.connect()
            println("chip: ${chip.label}")
            val image = System.getProperty("flashImage").orEmpty()
            if (image.isEmpty()) { flasher.hardReset(); return }
            val bytes = File(image).readBytes()
            val start = System.currentTimeMillis()
            var lastPct = -1
            flasher.flash(bytes, 0) { done, total ->
                val pct = done * 100 / total
                if (pct / 10 != lastPct / 10) { println("progress $pct%"); lastPct = pct }
            }
            println("flashed ${bytes.size} bytes in ${(System.currentTimeMillis() - start) / 1000} s")
        } finally {
            port.closePort()
        }
    }
}
