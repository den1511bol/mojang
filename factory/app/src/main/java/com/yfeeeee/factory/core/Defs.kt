// Pixel Factory - core: building definitions. Developer: @yfeeeee
package com.yfeeeee.factory.core

enum class Kind {
    CONVEYOR, SPLITTER, MERGER, SORTER, TUNNEL, CONTAINER, MINER, PUMP, MACHINE, GENERATOR,
    SOLAR, GEO, BATTERY, POLE, HUB, MAM, ELEVATOR, FOUNDATION, WALL, LAMP
}

enum class Mach(val ru: String, val en: String) {
    NONE("Руки", "Hands"), SMELTER("Плавильня", "Smelter"), FOUNDRY("Литейная", "Foundry"),
    CONSTRUCTOR("Конструктор", "Constructor"), ASSEMBLER("Сборщик", "Assembler"),
    MANUFACTURER("Фабрика", "Manufacturer"), REFINERY("Нефтезавод", "Refinery")
}

fun c(vararg p: Pair<Item, Int>): List<Pair<Item, Int>> = p.toList()

// cat: 0 logistics, 1 production, 2 power, 3 structure, 4 special
enum class Bld(
    val ru: String, val en: String, val w: Int, val h: Int, val kind: Kind, val cat: Int,
    val power: Float, val cost: List<Pair<Item, Int>>, val ms: String,
    val mach: Mach = Mach.NONE, val v: Float = 0f
) {
    CONVEYOR1("Конвейер Mk1", "Conveyor Mk1", 1, 1, Kind.CONVEYOR, 0, 0f, c(Item.IRON_PLATE to 1), "", v = 1.2f),
    CONVEYOR2("Конвейер Mk2", "Conveyor Mk2", 1, 1, Kind.CONVEYOR, 0, 0f, c(Item.STEEL_BEAM to 1), "belt2", v = 2.4f),
    CONVEYOR3("Конвейер Mk3", "Conveyor Mk3", 1, 1, Kind.CONVEYOR, 0, 0f, c(Item.ENCASED_BEAM to 1), "belt3", v = 4.8f),
    SPLITTER("Разветвитель", "Splitter", 1, 1, Kind.SPLITTER, 0, 0f, c(Item.IRON_PLATE to 2, Item.WIRE to 4), "logi"),
    MERGER("Слиятель", "Merger", 1, 1, Kind.MERGER, 0, 0f, c(Item.IRON_PLATE to 2, Item.IRON_ROD to 2), "logi"),
    SORTER("Сортировщик", "Smart Splitter", 1, 1, Kind.SORTER, 0, 0f, c(Item.IRON_PLATE to 2, Item.COPPER_SHEET to 2, Item.WIRE to 4), "belt2"),
    TUNNEL("Подземный конвейер", "Belt Tunnel", 1, 1, Kind.TUNNEL, 0, 0f, c(Item.IRON_PLATE to 4, Item.CONCRETE to 2), "belt2"),
    CONTAINER("Хранилище", "Storage", 2, 1, Kind.CONTAINER, 0, 0f, c(Item.IRON_PLATE to 4, Item.IRON_ROD to 4), "base"),

    MINER1("Бур Mk1", "Miner Mk1", 2, 2, Kind.MINER, 1, 5f, c(Item.IRON_PLATE to 8, Item.IRON_ROD to 4), "", v = 1f),
    MINER2("Бур Mk2", "Miner Mk2", 2, 2, Kind.MINER, 1, 12f, c(Item.REINFORCED_PLATE to 3, Item.ROTOR to 4), "miner2", v = 2f),
    MINER3("Бур Mk3", "Miner Mk3", 2, 2, Kind.MINER, 1, 30f, c(Item.MODULAR_FRAME to 4, Item.MOTOR to 3), "miner3", v = 4f),
    PUMP("Нефтяная качалка", "Oil Pump", 2, 2, Kind.PUMP, 1, 20f, c(Item.STEEL_PIPE to 6, Item.ROTOR to 2, Item.WIRE to 8), "oil", v = 1f),
    SMELTER("Плавильня", "Smelter", 2, 2, Kind.MACHINE, 1, 4f, c(Item.IRON_ROD to 4, Item.WIRE to 8), "", Mach.SMELTER),
    FOUNDRY("Литейная", "Foundry", 2, 2, Kind.MACHINE, 1, 16f, c(Item.MODULAR_FRAME to 2, Item.ROTOR to 4, Item.CONCRETE to 8), "steel", Mach.FOUNDRY),
    CONSTRUCTOR("Конструктор", "Constructor", 2, 2, Kind.MACHINE, 1, 4f, c(Item.IRON_PLATE to 6, Item.CABLE to 4, Item.SCREW to 20), "base", Mach.CONSTRUCTOR),
    ASSEMBLER("Сборщик", "Assembler", 3, 3, Kind.MACHINE, 1, 15f, c(Item.REINFORCED_PLATE to 2, Item.ROTOR to 4, Item.CABLE to 8), "assembly", Mach.ASSEMBLER),
    MANUFACTURER("Фабрика", "Manufacturer", 4, 4, Kind.MACHINE, 1, 55f, c(Item.MODULAR_FRAME to 6, Item.MOTOR to 4, Item.CABLE to 20), "manuf", Mach.MANUFACTURER),
    REFINERY("Нефтезавод", "Refinery", 3, 3, Kind.MACHINE, 1, 30f, c(Item.STEEL_PIPE to 8, Item.CIRCUIT_BOARD to 4, Item.CONCRETE to 12), "oil", Mach.REFINERY),

    BURNER("Биогенератор", "Biomass Burner", 2, 2, Kind.GENERATOR, 2, 30f, c(Item.IRON_ROD to 6, Item.WIRE to 8), ""),
    COAL_GEN("Угольный генератор", "Coal Generator", 3, 3, Kind.GENERATOR, 2, 75f, c(Item.REINFORCED_PLATE to 4, Item.ROTOR to 3, Item.CABLE to 10), "coal"),
    FUEL_GEN("Топливный генератор", "Fuel Generator", 3, 3, Kind.GENERATOR, 2, 150f, c(Item.MOTOR to 4, Item.CIRCUIT_BOARD to 6, Item.CABLE to 10), "fuelgen"),
    SOLAR("Солнечная панель", "Solar Panel", 3, 3, Kind.SOLAR, 2, 15f, c(Item.REINFORCED_PLATE to 2, Item.CIRCUIT_BOARD to 4, Item.QUARTZ_CRYSTAL to 6), "solar"),
    GEO("Геотермальная станция", "Geothermal Plant", 3, 3, Kind.GEO, 2, 100f, c(Item.MODULAR_FRAME to 4, Item.MOTOR to 3, Item.CIRCUIT_BOARD to 6), "geo"),
    BATTERY("Аккумулятор", "Battery", 2, 2, Kind.BATTERY, 2, 0f, c(Item.PLASTIC to 6, Item.CABLE to 8, Item.STEEL_BEAM to 4), "solar", v = 600f),
    POLE("Опора ЛЭП", "Power Pole", 1, 1, Kind.POLE, 2, 0f, c(Item.IRON_ROD to 1, Item.WIRE to 2), ""),

    HUB("ХАБ", "HUB", 5, 5, Kind.HUB, 4, 0f, c(), "hub"),
    MAM("МАМ (исследования)", "MAM (research)", 2, 2, Kind.MAM, 4, 25f, c(Item.REINFORCED_PLATE to 5, Item.CABLE to 15, Item.WIRE to 45), "mam"),
    ELEVATOR("Космический лифт", "Space Elevator", 6, 6, Kind.ELEVATOR, 4, 0f, c(Item.CONCRETE to 60, Item.REINFORCED_PLATE to 20, Item.ROTOR to 10), "assembly"),
    FOUNDATION("Бетонный пол", "Concrete Floor", 1, 1, Kind.FOUNDATION, 3, 0f, c(Item.CONCRETE to 1), "base"),
    WALL("Бетонная стена", "Concrete Wall", 1, 1, Kind.WALL, 3, 0f, c(Item.CONCRETE to 2), "base"),
    LAMP("Фонарь", "Lamp", 1, 1, Kind.LAMP, 3, 1f, c(Item.IRON_ROD to 2, Item.WIRE to 4), "logi");

    fun name(en: Boolean): String = if (en) this.en else this.ru
    val isGenerator: Boolean get() = kind == Kind.GENERATOR || kind == Kind.SOLAR || kind == Kind.GEO
    val needsPower: Boolean get() = power > 0f && !isGenerator
    val solid: Boolean get() = !(kind == Kind.CONVEYOR || kind == Kind.TUNNEL || kind == Kind.FOUNDATION || kind == Kind.SPLITTER || kind == Kind.MERGER || kind == Kind.SORTER)

    fun fuels(): List<Item> = when (this) {
        BURNER -> listOf(Item.BIOMASS, Item.SOLID_BIOFUEL, Item.WOOD, Item.LEAVES)
        COAL_GEN -> listOf(Item.COAL)
        FUEL_GEN -> listOf(Item.FUEL, Item.SOLID_BIOFUEL)
        else -> emptyList()
    }
}
