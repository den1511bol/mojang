// Pixel Factory - core: game state and simulation. Developer: @yfeeeee
package com.yfeeeee.factory.core

import java.util.Random

class Toast(val msg: String) { var t = 3.6f }

class Creature(val type: Int, var x: Float, var y: Float) {
    var hp = CT.hp[type]
    var cd = 0f
    var wx = 0f; var wy = 0f; var wt = 0f
    var flash = 0f
    var faceX = 1f
    var anim = 0f
    var stuck = 0f
}

class Proj(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    var life = 2.2f
    var dmg = 6f
}

object CT {
    const val HOG = 0; const val SPITTER = 1; const val STINGER = 2
    val hp = floatArrayOf(40f, 32f, 150f)
    val speed = floatArrayOf(2.2f, 1.6f, 3.0f)
    val dmg = floatArrayOf(8f, 7f, 22f)
    val aggro = floatArrayOf(7f, 10f, 11f)
    fun name(t: Int, en: Boolean): String = when (t) {
        HOG -> if (en) "Hog" else "Кабан"; SPITTER -> if (en) "Spitter" else "Плевун"; else -> if (en) "Stinger" else "Жалоносец"
    }
}

class PowerNet {
    val gens = ArrayList<Building>()
    val cons = ArrayList<Building>()
    val bats = ArrayList<Building>()
    var cap = 0f
    var demand = 0f
    var ratio = 1f
    var batCharge = 0f
    var batMax = 0f
}

class Player {
    var x = 0f; var y = 0f
    var dir = 1
    var hp = 100f
    val maxHp = 100f
    val inv = IntArray(NITEMS)
    var slots = 24
    var speedMul = 1f
    var sprint = false
    var tkind = 0           // 0 none, 1 node, 2 deco, 3 pod, 4 building, 5 creature, 6 goal
    var tx = 0; var ty = 0
    var tcre: Creature? = null
    var actT = 0f
    var actMax = 1f
    var craft: Recipe? = null
    var craftT = 0f
    var craftQty = 0
    var atkCd = 0f
    var hurt = 0f
    var anim = 0f
    var moving = false
    var stuck = 0f
    var deaths = 0

    fun count(i: Item): Int = inv[i.ordinal]
    fun slotsUsed(): Int {
        var s = 0
        for (i in 0 until NITEMS) if (inv[i] > 0) s += (inv[i] + ITEMS[i].stack - 1) / ITEMS[i].stack
        return s
    }
    fun room(item: Item): Int {
        val cur = inv[item.ordinal]
        val st = item.stack
        val partial = if (cur % st == 0) 0 else st - cur % st
        val free = slots - slotsUsed()
        return partial + Math.max(0, free) * st
    }
    fun give(item: Item, n: Int): Int {
        val k = Math.min(n, room(item))
        if (k > 0) inv[item.ordinal] += k
        return k
    }
    fun take(item: Item, n: Int): Boolean {
        if (inv[item.ordinal] < n) return false
        inv[item.ordinal] -= n
        return true
    }
}

class Game(val seed: Int) {
    val world = World(seed)
    val N = world.N
    val player = Player()
    val progress = Progress()
    val buildings = ArrayList<Building>()
    private val idMap = HashMap<Int, Building>()
    val occ = arrayOfNulls<Building>(N * N)
    var nextId = 1
    val creatures = ArrayList<Creature>()
    val projs = ArrayList<Proj>()
    val toasts = ArrayList<Toast>()
    var time = 0.32f
    var days = 1
    var playTime = 0f
    var en = false
    var hub: Building? = null
    var mamDrives = 0
    var mamTimer = 0f
    var mamBusy = false
    var pendingAlts = ArrayList<String>()
    var nets = ArrayList<PowerNet>()
    var netDirty = true
    var totalCap = 0f
    var totalDemand = 0f
    var totalBat = 0f
    var totalBatMax = 0f
    var openBuilding: Building? = null   // set by tap, consumed by UI
    var openPodLoot: String = ""
    var rnd = Random(seed.toLong())
    var spawnT = 0f
    var autosaveT = 0f
    var shake = 0f
    var killCount = 0
    var tick = 0L

    fun tr(ru: String, e: String): String = if (en) e else ru
    fun toast(s: String) { toasts.add(Toast(s)); if (toasts.size > 5) toasts.removeAt(0) }

    // ---------------- setup ----------------
    fun newGame() {
        world.generate()
        val h = placeRaw(Bld.HUB, world.startX - 2, world.startY - 2, 0)
        hub = h
        player.x = world.startX + 0.5f
        player.y = world.startY + 3.6f
        player.give(Item.IRON_PLATE, 40); player.give(Item.IRON_ROD, 30)
        player.give(Item.WIRE, 40); player.give(Item.BIOMASS, 40)
        world.reveal(player.x, player.y, 16)
        progress.active = ""
    }

    fun occAt(x: Int, y: Int): Building? = if (x < 0 || y < 0 || x >= N || y >= N) null else occ[y * N + x]
    fun byId(id: Int): Building? = idMap[id]
    fun daylight(): Float {
        val v = 0.5 + 0.95 * Math.sin(2.0 * Math.PI * (time - 0.25))
        return if (v < 0.0) 0f else if (v > 1.0) 1f else v.toFloat()
    }

    fun activeHubMilestone(): Milestone? {
        val m = Research.byId(progress.active) ?: return null
        if (m.elevator || !progress.available(m)) return null
        return m
    }

    fun activeElevatorMilestone(): Milestone? {
        for (m in Research.list) if (m.elevator && progress.available(m)) return m
        return null
    }

    fun checkMilestone(m: Milestone) {
        if (progress.done.contains(m.id)) return
        if (!progress.complete(m)) return
        progress.done.add(m.id)
        if (progress.active == m.id) progress.active = ""
        toast(tr("Веха завершена: ", "Milestone complete: ") + m.name(en))
        shake = 0.3f
        when (m.perk) {
            "slots10" -> { player.slots += 10 }
            "speed" -> { player.speedMul *= 1.25f }
        }
        if (m.id == "elev5") { progress.won = true; toast(tr("Космический лифт запущен! Вы победили!", "Space Elevator launched! You win!")) }
    }

    // ---------------- placement ----------------
    fun canAfford(d: Bld): Boolean {
        for (p in d.cost) if (player.count(p.first) < p.second) return false
        return true
    }

    fun footprintW(d: Bld, rot: Int): Int = if (rot % 2 == 0) d.w else d.h
    fun footprintH(d: Bld, rot: Int): Int = if (rot % 2 == 0) d.h else d.w

    /** returns 0 if ok, otherwise a reason code (1 terrain/occupied, 2 needs node, 3 limit) */
    fun placeCheck(d: Bld, x: Int, y: Int, rot: Int): Int {
        val w = footprintW(d, rot); val h = footprintH(d, rot)
        var nodeFound: Node? = null
        for (ty in y until y + h) for (tx in x until x + w) {
            if (!world.inB(tx, ty)) return 1
            if (!T.buildable(world.g(tx, ty))) return 1
            if (occ[ty * N + tx] != null) return 1
            if (Deco.blocks(world.d(tx, ty))) return 1
            if (world.podAt(tx, ty) != null) return 1
            val nd = world.node(tx, ty)
            if (nd != null) {
                val minerOk = (d.kind == Kind.MINER && nd.type != Res.OIL && nd.type != Res.GEYSER) ||
                    (d.kind == Kind.PUMP && nd.type == Res.OIL) || (d.kind == Kind.GEO && nd.type == Res.GEYSER)
                if (!minerOk) return 1
                if (nd.minerId >= 0) return 1
                if (nodeFound == null) nodeFound = nd else if (nodeFound !== nd) return 1
            }
        }
        if ((d.kind == Kind.MINER || d.kind == Kind.PUMP || d.kind == Kind.GEO) && nodeFound == null) return 2
        if (d.kind == Kind.ELEVATOR) for (b in buildings) if (b.def == Bld.ELEVATOR) return 3
        if (d.kind == Kind.HUB) return 3
        return 0
    }

    fun placeRaw(d: Bld, x: Int, y: Int, rot: Int, forceId: Int = -1): Building {
        val b = Building(if (forceId >= 0) forceId else nextId++, d, x, y, rot)
        buildings.add(b); idMap[b.id] = b
        for (ty in y until y + b.h) for (tx in x until x + b.w) {
            occ[ty * N + tx] = b
            val dc = world.d(tx, ty)
            if (Deco.soft(dc)) world.deco[ty * N + tx] = 0
            if (d.kind == Kind.MINER || d.kind == Kind.PUMP || d.kind == Kind.GEO) {
                val nd = world.node(tx, ty)
                if (nd != null && b.node == null) { b.node = nd; nd.minerId = b.id }
            }
        }
        if (d.kind == Kind.TUNNEL && forceId < 0) linkTunnel(b)
        refreshShapes(x, y)
        netDirty = true
        return b
    }

    fun place(d: Bld, x: Int, y: Int, rot: Int): Building? {
        if (!progress.canBuild(d)) return null
        if (placeCheck(d, x, y, rot) != 0) return null
        if (!canAfford(d)) return null
        for (p in d.cost) player.take(p.first, p.second)
        return placeRaw(d, x, y, rot)
    }

    /** conveyor painting helper: place or re-orient */
    fun placeConveyor(d: Bld, x: Int, y: Int, rot: Int): Boolean {
        val ex = occAt(x, y)
        if (ex != null && (ex.def.kind == Kind.CONVEYOR)) {
            if (ex.def == d && ex.rot == rot) return false
            if (ex.def != d) {
                if (!canAffordDiff(d)) return false
                for (p in d.cost) player.take(p.first, p.second)
                for (p in ex.def.cost) player.give(p.first, p.second)
                val nb = Building(ex.id, d, x, y, rot)
                nb.cn = ex.cn
                for (i in 0 until ex.cn) { nb.ci[i] = ex.ci[i]; nb.cp[i] = ex.cp[i] }
                val idx = buildings.indexOf(ex)
                buildings[idx] = nb; idMap[nb.id] = nb; occ[y * N + x] = nb
                refreshShapes(x, y); netDirty = true
                return true
            }
            ex.rot = rot
            refreshShapes(x, y)
            return true
        }
        return place(d, x, y, rot) != null
    }

    private fun canAffordDiff(d: Bld): Boolean = canAfford(d)

    fun remove(b: Building) {
        if (b.def.kind == Kind.HUB) { toast(tr("ХАБ нельзя убрать", "The HUB can't be removed")); return }
        for (p in b.def.cost) player.give(p.first, p.second)
        if (b.inv.isNotEmpty()) for (i in 0 until NITEMS) if (b.inv[i] > 0) player.give(ITEMS[i], b.inv[i])
        if (b.outb.isNotEmpty()) for (i in 0 until NITEMS) if (b.outb[i] > 0) player.give(ITEMS[i], b.outb[i])
        for (i in 0 until b.cn) if (b.def.kind == Kind.CONVEYOR) player.give(ITEMS[b.ci[i]], 1)
        for (ty in b.y until b.y + b.h) for (tx in b.x until b.x + b.w) occ[ty * N + tx] = null
        buildings.remove(b); idMap.remove(b.id)
        b.node?.minerId = -1
        if (b.def.kind == Kind.TUNNEL) {
            for (o in buildings) if (o.def.kind == Kind.TUNNEL && o.link == b.id) { o.link = -1 }
        }
        if (openBuilding === b) openBuilding = null
        refreshShapes(b.x, b.y)
        netDirty = true
    }

    private fun linkTunnel(b: Building) {
        if (b.link >= 0 || b.isExit) return
        // search backward for an unlinked entrance with same rotation
        for (k in 1..6) {
            val ox = b.x - DX[b.rot] * k; val oy = b.y - DY[b.rot] * k
            val o = occAt(ox, oy) ?: continue
            if (o.def.kind == Kind.TUNNEL && o !== b && o.rot == b.rot && o.link < 0 && !o.isExit) {
                o.link = b.id; b.isExit = true; return
            }
        }
        for (k in 1..6) {
            val ox = b.x + DX[b.rot] * k; val oy = b.y + DY[b.rot] * k
            val o = occAt(ox, oy) ?: continue
            if (o.def.kind == Kind.TUNNEL && o !== b && o.rot == b.rot && o.link < 0 && !o.isExit) {
                b.link = o.id; o.isExit = true; return
            }
        }
    }

    fun refreshShapes(x: Int, y: Int) {
        for (dy in -1..1) for (dx in -1..1) {
            val b = occAt(x + dx, y + dy) ?: continue
            if (b.def.kind == Kind.CONVEYOR) computeShape(b)
        }
    }

    private fun feeds(n: Building?, tx: Int, ty: Int): Boolean {
        if (n == null) return false
        if (n.def.kind == Kind.CONVEYOR) return n.frontX() == tx && n.frontY() == ty
        if (n.def.kind == Kind.TUNNEL && n.isExit) return n.frontX() == tx && n.frontY() == ty
        return false
    }

    private fun computeShape(c: Building) {
        val r = c.rot
        val back = occAt(c.x - DX[r], c.y - DY[r])
        val lDir = (r + 3) % 4; val rDir = (r + 1) % 4
        val left = occAt(c.x + DX[lDir], c.y + DY[lDir])
        val right = occAt(c.x + DX[rDir], c.y + DY[rDir])
        val hasBack = feeds(back, c.x, c.y)
        val hasL = feeds(left, c.x, c.y)
        val hasR = feeds(right, c.x, c.y)
        c.shape = if (!hasBack && hasL && !hasR) 1 else if (!hasBack && hasR && !hasL) 2 else 0
    }

    // ---------------- power ----------------
    fun rebuildPower() {
        netDirty = false
        val poles = ArrayList<Building>()
        for (b in buildings) { b.net = -1; b.pole = -1; if (b.def.kind == Kind.POLE) poles.add(b) }
        val parent = IntArray(poles.size) { it }
        fun find(a: Int): Int { var r = a; while (parent[r] != r) r = parent[r]; var c = a; while (parent[c] != r) { val n = parent[c]; parent[c] = r; c = n }; return r }
        for (i in poles.indices) for (j in i + 1 until poles.size) {
            val dx = poles[i].x - poles[j].x; val dy = poles[i].y - poles[j].y
            if (dx * dx + dy * dy <= 100) { val a = find(i); val b = find(j); if (a != b) parent[a] = b }
        }
        val rootIdx = HashMap<Int, Int>()
        nets = ArrayList()
        for (i in poles.indices) {
            val r = find(i)
            var ni = rootIdx[r]
            if (ni == null) { ni = nets.size; rootIdx[r] = ni; nets.add(PowerNet()) }
            poles[i].net = ni
        }
        for (b in buildings) {
            val d = b.def
            val uses = d.needsPower || d.isGenerator || d.kind == Kind.BATTERY
            if (!uses) continue
            var best = -1; var bd = 64f
            for (i in poles.indices) {
                val dx = poles[i].x + 0.5f - b.cx; val dy = poles[i].y + 0.5f - b.cy
                val dd = dx * dx + dy * dy
                if (dd < bd) { bd = dd; best = i }
            }
            if (best >= 0) {
                b.net = poles[best].net; b.pole = poles[best].id
                val n = nets[b.net]
                if (d.isGenerator) n.gens.add(b) else if (d.kind == Kind.BATTERY) n.bats.add(b) else n.cons.add(b)
            }
        }
    }

    private fun genCapacity(b: Building): Float {
        return when (b.def.kind) {
            Kind.SOLAR -> b.def.power * daylight()
            Kind.GEO -> { val nd = b.node; if (nd != null) b.def.power * Res.purityMul(nd.purity) else 0f }
            else -> if (b.avail) b.def.power else 0f
        }
    }

    private fun updatePower(dt: Float) {
        if (netDirty) rebuildPower()
        totalCap = 0f; totalDemand = 0f; totalBat = 0f; totalBatMax = 0f
        for (b in buildings) {
            if (b.def.kind == Kind.GENERATOR) {
                // ensure fuel
                if (b.burn <= 0f) {
                    b.avail = false
                    for (f in b.def.fuels()) if (b.inv[f.ordinal] > 0) { b.inv[f.ordinal]--; b.burn = f.fuelValue; b.avail = true; break }
                } else b.avail = true
            }
        }
        for (n in nets) {
            var cap = 0f
            for (gb in n.gens) cap += genCapacity(gb)
            var demand = 0f
            for (c in n.cons) if (c.wants || c.def.kind == Kind.LAMP) demand += c.def.power
            var batC = 0f; var batM = 0f
            for (bb in n.bats) { batC += bb.charge; batM += bb.def.v }
            n.cap = cap; n.demand = demand; n.batCharge = batC; n.batMax = batM
            var ratio = 1f
            var load = 1f
            if (demand <= 0f) { ratio = 1f; load = 0f }
            else if (cap >= demand) { ratio = 1f; load = demand / cap }
            else {
                val deficit = demand - cap
                val avail = batC / dt
                val take = Math.min(deficit, avail)
                ratio = (cap + take) / demand
                load = 1f
                if (take > 0f && batC > 0f) { val f = (take * dt) / batC; for (bb in n.bats) bb.charge -= bb.charge * f }
            }
            if (cap > demand && batM > 0f) {
                var surplus = (cap - demand) * dt
                for (bb in n.bats) { val room = bb.def.v - bb.charge; val put = Math.min(room, surplus); bb.charge += put; surplus -= put }
            }
            n.ratio = ratio
            for (c in n.cons) c.ratio = ratio
            if (cap > 0f) for (gb in n.gens) {
                if (gb.def.kind == Kind.GENERATOR) {
                    val out = genCapacity(gb) * load
                    gb.outPower = out
                    gb.burn -= out * dt
                    gb.anim += dt
                } else gb.outPower = genCapacity(gb) * load
            }
            totalCap += cap; totalDemand += demand; totalBat += batC; totalBatMax += batM
        }
        for (b in buildings) {
            if (b.net < 0 && b.def.needsPower) b.ratio = 0f
            if (b.net < 0 && b.def.kind == Kind.GENERATOR) b.avail = false
        }
    }

    // ---------------- hand crafting ----------------
    fun canCraft(r: Recipe): Boolean {
        for (p in r.ins) if (player.count(p.first) < p.second) return false
        return true
    }

    fun startCraft(r: Recipe, qty: Int) {
        if (!progress.canMake(r)) return
        if (player.craft != null && player.craft !== r) return
        player.craft = r
        player.craftQty += qty
        if (player.craftT <= 0f) player.craftT = 0.01f
    }

    private fun updateCraft(dt: Float) {
        val r = player.craft ?: return
        if (player.craftQty <= 0) { player.craft = null; player.craftT = 0f; return }
        if (player.craftT == 0.01f || player.craftT < 0f) {
            // starting a new item
            if (!canCraft(r)) { toast(tr("Не хватает материалов", "Not enough materials")); player.craft = null; player.craftQty = 0; player.craftT = 0f; return }
            for (p in r.ins) player.take(p.first, p.second)
            player.craftT = r.time * 0.5f
        }
        player.craftT -= dt
        if (player.craftT <= 0f) {
            for (p in r.outs) {
                val got = player.give(p.first, p.second)
                if (got < p.second) toast(tr("Инвентарь полон!", "Inventory full!"))
            }
            player.craftQty--
            player.craftT = if (player.craftQty > 0) 0.01f else 0f
            if (player.craftQty <= 0) player.craft = null
        }
    }

    // ---------------- player & world interaction ----------------
    fun solidAt(tx: Int, ty: Int): Boolean {
        if (!world.walkableTile(tx, ty)) return true
        val b = occ[ty * N + tx]
        return b != null && b.def.solid
    }

    private fun blockedBox(px: Float, py: Float, r: Float): Boolean {
        val x0 = Math.floor((px - r).toDouble()).toInt(); val x1 = Math.floor((px + r).toDouble()).toInt()
        val y0 = Math.floor((py - r).toDouble()).toInt(); val y1 = Math.floor((py + r).toDouble()).toInt()
        for (ty in y0..y1) for (tx in x0..x1) if (solidAt(tx, ty)) return true
        return false
    }

    fun clearTarget() { player.tkind = 0; player.tcre = null; player.actT = 0f }

    fun movePlayer(ix: Float, iy: Float, dt: Float) {
        val p = player
        var vx = ix; var vy = iy
        val mag = Math.sqrt((vx * vx + vy * vy).toDouble()).toFloat()
        p.moving = false
        if (mag > 0.05f) {
            if (mag > 1f) { vx /= mag; vy /= mag }
            clearTarget()
            p.moving = true
        } else if (p.tkind != 0) {
            // auto-walk toward target if too far
            var gx = p.tx + 0.5f; var gy = p.ty + 0.5f
            val tc = p.tcre
            if (p.tkind == 5 && tc != null) { gx = tc.x; gy = tc.y }
            if (p.tkind == 4) { val b = occAt(p.tx, p.ty); if (b != null) { gx = b.cx; gy = b.cy } }
            val dx = gx - p.x; val dy = gy - p.y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val reach = reachFor()
            if (dist > reach) {
                vx = dx / dist; vy = dy / dist
                p.moving = true
            } else if (p.tkind == 6) clearTarget()
        }
        if (p.moving) {
            var sp = 3.6f * p.speedMul * (if (p.sprint) 1.6f else 1f)
            val g = world.g(Math.floor(p.x.toDouble()).toInt(), Math.floor(p.y.toDouble()).toInt())
            if (g == T.SHALLOW) sp *= 0.6f else if (g == T.SWAMP) sp *= 0.8f else if (g == T.SNOW) sp *= 0.9f
            val ox = p.x; val oy = p.y
            val nx = p.x + vx * sp * dt
            if (!blockedBox(nx, p.y, 0.28f)) p.x = nx
            val ny = p.y + vy * sp * dt
            if (!blockedBox(p.x, ny, 0.28f)) p.y = ny
            if (Math.abs(vx) > Math.abs(vy)) p.dir = if (vx > 0) 0 else 2 else p.dir = if (vy > 0) 1 else 3
            p.anim += dt * (if (p.sprint) 11f else 8f)
            val moved = Math.abs(p.x - ox) + Math.abs(p.y - oy)
            if (moved < sp * dt * 0.2f && p.tkind != 0) { p.stuck += dt; if (p.stuck > 1.2f) { clearTarget(); p.stuck = 0f } } else p.stuck = 0f
            if (tick % 6L == 0L) world.reveal(p.x, p.y, 13)
        } else p.stuck = 0f
    }

    private fun reachFor(): Float = when (player.tkind) {
        1 -> 2.6f; 2 -> 1.9f; 3 -> 1.9f; 4 -> 3.2f; 5 -> 1.5f; else -> 0.4f
    }

    /** tap on a world position (in tile coordinates, float) */
    fun tapWorld(fx: Float, fy: Float) {
        val tx = Math.floor(fx.toDouble()).toInt(); val ty = Math.floor(fy.toDouble()).toInt()
        // creature first
        var best: Creature? = null; var bd = 1.1f
        for (c in creatures) { val dx = c.x - fx; val dy = c.y - fy; val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat(); if (d < bd) { bd = d; best = c } }
        if (best != null) { player.tkind = 5; player.tcre = best; player.actT = 0f; return }
        val b = occAt(tx, ty)
        if (b != null) { player.tkind = 4; player.tx = tx; player.ty = ty; player.actT = 0f; return }
        if (world.podAt(tx, ty) != null && !world.podAt(tx, ty)!!.opened) { player.tkind = 3; player.tx = tx; player.ty = ty; player.actT = 0f; return }
        val nd = world.node(tx, ty)
        if (nd != null && Res.item(nd.type) != null) { player.tkind = 1; player.tx = tx; player.ty = ty; player.actT = 0f; return }
        val dc = world.d(tx, ty)
        if (dc != Deco.NONE && dc != Deco.FLOWER && dc != Deco.TUFT) { player.tkind = 2; player.tx = tx; player.ty = ty; player.actT = 0f; return }
        player.tkind = 6; player.tx = tx; player.ty = ty; player.tcre = null
    }

    private fun decoTime(dc: Int): Float = when (dc) {
        Deco.TREE, Deco.PINE, Deco.DEADTREE -> 2.2f
        Deco.BOULDER -> 3.0f
        Deco.CACTUS -> 1.0f
        else -> 0.8f
    }

    private fun doGather(tx: Int, ty: Int) {
        val dc = world.d(tx, ty)
        val i = ty * N + tx
        val p = player
        var full = false
        fun gv(item: Item, n: Int) { if (p.give(item, n) < n) full = true }
        when (dc) {
            Deco.TREE, Deco.PINE, Deco.DEADTREE -> { gv(Item.WOOD, 2 + rnd.nextInt(3)); if (dc != Deco.DEADTREE) gv(Item.LEAVES, 1 + rnd.nextInt(3)); world.deco[i] = 0 }
            Deco.BOULDER -> { gv(Item.LIMESTONE, 3 + rnd.nextInt(4)); if (rnd.nextInt(4) == 0) gv(Item.COAL, 1 + rnd.nextInt(2)); world.deco[i] = 0 }
            Deco.BUSH -> { gv(Item.LEAVES, 1 + rnd.nextInt(3)); world.deco[i] = 0 }
            Deco.BERRYBUSH -> { gv(Item.BERRIES, 2 + rnd.nextInt(3)); gv(Item.LEAVES, 1); world.deco[i] = Deco.BUSH.toByte() }
            Deco.CACTUS -> { gv(Item.LEAVES, 2); world.deco[i] = 0 }
        }
        if (full) toast(tr("Инвентарь полон!", "Inventory full!"))
        clearTarget()
    }

    fun openPod(pod: Pod) {
        pod.opened = true
        val d = Math.hypot((pod.x - world.startX).toDouble(), (pod.y - world.startY).toDouble())
        val sb = StringBuilder()
        player.give(Item.HARD_DRIVE, 1)
        sb.append(Item.HARD_DRIVE.name(en))
        val picks: Array<Item> = if (d < 90) arrayOf(Item.IRON_PLATE, Item.WIRE, Item.CONCRETE, Item.IRON_ROD, Item.SCREW)
        else if (d < 170) arrayOf(Item.REINFORCED_PLATE, Item.ROTOR, Item.STEEL_BEAM, Item.CABLE, Item.COPPER_SHEET)
        else arrayOf(Item.MOTOR, Item.CIRCUIT_BOARD, Item.MODULAR_FRAME, Item.COMPUTER, Item.ENCASED_BEAM)
        val n = if (d < 90) 20 else if (d < 170) 6 else 3
        for (k in 0 until 2) {
            val it = picks[rnd.nextInt(picks.size)]
            val amt = n / 2 + rnd.nextInt(n)
            player.give(it, amt)
            sb.append(", ").append(it.name(en)).append(" x").append(amt)
        }
        toast(tr("Капсула открыта: ", "Pod opened: ") + sb)
        shake = 0.2f
        clearTarget()
    }

    private fun updateTarget(dt: Float) {
        val p = player
        if (p.tkind == 0 || p.tkind == 6) return
        val tc = p.tcre
        var gx = p.tx + 0.5f; var gy = p.ty + 0.5f
        if (p.tkind == 5) { if (tc == null || tc.hp <= 0f) { clearTarget(); return }; gx = tc.x; gy = tc.y }
        if (p.tkind == 4) { val b = occAt(p.tx, p.ty); if (b == null) { clearTarget(); return }; gx = b.cx; gy = b.cy }
        val dx = gx - p.x; val dy = gy - p.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist > reachFor() + 0.15f) return
        if (Math.abs(dx) > Math.abs(dy)) p.dir = if (dx > 0) 0 else 2 else p.dir = if (dy > 0) 1 else 3
        when (p.tkind) {
            1 -> {
                val nd = world.node(p.tx, p.ty)
                if (nd == null) { clearTarget(); return }
                val it = Res.item(nd.type) ?: return
                p.actMax = 1.7f
                p.actT += dt
                p.anim += dt * 9f
                if (p.actT >= p.actMax) {
                    p.actT = 0f
                    if (p.give(it, 1) < 1) { toast(tr("Инвентарь полон!", "Inventory full!")); clearTarget() }
                }
            }
            2 -> {
                val dc = world.d(p.tx, p.ty)
                if (dc == Deco.NONE) { clearTarget(); return }
                p.actMax = decoTime(dc)
                p.actT += dt
                p.anim += dt * 9f
                if (p.actT >= p.actMax) doGather(p.tx, p.ty)
            }
            3 -> {
                val pod = world.podAt(p.tx, p.ty)
                if (pod == null || pod.opened) { clearTarget(); return }
                p.actMax = 1.8f
                p.actT += dt
                if (p.actT >= p.actMax) openPod(pod)
            }
            4 -> {
                val b = occAt(p.tx, p.ty)
                if (b != null) { openBuilding = b }
                clearTarget()
            }
            5 -> {
                if (tc != null && p.atkCd <= 0f) attackCreature(tc)
            }
        }
    }

    fun attackCreature(c: Creature) {
        val p = player
        if (p.atkCd > 0f) return
        p.atkCd = 0.5f
        c.hp -= 22f
        c.flash = 0.2f
        // knockback
        val dx = c.x - p.x; val dy = c.y - p.y
        val d = Math.max(0.1f, Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat())
        c.x += dx / d * 0.35f; c.y += dy / d * 0.35f
        if (c.hp <= 0f) {
            killCount++
            val n = 1 + rnd.nextInt(2) + c.type
            p.give(Item.REMAINS, n)
            if (rnd.nextInt(3) == 0) p.give(Item.LEAVES, 2)
            if (c.type == CT.STINGER && rnd.nextInt(3) == 0) p.give(Item.HARD_DRIVE, 1)
        }
    }

    /** attack button: hit everything in front */
    fun attackFront() {
        val p = player
        if (p.atkCd > 0f) return
        var hit = false
        for (c in creatures) {
            val dx = c.x - p.x; val dy = c.y - p.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (d < 1.7f) { attackCreature(c); hit = true; p.atkCd = 0.5f }
        }
        if (!hit) p.atkCd = 0.3f
        p.anim += 2f
    }

    fun eatBerries() {
        if (player.hp >= player.maxHp) return
        if (player.take(Item.BERRIES, 1)) player.hp = Math.min(player.maxHp, player.hp + 20f)
    }

    // ---------------- creatures ----------------
    private fun updateCreatures(dt: Float) {
        val p = player
        spawnT -= dt
        val dl = daylight()
        val cap = if (dl > 0.4f) 7 else 14
        if (spawnT <= 0f) {
            spawnT = 1.2f
            if (creatures.size < cap) trySpawn()
        }
        val it = creatures.iterator()
        while (it.hasNext()) {
            val c = it.next()
            if (c.hp <= 0f) { it.remove(); continue }
            val dx = p.x - c.x; val dy = p.y - c.y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist > 55f) { it.remove(); continue }
            c.flash = Math.max(0f, c.flash - dt)
            c.cd = Math.max(0f, c.cd - dt)
            var mx = 0f; var my = 0f
            val ag = CT.aggro[c.type]
            if (dist < ag && p.hp > 0f) {
                if (c.type == CT.SPITTER) {
                    if (dist > 6f) { mx = dx / dist; my = dy / dist } else if (dist < 3.5f) { mx = -dx / dist; my = -dy / dist }
                    if (c.cd <= 0f && dist < 9f) {
                        c.cd = 2.2f
                        val pr = Proj(c.x, c.y, dx / dist * 6f, dy / dist * 6f); pr.dmg = CT.dmg[c.type]
                        projs.add(pr)
                    }
                } else {
                    if (dist > 0.9f) { mx = dx / dist; my = dy / dist }
                    else if (c.cd <= 0f) { c.cd = if (c.type == CT.STINGER) 0.8f else 1.0f; hurtPlayer(CT.dmg[c.type]) }
                }
            } else {
                c.wt -= dt
                if (c.wt <= 0f) { c.wt = 1.5f + rnd.nextFloat() * 3f; val a = rnd.nextFloat() * 6.283f; c.wx = Math.cos(a.toDouble()).toFloat() * 0.4f; c.wy = Math.sin(a.toDouble()).toFloat() * 0.4f; if (rnd.nextInt(3) == 0) { c.wx = 0f; c.wy = 0f } }
                mx = c.wx; my = c.wy
            }
            val sp = CT.speed[c.type]
            if (mx != 0f || my != 0f) {
                c.anim += dt * 8f
                val nx = c.x + mx * sp * dt
                if (!blockedBox(nx, c.y, 0.3f)) c.x = nx
                val ny = c.y + my * sp * dt
                if (!blockedBox(c.x, ny, 0.3f)) c.y = ny
                if (mx != 0f) c.faceX = if (mx > 0) 1f else -1f
            }
        }
        val pi = projs.iterator()
        while (pi.hasNext()) {
            val pr = pi.next()
            pr.x += pr.vx * dt; pr.y += pr.vy * dt; pr.life -= dt
            val dx = p.x - pr.x; val dy = p.y - pr.y
            if (dx * dx + dy * dy < 0.25f) { hurtPlayer(pr.dmg); pr.life = 0f }
            if (solidAt(Math.floor(pr.x.toDouble()).toInt(), Math.floor(pr.y.toDouble()).toInt())) pr.life = 0f
            if (pr.life <= 0f) pi.remove()
        }
    }

    private fun trySpawn() {
        val p = player
        for (attempt in 0 until 6) {
            val a = rnd.nextFloat() * 6.283f
            val r = 18f + rnd.nextFloat() * 14f
            val x = p.x + Math.cos(a.toDouble()).toFloat() * r
            val y = p.y + Math.sin(a.toDouble()).toFloat() * r
            val tx = Math.floor(x.toDouble()).toInt(); val ty = Math.floor(y.toDouble()).toInt()
            if (!world.inB(tx, ty) || solidAt(tx, ty)) continue
            if (!world.reach[ty * N + tx]) continue
            val ds = Math.hypot((tx - world.startX).toDouble(), (ty - world.startY).toDouble()).toFloat()
            if (ds < 30f) continue
            var nearBuilding = false
            for (dy in -6..6) { for (dx in -6..6) { if (occAt(tx + dx, ty + dy) != null) { nearBuilding = true; break } }; if (nearBuilding) break }
            if (nearBuilding) continue
            val roll = rnd.nextFloat()
            val type = if (ds > 150f && roll < 0.35f) CT.STINGER else if (ds > 75f && roll < 0.6f) CT.SPITTER else CT.HOG
            creatures.add(Creature(type, x, y))
            return
        }
    }

    fun hurtPlayer(d: Float) {
        val p = player
        p.hp -= d
        p.hurt = 0.35f
        shake = Math.max(shake, 0.12f)
        if (p.hp <= 0f) {
            p.deaths++
            p.hp = p.maxHp
            val h = hub
            if (h != null) { p.x = h.cx; p.y = h.y + h.h + 1.2f }
            creatures.clear(); projs.clear()
            clearTarget()
            toast(tr("Вы погибли и возродились у ХАБа", "You died and respawned at the HUB"))
        }
    }

    // ---------------- main update ----------------
    fun update(dt: Float, ix: Float, iy: Float) {
        tick++
        playTime += dt
        time += dt / 600f
        if (time >= 1f) { time -= 1f; days++ }
        movePlayer(ix, iy, dt)
        val p = player
        p.atkCd = Math.max(0f, p.atkCd - dt)
        p.hurt = Math.max(0f, p.hurt - dt)
        if (p.hurt <= 0f && p.hp < p.maxHp) p.hp = Math.min(p.maxHp, p.hp + dt * 1.2f)
        updateTarget(dt)
        updateCraft(dt)
        for (b in buildings) b.update(this, dt)
        updatePower(dt)
        // MAM research
        if (mamBusy) {
            var powered = false
            for (b in buildings) if (b.def.kind == Kind.MAM) { b.wants = true; if (b.ratio > 0f) powered = true }
            if (powered) mamTimer -= dt
            if (mamTimer <= 0f) { mamBusy = false; rollAlts() }
        } else for (b in buildings) if (b.def.kind == Kind.MAM) b.wants = false
        for (b in buildings) if (b.def.kind == Kind.LAMP) b.wants = true
        updateCreatures(dt)
        for (t in toasts) t.t -= dt
        toasts.removeAll { it.t <= 0f }
        shake = Math.max(0f, shake - dt)
    }

    // ---------------- MAM ----------------
    fun startAnalysis(): Boolean {
        if (mamBusy || pendingAlts.isNotEmpty()) return false
        if (mamDrives <= 0) {
            if (player.take(Item.HARD_DRIVE, 1)) mamDrives++ else return false
        }
        mamDrives--
        mamBusy = true
        mamTimer = 25f
        return true
    }

    private fun rollAlts() {
        val pool = ArrayList<String>()
        for (r in Recipes.alts) if (!progress.altRecipes.contains(r.key)) pool.add(r.key)
        pool.shuffle(rnd)
        pendingAlts = ArrayList(pool.take(3))
        if (pendingAlts.isEmpty()) toast(tr("Все альтернативные рецепты изучены", "All alternate recipes researched"))
        else toast(tr("Анализ завершён: выберите рецепт в МАМ", "Analysis done: pick a recipe at the MAM"))
    }

    fun pickAlt(key: String) {
        if (pendingAlts.contains(key)) {
            progress.altRecipes.add(key)
            pendingAlts = ArrayList()
            toast(tr("Новый рецепт изучен", "New recipe researched"))
        }
    }

    fun deliverFromInventory(m: Milestone) {
        for (p in m.cost) {
            val need = progress.need(m, p.first)
            if (need > 0) {
                val have = player.count(p.first)
                val n = Math.min(need, have)
                if (n > 0) { player.take(p.first, n); progress.deliver(m, p.first, n) }
            }
        }
        checkMilestone(m)
    }
}
