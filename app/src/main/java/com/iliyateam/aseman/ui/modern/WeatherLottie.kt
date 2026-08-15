package com.iliyateam.aseman.ui.modern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iliyateam.aseman.weatherIcon

fun weatherLottieAsset(code: Int, isDay: Boolean): String {
    return when {
        code in 95..99 ->
            "weather_lottie/Weather-thunder.json"

        code in 71..77 || code in 85..86 ->
            if (isDay)
                "weather_lottie/Weather-snow sunny.json"
            else
                "weather_lottie/Weather-snow(night).json"

        code in 51..67 || code in 80..82 ->
            if (isDay)
                "weather_lottie/Weather-partly shower.json"
            else
                "weather_lottie/Weather-rainy(night).json"

        code in 45..48 ->
            if (isDay)
                "weather_lottie/Foggy.json"
            else
                "weather_lottie/Weather-mist.json"

        code == 3 ->
            if (isDay)
                "weather_lottie/Weather-partly cloudy.json"
            else
                "weather_lottie/Weather-cloudy(night).json"

        code in 1..2 ->
            if (isDay)
                "weather_lottie/Weather-partly cloudy.json"
            else
                "weather_lottie/Weather-cloudy(night).json"

        code == 0 ->
            if (isDay)
                "weather_lottie/Weather-sunny.json"
            else
                "weather_lottie/Weather-night.json"

        else ->
            if (isDay)
                "weather_lottie/Weather-sunny.json"
            else
                "weather_lottie/Weather-night.json"
    }
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

    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset(assetPath)
    )

    val composition = compositionResult.value

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {

            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true,
                speed = 1f
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )

        } else {
            // فقط هنگام لود شدن، آیکن قدیمی را موقتاً نشان می‌دهیم
            Icon(
                imageVector = weatherIcon(code, isDay),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = tint
            )
        }
    }
}