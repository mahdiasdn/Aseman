package com.iliyateam.aseman.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.WeatherViewModel
import com.iliyateam.aseman.cityDisplayName
import com.iliyateam.aseman.data.WeatherApi
import com.iliyateam.aseman.descOf
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.t
import com.iliyateam.aseman.ui.modern.WeatherLottieIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CityDef(
    val lat: Double,
    val lon: Double,
    val fa: String,
    val en: String,
    val defaultTemp: Int = 18,
    val defaultCode: Int = 0,
    val defaultIsDay: Boolean = true
)

private data class CityWeatherBrief(
    val temp: Int,
    val code: Int,
    val isDay: Boolean
)

private val DEFAULT_CITIES = listOf(
    CityDef(35.6892, 51.3890, "تهران", "Tehran", 18, 0, true),
    CityDef(36.2605, 59.6168, "مشهد", "Mashhad", 12, 61, true),
    CityDef(32.6546, 51.6680, "اصفهان", "Isfahan", 15, 2, true),
    CityDef(29.5918, 52.5837, "شیراز", "Shiraz", 21, 0, false),
    CityDef(38.0800, 46.2919, "تبریز", "Tabriz", 8, 3, true),
    CityDef(37.2808, 49.5832, "رشت", "Rasht", 14, 63, true)
)

@Composable
fun CitiesTab(
    prefs: Prefs,
    pad: PaddingValues,
    onCitySelected: () -> Unit,
    vm: WeatherViewModel = viewModel()
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()
    val vpn by vm.vpnHint.collectAsState()
    val favs by vm.favs.collectAsState()
    val hidden by vm.hidden.collectAsState()
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val faLang = prefs.lang == "fa"

    var isEditMode by remember { mutableStateOf(false) }
    var cityToDelete by remember { mutableStateOf<Triple<Double, Double, String>?>(null) }
    var defaultCityToDelete by remember { mutableStateOf<CityDef?>(null) }

    val weatherMap = remember { mutableStateMapOf<String, CityWeatherBrief>() }

    LaunchedEffect(Unit) {
        vm.prefs = prefs
        vm.loadFavs(ctx)
        vm.loadHidden(ctx)

        // Asynchronously fetch real weather for default & favorite cities
        scope.launch(Dispatchers.IO) {
            val allCities = mutableListOf<Triple<Double, Double, String>>()
            DEFAULT_CITIES.forEach { allCities.add(Triple(it.lat, it.lon, it.fa)) }
            favs.forEach { allCities.add(Triple(it.first, it.second, it.third)) }

            allCities.distinctBy { "${it.first},${it.second}" }.forEach { (lat, lon, key) ->
                try {
                    val res = WeatherApi.instance.getWeather(lat, lon)
                    val c = res.current
                    withContext(Dispatchers.Main) {
                        weatherMap["$lat,$lon"] = CityWeatherBrief(
                            temp = c.temp.toInt(),
                            code = c.code,
                            isDay = c.isDay == 1
                        )
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // Confirmation Dialog for Favorite City Removal
    if (cityToDelete != null) {
        val target = cityToDelete!!
        val name = cityDisplayName(target.third, faLang)
        AlertDialog(
            onDismissRequest = { cityToDelete = null },
            shape = RoundedCornerShape(22.dp),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (faLang) "حذف شهر از علاقه‌مندی‌ها" else "Remove Favorite City",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (faLang)
                        "آیا از حذف شهر «$name» از لیست شهرهای مورد علاقه اطمینان دارید؟"
                    else
                        "Are you sure you want to remove \"$name\" from your favorites?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.toggleFav(target.first, target.second, target.third) { msg ->
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        }
                        cityToDelete = null
                    }
                ) {
                    Text(
                        text = if (faLang) "حذف" else "Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { cityToDelete = null }) {
                    Text(
                        text = if (faLang) "انصراف" else "Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    // Confirmation Dialog for Default City Removal / Hiding
    if (defaultCityToDelete != null) {
        val target = defaultCityToDelete!!
        val name = if (faLang) target.fa else target.en
        AlertDialog(
            onDismissRequest = { defaultCityToDelete = null },
            shape = RoundedCornerShape(22.dp),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (faLang) "حذف شهر پیش‌فرض" else "Remove Default City",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (faLang)
                        "آیا می‌خواهید شهر «$name» از لیست حذف شود؟ (هر زمان می‌توانید از دکمه بازیابی آن را برگردانید)"
                    else
                        "Do you want to hide \"$name\"? (You can restore it anytime)",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.hideDefault(target.fa)
                        Toast.makeText(
                            ctx,
                            if (faLang) "شهر «$name» حذف شد" else "City removed",
                            Toast.LENGTH_SHORT
                        ).show()
                        defaultCityToDelete = null
                    }
                ) {
                    Text(
                        text = if (faLang) "حذف" else "Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { defaultCityToDelete = null }) {
                    Text(
                        text = if (faLang) "انصراف" else "Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(pad)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Modern Pill Search Box
        TextField(
            value = query,
            onValueChange = { q ->
                query = q
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(300)
                    vm.search(q, if (faLang) "fa" else "en")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = if (faLang) "جستجوی شهرهای ایران و جهان" else "Search cities in Iran & World",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { query = ""; vm.clearSearch() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (query.trim().length >= 2) {
            // Search Results Mode
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (vpn) prefs.t("vpn_hint") else prefs.t("no_result"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = if (faLang) "نتایج جستجو" else "Search Results",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(
                        items = results,
                        key = { "${it.latitude},${it.longitude}_${it.name}" }
                    ) { r ->
                        SearchResultCityCard(
                            name = r.name,
                            onClick = {
                                vm.load(r.latitude, r.longitude, r.name)
                                query = ""
                                onCitySelected()
                            }
                        )
                    }
                }
            }
        } else {
            // Main Content: Favorites & Default Cities
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section 1: Favorites Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (faLang) "شهرهای مورد علاقه" else "Favorite Cities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (favs.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${favs.size}".faDigits(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Section 1 Body: Favorites Empty State or 2-Col Grid
                if (favs.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935).copy(alpha = 0.14f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (faLang) "هنوز شهری اضافه نکردی!" else "No favorite cities yet!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (faLang) "از صفحه آب‌وهوا روی ❤️ بزن" else "Tap ❤️ on the weather screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Favorites Grid in pairs of 2
                    val favChunks = favs.chunked(2)
                    items(favChunks) { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { f ->
                                val wData = weatherMap["${f.first},${f.second}"]
                                val temp = wData?.temp ?: 20
                                val code = wData?.code ?: 0
                                val isDay = wData?.isDay ?: true
                                val desc = descOf(code, faLang)

                                ExpressiveCityBentoCard(
                                    name = cityDisplayName(f.third, faLang),
                                    desc = desc,
                                    temp = temp,
                                    code = code,
                                    isDay = isDay,
                                    isFavorite = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        vm.load(f.first, f.second, f.third)
                                        onCitySelected()
                                    },
                                    onDelete = {
                                        cityToDelete = f
                                    }
                                )
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Section 2: Default Cities Header with Edit/Manage Button
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (faLang) "شهرهای پیش‌فرض" else "Default Cities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Edit / Manage Mode Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEditMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (isEditMode) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clickable { isEditMode = !isEditMode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Filled.Check else Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = if (isEditMode)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEditMode)
                                        (if (faLang) "تکمیل" else "Done")
                                    else
                                        (if (faLang) "ویرایش" else "Edit"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditMode)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Section 2 Body: 2-Column Grid for Default Cities
                val visibleDefaults = DEFAULT_CITIES.filterNot { hidden.contains(it.fa) }
                val defaultChunks = visibleDefaults.chunked(2)

                items(defaultChunks) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { c ->
                            val wData = weatherMap["${c.lat},${c.lon}"]
                            val temp = wData?.temp ?: c.defaultTemp
                            val code = wData?.code ?: c.defaultCode
                            val isDay = wData?.isDay ?: c.defaultIsDay
                            val desc = descOf(code, faLang)

                            ExpressiveCityBentoCard(
                                name = if (faLang) c.fa else c.en,
                                desc = desc,
                                temp = temp,
                                code = code,
                                isDay = isDay,
                                isEditMode = isEditMode,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (isEditMode) {
                                        defaultCityToDelete = c
                                    } else {
                                        vm.load(c.lat, c.lon, if (faLang) c.fa else c.en)
                                        onCitySelected()
                                    }
                                },
                                onDelete = if (isEditMode) {
                                    { defaultCityToDelete = c }
                                } else null
                            )
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Restore hidden default cities button
                if (hidden.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    vm.restoreDefaults()
                                    Toast.makeText(
                                        ctx,
                                        if (faLang) "شهرهای پیش‌فرض بازگردانی شدند" else "Default cities restored",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (faLang) "بازگردانی شهرهای حذف‌شده (${hidden.size})".faDigits() else "Restore Hidden Cities (${hidden.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive City Bento Card featuring:
 * - Rounded 22dp shape
 * - Vibrant Location Pin chip
 * - City name and live weather description
 * - Large bold temperature reading
 * - Smooth dynamic Lottie weather animation
 * - Sleek circular Action / Delete button with confirmation
 */
@Composable
private fun ExpressiveCityBentoCard(
    name: String,
    desc: String,
    temp: Int,
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isEditMode)
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Location Pin + City Name (+ Sleek Delete Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                // Sleek, minimal circular delete badge
                if (onDelete != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935).copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, Color(0xFFE53935).copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onDelete)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle Row: Weather Description
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Large Temperature + Lottie Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${temp}°".faDigits(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp
                )

                WeatherLottieIcon(
                    code = code,
                    isDay = isDay,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultCityCard(
    name: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}