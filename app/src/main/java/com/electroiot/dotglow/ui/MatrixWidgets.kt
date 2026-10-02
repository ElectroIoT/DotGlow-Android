package com.electroiot.dotglow.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroiot.dotglow.ui.matrix.ClockScene
import com.electroiot.dotglow.ui.matrix.FaceStyle
import com.electroiot.dotglow.ui.matrix.LedMatrix
import com.electroiot.dotglow.ui.matrix.Scene
import com.electroiot.dotglow.ui.matrix.TextScene
import com.electroiot.dotglow.ui.matrix.animationScene
import com.electroiot.dotglow.ui.matrix.digitStyleDemo
import java.time.LocalTime

/** Milliseconds since midnight on the phone. */
fun phoneMsOfDay(): Long = LocalTime.now().toNanoOfDay() / 1_000_000

/** A clock face showing the phone's time, restyled live from [style]. */
@Composable
fun rememberPhoneClock(style: FaceStyle = FaceStyle()): Scene {
    val current by rememberUpdatedState(style)
    return remember { ClockScene({ current }) { phoneMsOfDay() } }
}

/** The big matrix at the top of a screen, with a caption under it. */
@Composable
fun MatrixHero(scene: Scene, caption: String = "", modules: Int = 4, label: String = "") {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF050506))
            .border(1.dp, PanelLine, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(LedRed))
                Spacer(Modifier.width(6.dp))
                Text(label.uppercase(), color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp)
            }
        }
        // a 16-module display would be tiny on a phone: show at most 8, scaled to the width
        LedMatrix(scene, modules = modules.coerceIn(4, 8))
        if (caption.isNotEmpty()) {
            Text(caption, color = Muted, modifier = Modifier.padding(top = 10.dp), textAlign = TextAlign.Center)
        }
    }
}

/** Cards in a grid that adapts to the screen width (2 columns on phones, more on tablets). */
@Composable
private fun <T> PreviewGrid(items: List<T>, minCell: Int = 150, cell: @Composable (T, Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = (maxWidth.value / minCell).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { cell(it, Modifier.weight(1f)) }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** One choice with a live preview; selected = red outline and a check. */
@Composable
fun PreviewCard(
    title: String,
    scene: Scene,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (selected) LedRed else PanelLine, label = "border")
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, label = "scale")
    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, border), RoundedCornerShape(14.dp))
            .background(Color(0xFF101217))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        LedMatrix(scene, modules = 4, pauseMs = 500)
        Row(Modifier.padding(top = 8.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = LedRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f), maxLines = 1)
            if (onPlay != null) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1E222B)).clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.PlayArrow, "Play on the clock", tint = LedRed, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

/** Effects (Off, Sparkle ... Random) with live previews. */
@Composable
fun AnimationPicker(selected: Int, includeOff: Boolean = true, onPlay: ((Int) -> Unit)? = null, onPick: (Int) -> Unit) {
    val items = AnimationNames.filter { includeOff || it.first != 0 }
    val scenes = remember { items.associate { (n, _) -> n to (if (n == 0) TextScene("OFF") else animationScene(n, 32)) } }
    PreviewGrid(items) { (n, name), mod ->
        PreviewCard(name, scenes.getValue(n), selected == n, mod,
            onPlay = if (onPlay != null && n in 1..5) ({ onPlay(n) }) else null) { onPick(n) }
    }
}

/** Digit-change styles, each showing the minutes ticking over. */
@Composable
fun DigitStylePicker(selected: Int, fmt24: Boolean, onPick: (Int) -> Unit) {
    val scenes = remember(fmt24) { DigitStyles.associate { (n, _) -> n to digitStyleDemo(n, fmt24) } }
    PreviewGrid(DigitStyles) { (n, name), mod ->
        PreviewCard(name, scenes.getValue(n), selected == n, mod) { onPick(n) }
    }
}

/** How the clock face looks, from the clock's settings. */
fun faceStyle(c: com.electroiot.dotglow.ClockConfig) = FaceStyle(
    fmt24 = c.flag("fmt24"), leadingZero = c.flag("leadingZero"), showSecs = c.flag("showSecs"),
    blinkColon = c.flag("blinkColon"), secondsBar = c.flag("secondsBar"), roll = c.int("roll"),
)
