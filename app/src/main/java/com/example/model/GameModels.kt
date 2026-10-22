package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class GameMode {
    QUEST,
    ENDLESS,
    RUSH_100
}

enum class BrickType {
    NORMAL,
    ADD_BALL,
    LASER_HORIZONTAL,
    LASER_VERTICAL,
    BOMB,
    SPLITTER
}

enum class BrickShape {
    RECTANGLE,
    TRIANGLE_TOP_LEFT,     // Right angle at top-left, hypotenuse from bottom-left to top-right
    TRIANGLE_TOP_RIGHT,    // Right angle at top-right, hypotenuse from top-left to bottom-right
    TRIANGLE_BOTTOM_LEFT,  // Right angle at bottom-left, hypotenuse from top-left to bottom-right
    TRIANGLE_BOTTOM_RIGHT  // Right angle at bottom-right, hypotenuse from bottom-left to top-right
}

data class BrickTones(
    val border: Color,
    val fill: Color,
    val glow: Color,
    val outerAura: Color
)

data class Brick(
    val id: Long,
    val col: Int,
    var row: Int,
    var currentHp: Int,
    val maxHp: Int,
    val type: BrickType,
    var isAlive: Boolean = true,
    val shape: BrickShape = BrickShape.RECTANGLE
) {
    fun tones(): BrickTones {
        return when (type) {
            BrickType.ADD_BALL -> BrickTones(
                border = Color(0xFF00E676),
                fill = Color(0x3500E676),
                glow = Color(0xFF69F0AE),
                outerAura = Color(0x5500E676)
            )
            BrickType.BOMB -> BrickTones(
                border = Color(0xFFFF5722),
                fill = Color(0x40FF5722),
                glow = Color(0xFFFF8A65),
                outerAura = Color(0x55FF5722)
            )
            BrickType.LASER_HORIZONTAL, BrickType.LASER_VERTICAL -> BrickTones(
                border = Color(0xFF00E5FF),
                fill = Color(0x3500E5FF),
                glow = Color(0xFF80D8FF),
                outerAura = Color(0x5500E5FF)
            )
            BrickType.SPLITTER -> BrickTones(
                border = Color(0xFFFFD600),
                fill = Color(0x35FFD600),
                glow = Color(0xFFFFFF72),
                outerAura = Color(0x55FFD600)
            )
            BrickType.NORMAL -> {
                // Vibrant neon colors matching the user's reference picture:
                // Neon Cyan, Neon Yellow, Neon Magenta/Red, Neon Purple, Neon Lime Green, Neon Cobalt
                val palettes = listOf(
                    // 1. Neon Cyan / Aqua (Matches picture brick 32, 21, 32)
                    BrickTones(
                        border = Color(0xFF00E5FF),
                        fill = Color(0x3000E5FF),
                        glow = Color(0xFF84FFFF),
                        outerAura = Color(0x4500E5FF)
                    ),
                    // 2. Neon Golden Yellow (Matches picture brick 31, 24, 31)
                    BrickTones(
                        border = Color(0xFFFFD600),
                        fill = Color(0x30FFD600),
                        glow = Color(0xFFFFFF8D),
                        outerAura = Color(0x45FFD600)
                    ),
                    // 3. Neon Ruby / Magenta Red (Matches picture brick 35, 3)
                    BrickTones(
                        border = Color(0xFFFF1744),
                        fill = Color(0x30FF1744),
                        glow = Color(0xFFFF8A80),
                        outerAura = Color(0x45FF1744)
                    ),
                    // 4. Neon Purple / Electric Violet (Matches picture brick 11, 20)
                    BrickTones(
                        border = Color(0xFFB388FF),
                        fill = Color(0x30B388FF),
                        glow = Color(0xFFD1C4E9),
                        outerAura = Color(0x45B388FF)
                    ),
                    // 5. Neon Cyber Lime (Matches picture brick 25, 27)
                    BrickTones(
                        border = Color(0xFF76FF03),
                        fill = Color(0x3076FF03),
                        glow = Color(0xFFCCFF90),
                        outerAura = Color(0x4576FF03)
                    ),
                    // 6. Neon Sky Blue (Matches picture brick 34, 33)
                    BrickTones(
                        border = Color(0xFF40C4FF),
                        fill = Color(0x3040C4FF),
                        glow = Color(0xFFB3E5FC),
                        outerAura = Color(0x4540C4FF)
                    ),
                    // 7. Neon Coral Orange (Matches picture brick 29)
                    BrickTones(
                        border = Color(0xFFFF6D00),
                        fill = Color(0x30FF6D00),
                        glow = Color(0xFFFFD180),
                        outerAura = Color(0x45FF6D00)
                    )
                )
                val seed = (row * 3 + col + (maxHp % 7)).let { if (it < 0) -it else it }
                palettes[seed % palettes.size]
            }
        }
    }

    fun color(): Color = tones().border
}

data class Ball(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var isActive: Boolean = false,
    var hasLanded: Boolean = false,
    var trail: MutableList<Offset> = mutableListOf(),
    var bounceCount: Int = 0
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val isSquare: Boolean = false,
    var rotation: Float = 0f
)

data class FloatText(
    val text: String,
    var x: Float,
    var y: Float,
    var alpha: Float = 1f,
    val color: Color
)

data class LaserEffect(
    val isHorizontal: Boolean,
    val centerCoord: Float,
    var timer: Float = 0.25f,
    val color: Color
)

data class Shockwave(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    var currentRadius: Float = 5f,
    var alpha: Float = 1f,
    val color: Color
)

data class BallSkin(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val glowColor: Color,
    val price: Int,
    val isDefault: Boolean = false
) {
    companion object {
        val ALL_SKINS = listOf(
            BallSkin("classic_pearl", "Classic Pearl", Color.White, NeonCyan, 0, isDefault = true),
            BallSkin("neon_cyan", "Neon Cyber", NeonCyan, Color(0xFF00B0FF), 100),
            BallSkin("fire_blaze", "Fire Blaze", NeonOrange, NeonRed, 250),
            BallSkin("gold_star", "Golden Sun", NeonGold, Color(0xFFFFAB00), 500),
            BallSkin("plasma_pink", "Plasma Pink", NeonMagenta, Color(0xFFFF4081), 750),
            BallSkin("acid_lime", "Acid Lime", NeonLime, Color(0xFF76FF03), 1000),
            BallSkin("dark_matter", "Dark Nebula", NeonPurple, Color(0xFFAA00FF), 1500)
        )
    }
}
