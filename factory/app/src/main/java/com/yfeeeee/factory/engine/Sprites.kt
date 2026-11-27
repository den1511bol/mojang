// Pixel Factory - engine: sprite cache. Developer: @yfeeeee
package com.yfeeeee.factory.engine

import android.graphics.Bitmap
import com.yfeeeee.factory.core.Bld
import com.yfeeeee.factory.core.Item
import com.yfeeeee.factory.core.ITEMS

class Sprites {
    val ground = Array(12) { t -> Array(4) { v -> ArtWorld.ground(t, v, 0).toBitmap() } }
    val water = Array(2) { t -> Array(4) { f -> ArtWorld.ground(t, 0, f).toBitmap() } }
    val lava = Array(4) { f -> ArtWorld.ground(11, f, f).toBitmap() }
    val trees = Array(3) { ArtWorld.tree(it).toBitmap() }
    val pine = ArtWorld.pine().toBitmap()
    val dead = ArtWorld.deadTree().toBitmap()
    val cactus = ArtWorld.cactus().toBitmap()
    val boulders = Array(3) { t -> Array(2) { v -> ArtWorld.boulder(v, t).toBitmap() } }
    val bush = ArtWorld.bush(false).toBitmap()
    val berry = ArtWorld.bush(true).toBitmap()
    val flowers = Array(4) { ArtWorld.flower(it).toBitmap() }
    val tufts = Array(3) { ArtWorld.tuft(it).toBitmap() }
    val pods = arrayOf(ArtWorld.pod(false).toBitmap(), ArtWorld.pod(true).toBitmap())
    val items: Array<Bitmap> = Array(ITEMS.size) { ArtBuild.itemIcon(ITEMS[it].shape, ITEMS[it].color).toBitmap() }
    val projectile = ArtChars.proj().toBitmap()
    private val nodeCache = HashMap<Int, Bitmap>()
    private val bldCache = HashMap<Int, Bitmap>()
    private val beltCache = HashMap<Int, Bitmap>()
    private val playerCache = HashMap<Int, Bitmap>()
    private val creatureCache = HashMap<Int, Bitmap>()
    private val iconCache = HashMap<String, Bitmap>()

    fun node(type: Int, purity: Int, frame: Int): Bitmap {
        val k = type * 100 + purity * 10 + (frame and 1)
        var b = nodeCache[k]
        if (b == null) { b = ArtWorld.node(type, purity, frame and 1).toBitmap(); nodeCache[k] = b }
        return b!!
    }

    fun building(d: Bld, rot: Int, frame: Int): Bitmap {
        val animated = when (d.kind) {
            com.yfeeeee.factory.core.Kind.MINER, com.yfeeeee.factory.core.Kind.PUMP, com.yfeeeee.factory.core.Kind.MACHINE, com.yfeeeee.factory.core.Kind.GENERATOR,
            com.yfeeeee.factory.core.Kind.GEO, com.yfeeeee.factory.core.Kind.MAM, com.yfeeeee.factory.core.Kind.ELEVATOR, com.yfeeeee.factory.core.Kind.HUB -> true
            else -> false
        }
        val f = if (animated) frame and 3 else 0
        val r = if (d.kind == com.yfeeeee.factory.core.Kind.SPLITTER || d.kind == com.yfeeeee.factory.core.Kind.MERGER || d.kind == com.yfeeeee.factory.core.Kind.SORTER ||
            d.kind == com.yfeeeee.factory.core.Kind.TUNNEL || d.kind == com.yfeeeee.factory.core.Kind.CONTAINER) rot else 0
        val k = d.ordinal * 100 + r * 10 + f
        var b = bldCache[k]
        if (b == null) { b = ArtBuild.building(d, r, f).toBitmap(); bldCache[k] = b }
        return b!!
    }

    fun belt(mk: Int, rot: Int, shape: Int, frame: Int): Bitmap {
        val k = ((mk * 4 + rot) * 3 + shape) * 4 + (frame and 3)
        var b = beltCache[k]
        if (b == null) { b = ArtBuild.belt(mk, rot, shape, frame and 3).toBitmap(); beltCache[k] = b }
        return b!!
    }

    fun player(dir: Int, frame: Int): Bitmap {
        val k = dir * 4 + (frame and 3)
        var b = playerCache[k]
        if (b == null) { b = ArtChars.player(dir, frame and 3).toBitmap(); playerCache[k] = b }
        return b!!
    }

    fun creature(type: Int, frame: Int, flash: Boolean): Bitmap {
        val k = type * 10 + (frame and 1) * 2 + (if (flash) 1 else 0)
        var b = creatureCache[k]
        if (b == null) {
            val pb = when (type) { 0 -> ArtChars.hog(frame and 1, flash); 1 -> ArtChars.spitter(frame and 1, flash); else -> ArtChars.stinger(frame and 1, flash) }
            b = pb.toBitmap(); creatureCache[k] = b
        }
        return b!!
    }

    fun icon(name: String): Bitmap {
        var b = iconCache[name]
        if (b == null) { b = ArtChars.icon(name).toBitmap(); iconCache[name] = b }
        return b!!
    }

    fun item(i: Item): Bitmap = items[i.ordinal]
}
