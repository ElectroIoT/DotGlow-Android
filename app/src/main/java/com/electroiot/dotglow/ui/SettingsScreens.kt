package com.electroiot.dotglow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electroiot.dotglow.ClockViewModel

// =================================================================================
// WiFi
// =================================================================================

@Composable
fun WifiScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val networks by vm.networks.collectAsStateWithLifecycle()
    val scanning by vm.wifiScanning.collectAsStateWithLifecycle()
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.scanWifi() }

    SubPage("WiFi", snackbar, onBack) {
        SectionCard("Current") {
            InfoRow("Network", status?.ssid?.takeIf { it.isNotEmpty() } ?: "Not connected")
            InfoRow("IP address", status?.ip ?: "-")
        }
        SectionCard("Choose a network") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (scanning) "Scanning..." else "${networks?.size ?: 0} networks found", color = Muted)
                Spacer(Modifier.weight(1f))
                if (scanning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else OutlinedButton(onClick = vm::scanWifi) { Text("Rescan") }
            }
            networks?.forEach { n ->
                Row(
                    Modifier.fillMaxWidth().clickable { ssid = n.ssid; pass = "" }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (n.locked) Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = Muted) else Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(n.ssid, Modifier.weight(1f), fontWeight = if (n.ssid == ssid) FontWeight.Bold else FontWeight.Normal,
                        color = if (n.ssid == ssid) LedRed else MaterialTheme.colorScheme.onSurface)
                    SignalBars(n.rssi + 10)
                }
                RowDivider()
            }
        }
        SectionCard("Connect") {
            OutlinedTextField(ssid, { ssid = it }, Modifier.fillMaxWidth(), label = { Text("Network name") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show") } },
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.joinWifi(ssid, pass) }, enabled = ssid.isNotBlank()) { Text("Save & connect") }
            Text("The clock restarts and joins the network; the app reconnects by itself.", color = Muted, fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// =================================================================================
// Clock & time
// =================================================================================

@Composable
fun ClockSettingsScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    SubPage("Clock & time", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        SectionCard("Time") {
            ChoiceRow("Time format", c.int("fmt24"), listOf(0 to "12 hour", 1 to "24 hour")) { vm.set("fmt24", it) }
            ChoiceRow("Time zone", c.str("tz"), TimeZones.let { z -> if (z.any { it.first == c.str("tz") }) z else z + (c.str("tz") to c.str("tz")) }) { vm.set("tz", it) }
            OutlinedButton(onClick = vm::syncTime, Modifier.padding(top = 8.dp)) { Text("Re-sync time now") }
        }
        SectionCard("Look") {
            SwitchRow("Blinking colon", c.flag("blinkColon"), "Middle dots blink every second") { vm.set("blinkColon", it) }
            SwitchRow("Seconds bar", c.flag("secondsBar"), "Thin line that fills up each minute") { vm.set("secondsBar", it) }
            SwitchRow("Leading zero", c.flag("leadingZero"), "07:05 instead of 7:05") { vm.set("leadingZero", it) }
            SwitchRow("Show date", c.flag("showDate"), "Scrolls by once a minute") { vm.set("showDate", it) }
        }
    }
}

// =================================================================================
// Brightness & night
// =================================================================================

@Composable
fun BrightnessScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    SubPage("Brightness & night", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        SectionCard("Brightness") {
            SliderRow("Brightness", c.int("br"), 0..15) { v, done -> vm.setBrightness(v, done) }
        }
        SectionCard("Night mode") {
            SwitchRow("Dim at night", c.flag("autoDim")) { vm.set("autoDim", it) }
            if (c.flag("autoDim")) {
                val hours = (0..23).map { it to hourLabel(it) }
                ChoiceRow("From", c.int("nightFrom"), hours) { vm.set("nightFrom", it) }
                ChoiceRow("Until", c.int("nightTo"), hours) { vm.set("nightTo", it) }
                ChoiceRow("Night brightness", c.int("nightBr"), (0..15).map { it to "$it" } + (16 to "Display off")) { vm.set("nightBr", it) }
            }
        }
        SectionCard("Auto brightness (light sensor)") {
            SwitchRow("Auto brightness", c.flag("ldrOn"), "Needs a light sensor (LDR) - see Setup guide") { vm.set("ldrOn", it) }
            if (c.flag("ldrOn")) {
                InfoRow("Light now", status?.light?.takeIf { it >= 0 }?.let { "$it% · brightness ${status?.brightnessNow}" } ?: "-")
                ChoiceRow("In the dark", c.int("ldrMin"), (0..15).map { it to "$it" }) { vm.set("ldrMin", it) }
                ChoiceRow("In bright light", c.int("ldrMax"), (0..15).map { it to "$it" }) { vm.set("ldrMax", it) }
                SwitchRow("Reverse sensor", c.flag("ldrInvert"), "If the display gets brighter in the dark") { vm.set("ldrInvert", it) }
            }
        }
    }
}

// =================================================================================
// Display layout
// =================================================================================

@Composable
fun DisplayScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<Pair<String, Int>?>(null) }
    SubPage("Display", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        SectionCard("Modules") {
            ChoiceRow("Modules in the chain", c.int("modules", 4), (4..16).map { it to "$it  (8x${it * 8})" },
                "The clock restarts to apply this") { pending = "modules" to it }
            ChoiceRow("Module type", c.int("hwType"), listOf(0 to "FC-16 (common blue 4-in-1)", 1 to "Generic", 2 to "Parola", 3 to "IC-Station"),
                "Change it if the text looks scrambled") { pending = "hwType" to it }
            OutlinedButton(onClick = vm::showModuleNumbers, Modifier.padding(top = 8.dp)) { Text("Show module numbers") }
        }
        SectionCard("Orientation") {
            SwitchRow("Mirror left-right", c.flag("flipH")) { vm.set("flipH", it) }
            SwitchRow("Flip top-bottom", c.flag("flipV"), "Both on = upside down") { vm.set("flipV", it) }
            SwitchRow("Show seconds", c.flag("showSecs"), "Needs 6 or more modules") { vm.set("showSecs", it) }
        }
    }
    Confirm("Restart the clock?", pending?.let { "The clock restarts to apply the new display layout. The app reconnects by itself." },
        "Restart", onConfirm = { pending?.let { (k, v) -> vm.set(k, v) } }, onDismiss = { pending = null })
}

// =================================================================================
// Animations
// =================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimationsScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    SubPage("Animations", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        SectionCard("Digit change") {
            ChoiceRow("Style", c.int("roll"), DigitStyles, "Plays on the clock when you pick one") { vm.set("roll", it) }
        }
        SectionCard("Effects") {
            ChoiceRow("Every hour", c.int("hourlyAnim"), AnimationNames) { vm.set("hourlyAnim", it) }
            ChoiceRow("At power-on", c.int("bootAnim"), AnimationNames) { vm.set("bootAnim", it) }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = vm::previewDigits) { Text("Digit change") }
                AnimationNames.filter { it.first in 1..5 }.forEach { (n, name) ->
                    FilledTonalButton(onClick = { vm.playAnimation(n) }) { Text(name) }
                }
            }
        }
    }
}

// =================================================================================
// Message
// =================================================================================

@Composable
fun MessageScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    SubPage("Message", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        var text by remember(c.str("msg")) { mutableStateOf(c.str("msg")) }
        var every by remember(c.int("msgEvery")) { mutableIntStateOf(c.int("msgEvery")) }
        SectionCard("Scrolling message") {
            OutlinedTextField(text, { if (it.length <= 64) text = it }, Modifier.fillMaxWidth(), label = { Text("Message") },
                supportingText = { Text("${text.length}/64") })
            ChoiceRow("Repeat", every, listOf(0 to "Don't repeat", 1 to "Every minute", 5 to "Every 5 minutes",
                15 to "Every 15 minutes", 30 to "Every 30 minutes", 60 to "Every hour")) { every = it }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.saveMessage(text, every, true) }, enabled = text.isNotBlank()) { Text("Show now") }
                OutlinedButton(onClick = { vm.saveMessage(text, every, false) }) { Text("Save") }
                OutlinedButton(onClick = { text = ""; every = 0; vm.saveMessage("", 0, false) }) { Text("Clear") }
            }
        }
    }
}

// =================================================================================
// Special days
// =================================================================================

@Composable
fun SpecialDaysScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var day by remember { mutableIntStateOf(1) }
    var month by remember { mutableIntStateOf(1) }
    var anim by remember { mutableIntStateOf(6) }
    var text by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Int?>(null) }
    SubPage("Special days", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        SectionCard("Every year") {
            Text("Shown with an animation every 15 minutes on that day.", color = Muted, fontSize = 13.sp)
            if (c.events.isEmpty()) Text("No special days yet.", color = Muted, modifier = Modifier.padding(top = 10.dp))
            c.events.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.text, fontWeight = FontWeight.Medium)
                        Text("${e.day} ${Months[e.month - 1]} · ${AnimationNames.firstOrNull { it.first == e.anim }?.second ?: ""}", color = Muted, fontSize = 13.sp)
                    }
                    IconButton(onClick = { vm.showSpecialDay(e.index) }) { Icon(Icons.Default.PlayArrow, "Show now") }
                    IconButton(onClick = { deleting = e.index }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
                RowDivider()
            }
        }
        SectionCard("Add a special day") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { ChoiceRow("Day", day, (1..31).map { it to "$it" }) { day = it } }
                Column(Modifier.weight(1f)) { ChoiceRow("Month", month, Months.mapIndexed { i, m -> (i + 1) to m }) { month = it } }
            }
            OutlinedTextField(text, { if (it.length <= 40) text = it }, Modifier.fillMaxWidth(), label = { Text("Text") },
                placeholder = { Text("Happy Birthday Rahul!") }, singleLine = true)
            ChoiceRow("Animation", anim, AnimationNames) { anim = it }
            Button(onClick = { vm.addSpecialDay(month, day, anim, text.trim()); text = "" }, enabled = text.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp)) { Text("Add special day") }
        }
    }
    Confirm("Delete?", deleting?.let { "Remove this special day?" }, "Delete",
        onConfirm = { deleting?.let(vm::deleteSpecialDay) }, onDismiss = { deleting = null })
}

// =================================================================================
// Updates
// =================================================================================

@Composable
fun UpdatesScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    SubPage("Updates", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        val s = status
        SectionCard("Firmware") {
            InfoRow("Installed", s?.let { "v${it.version}" } ?: "-")
            InfoRow("Latest on GitHub", s?.latest?.takeIf { it.isNotEmpty() }?.let { "v$it" } ?: "-", highlight = s?.newer == true)
            s?.otaMessage?.takeIf { it.isNotEmpty() }?.let { Text(if (s.otaBusy) "Working..." else it, color = Muted, fontSize = 13.sp) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::checkUpdate, enabled = s?.mode == "clock") { Text("Check now") }
                if (s?.newer == true) Button(onClick = { confirm = true }) { Text("Install update") }
            }
            if (s?.mode != "clock") Text("Updates need the clock to be on WiFi.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        SectionCard("Automatic") {
            SwitchRow("Automatic updates", c.flag("autoUpdate"), "Installs new versions at night and after power-on") { vm.set("autoUpdate", it) }
        }
    }
    Confirm("Install the update?", if (confirm) "The clock downloads the new firmware and restarts. It takes about a minute." else null,
        "Install", onConfirm = vm::installUpdate, onDismiss = { confirm = false })
}

// =================================================================================
// Security (clock password, paired phones, app lock)
// =================================================================================

@Composable
fun SecurityScreen(
    vm: ClockViewModel,
    snackbar: SnackbarHostState,
    appLockOn: Boolean,
    biometricAvailable: Boolean,
    biometricOn: Boolean,
    onSetAppPin: () -> Unit,
    onRemoveAppPin: () -> Unit,
    onBiometric: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var cur by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var forget by remember { mutableStateOf(false) }
    SubPage("Security", snackbar, onBack) {
        val c = cfg ?: return@SubPage
        val has = c.flag("hasPassword")
        SectionCard("This app") {
            SwitchRow("App lock", appLockOn, "Ask for a PIN when DotGlow opens") { if (it) onSetAppPin() else onRemoveAppPin() }
            if (appLockOn && biometricAvailable) SwitchRow("Use fingerprint / face", biometricOn) { onBiometric(it) }
        }
        SectionCard("Web page password") {
            Text(if (has) "The clock's web page asks for this password." else "No password: anyone on your WiFi can change settings on the web page.",
                color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            val hidden = PasswordVisualTransformation()
            if (has) OutlinedTextField(cur, { cur = it }, Modifier.fillMaxWidth(), label = { Text("Current password") }, singleLine = true, visualTransformation = hidden)
            OutlinedTextField(new1, { new1 = it }, Modifier.fillMaxWidth(), label = { Text("New password (6+ characters)") }, singleLine = true, visualTransformation = hidden)
            OutlinedTextField(new2, { new2 = it }, Modifier.fillMaxWidth(), label = { Text("Repeat new password") }, singleLine = true, visualTransformation = hidden,
                isError = new2.isNotEmpty() && new1 != new2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.setPassword(cur, new1) { cur = ""; new1 = ""; new2 = "" } }, enabled = new1.length >= 6 && new1 == new2) {
                    Text(if (has) "Change" else "Set password")
                }
                if (has) OutlinedButton(onClick = { vm.setPassword(cur, "") { cur = "" } }) { Text("Remove") }
            }
            Text("Forgot it? Hold the clock's BOOT button for 10 seconds.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        SectionCard("Paired phones") {
            Text("Phones paired with the PIN on the clock's display can change every setting.", color = Muted, fontSize = 13.sp)
            OutlinedButton(onClick = { forget = true }, Modifier.padding(top = 8.dp)) { Text("Forget all paired phones") }
        }
    }
    Confirm("Forget all phones?", if (forget) "Every phone, including this one, has to pair again with the PIN on the clock." else null,
        "Forget", onConfirm = vm::forgetPhones, onDismiss = { forget = false })
}

// =================================================================================
// System
// =================================================================================

@Composable
fun SystemScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    var ask by remember { mutableStateOf<String?>(null) }
    SubPage("System", snackbar, onBack) {
        val s = status
        SectionCard("Clock") {
            InfoRow("Firmware", s?.let { "v${it.version} · ${it.board}" } ?: "-")
            InfoRow("Mode", when (s?.mode) { "setup" -> "WiFi setup"; "safe" -> "Safe mode"; null -> "-"; else -> "Normal" })
            InfoRow("Running for", s?.uptimeS?.let { val d = it / 86400; val h = it % 86400 / 3600; val m = it % 3600 / 60; (if (d > 0) "${d}d " else "") + "${h}h ${m}m" } ?: "-")
        }
        SectionCard("Actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::showIp) { Text("Show IP") }
                OutlinedButton(onClick = { ask = "restart" }) { Text("Restart") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { ask = "factory" }) { Text("Factory reset", color = MaterialTheme.colorScheme.error) }
        }
    }
    Confirm(
        if (ask == "factory") "Factory reset?" else "Restart the clock?",
        when (ask) {
            "factory" -> "Erases every setting, the WiFi network and all paired phones. The clock goes back to setup mode."
            "restart" -> "The app reconnects by itself."
            else -> null
        },
        if (ask == "factory") "Reset" else "Restart",
        onConfirm = { if (ask == "factory") vm.factoryReset() else vm.restartClock() },
        onDismiss = { ask = null },
    )
}
