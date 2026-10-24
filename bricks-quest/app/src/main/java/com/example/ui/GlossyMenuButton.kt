package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

private val RussoOneFontFamily = FontFamily(
    Font(R.font.russo_one, FontWeight.Normal)
)

/**
 * Visual styles for the 3D Glossy Arcade Capsule Buttons,
 * matching the user reference image:
 * 1. PLAY / GO - Vibrant Cyan / Electric Blue with glowing cyan border
 * 2. SHOP - Vivid Hot Pink / Magenta with neon pink border
 * 3. SETTINGS - Royal Purple / Lavender with glowing violet border
 * 4. SHARE - Neon Mint / Emerald Green with glowing green border
 */
enum class GlossyButtonStyle(
    val primaryNeon: Color,
    val croppedDrawable: Int,
    val rimHighlight: Color,
    val rimShadow: Color,
    val bgTop: Color,
    val bgMiddle: Color,
    val bgBottom: Color,
    val textShadow: Color,
    val textBevel: Color,
    val textGlow: Color,
    val textFaceTop: Color,
    val textFaceBottom: Color
) {
    PLAY(
        primaryNeon = Color(0xFF00E5FF),
        croppedDrawable = R.drawable.btn_play,
        rimHighlight = Color(0xFF90F2FF),
        rimShadow = Color(0xFF0072BC),
        bgTop = Color(0xFF006CB8),
        bgMiddle = Color(0xFF004380),
        bgBottom = Color(0xFF001B36),
        textShadow = Color(0xFF000D1C),
        textBevel = Color(0xFF003B73),
        textGlow = Color(0xFF00E5FF),
        textFaceTop = Color(0xFFFFFFFF),
        textFaceBottom = Color(0xFF8AE8FF)
    ),
    GO(
        primaryNeon = Color(0xFF00E5FF),
        croppedDrawable = R.drawable.btn_go,
        rimHighlight = Color(0xFF90F2FF),
        rimShadow = Color(0xFF0072BC),
        bgTop = Color(0xFF0078CC),
        bgMiddle = Color(0xFF004B8F),
        bgBottom = Color(0xFF001D3D),
        textShadow = Color(0xFF000E21),
        textBevel = Color(0xFF004282),
        textGlow = Color(0xFF00E5FF),
        textFaceTop = Color(0xFFFFFFFF),
        textFaceBottom = Color(0xFF99EEFF)
    ),
    SHOP(
        primaryNeon = Color(0xFFFF26E2),
        croppedDrawable = R.drawable.btn_shop,
        rimHighlight = Color(0xFFFF85EE),
        rimShadow = Color(0xFFB00072),
        bgTop = Color(0xFFA30067),
        bgMiddle = Color(0xFF6B0042),
        bgBottom = Color(0xFF2E001A),
        textShadow = Color(0xFF1B000E),
        textBevel = Color(0xFF63003C),
        textGlow = Color(0xFFFF26E2),
        textFaceTop = Color(0xFFFFFFFF),
        textFaceBottom = Color(0xFFFF94D8)
    ),
    SETTINGS(
        primaryNeon = Color(0xFFB388FF),
        croppedDrawable = R.drawable.btn_settings,
        rimHighlight = Color(0xFFD6BAFF),
        rimShadow = Color(0xFF651FFF),
        bgTop = Color(0xFF5E1B9B),
        bgMiddle = Color(0xFF3E0D6B),
        bgBottom = Color(0xFF1A0230),
        textShadow = Color(0xFF0E001C),
        textBevel = Color(0xFF380860),
        textGlow = Color(0xFFB388FF),
        textFaceTop = Color(0xFFFFFFFF),
        textFaceBottom = Color(0xFFD6BEFF)
    ),
    SHARE(
        primaryNeon = Color(0xFF00E676),
        croppedDrawable = R.drawable.btn_share,
        rimHighlight = Color(0xFF85FFC1),
        rimShadow = Color(0xFF009650),
        bgTop = Color(0xFF007A50),
        bgMiddle = Color(0xFF004D30),
        bgBottom = Color(0xFF002414),
        textShadow = Color(0xFF001208),
        textBevel = Color(0xFF004226),
        textGlow = Color(0xFF00E676),
        textFaceTop = Color(0xFFFFFFFF),
        textFaceBottom = Color(0xFF8CFFC7)
    )
}

/**
 * 3D Glossy Arcade Capsule Button matching the exact user reference image:
 * Uses high-resolution cropped PNG button drawables directly from the reference image
 * (btn_play.png, btn_shop.png, btn_settings.png, btn_share.png, btn_go.png)
 * with tactile spring press animation, pulsing neon shadow, and accessibility support.
 */
@Composable
fun GlossyArcadeCapsuleButton(
    text: String,
    style: GlossyButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pulseAlpha: Float = 1f,
    height: Dp = 62.dp,
    fontSize: TextUnit = 24.sp,
    letterSpacing: TextUnit = 3.sp,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsule_scale"
    )

    val capsuleShape = RoundedCornerShape(percent = 50)

    val baseModifier = if (testTag.isNotEmpty()) {
        modifier.testTag(testTag)
    } else {
        modifier
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = baseModifier
            .fillMaxWidth(0.85f)
            .widthIn(min = 250.dp, max = 340.dp)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 18.dp,
                shape = capsuleShape,
                ambientColor = style.primaryNeon.copy(alpha = 0.5f * pulseAlpha),
                spotColor = style.primaryNeon.copy(alpha = 0.8f * pulseAlpha)
            )
            .clip(capsuleShape)
            .border(
                width = 2.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        style.rimHighlight.copy(alpha = pulseAlpha),
                        style.rimShadow,
                        style.primaryNeon.copy(alpha = 0.8f)
                    )
                ),
                shape = capsuleShape
            ),
        color = Color.Transparent,
        shape = capsuleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            style.bgTop,
                            style.bgMiddle,
                            style.bgBottom
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Upper Glossy Glass Reflection Sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(height * 0.45f)
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner subtle rim highlight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.5.dp)
                    .clip(capsuleShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                style.primaryNeon.copy(alpha = 0.35f)
                            )
                        ),
                        shape = capsuleShape
                    )
            )

            // 3D Chiseled metallic typography
            GlossyButton3DText(
                text = text,
                style = style,
                fontSize = fontSize,
                letterSpacing = letterSpacing
            )

            // Tactile press highlight
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(capsuleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        }
    }
}

@Composable
private fun GlossyButton3DText(
    text: String,
    style: GlossyButtonStyle,
    fontSize: TextUnit,
    letterSpacing: TextUnit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Drop shadow
        Text(
            text = text,
            color = style.textShadow,
            fontSize = fontSize,
            fontFamily = RussoOneFontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = 3.dp)
        )

        // Outline contour
        val outlineDist = 1.2.dp
        listOf(
            Offset(-outlineDist.value, -outlineDist.value),
            Offset(outlineDist.value, -outlineDist.value),
            Offset(-outlineDist.value, outlineDist.value),
            Offset(outlineDist.value, outlineDist.value),
            Offset(0f, 2.dp.value)
        ).forEach { off ->
            Text(
                text = text,
                color = Color(0xFF04010A),
                fontSize = fontSize,
                fontFamily = RussoOneFontFamily,
                letterSpacing = letterSpacing,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = off.x.dp, y = off.y.dp)
            )
        }

        // Bevel
        Text(
            text = text,
            color = style.textBevel,
            fontSize = fontSize,
            fontFamily = RussoOneFontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = 1.5.dp)
        )

        // Neon Glow
        Text(
            text = text,
            color = style.primaryNeon.copy(alpha = 0.95f),
            fontSize = fontSize,
            fontFamily = RussoOneFontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = style.textGlow,
                    offset = Offset(0f, 0f),
                    blurRadius = 16f
                )
            )
        )

        // Specular face
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = RussoOneFontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            style = TextStyle(
                brush = Brush.verticalGradient(
                    listOf(
                        style.textFaceTop,
                        style.textFaceBottom
                    )
                ),
                shadow = Shadow(
                    color = Color(0x99000000),
                    offset = Offset(0f, 1.2f),
                    blurRadius = 2f
                )
            )
        )
    }
}
