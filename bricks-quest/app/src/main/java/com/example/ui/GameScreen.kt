package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.engine.GameEngine
import com.example.engine.StageData
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameScreen(
    engine: GameEngine,
    soundManager: SoundManager,
    storage: GameStorage,
    mode: GameMode,
    stageNumber: Int,
    onStageChanged: ((Int) -> Unit)? = null,
    onBackToMenu: () -> Unit,
    onSelectStages: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var soundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(soundManager.isHapticsEnabled) }

    var sessionInitialized by remember(mode, stageNumber) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        engine.onStageChangedListener = { nextStage ->
            onStageChanged?.invoke(nextStage)
        }
        onDispose {
            engine.onStageChangedListener = null
        }
    }

    val activeSkin = remember(storage.activeSkinId) {
        BallSkin.ALL_SKINS.find { it.id == storage.activeSkinId } ?: BallSkin.ALL_SKINS.first()
    }

    val context = LocalContext.current
    val russoTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.russo_one)
                ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val montserratTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.montserrat)?.let {
                Typeface.create(it, Typeface.BOLD)
            } ?: Typeface.create("sans-serif", Typeface.BOLD)
        } catch (e: Exception) {
            Typeface.create("sans-serif", Typeface.BOLD)
        }
    }

    val numberPaint = remember(montserratTypeface) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = montserratTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isSubpixelText = true
        }
    }
    val numberOutlinePaint = remember(montserratTypeface) {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(220, 8, 10, 24)
            typeface = montserratTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isSubpixelText = true
        }
    }
    val textPaint = remember(russoTypeface) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = russoTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val textShadowPaint = remember(russoTypeface) {
        Paint().apply {
            color = android.graphics.Color.argb(220, 15, 10, 30)
            typeface = russoTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val ftPaint = remember(russoTypeface) {
        Paint().apply {
            typeface = russoTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val countPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
    }
    val cyberFramePath = remember { Path() }

    BackHandler {
        if (!isPaused && !engine.isGameOver && !engine.isStageClear) {
            isPaused = true
            engine.isPaused = true
        } else {
            onBackToMenu()
        }
    }

    // High performance game loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                try {
                    engine.update(dt)
                } catch (e: Throwable) {
                    android.util.Log.e("GameScreen", "Frame update error suppressed", e)
                }
                frameTick = now
            }
        }
    }

    // Luminous cybernetic frame edge pulse
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_pulse")
    val frameGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frameGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070010))
    ) {
        // Continuous 3D Circular Rotating Majestic Mountains & Purple Cosmic Atmosphere
        Purple3DMountainBackground(modifier = Modifier.fillMaxSize())

        // Subtle dark vignette to ensure the bricks & balls have maximum contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55070010),
                            Color.Transparent,
                            Color(0x65070010)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 2. TOP HUD (COMPACT, HORIZONTALLY ALIGNED, ~90% SCREEN WIDTH)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Small LEVEL 1 Capsule with Rounded Corners
                    CyberHudLevelBadge(
                        mode = mode,
                        stageNumber = engine.stageNumber,
                        totalBalls = engine.totalBalls
                    )

                    // 2. Slim Dynamic Progress Bar Capsule with Stars
                    CyberHudProgressBar(
                        progress = engine.levelProgress,
                        turnsTaken = engine.turnsTaken,
                        stageNumber = engine.stageNumber,
                        modifier = Modifier.weight(1f)
                    )

                    // 3. Compact Coin Counter Capsule
                    CyberHudCoinBadge(
                        coins = storage.coins + engine.coinsEarnedInRun
                    )

                    // 4. Circular Glowing Pause Button
                    CyberHudPauseButton(
                        onClick = {
                            isPaused = true
                            engine.isPaused = true
                        }
                    )
                }
            }

            // 3. MAIN GAMEPLAY CANVAS WITH CYBERNETIC SCI-FI FRAME
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    engine.startAim(offset.x, offset.y)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    engine.dragAim(dragAmount.x, change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    engine.releaseAim()
                                },
                                onDragCancel = {
                                    engine.cancelAim()
                                }
                            )
                        }
                        .testTag("game_canvas")
                ) {
                    @Suppress("UNUSED_VARIABLE")
                    val tick = frameTick

                    if (!sessionInitialized || kotlin.math.abs(engine.screenWidth - size.width) > 4f || kotlin.math.abs(engine.screenHeight - size.height) > 4f) {
                        if (!sessionInitialized) {
                            engine.initialize(
                                selectedMode = mode,
                                stage = stageNumber,
                                width = size.width,
                                height = size.height,
                                resetScore = true
                            )
                            sessionInitialized = true
                        } else {
                            // Dimensions changed (e.g. orientation): preserve ongoing level and running score
                            engine.initialize(
                                selectedMode = mode,
                                stage = engine.stageNumber,
                                width = size.width,
                                height = size.height,
                                resetScore = false
                            )
                        }
                    }

                    val nativeCanvas = drawContext.canvas.nativeCanvas

                    // DRAW FULL CHORUS / RECTANGULAR SCI-FI ARENA FRAME (Encloses all bricks completely with zero cut corners)
                    val frameMargin = 4.dp.toPx()
                    val pLeft = engine.playLeft - frameMargin
                    val pRight = engine.playRight + frameMargin
                    val pTop = engine.playTop - 6.dp.toPx()
                    val pBottom = engine.groundY + 28.dp.toPx()
                    val arenaWidth = pRight - pLeft
                    val arenaHeight = pBottom - pTop

                    // 1. Outer intense cyan glow (Clean full rectangular border)
                    drawRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.45f * frameGlow),
                        topLeft = Offset(pLeft, pTop),
                        size = Size(arenaWidth, arenaHeight),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // 2. Primary bright cyan cyber frame line
                    drawRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(pLeft, pTop),
                        size = Size(arenaWidth, arenaHeight),
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 3. Inner neon magenta / violet accent line
                    drawRect(
                        color = Color(0xFFD500F9).copy(alpha = 0.55f),
                        topLeft = Offset(pLeft + 2.5.dp.toPx(), pTop + 2.5.dp.toPx()),
                        size = Size(arenaWidth - 5.dp.toPx(), arenaHeight - 5.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // FOUR 90-DEGREE CORNER CYBER BRACKETS (Hugging the sharp corners cleanly without cutting into any bricks)
                    val bLen = 22.dp.toPx()
                    val bOffset = 5.dp.toPx()

                    // Top-Left L-Bracket
                    val tlBracket = Path().apply {
                        moveTo(pLeft - bOffset, pTop + bLen)
                        lineTo(pLeft - bOffset, pTop - bOffset)
                        lineTo(pLeft + bLen, pTop - bOffset)
                    }
                    drawPath(tlBracket, color = Color(0xFF00E5FF), style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(pLeft - bOffset, pTop - bOffset))
                    drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(pLeft - bOffset, pTop - bOffset))

                    // Top-Right L-Bracket
                    val trBracket = Path().apply {
                        moveTo(pRight + bOffset, pTop + bLen)
                        lineTo(pRight + bOffset, pTop - bOffset)
                        lineTo(pRight - bLen, pTop - bOffset)
                    }
                    drawPath(trBracket, color = Color(0xFF00E5FF), style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(pRight + bOffset, pTop - bOffset))
                    drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(pRight + bOffset, pTop - bOffset))

                    // Bottom-Left L-Bracket
                    val blBracket = Path().apply {
                        moveTo(pLeft - bOffset, pBottom - bLen)
                        lineTo(pLeft - bOffset, pBottom + bOffset)
                        lineTo(pLeft + bLen, pBottom + bOffset)
                    }
                    drawPath(blBracket, color = Color(0xFF00E5FF), style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(pLeft - bOffset, pBottom + bOffset))
                    drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(pLeft - bOffset, pBottom + bOffset))

                    // Bottom-Right L-Bracket
                    val brBracket = Path().apply {
                        moveTo(pRight + bOffset, pBottom - bLen)
                        lineTo(pRight + bOffset, pBottom + bOffset)
                        lineTo(pRight - bLen, pBottom + bOffset)
                    }
                    drawPath(brBracket, color = Color(0xFF00E5FF), style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(pRight + bOffset, pBottom + bOffset))
                    drawCircle(color = Color.White, radius = 1.2.dp.toPx(), center = Offset(pRight + bOffset, pBottom + bOffset))

                    // BOTTOM DANGER / DEADLINE INDICATOR AT THE FULL BOTTOM FLOOR
                    val dangerLineY = engine.groundY
                    val isDanger = engine.isAnyBrickNearBottom
                    val dangerGlowColor = if (isDanger) Color(0xFFFF1744) else Color(0xFF00E5FF).copy(alpha = 0.45f)

                    // Dashed laser line indicating bottom deadline
                    drawLine(
                        color = dangerGlowColor,
                        start = Offset(pLeft, dangerLineY),
                        end = Offset(pRight, dangerLineY),
                        strokeWidth = if (isDanger) 2.5.dp.toPx() else 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )

                    if (isDanger) {
                        // Pulsing red danger glow zone at the bottom
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFF1744).copy(alpha = 0.18f * frameGlow)
                                ),
                                startY = dangerLineY - 36.dp.toPx(),
                                endY = dangerLineY
                            ),
                            topLeft = Offset(pLeft, dangerLineY - 36.dp.toPx()),
                            size = Size(arenaWidth, 36.dp.toPx())
                        )
                    }

                    // DRAW SLEEK CRUISING SPACECRAFTS INSIDE ARENA (EXACT TO REFERENCE)
                    val shipTime = (frameTick / 1_000_000_000.0)
                    val goldShipX = pLeft + (pRight - pLeft) * 0.68f + (kotlin.math.sin(shipTime * 0.8) * 12f).toFloat()
                    val goldShipY = pTop + (pBottom - pTop) * 0.51f + (kotlin.math.cos(shipTime * 0.6) * 10f).toFloat()

                    // Flame plumes
                    val flameLen = 16.dp.toPx() + (kotlin.math.sin(shipTime * 15.0) * 4f).toFloat()
                    drawLine(
                        color = Color(0xFFFF9100),
                        start = Offset(goldShipX - 10.dp.toPx(), goldShipY - 4.dp.toPx()),
                        end = Offset(goldShipX - 10.dp.toPx() - flameLen, goldShipY - 8.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFD600),
                        start = Offset(goldShipX - 8.dp.toPx(), goldShipY + 2.dp.toPx()),
                        end = Offset(goldShipX - 8.dp.toPx() - flameLen * 0.9f, goldShipY - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Arrowhead body
                    val goldShipPath = Path().apply {
                        moveTo(goldShipX + 16.dp.toPx(), goldShipY)
                        lineTo(goldShipX - 10.dp.toPx(), goldShipY - 10.dp.toPx())
                        lineTo(goldShipX - 4.dp.toPx(), goldShipY)
                        lineTo(goldShipX - 10.dp.toPx(), goldShipY + 10.dp.toPx())
                        close()
                    }
                    drawPath(goldShipPath, color = Color(0xFFFFD600))
                    drawPath(goldShipPath, color = Color(0xFFFF9100), style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(goldShipX + 2.dp.toPx(), goldShipY))

                    // 2. Purple Stealth Fighter darting in lower-mid with violet ion plumes
                    val purpleShipX = pLeft + (pRight - pLeft) * 0.44f + (kotlin.math.cos(shipTime * 0.9) * 14f).toFloat()
                    val purpleShipY = pTop + (pBottom - pTop) * 0.63f + (kotlin.math.sin(shipTime * 0.7) * 8f).toFloat()

                    val purpleFlameLen = 14.dp.toPx() + (kotlin.math.cos(shipTime * 14.0) * 3f).toFloat()
                    drawLine(
                        color = Color(0xFFD500F9),
                        start = Offset(purpleShipX + 8.dp.toPx(), purpleShipY + 4.dp.toPx()),
                        end = Offset(purpleShipX + 8.dp.toPx() + purpleFlameLen, purpleShipY + 8.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx()
                    )
                    val purpleShipPath = Path().apply {
                        moveTo(purpleShipX - 14.dp.toPx(), purpleShipY - 2.dp.toPx())
                        lineTo(purpleShipX + 8.dp.toPx(), purpleShipY - 8.dp.toPx())
                        lineTo(purpleShipX + 3.dp.toPx(), purpleShipY)
                        lineTo(purpleShipX + 8.dp.toPx(), purpleShipY + 8.dp.toPx())
                        close()
                    }
                    drawPath(purpleShipPath, color = Color(0xFFBA68C8))
                    drawPath(purpleShipPath, color = Color(0xFFE040FB), style = Stroke(width = 1.5.dp.toPx()))

                    // DRAW 8 COLUMNS OF NEON GLOWING BRICKS (ACCURATE MATCH TO REFERENCE PICTURE)
                    val brickGap = 3.dp.toPx()
                    val bCornerR = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    val safeBricks = synchronized(engine.bricks) { engine.bricks.toList() }

                    for (brick in safeBricks) {
                        if (!brick.isAlive) continue

                        val bX = engine.playLeft + brick.col * engine.cellWidth + brickGap / 2f
                        val bY = engine.playTop + brick.row * engine.cellHeight + brickGap / 2f
                        val bW = engine.cellWidth - brickGap
                        val bH = engine.cellHeight - brickGap

                        val tones = brick.tones()
                        val isTriangle = brick.shape != BrickShape.RECTANGLE

                        var textCenterX = bX + bW / 2f
                        var textCenterY = bY + bH / 2f

                        if (!isTriangle) {
                            // 1. Soft Outer Neon Glow Aura
                            drawRoundRect(
                                color = tones.outerAura,
                                topLeft = Offset(bX - 2.dp.toPx(), bY - 2.dp.toPx()),
                                size = Size(bW + 4.dp.toPx(), bH + 4.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            // 2. Translucent Dark Neon-Tinted Fill (Matches picture glass look)
                            drawRoundRect(
                                color = tones.fill,
                                topLeft = Offset(bX, bY),
                                size = Size(bW, bH),
                                cornerRadius = bCornerR
                            )

                            // 3. Crisp Vibrant Neon Border
                            drawRoundRect(
                                color = tones.border,
                                topLeft = Offset(bX, bY),
                                size = Size(bW, bH),
                                cornerRadius = bCornerR,
                                style = Stroke(width = 2.4.dp.toPx())
                            )

                            // 4. Subtle Inner Glow Rim
                            drawRoundRect(
                                color = tones.glow.copy(alpha = 0.35f),
                                topLeft = Offset(bX + 1.2.dp.toPx(), bY + 1.2.dp.toPx()),
                                size = Size(bW - 2.4.dp.toPx(), bH - 2.4.dp.toPx()),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        } else {
                            // TRIANGLE BRICK (Diagonal Hypotenuse Bounce)
                            val triPath = Path()
                            when (brick.shape) {
                                BrickShape.TRIANGLE_TOP_LEFT -> {
                                    triPath.moveTo(bX, bY)
                                    triPath.lineTo(bX + bW, bY)
                                    triPath.lineTo(bX, bY + bH)
                                    triPath.close()
                                    textCenterX = bX + bW * 0.34f
                                    textCenterY = bY + bH * 0.34f
                                }
                                BrickShape.TRIANGLE_TOP_RIGHT -> {
                                    triPath.moveTo(bX, bY)
                                    triPath.lineTo(bX + bW, bY)
                                    triPath.lineTo(bX + bW, bY + bH)
                                    triPath.close()
                                    textCenterX = bX + bW * 0.66f
                                    textCenterY = bY + bH * 0.34f
                                }
                                BrickShape.TRIANGLE_BOTTOM_LEFT -> {
                                    triPath.moveTo(bX, bY)
                                    triPath.lineTo(bX, bY + bH)
                                    triPath.lineTo(bX + bW, bY + bH)
                                    triPath.close()
                                    textCenterX = bX + bW * 0.34f
                                    textCenterY = bY + bH * 0.66f
                                }
                                BrickShape.TRIANGLE_BOTTOM_RIGHT -> {
                                    triPath.moveTo(bX + bW, bY)
                                    triPath.lineTo(bX + bW, bY + bH)
                                    triPath.lineTo(bX, bY + bH)
                                    triPath.close()
                                    textCenterX = bX + bW * 0.66f
                                    textCenterY = bY + bH * 0.66f
                                }
                                else -> {}
                            }

                            // 1. Soft Outer Neon Aura
                            drawPath(triPath, color = tones.outerAura, style = Stroke(width = 5.dp.toPx()))

                            // 2. Translucent Tinted Dark Fill
                            drawPath(triPath, color = tones.fill)

                            // 3. Crisp Neon Outer Border
                            drawPath(triPath, color = tones.border, style = Stroke(width = 2.4.dp.toPx()))

                            // 4. Subtle Inner Accent
                            drawPath(triPath, color = tones.glow.copy(alpha = 0.35f), style = Stroke(width = 1.dp.toPx()))
                        }

                        // RENDER NUMBER OR SPECIAL ICON
                        when (brick.type) {
                            BrickType.ADD_BALL -> {
                                val targetR = (bH * 0.28f).coerceAtMost(bW * 0.28f)
                                // Concentric glowing rings matching reference image
                                drawCircle(
                                    color = Color(0xFF00E676).copy(alpha = 0.35f),
                                    radius = targetR * 1.35f,
                                    center = Offset(textCenterX, textCenterY),
                                    style = Stroke(width = 1.8.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF00E676),
                                    radius = targetR,
                                    center = Offset(textCenterX, textCenterY),
                                    style = Stroke(width = 2.2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = targetR * 0.42f,
                                    center = Offset(textCenterX, textCenterY)
                                )
                            }
                            BrickType.BOMB -> {
                                val bombR = (bH * 0.26f).coerceAtMost(bW * 0.26f)
                                drawCircle(
                                    color = Color(0xFFFF5722).copy(alpha = 0.4f),
                                    radius = bombR * 1.3f,
                                    center = Offset(textCenterX, textCenterY)
                                )
                                drawCircle(
                                    color = Color(0xFFFF5722),
                                    radius = bombR,
                                    center = Offset(textCenterX, textCenterY),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = bombR * 0.35f,
                                    center = Offset(textCenterX, textCenterY)
                                )
                            }
                            BrickType.LASER_HORIZONTAL -> {
                                val symSize = (bH * 0.36f).coerceAtMost(bW * 0.36f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = textCenterY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("◀▶", textCenterX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("◀▶", textCenterX, textY, numberPaint)
                            }
                            BrickType.LASER_VERTICAL -> {
                                val symSize = (bH * 0.36f).coerceAtMost(bW * 0.36f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = textCenterY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("▲▼", textCenterX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("▲▼", textCenterX, textY, numberPaint)
                            }
                            BrickType.SPLITTER -> {
                                val symSize = (bH * 0.38f).coerceAtMost(bW * 0.38f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = textCenterY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("✕2", textCenterX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("✕2", textCenterX, textY, numberPaint)
                            }
                            BrickType.NORMAL -> {
                                val numStr = "${brick.currentHp}"
                                val maxAllowedWidth = if (isTriangle) bW * 0.50f else bW * 0.65f
                                var targetTextSize = if (isTriangle) bH * 0.38f else bH * 0.44f
                                numberPaint.textSize = targetTextSize
                                val measuredW = numberPaint.measureText(numStr)
                                if (measuredW > maxAllowedWidth) {
                                    targetTextSize *= (maxAllowedWidth / measuredW)
                                    numberPaint.textSize = targetTextSize
                                }
                                numberOutlinePaint.textSize = targetTextSize
                                val textY = textCenterY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)

                                // 1. Crisp dark contour outline
                                nativeCanvas.drawText(numStr, textCenterX, textY, numberOutlinePaint)

                                // 2. Solid pure white number fill (as in reference screenshot)
                                nativeCanvas.drawText(numStr, textCenterX, textY, numberPaint)
                            }
                        }
                    }

                    // DRAW LASER BEAMS & LIGHTNING (SAFE SNAPSHOT)
                    val safeLasers = synchronized(engine.laserEffects) { engine.laserEffects.toList() }
                    for (laser in safeLasers) {
                        val alpha = (laser.timer / 0.25f).coerceIn(0f, 1f)
                        if (laser.isHorizontal) {
                            drawRect(
                                color = laser.color.copy(alpha = alpha * 0.9f),
                                topLeft = Offset(engine.playLeft, laser.centerCoord - 8f),
                                size = Size(engine.playRight - engine.playLeft, 16f)
                            )
                            drawRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(engine.playLeft, laser.centerCoord - 2f),
                                size = Size(engine.playRight - engine.playLeft, 4f)
                            )
                        } else {
                            // Vertical lightning beam
                            drawRect(
                                color = laser.color.copy(alpha = alpha * 0.9f),
                                topLeft = Offset(laser.centerCoord - 8f, engine.playTop),
                                size = Size(16f, engine.groundY - engine.playTop)
                            )
                            drawRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(laser.centerCoord - 2f, engine.playTop),
                                size = Size(4f, engine.groundY - engine.playTop)
                            )
                        }
                    }

                    // DRAW SHOCKWAVES (SAFE SNAPSHOT)
                    val safeShockwaves = synchronized(engine.shockwaves) { engine.shockwaves.toList() }
                    for (sw in safeShockwaves) {
                        drawCircle(
                            color = sw.color.copy(alpha = sw.alpha),
                            radius = sw.currentRadius,
                            center = Offset(sw.x, sw.y),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // DRAW PARTICLES & SQUARE COSMIC DEBRIS (SAFE SNAPSHOT)
                    val safeParticles = synchronized(engine.particles) { engine.particles.toList() }
                    for (p in safeParticles) {
                        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                        if (p.isSquare) {
                            // Glowing square fragment
                            drawRoundRect(
                                color = p.color.copy(alpha = alpha),
                                topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                                size = Size(p.size * alpha, p.size * alpha),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = alpha * 0.8f),
                                topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                                size = Size(p.size * alpha, p.size * alpha),
                                cornerRadius = CornerRadius(2f, 2f),
                                style = Stroke(width = 1f)
                            )
                        } else {
                            // Circular spark
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = p.size * alpha,
                                center = Offset(p.x, p.y)
                            )
                        }
                    }

                    // DRAW FLOATING TEXTS (SAFE SNAPSHOT)
                    ftPaint.textSize = 17.sp.toPx()
                    val safeTexts = synchronized(engine.floatingTexts) { engine.floatingTexts.toList() }
                    for (ft in safeTexts) {
                        ftPaint.color = ft.color.copy(alpha = ft.alpha.coerceIn(0f, 1f)).toArgb()
                        nativeCanvas.drawText(ft.text, ft.x, ft.y, ftPaint)
                    }

                    // DRAW AIMING TRAJECTORY (ELECTRIC BEAM WITH GLOWING ORBS LIKE REFERENCE)
                    if (engine.isAiming) {
                        val trajectory = engine.calculateTrajectory()
                        for (i in 0 until trajectory.size - 1) {
                            val p1 = trajectory[i]
                            val p2 = trajectory[i + 1]

                            // Solid electric laser beam core
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                                start = p1,
                                end = p2,
                                strokeWidth = 2.dp.toPx()
                            )

                            // Glowing orbs spaced along trajectory (matching reference image)
                            val segmentDist = (p2 - p1).getDistance()
                            val orbSpacing = 28.dp.toPx()
                            val numOrbs = (segmentDist / orbSpacing).toInt().coerceAtLeast(1)
                            for (orbIdx in 0..numOrbs) {
                                val t = orbIdx.toFloat() / numOrbs
                                val orbPos = p1 + (p2 - p1) * t
                                // Outer cyan halo
                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                                    radius = 7.dp.toPx(),
                                    center = orbPos
                                )
                                // Inner glowing orb
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.5.dp.toPx(),
                                    center = orbPos
                                )
                            }
                        }

                        // Target reticle at hit point
                        if (trajectory.size > 1) {
                            val hitPoint = trajectory[1]
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                radius = 18.dp.toPx(),
                                center = hitPoint
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 9.dp.toPx(),
                                center = hitPoint,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // DRAW ACTIVE BALLS & MOTION TRAILS (SAFE SNAPSHOT)
                    val safeBalls = synchronized(engine.balls) { engine.balls.toList() }
                    for (ball in safeBalls) {
                        if (ball.isActive) {
                            val safeTrail = synchronized(ball.trail) { ball.trail.toList() }
                            for ((tIdx, trailPoint) in safeTrail.withIndex()) {
                                val trailAlpha = (tIdx.toFloat() / (safeTrail.size.coerceAtLeast(1))) * 0.45f
                                drawCircle(
                                    color = activeSkin.glowColor.copy(alpha = trailAlpha),
                                    radius = engine.ballRadius * (0.6f + 0.4f * trailAlpha),
                                    center = trailPoint
                                )
                            }
                            drawCircle(
                                color = activeSkin.glowColor.copy(alpha = 0.5f),
                                radius = engine.ballRadius * 1.5f,
                                center = Offset(ball.x, ball.y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = engine.ballRadius,
                                center = Offset(ball.x, ball.y)
                            )
                        } else if (ball.hasLanded) {
                            drawCircle(
                                color = activeSkin.glowColor.copy(alpha = 0.4f),
                                radius = engine.ballRadius * 1.3f,
                                center = Offset(ball.x, ball.y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = engine.ballRadius,
                                center = Offset(ball.x, ball.y)
                            )
                        }
                    }

                    // DRAW NEON PURPLE LAUNCHER CAPSULE TRACK & BALL WITH TOP INDICATOR
                    val displayCount = if (engine.isShooting) engine.ballsInReserve else engine.totalBalls
                    val capsuleHeight = 20.dp.toPx()
                    val capsuleWidth = 145.dp.toPx()
                    val baseBallRadius = 8.5.dp.toPx()
                    engine.capsuleWidth = capsuleWidth
                    engine.controlBallRadius = baseBallRadius

                    val capsuleLeftTrack = engine.launcherBoxX - capsuleWidth / 2f
                    val capsuleRightTrack = engine.launcherBoxX + capsuleWidth / 2f
                    val capsuleCorner = CornerRadius(capsuleHeight / 2f, capsuleHeight / 2f)

                    // 1. TOP BALL COUNT INDICATOR ABOVE CYAN LINE (FIXED AT CENTER, MOVED SLIGHTLY DOWNWARD CLOSER TO LINE)
                    val countText = "x $displayCount"
                    countPaint.textSize = 15.sp.toPx()
                    val textWidth = countPaint.measureText(countText)
                    val indicatorIconRadius = 4.5.dp.toPx()
                    val indicatorSpacing = 7.dp.toPx()
                    val totalIndicatorWidth = (indicatorIconRadius * 2) + indicatorSpacing + textWidth

                    // Centered horizontally, positioned just slightly above the cyan horizontal line (pBottom) with a small clear gap
                    val indicatorStartX = (size.width / 2f) - (totalIndicatorWidth / 2f)
                    val indicatorY = pBottom - 6.dp.toPx()

                    // Glowing Circle Icon before "x [count]"
                    val circleCenter = Offset(indicatorStartX + indicatorIconRadius, indicatorY - 4.5.dp.toPx())
                    drawCircle(
                        color = Color(0xFFD500F9).copy(alpha = 0.5f),
                        radius = indicatorIconRadius + 3.dp.toPx(),
                        center = circleCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = indicatorIconRadius,
                        center = circleCenter,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE040FB).copy(alpha = 0.4f),
                        radius = indicatorIconRadius * 0.7f,
                        center = circleCenter
                    )

                    // Text "x [count]"
                    val textStartX = indicatorStartX + (indicatorIconRadius * 2) + indicatorSpacing
                    nativeCanvas.drawText(countText, textStartX, indicatorY, countPaint)

                    // 2. NEON PURPLE CAPSULE TRACK (Completely below the cyan line with clear visible gap)
                    val capsuleTopTrack = pBottom + 16.dp.toPx()
                    
                    // Inner Translucent Purple Fill
                    drawRoundRect(
                        color = Color(0xFF130324).copy(alpha = 0.88f),
                        topLeft = Offset(capsuleLeftTrack, capsuleTopTrack),
                        size = Size(capsuleWidth, capsuleHeight),
                        cornerRadius = capsuleCorner
                    )

                    // Wide Outer Neon Purple Bloom
                    drawRoundRect(
                        color = Color(0xFFD500F9).copy(alpha = 0.3f),
                        topLeft = Offset(capsuleLeftTrack - 3.dp.toPx(), capsuleTopTrack - 3.dp.toPx()),
                        size = Size(capsuleWidth + 6.dp.toPx(), capsuleHeight + 6.dp.toPx()),
                        cornerRadius = CornerRadius((capsuleHeight + 6.dp.toPx()) / 2f, (capsuleHeight + 6.dp.toPx()) / 2f),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Crisp Glowing Magenta/Purple Border
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFD500F9),
                                Color(0xFFFA58FF),
                                Color(0xFFD500F9)
                            )
                        ),
                        topLeft = Offset(capsuleLeftTrack, capsuleTopTrack),
                        size = Size(capsuleWidth, capsuleHeight),
                        cornerRadius = capsuleCorner,
                        style = Stroke(width = 2.4.dp.toPx())
                    )

                    // 3. GLOWING CENTER LAUNCHER BALL (Clamped strictly inside container inner edges)
                    val clampedLauncherX = engine.launcherX.coerceIn(
                        capsuleLeftTrack + baseBallRadius,
                        capsuleRightTrack - baseBallRadius
                    )
                    val launcherCenter = Offset(clampedLauncherX, capsuleTopTrack + capsuleHeight / 2f)

                    // Outermost diffuse violet halo
                    drawCircle(
                        color = Color(0xFFD500F9).copy(alpha = 0.4f),
                        radius = baseBallRadius * 2.8f,
                        center = launcherCenter
                    )
                    // Mid neon magenta halo
                    drawCircle(
                        color = Color(0xFFE040FB).copy(alpha = 0.75f),
                        radius = baseBallRadius * 1.8f,
                        center = launcherCenter
                    )
                    // Intense bright rim
                    drawCircle(
                        color = Color(0xFFFA58FF),
                        radius = baseBallRadius * 1.25f,
                        center = launcherCenter
                    )
                    // Radiant pure white core
                    drawCircle(
                        color = Color.White,
                        radius = baseBallRadius * 0.9f,
                        center = launcherCenter
                    )
                }
            }

            // 4. BOTTOM CYBERNETIC COCKPIT CONSOLE (EXACT MATCH TO REFERENCE IMAGE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT POWER-UP: LIGHTNING BOLT ⚡ with x5 badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(14.dp, CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF04284D), Color(0xFF051329))
                                )
                            )
                            .border(2.6.dp, Color(0xFF00E5FF), CircleShape)
                            .clickable { engine.useLightningStrike() }
                            .testTag("powerup_lightning")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Lightning Strike",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Circular x5 Badge with Cyan Border
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF061426))
                            .border(1.6.dp, Color(0xFF00E5FF), CircleShape)
                    ) {
                        Text(
                            text = "x${engine.lightningCharges}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // CENTER CONTROLS (Chamfered Multiplier Badge x1.00 & RECALL)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chamfered Multiplier Capsule: [x1.00]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .shadow(10.dp, CutCornerShape(8.dp), ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                            .clip(CutCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF06203D), Color(0xFF020F20))
                                )
                            )
                            .border(1.8.dp, Color(0xFF00E5FF), CutCornerShape(8.dp))
                            .clickable { engine.toggleSpeed() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("speed_toggle_button")
                    ) {
                        Text(
                            text = "x" + String.format(Locale.US, "%.2f", engine.gameSpeed),
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // Recall button (appears during shooting, styled like reference picture)
                    AnimatedVisibility(
                        visible = engine.isShooting,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(10.dp), ambientColor = Color(0xFFFF1744), spotColor = Color(0xFFFF1744))
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF260511))
                                .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(10.dp))
                                .clickable { engine.recallBalls() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("recall_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // White ball icon
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Down arrow ↓
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Recall",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // RIGHT POWER-UP: CLUSTER MULTI-BALL ✣ with x5 badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(14.dp, CircleShape, ambientColor = Color(0xFFD500F9), spotColor = Color(0xFFD500F9))
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF330642), Color(0xFF160321))
                                )
                            )
                            .border(2.6.dp, Color(0xFFD500F9), CircleShape)
                            .clickable { engine.useClusterBomb() }
                            .testTag("powerup_cluster")
                    ) {
                        // Diamond quad-orb cluster icon
                        Canvas(modifier = Modifier.size(32.dp)) {
                            val r = 4.5.dp.toPx()
                            val dist = 7.dp.toPx()
                            val c = center
                            listOf(
                                Offset(c.x - dist, c.y),
                                Offset(c.x + dist, c.y),
                                Offset(c.x, c.y - dist),
                                Offset(c.x, c.y + dist)
                            ).forEach { pos ->
                                drawCircle(color = Color(0xFFFF4081), radius = r * 1.3f, center = pos)
                                drawCircle(color = Color.White, radius = r * 0.5f, center = pos)
                            }
                        }
                    }

                    // Circular x5 Badge with Magenta Border
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D0529))
                            .border(1.6.dp, Color(0xFFD500F9), CircleShape)
                    ) {
                        Text(
                            text = "x${engine.clusterCharges}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // SCI-FI CYBER "GAME PAUSED" DIALOG (Exact Match to Image 2)
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(top = 28.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // MAIN SCI-FI CYBER CONTAINER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF061428),
                                        Color(0xFF030B18),
                                        Color(0xFF020610)
                                    )
                                )
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF0091EA),
                                        Color(0xFF00E5FF)
                                    )
                                ),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(horizontal = 22.dp, vertical = 24.dp)
                    ) {
                        // Inner cybernetic corner accents
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val cyan = Color(0xFF00E5FF)
                            val accentLen = 20.dp.toPx()

                            // Top-left notch
                            drawLine(cyan, Offset(0f, accentLen), Offset(accentLen, 0f), strokeWidth = 3.dp.toPx())
                            // Top-right notch
                            drawLine(cyan, Offset(w, accentLen), Offset(w - accentLen, 0f), strokeWidth = 3.dp.toPx())
                            // Bottom-left notch
                            drawLine(cyan, Offset(0f, h - accentLen), Offset(accentLen, h), strokeWidth = 3.dp.toPx())
                            // Bottom-right notch
                            drawLine(cyan, Offset(w, h - accentLen), Offset(w - accentLen, h), strokeWidth = 3.dp.toPx())
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(28.dp))

                            // TITLE: "GAME" (White Italic) & "PAUSED" (Neon Cyan Italic)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GAME",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "PAUSED",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // SCI-FI STAR DIVIDER (― ★ ―)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, Color(0xFF00E5FF))
                                            )
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(horizontal = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E5FF), Color.Transparent)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // 1. RESUME BUTTON (Bright Cyan Cyber Pill)
                            Surface(
                                onClick = {
                                    isPaused = false
                                    engine.isPaused = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF80D8FF)),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .align(Alignment.CenterHorizontally)
                                    .height(48.dp)
                                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                                    .testTag("resume_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF00E5FF),
                                                    Color(0xFF0091EA),
                                                    Color(0xFF0070E0),
                                                    Color(0xFF004EC2)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "▶",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(28.dp)
                                                .background(Color.White.copy(alpha = 0.4f))
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Text(
                                            text = "RESUME",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 19.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. RESTART BUTTON (Dark Steel Blue Cyber Pill)
                            Surface(
                                onClick = {
                                    isPaused = false
                                    engine.restart()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.85f)),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .align(Alignment.CenterHorizontally)
                                    .height(48.dp)
                                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF0284C7), spotColor = Color(0xFF0284C7))
                                    .testTag("restart_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF1E3A5F),
                                                    Color(0xFF132742),
                                                    Color(0xFF0B1728)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(28.dp)
                                                .background(Color.White.copy(alpha = 0.35f))
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Text(
                                            text = "RESTART",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 19.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3. QUIT TO MENU BUTTON (Crimson Red Cyber Pill with Neon Glow)
                            Surface(
                                onClick = {
                                    isPaused = false
                                    onBackToMenu()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5252)),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .align(Alignment.CenterHorizontally)
                                    .height(48.dp)
                                    .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFFF1744), spotColor = Color(0xFFFF1744))
                                    .testTag("quit_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFFFF1744),
                                                    Color(0xFFD50032),
                                                    Color(0xFFA00020),
                                                    Color(0xFF6B0014)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(28.dp)
                                                .background(Color.White.copy(alpha = 0.4f))
                                        )
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Text(
                                            text = "QUIT TO MENU",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TOP OVERLAPPING CIRCULAR SCI-FI PAUSE BADGE || WITH TECH WINGS
                    Box(
                        modifier = Modifier
                            .offset(y = (-32).dp)
                            .size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Flanking Cyber Tech Wings (<<< and >>>)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cyan = Color(0xFF00E5FF)
                            // Left horizontal wing bracket
                            drawLine(cyan, Offset(-12.dp.toPx(), h * 0.5f), Offset(-2.dp.toPx(), h * 0.5f), strokeWidth = 3.dp.toPx())
                            drawLine(cyan, Offset(-12.dp.toPx(), h * 0.35f), Offset(-6.dp.toPx(), h * 0.5f), strokeWidth = 2.dp.toPx())

                            // Right horizontal wing bracket
                            drawLine(cyan, Offset(w + 2.dp.toPx(), h * 0.5f), Offset(w + 12.dp.toPx(), h * 0.5f), strokeWidth = 3.dp.toPx())
                            drawLine(cyan, Offset(w + 6.dp.toPx(), h * 0.5f), Offset(w + 12.dp.toPx(), h * 0.35f), strokeWidth = 2.dp.toPx())
                        }

                        // Outer ring with glowing neon cyan border
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(14.dp, CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                                .clip(CircleShape)
                                .background(Color(0xFF021B36))
                                .border(2.5.dp, Color(0xFF00E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner glossy blue circle
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0284C7),
                                                Color(0xFF034A7B),
                                                Color(0xFF021B36)
                                            )
                                        )
                                    )
                                    .border(1.2.dp, Color(0xFF80D8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Paused",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STAGE CLEAR / LEVEL COMPLETE DIALOG & LEVEL 5000 CONGRATULATIONS
        if (engine.isStageClear) {
            val isFinalLevel = engine.stageNumber >= 5000

            val stageInfo = StageData.getStage(engine.stageNumber)
            val starsEarned = when {
                engine.turnsTaken <= stageInfo.targetTurnsFor3Stars -> 3
                engine.turnsTaken <= stageInfo.targetTurnsFor2Stars -> 2
                else -> 1
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1526)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (isFinalLevel) Color(0xFFFFD700) else Color(0xFF00E5FF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isFinalLevel) {
                            Text(
                                text = "🏆 CONGRATULATIONS! 🏆",
                                color = Color(0xFFFFD700),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "YOU CONQUERED ALL 5,000 LEVELS!",
                                color = Color(0xFF00E5FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Grand Final Score: ${NumberFormat.getNumberInstance(Locale.US).format(engine.score)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = "Total Level 5,000 Master",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onSelectStages,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("stage_map_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color(0xFF030712)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("VIEW ALL CHAPTERS", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { engine.restart() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("replay_5000_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("REPLAY LEVEL 5000", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        } else {
                            Text(
                                text = "LEVEL ${engine.stageNumber} COMPLETED!",
                                color = Color(0xFFFFD600),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (s in 1..3) {
                                    val has = s <= starsEarned
                                    Icon(
                                        imageVector = if (has) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = if (has) Color(0xFFFFD600) else Color(0xFF334155),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Score: ${NumberFormat.getNumberInstance(Locale.US).format(engine.score)}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Turns taken: ${engine.turnsTaken}",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )

                            Text(
                                text = "+${50 * starsEarned} Bonus Diamonds! 💎",
                                color = Color(0xFFFFD600),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // 1. NEXT LEVEL (Glossy 3D Capsule GO button)
                            GlossyArcadeCapsuleButton(
                                text = "NEXT LEVEL",
                                style = GlossyButtonStyle.GO,
                                onClick = { engine.nextStage() },
                                height = 56.dp,
                                fontSize = 20.sp,
                                testTag = "next_stage_button",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. LEVEL SELECT
                            OutlinedButton(
                                onClick = onSelectStages,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("stage_map_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Apps, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LEVEL SELECT", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // GAME OVER DIALOG
        if (engine.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1526)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF1744))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GAME OVER",
                            color = Color(0xFFFF1744),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Final Score: ${NumberFormat.getNumberInstance(Locale.US).format(engine.score)}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { engine.restart() },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("retry_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF030712)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("TRY AGAIN", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("gameover_menu_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("MAIN MENU", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. Compact LEVEL 1 Capsule:
 * - Small pill with smooth rounded corners matching reference image
 * - Neon electric cyan to purple-magenta gradient border
 * - Radiant glowing cyan diamond jewel emblem with top/bottom accent notches
 * - Uppercase bold "LEVEL" typography
 * - Dedicated magenta circular chip plate enclosing the stage number
 */
@Composable
private fun CyberHudLevelBadge(
    mode: GameMode,
    stageNumber: Int,
    totalBalls: Int,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(
                elevation = 8.dp,
                shape = pillShape,
                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                spotColor = Color(0xFFD500F9).copy(alpha = 0.6f)
            )
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B24),
                        Color(0xFF030514),
                        Color(0xFF01020A)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF0066FF),
                        Color(0xFF9C27B0),
                        Color(0xFFFF0090)
                    )
                ),
                shape = pillShape
            )
            .padding(start = 5.dp, end = 5.dp)
            .testTag("level_capsule"),
        contentAlignment = Alignment.Center
    ) {
        // Specular upper glass sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 16f
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Radiant Cyan Diamond Emblem with top/bottom accent notches
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.42f

                    // Subtle cyan top/bottom notch brackets matching screenshot
                    val notchW = 4.dp.toPx()
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(cx - notchW / 2, 0.5f),
                        end = Offset(cx + notchW / 2, 0.5f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(cx - notchW / 2, size.height - 0.5f),
                        end = Offset(cx + notchW / 2, size.height - 0.5f),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Diamond Path
                    val diamondPath = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }

                    // Ambient cyan glow
                    drawPath(
                        path = diamondPath,
                        color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                        style = Stroke(width = 2.2.dp.toPx())
                    )

                    // Gradient fill
                    drawPath(
                        path = diamondPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE0F7FA),
                                Color(0xFF00E5FF),
                                Color(0xFF0052CC)
                            ),
                            center = Offset(cx, cy),
                            radius = r
                        )
                    )

                    // Crystal facet highlights
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 0.9.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 0.9.dp.toPx())
                    drawPath(diamondPath, color = Color.White, style = Stroke(width = 0.8.dp.toPx()))
                }
            }

            if (mode == GameMode.QUEST) {
                // "LEVEL" label
                Text(
                    text = "LEVEL",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                            offset = Offset(0f, 0f),
                            blurRadius = 4f
                        )
                    )
                )

                // Magenta/Purple Stage Number Chip
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2E0045),
                                    Color(0xFF150020)
                                )
                            )
                        )
                        .border(
                            width = 1.3.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFF26E2),
                                    Color(0xFFD500F9)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stageNumber",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "$totalBalls-BALLS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

/**
 * 2. Slim Dynamic Progress Bar Capsule:
 * - Slim horizontal rounded pill matching reference screenshot
 * - Neon blue-cyan to purple-magenta gradient border
 * - Glowing 3D Golden Star Leader on left
 * - Slim dynamic progress groove with dashed indicator ticks that fills as bricks are cleared
 * - Exactly 3 circular milestone star indicators on right
 */
@Composable
private fun CyberHudProgressBar(
    progress: Float,
    turnsTaken: Int,
    stageNumber: Int,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)
    val stageInfo = remember(stageNumber) { StageData.getStage(stageNumber) }
    val earnedStars = when {
        turnsTaken <= stageInfo.targetTurnsFor3Stars -> 3
        turnsTaken <= stageInfo.targetTurnsFor2Stars -> 2
        else -> 1
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hud_progress"
    )

    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(
                elevation = 8.dp,
                shape = pillShape,
                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
                spotColor = Color(0xFF7C4DFF).copy(alpha = 0.5f)
            )
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050E26),
                        Color(0xFF020614),
                        Color(0xFF01020A)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF0066FF),
                        Color(0xFF8800FF),
                        Color(0xFFE040FB)
                    )
                ),
                shape = pillShape
            )
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        // Upper glass highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 16f
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Glowing 3D Golden Star Leader
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(18.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD600).copy(alpha = 0.45f), Color.Transparent)
                        ),
                        radius = size.minDimension / 1.4f
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Slim Center Progress Groove with Dynamic Cyan Plasma Fill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF021022))
                    .border(0.8.dp, Color(0xFF052B52), RoundedCornerShape(3.dp))
            ) {
                // Background dashed tick marks matching reference image
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tickCount = 9
                    val step = size.width / (tickCount + 1)
                    val tickW = 3.dp.toPx()
                    val tickH = 1.5.dp.toPx()
                    val cy = size.height / 2f
                    for (i in 1..tickCount) {
                        val cx = i * step
                        drawRoundRect(
                            color = Color(0xFF084175).copy(alpha = 0.7f),
                            topLeft = Offset(cx - tickW / 2, cy - tickH / 2),
                            size = Size(tickW, tickH),
                            cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
                        )
                    }
                }

                // Dynamic filled cyan plasma bar
                if (animatedProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0077FE),
                                        Color(0xFF00E5FF),
                                        Color(0xFF80D8FF)
                                    )
                                )
                            )
                    )

                    // Glowing tip beacon
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .shadow(4.dp, CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                        )
                    }
                }
            }

            // Exactly 3 Circular Milestone Star Indicators (Neatly Aligned)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val isEarned = idx < earnedStars
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEarned) Color(0xFF423300) else Color(0xFF0C061C)
                            )
                            .border(
                                width = 1.1.dp,
                                color = if (isEarned) Color(0xFFFFD600) else Color(0xFF281845),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isEarned) Color(0xFFFFD600) else Color(0xFF4F3A70),
                            modifier = Modifier.size(9.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Compact Coin Counter Capsule:
 * - Small rounded pill with golden neon border matching reference screenshot
 * - 3D embossed gold coin icon
 * - Shows actual coin count in bright electric cyan/white typography
 */
@Composable
private fun CyberHudCoinBadge(
    coins: Int,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(
                elevation = 8.dp,
                shape = pillShape,
                ambientColor = Color(0xFFFFD600).copy(alpha = 0.5f),
                spotColor = Color(0xFFFF9100).copy(alpha = 0.6f)
            )
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF241804),
                        Color(0xFF100A01),
                        Color(0xFF060400)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFEA00),
                        Color(0xFFFF9100)
                    )
                ),
                shape = pillShape
            )
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        // Upper glass highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 16f
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            // 3D Embossed Gold Coin Icon
            Canvas(modifier = Modifier.size(17.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f - 1.dp.toPx()

                // Coin ambient amber glow
                drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = 0.35f),
                    radius = r + 1.dp.toPx()
                )

                // Outer coin body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF176),
                            Color(0xFFFFD600),
                            Color(0xFFFF8F00),
                            Color(0xFFE65100)
                        ),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r
                )

                // Inner embossed ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r * 0.72f,
                    style = Stroke(width = 0.9.dp.toPx())
                )

                // Center diamond / coin star
                val diamondPath = Path().apply {
                    val cr = r * 0.38f
                    moveTo(cx, cy - cr)
                    lineTo(cx + cr, cy)
                    lineTo(cx, cy + cr)
                    lineTo(cx - cr, cy)
                    close()
                }
                drawPath(diamondPath, color = Color.White.copy(alpha = 0.95f))
            }

            // Actual Coin Count Number (Crisp Cyan matching reference screenshot)
            Text(
                text = "$coins",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Black,
                fontSize = 12.5.sp,
                letterSpacing = 0.5.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        offset = Offset(0f, 0f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

/**
 * 4. Small Circular Glowing Pause Button:
 * - Circular 32x32dp button matching reference screenshot
 * - Neon blue-cyan to magenta-purple outer circular ring with 4 subtle tick notches
 * - Deep dark sapphire interior
 * - Glowing dual vertical cyan pause bars
 */
@Composable
private fun CyberHudPauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pause_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                spotColor = Color(0xFFD500F9).copy(alpha = 0.6f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF003D73),
                        Color(0xFF021B3B),
                        Color(0xFF010A1A)
                    )
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFD500F9),
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFF00E5FF)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("pause_button")
    ) {
        // Holographic segmented circular guide with 4 tick marks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 2.5.dp.toPx()
            val c = center
            // 4 Cyber tick notches
            listOf(0f, 90f, 180f, 270f).forEach { deg ->
                val rad = Math.toRadians(deg.toDouble())
                val p1 = Offset(
                    (c.x + cos(rad) * (r - 2.dp.toPx())).toFloat(),
                    (c.y + sin(rad) * (r - 2.dp.toPx())).toFloat()
                )
                val p2 = Offset(
                    (c.x + cos(rad) * (r + 1.5.dp.toPx())).toFloat(),
                    (c.y + sin(rad) * (r + 1.5.dp.toPx())).toFloat()
                )
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

        // Two vertical glowing cyan pause bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(1.6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE0F7FA),
                                    Color(0xFF00E5FF)
                                )
                            )
                        )
                )
            }
        }
    }
}
