package com.iliyateam.aseman.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.data.AirCurrent
import com.iliyateam.aseman.data.WeatherResponse

data class LifestyleDetail(
    val icon: ImageVector,
    val title: String,
    val status: String,
    val desc: String
)

@Composable
fun LifestyleCard(
    data: WeatherResponse,
    air: AirCurrent?,
    prefs: Prefs,
    modifier: Modifier = Modifier
) {
    val isFa = prefs.lang == "fa"
    val current = data.current
    val daily = data.daily
    var activeDialog by remember { mutableStateOf<LifestyleDetail?>(null) }

    // 1. Outdoor Activity (Sport & Exercise)
    val aqi = air?.usAqi ?: 50f
    val temp = current.temp
    val code = current.code
    val isRainOrStorm = code in 51..67 || code in 80..82 || code in 95..99
    val isSnow = code in 71..77 || code in 85..86

    val (exerciseStatus, exerciseDesc) = when {
        isRainOrStorm -> if (isFa) "نامناسب" to "به دلیل بارندگی ورزش در فضای بسته توصیه می‌شود" else "Poor" to "Rainy weather; indoor exercise recommended"
        isSnow -> if (isFa) "نامناسب" to "به دلیل بارش برف مراقب لغزندگی باشید" else "Poor" to "Snow conditions; exercise indoors"
        aqi > 150 -> if (isFa) "خطرناک" to "آلودگی شدید هوا؛ از فعالیت خارج از منزل خودداری کنید" else "Dangerous" to "High air pollution; avoid outdoor exercise"
        aqi > 100 -> if (isFa) "احتیاط" to "کیفیت هوای ناسالم برای گروه‌های حساس" else "Caution" to "Unhealthy air for sensitive groups"
        temp > 38 -> if (isFa) "گرمای شدید" to "خطر گرمازدگی؛ ورزش را به ساعات خنک موکول کنید" else "Extreme Heat" to "Risk of heat exhaustion; exercise in shade"
        temp < 0 -> if (isFa) "سرمای شدید" to "هوای بسیار سرد؛ نیاز به پوشش حرارتی کامل" else "Freezing" to "Freezing temperatures; dress warmly"
        aqi <= 50 && temp in 15.0..26.0 -> if (isFa) "عالی" to "کیفیت هوا پاک و دمای معتدل و ایده‌آل" else "Ideal" to "Clean air and ideal comfortable temperatures"
        else -> if (isFa) "مناسب" to "شرایط جوی برای پیاده‌روی و ورزش عادی است" else "Good" to "Favorable weather for outdoor workout"
    }

    // 2. Car Wash
    val next24hPrecipProb = data.hourly.precipitationProbability.take(24).maxOrNull() ?: 0
    val (carWashStatus, carWashDesc) = when {
        isRainOrStorm || isSnow -> if (isFa) "نامناسب" to "بارندگی فعال در حال حاضر" else "Not Recommended" to "Currently precipitation active"
        next24hPrecipProb >= 50 -> if (isFa) "توصیه نمی‌شود" to "احتمال بالای بارندگی تا ۲۴ ساعت آینده" else "Not Recommended" to "High chance of rain within 24 hours"
        next24hPrecipProb in 20..49 -> if (isFa) "با احتیاط" to "احتمال بارش پراکنده در ساعات آتی" else "Fair" to "Slight chance of scattered rain"
        else -> if (isFa) "بسیار مناسب" to "هوای پایدار و بدون بارش تا ۲۴ ساعت آینده" else "Great Time" to "Clear skies and dry for the next 24 hours"
    }

    // 3. UV Sun Protection
    val uv = current.uvIndex
    val (uvStatus, uvDesc) = when {
        uv >= 8 -> if (isFa) "خیلی زیاد" to "نیاز به ضدآفتاب قوی، کلاه و عینک آفتابی" else "Very High" to "Strong sunscreen, hat & sunglasses required"
        uv >= 6 -> if (isFa) "زیاد" to "از قرار گرفتن طولانی در آفتاب مستقیم پرهیز کنید" else "High" to "Use sunscreen and seek shade midday"
        uv >= 3 -> if (isFa) "متوسط" to "استفاده از کرم ضدآفتاب در ساعات اوج تابش" else "Moderate" to "Apply sunscreen during peak afternoon hours"
        uv > 0 -> if (isFa) "کم" to "تابش ضعیف، محافظت پایه کافی است" else "Low" to "Minimal risk; standard exposure"
        else -> if (isFa) "بدون خطر" to "عدم وجود پرتو فرابنفش در این ساعت" else "None" to "No UV radiation currently"
    }

    // 4. Clothing Advice
    val feels = current.feels
    val (clothingStatus, clothingDesc) = when {
        feels < 5 -> if (isFa) "لباس گرم" to "کاپشن، شال‌گردن و لباس ضخیم زمستانی" else "Heavy Winter" to "Warm jacket, scarf & thick coat"
        feels in 5.0..15.0 -> if (isFa) "لباس معتدل" to "سویشرت، هودی یا بالاپوش بهاره" else "Light Layers" to "Sweater, hoodie or light jacket"
        feels in 15.0..25.0 -> if (isFa) "لباس راحت" to "تی‌شرت و پیراهن خنک معمولی" else "Comfortable" to "Standard comfortable shirt & pants"
        else -> if (isFa) "لباس خنک" to "لباس‌های نخی، نازک و تابستانی" else "Light Summer" to "Breathable, light cotton clothes"
    }

    PixelGlassCard(
        isDynamicTheme = prefs.themeStyle == "dynamic",
        modifier = modifier.fillMaxWidth(),
        shapeRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = if (isFa) "پیشنهادهای سبک زندگی" else "Lifestyle Index",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LifestyleItem(
                        icon = Icons.Outlined.DirectionsRun,
                        title = if (isFa) "ورزش و فعالیت" else "Outdoor Activity",
                        status = exerciseStatus,
                        desc = exerciseDesc,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeDialog = LifestyleDetail(
                                icon = Icons.Outlined.DirectionsRun,
                                title = if (isFa) "ورزش و فعالیت خارج از منزل" else "Outdoor Activity",
                                status = exerciseStatus,
                                desc = exerciseDesc
                            )
                        }
                    )
                    LifestyleItem(
                        icon = Icons.Outlined.DirectionsCar,
                        title = if (isFa) "شستشوی خودرو" else "Car Wash",
                        status = carWashStatus,
                        desc = carWashDesc,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeDialog = LifestyleDetail(
                                icon = Icons.Outlined.DirectionsCar,
                                title = if (isFa) "شستشوی خودرو (کارواش)" else "Car Wash",
                                status = carWashStatus,
                                desc = carWashDesc
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LifestyleItem(
                        icon = Icons.Outlined.WbSunny,
                        title = if (isFa) "ضدآفتاب (UV)" else "Sun Protection",
                        status = uvStatus,
                        desc = uvDesc,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeDialog = LifestyleDetail(
                                icon = Icons.Outlined.WbSunny,
                                title = if (isFa) "محافظت در برابر آفتاب (UV)" else "Sun Protection",
                                status = uvStatus,
                                desc = uvDesc
                            )
                        }
                    )
                    LifestyleItem(
                        icon = Icons.Outlined.Checkroom,
                        title = if (isFa) "پوشش مناسب" else "Clothing Advice",
                        status = clothingStatus,
                        desc = clothingDesc,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeDialog = LifestyleDetail(
                                icon = Icons.Outlined.Checkroom,
                                title = if (isFa) "پیشنهاد پوشش و لباس" else "Clothing Advice",
                                status = clothingStatus,
                                desc = clothingDesc
                            )
                        }
                    )
                }
            }
        }
    }

    // Detail Pop-up Dialog
    activeDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            icon = {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.status,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = item.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = null }) {
                    Text(
                        text = if (isFa) "متوجه شدم" else "Got it",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun LifestyleItem(
    icon: ImageVector,
    title: String,
    status: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                    fontWeight = FontWeight.Medium
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
