package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameStorage
import com.example.ui.theme.*

@Composable
fun StageSelectScreen(
    storage: GameStorage,
    onSelectStage: (Int) -> Unit,
    onBack: () -> Unit
) {
    val highestUnlocked = storage.highestStageUnlocked
    val totalStages = 60
    var totalStars = 0
    for (s in 1..totalStages) {
        totalStars += storage.getStageStars(s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99030712))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .testTag("stage_select_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }

                Text(
                    text = "SELECT LEVEL",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                // Star Counter
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Stars",
                            tint = NeonGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalStars / ${totalStages * 3}",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(totalStages) { index ->
                    val stageNumber = index + 1
                    val isUnlocked = stageNumber <= highestUnlocked
                    val stars = storage.getStageStars(stageNumber)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = isUnlocked) {
                                onSelectStage(stageNumber)
                            }
                            .border(
                                width = if (stageNumber == highestUnlocked) 2.dp else 1.dp,
                                color = if (stageNumber == highestUnlocked) NeonCyan else Color(0x4438BDF8),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("stage_card_$stageNumber"),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                stageNumber == highestUnlocked -> Color(0xD91E293B)
                                isUnlocked -> Color(0xB30F172A)
                                else -> Color(0x66090E17)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isUnlocked) {
                                Text(
                                    text = "$stageNumber",
                                    color = if (stageNumber == highestUnlocked) NeonCyan else TextWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (starIdx in 1..3) {
                                        val hasStar = starIdx <= stars
                                        Icon(
                                            imageVector = if (hasStar) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = if (hasStar) NeonGold else Color(0xFF334155),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$stageNumber",
                                    color = TextMuted.copy(alpha = 0.5f),
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
