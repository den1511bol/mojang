// Developer: @yfeeeee
package com.yfeeeee.mahjong

import kotlin.math.abs
import kotlin.random.Random

const val COLS = 6   // чётное число - раскладка симметрична, плиток всегда чётное количество
const val ROWS = 8
const val TYPES = 34 // 9 кружков, 9 бамбука, 9 иероглифов, 4 ветра, 3 дракона

/** Позиция плитки: x и y в половинках плитки, z - слой. */
data class Pos(val x: Int, val y: Int, val z: Int)

class LevelData(val positions: List<Pos>, val types: List<Int>)

fun typeCountFor(level: Int): Int = (6 + level * 2).coerceAtMost(TYPES)

/** Плитка свободна, если сверху ничего нет и свободна левая или правая сторона. */
fun isFree(i: Int, ps: List<Pos>, alive: (Int) -> Boolean): Boolean {
    val a = ps[i]
    var left = false
    var right = false
    for (j in ps.indices) {
        if (j == i || !alive(j)) continue
        val b = ps[j]
        if (abs(a.y - b.y) >= 2) continue
        if (b.z > a.z && abs(a.x - b.x) < 2) return false
        if (b.z == a.z) {
            if (b.x == a.x - 2) left = true
            if (b.x == a.x + 2) right = true
        }
    }
    return !(left && right)
}

/** Процедурная раскладка: чем выше уровень, тем плотнее и выше "башня". */
fun generateLayout(level: Int, rnd: Random): List<Pos> {
    val density = (0.5 + level * 0.04).coerceAtMost(0.9)
    val layers = (1 + level / 3).coerceAtMost(4)
    val result = mutableListOf<Pos>()
    var prev: Set<Pair<Int, Int>>? = null
    for (z in 0 until layers) {
        val cur = mutableSetOf<Pair<Int, Int>>()
        val p = if (z == 0) density else 0.55
        for (r in 0 until ROWS) for (c in 0 until COLS / 2) {
            if (prev != null && (c to r) !in prev) continue
            if (rnd.nextDouble() < p) {
                cur.add(c to r)
                cur.add((COLS - 1 - c) to r)
            }
        }
        if (cur.isEmpty()) break
        cur.forEach { (c, r) -> result.add(Pos(c * 2, r * 2, z)) }
        prev = cur
    }
    return result
}

/**
 * Решаемая раздача: "разбираем" поле в обратном порядке - берём две свободные плитки,
 * даём им одинаковый символ и убираем. Так всегда есть путь до полной очистки.
 */
fun assignTypes(ps: List<Pos>, alive: List<Int>, typeCount: Int, rnd: Random): Map<Int, Int>? {
    val remaining = alive.toMutableSet()
    val bag = (0 until TYPES).shuffled(rnd).take(typeCount)
    val out = HashMap<Int, Int>()
    var k = 0
    while (remaining.isNotEmpty()) {
        val free = remaining.filter { isFree(it, ps) { j -> j in remaining } }
        if (free.size < 2) return null
        val pick = free.shuffled(rnd).take(2)
        val t = bag[k % bag.size]
        k++
        out[pick[0]] = t
        out[pick[1]] = t
        remaining.removeAll(pick.toSet())
    }
    return out
}

fun generateLevel(level: Int, attempt: Int): LevelData {
    var s = 0
    while (true) {
        val ps = generateLayout(level, Random(level * 7919L + s * 104729L + 13))
        if (ps.size >= 12) {
            val m = assignTypes(ps, ps.indices.toList(), typeCountFor(level), Random(level * 31L + attempt * 17L + s))
            if (m != null) return LevelData(ps, ps.indices.map { m.getValue(it) })
        }
        s++
    }
}

/** Ищет пару свободных одинаковых плиток. */
fun findHint(ps: List<Pos>, types: List<Int>, removed: List<Boolean>): Pair<Int, Int>? {
    val free = ps.indices.filter { !removed[it] && isFree(it, ps) { j -> !removed[j] } }
    for (a in free) for (b in free) {
        if (a < b && types[a] == types[b]) return a to b
    }
    return null
}
