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

data class BrickTones(val top: Color, val mid: Color, val bot: Color, val glow: Color)

data class Brick(
    val id: Long,
    val col: Int,
    var row: Int,
    var currentHp: Int,
    val maxHp: Int,
    val type: BrickType,
    var isAlive: Boolean = true
) {
    fun tones(): BrickTones {
        return when (type) {
            BrickType.ADD_BALL -> BrickTones(
                top = Color(0xFF69F0AE),
                mid = Color(0xFF00E676),
                bot = Color(0xFF007E33),
                glow = Color(0xFF00E676)
            )
            BrickType.BOMB -> BrickTones(
                top = Color(0xFFFFAB40),
                mid = Color(0xFFFF6D00),
                bot = Color(0xFFD50000),
                glow = Color(0xFFFF6D00)
            )
            BrickType.LASER_HORIZONTAL, BrickType.LASER_VERTICAL -> BrickTones(
                top = Color(0xFF80D8FF),
                mid = Color(0xFF00E5FF),
                bot = Color(0xFF006064),
                glow = Color(0xFF00E5FF)
            )
            BrickType.SPLITTER -> BrickTones(
                top = Color(0xFFFFFF72),
                mid = Color(0xFFFFD600),
                bot = Color(0xFFE65100),
                glow = Color(0xFFFFD600)
            )
            BrickType.NORMAL -> {
                // Beautiful variety of neon pink, blue, purple, cyan, orange, yellow, and coral
                val palettes = listOf(
                    // 1. Vibrant Neon Pink / Magenta
                    BrickTones(Color(0xFFFF4081), Color(0xFFFF007F), Color(0xFF880E4F), Color(0xFFFF007F)),
                    // 2. Electric Cobalt / Neon Blue
                    BrickTones(Color(0xFF40C4FF), Color(0xFF0070F3), Color(0xFF0D47A1), Color(0xFF0070F3)),
                    // 3. Cosmic Purple / Electric Violet
                    BrickTones(Color(0xFFE040FB), Color(0xFF9D00FF), Color(0xFF4A148C), Color(0xFF9D00FF)),
                    // 4. Cyber Cyan / Luminous Turquoise
                    BrickTones(Color(0xFF84FFFF), Color(0xFF00E5FF), Color(0xFF004D40), Color(0xFF00E5FF)),
                    // 5. Fiery Neon Amber / Orange
                    BrickTones(Color(0xFFFFAB40), Color(0xFFFF6D00), Color(0xFFBF360C), Color(0xFFFF6D00)),
                    // 6. Neon Electric Gold
                    BrickTones(Color(0xFFFFFF72), Color(0xFFFFD600), Color(0xFFF57F17), Color(0xFFFFD600)),
                    // 7. Neon Coral Red
                    BrickTones(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFB71C1C), Color(0xFFFF1744))
                )
                // Deterministic distributed palette based on row, col, and maxHp
                val seed = (row * 3 + col + (maxHp % 7)).let { if (it < 0) -it else it }
                palettes[seed % palettes.size]
            }
        }
    }

    fun color(): Color = tones().mid
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
