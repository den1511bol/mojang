// Pixel Factory - core: procedural world. Developer: @yfeeeee
package com.yfeeeee.factory.core

import java.util.Random

object T {
    const val DEEP = 0; const val SHALLOW = 1; const val SAND = 2; const val GRASS = 3; const val FOREST = 4
    const val DIRT = 5; const val ROCK = 6; const val SNOW = 7; const val SWAMP = 8; const val CLIFF = 9
    const val ASH = 10; const val LAVA = 11
    fun walkable(g: Int): Boolean = !(g == DEEP || g == CLIFF || g == LAVA)
    fun buildable(g: Int): Boolean = g == SAND || g == GRASS || g == FOREST || g == DIRT || g == ROCK || g == SNOW || g == SWAMP || g == ASH
}

object Deco {
    const val NONE = 0; const val TREE = 1; const val PINE = 2; const val CACTUS = 3; const val BOULDER = 4
    const val BUSH = 5; const val FLOWER = 6; const val TUFT = 7; const val DEADTREE = 8; const val BERRYBUSH = 9
    fun blocks(d: Int): Boolean = d == TREE || d == PINE || d == CACTUS || d == BOULDER || d == DEADTREE
    fun soft(d: Int): Boolean = d == FLOWER || d == TUFT || d == BUSH || d == BERRYBUSH
}

object Res {
    const val NONE = 0; const val IRON = 1; const val COPPER = 2; const val LIMESTONE = 3; const val COAL = 4
    const val CATERIUM = 5; const val QUARTZ = 6; const val SULFUR = 7; const val BAUXITE = 8; const val OIL = 9; const val GEYSER = 10
    fun item(t: Int): Item? = when (t) {
        IRON -> Item.IRON_ORE; COPPER -> Item.COPPER_ORE; LIMESTONE -> Item.LIMESTONE; COAL -> Item.COAL
        CATERIUM -> Item.CATERIUM_ORE; QUARTZ -> Item.RAW_QUARTZ; SULFUR -> Item.SULFUR; BAUXITE -> Item.BAUXITE
        OIL -> Item.CRUDE_OIL; else -> null
    }
    fun name(t: Int, en: Boolean): String = when (t) {
        IRON -> if (en) "Iron" else "Железо"; COPPER -> if (en) "Copper" else "Медь"
        LIMESTONE -> if (en) "Limestone" else "Известняк"; COAL -> if (en) "Coal" else "Уголь"
        CATERIUM -> if (en) "Caterium" else "Каверий"; QUARTZ -> if (en) "Quartz" else "Кварц"
        SULFUR -> if (en) "Sulfur" else "Сера"; BAUXITE -> if (en) "Bauxite" else "Боксит"
        OIL -> if (en) "Crude oil" else "Нефть"; GEYSER -> if (en) "Geyser" else "Гейзер"
        else -> "?"
    }
    fun purityName(p: Int, en: Boolean): String = when (p) {
        0 -> if (en) "impure" else "бедный"; 1 -> if (en) "normal" else "обычный"; else -> if (en) "pure" else "богатый"
    }
    fun purityMul(p: Int): Float = when (p) { 0 -> 0.5f; 1 -> 1f; else -> 2f }
}

class Node(val id: Int, val type: Int, val purity: Int, val cx: Int, val cy: Int) {
    var minerId = -1
}

class Pod(val x: Int, val y: Int) { var opened = false }

class World(val seed: Int) {
    val N = 512
    val startX = N / 2
    val startY = N / 2
    val ground = ByteArray(N * N)
    val deco = ByteArray(N * N)
    val nodeAt = ShortArray(N * N)
    val explored = ByteArray(N * N)
    val reach = BooleanArray(N * N)
    val nodes = ArrayList<Node>()
    val pods = ArrayList<Pod>()

    fun inB(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < N && y < N
    fun g(x: Int, y: Int): Int = if (inB(x, y)) ground[y * N + x].toInt() else T.DEEP
    fun d(x: Int, y: Int): Int = if (inB(x, y)) deco[y * N + x].toInt() else Deco.NONE
    fun node(x: Int, y: Int): Node? {
        if (!inB(x, y)) return null
        val v = nodeAt[y * N + x].toInt()
        return if (v == 0) null else nodes[v - 1]
    }

    fun walkableTile(x: Int, y: Int): Boolean = inB(x, y) && T.walkable(g(x, y)) && !Deco.blocks(d(x, y))
    fun isExplored(x: Int, y: Int): Boolean = inB(x, y) && explored[y * N + x].toInt() != 0

    fun reveal(px: Float, py: Float, r: Int) {
        val cx = px.toInt(); val cy = py.toInt()
        for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
            if (!inB(x, y)) continue
            val dx = x - cx; val dy = y - cy
            if (dx * dx + dy * dy <= r * r) explored[y * N + x] = 1
        }
    }

    fun generate() {
        val startRad = 11f
        for (y in 0 until N) for (x in 0 until N) {
            val fx = x.toFloat(); val fy = y.toFloat()
            var e = Noise.fbm(fx / 90f, fy / 90f, seed, 4)
            val edge = minOf(minOf(x, N - 1 - x), minOf(y, N - 1 - y)).toFloat()
            if (edge < 26f) e -= (26f - edge) / 26f * 0.45f
            val dd = Math.hypot((x - startX).toDouble(), (y - startY).toDouble()).toFloat()
            if (dd < 45f) { val k = dd / 45f; e = e * k + 0.5f * (1f - k) }
            val m = Noise.fbm(fx / 60f + 100f, fy / 60f, seed + 7, 3)
            val t = Noise.fbm(fx / 110f, fy / 110f + 50f, seed + 13, 3) + (fy / N - 0.5f) * 0.45f
            val volc = dd > 170f && Noise.fbm(fx / 50f + 300f, fy / 50f, seed + 21, 3) > 0.56f
            val gg: Int = when {
                e < 0.31f -> T.DEEP
                e < 0.355f -> T.SHALLOW
                e < 0.385f && t < 0.80f -> T.SAND
                volc && e > 0.40f -> if (Noise.fbm(fx / 14f, fy / 14f, seed + 29, 2) > 0.66f) T.LAVA else T.ASH
                e > 0.73f -> T.CLIFF
                e > 0.65f -> if (t < 0.30f) T.SNOW else T.ROCK
                t < 0.30f -> T.SNOW
                t > 0.64f && m < 0.52f -> T.SAND
                m > 0.60f && e < 0.47f -> T.SWAMP
                m > 0.53f -> T.FOREST
                else -> if (Noise.fbm(fx / 12f, fy / 12f, seed + 31, 2) > 0.66f) T.DIRT else T.GRASS
            }
            ground[y * N + x] = (if (dd < startRad) T.GRASS else gg).toByte()
        }
        // reachability (flood fill from start over walkable ground)
        val q = IntArray(N * N)
        var qh = 0; var qt = 0
        val s0 = startY * N + startX
        reach[s0] = true; q[qt++] = s0
        while (qh < qt) {
            val cur = q[qh++]
            val cx = cur % N; val cy = cur / N
            for (k in 0 until 4) {
                val nx = cx + DX[k]; val ny = cy + DY[k]
                if (!inB(nx, ny)) continue
                val ni = ny * N + nx
                if (reach[ni] || !T.walkable(ground[ni].toInt())) continue
                reach[ni] = true; q[qt++] = ni
            }
        }
        placeNodes()
        placeDeco()
        placePods()
        reveal(startX.toFloat(), startY.toFloat(), 14)
    }

    private fun nodeSpotOk(cx: Int, cy: Int, oilLike: Boolean): Boolean {
        for (y in cy - 1..cy + 1) for (x in cx - 1..cx + 1) {
            if (!inB(x, y)) return false
            val gi = y * N + x
            val gr = ground[gi].toInt()
            if (!T.buildable(gr) || !reach[gi]) return false
            if (oilLike && gr != T.SAND && gr != T.SWAMP && gr != T.ASH && gr != T.ROCK) return false
        }
        for (y in cy - 5..cy + 5) for (x in cx - 5..cx + 5) if (inB(x, y) && nodeAt[y * N + x].toInt() != 0) return false
        val dd = Math.hypot((cx - startX).toDouble(), (cy - startY).toDouble())
        return dd > 9.0
    }

    private fun addNode(type: Int, purity: Int, cx: Int, cy: Int) {
        val n = Node(nodes.size, type, purity, cx, cy)
        nodes.add(n)
        for (y in cy - 1..cy + 1) for (x in cx - 1..cx + 1) {
            nodeAt[y * N + x] = nodes.size.toShort()
            deco[y * N + x] = 0
        }
    }

    private fun placeNear(type: Int, purity: Int, tx: Int, ty: Int) {
        val oil = type == Res.OIL || type == Res.GEYSER
        for (r in 0..14) for (a in 0 until 16) {
            val ang = a / 16.0 * Math.PI * 2
            val x = tx + (Math.cos(ang) * r).toInt(); val y = ty + (Math.sin(ang) * r).toInt()
            if (inB(x, y) && nodeSpotOk(x, y, oil)) { addNode(type, purity, x, y); return }
        }
    }

    private fun placeNodes() {
        val rnd = Random(seed.toLong() * 7919L + 13L)
        val sx = startX; val sy = startY
        placeNear(Res.IRON, 1, sx - 20, sy - 8)
        placeNear(Res.COPPER, 1, sx + 18, sy - 12)
        placeNear(Res.LIMESTONE, 1, sx + 6, sy + 20)
        placeNear(Res.IRON, 2, sx + 22, sy + 14)
        placeNear(Res.COPPER, 0, sx - 16, sy + 18)
        placeNear(Res.LIMESTONE, 0, sx - 12, sy - 24)
        placeNear(Res.COAL, 1, sx + 34, sy - 2)
        placeNear(Res.COAL, 1, sx - 36, sy + 6)
        placeNear(Res.IRON, 0, sx + 2, sy - 30)
        var by = 12
        while (by < N - 12) {
            var bx = 12
            while (bx < N - 12) {
                if (rnd.nextFloat() < 0.62f) {
                    val x = bx + rnd.nextInt(18); val y = by + rnd.nextInt(18)
                    val dd = Math.hypot((x - sx).toDouble(), (y - sy).toDouble()).toFloat()
                    if (dd > 22f) {
                        val gr = g(x, y)
                        var wIron = 10f; var wCopper = 8f; var wLime = 7f
                        var wCoal = if (dd > 30f) 5f else 0f
                        var wCat = if (dd > 80f) 3.5f else 0f
                        var wQuartz = if (dd > 65f && (gr == T.ROCK || gr == T.SNOW)) 6f else 0f
                        var wSulfur = if (dd > 100f) 3.5f else 0f
                        var wBaux = if (dd > 125f) 4f else 0f
                        var wOil = if (dd > 85f && (gr == T.SAND || gr == T.SWAMP || gr == T.ASH)) 5f else 0f
                        var wGey = if (dd > 100f && (gr == T.ASH || gr == T.ROCK)) 3f else 0f
                        if (gr == T.ASH) { wSulfur *= 3f; wIron *= 0.3f; wCopper *= 0.3f; wLime *= 0.3f }
                        val tot = wIron + wCopper + wLime + wCoal + wCat + wQuartz + wSulfur + wBaux + wOil + wGey
                        var pick = rnd.nextFloat() * tot
                        var type = Res.IRON
                        val ws = floatArrayOf(wIron, wCopper, wLime, wCoal, wCat, wQuartz, wSulfur, wBaux, wOil, wGey)
                        val ts = intArrayOf(Res.IRON, Res.COPPER, Res.LIMESTONE, Res.COAL, Res.CATERIUM, Res.QUARTZ, Res.SULFUR, Res.BAUXITE, Res.OIL, Res.GEYSER)
                        for (i in ws.indices) { if (pick < ws[i]) { type = ts[i]; break }; pick -= ws[i] }
                        val pr = rnd.nextFloat()
                        val purity = if (pr < 0.30f) 0 else if (pr < 0.75f) 1 else 2
                        if (nodeSpotOk(x, y, type == Res.OIL || type == Res.GEYSER)) addNode(type, purity, x, y)
                    }
                }
                bx += 18
            }
            by += 18
        }
    }

    private fun placeDeco() {
        for (y in 0 until N) for (x in 0 until N) {
            val i = y * N + x
            if (nodeAt[i].toInt() != 0) continue
            val dd = Math.hypot((x - startX).toDouble(), (y - startY).toDouble())
            if (dd < 9.0) continue
            val r = Noise.rnd(x, y, seed + 77)
            val clump = Noise.fbm(x / 16f, y / 16f, seed + 55, 2)
            var dc = 0
            when (ground[i].toInt()) {
                T.FOREST -> dc = if (r < 0.32f * (0.5f + clump)) (if (r < 0.04f) Deco.PINE else Deco.TREE) else if (r < 0.34f) Deco.BUSH else if (r < 0.37f) Deco.BERRYBUSH else if (r < 0.46f) Deco.TUFT else 0
                T.GRASS -> dc = if (r < 0.025f * (0.4f + clump * 1.4f)) Deco.TREE else if (r < 0.03f) Deco.BOULDER else if (r < 0.04f) Deco.BUSH else if (r < 0.045f) Deco.BERRYBUSH else if (r < 0.10f) Deco.FLOWER else if (r < 0.22f) Deco.TUFT else 0
                T.SAND -> dc = if (r < 0.012f) Deco.CACTUS else if (r < 0.016f) Deco.BOULDER else if (r < 0.03f) Deco.TUFT else 0
                T.SWAMP -> dc = if (r < 0.07f * (0.5f + clump)) Deco.DEADTREE else if (r < 0.2f) Deco.TUFT else 0
                T.SNOW -> dc = if (r < 0.09f * (0.4f + clump)) Deco.PINE else if (r < 0.11f) Deco.BOULDER else 0
                T.ROCK -> dc = if (r < 0.04f) Deco.BOULDER else 0
                T.ASH -> dc = if (r < 0.035f) Deco.BOULDER else 0
                T.DIRT -> dc = if (r < 0.06f) Deco.TUFT else 0
            }
            deco[i] = dc.toByte()
        }
    }

    private fun placePods() {
        val rnd = Random(seed.toLong() * 104729L + 7L)
        var tries = 0
        while (pods.size < 110 && tries < 20000) {
            tries++
            val x = 8 + rnd.nextInt(N - 16); val y = 8 + rnd.nextInt(N - 16)
            val i = y * N + x
            val dd = Math.hypot((x - startX).toDouble(), (y - startY).toDouble())
            if (dd < 35.0 || !reach[i] || !T.buildable(ground[i].toInt()) || nodeAt[i].toInt() != 0) continue
            var near = false
            for (p in pods) if (Math.abs(p.x - x) < 14 && Math.abs(p.y - y) < 14) { near = true; break }
            if (near) continue
            deco[i] = 0
            pods.add(Pod(x, y))
        }
    }

    fun podAt(x: Int, y: Int): Pod? { for (p in pods) if (p.x == x && p.y == y) return p; return null }
}
