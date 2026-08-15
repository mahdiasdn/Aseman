package com.iliyateam.aseman.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.iliyateam.aseman.next24
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iliyateam.aseman.HourItem
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.WeatherViewModel
import com.iliyateam.aseman.aqiInfo
import com.iliyateam.aseman.cityDisplayName
import com.iliyateam.aseman.clockOf
import com.iliyateam.aseman.data.AirCurrent
import com.iliyateam.aseman.dayLabel
import com.iliyateam.aseman.descOf
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.formatDateTime
import com.iliyateam.aseman.t
import com.iliyateam.aseman.timeMinutes
import com.iliyateam.aseman.weatherIcon
import java.util.Locale

@Composable
fun WeatherTab(prefs: Prefs, pad: PaddingValues, vm: WeatherViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    val gpsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            vm.useGps(ctx) { msg ->
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(ctx, prefs.t("gps_err"), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.prefs = prefs
        vm.init(ctx)
        vm.loadFavs(ctx)
        vm.loadHidden(ctx)
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(pad)
    ) {
        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                is WeatherViewModel.State.Loading -> {
                    ModernLaunchLoadingView(prefs)
                }

                is WeatherViewModel.State.Error ->
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                s.msg,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )

                            Button(onClick = vm::retry) {
                                Text(prefs.t("retry"))
                            }
                        }
                    }

                is WeatherViewModel.State.Success -> {
                    if (prefs.themeStyle == "dynamic") {
                        com.iliyateam.aseman.ui.modern.ModernWeatherScreen(s, prefs, vm, ctx)
                    } else {
                        WeatherBody(s, prefs, vm, ctx)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherBody(
    s: WeatherViewModel.State.Success,
    prefs: Prefs,
    vm: WeatherViewModel,
    ctx: Context
) {
    val d = s.data
    val nextHours = remember(d) {
        next24(d)
    }

    val favs by vm.favs.collectAsState()

    val isFav = remember(favs, s.lat, s.lon) {
        val targetLat = (s.lat * 1000).toInt()
        val targetLon = (s.lon * 1000).toInt()
        favs.any { fav ->
            (fav.first * 1000).toInt() == targetLat && (fav.second * 1000).toInt() == targetLon
        }
    }

    var showAllDays by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* هیرو */
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            cityDisplayName(s.city, prefs.lang == "fa"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Icon(
                            weatherIcon(
                                d.current.code,
                                d.current.isDay == 1
                            ),
                            null,
                            Modifier.size(84.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "${d.current.temp.toInt()}°",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light
                            )
                        )

                        Text(
                            descOf(
                                d.current.code,
                                prefs.lang == "fa"
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "${prefs.t("feels")}: ${d.current.feels.toInt()}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "${prefs.t("updated")}: ${formatDateTime(s.updated)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            vm.toggleFav(s.lat, s.lon, s.city) { msg ->
                                Toast.makeText(
                                    ctx,
                                    msg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(52.dp)
                    ) {
                        Icon(
                            if (isFav) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isFav) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }



        item {
            WeatherTimeline(
                data = d,
                prefs = prefs,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            LifestyleCard(
                data = d,
                air = s.air,
                prefs = prefs
            )
        }

        /* پیش‌بینی روزانه (تا ۱۴ روز) */
        item {
            Text(
                if (prefs.lang == "fa") "پیش‌بینی روزانه" else "Daily Forecast",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val totalDays = d.daily.time.size
        val countToShow = if (showAllDays) totalDays else minOf(7, totalDays)

        items(countToShow) { i ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (i) {
                            0 -> prefs.t("today")
                            1 -> prefs.t("tomorrow")
                            else -> dayLabel(
                                d.daily.time[i],
                                prefs.lang == "fa"
                            )
                        },
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        weatherIcon(
                            d.daily.code[i],
                            true
                        ),
                        null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        "${d.daily.min[i].toInt()}°",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "${d.daily.max[i].toInt()}°",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (totalDays > 7) {
            item {
                Surface(
                    onClick = { showAllDays = !showAllDays },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAllDays) {
                                if (prefs.lang == "fa") "نمایش کمتر" else "Show Less"
                            } else {
                                if (prefs.lang == "fa") "مشاهده پیش‌بینی ۱۴ روزه" else "Show 14-Day Forecast"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (showAllDays) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        /* جزئیات */
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DetailCard(
                    Modifier.weight(1f),
                    Icons.Outlined.Air,
                    prefs.t("wind"),
                    "${d.current.wind.toInt()} ${prefs.windLabel()}"
                )

                DetailCard(
                    Modifier.weight(1f),
                    Icons.Outlined.WaterDrop,
                    prefs.t("humidity"),
                    "${d.current.humidity}٪"
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DetailCard(
                    Modifier.weight(1f),
                    Icons.Outlined.Speed,
                    prefs.t("pressure"),
                    "${d.current.pressure.toInt()} hPa"
                )

                DetailCard(
                    Modifier.weight(1f),
                    Icons.Outlined.Cloud,
                    prefs.t("clouds"),
                    "${d.current.clouds}٪"
                )
            }
        }

        item {
            SunCard(
                d.daily.sunrise.firstOrNull() ?: "",
                d.daily.sunset.firstOrNull() ?: "",
                prefs
            )
        }

        item {
            TempChart(nextHours, prefs)
        }

        item {
            s.air?.let { air ->
                AqiCard(air, prefs)
            }
        }
    }
}

@Composable
private fun DetailCard(
    mod: Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    ElevatedCard(mod) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SunCard(
    sunrise: String,
    sunset: String,
    prefs: Prefs
) {
    val sr = timeMinutes(sunrise)
    val ss = timeMinutes(sunset)

    val nowMin = java.util.Calendar.getInstance().let {
        it.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                it.get(java.util.Calendar.MINUTE)
    }

    val frac =
        if (ss > sr) {
            ((nowMin - sr).toFloat() / (ss - sr))
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {

            Text(
                "${prefs.t("sunrise")} / ${prefs.t("sunset")}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val w = size.width
                val h = size.height
                val baseY = h * 0.92f
                val r = h * 0.72f
                val cx = w / 2f

                drawLine(
                    androidx.compose.ui.graphics.Color.Gray.copy(
                        alpha = 0.35f
                    ),
                    androidx.compose.ui.geometry.Offset(
                        0f,
                        baseY
                    ),
                    androidx.compose.ui.geometry.Offset(
                        w,
                        baseY
                    ),
                    strokeWidth = 3f
                )

                drawArc(
                    color = androidx.compose.ui.graphics.Color(
                        0xFFFFC107
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        cx - r,
                        baseY - r
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        r * 2,
                        r * 2
                    ),
                    style = Stroke(width = 5f)
                )

                val ang = Math.PI * (1.0 - frac)

                val x =
                    cx + (r * Math.cos(ang)).toFloat()

                val y =
                    baseY - (r * Math.sin(ang)).toFloat()

                drawCircle(
                    androidx.compose.ui.graphics.Color(
                        0xFFFFEB3B
                    ),
                    radius = 14f,
                    center = androidx.compose.ui.geometry.Offset(
                        x,
                        y
                    )
                )
            }

            Row(Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        prefs.t("sunrise"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        clockOf(sunrise),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        prefs.t("sunset"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        clockOf(sunset),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AqiCard(
    air: AirCurrent,
    prefs: Prefs
) {
    val aqi = air.usAqi?.toInt() ?: return
    val info = aqiInfo(
        aqi,
        prefs.lang == "fa"
    )
    val color = androidx.compose.ui.graphics.Color(
        info.second
    )

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Icon(
                Icons.Outlined.Air,
                null,
                tint = color
            )

            Column(Modifier.weight(1f)) {
                Text(
                    prefs.t("aqi"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    info.first,
                    color = color,
                    fontWeight = FontWeight.Bold
                )

                air.pm25?.let {
                    Text(
                        "${prefs.t("pm25")}: ${it.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "$aqi",
                style = MaterialTheme.typography.displaySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TempChart(
    hours: List<HourItem>,
    prefs: Prefs
) {
    if (hours.size < 2) return

    val paintY = remember {
        android.text.TextPaint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
        }
    }
    val paintX = remember {
        android.text.TextPaint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {

            Text(
                prefs.t("next24"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val w = size.width
                val h = size.height

                val temps = hours.map {
                    it.temp.toFloat()
                }

                val minT = temps.min()
                val maxT = temps.max()
                val range = maxOf(
                    1f,
                    maxT - minT
                )

                val left = 70f
                val right = 30f
                val top = 50f
                val bottom = 60f

                fun px(i: Int) =
                    left +
                            (w - left - right) *
                            i /
                            (hours.size - 1)

                fun py(t: Float) =
                    h - bottom -
                            (h - top - bottom) *
                            (t - minT) /
                            range

                listOf(
                    maxT,
                    (maxT + minT) / 2,
                    minT
                ).forEach { tv ->

                    val y = py(tv)

                    drawLine(
                        androidx.compose.ui.graphics.Color.Gray.copy(
                            alpha = 0.25f
                        ),
                        androidx.compose.ui.geometry.Offset(
                            left,
                            y
                        ),
                        androidx.compose.ui.geometry.Offset(
                            w - right,
                            y
                        ),
                        strokeWidth = 2f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "${tv.toInt()}°",
                        left - 12f,
                        y + 8f,
                        paintY
                    )
                }

                for (i in 1 until hours.size) {
                    drawLine(
                        androidx.compose.ui.graphics.Color(0xFFFFC107),
                        start = androidx.compose.ui.geometry.Offset(
                            px(i - 1),
                            py(temps[i - 1])
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            px(i),
                            py(temps[i])
                        ),
                        strokeWidth = 4f
                    )
                }

                hours.forEachIndexed { i, hh ->

                    drawCircle(
                        androidx.compose.ui.graphics.Color(
                            0xFFFFEB3B
                        ),
                        7f,
                        androidx.compose.ui.geometry.Offset(
                            px(i),
                            py(temps[i])
                        )
                    )

                    if (i % 6 == 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            hh.time.take(2) + ":00",
                            px(i),
                            h - bottom + 40f,
                            paintX
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernLaunchLoadingView(prefs: Prefs) {
    val isFa = prefs.lang == "fa"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = weatherIcon(0, true),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isFa) "آسمان" else "Aseman",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFa) "در حال دریافت اطلاعات آب و هوا..." else "Loading weather data...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.5.dp
            )
        }
    }
}