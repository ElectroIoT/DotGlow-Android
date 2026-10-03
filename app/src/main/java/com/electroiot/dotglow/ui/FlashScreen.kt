package com.electroiot.dotglow.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electroiot.dotglow.R
import com.electroiot.dotglow.flash.FlashState
import com.electroiot.dotglow.flash.FlashViewModel
import com.electroiot.dotglow.flash.NextSteps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(vm: FlashViewModel, snackbar: SnackbarHostState, onBack: () -> Unit, onGuide8266: () -> Unit) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val catalogError by vm.catalogError.collectAsStateWithLifecycle()
    var fromFile by rememberSaveable { mutableStateOf(false) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var showLog by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { vm.useFile(uri); fileName = vm.customFileName }
    }

    SubPage("Install firmware", snackbar, onBack = { vm.reset(); onBack() }) {
        when (val s = state) {
            FlashState.Idle -> {
                SectionCard("Which firmware") {
                    InfoRow("ESP32 / ESP32-S3", "DotGlow ${catalog?.latestVersion ?: ""}".trim())
                    Text("Bluetooth + WiFi clock, set up with this app", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("ESP8266 / NodeMCU", "Matrix Clock ${catalog?.full("esp8266")?.version ?: ""}".trim())
                    Text("WiFi clock, set up from its own web page (no Bluetooth on ESP8266)", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("The app picks the right one for your board automatically.", fontSize = 14.sp)
                    catalogError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        TextButton(onClick = vm::loadCatalog) { Text("Try again") }
                    }
                }

                SectionCard("Connect the board") {
                    GuideImage(R.drawable.guide_usb_otg, "Phone, USB-OTG adapter, cable and board")
                    Spacer(Modifier.height(10.dp))
                    Bullet("Use a USB-OTG adapter, or a USB-C to micro-USB / USB-C cable that carries data (not a charge-only cable).")
                    Bullet("ESP32-S3: use the port marked USB (or either port).")
                    Bullet("Unplug the display while flashing: a phone's USB port cannot power it, and the board may keep restarting.")
                    Bullet("Installing erases the board, including its saved settings.")
                }

                SectionCard("Advanced") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(!fromFile, { fromFile = false; vm.useFile(null); fileName = null },
                            SegmentedButtonDefaults.itemShape(0, 2)) { Text("Latest firmware") }
                        SegmentedButton(fromFile, { fromFile = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(".bin file") }
                    }
                    if (fromFile) {
                        Spacer(Modifier.height(10.dp))
                        Text("Full images (bootloader included) are written at 0x0, ESP32 app images at 0x10000.", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }) {
                            Text(fileName ?: "Choose a .bin file")
                        }
                    }
                }

                Button(
                    onClick = vm::detect,
                    enabled = !fromFile || fileName != null,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp),
                ) { Text("Find my board", fontSize = 16.sp) }
            }

            is FlashState.Busy -> SectionCard(s.title) {
                if (s.progress != null) {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("${(s.progress * 100).toInt()} %", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                } else {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
                if (s.detail.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(s.detail, color = Muted)
                }
            }

            is FlashState.Ready -> {
                SectionCard("Board found") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null, tint = LedRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(s.chip.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(s.adapter, color = Muted, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    s.firmware?.let { fw ->
                        InfoRow("Install", fw.title)
                        if (fw.version.isNotEmpty()) InfoRow("Version", fw.version)
                        InfoRow("Size", "${fw.size / 1024} KB")
                        if (fw.offset != 0) InfoRow("Address", "0x" + fw.offset.toString(16))
                    }
                    s.problem?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (s.problem == null && s.firmware != null) {
                    Text("This erases the board, including any saved settings.", color = Muted, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp))
                    Button(onClick = vm::install, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Install", fontSize = 16.sp) }
                }
                TextButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }

            is FlashState.Done -> {
                SectionCard("Done") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Good, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("${s.firmware.title} installed on the ${s.chip.label} in ${s.seconds} s",
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                when (s.next) {
                    NextSteps.DotGlow -> SectionCard("Next") {
                        Bullet("Unplug the board from the phone and power it from a USB charger (display connected).")
                        Bullet("It shows an animation and its welcome text.")
                        Bullet("Go back to the clock list, tap \"DotGlow-XXXX\" and type the PIN shown on the display.")
                        Bullet("Then set up WiFi from the app.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.reset(); onBack() }) { Text("Find my clock") }
                    }
                    NextSteps.Esp8266 -> SectionCard("Next: WiFi setup") {
                        Bullet("Power the clock from a USB charger. It scrolls its setup hotspot name.")
                        Bullet("On this phone, join the WiFi \"MatrixClock-XXXX\".")
                        Bullet("The setup page opens by itself (or open http://192.168.4.1). Pick your WiFi, type its password, save.")
                        Bullet("The clock connects, gets the time and scrolls its IP address. Open that address for all settings.")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }) { Text("WiFi settings") }
                            OutlinedButton(onClick = onGuide8266) { Text("ESP8266 guide") }
                        }
                    }
                    NextSteps.Custom -> SectionCard("Next") {
                        Text("The board restarted with the new firmware.")
                    }
                }
                TextButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Install on another board") }
            }

            is FlashState.Failed -> {
                SectionCard("Didn't work") {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(s.message)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Tips", fontWeight = FontWeight.SemiBold)
                    Bullet("Try another cable: many cables only charge.")
                    Bullet("Hold BOOT, tap RESET (EN), let go of BOOT, then tap Try again.")
                    Bullet("Unplug the display and other wires from the board.")
                }
                Button(onClick = vm::detect, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Try again") }
                TextButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }

        if (log.isNotEmpty()) {
            TextButton(onClick = { showLog = !showLog }) { Text(if (showLog) "Hide details" else "Show details") }
            if (showLog) SectionCard("Details") {
                log.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Muted) }
            }
        }
    }
}
