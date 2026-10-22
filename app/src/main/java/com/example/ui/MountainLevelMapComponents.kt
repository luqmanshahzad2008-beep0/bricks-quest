package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random

// Floating space rock / mountain asteroid data (پتھر)
data class FloatingRock(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val baseColor: Color,
    val highlightColor: Color,
    val rimColor: Color,
    val rockPath: Path
)

/**
 * Majestic Purple Nebula Cosmic Mountain Backdrop ("پرپل پرپل بیک گراؤنڈ اندر پتھر")
 * Features:
 * - Rich deep purple / violet cosmic nebula sky with glowing magenta dust clouds.
 * - Layered majestic purple mountains with radiant neon crests and snowcaps.
 * - Dynamic drifting space rocks / mountain asteroids (پتھر) with crystal veins.
 * - Passing sci-fi spaceships gliding across the purple sky with cyan/magenta thruster plumes.
 */
@Composable
fun MountainSkylineBackdrop(
    modifier: Modifier = Modifier
) {
    // Sparkling cosmic stars
    val stars = remember {
        List(65) {
            Triple(
                Random.nextFloat(), // x
                Random.nextFloat(), // y
                Random.nextFloat() * 2.2f + 1f // size
            )
        }
    }

    // Procedural floating purple-toned rocks / asteroids (پتھر)
    val floatingRocks = remember {
        val list = mutableListOf<FloatingRock>()
        val rockBases = listOf(
            Color(0xFF3B1259), // Deep Amethyst Slate
            Color(0xFF280B42), // Dark Obsidian Purple
            Color(0xFF4A196D), // Royal Violet Rock
            Color(0xFF32134F), // Shadow Granite
            Color(0xFF431862)  // Nebula Stone
        )
        val rockHighlights = listOf(
            Color(0xFF8E44AD),
            Color(0xFFA569BD),
            Color(0xFF9B59B6),
            Color(0xFFC39BD3),
            Color(0xFFD2B4DE)
        )
        val rimGlows = listOf(
            Color(0xFFD500F9), // Neon Magenta
            Color(0xFFE040FB), // Bright Purple
            Color(0xFF00E5FF), // Cyan Crystal
            Color(0xFFFF80AB), // Pink Quartz
            Color(0xFFB388FF)  // Lavender Glow
        )

        for (i in 0 until 10) {
            val radius = Random.nextFloat() * 24f + 14f
            val vertices = Random.nextInt(7, 11)
            val path = Path()
            for (v in 0 until vertices) {
                val angle = (v.toFloat() / vertices) * 2f * PI.toFloat()
                val r = radius * (0.75f + Random.nextFloat() * 0.45f)
                val px = cos(angle) * r
                val py = sin(angle) * r
                if (v == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            list.add(
                FloatingRock(
                    x = Random.nextFloat() * 1080f,
                    y = Random.nextFloat() * 1920f,
                    speedX = (Random.nextFloat() * 18f - 9f),
                    speedY = Random.nextFloat() * 24f + 10f,
                    size = radius,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() * 24f - 12f),
                    baseColor = rockBases[i % rockBases.size],
                    highlightColor = rockHighlights[i % rockHighlights.size],
                    rimColor = rimGlows[i % rimGlows.size],
                    rockPath = path
                )
            )
        }
        list
    }

    // Animation transition for continuous celestial motion
    val infiniteTransition = rememberInfiniteTransition(label = "purple_cosmic_anim")
    val nebulaPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaPulse"
    )

    val shipX1 by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 1250f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shipX1"
    )

    val shipX2 by infiniteTransition.animateFloat(
        initialValue = 1250f,
        targetValue = -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shipX2"
    )

    // Continuous smooth animation tick for moving rocks
    var animTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                // Update drifting rocks positions
                for (rock in floatingRocks) {
                    rock.x += rock.speedX * dt
                    rock.y += rock.speedY * dt
                    rock.rotation += rock.rotSpeed * dt

                    // Screen wrap
                    if (rock.y > 2100f) {
                        rock.y = -60f
                        rock.x = Random.nextFloat() * 1080f
                    }
                    if (rock.x > 1150f) rock.x = -60f
                    if (rock.x < -80f) rock.x = 1130f
                }
                animTick = now
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val tick = animTick
        val w = size.width
        val h = size.height

        // 1. Lush Deep Purple / Violet Cosmic Sky Gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F021D), // Darkest Cosmic Violet
                    Color(0xFF22063B), // Rich Royal Purple
                    Color(0xFF380A59), // Luminous Purple Nebula
                    Color(0xFF25063F), // Deep Amethyst
                    Color(0xFF0B0117)  // Midnight Abyss
                )
            ),
            size = size
        )

        // 2. Radiant Purple & Magenta Nebula Clouds
        // Upper-right purple nebula glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD500F9).copy(alpha = nebulaPulse * 0.5f),
                    Color(0xFF8E24AA).copy(alpha = nebulaPulse * 0.25f),
                    Color.Transparent
                ),
                center = Offset(w * 0.82f, h * 0.22f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.82f, h * 0.22f)
        )

        // Center-left vibrant violet nebula bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF9C27B0).copy(alpha = nebulaPulse * 0.45f),
                    Color(0xFF6A1B9A).copy(alpha = nebulaPulse * 0.2f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.55f),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.18f, h * 0.55f)
        )

        // Lower cyan-purple cosmic dust reflection
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.15f),
                    Color(0xFF4A148C).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.85f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * 0.85f)
        )

        // 3. Twinkling Purple and White Stars
        stars.forEach { (rx, ry, s) ->
            val sx = rx * w
            val sy = ry * h
            val isPurpleStar = (rx * 10).toInt() % 3 == 0
            drawCircle(
                color = if (isPurpleStar) Color(0xFFE1BEE7) else Color.White.copy(alpha = 0.75f),
                radius = s,
                center = Offset(sx, sy)
            )
            // Star flare for larger stars
            if (s > 2.2f) {
                drawCircle(
                    color = Color(0xFFD500F9).copy(alpha = 0.35f),
                    radius = s * 2.5f,
                    center = Offset(sx, sy)
                )
            }
        }

        // 4. Passing Spaceships in the high purple sky (جہاز)
        // Cruiser 1: Left to right with glowing cyan/blue engine plume
        val s1X = (shipX1 / 1000f) * w
        val s1Y = h * 0.16f + sin(shipX1 * 0.006f) * 18f
        // Thruster glow plume
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00E5FF), Color.White, Color.Transparent),
                startX = s1X,
                endX = s1X - 45f
            ),
            topLeft = Offset(s1X - 45f, s1Y - 5f),
            size = Size(45f, 10f)
        )
        // Ship Hull
        val s1Path = Path().apply {
            moveTo(s1X + 20f, s1Y)
            lineTo(s1X - 12f, s1Y - 11f)
            lineTo(s1X - 7f, s1Y)
            lineTo(s1X - 12f, s1Y + 11f)
            close()
        }
        drawPath(s1Path, color = Color(0xFF1E0A36), style = Fill)
        drawPath(s1Path, color = Color(0xFF00E5FF), style = Stroke(width = 1.8f))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(s1X + 5f, s1Y))

        // Cruiser 2: Right to left with luminous magenta/purple trail
        val s2X = (shipX2 / 1000f) * w
        val s2Y = h * 0.36f + cos(shipX2 * 0.005f) * 15f
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFD500F9), Color(0xFFFF80AB), Color.Transparent),
                startX = s2X,
                endX = s2X + 45f
            ),
            topLeft = Offset(s2X, s2Y - 5f),
            size = Size(45f, 10f)
        )
        val s2Path = Path().apply {
            moveTo(s2X - 20f, s2Y)
            lineTo(s2X + 12f, s2Y - 10f)
            lineTo(s2X + 7f, s2Y)
            lineTo(s2X + 12f, s2Y + 10f)
            close()
        }
        drawPath(s2Path, color = Color(0xFF280B4A), style = Fill)
        drawPath(s2Path, color = Color(0xFFE040FB), style = Stroke(width = 1.8f))

        // 5. Moving Rocks / Asteroids with Crystal Veins (اندر پتھر ا رہے ہوں)
        for (rock in floatingRocks) {
            val posX = (rock.x / 1080f) * w
            val posY = (rock.y / 1920f) * h

            rotate(degrees = rock.rotation, pivot = Offset(posX, posY)) {
                drawContext.canvas.save()
                drawContext.canvas.translate(posX, posY)

                // Outer Glowing Purple Halo
                drawCircle(
                    color = rock.rimColor.copy(alpha = 0.22f),
                    radius = rock.size * 1.5f,
                    center = Offset.Zero
                )

                // Rock Body Fill
                drawPath(
                    path = rock.rockPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            rock.highlightColor,
                            rock.baseColor,
                            Color(0xFF10021C)
                        ),
                        center = Offset(-rock.size * 0.3f, -rock.size * 0.3f),
                        radius = rock.size * 1.4f
                    ),
                    style = Fill
                )

                // Neon Purple/Magenta Rim Edge
                drawPath(
                    path = rock.rockPath,
                    color = rock.rimColor,
                    style = Stroke(width = 2.2f)
                )

                // Internal Crater
                drawCircle(
                    color = Color(0xFF120324),
                    radius = rock.size * 0.24f,
                    center = Offset(rock.size * 0.18f, rock.size * 0.14f)
                )
                drawCircle(
                    color = rock.rimColor.copy(alpha = 0.6f),
                    radius = rock.size * 0.24f,
                    center = Offset(rock.size * 0.18f, rock.size * 0.14f),
                    style = Stroke(width = 1f)
                )

                // Glowing Crystal Mineral Vein across the stone
                val vein = Path().apply {
                    moveTo(-rock.size * 0.25f, rock.size * 0.28f)
                    lineTo(0f, 0f)
                    lineTo(rock.size * 0.35f, -rock.size * 0.12f)
                }
                drawPath(
                    path = vein,
                    color = rock.rimColor.copy(alpha = 0.9f),
                    style = Stroke(width = 1.6f)
                )

                drawContext.canvas.restore()
            }
        }

        // 6. Distant Majestic Mountain Peaks Layer (Royal Violet / Magenta Snow Crests)
        val farPath = Path().apply {
            moveTo(0f, h * 0.46f)
            lineTo(w * 0.15f, h * 0.30f)
            lineTo(w * 0.30f, h * 0.42f)
            lineTo(w * 0.50f, h * 0.24f) // Grand Summit Peak
            lineTo(w * 0.70f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.27f)
            lineTo(w, h * 0.36f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = farPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF260842).copy(alpha = 0.95f),
                    Color(0xFF140324)
                ),
                startY = h * 0.24f,
                endY = h
            )
        )
        // Radiant Magenta / Pink glowing snow crest
        drawPath(
            path = farPath,
            color = Color(0xFFE040FB).copy(alpha = 0.55f),
            style = Stroke(width = 2.4f)
        )

        // 7. Midground Craggy Mountain Layer (Deep Purple Slate)
        val midPath = Path().apply {
            moveTo(0f, h * 0.66f)
            lineTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.62f)
            lineTo(w * 0.64f, h * 0.47f)
            lineTo(w * 0.84f, h * 0.58f)
            lineTo(w, h * 0.51f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF340D57).copy(alpha = 0.98f),
                    Color(0xFF19042B)
                ),
                startY = h * 0.47f,
                endY = h
            )
        )
        // Neon Purple & Cyan glowing crest
        drawPath(
            path = midPath,
            color = Color(0xFFD500F9).copy(alpha = 0.65f),
            style = Stroke(width = 2.8f)
        )

        // 8. Soft Rolling Lavender Valley Mist
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF9C27B0).copy(alpha = 0.18f),
                    Color(0xFF1B042E).copy(alpha = 0.45f)
                ),
                startY = h * 0.62f,
                endY = h
            ),
            size = size
        )

        // 9. Foreground Craggy Cliffs Framing Left & Right Corners
        val leftCliff = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.11f, h * 0.18f)
            lineTo(w * 0.07f, h * 0.44f)
            lineTo(w * 0.13f, h * 0.68f)
            lineTo(w * 0.09f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = leftCliff,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF1D0533), Color(0xFF1D0533).copy(alpha = 0.25f)),
                startX = 0f,
                endX = w * 0.13f
            )
        )
        drawPath(leftCliff, color = Color(0xFFE040FB).copy(alpha = 0.4f), style = Stroke(width = 1.6f))

        val rightCliff = Path().apply {
            moveTo(w, 0f)
            lineTo(w * 0.89f, h * 0.22f)
            lineTo(w * 0.93f, h * 0.48f)
            lineTo(w * 0.87f, h * 0.72f)
            lineTo(w * 0.91f, h)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = rightCliff,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF1D0533).copy(alpha = 0.25f), Color(0xFF1D0533)),
                startX = w * 0.87f,
                endX = w
            )
        )
        drawPath(rightCliff, color = Color(0xFF00E5FF).copy(alpha = 0.4f), style = Stroke(width = 1.6f))
    }
}

/**
 * A Single Mountain Trail Step Row in the Winding Mountain Ascent
 * Places the level node at the LEFT corner (if level is odd) or RIGHT corner (if level is even).
 * On the opposite corner, renders a decorative floating purple crystal rock / mountain geode!
 * Draws an S-curve paved road with neon purple curbs and cyan center energy beam.
 */
@Composable
fun MountainLevelStepRow(
    levelNum: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    pulseGlow: Float,
    isChapterStart: Boolean,
    isChapterEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLeft = (levelNum % 2 != 0) // Alternating corners: Left -> Right -> Left -> Right!

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        // 1. Curving Mountain Road & Opposite-Corner Decorative Floating Rock
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val nodeX = if (isLeft) w * 0.22f else w * 0.78f
            val nodeY = h * 0.50f

            // Decorative Floating Purple Crystal Stone on the OPPOSITE corner
            val oppX = if (isLeft) w * 0.80f else w * 0.20f
            val oppY = h * 0.50f
            val geodeRadius = 14.dp.toPx()

            // Floating stone halo
            drawCircle(
                color = Color(0xFFD500F9).copy(alpha = 0.22f),
                radius = geodeRadius * 1.6f,
                center = Offset(oppX, oppY)
            )
            // Faceted crystal stone body
            val rockGeode = Path().apply {
                moveTo(oppX, oppY - geodeRadius)
                lineTo(oppX + geodeRadius * 0.9f, oppY - geodeRadius * 0.3f)
                lineTo(oppX + geodeRadius * 0.7f, oppY + geodeRadius * 0.8f)
                lineTo(oppX - geodeRadius * 0.2f, oppY + geodeRadius)
                lineTo(oppX - geodeRadius * 0.85f, oppY + geodeRadius * 0.4f)
                lineTo(oppX - geodeRadius * 0.7f, oppY - geodeRadius * 0.6f)
                close()
            }
            drawPath(
                path = rockGeode,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9C27B0),
                        Color(0xFF4A148C),
                        Color(0xFF1A052C)
                    ),
                    center = Offset(oppX - geodeRadius * 0.3f, oppY - geodeRadius * 0.3f),
                    radius = geodeRadius * 1.5f
                ),
                style = Fill
            )
            // Luminous neon purple rim
            drawPath(
                path = rockGeode,
                color = Color(0xFFE040FB).copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx())
            )
            // Crystal facet lines inside geode
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                start = Offset(oppX - geodeRadius * 0.3f, oppY - geodeRadius * 0.3f),
                end = Offset(oppX + geodeRadius * 0.4f, oppY + geodeRadius * 0.3f),
                strokeWidth = 1.4f
            )

            // Inward trail from bottom center
            if (!isChapterStart) {
                val startX = w * 0.50f
                val startY = h
                val ctrlX = if (isLeft) w * 0.30f else w * 0.70f
                val ctrlY = h * 0.82f

                val inPath = Path().apply {
                    moveTo(startX, startY)
                    quadraticTo(ctrlX, ctrlY, nodeX, nodeY)
                }

                // Road underlay (deep obsidian stone)
                drawPath(
                    path = inPath,
                    color = Color(0xFF160429).copy(alpha = 0.92f),
                    style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round)
                )
                // Road embankment border (vibrant purple glow)
                drawPath(
                    path = inPath,
                    color = Color(0xFFD500F9).copy(alpha = if (isUnlocked) 0.55f else 0.25f),
                    style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
                )
                // Glowing center energy trail (electric cyan)
                drawPath(
                    path = inPath,
                    color = Color(0xFF00E5FF).copy(alpha = if (isUnlocked) 0.75f else 0.3f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // Energy stepping stone orbs along bottom half
                for (s in 1..2) {
                    val t = s * 0.33f
                    val u = 1f - t
                    val ox = u * u * startX + 2 * u * t * ctrlX + t * t * nodeX
                    val oy = u * u * startY + 2 * u * t * ctrlY + t * t * nodeY
                    drawCircle(
                        color = Color(0xFFD500F9).copy(alpha = if (isUnlocked) 0.85f else 0.3f),
                        radius = 4.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.6.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                }
            }

            // Outward trail to top center
            if (!isChapterEnd) {
                val endX = w * 0.50f
                val endY = 0f
                val ctrlX = if (isLeft) w * 0.30f else w * 0.70f
                val ctrlY = h * 0.18f

                val outPath = Path().apply {
                    moveTo(nodeX, nodeY)
                    quadraticTo(ctrlX, ctrlY, endX, endY)
                }

                // Road underlay (deep obsidian stone)
                drawPath(
                    path = outPath,
                    color = Color(0xFF160429).copy(alpha = 0.92f),
                    style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round)
                )
                // Road embankment border (vibrant purple glow)
                drawPath(
                    path = outPath,
                    color = Color(0xFFD500F9).copy(alpha = if (isUnlocked) 0.55f else 0.25f),
                    style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
                )
                // Glowing center energy trail (electric cyan)
                drawPath(
                    path = outPath,
                    color = Color(0xFF00E5FF).copy(alpha = if (isUnlocked) 0.75f else 0.3f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // Energy stepping stone orbs along top half
                for (s in 1..2) {
                    val t = s * 0.33f
                    val u = 1f - t
                    val ox = u * u * nodeX + 2 * u * t * ctrlX + t * t * endX
                    val oy = u * u * nodeY + 2 * u * t * ctrlY + t * t * endY
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = if (isUnlocked) 0.85f else 0.3f),
                        radius = 4.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.6.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                }
            }
        }

        // 2. The Mountain Level Node (Positioned at Left corner or Right corner)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isLeft) 22.dp else 0.dp,
                    end = if (!isLeft) 22.dp else 0.dp
                ),
            contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            MountainLevelNode(
                levelNum = levelNum,
                isUnlocked = isUnlocked,
                isCurrent = isCurrent,
                stars = stars,
                pulseGlow = pulseGlow,
                onClick = onClick
            )
        }
    }
}

/**
 * Stylish Mountain Waypoint Level Node
 * A floating chiseled purple stone pedestal with glowing neon rim,
 * level number, stars, and summit pin for the active level.
 */
@Composable
fun MountainLevelNode(
    levelNum: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    pulseGlow: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMilestone = (levelNum % 5 == 0) // Summit Milestone every 5 levels!

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating Active Beacon / Mountain Flag for Current Level
        if (isCurrent) {
            Surface(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .offset(y = (-4).dp)
                    .padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚩", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SUMMIT",
                        color = Color(0xFF1A052C),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else if (isMilestone) {
            // Milestone Peak Crest Badge (🏰 / 💎)
            Surface(
                color = Color(0xFF3B0764),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE040FB)),
                shadowElevation = 6.dp,
                modifier = Modifier.offset(y = (-2).dp)
            ) {
                Text(
                    text = if (isUnlocked) "★ PEAK" else "🔒 PEAK",
                    color = if (isUnlocked) Color(0xFFFFD700) else Color(0xFFC084FC),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Main Mountain Stone Node Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (isMilestone) 70.dp else 62.dp)
                .shadow(
                    elevation = if (isCurrent) 16.dp else if (isUnlocked) 10.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = if (isCurrent) Color(0xFFD500F9) else if (isMilestone) Color(0xFFE040FB) else Color(0xFF9C27B0),
                    spotColor = if (isCurrent) Color(0xFFFFD700) else Color(0xFF00E5FF)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when {
                            isCurrent -> listOf(Color(0xFF4A148C), Color(0xFF2E0854), Color(0xFF120324))
                            isMilestone && isUnlocked -> listOf(Color(0xFF6A1B9A), Color(0xFF38006B), Color(0xFF150029))
                            isUnlocked -> listOf(Color(0xFF3B1259), Color(0xFF240A3D), Color(0xFF0E021A))
                            else -> listOf(Color(0xFF210D33), Color(0xFF140621), Color(0xFF0A0212))
                        }
                    )
                )
                .border(
                    width = if (isCurrent) 3.2.dp else if (isMilestone) 2.5.dp else 2.2.dp,
                    color = when {
                        isCurrent -> Color(0xFFD500F9).copy(alpha = pulseGlow)
                        isMilestone && isUnlocked -> Color(0xFFE040FB)
                        isUnlocked -> Color(0xFF00E5FF)
                        else -> Color(0xFF6B21A8).copy(alpha = 0.7f)
                    },
                    shape = CircleShape
                )
                .clickable(enabled = isUnlocked, onClick = onClick)
                .testTag("level_button_$levelNum")
        ) {
            // Glass sheen reflection across top arc
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.46f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isUnlocked) 0.38f else 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Center Content: Level Number or Lock Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = "$levelNum",
                        color = if (isCurrent) Color(0xFFFFD700) else Color.White,
                        fontSize = if (levelNum >= 1000) 13.sp else if (isMilestone) 19.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "$levelNum",
                        color = Color(0xFF7E22CE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Golden Stars Rating Beneath Node
        if (isUnlocked) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xE61E0538), RoundedCornerShape(8.dp))
                    .border(0.8.dp, Color(0xFF9C27B0).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                for (s in 1..3) {
                    val earned = s <= stars
                    Icon(
                        imageVector = if (earned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (earned) Color(0xFFFFD700) else Color(0xFF4A148C),
                        modifier = Modifier.size(9.5.dp)
                    )
                }
            }
        }
    }
}
