package com.iliyateam.aseman.ui.modern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iliyateam.aseman.weatherIcon

fun weatherLottieAsset(code: Int, isDay: Boolean): String {
    val name = when {
        // 1. Storm / Thunderstorm
        code in 95..99 -> "Weather-thunder.json"

        // 2. Snow / Sleet
        code in 71..77 || code in 85..86 -> {
            if (isDay) "Weather-snow sunny.json" else "Weather-snow(night).json"
        }

        // 3. Rain / Showers / Drizzle
        code in 51..67 || code in 80..82 -> {
            if (isDay) "Weather-partly shower.json" else "Weather-rainy(night).json"
        }

        // 4. Fog / Mist
        code in 45..48 -> {
            if (isDay) "Foggy.json" else "Weather-mist.json"
        }

        // 5. Overcast / Cloudy
        code == 3 -> {
            if (isDay) "Weather-partly cloudy.json" else "Weather-cloudy(night).json"
        }

        // 6. Partly Cloudy
        code in 1..2 -> {
            if (isDay) "Weather-partly cloudy.json" else "Weather-cloudy(night).json"
        }

        // 7. Clear / Sunny
        code == 0 -> {
            if (isDay) "Weather-sunny.json" else "Weather-night.json"
        }

        // Default
        else -> {
            if (isDay) "Weather-sunny.json" else "Weather-night.json"
        }
    }
    return "weather_lottie/$name"
}

@Composable
fun WeatherLottieIcon(
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    tint: Color = MaterialTheme.colorScheme.primary,
    animate: Boolean = true
) {
    if (!animate) {
        Icon(
            imageVector = weatherIcon(code, isDay),
            contentDescription = null,
            modifier = modifier,
            tint = tint
        )
        return
    }

    val assetPath = weatherLottieAsset(code, isDay)
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val composition = compositionResult.value

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (composition != null && !compositionResult.isFailure) {
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true,
                speed = 1.0f
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        } else {
            // Fallback to high-contrast Vector Icon if Lottie asset is not yet added
            Icon(
                imageVector = weatherIcon(code, isDay),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = tint
            )
        }
    }
}
