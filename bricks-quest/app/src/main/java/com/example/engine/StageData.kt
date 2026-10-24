package com.example.engine

import com.example.model.Brick
import com.example.model.BrickShape
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
        val ballCount = 60 // Generous 60 balls matching the user's reference picture!
        val random = Random(safeStage * 47L + 2026)

        when (safeStage) {
            1 -> {
                // Layout accurately matching the user's reference screenshot:
                // Row 0
                bricks.add(Brick(idCounter++, 0, 0, 35, 35, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 1, 0, 32, 32, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 2, 0, 35, 35, BrickType.NORMAL, shape = BrickShape.TRIANGLE_TOP_LEFT))
                bricks.add(Brick(idCounter++, 3, 0, 1, 1, BrickType.ADD_BALL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 5, 0, 20, 20, BrickType.NORMAL, shape = BrickShape.TRIANGLE_TOP_RIGHT))
                bricks.add(Brick(idCounter++, 6, 0, 11, 11, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 7, 0, 27, 27, BrickType.NORMAL, shape = BrickShape.RECTANGLE))

                // Row 1
                bricks.add(Brick(idCounter++, 0, 1, 25, 25, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 2, 1, 31, 31, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 3, 1, 32, 32, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 4, 1, 23, 23, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 6, 1, 1, 1, BrickType.ADD_BALL, shape = BrickShape.RECTANGLE))

                // Row 2
                bricks.add(Brick(idCounter++, 0, 2, 34, 34, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 1, 2, 34, 34, BrickType.NORMAL, shape = BrickShape.TRIANGLE_BOTTOM_RIGHT))
                bricks.add(Brick(idCounter++, 3, 2, 21, 21, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 5, 2, 1, 1, BrickType.ADD_BALL, shape = BrickShape.RECTANGLE))

                // Row 3
                bricks.add(Brick(idCounter++, 0, 3, 33, 33, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 1, 3, 31, 31, BrickType.NORMAL, shape = BrickShape.TRIANGLE_TOP_LEFT))
                bricks.add(Brick(idCounter++, 2, 3, 1, 1, BrickType.ADD_BALL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 4, 3, 21, 21, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 5, 3, 29, 29, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 6, 3, 3, 3, BrickType.NORMAL, shape = BrickShape.RECTANGLE))

                // Row 4
                bricks.add(Brick(idCounter++, 1, 4, 31, 31, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 2, 4, 31, 31, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 3, 4, 24, 24, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 4, 4, 31, 31, BrickType.NORMAL, shape = BrickShape.TRIANGLE_BOTTOM_LEFT))

                // Row 5
                bricks.add(Brick(idCounter++, 2, 5, 32, 32, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 3, 5, 32, 32, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
                bricks.add(Brick(idCounter++, 4, 5, 32, 32, BrickType.NORMAL, shape = BrickShape.RECTANGLE))
            }
            else -> {
                // Procedural generation with balanced HP, triangle shapes, and add-ball pickups
                val numRows = (5 + (safeStage % 3)).coerceAtMost(7)
                var bombsSpawned = 0
                val shapes = listOf(
                    BrickShape.RECTANGLE,
                    BrickShape.RECTANGLE,
                    BrickShape.RECTANGLE,
                    BrickShape.TRIANGLE_TOP_LEFT,
                    BrickShape.TRIANGLE_TOP_RIGHT,
                    BrickShape.TRIANGLE_BOTTOM_LEFT,
                    BrickShape.TRIANGLE_BOTTOM_RIGHT
                )

                for (r in 0 until numRows) {
                    for (c in 0 until COLS) {
                        val roll = random.nextFloat()
                        if (roll < 0.75f) {
                            val type = when {
                                roll < 0.10f -> BrickType.ADD_BALL
                                bombsSpawned < 2 && roll < 0.16f -> {
                                    bombsSpawned++
                                    BrickType.BOMB
                                }
                                roll < 0.22f -> BrickType.LASER_HORIZONTAL
                                roll < 0.26f -> BrickType.LASER_VERTICAL
                                roll < 0.30f -> BrickType.SPLITTER
                                else -> BrickType.NORMAL
                            }
                            val shape = if (type == BrickType.NORMAL && random.nextFloat() < 0.35f) {
                                shapes.random(random)
                            } else {
                                BrickShape.RECTANGLE
                            }

                            val baseHp = (20 + (safeStage * 2) % 30).coerceIn(15, 55)
                            val hp = if (type == BrickType.NORMAL) {
                                (baseHp + random.nextInt(-5, 10)).coerceIn(10, 60)
                            } else 1

                            bricks.add(Brick(idCounter++, c, r, hp, hp, type, shape = shape))
                        }
                    }
                }

                // Guarantee at least 3 ADD_BALL pickups per stage so ball stream keeps multiplying
                val addBallCount = bricks.count { it.type == BrickType.ADD_BALL }
                if (addBallCount < 3) {
                    for (i in 0 until (3 - addBallCount)) {
                        val freeCol = (0 until COLS).filter { col -> bricks.none { it.col == col && it.row == i + 1 } }
                        if (freeCol.isNotEmpty()) {
                            bricks.add(Brick(idCounter++, freeCol.random(random), i + 1, 1, 1, BrickType.ADD_BALL))
                        }
                    }
                }
            }
        }

        val totalNormalHp = bricks.filter { it.type == BrickType.NORMAL }.sumOf { it.currentHp }
        val turnsFor3 = (totalNormalHp / (ballCount * 1.2f)).toInt().coerceIn(15, 50)
        val turnsFor2 = turnsFor3 + 10

        return StageInfo(
            stageNumber = safeStage,
            initialBallCount = ballCount,
            initialBricks = bricks,
            targetTurnsFor3Stars = turnsFor3,
            targetTurnsFor2Stars = turnsFor2
        )
    }
}
