// Pixel Factory - UI toolkit: immediate-mode drawing + button registry. Developer: @yfeeeee
package com.yfeeeee.factory

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.yfeeeee.factory.core.Item
import com.yfeeeee.factory.engine.Sprites
import com.yfeeeee.factory.engine.rgb
import com.yfeeeee.factory.engine.shade

class Btn(val l: Float, val t: Float, val r: Float, val b: Float, val hold: Boolean, val act: () -> Unit) {
    fun hit(x: Float, y: Float): Boolean = x >= l && x <= r && y >= t && y <= b
}

object Col {
    val panel = rgb(22, 28, 40)
    val panel2 = rgb(32, 40, 56)
    val border = rgb(96, 118, 150)
    val btn = rgb(50, 68, 98)
    val btnOn = rgb(70, 120, 90)
    val btnOff = rgb(40, 44, 54)
    val danger = rgb(150, 56, 56)
    val accent = rgb(250, 200, 60)
    val text = rgb(235, 238, 245)
    val dim = rgb(140, 150, 168)
    val bad = rgb(255, 110, 100)
    val good = rgb(130, 235, 150)
    val bar = rgb(14, 18, 26)
}

class Ui(val sp: Sprites) {
    var W = 1
    var H = 1
    var u = 3f
    val btns = ArrayList<Btn>()
    var scroll = 0f
    var scrollMax = 0f
    var scrollL = 0f
    var scrollT = 0f
    var scrollR = 0f
    var scrollB = 0f
    var hasScroll = false
    private var clipT = -1e9f
    private var clipB = 1e9f

    private val fill = Paint()
    private val tp = Paint()
    private val bmp = Paint()
    private val rf = RectF()

    init {
        tp.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        tp.isAntiAlias = false
        bmp.isFilterBitmap = false
        fill.isAntiAlias = false
    }

    fun resize(w: Int, h: Int) {
        W = w; H = h
        u = Math.max(1.6f, h / 360f)
    }

    fun begin() {
        btns.clear()
        hasScroll = false
        clipT = -1e9f; clipB = 1e9f
    }

    // ---------- drawing ----------
    fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, col: Int) {
        fill.color = col; fill.style = Paint.Style.FILL
        c.drawRect(l, t, r, b, fill)
    }

    fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, bg: Int, border: Int) {
        val bw = Math.max(2f, u * 0.8f)
        rect(c, l, t, r, b, border)
        rect(c, l + bw, t + bw, r - bw, b - bw, bg)
        rect(c, l + bw, t + bw, r - bw, t + bw + Math.max(1f, u * 0.4f), shade(bg, 1.25f))
    }

    fun tw(s: String, size: Float): Float {
        tp.textSize = size * u
        return tp.measureText(s)
    }

    /** align: 0 left, 1 center, 2 right. y is the top of the text line. */
    fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, align: Int = 0) {
        tp.textSize = size * u
        val w = tp.measureText(s)
        val px = if (align == 1) x - w / 2f else if (align == 2) x - w else x
        tp.color = rgb(0, 0, 0)
        c.drawText(s, px + Math.max(1f, u * 0.4f), y + size * u * 0.85f + Math.max(1f, u * 0.4f), tp)
        tp.color = col
        c.drawText(s, px, y + size * u * 0.85f, tp)
    }

    fun fit(s: String, maxW: Float, size: Float): String {
        if (tw(s, size) <= maxW) return s
        var n = s.length
        while (n > 1 && tw(s.substring(0, n) + "..", size) > maxW) n--
        return s.substring(0, n) + ".."
    }

    fun textFit(c: Canvas, s: String, x: Float, y: Float, maxW: Float, size: Float, col: Int, align: Int = 0) {
        text(c, fit(s, maxW, size), x, y, size, col, align)
    }

    /** wraps text into lines that fit maxW */
    fun wrap(s: String, maxW: Float, size: Float): List<String> {
        val out = ArrayList<String>()
        for (para in s.split("\n")) {
            var cur = ""
            for (word in para.split(" ")) {
                val t = if (cur.isEmpty()) word else "$cur $word"
                if (tw(t, size) <= maxW || cur.isEmpty()) cur = t else { out.add(cur); cur = word }
            }
            out.add(cur)
        }
        return out
    }

    fun bitmap(c: Canvas, b: Bitmap, l: Float, t: Float, w: Float, h: Float) {
        rf.set(Math.round(l).toFloat(), Math.round(t).toFloat(), Math.round(l + w).toFloat(), Math.round(t + h).toFloat())
        c.drawBitmap(b, null, rf, bmp)
    }

    /** draw bitmap keeping aspect ratio inside the box */
    fun bitmapFit(c: Canvas, b: Bitmap, l: Float, t: Float, w: Float, h: Float) {
        val k = Math.min(w / b.width, h / b.height)
        val dw = b.width * k; val dh = b.height * k
        bitmap(c, b, l + (w - dw) / 2f, t + (h - dh) / 2f, dw, dh)
    }

    fun icon(c: Canvas, name: String, l: Float, t: Float, size: Float) {
        bitmap(c, sp.icon(name), l, t, size, size)
    }

    fun item(c: Canvas, it: Item, l: Float, t: Float, size: Float) {
        bitmap(c, sp.item(it), l, t, size, size)
    }

    fun bar(c: Canvas, l: Float, t: Float, r: Float, b: Float, frac: Float, col: Int) {
        rect(c, l, t, r, b, rgb(8, 10, 16))
        val k = Math.max(0f, Math.min(1f, frac))
        val bw = Math.max(1f, u * 0.6f)
        if (k > 0f) rect(c, l + bw, t + bw, l + bw + (r - l - 2 * bw) * k, b - bw, col)
    }

    /** item chip: icon + count; returns the width used */
    fun chip(c: Canvas, it: Item, n: Int, x: Float, y: Float, size: Float, ok: Boolean): Float {
        return chipText(c, it, n.toString(), x, y, size, ok)
    }

    fun chipText(c: Canvas, it: Item, label: String, x: Float, y: Float, size: Float, ok: Boolean): Float {
        val s = size * u
        item(c, it, x, y, s)
        text(c, label, x + s + u, y + (s - size * u * 0.8f) / 2f, size * 0.8f, if (ok) Col.text else Col.bad)
        return s + u + tw(label, size * 0.8f) + u * 3f
    }

    // ---------- buttons ----------
    private fun reg(l: Float, t: Float, r: Float, b: Float, hold: Boolean, act: () -> Unit) {
        val tt = Math.max(t, clipT); val bb = Math.min(b, clipB)
        if (bb <= tt) return
        btns.add(Btn(l, tt, r, bb, hold, act))
    }

    fun button(c: Canvas, l: Float, t: Float, r: Float, b: Float, label: String, bg: Int, enabled: Boolean = true, size: Float = 9f, act: () -> Unit) {
        val fillc = if (enabled) bg else Col.btnOff
        box(c, l, t, r, b, fillc, if (enabled) Col.border else rgb(60, 66, 80))
        val ty = t + ((b - t) - size * u) / 2f
        textFit(c, label, (l + r) / 2f, ty, r - l - u * 2f, size, if (enabled) Col.text else Col.dim, 1)
        if (enabled) reg(l, t, r, b, false, act)
    }

    fun iconButton(c: Canvas, name: String, l: Float, t: Float, size: Float, bg: Int, on: Boolean = false, act: () -> Unit) {
        box(c, l, t, l + size, t + size, if (on) Col.btnOn else bg, if (on) Col.good else Col.border)
        val s = size * 0.62f
        icon(c, name, l + (size - s) / 2f, t + (size - s) / 2f, s)
        reg(l, t, l + size, t + size, false, act)
    }

    fun holdButton(c: Canvas, name: String, l: Float, t: Float, size: Float, bg: Int, act: () -> Unit) {
        box(c, l, t, l + size, t + size, bg, Col.border)
        val s = size * 0.62f
        icon(c, name, l + (size - s) / 2f, t + (size - s) / 2f, s)
        reg(l, t, l + size, t + size, true, act)
    }

    /** invisible click area */
    fun area(l: Float, t: Float, r: Float, b: Float, act: () -> Unit) {
        reg(l, t, r, b, false, act)
    }

    // ---------- scroll ----------
    fun beginScroll(c: Canvas, l: Float, t: Float, r: Float, b: Float, contentH: Float): Float {
        scrollMax = Math.max(0f, contentH - (b - t))
        if (scroll < 0f) scroll = 0f
        if (scroll > scrollMax) scroll = scrollMax
        scrollL = l; scrollT = t; scrollR = r; scrollB = b
        hasScroll = true
        c.save()
        c.clipRect(l, t, r, b)
        clipT = t; clipB = b
        return t - scroll
    }

    fun endScroll(c: Canvas) {
        c.restore()
        clipT = -1e9f; clipB = 1e9f
        // scroll bar
        if (scrollMax > 0f) {
            val h = scrollB - scrollT
            val total = h + scrollMax
            val bh = Math.max(12f * u, h * h / total)
            val by = scrollT + (h - bh) * (scroll / scrollMax)
            rect(c, scrollR - 3f * u, by, scrollR - 1f * u, by + bh, rgb(140, 160, 190))
        }
    }
}
