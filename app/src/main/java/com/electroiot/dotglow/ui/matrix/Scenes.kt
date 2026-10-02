package com.electroiot.dotglow.ui.matrix

import kotlin.random.Random

/**
 * Pictures for the on-screen LED matrix: the same clock face, digit-change styles and
 * animations as the firmware (DotGlow-ESP32 src/display.cpp), so a preview in the app
 * looks like the real display. Everything here is plain Kotlin, drawn into a [Frame].
 */

/** An 8-row LED frame buffer, [width] = 8 x number of modules. */
class Frame(val width: Int) {
    private val px = BooleanArray(width * 8)
    fun clear() = px.fill(false)
    operator fun set(x: Int, y: Int, on: Boolean) {
        if (x in 0 until width && y in 0..7) px[y * width + x] = on
    }
    operator fun get(x: Int, y: Int) = px[y * width + x]
}

/** Something drawn on the matrix; [draw] gets the time since it started. */
interface Scene {
    /** Length of one run in ms (looping scenes repeat after it); Long.MAX_VALUE = endless. */
    val durationMs: Long
    fun draw(f: Frame, tMs: Long)
}

// ---- fonts ------------------------------------------------------------------------------

/** The firmware's 5x7 clock digits, one byte per row, bit 4 = left-most pixel. */
internal val DIGITS = arrayOf(
    intArrayOf(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E), intArrayOf(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E),
    intArrayOf(0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F), intArrayOf(0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E),
    intArrayOf(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02), intArrayOf(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E),
    intArrayOf(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E), intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08),
    intArrayOf(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E), intArrayOf(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C),
)

/** 5x7 letters for scrolling text (upper case; lower case is shown as upper case). */
private val LETTERS: Map<Char, IntArray> = mapOf(
    'A' to intArrayOf(0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11), 'B' to intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E),
    'C' to intArrayOf(0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E), 'D' to intArrayOf(0x1C, 0x12, 0x11, 0x11, 0x11, 0x12, 0x1C),
    'E' to intArrayOf(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F), 'F' to intArrayOf(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10),
    'G' to intArrayOf(0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0F), 'H' to intArrayOf(0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11),
    'I' to intArrayOf(0x0E, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0E), 'J' to intArrayOf(0x07, 0x02, 0x02, 0x02, 0x02, 0x12, 0x0C),
    'K' to intArrayOf(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11), 'L' to intArrayOf(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F),
    'M' to intArrayOf(0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11), 'N' to intArrayOf(0x11, 0x11, 0x19, 0x15, 0x13, 0x11, 0x11),
    'O' to intArrayOf(0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E), 'P' to intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10),
    'Q' to intArrayOf(0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D), 'R' to intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11),
    'S' to intArrayOf(0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E), 'T' to intArrayOf(0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04),
    'U' to intArrayOf(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E), 'V' to intArrayOf(0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04),
    'W' to intArrayOf(0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x0A), 'X' to intArrayOf(0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11),
    'Y' to intArrayOf(0x11, 0x11, 0x11, 0x0A, 0x04, 0x04, 0x04), 'Z' to intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F),
    '.' to intArrayOf(0, 0, 0, 0, 0, 0x0C, 0x0C), ',' to intArrayOf(0, 0, 0, 0, 0x0C, 0x04, 0x08),
    '!' to intArrayOf(0x04, 0x04, 0x04, 0x04, 0x04, 0, 0x04), '?' to intArrayOf(0x0E, 0x11, 0x01, 0x02, 0x04, 0, 0x04),
    '-' to intArrayOf(0, 0, 0, 0x1F, 0, 0, 0), ':' to intArrayOf(0, 0x0C, 0x0C, 0, 0x0C, 0x0C, 0),
    '\'' to intArrayOf(0x04, 0x04, 0x08, 0, 0, 0, 0), '+' to intArrayOf(0, 0x04, 0x04, 0x1F, 0x04, 0x04, 0),
    '/' to intArrayOf(0x01, 0x01, 0x02, 0x04, 0x08, 0x10, 0x10), '&' to intArrayOf(0x0C, 0x12, 0x14, 0x08, 0x15, 0x12, 0x0D),
    '@' to intArrayOf(0x0E, 0x11, 0x17, 0x15, 0x17, 0x10, 0x0E), '#' to intArrayOf(0x0A, 0x0A, 0x1F, 0x0A, 0x1F, 0x0A, 0x0A),
    '*' to intArrayOf(0, 0x0A, 0x1F, 0x1F, 0x0E, 0x04, 0),   // a heart: the clock's separator between texts
)

private fun glyph(c: Char): IntArray? = when (c) {
    in '0'..'9' -> DIGITS[c - '0']
    else -> LETTERS[c.uppercaseChar()]
}

/** Columns of [c] that have any pixel (letters are proportional, like the clock's font). */
private fun glyphCols(rows: IntArray): IntRange {
    var first = 5; var last = -1
    for (col in 0 until 5) if (rows.any { it and (0x10 shr col) != 0 }) { first = minOf(first, col); last = col }
    return if (last < 0) 0..-1 else first..last
}

fun textWidth(s: String): Int = s.sumOf { c ->
    val g = glyph(c)
    if (g == null) 3 else glyphCols(g).count() + 1
}

fun drawText(f: Frame, x0: Int, s: String) {
    var x = x0
    for (c in s) {
        val g = glyph(c)
        if (g == null) { x += 3; continue }
        val cols = glyphCols(g)
        for (r in 0 until 7) for ((i, col) in cols.withIndex()) if (g[r] and (0x10 shr col) != 0) f[x + i, r] = true
        x += cols.count() + 1
    }
}

// ---- clock face -----------------------------------------------------------------------------

/** Clock settings that change how the face looks (keys as in the clock's config). */
data class FaceStyle(
    val fmt24: Boolean = false,
    val leadingZero: Boolean = false,
    val showSecs: Boolean = false,
    val blinkColon: Boolean = true,
    val secondsBar: Boolean = false,
    /** Roll value: 0 down, 1 up, 2 instant, 3 dissolve, 4 slide, 5 flip, 6 drop, 7 random. */
    val roll: Int = 0,
)

private const val FRAMES = 12          // digit change: 12 frames x 30 ms, like the clock
private const val FRAME_MS = 30L
private val DIGIT_X = intArrayOf(0, 6, 17, 23, 34, 40)
private val COLON_X = intArrayOf(13, 30)
private const val BLANK = 10

private fun digitPx(d: Int, r: Int, c: Int) = d in 0..9 && r in 0..6 && c in 0..4 && DIGITS[d][r] and (0x10 shr c) != 0

private fun drawDigit(f: Frame, x: Int, d: Int, ox: Int = 0, oy: Int = 0) {
    for (r in 0 until 7) for (c in 0 until 5) if (digitPx(d, r - oy, c - ox)) f[x + c, r] = true
}

private fun drawSquashed(f: Frame, x: Int, d: Int, h: Int) {
    if (h <= 0) return
    val top = 3 - h / 2
    for (r in 0 until h) for (c in 0 until 5) if (digitPx(d, r * 7 / h, c)) f[x + c, top + r] = true
}

private val BOUNCE = intArrayOf(-8, -6, -4, -2, 0, -2, -3, -2, 0, -1, 0, 0)

/** One digit cell, frame p (0..11) of the change from a to b. */
private fun drawChange(f: Frame, slot: Int, x: Int, a: Int, b: Int, p: Int, style: Int) {
    when (style) {
        1 -> { val s = p * 8 / FRAMES; drawDigit(f, x, a, 0, -s); drawDigit(f, x, b, 0, 8 - s) }
        3 -> {
            val k = p * 35 / FRAMES
            for (r in 0 until 7) for (c in 0 until 5) {
                val order = ((r * 5 + c) * 23 + slot * 11) % 35
                if (if (order < k) digitPx(b, r, c) else digitPx(a, r, c)) f[x + c, r] = true
            }
        }
        4 -> { val s = p * 6 / FRAMES; drawDigit(f, x, a, -s, 0); drawDigit(f, x, b, 6 - s, 0) }
        5 -> {
            val half = FRAMES / 2
            if (p < half) drawSquashed(f, x, a, 7 - p * 7 / half) else drawSquashed(f, x, b, (p - half + 1) * 7 / half)
        }
        6 -> { drawDigit(f, x, a, 0, p * p / 2); drawDigit(f, x, b, 0, BOUNCE[p]) }
        else -> { val s = p * 8 / FRAMES; drawDigit(f, x, a, 0, s); drawDigit(f, x, b, 0, s - 8) }
    }
}

/**
 * The clock face for a time of day given by [secondOfDay] (ms since midnight, at scene time t).
 * Digit changes animate exactly like the clock, in the chosen [FaceStyle.roll].
 */
class ClockScene(private val style: () -> FaceStyle, private val msOfDay: (Long) -> Long) : Scene {
    override val durationMs = Long.MAX_VALUE
    private val cur = IntArray(6) { -1 }
    private val prev = IntArray(6)
    private val changedAt = LongArray(6) { Long.MIN_VALUE / 2 }
    private var changeStyle = 0
    private val rnd = Random(7)

    override fun draw(f: Frame, tMs: Long) {
        val st = style()
        val ms = Math.floorMod(msOfDay(tMs), 86_400_000L)
        val sec = (ms / 1000).toInt()
        var h = sec / 3600
        if (!st.fmt24) { h %= 12; if (h == 0) h = 12 }
        val m = sec / 60 % 60
        val s = sec % 60
        val secs = st.showSecs && f.width >= 47
        val slots = if (secs) 6 else 4
        val x0 = (f.width - if (secs) 45 else 28) / 2
        val want = intArrayOf(h / 10, h % 10, m / 10, m % 10, s / 10, s % 10)
        if (want[0] == 0 && !st.leadingZero) want[0] = BLANK

        var changed = false
        for (i in 0 until slots) {
            if (want[i] == cur[i]) continue
            val first = cur[i] < 0
            prev[i] = cur[i]
            cur[i] = want[i]
            changedAt[i] = if (first || st.roll == 2) Long.MIN_VALUE / 2 else tMs
            changed = changed || !first
        }
        if (changed) changeStyle = if (st.roll == 7) intArrayOf(0, 1, 3, 4, 5, 6)[rnd.nextInt(6)] else st.roll

        for (i in 0 until slots) {
            val p = ((tMs - changedAt[i]) / FRAME_MS).toInt()
            val x = x0 + DIGIT_X[i]
            if (p in 0 until FRAMES) drawChange(f, i, x, prev[i], cur[i], p, changeStyle) else drawDigit(f, x, cur[i])
        }
        if (!st.blinkColon || ms % 1000 < 500) {
            for (k in 0 until if (secs) 2 else 1) for (c in 0..1) for (y in intArrayOf(1, 2, 4, 5)) f[x0 + COLON_X[k] + c, y] = true
        }
        if (st.secondsBar) {
            val len = (ms % 60_000 * f.width / 60_000).toInt()
            for (x in 0 until len) f[x, 7] = true
        }
    }
}

/** Shows off one digit-change style: the minutes tick over every 1.5 s. */
fun digitStyleDemo(roll: Int, fmt24: Boolean = false): Scene {
    val start = (12 * 3600 + 58 * 60) * 1000L   // 12:58 -> 12:59 -> 1:00 ...
    return ClockScene({ FaceStyle(fmt24 = fmt24, roll = roll, blinkColon = false) }) { t -> start + t / 1500 * 60_000 }
}

// ---- animations (same frames and timing as the clock) -------------------------------------------

/** Animation numbers as in the clock's settings: 1 sparkle, 2 wipe, 3 rain, 4 boxes, 5 Pac-Man. */
class AnimationScene(private val anim: Int, private val width: Int) : Scene {
    private val frameMs: Long
    private val frames: Int
    init {
        when (anim) {
            1 -> { frameMs = 28; frames = 80 }
            2 -> { frameMs = 16; frames = 2 * width }
            3 -> { frameMs = 40; frames = 110 }
            4 -> { frameMs = 30; frames = 3 * (width / 2 + 2) }
            else -> { frameMs = 55; frames = width + 11 + 16 }
        }
    }
    override val durationMs = frameMs * frames

    override fun draw(f: Frame, tMs: Long) {
        val i = (tMs / frameMs).toInt().coerceIn(0, frames - 1)
        when (anim) {
            1 -> sparkle(f, i)
            2 -> { val pass = i / width; val x = i % width; for (c in 0 until width) for (y in 0..7) f[c, y] = if (pass == 0) c <= x else c > x }
            3 -> rain(f, i)
            4 -> boxes(f, i)
            else -> pacman(f, i)
        }
    }

    // Stateful effects are replayed from a fixed seed up to frame i: cheap, and any frame can be drawn.
    private fun sparkle(f: Frame, i: Int) {
        val r = Random(11)
        for (k in 0..i) {
            if (k >= 70) { f.clear(); continue }
            repeat(7) { f[r.nextInt(width), r.nextInt(8)] = k < 40 }
        }
    }

    private fun rain(f: Frame, i: Int) {
        val r = Random(5)
        val n = 12
        val dx = IntArray(n) { r.nextInt(width) }
        val dy = IntArray(n) { -r.nextInt(14) }
        for (k in 0..i) {
            if (k == i) for (d in 0 until n) { f[dx[d], dy[d]] = true; f[dx[d], dy[d] - 1] = true; f[dx[d], dy[d] - 2] = k and 1 == 1 }
            for (d in 0 until n) {
                dy[d]++
                if (dy[d] > 10 && k < 85) { dx[d] = r.nextInt(width); dy[d] = -r.nextInt(6) }
            }
        }
    }

    private fun boxes(f: Frame, i: Int) {
        val per = width / 2 + 2
        val rr = i % per
        val cx = width / 2 - 1
        val x0 = cx - rr; val x1 = cx + 1 + rr; val y0 = 3 - rr; val y1 = 4 + rr
        for (x in x0..x1) { f[x, y0] = true; f[x, y1] = true }
        for (y in y0..y1) { f[x0, y] = true; f[x1, y] = true }
    }

    private fun pacman(f: Frame, i: Int) {
        val x = i - 8
        var d = 2
        while (d < width) { if (d > x + 3) f[d, 3] = true; d += 4 }
        sprite(f, x, if (x and 2 != 0) PAC_OPEN else PAC_CLOSED)
        sprite(f, x - 11, if (x and 2 != 0) GHOST_A else GHOST_B)
    }

    private fun sprite(f: Frame, x: Int, rows: IntArray) {
        for (r in 0 until 7) for (c in 0 until 7) if (rows[r] and (0x40 shr c) != 0) f[x + c, r] = true
    }

    private companion object {
        val PAC_OPEN = intArrayOf(0x1C, 0x3E, 0x78, 0x70, 0x78, 0x3E, 0x1C)
        val PAC_CLOSED = intArrayOf(0x1C, 0x3E, 0x7F, 0x7F, 0x7F, 0x3E, 0x1C)
        val GHOST_A = intArrayOf(0x1C, 0x3E, 0x6B, 0x7F, 0x7F, 0x7F, 0x55)
        val GHOST_B = intArrayOf(0x1C, 0x3E, 0x6B, 0x7F, 0x7F, 0x7F, 0x2A)
    }
}

/** "Random": a different animation every run. */
class RandomAnimationScene(private val width: Int) : Scene {
    override val durationMs = 1600L
    private var shown = -1L
    private var scene: Scene = AnimationScene(1, width)
    override fun draw(f: Frame, tMs: Long) {
        val run = tMs / durationMs
        if (run != shown) { shown = run; scene = AnimationScene(1 + (run % 5).toInt(), width) }
        val t = tMs % durationMs
        scene.draw(f, t * scene.durationMs / durationMs)
    }
}

fun animationScene(anim: Int, width: Int): Scene = if (anim == 6) RandomAnimationScene(width) else AnimationScene(anim, width)

/** Text scrolling right to left, then a short pause. */
class ScrollScene(private val text: String, private val width: Int, private val msPerPx: Long = 35) : Scene {
    private val textW = textWidth(text)
    override val durationMs = (width + textW) * msPerPx + 400
    override fun draw(f: Frame, tMs: Long) = drawText(f, width - (tMs / msPerPx).toInt(), text)
}

/** Static centred text (e.g. "OFF"). */
class TextScene(private val text: String) : Scene {
    override val durationMs = Long.MAX_VALUE
    override fun draw(f: Frame, tMs: Long) = drawText(f, (f.width - textWidth(text) + 1) / 2, text)
}

/** Plays [scenes] one after another, then starts over. */
class SequenceScene(private vararg val scenes: Scene) : Scene {
    override val durationMs = scenes.sumOf { it.durationMs }
    override fun draw(f: Frame, tMs: Long) {
        var t = tMs
        for (s in scenes) {
            if (t < s.durationMs) { s.draw(f, t); return }
            t -= s.durationMs
        }
    }
}

/** A finite scene, then [hold] for [holdMs]. */
class ThenHold(private val first: Scene, private val hold: Scene, private val holdMs: Long) : Scene {
    override val durationMs = first.durationMs + holdMs
    override fun draw(f: Frame, tMs: Long) = if (tMs < first.durationMs) first.draw(f, tMs) else hold.draw(f, tMs - first.durationMs)
}

/** An endless scene (like the clock) cut to [ms], so it can sit in a [SequenceScene]. */
class Timed(private val scene: Scene, private val ms: Long) : Scene {
    override val durationMs = ms
    override fun draw(f: Frame, tMs: Long) = scene.draw(f, tMs)
}

/** The clock's stopwatch face (firmware drawStopwatch): MM:SS, HH:MM after an hour, tenths on the bottom row. */
class StopwatchScene(private val ms: () -> Long, private val running: () -> Boolean) : Scene {
    override val durationMs = Long.MAX_VALUE
    override fun draw(f: Frame, tMs: Long) {
        val total = ms().coerceAtLeast(0)
        val s = total / 1000
        val h = (s / 3600).coerceAtMost(99).toInt(); val m = (s / 60 % 60).toInt(); val sec = (s % 60).toInt()
        val wide = f.width >= 47
        val d = when {
            wide -> intArrayOf(h / 10, h % 10, m / 10, m % 10, sec / 10, sec % 10)
            s < 3600 -> intArrayOf(m / 10, m % 10, sec / 10, sec % 10)
            else -> intArrayOf(h / 10, h % 10, m / 10, m % 10)
        }
        val x0 = (f.width - if (wide) 45 else 28) / 2
        val hide = !running() && total > 0 && System.currentTimeMillis() % 1000 >= 650
        if (!hide) {
            d.forEachIndexed { i, v -> drawDigit(f, x0 + DIGIT_X[i], v) }
            for (k in 0 until if (wide) 2 else 1) for (c in 0..1) for (y in intArrayOf(1, 2, 4, 5)) f[x0 + COLON_X[k] + c, y] = true
        }
        val len = if (wide || s < 3600) (total % 1000 * f.width / 1000).toInt() else sec * f.width / 60
        for (x in 0 until len) f[x, 7] = true
    }
}
