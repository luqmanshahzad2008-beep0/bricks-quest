package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.engine.GameEngine
import com.example.engine.StageData
import com.example.model.BrickType
import com.example.model.GameMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bricks Breaker Quest", appName)
  }

  @Test
  fun `test sequential level progression from Level 1 to 4`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    // Player starts at Level 1
    engine.initialize(GameMode.QUEST, stage = 1, width = 1080f, height = 1920f)
    assertEquals("Must start at Level 1", 1, engine.stageNumber)
    assertEquals("Game storage starts at Level 1", 1, storage.highestStageUnlocked)
    assertFalse("Stage not clear initially", engine.isStageClear)
    assertTrue("Has bricks on Level 1", engine.bricks.isNotEmpty())

    // 1. Complete Level 1
    engine.completeStageInstantly()
    assertTrue("Stage 1 clear flag must be true", engine.isStageClear)
    assertEquals("Storage highest unlocked must increment to 2", 2, storage.highestStageUnlocked)

    // Advance to Level 2
    engine.nextStage()
    assertEquals("Engine stageNumber must now be 2", 2, engine.stageNumber)
    assertFalse("Stage clear must be reset to false for new level", engine.isStageClear)
    assertFalse("Game must not be game over", engine.isGameOver)

    // 2. Complete Level 2
    engine.completeStageInstantly()
    assertTrue("Stage 2 clear flag must be true", engine.isStageClear)
    assertEquals("Storage highest unlocked must increment to 3", 3, storage.highestStageUnlocked)

    // Advance to Level 3
    engine.nextStage()
    assertEquals("Engine stageNumber must now be 3", 3, engine.stageNumber)
    assertFalse("Stage clear must be reset for Level 3", engine.isStageClear)

    // 3. Complete Level 3
    engine.completeStageInstantly()
    assertTrue("Stage 3 clear flag must be true", engine.isStageClear)
    assertEquals("Storage highest unlocked must increment to 4", 4, storage.highestStageUnlocked)

    // Advance to Level 4
    engine.nextStage()
    assertEquals("Engine stageNumber must now be 4", 4, engine.stageNumber)
    assertFalse("Stage clear must be reset for Level 4", engine.isStageClear)
  }

  @Test
  fun `test starting a later level does not reset to Level 1`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    // Player unlocks up to Level 50 and starts Level 50
    storage.highestStageUnlocked = 50
    engine.initialize(GameMode.QUEST, stage = 50, width = 1080f, height = 1920f)

    assertEquals("Gameplay must start at Level 50", 50, engine.stageNumber)
    assertFalse("Stage clear must be false", engine.isStageClear)

    // Completing Level 50 unlocks Level 51
    engine.completeStageInstantly()
    assertEquals("Highest unlocked must be 51", 51, storage.highestStageUnlocked)

    engine.nextStage()
    assertEquals("Next stage must be Level 51", 51, engine.stageNumber)
  }

  @Test
  fun `test multiple consecutive level completions without closing or reloading`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    engine.initialize(GameMode.QUEST, stage = 1, width = 1080f, height = 1920f)

    for (lvl in 1..15) {
      assertEquals("Current level before completion", lvl, engine.stageNumber)
      engine.completeStageInstantly()
      assertTrue("Level $lvl must be marked cleared", engine.isStageClear)
      assertEquals("Storage highest unlocked after clearing $lvl", lvl + 1, storage.highestStageUnlocked)

      engine.nextStage()
      assertEquals("Current level after nextStage", lvl + 1, engine.stageNumber)
      assertFalse("Level ${lvl + 1} must be active and not cleared", engine.isStageClear)
      assertTrue("Level ${lvl + 1} must have bricks loaded", engine.bricks.isNotEmpty())
    }
  }

  @Test
  fun `test Level 5000 completion and safety`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    engine.initialize(GameMode.QUEST, stage = 5000, width = 1080f, height = 1920f)
    assertEquals(5000, engine.stageNumber)

    engine.completeStageInstantly()
    assertTrue(engine.isStageClear)
    assertEquals(5000, storage.highestStageUnlocked)

    // Advance on 5000 must not crash or exceed 5000
    engine.nextStage()
    assertEquals(5000, engine.stageNumber)
    assertTrue("Final stage clear state preserved for congratulations", engine.isStageClear)
  }

  @Test
  fun `test replayCurrentStage restarts only the current level`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    engine.initialize(GameMode.QUEST, stage = 3, width = 1080f, height = 1920f)
    assertEquals(3, engine.stageNumber)

    // Complete stage 3
    engine.completeStageInstantly()
    assertTrue(engine.isStageClear)
    assertEquals(4, storage.highestStageUnlocked)

    // Replay stage 3
    engine.replayCurrentStage()
    assertEquals("Replay must keep level at 3", 3, engine.stageNumber)
    assertFalse("Replay must reset clear flag to active", engine.isStageClear)
    assertFalse("Replay must not be game over", engine.isGameOver)
    assertTrue("Replay must regenerate stage 3 bricks", engine.bricks.isNotEmpty())
  }

  @Test
  fun `test concurrent safety when level completes during active physics simulation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val soundManager = SoundManager(context).apply {
      isSoundEnabled = false
      isHapticsEnabled = false
    }
    val engine = GameEngine(soundManager, storage)

    engine.initialize(GameMode.QUEST, stage = 1, width = 1080f, height = 1920f)

    // Launch aim
    engine.updateAim(540f, 200f)
    engine.releaseAim()
    assertTrue("Should be shooting", engine.isShooting)

    // Run several updates while shooting
    for (i in 0 until 5) {
      engine.update(0.016f)
    }

    // Now trigger completion while balls are active
    engine.completeStageInstantly()
    assertTrue(engine.isStageClear)

    // Next update call must NOT throw ConcurrentModificationException
    engine.update(0.016f)
    assertFalse(engine.isShooting)
    assertEquals(0, engine.balls.size)
  }

  @Test
  fun `test 5000 levels validity and completion conditions`() {
    val checkLevels = listOf(1, 2, 3, 4, 12, 50, 100, 250, 500, 1000, 2500, 4999, 5000)
    for (lvl in checkLevels) {
      val stage = StageData.getStage(lvl)
      assertEquals("Stage number matches", lvl, stage.stageNumber)
      assertTrue("Level $lvl has normal bricks to destroy", stage.initialBricks.any { it.type == BrickType.NORMAL })
      assertTrue("Level $lvl has add-ball items", stage.initialBricks.any { it.type == BrickType.ADD_BALL })
      assertTrue("Level $lvl has ball count >= 30", stage.initialBallCount >= 30)
      assertTrue("Level $lvl has positive 3-star turns", stage.targetTurnsFor3Stars > 0)
    }
  }
  @Test
  fun `test SoundManager playLaunchSound does not crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundManager = SoundManager(context)
    soundManager.isSoundEnabled = true
    soundManager.playLaunchSound()
  }

  @Test
  fun `test new startup splash screen drawable resource loads successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val splashResId = R.drawable.bricks_quest_splash_1792317360870
    assertTrue("Splash resource ID must be valid", splashResId != 0)
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, splashResId)
    assertNotNull("New splash screen drawable must load", drawable)
  }

  @Test
  fun `test previous splash asset backup exists and is preserved`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val backupResId = R.drawable.bricks_quest_splash_previous_backup
    assertTrue("Backup splash resource ID must be valid", backupResId != 0)
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, backupResId)
    assertNotNull("Previous splash backup must be available", drawable)
  }

  @Test
  fun `test ball package purchasing updates ball balance and diamonds correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = GameStorage(context)
    val initialBalls = storage.ballBalance
    val initialDiamonds = storage.diamonds

    // Purchase 10 balls for 50 diamonds
    storage.diamonds -= 50
    storage.addBalls(10)

    assertEquals(initialBalls + 10, storage.ballBalance)
    assertEquals(initialDiamonds - 50, storage.diamonds)
  }
}
