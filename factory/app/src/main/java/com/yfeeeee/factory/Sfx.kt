// Pixel Factory - synthesized sound effects (no audio files, no libraries). Developer: @yfeeeee
package com.yfeeeee.factory

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.Random

class Sfx {
    private val rate = 22050
    private val tracks = HashMap<String, AudioTrack>()
    var enabled = true

    private fun gen(name: String): ShortArray {
        val rnd = Random(name.hashCode().toLong())
        val parts = ArrayList<FloatArray>()
        fun tone(freq: Float, freq2: Float, dur: Float, vol: Float, noise: Float): FloatArray {
            val n = (dur * rate).toInt()
            val out = FloatArray(n)
            var ph = 0.0
            for (i in 0 until n) {
                val t = i / n.toFloat()
                val f = freq + (freq2 - freq) * t
                ph += 2.0 * Math.PI * f / rate
                val env = (1f - t) * (1f - t)
                val sq = if (Math.sin(ph) >= 0.0) 1f else -1f
                val v = (sq * 0.5f + Math.sin(ph).toFloat() * 0.5f) * (1f - noise) + (rnd.nextFloat() * 2f - 1f) * noise
                out[i] = v * env * vol
            }
            return out
        }
        when (name) {
            "click" -> parts.add(tone(700f, 900f, 0.05f, 0.5f, 0f))
            "place" -> { parts.add(tone(220f, 120f, 0.09f, 0.7f, 0.15f)) }
            "mine" -> parts.add(tone(160f, 90f, 0.07f, 0.6f, 0.7f))
            "hit" -> { parts.add(tone(300f, 90f, 0.10f, 0.7f, 0.5f)) }
            "hurt" -> parts.add(tone(420f, 110f, 0.22f, 0.7f, 0.2f))
            "deny" -> parts.add(tone(140f, 120f, 0.12f, 0.6f, 0.1f))
            "good" -> { parts.add(tone(520f, 520f, 0.08f, 0.5f, 0f)); parts.add(tone(660f, 660f, 0.08f, 0.5f, 0f)); parts.add(tone(880f, 880f, 0.16f, 0.5f, 0f)) }
            "toast" -> { parts.add(tone(600f, 800f, 0.07f, 0.35f, 0f)) }
            "pod" -> { parts.add(tone(400f, 900f, 0.12f, 0.5f, 0f)); parts.add(tone(900f, 1200f, 0.12f, 0.4f, 0f)) }
            else -> parts.add(tone(500f, 500f, 0.05f, 0.4f, 0f))
        }
        var total = 0
        for (p in parts) total += p.size
        val res = ShortArray(total)
        var pos = 0
        for (p in parts) {
            for (i in p.indices) {
                var v = p[i] * 0.55f
                if (v > 1f) v = 1f
                if (v < -1f) v = -1f
                res[pos++] = (v * 32000f).toInt().toShort()
            }
        }
        return res
    }

    private fun make(name: String): AudioTrack? {
        return try {
            val data = gen(name)
            @Suppress("DEPRECATION")
            val t = AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, data.size * 2, AudioTrack.MODE_STATIC)
            t.write(data, 0, data.size)
            t
        } catch (e: Throwable) {
            null
        }
    }

    fun init() {
        for (n in arrayOf("click", "place", "mine", "hit", "hurt", "deny", "good", "toast", "pod")) {
            val t = make(n)
            if (t != null) tracks[n] = t
        }
    }

    fun play(name: String) {
        if (!enabled) return
        val t = tracks[name] ?: return
        try {
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: Throwable) {
        }
    }

    fun release() {
        for (t in tracks.values) {
            try { t.release() } catch (e: Throwable) { }
        }
        tracks.clear()
    }
}
