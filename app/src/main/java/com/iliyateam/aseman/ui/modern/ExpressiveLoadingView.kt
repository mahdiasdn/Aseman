package com.iliyateam.aseman.ui.modern

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iliyateam.aseman.Prefs

@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1200f,
    durationMillis: Int = 1300
): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 300f, translateAnimation - 300f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

@Composable
fun M3ExpressiveKineticLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kinetic_loader")

    val p1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1"
    )
    val p2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p2"
    )
    val p3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, delayMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p3"
    )
    val p4 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, delayMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p4"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height((10 + 22 * p1).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f + 0.5f * p1))
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .height((10 + 22 * p2).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f + 0.5f * p2))
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .height((10 + 22 * p3).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f + 0.5f * p3))
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .height((10 + 22 * p4).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f + 0.5f * p4))
        )
    }
}

@Composable
fun M3ExpressiveLoadingScreen(prefs: Prefs) {
    val isFa = prefs.lang == "fa"
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_pulse")

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Expressive Morphing Halo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Rotating Expressive Aura Ring
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .rotate(haloRotation)
                        .clip(RoundedCornerShape(38.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                // Inner Expressive Frosted Squircle
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        WeatherLottieIcon(
                            code = 2,
                            isDay = true,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Name (Expressive Headline)
            Text(
                text = if (isFa) "آسمان" else "Aseman",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = if (isFa) 0.sp else 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status message badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = if (isFa) "دریافت وضعیت زنده جوی..." else "Syncing live weather...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // M3 Expressive Kinetic Loader
            M3ExpressiveKineticLoader(
                modifier = Modifier.height(34.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Expressive Skeleton Preview
            Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Skeleton Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(shimmerBrush)
                )

                // Skeleton Hourly Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}
