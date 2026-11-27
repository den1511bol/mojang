// Pixel Factory - core: items. Developer: @yfeeeee
package com.yfeeeee.factory.core

// shape ids are used by the sprite generator in engine/Sprites.kt
object Shape {
    const val ORE = 0; const val INGOT = 1; const val PLATE = 2; const val ROD = 3; const val SCREW = 4
    const val COIL = 5; const val CABLE = 6; const val GEAR = 7; const val BOX = 8; const val CRYSTAL = 9
    const val DROP = 10; const val LEAF = 11; const val LOG = 12; const val POWDER = 13; const val BOARD = 14
    const val MOTOR = 15; const val BERRY = 16; const val BRICK = 17; const val PIPE = 18; const val COMPUTER = 19
    const val PELLET = 20; const val ORB = 21; const val DISK = 22; const val BEAM = 23
}

enum class Item(val ru: String, val en: String, val stack: Int, val color: Int, val shape: Int) {
    IRON_ORE("Железная руда", "Iron Ore", 100, 0xFF9A6A55.toInt(), Shape.ORE),
    COPPER_ORE("Медная руда", "Copper Ore", 100, 0xFFC8743A.toInt(), Shape.ORE),
    LIMESTONE("Известняк", "Limestone", 100, 0xFFC9C3A5.toInt(), Shape.ORE),
    COAL("Уголь", "Coal", 100, 0xFF2E2E36.toInt(), Shape.ORE),
    CATERIUM_ORE("Руда каверия", "Caterium Ore", 100, 0xFFE0C23C.toInt(), Shape.ORE),
    RAW_QUARTZ("Сырой кварц", "Raw Quartz", 100, 0xFFE58AC8.toInt(), Shape.CRYSTAL),
    SULFUR("Сера", "Sulfur", 100, 0xFFE8E04A.toInt(), Shape.POWDER),
    BAUXITE("Боксит", "Bauxite", 100, 0xFFB5543C.toInt(), Shape.ORE),
    CRUDE_OIL("Нефть", "Crude Oil", 50, 0xFF3A2A4A.toInt(), Shape.DROP),
    WOOD("Древесина", "Wood", 100, 0xFF8A5A2B.toInt(), Shape.LOG),
    LEAVES("Листья", "Leaves", 100, 0xFF4EA34A.toInt(), Shape.LEAF),
    BERRIES("Ягоды", "Berries", 50, 0xFFD23C6A.toInt(), Shape.BERRY),
    REMAINS("Останки", "Remains", 50, 0xFF9E7A6A.toInt(), Shape.POWDER),

    IRON_INGOT("Железный слиток", "Iron Ingot", 100, 0xFFB0B7BE.toInt(), Shape.INGOT),
    COPPER_INGOT("Медный слиток", "Copper Ingot", 100, 0xFFE08A4A.toInt(), Shape.INGOT),
    CATERIUM_INGOT("Слиток каверия", "Caterium Ingot", 100, 0xFFF2D24A.toInt(), Shape.INGOT),
    STEEL_INGOT("Стальной слиток", "Steel Ingot", 100, 0xFF6E7C8C.toInt(), Shape.INGOT),
    ALUMINUM_INGOT("Алюминиевый слиток", "Aluminum Ingot", 100, 0xFFD6E4EC.toInt(), Shape.INGOT),

    IRON_PLATE("Железная пластина", "Iron Plate", 200, 0xFFA7AEB6.toInt(), Shape.PLATE),
    IRON_ROD("Железный стержень", "Iron Rod", 200, 0xFF9AA2AA.toInt(), Shape.ROD),
    SCREW("Винт", "Screw", 500, 0xFF8E969E.toInt(), Shape.SCREW),
    REINFORCED_PLATE("Усиленная пластина", "Reinforced Plate", 100, 0xFF8FA0B4.toInt(), Shape.PLATE),
    MODULAR_FRAME("Модульная рама", "Modular Frame", 50, 0xFF7C8FA6.toInt(), Shape.BOX),
    ROTOR("Ротор", "Rotor", 100, 0xFF8A92A0.toInt(), Shape.GEAR),
    STATOR("Статор", "Stator", 100, 0xFFB9774A.toInt(), Shape.GEAR),
    MOTOR("Мотор", "Motor", 50, 0xFF5E8AA8.toInt(), Shape.MOTOR),
    COPPER_SHEET("Медный лист", "Copper Sheet", 200, 0xFFD98A52.toInt(), Shape.PLATE),
    WIRE("Провод", "Wire", 500, 0xFFE6955A.toInt(), Shape.COIL),
    CABLE("Кабель", "Cable", 200, 0xFF4A4A5A.toInt(), Shape.CABLE),
    CONCRETE("Бетон", "Concrete", 500, 0xFFA8A8A0.toInt(), Shape.BRICK),
    STEEL_BEAM("Стальная балка", "Steel Beam", 200, 0xFF5E6C7C.toInt(), Shape.BEAM),
    STEEL_PIPE("Стальная труба", "Steel Pipe", 200, 0xFF68788A.toInt(), Shape.PIPE),
    ENCASED_BEAM("Бетонная балка", "Encased Beam", 100, 0xFF8C8C86.toInt(), Shape.BEAM),
    QUARTZ_CRYSTAL("Кристалл кварца", "Quartz Crystal", 200, 0xFFF0A8DC.toInt(), Shape.CRYSTAL),
    SILICA("Кремнезём", "Silica", 500, 0xFFEDEAD8.toInt(), Shape.POWDER),
    QUICKWIRE("Быстропровод", "Quickwire", 500, 0xFFF6D860.toInt(), Shape.COIL),
    CIRCUIT_BOARD("Плата", "Circuit Board", 200, 0xFF3C9A6A.toInt(), Shape.BOARD),
    PLASTIC("Пластик", "Plastic", 200, 0xFF4AA0E0.toInt(), Shape.PLATE),
    RUBBER("Резина", "Rubber", 200, 0xFF3A3A3A.toInt(), Shape.BRICK),
    FUEL("Топливо", "Fuel", 50, 0xFFE8962E.toInt(), Shape.DROP),
    AI_LIMITER("ИИ-ограничитель", "AI Limiter", 100, 0xFFE8C84A.toInt(), Shape.BOARD),
    COMPUTER("Компьютер", "Computer", 50, 0xFF5A6A8C.toInt(), Shape.COMPUTER),
    HEAVY_FRAME("Тяжёлая рама", "Heavy Frame", 50, 0xFF6A7C92.toInt(), Shape.BOX),
    ALUMINUM_CASING("Алюминиевый корпус", "Aluminum Casing", 200, 0xFFE0ECF2.toInt(), Shape.PLATE),
    BIOMASS("Биомасса", "Biomass", 200, 0xFF7AB04A.toInt(), Shape.POWDER),
    SOLID_BIOFUEL("Твёрдое биотопливо", "Solid Biofuel", 200, 0xFF9AC24A.toInt(), Shape.PELLET),
    SMART_PLATING("Умная обшивка", "Smart Plating", 50, 0xFFE06A5A.toInt(), Shape.PLATE),
    VERSATILE_FRAMEWORK("Универсальный каркас", "Versatile Framework", 50, 0xFF8A7CC0.toInt(), Shape.BOX),
    AUTOMATED_WIRING("Автопроводка", "Automated Wiring", 50, 0xFFE08AB0.toInt(), Shape.COIL),
    MODULAR_ENGINE("Модульный двигатель", "Modular Engine", 50, 0xFF5AA8A0.toInt(), Shape.MOTOR),
    ADAPTIVE_CONTROL_UNIT("Адаптивный блок управления", "Adaptive Control Unit", 50, 0xFFB0E0F0.toInt(), Shape.ORB),
    HARD_DRIVE("Жёсткий диск", "Hard Drive", 10, 0xFF4A4A5C.toInt(), Shape.DISK);

    fun name(en: Boolean): String = if (en) this.en else this.ru
    val isFuel: Boolean get() = fuelValue > 0f
    // energy in MJ (MW*s) when burned in a generator
    val fuelValue: Float
        get() = when (this) {
            BIOMASS -> 180f
            SOLID_BIOFUEL -> 450f
            WOOD -> 120f
            LEAVES -> 30f
            COAL -> 300f
            FUEL -> 600f
            else -> 0f
        }
}
