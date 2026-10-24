package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed

@Composable
fun BricksLogoHeader(
    modifier: Modifier = Modifier,
    topGemsCount: Int = 30,
    onTopGemsClick: () -> Unit = {},
    onPowerClick: () -> Unit = {}
) {
    val letters = listOf(
        Pair("B", Color(0xFF00B4D8)), // Cyan
        Pair("R", Color(0xFF22C55E)), // Emerald Green
        Pair("I", Color(0xFFF59E0B)), // Amber Gold with Crown
        Pair("C", Color(0xFFA855F7)), // Royal Purple
        Pair("K", Color(0xFFEF4444)), // Crimson Red
        Pair("S", Color(0xFF00E5FF))  // Electric Cyan
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TOP-LEFT: [ 💎 x30  (+) ] (Exact match to image.png)
        Surface(
            onClick = onTopGemsClick,
            color = Color(0xD9061528),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier.testTag("top_gems_pill")
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 3.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💎", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "x$topGemsCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Glowing Green (+) Button inside the pill
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF059669))
                            )
                        )
                        .border(1.dp, Color(0xFF34D399), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // CENTER: 3D BRICKS BREAKER QUEST LOGO WITH CROWN & METALLIC WINGS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            // Golden Crown & Letter Blocks
            Box(contentAlignment = Alignment.TopCenter) {
                // Majestic 3-Pointed Golden Crown with Jewel Accents
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(y = (-8).dp)
                ) {
                    Text(text = "✨", fontSize = 10.sp, color = Color(0xFFFFE600))
                    Text(
                        text = "👑",
                        fontSize = 20.sp,
                        modifier = Modifier.shadow(8.dp, CircleShape, ambientColor = Color(0xFFFFD700), spotColor = Color(0xFFFFD700))
                    )
                    Text(text = "✨", fontSize = 10.sp, color = Color(0xFFFFE600))
                }

                // 6 3D Jelly/Gem Block Tiles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    letters.forEach { (char, baseColor) ->
                        Box(
                            modifier = Modifier
                                .size(width = 33.dp, height = 37.dp)
                                .shadow(6.dp, RoundedCornerShape(8.dp), ambientColor = baseColor, spotColor = baseColor)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            baseColor.copy(alpha = 0.95f),
                                            baseColor,
                                            baseColor.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Top glass highlight
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.48f)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.65f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        )
                                    )
                            )

                            // 3D Shadowed Letter
                            Text(
                                text = char,
                                color = Color(0x77000000),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.offset(x = 1.dp, y = 1.5.dp)
                            )
                            Text(
                                text = char,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle: ═ BREAKER QUEST ═ with Metallic Wings Plaque
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Wing Stripes
                Canvas(modifier = Modifier.size(width = 24.dp, height = 8.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, h * 0.3f),
                        end = Offset(w, h * 0.3f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(w * 0.3f, h * 0.75f),
                        end = Offset(w, h * 0.75f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "BREAKER QUEST",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Right Wing Stripes
                Canvas(modifier = Modifier.size(width = 24.dp, height = 8.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, h * 0.3f),
                        end = Offset(w, h * 0.3f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, h * 0.75f),
                        end = Offset(w * 0.7f, h * 0.75f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // TOP-RIGHT: GLOWING CIRCULAR POWER BUTTON (⏻) (Exact match to image.png)
        IconButton(
            onClick = onPowerClick,
            modifier = Modifier
                .size(40.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color(0xFFFF1744), spotColor = Color(0xFFFF1744))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF881337), Color(0xFF4C0519), Color(0xFF030712))
                    )
                )
                .border(2.dp, Color(0xFFFF2A6D), CircleShape)
                .testTag("power_exit_button")
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power Settings",
                tint = Color(0xFFFF1744),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
