package com.electroiot.dotglow.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroiot.dotglow.AboutInfo
import com.electroiot.dotglow.AboutRepo
import com.electroiot.dotglow.LinkItem
import com.electroiot.dotglow.ui.matrix.ScrollScene
import kotlinx.coroutines.delay

/** Slides + fades its content in, [index] x 70 ms after the screen opens. */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 70L); shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) { content() }
}

@Composable
private fun rememberAbout(): AboutInfo {
    val context = LocalContext.current
    val repo = remember { AboutRepo(context) }
    var info by remember { mutableStateOf(repo.cached) }
    LaunchedEffect(Unit) { repo.refresh()?.let { info = it } }
    return info
}

private fun open(context: android.content.Context, url: String) {
    val uri = if (url.contains('@') && !url.startsWith("http")) Uri.parse("mailto:$url") else Uri.parse(url)
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, value: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open(context, url) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(LedOff), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = LedRed, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (value.isNotEmpty()) Text(value, color = Muted, fontSize = 13.sp, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LinkList(icon: ImageVector, items: List<LinkItem>) {
    items.forEachIndexed { i, l ->
        if (i > 0) RowDivider()
        LinkRow(icon, l.title, l.subtitle.ifEmpty { l.url.removePrefix("https://") }, l.url)
    }
}

/** A soft red glow that breathes behind its content. */
private fun Modifier.breathingGlow(alpha: Float) = drawBehind {
    drawCircle(Brush.radialGradient(listOf(LedRed.copy(alpha = alpha), Color.Transparent)), radius = size.minDimension * 0.9f)
}

// =================================================================================
// About the app
// =================================================================================

@Composable
fun AboutAppScreen(snackbar: SnackbarHostState, onDeveloper: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val info = rememberAbout()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        0.15f, 0.45f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "glow",
    )
    SubPage("About DotGlow", snackbar, onBack) {
        Reveal(0) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(150.dp).breathingGlow(pulse), contentAlignment = Alignment.Center) {
                    // the launcher icon is an adaptive icon: draw its two layers ourselves
                    Box(Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.res.colorResource(com.electroiot.dotglow.R.color.ic_bg))) {
                        Image(painterResource(com.electroiot.dotglow.R.drawable.ic_launcher_foreground), "DotGlow", Modifier.size(96.dp))
                    }
                }
                Text("DotGlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("LED Matrix Clock", color = Muted)
                Spacer(Modifier.height(4.dp))
                Text("Version $version", color = LedRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                MatrixHero(remember(version) { ScrollScene("DOTGLOW V$version * MADE IN INDIA", 32) })
            }
        }
        Reveal(1) {
            SectionCard("The app") {
                Text("Set up and control DotGlow LED matrix clocks over Bluetooth, install firmware on new " +
                    "ESP32 / ESP8266 boards over USB, and follow the wiring guide.", fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                LinkRow(Icons.Default.Code, "Source code", info.appGithub.removePrefix("https://"), info.appGithub)
                RowDivider()
                LinkRow(Icons.Default.Language, "Website", info.appWebsite.removePrefix("https://"), info.appWebsite)
                RowDivider()
                LinkRow(Icons.Default.Email, "Email", info.appEmail, info.appEmail)
            }
        }
        Reveal(2) {
            SectionCard("Legal") {
                LinkRow(Icons.Default.PrivacyTip, "Privacy policy", "No accounts, no tracking", info.privacy)
                RowDivider()
                LinkRow(Icons.Default.Description, "Terms of use", "", info.terms)
            }
        }
        Reveal(3) { SectionCard("More projects") { LinkList(Icons.Default.Lightbulb, info.projects) } }
        Reveal(4) { SectionCard("More apps") { LinkList(Icons.Default.Apps, info.apps) } }
        Reveal(5) {
            SectionCard("Made by") {
                NavRow(Icons.Default.Person, info.devName, "About the developer") { onDeveloper() }
            }
        }
        Reveal(6) {
            SectionCard("Open source used") {
                InfoRow("MD_MAX72XX", "LGPL-2.1")
                InfoRow("NimBLE-Arduino", "Apache-2.0")
                InfoRow("ArduinoJson", "MIT")
                InfoRow("usb-serial-for-android", "MIT")
                InfoRow("Jetpack Compose", "Apache-2.0")
            }
        }
    }
}

// =================================================================================
// About the developer
// =================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutDeveloperScreen(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val context = LocalContext.current
    val info = rememberAbout()
    val repo = remember { AboutRepo(context) }
    var photo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(info.devPhoto) { photo = repo.photo(info.devPhoto) }
    val spin by rememberInfiniteTransition(label = "ring").animateFloat(
        0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ring",
    )

    SubPage("About the developer", snackbar, onBack) {
        Reveal(0) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // a turning LED-red ring around the photo
                    Box(
                        Modifier.size(150.dp).rotate(spin).clip(CircleShape).background(
                            Brush.sweepGradient(listOf(LedRed, Color(0xFFFF9A3B), Color(0xFF7C3AED), LedRed)),
                        ),
                    )
                    Box(Modifier.size(140.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background))
                    val bmp = photo
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), info.devName, Modifier.size(132.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.size(132.dp).clip(CircleShape).background(LedOff), contentAlignment = Alignment.Center) {
                            Text(info.devName.take(1), style = MaterialTheme.typography.displayMedium, color = LedRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(info.devName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(info.devTagline, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
        Reveal(1) {
            SectionCard("Find me on") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.socials.forEach { s ->
                        AssistChip(
                            onClick = { open(context, s.url) },
                            label = { Text(s.title) },
                            leadingIcon = { Icon(socialIcon(s.title), null, Modifier.size(18.dp), tint = LedRed) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1A1D25)),
                            border = null,
                        )
                    }
                }
            }
        }
        Reveal(2) {
            SectionCard("Contact") {
                LinkRow(Icons.Default.Language, "Website", info.devWebsite.removePrefix("https://"), info.devWebsite)
                RowDivider()
                LinkRow(Icons.Default.Email, "Email", info.devEmail, info.devEmail)
                RowDivider()
                LinkRow(Icons.Default.Code, "GitHub", info.devGithub.removePrefix("https://"), info.devGithub)
            }
        }
        Reveal(3) { SectionCard("Projects") { LinkList(Icons.Default.Lightbulb, info.projects) } }
        Reveal(4) { SectionCard("Apps") { LinkList(Icons.Default.Apps, info.apps) } }
        Reveal(5) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Info, null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Made with care in India", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

private fun socialIcon(title: String): ImageVector = when {
    title.contains("github", true) -> Icons.Default.Code
    title.contains("linkedin", true) -> Icons.Default.Work
    title.contains("google", true) || title.contains("play", true) -> Icons.Default.Android
    title.contains("youtube", true) -> Icons.Default.PlayCircle
    title.contains("instagram", true) -> Icons.Default.PhotoCamera
    title.contains("twitter", true) || title.startsWith("X", true) -> Icons.Default.Tag
    else -> Icons.Default.Memory
}

@Composable
private fun painterResource(id: Int) = androidx.compose.ui.res.painterResource(id)
