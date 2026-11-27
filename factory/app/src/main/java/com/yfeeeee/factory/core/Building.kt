// Pixel Factory - core: building logic (belts, machines, miners, containers). Developer: @yfeeeee
package com.yfeeeee.factory.core

const val SPACING = 0.34f
val ITEMS: Array<Item> = Item.values()
val NITEMS: Int = ITEMS.size

class Building(val id: Int, val def: Bld, var x: Int, var y: Int, var rot: Int) {
    val w: Int get() = if (rot % 2 == 0) def.w else def.h
    val h: Int get() = if (rot % 2 == 0) def.h else def.w
    val cx: Float get() = x + w / 2f
    val cy: Float get() = y + h / 2f

    // belts / tunnel queues
    var ci = IntArray(0)
    var cp = FloatArray(0)
    var cn = 0
    var shape = 0              // conveyor: 0 straight, 1 corner (feeds from left), 2 corner (feeds from right)
    // storage / buffers
    var inv = IntArray(0)
    var outb = IntArray(0)
    var recipe: Recipe? = null
    var filter = -1            // sorter filter (item ordinal) / unused
    var progress = 0f
    var crafting = false
    var active = false         // consumed power last tick
    var rr = 0
    var slot = -1              // router slot (item ordinal)
    var slotDir = 0            // direction router received from
    var link = -1              // tunnel partner id (entrance->exit)
    var isExit = false
    var node: Node? = null
    var burn = 0f              // generator: remaining MJ of current fuel
    var charge = 0f            // battery
    var avail = false          // generator currently supplies power
    var outPower = 0f
    var net = -1
    var pole = -1
    var wants = false
    var ratio = 0f             // power satisfaction of my network
    var anim = 0f
    var timer = 0f
    var perim = IntArray(0)

    init {
        when (def.kind) {
            Kind.CONVEYOR -> { ci = IntArray(3); cp = FloatArray(3) }
            Kind.TUNNEL -> { ci = IntArray(4); cp = FloatArray(4) }
            Kind.MACHINE, Kind.MINER, Kind.PUMP -> { inv = IntArray(NITEMS); outb = IntArray(NITEMS) }
            Kind.CONTAINER, Kind.GENERATOR -> { inv = IntArray(NITEMS) }
            else -> {}
        }
        if (def.kind == Kind.MACHINE || def.kind == Kind.MINER || def.kind == Kind.PUMP || def.kind == Kind.CONTAINER) buildPerim()
    }

    private fun buildPerim() {
        val list = ArrayList<Int>()
        for (i in 0 until w) { list.add(x + i); list.add(y); list.add(x + i); list.add(y - 1) }
        for (i in 0 until h) { list.add(x + w - 1); list.add(y + i); list.add(x + w); list.add(y + i) }
        for (i in w - 1 downTo 0) { list.add(x + i); list.add(y + h - 1); list.add(x + i); list.add(y + h) }
        for (i in h - 1 downTo 0) { list.add(x); list.add(y + i); list.add(x - 1); list.add(y + i) }
        perim = list.toIntArray()
    }

    fun contains(tx: Int, ty: Int): Boolean = tx >= x && ty >= y && tx < x + w && ty < y + h
    fun frontX(): Int = x + DX[rot]
    fun frontY(): Int = y + DY[rot]

    fun invCount(): Int { var s = 0; for (v in inv) s += v; return s }
    fun outCount(): Int { var s = 0; for (v in outb) s += v; return s }

    fun slotsUsed(): Int {
        var s = 0
        for (i in 0 until NITEMS) if (inv[i] > 0) s += (inv[i] + ITEMS[i].stack - 1) / ITEMS[i].stack
        return s
    }

    // ---------- accepting items ----------
    fun accept(g: Game, item: Item, fx: Int, fy: Int): Boolean {
        when (def.kind) {
            Kind.CONVEYOR -> {
                if (fx == frontX() && fy == frontY()) return false
                if (cn >= 3) return false
                if (cn > 0 && cp[cn - 1] < SPACING) return false
                ci[cn] = item.ordinal; cp[cn] = 0f; cn++
                return true
            }
            Kind.TUNNEL -> {
                if (isExit || link < 0) return false
                if (!(fx == x - DX[rot] && fy == y - DY[rot])) return false
                if (cn >= 4) return false
                val ex = g.byId(link)
                val dist = if (ex == null) 1 else Math.max(Math.abs(ex.x - x), Math.abs(ex.y - y))
                ci[cn] = item.ordinal; cp[cn] = 0.5f + 0.12f * dist; cn++
                return true
            }
            Kind.SPLITTER, Kind.MERGER -> {
                if (slot >= 0) return false
                if (fx == frontX() && fy == frontY() && def.kind == Kind.MERGER) return false
                slot = item.ordinal
                slotDir = dirFrom(fx, fy)
                return true
            }
            Kind.SORTER -> {
                if (slot >= 0) return false
                if (!(fx == x - DX[rot] && fy == y - DY[rot])) return false
                slot = item.ordinal; slotDir = (rot + 2) % 4
                return true
            }
            Kind.CONTAINER -> {
                val i = item.ordinal
                if (inv[i] % item.stack == 0 && slotsUsed() >= 16) return false
                inv[i]++
                return true
            }
            Kind.MACHINE -> {
                val r = recipe
                if (r == null) {
                    if (def.mach == Mach.SMELTER) {
                        for (rc in Recipes.forMach(Mach.SMELTER)) if (rc.accepts(item) && g.progress.canMake(rc)) { recipe = rc; break }
                    }
                    if (recipe == null) return false
                }
                val rc = recipe!!
                val need = rc.needOf(item)
                if (need == 0) return false
                if (inv[item.ordinal] >= need * 2 + 2) return false
                inv[item.ordinal]++
                return true
            }
            Kind.GENERATOR -> {
                if (!def.fuels().contains(item)) return false
                if (inv[item.ordinal] >= 40) return false
                inv[item.ordinal]++
                return true
            }
            Kind.HUB -> {
                val m = g.activeHubMilestone() ?: return false
                if (g.progress.need(m, item) <= 0) return false
                g.progress.deliver(m, item, 1)
                g.checkMilestone(m)
                return true
            }
            Kind.ELEVATOR -> {
                val m = g.activeElevatorMilestone() ?: return false
                if (g.progress.need(m, item) <= 0) return false
                g.progress.deliver(m, item, 1)
                g.checkMilestone(m)
                return true
            }
            Kind.MAM -> {
                if (item == Item.HARD_DRIVE) { g.mamDrives++; return true }
                return false
            }
            else -> return false
        }
    }

    fun dirFrom(fx: Int, fy: Int): Int {
        for (k in 0 until 4) if (fx == x + DX[k] && fy == y + DY[k]) return k
        return 0
    }

    // ---------- helper: push an item to a neighbour tile ----------
    private fun pushTo(g: Game, tx: Int, ty: Int, item: Item, fx: Int, fy: Int): Boolean {
        val n = g.occAt(tx, ty) ?: return false
        if (n === this) return false
        return n.accept(g, item, fx, fy)
    }

    private fun tryOutput(g: Game, item: Item): Boolean {
        val n = perim.size / 4
        if (n == 0) return false
        for (k in 0 until n) {
            val idx = ((k + rr) % n) * 4
            val nb = g.occAt(perim[idx + 2], perim[idx + 3]) ?: continue
            if (nb === this) continue
            if (nb.accept(g, item, perim[idx], perim[idx + 1])) { rr = (rr + k + 1) % n; return true }
        }
        return false
    }

    // ---------- update ----------
    fun update(g: Game, dt: Float) {
        when (def.kind) {
            Kind.CONVEYOR -> updConveyor(g, dt)
            Kind.TUNNEL -> updTunnel(g, dt)
            Kind.SPLITTER, Kind.MERGER, Kind.SORTER -> updRouter(g, dt)
            Kind.CONTAINER -> updContainer(g, dt)
            Kind.MINER, Kind.PUMP -> updMiner(g, dt)
            Kind.MACHINE -> updMachine(g, dt)
            else -> {}
        }
    }

    private fun updConveyor(g: Game, dt: Float) {
        if (cn == 0) return
        val sp = def.v * dt
        var limit = 1f
        for (i in 0 until cn) {
            var np = cp[i] + sp
            if (np > limit) np = limit
            cp[i] = np
            limit = np - SPACING
        }
        if (cp[0] >= 0.999f) {
            if (pushTo(g, frontX(), frontY(), ITEMS[ci[0]], x, y)) {
                for (i in 1 until cn) { ci[i - 1] = ci[i]; cp[i - 1] = cp[i] }
                cn--
            }
        }
    }

    private fun updTunnel(g: Game, dt: Float) {
        if (isExit || link < 0 || cn == 0) return
        for (i in 0 until cn) if (cp[i] > 0f) cp[i] -= dt
        if (cp[0] <= 0f) {
            val ex = g.byId(link) ?: return
            if (pushTo(g, ex.frontX(), ex.frontY(), ITEMS[ci[0]], ex.x, ex.y)) {
                for (i in 1 until cn) { ci[i - 1] = ci[i]; cp[i - 1] = cp[i] }
                cn--
            }
        }
    }

    private fun updRouter(g: Game, dt: Float) {
        if (slot < 0) return
        val item = ITEMS[slot]
        for (t in 0 until 3) {
            val k = (rr + t) % 3
            // candidate directions are the three not equal to slotDir
            var dirs = 0
            var chosen = -1
            for (d in 0 until 4) {
                if (d == slotDir) continue
                if (dirs == k) { chosen = d; break }
                dirs++
            }
            if (chosen < 0) continue
            if (def.kind == Kind.MERGER && chosen != rot) continue
            if (def.kind == Kind.SORTER) {
                val match = filter >= 0 && filter == slot
                val forward = chosen == rot
                if (filter >= 0 && match != forward) continue
                if (filter < 0 && !forward) continue
            }
            if (pushTo(g, x + DX[chosen], y + DY[chosen], item, x, y)) { slot = -1; rr = (k + 1) % 3; return }
        }
    }

    private fun updContainer(g: Game, dt: Float) {
        timer -= dt
        if (timer > 0f) return
        timer = 0.22f
        for (i in 0 until NITEMS) {
            val k = (i + rr) % NITEMS
            if (inv[k] > 0) {
                if (tryOutput(g, ITEMS[k])) { inv[k]--; rr = k + 1; return }
            }
        }
    }

    private fun updMiner(g: Game, dt: Float) {
        val nd = node
        wants = false
        active = false
        if (nd != null) {
            val it = Res.item(nd.type)
            if (it != null && outCount() < 6) {
                wants = true
            }
            if (it != null && wants && ratio > 0f) {
                active = true
                val rate = 0.5f * def.v * Res.purityMul(nd.purity)
                progress += rate * ratio * dt
                anim += dt * ratio
                if (progress >= 1f) { progress -= 1f; outb[it.ordinal]++ }
            }
        }
        if (outCount() > 0) {
            for (i in 0 until NITEMS) if (outb[i] > 0) { if (tryOutput(g, ITEMS[i])) outb[i]--; break }
        }
    }

    private fun updMachine(g: Game, dt: Float) {
        val r = recipe
        active = false
        wants = false
        if (r != null) {
            if (!crafting) {
                var ok = outCount() < 24
                if (ok) for (p in r.ins) if (inv[p.first.ordinal] < p.second) { ok = false; break }
                if (ok) {
                    for (p in r.ins) inv[p.first.ordinal] -= p.second
                    crafting = true; progress = 0f
                }
            }
            if (crafting) {
                wants = true
                if (ratio > 0f) {
                    active = true
                    progress += dt * ratio
                    anim += dt * ratio
                    if (progress >= r.time) {
                        for (p in r.outs) outb[p.first.ordinal] += p.second
                        crafting = false; progress = 0f
                    }
                }
            }
        }
        if (outCount() > 0) {
            for (i in 0 until NITEMS) if (outb[i] > 0) { if (tryOutput(g, ITEMS[i])) outb[i]--; break }
        }
    }
}
