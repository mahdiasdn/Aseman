package com.iliyateam.aseman.ui.modern

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.WeatherViewModel
import com.iliyateam.aseman.cityDisplayName
import com.iliyateam.aseman.descOf
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.next24
import com.iliyateam.aseman.ui.LifestyleCard
import com.iliyateam.aseman.ui.PixelGlassCard

@Composable
fun ModernWeatherScreen(
    s: WeatherViewModel.State.Success,
    prefs: Prefs,
    vm: WeatherViewModel,
    ctx: Context
) {
    val d = s.data
    val isFa = prefs.lang == "fa"
    val isDark = isSystemInDarkTheme()

    val favs by vm.favs.collectAsState()
    val isFav = remember(favs, s.lat, s.lon) {
        val targetLat = (s.lat * 1000).toInt()
        val targetLon = (s.lon * 1000).toInt()
        favs.any { fav ->
            (fav.first * 1000).toInt() == targetLat && (fav.second * 1000).toInt() == targetLon
        }
    }

    val nextHours = remember(d) { next24(d) }
    var selectedHourIndex by remember { mutableIntStateOf(0) }

    val todayMin = d.daily.min.firstOrNull() ?: d.current.temp
    val todayMax = d.daily.max.firstOrNull() ?: d.current.temp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Giant Modern Hero Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // City Title & Favorite Action
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = cityDisplayName(s.city, isFa),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        IconButton(
                            onClick = {
                                vm.toggleFav(s.lat, s.lon, s.city) { msg ->
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Lottie Weather Animation (Hero Visual)
                    WeatherLottieIcon(
                        code = d.current.code,
                        isDay = d.current.isDay == 1,
                        modifier = Modifier.size(160.dp)
                    )

                    // Huge Clean Temperature
                    Text(
                        text = "${d.current.temp.toInt()}°".faDigits(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    // Condition Label
                    Text(
                        text = descOf(d.current.code, isFa),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Frosted Pill Summary (High / Low / Feels Like)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = if (isFa) {
                                "بیشینه ${todayMax.toInt()}° / کمینه ${todayMin.toInt()}° • حس واقعی ${d.current.feels.toInt()}°".faDigits()
                            } else {
                                "H: ${todayMax.toInt()}° / L: ${todayMin.toInt()}° • Feels ${d.current.feels.toInt()}°"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 2. Interactive Modern Hourly 24-Hour Forecast
            item {
                val nowPrefix = d.current.time.take(13) + ":00"
                val hourlyStartIndex = remember(d) {
                    d.hourly.time.indexOfFirst { it >= nowPrefix }.coerceAtLeast(0)
                }
                val currentActualIdx = (hourlyStartIndex + selectedHourIndex).coerceIn(0, (d.hourly.time.size - 1).coerceAtLeast(0))

                val selTime = d.hourly.time.getOrNull(currentActualIdx)?.takeLast(5) ?: "00:00"
                val selFeels = d.hourly.feels.getOrNull(currentActualIdx) ?: d.current.feels.toDouble()
                val selPop = d.hourly.precipitationProbability.getOrNull(currentActualIdx) ?: 0
                val selWind = d.hourly.wind.getOrNull(currentActualIdx) ?: d.current.wind.toDouble()
                val selHumidity = d.hourly.humidity.getOrNull(currentActualIdx) ?: d.current.humidity

                PixelGlassCard(
                    isDynamicTheme = true,
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 24.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "پیش‌بینی ۲۴ ساعت آینده" else "24-Hour Forecast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${if (isFa) "ساعت " else ""}${selTime.faDigits()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = nextHours,
                                key = { idx, h -> "${h.time}_$idx" }
                            ) { idx, h ->
                                val isSelected = idx == selectedHourIndex
                                val pillScale by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (isSelected) 1.04f else 1.0f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                    ),
                                    label = "pill_scale"
                                )
                                val pillElevation by androidx.compose.animation.core.animateDpAsState(
                                    targetValue = if (isSelected) 6.dp else 1.dp,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    ),
                                    label = "pill_elevation"
                                )

                                Surface(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(126.dp)
                                        .graphicsLayer(scaleX = pillScale, scaleY = pillScale)
                                        .clickable {
                                            selectedHourIndex = idx
                                        },
                                    shape = RoundedCornerShape(if (isSelected) 22.dp else 18.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = pillElevation,
                                    shadowElevation = if (isSelected) 4.dp else 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (idx == 0) (if (isFa) "الان" else "Now") else h.time.faDigits(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )

                                        WeatherLottieIcon(
                                            code = h.code,
                                            isDay = h.isDay,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            animate = true
                                        )

                                        Text(
                                            text = "${h.temp.toInt()}°".faDigits(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )

                                        if (h.pop > 0) {
                                            Text(
                                                text = "${h.pop}%".faDigits(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "—",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Transparent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Modern Detail Bar for Selected Hour
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModernHourDetailMetric(
                                    label = if (isFa) "حس واقعی" else "Feels Like",
                                    value = "${selFeels.toInt()}°".faDigits()
                                )
                                ModernHourDetailMetric(
                                    label = if (isFa) "بارش" else "Rain",
                                    value = if (selPop > 0) "${selPop}%".faDigits() else if (isFa) "بدون بارش" else "0%"
                                )
                                ModernHourDetailMetric(
                                    label = if (isFa) "باد" else "Wind",
                                    value = "${selWind.toInt()} ${prefs.windLabel()}".faDigits()
                                )
                                ModernHourDetailMetric(
                                    label = if (isFa) "رطوبت" else "Humidity",
                                    value = "$selHumidity%".faDigits()
                                )
                            }
                        }
                    }
                }
            }

            // 3. 7-Day Forecast with Range Bars
            item {
                ModernForecastRangeCard(
                    dailyTimes = d.daily.time,
                    dailyCodes = d.daily.code,
                    dailyMins = d.daily.min,
                    dailyMaxs = d.daily.max,
                    dailyRainProbs = d.daily.precipitationProbabilityMax,
                    currentTemp = d.current.temp,
                    prefs = prefs
                )
            }

            // 4. Bento Box Modular Grid
            item {
                ModernBentoGrid(
                    data = d,
                    air = s.air,
                    prefs = prefs
                )
            }

            // 5. Lifestyle & Health Suggestions
            item {
                LifestyleCard(
                    data = d,
                    air = s.air,
                    prefs = prefs
                )
            }
        }
    }
}

@Composable
private fun ModernHourDetailMetric(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
