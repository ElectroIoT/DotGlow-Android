package com.electroiot.dotglow.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Trouble(val problem: String, val steps: List<String>)

private data class TroubleGroup(val title: String, val items: List<Trouble>)

private val troubleGroups = listOf(
    TroubleGroup("POWER AND GROUND", listOf(
        Trouble("The board's light is on, but the app doesn't see the clock", listOf(
            "Join the grounds. The display's GND, the board's GND and the power supply's GND must all be connected together. " +
                "With separate supplies this is the most common mistake.",
            "Power the board from a stable 5 V source (a phone charger, a short thick cable). A weak supply makes it restart " +
                "again and again when Bluetooth starts, while the light stays on.",
            "Look at the display: a few seconds after power-on it shows a start-up animation. If it doesn't, the board isn't running.",
        )),
        Trouble("Display dim, every LED on, or scrambled", listOf(
            "Power the display from a 5 V / 1 A (or stronger) phone charger. A computer's USB port is too weak.",
            "Add a 470-1000 µF capacitor across the display's VCC and GND (+ leg to VCC).",
            "Check that GND of the display and the board are joined.",
        )),
        Trouble("The board restarts or drops off USB", listOf(
            "Take the display's 5 V wire off the board while flashing or testing on USB. The display takes more current than a USB port gives.",
            "Power the display from its own 5 V charger, and keep GND joined to the board.",
            "Try another USB cable (short, thick, with data) and another port.",
        )),
    )),
    TroubleGroup("DISPLAY AND WIRING", listOf(
        Trouble("Display stays dark (only blinks once at power-on)", listOf(
            "Use the IN end of the display (pins marked DIN), not OUT.",
            "Check DIN, CS and CLK against the wiring picture for your board, and VCC / GND.",
            "Make sure GND is joined (see above), and the display gets 5 V, not 3.3 V.",
        )),
        Trouble("Only the first module works", listOf(
            "The link between module 1 and 2 is broken: re-solder those joints.",
            "Check that the wires are on the IN end of the first module.",
            "Set the number of modules in Settings → Display.",
        )),
        Trouble("ESP32 won't start after wiring", listOf(
            "A wire is on a flash pin. Never use the pins labelled CLK, SD0, SD1, SD2, SD3 or CMD (GPIO6-11) on an ESP32 DevKit.",
            "Unplug everything from the board, then add the wires one by one.",
        )),
        Trouble("Text is scrambled, mirrored or upside down", listOf(
            "Settings → Display → Module type: try the other types until the text is right.",
            "Upside down: Display → Rotate 180° (or the Rotate button on the dashboard), or double-press the BOOT button.",
            "Mirrored: Display → Mirror.",
        )),
    )),
    TroubleGroup("BLUETOOTH AND THE APP", listOf(
        Trouble("The app doesn't find the clock", listOf(
            "Power the clock and keep the phone within a few metres.",
            "Bluetooth on, and \"Allow Bluetooth\" accepted. If the dialog doesn't appear any more: phone Settings → Apps → DotGlow → " +
                "Permissions → Nearby devices → Allow.",
            "Tap the refresh icon (Scan again).",
            "An ESP8266 / NodeMCU clock has no Bluetooth: set it up from its own web page instead.",
            "Restart the clock by unplugging it for 5 seconds.",
        )),
        Trouble("Can't pair, or the PIN is wrong", listOf(
            "Type the 6 digits shown on the clock's display, not 0000 or 1234. Do it within about 30 seconds.",
            "No PIN on the display means the display isn't working: check power, ground and wiring first.",
            "Still failing: phone Settings → Bluetooth → forget the DotGlow clock, restart the clock and try again.",
        )),
        Trouble("The app keeps reconnecting or says it timed out", listOf(
            "Forget the clock in the phone's Bluetooth settings, restart the clock, then connect again.",
            "Move closer and away from other Bluetooth devices.",
            "After a firmware install or a factory reset the old pairing is invalid; the app removes it and pairs again by itself.",
        )),
        Trouble("The Bluetooth update stops or is slow", listOf(
            "Stay close to the clock and keep the app open until it says it is done.",
            "Use a stable power supply for the clock (see Power and ground).",
            "Try again; if it still fails, install the firmware over USB (Install firmware).",
        )),
    )),
    TroubleGroup("WIFI AND TIME", listOf(
        Trouble("The clock can't join WiFi", listOf(
            "Use a 2.4 GHz network. ESP boards can't use 5 GHz only networks.",
            "Check the password (upper and lower case matter). A hotspot on the phone works too.",
            "Settings → WiFi → Rescan, and move the clock closer to the router.",
        )),
        Trouble("The display says \"Getting time...\"", listOf(
            "The WiFi has no internet access.",
            "Check the time zone in Settings → Clock & time, and tap Re-sync time now.",
        )),
    )),
    TroubleGroup("INSTALLING FIRMWARE", listOf(
        Trouble("Install firmware fails or the board isn't found", listOf(
            "Use a USB data cable (some cables only charge) and an OTG adapter if the phone needs one.",
            "Unplug the display's 5 V wire and every other wire from the board while flashing.",
            "Hold the BOOT button, tap RESET, release BOOT, then try again.",
            "Allow the USB permission when Android asks.",
        )),
    )),
)

@Composable
internal fun TroubleshootingCard() {
    SectionCard("Troubleshooting") {
        Text("Tap a problem to see what to check.", fontSize = 13.sp, color = Muted)
        troubleGroups.forEach { group ->
            Spacer(Modifier.height(14.dp))
            Text(group.title, style = MaterialTheme.typography.labelMedium, color = Muted, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.items.forEach { TroubleRow(it) }
            }
        }
    }
}

@Composable
private fun TroubleRow(item: Trouble) {
    var open by rememberSaveable(item.problem) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel)
            .clickable { open = !open }.animateContentSize().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.problem, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ExpandMore, if (open) "Hide" else "Show", Modifier.size(22.dp).rotate(turn), tint = Muted)
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            item.steps.forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("${i + 1}.", Modifier.width(22.dp), color = LedRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(step, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
