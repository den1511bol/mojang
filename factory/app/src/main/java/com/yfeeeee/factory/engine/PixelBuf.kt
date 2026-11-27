// Pixel Factory - engine: pixel buffer and colour helpers. Developer: @yfeeeee
package com.yfeeeee.factory.engine

import android.graphics.Bitmap

fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)
fun argb(a: Int, r: Int, g: Int, b: Int): Int = ((a and 255) shl 24) or ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)
fun cr(c: Int): Int = (c shr 16) and 255
fun cg(c: Int): Int = (c shr 8) and 255
fun cb(c: Int): Int = c and 255
fun ca(c: Int): Int = (c ushr 24) and 255

fun shade(c: Int, f: Float): Int {
    var r = (cr(c) * f).toInt(); var g = (cg(c) * f).toInt(); var b = (cb(c) * f).toInt()
    if (r > 255) r = 255; if (g > 255) g = 255; if (b > 255) b = 255
    if (r < 0) r = 0; if (g < 0) g = 0; if (b < 0) b = 0
    return (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
}

fun mix(a: Int, b: Int, t: Float): Int {
    val r = (cr(a) + (cr(b) - cr(a)) * t).toInt()
    val g = (cg(a) + (cg(b) - cg(a)) * t).toInt()
    val bl = (cb(a) + (cb(b) - cb(a)) * t).toInt()
    return rgb(r, g, bl)
}

fun withAlpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or ((a and 255) shl 24)

fun hash2(x: Int, y: Int, s: Int): Int {
    var h = x * 374761393 + y * 668265263 + s * 144269504
    h = (h xor (h ushr 13)) * 1274126177
    return h xor (h ushr 16)
}

fun hf(x: Int, y: Int, s: Int): Float = (hash2(x, y, s) and 0xFFFF) / 65536f

class PB(val w: Int, val h: Int) {
    val p = IntArray(w * h)

    fun set(x: Int, y: Int, c: Int) { if (x >= 0 && y >= 0 && x < w && y < h) p[y * w + x] = c }
    fun get(x: Int, y: Int): Int = if (x >= 0 && y >= 0 && x < w && y < h) p[y * w + x] else 0
    fun fill(c: Int) { java.util.Arrays.fill(p, c) }

    fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (yy in y until y + rh) for (xx in x until x + rw) set(xx, yy, c)
    }

    fun frame(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (xx in x until x + rw) { set(xx, y, c); set(xx, y + rh - 1, c) }
        for (yy in y until y + rh) { set(x, yy, c); set(x + rw - 1, yy, c) }
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        var x = x0; var y = y0
        val dx = Math.abs(x1 - x0); val dy = -Math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            set(x, y, c)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    fun disc(cx: Float, cy: Float, r: Float, c: Int) {
        val x0 = Math.floor((cx - r).toDouble()).toInt(); val x1 = Math.ceil((cx + r).toDouble()).toInt()
        val y0 = Math.floor((cy - r).toDouble()).toInt(); val y1 = Math.ceil((cy + r).toDouble()).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = x + 0.5f - cx; val dy = y + 0.5f - cy
            if (dx * dx + dy * dy <= r * r) set(x, y, c)
        }
    }

    fun ring(cx: Float, cy: Float, r: Float, thick: Float, c: Int) {
        val x0 = Math.floor((cx - r).toDouble()).toInt(); val x1 = Math.ceil((cx + r).toDouble()).toInt()
        val y0 = Math.floor((cy - r).toDouble()).toInt(); val y1 = Math.ceil((cy + r).toDouble()).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = x + 0.5f - cx; val dy = y + 0.5f - cy
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (d <= r && d >= r - thick) set(x, y, c)
        }
    }

    /** body with bevel: light top-left, dark bottom-right, 1px dark outline */
    fun bevel(x: Int, y: Int, rw: Int, rh: Int, base: Int, outline: Int) {
        rect(x, y, rw, rh, base)
        for (xx in x until x + rw) { set(xx, y + 1, shade(base, 1.18f)); set(xx, y + rh - 2, shade(base, 0.78f)) }
        for (yy in y until y + rh) { set(x + 1, yy, shade(base, 1.12f)); set(x + rw - 2, yy, shade(base, 0.82f)) }
        frame(x, y, rw, rh, outline)
    }

    fun noise(x: Int, y: Int, rw: Int, rh: Int, seed: Int, amt: Float) {
        for (yy in y until y + rh) for (xx in x until x + rw) {
            val c = get(xx, yy)
            if (ca(c) == 0) continue
            val f = 1f + (hf(xx, yy, seed) - 0.5f) * amt
            set(xx, yy, shade(c, f))
        }
    }

    /** darken one-pixel outline around opaque shape (for sprites with transparency) */
    fun outlineShape(col: Int) {
        val copy = p.copyOf()
        for (y in 0 until h) for (x in 0 until w) {
            if (ca(copy[y * w + x]) != 0) continue
            var near = false
            if (x > 0 && ca(copy[y * w + x - 1]) != 0) near = true
            if (x < w - 1 && ca(copy[y * w + x + 1]) != 0) near = true
            if (y > 0 && ca(copy[(y - 1) * w + x]) != 0) near = true
            if (y < h - 1 && ca(copy[(y + 1) * w + x]) != 0) near = true
            if (near) p[y * w + x] = col
        }
    }

    fun blit(src: PB, dx: Int, dy: Int) {
        for (y in 0 until src.h) for (x in 0 until src.w) {
            val c = src.p[y * src.w + x]
            if (ca(c) != 0) set(dx + x, dy + y, c)
        }
    }

    fun toBitmap(): Bitmap = Bitmap.createBitmap(p, w, h, Bitmap.Config.ARGB_8888)
}
