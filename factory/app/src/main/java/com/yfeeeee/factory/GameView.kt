// Pixel Factory - game view: loop, input, HUD, menus, build mode, save/load. Developer: @yfeeeee
package com.yfeeeee.factory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.yfeeeee.factory.core.*
import com.yfeeeee.factory.engine.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue

class Ptr(val id: Int, val x0: Float, val y0: Float) {
    var x = x0
    var y = y0
    var kind = 0          // 0 world, 1 stick, 2 button, 4 hold button, 5 build drag, 6 scroll, -1 ignored
    var btn: Btn? = null
    var moved = false
    var lastTx = 0
    var lastTy = 0
}

class GameView(val act: MainActivity) : SurfaceView(act), SurfaceHolder.Callback, Runnable, Callbacks {
    val st = AppState()
    private val lock = Any()
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var assetsReady = false
    private var assetsStarted = false
    private var sp: Sprites? = null
    private var rend: Renderer? = null
    private var ui: Ui? = null
    private var panels: Panels? = null
    private val snd = Sfx()
    private val events = ConcurrentLinkedQueue<FloatArray>()
    private val ptrs = HashMap<Int, Ptr>()
    private var W = 0
    private var H = 0
    private var sizeDirty = false
    private val pnt = Paint()
    private val loadPaint = Paint()

    // stick
    private var stickPid = -1
    private var stickCx = 0f
    private var stickCy = 0f
    private var stickX = 0f
    private var stickY = 0f

    private var menuT = 0f
    private var confirmNew = false
    private var autoT = 0f
    private var loadT = 0f

    // sound edge detection
    private var pHurt = 0f
    private var pToasts = 0
    private var pAtk = 0f
    private var pAct = 0f
    private var pDrives = 0

    init {
        holder.addCallback(this)
        isFocusable = true
        pnt.isAntiAlias = false
        loadPaint.color = 0xFFFFFFFF.toInt()
        loadPaint.textSize = 40f
    }

    private fun tr(ru: String, en: String): String = if (st.en) en else ru

    // ------------------------------------------------------------------ lifecycle
    override fun surfaceCreated(h: SurfaceHolder) {
        ensureAssets()
        startLoop()
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, hh: Int) {
        W = w; H = hh
        sizeDirty = true
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        val t = Thread(this, "pf-loop")
        thread = t
        t.start()
    }

    private fun stopLoop() {
        running = false
        val t = thread
        if (t != null) {
            try { t.join(800) } catch (e: InterruptedException) { }
        }
        thread = null
    }

    private fun ensureAssets() {
        if (assetsStarted) return
        assetsStarted = true
        Thread {
            try {
                val prefs = act.getSharedPreferences("pf", Context.MODE_PRIVATE)
                val defEn = java.util.Locale.getDefault().language != "ru"
                st.en = prefs.getBoolean("en", defEn)
                st.sfxOn = prefs.getBoolean("sfx", true)
                snd.enabled = st.sfxOn
                val s = Sprites()
                val r = Renderer(s)
                val u = Ui(s)
                val p = Panels(u, s, st, this)
                snd.init()
                st.hasSave = File(act.filesDir, "save.bin").exists()
                sp = s; rend = r; ui = u; panels = p
                st.screen = 1
                sizeDirty = true
                assetsReady = true
            } catch (e: Throwable) {
                st.loadMsg = "Init error: " + e
            }
        }.start()
    }

    fun pauseGame() {
        synchronized(lock) { if (st.screen == 2) saveNow() }
    }

    fun resumeGame() { }

    /** returns true if the back press was consumed */
    fun back(): Boolean {
        synchronized(lock) {
            val pn = panels
            if (st.screen == 0) return true
            if (st.screen == 1) {
                if (st.panel != P.NONE && pn != null) { pn.close(); return true }
                return false
            }
            if (st.panel != P.NONE && pn != null) { pn.close(); return true }
            if (st.buildDef != null || st.demolish) { st.buildDef = null; st.demolish = false; return true }
            if (pn != null) pn.open(P.PAUSE)
            return true
        }
    }

    // ------------------------------------------------------------------ callbacks
    override fun sfx(n: String) { snd.play(n) }

    override fun saveNow() {
        val g = st.g ?: return
        try {
            val f = File(act.filesDir, "save.bin")
            val tmp = File(act.filesDir, "save.tmp")
            val os = BufferedOutputStream(FileOutputStream(tmp))
            Save.write(g, os)
            os.flush()
            os.close()
            if (f.exists()) f.delete()
            tmp.renameTo(f)
            st.hasSave = true
        } catch (e: Throwable) {
            g.toast("Save error")
        }
    }

    override fun toMenu() {
        st.screen = 1
        st.g = null
        st.panel = P.NONE
        st.buildDef = null
        st.demolish = false
        ptrs.clear()
        stickPid = -1
        stickX = 0f; stickY = 0f
        confirmNew = false
    }

    override fun zoom(d: Int) {
        val r = rend ?: return
        r.changeScale(r.scale + d)
    }

    override fun toggleSfx() {
        st.sfxOn = !st.sfxOn
        snd.enabled = st.sfxOn
        act.getSharedPreferences("pf", Context.MODE_PRIVATE).edit().putBoolean("sfx", st.sfxOn).apply()
        snd.play("click")
    }

    private fun saveLang() {
        act.getSharedPreferences("pf", Context.MODE_PRIVATE).edit().putBoolean("en", st.en).apply()
    }

    private fun startGame(cont: Boolean) {
        st.screen = 0
        st.loadMsg = tr("Генерация мира...", "Generating world...")
        loadT = 0f
        Thread {
            var g: Game? = null
            var fresh = false
            if (cont) {
                try {
                    val ins = BufferedInputStream(FileInputStream(File(act.filesDir, "save.bin")))
                    g = Save.read(ins)
                    ins.close()
                } catch (e: Throwable) {
                    g = null
                }
            }
            if (g == null) {
                val ng = Game(Random().nextInt(1000000))
                ng.newGame()
                g = ng
                fresh = true
            }
            val fin: Game = g
            fin.en = st.en
            if (fresh) {
                fin.toast(if (st.en) "Tap an ore patch to mine it. The hammer opens the build menu." else "Коснитесь месторождения, чтобы добывать. Молоток - меню стройки.")
                fin.toast(if (st.en) "Build a Miner + Smelter + Power. Open the HUB for goals." else "Постройте Бур, Плавильню и энергию. В ХАБе - цели.")
            }
            synchronized(lock) {
                st.g = fin
                st.panel = P.NONE
                st.buildDef = null
                st.demolish = false
                st.winShown = fin.progress.won
                ptrs.clear()
                stickPid = -1; stickX = 0f; stickY = 0f
                val r = rend
                if (r != null) { r.camX = fin.player.x; r.camY = fin.player.y }
                st.screen = 2
                autoT = 0f
                confirmNew = false
                if (fresh) saveNow()
            }
        }.start()
    }

    // ------------------------------------------------------------------ loop
    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1e9f
            last = now
            if (dt > 0.05f) dt = 0.05f
            var locked: Canvas? = null
            try { locked = holder.lockCanvas() } catch (e: Exception) { locked = null }
            val cv = locked
            if (cv != null) {
                try {
                    synchronized(lock) { frame(cv, dt) }
                } catch (e: Throwable) {
                    st.loadMsg = "Error: " + e
                } finally {
                    try { holder.unlockCanvasAndPost(cv) } catch (e: Exception) { }
                }
            }
            val spent = (System.nanoTime() - now) / 1000000L
            if (spent < 14L) {
                try { Thread.sleep(14L - spent) } catch (e: InterruptedException) { }
            }
        }
    }

    private fun frame(c: Canvas, dt: Float) {
        val u = ui
        val r = rend
        if (!assetsReady || u == null || r == null) {
            c.drawColor(rgb(12, 16, 24))
            c.drawText("Pixel Factory...", 40f, 80f, loadPaint)
            if (st.loadMsg.isNotEmpty()) c.drawText(st.loadMsg, 40f, 140f, loadPaint)
            return
        }
        if (sizeDirty && W > 0 && H > 0) {
            u.resize(W, H); r.resize(W, H); sizeDirty = false
        }
        processEvents(dt)
        u.begin()
        when (st.screen) {
            0 -> drawLoading(c, u, dt)
            1 -> drawMenu(c, u, dt)
            else -> drawPlay(c, u, r, dt)
        }
        if (st.loadMsg.startsWith("Error") && st.screen != 0) {
            u.text(c, st.loadMsg, 10f, 10f, 7f, Col.bad)
        }
    }

    // ------------------------------------------------------------------ input
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                events.add(floatArrayOf(0f, e.getPointerId(i).toFloat(), e.getX(i), e.getY(i)))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) events.add(floatArrayOf(1f, e.getPointerId(i).toFloat(), e.getX(i), e.getY(i)))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                events.add(floatArrayOf(2f, e.getPointerId(i).toFloat(), e.getX(i), e.getY(i)))
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until e.pointerCount) events.add(floatArrayOf(3f, e.getPointerId(i).toFloat(), e.getX(i), e.getY(i)))
            }
        }
        return true
    }

    private fun processEvents(dt: Float) {
        var ev = events.poll()
        while (ev != null) {
            val a = ev[0].toInt(); val id = ev[1].toInt()
            when (a) {
                0 -> onDown(id, ev[2], ev[3])
                1 -> onMove(id, ev[2], ev[3])
                2 -> onUp(id, ev[2], ev[3], false)
                else -> onUp(id, ev[2], ev[3], true)
            }
            ev = events.poll()
        }
        for (p in ptrs.values) {
            if (p.kind == 4) { val b = p.btn; if (b != null) b.act() }
        }
    }

    private fun hitBtn(x: Float, y: Float): Btn? {
        val u = ui ?: return null
        for (i in u.btns.indices.reversed()) {
            val b = u.btns[i]
            if (b.hit(x, y)) return b
        }
        return null
    }

    private fun inBuildMode(): Boolean = st.screen == 2 && st.panel == P.NONE && (st.buildDef != null || st.demolish)

    private fun inStickZone(x: Float, y: Float): Boolean {
        return if (inBuildMode()) (x < W * 0.28f && y > H * 0.5f) else x < W * 0.42f
    }

    private fun onDown(id: Int, x: Float, y: Float) {
        val u = ui ?: return
        val p = Ptr(id, x, y)
        ptrs[id] = p
        val b = hitBtn(x, y)
        if (st.screen == 2 && st.panel == P.NONE) {
            // HUD buttons win over the world; background area buttons do not exist in play mode
            if (b != null) {
                if (b.hold) { p.kind = 4; p.btn = b } else { p.kind = 2; p.btn = b }
                return
            }
            if (stickPid < 0 && inStickZone(x, y)) {
                stickPid = id; stickCx = x; stickCy = y; stickX = 0f; stickY = 0f
                p.kind = 1
                return
            }
            if (inBuildMode() && st.buildDef != null) {
                p.kind = 5
                val g = st.g
                val r = rend
                if (g != null && r != null && st.buildDef != null) {
                    placeGhostAt(g, r, x, y)
                    p.lastTx = st.ghostX; p.lastTy = st.ghostY
                }
                return
            }
            p.kind = 0
            return
        }
        if (b != null) {
            if (b.hold) { p.kind = 4; p.btn = b } else { p.kind = 2; p.btn = b }
        } else p.kind = -1
        if (u.btns.isEmpty()) p.kind = -1
    }

    private fun onMove(id: Int, x: Float, y: Float) {
        val u = ui ?: return
        val p = ptrs[id] ?: return
        val oldY = p.y
        p.x = x; p.y = y
        val th = 10f * u.u
        when (p.kind) {
            1 -> {
                var dx = x - stickCx; var dy = y - stickCy
                val rad = 42f * u.u
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d > rad) {
                    // drag the base along so the stick feels floating
                    val k = (d - rad) / d
                    stickCx += dx * k; stickCy += dy * k
                    dx = x - stickCx; dy = y - stickCy
                }
                stickX = Math.max(-1f, Math.min(1f, dx / rad))
                stickY = Math.max(-1f, Math.min(1f, dy / rad))
            }
            2 -> {
                if (!p.moved) {
                    val dx = x - p.x0; val dy = y - p.y0
                    if (dx * dx + dy * dy > th * th) {
                        p.moved = true
                        if (u.hasScroll && p.x0 >= u.scrollL && p.x0 <= u.scrollR && p.y0 >= u.scrollT && p.y0 <= u.scrollB) {
                            p.kind = 6
                            u.scroll -= (y - p.y0)
                        } else p.kind = -1
                    }
                }
            }
            6 -> u.scroll -= (y - oldY)
            -1 -> {
                if (u.hasScroll && st.screen == 2 && p.x0 >= u.scrollL && p.x0 <= u.scrollR && p.y0 >= u.scrollT && p.y0 <= u.scrollB) u.scroll -= (y - oldY)
            }
            5 -> {
                p.moved = true
                val g = st.g
                val r = rend
                if (g != null && r != null) buildDrag(g, r, p, x, y)
            }
            0 -> {
                if (!p.moved) {
                    val dx = x - p.x0; val dy = y - p.y0
                    if (dx * dx + dy * dy > th * th) p.moved = true
                }
            }
        }
    }

    private fun onUp(id: Int, x: Float, y: Float, cancel: Boolean) {
        val p = ptrs.remove(id) ?: return
        when (p.kind) {
            1 -> { if (stickPid == id) { stickPid = -1; stickX = 0f; stickY = 0f } }
            2 -> {
                val b = p.btn
                if (!cancel && !p.moved && b != null && b.hit(x, y)) b.act()
            }
            0 -> {
                if (!cancel && !p.moved) worldTap(x, y)
            }
            5 -> {
                if (!cancel && p.moved) buildDragEnd(p)
            }
        }
    }

    // ------------------------------------------------------------------ world interaction
    private fun worldTap(x: Float, y: Float) {
        val g = st.g ?: return
        val r = rend ?: return
        if (st.demolish && st.buildDef == null) { demolishTap(g, r, x, y); return }
        g.tapWorld(r.worldX(x), r.worldY(y))
        snd.play("click")
    }

    private fun placeGhostAt(g: Game, r: Renderer, x: Float, y: Float) {
        val d = st.buildDef ?: return
        val w = g.footprintW(d, st.buildRot); val h = g.footprintH(d, st.buildRot)
        st.ghostX = Math.floor((r.worldX(x) - w / 2f + 0.5f).toDouble()).toInt()
        st.ghostY = Math.floor((r.worldY(y) - h / 2f + 0.5f).toDouble()).toInt()
    }

    private fun tooFar(g: Game, tx: Int, ty: Int): Boolean {
        val dx = tx + 0.5f - g.player.x; val dy = ty + 0.5f - g.player.y
        return dx * dx + dy * dy > 26f * 26f
    }

    private fun buildDrag(g: Game, r: Renderer, p: Ptr, x: Float, y: Float) {
        val d = st.buildDef ?: return
        placeGhostAt(g, r, x, y)
        if (d.kind != Kind.CONVEYOR) return
        val nx = st.ghostX; val ny = st.ghostY
        var guard = 0
        while ((p.lastTx != nx || p.lastTy != ny) && guard < 40) {
            guard++
            val ddx = nx - p.lastTx; val ddy = ny - p.lastTy
            val dir = if (Math.abs(ddx) >= Math.abs(ddy)) (if (ddx > 0) 0 else 2) else (if (ddy > 0) 1 else 3)
            if (!tooFar(g, p.lastTx, p.lastTy)) {
                if (g.placeConveyor(d, p.lastTx, p.lastTy, dir)) snd.play("place")
            }
            st.buildRot = dir
            p.lastTx += DX[dir]; p.lastTy += DY[dir]
        }
        st.ghostX = p.lastTx; st.ghostY = p.lastTy
    }

    private fun buildDragEnd(p: Ptr) {
        val g = st.g ?: return
        val d = st.buildDef ?: return
        if (d.kind != Kind.CONVEYOR) return
        if (!tooFar(g, p.lastTx, p.lastTy)) {
            if (g.placeConveyor(d, p.lastTx, p.lastTy, st.buildRot)) snd.play("place")
        }
        st.ghostX = p.lastTx + DX[st.buildRot]
        st.ghostY = p.lastTy + DY[st.buildRot]
    }

    private fun confirmPlace(g: Game) {
        val d = st.buildDef ?: return
        if (tooFar(g, st.ghostX, st.ghostY)) { g.toast(tr("Слишком далеко", "Too far away")); snd.play("deny"); return }
        if (!g.progress.canBuild(d)) { snd.play("deny"); return }
        val chk = g.placeCheck(d, st.ghostX, st.ghostY, st.buildRot)
        if (chk != 0 && !(d.kind == Kind.CONVEYOR && isConveyorAt(g, st.ghostX, st.ghostY))) {
            g.toast(if (chk == 2) tr("Нужно подходящее месторождение", "Needs a matching resource node") else if (chk == 3) tr("Здесь нельзя (лимит)", "Not allowed (limit)") else tr("Место занято", "Blocked"))
            snd.play("deny")
            return
        }
        if (!g.canAfford(d)) { g.toast(tr("Не хватает материалов", "Not enough materials")); snd.play("deny"); return }
        val ok: Boolean
        if (d.kind == Kind.CONVEYOR) ok = g.placeConveyor(d, st.ghostX, st.ghostY, st.buildRot)
        else ok = g.place(d, st.ghostX, st.ghostY, st.buildRot) != null
        if (ok) {
            snd.play("place")
            if (d.kind == Kind.CONVEYOR) {
                st.ghostX += DX[st.buildRot]; st.ghostY += DY[st.buildRot]
            } else if (d.kind == Kind.ELEVATOR) {
                st.buildDef = null
            }
        } else snd.play("deny")
    }

    private fun isConveyorAt(g: Game, x: Int, y: Int): Boolean {
        val b = g.occAt(x, y)
        return b != null && b.def.kind == Kind.CONVEYOR
    }

    private fun demolishTap(g: Game, r: Renderer, x: Float, y: Float) {
        val tx = Math.floor(r.worldX(x).toDouble()).toInt(); val ty = Math.floor(r.worldY(y).toDouble()).toInt()
        val b = g.occAt(tx, ty) ?: return
        if (tooFar(g, tx, ty)) { g.toast(tr("Слишком далеко", "Too far away")); return }
        if (b.def.kind == Kind.HUB) { g.toast(tr("ХАБ нельзя убрать", "The HUB can't be removed")); return }
        g.remove(b)
        snd.play("place")
    }

    // ------------------------------------------------------------------ screens
    private fun drawLoading(c: Canvas, u: Ui, dt: Float) {
        loadT += dt
        c.drawColor(rgb(12, 16, 24))
        val cx = W / 2f; val cy = H / 2f
        u.text(c, "PIXEL FACTORY", cx, cy - 50f * u.u, 20f, Col.accent, 1)
        u.text(c, st.loadMsg, cx, cy - 10f * u.u, 10f, Col.text, 1)
        val bw = 200f * u.u
        u.bar(c, cx - bw / 2f, cy + 14f * u.u, cx + bw / 2f, cy + 26f * u.u, (loadT % 2f) / 2f, Col.accent)
        u.text(c, "Developer: @yfeeeee", cx, cy + 40f * u.u, 8f, Col.dim, 1)
    }

    private fun drawMenu(c: Canvas, u: Ui, dt: Float) {
        val s = sp ?: return
        menuT += dt
        c.drawColor(rgb(16, 22, 30))
        val ts = 16f * Math.max(3, Math.round(H / 200f))
        val off = (menuT * 14f) % ts
        val cols = (W / ts).toInt() + 2
        val rows = (H / ts).toInt() + 2
        for (ty in 0 until rows) for (tx in 0 until cols) {
            val gt = if (hash2(tx + (menuT * 14f / ts).toInt(), ty, 3) % 9 == 0) T.DIRT else T.GRASS
            val bm = s.ground[gt][hash2(tx + (menuT * 14f / ts).toInt(), ty, 5) and 3]
            val l = tx * ts - off; val t = ty * ts
            u.bitmap(c, bm, l, t, ts + 1f, ts + 1f)
        }
        u.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), argb(150, 6, 10, 18))
        val cx = W / 2f
        val uu = u.u
        // belt with items crossing the title
        val by = H * 0.50f
        for (i in 0 until 40) {
            val x = ((i * 40f * uu + menuT * 30f * uu) % (W + 80f * uu)) - 40f * uu
            val itm = ITEMS[(i * 7) % 12]
            u.item(c, itm, x, by, 12f * uu)
        }
        u.rect(c, 0f, by + 13f * uu, W.toFloat(), by + 15f * uu, rgb(70, 76, 90))
        u.text(c, "PIXEL", cx, H * 0.10f, 38f, Col.accent, 1)
        u.text(c, "FACTORY", cx, H * 0.10f + 40f * uu, 38f, Col.text, 1)
        u.text(c, tr("Постройте фабрику на чужой планете", "Build a factory on an alien planet"), cx, H * 0.10f + 86f * uu, 9f, Col.dim, 1)
        val bw = 200f * uu; val bh = 28f * uu; val gap = 7f * uu
        var y = H * 0.50f + 26f * uu
        if (st.hasSave) {
            u.button(c, cx - bw - gap / 2f, y, cx - gap / 2f, y + bh, tr("Продолжить", "Continue"), Col.btnOn, true, 10f) { startGame(true) }
            u.button(c, cx + gap / 2f, y, cx + bw + gap / 2f, y + bh, if (confirmNew) tr("Точно? Сохранение сотрётся", "Sure? Save will be erased") else tr("Новая игра", "New game"), if (confirmNew) Col.danger else Col.btn, true, 9f) {
                if (confirmNew) startGame(false) else confirmNew = true
            }
        } else {
            u.button(c, cx - bw / 2f, y, cx + bw / 2f, y + bh, tr("Новая игра", "New game"), Col.btnOn, true, 10f) { startGame(false) }
        }
        y += bh + gap
        u.button(c, cx - bw - gap / 2f, y, cx - gap / 2f, y + bh, tr("Язык: Русский", "Language: English"), Col.btn, true, 9f) { st.en = !st.en; saveLang(); snd.play("click") }
        u.button(c, cx + gap / 2f, y, cx + bw + gap / 2f, y + bh, tr("Помощь", "Help"), Col.btn, true, 9f) { panels?.open(P.HELP) }
        y += bh + gap
        u.button(c, cx - bw / 2f, y, cx + bw / 2f, y + bh, tr("Звук: ", "Sound: ") + (if (st.sfxOn) "ON" else "OFF"), Col.btn, true, 9f) { toggleSfx() }
        u.text(c, "Developer: @yfeeeee", cx, H - 16f * uu, 8f, Col.accent, 1)
        if (st.panel == P.HELP) panels?.draw(c, dt)
    }

    private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

    private fun drawPlay(c: Canvas, u: Ui, r: Renderer, dt: Float) {
        val g = st.g ?: return
        val pn = panels ?: return
        val p = g.player
        val paused = st.panel == P.PAUSE || st.panel == P.HELP || st.panel == P.WIN
        val noMove = st.panel != P.NONE
        if (!paused) {
            val ix = if (noMove) 0f else (if (Math.abs(stickX) < 0.12f && Math.abs(stickY) < 0.12f) 0f else stickX)
            val iy = if (noMove) 0f else (if (Math.abs(stickX) < 0.12f && Math.abs(stickY) < 0.12f) 0f else stickY)
            g.update(dt, ix, iy)
            autoT += dt
            if (autoT > 45f) { autoT = 0f; saveNow() }
        }
        if (g.openBuilding != null) {
            val ob = g.openBuilding
            g.openBuilding = null
            if (ob != null && st.panel == P.NONE && !inBuildMode()) pn.openBuilding(ob)
        }
        if (g.progress.won && !st.winShown) { st.winShown = true; pn.open(P.WIN); snd.play("good") }

        // sounds
        if (p.hurt > pHurt + 0.1f) snd.play("hurt")
        pHurt = p.hurt
        if (g.toasts.size > pToasts) snd.play("toast")
        pToasts = g.toasts.size
        if (p.atkCd > pAtk + 0.25f) snd.play("hit")
        pAtk = p.atkCd
        if (pAct > 0.2f && p.actT < pAct - 0.2f && (p.tkind == 1 || p.tkind == 2 || p.tkind == 3)) snd.play(if (p.tkind == 3) "pod" else "mine")
        pAct = p.actT
        val drives = p.count(Item.HARD_DRIVE)
        if (drives > pDrives) snd.play("good")
        pDrives = drives

        r.showPower = st.showPower
        var ghost: Renderer.Ghost? = null
        val bd = st.buildDef
        if (bd != null && st.panel == P.NONE) {
            val ok = g.placeCheck(bd, st.ghostX, st.ghostY, st.buildRot) == 0 && g.canAfford(bd) && g.progress.canBuild(bd) && !tooFar(g, st.ghostX, st.ghostY)
            ghost = Renderer.Ghost(bd, st.ghostX, st.ghostY, st.buildRot, ok)
        }
        r.render(c, g, if (paused) 0f else dt, ghost, true)
        if (p.hurt > 0f) u.rect(c, 0f, 0f, W.toFloat(), H.toFloat(), argb((p.hurt * 120f).toInt(), 200, 20, 20))

        drawHud(c, u, g, pn)
        if (st.panel != P.NONE) pn.draw(c, dt)
    }

    private fun drawHud(c: Canvas, u: Ui, g: Game, pn: Panels) {
        val uu = u.u
        val m = 8f * uu
        val p = g.player
        // health
        u.icon(c, "heart", m, m, 16f * uu)
        u.bar(c, m + 19f * uu, m + 3f * uu, m + 130f * uu, m + 13f * uu, p.hp / p.maxHp, rgb(220, 60, 70))
        u.text(c, p.hp.toInt().toString(), m + 134f * uu, m + 2f * uu, 8f, Col.text)
        // time
        val hrs = (g.time * 24f).toInt()
        val mins = ((g.time * 24f - hrs) * 60f).toInt()
        u.icon(c, if (g.daylight() > 0.5f) "sun" else "moon", m, m + 20f * uu, 14f * uu)
        u.text(c, tr("День ", "Day ") + g.days + "  " + two(hrs) + ":" + two(mins), m + 18f * uu, m + 22f * uu, 8.5f, Col.text)
        var y = m + 38f * uu
        if (g.totalCap > 0f || g.totalDemand > 0f) {
            u.icon(c, "bolt", m, y, 14f * uu)
            val low = g.totalDemand > g.totalCap + 0.01f
            var s = g.totalCap.toInt().toString() + "/" + g.totalDemand.toInt() + " MW"
            if (g.totalBatMax > 0f) s += "  [" + (100f * g.totalBat / g.totalBatMax).toInt() + "%]"
            u.text(c, s, m + 18f * uu, y + 2f * uu, 8.5f, if (low) Col.bad else Col.good)
            y += 16f * uu
        }
        val am = g.activeHubMilestone()
        if (am != null) {
            val arr = g.progress.deliveredArr(am)
            var have = 0; var need = 0
            for (k in am.cost.indices) { have += Math.min(arr[k], am.cost[k].second); need += am.cost[k].second }
            u.textFit(c, tr("Веха: ", "Goal: ") + am.name(st.en) + " " + (100 * have / Math.max(1, need)) + "%", m, y, 170f * uu, 8f, Col.accent)
        } else if (!g.progress.won) {
            u.textFit(c, tr("Выберите веху в ХАБе", "Pick a milestone at the HUB"), m, y, 170f * uu, 8f, Col.dim)
        }
        // top-right buttons
        val bs = 30f * uu
        u.iconButton(c, "menu", W - m - bs, m, bs, Col.btn) { pn.open(P.PAUSE) }
        u.iconButton(c, "map", W - m - bs * 2f - 5f * uu, m, bs, Col.btn) { pn.open(P.MAP) }
        // inventory slots
        u.text(c, p.slotsUsed().toString() + "/" + p.slots, W - m - bs * 2f - 10f * uu, m + 10f * uu, 8f, Col.dim, 2)

        // toasts
        var ty = m + 4f * uu
        for (t in g.toasts) {
            val col = if (t.t < 0.8f) Col.dim else Col.text
            val w = u.tw(t.msg, 8.5f) + 12f * uu
            val x0 = W / 2f - w / 2f
            u.rect(c, x0, ty - 2f * uu, x0 + w, ty + 12f * uu, argb(150, 8, 12, 20))
            u.text(c, t.msg, W / 2f, ty, 8.5f, col, 1)
            ty += 15f * uu
        }

        // right-bottom action buttons
        val ab = 48f * uu
        val ax = W - m - ab; val ay = H - m - ab
        u.holdButton(c, "sword", ax, ay, ab, rgb(120, 50, 50)) { g.attackFront() }
        val sb = 32f * uu
        u.iconButton(c, "run", ax - sb - 8f * uu, ay + ab - sb, sb, Col.btn, p.sprint) { p.sprint = !p.sprint }
        val berries = p.count(Item.BERRIES)
        u.iconButton(c, "berry", ax + (ab - sb) / 2f, ay - sb - 8f * uu, sb, Col.btn) { g.eatBerries(); snd.play("good") }
        u.text(c, berries.toString(), ax + (ab - sb) / 2f + sb, ay - sb - 8f * uu, 8f, Col.text, 2)

        val d = st.buildDef
        if (d != null || st.demolish) {
            drawBuildControls(c, u, g, d)
        } else {
            val cs = 36f * uu
            val gap = 6f * uu
            val x0 = W / 2f - (cs * 3f + gap * 2f) / 2f
            val by = H - m - cs
            u.iconButton(c, "bag", x0, by, cs, Col.btn) { pn.open(P.INV) }
            u.iconButton(c, "gear", x0 + cs + gap, by, cs, Col.btn) { pn.open(P.CRAFT) }
            u.iconButton(c, "hammer", x0 + (cs + gap) * 2f, by, cs, Col.btn) { pn.open(P.BUILD) }
        }

        // joystick
        pnt.style = Paint.Style.FILL
        if (stickPid >= 0) {
            pnt.color = argb(60, 255, 255, 255)
            c.drawCircle(stickCx, stickCy, 42f * uu, pnt)
            pnt.color = argb(150, 255, 255, 255)
            c.drawCircle(stickCx + stickX * 42f * uu, stickCy + stickY * 42f * uu, 18f * uu, pnt)
        } else if (!inBuildMode()) {
            pnt.color = argb(36, 255, 255, 255)
            c.drawCircle(m + 56f * uu, H - m - 56f * uu, 42f * uu, pnt)
        }
    }

    private fun drawBuildControls(c: Canvas, u: Ui, g: Game, d: Bld?) {
        val uu = u.u
        val m = 8f * uu
        val bs = 40f * uu
        val gap = 6f * uu
        val by = H - m - bs
        if (d == null) {
            u.text(c, tr("Режим сноса: коснитесь постройки", "Demolish mode: tap a building"), W / 2f, by - 18f * uu, 9f, Col.bad, 1)
            u.iconButton(c, "x", W / 2f - bs / 2f, by, bs, Col.danger) { st.demolish = false }
            return
        }
        u.text(c, d.name(st.en) + " " + g.footprintW(d, st.buildRot) + "x" + g.footprintH(d, st.buildRot), W / 2f, by - 36f * uu, 9f, Col.accent, 1)
        var tw = 0f
        for (pr in d.cost) tw += u.chip(c, pr.first, pr.second, -1000f, -1000f, 10f, true)
        var x = W / 2f - tw / 2f
        for (pr in d.cost) x += u.chip(c, pr.first, pr.second, x, by - 22f * uu, 10f, g.player.count(pr.first) >= pr.second)
        val x0 = W / 2f - (bs * 3f + gap * 2f) / 2f
        u.iconButton(c, "ok", x0, by, bs, Col.btnOn) { confirmPlace(g) }
        u.iconButton(c, "rot", x0 + bs + gap, by, bs, Col.btn) {
            val old = st.buildRot
            st.buildRot = (old + 1) % 4
            val dd = st.buildDef
            if (dd != null) {
                // keep the centre of the footprint stable when rotating
                val ow = g.footprintW(dd, old); val oh = g.footprintH(dd, old)
                val nw = g.footprintW(dd, st.buildRot); val nh = g.footprintH(dd, st.buildRot)
                st.ghostX += (ow - nw) / 2
                st.ghostY += (oh - nh) / 2
            }
            snd.play("click")
        }
        u.iconButton(c, "x", x0 + (bs + gap) * 2f, by, bs, Col.danger) { st.buildDef = null }
    }
}
