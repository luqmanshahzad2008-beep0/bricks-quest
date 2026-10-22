package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.model.BallSkin
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(
    storage: GameStorage,
    onBack: () -> Unit,
    initialTab: String = "BALLS",
    soundManager: SoundManager? = null
) {
    var coins by remember { mutableIntStateOf(storage.coins) }
    var ballBalance by remember { mutableIntStateOf(storage.ballBalance) }
    var lightningCharges by remember { mutableIntStateOf(storage.lightningCharges) }
    var clusterCharges by remember { mutableIntStateOf(storage.clusterCharges) }
    var activeSkinId by remember { mutableStateOf(storage.activeSkinId) }
    var selectedSkin by remember {
        mutableStateOf(BallSkin.ALL_SKINS.find { it.id == activeSkinId } ?: BallSkin.ALL_SKINS.first())
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    var showPurchaseToast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "shop_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceNavyDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top Bar (Exact Match to Portrait Reference Screenshot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Back Button with Glowing Cyan Ring
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF030D1C))
                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    .testTag("shop_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center Title flanked by Glowing Neon Purple Streaks: — BALL —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFFD500F9))
                            )
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedTab == "BALLS") "BALL" else if (selectedTab == "POWERUPS") "POWER-UPS" else selectedTab,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFD500F9), Color.Transparent)
                            )
                        )
                )
            }

            // Right: Glowing Ball Inventory Pill [⚪ x 57] (or Diamonds chip when on Diamonds tab)
            if (selectedTab == "DIAMONDS") {
                Surface(
                    color = SpaceNavyCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💎", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coins",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF130426),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD500F9)),
                    modifier = Modifier.testTag("ball_balance_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCircle(
                                color = Color(0xFFD500F9).copy(alpha = 0.5f),
                                radius = 8.dp.toPx()
                            )
                            drawCircle(
                                color = Color(0xFFFA58FF),
                                radius = 5.5.dp.toPx(),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx()
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "x $ballBalance",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Navigation Tabs: [💎 DIAMONDS], [🟣 BALLS], [⚡ POWER-UPS], [📦 SKINS]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShopTabButton(
                title = "DIAMONDS",
                icon = "💎",
                isSelected = selectedTab == "DIAMONDS",
                onClick = {
                    soundManager?.playClickSound()
                    selectedTab = "DIAMONDS"
                },
                modifier = Modifier.weight(1f)
            )
            ShopTabButton(
                title = "BALLS",
                icon = "🟣",
                isSelected = selectedTab == "BALLS",
                onClick = {
                    soundManager?.playClickSound()
                    selectedTab = "BALLS"
                },
                modifier = Modifier.weight(1f)
            )
            ShopTabButton(
                title = "POWER-UPS",
                icon = "⚡",
                isSelected = selectedTab == "POWERUPS",
                onClick = {
                    soundManager?.playClickSound()
                    selectedTab = "POWERUPS"
                },
                modifier = Modifier.weight(1.15f)
            )
            ShopTabButton(
                title = "SKINS",
                icon = "📦",
                isSelected = selectedTab == "SKINS",
                onClick = {
                    soundManager?.playClickSound()
                    selectedTab = "SKINS"
                },
                modifier = Modifier.weight(0.9f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toast feedback message if purchase made
        showPurchaseToast?.let { msg ->
            Surface(
                color = Color(0xFF065F46),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2000L)
                showPurchaseToast = null
            }
        }

        when (selectedTab) {
            "DIAMONDS" -> {
                // Diamond & Bundle Packs (Exact match to reference user image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Featured Mega Bundle (Green glowing border)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                storage.addDiamonds(5000)
                                storage.addLightningCharges(7)
                                coins = storage.coins
                                lightningCharges = storage.lightningCharges
                                showPurchaseToast = "Successfully purchased 5,000 Diamonds + 7 Lightning + 8 Balls! 💎⚡"
                            }
                            .testTag("bundle_mega"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF051B11)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+30%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("5 000 Diamonds + Bundle", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⚡ 7 Lightning", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                    Text("⚪ 8 Balls", color = NeonGold, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    storage.addDiamonds(5000)
                                    storage.addLightningCharges(7)
                                    coins = storage.coins
                                    lightningCharges = storage.lightningCharges
                                    showPurchaseToast = "Successfully purchased 5,000 Diamonds + Bundle! 💎"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Rs 9,500.00", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }

                    // Standard Diamond Packs Grid/List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiamondPackCard(
                            title = "1000 Diamonds",
                            price = "Rs 1,900.00",
                            isHot = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(1000)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 1,000 Diamonds! 💎"
                            }
                        )
                        DiamondPackCard(
                            title = "10000 + 3000",
                            subtitle = "Bonus Diamonds",
                            bonus = "+30%",
                            price = "Rs 25,600.00",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(13000)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 13,000 Diamonds! 💎"
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiamondPackCard(
                            title = "300 Diamonds",
                            price = "Rs 550.00",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(300)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 300 Diamonds! 💎"
                            }
                        )
                        DiamondPackCard(
                            title = "500 Diamonds",
                            price = "Rs 1,250.00",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(500)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 500 Diamonds! 💎"
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiamondPackCard(
                            title = "3000 + 300",
                            bonus = "+10%",
                            price = "Rs 5,500.00",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(3300)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 3,300 Diamonds! 💎"
                            }
                        )
                        DiamondPackCard(
                            title = "5000 + 1000",
                            bonus = "+20%",
                            price = "Rs 9,500.00",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                storage.addDiamonds(6000)
                                coins = storage.coins
                                showPurchaseToast = "Purchased 6,000 Diamonds! 💎"
                            }
                        )
                    }
                }
            }
            "BALLS" -> {
                // 6 Ball packages vertically stacked inside rounded neon container (Exact match to reference)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF030D1C)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BallPackageItem(
                                title = "50  Balls",
                                bonusText = "+30%",
                                priceText = "Rs 150.00",
                                isGreenGlow = true,
                                onPurchase = {
                                    val added = (50 * 1.3f).toInt()
                                    storage.addBalls(added)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 50 Balls (+30% Bonus = $added Balls)! 🟣"
                                }
                            )

                            BallPackageItem(
                                title = "10  Balls",
                                bonusText = null,
                                priceText = "Rs 50.00",
                                isGreenGlow = false,
                                onPurchase = {
                                    storage.addBalls(10)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 10 Balls! 🟣"
                                }
                            )

                            BallPackageItem(
                                title = "30  Balls",
                                bonusText = "+20%",
                                priceText = "Rs 100.00",
                                isGreenGlow = false,
                                onPurchase = {
                                    val added = (30 * 1.2f).toInt()
                                    storage.addBalls(added)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 30 Balls (+20% Bonus = $added Balls)! 🟣"
                                }
                            )

                            BallPackageItem(
                                title = "100  Balls",
                                bonusText = null,
                                priceText = "Rs 250.00",
                                isGreenGlow = false,
                                onPurchase = {
                                    storage.addBalls(100)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 100 Balls! 🟣"
                                }
                            )

                            BallPackageItem(
                                title = "200  Balls",
                                bonusText = "+25%",
                                priceText = "Rs 450.00",
                                isGreenGlow = false,
                                onPurchase = {
                                    val added = (200 * 1.25f).toInt()
                                    storage.addBalls(added)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 200 Balls (+25% Bonus = $added Balls)! 🟣"
                                }
                            )

                            BallPackageItem(
                                title = "500  Balls",
                                bonusText = "+25%",
                                priceText = "Rs 1,000.00",
                                isGreenGlow = false,
                                onPurchase = {
                                    val added = (500 * 1.25f).toInt()
                                    storage.addBalls(added)
                                    ballBalance = storage.ballBalance
                                    showPurchaseToast = "Successfully purchased 500 Balls (+25% Bonus = $added Balls)! 🟣"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            "POWERUPS" -> {
                // Power-ups Shop Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("BOOSTERS & POWER-UPS", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                    PowerUpShopCard(
                        title = "Lightning Laser Charges (+5)",
                        description = "Instantly clears entire rows & columns of bricks!",
                        icon = "⚡",
                        price = "200 💎",
                        currentCount = lightningCharges,
                        onClick = {
                            if (coins >= 200) {
                                storage.diamonds -= 200
                                storage.addLightningCharges(5)
                                coins = storage.coins
                                lightningCharges = storage.lightningCharges
                                showPurchaseToast = "Purchased +5 Lightning Charges! ⚡"
                            } else {
                                showPurchaseToast = "Not enough diamonds! 💎"
                            }
                        }
                    )

                    PowerUpShopCard(
                        title = "Cluster Bomb Charges (+5)",
                        description = "Destroys 3x3 brick grids with massive shockwaves!",
                        icon = "💣",
                        price = "250 💎",
                        currentCount = clusterCharges,
                        onClick = {
                            if (coins >= 250) {
                                storage.diamonds -= 250
                                storage.clusterCharges += 5
                                coins = storage.coins
                                clusterCharges = storage.clusterCharges
                                showPurchaseToast = "Purchased +5 Cluster Bomb Charges! 💣"
                            } else {
                                showPurchaseToast = "Not enough diamonds! 💎"
                            }
                        }
                    )
                }
            }
            "SKINS" -> {
                // Ball Skins Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "CHOOSE YOUR BALL",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(BallSkin.ALL_SKINS) { skin ->
                            val isUnlocked = storage.isSkinUnlocked(skin.id)
                            val isEquipped = skin.id == activeSkinId
                            val isSelected = skin.id == selectedSkin.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSkin = skin }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else SpaceNavyBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .testTag("skin_card_${skin.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SpaceNavyLight else SpaceNavyCard
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                color = skin.glowColor.copy(alpha = 0.4f),
                                                radius = size.width * 0.45f
                                            )
                                            drawCircle(
                                                color = skin.primaryColor,
                                                radius = size.width * 0.28f
                                            )
                                        }
                                    }

                                    Text(
                                        text = skin.name,
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )

                                    if (isEquipped) {
                                        Surface(
                                            color = NeonLime.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "EQUIPPED",
                                                color = NeonLime,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isUnlocked) {
                                        Text("OWNED", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonGold, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${skin.price}", color = NeonGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val isUnlocked = storage.isSkinUnlocked(selectedSkin.id)
                    val isEquipped = selectedSkin.id == activeSkinId

                    Button(
                        onClick = {
                            if (isUnlocked) {
                                storage.activeSkinId = selectedSkin.id
                                activeSkinId = selectedSkin.id
                            } else if (coins >= selectedSkin.price) {
                                storage.coins -= selectedSkin.price
                                coins = storage.coins
                                storage.unlockSkin(selectedSkin.id)
                                storage.activeSkinId = selectedSkin.id
                                activeSkinId = selectedSkin.id
                            }
                        },
                        enabled = isUnlocked || coins >= selectedSkin.price,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("skin_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) NeonCyan else NeonGold,
                            contentColor = SpaceNavyDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                isEquipped -> "CURRENTLY EQUIPPED"
                                isUnlocked -> "EQUIP SKIN"
                                coins >= selectedSkin.price -> "UNLOCK FOR ${selectedSkin.price} COINS"
                                else -> "NEED ${selectedSkin.price - coins} MORE COINS"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopTabButton(
    title: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1E1B4B) else SpaceNavyCard,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NeonCyan else SpaceNavyBorder
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) NeonCyan else TextMuted,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun DiamondPackCard(
    title: String,
    subtitle: String? = null,
    bonus: String? = null,
    price: String,
    isHot: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(95.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isHot) NeonCyan else SpaceNavyBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            if (bonus != null) {
                Surface(
                    color = Color(0xFF8B5CF6),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(text = bonus, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
            if (isHot) {
                Surface(
                    color = NeonRed,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(text = "HOT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "💎", fontSize = 18.sp)
                Text(text = title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (subtitle != null) {
                    Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = price, color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PowerUpShopCard(
    title: String,
    description: String,
    icon: String,
    price: String,
    currentCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceNavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = SpaceNavyLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = description, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    Text(text = "Owned: $currentCount", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = price, color = SpaceNavyDark, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BallPackCard(
    title: String,
    ballsCount: Int,
    bonusText: String?,
    priceText: String,
    diamondsCost: Int,
    testTag: String,
    isPopular: Boolean = false,
    isBestValue: Boolean = false,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPurchase)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0728)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isBestValue || isPopular) 1.8.dp else 1.2.dp,
            color = when {
                isBestValue -> Color(0xFFFFD700)
                isPopular -> Color(0xFFD500F9)
                else -> Color(0xFF4A148C)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Round glowing purple ball icon (Matching user design reference)
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Outer neon purple glow aura
                        drawCircle(
                            color = Color(0xFFD500F9).copy(alpha = 0.30f),
                            radius = size.width * 0.48f
                        )
                        // Electric magenta boundary ring
                        drawCircle(
                            color = Color(0xFFFA58FF),
                            radius = size.width * 0.36f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2.dp.toPx())
                        )
                        // Inner saturated glowing sphere
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFA8BFF), Color(0xFF8A00D4), Color(0xFF4A0072)),
                                center = Offset(size.width * 0.38f, size.height * 0.38f),
                                radius = size.width * 0.35f
                            ),
                            radius = size.width * 0.32f
                        )
                        // Bright specular reflection dot
                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f),
                            radius = size.width * 0.09f,
                            center = Offset(size.width * 0.38f, size.height * 0.38f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        if (bonusText != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isBestValue) Color(0xFFB45309) else Color(0xFF7C3AED),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = bonusText,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Increases active ball stream by +$ballsCount",
                        color = Color(0xFFC084FC),
                        fontSize = 11.sp
                    )
                }
            }

            // Purchase Button with Diamond Cost
            Button(
                onClick = onPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBestValue) Color(0xFFFFB300) else Color(0xFFD500F9)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = priceText,
                    color = if (isBestValue) Color.Black else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BallPackageItem(
    title: String,
    bonusText: String?,
    priceText: String,
    isGreenGlow: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPurchase),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061528)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isGreenGlow) 2.dp else 1.5.dp,
            color = if (isGreenGlow) Color(0xFF10B981) else Color(0xFF00E5FF)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Bonus badge in top-left if present
            if (bonusText != null) {
                Surface(
                    color = if (isGreenGlow) Color(0xFF10B981) else Color(0xFF8B5CF6),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = bonusText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (bonusText != null) 8.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Ball icon (round glowing purple ball matching reference image)
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFFD500F9).copy(alpha = 0.4f),
                                radius = size.width * 0.46f
                            )
                            drawCircle(
                                color = Color(0xFFFA58FF),
                                radius = size.width * 0.35f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFFFF80FF), Color(0xFF9300FF), Color(0xFF4A0072)),
                                    center = Offset(size.width * 0.38f, size.height * 0.38f),
                                    radius = size.width * 0.35f
                                ),
                                radius = size.width * 0.32f
                            )
                            drawCircle(
                                color = Color.White,
                                radius = size.width * 0.1f,
                                center = Offset(size.width * 0.38f, size.height * 0.38f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Cyan neon price button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF030D1C),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = priceText,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

