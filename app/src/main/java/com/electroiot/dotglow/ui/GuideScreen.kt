package com.electroiot.dotglow.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.electroiot.dotglow.GuideLinks
import com.electroiot.dotglow.R

private data class Board(
    val name: String,
    @DrawableRes val image: Int,
    val pins: List<Pair<String, String>>,
    val ldrPin: String,
    val esp8266: Boolean = false,   // the ESP8266 Matrix Clock: no Bluetooth, set up from its web page
)

private val boards = listOf(
    Board("ESP32-S3", R.drawable.guide_wiring_esp32s3,
        listOf("VCC" to "5V", "GND" to "GND", "DIN" to "GPIO11", "CS" to "GPIO10", "CLK" to "GPIO12"), "GPIO1"),
    Board("ESP32 DevKit", R.drawable.guide_wiring_esp32,
        listOf("VCC" to "VIN (5V)", "GND" to "GND", "DIN" to "GPIO23", "CS" to "GPIO5", "CLK" to "GPIO18"), "GPIO34"),
    Board("ESP8266", R.drawable.guide_wiring_esp8266,
        listOf("VCC" to "VIN (5V)", "GND" to "GND", "DIN" to "D7", "CS" to "D6", "CLK" to "D5"), "A0", esp8266 = true),
)

/** Index of the ESP8266 tab, for opening the guide straight on it. */
const val GUIDE_BOARD_ESP8266 = 2

@Composable
private fun Step(number: Int, title: String, content: @Composable () -> Unit) {
    SectionCard("Step $number") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(LedRed), contentAlignment = Alignment.Center) {
                Text("$number", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
internal fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = LedRed, modifier = Modifier.width(16.dp))
        Text(text)
    }
}

/** A guide picture; tap to see it full screen. */
@Composable
internal fun GuideImage(@DrawableRes res: Int, description: String) {
    var zoom by remember { mutableStateOf(false) }
    Image(
        painterResource(res), description, contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { zoom = true },
    )
    Text("Tap the picture to enlarge it", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    if (zoom) Dialog(onDismissRequest = { zoom = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).clickable { zoom = false }, contentAlignment = Alignment.Center) {
            Image(painterResource(res), description, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(snackbar: SnackbarHostState, initialBoard: Int = 0, onFlash: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val links = remember { GuideLinks(context) }
    var videoUrl by remember { mutableStateOf(links.videoUrl) }
    var videoTitle by remember { mutableStateOf(links.videoTitle) }
    LaunchedEffect(Unit) {
        if (links.refresh()) { videoUrl = links.videoUrl; videoTitle = links.videoTitle }
    }
    var boardIndex by remember { mutableIntStateOf(initialBoard) }
    val board = boards[boardIndex]

    SubPage("Setup guide", snackbar, onBack) {
        MatrixHero(remember { com.electroiot.dotglow.ui.matrix.ScrollScene("BUILD YOUR OWN DOTGLOW CLOCK", 32) },
            "Build and set up your LED matrix clock")

        SectionCard("Video") {
            if (videoUrl.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, null, tint = LedRed, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(videoTitle.ifEmpty { "Watch the setup video" }, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))) }) { Text("Watch on YouTube") }
            } else {
                Text("A step-by-step setup video is coming soon. It will appear here automatically.", color = Muted)
            }
        }

        Step(1, "What you need") {
            Bullet("A board: ESP32-S3, ESP32 DevKit, or ESP8266 NodeMCU / Wemos D1 mini (not ESP32-S2: no Bluetooth)")
            Bullet("A MAX7219 4-in-1 LED matrix (8x32, the common blue \"FC-16\" module)")
            Bullet("5 jumper wires (female-female for most modules)")
            Bullet("A good 5 V / 1 A USB adapter and cable")
            Bullet("Optional: 470-1000 µF capacitor, LDR + 10 kΩ resistor")
            Spacer(Modifier.height(6.dp))
            InfoRow("ESP32 / ESP32-S3", "DotGlow: app + WiFi")
            InfoRow("ESP8266", "Matrix Clock: WiFi + web page")
        }

        Step(2, "Connect the display") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                boards.forEachIndexed { i, b ->
                    SegmentedButton(selected = boardIndex == i, onClick = { boardIndex = i },
                        shape = SegmentedButtonDefaults.itemShape(i, boards.size)) { Text(b.name, maxLines = 1, fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            GuideImage(board.image, "Wiring for ${board.name}")
            Spacer(Modifier.height(10.dp))
            board.pins.forEach { (m, e) -> InfoRow("Display $m", "${board.name} $e") }
        }

        Step(3, "Install the firmware") {
            Text("Plug the board into this phone with a USB-OTG adapter (or a USB-C data cable) and tap the button. " +
                "The app finds out which board it is and installs the right firmware" +
                (if (board.esp8266) " (Matrix Clock for ESP8266)." else " (DotGlow)."))
            Spacer(Modifier.height(10.dp))
            Button(onClick = onFlash) { Text("Install firmware") }
            Spacer(Modifier.height(6.dp))
            Text("From a computer instead: PlatformIO or Arduino IDE, see the GitHub page.", color = Muted, fontSize = 13.sp)
        }

        if (board.esp8266) {
            Step(4, "Connect it to WiFi") {
                Bullet("Power the clock. With no WiFi saved it scrolls its setup hotspot: \"MatrixClock-XXXX\".")
                Bullet("On your phone, join that WiFi. The setup page opens by itself (or open http://192.168.4.1).")
                Bullet("Pick your home WiFi, type its password and save. The clock restarts and gets the time.")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }) {
                    Text("Open WiFi settings")
                }
            }
            Step(5, "Settings page") {
                Bullet("The clock scrolls its IP address after connecting. Open it in a browser (or http://matrixclock.local).")
                Bullet("Brightness, night mode, animations, messages, special days, display size, updates: all on that page.")
                Bullet("Protect the page with a password: Settings password card.")
                Bullet("FLASH button: short press shows the IP, hold 5 s for WiFi setup, hold 10 s to remove the page password.")
                Spacer(Modifier.height(6.dp))
                Text("ESP8266 has no Bluetooth, so this app can install its firmware but cannot change its settings.",
                    color = Muted, fontSize = 13.sp)
            }
        } else {
            Step(4, "Pair the app with your clock") {
                Bullet("Plug the clock in. It shows an animation and its welcome text.")
                Bullet("Open DotGlow and tap your clock (\"DotGlow-XXXX\") in the list.")
                Bullet("The clock shows a 6-digit PIN on its display. Type it in the pairing request on your phone.")
            }

            Step(5, "Connect it to WiFi") {
                Bullet("In the app, tap Set up next to \"This clock isn't on WiFi yet\" (or Settings → WiFi).")
                Bullet("Pick your network, type its password, Save & connect.")
                Bullet("The clock restarts, gets the time from the internet, and the app reconnects by itself.")
            }
        }

        SectionCard("Optional: more modules") {
            GuideImage(R.drawable.guide_chain, "Chaining boards with a separate power supply")
            Spacer(Modifier.height(10.dp))
            Text("Connect OUT of the last board to IN of the next, then set the module count in Settings → Display. " +
                "Above 4 modules, use a separate 5 V supply and a 470-1000 µF capacitor at each board (+ leg to 5 V).", fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            listOf("4 modules" to "1 A", "8 modules" to "2 A", "12 modules" to "3 A", "16 modules" to "4 A").forEach { (m, a) -> InfoRow(m, "5 V / $a") }
        }

        SectionCard("Optional: light sensor") {
            GuideImage(R.drawable.guide_ldr, "Light sensor wiring")
            Spacer(Modifier.height(10.dp))
            Text("LDR from 3V3 to ${board.ldrPin}, 10 kΩ from ${board.ldrPin} to GND. Then turn on Auto brightness " +
                (if (board.esp8266) "on the clock's settings page." else "in Settings → Brightness & night."), fontSize = 14.sp)
        }

        if (!board.esp8266) SectionCard("Stopwatch") {
            Text("How it looks on the clock", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            StopwatchLooks()
            Spacer(Modifier.height(6.dp))
            Text("MM:SS with tenths filling the bottom row; HH:MM after an hour (HH:MM:SS on 6+ modules). " +
                "Start it from the app (Stopwatch) or with an optional button:", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(10.dp))
            GuideImage(R.drawable.guide_button, "Stopwatch button wiring")
            Spacer(Modifier.height(10.dp))
            Text("A push button from ${if (board.name == "ESP32-S3") "GPIO4" else "GPIO13"} to GND. Tap: start / pause " +
                "(from the clock it opens the stopwatch). Hold: reset; hold again: back to the clock. " +
                "The app has the same controls under Stopwatch.", fontSize = 14.sp)
        }

        SectionCard("Troubleshooting") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("All LEDs on", "Check CS and 5 V")
                InfoRow("Text scrambled", "Settings → Display → type")
                InfoRow("Upside down", "Settings → Display → flip")
                InfoRow("Keeps restarting", "Better 5 V supply")
                InfoRow("Can't pair", "Use the PIN on the display")
                InfoRow("Install fails", "Data cable; hold BOOT")
            }
        }
    }
}
