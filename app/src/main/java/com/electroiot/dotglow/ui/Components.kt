package com.electroiot.dotglow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A settings page: back arrow + title + scrolling content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPage(title: String, snackbar: SnackbarHostState, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            subtitle?.let { Text(it, color = Muted, fontSize = 13.sp) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A labelled drop-down: options are (value, label) pairs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceRow(title: String, value: T, options: List<Pair<T, String>>, subtitle: String? = null, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title)
        subtitle?.let { Text(it, color = Muted, fontSize = 13.sp) }
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(
                value = options.firstOrNull { it.first == value }?.second ?: value.toString(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (v, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(v) })
                }
            }
        }
    }
}

/** Integer slider; [onChange] gets (value, finished). */
@Composable
fun SliderRow(title: String, value: Int, range: IntRange, valueLabel: (Int) -> String = { it.toString() }, onChange: (Int, Boolean) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(title)
            Spacer(Modifier.weight(1f))
            Text(valueLabel(v.toInt()), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = v,
            onValueChange = { v = it; onChange(it.toInt(), false) },
            onValueChangeFinished = { onChange(v.toInt(), true) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

/** A row on the home screen that opens a settings page. */
@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = LedRed)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
    }
}

@Composable
fun RowDivider() = HorizontalDivider(color = PanelLine)

/** Yes / no confirmation; null [text] = closed. */
@Composable
fun Confirm(title: String, text: String?, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (text == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val AnimationNames = listOf(0 to "Off", 1 to "Sparkle", 2 to "Wipe", 3 to "Rain", 4 to "Boxes", 5 to "Pac-Man", 6 to "Random")
val DigitStyles = listOf(0 to "Roll down", 1 to "Roll up", 3 to "Dissolve", 4 to "Slide", 5 to "Flip", 6 to "Drop & bounce", 7 to "Random", 2 to "Instant")
val TimeZones = listOf(
    "IST-5:30" to "India (UTC+5:30)", "<+0545>-5:45" to "Nepal (UTC+5:45)", "<+06>-6" to "Bangladesh (UTC+6)",
    "<+0530>-5:30" to "Sri Lanka (UTC+5:30)", "<+04>-4" to "UAE / Oman (UTC+4)", "<+03>-3" to "Saudi Arabia / Qatar (UTC+3)",
    "<+08>-8" to "Singapore / Malaysia (UTC+8)", "GMT0" to "UTC / GMT", "GMT0BST,M3.5.0/1,M10.5.0" to "UK (London)",
    "CET-1CEST,M3.5.0,M10.5.0/3" to "Central Europe", "EST5EDT,M3.2.0,M11.1.0" to "US Eastern",
    "CST6CDT,M3.2.0,M11.1.0" to "US Central", "PST8PDT,M3.2.0,M11.1.0" to "US Pacific",
    "AEST-10AEDT,M10.1.0,M4.1.0/3" to "Australia (Sydney)",
)
val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
fun hourLabel(h: Int) = when { h == 0 -> "12 AM"; h < 12 -> "$h AM"; h == 12 -> "12 PM"; else -> "${h - 12} PM" }
