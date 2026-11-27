// Pixel Factory - core: save / load. Developer: @yfeeeee
package com.yfeeeee.factory.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

object Save {
    const val VERSION = 1

    fun write(g: Game, os: OutputStream) {
        val defl = DeflaterOutputStream(os)
        val out = DataOutputStream(defl)
        out.writeInt(0x50464143)
        out.writeInt(VERSION)
        out.writeInt(g.seed)
        out.writeFloat(g.time); out.writeInt(g.days); out.writeFloat(g.playTime)
        out.writeInt(g.mamDrives); out.writeFloat(g.mamTimer); out.writeBoolean(g.mamBusy)
        out.writeInt(g.pendingAlts.size); for (k in g.pendingAlts) out.writeUTF(k)
        out.writeInt(g.nextId)
        out.writeInt(g.killCount)
        val p = g.player
        out.writeFloat(p.x); out.writeFloat(p.y); out.writeInt(p.dir); out.writeFloat(p.hp)
        out.writeInt(p.slots); out.writeFloat(p.speedMul); out.writeInt(p.deaths)
        out.writeInt(NITEMS)
        for (i in 0 until NITEMS) out.writeInt(p.inv[i])
        val pr = g.progress
        out.writeInt(pr.done.size); for (k in pr.done) out.writeUTF(k)
        out.writeInt(pr.delivered.size)
        for ((k, v) in pr.delivered) { out.writeUTF(k); out.writeInt(v.size); for (x in v) out.writeInt(x) }
        out.writeInt(pr.altRecipes.size); for (k in pr.altRecipes) out.writeUTF(k)
        out.writeUTF(pr.active); out.writeBoolean(pr.won)
        out.writeInt(g.buildings.size)
        for (b in g.buildings) {
            out.writeInt(b.id); out.writeInt(b.def.ordinal); out.writeInt(b.x); out.writeInt(b.y); out.writeInt(b.rot)
            out.writeUTF(b.recipe?.key ?: "")
            out.writeInt(b.filter)
            out.writeFloat(b.progress); out.writeBoolean(b.crafting)
            out.writeInt(b.slot); out.writeInt(b.slotDir); out.writeInt(b.link); out.writeBoolean(b.isExit)
            out.writeFloat(b.burn); out.writeFloat(b.charge); out.writeInt(b.rr)
            out.writeInt(b.cn)
            for (i in 0 until b.cn) { out.writeInt(b.ci[i]); out.writeFloat(b.cp[i]) }
            var n = 0
            for (v in b.inv) if (v > 0) n++
            out.writeInt(n)
            for (i in b.inv.indices) if (b.inv[i] > 0) { out.writeInt(i); out.writeInt(b.inv[i]) }
            n = 0
            for (v in b.outb) if (v > 0) n++
            out.writeInt(n)
            for (i in b.outb.indices) if (b.outb[i] > 0) { out.writeInt(i); out.writeInt(b.outb[i]) }
        }
        out.write(g.world.deco)
        out.write(g.world.explored)
        out.writeInt(g.world.pods.size)
        for (pd in g.world.pods) out.writeBoolean(pd.opened)
        out.flush()
        defl.finish()
    }

    fun read(ins: InputStream): Game? {
        val inp = DataInputStream(InflaterInputStream(ins))
        if (inp.readInt() != 0x50464143) return null
        val ver = inp.readInt()
        if (ver != VERSION) return null
        val seed = inp.readInt()
        val g = Game(seed)
        g.world.generate()
        g.time = inp.readFloat(); g.days = inp.readInt(); g.playTime = inp.readFloat()
        g.mamDrives = inp.readInt(); g.mamTimer = inp.readFloat(); g.mamBusy = inp.readBoolean()
        val na = inp.readInt(); val alts = ArrayList<String>(); for (i in 0 until na) alts.add(inp.readUTF()); g.pendingAlts = alts
        val nextId = inp.readInt()
        g.killCount = inp.readInt()
        val p = g.player
        p.x = inp.readFloat(); p.y = inp.readFloat(); p.dir = inp.readInt(); p.hp = inp.readFloat()
        p.slots = inp.readInt(); p.speedMul = inp.readFloat(); p.deaths = inp.readInt()
        val ni = inp.readInt()
        for (i in 0 until ni) { val v = inp.readInt(); if (i < NITEMS) p.inv[i] = v }
        val pr = g.progress
        val nd = inp.readInt(); for (i in 0 until nd) pr.done.add(inp.readUTF())
        val ndl = inp.readInt()
        for (i in 0 until ndl) { val k = inp.readUTF(); val sz = inp.readInt(); val arr = IntArray(sz); for (j in 0 until sz) arr[j] = inp.readInt(); pr.delivered[k] = arr }
        val nr = inp.readInt(); for (i in 0 until nr) pr.altRecipes.add(inp.readUTF())
        pr.active = inp.readUTF(); pr.won = inp.readBoolean()
        val nb = inp.readInt()
        for (i in 0 until nb) {
            val id = inp.readInt(); val di = inp.readInt(); val x = inp.readInt(); val y = inp.readInt(); val rot = inp.readInt()
            val def = Bld.values()[di]
            val b = g.placeRaw(def, x, y, rot, id)
            val rk = inp.readUTF()
            if (rk.isNotEmpty()) b.recipe = Recipes.byKey(rk)
            b.filter = inp.readInt()
            b.progress = inp.readFloat(); b.crafting = inp.readBoolean()
            b.slot = inp.readInt(); b.slotDir = inp.readInt(); b.link = inp.readInt(); b.isExit = inp.readBoolean()
            b.burn = inp.readFloat(); b.charge = inp.readFloat(); b.rr = inp.readInt()
            b.cn = inp.readInt()
            for (j in 0 until b.cn) { val it = inp.readInt(); val pos = inp.readFloat(); if (j < b.ci.size) { b.ci[j] = it; b.cp[j] = pos } }
            var n = inp.readInt()
            for (j in 0 until n) { val k = inp.readInt(); val v = inp.readInt(); if (k < b.inv.size) b.inv[k] = v }
            n = inp.readInt()
            for (j in 0 until n) { val k = inp.readInt(); val v = inp.readInt(); if (k < b.outb.size) b.outb[k] = v }
            if (def.kind == Kind.HUB) g.hub = b
        }
        g.nextId = nextId
        inp.readFully(g.world.deco)
        inp.readFully(g.world.explored)
        val np = inp.readInt()
        for (i in 0 until np) { val o = inp.readBoolean(); if (i < g.world.pods.size) g.world.pods[i].opened = o }
        for (b in g.buildings) if (b.def.kind == Kind.CONVEYOR) g.refreshShapes(b.x, b.y)
        g.netDirty = true
        return g
    }
}
