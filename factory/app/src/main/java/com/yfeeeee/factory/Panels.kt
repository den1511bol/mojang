// Pixel Factory - overlay panels (inventory, crafting, building menu, machines, map...). Developer: @yfeeeee
package com.yfeeeee.factory

import android.graphics.Bitmap
import android.graphics.Canvas
import com.yfeeeee.factory.core.*
import com.yfeeeee.factory.engine.*

object P {
    const val NONE = 0; const val INV = 1; const val CRAFT = 2; const val BUILD = 3; const val BLD = 4
    const val MAP = 5; const val PAUSE = 6; const val HELP = 7; const val WIN = 8
}

class AppState {
    var g: Game? = null
    var screen = 0            // 0 loading, 1 main menu, 2 play
    var panel = 0
    var curB: Building? = null
    var buildDef: Bld? = null
    var buildRot = 0
    var ghostX = 0
    var ghostY = 0
    var demolish = false
    var tab = 0
    var en = false
    var loadMsg = ""
    var loadFrac = 0f
    var hasSave = false
    var invSel = -1
    var sfxOn = true
    var showPower = false
    var winShown = false
}

interface Callbacks {
    fun sfx(n: String)
    fun saveNow()
    fun toMenu()
    fun zoom(d: Int)
    fun toggleSfx()
}

class Panels(val ui: Ui, val sp: Sprites, val st: AppState, val cb: Callbacks) {
    private var pl = 0f
    private var pt = 0f
    private var pr = 0f
    private var pb = 0f
    private var mapBmp: Bitmap? = null
    private val mapPix = IntArray(256 * 256)
    private var mapT = 0f
    private var mapBlink = 0f

    private fun tr(ru: String, en: String): String = if (st.en) en else ru

    fun close() {
        st.panel = P.NONE
        st.curB = null
        ui.scroll = 0f
    }

    fun open(p: Int) {
        st.panel = p
        ui.scroll = 0f
        mapT = 0f
    }

    fun openBuilding(b: Building) {
        st.curB = b
        st.panel = P.BLD
        ui.scroll = 0f
    }

    fun draw(c: Canvas, dt: Float) {
        when (st.panel) {
            P.INV -> drawInv(c)
            P.CRAFT -> drawCraft(c)
            P.BUILD -> drawBuild(c)
            P.BLD -> drawBld(c)
            P.MAP -> drawMap(c, dt)
            P.PAUSE -> drawPause(c)
            P.HELP -> drawHelp(c)
            P.WIN -> drawWin(c)
        }
    }

    // ------------------------------------------------------------------ frame
    private fun frame(c: Canvas, title: String, showClose: Boolean = true) {
        val u = ui.u
        ui.rect(c, 0f, 0f, ui.W.toFloat(), ui.H.toFloat(), argb(165, 0, 0, 0))
        ui.area(0f, 0f, ui.W.toFloat(), ui.H.toFloat()) { close() }
        val l = ui.W * 0.05f; val r = ui.W * 0.95f; val t = ui.H * 0.04f; val b = ui.H * 0.96f
        ui.box(c, l, t, r, b, Col.panel, Col.border)
        ui.area(l, t, r, b) { }
        ui.textFit(c, title, l + 8f * u, t + 8f * u, (r - l) - 50f * u, 12f, Col.accent)
        if (showClose) ui.iconButton(c, "x", r - 26f * u, t + 4f * u, 22f * u, Col.danger) { close() }
        pl = l + 8f * u; pr = r - 8f * u; pt = t + 30f * u; pb = b - 8f * u
    }

    private fun chips(c: Canvas, g: Game, list: List<Pair<Item, Int>>, x0: Float, y: Float, size: Float): Float {
        var x = x0
        for (p in list) x += ui.chip(c, p.first, p.second, x, y, size, g.player.count(p.first) >= p.second)
        return x
    }

    private fun recipeLine(c: Canvas, g: Game, r: Recipe, x0: Float, y: Float, size: Float): Float {
        var x = chips(c, g, r.ins, x0, y, size)
        ui.text(c, ">", x, y + 1f, size, Col.accent)
        x += ui.tw(">", size) + 3f * ui.u
        for (p in r.outs) x += ui.chip(c, p.first, p.second, x, y, size, true)
        return x
    }

    // ------------------------------------------------------------------ inventory
    private fun drawInv(c: Canvas) {
        val g = st.g ?: return
        val p = g.player
        val u = ui.u
        frame(c, tr("Рюкзак", "Backpack") + "   " + p.slotsUsed() + "/" + p.slots)
        val list = ArrayList<Item>()
        for (itm in ITEMS) if (p.count(itm) > 0) list.add(itm)
        val cell = 50f * u
        val cols = Math.max(1, ((pr - pl) / cell).toInt())
        val rows = (list.size + cols - 1) / cols
        val detailH = 50f * u
        val top = ui.beginScroll(c, pl, pt, pr, pb - detailH, rows * cell + 4f * u)
        for (i in list.indices) {
            val itm = list[i]
            val x = pl + (i % cols) * cell
            val y = top + (i / cols) * cell
            val sel = st.invSel == itm.ordinal
            ui.box(c, x + u, y + u, x + cell - u, y + cell - u, if (sel) Col.btnOn else Col.panel2, if (sel) Col.good else Col.border)
            ui.item(c, itm, x + 11f * u, y + 6f * u, 26f * u)
            ui.text(c, p.count(itm).toString(), x + cell - 5f * u, y + cell - 16f * u, 8f, Col.text, 2)
            ui.area(x + u, y + u, x + cell - u, y + cell - u) { st.invSel = itm.ordinal; cb.sfx("click") }
        }
        ui.endScroll(c)
        val dy = pb - detailH + 6f * u
        if (list.isEmpty()) ui.text(c, tr("Пусто", "Empty"), pl, dy, 10f, Col.dim)
        val sel = if (st.invSel >= 0) ITEMS[st.invSel] else null
        if (sel != null && p.count(sel) > 0) {
            ui.item(c, sel, pl, dy, 28f * u)
            ui.text(c, sel.name(st.en) + "  x" + p.count(sel), pl + 34f * u, dy, 10f, Col.text)
            var info = tr("Стак: ", "Stack: ") + sel.stack
            if (sel.isFuel) info += "   " + tr("Топливо: ", "Fuel: ") + sel.fuelValue.toInt() + " MJ"
            ui.text(c, info, pl + 34f * u, dy + 14f * u, 8f, Col.dim)
            if (sel == Item.BERRIES) {
                ui.button(c, pr - 130f * u, dy - 2f * u, pr, dy + 28f * u, tr("Съесть +20", "Eat +20"), Col.btnOn, p.hp < p.maxHp) { g.eatBerries(); cb.sfx("good") }
            }
        }
    }

    // ------------------------------------------------------------------ crafting
    private fun drawCraft(c: Canvas) {
        val g = st.g ?: return
        val p = g.player
        val u = ui.u
        frame(c, tr("Ручной крафт", "Hand crafting"))
        var y = pt
        val cur = p.craft
        if (cur != null) {
            ui.text(c, tr("Крафт: ", "Crafting: ") + cur.name(st.en) + " x" + p.craftQty, pl, y, 9f, Col.good)
            val frac = if (cur.time > 0f) 1f - Math.max(0f, p.craftT) / (cur.time * 0.5f) else 0f
            ui.bar(c, pl + 220f * u, y + 1f * u, pr - 60f * u, y + 11f * u, frac, Col.accent)
            ui.button(c, pr - 54f * u, y - 2f * u, pr, y + 14f * u, tr("Стоп", "Stop"), Col.danger, true, 7f) {
                p.craft = null; p.craftQty = 0; p.craftT = 0f
            }
            y += 20f * u
        }
        val list = ArrayList<Recipe>()
        for (r in Recipes.hand) if (g.progress.canMake(r)) list.add(r)
        val rowH = 40f * u
        val top = ui.beginScroll(c, pl, y, pr, pb, list.size * rowH + 4f * u)
        for (i in list.indices) {
            val r = list[i]
            val ry = top + i * rowH
            ui.box(c, pl, ry + u, pr, ry + rowH - u, Col.panel2, Col.border)
            ui.item(c, r.outs[0].first, pl + 6f * u, ry + 8f * u, 24f * u)
            ui.textFit(c, r.name(st.en), pl + 36f * u, ry + 6f * u, 130f * u, 9f, Col.text)
            ui.text(c, "x" + r.outs[0].second + "  " + r.time.toInt() / 2 + "s", pl + 36f * u, ry + 22f * u, 7.5f, Col.dim)
            recipeLine(c, g, r, pl + 170f * u, ry + 12f * u, 10f)
            val can = g.canCraft(r)
            ui.button(c, pr - 96f * u, ry + 6f * u, pr - 50f * u, ry + rowH - 6f * u, "x1", Col.btn, can, 9f) { g.startCraft(r, 1); cb.sfx("click") }
            ui.button(c, pr - 46f * u, ry + 6f * u, pr - 4f * u, ry + rowH - 6f * u, "x5", Col.btn, can, 9f) {
                var n = 0
                while (n < 5 && countAfter(g, r, n + 1)) n++
                if (n > 0) g.startCraft(r, n)
                cb.sfx("click")
            }
        }
        ui.endScroll(c)
    }

    private fun countAfter(g: Game, r: Recipe, n: Int): Boolean {
        for (p in r.ins) if (g.player.count(p.first) < p.second * n) return false
        return true
    }

    // ------------------------------------------------------------------ build menu
    private fun drawBuild(c: Canvas) {
        val g = st.g ?: return
        val u = ui.u
        frame(c, tr("Строительство", "Build"))
        val names = arrayOf(tr("Логистика", "Logistics"), tr("Производство", "Production"), tr("Энергия", "Power"), tr("Конструкции", "Structures"), tr("Особое", "Special"))
        val gap = 3f * u
        val tw = ((pr - pl) - gap * 4f) / 5f
        for (i in 0 until 5) {
            val l = pl + i * (tw + gap)
            val sel = st.tab == i
            ui.button(c, l, pt, l + tw, pt + 24f * u, names[i], if (sel) Col.btnOn else Col.btn, true, 8f) { st.tab = i; ui.scroll = 0f; cb.sfx("click") }
        }
        val list = ArrayList<Bld>()
        for (d in Bld.values()) if (d.cat == st.tab && d.kind != Kind.HUB) list.add(d)
        val rowH = 44f * u
        val listBottom = pb - 30f * u
        val top = ui.beginScroll(c, pl, pt + 28f * u, pr, listBottom, list.size * rowH + 4f * u)
        for (i in list.indices) {
            val d = list[i]
            val ry = top + i * rowH
            val unlocked = g.progress.canBuild(d)
            ui.box(c, pl, ry + u, pr, ry + rowH - u, if (unlocked) Col.panel2 else rgb(26, 28, 34), Col.border)
            ui.bitmapFit(c, sp.building(d, 0, 0), pl + 6f * u, ry + 6f * u, 34f * u, 32f * u)
            ui.text(c, d.name(st.en) + "  " + d.w + "x" + d.h, pl + 48f * u, ry + 5f * u, 9f, if (unlocked) Col.text else Col.dim)
            if (unlocked) {
                chips(c, g, d.cost, pl + 48f * u, ry + 22f * u, 10f)
                if (d.power > 0f && !d.isGenerator) ui.text(c, d.power.toInt().toString() + " MW", pr - 8f * u, ry + 6f * u, 8f, Col.accent, 2)
                if (d.isGenerator && d.power > 0f) ui.text(c, "+" + d.power.toInt() + " MW", pr - 8f * u, ry + 6f * u, 8f, Col.good, 2)
                ui.area(pl, ry + u, pr, ry + rowH - u) { selectBuild(g, d) }
            } else {
                val mn = Research.byId(d.ms)
                ui.text(c, tr("Нужна веха: ", "Needs milestone: ") + (if (mn != null) mn.name(st.en) else d.ms), pl + 48f * u, ry + 22f * u, 8f, Col.bad)
                ui.area(pl, ry + u, pr, ry + rowH - u) { cb.sfx("deny") }
            }
        }
        ui.endScroll(c)
        val by = pb - 26f * u
        ui.button(c, pl, by, pl + 130f * u, pb, tr("Снос", "Demolish"), Col.danger, true, 9f) {
            st.buildDef = null; st.demolish = true; close(); cb.sfx("click")
        }
        ui.button(c, pl + 136f * u, by, pl + 290f * u, pb, tr("Линии ЛЭП: ", "Power lines: ") + (if (st.showPower) "ON" else "OFF"), if (st.showPower) Col.btnOn else Col.btn, true, 8f) {
            st.showPower = !st.showPower; cb.sfx("click")
        }
    }

    private fun selectBuild(g: Game, d: Bld) {
        st.buildDef = d
        st.demolish = false
        st.buildRot = 0
        val p = g.player
        val w = g.footprintW(d, 0); val h = g.footprintH(d, 0)
        st.ghostX = Math.floor(p.x.toDouble()).toInt() + 2
        st.ghostY = Math.floor(p.y.toDouble()).toInt() - h / 2
        if (w < 0) st.ghostX = 0
        close()
        cb.sfx("click")
    }

    // ------------------------------------------------------------------ building panel
    private fun drawBld(c: Canvas) {
        val g = st.g ?: return
        val b = st.curB
        if (b == null || g.byId(b.id) !== b) { close(); return }
        val u = ui.u
        frame(c, b.def.name(st.en) + "  " + b.w + "x" + b.h)
        val barY = pb - 26f * u
        if (b.def.kind != Kind.HUB) {
            ui.button(c, pr - 130f * u, barY, pr, pb, tr("Разобрать", "Demolish"), Col.danger, true, 9f) {
                g.remove(b); cb.sfx("place"); close()
            }
        }
        val y0 = pt
        val y1 = barY - 4f * u
        when (b.def.kind) {
            Kind.MACHINE -> bldMachine(c, g, b, y0, y1)
            Kind.CONTAINER -> bldContainer(c, g, b, y0, y1)
            Kind.GENERATOR -> bldGenerator(c, g, b, y0, y1)
            Kind.MINER, Kind.PUMP -> bldMiner(c, g, b, y0)
            Kind.HUB -> bldHub(c, g, y0, y1)
            Kind.ELEVATOR -> bldElevator(c, g, y0)
            Kind.MAM -> bldMam(c, g, b, y0, y1)
            Kind.SORTER -> bldSorter(c, g, b, y0, y1)
            Kind.SOLAR, Kind.GEO -> {
                ui.text(c, tr("Выработка: ", "Output: ") + b.outPower.toInt() + " MW", pl, y0, 10f, Col.good)
                if (b.net < 0) ui.text(c, tr("Не подключено к опоре ЛЭП (до 8 клеток)", "Not connected to a power pole (up to 8 tiles)"), pl, y0 + 16f * u, 9f, Col.bad)
            }
            Kind.BATTERY -> {
                ui.text(c, tr("Заряд: ", "Charge: ") + b.charge.toInt() + " / " + b.def.v.toInt() + " MJ", pl, y0, 10f, Col.good)
                ui.bar(c, pl, y0 + 18f * u, pl + 220f * u, y0 + 28f * u, b.charge / b.def.v, Col.accent)
            }
            Kind.POLE -> {
                ui.text(c, tr("Соединяет постройки в радиусе 8 клеток.", "Links buildings within 8 tiles."), pl, y0, 9f, Col.dim)
            }
            Kind.LAMP -> ui.text(c, tr("Светит ночью, если есть питание.", "Lights up at night when powered."), pl, y0, 9f, Col.dim)
            else -> ui.text(c, tr("Нет настроек.", "No settings."), pl, y0, 9f, Col.dim)
        }
    }

    private fun powerLine(c: Canvas, b: Building, x: Float, y: Float): String {
        return if (b.def.needsPower && b.net < 0) tr("Нет связи с ЛЭП", "No power link")
        else if (b.def.needsPower && b.wants && b.ratio <= 0f) tr("Нет питания", "No power")
        else if (b.active) tr("Работает", "Running") else tr("Ожидание", "Idle")
    }

    private fun takeOutputs(g: Game, b: Building) {
        for (i in 0 until NITEMS) {
            if (b.outb[i] > 0) {
                val k = g.player.give(ITEMS[i], b.outb[i])
                b.outb[i] -= k
            }
        }
        cb.sfx("click")
    }

    private fun bldMachine(c: Canvas, g: Game, b: Building, y0: Float, y1: Float) {
        val u = ui.u
        var y = y0
        val r = b.recipe
        ui.text(c, if (r != null) r.name(st.en) else tr("Рецепт не выбран", "No recipe selected"), pl, y, 10f, Col.accent)
        ui.text(c, powerLine(c, b, pl, y) + "  " + b.def.power.toInt() + " MW", pr, y, 8f, if (b.active) Col.good else Col.dim, 2)
        y += 16f * u
        if (r != null) {
            var x = pl
            for (p in r.ins) {
                val have = b.inv[p.first.ordinal]
                x += ui.chipText(c, p.first, have.toString() + "/" + p.second, x, y, 10f, have >= p.second)
            }
            ui.text(c, ">", x, y + u, 10f, Col.accent)
            x += ui.tw(">", 10f) + 3f * u
            for (p in r.outs) x += ui.chipText(c, p.first, "x" + p.second + " (" + b.outb[p.first.ordinal] + ")", x, y, 10f, true)
            y += 16f * u
            ui.bar(c, pl, y, pl + 240f * u, y + 9f * u, if (b.crafting && r.time > 0f) b.progress / r.time else 0f, Col.accent)
            ui.text(c, r.time.toInt().toString() + "s", pl + 246f * u, y - 1f * u, 8f, Col.dim)
            y += 14f * u
        }
        val oc = b.outCount()
        ui.button(c, pl, y, pl + 170f * u, y + 24f * u, tr("Забрать продукцию (", "Take output (") + oc + ")", Col.btn, oc > 0, 8f) { takeOutputs(g, b) }
        ui.button(c, pl + 176f * u, y, pl + 340f * u, y + 24f * u, tr("Загрузить сырьё", "Load inputs"), Col.btn, r != null, 8f) {
            val rr = b.recipe
            if (rr != null) for (p in rr.ins) {
                val cap = p.second * 2 + 2 - b.inv[p.first.ordinal]
                val n = Math.min(cap, g.player.count(p.first))
                if (n > 0 && g.player.take(p.first, n)) b.inv[p.first.ordinal] += n
            }
            cb.sfx("click")
        }
        y += 30f * u
        ui.text(c, tr("Выбор рецепта:", "Choose recipe:"), pl, y, 8.5f, Col.dim)
        y += 14f * u
        val list = ArrayList<Recipe>()
        for (rc in Recipes.forMach(b.def.mach)) if (g.progress.canMake(rc)) list.add(rc)
        val rowH = 32f * u
        val top = ui.beginScroll(c, pl, y, pr, y1, list.size * rowH + 4f * u)
        for (i in list.indices) {
            val rc = list[i]
            val ry = top + i * rowH
            val sel = b.recipe === rc
            ui.box(c, pl, ry + u, pr, ry + rowH - u, if (sel) Col.btnOn else Col.panel2, if (sel) Col.good else Col.border)
            ui.textFit(c, rc.name(st.en), pl + 6f * u, ry + 9f * u, 120f * u, 8.5f, Col.text)
            recipeLine(c, g, rc, pl + 130f * u, ry + 8f * u, 9f)
            ui.area(pl, ry + u, pr, ry + rowH - u) { setRecipe(g, b, rc); cb.sfx("click") }
        }
        ui.endScroll(c)
    }

    private fun setRecipe(g: Game, b: Building, rec: Recipe) {
        if (b.recipe === rec) return
        val old = b.recipe
        if (b.crafting && old != null) for (p in old.ins) g.player.give(p.first, p.second)
        for (i in 0 until NITEMS) {
            if (b.inv[i] > 0) { val k = g.player.give(ITEMS[i], b.inv[i]); b.inv[i] -= k }
            if (b.outb[i] > 0) { val k = g.player.give(ITEMS[i], b.outb[i]); b.outb[i] -= k }
        }
        b.crafting = false
        b.progress = 0f
        b.recipe = rec
    }

    private fun bldContainer(c: Canvas, g: Game, b: Building, y0: Float, y1: Float) {
        val u = ui.u
        ui.text(c, tr("Ячейки: ", "Slots: ") + b.slotsUsed() + "/16   " + tr("Выдаёт предметы на соседние конвейеры.", "Pushes items to adjacent belts."), pl, y0, 9f, Col.dim)
        val sList = ArrayList<Item>()
        val pList = ArrayList<Item>()
        for (itm in ITEMS) {
            if (b.inv[itm.ordinal] > 0) sList.add(itm)
            if (g.player.count(itm) > 0) pList.add(itm)
        }
        val rowH = 28f * u
        val total = (sList.size + pList.size + 2) * rowH + 6f * u
        val top = ui.beginScroll(c, pl, y0 + 16f * u, pr, y1, total)
        var y = top
        ui.text(c, tr("В хранилище", "In storage"), pl, y + 6f * u, 9f, Col.accent)
        y += rowH
        for (itm in sList) {
            ui.box(c, pl, y + u, pr, y + rowH - u, Col.panel2, Col.border)
            ui.item(c, itm, pl + 6f * u, y + 5f * u, 18f * u)
            ui.textFit(c, itm.name(st.en), pl + 30f * u, y + 8f * u, 150f * u, 8.5f, Col.text)
            ui.text(c, "x" + b.inv[itm.ordinal], pl + 190f * u, y + 8f * u, 8.5f, Col.text)
            ui.button(c, pr - 90f * u, y + 3f * u, pr - 4f * u, y + rowH - 3f * u, tr("Взять", "Take"), Col.btn, true, 8f) {
                val n = Math.min(b.inv[itm.ordinal], itm.stack)
                val k = g.player.give(itm, n)
                b.inv[itm.ordinal] -= k
                cb.sfx("click")
            }
            y += rowH
        }
        ui.text(c, tr("В рюкзаке", "In backpack"), pl, y + 6f * u, 9f, Col.accent)
        y += rowH
        for (itm in pList) {
            ui.box(c, pl, y + u, pr, y + rowH - u, Col.panel2, Col.border)
            ui.item(c, itm, pl + 6f * u, y + 5f * u, 18f * u)
            ui.textFit(c, itm.name(st.en), pl + 30f * u, y + 8f * u, 150f * u, 8.5f, Col.text)
            ui.text(c, "x" + g.player.count(itm), pl + 190f * u, y + 8f * u, 8.5f, Col.text)
            ui.button(c, pr - 90f * u, y + 3f * u, pr - 4f * u, y + rowH - 3f * u, tr("Положить", "Put"), Col.btn, true, 8f) {
                var n = 0
                val want = Math.min(g.player.count(itm), itm.stack)
                while (n < want) {
                    if (b.inv[itm.ordinal] % itm.stack == 0 && b.slotsUsed() >= 16) break
                    b.inv[itm.ordinal]++
                    n++
                }
                if (n > 0) g.player.take(itm, n)
                cb.sfx("click")
            }
            y += rowH
        }
        ui.endScroll(c)
    }

    private fun bldGenerator(c: Canvas, g: Game, b: Building, y0: Float, y1: Float) {
        val u = ui.u
        val on = b.avail
        ui.text(c, if (on) tr("Вырабатывает ", "Producing ") + b.outPower.toInt() + " / " + b.def.power.toInt() + " MW" else tr("Нет топлива или сети", "No fuel or no grid"), pl, y0, 10f, if (on) Col.good else Col.bad)
        ui.text(c, tr("Горит: ", "Burning: ") + b.burn.toInt() + " MJ", pl, y0 + 16f * u, 8.5f, Col.dim)
        val fuels = b.def.fuels()
        val rowH = 32f * u
        val top = ui.beginScroll(c, pl, y0 + 34f * u, pr, y1, fuels.size * rowH + 4f * u)
        for (i in fuels.indices) {
            val f = fuels[i]
            val ry = top + i * rowH
            ui.box(c, pl, ry + u, pr, ry + rowH - u, Col.panel2, Col.border)
            ui.item(c, f, pl + 6f * u, ry + 6f * u, 20f * u)
            ui.textFit(c, f.name(st.en), pl + 32f * u, ry + 4f * u, 150f * u, 8.5f, Col.text)
            ui.text(c, tr("в ген.: ", "inside: ") + b.inv[f.ordinal] + "   " + tr("в рюкзаке: ", "bag: ") + g.player.count(f), pl + 32f * u, ry + 17f * u, 7.5f, Col.dim)
            val can = g.player.count(f) > 0 && b.inv[f.ordinal] < 40
            ui.button(c, pr - 90f * u, ry + 3f * u, pr - 4f * u, ry + rowH - 3f * u, tr("Заправить", "Fuel up"), Col.btn, can, 8f) {
                val n = Math.min(Math.min(g.player.count(f), 20), 40 - b.inv[f.ordinal])
                if (n > 0 && g.player.take(f, n)) b.inv[f.ordinal] += n
                cb.sfx("click")
            }
        }
        ui.endScroll(c)
    }

    private fun bldMiner(c: Canvas, g: Game, b: Building, y0: Float) {
        val u = ui.u
        val nd = b.node
        if (nd == null) { ui.text(c, tr("Нет месторождения", "No resource node"), pl, y0, 10f, Col.bad); return }
        val rate = 0.5f * b.def.v * Res.purityMul(nd.purity)
        ui.text(c, Res.name(nd.type, st.en) + " (" + Res.purityName(nd.purity, st.en) + ")", pl, y0, 10f, Col.accent)
        ui.text(c, tr("Скорость: ", "Rate: ") + String.format("%.2f", rate * 60f / 60f) + tr(" шт/с", " /s") + "   " + powerLine(c, b, pl, y0), pl, y0 + 16f * u, 9f, if (b.active) Col.good else Col.dim)
        val oc = b.outCount()
        ui.button(c, pl, y0 + 36f * u, pl + 190f * u, y0 + 62f * u, tr("Забрать (", "Take (") + oc + ")", Col.btn, oc > 0, 9f) { takeOutputs(g, b) }
        ui.text(c, tr("Выход идёт на соседние конвейеры и хранилища.", "Output goes to adjacent belts and storage."), pl, y0 + 70f * u, 8f, Col.dim)
    }

    private fun bldHub(c: Canvas, g: Game, y0: Float, y1: Float) {
        val u = ui.u
        val pr0 = g.progress
        ui.text(c, tr("Ступень: ", "Tier: ") + pr0.currentTier() + "   " + tr("Выберите веху и сдайте ресурсы (рюкзак или конвейер в ХАБ).", "Pick a milestone and deliver resources (bag or belt into the HUB)."), pl, y0, 8.5f, Col.dim)
        val list = ArrayList<Milestone>()
        for (m in Research.list) if (!m.elevator) list.add(m)
        val rowH = 68f * u
        val top = ui.beginScroll(c, pl, y0 + 16f * u, pr, y1, list.size * rowH + 4f * u)
        for (i in list.indices) {
            val m = list[i]
            val ry = top + i * rowH
            val done = pr0.done.contains(m.id)
            val avail = pr0.available(m)
            val active = pr0.active == m.id
            ui.box(c, pl, ry + u, pr, ry + rowH - u, if (active) rgb(40, 70, 60) else Col.panel2, if (active) Col.good else Col.border)
            ui.textFit(c, "T" + m.tier + "  " + m.name(st.en), pl + 6f * u, ry + 5f * u, 230f * u, 9.5f, if (done) Col.good else if (avail) Col.accent else Col.dim)
            ui.textFit(c, m.desc(st.en), pl + 6f * u, ry + 20f * u, (pr - pl) - 120f * u, 7.5f, Col.dim)
            if (done) {
                ui.text(c, tr("Готово", "Done"), pr - 10f * u, ry + 8f * u, 9f, Col.good, 2)
            } else if (!avail) {
                ui.text(c, tr("Закрыто", "Locked"), pr - 10f * u, ry + 8f * u, 9f, Col.dim, 2)
            } else {
                val arr = pr0.deliveredArr(m)
                var x = pl + 6f * u
                for (k in m.cost.indices) {
                    val p = m.cost[k]
                    x += ui.chipText(c, p.first, arr[k].toString() + "/" + p.second, x, ry + 40f * u, 10f, arr[k] >= p.second)
                }
                ui.button(c, pr - 110f * u, ry + 4f * u, pr - 4f * u, ry + 26f * u, if (active) tr("Выбрано", "Selected") else tr("Выбрать", "Select"), if (active) Col.btnOn else Col.btn, !active, 8f) {
                    pr0.active = m.id; cb.sfx("click")
                }
                ui.button(c, pr - 110f * u, ry + 30f * u, pr - 4f * u, ry + 56f * u, tr("Сдать из рюкзака", "Deliver from bag"), Col.btn, true, 7.5f) {
                    g.deliverFromInventory(m); cb.sfx("good")
                }
            }
        }
        ui.endScroll(c)
    }

    private fun bldElevator(c: Canvas, g: Game, y0: Float) {
        val u = ui.u
        val m = g.activeElevatorMilestone()
        if (m == null) {
            ui.text(c, if (g.progress.won) tr("Все фазы завершены!", "All phases complete!") else tr("Нет доступной фазы: завершите все вехи предыдущей ступени в ХАБе.", "No phase available: finish all milestones of the previous tier at the HUB."), pl, y0, 9f, Col.dim)
            return
        }
        ui.text(c, m.name(st.en), pl, y0, 11f, Col.accent)
        ui.text(c, m.desc(st.en), pl, y0 + 16f * u, 8.5f, Col.dim)
        val arr = g.progress.deliveredArr(m)
        var x = pl
        for (k in m.cost.indices) {
            val p = m.cost[k]
            x += ui.chipText(c, p.first, arr[k].toString() + "/" + p.second, x, y0 + 34f * u, 11f, arr[k] >= p.second)
        }
        ui.button(c, pl, y0 + 60f * u, pl + 200f * u, y0 + 88f * u, tr("Сдать из рюкзака", "Deliver from bag"), Col.btn, true, 9f) { g.deliverFromInventory(m); cb.sfx("good") }
        ui.text(c, tr("Можно подвести конвейер прямо к лифту.", "You can run a belt straight into the elevator."), pl, y0 + 96f * u, 8f, Col.dim)
    }

    private fun bldMam(c: Canvas, g: Game, b: Building, y0: Float, y1: Float) {
        val u = ui.u
        ui.text(c, tr("Жёстких дисков в МАМ: ", "Hard drives in MAM: ") + g.mamDrives + "   " + tr("в рюкзаке: ", "in bag: ") + g.player.count(Item.HARD_DRIVE), pl, y0, 9.5f, Col.text)
        val busy = g.mamBusy
        if (busy) {
            ui.text(c, tr("Анализ... ", "Analyzing... ") + g.mamTimer.toInt() + "s" + (if (b.ratio <= 0f) "  " + tr("(нужно питание)", "(needs power)") else ""), pl, y0 + 16f * u, 9f, Col.good)
            ui.bar(c, pl, y0 + 30f * u, pl + 240f * u, y0 + 40f * u, 1f - g.mamTimer / 25f, Col.accent)
        } else {
            val can = g.pendingAlts.isEmpty() && (g.mamDrives > 0 || g.player.count(Item.HARD_DRIVE) > 0)
            ui.button(c, pl, y0 + 16f * u, pl + 220f * u, y0 + 42f * u, tr("Анализ диска (25с)", "Analyze drive (25s)"), Col.btn, can, 9f) { g.startAnalysis(); cb.sfx("click") }
        }
        ui.text(c, tr("Диски падают из капсул и с Жалоносцев.", "Drives drop from drop pods and Stingers."), pl, y0 + 46f * u, 8f, Col.dim)
        val alts = ArrayList<Recipe>()
        for (k in g.pendingAlts) { val r = Recipes.byKey(k); if (r != null) alts.add(r) }
        val rowH = 44f * u
        val top = ui.beginScroll(c, pl, y0 + 62f * u, pr, y1, alts.size * rowH + 4f * u)
        for (i in alts.indices) {
            val r = alts[i]
            val ry = top + i * rowH
            ui.box(c, pl, ry + u, pr, ry + rowH - u, Col.panel2, Col.border)
            ui.textFit(c, r.name(st.en) + "  [" + (if (st.en) r.mach.en else r.mach.ru) + "]", pl + 6f * u, ry + 5f * u, 260f * u, 9f, Col.accent)
            recipeLine(c, g, r, pl + 6f * u, ry + 22f * u, 10f)
            ui.button(c, pr - 100f * u, ry + 8f * u, pr - 4f * u, ry + rowH - 8f * u, tr("Изучить", "Research"), Col.btnOn, true, 8.5f) { g.pickAlt(r.key); cb.sfx("good") }
        }
        ui.endScroll(c)
    }

    private fun bldSorter(c: Canvas, g: Game, b: Building, y0: Float, y1: Float) {
        val u = ui.u
        val cur = if (b.filter >= 0) ITEMS[b.filter].name(st.en) else tr("нет (всё прямо)", "none (all forward)")
        ui.text(c, tr("Фильтр: ", "Filter: ") + cur, pl, y0, 10f, Col.accent)
        ui.text(c, tr("Выбранный предмет идёт вперёд, остальные в стороны.", "Chosen item goes forward, others go sideways."), pl, y0 + 16f * u, 8f, Col.dim)
        val cell = 44f * u
        val cols = Math.max(1, ((pr - pl) / cell).toInt())
        val n = ITEMS.size + 1
        val rows = (n + cols - 1) / cols
        val top = ui.beginScroll(c, pl, y0 + 32f * u, pr, y1, rows * cell + 4f * u)
        for (i in 0 until n) {
            val x = pl + (i % cols) * cell
            val y = top + (i / cols) * cell
            val idx = i - 1
            val sel = b.filter == idx
            ui.box(c, x + u, y + u, x + cell - u, y + cell - u, if (sel) Col.btnOn else Col.panel2, if (sel) Col.good else Col.border)
            if (idx < 0) ui.icon(c, "x", x + 11f * u, y + 11f * u, 22f * u) else ui.item(c, ITEMS[idx], x + 11f * u, y + 11f * u, 22f * u)
            ui.area(x + u, y + u, x + cell - u, y + cell - u) { b.filter = idx; cb.sfx("click") }
        }
        ui.endScroll(c)
    }

    // ------------------------------------------------------------------ map
    private fun groundColor(gt: Int): Int = when (gt) {
        T.DEEP -> rgb(18, 38, 92); T.SHALLOW -> rgb(40, 92, 150); T.SAND -> rgb(204, 184, 122); T.GRASS -> rgb(72, 134, 62)
        T.FOREST -> rgb(38, 96, 46); T.DIRT -> rgb(124, 92, 62); T.ROCK -> rgb(112, 112, 118); T.SNOW -> rgb(236, 241, 246)
        T.SWAMP -> rgb(62, 92, 72); T.CLIFF -> rgb(66, 66, 72); T.ASH -> rgb(62, 56, 62); else -> rgb(224, 84, 30)
    }

    private fun refreshMap(g: Game) {
        val w = g.world
        for (my in 0 until 256) for (mx in 0 until 256) {
            val x = mx * 2; val y = my * 2
            var col = rgb(10, 12, 16)
            if (w.isExplored(x, y)) {
                col = groundColor(w.g(x, y))
                if (w.d(x, y) == Deco.TREE || w.d(x, y) == Deco.PINE) col = shade(col, 0.82f)
                if (g.occAt(x, y) != null) col = rgb(236, 206, 90)
            }
            mapPix[my * 256 + mx] = col
        }
        for (n in w.nodes) {
            if (!w.isExplored(n.cx, n.cy)) continue
            val col = ArtWorld.nodeColor(n.type)
            for (dy in -1..1) for (dx in -1..1) {
                val px = n.cx / 2 + dx; val py = n.cy / 2 + dy
                if (px in 0..255 && py in 0..255) mapPix[py * 256 + px] = col
            }
        }
        for (pd in w.pods) {
            if (pd.opened || !w.isExplored(pd.x, pd.y)) continue
            val px = pd.x / 2; val py = pd.y / 2
            if (px in 0..255 && py in 0..255) mapPix[py * 256 + px] = rgb(255, 255, 255)
        }
        val old = mapBmp
        mapBmp = Bitmap.createBitmap(mapPix, 256, 256, Bitmap.Config.ARGB_8888)
        if (old != null) old.recycle()
    }

    private fun drawMap(c: Canvas, dt: Float) {
        val g = st.g ?: return
        val u = ui.u
        frame(c, tr("Карта", "Map"))
        mapT -= dt
        mapBlink += dt
        if (mapT <= 0f || mapBmp == null) { refreshMap(g); mapT = 1.2f }
        val size = Math.min(pr - pl, pb - pt)
        val l = pl; val t = pt
        val bm = mapBmp
        if (bm != null) ui.bitmap(c, bm, l, t, size, size)
        val k = size / g.world.N
        if (((mapBlink * 3f).toInt() and 1) == 0) {
            val px = l + g.player.x * k; val py = t + g.player.y * k
            ui.rect(c, px - 3f * u, py - 3f * u, px + 3f * u, py + 3f * u, rgb(255, 80, 80))
        }
        val lx = l + size + 14f * u
        var y = pt
        ui.text(c, tr("Вы: ", "You: ") + g.player.x.toInt() + ", " + g.player.y.toInt(), lx, y, 9f, Col.text); y += 16f * u
        val h = g.hub
        if (h != null) ui.text(c, tr("ХАБ: ", "HUB: ") + h.x + ", " + h.y, lx, y, 9f, Col.text)
        y += 20f * u
        val types = intArrayOf(Res.IRON, Res.COPPER, Res.LIMESTONE, Res.COAL, Res.CATERIUM, Res.QUARTZ, Res.SULFUR, Res.BAUXITE, Res.OIL, Res.GEYSER)
        for (tp in types) {
            ui.rect(c, lx, y + 2f * u, lx + 9f * u, y + 11f * u, ArtWorld.nodeColor(tp))
            ui.text(c, Res.name(tp, st.en), lx + 14f * u, y, 8.5f, Col.text)
            y += 14f * u
        }
        ui.rect(c, lx, y + 2f * u, lx + 9f * u, y + 11f * u, rgb(255, 255, 255))
        ui.text(c, tr("Капсула", "Drop pod"), lx + 14f * u, y, 8.5f, Col.text)
    }

    // ------------------------------------------------------------------ pause
    private fun drawPause(c: Canvas) {
        val g = st.g ?: return
        val u = ui.u
        frame(c, tr("Пауза", "Paused"))
        val mins = (g.playTime / 60f).toInt()
        ui.text(c, tr("Время игры: ", "Play time: ") + mins + tr(" мин   День ", " min   Day ") + g.days + "   " + tr("Убито: ", "Kills: ") + g.killCount + "   " + tr("Смертей: ", "Deaths: ") + g.player.deaths, pl, pt, 9f, Col.dim)
        ui.text(c, "Pixel Factory  -  Developer: @yfeeeee", pl, pt + 16f * u, 8.5f, Col.accent)
        val w = 180f * u; val h = 28f * u; val gap = 8f * u
        val x1 = pl; val x2 = pl + w + gap
        var y = pt + 40f * u
        ui.button(c, x1, y, x1 + w, y + h, tr("Продолжить", "Resume"), Col.btnOn) { close() }
        ui.button(c, x2, y, x2 + w, y + h, tr("Сохранить", "Save"), Col.btn) { cb.saveNow(); g.toast(tr("Игра сохранена", "Game saved")) }
        y += h + gap
        ui.button(c, x1, y, x1 + w, y + h, tr("Язык: Русский", "Language: English"), Col.btn) { st.en = !st.en; g.en = st.en; cb.sfx("click") }
        ui.button(c, x2, y, x2 + w, y + h, tr("Звук: ", "Sound: ") + (if (st.sfxOn) "ON" else "OFF"), Col.btn) { cb.toggleSfx() }
        y += h + gap
        ui.button(c, x1, y, x1 + w, y + h, tr("Масштаб +", "Zoom +"), Col.btn) { cb.zoom(1) }
        ui.button(c, x2, y, x2 + w, y + h, tr("Масштаб -", "Zoom -"), Col.btn) { cb.zoom(-1) }
        y += h + gap
        ui.button(c, x1, y, x1 + w, y + h, tr("Помощь", "Help"), Col.btn) { open(P.HELP) }
        ui.button(c, x2, y, x2 + w, y + h, tr("В главное меню", "Main menu"), Col.danger) { cb.saveNow(); cb.toMenu() }
    }

    private fun drawHelp(c: Canvas) {
        val u = ui.u
        frame(c, tr("Как играть", "How to play"))
        val textRu = "ЦЕЛЬ: постройте и запустите Космический лифт (5 фаз).\n\n" +
            "ДВИЖЕНИЕ: стик слева. Касание карты: идти к объекту, добывать руду из месторождений (цветные пятна 3x3), рубить деревья, бить камни, открывать капсулы с жёсткими дисками.\n\n" +
            "БОЙ: кнопка с мечом бьёт врагов перед вами. Ягоды лечат. Ночью зверей больше.\n\n" +
            "СТАРТ: поставьте Бур Mk1 на месторождение железа, Плавильню рядом, Конструктор (после вехи) и соедините конвейерами. Бур и станки нуждаются в энергии: поставьте Биогенератор (топливо: биомасса, дерево, листья) и Опору ЛЭП между ними (радиус 8 клеток).\n\n" +
            "СТРОЙКА: молоток открывает меню. Выберите постройку, коснитесь карты, чтобы поставить призрак, кнопка с галочкой ставит. Конвейеры можно рисовать, ведя пальцем. Кнопка поворота вращает. Режим 'Снос' возвращает материалы.\n\n" +
            "ХАБ: в центре карты. Выберите веху и сдайте ресурсы вручную или конвейером прямо в ХАБ. Вехи открывают новые постройки.\n\n" +
            "МАМ: исследуйте жёсткие диски, чтобы получить альтернативные рецепты.\n\n" +
            "Сохранение автоматическое. Разработчик: @yfeeeee"
        val textEn = "GOAL: build and launch the Space Elevator (5 phases).\n\n" +
            "MOVE: stick on the left. Tap the world to walk to things, mine ore at resource nodes (coloured 3x3 patches), chop trees, smash boulders, open drop pods for hard drives.\n\n" +
            "COMBAT: the sword button hits enemies in front of you. Berries heal. More creatures spawn at night.\n\n" +
            "START: put a Miner Mk1 on an iron node, a Smelter next to it, a Constructor (after milestone) and link them with conveyors. Miners and machines need power: build a Biomass Burner (fuel: biomass, wood, leaves) and a Power Pole between them (8 tile radius).\n\n" +
            "BUILDING: the hammer opens the menu. Pick a building, tap the world to place the ghost, press the check button to build. Drag a finger to paint conveyors. The rotate button turns it. 'Demolish' mode refunds materials.\n\n" +
            "HUB: in the map centre. Pick a milestone and deliver resources by hand or by belt straight into the HUB. Milestones unlock buildings.\n\n" +
            "MAM: research hard drives to unlock alternate recipes.\n\n" +
            "Autosave is on. Developer: @yfeeeee"
        val lines = ui.wrap(if (st.en) textEn else textRu, (pr - pl) - 12f * u, 9f)
        val lh = 12.5f * u
        val top = ui.beginScroll(c, pl, pt, pr, pb, lines.size * lh + 8f * u)
        for (i in lines.indices) ui.text(c, lines[i], pl + 4f * u, top + i * lh, 9f, Col.text)
        ui.endScroll(c)
    }

    private fun drawWin(c: Canvas) {
        val g = st.g ?: return
        val u = ui.u
        frame(c, tr("Победа!", "Victory!"))
        val cx = (pl + pr) / 2f
        ui.text(c, tr("Космический лифт запущен!", "The Space Elevator has launched!"), cx, pt + 20f * u, 14f, Col.good, 1)
        ui.text(c, tr("Вы построили целую фабрику на чужой планете.", "You built a whole factory on an alien world."), cx, pt + 48f * u, 10f, Col.text, 1)
        ui.text(c, tr("Время игры: ", "Play time: ") + (g.playTime / 60f).toInt() + tr(" мин", " min"), cx, pt + 68f * u, 9f, Col.dim, 1)
        ui.text(c, "Developer: @yfeeeee", cx, pt + 86f * u, 9f, Col.accent, 1)
        ui.button(c, cx - 100f * u, pt + 112f * u, cx + 100f * u, pt + 142f * u, tr("Играть дальше", "Keep playing"), Col.btnOn) { close() }
    }
}
