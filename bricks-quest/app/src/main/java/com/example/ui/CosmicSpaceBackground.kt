package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

// Dynamic Space Rock / Asteroid (پتھر)
data class Asteroid(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val rockPath: Path,
    val baseColor: Color,
    val highlightColor: Color,
    val rimColor: Color
)

// Dynamic Passing Spaceship (جہاز)
data class SpaceShip(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val scale: Float,
    val angleDegrees: Float,
    val thrusterColor: Color,
    val shipColor: Color,
    val trimColor: Color
)

@Composable
fun CosmicSpaceBackground(
    modifier: Modifier = Modifier
) {
    // Generate static starry sky
    val stars = remember {
        List(70) {
            Triple(
                Random.nextFloat(), // x ratio
                Random.nextFloat(), // y ratio
                Random.nextFloat() * 2.2f + 1f // size
            )
        }
    }

    // Generate procedural stylish asteroids with craggy rocky geometry and mineral veins
    val asteroids = remember {
        val list = mutableListOf<Asteroid>()
        val rockBaseColors = listOf(
            Color(0xFF334155), // Slate Asteroid
            Color(0xFF475569), // Granite Rock
            Color(0xFF3E3054), // Amethyst Geode
            Color(0xFF4A3B2A), // Bronze Ore
            Color(0xFF1E293B), // Dark Obsidian
            Color(0xFF2C3E50)  // Deep Blue Meteor
        )
        val rockHighlights = listOf(
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFF7E57C2),
            Color(0xFFB45309),
            Color(0xFF475569),
            Color(0xFF38BDF8)
        )
        val rimColors = listOf(
            Color(0xFF00E5FF), // Neon Cyan
            Color(0xFFD500F9), // Electric Violet
            Color(0xFFFFD700), // Cosmic Gold
            Color(0xFF00F5D4), // Mint Emerald
            Color(0xFF38BDF8), // Laser Sky
            Color(0xFFFF4081)  // Hot Pink
        )

        for (i in 0 until 12) {
            val radius = Random.nextFloat() * 28f + 18f
            val numVertices = Random.nextInt(7, 11)
            val path = Path()

            for (v in 0 until numVertices) {
                val angle = (v.toFloat() / numVertices) * 2f * PI.toFloat()
                val r = radius * (0.75f + Random.nextFloat() * 0.45f)
                val px = cos(angle) * r
                val py = sin(angle) * r
                if (v == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            list.add(
                Asteroid(
                    x = Random.nextFloat() * 1100f,
                    y = Random.nextFloat() * 2000f,
                    speedX = (Random.nextFloat() * 24f - 12f),
                    speedY = Random.nextFloat() * 32f + 16f,
                    size = radius,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() * 28f - 14f),
                    rockPath = path,
                    baseColor = rockBaseColors[i % rockBaseColors.size],
                    highlightColor = rockHighlights[i % rockHighlights.size],
                    rimColor = rimColors[i % rimColors.size]
                )
            )
        }
        list
    }

    // Dynamic passing spaceships (جہاز) gliding across the cosmos
    val ships = remember {
        mutableStateListOf(
            SpaceShip(
                x = -150f,
                y = 280f,
                speedX = 135f,
                speedY = 45f,
                scale = 1.15f,
                angleDegrees = 18f,
                thrusterColor = Color(0xFF00E5FF),
                shipColor = Color(0xFF1E293B),
                trimColor = Color(0xFF00E5FF)
            ),
            SpaceShip(
                x = 1200f,
                y = 650f,
                speedX = -155f,
                speedY = 35f,
                scale = 1.05f,
                angleDegrees = 167f,
                thrusterColor = Color(0xFFD500F9),
                shipColor = Color(0xFF28113E),
                trimColor = Color(0xFFE040FB)
            ),
            SpaceShip(
                x = -200f,
                y = 1100f,
                speedX = 165f,
                speedY = -40f,
                scale = 0.95f,
                angleDegrees = -14f,
                thrusterColor = Color(0xFFFF9100),
                shipColor = Color(0xFF1B2A4A),
                trimColor = Color(0xFFFFD700)
            ),
            SpaceShip(
                x = 1250f,
                y = 1450f,
                speedX = -120f,
                speedY = -25f,
                scale = 1.1f,
                angleDegrees = 192f,
                thrusterColor = Color(0xFF00E676),
                shipColor = Color(0xFF0F172A),
                trimColor = Color(0xFF00F5D4)
            )
        )
    }

    // Smooth 60fps animation tick
    var animNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                // Update Asteroids position and rotation
                for (rock in asteroids) {
                    rock.x += rock.speedX * dt
                    rock.y += rock.speedY * dt
                    rock.rotation += rock.rotSpeed * dt

                    // Screen wrap
                    if (rock.y > 2200f) {
                        rock.y = -80f
                        rock.x = Random.nextFloat() * 1080f
                    }
                    if (rock.x > 1200f) rock.x = -80f
                    if (rock.x < -100f) rock.x = 1180f
                }

                // Update Spaceships
                for (ship in ships) {
                    ship.x += ship.speedX * dt
                    ship.y += ship.speedY * dt

                    // Wrap / Respawn ships cleanly when they leave screen
                    if (ship.speedX > 0 && ship.x > 1350f) {
                        ship.x = -180f
                        ship.y = Random.nextFloat() * 1400f + 150f
                    } else if (ship.speedX < 0 && ship.x < -220f) {
                        ship.x = 1280f
                        ship.y = Random.nextFloat() * 1400f + 150f
                    }
                }

                animNanos = now
            }
        }
    }

    // Draw cosmic elements
    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val tick = animNanos
        val w = size.width
        val h = size.height

        // 1. Subtle transparent cosmic tint (allows high-res nebula wallpaper to show through)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x20020612),
                    Color.Transparent,
                    Color(0x300C1636),
                    Color(0x30030714)
                )
            ),
            size = size
        )

        // 2. Distant Twinkling Stars
        stars.forEach { (relX, relY, starSize) ->
            val sx = relX * w
            val sy = relY * h
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }

        // 3. Drifting Rocky Asteroids (پتھر)
        for (rock in asteroids) {
            val posX = (rock.x / 1080f) * w
            val posY = (rock.y / 1920f) * h

            rotate(degrees = rock.rotation, pivot = Offset(posX, posY)) {
                drawContext.canvas.save()
                drawContext.canvas.translate(posX, posY)

                // Rock Body Fill with lit specular surface
                drawPath(
                    path = rock.rockPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            rock.highlightColor,
                            rock.baseColor,
                            Color(0xFF050814)
                        ),
                        center = Offset(-rock.size * 0.35f, -rock.size * 0.35f),
                        radius = rock.size * 1.5f
                    ),
                    style = Fill
                )

                // Neon Edge Rim Glow (Reflecting cosmic nebula / stardust)
                drawPath(
                    path = rock.rockPath,
                    color = rock.rimColor.copy(alpha = 0.75f),
                    style = Stroke(width = 2.2f)
                )

                // Interior Craters
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = rock.size * 0.24f,
                    center = Offset(rock.size * 0.18f, rock.size * 0.15f)
                )
                drawCircle(
                    color = rock.rimColor.copy(alpha = 0.5f),
                    radius = rock.size * 0.24f,
                    center = Offset(rock.size * 0.18f, rock.size * 0.15f),
                    style = Stroke(width = 1f)
                )

                // Second smaller crater
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = rock.size * 0.16f,
                    center = Offset(-rock.size * 0.22f, -rock.size * 0.12f)
                )

                // Glowing Crystal Mineral Vein
                val veinPath = Path().apply {
                    moveTo(-rock.size * 0.2f, rock.size * 0.3f)
                    lineTo(0f, 0f)
                    lineTo(rock.size * 0.35f, -rock.size * 0.1f)
                }
                drawPath(
                    path = veinPath,
                    color = rock.rimColor.copy(alpha = 0.85f),
                    style = Stroke(width = 1.4f)
                )

                drawContext.canvas.restore()
            }
        }

        // 4. Passing Sci-Fi Spaceships (جہاز)
        for (ship in ships) {
            val sx = (ship.x / 1080f) * w
            val sy = (ship.y / 1920f) * h

            drawContext.canvas.save()
            drawContext.canvas.translate(sx, sy)
            drawContext.canvas.nativeCanvas.rotate(ship.angleDegrees)

            drawSpaceShip(ship)

            drawContext.canvas.restore()
        }
    }
}

private fun DrawScope.drawSpaceShip(ship: SpaceShip) {
    val s = ship.scale

    // 1. Plasma Engine Thruster Exhaust Plume (Bright glowing flame)
    val flameLength = 65f * s
    val flameWidth = 14f * s
    val flameBrush = Brush.horizontalGradient(
        colors = listOf(
            ship.thrusterColor,
            Color.White,
            ship.thrusterColor.copy(alpha = 0.6f),
            Color.Transparent
        ),
        startX = 0f,
        endX = -flameLength
    )

    // Left Thruster Flame
    drawOval(
        brush = flameBrush,
        topLeft = Offset(-flameLength, -flameWidth * 1.4f),
        size = Size(flameLength, flameWidth)
    )
    // Right Thruster Flame
    drawOval(
        brush = flameBrush,
        topLeft = Offset(-flameLength, flameWidth * 0.4f),
        size = Size(flameLength, flameWidth)
    )

    // 2. Sleek Jet Hull (Delta Interceptor Wings)
    val shipPath = Path().apply {
        moveTo(36f * s, 0f)         // Nose Cone
        lineTo(-24f * s, -22f * s)  // Left Wingtip
        lineTo(-14f * s, -8f * s)   // Left Body Inset
        lineTo(-26f * s, 0f)        // Engine Mount Center
        lineTo(-14f * s, 8f * s)    // Right Body Inset
        lineTo(-24f * s, 22f * s)   // Right Wingtip
        close()
    }

    // Ship Body Fill with metallic sci-fi hull gradient
    drawPath(
        path = shipPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF334155), ship.shipColor, Color(0xFF0A0F1D)),
            start = Offset(30f * s, 0f),
            end = Offset(-24f * s, 0f)
        ),
        style = Fill
    )

    // Bright Glowing Neon Edge Wing Trim
    drawPath(
        path = shipPath,
        color = ship.trimColor,
        style = Stroke(width = 2.4f * s)
    )

    // Cockpit Canopy Glow
    val canopyPath = Path().apply {
        moveTo(18f * s, 0f)
        lineTo(2f * s, -5f * s)
        lineTo(-6f * s, 0f)
        lineTo(2f * s, 5f * s)
        close()
    }
    drawPath(
        path = canopyPath,
        brush = Brush.linearGradient(
            colors = listOf(Color.White, ship.trimColor),
            start = Offset(18f * s, 0f),
            end = Offset(-6f * s, 0f)
        ),
        style = Fill
    )
    drawPath(
        path = canopyPath,
        color = Color.White,
        style = Stroke(width = 1f * s)
    )

    // Dual Forward Wingtip Laser Pods
    drawCircle(
        color = ship.trimColor,
        radius = 3.2f * s,
        center = Offset(-20f * s, -20f * s)
    )
    drawCircle(
        color = Color.White,
        radius = 1.5f * s,
        center = Offset(-20f * s, -20f * s)
    )
    drawCircle(
        color = ship.trimColor,
        radius = 3.2f * s,
        center = Offset(-20f * s, 20f * s)
    )
    drawCircle(
        color = Color.White,
        radius = 1.5f * s,
        center = Offset(-20f * s, 20f * s)
    )
}
