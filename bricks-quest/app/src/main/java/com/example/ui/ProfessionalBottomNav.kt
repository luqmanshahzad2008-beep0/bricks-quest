package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager

enum class BottomTab {
    SHOP, BALL, STAGE, BATTLE, TUTORIALS
}

@Composable
fun ProfessionalBottomNav(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    soundManager: SoundManager,
    modifier: Modifier = Modifier
) {
    val tabs = BottomTab.values()
    val selectedIndex = tabs.indexOf(currentTab)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Base Rounded Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(30.dp))
        )

        // Items Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(26.dp))
                                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(26.dp))
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            soundManager.playClickSound()
                            soundManager.triggerHitHaptic()
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            when(tab) {
                                BottomTab.SHOP -> ShopStorefrontIcon()
                                BottomTab.BALL -> BallIconWithTrail()
                                BottomTab.STAGE -> StageNineDotsIconExact()
                                BottomTab.BATTLE -> MultiplayerSwordsIconExact()
                                BottomTab.TUTORIALS -> TutorialsCapIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.name,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (index < tabs.size - 1) {
                    VerticalDividerLine()
                }
            }
        }
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFF00E5FF).copy(alpha = 0.5f))
    )
}

// Icons (Keep same as before for consistency)
@Composable
private fun ShopStorefrontIcon() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val roofPath = Path().apply {
            moveTo(w * 0.1f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.9f, h * 0.45f)
            close()
        }
        drawPath(roofPath, color = Color(0xFFFF1744), style = Fill)
        drawPath(roofPath, color = Color.White, style = Stroke(0.8.dp.toPx()))
        drawRect(
            color = Color(0xFF00B0FF),
            topLeft = Offset(w * 0.2f, h * 0.45f),
            size = Size(w * 0.6f, h * 0.45f)
        )
    }
}

@Composable
private fun BallIconWithTrail() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val radius = w * 0.3f
        drawCircle(color = Color(0xFFD500F9), radius = radius, center = center)
        drawCircle(color = Color.White, radius = radius * 0.6f, center = center)
    }
}

@Composable
private fun StageNineDotsIconExact() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val boxSize = w * 0.9f
        val gap = boxSize * 0.1f
        val dotSize = (boxSize - gap * 2f) / 3f
        
        for (r in 0..2) {
            for (c in 0..2) {
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(c * (dotSize + gap), r * (dotSize + gap)),
                    size = Size(dotSize, dotSize),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun MultiplayerSwordsIconExact() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawLine(Color.White, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth = 3.dp.toPx())
        drawLine(Color.White, Offset(w * 0.8f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), strokeWidth = 3.dp.toPx())
    }
}

@Composable
private fun TutorialsCapIcon() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val capPath = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.9f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.1f, h * 0.35f)
            close()
        }
        drawPath(capPath, color = Color(0xFFFFD600), style = Fill)
    }
}
