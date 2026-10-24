package com.example.engine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

class GameEngine(
    val soundManager: SoundManager,
    val storage: GameStorage
) {
    var mode: GameMode by mutableStateOf(GameMode.QUEST)
    var stageNumber: Int by mutableIntStateOf(1)
    var wave: Int by mutableIntStateOf(1)
    var score: Int by mutableIntStateOf(0)
    var turnsTaken: Int by mutableIntStateOf(0)
    var coinsEarnedInRun: Int by mutableIntStateOf(0)

    var onStageChangedListener: ((Int) -> Unit)? = null

    // Board & layout geometry (calculated on layout)
    var screenWidth: Float = 1080f
    var screenHeight: Float = 1920f
    var playLeft: Float = 0f
    var playRight: Float = 1080f
    var playTop: Float = 220f
    var playBottom: Float = 1600f
    var groundY: Float = 1580f
    var cellWidth: Float = 1080f / 8f
    var cellHeight: Float = 85f
    val cols: Int = StageData.COLS // 8 Columns

    // Dynamic bottom danger threshold: bricks can travel all the way down to the launcher ground line!
    val maxRow: Int
        get() {
            if (cellHeight <= 0f) return 14
            val available = (groundY - playTop) / cellHeight
            return available.toInt().coerceAtLeast(12)
        }

    val isAnyBrickNearBottom: Boolean
        get() {
            val threshold = (maxRow - 2).coerceAtLeast(1)
            return synchronized(bricks) {
                bricks.any { it.isAlive && it.type != BrickType.ADD_BALL && it.row >= threshold }
            }
        }

    // Ball attributes
    var ballRadius: Float = 10f
    var totalBalls: Int by mutableIntStateOf(30)
    var ballsInReserve: Int by mutableIntStateOf(30)
    var ballsEarnedThisRound: Int = 0
    var launcherX: Float = 540f
    var launcherBoxX: Float by mutableFloatStateOf(540f)
    var nextLauncherX: Float = 540f
    var firstBallLanded: Boolean = false
    var capsuleWidth: Float = 0f
    var controlBallRadius: Float = 0f

    val minLauncherX: Float
        get() {
            val halfW = if (capsuleWidth > 0f) capsuleWidth / 2f else 180f
            val r = if (controlBallRadius > 0f) controlBallRadius else ballRadius
            return launcherBoxX - halfW + r
        }

    val maxLauncherX: Float
        get() {
            val halfW = if (capsuleWidth > 0f) capsuleWidth / 2f else 180f
            val r = if (controlBallRadius > 0f) controlBallRadius else ballRadius
            return launcherBoxX + halfW - r
        }

    // State flags
    var isShooting: Boolean by mutableStateOf(false)
    var isAiming: Boolean by mutableStateOf(false)
    var aimAngle: Float by mutableFloatStateOf(-PI.toFloat() / 2f)
    var aimVector: Offset by mutableStateOf(Offset(0f, -1f))
    var isGameOver: Boolean by mutableStateOf(false)
    var isStageClear: Boolean by mutableStateOf(false)
    var isPaused: Boolean by mutableStateOf(false)
    var gameSpeed: Float by mutableFloatStateOf(1.0f)

    // Power-up charges (matching the x5 badges in the reference image)
    var lightningCharges: Int by mutableIntStateOf(5)
    var clusterCharges: Int by mutableIntStateOf(5)

    // Level progress tracking
    var initialLevelHp: Int by mutableIntStateOf(100)
    var currentLevelHp: Int by mutableIntStateOf(100)

    val levelProgress: Float
        get() {
            if (initialLevelHp <= 0) return 1f
            return (1f - (currentLevelHp.toFloat() / initialLevelHp)).coerceIn(0f, 1f)
        }

    // Entities
    val balls = mutableListOf<Ball>()
    val bricks = mutableListOf<Brick>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatText>()
    val laserEffects = mutableListOf<LaserEffect>()
    val shockwaves = mutableListOf<Shockwave>()

    // Launch queue
    private var launchTimer: Float = 0f
    private var ballsToLaunchRemaining: Int = 0
    private val launchInterval: Float = 0.045f
    private var nextBrickId: Long = 1000L
    private var shootingTimer: Float = 0f

    // Combos & Juiciness
    var comboCount: Int = 0
    var shakeDuration: Float = 0f
    var shakeMagnitude: Float = 0f

    fun initialize(
        selectedMode: GameMode,
        stage: Int = 1,
        width: Float,
        height: Float,
        resetScore: Boolean = true
    ) {
        mode = selectedMode
        stageNumber = stage.coerceIn(1, 5000)
        screenWidth = width
        screenHeight = height

        playLeft = 16f
        playRight = width - 16f
        playTop = 8f // Starts right below Top HUD row with zero gap!
        groundY = height * 0.88f
        playBottom = groundY

        val boardWidth = playRight - playLeft
        cellWidth = boardWidth / cols
        cellHeight = cellWidth * 0.98f // Large, chunky, prominent boxes filling the arena!

        launcherBoxX = width / 2f
        launcherX = width / 2f
        nextLauncherX = launcherX
        isGameOver = false
        isStageClear = false
        isShooting = false
        isAiming = false
        turnsTaken = 0
        if (resetScore) {
            score = 0
            coinsEarnedInRun = 0
        }
        comboCount = 0
        gameSpeed = 1.0f
        lightningCharges = storage.lightningCharges
        clusterCharges = storage.clusterCharges

        balls.clear()
        bricks.clear()
        particles.clear()
        floatingTexts.clear()
        laserEffects.clear()
        shockwaves.clear()

        when (mode) {
            GameMode.QUEST -> {
                val stageInfo = StageData.getStage(stageNumber)
                totalBalls = stageInfo.initialBallCount
                ballsInReserve = totalBalls
                bricks.addAll(stageInfo.initialBricks)
                initialLevelHp = bricks.filter { it.type == BrickType.NORMAL }.sumOf { it.currentHp }.coerceAtLeast(1)
                currentLevelHp = initialLevelHp
            }
            GameMode.ENDLESS -> {
                wave = 1
                totalBalls = 45 // Generous starting balls for endless mode
                ballsInReserve = 45
                spawnEndlessWave(1, baseHp = 15)
                initialLevelHp = bricks.filter { it.type == BrickType.NORMAL }.sumOf { it.currentHp }.coerceAtLeast(1)
                currentLevelHp = initialLevelHp
            }
            GameMode.RUSH_100 -> {
                wave = 1
                totalBalls = 100
                ballsInReserve = 100
                spawnEndlessWave(1, baseHp = 35)
                initialLevelHp = bricks.filter { it.type == BrickType.NORMAL }.sumOf { it.currentHp }.coerceAtLeast(1)
                currentLevelHp = initialLevelHp
            }
        }
    }

    private fun spawnEndlessWave(currentWave: Int, baseHp: Int = currentWave) {
        val random = Random(System.currentTimeMillis() + currentWave)
        val numBricks = random.nextInt(4, 7)
        val columns = (0 until cols).shuffled(random).take(numBricks)
        val shapes = listOf(
            BrickShape.RECTANGLE,
            BrickShape.RECTANGLE,
            BrickShape.TRIANGLE_TOP_LEFT,
            BrickShape.TRIANGLE_TOP_RIGHT,
            BrickShape.TRIANGLE_BOTTOM_LEFT,
            BrickShape.TRIANGLE_BOTTOM_RIGHT
        )

        var extraBallCount = 0

        for (c in columns) {
            val roll = random.nextFloat()
            val type = when {
                extraBallCount < 2 && roll < 0.28f -> {
                    extraBallCount++
                    BrickType.ADD_BALL
                }
                roll < 0.08f -> BrickType.BOMB
                roll < 0.14f -> BrickType.LASER_HORIZONTAL
                roll < 0.18f -> BrickType.LASER_VERTICAL
                roll < 0.22f -> BrickType.SPLITTER
                else -> BrickType.NORMAL
            }
            val shape = if (type == BrickType.NORMAL && random.nextFloat() < 0.35f) {
                shapes.random(random)
            } else {
                BrickShape.RECTANGLE
            }
            val hp = if (type == BrickType.NORMAL) {
                (baseHp + random.nextInt(0, 5)).coerceIn(10, 60)
            } else 1
            bricks.add(Brick(nextBrickId++, c, 1, hp, hp, type, shape = shape))
        }

        if (extraBallCount == 0) {
            val available = (0 until cols).filter { c -> bricks.none { it.col == c && it.row == 1 } }
            if (available.isNotEmpty()) {
                val c = available.random(random)
                bricks.add(Brick(nextBrickId++, c, 1, 1, 1, BrickType.ADD_BALL))
            }
        }
    }

    fun useLightningStrike() {
        if (lightningCharges <= 0 || isGameOver || isStageClear) return
        lightningCharges--
        storage.lightningCharges = lightningCharges
        soundManager.playLaserSound()
        soundManager.triggerExplosionHaptic()

        // Choose columns that actually have alive bricks
        val aliveBricks = synchronized(bricks) { bricks.filter { it.isAlive && it.type != BrickType.ADD_BALL } }
        val colsWithBricks = aliveBricks.map { it.col }.distinct()

        val targetCols = if (colsWithBricks.isNotEmpty()) {
            colsWithBricks.shuffled().take(2)
        } else {
            val c1 = Random.nextInt(cols)
            listOf(c1, (c1 + 1) % cols)
        }

        targetCols.forEach { col ->
            val colCenterX = playLeft + col * cellWidth + cellWidth / 2f
            synchronized(laserEffects) {
                if (laserEffects.size < 6) {
                    laserEffects.add(LaserEffect(isHorizontal = false, centerCoord = colCenterX, color = Color(0xFF00E5FF)))
                }
            }
            val colBricks = synchronized(bricks) { bricks.filter { it.isAlive && it.col == col } }
            for (b in colBricks) {
                if (!b.isAlive) continue
                b.currentHp -= 50
                val ry = playTop + b.row * cellHeight + cellHeight / 2f
                if (b.currentHp <= 0) {
                    destroyBrick(b, colCenterX, ry)
                } else {
                    spawnCosmicDebris(colCenterX, ry, b.color(), 6)
                }
            }
        }
        synchronized(floatingTexts) {
            if (floatingTexts.size < 15) {
                floatingTexts.add(FloatText("⚡ LIGHTNING!", screenWidth / 2f, playTop + 100f, 1f, Color(0xFF00E5FF)))
            }
        }
    }

    fun useClusterBomb() {
        if (clusterCharges <= 0 || isGameOver || isStageClear) return
        clusterCharges--
        storage.clusterCharges = clusterCharges
        soundManager.playBombSound()
        soundManager.triggerExplosionHaptic()

        totalBalls += 30
        if (isShooting) {
            ballsToLaunchRemaining += 30
        } else {
            ballsInReserve += 30
        }
        synchronized(floatingTexts) {
            if (floatingTexts.size < 15) {
                floatingTexts.add(FloatText("✦ +30 MEGA BALLS!", screenWidth / 2f, groundY - 80f, 1f, Color(0xFFFF007F)))
            }
        }
        synchronized(shockwaves) {
            shockwaves.add(Shockwave(launcherX, groundY, maxRadius = 250f, color = Color(0xFFFF007F)))
        }
    }

    fun startAim(touchX: Float, touchY: Float) {
        if (isShooting || isGameOver || isStageClear || isPaused) return
        launcherX = launcherX.coerceIn(minLauncherX, maxLauncherX)

        val dx = touchX - launcherX
        val dy = touchY - groundY

        val effectiveDy = if (dy >= -10f) -120f else dy
        val effectiveDx = if (abs(dx) < 1f) 0.001f else dx

        val rawAngle = atan2(effectiveDy, effectiveDx)
        val minAngle = -170f * (PI.toFloat() / 180f)
        val maxAngle = -10f * (PI.toFloat() / 180f)
        aimAngle = rawAngle.coerceIn(minAngle, maxAngle)
        aimVector = Offset(cos(aimAngle), sin(aimAngle))
        isAiming = true
    }

    fun dragAim(deltaX: Float, touchX: Float, touchY: Float) {
        if (isShooting || isGameOver || isStageClear || isPaused) return

        launcherX = (launcherX + deltaX).coerceIn(minLauncherX, maxLauncherX)

        val dx = touchX - launcherX
        val dy = touchY - groundY

        val effectiveDy = if (dy >= -10f) -120f else dy
        val effectiveDx = if (abs(dx) < 1f) 0.001f else dx

        val rawAngle = atan2(effectiveDy, effectiveDx)
        val minAngle = -170f * (PI.toFloat() / 180f)
        val maxAngle = -10f * (PI.toFloat() / 180f)
        aimAngle = rawAngle.coerceIn(minAngle, maxAngle)
        aimVector = Offset(cos(aimAngle), sin(aimAngle))
        isAiming = true
    }

    fun updateAim(touchX: Float, touchY: Float) {
        if (isShooting || isGameOver || isStageClear || isPaused) return
        launcherX = launcherX.coerceIn(minLauncherX, maxLauncherX)

        val dx = touchX - launcherX
        val dy = touchY - groundY

        val effectiveDy = if (dy >= -10f) -120f else dy
        val effectiveDx = if (abs(dx) < 1f) 0.001f else dx

        val rawAngle = atan2(effectiveDy, effectiveDx)
        val minAngle = -170f * (PI.toFloat() / 180f)
        val maxAngle = -10f * (PI.toFloat() / 180f)
        aimAngle = rawAngle.coerceIn(minAngle, maxAngle)
        aimVector = Offset(cos(aimAngle), sin(aimAngle))
        isAiming = true
    }

    fun releaseAim() {
        if (!isAiming || isShooting || isGameOver || isStageClear || isPaused) return
        isAiming = false
        startVolley()
    }

    fun cancelAim() {
        isAiming = false
    }

    private fun startVolley() {
        isShooting = true
        firstBallLanded = false
        ballsToLaunchRemaining = totalBalls
        ballsInReserve = 0
        launchTimer = 0f
        shootingTimer = 0f
        comboCount = 0
        turnsTaken++
        soundManager.playLaunchSound()
    }

    fun recallBalls() {
        if (!isShooting) return
        val targetX = launcherX.coerceIn(playLeft + ballRadius, playRight - ballRadius)
        firstBallLanded = true
        nextLauncherX = targetX
        balls.forEach { ball ->
            if (ball.isActive) {
                ball.isActive = false
                ball.hasLanded = true
                ball.bounceCount = 0
                ball.trail.clear()
                ballsInReserve++
                ball.x = targetX
                ball.y = groundY
            }
        }
        ballsToLaunchRemaining = 0
        shootingTimer = 0f
        launcherX = targetX
        launcherBoxX = targetX
    }

    fun toggleSpeed() {
        gameSpeed = when (gameSpeed) {
            1.0f -> 2.0f
            2.0f -> 3.0f
            else -> 1.0f
        }
    }

    fun calculateTrajectory(): List<Offset> {
        val points = mutableListOf<Offset>()
        var currX = launcherX
        var currY = groundY - ballRadius
        var dirX = aimVector.x
        var dirY = aimVector.y

        points.add(Offset(currX, currY))

        for (bounce in 0 until 3) {
            var closestDist = Float.MAX_VALUE
            var hitX = 0f
            var hitY = 0f
            var hitNormalX = 0f
            var hitNormalY = 0f
            var didHit = false

            // Left Wall
            if (dirX < 0) {
                val targetX = playLeft + ballRadius
                val dist = (targetX - currX) / dirX
                if (dist > 0.1f && dist < closestDist) {
                    val candidateY = currY + dirY * dist
                    if (candidateY in playTop..groundY) {
                        closestDist = dist
                        hitX = targetX
                        hitY = candidateY
                        hitNormalX = 1f
                        hitNormalY = 0f
                        didHit = true
                    }
                }
            }
            // Right Wall
            if (dirX > 0) {
                val targetX = playRight - ballRadius
                val dist = (targetX - currX) / dirX
                if (dist > 0.1f && dist < closestDist) {
                    val candidateY = currY + dirY * dist
                    if (candidateY in playTop..groundY) {
                        closestDist = dist
                        hitX = targetX
                        hitY = candidateY
                        hitNormalX = -1f
                        hitNormalY = 0f
                        didHit = true
                    }
                }
            }
            // Ceiling
            if (dirY < 0) {
                val targetY = playTop + ballRadius
                val dist = (targetY - currY) / dirY
                if (dist > 0.1f && dist < closestDist) {
                    val candidateX = currX + dirX * dist
                    if (candidateX in (playLeft + ballRadius)..(playRight - ballRadius)) {
                        closestDist = dist
                        hitX = candidateX
                        hitY = targetY
                        hitNormalX = 0f
                        hitNormalY = 1f
                        didHit = true
                    }
                }
            }

            // Alive bricks
            for (brick in bricks) {
                if (!brick.isAlive) continue
                val bLeft = playLeft + brick.col * cellWidth
                val bTop = playTop + brick.row * cellHeight
                val bRight = bLeft + cellWidth - 3f
                val bBottom = bTop + cellHeight - 3f

                val expandedLeft = bLeft - ballRadius
                val expandedRight = bRight + ballRadius
                val expandedTop = bTop - ballRadius
                val expandedBottom = bBottom + ballRadius

                val t1 = (expandedLeft - currX) / (if (dirX != 0f) dirX else 0.0001f)
                val t2 = (expandedRight - currX) / (if (dirX != 0f) dirX else 0.0001f)
                val t3 = (expandedTop - currY) / (if (dirY != 0f) dirY else 0.0001f)
                val t4 = (expandedBottom - currY) / (if (dirY != 0f) dirY else 0.0001f)

                val tmin = max(min(t1, t2), min(t3, t4))
                val tmax = min(max(t1, t2), max(t3, t4))

                if (tmax >= 0 && tmin <= tmax && tmin > 0.1f && tmin < closestDist) {
                    closestDist = tmin
                    hitX = currX + dirX * tmin
                    hitY = currY + dirY * tmin
                    didHit = true

                    if (abs(hitX - expandedLeft) < 1.5f) {
                        hitNormalX = -1f; hitNormalY = 0f
                    } else if (abs(hitX - expandedRight) < 1.5f) {
                        hitNormalX = 1f; hitNormalY = 0f
                    } else if (abs(hitY - expandedTop) < 1.5f) {
                        hitNormalX = 0f; hitNormalY = -1f
                    } else {
                        hitNormalX = 0f; hitNormalY = 1f
                    }
                }
            }

            if (didHit) {
                points.add(Offset(hitX, hitY))
                val dot = dirX * hitNormalX + dirY * hitNormalY
                dirX -= 2 * dot * hitNormalX
                dirY -= 2 * dot * hitNormalY
                currX = hitX
                currY = hitY
            } else {
                break
            }
        }

        return points
    }

    fun update(rawDt: Float) {
        if (isPaused) return

        if (isStageClear) {
            isShooting = false
            if (balls.isNotEmpty()) {
                balls.clear()
            }
            return
        }

        val dt = (rawDt.coerceIn(0.001f, 0.033f)) * gameSpeed

        if (shakeDuration > 0f) {
            shakeDuration -= rawDt
            if (shakeDuration <= 0f) {
                shakeMagnitude = 0f
            }
        }

        val laserIter = laserEffects.iterator()
        while (laserIter.hasNext()) {
            val laser = laserIter.next()
            laser.timer -= rawDt
            if (laser.timer <= 0f) {
                laserIter.remove()
            }
        }

        val swIter = shockwaves.iterator()
        while (swIter.hasNext()) {
            val sw = swIter.next()
            sw.currentRadius += 450f * rawDt
            sw.alpha = (1f - (sw.currentRadius / sw.maxRadius)).coerceAtLeast(0f)
            if (sw.currentRadius >= sw.maxRadius) {
                swIter.remove()
            }
        }

        val textIter = floatingTexts.iterator()
        while (textIter.hasNext()) {
            val ft = textIter.next()
            ft.y -= 75f * rawDt
            ft.alpha -= 1.2f * rawDt
            if (ft.alpha <= 0f) {
                textIter.remove()
            }
        }

        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * rawDt
            p.y += p.vy * rawDt
            p.vy += 320f * rawDt
            p.rotation += 180f * rawDt
            p.life -= rawDt
            if (p.life <= 0f) {
                pIter.remove()
            }
        }

        if (isShooting) {
            shootingTimer += rawDt
            // Anti-hang protection: if balls have been bouncing for >14s, push downward
            if (shootingTimer >= 14f) {
                for (b in balls) {
                    if (b.isActive) {
                        b.vy = abs(b.vy).coerceAtLeast(350f)
                    }
                }
            }
            // If still bouncing after 22s, auto-recall so the turn finishes cleanly and game never hangs
            if (shootingTimer >= 22f) {
                recallBalls()
            }

            if (ballsToLaunchRemaining > 0) {
                launchTimer += dt
                while (launchTimer >= launchInterval && ballsToLaunchRemaining > 0) {
                    launchTimer -= launchInterval
                    ballsToLaunchRemaining--
                    val baseSpeed = 1150f
                    val newBall = Ball(
                        id = totalBalls - ballsToLaunchRemaining,
                        x = launcherX,
                        y = groundY - ballRadius,
                        vx = aimVector.x * baseSpeed,
                        vy = aimVector.y * baseSpeed,
                        isActive = true,
                        hasLanded = false
                    )
                    balls.add(newBall)
                }
            }

            val subSteps = 3
            val subDt = dt / subSteps

            for (step in 0 until subSteps) {
                if (isStageClear) break
                updateBallsPhysics(subDt)
            }

            if (isStageClear) {
                isShooting = false
                shootingTimer = 0f
                balls.clear()
                return
            }

            var allFinished = ballsToLaunchRemaining == 0
            for (ball in balls) {
                if (ball.isActive) {
                    allFinished = false
                } else if (ball.hasLanded) {
                    val dx = nextLauncherX - ball.x
                    if (abs(dx) > 2f) {
                        ball.x += sign(dx) * 750f * dt
                    } else {
                        ball.x = nextLauncherX
                    }
                }
            }

            if (allFinished) {
                endRound()
            }
        }
    }

    private fun updateBallsPhysics(dt: Float) {
        if (isStageClear || isGameOver) return
        val ballsToSplit = mutableListOf<Ball>()

        for (ball in balls) {
            if (isStageClear) break
            if (!ball.isActive) continue

            ball.trail.add(Offset(ball.x, ball.y))
            if (ball.trail.size > 5) {
                ball.trail.removeAt(0)
            }

            ball.x += ball.vx * dt
            ball.y += ball.vy * dt

            // Left / Right wall
            if (ball.x <= playLeft + ballRadius) {
                ball.x = playLeft + ballRadius
                ball.vx = abs(ball.vx)
                ball.bounceCount++
                soundManager.playBounceSound()
            } else if (ball.x >= playRight - ballRadius) {
                ball.x = playRight - ballRadius
                ball.vx = -abs(ball.vx)
                ball.bounceCount++
                soundManager.playBounceSound()
            }

            // Top wall
            if (ball.y <= playTop + ballRadius) {
                ball.y = playTop + ballRadius
                ball.vy = abs(ball.vy)
                ball.bounceCount++
                soundManager.playBounceSound()
            }

            // Anti-horizontal trap: prevent near-zero vertical velocity
            if (abs(ball.vy) < 130f) {
                ball.vy = if (ball.vy >= 0) 160f else -160f
            }
            // Anti-infinite bounce trap: if a ball bounced 18+ times in a turn, nudge it down
            if (ball.bounceCount > 18) {
                ball.vy = abs(ball.vy).coerceAtLeast(420f)
            }

            // Brick collisions (supports both rectangular and triangular angled bricks)
            for (brick in bricks) {
                if (!brick.isAlive) continue

                val bLeft = playLeft + brick.col * cellWidth
                val bTop = playTop + brick.row * cellHeight
                val bRight = bLeft + cellWidth - 3f
                val bBottom = bTop + cellHeight - 3f

                val isTriangle = brick.shape != BrickShape.RECTANGLE
                var collided = false
                var hitNormalX = 0f
                var hitNormalY = 0f

                if (!isTriangle) {
                    val closestX = ball.x.coerceIn(bLeft, bRight)
                    val closestY = ball.y.coerceIn(bTop, bBottom)
                    val distX = ball.x - closestX
                    val distY = ball.y - closestY
                    val distSq = distX * distX + distY * distY
                    if (distSq <= ballRadius * ballRadius) {
                        collided = true
                        val overlapX = ballRadius - abs(distX)
                        val overlapY = ballRadius - abs(distY)
                        if (overlapX < overlapY) {
                            hitNormalX = if (distX > 0) 1f else -1f
                            hitNormalY = 0f
                            ball.x += if (distX > 0) overlapX else -overlapX
                        } else {
                            hitNormalX = 0f
                            hitNormalY = if (distY > 0) 1f else -1f
                            ball.y += if (distY > 0) overlapY else -overlapY
                        }
                    }
                } else {
                    // Triangle collision with 45-degree hypotenuse reflection
                    val inAABB = ball.x + ballRadius >= bLeft && ball.x - ballRadius <= bRight &&
                                 ball.y + ballRadius >= bTop && ball.y - ballRadius <= bBottom
                    if (inAABB) {
                        val relX = ball.x - bLeft
                        val relY = ball.y - bTop
                        val w = bRight - bLeft

                        when (brick.shape) {
                            BrickShape.TRIANGLE_TOP_LEFT -> {
                                val diagDist = (relX + relY - w) / 1.4142f
                                if (diagDist >= -ballRadius && diagDist <= ballRadius && relX >= -ballRadius && relY >= -ballRadius) {
                                    collided = true
                                    hitNormalX = 0.7071f
                                    hitNormalY = 0.7071f
                                    ball.x += (ballRadius - diagDist) * 0.7071f
                                    ball.y += (ballRadius - diagDist) * 0.7071f
                                }
                            }
                            BrickShape.TRIANGLE_TOP_RIGHT -> {
                                val diagDist = (relY - relX) / 1.4142f
                                if (diagDist >= -ballRadius && diagDist <= ballRadius) {
                                    collided = true
                                    hitNormalX = -0.7071f
                                    hitNormalY = 0.7071f
                                    ball.x -= (ballRadius - diagDist) * 0.7071f
                                    ball.y += (ballRadius - diagDist) * 0.7071f
                                }
                            }
                            BrickShape.TRIANGLE_BOTTOM_LEFT -> {
                                val diagDist = (relX - relY) / 1.4142f
                                if (diagDist >= -ballRadius && diagDist <= ballRadius) {
                                    collided = true
                                    hitNormalX = 0.7071f
                                    hitNormalY = -0.7071f
                                    ball.x += (ballRadius - diagDist) * 0.7071f
                                    ball.y -= (ballRadius - diagDist) * 0.7071f
                                }
                            }
                            BrickShape.TRIANGLE_BOTTOM_RIGHT -> {
                                val diagDist = (w - (relX + relY)) / 1.4142f
                                if (diagDist >= -ballRadius && diagDist <= ballRadius) {
                                    collided = true
                                    hitNormalX = -0.7071f
                                    hitNormalY = -0.7071f
                                    ball.x -= (ballRadius - diagDist) * 0.7071f
                                    ball.y -= (ballRadius - diagDist) * 0.7071f
                                }
                            }
                            else -> {}
                        }
                    }
                }

                if (collided) {
                    ball.bounceCount++
                    val dot = ball.vx * hitNormalX + ball.vy * hitNormalY
                    if (dot < 0) {
                        ball.vx -= 2 * dot * hitNormalX
                        ball.vy -= 2 * dot * hitNormalY
                    } else if (!isTriangle) {
                        if (hitNormalX != 0f) ball.vx = -ball.vx
                        if (hitNormalY != 0f) ball.vy = -ball.vy
                    }

                    if (abs(ball.vy) < 80f) {
                        ball.vy = if (ball.vy >= 0) 100f else -100f
                    }

                    val hitCenterX = bLeft + (bRight - bLeft) / 2f
                    val hitCenterY = bTop + (bBottom - bTop) / 2f

                    when (brick.type) {
                        BrickType.ADD_BALL -> {
                            brick.isAlive = false
                            ballsEarnedThisRound++
                            soundManager.playCollectBallSound()
                            soundManager.triggerHitHaptic()
                            spawnCosmicDebris(hitCenterX, hitCenterY, Color(0xFF00E676), 8)
                            synchronized(floatingTexts) {
                                if (floatingTexts.size < 15) {
                                    floatingTexts.add(FloatText("+1 BALL", hitCenterX, hitCenterY, 1f, Color(0xFF00E676)))
                                }
                            }
                        }
                        BrickType.SPLITTER -> {
                            brick.isAlive = false
                            soundManager.playHitSound(comboCount++)
                            spawnCosmicDebris(hitCenterX, hitCenterY, Color(0xFFFFD600), 8)
                            if (balls.size + ballsToSplit.size < 70) {
                                val speed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
                                val currentAngle = atan2(ball.vy, ball.vx)
                                ballsToSplit.add(
                                    Ball(
                                        id = Random.nextInt(),
                                        x = ball.x,
                                        y = ball.y,
                                        vx = cos(currentAngle + 0.45f) * speed,
                                        vy = sin(currentAngle + 0.45f) * speed,
                                        isActive = true
                                    )
                                )
                                ballsToSplit.add(
                                    Ball(
                                        id = Random.nextInt(),
                                        x = ball.x,
                                        y = ball.y,
                                        vx = cos(currentAngle - 0.45f) * speed,
                                        vy = sin(currentAngle - 0.45f) * speed,
                                        isActive = true
                                    )
                                )
                            }
                        }
                        else -> {
                            handleBrickHit(brick, hitCenterX, hitCenterY)
                        }
                    }
                    break
                }
            }

            // Bottom landing
            if (ball.y >= groundY) {
                ball.y = groundY
                ball.isActive = false
                ball.hasLanded = true
                ball.bounceCount = 0
                ball.trail.clear()
                ballsInReserve++

                if (!firstBallLanded) {
                    firstBallLanded = true
                    nextLauncherX = ball.x.coerceIn(playLeft + ballRadius, playRight - ballRadius)
                }
            }
        }

        balls.addAll(ballsToSplit)
    }

    private fun handleBrickHit(brick: Brick, hitX: Float, hitY: Float) {
        brick.currentHp--
        comboCount++
        score += 15 * (1 + comboCount / 5)

        soundManager.playHitSound(comboCount)
        soundManager.triggerHitHaptic()

        if (brick.type == BrickType.NORMAL) {
            currentLevelHp = (currentLevelHp - 1).coerceAtLeast(0)
        }

        if (brick.currentHp <= 0) {
            destroyBrick(brick, hitX, hitY)
        } else {
            // Spawn neon cosmic debris squares & sparks on hit
            spawnCosmicDebris(hitX, hitY, brick.color(), count = 6)
        }
    }

    private fun destroyBrick(brick: Brick, hitX: Float, hitY: Float) {
        brick.isAlive = false
        score += 75
        coinsEarnedInRun += 1
        currentLevelHp = (currentLevelHp - 1).coerceAtLeast(0)

        soundManager.playBreakSound()
        soundManager.triggerBreakHaptic()
        spawnCosmicDebris(hitX, hitY, brick.color(), count = 18)

        when (brick.type) {
            BrickType.BOMB -> triggerBombExplosion(brick.col, brick.row, hitX, hitY)
            BrickType.LASER_HORIZONTAL -> triggerHorizontalLaser(brick.row, hitY)
            BrickType.LASER_VERTICAL -> triggerVerticalLaser(brick.col, hitX)
            else -> {}
        }

        if (mode == GameMode.QUEST && !isStageClear) {
            val normalBricksLeft = bricks.count { it.isAlive && it.type != BrickType.ADD_BALL }
            if (normalBricksLeft == 0) {
                completeStageInstantly()
            }
        }
    }

    private var isBombExploding = false

    private fun triggerBombExplosion(bCol: Int, bRow: Int, x: Float, y: Float) {
        if (isBombExploding) return
        isBombExploding = true
        try {
            soundManager.playBombSound()
            soundManager.triggerExplosionHaptic()
            shockwaves.add(Shockwave(x, y, maxRadius = cellWidth * 2.5f, color = BrickBomb))
            floatingTexts.add(FloatText("💥 BOOM!", x, y - 20f, 1f, BrickBomb))

            for (r in (bRow - 1)..(bRow + 1)) {
                for (c in (bCol - 1)..(bCol + 1)) {
                    if (r == bRow && c == bCol) continue
                    bricks.find { it.isAlive && it.col == c && it.row == r }?.let { adj ->
                        // Do not cascade into another bomb or let bombs wipe each other out in chains
                        if (adj.type == BrickType.BOMB) {
                            return@let
                        }
                        adj.currentHp -= 18
                        if (adj.currentHp <= 0) {
                            val adjX = playLeft + adj.col * cellWidth + cellWidth / 2f
                            val adjY = playTop + adj.row * cellHeight + cellHeight / 2f
                            destroyBrick(adj, adjX, adjY)
                        } else {
                            val adjX = playLeft + adj.col * cellWidth + cellWidth / 2f
                            val adjY = playTop + adj.row * cellHeight + cellHeight / 2f
                            spawnCosmicDebris(adjX, adjY, adj.color(), 8)
                        }
                    }
                }
            }
        } finally {
            isBombExploding = false
        }
    }

    private val activeLaserRows = mutableSetOf<Int>()
    private val activeLaserCols = mutableSetOf<Int>()

    private fun triggerHorizontalLaser(row: Int, y: Float) {
        if (!activeLaserRows.add(row)) return
        try {
            soundManager.playLaserSound()
            soundManager.triggerBreakHaptic()
            synchronized(laserEffects) {
                if (laserEffects.size < 6) {
                    laserEffects.add(LaserEffect(isHorizontal = true, centerCoord = y, color = Color(0xFF00E5FF)))
                }
            }
            synchronized(floatingTexts) {
                if (floatingTexts.size < 15) {
                    floatingTexts.add(FloatText("⚡ LASER!", playRight - 100f, y, 1f, Color(0xFF00E5FF)))
                }
            }

            val rowBricks = synchronized(bricks) { bricks.filter { it.isAlive && it.row == row } }
            for (rowBrick in rowBricks) {
                rowBrick.currentHp -= 50
                val rx = playLeft + rowBrick.col * cellWidth + cellWidth / 2f
                if (rowBrick.currentHp <= 0) {
                    destroyBrick(rowBrick, rx, y)
                } else {
                    spawnCosmicDebris(rx, y, rowBrick.color(), 6)
                }
            }
        } finally {
            activeLaserRows.remove(row)
        }
    }

    private fun triggerVerticalLaser(col: Int, x: Float) {
        if (!activeLaserCols.add(col)) return
        try {
            soundManager.playLaserSound()
            soundManager.triggerBreakHaptic()
            synchronized(laserEffects) {
                if (laserEffects.size < 6) {
                    laserEffects.add(LaserEffect(isHorizontal = false, centerCoord = x, color = Color(0xFF00E5FF)))
                }
            }

            val colBricks = synchronized(bricks) { bricks.filter { it.isAlive && it.col == col } }
            for (colBrick in colBricks) {
                colBrick.currentHp -= 50
                val ry = playTop + colBrick.row * cellHeight + cellHeight / 2f
                if (colBrick.currentHp <= 0) {
                    destroyBrick(colBrick, x, ry)
                } else {
                    spawnCosmicDebris(x, ry, colBrick.color(), 6)
                }
            }
        } finally {
            activeLaserCols.remove(col)
        }
    }

    // Spawns square cosmic fragments and sparks matching the user reference
    private fun spawnCosmicDebris(x: Float, y: Float, color: Color, count: Int) {
        if (particles.size >= 75) return
        val actualCount = count.coerceAtMost(75 - particles.size)
        for (i in 0 until actualCount) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 320f + 60f
            val isSquare = (i % 2 == 0) // alternate between glowing square debris and circular sparks
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 3 == 0) Color(0xFF00E5FF) else color,
                    life = Random.nextFloat() * 0.45f + 0.2f,
                    maxLife = 0.65f,
                    size = if (isSquare) Random.nextFloat() * 6f + 4f else Random.nextFloat() * 4f + 2f,
                    isSquare = isSquare
                )
            )
        }
    }

    fun completeStageInstantly() {
        if (isStageClear || isGameOver) return
        isStageClear = true
        isShooting = false
        // Automatically collect any remaining add-ball items
        bricks.filter { it.isAlive && it.type == BrickType.ADD_BALL }.forEach {
            it.isAlive = false
            ballsEarnedThisRound++
        }
        totalBalls += ballsEarnedThisRound
        ballsInReserve = totalBalls
        ballsEarnedThisRound = 0
        // Deactivate balls safely; collection is cleared outside iteration in update()
        for (b in balls) {
            b.isActive = false
        }
        launcherX = nextLauncherX

        try {
            soundManager.playVictorySound()
        } catch (_: Throwable) {}

        val stageInfo = StageData.getStage(stageNumber)
        val stars = when {
            turnsTaken <= stageInfo.targetTurnsFor3Stars -> 3
            turnsTaken <= stageInfo.targetTurnsFor2Stars -> 2
            else -> 1
        }
        storage.setStageStars(stageNumber, stars)
        if (stageNumber >= storage.highestStageUnlocked) {
            storage.highestStageUnlocked = (stageNumber + 1).coerceAtMost(5000)
        }
        if (coinsEarnedInRun > 0) {
            storage.addCoins(coinsEarnedInRun)
            coinsEarnedInRun = 0
        }
        storage.addCoins(50 * stars)
    }

    private fun endRound() {
        isShooting = false
        shootingTimer = 0f
        balls.clear()
        totalBalls += ballsEarnedThisRound
        ballsInReserve = totalBalls
        ballsEarnedThisRound = 0
        if (coinsEarnedInRun > 0) {
            storage.addCoins(coinsEarnedInRun)
            coinsEarnedInRun = 0
        }
        launcherX = nextLauncherX.coerceIn(minLauncherX, maxLauncherX)

        if (mode == GameMode.QUEST && !isStageClear) {
            val normalBricksLeft = bricks.count { it.isAlive && it.type != BrickType.ADD_BALL }
            if (normalBricksLeft == 0) {
                completeStageInstantly()
                return
            }
        }

        if (mode != GameMode.QUEST) {
            bricks.forEach { brick ->
                if (brick.isAlive) {
                    brick.row++
                }
            }

            // Game over ONLY triggers when a live brick reaches the full bottom line (groundY) or maxRow
            val reachedBottom = bricks.any { 
                it.isAlive && it.type != BrickType.ADD_BALL && 
                (it.row >= maxRow || (playTop + (it.row + 1) * cellHeight) >= groundY)
            }
            if (reachedBottom) {
                isGameOver = true
                soundManager.playBombSound()
                soundManager.triggerExplosionHaptic()
                if (mode == GameMode.ENDLESS) {
                    storage.endlessHighScore = score
                } else if (mode == GameMode.RUSH_100) {
                    storage.rushHighScore = score
                }
                return
            }
        }

        bricks.removeAll { !it.isAlive }

        if (mode == GameMode.ENDLESS || mode == GameMode.RUSH_100) {
            wave++
            spawnEndlessWave(wave, baseHp = if (mode == GameMode.RUSH_100) wave * 4 else wave)
            if (mode == GameMode.ENDLESS) {
                storage.endlessHighScore = score
            } else {
                storage.rushHighScore = score
            }
        }
    }

    fun restart() {
        initialize(mode, stageNumber, screenWidth, screenHeight, resetScore = true)
    }

    fun replayCurrentStage() {
        isGameOver = false
        isStageClear = false
        initialize(
            selectedMode = mode,
            stage = stageNumber,
            width = screenWidth,
            height = screenHeight,
            resetScore = false // Preserve overall running score while restarting level objects
        )
    }

    fun nextStage() {
        if (isGameOver) return
        if (stageNumber >= 5000) {
            isStageClear = true
            return
        }
        val nextLevel = stageNumber + 1
        initialize(
            selectedMode = GameMode.QUEST,
            stage = nextLevel,
            width = screenWidth,
            height = screenHeight,
            resetScore = false // Keep running score across consecutive levels
        )
        onStageChangedListener?.invoke(nextLevel)
    }
}
