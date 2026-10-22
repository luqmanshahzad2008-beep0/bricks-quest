package com.example

import com.example.engine.StageData
import com.example.model.BallSkin
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testStageDataGeneration() {
    val stage1 = StageData.getStage(1)
    assertEquals(1, stage1.stageNumber)
    assertTrue(stage1.initialBricks.isNotEmpty())
    assertTrue(stage1.initialBallCount >= 25)

    val stage10 = StageData.getStage(10)
    assertEquals(10, stage10.stageNumber)
    assertTrue(stage10.initialBricks.isNotEmpty())
  }

  @Test
  fun testBallSkins() {
    assertTrue(BallSkin.ALL_SKINS.size >= 5)
    val defaultSkin = BallSkin.ALL_SKINS.find { it.isDefault }
    assertNotNull(defaultSkin)
    assertEquals("classic_pearl", defaultSkin?.id)
  }
}
