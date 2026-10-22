package com.example.ui

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Level Map Screen with Live Downward Scrolling Mountains:
 * - Massive purple fantasy mountains continuously slide and cascade downwards from top to bottom in real-time.
 * - Glowing serpentine waterfall trail connecting radiant crystal level nodes (1 to 5000).
 * - Premium 3D crystal badges, golden crowns, and neon lighting.
 */
@Composable
fun LevelMapScreen(
    storage: GameStorage,
    soundManager: SoundManager,
    onStartStage: (Int) -> Unit,
    onBack: () -> Unit = {},
    onStartClassic: () -> Unit = {},
    onStartRush: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    onOpenBallSkins: () -> Unit = {}
) {
    var highestUnlocked by remember { mutableIntStateOf(storage.highestStageUnlocked) }
    var selectedStage by remember { mutableStateOf<Int?>(null) }
    val totalLevels = 5000
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.cascading_mountain_bg)
    }

    var scrollOffset by remember { mutableFloatStateOf(0f) }

    // Continuously animate downward mountain scrolling
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now
                // Scroll downwards smoothly
                scrollOffset = (scrollOffset + 55f * dt)
            }
        }
    }

    LaunchedEffect(Unit) {
        highestUnlocked = storage.highestStageUnlocked.coerceIn(1, totalLevels)
        val targetIndex = (totalLevels - highestUnlocked).coerceIn(0, totalLevels - 1)
        listState.scrollToItem(targetIndex)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cascading_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F021D))
            .testTag("level_map_screen")
    ) {
        // 1. LIVE SEAMLESS MERGED MOUNTAIN & COSMIC NEBULA LOOPING BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep cosmic background base
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF16062A), Color(0xFF0F021D), Color(0xFF06000D))
                )
            )

            if (bitmap != null) {
                val currentOffset = scrollOffset % h
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)

                val dst1 = RectF(0f, currentOffset, w, h + currentOffset)
                val dst2 = RectF(0f, currentOffset - h, w, currentOffset)

                val paint = android.graphics.Paint().apply {
                    alpha = 185
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dst1, paint)
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dst2, paint)

                // Seamless merging cross-fade band across the tile junction
                val fadeHeight = 260.dp.toPx()
                val seamY = currentOffset
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0F021D).copy(alpha = 0.75f),
                            Color(0xFF0F021D).copy(alpha = 0.95f),
                            Color(0xFF0F021D).copy(alpha = 0.75f),
                            Color.Transparent
                        ),
                        startY = seamY - fadeHeight / 2f,
                        endY = seamY + fadeHeight / 2f
                    )
                )
            }

            // Continuous drifting cosmic nebulae that unify the space organically
            val animShift = scrollOffset * 0.4f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFAB47BC).copy(alpha = 0.24f * pulseAlpha), Color.Transparent),
                    radius = w * 0.8f,
                    center = Offset(w * 0.25f, (animShift % h))
                ),
                radius = w * 0.8f,
                center = Offset(w * 0.25f, (animShift % h))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.16f), Color.Transparent),
                    radius = w * 0.7f,
                    center = Offset(w * 0.8f, ((animShift + h * 0.5f) % h))
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.8f, ((animShift + h * 0.5f) % h))
            )

            // Atmospheric cosmic overlay with subtle vignette
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140226).copy(alpha = 0.65f),
                        Color.Transparent,
                        Color(0xFF05000C).copy(alpha = 0.80f)
                    )
                )
            )
        }

        // 2. Vertical Scrolling Levels List with Dynamic Connecting Cosmic Path (5000 to 1)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalLevels) { index ->
                val level = totalLevels - index
                val isUnlocked = level <= highestUnlocked
                val isCurrent = level == highestUnlocked

                CascadingLevelNodeItem(
                    level = level,
                    totalLevels = totalLevels,
                    highestUnlocked = highestUnlocked,
                    isUnlocked = isUnlocked,
                    isCurrent = isCurrent,
                    pulseAlpha = pulseAlpha,
                    onClick = {
                        soundManager.playClickSound()
                        if (isUnlocked) {
                            selectedStage = level
                        }
                    }
                )
            }
        }

        // 3. TOP STYLISH HEADER: 3D BRICKS QUEST LOGO & BACK BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Surface(
                onClick = {
                    soundManager.playClickSound()
                    onBack()
                },
                modifier = Modifier
                    .size(46.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape),
                color = Color(0xFF240A42),
                border = BorderStroke(2.dp, Color(0xFFE040FB))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 3D Logo Badge Center
            Image(
                painter = painterResource(id = R.drawable.bricks_quest_logo),
                contentDescription = "BRICKS QUEST",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(170.dp)
                    .height(68.dp)
            )

            Spacer(modifier = Modifier.size(46.dp))
        }

        // 4. STAGE START DIALOG WITH GLOSSY 3D CAPSULE "GO" BUTTON
        selectedStage?.let { stage ->
            val stageStars = storage.getStageStars(stage)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { selectedStage = null },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 380.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
                            spotColor = Color(0xFFE040FB).copy(alpha = 0.75f)
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7C4DFF),
                                    Color(0xFFE040FB)
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF16062A),
                                    Color(0xFF0D021D),
                                    Color(0xFF070010)
                                )
                            )
                        )
                        .clickable(enabled = false) {}
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header: Stage Title & Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STAGE $stage",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            IconButton(
                                onClick = { selectedStage = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Star ratings (3 stars preview)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (s in 1..3) {
                                val has = s <= stageStars
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (has) Color(0xFFFFD700) else Color(0xFF4A148C),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "60 Starting Balls • Aim & Destroy Bricks",
                            color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // GLOSSY 3D CAPSULE "GO" BUTTON FROM USER REFERENCE IMAGE
                        GlossyArcadeCapsuleButton(
                            text = "GO",
                            style = GlossyButtonStyle.GO,
                            onClick = {
                                soundManager.playClickSound()
                                selectedStage = null
                                onStartStage(stage)
                            },
                            pulseAlpha = pulseAlpha,
                            height = 62.dp,
                            fontSize = 26.sp,
                            testTag = "stage_dialog_go_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CascadingLevelNodeItem(
    level: Int,
    totalLevels: Int,
    highestUnlocked: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val currentBiasDp = getLevelBiasDp(level)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. GLOWING ENERGY PATH SEGMENTS (Drawn inside each item so it scrolls seamlessly with the levels)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val itemHeightPx = size.height
            val centerY = itemHeightPx / 2f

            val currentOffsetPx = currentBiasDp.dp.toPx()
            val prevOffsetPx = if (level < totalLevels) getLevelBiasDp(level + 1).dp.toPx() else currentOffsetPx
            val nextOffsetPx = if (level > 1) getLevelBiasDp(level - 1).dp.toPx() else currentOffsetPx

            val xCurrent = centerX + currentOffsetPx
            val xTop = (xCurrent + (centerX + prevOffsetPx)) / 2f
            val xBottom = (xCurrent + (centerX + nextOffsetPx)) / 2f

            val isLevelUnlocked = level <= highestUnlocked
            val isNextUnlocked = (level - 1) <= highestUnlocked

            // Colors for vibrant cosmic energy trail
            val glowColor = if (isLevelUnlocked) Color(0xFF00E5FF) else Color(0xFF7C4DFF).copy(alpha = 0.35f)
            val beamBrush = if (isLevelUnlocked) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFE040FB),
                        Color(0xFFFFD700)
                    ),
                    startY = 0f,
                    endY = itemHeightPx
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6A1B9A).copy(alpha = 0.5f),
                        Color(0xFF38006B).copy(alpha = 0.35f)
                    ),
                    startY = 0f,
                    endY = itemHeightPx
                )
            }

            // Top segment (connecting from top boundary to node center)
            if (level < totalLevels) {
                val pathTop = Path().apply {
                    moveTo(xTop, 0f)
                    cubicTo(
                        xTop, centerY * 0.45f,
                        xCurrent, centerY * 0.55f,
                        xCurrent, centerY
                    )
                }

                // Wide diffuse outer aura
                drawPath(
                    path = pathTop,
                    color = glowColor.copy(alpha = 0.28f * pulseAlpha),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                // Vibrant neon core beam
                drawPath(
                    path = pathTop,
                    brush = beamBrush,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Center bright laser line
                if (isLevelUnlocked) {
                    drawPath(
                        path = pathTop,
                        color = Color.White.copy(alpha = 0.92f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Glowing energy stardust pearl
                val midTopX = (xTop + xCurrent) / 2f
                val midTopY = centerY * 0.5f
                drawCircle(
                    color = if (isLevelUnlocked) Color(0xFF00E5FF) else Color(0xFF9575CD),
                    radius = 4.5.dp.toPx(),
                    center = Offset(midTopX, midTopY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(midTopX, midTopY)
                )
            }

            // Bottom segment (connecting from node center to bottom boundary)
            if (level > 1) {
                val pathBottom = Path().apply {
                    moveTo(xCurrent, centerY)
                    cubicTo(
                        xCurrent, centerY + (itemHeightPx - centerY) * 0.45f,
                        xBottom, centerY + (itemHeightPx - centerY) * 0.55f,
                        xBottom, itemHeightPx
                    )
                }

                // Wide diffuse outer aura
                drawPath(
                    path = pathBottom,
                    color = glowColor.copy(alpha = 0.28f * pulseAlpha),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                // Vibrant neon core beam
                drawPath(
                    path = pathBottom,
                    brush = beamBrush,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Center bright laser line
                if (isNextUnlocked) {
                    drawPath(
                        path = pathBottom,
                        color = Color.White.copy(alpha = 0.92f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Glowing energy stardust pearl
                val midBottomX = (xCurrent + xBottom) / 2f
                val midBottomY = centerY + (itemHeightPx - centerY) * 0.5f
                drawCircle(
                    color = if (isNextUnlocked) Color(0xFFFFD700) else Color(0xFF9575CD),
                    radius = 4.5.dp.toPx(),
                    center = Offset(midBottomX, midBottomY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(midBottomX, midBottomY)
                )
            }
        }

        // 2. LEVEL NODE (Centered directly on the path at xCurrent)
        Box(
            modifier = Modifier
                .offset(x = currentBiasDp.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCurrent) {
                    Text(
                        text = "👑",
                        fontSize = 26.sp,
                        modifier = Modifier.offset(y = 6.dp)
                    )
                }

                Surface(
                    onClick = onClick,
                    modifier = Modifier
                        .size(if (isCurrent) 84.dp else 70.dp)
                        .shadow(
                            elevation = if (isCurrent) 28.dp else 12.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFE040FB),
                            spotColor = if (isCurrent) Color(0xFFFF26E2) else Color(0xFF00E5FF)
                        )
                        .clip(CircleShape)
                        .border(
                            width = if (isCurrent) 4.5.dp else 2.5.dp,
                            brush = Brush.verticalGradient(
                                colors = if (isUnlocked) {
                                    listOf(
                                        Color(0xFFFF26E2).copy(alpha = if (isCurrent) pulseAlpha else 1f),
                                        Color(0xFF00E5FF),
                                        Color(0xFFFFD700)
                                    )
                                } else {
                                    listOf(Color(0xFF6A1B9A), Color(0xFF38006B))
                                }
                            ),
                            shape = CircleShape
                        )
                        .testTag("level_node_$level"),
                    color = Color.Transparent,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isUnlocked) {
                                        listOf(Color(0xFF4A148C), Color(0xFF22063B), Color(0xFF0F021D))
                                    } else {
                                        listOf(Color(0xFF1E0836), Color(0xFF100220), Color(0xFF070010))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(26.dp)
                                .align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(topStartPercent = 100, topEndPercent = 100))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.32f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        if (isUnlocked) {
                            Text(
                                text = "$level",
                                color = Color.White,
                                fontSize = if (isCurrent) 28.sp else 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFFE1BEE7).copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$level",
                                    color = Color(0xFFE1BEE7).copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getLevelBiasDp(level: Int): Float = when (level % 4) {
    1 -> 85f      // Level 1, 5, 9... (right)
    2 -> -50f     // Level 2, 6, 10... (mid-left)
    3 -> 50f      // Level 3, 7, 11... (mid-right)
    else -> -85f  // Level 4, 8, 12... (left)
}
