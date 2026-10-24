package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Quit Game dialog reproducing the user's high-tech glowing design.
 * Features:
 * - Octagonal / cut-corner sci-fi card with cyan-to-magenta neon gradient border
 * - Exact text: "Would you like to quit the game?"
 * - [X] Cancel button
 * - [MORE GAME] Green neon button
 * - [O] Confirm exit button
 */
@Composable
fun QuitGameDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
    onMoreGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            }
            .testTag("quit_game_dialog"),
        contentAlignment = Alignment.Center
    ) {
        val dialogShape = RoundedCornerShape(24.dp)
        val innerShape = RoundedCornerShape(20.dp)

        val neonBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00E5FF), // Cyan on left / bottom
                Color(0xFF00B0FF),
                Color(0xFF7C4DFF),
                Color(0xFFD500F9), // Magenta on top / right
                Color(0xFFFF1744)
            ),
            start = Offset(0f, 600f),
            end = Offset(600f, 0f)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .padding(16.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = dialogShape,
                    ambientColor = Color(0xFF00E5FF),
                    spotColor = Color(0xFFD500F9)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    /* Consume tap on dialog body so it doesn't dismiss */
                },
            shape = dialogShape,
            color = Color(0xFF040A18),
            border = BorderStroke(3.dp, neonBorderBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF081326),
                                Color(0xFF040A16),
                                Color(0xFF02060D)
                            )
                        )
                    )
                    .padding(2.dp)
            ) {
                // Inner accent border for layered futuristic cockpit appearance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(innerShape)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), innerShape)
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title: "Would you like to quit the game?"
                        Text(
                            text = "Would you like to\nquit the game?",
                            color = Color(0xFFEDF8FF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Three buttons row: [ X ]  [ MORE GAME ]  [ O ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. X BUTTON (Cancel)
                            Surface(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF031024),
                                border = BorderStroke(2.2.dp, Color(0xFF00E5FF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = Color(0xFF00E5FF),
                                        spotColor = Color(0xFF00E5FF)
                                    )
                                    .testTag("quit_dialog_cancel_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            // 2. MORE GAME BUTTON
                            Surface(
                                onClick = onMoreGames,
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF02240E),
                                border = BorderStroke(2.2.dp, Color(0xFF00E676)),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(64.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = Color(0xFF00E676),
                                        spotColor = Color(0xFF00E676)
                                    )
                                    .testTag("quit_dialog_more_game_button")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "MORE",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        lineHeight = 16.sp
                                    )
                                    Text(
                                        text = "GAME",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // 3. O BUTTON (Confirm Exit)
                            Surface(
                                onClick = onConfirmExit,
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF031024),
                                border = BorderStroke(2.2.dp, Color(0xFF00E5FF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = Color(0xFF00E5FF),
                                        spotColor = Color(0xFF00E5FF)
                                    )
                                    .testTag("quit_dialog_confirm_exit_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Canvas(modifier = Modifier.size(28.dp)) {
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension / 2f - 2.dp.toPx(),
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
