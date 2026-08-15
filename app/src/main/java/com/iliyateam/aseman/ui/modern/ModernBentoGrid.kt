package com.iliyateam.aseman.ui.modern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.data.AirCurrent
import com.iliyateam.aseman.data.WeatherResponse
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.ui.PixelGlassCard
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ModernBentoGrid(
    data: WeatherResponse,
    air: AirCurrent?,
    prefs: Prefs,
    modifier: Modifier = Modifier
) {
    val isFa = prefs.lang == "fa"
    val c = data.current
    val d = data.daily

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: UV Index & Air Quality (Aligned pair)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UvBentoCard(
                uvIndex = c.uvIndex,
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
            AqiBentoCard(
                air = air,
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Wind Compass & Sun Cycle with Real-time position (Aligned pair)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WindBentoCard(
                windSpeed = c.wind,
                windDirection = c.windDirection,
                windGusts = c.windGusts,
                windUnit = prefs.windLabel(),
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
            SunCycleBentoCard(
                sunrise = d.sunrise.firstOrNull() ?: "06:00",
                sunset = d.sunset.firstOrNull() ?: "18:30",
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Humidity & Visibility / Pressure (Aligned pair)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HumidityBentoCard(
                humidity = c.humidity,
                feels = c.feels,
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
            VisibilityBentoCard(
                visibility = c.visibility,
                pressure = c.pressure,
                isFa = isFa,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* =========================================================================
   1. UV Index Bento Card
   ========================================================================= */
@Composable
private fun UvBentoCard(
    uvIndex: Float,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    val uv = uvIndex.coerceAtLeast(0f)
    val uvText = when {
        uv >= 8 -> if (isFa) "خیلی زیاد" else "Very High"
        uv >= 6 -> if (isFa) "زیاد" else "High"
        uv >= 3 -> if (isFa) "متوسط" else "Moderate"
        uv > 0 -> if (isFa) "کم" else "Low"
        else -> if (isFa) "بدون تابش" else "Zero"
    }

    BentoContainer(
        icon = Icons.Outlined.WbSunny,
        title = if (isFa) "شاخص UV" else "UV Index",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${uv.toInt()}".faDigits(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = uvText,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    uv >= 8 -> Color(0xFFEF4444)
                    uv >= 6 -> Color(0xFFF97316)
                    uv >= 3 -> Color(0xFFEAB308)
                    else -> Color(0xFF22C55E)
                },
                fontWeight = FontWeight.Bold
            )
        }

        // Linear Spectrum Meter with Pin Indicator
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val w = size.width
            val h = size.height

            // Gradient Bar
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF22C55E), // Low
                        Color(0xFFEAB308), // Moderate
                        Color(0xFFF97316), // High
                        Color(0xFFEF4444), // Very High
                        Color(0xFF8B5CF6)  // Extreme
                    )
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )

            // Indicator Pin
            val pinX = ((uv / 11f).coerceIn(0.05f, 0.95f)) * w
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = h * 0.9f,
                center = Offset(pinX, h / 2)
            )
            drawCircle(
                color = Color.White,
                radius = h * 0.7f,
                center = Offset(pinX, h / 2)
            )
        }
    }
}

/* =========================================================================
   2. Air Quality (AQI) Bento Card
   ========================================================================= */
@Composable
private fun AqiBentoCard(
    air: AirCurrent?,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    val aqi = air?.usAqi?.toInt() ?: 45
    val (status, color) = when {
        aqi <= 50 -> (if (isFa) "هوای پاک" else "Good") to Color(0xFF22C55E)
        aqi <= 100 -> (if (isFa) "قابل قبول" else "Moderate") to Color(0xFFEAB308)
        aqi <= 150 -> (if (isFa) "گروه حساس" else "Sensitive") to Color(0xFFF97316)
        else -> (if (isFa) "ناسالم" else "Unhealthy") to Color(0xFFEF4444)
    }

    BentoContainer(
        icon = Icons.Outlined.Air,
        title = if (isFa) "کیفیت هوا" else "Air Quality",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$aqi".faDigits(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        // Color Spectrum Bar with Pin
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFF7C3AED))
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )

            // Active Marker Dot
            val pinX = ((aqi / 200f).coerceIn(0.05f, 0.95f)) * w
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = h * 0.9f,
                center = Offset(pinX, h / 2)
            )
            drawCircle(
                color = Color.White,
                radius = h * 0.7f,
                center = Offset(pinX, h / 2)
            )
        }
    }
}

/* =========================================================================
   3. Wind & Compass Bento Card
   ========================================================================= */
@Composable
private fun WindBentoCard(
    windSpeed: Float,
    windDirection: Int,
    windGusts: Float,
    windUnit: String,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    BentoContainer(
        icon = Icons.Outlined.Explore,
        title = if (isFa) "جهت و سرعت باد" else "Wind",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${windSpeed.toInt()}".faDigits(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = windUnit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isFa) "تندباد: ${windGusts.toInt()}".faDigits() else "Gusts: ${windGusts.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Minimalist Compass Dial
            val dialRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            val centerPivotColor = MaterialTheme.colorScheme.onSurface
            val textMeasurer = rememberTextMeasurer()
            val northLabel = if (isFa) "شمال" else "N"

            Canvas(modifier = Modifier.size(54.dp)) {
                val r = size.width / 2f
                val center = Offset(r, r)

                // Dial Outer Ring
                drawCircle(
                    color = dialRingColor,
                    radius = r - 2.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // North Label (Text)
                val textResult = textMeasurer.measure(
                    text = northLabel,
                    style = TextStyle(
                        color = Color(0xFFEF4444),
                        fontSize = if (isFa) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(r - textResult.size.width / 2f, 2.dp.toPx())
                )

                // Pointer Arrow (pointing to windDirection)
                val rad = (windDirection - 90) * (Math.PI / 180.0)
                val needleLength = r - 10.dp.toPx()
                val needleX = (r + needleLength * cos(rad)).toFloat()
                val needleY = (r + needleLength * sin(rad)).toFloat()

                // Arrow Shaft
                drawLine(
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    start = center,
                    end = Offset(needleX, needleY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = centerPivotColor, radius = 3.5.dp.toPx(), center = center)
            }
        }
    }
}

/* =========================================================================
   4. Sun Cycle Bento Card (Accurate Real-Time Sun Position Arc)
   ========================================================================= */
private fun parseMinutes(timeStr: String): Int {
    val t = timeStr.substringAfter("T").take(5)
    val parts = t.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

@Composable
private fun SunCycleBentoCard(
    sunrise: String,
    sunset: String,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    val srMin = remember(sunrise) { parseMinutes(sunrise) }
    val ssMin = remember(sunset) { parseMinutes(sunset) }

    val nowMin = remember(sunrise, sunset) {
        Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
    }

    val isDaytime = nowMin in srMin..ssMin
    val fraction = if (ssMin > srMin) {
        ((nowMin - srMin).toFloat() / (ssMin - srMin)).coerceIn(0f, 1f)
    } else 0.5f

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) }

    BentoContainer(
        icon = Icons.Outlined.WbSunny,
        title = if (isFa) "طلوع و غروب" else "Sun Cycle",
        modifier = modifier
    ) {
        // Solar Arc Canvas
        val solarBaseLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        val solarDashedArcColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val w = size.width
            val h = size.height
            val baseY = h * 0.88f
            val r = w * 0.40f
            val cx = w / 2f

            // Horizon Base Line
            drawLine(
                color = solarBaseLineColor,
                start = Offset(0f, baseY),
                end = Offset(w, baseY),
                strokeWidth = 1.5f
            )

            // Dashed Celestial Arc
            drawArc(
                color = solarDashedArcColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, baseY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = dashEffect
                )
            )

            // Active Illuminated Daytime Path
            if (isDaytime && fraction > 0.01f) {
                drawArc(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))),
                    startAngle = 180f,
                    sweepAngle = 180f * fraction,
                    useCenter = false,
                    topLeft = Offset(cx - r, baseY - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Real-Time Sun Position Orb
            val sunAngle = Math.PI * (1.0 - fraction)
            val sunX = cx + (r * cos(sunAngle)).toFloat()
            val sunY = baseY - (r * sin(sunAngle)).toFloat()

            if (isDaytime) {
                // Sun Outer Glow
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                    radius = 10.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
                // Sun Core
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 5.5.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            } else {
                // Moon Indicator (Nighttime)
                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.8f),
                    radius = 4.5.dp.toPx(),
                    center = Offset(if (nowMin < srMin) cx - r else cx + r, baseY)
                )
            }
        }

        // Sunrise & Sunset Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${if (isFa) "طلوع " else "Rise "}${sunrise.substringAfter("T").take(5)}".faDigits(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${if (isFa) "غروب " else "Set "}${sunset.substringAfter("T").take(5)}".faDigits(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* =========================================================================
   5. Humidity Bento Card
   ========================================================================= */
@Composable
private fun HumidityBentoCard(
    humidity: Int,
    feels: Float,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    BentoContainer(
        icon = Icons.Outlined.WaterDrop,
        title = if (isFa) "رطوبت هوا" else "Humidity",
        modifier = modifier
    ) {
        Column {
            Text(
                text = "$humidity٪".faDigits(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (isFa) "حس واقعی: ${feels.toInt()}°".faDigits() else "Feels: ${feels.toInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress Bar
        val humidityTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                color = humidityTrackColor,
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                size = Size((humidity / 100f).coerceIn(0.05f, 1f) * w, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
        }
    }
}

/* =========================================================================
   6. Visibility & Pressure Bento Card
   ========================================================================= */
@Composable
private fun VisibilityBentoCard(
    visibility: Float,
    pressure: Float,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    BentoContainer(
        icon = Icons.Outlined.Visibility,
        title = if (isFa) "دید و فشار" else "Visibility",
        modifier = modifier
    ) {
        Column {
            Text(
                text = "${(visibility / 1000f).toInt()} ${if (isFa) "کیلومتر" else "km"}".faDigits(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (isFa) "دید افقی واضح" else "Clear visibility",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${if (isFa) "فشار: " else "Pressure: "}${pressure.toInt()} hPa".faDigits(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

/* =========================================================================
   Universal Bento Box Frame (100% Height & Padding Aligned)
   ========================================================================= */
@Composable
private fun BentoContainer(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    PixelGlassCard(
        isDynamicTheme = true,
        modifier = modifier.height(154.dp),
        shapeRadius = 22.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            content()
        }
    }
}
