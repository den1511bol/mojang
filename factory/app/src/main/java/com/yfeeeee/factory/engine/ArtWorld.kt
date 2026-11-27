// Pixel Factory - engine: procedural art for terrain, decorations and resource nodes. Developer: @yfeeeee
package com.yfeeeee.factory.engine

import com.yfeeeee.factory.core.Deco
import com.yfeeeee.factory.core.Res
import com.yfeeeee.factory.core.T

object ArtWorld {
    private val baseCol = intArrayOf(
        rgb(24, 60, 120), rgb(52, 120, 178), rgb(224, 202, 142), rgb(96, 164, 72), rgb(62, 124, 58),
        rgb(142, 102, 66), rgb(128, 128, 136), rgb(232, 240, 250), rgb(72, 104, 62), rgb(88, 84, 96),
        rgb(62, 54, 58), rgb(230, 92, 22)
    )

    fun ground(type: Int, variant: Int, frame: Int): PB {
        val pb = PB(16, 16)
        val base = baseCol[type]
        val s = type * 31 + variant * 7
        for (y in 0 until 16) for (x in 0 until 16) {
            val n = hf(x, y, s)
            pb.set(x, y, shade(base, 0.93f + n * 0.14f))
        }
        when (type) {
            T.GRASS, T.FOREST -> {
                val dark = shade(base, 0.78f); val light = shade(base, 1.22f)
                for (i in 0 until 9) {
                    val x = (hash2(i, variant, s) and 15); val y = ((hash2(i, variant, s + 3) and 15) % 14) + 1
                    pb.set(x, y, dark); pb.set(x, y - 1, light)
                }
                if (type == T.FOREST) for (i in 0 until 5) { val x = hash2(i, variant, s + 9) and 15; val y = hash2(i, variant, s + 11) and 15; pb.set(x, y, shade(base, 0.62f)); pb.set(x + 1, y, shade(base, 0.7f)) }
            }
            T.SAND -> {
                val light = shade(base, 1.1f); val dark = shade(base, 0.88f)
                for (i in 0 until 6) { val x = hash2(i, variant, s) and 15; val y = hash2(i, variant, s + 1) and 15; pb.set(x, y, light); pb.set(x + 1, y, light); pb.set(x + 2, y + 1, dark) }
            }
            T.DIRT -> {
                for (i in 0 until 8) { val x = hash2(i, variant, s) and 15; val y = hash2(i, variant, s + 1) and 15; pb.set(x, y, shade(base, 0.7f)); if (i % 3 == 0) pb.set(x + 1, y, shade(base, 1.2f)) }
            }
            T.ROCK -> {
                val dark = shade(base, 0.72f); val light = shade(base, 1.15f)
                val x0 = 2 + (hash2(variant, 1, s) and 7); val y0 = 3 + (hash2(variant, 2, s) and 7)
                pb.line(x0, y0, x0 + 5, y0 + 2, dark); pb.line(x0 + 5, y0 + 2, x0 + 4, y0 + 7, dark)
                for (i in 0 until 6) { val x = hash2(i, variant, s + 5) and 15; val y = hash2(i, variant, s + 6) and 15; pb.set(x, y, light); pb.set(x + 1, y, light) }
            }
            T.SNOW -> {
                for (i in 0 until 7) { val x = hash2(i, variant, s) and 15; val y = hash2(i, variant, s + 1) and 15; pb.set(x, y, rgb(255, 255, 255)) }
                for (i in 0 until 6) { val x = hash2(i, variant, s + 2) and 15; val y = hash2(i, variant, s + 4) and 15; pb.set(x, y, rgb(196, 214, 236)); pb.set(x + 1, y, rgb(206, 222, 240)) }
            }
            T.SWAMP -> {
                for (i in 0 until 4) { val x = hash2(i, variant, s) and 11; val y = hash2(i, variant, s + 1) and 11; pb.rect(x + 1, y + 1, 4, 3, rgb(52, 76, 60)); pb.rect(x + 2, y + 1, 2, 1, rgb(70, 96, 90)) }
                for (i in 0 until 5) { val x = hash2(i, variant, s + 3) and 15; val y = hash2(i, variant, s + 4) and 15; pb.set(x, y, rgb(110, 150, 70)) }
            }
            T.CLIFF -> {
                for (y in 0 until 16) for (x in 0 until 16) { val v = if (y % 5 == 4) 0.7f else if ((x + (y / 5) * 4) % 8 == 0) 0.8f else 1f; pb.set(x, y, shade(pb.get(x, y), v)) }
                for (x in 0 until 16) { pb.set(x, 0, shade(base, 1.3f)); pb.set(x, 1, shade(base, 1.15f)) }
            }
            T.ASH -> {
                for (i in 0 until 6) { val x = hash2(i, variant, s) and 15; val y = hash2(i, variant, s + 1) and 15; pb.set(x, y, rgb(40, 34, 38)) }
                if (variant % 2 == 0) { val x = 3 + (hash2(variant, 5, s) and 7); val y = 3 + (hash2(variant, 6, s) and 7); pb.line(x, y, x + 3, y + 2, rgb(200, 70, 20)); pb.set(x + 1, y, rgb(255, 150, 40)) }
            }
            T.LAVA -> {
                for (y in 0 until 16) for (x in 0 until 16) {
                    val w = Math.sin(((x + frame * 4) * 0.5 + y * 0.35 + variant)) * 0.5 + 0.5
                    pb.set(x, y, mix(rgb(210, 60, 16), rgb(255, 190, 50), w.toFloat() * 0.8f))
                }
                for (i in 0 until 3) { val x = (hash2(i, variant, s) and 15); val y = (hash2(i, variant + frame, s) and 15); pb.rect(x, y, 3, 2, rgb(90, 30, 20)) }
            }
            T.DEEP, T.SHALLOW -> {
                val light = if (type == T.DEEP) rgb(50, 100, 170) else rgb(150, 210, 240)
                for (i in 0 until 3) {
                    val y = ((hash2(i, variant, s) and 15) + frame * 1) % 16; val x = hash2(i, variant, s + 1) and 7
                    pb.line(x, y, x + 4, y, light)
                }
                if (type == T.SHALLOW) for (i in 0 until 4) { val x = hash2(i, variant, s + 2) and 15; val y = hash2(i, variant, s + 3) and 15; pb.set(x, y, rgb(200, 190, 140)) }
            }
        }
        return pb
    }

    private val dkOutline = rgb(26, 30, 22)

    fun tree(variant: Int): PB {
        val pb = PB(16, 24)
        val trunk = rgb(104, 70, 40)
        pb.rect(6, 14, 4, 9, trunk); pb.rect(6, 14, 1, 9, shade(trunk, 1.2f)); pb.rect(9, 14, 1, 9, shade(trunk, 0.7f))
        val g1 = if (variant % 2 == 0) rgb(44, 112, 52) else rgb(60, 124, 44)
        pb.disc(8f, 9f, 7.5f, shade(g1, 0.8f))
        pb.disc(7f, 8f, 6f, g1)
        pb.disc(9f, 6.5f, 4.2f, shade(g1, 1.25f))
        pb.disc(6f, 5.5f, 2.2f, shade(g1, 1.45f))
        for (i in 0 until 6) { pb.set(3 + (hash2(i, variant, 3) and 9), 6 + (hash2(i, variant, 4) and 7), shade(g1, 0.62f)) }
        pb.outlineShape(dkOutline)
        return pb
    }

    fun pine(): PB {
        val pb = PB(16, 24)
        pb.rect(7, 18, 2, 5, rgb(96, 64, 40))
        val g = rgb(36, 96, 66)
        for (k in 0 until 4) {
            val top = 2 + k * 4; val half = 3 + k * 1
            for (y in 0 until 7) { val hw = (half * (y + 1)) / 7; pb.rect(8 - hw, top + y, hw * 2, 1, shade(g, 0.8f + 0.1f * k + (if (y > 4) -0.15f else 0f))) }
            pb.rect(8 - half + 1, top + 5, 2, 1, rgb(240, 248, 255))
        }
        pb.rect(7, 1, 2, 2, rgb(52, 120, 82))
        pb.outlineShape(dkOutline)
        return pb
    }

    fun deadTree(): PB {
        val pb = PB(16, 24)
        val c = rgb(86, 70, 60)
        pb.rect(7, 8, 2, 15, c); pb.line(8, 12, 3, 6, c); pb.line(8, 10, 13, 4, c); pb.line(8, 15, 12, 12, c); pb.line(4, 7, 4, 4, c)
        pb.rect(7, 8, 1, 15, shade(c, 1.25f))
        pb.outlineShape(rgb(30, 26, 24))
        return pb
    }

    fun cactus(): PB {
        val pb = PB(16, 20)
        val g = rgb(70, 150, 74)
        pb.rect(6, 4, 4, 15, g); pb.rect(2, 8, 2, 5, g); pb.rect(3, 12, 3, 2, g); pb.rect(12, 6, 2, 5, g); pb.rect(10, 10, 3, 2, g)
        pb.rect(6, 4, 1, 15, shade(g, 1.3f)); pb.rect(9, 4, 1, 15, shade(g, 0.75f))
        pb.set(7, 2, rgb(240, 120, 160)); pb.set(8, 3, rgb(240, 120, 160)); pb.set(7, 3, rgb(250, 180, 200))
        pb.outlineShape(rgb(20, 50, 28))
        return pb
    }

    fun boulder(variant: Int, tone: Int): PB {
        val pb = PB(16, 16)
        val base = if (tone == 0) rgb(138, 138, 146) else if (tone == 1) rgb(176, 166, 138) else rgb(70, 60, 72)
        pb.disc(8f, 9.5f, 6.2f, shade(base, 0.8f))
        pb.disc(7.4f, 8.6f, 5.2f, base)
        pb.disc(6.6f, 7f, 3f, shade(base, 1.2f))
        pb.disc(5.8f, 6.2f, 1.4f, shade(base, 1.4f))
        pb.line(9, 8, 11, 12, shade(base, 0.6f))
        pb.outlineShape(rgb(30, 30, 36))
        return pb
    }

    fun bush(berries: Boolean): PB {
        val pb = PB(16, 16)
        val g = rgb(58, 132, 58)
        pb.disc(5f, 10f, 4.4f, shade(g, 0.85f)); pb.disc(10.5f, 10f, 4.6f, shade(g, 0.9f)); pb.disc(8f, 8f, 5.2f, g)
        pb.disc(7f, 6.5f, 2.6f, shade(g, 1.3f))
        if (berries) { val r = rgb(220, 50, 90); pb.set(5, 9, r); pb.set(10, 8, r); pb.set(8, 11, r); pb.set(12, 11, r); pb.set(4, 11, r); pb.set(9, 6, r) }
        pb.outlineShape(rgb(24, 52, 28))
        return pb
    }

    fun flower(variant: Int): PB {
        val pb = PB(16, 16)
        val cols = intArrayOf(rgb(250, 230, 80), rgb(240, 100, 140), rgb(250, 250, 250), rgb(150, 130, 240))
        val c = cols[variant % 4]
        for (i in 0 until 3) {
            val x = 2 + (hash2(i, variant, 9) % 11 + 11) % 11; val y = 3 + (hash2(i, variant, 10) % 10 + 10) % 10
            pb.set(x, y, c); pb.set(x, y + 1, rgb(60, 130, 50)); pb.set(x + 1, y, shade(c, 0.85f)); pb.set(x - 1, y, shade(c, 0.85f)); pb.set(x, y - 1, shade(c, 1.1f))
        }
        return pb
    }

    fun tuft(variant: Int): PB {
        val pb = PB(16, 16)
        val g = rgb(70, 140, 56)
        for (i in 0 until 5) {
            val x = 3 + (hash2(i, variant, 20) % 10 + 10) % 10; val y = 6 + (hash2(i, variant, 21) % 8 + 8) % 8
            pb.set(x, y, g); pb.set(x, y - 1, shade(g, 1.3f)); pb.set(x + 1, y - 2, shade(g, 1.2f))
        }
        return pb
    }

    private val nodeCol = intArrayOf(
        0, rgb(168, 110, 90), rgb(214, 120, 62), rgb(226, 220, 196), rgb(40, 40, 48), rgb(232, 200, 60),
        rgb(240, 150, 210), rgb(236, 226, 74), rgb(190, 84, 60), rgb(60, 40, 80), rgb(210, 220, 230)
    )

    fun nodeColor(type: Int): Int = nodeCol[type]

    fun node(type: Int, purity: Int, frame: Int): PB {
        val pb = PB(48, 48)
        val c = nodeCol[type]
        val rock = rgb(92, 90, 98)
        if (type == Res.OIL) {
            pb.disc(24f, 24f, 20f, rgb(40, 34, 30)); pb.disc(24f, 24f, 17f, rgb(22, 16, 30))
            for (i in 0 until 4) { val x = 12 + (hash2(i, frame, 3) and 23); val y = 12 + (hash2(i, frame, 4) and 23); pb.disc(x.toFloat(), y.toFloat(), 3f, rgb(90, 60, 130)); pb.set(x - 1, y - 1, rgb(190, 150, 230)) }
        } else if (type == Res.GEYSER) {
            pb.disc(24f, 24f, 20f, rgb(60, 54, 56)); pb.disc(24f, 24f, 15f, rgb(36, 30, 32)); pb.disc(24f, 24f, 9f, rgb(210, 90, 30))
            pb.disc(24f, 24f, 5f, rgb(255, 190, 70))
        } else {
            pb.disc(24f, 25f, 20f, shade(rock, 0.7f)); pb.disc(24f, 24f, 19f, rock); pb.disc(24f, 24f, 15f, shade(rock, 0.85f))
            val cnt = 4 + purity * 3
            for (i in 0 until cnt) {
                val a = hf(i, 7, type) * 6.283f; val r = 3f + hf(i, 8, type) * 11f
                val x = 24f + Math.cos(a.toDouble()).toFloat() * r; val y = 24f + Math.sin(a.toDouble()).toFloat() * r
                val sz = 2.4f + hf(i, 9, type) * 2f
                pb.disc(x, y, sz, shade(c, 0.7f)); pb.disc(x - 0.5f, y - 0.5f, sz - 0.9f, c); pb.set(x.toInt() - 1, y.toInt() - 1, shade(c, 1.45f))
            }
        }
        // purity ring
        val ringC = if (purity == 0) rgb(150, 150, 150) else if (purity == 1) rgb(90, 200, 120) else rgb(90, 200, 255)
        if (frame % 2 == 0 || purity < 2) pb.ring(24f, 24f, 22f, 1.2f, withAlpha(ringC, 255))
        pb.outlineShape(rgb(24, 22, 28))
        return pb
    }

    fun pod(opened: Boolean): PB {
        val pb = PB(16, 16)
        val metal = rgb(120, 136, 150)
        pb.disc(8f, 9f, 7f, shade(metal, 0.7f)); pb.disc(8f, 8.5f, 6f, metal)
        if (!opened) {
            pb.disc(7f, 7f, 3.5f, shade(metal, 1.25f)); pb.rect(6, 6, 4, 4, rgb(220, 50, 50)); pb.set(6, 6, rgb(255, 150, 140))
            pb.line(3, 9, 12, 9, shade(metal, 0.6f))
        } else {
            pb.disc(8f, 9f, 4.5f, rgb(30, 34, 40)); pb.rect(3, 3, 8, 2, shade(metal, 1.1f)); pb.line(3, 3, 1, 1, shade(metal, 1.1f))
        }
        pb.outlineShape(rgb(24, 26, 32))
        return pb
    }
}
