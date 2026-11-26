// Developer: @yfeeeee
package com.yfeeeee.mahjong

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Все звуки синтезируются в коде (аудиофайлы не нужны):
 * стук плиток, колокольчики, дребезг перемешивания, фанфары победы.
 */
class Sfx {
    var enabled = true

    private val rate = 22050
    private val bank = HashMap<String, ShortArray>()
    private val handler = Handler(Looper.getMainLooper())
    private var active = 0

    init {
        bank["tick"] = tick()
        bank["select"] = select()
        bank["thud"] = thud()
        bank["clack"] = clack()
        bank["match"] = match()
        bank["error"] = error()
        bank["shuffle"] = shuffle()
        bank["hint"] = hint()
        bank["win"] = win()
    }

    fun play(name: String, volume: Float = 1f, pitch: Float = 1f) {
        if (!enabled || active >= 12) return
        val data = bank[name] ?: return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(data, 0, data.size)
            track.setVolume(volume.coerceIn(0f, 1f))
            track.playbackRate = (rate * pitch).toInt()
            track.play()
            active++
            val ms = (data.size * 1000L / (rate * pitch)).toLong() + 150L
            handler.postDelayed({
                try {
                    track.release()
                } catch (_: Exception) {
                }
                active--
            }, ms)
        } catch (_: Exception) {
        }
    }

    // ---------- синтез ----------

    private fun buf(sec: Double) = DoubleArray((rate * sec).toInt())

    /** Колокольный тон: основная частота + негармонические обертоны. */
    private fun addTone(b: DoubleArray, startSec: Double, freq: Double, amp: Double, decay: Double, bell: Boolean = true) {
        val s0 = (startSec * rate).toInt()
        var i = 0
        while (s0 + i < b.size) {
            val t = i.toDouble() / rate
            val env = exp(-decay * t) * min(1.0, t * 500.0)
            var v = sin(2 * PI * freq * t)
            if (bell) {
                v += 0.40 * sin(2 * PI * freq * 2.76 * t) * exp(-decay * 0.8 * t)
                v += 0.18 * sin(2 * PI * freq * 5.40 * t) * exp(-decay * 1.6 * t)
            }
            b[s0 + i] += amp * env * v
            i++
            if (t > 0.02 && env < 0.0004) break
        }
    }

    /** Шумовой щелчок с фильтром (a ближе к 1 - ярче, ближе к 0 - глуше). */
    private fun addNoise(b: DoubleArray, startSec: Double, amp: Double, decay: Double, a: Double, rnd: Random) {
        val s0 = (startSec * rate).toInt()
        var lp = 0.0
        var i = 0
        while (s0 + i < b.size) {
            val t = i.toDouble() / rate
            val env = exp(-decay * t) * min(1.0, t * 800.0)
            val r = rnd.nextDouble() * 2 - 1
            lp += a * (r - lp)
            b[s0 + i] += amp * env * lp
            i++
            if (t > 0.01 && env < 0.0004) break
        }
    }

    private fun shorts(b: DoubleArray, gain: Double = 1.0): ShortArray =
        ShortArray(b.size) { (tanh(b[it] * gain) * 32000).toInt().toShort() }

    private fun tick(): ShortArray {
        val b = buf(0.07)
        addTone(b, 0.0, 1500.0, 0.5, 90.0, false)
        addNoise(b, 0.0, 0.3, 140.0, 0.6, Random(1))
        return shorts(b)
    }

    private fun select(): ShortArray {
        val b = buf(0.3)
        addTone(b, 0.0, 1046.5, 0.5, 14.0)
        addTone(b, 0.0, 1568.0, 0.25, 18.0)
        return shorts(b)
    }

    private fun thud(): ShortArray {
        val b = buf(0.14)
        addTone(b, 0.0, 150.0, 0.8, 35.0, false)
        addNoise(b, 0.0, 0.25, 70.0, 0.15, Random(2))
        return shorts(b)
    }

    /** Деревянный "клак" двух плиток: двойной удар. */
    private fun clack(): ShortArray {
        val b = buf(0.34)
        val rnd = Random(3)
        addNoise(b, 0.0, 1.0, 75.0, 0.55, rnd)
        addTone(b, 0.0, 210.0, 0.8, 30.0, false)
        addTone(b, 0.0, 1480.0, 0.35, 70.0, false)
        addNoise(b, 0.038, 0.8, 85.0, 0.7, rnd)
        addTone(b, 0.038, 2350.0, 0.25, 90.0, false)
        addTone(b, 0.038, 320.0, 0.5, 40.0, false)
        return shorts(b, 1.3)
    }

    /** Приятное арпеджио "собрал пару". */
    private fun match(): ShortArray {
        val b = buf(1.0)
        addTone(b, 0.00, 1046.5, 0.38, 7.0)
        addTone(b, 0.07, 1318.5, 0.36, 7.0)
        addTone(b, 0.14, 1568.0, 0.34, 6.5)
        addTone(b, 0.21, 2093.0, 0.30, 6.0)
        addTone(b, 0.21, 3136.0, 0.10, 9.0, false)
        return shorts(b)
    }

    private fun error(): ShortArray {
        val b = buf(0.24)
        for (i in b.indices) {
            val t = i.toDouble() / rate
            val f = 150.0 - 60.0 * t
            val sq = if (sin(2 * PI * f * t) > 0) 1.0 else -1.0
            b[i] = 0.28 * sq * exp(-10.0 * t) * min(1.0, t * 400.0)
        }
        return shorts(b)
    }

    /** Дребезжание костяшек. */
    private fun shuffle(): ShortArray {
        val b = buf(0.75)
        val rnd = Random(4)
        for (k in 0 until 18) {
            val at = k * 0.034 + rnd.nextDouble() * 0.02
            addNoise(b, at, 0.45, 150.0, 0.5, rnd)
            addTone(b, at, 500.0 + rnd.nextDouble() * 900.0, 0.15, 120.0, false)
        }
        return shorts(b)
    }

    private fun hint(): ShortArray {
        val b = buf(0.6)
        addTone(b, 0.0, 1760.0, 0.4, 8.0)
        addTone(b, 0.10, 2349.0, 0.36, 8.0)
        return shorts(b)
    }

    private fun win(): ShortArray {
        val b = buf(2.4)
        val notes = doubleArrayOf(523.3, 659.3, 784.0, 1046.5, 1318.5, 1568.0)
        for (k in notes.indices) addTone(b, k * 0.13, notes[k], 0.34, 3.5)
        for (f in doubleArrayOf(523.3, 659.3, 784.0, 1046.5)) addTone(b, 0.95, f, 0.26, 1.8)
        addTone(b, 0.95, 2093.0, 0.12, 3.0, false)
        return shorts(b)
    }
}
