// Pixel Factory - engine: procedural art for items, buildings and belts. Developer: @yfeeeee
package com.yfeeeee.factory.engine

import com.yfeeeee.factory.core.Bld
import com.yfeeeee.factory.core.Kind
import com.yfeeeee.factory.core.Shape

object ArtBuild {
    fun itemIcon(shape: Int, col: Int): PB {
        val pb = PB(12, 12)
        val dk = shade(col, 0.62f); val lt = shade(col, 1.32f)
        when (shape) {
            Shape.ORE -> { pb.disc(6f, 6.8f, 4.9f, dk); pb.disc(5.6f, 6.1f, 4.1f, col); pb.disc(4.5f, 4.6f, 1.7f, lt); pb.set(8, 8, dk); pb.set(7, 3, lt) }
            Shape.INGOT -> { pb.rect(1, 5, 10, 4, col); pb.rect(2, 3, 8, 2, lt); pb.rect(1, 8, 10, 1, dk); pb.set(10, 4, dk); pb.set(10, 5, dk) }
            Shape.PLATE -> { pb.bevel(1, 3, 10, 6, col, dk); pb.set(3, 5, lt); pb.set(4, 5, lt) }
            Shape.ROD -> { pb.line(2, 10, 10, 2, dk); pb.line(2, 9, 9, 2, col); pb.line(3, 9, 9, 3, lt) }
            Shape.SCREW -> { pb.rect(4, 1, 5, 2, lt); pb.rect(5, 3, 3, 7, col); pb.set(5, 4, dk); pb.set(7, 6, dk); pb.set(5, 8, dk); pb.set(6, 10, dk) }
            Shape.COIL -> { pb.ring(6f, 6f, 5f, 2.5f, col); pb.ring(6f, 6f, 5f, 1f, lt); pb.disc(6f, 6f, 1.6f, dk) }
            Shape.CABLE -> { pb.line(1, 8, 4, 4, dk); pb.line(4, 4, 7, 8, dk); pb.line(7, 8, 10, 4, dk); pb.line(1, 7, 4, 3, col); pb.line(4, 3, 7, 7, col); pb.line(7, 7, 10, 3, col); pb.set(10, 4, lt) }
            Shape.GEAR -> { pb.disc(6f, 6f, 4f, col); for (a in 0 until 8) { val x = 6 + Math.round(Math.cos(a * 0.785) * 5).toInt(); val y = 6 + Math.round(Math.sin(a * 0.785) * 5).toInt(); pb.rect(x - 1, y - 1, 2, 2, dk) }; pb.disc(6f, 6f, 1.4f, dk); pb.set(4, 4, lt) }
            Shape.BOX -> { pb.rect(1, 1, 10, 10, col); pb.frame(1, 1, 10, 10, dk); pb.line(1, 1, 10, 10, dk); pb.line(10, 1, 1, 10, dk); pb.rect(2, 2, 3, 1, lt) }
            Shape.CRYSTAL -> { pb.line(6, 0, 10, 5, col); pb.line(10, 5, 6, 11, col); pb.line(6, 11, 2, 5, col); pb.line(2, 5, 6, 0, col); for (y in 1..10) for (x in 2..10) if (pb.get(x, y) == 0) { val ins = Math.abs(x - 6) <= Math.min(y, 11 - y) / 2 + (if (y < 5) y / 2 else (11 - y) / 2); if (ins && Math.abs(x - 6) <= 4 - Math.abs(y - 5) / 2) pb.set(x, y, if (x < 6) lt else col) }; pb.line(6, 0, 2, 5, lt); pb.line(6, 0, 6, 11, dk) }
            Shape.DROP -> { pb.disc(6f, 7.5f, 3.8f, col); pb.rect(5, 2, 2, 3, col); pb.rect(5, 4, 2, 2, col); pb.set(5, 1, col); pb.set(4, 6, lt); pb.set(4, 7, lt); pb.outlineShape(dk) }
            Shape.LEAF -> { pb.disc(6f, 6f, 4.8f, col); pb.disc(4.5f, 4.5f, 2f, lt); pb.line(2, 10, 9, 3, dk); pb.set(10, 2, dk) }
            Shape.LOG -> { pb.rect(1, 3, 9, 6, col); pb.rect(1, 3, 9, 1, lt); pb.rect(1, 8, 9, 1, dk); pb.disc(9f, 6f, 3.2f, shade(col, 1.4f)); pb.ring(9f, 6f, 2.2f, 0.9f, dk); pb.set(9, 6, dk) }
            Shape.POWDER -> { for (y in 0 until 6) { val hw = y / 2 + 1; pb.rect(6 - hw - 1, 4 + y, hw * 2 + 2, 1, if (y % 2 == 0) col else dk) }; pb.set(5, 5, lt); pb.set(7, 6, lt); pb.set(6, 3, col) }
            Shape.BOARD -> { pb.bevel(1, 2, 10, 8, col, dk); pb.rect(3, 4, 3, 3, dk); pb.rect(7, 4, 2, 2, lt); pb.line(3, 8, 9, 8, lt); pb.set(8, 7, shade(col, 1.6f)) }
            Shape.MOTOR -> { pb.rect(1, 4, 8, 5, col); pb.rect(9, 5, 2, 3, lt); for (x in 2 until 8 step 2) pb.rect(x, 3, 1, 7, dk); pb.rect(1, 4, 8, 1, lt) }
            Shape.BERRY -> { pb.disc(6f, 7f, 4.2f, col); pb.disc(5f, 6f, 1.3f, lt); pb.rect(6, 1, 1, 3, rgb(60, 140, 60)); pb.rect(7, 2, 2, 1, rgb(60, 140, 60)) }
            Shape.BRICK -> { pb.rect(1, 3, 10, 6, col); pb.frame(1, 3, 10, 6, dk); pb.line(1, 6, 10, 6, dk); pb.line(6, 3, 6, 5, dk); pb.line(3, 6, 3, 8, dk); pb.line(8, 6, 8, 8, dk); pb.rect(2, 4, 3, 1, lt) }
            Shape.PIPE -> { pb.rect(1, 4, 10, 4, col); pb.rect(1, 4, 10, 1, lt); pb.rect(1, 7, 10, 1, dk); pb.rect(0, 3, 2, 6, dk); pb.rect(10, 3, 2, 6, dk) }
            Shape.COMPUTER -> { pb.rect(1, 1, 10, 7, col); pb.frame(1, 1, 10, 7, dk); pb.rect(2, 2, 8, 5, rgb(90, 220, 220)); pb.rect(2, 2, 3, 1, rgb(200, 255, 255)); pb.rect(5, 8, 2, 2, dk); pb.rect(3, 10, 6, 1, dk) }
            Shape.PELLET -> { pb.disc(3.5f, 6f, 2.5f, col); pb.disc(8.5f, 6f, 2.5f, col); pb.rect(3, 3, 6, 6, col); pb.rect(3, 3, 6, 1, lt); pb.rect(3, 8, 6, 1, dk) }
            Shape.ORB -> { pb.disc(6f, 6f, 5.4f, dk); pb.disc(6f, 6f, 4.4f, col); pb.disc(4.5f, 4.5f, 1.8f, lt); pb.ring(6f, 6f, 5.4f, 0.8f, rgb(255, 255, 255)) }
            Shape.DISK -> { pb.disc(6f, 6f, 5.2f, col); pb.disc(6f, 6f, 1.8f, rgb(200, 200, 220)); pb.ring(6f, 6f, 4.6f, 0.8f, lt); pb.set(3, 3, lt) }
            Shape.BEAM -> { pb.rect(1, 2, 10, 2, lt); pb.rect(1, 8, 10, 2, col); pb.rect(5, 4, 2, 4, col); pb.rect(1, 9, 10, 1, dk) }
            else -> pb.disc(6f, 6f, 4f, col)
        }
        pb.outlineShape(shade(col, 0.32f))
        return pb
    }

    fun rotate90(src: PB): PB {
        val d = PB(src.h, src.w)
        for (y in 0 until src.h) for (x in 0 until src.w) d.set(src.h - 1 - y, x, src.p[y * src.w + x])
        return d
    }

    fun belt(mk: Int, rot: Int, shape: Int, frame: Int): PB {
        // built for flow towards east; corner shapes feed from north (shape 1) or south (shape 2)
        val pb = PB(16, 16)
        val rail = if (mk == 0) rgb(214, 176, 60) else if (mk == 1) rgb(80, 150, 220) else rgb(190, 100, 230)
        val railL = shade(rail, 1.3f); val railD = shade(rail, 0.7f)
        val track = rgb(48, 50, 58); val stripe = rgb(74, 78, 90); val arrowC = rgb(160, 164, 176)
        if (shape == 0) {
            for (y in 3..12) for (x in 0 until 16) pb.set(x, y, if (((x + 16 - frame * 4) / 4) % 2 == 0) stripe else track)
            pb.rect(0, 1, 16, 2, rail); pb.rect(0, 13, 16, 2, rail)
            pb.rect(0, 1, 16, 1, railL); pb.rect(0, 14, 16, 1, railD)
            pb.rect(7, 7, 4, 2, arrowC); pb.rect(10, 6, 1, 4, arrowC); pb.rect(11, 7, 1, 2, arrowC)
        } else {
            for (y in 0..12) for (x in 3..12) pb.set(x, y, if (((y + 16 - frame * 4) / 4) % 2 == 0) stripe else track)
            for (y in 3..12) for (x in 9..15) pb.set(x, y, if (((x + 16 - frame * 4) / 4) % 2 == 0) stripe else track)
            pb.rect(1, 0, 2, 15, rail); pb.rect(1, 13, 15, 2, rail)
            pb.rect(13, 0, 2, 3, rail); pb.rect(13, 1, 3, 2, rail)
            pb.rect(1, 0, 1, 15, railL); pb.rect(1, 14, 15, 1, railD); pb.rect(13, 1, 3, 1, railL)
            pb.rect(7, 7, 4, 2, arrowC); pb.rect(10, 6, 1, 4, arrowC); pb.rect(11, 7, 1, 2, arrowC)
            if (shape == 2) {
                val m = PB(16, 16)
                for (y in 0 until 16) for (x in 0 until 16) m.set(x, 15 - y, pb.p[y * 16 + x])
                var r2 = m
                for (i in 0 until rot) r2 = rotate90(r2)
                return r2
            }
        }
        var r = pb
        for (i in 0 until rot) r = rotate90(r)
        return r
    }

    private fun arrow(pb: PB, cx: Int, cy: Int, dir: Int, c: Int) {
        when (dir) {
            0 -> { pb.rect(cx, cy - 1, 2, 3, c); pb.set(cx + 2, cy, c) }
            1 -> { pb.rect(cx - 1, cy, 3, 2, c); pb.set(cx, cy + 2, c) }
            2 -> { pb.rect(cx - 1, cy - 1, 2, 3, c); pb.set(cx - 2, cy, c) }
            else -> { pb.rect(cx - 1, cy - 1, 3, 2, c); pb.set(cx, cy - 2, c) }
        }
    }

    private fun bolts(pb: PB, col: Int) {
        val c = shade(col, 0.55f); val l = shade(col, 1.4f)
        for (p in intArrayOf(3, pb.w - 5)) for (q in intArrayOf(3, pb.h - 5)) { pb.rect(p, q, 2, 2, c); pb.set(p, q, l) }
    }

    private fun led(pb: PB, x: Int, y: Int, c: Int) { pb.rect(x, y, 3, 3, shade(c, 0.5f)); pb.rect(x, y, 2, 2, c) }

    fun building(d: Bld, rot: Int, frame: Int): PB {
        val w = (if (rot % 2 == 0) d.w else d.h) * 16
        val h = (if (rot % 2 == 0) d.h else d.w) * 16
        val pb = PB(w, h)
        val dark = rgb(24, 26, 32)
        when (d.kind) {
            Kind.MINER -> {
                val acc = if (d.v >= 4f) rgb(180, 90, 230) else if (d.v >= 2f) rgb(80, 150, 230) else rgb(236, 140, 40)
                pb.bevel(0, 0, 32, 32, rgb(96, 104, 116), dark); bolts(pb, rgb(96, 104, 116))
                pb.rect(2, 2, 28, 3, acc); pb.rect(2, 27, 28, 3, acc)
                pb.disc(16f, 16f, 10.5f, rgb(30, 34, 40)); pb.ring(16f, 16f, 10.5f, 1.5f, rgb(120, 128, 140))
                val ang = frame * 0.52
                for (k in 0 until 3) {
                    val a = ang + k * 2.094
                    val x1 = 16 + Math.round(Math.cos(a) * 9).toInt(); val y1 = 16 + Math.round(Math.sin(a) * 9).toInt()
                    pb.line(16, 16, x1, y1, rgb(200, 206, 214)); pb.line(17, 16, x1, y1 + 0, rgb(150, 156, 166))
                }
                pb.disc(16f, 16f, 3f, acc); pb.set(15, 15, shade(acc, 1.5f))
                led(pb, 3, 3, rgb(90, 230, 110))
            }
            Kind.PUMP -> {
                pb.bevel(0, 0, 32, 32, rgb(70, 74, 86), dark); bolts(pb, rgb(70, 74, 86))
                pb.disc(16f, 17f, 11f, rgb(36, 28, 44)); pb.ring(16f, 17f, 11f, 1.5f, rgb(130, 120, 150))
                pb.rect(15, 4 + (frame % 2) * 2, 2, 14, rgb(200, 200, 210)); pb.rect(11, 4, 10, 2, rgb(150, 152, 164))
                pb.disc(16f, 17f, 3f, rgb(150, 100, 210))
            }
            Kind.GEO -> {
                pb.bevel(0, 0, 48, 48, rgb(78, 66, 70), dark); bolts(pb, rgb(78, 66, 70))
                pb.disc(24f, 24f, 16f, rgb(40, 34, 38)); pb.ring(24f, 24f, 16f, 2f, rgb(150, 120, 110)); pb.disc(24f, 24f, 8f, rgb(220, 100, 40)); pb.disc(24f, 24f, 4f, rgb(255, 200, 80))
                for (i in 0 until 3) { val y = 10 - ((frame * 2 + i * 5) % 14); pb.disc((18 + i * 6).toFloat(), (14 + y).toFloat(), 2.4f, withAlpha(rgb(235, 235, 240), 255)) }
            }
            Kind.MACHINE -> machineArt(pb, d, frame)
            Kind.GENERATOR -> genArt(pb, d, frame)
            Kind.SOLAR -> {
                pb.bevel(0, 0, 48, 48, rgb(60, 70, 90), dark)
                for (cy in 0 until 3) for (cx in 0 until 3) { pb.rect(3 + cx * 14, 3 + cy * 14, 12, 12, rgb(40, 84, 160)); pb.rect(3 + cx * 14, 3 + cy * 14, 12, 2, rgb(80, 130, 210)); pb.line(3 + cx * 14, 8 + cy * 14, 14 + cx * 14, 8 + cy * 14, rgb(60, 110, 190)); pb.line(8 + cx * 14, 3 + cy * 14, 8 + cx * 14, 14 + cy * 14, rgb(60, 110, 190)) }
                pb.rect(6, 6, 3, 1, rgb(220, 240, 255)); pb.rect(20, 20, 3, 1, rgb(220, 240, 255))
            }
            Kind.BATTERY -> {
                pb.bevel(0, 0, 32, 32, rgb(52, 58, 68), dark); bolts(pb, rgb(52, 58, 68))
                for (i in 0 until 3) { pb.rect(5 + i * 8, 6, 6, 20, rgb(36, 40, 46)); pb.rect(6 + i * 8, 8, 4, 16, rgb(90, 220, 110)); pb.rect(6 + i * 8, 8, 4, 5, rgb(230, 220, 90)); pb.rect(7 + i * 8, 4, 2, 2, rgb(200, 200, 210)) }
            }
            Kind.POLE -> {
                pb.disc(8f, 9f, 5f, withAlpha(rgb(0, 0, 0), 70))
                pb.rect(7, 4, 2, 9, rgb(120, 86, 52)); pb.rect(3, 5, 10, 1, rgb(150, 150, 160)); pb.rect(4, 8, 8, 1, rgb(150, 150, 160))
                pb.rect(2, 4, 2, 2, rgb(240, 240, 250)); pb.rect(12, 4, 2, 2, rgb(240, 240, 250)); pb.rect(7, 2, 2, 2, rgb(240, 200, 60))
            }
            Kind.HUB -> hubArt(pb, frame)
            Kind.MAM -> {
                pb.bevel(0, 0, 32, 32, rgb(54, 56, 72), dark); bolts(pb, rgb(54, 56, 72))
                pb.rect(5, 5, 22, 16, rgb(20, 16, 36)); pb.frame(5, 5, 22, 16, rgb(120, 110, 170))
                val sy = 6 + (frame * 4) % 14
                pb.rect(6, sy, 20, 2, rgb(170, 110, 255)); pb.rect(6, sy + 2, 20, 1, rgb(110, 70, 190))
                pb.disc(16f, 13f, 4f, rgb(150, 100, 230)); pb.ring(16f, 13f, 4f, 1f, rgb(220, 200, 255))
                pb.rect(7, 23, 18, 4, rgb(36, 38, 50)); for (i in 0 until 5) pb.rect(8 + i * 3, 24, 2, 2, if ((i + frame) % 2 == 0) rgb(120, 220, 140) else rgb(200, 80, 80))
            }
            Kind.ELEVATOR -> {
                pb.bevel(0, 0, 96, 96, rgb(120, 122, 130), dark)
                for (i in 0 until 12) { val c = if (i % 2 == 0) rgb(240, 190, 40) else dark; pb.rect(2 + i * 8, 2, 4, 3, c); pb.rect(2 + i * 8, 91, 4, 3, c); pb.rect(2, 2 + i * 8, 3, 4, c); pb.rect(91, 2 + i * 8, 3, 4, c) }
                pb.disc(48f, 48f, 38f, rgb(86, 88, 98)); pb.ring(48f, 48f, 38f, 3f, rgb(60, 62, 70))
                pb.disc(48f, 48f, 26f, rgb(60, 64, 78)); pb.ring(48f, 48f, 26f, 2f, rgb(160, 170, 190))
                pb.disc(48f, 48f, 14f, rgb(34, 38, 52)); pb.disc(48f, 48f, 8f, rgb(120, 200, 250))
                for (i in 0 until 8) { val a = i * 0.785; val x = 48 + Math.round(Math.cos(a) * 32).toInt(); val y = 48 + Math.round(Math.sin(a) * 32).toInt(); pb.rect(x - 2, y - 2, 4, 4, if ((i + frame) % 2 == 0) rgb(255, 120, 80) else rgb(120, 60, 50)) }
            }
            Kind.CONTAINER -> {
                pb.bevel(0, 0, w, h, rgb(112, 120, 132), dark); bolts(pb, rgb(112, 120, 132))
                if (w > h) { pb.rect(14, 2, 4, h - 4, rgb(80, 88, 100)); pb.rect(4, 6, w - 8, 2, rgb(90, 98, 110)); pb.rect(4, h - 8, w - 8, 2, rgb(90, 98, 110)); pb.rect(w / 2 - 3, h / 2 - 2, 6, 4, rgb(240, 190, 50)) }
                else { pb.rect(2, 14, w - 4, 4, rgb(80, 88, 100)); pb.rect(6, 4, 2, h - 8, rgb(90, 98, 110)); pb.rect(w - 8, 4, 2, h - 8, rgb(90, 98, 110)); pb.rect(w / 2 - 2, h / 2 - 3, 4, 6, rgb(240, 190, 50)) }
            }
            Kind.FOUNDATION -> {
                pb.rect(0, 0, 16, 16, rgb(150, 150, 154)); pb.frame(0, 0, 16, 16, rgb(120, 120, 126)); pb.rect(1, 1, 14, 1, rgb(176, 176, 180)); pb.rect(1, 1, 1, 14, rgb(170, 170, 174))
                pb.set(4, 5, rgb(130, 130, 136)); pb.set(11, 10, rgb(130, 130, 136)); pb.set(8, 13, rgb(166, 166, 170))
            }
            Kind.WALL -> {
                pb.rect(0, 0, 16, 11, rgb(176, 176, 182)); pb.rect(0, 11, 16, 5, rgb(110, 110, 118)); pb.frame(0, 0, 16, 16, dark)
                pb.rect(1, 1, 14, 2, rgb(206, 206, 212)); pb.line(0, 11, 15, 11, rgb(86, 86, 94)); pb.line(5, 12, 5, 14, rgb(86, 86, 94)); pb.line(11, 12, 11, 14, rgb(86, 86, 94))
            }
            Kind.LAMP -> {
                pb.disc(8f, 9f, 4f, withAlpha(rgb(0, 0, 0), 70))
                pb.rect(7, 6, 2, 7, rgb(90, 94, 104)); pb.disc(8f, 5f, 3.4f, rgb(250, 220, 110)); pb.disc(8f, 5f, 1.8f, rgb(255, 250, 220)); pb.ring(8f, 5f, 3.4f, 0.9f, rgb(150, 120, 40))
            }
            Kind.SPLITTER, Kind.MERGER, Kind.SORTER -> {
                pb.bevel(1, 1, 14, 14, rgb(90, 96, 108), dark)
                val a = rgb(240, 190, 50); val a2 = rgb(150, 160, 180)
                when (d.kind) {
                    Kind.SPLITTER -> { for (k in 0 until 4) if (k != (rot + 2) % 4) arrow(pb, 8 + com.yfeeeee.factory.core.DX[k] * 4 - (if (k == 0) 0 else 0), 8 + com.yfeeeee.factory.core.DY[k] * 4, k, a); pb.disc(8f, 8f, 2f, dark) }
                    Kind.MERGER -> { for (k in 0 until 4) if (k != rot) arrow(pb, 8 + com.yfeeeee.factory.core.DX[k] * 4, 8 + com.yfeeeee.factory.core.DY[k] * 4, (k + 2) % 4, a2); arrow(pb, 8 + com.yfeeeee.factory.core.DX[rot] * 4, 8 + com.yfeeeee.factory.core.DY[rot] * 4, rot, a); pb.disc(8f, 8f, 2f, dark) }
                    else -> { arrow(pb, 8 + com.yfeeeee.factory.core.DX[rot] * 4, 8 + com.yfeeeee.factory.core.DY[rot] * 4, rot, a); for (k in intArrayOf((rot + 1) % 4, (rot + 3) % 4)) arrow(pb, 8 + com.yfeeeee.factory.core.DX[k] * 4, 8 + com.yfeeeee.factory.core.DY[k] * 4, k, a2); pb.rect(6, 6, 4, 4, rgb(60, 220, 120)); pb.frame(6, 6, 4, 4, dark) }
                }
            }
            Kind.TUNNEL -> {
                pb.bevel(0, 0, 16, 16, rgb(100, 104, 114), dark)
                pb.disc(8f, 8f, 5f, rgb(10, 10, 14)); arrow(pb, 8 + com.yfeeeee.factory.core.DX[rot] * 3, 8 + com.yfeeeee.factory.core.DY[rot] * 3, rot, rgb(240, 190, 50))
            }
            else -> { pb.bevel(0, 0, w, h, rgb(100, 100, 110), dark) }
        }
        return pb
    }

    private fun machineArt(pb: PB, d: Bld, frame: Int) {
        val dark = rgb(24, 26, 32)
        val w = pb.w; val h = pb.h
        when (d) {
            Bld.SMELTER -> {
                val b = rgb(150, 86, 66)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                pb.rect(21, 3, 7, 9, rgb(70, 72, 82)); pb.rect(20, 2, 9, 2, rgb(100, 104, 116))
                pb.rect(7, 13, 14, 12, rgb(16, 14, 18)); pb.rect(9, 15, 10, 8, rgb(230, 100, 30))
                if (frame % 2 == 0) pb.rect(11, 17, 6, 4, rgb(255, 210, 80)) else pb.rect(12, 18, 4, 3, rgb(255, 240, 150))
                pb.rect(7, 13, 14, 2, rgb(70, 70, 80)); led(pb, 4, 4, rgb(90, 230, 110))
            }
            Bld.FOUNDRY -> {
                val b = rgb(80, 86, 100)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                pb.disc(16f, 16f, 11f, rgb(30, 30, 36)); pb.ring(16f, 16f, 11f, 2f, rgb(150, 154, 166))
                pb.disc(16f, 16f, 7f, if (frame % 2 == 0) rgb(250, 130, 40) else rgb(255, 170, 70)); pb.disc(15f, 15f, 3f, rgb(255, 230, 140))
                pb.rect(2, 12, 4, 8, rgb(110, 114, 126)); pb.rect(26, 12, 4, 8, rgb(110, 114, 126))
            }
            Bld.CONSTRUCTOR -> {
                val b = rgb(94, 132, 108)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                pb.rect(5, 12, 22, 14, rgb(34, 40, 38)); pb.frame(5, 12, 22, 14, rgb(140, 160, 150))
                pb.disc(9f, 9f, 3.5f, rgb(200, 204, 212))
                val a = -0.2 + 0.9 * Math.sin(frame * 1.57 + 0.5)
                val ex = 9 + Math.round(Math.cos(a + 0.8) * 13).toInt(); val ey = 9 + Math.round(Math.sin(a + 0.8) * 13).toInt()
                pb.line(9, 9, ex, ey, rgb(220, 224, 230)); pb.line(10, 9, ex, ey + 1, rgb(150, 154, 164)); pb.disc(ex.toFloat(), ey.toFloat(), 2f, rgb(240, 190, 50))
                led(pb, 23, 3, rgb(90, 230, 110))
            }
            Bld.ASSEMBLER -> {
                val b = rgb(84, 118, 170)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                pb.rect(8, 14, 32, 26, rgb(30, 36, 50)); pb.frame(8, 14, 32, 26, rgb(150, 170, 200))
                pb.rect(10, 28, 28, 3, rgb(60, 64, 76)); for (i in 0 until 7) pb.rect(10 + ((i * 4 + frame * 2) % 28), 28, 2, 3, rgb(110, 116, 130))
                for (side in 0 until 2) {
                    val bx = 10 + side * 28
                    pb.disc(bx.toFloat(), 8f, 3.5f, rgb(200, 204, 212))
                    val a = 0.6 + 0.7 * Math.sin(frame * 1.57 + side * 3.1)
                    val ex = bx + Math.round(Math.cos(1.57 + (if (side == 0) a else -a)) * 14).toInt(); val ey = 8 + Math.round(Math.sin(1.57 + a) * 14).toInt()
                    pb.line(bx, 8, ex, ey, rgb(220, 224, 230)); pb.disc(ex.toFloat(), ey.toFloat(), 2f, rgb(240, 190, 50))
                }
                led(pb, 4, 42, rgb(90, 230, 110)); pb.rect(34, 42, 10, 3, rgb(120, 200, 250))
            }
            Bld.MANUFACTURER -> {
                val b = rgb(120, 98, 168)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                pb.rect(10, 12, 44, 40, rgb(30, 26, 44)); pb.frame(10, 12, 44, 40, rgb(170, 150, 210))
                pb.rect(12, 30, 40, 4, rgb(70, 66, 84)); for (i in 0 until 9) pb.rect(12 + ((i * 5 + frame * 2) % 40), 30, 2, 4, rgb(120, 116, 140))
                for (k in 0 until 4) {
                    val bx = if (k % 2 == 0) 14 else 50; val by = if (k < 2) 8 else 56
                    pb.disc(bx.toFloat(), by.toFloat(), 3.5f, rgb(200, 204, 212))
                    val a = 0.5 + 0.8 * Math.sin(frame * 1.57 + k * 1.3)
                    val ex = bx + Math.round(Math.cos(a + k * 0.4) * 13 * (if (bx < 32) 1 else -1)).toInt(); val ey = by + Math.round(Math.sin(a) * 13 * (if (by < 32) 1 else -1)).toInt()
                    pb.line(bx, by, ex, ey, rgb(220, 224, 230)); pb.disc(ex.toFloat(), ey.toFloat(), 2f, rgb(240, 190, 50))
                }
                for (i in 0 until 6) pb.rect(8 + i * 8, 58, 4, 3, if ((i + frame) % 3 == 0) rgb(250, 120, 80) else rgb(90, 230, 110))
            }
            Bld.REFINERY -> {
                val b = rgb(150, 152, 164)
                pb.bevel(0, 0, w, h, b, dark); bolts(pb, b)
                for (k in 0 until 2) { val cx = 14 + k * 20; pb.disc(cx.toFloat(), 15f, 10f, rgb(110, 112, 124)); pb.ring(cx.toFloat(), 15f, 10f, 1.5f, rgb(70, 72, 82)); pb.disc((cx - 2).toFloat(), 13f, 4f, rgb(190, 192, 204)); pb.rect(cx - 9, 14, 18, 2, rgb(236, 140, 40)) }
                pb.rect(6, 28, 36, 3, rgb(90, 94, 104)); pb.rect(10, 27, 3, 12, rgb(90, 94, 104)); pb.rect(34, 27, 3, 12, rgb(90, 94, 104))
                pb.rect(38, 36, 6, 8, rgb(60, 60, 70)); pb.rect(39 + (frame % 2), 32, 4, 5, rgb(255, 170, 50)); pb.rect(40, 34, 2, 3, rgb(255, 230, 120))
            }
            else -> pb.bevel(0, 0, w, h, rgb(100, 100, 110), dark)
        }
    }

    private fun genArt(pb: PB, d: Bld, frame: Int) {
        val dark = rgb(24, 26, 32)
        val w = pb.w
        when (d) {
            Bld.BURNER -> {
                val b = rgb(128, 90, 60)
                pb.bevel(0, 0, w, w, b, dark); bolts(pb, b)
                pb.disc(15f, 17f, 11f, rgb(36, 30, 28)); pb.ring(15f, 17f, 11f, 1.6f, rgb(160, 120, 80))
                pb.disc(15f, 17f, 6f, if (frame % 2 == 0) rgb(240, 120, 30) else rgb(250, 160, 50)); pb.disc(15f, 16f, 3f, rgb(255, 230, 120))
                pb.rect(24, 3, 5, 9, rgb(70, 72, 82)); pb.rect(23, 2, 7, 2, rgb(100, 104, 116))
            }
            Bld.COAL_GEN -> {
                val b = rgb(72, 76, 88)
                pb.bevel(0, 0, w, w, b, dark); bolts(pb, b)
                pb.disc(22f, 26f, 15f, rgb(30, 32, 38)); pb.ring(22f, 26f, 15f, 2f, rgb(140, 146, 158))
                for (k in 0 until 6) { val a = frame * 0.5 + k * 1.047; val x = 22 + Math.round(Math.cos(a) * 12).toInt(); val y = 26 + Math.round(Math.sin(a) * 12).toInt(); pb.line(22, 26, x, y, rgb(190, 196, 206)) }
                pb.disc(22f, 26f, 3f, rgb(240, 190, 50))
                pb.rect(36, 4, 8, 14, rgb(60, 62, 72)); pb.rect(35, 3, 10, 3, rgb(100, 104, 116)); pb.disc(40f, (2 - (frame % 3)).toFloat(), 3f, withAlpha(rgb(120, 120, 128), 255))
            }
            Bld.FUEL_GEN -> {
                val b = rgb(172, 112, 52)
                pb.bevel(0, 0, w, w, b, dark); bolts(pb, b)
                for (k in 0 until 4) { val x = 6 + k * 9; val up = ((frame + k) % 4) * 2; pb.rect(x, 6, 7, 28, rgb(50, 44, 40)); pb.rect(x + 1, 8 + up, 5, 12, rgb(210, 214, 222)); pb.rect(x + 1, 8 + up, 5, 2, rgb(250, 250, 255)) }
                pb.rect(6, 38, 36, 5, rgb(236, 140, 40)); pb.rect(6, 38, 36, 1, rgb(255, 200, 120))
            }
            else -> pb.bevel(0, 0, w, w, rgb(100, 100, 110), dark)
        }
    }

    private fun hubArt(pb: PB, frame: Int) {
        val dark = rgb(24, 26, 32)
        pb.bevel(0, 0, 80, 80, rgb(140, 142, 150), dark)
        for (i in 0 until 10) { val c = if (i % 2 == 0) rgb(240, 190, 40) else dark; pb.rect(2 + i * 8, 2, 5, 3, c); pb.rect(2 + i * 8, 75, 5, 3, c); pb.rect(2, 2 + i * 8, 3, 5, c); pb.rect(75, 2 + i * 8, 3, 5, c) }
        pb.rect(10, 10, 60, 60, rgb(120, 122, 130)); pb.frame(10, 10, 60, 60, rgb(90, 92, 100))
        pb.disc(40f, 40f, 22f, rgb(70, 74, 86)); pb.ring(40f, 40f, 22f, 3f, rgb(236, 140, 40)); pb.disc(40f, 40f, 15f, rgb(46, 50, 62))
        pb.rect(32, 32, 3, 16, rgb(240, 240, 250)); pb.rect(45, 32, 3, 16, rgb(240, 240, 250)); pb.rect(32, 39, 16, 3, rgb(240, 240, 250))
        for (k in 0 until 4) { val x = if (k % 2 == 0) 14 else 62; val y = if (k < 2) 14 else 62; pb.disc((x + 2).toFloat(), (y + 2).toFloat(), 4f, rgb(60, 62, 72)); pb.disc((x + 2).toFloat(), (y + 2).toFloat(), 2.4f, if ((frame + k) % 2 == 0) rgb(255, 240, 150) else rgb(200, 160, 60)) }
        pb.rect(30, 62, 20, 6, rgb(36, 38, 48)); pb.rect(32, 63, 16, 4, rgb(100, 220, 130))
    }
}
