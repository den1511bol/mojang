// Pixel Factory - core: recipes. Developer: @yfeeeee
package com.yfeeeee.factory.core

class Recipe(
    val key: String, val mach: Mach, val ins: List<Pair<Item, Int>>, val outs: List<Pair<Item, Int>>,
    val time: Float, val ru: String, val en: String, val hand: Boolean = false, val alt: Boolean = false, val ms: String = ""
) {
    fun name(en: Boolean): String = if (en) this.en else this.ru
    fun accepts(i: Item): Boolean = ins.any { it.first == i }
    fun needOf(i: Item): Int { for (p in ins) if (p.first == i) return p.second; return 0 }
}

object Recipes {
    val all = ArrayList<Recipe>()
    private fun r(key: String, m: Mach, ins: List<Pair<Item, Int>>, outs: List<Pair<Item, Int>>, t: Float,
                  ms: String = "", hand: Boolean = false, alt: Boolean = false, ru: String = "", en: String = ""): Recipe {
        val first = outs[0].first
        val rec = Recipe(key, m, ins, outs, t, if (ru.isEmpty()) first.ru else ru, if (en.isEmpty()) first.en else en, hand, alt, ms)
        all.add(rec)
        return rec
    }

    init {
        // --- Smelter
        r("iron_ingot", Mach.SMELTER, c(Item.IRON_ORE to 1), c(Item.IRON_INGOT to 1), 2f)
        r("copper_ingot", Mach.SMELTER, c(Item.COPPER_ORE to 1), c(Item.COPPER_INGOT to 1), 2f)
        r("caterium_ingot", Mach.SMELTER, c(Item.CATERIUM_ORE to 4), c(Item.CATERIUM_INGOT to 1), 4f, "cat")
        // --- Foundry
        r("steel_ingot", Mach.FOUNDRY, c(Item.IRON_ORE to 3, Item.COAL to 3), c(Item.STEEL_INGOT to 3), 4f, "steel")
        r("aluminum_ingot", Mach.FOUNDRY, c(Item.BAUXITE to 3, Item.SILICA to 1), c(Item.ALUMINUM_INGOT to 2), 4f, "alu")
        // --- Constructor (and by hand)
        r("iron_plate", Mach.CONSTRUCTOR, c(Item.IRON_INGOT to 3), c(Item.IRON_PLATE to 2), 4f, hand = true)
        r("iron_rod", Mach.CONSTRUCTOR, c(Item.IRON_INGOT to 1), c(Item.IRON_ROD to 1), 3f, hand = true)
        r("screw", Mach.CONSTRUCTOR, c(Item.IRON_ROD to 1), c(Item.SCREW to 4), 4f, hand = true)
        r("wire", Mach.CONSTRUCTOR, c(Item.COPPER_INGOT to 1), c(Item.WIRE to 2), 3f, hand = true)
        r("cable", Mach.CONSTRUCTOR, c(Item.WIRE to 2), c(Item.CABLE to 1), 2f, hand = true)
        r("concrete", Mach.CONSTRUCTOR, c(Item.LIMESTONE to 3), c(Item.CONCRETE to 1), 3f, hand = true)
        r("copper_sheet", Mach.CONSTRUCTOR, c(Item.COPPER_INGOT to 2), c(Item.COPPER_SHEET to 1), 4f, "base", hand = true)
        r("biomass_wood", Mach.CONSTRUCTOR, c(Item.WOOD to 1), c(Item.BIOMASS to 6), 2f, hand = true)
        r("biomass_leaves", Mach.CONSTRUCTOR, c(Item.LEAVES to 1), c(Item.BIOMASS to 2), 1f, hand = true)
        r("biomass_remains", Mach.CONSTRUCTOR, c(Item.REMAINS to 1), c(Item.BIOMASS to 10), 2f, hand = true)
        r("solid_biofuel", Mach.CONSTRUCTOR, c(Item.BIOMASS to 8), c(Item.SOLID_BIOFUEL to 4), 4f, "coal")
        r("steel_beam", Mach.CONSTRUCTOR, c(Item.STEEL_INGOT to 4), c(Item.STEEL_BEAM to 1), 4f, "steel")
        r("steel_pipe", Mach.CONSTRUCTOR, c(Item.STEEL_INGOT to 3), c(Item.STEEL_PIPE to 2), 5f, "steel")
        r("quartz_crystal", Mach.CONSTRUCTOR, c(Item.RAW_QUARTZ to 5), c(Item.QUARTZ_CRYSTAL to 3), 6f, "solar")
        r("silica", Mach.CONSTRUCTOR, c(Item.RAW_QUARTZ to 3), c(Item.SILICA to 5), 6f, "solar")
        r("quickwire", Mach.CONSTRUCTOR, c(Item.CATERIUM_INGOT to 1), c(Item.QUICKWIRE to 5), 4f, "cat")
        r("aluminum_casing", Mach.CONSTRUCTOR, c(Item.ALUMINUM_INGOT to 3), c(Item.ALUMINUM_CASING to 2), 4f, "alu")
        // --- Assembler
        r("reinforced_plate", Mach.ASSEMBLER, c(Item.IRON_PLATE to 6, Item.SCREW to 12), c(Item.REINFORCED_PLATE to 1), 6f)
        r("modular_frame", Mach.ASSEMBLER, c(Item.REINFORCED_PLATE to 3, Item.IRON_ROD to 12), c(Item.MODULAR_FRAME to 2), 12f)
        r("rotor", Mach.ASSEMBLER, c(Item.IRON_ROD to 5, Item.SCREW to 25), c(Item.ROTOR to 1), 8f)
        r("stator", Mach.ASSEMBLER, c(Item.STEEL_PIPE to 3, Item.WIRE to 8), c(Item.STATOR to 1), 8f, "steel")
        r("motor", Mach.ASSEMBLER, c(Item.ROTOR to 2, Item.STATOR to 2), c(Item.MOTOR to 1), 6f, "steel")
        r("encased_beam", Mach.ASSEMBLER, c(Item.STEEL_BEAM to 3, Item.CONCRETE to 6), c(Item.ENCASED_BEAM to 1), 5f, "steel")
        r("circuit_board", Mach.ASSEMBLER, c(Item.COPPER_SHEET to 2, Item.PLASTIC to 4), c(Item.CIRCUIT_BOARD to 1), 6f, "oil")
        r("ai_limiter", Mach.ASSEMBLER, c(Item.COPPER_SHEET to 5, Item.QUICKWIRE to 20), c(Item.AI_LIMITER to 1), 6f, "cat")
        r("smart_plating", Mach.ASSEMBLER, c(Item.REINFORCED_PLATE to 1, Item.ROTOR to 1), c(Item.SMART_PLATING to 1), 12f)
        r("versatile_framework", Mach.ASSEMBLER, c(Item.MODULAR_FRAME to 1, Item.STEEL_BEAM to 12), c(Item.VERSATILE_FRAMEWORK to 2), 10f, "steel")
        r("automated_wiring", Mach.ASSEMBLER, c(Item.STATOR to 1, Item.CABLE to 20), c(Item.AUTOMATED_WIRING to 1), 12f, "steel")
        // --- Manufacturer
        r("computer", Mach.MANUFACTURER, c(Item.CIRCUIT_BOARD to 4, Item.CABLE to 6, Item.PLASTIC to 8, Item.SCREW to 24), c(Item.COMPUTER to 1), 18f, "manuf")
        r("heavy_frame", Mach.MANUFACTURER, c(Item.MODULAR_FRAME to 2, Item.STEEL_PIPE to 6, Item.ENCASED_BEAM to 2, Item.SCREW to 12), c(Item.HEAVY_FRAME to 1), 20f, "manuf")
        r("modular_engine", Mach.MANUFACTURER, c(Item.MOTOR to 2, Item.RUBBER to 15, Item.SMART_PLATING to 2), c(Item.MODULAR_ENGINE to 1), 20f, "manuf")
        r("adaptive_control", Mach.MANUFACTURER, c(Item.AUTOMATED_WIRING to 5, Item.CIRCUIT_BOARD to 5, Item.HEAVY_FRAME to 1, Item.COMPUTER to 2), c(Item.ADAPTIVE_CONTROL_UNIT to 1), 24f, "manuf")
        // --- Refinery
        r("plastic", Mach.REFINERY, c(Item.CRUDE_OIL to 3), c(Item.PLASTIC to 2, Item.FUEL to 1), 6f, "oil")
        r("rubber", Mach.REFINERY, c(Item.CRUDE_OIL to 3), c(Item.RUBBER to 2, Item.FUEL to 1), 6f, "oil")
        r("fuel_only", Mach.REFINERY, c(Item.CRUDE_OIL to 3), c(Item.FUEL to 2), 4f, "fuelgen")

        // --- Alternate recipes (unlocked by hard drives in the MAM)
        r("alt_cast_screw", Mach.CONSTRUCTOR, c(Item.IRON_INGOT to 5), c(Item.SCREW to 12), 12f, alt = true, ru = "Литой винт", en = "Cast Screw")
        r("alt_steel_rod", Mach.CONSTRUCTOR, c(Item.STEEL_INGOT to 1), c(Item.IRON_ROD to 4), 5f, alt = true, ru = "Стальной стержень", en = "Steel Rod")
        r("alt_caterium_wire", Mach.CONSTRUCTOR, c(Item.CATERIUM_INGOT to 1), c(Item.WIRE to 8), 4f, alt = true, ru = "Каверий-провод", en = "Caterium Wire")
        r("alt_iron_alloy", Mach.FOUNDRY, c(Item.IRON_ORE to 2, Item.COPPER_ORE to 2), c(Item.IRON_INGOT to 5), 6f, alt = true, ru = "Железный сплав", en = "Iron Alloy Ingot")
        r("alt_solid_steel", Mach.FOUNDRY, c(Item.IRON_INGOT to 2, Item.COAL to 2), c(Item.STEEL_INGOT to 3), 3f, alt = true, ru = "Цельная сталь", en = "Solid Steel Ingot")
        r("alt_bolted_plate", Mach.ASSEMBLER, c(Item.IRON_PLATE to 18, Item.SCREW to 50), c(Item.REINFORCED_PLATE to 3), 12f, alt = true, ru = "Болтовая пластина", en = "Bolted Iron Plate")
        r("alt_stitched_plate", Mach.ASSEMBLER, c(Item.IRON_PLATE to 10, Item.WIRE to 20), c(Item.REINFORCED_PLATE to 3), 16f, alt = true, ru = "Прошитая пластина", en = "Stitched Iron Plate")
        r("alt_fused_wire", Mach.ASSEMBLER, c(Item.COPPER_INGOT to 4, Item.CATERIUM_INGOT to 1), c(Item.WIRE to 30), 8f, alt = true, ru = "Плавленый провод", en = "Fused Wire")
        r("alt_cheap_silica", Mach.ASSEMBLER, c(Item.RAW_QUARTZ to 3, Item.LIMESTONE to 5), c(Item.SILICA to 7), 8f, alt = true, ru = "Дешёвый кремнезём", en = "Cheap Silica")
        r("alt_fine_concrete", Mach.ASSEMBLER, c(Item.SILICA to 3, Item.LIMESTONE to 12), c(Item.CONCRETE to 10), 8f, alt = true, ru = "Тонкий бетон", en = "Fine Concrete")
        r("alt_insulated_cable", Mach.ASSEMBLER, c(Item.WIRE to 9, Item.RUBBER to 6), c(Item.CABLE to 20), 8f, alt = true, ru = "Изолированный кабель", en = "Insulated Cable")
        r("alt_pure_aluminum", Mach.FOUNDRY, c(Item.BAUXITE to 4), c(Item.ALUMINUM_INGOT to 1), 4f, alt = true, ru = "Чистый алюминий", en = "Pure Aluminum")
        r("alt_plastic_scrap", Mach.REFINERY, c(Item.CRUDE_OIL to 4), c(Item.PLASTIC to 4), 8f, alt = true, ru = "Чистый пластик", en = "Clean Plastic")
        r("alt_biofuel_leaves", Mach.CONSTRUCTOR, c(Item.LEAVES to 12), c(Item.SOLID_BIOFUEL to 4), 5f, alt = true, ru = "Биотопливо из листьев", en = "Leaf Biofuel")
    }

    fun byKey(k: String): Recipe? { for (x in all) if (x.key == k) return x; return null }
    fun forMach(m: Mach): List<Recipe> = all.filter { it.mach == m }
    val hand: List<Recipe> get() = all.filter { it.hand }
    val alts: List<Recipe> get() = all.filter { it.alt }
}
