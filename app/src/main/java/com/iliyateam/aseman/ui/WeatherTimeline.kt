package com.iliyateam.aseman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.data.WeatherResponse
import com.iliyateam.aseman.descOf
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.t
import com.iliyateam.aseman.weatherIcon
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherTimeline(
    data: WeatherResponse,
    prefs: Prefs,
    modifier: Modifier = Modifier
) {
    val dataCount = minOf(
        data.hourly.time.size,
        data.hourly.temp.size,
        data.hourly.humidity.size,
        data.hourly.feels.size,
        data.hourly.code.size,
        data.hourly.precipitationProbability.size,
        data.hourly.precipitation.size,
        data.hourly.wind.size,
        data.hourly.windDirection.size,
        data.hourly.windGusts.size,
        data.hourly.visibility.size,
        data.hourly.uvIndex.size,
        data.hourly.isDay.size
    )

    if (dataCount == 0) return

    val startIndex =
        data.hourly.time.indexOfFirst { it >= data.current.time }
            .let {
                if (it >= 0) {
                    it
                } else {
                    0
                }
            }
            .coerceIn(0, dataCount - 1)

    val endIndex =
        minOf(
            startIndex + 24,
            dataCount
        )

    val visibleIndices = remember(data) {
        (startIndex until endIndex).toList()
    }

    if (visibleIndices.isEmpty()) return

    var selectedPosition by remember(data) {
        mutableIntStateOf(0)
    }

    val safePosition =
        selectedPosition.coerceIn(
            0,
            visibleIndices.lastIndex
        )

    val index =
        visibleIndices[safePosition]

    val time =
        data.hourly.time[index]

    val temp =
        data.hourly.temp[index]

    val feels =
        data.hourly.feels[index]

    val humidity =
        data.hourly.humidity[index]

    val code =
        data.hourly.code[index]

    val rainProbability =
        data.hourly.precipitationProbability[index]

    val precipitation =
        data.hourly.precipitation[index]

    val wind =
        data.hourly.wind[index]

    val windDirection =
        data.hourly.windDirection[index]

    val gust =
        data.hourly.windGusts[index]

    val visibility =
        data.hourly.visibility[index]

    val uv =
        data.hourly.uvIndex[index]

    val isDay =
        data.hourly.isDay[index] == 1

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        prefs.t("next24"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (prefs.lang == "fa") {
                            "پیش‌بینی ساعتی"
                        } else {
                            "Hourly forecast"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    time
                        .substringAfter("T")
                        .take(5)
                        .faDigits(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = visibleIndices,
                    key = { _, actualIndex -> actualIndex }
                ) { position, actualIndex ->
                    val selected =
                        position == safePosition

                    val hourTime =
                        data.hourly.time[actualIndex]
                            .substringAfter("T")
                            .take(5)
                            .faDigits()

                    Surface(
                        modifier = Modifier
                            .width(72.dp)
                            .height(125.dp)
                            .clickable {
                                selectedPosition = position
                            },
                        shape = RoundedCornerShape(18.dp),
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 10.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                hourTime,
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                fontWeight =
                                    if (selected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                            )

                            Icon(
                                weatherIcon(
                                    data.hourly.code[actualIndex],
                                    data.hourly.isDay[actualIndex] == 1
                                ),
                                null,
                                modifier = Modifier.size(26.dp),
                                tint =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                            )

                            Text(
                                "${data.hourly.temp[actualIndex].toInt()}°",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                            )

                            val rainProb = data.hourly.precipitationProbability[actualIndex]
                            if (rainProb > 0) {
                                Text(
                                    "${rainProb}٪".faDigits(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme
                                                .onPrimary
                                                .copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.Transparent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            weatherIcon(
                                code,
                                isDay
                            ),
                            null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${temp.toInt()}°",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                descOf(
                                    code,
                                    prefs.lang == "fa"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                time
                                    .substringAfter("T")
                                    .take(5)
                                    .faDigits(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                if (isDay) {
                                    if (prefs.lang == "fa") {
                                        "روز"
                                    } else {
                                        "Day"
                                    }
                                } else {
                                    if (prefs.lang == "fa") {
                                        "شب"
                                    } else {
                                        "Night"
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    TimelineMetric(
                        label = prefs.t("feels"),
                        value = "${feels.toInt()}°"
                    )

                    TimelineMetric(
                        label = prefs.t("humidity"),
                        value = "$humidity٪"
                    )

                    RainMetric(
                        probability = rainProbability,
                        amount = precipitation,
                        prefs = prefs
                    )

                    WindMetric(
                        wind = wind,
                        gust = gust,
                        direction = windDirection,
                        prefs = prefs
                    )

                    val uvLabel =
                        uvLevel(
                            uv,
                            prefs.lang == "fa"
                        )

                    TimelineMetric(
                        label =
                            if (prefs.lang == "fa") {
                                "شاخص UV"
                            } else {
                                "UV Index"
                            },
                        value =
                            "${formatNumber(uv)} ($uvLabel)"
                    )

                    val visibilityLabel =
                        visibilityLevel(
                            visibility,
                            prefs.lang == "fa"
                        )

                    TimelineMetric(
                        label =
                            if (prefs.lang == "fa") {
                                "دید"
                            } else {
                                "Visibility"
                            },
                        value =
                            "${formatVisibility(
                                visibility,
                                prefs.uDistance
                            )} ($visibilityLabel)"
                    )
                }
            }
        }
    }


}

@Composable
private fun TimelineMetric(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }


}

@Composable
private fun RainMetric(
    probability: Int,
    amount: Double,
    prefs: Prefs
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (prefs.lang == "fa") {
                    "احتمال بارش"
                } else {
                    "Rain chance"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Text(
                "$probability٪",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(50.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        probability.coerceIn(0, 100) / 100f
                    )
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(50.dp)
                    ))
        }
    }

    Spacer(
        modifier = Modifier.height(4.dp)
    )

    Text(
        if (prefs.lang == "fa") {
            "مقدار بارش: ${
                formatPrecipitation(
                    amount,
                    prefs.uPrecip
                )
            }"
        } else {
            "Amount: ${
                formatPrecipitation(
                    amount,
                    prefs.uPrecip
                )
            }"
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WindMetric(
    wind: Double,
    gust: Double,
    direction: Int,
    prefs: Prefs
) {
    val directionName =
        windDirectionName(
            direction,
            prefs.lang == "fa"
        )


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindCompass(
            direction = direction,
            modifier = Modifier.size(70.dp)
        )

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (prefs.lang == "fa") {
                        "باد"
                    } else {
                        "Wind"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "${wind.toInt()} ${prefs.windLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (prefs.lang == "fa") {
                        "تندباد"
                    } else {
                        "Gusts"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "${gust.toInt()} ${prefs.windLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                if (prefs.lang == "fa") {
                    "از $directionName"
                } else {
                    "From $directionName"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }


}

@Composable
private fun WindCompass(
    direction: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant

    val paint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.42f

        drawCircle(
            color = outline,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val labelRadius = radius + 5.dp.toPx()
        paint.textSize = 10.dp.toPx()

        drawContext.canvas.nativeCanvas.drawText(
            "N",
            center.x,
            center.y - labelRadius + paint.textSize,
            paint
        )

        drawContext.canvas.nativeCanvas.drawText(
            "E",
            center.x + labelRadius,
            center.y + paint.textSize / 3f,
            paint
        )

        drawContext.canvas.nativeCanvas.drawText(
            "S",
            center.x,
            center.y + labelRadius + paint.textSize,
            paint
        )

        drawContext.canvas.nativeCanvas.drawText(
            "W",
            center.x - labelRadius,
            center.y + paint.textSize / 3f,
            paint
        )

        val angle =
            Math.toRadians(
                (direction - 90).toDouble()
            )

        val arrowLength =
            radius * 0.64f

        val tip =
            Offset(
                center.x +
                        (cos(angle) * arrowLength).toFloat(),
                center.y +
                        (sin(angle) * arrowLength).toFloat()
            )

        val tail =
            Offset(
                center.x -
                        (cos(angle) * arrowLength * 0.45f).toFloat(),
                center.y -
                        (sin(angle) * arrowLength * 0.45f).toFloat()
            )

        drawLine(
            color = primary,
            start = tail,
            end = tip,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val headSize =
            7.dp.toPx()

        val leftAngle =
            angle + Math.toRadians(145.0)

        val rightAngle =
            angle - Math.toRadians(145.0)

        drawLine(
            color = primary,
            start = tip,
            end = Offset(
                tip.x +
                        (cos(leftAngle) * headSize).toFloat(),
                tip.y +
                        (sin(leftAngle) * headSize).toFloat()
            ),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = primary,
            start = tip,
            end = Offset(
                tip.x +
                        (cos(rightAngle) * headSize).toFloat(),
                tip.y +
                        (sin(rightAngle) * headSize).toFloat()
            ),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = primary,
            radius = 3.5.dp.toPx(),
            center = center
        )
    }


}

private fun windDirectionName(
    degrees: Int,
    fa: Boolean
): String {
    val normalized =
        ((degrees % 360) + 360) % 360


    val index =
        (((normalized + 22.5) / 45.0).toInt()) % 8

    val faNames =
        listOf(
            "شمال",
            "شمال‌شرق",
            "شرق",
            "جنوب‌شرق",
            "جنوب",
            "جنوب‌غرب",
            "غرب",
            "شمال‌غرب"
        )

    val enNames =
        listOf(
            "North",
            "Northeast",
            "East",
            "Southeast",
            "South",
            "Southwest",
            "West",
            "Northwest"
        )

    return if (fa) {
        faNames[index]
    } else {
        enNames[index]
    }


}

private fun uvLevel(
    value: Double,
    fa: Boolean
): String {
    if (
        value.isNaN() ||
        value.isInfinite() ||
        value < 0.0
    ) {
        return if (fa) {
            "نامشخص"
        } else {
            "Unknown"
        }
    }


    return when {
        value == 0.0 ->
            if (fa) {
                "بدون خطر"
            } else {
                "No risk"
            }

        value < 3.0 ->
            if (fa) {
                "کم"
            } else {
                "Low"
            }

        value < 6.0 ->
            if (fa) {
                "متوسط"
            } else {
                "Moderate"
            }

        value < 8.0 ->
            if (fa) {
                "زیاد"
            } else {
                "High"
            }

        value < 11.0 ->
            if (fa) {
                "خیلی زیاد"
            } else {
                "Very high"
            }

        else ->
            if (fa) {
                "خطرناک"
            } else {
                "Extreme"
            }
    }


}

private fun visibilityLevel(
    meters: Double,
    fa: Boolean
): String {
    if (
        meters.isNaN() ||
        meters.isInfinite() ||
        meters < 0.0
    ) {
        return if (fa) {
            "نامشخص"
        } else {
            "Unknown"
        }
    }


    return when {
        meters < 1000.0 ->
            if (fa) {
                "بسیار ضعیف"
            } else {
                "Very poor"
            }

        meters < 4000.0 ->
            if (fa) {
                "ضعیف"
            } else {
                "Poor"
            }

        meters < 10000.0 ->
            if (fa) {
                "متوسط"
            } else {
                "Moderate"
            }

        meters < 20000.0 ->
            if (fa) {
                "خوب"
            } else {
                "Good"
            }

        else ->
            if (fa) {
                "عالی"
            } else {
                "Excellent"
            }
    }


}

private fun formatNumber(
    value: Double
): String {
    if (
        value.isNaN() ||
        value.isInfinite()
    ) {
        return "—"
    }


    return if (
        value == value.toInt().toDouble()
    ) {
        value.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            value
        )
    }


}

private fun formatPrecipitation(
    millimeters: Double,
    unit: String
): String {
    if (
        millimeters.isNaN() ||
        millimeters.isInfinite() ||
        millimeters < 0.0
    ) {
        return "—"
    }


    return if (unit == "in") {
        String.format(
            Locale.US,
            "%.2f in",
            millimeters / 25.4
        )
    } else {
        String.format(
            Locale.US,
            "%.1f mm",
            millimeters
        )
    }


}

private fun formatVisibility(
    meters: Double,
    distanceUnit: String
): String {
    if (
        meters.isNaN() ||
        meters.isInfinite() ||
        meters < 0.0
    ) {
        return "—"
    }


    return if (distanceUnit == "mi") {
        String.format(
            Locale.US,
            "%.1f mi",
            meters / 1609.344
        )
    } else {
        String.format(
            Locale.US,
            "%.1f km",
            meters / 1000.0
        )
    }


}
