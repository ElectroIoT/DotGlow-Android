package com.electroiot.dotglow.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electroiot.dotglow.ClockViewModel
import com.electroiot.dotglow.Stopwatch
import com.electroiot.dotglow.ui.matrix.StopwatchScene

private fun format(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d.%d".format(s / 3600, s / 60 % 60, s % 60, ms % 1000 / 100)
    else "%02d:%02d.%d".format(s / 60, s % 60, ms % 1000 / 100)
}

@Composable
fun StopwatchScreen(vm: ClockViewModel, snackbar: SnackbarHostState, onGuide: () -> Unit, onBack: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val sw = status?.stopwatch ?: Stopwatch()
    val current by rememberUpdatedState(sw)
    val scene = remember { StopwatchScene({ current.now() }, { current.running }) }

    // the big digital readout under the matrix, updated every frame while it runs
    var shown by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { shown = current.now() } }

    SubPage("Stopwatch", snackbar, onBack) {
        MatrixHero(
            scene, modules = cfg?.int("modules", 4) ?: 4,
            label = if (sw.on) "On the clock" else "",
            caption = when {
                !sw.on -> "Start it: the clock switches to the stopwatch"
                sw.running -> "Running"
                sw.ms > 0 -> "Paused"
                else -> "Ready"
            },
        )
        Text(
            format(shown), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 44.sp,
            color = if (sw.running) LedRed else Muted,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(sw.running, label = "startPause", modifier = Modifier.weight(1f)) { running ->
                Button(onClick = { vm.stopwatch(if (running) "pause" else "start") }, Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Pause" else if (sw.ms > 0) "Resume" else "Start", fontSize = 16.sp)
                }
            }
            FilledTonalButton(onClick = { vm.stopwatch("reset") }, Modifier.weight(1f).height(56.dp), enabled = sw.ms > 0 || sw.running) {
                Icon(Icons.Default.Replay, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset", fontSize = 16.sp)
            }
        }
        if (sw.on) {
            OutlinedButton(onClick = { vm.stopwatch("exit") }, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Back to the clock")
            }
        }
        SectionCard("How it looks on the clock") { StopwatchLooks() }
        SectionCard("Button on the clock (optional)") {
            Text("Wire a push button to the clock and use the stopwatch without the phone: tap = start / pause, " +
                "hold = reset, hold again = back to the clock.", fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            InfoRow("ESP32-S3", "GPIO4 to GND")
            InfoRow("ESP32 DevKit", "GPIO13 to GND")
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onGuide) { Text("Wiring picture in the setup guide") }
            }
        }
    }
}
