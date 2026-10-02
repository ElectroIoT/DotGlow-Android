package com.electroiot.dotglow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full-screen PIN pad shown when the app lock is on. */
@Composable
fun LockScreen(check: (String) -> Boolean, biometric: Boolean, onBiometric: () -> Unit, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (biometric) onBiometric() }

    fun press(d: String) {
        if (pin.length >= 8) return
        pin += d
        wrong = false
        if (pin.length >= 4 && check(pin)) onUnlocked()
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, null, tint = LedRed, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Enter your DotGlow PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maxOf(4, pin.length)) { i ->
                Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) LedRed else PanelLine))
            }
        }
        if (wrong) Text("Wrong PIN", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(28.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                row.forEach { k ->
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        when (k) {
                            "bio" -> if (biometric) IconButton(onClick = onBiometric) { Icon(Icons.Default.Fingerprint, "Fingerprint", tint = LedRed, modifier = Modifier.size(32.dp)) }
                            "del" -> IconButton(onClick = { pin = pin.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Backspace, "Delete", tint = Muted) }
                            else -> FilledTonalButton(onClick = { press(k) }, modifier = Modifier.size(72.dp), shape = CircleShape) {
                                Text(k, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        if (pin.length >= 4) TextButton(onClick = { wrong = !check(pin); if (!wrong) onUnlocked() else pin = "" }) { Text("OK") }
    }
}

/** Asks for a new app PIN twice. */
@Composable
fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val digits = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set app PIN") },
        text = {
            Column {
                Text("4 to 8 digits. You'll need it every time DotGlow opens.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(a, { if (it.length <= 8 && it.all(Char::isDigit)) a = it }, label = { Text("PIN") },
                    singleLine = true, keyboardOptions = digits, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(b, { if (it.length <= 8 && it.all(Char::isDigit)) b = it }, label = { Text("Repeat PIN") },
                    singleLine = true, keyboardOptions = digits, visualTransformation = PasswordVisualTransformation(),
                    isError = b.isNotEmpty() && a != b)
            }
        },
        confirmButton = { TextButton(onClick = { onSet(a) }, enabled = a.length >= 4 && a == b) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
