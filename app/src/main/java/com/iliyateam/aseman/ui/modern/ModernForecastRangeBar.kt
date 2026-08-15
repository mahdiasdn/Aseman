package com.iliyateam.aseman.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.dayLabel
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.ui.PixelGlassCard

@Composable
fun ModernForecastRangeCard(
    dailyTimes: List<String>,
    dailyCodes: List<Int>,
    dailyMins: List<Float>,
    dailyMaxs: List<Float>,
    dailyRainProbs: List<Int>,
    currentTemp: Float,
    prefs: Prefs,
    modifier: Modifier = Modifier
) {
    val isFa = prefs.lang == "fa"
    var expanded by remember { mutableStateOf(false) }

    val globalMin = dailyMins.minOrNull() ?: 0f
    val globalMax = dailyMaxs.maxOrNull() ?: (globalMin + 10f)
    val globalSpan = (globalMax - globalMin).coerceAtLeast(1f)

    val totalDays = dailyTimes.size
    val firstWeekCount = minOf(7, totalDays)

    PixelGlassCard(
        isDynamicTheme = true,
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isFa) "پیش‌بینی روزانه" else "Daily Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isFa) "۱۴ روزه".faDigits() else "14-Day",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // First 7 Days
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until firstWeekCount) {
                    ForecastDayRow(
                        index = i,
                        time = dailyTimes[i],
                        code = dailyCodes.getOrNull(i) ?: 0,
                        min = dailyMins.getOrNull(i) ?: 0f,
                        max = dailyMaxs.getOrNull(i) ?: 0f,
                        rainProb = dailyRainProbs.getOrNull(i) ?: 0,
                        currentTemp = currentTemp,
                        globalMin = globalMin,
                        globalSpan = globalSpan,
                        isFa = isFa
                    )
                }
            }

            // Expandable Second Week (Days 8 to 14)
            if (totalDays > 7) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in 7 until totalDays) {
                            ForecastDayRow(
                                index = i,
                                time = dailyTimes[i],
                                code = dailyCodes.getOrNull(i) ?: 0,
                                min = dailyMins.getOrNull(i) ?: 0f,
                                max = dailyMaxs.getOrNull(i) ?: 0f,
                                rainProb = dailyRainProbs.getOrNull(i) ?: 0,
                                currentTemp = currentTemp,
                                globalMin = globalMin,
                                globalSpan = globalSpan,
                                isFa = isFa
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                // Expand / Collapse Footer Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) {
                            if (isFa) "نمایش کمتر" else "Show Less"
                        } else {
                            if (isFa) "مشاهده پیش‌بینی ۱۴ روزه" else "Show 14-Day Forecast"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastDayRow(
    index: Int,
    time: String,
    code: Int,
    min: Float,
    max: Float,
    rainProb: Int,
    currentTemp: Float,
    globalMin: Float,
    globalSpan: Float,
    isFa: Boolean
) {
    val label = remember(index, time, isFa) {
        when (index) {
            0 -> if (isFa) "امروز" else "Today"
            1 -> if (isFa) "فردا" else "Tomorrow"
            else -> dayLabel(time, isFa)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Label
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium
        )

        // Condition Lottie Animation
        WeatherLottieIcon(
            code = code,
            isDay = true,
            modifier = Modifier.size(28.dp),
            animate = true
        )

        // Rain Probability
        if (rainProb >= 15) {
            Text(
                text = "$rainProb%".faDigits(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(38.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.width(38.dp))
        }

        // Min Temp
        Text(
            text = "${min.toInt()}°".faDigits(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Pixel Range Bar
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        ) {
            val w = size.width
            val h = size.height

            // Track Background
            drawRoundRect(
                color = trackColor,
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )

            // Normalized Range
            val startPercent = ((min - globalMin) / globalSpan).coerceIn(0f, 1f)
            val endPercent = ((max - globalMin) / globalSpan).coerceIn(0f, 1f)

            if (isFa) {
                // RTL: Min temp is on the right, Max temp is on the left
                val barStartX = (1f - endPercent) * w
                val barWidth = ((endPercent - startPercent) * w).coerceAtLeast(h)

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF97316), // High temp (Warm / Red on Left)
                            Color(0xFFFBBF24),
                            Color(0xFF38BDF8)  // Low temp (Cool / Blue on Right)
                        ),
                        startX = 0f,
                        endX = w
                    ),
                    topLeft = Offset(barStartX, 0f),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )

                // Current Temp Indicator Dot for Today
                if (index == 0) {
                    val currentPercent = ((currentTemp - globalMin) / globalSpan).coerceIn(0f, 1f)
                    val dotX = (1f - currentPercent) * w

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = h * 0.9f,
                        center = Offset(dotX, h / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = h * 0.7f,
                        center = Offset(dotX, h / 2f)
                    )
                }
            } else {
                // LTR: Min temp is on the left, Max temp is on the right
                val barStartX = startPercent * w
                val barWidth = ((endPercent - startPercent) * w).coerceAtLeast(h)

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8), // Low temp (Cool / Blue on Left)
                            Color(0xFFFBBF24),
                            Color(0xFFF97316)  // High temp (Warm / Red on Right)
                        ),
                        startX = 0f,
                        endX = w
                    ),
                    topLeft = Offset(barStartX, 0f),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )

                // Current Temp Indicator Dot for Today
                if (index == 0) {
                    val currentPercent = ((currentTemp - globalMin) / globalSpan).coerceIn(0f, 1f)
                    val dotX = currentPercent * w

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = h * 0.9f,
                        center = Offset(dotX, h / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = h * 0.7f,
                        center = Offset(dotX, h / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Max Temp
        Text(
            text = "${max.toInt()}°".faDigits(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.Start
        )
    }
}
