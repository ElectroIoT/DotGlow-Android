package com.electroiot.dotglow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroiot.dotglow.ClockStatus
import com.electroiot.dotglow.ui.matrix.ClockScene
import com.electroiot.dotglow.ui.matrix.FaceStyle
import com.electroiot.dotglow.ui.matrix.ScrollScene
import com.electroiot.dotglow.ui.matrix.SequenceScene
import com.electroiot.dotglow.ui.matrix.Timed
import com.electroiot.dotglow.ui.matrix.animationScene
import com.electroiot.dotglow.ui.matrix.digitStyleDemo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.electroiot.dotglow.ble.ClockInfo
import com.electroiot.dotglow.ble.ConnState
import com.electroiot.dotglow.ble.FoundClock

val ledText = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = LedRed,
    shadow = Shadow(color = LedRed.copy(alpha = 0.6f), blurRadius = 24f),
)

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Dotted LED-panel backdrop with glowing red text, like the real display. */
@Composable
fun LedHero(big: String, small: String = "", caption: String = "") {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF050505))
            .border(1.dp, PanelLine, RoundedCornerShape(18.dp))
            .padding(vertical = 26.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(big, style = ledText.copy(fontSize = 52.sp))
                if (small.isNotEmpty()) Text(" $small", style = ledText.copy(fontSize = 18.sp), modifier = Modifier.padding(bottom = 8.dp))
            }
            if (caption.isNotEmpty()) Text(caption, color = Muted, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
        }
    }
}

// =================================================================================
// Find clocks
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    hasPermission: Boolean,
    bluetoothOn: Boolean,
    scanning: Boolean,
    clocks: List<FoundClock>,
    snackbar: SnackbarHostState,
    onGrant: () -> Unit,
    onRescan: () -> Unit,
    onPick: (FoundClock) -> Unit,
    onGuide: () -> Unit,
    onFlash: () -> Unit,
    onDemo: () -> Unit,
    onAbout: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("DotGlow", fontWeight = FontWeight.Bold) },
                actions = {
                    if (hasPermission && bluetoothOn) IconButton(onClick = onRescan) { Icon(Icons.Default.Refresh, "Scan again") }
                    IconButton(onClick = onAbout) { Icon(Icons.Default.Info, "About") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().wrapContentWidth().widthIn(max = 640.dp).padding(horizontal = 16.dp)) {
            val clock = rememberPhoneClock()
            val hero = remember { SequenceScene(ScrollScene("DOTGLOW * MANORANJAN.DEV", 32), Timed(clock, 9_000)) }
            MatrixHero(hero, "Find your clock")
            when {
                !hasPermission -> SectionCard("Bluetooth permission") {
                    Text("DotGlow uses Bluetooth to find and set up clocks near you. It never uses your location.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGrant) { Text("Allow Bluetooth") }
                }
                !bluetoothOn -> SectionCard("Bluetooth is off") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BluetoothDisabled, null, tint = Muted)
                        Spacer(Modifier.width(10.dp))
                        Text("Turn Bluetooth on, then tap refresh.")
                    }
                }
                else -> {
                    Row(Modifier.padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("NEARBY CLOCKS", style = MaterialTheme.typography.labelMedium, color = Muted, letterSpacing = 1.2.sp)
                        Spacer(Modifier.weight(1f))
                        if (scanning) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                    if (clocks.isEmpty()) {
                        Text(
                            if (scanning) "Looking for clocks... Make sure the clock is powered on and close to your phone."
                            else "No clocks found. Tap refresh to look again.",
                            color = Muted, modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(clocks, key = { it.address }) { clock -> ClockRow(clock) { onPick(clock) } }
                    }
                }
            }
            SectionCard("New here?") {
                NavRow(Icons.Default.MenuBook, "Setup guide", "Parts, wiring, first setup, video") { onGuide() }
                RowDivider()
                NavRow(Icons.Default.Usb, "Install firmware", "New ESP32, ESP32-S3 or ESP8266 (NodeMCU) board, over USB") { onFlash() }
                RowDivider()
                NavRow(Icons.Default.PlayCircle, "Try the demo clock", "See every screen without a clock") { onDemo() }
            }
        }
    }
}

@Composable
private fun ClockRow(clock: FoundClock, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(LedOff), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bluetooth, null, tint = LedRed)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(clock.name, fontWeight = FontWeight.SemiBold)
                Text(if (clock.paired) "Paired" else "Tap to pair", color = if (clock.paired) Good else Muted, fontSize = 13.sp)
            }
            SignalBars(clock.rssi)
        }
    }
}

@Composable
fun SignalBars(rssi: Int) {
    val level = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..4) Box(Modifier.width(4.dp).height((4 * i).dp).clip(RoundedCornerShape(1.dp)).background(if (i <= level) LedRed else PanelLine))
    }
}

// =================================================================================
// Connecting / pairing
// =================================================================================

@Composable
fun ConnectingScreen(state: ConnState?, onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            is ConnState.Pairing -> {
                Icon(Icons.Default.Lock, null, tint = LedRed, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Pair with your clock", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "The clock is showing a 6-digit PIN on its display. Type it in the pairing request that " +
                        "appears on your phone. Only phones paired this way can change the clock's settings.",
                    textAlign = TextAlign.Center, color = Muted,
                )
            }
            is ConnState.Failed -> {
                Text("Couldn't connect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(state.reason, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Connecting...", color = Muted)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (state is ConnState.Failed) {
            Button(onClick = onRetry) { Text("Try again") }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onCancel) { Text(if (state is ConnState.Failed) "Back" else "Cancel") }
    }
}

// =================================================================================
// Dashboard
// =================================================================================

/** Pages reachable from the home screen. */
enum class Page { Home, Wifi, Clock, Brightness, Display, Animations, Message, SpecialDays, Updates, Security, System, Guide, Flash, AboutApp, AboutDev }

private val effects = listOf(1 to "Sparkle", 2 to "Wipe", 3 to "Rain", 4 to "Boxes", 5 to "Pac-Man")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    info: ClockInfo,
    status: ClockStatus?,
    brightness: Int,
    face: FaceStyle,
    modules: Int,
    reconnecting: Boolean,
    snackbar: SnackbarHostState,
    onBrightness: (Int, Boolean) -> Unit,
    onAnimation: (Int) -> Unit,
    onDigits: () -> Unit,
    onOpen: (Page) -> Unit,
    onDisconnect: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(info.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onDisconnect) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Disconnect") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().wrapContentWidth().widthIn(max = 640.dp).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            // The clock's own time (polled every 2 s), run smoothly on the phone's clock in between.
            val offset = remember { longArrayOf(Long.MIN_VALUE) }
            LaunchedEffect(status?.time, status?.ampm) {
                val st = status ?: return@LaunchedEffect
                val clockMs = clockMsOfDay(st.time, st.ampm) ?: return@LaunchedEffect
                var d = clockMs - phoneMsOfDay()
                if (d > 43_200_000) d -= 86_400_000 else if (d < -43_200_000) d += 86_400_000
                if (offset[0] == Long.MIN_VALUE || kotlin.math.abs(d - offset[0]) > 1500) offset[0] = d
            }
            val faceNow by rememberUpdatedState(face)
            val live = remember { ClockScene({ faceNow }) { phoneMsOfDay() + offset[0].coerceAtLeast(-86_400_000) } }
            val waiting = remember { ScrollScene("WAITING FOR TIME", 32) }
            MatrixHero(
                scene = if (status?.synced == true) live else waiting,
                modules = modules,
                label = if (reconnecting) "" else "Live",
                caption = when {
                    reconnecting -> "Reconnecting..."
                    status == null -> "Reading the clock..."
                    !status.synced -> "Waiting for internet time"
                    else -> status.date
                },
            )

            if (status?.mode == "setup") {
                SectionCard("WiFi") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, tint = LedRed)
                        Spacer(Modifier.width(10.dp))
                        Text("This clock isn't on WiFi yet.", Modifier.weight(1f))
                        Button(onClick = { onOpen(Page.Wifi) }) { Text("Set up") }
                    }
                }
            }
            if (status?.newer == true) {
                SectionCard("Update") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Version ${status.latest} is available.", Modifier.weight(1f))
                        OutlinedButton(onClick = { onOpen(Page.Updates) }) { Text("View") }
                    }
                }
            }

            SectionCard("Brightness") {
                var slider by remember(brightness) { mutableFloatStateOf(brightness.toFloat()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness")
                    Spacer(Modifier.weight(1f))
                    Text(slider.toInt().toString(), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = slider,
                    onValueChange = { slider = it; onBrightness(it.toInt(), false) },
                    onValueChangeFinished = { onBrightness(slider.toInt(), true) },
                    valueRange = 0f..15f,
                    steps = 14,
                )
            }

            SectionCard("Play now") {
                Text("Tap to play it on the clock", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                val digitDemo = remember(face.roll, face.fmt24) { digitStyleDemo(face.roll, face.fmt24) }
                val scenes = remember { effects.map { (n, _) -> animationScene(n, 32) } }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { PreviewCard("Digit change", digitDemo, false, Modifier.width(150.dp)) { onDigits() } }
                    items(effects.size) { i ->
                        PreviewCard(effects[i].second, scenes[i], false, Modifier.width(150.dp)) { onAnimation(effects[i].first) }
                    }
                }
            }

            SectionCard("Settings") {
                NavRow(Icons.Default.Wifi, "WiFi", status?.ssid?.takeIf { it.isNotEmpty() } ?: "Not connected") { onOpen(Page.Wifi) }
                RowDivider()
                NavRow(Icons.Default.Schedule, "Clock & time", "Format, time zone, colon, date") { onOpen(Page.Clock) }
                RowDivider()
                NavRow(Icons.Default.LightMode, "Brightness & night", "Night mode, light sensor") { onOpen(Page.Brightness) }
                RowDivider()
                NavRow(Icons.Default.GridOn, "Display", "Modules, type, orientation") { onOpen(Page.Display) }
                RowDivider()
                NavRow(Icons.Default.AutoAwesome, "Animations", "Digit change, hourly, power-on") { onOpen(Page.Animations) }
                RowDivider()
                NavRow(Icons.Default.Message, "Message", "Your own scrolling text") { onOpen(Page.Message) }
                RowDivider()
                NavRow(Icons.Default.Cake, "Special days", "Birthdays and anniversaries") { onOpen(Page.SpecialDays) }
                RowDivider()
                NavRow(Icons.Default.SystemUpdate, "Updates", status?.let { "v${it.version}" + if (it.newer) " · update available" else "" } ?: "") { onOpen(Page.Updates) }
                RowDivider()
                NavRow(Icons.Default.Security, "Security", "App lock, web password, paired phones") { onOpen(Page.Security) }
                RowDivider()
                NavRow(Icons.Default.Settings, "System", "Restart, factory reset, info") { onOpen(Page.System) }
            }
            SectionCard("Help") {
                NavRow(Icons.Default.MenuBook, "Setup guide", "Wiring, first setup, video") { onOpen(Page.Guide) }
                RowDivider()
                NavRow(Icons.Default.Usb, "Install firmware on a new board", "ESP32, ESP32-S3, ESP8266 (NodeMCU) over USB") { onOpen(Page.Flash) }
                RowDivider()
                NavRow(Icons.Default.Info, "About DotGlow", "Version, links, developer") { onOpen(Page.AboutApp) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** "07:05:09" + "PM" (12-hour) or "19:05:09" -> ms since midnight. */
fun clockMsOfDay(time: String, ampm: String): Long? {
    val p = time.split(':').mapNotNull { it.trim().toIntOrNull() }
    if (p.size < 2) return null
    var h = p[0]
    if (ampm.equals("PM", true) && h < 12) h += 12
    if (ampm.equals("AM", true) && h == 12) h = 0
    return ((h * 60L + p[1]) * 60 + p.getOrElse(2) { 0 }) * 1000
}

@Composable
fun InfoRow(label: String, value: String, highlight: Boolean = false, icon: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { it(); Spacer(Modifier.width(6.dp)) }
        Text(label, color = Muted)
        Spacer(Modifier.weight(1f))
        Text(value, color = if (highlight) Good else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}
