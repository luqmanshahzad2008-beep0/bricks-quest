package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

// Mountain Model for 3D Circular Orbit
private data class MountainSpec(
    val baseWidth: Float,
    val baseHeight: Float,
    val mainPath: Path,
    val ridgePath: Path,
    val fissurePath: Path,
    val gradientColors: List<Color>,
    val rimColor: Color,
    val energyColor: Color
)

private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float
)

private data class OrbitingCrystal(
    val orbitPhase: Float,
    val orbitRadiusX: Float,
    val orbitRadiusY: Float,
    val size: Float,
    val rotSpeed: Float,
    val color: Color
)

@Composable
fun Purple3DMountainBackground(
    modifier: Modifier = Modifier
) {
    // 1. Continuous Smooth 3D Circular Rotation (24 seconds per complete revolution)
    val infiniteTransition = rememberInfiniteTransition(label = "mountain_3d_loop")
    val orbitProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitProgress"
    )

    // Glow pulse animation for energy veins & cosmic nebulae
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // 2. Remember Normalized Mountain 3D Meshes (Created once, transformed on GPU)
    val mountainSpecs = remember {
        listOf(
            // Peak A: Majestic Twin Amethyst Spire
            createMountainSpec(
                width = 380f,
                height = 240f,
                peakPoints = listOf(
                    0.0f to 1.0f,
                    0.15f to 0.62f,
                    0.35f to 0.12f, // Left high needle peak
                    0.48f to 0.42f, // Saddle
                    0.68f to 0.04f, // Highest central spire
                    0.82f to 0.48f,
                    1.0f to 1.0f
                ),
                ridgePoints = listOf(
                    0.35f to 0.12f, 0.42f to 0.55f, 0.48f to 0.85f,
                    0.68f to 0.04f, 0.62f to 0.45f, 0.58f to 0.95f
                ),
                fissures = listOf(
                    0.68f to 0.04f, 0.72f to 0.28f, 0.69f to 0.52f, 0.74f to 0.78f
                ),
                topColor = Color(0xFF6A1B9A),
                bottomColor = Color(0xFF1A0033),
                rimColor = Color(0xFFE040FB),
                energyColor = Color(0xFF00E5FF)
            ),
            // Peak B: Grand Razor Citadel
            createMountainSpec(
                width = 420f,
                height = 270f,
                peakPoints = listOf(
                    0.0f to 1.0f,
                    0.18f to 0.54f,
                    0.30f to 0.32f,
                    0.52f to 0.02f, // Dominant apex
                    0.65f to 0.38f,
                    0.84f to 0.22f, // Secondary jagged horn
                    1.0f to 1.0f
                ),
                ridgePoints = listOf(
                    0.52f to 0.02f, 0.48f to 0.42f, 0.45f to 0.95f,
                    0.84f to 0.22f, 0.78f to 0.60f
                ),
                fissures = listOf(
                    0.52f to 0.02f, 0.56f to 0.26f, 0.51f to 0.58f, 0.57f to 0.88f
                ),
                topColor = Color(0xFF8E24AA),
                bottomColor = Color(0xFF130026),
                rimColor = Color(0xFFD500F9),
                energyColor = Color(0xFFB388FF)
            ),
            // Peak C: Sharp Cosmic Ridge Needle
            createMountainSpec(
                width = 340f,
                height = 230f,
                peakPoints = listOf(
                    0.0f to 1.0f,
                    0.22f to 0.48f,
                    0.44f to 0.08f, // Ultra-sharp Needle
                    0.58f to 0.38f,
                    0.76f to 0.18f,
                    0.90f to 0.55f,
                    1.0f to 1.0f
                ),
                ridgePoints = listOf(
                    0.44f to 0.08f, 0.50f to 0.52f, 0.52f to 0.95f
                ),
                fissures = listOf(
                    0.44f to 0.08f, 0.38f to 0.35f, 0.43f to 0.65f
                ),
                topColor = Color(0xFF7B1FA2),
                bottomColor = Color(0xFF17022C),
                rimColor = Color(0xFF00E5FF),
                energyColor = Color(0xFFFF4081)
            ),
            // Peak D: Broad Fortress Crag
            createMountainSpec(
                width = 440f,
                height = 250f,
                peakPoints = listOf(
                    0.0f to 1.0f,
                    0.12f to 0.58f,
                    0.28f to 0.24f,
                    0.46f to 0.10f,
                    0.64f to 0.06f,
                    0.78f to 0.32f,
                    1.0f to 1.0f
                ),
                ridgePoints = listOf(
                    0.46f to 0.10f, 0.42f to 0.50f, 0.38f to 0.90f,
                    0.64f to 0.06f, 0.68f to 0.48f, 0.72f to 0.95f
                ),
                fissures = listOf(
                    0.64f to 0.06f, 0.60f to 0.32f, 0.66f to 0.64f
                ),
                topColor = Color(0xFF512DA8),
                bottomColor = Color(0xFF0F001F),
                rimColor = Color(0xFFE040FB),
                energyColor = Color(0xFF69F0AE)
            )
        )
    }

    // 3. Stardust & Cosmic Particles
    val stars = remember {
        List(60) {
            StarParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 2.2f + 1f,
                phase = Random.nextFloat() * (2f * PI.toFloat()),
                speed = Random.nextFloat() * 0.8f + 0.4f
            )
        }
    }

    // 4. Orbiting Glowing Crystal Shards
    val crystals = remember {
        List(14) { index ->
            OrbitingCrystal(
                orbitPhase = (index.toFloat() / 14f) * (2f * PI.toFloat()),
                orbitRadiusX = Random.nextFloat() * 120f + 260f,
                orbitRadiusY = Random.nextFloat() * 180f + 380f,
                size = Random.nextFloat() * 10f + 6f,
                rotSpeed = Random.nextFloat() * 140f + 60f,
                color = when (index % 4) {
                    0 -> Color(0xFFE040FB)
                    1 -> Color(0xFF00E5FF)
                    2 -> Color(0xFFB388FF)
                    else -> Color(0xFFFF4081)
                }
            )
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h * 0.52f

        // A. DEEP SPACE / ROYAL PURPLE COSMIC BACKGROUND
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF070010), // Midnight space void
                    Color(0xFF140228), // Deep violet
                    Color(0xFF260548), // Royal purple core
                    Color(0xFF380962), // Radiant amethyst glow
                    Color(0xFF1D0332), // Dark valley purple
                    Color(0xFF0A0014)  // Obsidian base
                ),
                startY = 0f,
                endY = h
            ),
            size = size
        )

        // B. RADIANT PULSING PURPLE & CYAN NEBULAE
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD500F9).copy(alpha = 0.22f * pulseGlow),
                    Color(0xFF7B1FA2).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY - h * 0.12f),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(centerX, centerY - h * 0.12f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.14f * pulseGlow),
                    Color.Transparent
                ),
                center = Offset(centerX - w * 0.28f, centerY + h * 0.18f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(centerX - w * 0.28f, centerY + h * 0.18f)
        )

        // C. TWINKLING COSMIC STARDUST
        stars.forEach { star ->
            val starAlpha = (sin(orbitProgress * 2f * PI.toFloat() * star.speed + star.phase) * 0.35f + 0.65f)
                .coerceIn(0.2f, 1f)
            val px = star.xRatio * w
            val py = star.yRatio * h
            drawCircle(
                color = Color(0xFFE040FB).copy(alpha = 0.45f * starAlpha * pulseGlow),
                radius = star.radius * 1.8f,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * starAlpha),
                radius = star.radius * 0.8f,
                center = Offset(px, py)
            )
        }

        // D. 3D CONTINUOUS REVOLVING MOUNTAINS (Top to Bottom Circular 3D Orbit)
        // 5 Mountain Nodes positioned in a full 360-degree cylindrical orbit
        // As they rotate:
        // - Top-to-Bottom vertical traversal (smooth sinusoidal Y oscillation)
        // - 3D depth scale: large & crisp at bottom (front), smaller & misty at top (back)
        // - Z-sorting ensures distant peaks are drawn behind nearer peaks
        val numMountains = 5
        val orbitRadiusY = h * 0.38f // Ample vertical range covering top to bottom
        val orbitRadiusX = w * 0.22f // Gentle horizontal elliptical tilt

        // Compute 3D state for each orbiting mountain
        val mountainStates = (0 until numMountains).map { i ->
            val phaseOffset = (i.toFloat() / numMountains) * (2f * PI.toFloat())
            val currentAngle = (orbitProgress * 2f * PI.toFloat()) + phaseOffset

            // 3D coordinates on an inclined circular cylinder
            val sinA = sin(currentAngle)
            val cosA = cos(currentAngle)

            // Circular motion from top to bottom:
            // sinA = -1 -> Top of orbit (distant)
            // sinA = +1 -> Bottom of orbit (near, full scale)
            val posX = centerX + (cosA * orbitRadiusX)
            val posY = centerY + (sinA * orbitRadiusY)

            // Depth calculation: depthFactor goes from 0.0 (top/back) to 1.0 (bottom/front)
            val depthFactor = ((sinA + 1f) / 2f).coerceIn(0f, 1f)

            // Perspective scale: 0.58x in distance (top) to 1.15x in front (bottom)
            val scale = 0.58f + (depthFactor * 0.57f)

            // Opacity: softer & hazier in the back, intense & vibrant in the front
            val alpha = 0.38f + (depthFactor * 0.58f)

            // Subtle 3D tilt banking angle
            val tiltAngle = -cosA * 6.5f

            val spec = mountainSpecs[i % mountainSpecs.size]

            Mountain3DState(
                spec = spec,
                x = posX,
                y = posY,
                scale = scale,
                alpha = alpha,
                tiltAngle = tiltAngle,
                depth = depthFactor,
                isFront = sinA > 0f
            )
        }

        // Sort by depth (back mountains first, front mountains last for true 3D occlusion)
        val sortedMountains = mountainStates.sortedBy { it.depth }

        sortedMountains.forEach { m ->
            withTransform({
                // Apply 3D position, dynamic scale, and perspective tilt
                translate(left = m.x, top = m.y)
                rotate(degrees = m.tiltAngle, pivot = Offset.Zero)
                scale(scaleX = m.scale, scaleY = m.scale, pivot = Offset.Zero)
            }) {
                val originOffset = Offset(-m.spec.baseWidth / 2f, -m.spec.baseHeight)

                // 1. Soft Outer Neon Glow Aura (Only prominent for front mountains)
                if (m.depth > 0.4f) {
                    drawPath(
                        path = m.spec.mainPath,
                        color = m.spec.rimColor.copy(alpha = 0.25f * m.alpha * pulseGlow),
                        style = Stroke(width = 12.dp.toPx() * m.scale)
                    )
                }

                // 2. Mountain Mass Fill with Rich Purple-To-Black Celestial Gradient
                drawPath(
                    path = m.spec.mainPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            m.spec.gradientColors[0].copy(alpha = m.alpha),
                            m.spec.gradientColors[1].copy(alpha = m.alpha * 0.95f),
                            Color(0xFF080012).copy(alpha = m.alpha)
                        ),
                        startY = 0f,
                        endY = m.spec.baseHeight
                    )
                )

                // 3. Shaded Rocky Facets / Ridges
                drawPath(
                    path = m.spec.ridgePath,
                    color = Color.Black.copy(alpha = 0.35f * m.alpha),
                    style = Stroke(width = 3.5.dp.toPx() * m.scale)
                )

                // 4. Glowing Neon Energy Fissures / Crystal Veins
                drawPath(
                    path = m.spec.fissurePath,
                    color = m.spec.energyColor.copy(alpha = 0.85f * m.alpha * pulseGlow),
                    style = Stroke(width = 2.dp.toPx() * m.scale)
                )

                // 5. Razor-Sharp Neon Rim Contour
                drawPath(
                    path = m.spec.mainPath,
                    color = m.spec.rimColor.copy(alpha = 0.85f * m.alpha * pulseGlow),
                    style = Stroke(width = 2.2.dp.toPx() * m.scale)
                )

                // 6. Glowing Peak Spire Star
                val peakGlowRadius = 6.dp.toPx() * m.scale
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * m.alpha),
                    radius = peakGlowRadius * 0.6f,
                    center = Offset(m.spec.baseWidth * 0.48f, 0f)
                )
                drawCircle(
                    color = m.spec.energyColor.copy(alpha = 0.7f * m.alpha * pulseGlow),
                    radius = peakGlowRadius * 1.6f,
                    center = Offset(m.spec.baseWidth * 0.48f, 0f)
                )
            }
        }

        // E. ORBITING GLOWING CRYSTAL SHARDS & RUNIC EMBERS
        crystals.forEach { crystal ->
            val angle = (orbitProgress * 2f * PI.toFloat()) + crystal.orbitPhase
            val cx = centerX + cos(angle) * crystal.orbitRadiusX
            val cy = centerY + sin(angle) * crystal.orbitRadiusY
            val depth = (sin(angle) + 1f) / 2f
            val cScale = 0.6f + (depth * 0.6f)
            val cAlpha = (0.4f + depth * 0.6f) * pulseGlow

            withTransform({
                translate(left = cx, top = cy)
                rotate(degrees = orbitProgress * crystal.rotSpeed, pivot = Offset.Zero)
                scale(scaleX = cScale, scaleY = cScale, pivot = Offset.Zero)
            }) {
                // Diamond Crystal
                val crystalPath = Path().apply {
                    moveTo(0f, -crystal.size)
                    lineTo(crystal.size * 0.65f, 0f)
                    lineTo(0f, crystal.size)
                    lineTo(-crystal.size * 0.65f, 0f)
                    close()
                }
                drawPath(
                    path = crystalPath,
                    color = crystal.color.copy(alpha = cAlpha)
                )
                drawPath(
                    path = crystalPath,
                    color = Color.White.copy(alpha = cAlpha * 0.8f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // F. ATMOSPHERIC BOTTOM VALLEY HAZE & SOFT PURPLE FOG
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x334A0E78),
                    Color(0x7720053D),
                    Color(0xDD0C0118)
                ),
                startY = h * 0.68f,
                endY = h
            ),
            size = size
        )
    }
}

private data class Mountain3DState(
    val spec: MountainSpec,
    val x: Float,
    val y: Float,
    val scale: Float,
    val alpha: Float,
    val tiltAngle: Float,
    val depth: Float,
    val isFront: Boolean
)

private fun createMountainSpec(
    width: Float,
    height: Float,
    peakPoints: List<Pair<Float, Float>>,
    ridgePoints: List<Pair<Float, Float>>,
    fissures: List<Pair<Float, Float>>,
    topColor: Color,
    bottomColor: Color,
    rimColor: Color,
    energyColor: Color
): MountainSpec {
    val mainPath = Path().apply {
        if (peakPoints.isNotEmpty()) {
            moveTo(peakPoints[0].first * width, peakPoints[0].second * height)
            for (i in 1 until peakPoints.size) {
                lineTo(peakPoints[i].first * width, peakPoints[i].second * height)
            }
            close()
        }
    }

    val ridgePath = Path().apply {
        if (ridgePoints.size >= 2) {
            moveTo(ridgePoints[0].first * width, ridgePoints[0].second * height)
            for (i in 1 until ridgePoints.size) {
                lineTo(ridgePoints[i].first * width, ridgePoints[i].second * height)
            }
        }
    }

    val fissurePath = Path().apply {
        if (fissures.size >= 2) {
            moveTo(fissures[0].first * width, fissures[0].second * height)
            for (i in 1 until fissures.size) {
                lineTo(fissures[i].first * width, fissures[i].second * height)
            }
        }
    }

    return MountainSpec(
        baseWidth = width,
        baseHeight = height,
        mainPath = mainPath,
        ridgePath = ridgePath,
        fissurePath = fissurePath,
        gradientColors = listOf(topColor, bottomColor),
        rimColor = rimColor,
        energyColor = energyColor
    )
}
