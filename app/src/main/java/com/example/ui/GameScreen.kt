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
            .background(Color(0xFF030712))
    ) {
        // High-resolution deep space cosmic nebula background matching reference image
        Image(
            painter = painterResource(id = R.drawable.cosmic_arcade_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dynamic drifting asteroids and cosmic star dust
        CosmicSpaceBackground(modifier = Modifier.fillMaxSize())

        // Subtle dark space vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x35000000),
                            Color.Transparent,
                            Color(0x55000000)
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
            // 2. TOP HUD (ULTRA-STYLISH CYBERNETIC ARCADE DESIGN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Ultra-Stylish Cyber Level Badge
                CyberHudLevelBadge(
                    mode = mode,
                    stageNumber = engine.stageNumber,
                    totalBalls = engine.totalBalls
                )

                // 2. Futuristic Cosmic Star Energy Gauge
                CyberHudProgressBar(
                    progress = engine.levelProgress,
                    turnsTaken = engine.turnsTaken,
                    modifier = Modifier.weight(1f)
                )

                // 3. Cyber Gold Score Badge
                CyberHudScoreBadge(
                    score = engine.score.toLong()
                )

                // 4. Holographic Cyber Pause Orb (فریز / پاز والا بٹن)
                CyberHudPauseButton(
                    onClick = {
                        isPaused = true
                        engine.isPaused = true
                    }
                )
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

                    // DRAW 8 COLUMNS OF NEON BRICKS (STYLISH, GLOSSY 3D JEWEL TILES)
                    val brickGap = 3.dp.toPx()
                    val bCornerR = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                    val bCornerPx = 7.dp.toPx()

                    for (brick in engine.bricks) {
                        if (!brick.isAlive) continue

                        val bX = engine.playLeft + brick.col * engine.cellWidth + brickGap / 2f
                        val bY = engine.playTop + brick.row * engine.cellHeight + brickGap / 2f
                        val bW = engine.cellWidth - brickGap
                        val bH = engine.cellHeight - brickGap

                        val centerX = bX + bW / 2f
                        val centerY = bY + bH / 2f
                        val tones = brick.tones()
                        val bColor = tones.glow

                        // 1. 3D Base Drop Shadow for Physical Depth
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(bX, bY + 2.dp.toPx()),
                            size = Size(bW, bH),
                            cornerRadius = bCornerR
                        )

                        // 2. Rich Multi-Tone 3D Gradient Jewel Body
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(tones.top, tones.mid, tones.bot),
                                startY = bY,
                                endY = bY + bH
                            ),
                            topLeft = Offset(bX, bY),
                            size = Size(bW, bH),
                            cornerRadius = bCornerR
                        )

                        // 3. Delicate Top Specular Shine (Keeps tile center clear and vibrant)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                startY = bY + 1.dp.toPx(),
                                endY = bY + bH * 0.28f
                            ),
                            topLeft = Offset(bX + 1.5.dp.toPx(), bY + 1.5.dp.toPx()),
                            size = Size(bW - 3.dp.toPx(), bH * 0.26f),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )

                        // 4. 3D Specular Highlight Bevel (Top & Left Edges)
                        val highlightPath = Path().apply {
                            moveTo(bX, bY + bH - bCornerPx)
                            lineTo(bX, bY + bCornerPx)
                            quadraticTo(bX, bY, bX + bCornerPx, bY)
                            lineTo(bX + bW - bCornerPx, bY)
                        }
                        drawPath(
                            path = highlightPath,
                            color = Color.White.copy(alpha = 0.50f),
                            style = Stroke(width = 1.4.dp.toPx())
                        )

                        // 3D Shadow Bevel (Bottom & Right Edges)
                        val shadowPath = Path().apply {
                            moveTo(bX + bW, bY + bCornerPx)
                            lineTo(bX + bW, bY + bH - bCornerPx)
                            quadraticTo(bX + bW, bY + bH, bX + bW - bCornerPx, bY + bH)
                            lineTo(bX + bCornerPx, bY + bH)
                        }
                        drawPath(
                            path = shadowPath,
                            color = Color.Black.copy(alpha = 0.38f),
                            style = Stroke(width = 1.4.dp.toPx())
                        )

                        // 5. Crisp Outer Jewel Border
                        drawRoundRect(
                            color = bColor.copy(alpha = 0.75f),
                            topLeft = Offset(bX, bY),
                            size = Size(bW, bH),
                            cornerRadius = bCornerR,
                            style = Stroke(width = 1.3.dp.toPx())
                        )

                        // 6. Render Brick Icon / Number (Ultra-crisp, beautiful, modern typography)
                        when (brick.type) {
                            BrickType.ADD_BALL -> {
                                val circleR = (bH * 0.28f).coerceAtMost(bW * 0.28f)
                                drawCircle(
                                    color = Color(0xFF00E676),
                                    radius = circleR,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF00E676).copy(alpha = 0.30f),
                                    radius = circleR,
                                    center = Offset(centerX, centerY)
                                )
                                val plusSize = (bH * 0.44f).coerceAtMost(bW * 0.44f)
                                numberOutlinePaint.textSize = plusSize
                                numberPaint.textSize = plusSize
                                val textY = centerY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("+", centerX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("+", centerX, textY, numberPaint)
                            }
                            BrickType.BOMB -> {
                                val bombR = (bH * 0.26f).coerceAtMost(bW * 0.26f)
                                val bombCenter = Offset(centerX, centerY + 2.dp.toPx())
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF546E7A), Color(0xFF263238), Color(0xFF10171A)),
                                        center = Offset(bombCenter.x - bombR * 0.35f, bombCenter.y - bombR * 0.35f),
                                        radius = bombR * 1.2f
                                    ),
                                    radius = bombR,
                                    center = bombCenter
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = bombR * 0.22f,
                                    center = Offset(bombCenter.x - bombR * 0.32f, bombCenter.y - bombR * 0.32f)
                                )
                                drawRect(
                                    color = Color(0xFF37474F),
                                    topLeft = Offset(centerX - 2.5.dp.toPx(), bombCenter.y - bombR - 2.5.dp.toPx()),
                                    size = Size(5.dp.toPx(), 3.5.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFFFFD600),
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(centerX + 3.dp.toPx(), bombCenter.y - bombR - 5.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFFFF3D00).copy(alpha = 0.6f),
                                    radius = 5.dp.toPx(),
                                    center = Offset(centerX + 3.dp.toPx(), bombCenter.y - bombR - 5.dp.toPx())
                                )
                            }
                            BrickType.LASER_HORIZONTAL -> {
                                val symSize = (bH * 0.36f).coerceAtMost(bW * 0.36f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = centerY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("◀▶", centerX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("◀▶", centerX, textY, numberPaint)
                            }
                            BrickType.LASER_VERTICAL -> {
                                val symSize = (bH * 0.36f).coerceAtMost(bW * 0.36f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = centerY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("▲▼", centerX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("▲▼", centerX, textY, numberPaint)
                            }
                            BrickType.SPLITTER -> {
                                val symSize = (bH * 0.38f).coerceAtMost(bW * 0.38f)
                                numberOutlinePaint.textSize = symSize
                                numberPaint.textSize = symSize
                                val textY = centerY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)
                                nativeCanvas.drawText("✕2", centerX, textY, numberOutlinePaint)
                                nativeCanvas.drawText("✕2", centerX, textY, numberPaint)
                            }
                            BrickType.NORMAL -> {
                                val numStr = "${brick.currentHp}"
                                // Beautiful, clear geometric typography with comfortable padding
                                val maxAllowedWidth = bW * 0.65f
                                var targetTextSize = bH * 0.44f
                                numberPaint.textSize = targetTextSize
                                val measuredW = numberPaint.measureText(numStr)
                                if (measuredW > maxAllowedWidth) {
                                    targetTextSize *= (maxAllowedWidth / measuredW)
                                    numberPaint.textSize = targetTextSize
                                }
                                numberOutlinePaint.textSize = targetTextSize
                                val textY = centerY - ((numberPaint.descent() + numberPaint.ascent()) / 2f)

                                // 1. Sharp dark contour/beveled outline for razor-sharp separation on any color
                                nativeCanvas.drawText(numStr, centerX, textY, numberOutlinePaint)

                                // 2. Brilliant pure white number fill
                                nativeCanvas.drawText(numStr, centerX, textY, numberPaint)
                            }
                        }
                    }

                    // DRAW LASER BEAMS & LIGHTNING
                    for (laser in engine.laserEffects) {
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

                    // DRAW SHOCKWAVES
                    for (sw in engine.shockwaves) {
                        drawCircle(
                            color = sw.color.copy(alpha = sw.alpha),
                            radius = sw.currentRadius,
                            center = Offset(sw.x, sw.y),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // DRAW PARTICLES & SQUARE COSMIC DEBRIS (Matching the reference shatter FX)
                    for (p in engine.particles) {
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

                    // DRAW FLOATING TEXTS
                    ftPaint.textSize = 17.sp.toPx()
                    for (ft in engine.floatingTexts) {
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

                    // DRAW ACTIVE BALLS & MOTION TRAILS (INCLUDING RETURNED BALLS SLIDING TO BASE)
                    for (ball in engine.balls) {
                        if (ball.isActive) {
                            for ((tIdx, trailPoint) in ball.trail.withIndex()) {
                                val trailAlpha = (tIdx.toFloat() / ball.trail.size) * 0.45f
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

                    // Recall button (appears during shooting)
                    AnimatedVisibility(
                        visible = engine.isShooting,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = { engine.recallBalls() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF007F),
                                contentColor = Color.White
                            ),
                            shape = CutCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recall_button")
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Recall", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("RECALL", fontSize = 11.sp, fontWeight = FontWeight.Black)
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

                            // 1. NEXT LEVEL
                            Button(
                                onClick = { engine.nextStage() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("next_stage_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF030712)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF030712))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NEXT LEVEL (LEVEL ${engine.stageNumber + 1})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }

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
 * Ultra-Stylish Cyber Level Badge:
 * - Curved cyber capsule with dark cosmic sapphire interior
 * - Gradient neon border (Magenta -> Cyan -> Violet)
 * - Upper glossy glass reflection highlight
 * - Radiant 3D faceted neon crystal diamond emblem
 * - 3D crystal metallic stage number typography
 */
@Composable
private fun CyberHudLevelBadge(
    mode: GameMode,
    stageNumber: Int,
    totalBalls: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFD500F9), spotColor = Color(0xFF00E5FF))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF240A42),
                        Color(0xFF13092C),
                        Color(0xFF070B1F)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF26E2),
                        Color(0xFF00E5FF),
                        Color(0xFFB388FF)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .testTag("level_capsule")
    ) {
        // Upper glass highlight sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 22f
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Glowing 3D Diamond Jewel Emblem
            Canvas(modifier = Modifier.size(13.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val w = size.width
                val h = size.height

                val diamondPath = Path().apply {
                    moveTo(cx, 0f)
                    lineTo(w, cy)
                    lineTo(cx, h)
                    lineTo(0f, cy)
                    close()
                }

                // Outer neon glow
                drawPath(diamondPath, color = Color(0xFF00E5FF).copy(alpha = 0.45f), style = Stroke(width = 2.5.dp.toPx()))

                // Diamond jewel gradient fill
                drawPath(
                    path = diamondPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFF00E5FF), Color(0xFF0051CB)),
                        center = Offset(cx, cy),
                        radius = size.width
                    )
                )

                // Facet highlights
                drawLine(Color.White.copy(alpha = 0.85f), Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, cy), Offset(w, cy), strokeWidth = 1.dp.toPx())
                drawPath(diamondPath, color = Color.White, style = Stroke(width = 1.2.dp.toPx()))
            }

            if (mode == GameMode.QUEST) {
                Text(
                    text = "LEVEL",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                            offset = Offset(0f, 0f),
                            blurRadius = 6f
                        )
                    )
                )

                // 3D Crystal Stage Number
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$stageNumber",
                        color = Color.Black.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        modifier = Modifier.offset(y = 1.8.dp)
                    )
                    Text(
                        text = "$stageNumber",
                        color = Color(0xFF7C4DFF),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        modifier = Modifier.offset(y = 0.9.dp)
                    )
                    Text(
                        text = "$stageNumber",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF00E5FF),
                                offset = Offset(0f, 0f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
            } else {
                Text(
                    text = "$totalBalls-BALLS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * Futuristic Cosmic Star Energy Gauge:
 * - Sleek stadium capsule with glowing cyan-blue-violet neon frame
 * - Inset plasma power track with pulsing energy beacon at tip
 * - Radiant 3D Gold Star Leader with glowing amber aura
 * - 3 Sculpted jewel milestone nodes that ignite into brilliant golden crystals
 */
@Composable
private fun CyberHudProgressBar(
    progress: Float,
    turnsTaken: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse"
    )

    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071938),
                        Color(0xFF030D20),
                        Color(0xFF020712)
                    )
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                        Color(0xFF0091EA),
                        Color(0xFF7C4DFF).copy(alpha = pulseAlpha)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Upper glass highlight sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 18f
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Radiant 3D Gold Star Leader with amber aura
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD600).copy(alpha = 0.5f), Color.Transparent)
                        ),
                        radius = size.minDimension / 1.5f
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(16.dp)
                )
            }

            // High-Tech Glowing Energy Track
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF061427))
                    .border(0.8.dp, Color(0xFF0D3256), RoundedCornerShape(5.dp))
            ) {
                val clampedProgress = progress.coerceIn(0.08f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clampedProgress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00B0FF),
                                    Color(0xFF00E5FF),
                                    Color(0xFF69F0AE)
                                )
                            )
                        )
                )

                // Pulsing energy beacon at head of bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clampedProgress),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Three Star Milestone Nodes (3D faceted gold gems)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val earnedStars = when {
                    turnsTaken <= 12 -> 3
                    turnsTaken <= 20 -> 2
                    else -> 1
                }
                repeat(3) { idx ->
                    val isEarned = idx < earnedStars
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEarned) Color(0xFF4A3B00) else Color(0xFF081C30)
                            )
                            .border(
                                1.dp,
                                if (isEarned) Color(0xFFFFD600) else Color(0xFF133454),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isEarned) Color(0xFFFFD600) else Color(0xFF1C456E),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cyber Gold Score Badge:
 * - Dedicated sleek HUD pill capsule
 * - Gold/amber gradient border and dark sapphire backdrop
 * - Mini glowing diamond and bold 3D metallic crystal score digits
 */
@Composable
private fun CyberHudScoreBadge(
    score: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = Color(0xFFFFD600), spotColor = Color(0xFFFFAB00))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF261D07),
                        Color(0xFF140F03),
                        Color(0xFF0A0701)
                    )
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFD600),
                        Color(0xFFFF9100),
                        Color(0xFFFFD600).copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        // Upper glass highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Little sparkling score diamond
            Canvas(modifier = Modifier.size(9.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val p = Path().apply {
                    moveTo(cx, 0f)
                    lineTo(size.width, cy)
                    lineTo(cx, size.height)
                    lineTo(0f, cy)
                    close()
                }
                drawPath(p, color = Color(0xFFFFD600))
            }

            // 3D Crystal score digits
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$score",
                    color = Color.Black.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.offset(y = 1.5.dp)
                )
                Text(
                    text = "$score",
                    color = Color(0xFFFF8F00),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.offset(y = 0.8.dp)
                )
                Text(
                    text = "$score",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFFD600),
                            offset = Offset(0f, 0f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }
    }
}

/**
 * Holographic Cyber Pause Orb (فریز / پاز والا بٹن):
 * - Radiant circular cyber orb with glowing neon ambient aura
 * - Metallic multi-tone sweep border (Cyan -> Violet -> Cyan)
 * - Holographic crosshair reticle with 4 cyber ticks
 * - Tactile spring press animation on tap
 * - Two modern 3D rounded crystal pause bars with frosted cyan highlights
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
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(16.dp, CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFFD500F9))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF005A9C),
                        Color(0xFF02325E),
                        Color(0xFF031630),
                        Color(0xFF010814)
                    )
                )
            )
            .border(
                width = 2.4.dp,
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
        // Holographic segmented crosshair reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 4.dp.toPx()
            // Inner circular guide
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                radius = r,
                style = Stroke(width = 1.2.dp.toPx())
            )
            // 4 Cyber tick notches
            val c = center
            listOf(0f, 90f, 180f, 270f).forEach { deg ->
                val rad = Math.toRadians(deg.toDouble())
                val p1 = Offset((c.x + cos(rad) * (r - 3.5.dp.toPx())).toFloat(), (c.y + sin(rad) * (r - 3.5.dp.toPx())).toFloat())
                val p2 = Offset((c.x + cos(rad) * (r + 3.5.dp.toPx())).toFloat(), (c.y + sin(rad) * (r + 3.dp.toPx())).toFloat())
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = p1,
                    end = p2,
                    strokeWidth = 2.2.dp.toPx()
                )
            }
        }

        // Upper glass highlight
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(18.dp)
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Two stylish 3D crystal pause bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(4.5.dp)
                        .height(17.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE0F7FA),
                                    Color(0xFF80DEEA)
                                )
                            )
                        )
                        .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.5.dp))
                )
            }
        }
    }
}
