package com.example.engine

import com.example.model.Brick
import com.example.model.BrickType
import kotlin.random.Random

data class StageInfo(
    val stageNumber: Int,
    val initialBallCount: Int,
    val initialBricks: List<Brick>,
    val targetTurnsFor3Stars: Int,
    val targetTurnsFor2Stars: Int
)

object StageData {
    const val COLS = 8 // 8 columns matching the reference image!
    const val ROWS = 9

    fun getStage(stageNumber: Int): StageInfo {
        val safeStage = stageNumber.coerceIn(1, 5000)
        var idCounter = 1L
        val bricks = mutableListOf<Brick>()
        val ballCount = 95

        val random = Random(safeStage * 47L + 2026)
        val mixedNumbers = intArrayOf(20, 25, 30, 35, 40, 45, 50, 55, 60)

        when (safeStage) {
            1 -> {
                // Densely packed 7 full rows of bricks starting from row 0 to fill the top space completely!
                // All numbers strictly capped at 60 max (20 to 60)
                // Row 0: Top-most row right under the frame border
                val row0 = intArrayOf(40, 55, 30, 60, 45, 50, 60, 35)
                for (c in 0 until COLS) {
                    val hp = row0[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 0, hp, hp, BrickType.NORMAL))
                }

                // Row 1
                val row1 = intArrayOf(50, 45, 60, 30, 55, 40, 50, 60)
                for (c in 0 until COLS) {
                    val hp = row1[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 1, hp, hp, BrickType.NORMAL))
                }

                // Row 2: Add-ball powerups flanked by numbers
                val row2 = intArrayOf(35, 60, 1, 50, 45, 1, 60, 40)
                for (c in 0 until COLS) {
                    val isAddBall = (c == 2 || c == 5)
                    val type = if (isAddBall) BrickType.ADD_BALL else BrickType.NORMAL
                    val hp = if (isAddBall) 1 else row2[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 2, hp, hp, type))
                }

                // Row 3
                val row3 = intArrayOf(45, 50, 60, 35, 40, 55, 30, 50)
                for (c in 0 until COLS) {
                    val hp = row3[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 3, hp, hp, BrickType.NORMAL))
                }

                // Row 4: Exactly 2 separated tactical bombs (c=2 and c=5) with side bricks
                val row4 = intArrayOf(60, 40, 1, 35, 50, 1, 60, 45)
                for (c in 0 until COLS) {
                    val isBomb = (c == 2 || c == 5)
                    val type = if (isBomb) BrickType.BOMB else BrickType.NORMAL
                    val hp = if (isBomb) 1 else row4[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 4, hp, hp, type))
                }

                // Row 5
                val row5 = intArrayOf(30, 60, 45, 50, 60, 35, 55, 40)
                for (c in 0 until COLS) {
                    val hp = row5[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 5, hp, hp, BrickType.NORMAL))
                }

                // Row 6: Frontline
                val row6 = intArrayOf(50, 35, 60, 40, 50, 60, 45, 30)
                for (c in 0 until COLS) {
                    val hp = row6[c].coerceIn(15, 60)
                    bricks.add(Brick(idCounter++, c, 6, hp, hp, BrickType.NORMAL))
                }
            }
            else -> {
                // Procedural generation: strictly at most 2 bombs per stage
                val numRows = 6
                var bombsSpawned = 0
                for (r in 0 until numRows) {
                    for (c in 0 until COLS) {
                        val roll = random.nextFloat()
                        if (roll < 0.88f) {
                            val type = when {
                                roll < 0.08f -> BrickType.ADD_BALL
                                bombsSpawned < 2 && roll < 0.14f -> {
                                    bombsSpawned++
                                    BrickType.BOMB
                                }
                                roll < 0.20f -> BrickType.LASER_HORIZONTAL
                                roll < 0.26f -> BrickType.SPLITTER
                                else -> BrickType.NORMAL
                            }
                            val rawHp = if (type == BrickType.NORMAL) {
                                mixedNumbers[random.nextInt(mixedNumbers.size)]
                            } else 1
                            val hp = rawHp.coerceIn(1, 60) // Strictly never above 60!
                            bricks.add(Brick(idCounter++, c, r, hp, hp, type))
                        }
                    }
                }

                if (bricks.none { it.type == BrickType.ADD_BALL }) {
                    bricks.add(Brick(idCounter++, 2, 2, 1, 1, BrickType.ADD_BALL))
                    bricks.add(Brick(idCounter++, 5, 2, 1, 1, BrickType.ADD_BALL))
                }
            }
        }

        val totalNormalHp = bricks.filter { it.type == BrickType.NORMAL }.sumOf { it.currentHp }
        val turnsFor3 = (totalNormalHp / (ballCount * 1.5f)).toInt().coerceIn(12, 40)
        val turnsFor2 = turnsFor3 + 8

        return StageInfo(
            stageNumber = safeStage,
            initialBallCount = ballCount,
            initialBricks = bricks,
            targetTurnsFor3Stars = turnsFor3,
            targetTurnsFor2Stars = turnsFor2
        )
    }
}
