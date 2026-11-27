// Pixel Factory - engine: characters, creatures, UI icons. Developer: @yfeeeee
package com.yfeeeee.factory.engine

object ArtChars {
    private val outl = rgb(22, 20, 26)

    /** player 16x20, dir 0 right,1 down,2 left,3 up ; frame 0..3 */
    fun player(dir: Int, frame: Int): PB {
        if (dir == 2) return mirror(player(0, frame))
        val pb = PB(16, 20)
        val skin = rgb(240, 200, 160); val suit = rgb(232, 124, 40); val suitD = rgb(190, 92, 28)
        val hat = rgb(250, 214, 60); val hatD = rgb(214, 170, 30); val pants = rgb(60, 72, 96); val boots = rgb(52, 40, 36)
        val legUp = if (frame == 1 || frame == 3) 1 else 0
        val lOff = if (frame == 1) 1 else 0; val rOff = if (frame == 3) 1 else 0
        when (dir) {
            1 -> {
                pb.rect(3, 13, 3, 5 - lOff, pants); pb.rect(8, 13, 3, 5 - rOff, pants)
                pb.rect(3, 18 - lOff, 3, 1, boots); pb.rect(8, 18 - rOff, 3, 1, boots)
                pb.rect(2, 8, 10, 6, suit); pb.rect(2, 12, 10, 1, suitD); pb.rect(6, 8, 2, 5, suitD)
                pb.rect(0, 9, 2, 5, suit); pb.rect(12, 9, 2, 5, suit); pb.rect(0, 13, 2, 2, skin); pb.rect(12, 13, 2, 2, skin)
                pb.rect(3, 3, 8, 6, skin); pb.rect(4, 5, 1, 2, outl); pb.rect(9, 5, 1, 2, outl)
                pb.rect(2, 1, 10, 3, hat); pb.rect(2, 3, 10, 1, hatD); pb.rect(6, 0, 2, 2, hat); pb.rect(6, 2, 2, 1, rgb(255, 250, 200))
            }
            3 -> {
                pb.rect(3, 13, 3, 5 - lOff, pants); pb.rect(8, 13, 3, 5 - rOff, pants)
                pb.rect(3, 18 - lOff, 3, 1, boots); pb.rect(8, 18 - rOff, 3, 1, boots)
                pb.rect(2, 8, 10, 6, suit); pb.rect(3, 9, 8, 4, rgb(120, 126, 138)); pb.rect(2, 12, 10, 1, suitD)
                pb.rect(0, 9, 2, 5, suit); pb.rect(12, 9, 2, 5, suit)
                pb.rect(3, 3, 8, 6, hat); pb.rect(2, 1, 10, 3, hat); pb.rect(3, 7, 8, 2, hatD); pb.rect(6, 0, 2, 2, hat)
            }
            else -> {
                val swing = if (frame == 1) 2 else if (frame == 3) -2 else 0
                pb.rect(5 + swing / 2, 13, 3, 5, pants); pb.rect(6 - swing / 2, 13, 3, 5, shade(pants, 0.8f))
                pb.rect(5 + swing / 2, 18, 4, 1, boots); pb.rect(6 - swing / 2, 18, 4, 1, shade(boots, 0.8f))
                pb.rect(4, 8, 7, 6, suit); pb.rect(4, 12, 7, 1, suitD)
                pb.rect(5 + swing, 9, 3, 5, suitD); pb.rect(5 + swing, 13, 3, 2, skin)
                pb.rect(4, 3, 7, 6, skin); pb.rect(9, 5, 1, 2, outl)
                pb.rect(3, 1, 9, 3, hat); pb.rect(3, 3, 10, 1, hatD); pb.rect(11, 3, 2, 1, hat); pb.rect(5, 0, 3, 2, hat)
            }
        }
        pb.outlineShape(outl)
        return pb
    }

    fun mirror(src: PB): PB {
        val d = PB(src.w, src.h)
        for (y in 0 until src.h) for (x in 0 until src.w) d.set(src.w - 1 - x, y, src.p[y * src.w + x])
        return d
    }

    fun hog(frame: Int, flash: Boolean): PB {
        val pb = PB(18, 14)
        val body = if (flash) rgb(255, 200, 200) else rgb(132, 86, 60); val dk = shade(body, 0.7f)
        val up = if (frame % 2 == 0) 0 else 1
        pb.rect(2, 4, 12, 7, body); pb.rect(2, 4, 12, 2, shade(body, 1.2f)); pb.rect(2, 9, 12, 2, dk)
        pb.rect(13, 3, 4, 7, body); pb.rect(16, 6, 2, 3, rgb(230, 150, 150)); pb.set(17, 7, rgb(120, 50, 60))
        pb.rect(14, 4, 1, 1, outl); pb.rect(15, 9, 1, 2, rgb(250, 250, 240)); pb.rect(12, 2, 2, 2, dk)
        pb.rect(3, 11, 2, 2 + up, dk); pb.rect(6, 11, 2, 3 - up, dk); pb.rect(9, 11, 2, 2 + up, dk); pb.rect(12, 11, 2, 3 - up, dk)
        pb.rect(2, 3, 5, 2, dk)
        pb.outlineShape(outl)
        return pb
    }

    fun spitter(frame: Int, flash: Boolean): PB {
        val pb = PB(16, 16)
        val body = if (flash) rgb(230, 255, 200) else rgb(120, 190, 70); val dk = shade(body, 0.65f)
        val b = frame % 2
        pb.disc(8f, 10f - b * 0.5f, 6f, dk); pb.disc(8f, 9.5f - b * 0.5f, 5.2f, body); pb.disc(6.5f, 7.5f, 2f, shade(body, 1.3f))
        pb.rect(3, 2 + b, 1, 4, dk); pb.rect(12, 2 + b, 1, 4, dk); pb.disc(3.5f, 2f + b, 1.6f, rgb(250, 250, 250)); pb.disc(12.5f, 2f + b, 1.6f, rgb(250, 250, 250))
        pb.set(3, 2 + b, outl); pb.set(12, 2 + b, outl)
        pb.rect(5, 11, 6, 2, rgb(80, 20, 40)); pb.rect(6, 11, 1, 1, rgb(240, 240, 220)); pb.rect(9, 11, 1, 1, rgb(240, 240, 220))
        pb.outlineShape(outl)
        return pb
    }

    fun stinger(frame: Int, flash: Boolean): PB {
        val pb = PB(22, 18)
        val body = if (flash) rgb(255, 210, 190) else rgb(150, 50, 52); val dk = shade(body, 0.6f)
        val l = frame % 2
        for (k in 0 until 3) {
            val y = 6 + k * 3
            pb.line(7, y, 2, y + 3 - l, dk); pb.line(14, y, 19, y + 3 - l, dk)
            pb.line(2, y + 3 - l, 1, y + 6 - l, dk); pb.line(19, y + 3 - l, 20, y + 6 - l, dk)
        }
        pb.disc(11f, 10f, 5.8f, dk); pb.disc(11f, 9.5f, 5f, body); pb.disc(10f, 8f, 2f, shade(body, 1.35f))
        pb.rect(8, 3, 6, 4, body); pb.rect(8, 4, 2, 2, rgb(255, 230, 90)); pb.rect(12, 4, 2, 2, rgb(255, 230, 90))
        pb.rect(10, 14, 2, 3, rgb(240, 220, 120)); pb.set(11, 17, rgb(255, 255, 200))
        pb.outlineShape(outl)
        return pb
    }

    fun proj(): PB {
        val pb = PB(6, 6)
        pb.disc(3f, 3f, 2.6f, rgb(130, 220, 70)); pb.disc(2.5f, 2.5f, 1.2f, rgb(220, 255, 160))
        pb.outlineShape(rgb(30, 70, 20))
        return pb
    }

    // ---- UI icons (14x14) ----
    fun icon(name: String): PB {
        val pb = PB(14, 14)
        val w = rgb(240, 242, 248); val d = rgb(40, 44, 56)
        when (name) {
            "bag" -> { pb.rect(3, 4, 8, 9, w); pb.rect(4, 2, 6, 3, w); pb.rect(5, 3, 4, 1, d); pb.rect(4, 8, 6, 2, d); pb.rect(5, 9, 4, 1, w); pb.rect(3, 12, 8, 1, shade(w, 0.7f)) }
            "hammer" -> { pb.rect(2, 2, 8, 4, w); pb.rect(2, 2, 8, 1, shade(w, 1.0f)); pb.rect(5, 6, 3, 7, rgb(214, 160, 90)); pb.rect(9, 3, 2, 2, d) }
            "gear" -> { pb.disc(7f, 7f, 4.6f, w); for (a in 0 until 8) { val x = 7 + Math.round(Math.cos(a * 0.785) * 5.6).toInt(); val y = 7 + Math.round(Math.sin(a * 0.785) * 5.6).toInt(); pb.rect(x - 1, y - 1, 2, 2, w) }; pb.disc(7f, 7f, 2f, d) }
            "map" -> { pb.rect(1, 2, 12, 10, w); pb.line(5, 2, 5, 11, d); pb.line(9, 3, 9, 12, d); pb.rect(6, 5, 2, 2, rgb(230, 70, 70)) }
            "menu" -> { pb.rect(2, 3, 10, 2, w); pb.rect(2, 6, 10, 2, w); pb.rect(2, 9, 10, 2, w) }
            "sword" -> { pb.line(2, 12, 11, 3, w); pb.line(3, 12, 12, 3, w); pb.rect(2, 9, 4, 1, rgb(214, 160, 90)); pb.rect(3, 10, 1, 3, rgb(214, 160, 90)); pb.rect(10, 1, 3, 3, w) }
            "run" -> { pb.disc(8f, 3f, 2f, w); pb.line(8, 5, 6, 9, w); pb.line(6, 9, 3, 12, w); pb.line(7, 8, 11, 9, w); pb.line(7, 6, 4, 6, w); pb.line(6, 9, 9, 12, w) }
            "berry" -> { pb.disc(7f, 8f, 4.5f, rgb(230, 60, 100)); pb.rect(7, 1, 1, 3, rgb(90, 190, 90)); pb.rect(8, 2, 3, 1, rgb(90, 190, 90)); pb.set(5, 6, w) }
            "plus" -> { pb.rect(6, 2, 2, 10, w); pb.rect(2, 6, 10, 2, w) }
            "minus" -> { pb.rect(2, 6, 10, 2, w) }
            "rot" -> { pb.ring(7f, 7f, 5f, 1.6f, w); pb.rect(7, 0, 5, 4, 0); pb.rect(8, 1, 4, 3, w); pb.rect(11, 3, 2, 3, w) }
            "ok" -> { pb.line(2, 7, 5, 11, rgb(120, 240, 140)); pb.line(3, 7, 5, 10, rgb(120, 240, 140)); pb.line(5, 11, 12, 2, rgb(120, 240, 140)); pb.line(5, 10, 11, 2, rgb(120, 240, 140)) }
            "x" -> { pb.line(2, 2, 11, 11, rgb(250, 110, 110)); pb.line(3, 2, 12, 11, rgb(250, 110, 110)); pb.line(11, 2, 2, 11, rgb(250, 110, 110)); pb.line(12, 2, 3, 11, rgb(250, 110, 110)) }
            "trash" -> { pb.rect(3, 4, 8, 9, w); pb.rect(2, 2, 10, 2, w); pb.rect(5, 1, 4, 1, w); pb.rect(5, 6, 1, 5, d); pb.rect(8, 6, 1, 5, d) }
            "bolt" -> { pb.line(8, 1, 4, 7, rgb(255, 224, 80)); pb.line(4, 7, 8, 7, rgb(255, 224, 80)); pb.line(8, 7, 5, 13, rgb(255, 224, 80)); pb.line(9, 1, 5, 7, rgb(255, 224, 80)); pb.line(9, 7, 6, 13, rgb(255, 224, 80)) }
            "heart" -> { pb.disc(4.5f, 5f, 3f, rgb(236, 70, 90)); pb.disc(9.5f, 5f, 3f, rgb(236, 70, 90)); for (y in 6..12) { val hw = 6 - (y - 6); pb.rect(7 - hw, y, hw * 2, 1, rgb(236, 70, 90)) } }
            "left" -> { pb.line(10, 2, 3, 7, w); pb.line(3, 7, 10, 12, w); pb.line(10, 3, 4, 7, w); pb.line(4, 7, 10, 11, w) }
            "right" -> { pb.line(3, 2, 10, 7, w); pb.line(10, 7, 3, 12, w); pb.line(3, 3, 9, 7, w); pb.line(9, 7, 3, 11, w) }
            "sun" -> { pb.disc(7f, 7f, 3f, rgb(255, 214, 70)); for (a in 0 until 8) { val x = 7 + Math.round(Math.cos(a * 0.785) * 5.5).toInt(); val y = 7 + Math.round(Math.sin(a * 0.785) * 5.5).toInt(); pb.set(x, y, rgb(255, 214, 70)) } }
            "moon" -> { pb.disc(7f, 7f, 5f, rgb(200, 210, 250)); pb.disc(9.5f, 5.5f, 4.2f, 0) }
            "hand" -> { pb.rect(4, 6, 6, 6, w); pb.rect(4, 2, 1, 5, w); pb.rect(6, 1, 1, 6, w); pb.rect(8, 2, 1, 5, w); pb.rect(10, 4, 1, 4, w) }
            "eye" -> { pb.disc(7f, 7f, 5f, w); pb.disc(7f, 7f, 2.5f, rgb(70, 150, 230)); pb.disc(7f, 7f, 1f, d) }
            else -> pb.rect(3, 3, 8, 8, w)
        }
        return pb
    }
}
