package com.electroiot.dotglow.ui.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.electroiot.dotglow.ui.LedRed

private val Board = Color(0xFF060607)
private val ModuleFace = Color(0xFF0D0D10)
private val DotOff = Color(0xFF2A0C0E)

/**
 * A MAX7219 matrix (8 rows x [modules] x 8 columns) playing [scene]. Looping scenes start
 * over after their [Scene.durationMs] plus [pauseMs]; [playing] = false freezes the picture.
 */
@Composable
fun LedMatrix(
    scene: Scene,
    modifier: Modifier = Modifier,
    modules: Int = 4,
    loop: Boolean = true,
    pauseMs: Long = 600,
    playing: Boolean = true,
    color: Color = LedRed,
    flipH: Boolean = false,   // the clock's "mirror left-right"
    flipV: Boolean = false,   // and "flip top-bottom"
) {
    val width = modules * 8
    val frame = remember(width) { Frame(width) }
    val clock = remember(scene) { mutableLongStateOf(0L) }
    LaunchedEffect(scene, playing) {
        if (!playing) return@LaunchedEffect
        val start = withFrameMillis { it } - clock.longValue
        while (true) withFrameMillis { now -> clock.longValue = now - start }
    }

    Canvas(modifier.fillMaxWidth().aspectRatio((width + 2f) / 10f)) {
        // scene time (looping), then draw it into the frame
        val raw = clock.longValue
        val period = scene.durationMs
        val t = when {
            period == Long.MAX_VALUE -> raw
            loop -> raw % (period + pauseMs)
            else -> raw.coerceAtMost(period + pauseMs)
        }
        frame.clear()
        if (t < period) scene.draw(frame, t)

        val cell = size.width / (width + 2f)
        val pad = cell
        drawRoundRect(Board, cornerRadius = CornerRadius(cell * 0.8f))
        for (m in 0 until modules) {   // each 8x8 module, with a thin gap between them
            drawRoundRect(
                ModuleFace,
                topLeft = Offset(pad + m * 8 * cell + cell * 0.08f, pad - cell * 0.1f),
                size = Size(8 * cell - cell * 0.16f, 8 * cell + cell * 0.2f),
                cornerRadius = CornerRadius(cell * 0.3f),
            )
        }
        val r = cell * 0.36f
        val glowColors = listOf(color.copy(alpha = 0.45f), Color.Transparent)
        for (y in 0 until 8) for (x in 0 until width) {
            val c = Offset(pad + (x + 0.5f) * cell, pad + (y + 0.5f) * cell)
            if (frame[if (flipH) width - 1 - x else x, if (flipV) 7 - y else y]) {
                drawCircle(Brush.radialGradient(glowColors, center = c, radius = cell * 0.95f), radius = cell * 0.95f, center = c)
                drawCircle(color, radius = r, center = c)
                drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 0.35f, center = c - Offset(r * 0.25f, r * 0.25f))
            } else {
                drawCircle(DotOff, radius = r, center = c)
            }
        }
    }
}
