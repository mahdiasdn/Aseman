package com.iliyateam.aseman.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iliyateam.aseman.LocalCity
import com.iliyateam.aseman.CityDb
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.data.WeatherRepository
import com.iliyateam.aseman.data.WeatherResult
import com.iliyateam.aseman.descOf
import com.iliyateam.aseman.t
import com.iliyateam.aseman.weatherIcon
import kotlinx.coroutines.launch

@Composable
fun CityCompareDialog(
    prefs: Prefs,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFa = prefs.lang == "fa"
    val repo = remember { WeatherRepository.getInstance(ctx) }

    val defaultCities = remember { CityDb.defaultCities() }

    var city1 by remember { mutableStateOf(defaultCities.firstOrNull() ?: LocalCity("تهران", "Tehran", 35.6892, 51.3890)) }
    var city2 by remember { mutableStateOf(defaultCities.getOrNull(1) ?: LocalCity("اصفهان", "Isfahan", 32.6546, 51.6680)) }

    var weather1 by remember { mutableStateOf<WeatherResult?>(null) }
    var weather2 by remember { mutableStateOf<WeatherResult?>(null) }
    var loading by remember { mutableStateOf(false) }

    var selectingFor by remember { mutableStateOf<Int?>(null) } // 1 or 2
    var searchCityQuery by remember { mutableStateOf("") }

    val loadComparison = {
        scope.launch {
            loading = true
            val tempUnit = if (prefs.uTemp == "f") "fahrenheit" else "celsius"
            val windUnit = when (prefs.uWind) {
                "ms" -> "ms"
                "mph" -> "mph"
                else -> "kmh"
            }

            val r1 = repo.fetchWeather(city1.lat, city1.lon, tempUnit, windUnit)
            val r2 = repo.fetchWeather(city2.lat, city2.lon, tempUnit, windUnit)

            weather1 = r1
            weather2 = r2
            loading = false
        }
    }

    LaunchedEffect(city1, city2) {
        loadComparison()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFa) "مقایسه آب‌وهوای شهرها" else "City Weather Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = if (isFa) "بستن" else "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectingFor = 1 },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa) "شهر اول (تغییر)" else "City 1 (Change)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) city1.fa else city1.en,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val temp = city1
                            city1 = city2
                            city2 = temp
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = if (isFa) "جابجایی" else "Swap",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectingFor = 2 },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa) "شهر دوم (تغییر)" else "City 2 (Change)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isFa) city2.fa else city2.en,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val w1 = weather1?.weather
                    val w2 = weather2?.weather

                    if (w1 != null && w2 != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Main Temp & Condition Comparison
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CityHeroSummary(
                                        cityName = if (isFa) city1.fa else city1.en,
                                        temp = w1.current.temp,
                                        code = w1.current.code,
                                        isDay = w1.current.isDay == 1,
                                        isFa = isFa,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CityHeroSummary(
                                        cityName = if (isFa) city2.fa else city2.en,
                                        temp = w2.current.temp,
                                        code = w2.current.code,
                                        isDay = w2.current.isDay == 1,
                                        isFa = isFa,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Temperature Difference Note
                            item {
                                val diff = w1.current.temp - w2.current.temp
                                val name1 = if (isFa) city1.fa else city1.en
                                val name2 = if (isFa) city2.fa else city2.en
                                val diffText = when {
                                    diff > 0.5 -> if (isFa) "$name1 به میزان ${diff.toInt()}° گرم‌تر از $name2 است ☀️" else "$name1 is ${diff.toInt()}° warmer than $name2"
                                    diff < -0.5 -> if (isFa) "$name1 به میزان ${(-diff).toInt()}° سردتر از $name2 است ❄️" else "$name1 is ${(-diff).toInt()}° colder than $name2"
                                    else -> if (isFa) "دمای هر دو شهر تقریباً یکسان است ✨" else "Temperatures are about the same"
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = diffText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            // Comparison Rows
                            item {
                                ComparisonMetricRow(
                                    icon = Icons.Outlined.Thermostat,
                                    label = if (isFa) "حس واقعی" else "Feels Like",
                                    val1 = "${w1.current.feels.toInt()}°",
                                    val2 = "${w2.current.feels.toInt()}°"
                                )
                            }

                            item {
                                ComparisonMetricRow(
                                    icon = Icons.Outlined.WaterDrop,
                                    label = if (isFa) "رطوبت هوا" else "Humidity",
                                    val1 = "${w1.current.humidity}%",
                                    val2 = "${w2.current.humidity}%"
                                )
                            }

                            item {
                                ComparisonMetricRow(
                                    icon = Icons.Outlined.Air,
                                    label = if (isFa) "سرعت باد" else "Wind Speed",
                                    val1 = "${w1.current.wind.toInt()} ${prefs.windLabel()}",
                                    val2 = "${w2.current.wind.toInt()} ${prefs.windLabel()}"
                                )
                            }

                            item {
                                val uv1 = w1.hourly.uvIndex.firstOrNull() ?: 0.0
                                val uv2 = w2.hourly.uvIndex.firstOrNull() ?: 0.0
                                ComparisonMetricRow(
                                    icon = Icons.Outlined.WbSunny,
                                    label = if (isFa) "شاخص UV" else "UV Index",
                                    val1 = "$uv1",
                                    val2 = "$uv2"
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "خطا در دریافت اطلاعات هوا" else "Failed to load weather data",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // City Selection Sub-Dialog
    if (selectingFor != null) {
        val filteredCities = if (searchCityQuery.isBlank()) {
            defaultCities
        } else {
            CityDb.search(searchCityQuery)
        }

        Dialog(onDismissRequest = { selectingFor = null; searchCityQuery = "" }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isFa) "انتخاب شهر برای مقایسه" else "Select City to Compare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchCityQuery,
                        onValueChange = { searchCityQuery = it },
                        placeholder = { Text(if (isFa) "جستجوی شهر..." else "Search city...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCities) { c ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectingFor == 1) city1 = c else city2 = c
                                        selectingFor = null
                                        searchCityQuery = ""
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Text(
                                    text = if (isFa) c.fa else c.en,
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { selectingFor = null; searchCityQuery = "" },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isFa) "انصراف" else "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun CityHeroSummary(
    cityName: String,
    temp: Float,
    code: Int,
    isDay: Boolean,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = weatherIcon(code, isDay),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${temp.toInt()}°",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = descOf(code, isFa),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    icon: ImageVector,
    label: String,
    val1: String,
    val2: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = val1,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = val2,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}
