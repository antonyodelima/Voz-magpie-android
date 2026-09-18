package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.VozoAccent
import com.example.theme.VozoBorder
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurface
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoPrimary
import com.example.theme.VozoPrimaryVariant
import com.example.theme.VozoSecondary
import com.example.theme.VozoTextMuted
import com.example.theme.VozoTextPrimary
import com.example.theme.VozoTextSecondary

/**
 * Vozo Magpie Logo Composable
 * Renders the distinctive Vozo Magpie bird & sonic waveform visual emblem.
 */
@Composable
fun VozoMagpieLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 110
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vozo_logo_transition")

    // Pulsing halo glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Dynamic waveform wave heights inside the logo
    val barScale1 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val barScale2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val barScale3 by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val barScale4 by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 0.50f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar4"
    )
    val barScale5 by infiniteTransition.animateFloat(
        initialValue = 0.40f, targetValue = 0.80f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar5"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("vozo_magpie_logo")
    ) {
        // Outer pulsing neon ambient aura
        Box(
            modifier = Modifier
                .size((sizeDp * 1.15).dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VozoPrimary.copy(alpha = glowAlpha * 0.45f),
                            VozoSecondary.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Custom Vector Artwork representing Vozo Magpie Sonic Core
        Canvas(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val radius = width / 2f - 4f

            // Inner dark backdrop circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF241442),
                        Color(0xFF140D26),
                        Color(0xFF0C0717)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer metallic rim
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        VozoPrimary,
                        VozoSecondary,
                        VozoAccent,
                        VozoPrimary
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Equalizer audio bars
            val barCount = 5
            val barWidth = width * 0.07f
            val spacing = width * 0.045f
            val startX = center.x - ((barCount * barWidth + (barCount - 1) * spacing) / 2f)
            val maxBarHeight = height * 0.46f

            val barScales = listOf(barScale1, barScale2, barScale3, barScale4, barScale5)
            val barColors = listOf(
                VozoPrimary,
                VozoPrimaryVariant,
                VozoAccent,
                VozoSecondary,
                Color(0xFF6366F1)
            )

            for (i in 0 until barCount) {
                val currentBarHeight = maxBarHeight * barScales[i]
                val bx = startX + i * (barWidth + spacing)
                val by = center.y - (currentBarHeight / 2f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barColors[i],
                            barColors[i].copy(alpha = 0.8f)
                        ),
                        startY = by,
                        endY = by + currentBarHeight
                    ),
                    topLeft = Offset(bx, by),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

/**
 * Vozo Magpie Splash Screen Component
 * Displays the Vozo Magpie logo, animated typography, sound wave indicators,
 * and loading progress while the WebView initializes.
 */
@Composable
fun VozoSplashScreen(
    modifier: Modifier = Modifier,
    loadProgress: Int = 0,
    statusText: String = stringResource(R.string.loading_title)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_particles")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VozoDarkBg)
            .testTag("vozo_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle radial background glow
        Box(
            modifier = Modifier
                .size(380.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VozoSecondary.copy(alpha = 0.12f),
                            VozoPrimary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Main Vozo Magpie Logo
            VozoMagpieLogo(
                sizeDp = 120,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Brand Typography
            Text(
                text = "VOZO MAGPIE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = VozoTextPrimary,
                    fontSize = 26.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_brand_title")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = VozoDarkSurfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VozoBorder),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Text(
                    text = "VOICE LAB & NEURAL CLONING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = VozoAccent,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Bar & Percentage
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VozoDarkSurface.copy(alpha = 0.7f))
                    .border(1.dp, VozoBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (loadProgress >= 100) "Ready" else "Initialising Studio…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = VozoTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "${loadProgress.coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = VozoAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.testTag("splash_progress_text")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (loadProgress.coerceIn(5, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("splash_progress_bar"),
                    color = VozoPrimary,
                    trackColor = VozoDarkSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.loading_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = VozoTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
