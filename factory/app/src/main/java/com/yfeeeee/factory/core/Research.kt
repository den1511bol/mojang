// Pixel Factory - core: milestones (HUB) and space elevator phases. Developer: @yfeeeee
package com.yfeeeee.factory.core

class Milestone(
    val id: String, val tier: Int, val ru: String, val en: String,
    val cost: List<Pair<Item, Int>>, val descRu: String, val descEn: String,
    val elevator: Boolean = false, val perk: String = ""
) {
    fun name(en: Boolean): String = if (en) this.en else this.ru
    fun desc(en: Boolean): String = if (en) descEn else descRu
}

object Research {
    val list = ArrayList<Milestone>()
    private fun m(id: String, tier: Int, ru: String, en: String, cost: List<Pair<Item, Int>>, dRu: String, dEn: String,
                  elevator: Boolean = false, perk: String = "") {
        list.add(Milestone(id, tier, ru, en, cost, dRu, dEn, elevator, perk))
    }

    init {
        m("base", 1, "Строительство", "Base Building", c(Item.IRON_PLATE to 20, Item.IRON_ROD to 20),
            "Конструктор, хранилище, бетонные пол и стены", "Constructor, storage, concrete floor and walls")
        m("logi", 1, "Логистика", "Logistics", c(Item.IRON_ROD to 30, Item.WIRE to 30),
            "Разветвитель, слиятель, фонарь", "Splitter, merger, lamp")
        m("bag1", 1, "Большой рюкзак", "Bigger Backpack", c(Item.IRON_PLATE to 30, Item.SCREW to 60),
            "+10 ячеек инвентаря", "+10 inventory slots", perk = "slots10")

        m("assembly", 2, "Сборка деталей", "Part Assembly", c(Item.IRON_PLATE to 60, Item.CABLE to 20, Item.CONCRETE to 20),
            "Сборщик и Космический лифт", "Assembler and Space Elevator")
        m("coal", 2, "Угольная энергия", "Coal Power", c(Item.CONCRETE to 40, Item.WIRE to 50, Item.IRON_ROD to 40),
            "Угольный генератор, биотопливо", "Coal generator, solid biofuel")
        m("mam", 2, "МАМ", "MAM Research", c(Item.CABLE to 30, Item.CONCRETE to 30, Item.IRON_PLATE to 40),
            "Исследовательская станция: жёсткие диски дают альтернативные рецепты", "Research station: hard drives give alternate recipes")
        m("boots", 2, "Быстрые ботинки", "Quick Boots", c(Item.IRON_ROD to 60, Item.SCREW to 120, Item.CABLE to 10),
            "Скорость бега +25%", "Run speed +25%", perk = "speed")

        m("steel", 3, "Сталь", "Basic Steel", c(Item.REINFORCED_PLATE to 15, Item.CONCRETE to 60, Item.CABLE to 30),
            "Литейная, стальные рецепты, мотор", "Foundry, steel recipes, motor")
        m("belt2", 3, "Конвейеры Mk2", "Improved Logistics", c(Item.REINFORCED_PLATE to 10, Item.IRON_ROD to 80, Item.CABLE to 30),
            "Конвейер Mk2, подземный конвейер, сортировщик", "Conveyor Mk2, belt tunnel, smart splitter")
        m("cat", 3, "Каверий", "Caterium Research", c(Item.REINFORCED_PLATE to 10, Item.ROTOR to 6, Item.CONCRETE to 40),
            "Слитки каверия и быстропровод", "Caterium ingots and quickwire")
        m("elev1", 3, "Лифт: фаза 1", "Elevator: Phase 1", c(Item.SMART_PLATING to 10),
            "Доставьте обшивку в Космический лифт", "Deliver plating to the Space Elevator", elevator = true)

        m("miner2", 4, "Бур Mk2", "Miner Mk2", c(Item.REINFORCED_PLATE to 20, Item.ROTOR to 15, Item.STEEL_BEAM to 40),
            "Бур Mk2 (в 2 раза быстрее)", "Miner Mk2 (twice as fast)")
        m("oil", 4, "Переработка нефти", "Oil Processing", c(Item.STEEL_PIPE to 40, Item.MOTOR to 8, Item.CONCRETE to 100),
            "Качалка, нефтезавод, пластик, резина", "Oil pump, refinery, plastic, rubber")
        m("solar", 4, "Чистая энергия", "Clean Energy", c(Item.STEEL_BEAM to 40, Item.CONCRETE to 100, Item.REINFORCED_PLATE to 30),
            "Солнечные панели, аккумуляторы, кварц", "Solar panels, batteries, quartz")
        m("bag2", 4, "Рюкзак шахтёра", "Miner Backpack", c(Item.MODULAR_FRAME to 8, Item.STEEL_BEAM to 30),
            "+10 ячеек инвентаря", "+10 inventory slots", perk = "slots10")

        m("manuf", 5, "Производство", "Advanced Manufacturing", c(Item.MOTOR to 15, Item.MODULAR_FRAME to 20, Item.CIRCUIT_BOARD to 30),
            "Фабрика, компьютеры, тяжёлые рамы", "Manufacturer, computers, heavy frames")
        m("belt3", 5, "Конвейеры Mk3", "Conveyors Mk3", c(Item.ENCASED_BEAM to 20, Item.MOTOR to 10, Item.CIRCUIT_BOARD to 20),
            "Конвейер Mk3", "Conveyor Mk3")
        m("fuelgen", 5, "Топливная энергия", "Fuel Power", c(Item.MOTOR to 12, Item.PLASTIC to 60, Item.RUBBER to 60),
            "Топливный генератор", "Fuel generator")
        m("elev2", 5, "Лифт: фаза 2", "Elevator: Phase 2", c(Item.SMART_PLATING to 25, Item.VERSATILE_FRAMEWORK to 25),
            "Доставьте детали в Космический лифт", "Deliver parts to the Space Elevator", elevator = true)

        m("miner3", 6, "Бур Mk3", "Miner Mk3", c(Item.MODULAR_FRAME to 20, Item.MOTOR to 20, Item.COMPUTER to 5),
            "Бур Mk3 (в 4 раза быстрее)", "Miner Mk3 (four times faster)")
        m("alu", 6, "Алюминий", "Aluminum", c(Item.COMPUTER to 8, Item.ENCASED_BEAM to 30, Item.CIRCUIT_BOARD to 40),
            "Алюминиевые слитки и корпуса", "Aluminum ingots and casings")
        m("geo", 6, "Геотермальная энергия", "Geothermal", c(Item.COMPUTER to 10, Item.MOTOR to 20, Item.HEAVY_FRAME to 5),
            "Геотермальная станция на гейзерах", "Geothermal plant on geysers")
        m("elev3", 6, "Лифт: фаза 3", "Elevator: Phase 3", c(Item.VERSATILE_FRAMEWORK to 40, Item.AUTOMATED_WIRING to 20, Item.MODULAR_ENGINE to 10),
            "Доставьте детали в Космический лифт", "Deliver parts to the Space Elevator", elevator = true)

        m("speed2", 7, "Реактивные ботинки", "Jet Boots", c(Item.MODULAR_ENGINE to 10, Item.COMPUTER to 15, Item.AI_LIMITER to 10),
            "Скорость бега +25%", "Run speed +25%", perk = "speed")
        m("elev4", 7, "Лифт: фаза 4", "Elevator: Phase 4", c(Item.AUTOMATED_WIRING to 40, Item.MODULAR_ENGINE to 25, Item.ADAPTIVE_CONTROL_UNIT to 5, Item.AI_LIMITER to 20),
            "Доставьте детали в Космический лифт", "Deliver parts to the Space Elevator", elevator = true)

        m("elev5", 8, "Лифт: фаза 5 (финал)", "Elevator: Phase 5 (final)", c(Item.ADAPTIVE_CONTROL_UNIT to 15, Item.MODULAR_ENGINE to 40, Item.ALUMINUM_CASING to 100, Item.COMPUTER to 20),
            "Последний груз для Космического лифта!", "The last shipment for the Space Elevator!", elevator = true)
    }

    fun byId(id: String): Milestone? { for (x in list) if (x.id == id) return x; return null }
    fun maxTier(): Int = list.maxOf { it.tier }
}

/** Player progression state: completed milestones, delivered items, unlocked alt recipes. */
class Progress {
    val done = HashSet<String>()
    val delivered = HashMap<String, IntArray>()   // milestone id -> delivered per cost index
    val altRecipes = HashSet<String>()
    var active: String = ""
    var hardDrives = 0
    var won = false

    fun tierOpen(t: Int): Boolean {
        if (t <= 1) return true
        for (m in Research.list) if (m.tier == t - 1 && !done.contains(m.id)) return false
        return true
    }

    fun available(m: Milestone): Boolean = !done.contains(m.id) && tierOpen(m.tier)
    fun has(ms: String): Boolean = ms.isEmpty() || done.contains(ms)
    fun canBuild(b: Bld): Boolean = has(b.ms)
    fun canMake(r: Recipe): Boolean = (!r.alt && has(r.ms)) || (r.alt && altRecipes.contains(r.key))

    fun currentTier(): Int {
        var t = 0
        for (i in 1..Research.maxTier()) if (tierOpen(i)) t = i
        return t
    }

    fun deliveredArr(m: Milestone): IntArray {
        var a = delivered[m.id]
        if (a == null) { a = IntArray(m.cost.size); delivered[m.id] = a }
        return a
    }

    fun need(m: Milestone, item: Item): Int {
        val arr = deliveredArr(m)
        for (i in m.cost.indices) if (m.cost[i].first == item) return m.cost[i].second - arr[i]
        return 0
    }

    /** Deliver up to n items; returns how many were taken. */
    fun deliver(m: Milestone, item: Item, n: Int): Int {
        val arr = deliveredArr(m)
        var taken = 0
        for (i in m.cost.indices) if (m.cost[i].first == item) {
            val take = minOf(n, m.cost[i].second - arr[i])
            if (take > 0) { arr[i] += take; taken += take }
        }
        return taken
    }

    fun complete(m: Milestone): Boolean {
        val arr = deliveredArr(m)
        for (i in m.cost.indices) if (arr[i] < m.cost[i].second) return false
        return true
    }
}
