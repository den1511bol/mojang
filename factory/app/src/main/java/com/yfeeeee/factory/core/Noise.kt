// Pixel Factory - core: noise and helpers. Developer: @yfeeeee
package com.yfeeeee.factory.core

object Noise {
    fun hash(x: Int, y: Int, seed: Int): Int {
        var h = x * 374761393 + y * 668265263 + seed * 1442695041.toInt()
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return h
    }

    fun rnd(x: Int, y: Int, seed: Int): Float = (hash(x, y, seed) and 0xFFFFFF) / 16777216f

    private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

    fun value(x: Float, y: Float, seed: Int): Float {
        val x0 = Math.floor(x.toDouble()).toInt()
        val y0 = Math.floor(y.toDouble()).toInt()
        val fx = smooth(x - x0)
        val fy = smooth(y - y0)
        val a = rnd(x0, y0, seed)
        val b = rnd(x0 + 1, y0, seed)
        val c = rnd(x0, y0 + 1, seed)
        val d = rnd(x0 + 1, y0 + 1, seed)
        val top = a + (b - a) * fx
        val bot = c + (d - c) * fx
        return top + (bot - top) * fy
    }

    fun fbm(x: Float, y: Float, seed: Int, oct: Int): Float {
        var amp = 0.5f
        var freq = 1f
        var sum = 0f
        var norm = 0f
        for (i in 0 until oct) {
            sum += value(x * freq, y * freq, seed + i * 101) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2f
        }
        return sum / norm
    }
}

const val DX_STR = "1,0,-1,0"
val DX = intArrayOf(1, 0, -1, 0)
val DY = intArrayOf(0, 1, 0, -1)

fun clampF(v: Float, lo: Float, hi: Float): Float = if (v < lo) lo else if (v > hi) hi else v
fun clampI(v: Int, lo: Int, hi: Int): Int = if (v < lo) lo else if (v > hi) hi else v
