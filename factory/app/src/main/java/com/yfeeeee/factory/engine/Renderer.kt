// Pixel Factory - engine: world renderer. Developer: @yfeeeee
package com.yfeeeee.factory.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.yfeeeee.factory.core.*

class Renderer(val sp: Sprites) {
    var W = 1; var H = 1
    var scale = 4                // screen px per sprite px
    var camX = 0f; var camY = 0f
    var tile = 64
    var animT = 0f
    var showPower = false
    private val paint = Paint()
    private val ghostPaint = Paint()
    private val rf = RectF()
    private val line = Paint()
    private val lightPaint = Paint()
    private var lightMask: Bitmap? = null
    private var lightCanvas: Canvas? = null
    private val lightSpr: Bitmap
    private val lmScale = 6

    init {
        paint.isFilterBitmap = false
        ghostPaint.isFilterBitmap = false
        line.strokeWidth = 2f
        lightPaint.isFilterBitmap = true
        lightPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        val pb = PB(32, 32)
        for (y in 0 until 32) for (x in 0 until 32) {
            val dx = x + 0.5f - 16f; val dy = y + 0.5f - 16f
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / 16f
            val a = if (d >= 1f) 0f else (1f - d) * (1f - d) * 0.5f + (1f - d) * 0.5f
            pb.set(x, y, argb((a * 255).toInt(), 255, 255, 255))
        }
        lightSpr = pb.toBitmap()
    }

    fun resize(w: Int, h: Int) {
        W = w; H = h
        scale = Math.max(2, Math.min(8, Math.round(h / 208f)))
        lightMask = Bitmap.createBitmap(Math.max(1, w / lmScale), Math.max(1, h / lmScale), Bitmap.Config.ARGB_8888)
        lightCanvas = Canvas(lightMask!!)
    }

    fun changeScale(s: Int) { scale = Math.max(2, Math.min(8, s)) }

    fun sx(wx: Float): Float = (wx - camX) * tile + W / 2f
    fun sy(wy: Float): Float = (wy - camY) * tile + H / 2f
    fun worldX(px: Float): Float = (px - W / 2f) / tile + camX
    fun worldY(py: Float): Float = (py - H / 2f) / tile + camY

    private fun spr(c: Canvas, b: Bitmap, wx: Float, wy: Float, p: Paint = paint) {
        val l = Math.round(sx(wx)).toFloat(); val t = Math.round(sy(wy)).toFloat()
        rf.set(l, t, l + b.width * scale, t + b.height * scale)
        c.drawBitmap(b, null, rf, p)
    }

    private fun sprPx(c: Canvas, b: Bitmap, wx: Float, wy: Float, ox: Int, oy: Int, p: Paint = paint) {
        val l = Math.round(sx(wx)).toFloat() + ox * scale; val t = Math.round(sy(wy)).toFloat() + oy * scale
        rf.set(l, t, l + b.width * scale, t + b.height * scale)
        c.drawBitmap(b, null, rf, p)
    }

    private fun fillRect(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, col: Int) {
        paint.color = col; paint.style = Paint.Style.FILL
        c.drawRect(x0, y0, x1, y1, paint)
    }

    private fun outline(c: Canvas, wx: Float, wy: Float, ww: Float, wh: Float, col: Int, th: Float) {
        val l = sx(wx); val t = sy(wy); val r = sx(wx + ww); val b = sy(wy + wh)
        paint.color = col; paint.style = Paint.Style.FILL
        c.drawRect(l, t, r, t + th, paint); c.drawRect(l, b - th, r, b, paint)
        c.drawRect(l, t, l + th, b, paint); c.drawRect(r - th, t, r, b, paint)
    }

    fun render(c: Canvas, g: Game, dt: Float, ghost: Ghost?, target: Boolean) {
        animT += dt
        tile = 16 * scale
        val p = g.player
        val k = Math.min(1f, dt * 10f)
        camX += (p.x - camX) * k; camY += (p.y - camY) * k
        if (Math.abs(camX - p.x) > 20f) { camX = p.x; camY = p.y }
        var shx = 0f; var shy = 0f
        if (g.shake > 0f) { shx = (Math.random().toFloat() - 0.5f) * g.shake * 30f; shy = (Math.random().toFloat() - 0.5f) * g.shake * 30f }
        c.save()
        c.translate(shx, shy)
        c.drawColor(rgb(16, 22, 30))
        val w = g.world
        val x0 = Math.max(0, Math.floor(worldX(0f).toDouble()).toInt() - 1)
        val x1 = Math.min(w.N - 1, Math.floor(worldX(W.toFloat()).toDouble()).toInt() + 1)
        val y0 = Math.max(0, Math.floor(worldY(0f).toDouble()).toInt() - 1)
        val y1 = Math.min(w.N - 1, Math.floor(worldY(H.toFloat()).toDouble()).toInt() + 2)
        val wf = ((animT * 3f).toInt()) and 3
        for (ty in y0..y1) for (tx in x0..x1) {
            val gt = w.ground[ty * w.N + tx].toInt()
            val v = hash2(tx, ty, 5) and 3
            val b: Bitmap = if (gt == T.DEEP) sp.water[0][wf] else if (gt == T.SHALLOW) sp.water[1][wf] else if (gt == T.LAVA) sp.lava[wf] else sp.ground[gt][v]
            spr(c, b, tx.toFloat(), ty.toFloat())
        }
        // resource nodes & pods
        for (n in w.nodes) {
            if (n.cx + 2 < x0 || n.cx - 2 > x1 || n.cy + 2 < y0 || n.cy - 2 > y1) continue
            if (!w.isExplored(n.cx, n.cy)) continue
            spr(c, sp.node(n.type, n.purity, (animT * 2f).toInt()), (n.cx - 1).toFloat(), (n.cy - 1).toFloat())
        }
        for (pd in w.pods) {
            if (pd.x < x0 || pd.x > x1 || pd.y < y0 || pd.y > y1) continue
            spr(c, sp.pods[if (pd.opened) 1 else 0], pd.x.toFloat(), pd.y.toFloat())
        }
        drawBuildings(c, g, x0, y0, x1, y1)
        // decorations + entities, row sorted
        val cl = g.creatures
        for (ty in y0..y1) {
            for (tx in x0..x1) {
                val dc = w.deco[ty * w.N + tx].toInt()
                if (dc == 0) continue
                val h = hash2(tx, ty, 9)
                when (dc) {
                    Deco.TREE -> sprPx(c, sp.trees[(h and 7) % 3], tx.toFloat(), ty.toFloat(), 0, -8)
                    Deco.PINE -> sprPx(c, sp.pine, tx.toFloat(), ty.toFloat(), 0, -8)
                    Deco.DEADTREE -> sprPx(c, sp.dead, tx.toFloat(), ty.toFloat(), 0, -8)
                    Deco.CACTUS -> sprPx(c, sp.cactus, tx.toFloat(), ty.toFloat(), 0, -4)
                    Deco.BOULDER -> {
                        val gt = w.ground[ty * w.N + tx].toInt()
                        val tone = if (gt == T.SAND) 1 else if (gt == T.ASH) 2 else 0
                        spr(c, sp.boulders[tone][h and 1], tx.toFloat(), ty.toFloat())
                    }
                    Deco.BUSH -> spr(c, sp.bush, tx.toFloat(), ty.toFloat())
                    Deco.BERRYBUSH -> spr(c, sp.berry, tx.toFloat(), ty.toFloat())
                    Deco.FLOWER -> spr(c, sp.flowers[h and 3], tx.toFloat(), ty.toFloat())
                    Deco.TUFT -> spr(c, sp.tufts[(h and 7) % 3], tx.toFloat(), ty.toFloat())
                }
            }
            for (cr in cl) if (Math.floor(cr.y.toDouble()).toInt() == ty) {
                val b = sp.creature(cr.type, (cr.anim).toInt(), cr.flash > 0f)
                val l = Math.round(sx(cr.x)).toFloat() - b.width * scale / 2f; val t = Math.round(sy(cr.y)).toFloat() - b.height * scale + 3 * scale
                if (cr.faceX < 0f) { c.save(); c.scale(-1f, 1f); rf.set(-(l + b.width * scale), t, -l, t + b.height * scale); c.drawBitmap(b, null, rf, paint); c.restore() }
                else { rf.set(l, t, l + b.width * scale, t + b.height * scale); c.drawBitmap(b, null, rf, paint) }
                if (cr.hp < CT.hp[cr.type]) {
                    val bw = 14f * scale; val bx = sx(cr.x) - bw / 2f; val by = t - 3 * scale
                    fillRect(c, bx, by, bx + bw, by + scale.toFloat(), rgb(60, 20, 20))
                    fillRect(c, bx, by, bx + bw * (cr.hp / CT.hp[cr.type]), by + scale.toFloat(), rgb(230, 70, 70))
                }
            }
            if (Math.floor(p.y.toDouble()).toInt() == ty) {
                val b = sp.player(p.dir, if (p.moving || p.actT > 0f) (p.anim).toInt() else 0)
                val l = Math.round(sx(p.x)).toFloat() - b.width * scale / 2f; val t = Math.round(sy(p.y)).toFloat() - b.height * scale + 2 * scale
                if (p.hurt > 0f && ((animT * 20).toInt() and 1) == 0) paint.alpha = 120
                rf.set(l, t, l + b.width * scale, t + b.height * scale); c.drawBitmap(b, null, rf, paint)
                paint.alpha = 255
            }
        }
        for (pr in g.projs) spr(c, sp.projectile, pr.x - 0.19f, pr.y - 0.19f)
        // target highlight
        if (target && p.tkind in 1..4) {
            var ww = 1f; var hh = 1f; var tx = p.tx.toFloat(); var ty = p.ty.toFloat()
            if (p.tkind == 4) { val b = g.occAt(p.tx, p.ty); if (b != null) { tx = b.x.toFloat(); ty = b.y.toFloat(); ww = b.w.toFloat(); hh = b.h.toFloat() } }
            if (p.tkind == 1) { val nd = g.world.node(p.tx, p.ty); if (nd != null) { tx = (nd.cx - 1).toFloat(); ty = (nd.cy - 1).toFloat(); ww = 3f; hh = 3f } }
            outline(c, tx, ty, ww, hh, rgb(255, 255, 255), 2f)
            if (p.actT > 0f && p.actMax > 0f) {
                val bw = 24f * scale / 2f; val bx = sx(tx + ww / 2f) - bw / 2f; val by = sy(ty) - 8f * scale / 2f
                fillRect(c, bx, by, bx + bw, by + 6f, rgb(20, 20, 30)); fillRect(c, bx + 1, by + 1, bx + 1 + (bw - 2) * Math.min(1f, p.actT / p.actMax), by + 5f, rgb(250, 200, 60))
            }
        } else if (target && p.tkind == 6) outline(c, p.tx.toFloat(), p.ty.toFloat(), 1f, 1f, rgb(120, 220, 255), 2f)
        if (showPower) drawWires(c, g)
        if (ghost != null) drawGhost(c, g, ghost)
        drawLight(c, g)
        c.restore()
    }

    private fun beltFrame(b: Building): Int = ((animT * 4f * b.def.v).toInt()) and 3

    private fun drawBuildings(c: Canvas, g: Game, x0: Int, y0: Int, x1: Int, y1: Int) {
        val list = g.buildings
        // pass 1: floors, belts, routers
        for (b in list) {
            if (b.x > x1 || b.y > y1 || b.x + b.w < x0 || b.y + b.h < y0) continue
            val k = b.def.kind
            if (k == Kind.FOUNDATION || k == Kind.CONVEYOR || k == Kind.SPLITTER || k == Kind.MERGER || k == Kind.SORTER || k == Kind.TUNNEL) {
                val bm: Bitmap = if (k == Kind.CONVEYOR) sp.belt(if (b.def == Bld.CONVEYOR1) 0 else if (b.def == Bld.CONVEYOR2) 1 else 2, b.rot, b.shape, if (b.cn > 0) beltFrame(b) else (animT * 4f * b.def.v).toInt() and 3) else sp.building(b.def, b.rot, 0)
                spr(c, bm, b.x.toFloat(), b.y.toFloat())
                if (k == Kind.SORTER && b.filter >= 0) {
                    val ic = sp.items[b.filter]
                    sprPx(c, ic, b.x.toFloat(), b.y.toFloat(), 2, 2, paint)
                }
            }
        }
        // pass 2: items on belts
        for (b in list) {
            if (b.def.kind != Kind.CONVEYOR || b.cn == 0) continue
            if (b.x > x1 || b.y > y1 || b.x + 1 < x0 || b.y + 1 < y0) continue
            for (i in 0 until b.cn) {
                val s = b.cp[i]
                var px = b.x + 0.5f; var py = b.y + 0.5f
                if (b.shape == 0) { px += DX[b.rot] * (s - 0.5f); py += DY[b.rot] * (s - 0.5f) }
                else {
                    val sideDir = if (b.shape == 1) (b.rot + 3) % 4 else (b.rot + 1) % 4
                    if (s < 0.5f) { px += DX[sideDir] * (0.5f - s); py += DY[sideDir] * (0.5f - s) }
                    else { px += DX[b.rot] * (s - 0.5f); py += DY[b.rot] * (s - 0.5f) }
                }
                val ic = sp.items[b.ci[i]]
                val l = Math.round(sx(px)).toFloat() - ic.width * scale / 2f; val t = Math.round(sy(py)).toFloat() - ic.height * scale / 2f
                rf.set(l, t, l + ic.width * scale, t + ic.height * scale)
                c.drawBitmap(ic, null, rf, paint)
            }
        }
        // pass 3: machines etc.
        for (b in list) {
            if (b.x > x1 || b.y > y1 || b.x + b.w < x0 || b.y + b.h < y0) continue
            val k = b.def.kind
            if (k == Kind.FOUNDATION || k == Kind.CONVEYOR || k == Kind.SPLITTER || k == Kind.MERGER || k == Kind.SORTER || k == Kind.TUNNEL) continue
            val running = b.active || (b.def.kind == Kind.GENERATOR && b.avail) || b.def.kind == Kind.HUB || b.def.kind == Kind.ELEVATOR || (b.def.kind == Kind.GEO && b.net >= 0) || (b.def.kind == Kind.MAM && b.wants)
            val fr = if (running) (((animT * 6f).toInt()) + b.id) and 3 else 0
            spr(c, sp.building(b.def, b.rot, fr), b.x.toFloat(), b.y.toFloat())
            // unpowered marker
            if (b.def.needsPower && b.wants && b.ratio <= 0f) {
                val ic = sp.icon("bolt")
                if (((animT * 2f).toInt() and 1) == 0) sprPx(c, ic, b.cx - 0.5f, b.cy - 0.5f, 1, 1)
            }
            // recipe icon above machine
            if (k == Kind.MACHINE && b.recipe == null && b.def.mach != Mach.SMELTER) {
                sprPx(c, sp.icon("gear"), b.cx - 0.5f, b.y - 0.5f, 1, 1)
            }
        }
    }

    private fun drawWires(c: Canvas, g: Game) {
        line.color = rgb(255, 230, 90); line.style = Paint.Style.STROKE
        for (b in g.buildings) {
            if (b.pole >= 0) { val pl = g.byId(b.pole); if (pl != null) c.drawLine(sx(b.cx), sy(b.cy), sx(pl.cx), sy(pl.cy), line) }
            if (b.def.kind == Kind.POLE) {
                for (o in g.buildings) if (o.def.kind == Kind.POLE && o.id > b.id) {
                    val dx = o.x - b.x; val dy = o.y - b.y
                    if (dx * dx + dy * dy <= 100) c.drawLine(sx(b.cx), sy(b.cy), sx(o.cx), sy(o.cy), line)
                }
            }
        }
    }

    class Ghost(val def: Bld, val x: Int, val y: Int, val rot: Int, val ok: Boolean)

    private fun drawGhost(c: Canvas, g: Game, gh: Ghost) {
        val d = gh.def
        val w = g.footprintW(d, gh.rot); val h = g.footprintH(d, gh.rot)
        val bm: Bitmap = if (d.kind == Kind.CONVEYOR) sp.belt(if (d == Bld.CONVEYOR1) 0 else if (d == Bld.CONVEYOR2) 1 else 2, gh.rot, 0, 0) else sp.building(d, gh.rot, 0)
        ghostPaint.alpha = 170
        spr(c, bm, gh.x.toFloat(), gh.y.toFloat(), ghostPaint)
        ghostPaint.alpha = 255
        val col = if (gh.ok) argb(70, 80, 255, 120) else argb(110, 255, 70, 70)
        paint.color = col; paint.style = Paint.Style.FILL
        c.drawRect(sx(gh.x.toFloat()), sy(gh.y.toFloat()), sx((gh.x + w).toFloat()), sy((gh.y + h).toFloat()), paint)
        outline(c, gh.x.toFloat(), gh.y.toFloat(), w.toFloat(), h.toFloat(), if (gh.ok) rgb(120, 255, 150) else rgb(255, 100, 100), 2f)
        if (d.kind == Kind.POLE) {
            line.color = argb(120, 255, 230, 90); line.style = Paint.Style.STROKE
            c.drawCircle(sx(gh.x + 0.5f), sy(gh.y + 0.5f), 8f * tile, line)
        }
    }

    private fun drawLight(c: Canvas, g: Game) {
        val dl = g.daylight()
        val a = ((1f - dl) * 175f).toInt()
        if (a < 6) return
        val lm = lightMask ?: return
        val lc = lightCanvas ?: return
        lm.eraseColor(argb(a, 6, 10, 34))
        val ts = tile.toFloat() / lmScale
        fun light(wx: Float, wy: Float, rad: Float) {
            val cx = sx(wx) / lmScale; val cy = sy(wy) / lmScale; val r = rad * ts
            if (cx + r < 0 || cy + r < 0 || cx - r > lm.width || cy - r > lm.height) return
            rf.set(cx - r, cy - r, cx + r, cy + r)
            lc.drawBitmap(lightSpr, null, rf, lightPaint)
        }
        light(g.player.x, g.player.y, 5f)
        for (b in g.buildings) {
            val sxx = sx(b.cx); val syy = sy(b.cy)
            if (sxx < -400 || syy < -400 || sxx > W + 400 || syy > H + 400) continue
            when (b.def.kind) {
                Kind.LAMP -> if (b.ratio > 0.3f || b.net >= 0) light(b.cx, b.cy, 7f)
                Kind.HUB -> light(b.cx, b.cy, 8f)
                Kind.GENERATOR -> if (b.avail) light(b.cx, b.cy, 4f)
                Kind.MACHINE -> if (b.active && (b.def.mach == Mach.SMELTER || b.def.mach == Mach.FOUNDRY)) light(b.cx, b.cy, 3.5f)
                Kind.ELEVATOR -> light(b.cx, b.cy, 9f)
                else -> {}
            }
        }
        for (pd in g.world.pods) if (!pd.opened) light(pd.x + 0.5f, pd.y + 0.5f, 2.5f)
        rf.set(0f, 0f, W.toFloat(), H.toFloat())
        paint.alpha = 255
        val lp = Paint(); lp.isFilterBitmap = true
        c.drawBitmap(lm, null, rf, lp)
    }
}
