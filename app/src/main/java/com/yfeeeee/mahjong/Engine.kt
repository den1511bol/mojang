// Developer: @yfeeeee
package com.yfeeeee.mahjong

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val DROP_MS = 620f
private const val BACK_MS = 140f
private const val HIT_MS = 120f
private const val FLIP_MS = 520f
private const val PIF = 3.14159265f

private val GOLD = Color(0xFFFFD54F)
private val C_RED = Color(0xFFC62828)
private val C_GREEN = Color(0xFF2E7D32)
private val C_BLUE = Color(0xFF1A3A8F)
private val NUMS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
private val WINDS = listOf("東", "南", "西", "北")
private val ROW_LAYOUTS = listOf(
    listOf(1), listOf(1, 1), listOf(1, 1, 1), listOf(2, 2), listOf(2, 1, 2),
    listOf(2, 2, 2), listOf(1, 2, 2, 2), listOf(2, 2, 2, 2), listOf(3, 3, 3),
)
private val CONFETTI = listOf(
    Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFFFFFF),
)

private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

private fun easeInCubic(t: Float) = t * t * t

private fun bounce(t: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f
    return when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> { val u = t - 1.5f / d1; n1 * u * u + 0.75f }
        t < 2.5f / d1 -> { val u = t - 2.25f / d1; n1 * u * u + 0.9375f }
        else -> { val u = t - 2.625f / d1; n1 * u * u + 0.984375f }
    }
}

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var rot: Float, val vrot: Float, val w: Float, val h: Float,
    val color: Color, val maxLife: Float, val kind: Int, val gravity: Float, val drag: Float,
) {
    var life = maxLife
}

class Clash(val a: Int, val b: Int, val start: Long, val points: Int, val combo: Int)
class Ring(val x: Float, val y: Float, val start: Long, val maxR: Float, val color: Color, val thick: Float)
class FText(val x: Float, val y: Float, val text: String, val start: Long, val color: Color, val size: Float)

/**
 * Игровой движок: логика + анимации + отрисовка на одном Canvas.
 * Анимации: падение плиток с отскоком, подъём выбранной, дрожь при ошибке,
 * столкновение пары с замахом и ударом, осколки, искры, пыль, кольца,
 * переворот при перемешивании, конфетти и салют при победе.
 */
class Engine(
    val level: Int,
    attempt: Int,
    startScore: Int,
    private val sfx: Sfx,
    private val haptic: (Boolean) -> Unit,
) {
    private val data = generateLevel(level, attempt)
    val ps: List<Pos> = data.positions
    val n = ps.size
    private val types = IntArray(n) { data.types[it] }
    private val removed = BooleanArray(n)
    private val freeNow = BooleanArray(n)
    private val wasFree = BooleanArray(n)

    // состояние для интерфейса
    var frame by mutableLongStateOf(0L)
    var left by mutableIntStateOf(n)
    var score by mutableIntStateOf(startScore)
    var combo by mutableIntStateOf(0)
    var won by mutableStateOf(false)
    var stuck by mutableStateOf(false)
    var assists = 0

    private val rnd = Random(System.nanoTime())
    private val drawOrder = ps.indices.sortedWith(compareBy({ ps[it].z }, { ps[it].y }, { ps[it].x }))
    private val hitOrder = drawOrder.reversed()

    // анимационные состояния плиток
    private val delayMs = FloatArray(n)
    private val dropK = FloatArray(n) { 0.6f + rnd.nextFloat() * 0.5f }
    private val rotFrom = FloatArray(n) { (rnd.nextFloat() - 0.5f) * 30f }
    private val landed = BooleanArray(n)
    private val lift = FloatArray(n)
    private val dim = FloatArray(n) { 1f }
    private val shakeAt = LongArray(n) { -10000L }
    private val shakeRed = BooleanArray(n)
    private val pulseAt = LongArray(n) { -10000L }
    private val flipAt = LongArray(n) { -10000L }
    private val swapped = BooleanArray(n) { true }
    private val pendingTypes = IntArray(n)

    init {
        val step = max(7f, min(22f, 1000f / n))
        drawOrder.forEachIndexed { r, i -> delayMs[i] = 150f + r * step }
        recomputeFree()
        for (i in 0 until n) {
            wasFree[i] = freeNow[i]
            dim[i] = if (freeNow[i]) 0f else 1f
        }
        stuck = findPair() == null
    }

    private val introEnd = delayMs.max() + DROP_MS

    private var t0 = -1L
    private var lastMs = 0L
    private var now = 0L

    private var selected = -1
    private var hintA = -1
    private var hintB = -1
    private var hintUntil = 0L
    private var busyUntil = 0L
    private var lastMatchAt = -100000L
    private var lastTickAt = 0L
    private var winPending = false
    private var winAt = 0L
    private var winFxUntil = 0L
    private var nextFirework = 0L
    private var shakeMag = 0f
    private var shakeX = 0f
    private var shakeY = 0f

    private val clashes = ArrayList<Clash>()
    private val parts = ArrayList<Particle>()
    private val rings = ArrayList<Ring>()
    private val texts = ArrayList<FText>()
    private val pose = FloatArray(4)

    // геометрия (обновляется при отрисовке)
    private var tw = 0f
    private var th = 0f
    private var ox = 0f
    private var oy = 0f
    private var shift = 0f
    private var viewW = 1f
    private var viewH = 1f
    private var dp = 1f

    // фон
    private val starX = FloatArray(70) { rnd.nextFloat() }
    private val starY = FloatArray(70) { rnd.nextFloat() }
    private val starS = FloatArray(70) { 0.6f + rnd.nextFloat() * 1.6f }
    private val starP = FloatArray(70) { rnd.nextFloat() * 6.28f }
    private val orbX = FloatArray(16) { rnd.nextFloat() }
    private val orbSpeed = FloatArray(16) { 0.012f + rnd.nextFloat() * 0.02f }
    private val orbPhase = FloatArray(16) { rnd.nextFloat() }
    private val orbR = FloatArray(16) { 6f + rnd.nextFloat() * 14f }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // ---------------- логика ----------------

    private fun cxOf(i: Int) = ox + tw * (ps[i].x / 2f) - shift * ps[i].z + tw / 2f
    private fun cyOf(i: Int) = oy + th * (ps[i].y / 2f) - shift * ps[i].z + th / 2f

    private fun recomputeFree() {
        for (i in 0 until n) freeNow[i] = !removed[i] && isFree(i, ps) { j -> !removed[j] }
    }

    private fun findPair(): Pair<Int, Int>? {
        for (a in 0 until n) {
            if (!freeNow[a]) continue
            for (b in a + 1 until n) {
                if (freeNow[b] && types[a] == types[b]) return a to b
            }
        }
        return null
    }

    private fun hit(px: Float, py: Float): Int {
        val hw = tw * 0.47f
        val hh = th * 0.47f
        for (i in hitOrder) {
            if (removed[i]) continue
            if (abs(px - cxOf(i)) <= hw && abs(py - cyOf(i)) <= hh + th * 0.04f) return i
        }
        return -1
    }

    fun tap(px: Float, py: Float) {
        if (won || winPending || now < introEnd || now < busyUntil) return
        val i = hit(px, py)
        if (i < 0) {
            selected = -1
            return
        }
        if (!freeNow[i]) {
            shakeAt[i] = now
            shakeRed[i] = false
            sfx.play("thud", 0.7f)
            haptic(false)
            return
        }
        when {
            selected == i -> {
                selected = -1
                sfx.play("select", 0.4f, 0.8f)
            }
            selected == -1 -> {
                selected = i
                hintA = -1
                hintB = -1
                sfx.play("select", 0.7f)
                haptic(false)
            }
            types[selected] == types[i] -> match(selected, i)
            else -> {
                shakeAt[selected] = now
                shakeRed[selected] = true
                shakeAt[i] = now
                shakeRed[i] = true
                sfx.play("error", 0.6f)
                haptic(true)
                combo = 0
                selected = -1
            }
        }
    }

    private fun match(a: Int, b: Int) {
        removed[a] = true
        removed[b] = true
        selected = -1
        hintA = -1
        hintB = -1
        val c = if (combo > 0 && now - lastMatchAt < 6000L) combo + 1 else 1
        combo = c
        lastMatchAt = now
        clashes.add(Clash(a, b, now, 100 + 50 * (c - 1), c))
        left -= 2
        recomputeFree()
        for (i in 0 until n) {
            if (freeNow[i] && !wasFree[i]) pulseAt[i] = now + 260L
            wasFree[i] = freeNow[i]
        }
        stuck = left > 0 && findPair() == null
        if (left == 0) {
            winPending = true
            winAt = now + 950L
        }
        haptic(false)
    }

    fun hint() {
        if (won || winPending || now < introEnd || now < busyUntil) return
        val p = findPair() ?: return
        hintA = p.first
        hintB = p.second
        hintUntil = now + 3500L
        assists++
        selected = -1
        sfx.play("hint", 0.8f)
    }

    fun shuffle() {
        if (won || winPending || now < introEnd || now < busyUntil) return
        val alive = drawOrder.filter { !removed[it] }
        if (alive.isEmpty()) return
        val m = assignTypes(ps, alive, typeCountFor(level), Random(System.nanoTime())) ?: return
        assists++
        selected = -1
        hintA = -1
        hintB = -1
        alive.forEachIndexed { r, i ->
            pendingTypes[i] = m.getValue(i)
            swapped[i] = false
            flipAt[i] = now + r * 9L
        }
        busyUntil = now + FLIP_MS.toLong() + alive.size * 9L
        stuck = false
        sfx.play("shuffle", 0.9f)
        haptic(false)
    }

    // ---------------- обновление ----------------

    fun tick(timeMs: Long) {
        if (t0 < 0L) {
            t0 = timeMs
            lastMs = timeMs
        }
        now = timeMs - t0
        val dtMs = (timeMs - lastMs).coerceIn(0L, 40L)
        lastMs = timeMs
        if (tw > 0f) update(dtMs)
        frame = timeMs
    }

    private fun spawn(
        kind: Int, x: Float, y: Float, vx: Float, vy: Float, w: Float, h: Float,
        color: Color, life: Float, gravity: Float, drag: Float = 0f, rot: Float = 0f, vrot: Float = 0f,
    ) {
        parts.add(Particle(x, y, vx, vy, rot, vrot, w, h, color, life, kind, gravity, drag))
    }

    private fun update(dtMs: Long) {
        val dt = dtMs / 1000f
        val kLift = 1f - exp(-dtMs / 70f)
        val kDim = 1f - exp(-dtMs / 120f)

        for (i in 0 until n) {
            if (removed[i]) continue
            lift[i] += ((if (selected == i) 1f else 0f) - lift[i]) * kLift
            dim[i] += ((if (freeNow[i]) 0f else 1f) - dim[i]) * kDim
            if (!landed[i] && now >= delayMs[i] + 226f) {
                landed[i] = true
                landingFx(i)
            }
            val fa = flipAt[i]
            if (fa > -1000L) {
                val ft = (now - fa) / FLIP_MS
                if (ft >= 0.5f && !swapped[i]) {
                    types[i] = pendingTypes[i]
                    swapped[i] = true
                }
                if (ft > 1f) flipAt[i] = -10000L
            }
        }

        var k = clashes.size - 1
        while (k >= 0) {
            val c = clashes[k]
            if (now - c.start >= (BACK_MS + HIT_MS).toLong()) {
                impact(c)
                clashes.removeAt(k)
            }
            k--
        }

        if (winPending && now >= winAt) {
            winPending = false
            won = true
            winFxUntil = now + 4200L
            nextFirework = now
            sfx.play("win", 1f)
            haptic(true)
        }
        if (now < winFxUntil) {
            repeat(2) {
                spawn(
                    3, rnd.nextFloat() * viewW, -20f * dp,
                    (rnd.nextFloat() - 0.5f) * 160f * dp, (160f + rnd.nextFloat() * 220f) * dp,
                    8f * dp, 14f * dp, CONFETTI[rnd.nextInt(CONFETTI.size)],
                    4f + rnd.nextFloat() * 2f, 90f * dp, 0.2f, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 900f,
                )
            }
            if (now >= nextFirework) {
                nextFirework = now + 380L + rnd.nextInt(300)
                firework(viewW * (0.15f + rnd.nextFloat() * 0.7f), viewH * (0.15f + rnd.nextFloat() * 0.4f))
            }
        }

        k = parts.size - 1
        while (k >= 0) {
            val p = parts[k]
            p.life -= dt
            if (p.life <= 0f || p.y > viewH + 200f * dp) {
                parts.removeAt(k)
            } else {
                p.vy += p.gravity * dt
                if (p.drag > 0f) {
                    val f = exp(-p.drag * dt)
                    p.vx *= f
                    p.vy *= f
                }
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.rot += p.vrot * dt
            }
            k--
        }
        rings.removeAll { now - it.start > 520L }
        texts.removeAll { now - it.start > 1000L }

        shakeMag *= exp(-dtMs / 110f)
        shakeX = sin(now * 0.09f) * shakeMag
        shakeY = cos(now * 0.11f) * shakeMag
        if (combo > 0 && now - lastMatchAt > 6000L) combo = 0
    }

    private fun landingFx(i: Int) {
        if (now - lastTickAt >= 40L) {
            lastTickAt = now
            sfx.play("tick", 0.35f, 0.9f + rnd.nextFloat() * 0.4f)
        }
        val x = cxOf(i)
        val y = cyOf(i) + th * 0.45f
        repeat(2) {
            spawn(
                2, x + (rnd.nextFloat() - 0.5f) * tw * 0.6f, y,
                (rnd.nextFloat() - 0.5f) * 90f * dp, -20f * dp, 7f * dp, 0f,
                Color(0xFFD7F5E8), 0.5f, 0f, 3f,
            )
        }
    }

    private fun firework(x: Float, y: Float) {
        val col = CONFETTI[rnd.nextInt(CONFETTI.size - 1)]
        rings.add(Ring(x, y, now, 90f * dp, col, 5f * dp))
        repeat(36) {
            val a = rnd.nextFloat() * 2f * PIF
            val s = (150f + rnd.nextFloat() * 380f) * dp
            spawn(
                1, x, y, cos(a) * s, sin(a) * s, (2.5f + rnd.nextFloat() * 2.5f) * dp, 0f,
                if (rnd.nextInt(3) == 0) Color.White else col, 0.8f + rnd.nextFloat() * 0.6f, 260f * dp, 1.8f,
            )
        }
    }

    /** Поза плитки пары в момент t: замах назад, затем удар навстречу. */
    private fun clashPose(c: Clash, tMs: Float, side: Int) {
        val ax = cxOf(c.a)
        val ay = cyOf(c.a)
        val bx = cxOf(c.b)
        val by = cyOf(c.b)
        var ux = bx - ax
        var uy = by - ay
        val d = max(1f, hypot(ux, uy))
        ux /= d
        uy /= d
        val sgn = if (side == 0) -1f else 1f
        val sx0 = if (side == 0) ax else bx
        val sy0 = if (side == 0) ay else by
        val mx = (ax + bx) / 2f
        val my = (ay + by) / 2f
        val bw = tw * 0.94f
        val back = bw * 0.30f
        val bx0 = sx0 + ux * sgn * back
        val by0 = sy0 + uy * sgn * back
        val hx = mx + ux * sgn * bw * 0.47f
        val hy = my + uy * sgn * bw * 0.47f
        if (tMs < BACK_MS) {
            val f = easeOutCubic(tMs / BACK_MS)
            pose[0] = sx0 + (bx0 - sx0) * f
            pose[1] = sy0 + (by0 - sy0) * f
            pose[2] = sgn * -7f * f
            pose[3] = 1f + 0.10f * f
        } else {
            val g = easeInCubic(((tMs - BACK_MS) / HIT_MS).coerceIn(0f, 1f))
            pose[0] = bx0 + (hx - bx0) * g
            pose[1] = by0 + (hy - by0) * g
            pose[2] = sgn * -7f * (1f - g)
            pose[3] = 1.10f
        }
    }

    private fun impact(c: Clash) {
        val mx = (cxOf(c.a) + cxOf(c.b)) / 2f
        val my = (cyOf(c.a) + cyOf(c.b)) / 2f
        val bw = tw * 0.94f
        val bh = th * 0.94f
        val shardColors = listOf(Color(0xFFFFF6DD), Color(0xFFFFF6DD), Color(0xFFF1E8CF), Color(0xFF2E9E62))

        for (side in 0..1) {
            val idx = if (side == 0) c.a else c.b
            clashPose(c, BACK_MS + HIT_MS, side)
            val cx = pose[0]
            val cy = pose[1]
            val accent = accentOf(types[idx])
            for (r in 0 until 4) for (q in 0 until 3) {
                val sx = cx + (q - 1) * bw / 3f
                val sy = cy + (r - 1.5f) * bh / 4f
                var dx = sx - mx
                var dy = sy - my
                val len = max(1f, hypot(dx, dy))
                dx /= len
                dy /= len
                val v = (240f + rnd.nextFloat() * 360f) * dp
                spawn(
                    0, sx, sy,
                    dx * v * 0.75f + (rnd.nextFloat() - 0.5f) * 220f * dp,
                    dy * v * 0.75f - (180f + rnd.nextFloat() * 320f) * dp,
                    bw / 3f * 0.95f, bh / 4f * 0.95f,
                    if (rnd.nextInt(5) == 0) accent else shardColors[rnd.nextInt(shardColors.size)],
                    0.9f + rnd.nextFloat() * 0.6f, 1700f * dp, 0.3f,
                    rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 1100f,
                )
            }
        }
        val sparkColors = listOf(GOLD, Color.White, Color(0xFF9CFFD8), Color(0xFFFFB74D))
        repeat(24) {
            val a = rnd.nextFloat() * 2f * PIF
            val s = (200f + rnd.nextFloat() * 520f) * dp
            spawn(
                1, mx, my, cos(a) * s, sin(a) * s, (2.5f + rnd.nextFloat() * 3f) * dp, 0f,
                sparkColors[rnd.nextInt(sparkColors.size)], 0.5f + rnd.nextFloat() * 0.5f, 600f * dp, 2.4f,
            )
        }
        repeat(10) {
            val a = rnd.nextFloat() * 2f * PIF
            val s = (40f + rnd.nextFloat() * 120f) * dp
            spawn(
                2, mx, my, cos(a) * s, sin(a) * s - 30f * dp, (10f + rnd.nextFloat() * 8f) * dp, 0f,
                Color(0xFFE9F7EF), 0.6f + rnd.nextFloat() * 0.4f, 0f, 3f,
            )
        }
        rings.add(Ring(mx, my, now, bw * 1.1f, GOLD, 6f * dp))
        rings.add(Ring(mx, my, now, bw * 0.7f, Color.White, 3f * dp))
        shakeMag = max(shakeMag, 7f * dp)

        score += c.points
        texts.add(FText(mx, my - bh * 0.35f, "+${c.points}", now, GOLD, 30f * dp))
        if (c.combo >= 2) {
            texts.add(FText(mx, my - bh * 0.80f, "КОМБО ×${c.combo}", Color(0xFFFF9E40), now, 22f * dp))
        }
        sfx.play("clack", 1f, 0.95f + rnd.nextFloat() * 0.12f)
        sfx.play("match", 0.75f, min(1.6f, 1f + 0.07f * (c.combo - 1)))
        haptic(true)
    }

    private fun accentOf(type: Int): Color = when {
        type < 9 -> Color(0xFF1565C0)
        type < 18 -> C_GREEN
        type < 27 -> C_RED
        type < 31 -> C_BLUE
        type == 31 -> C_RED
        type == 32 -> C_GREEN
        else -> Color(0xFF1565C0)
    }

    // ---------------- отрисовка ----------------

    fun render(ds: DrawScope, topPad: Float, botPad: Float) {
        with(ds) {
            viewW = size.width
            viewH = size.height
            dp = density
            val availH = size.height - topPad - botPad
            tw = min(size.width / (COLS + 0.5f), availH / (ROWS * 1.3f + 0.4f))
            th = tw * 1.3f
            shift = tw * 0.07f
            ox = (size.width - tw * COLS) / 2f
            oy = topPad + (availH - th * ROWS) / 2f

            drawBackground()
            translate(shakeX, shakeY) {
                drawBoardPlate()
                for (i in drawOrder) {
                    if (!removed[i]) drawTileAt(i)
                }
                drawClashes()
                drawParticles()
                drawRings()
                drawTexts()
            }
        }
    }

    private fun DrawScope.drawBackground() {
        val w = size.width
        val h = size.height
        val t = now / 1000f
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF041C1A), Color(0xFF0B3B38), Color(0xFF0A2A33))),
        )
        val glowColors = listOf(Color(0xFF1DE9B6), Color(0xFFFFD54F), Color(0xFF26C6DA))
        for (k in 0 until 3) {
            val gx = w * (0.2f + 0.3f * k) + sin(t * 0.15f + k * 2f) * w * 0.12f
            val gy = h * (0.22f + 0.28f * k) + cos(t * 0.12f + k) * h * 0.04f
            val r = w * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glowColors[k].copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(gx, gy), radius = r,
                ),
                radius = r, center = Offset(gx, gy),
            )
        }
        for (i in starX.indices) {
            val tw2 = 0.5f + 0.5f * sin(t * 1.6f + starP[i])
            drawCircle(
                Color.White.copy(alpha = 0.15f + 0.65f * tw2),
                radius = starS[i] * dp,
                center = Offset(starX[i] * w, starY[i] * h),
            )
        }
        for (i in orbX.indices) {
            val prog = (t * orbSpeed[i] + orbPhase[i]) % 1f
            val x = orbX[i] * w + sin(t * 0.6f + i) * 18f * dp
            val y = h * (1.05f - prog * 1.1f)
            val a = sin(prog * PIF) * 0.22f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFE082).copy(alpha = a), Color.Transparent),
                    center = Offset(x, y), radius = orbR[i] * dp * 2.2f,
                ),
                radius = orbR[i] * dp * 2.2f, center = Offset(x, y),
            )
        }
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0x73000000)),
                center = Offset(w / 2f, h / 2f), radius = hypot(w, h) * 0.6f,
            ),
        )
    }

    private fun DrawScope.drawBoardPlate() {
        val x0 = ox - tw * 0.22f
        val y0 = oy - th * 0.10f
        val pw = tw * COLS + tw * 0.44f
        val ph = th * ROWS + th * 0.20f
        drawRoundRect(
            color = Color(0x26000000), topLeft = Offset(x0, y0), size = Size(pw, ph),
            cornerRadius = CornerRadius(26f * dp),
        )
        drawRoundRect(
            color = GOLD.copy(alpha = 0.14f), topLeft = Offset(x0, y0), size = Size(pw, ph),
            cornerRadius = CornerRadius(26f * dp), style = Stroke(width = 1.5f * dp),
        )
    }

    private fun DrawScope.drawTileAt(i: Int) {
        var offX = 0f
        var offY = 0f
        var sc = 1f
        var rot = 0f
        var alpha = 1f
        var sx = 1f

        val ti = (now - delayMs[i]) / DROP_MS
        if (ti <= 0f) return
        if (ti < 1f) {
            val e = bounce(ti)
            offY = -(1f - e) * viewH * 0.5f * dropK[i]
            rot = (1f - e) * rotFrom[i]
            alpha = min(1f, ti * 5f)
            sc = 0.85f + 0.15f * min(1f, ti * 3f)
        }
        offY -= lift[i] * tw * 0.14f
        sc *= 1f + lift[i] * 0.07f

        val pt = (now - pulseAt[i]) / 260f
        if (pt in 0f..1f) sc *= 1f + 0.08f * sin(pt * PIF)

        var red = 0f
        val st = now - shakeAt[i]
        if (st in 0..380L) {
            val f = 1f - st / 380f
            offX += sin(st * 0.05f) * tw * 0.07f * f
            if (shakeRed[i]) red = f
        }
        val fa = flipAt[i]
        if (fa > -1000L) {
            val ft = (now - fa) / FLIP_MS
            if (ft in 0f..1f) {
                sx = max(0.03f, abs(cos(ft * PIF)))
                sc *= 1f + 0.08f * sin(ft * PIF)
                offY -= sin(ft * PIF) * tw * 0.12f
            }
        }
        var glow = lift[i] * 0.9f
        if (now < hintUntil && (i == hintA || i == hintB)) {
            glow = max(glow, 0.55f + 0.45f * sin(now * 0.009f))
        }
        drawTile(
            cxOf(i) + offX, cyOf(i) + offY, tw * 0.94f, th * 0.94f,
            sc, sx, rot, alpha, dim[i], glow, red, types[i], 0.28f,
        )
    }

    private fun DrawScope.drawClashes() {
        for (c in clashes) {
            val tMs = (now - c.start).toFloat()
            for (side in 0..1) {
                val idx = if (side == 0) c.a else c.b
                clashPose(c, tMs, side)
                val px = pose[0]
                val py = pose[1]
                val pr = pose[2]
                val ps2 = pose[3]
                if (tMs > BACK_MS) {
                    // шлейф движения при ударе
                    for (g in 1..2) {
                        clashPose(c, max(BACK_MS, tMs - g * 22f), side)
                        drawTile(
                            pose[0], pose[1], tw * 0.94f, th * 0.94f, pose[3], 1f, pose[2],
                            0.22f / g, 0f, 0f, 0f, types[idx], 0f,
                        )
                    }
                }
                drawTile(px, py, tw * 0.94f, th * 0.94f, ps2, 1f, pr, 1f, 0f, 0.8f, 0f, types[idx], 0.28f)
            }
        }
    }

    private fun DrawScope.drawParticles() {
        for (p in parts) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            when (p.kind) {
                0 -> withTransform({
                    translate(p.x, p.y)
                    rotate(p.rot, Offset.Zero)
                }) {
                    drawRoundRect(
                        color = p.color, topLeft = Offset(-p.w / 2f, -p.h / 2f), size = Size(p.w, p.h),
                        cornerRadius = CornerRadius(p.w * 0.18f), alpha = min(1f, a * 2.2f),
                    )
                }
                1 -> {
                    val c = Offset(p.x, p.y)
                    drawCircle(p.color.copy(alpha = a * 0.30f), radius = p.w * 2.4f * (0.5f + a * 0.5f), center = c)
                    drawCircle(p.color.copy(alpha = a), radius = p.w * (0.4f + 0.6f * a), center = c)
                }
                2 -> drawCircle(
                    p.color.copy(alpha = a * 0.35f), radius = p.w * (1.7f - a * 0.7f), center = Offset(p.x, p.y),
                )
                else -> withTransform({
                    translate(p.x, p.y)
                    rotate(p.rot, Offset.Zero)
                    scale(1f, max(0.15f, abs(cos(p.rot * 0.04f))), Offset.Zero)
                }) {
                    drawRect(
                        color = p.color, topLeft = Offset(-p.w / 2f, -p.h / 2f), size = Size(p.w, p.h),
                        alpha = min(1f, a * 3f),
                    )
                }
            }
        }
    }

    private fun DrawScope.drawRings() {
        for (r in rings) {
            val p = ((now - r.start) / 520f).coerceIn(0f, 1f)
            drawCircle(
                r.color.copy(alpha = (1f - p) * 0.9f),
                radius = r.maxR * easeOutCubic(p),
                center = Offset(r.x, r.y),
                style = Stroke(width = max(1f, r.thick * (1f - p))),
            )
        }
    }

    private fun DrawScope.drawTexts() {
        val cv = drawContext.canvas.nativeCanvas
        for (t in texts) {
            val p = ((now - t.start) / 1000f).coerceIn(0f, 1f)
            val pop = 1f + 0.35f * sin(min(1f, p * 4f) * PIF)
            val a = (1f - p * p).coerceIn(0f, 1f)
            val x = t.x
            val y = t.y - p * 60f * dp
            uiPaint.textSize = t.size * pop
            uiPaint.color = Color.Black.copy(alpha = a * 0.55f).toArgb()
            cv.drawText(t.text, x + 2f * dp, y + 2f * dp, uiPaint)
            uiPaint.color = t.color.copy(alpha = a).toArgb()
            cv.drawText(t.text, x, y, uiPaint)
        }
    }

    private fun DrawScope.glyphText(s: String, cx: Float, cy: Float, size: Float, color: Color) {
        textPaint.textSize = size
        textPaint.color = color.toArgb()
        drawContext.canvas.nativeCanvas.drawText(s, cx, cy + size * 0.35f, textPaint)
    }

    private fun DrawScope.drawTile(
        cx: Float, cy: Float, bw: Float, bh: Float,
        sc: Float, sx: Float, rot: Float, alpha: Float,
        dimV: Float, glow: Float, red: Float, type: Int, shadowA: Float,
    ) {
        withTransform({
            translate(cx, cy)
            rotate(rot, Offset.Zero)
            scale(sc * sx, sc, Offset.Zero)
        }) {
            val r = bw * 0.14f
            val depth = bw * 0.10f
            val half = Offset(-bw / 2f, -bh / 2f)

            if (glow > 0.01f) {
                val e = 6f * dp * glow
                drawRoundRect(
                    color = GOLD.copy(alpha = 0.28f * glow * alpha),
                    topLeft = Offset(half.x - e, half.y - e), size = Size(bw + 2 * e, bh + 2 * e),
                    cornerRadius = CornerRadius(r + e),
                )
                drawRoundRect(
                    color = GOLD.copy(alpha = min(1f, glow) * alpha),
                    topLeft = Offset(half.x - 1f, half.y - 1f), size = Size(bw + 2f, bh + 2f),
                    cornerRadius = CornerRadius(r + 1f), style = Stroke(width = 3f * dp),
                )
            }
            if (shadowA > 0f) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = shadowA * alpha),
                    topLeft = Offset(half.x + depth * 0.5f, half.y + depth * 1.5f), size = Size(bw, bh),
                    cornerRadius = CornerRadius(r),
                )
            }
            // зелёная "толщина" плитки
            drawRoundRect(
                color = lerp(Color(0xFF1F8A55), Color(0xFF3F5F4F), dimV).copy(alpha = alpha),
                topLeft = Offset(half.x, half.y + depth), size = Size(bw, bh - depth),
                cornerRadius = CornerRadius(r),
            )
            // лицевая сторона
            val top = lerp(Color(0xFFFFFFF8), Color(0xFFB9BDB5), dimV)
            val bot = lerp(Color(0xFFF1E8CF), Color(0xFF9EA39A), dimV)
            val faceH = bh - depth
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(top, bot), startY = -bh / 2f, endY = -bh / 2f + faceH),
                topLeft = half, size = Size(bw, faceH), cornerRadius = CornerRadius(r), alpha = alpha,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f * alpha * (1f - dimV)),
                topLeft = Offset(half.x + 1.5f, half.y + 1.5f), size = Size(bw - 3f, faceH - 3f),
                cornerRadius = CornerRadius(r), style = Stroke(width = 1.5f * dp),
            )
            // символ
            translate(0f, -depth / 2f) {
                drawGlyph(type, bw * 0.70f, faceH * 0.76f, alpha)
            }
            if (dimV > 0.02f) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.18f * dimV * alpha),
                    topLeft = half, size = Size(bw, faceH), cornerRadius = CornerRadius(r),
                )
            }
            if (red > 0.01f) {
                drawRoundRect(
                    color = Color(0xFFFF1744).copy(alpha = 0.45f * red * alpha),
                    topLeft = half, size = Size(bw, faceH), cornerRadius = CornerRadius(r),
                )
            }
        }
    }

    private fun DrawScope.drawGlyph(type: Int, gw: Float, gh: Float, a: Float) {
        when {
            type < 18 -> drawGrid(type % 9 + 1, type >= 9, gw, gh, a)
            type < 27 -> {
                glyphText(NUMS[type - 18], 0f, -gh * 0.22f, gw * 0.64f, C_BLUE.copy(alpha = a))
                glyphText("萬", 0f, gh * 0.22f, gw * 0.64f, C_RED.copy(alpha = a))
            }
            type < 31 -> glyphText(WINDS[type - 27], 0f, 0f, gw * 1.05f, C_BLUE.copy(alpha = a))
            type == 31 -> glyphText("中", 0f, 0f, gw * 1.05f, C_RED.copy(alpha = a))
            type == 32 -> glyphText("發", 0f, 0f, gw * 1.05f, C_GREEN.copy(alpha = a))
            else -> {
                drawRoundRect(
                    color = Color(0xFF1565C0).copy(alpha = a),
                    topLeft = Offset(-gw * 0.42f, -gh * 0.42f), size = Size(gw * 0.84f, gh * 0.84f),
                    cornerRadius = CornerRadius(gw * 0.1f), style = Stroke(width = gw * 0.08f),
                )
            }
        }
    }

    private fun DrawScope.drawGrid(n: Int, bamboo: Boolean, gw: Float, gh: Float, a: Float) {
        val rows = ROW_LAYOUTS[n - 1]
        val rh = gh / rows.size
        val palette = listOf(C_GREEN, C_RED, Color(0xFF1565C0))
        rows.forEachIndexed { ri, cnt ->
            val cw = gw / cnt
            for (ci in 0 until cnt) {
                val cx = -gw / 2f + cw * (ci + 0.5f)
                val cy = -gh / 2f + rh * (ri + 0.5f)
                val color = palette[(ri + ci) % 3].copy(alpha = a)
                if (bamboo) {
                    val bw = min(cw * 0.42f, rh * 0.32f)
                    val bh = rh * 0.86f
                    drawRoundRect(
                        color = if (n == 1) C_RED.copy(alpha = a) else color,
                        topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh),
                        cornerRadius = CornerRadius(bw / 2f),
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.55f * a),
                        topLeft = Offset(cx - bw / 2f, cy - bw * 0.1f), size = Size(bw, bw * 0.2f),
                    )
                } else {
                    val rr = min(cw, rh) * 0.42f
                    drawCircle(color, radius = rr, center = Offset(cx, cy), style = Stroke(width = rr * 0.34f))
                    drawCircle(color, radius = rr * 0.30f, center = Offset(cx, cy))
                }
            }
        }
    }
}
