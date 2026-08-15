package com.iliyateam.aseman.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iliyateam.aseman.BuildConfig
import com.iliyateam.aseman.CityDb
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.faDigits
import com.iliyateam.aseman.openAutoStart
import com.iliyateam.aseman.openBatterySettings
import com.iliyateam.aseman.openMyket
import com.iliyateam.aseman.openUrl
import com.iliyateam.aseman.t

fun restartApp(context: Context) {
    try {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            context.startActivity(intent)
            (context as? android.app.Activity)?.finishAffinity()
        } else {
            (context as? android.app.Activity)?.recreate()
        }
    } catch (_: Exception) {
        (context as? android.app.Activity)?.recreate()
    }
}

@Composable
fun SettingsScreen(
    prefs: Prefs,
    pad: PaddingValues
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val isFa = prefs.lang == "fa"

    var dialog by remember {
        mutableStateOf<String?>(null)
    }

    var cityDialog by remember {
        mutableStateOf(false)
    }

    var cityQuery by remember {
        mutableStateOf("")
    }

    var aboutDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(pad)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group 1: Appearance & Theme
        SettingsSectionHeader(
            icon = Icons.Outlined.Palette,
            title = if (isFa) "ظاهر و پوسته" else "Appearance & Style"
        )
        SettingsGroupCard {
            ModernSettingRow(
                icon = Icons.Filled.Language,
                title = prefs.t("language"),
                value = if (prefs.lang == "fa") "فارسی" else "English",
                onClick = { dialog = "lang" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.DarkMode,
                title = prefs.t("mode"),
                value = prefs.t(prefs.mode),
                onClick = { dialog = "mode" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.AutoAwesome,
                title = prefs.t("theme_style"),
                value = if (prefs.themeStyle == "dynamic") prefs.t("style_dynamic") else prefs.t("style_classic"),
                onClick = { dialog = "theme_style" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.Palette,
                title = prefs.t("accent"),
                value = prefs.t(prefs.accent),
                onClick = { dialog = "accent" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.TextFields,
                title = prefs.t("font"),
                value = if (prefs.font == "vazir") prefs.t("vazir_font") else prefs.t("default_font"),
                onClick = { dialog = "font" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.FormatSize,
                title = prefs.t("font_size"),
                value = "×${prefs.fontScale.toString().take(4)}".faDigits(),
                onClick = { dialog = "size" }
            )
        }

        // Group 2: Units & Auto Refresh
        SettingsSectionHeader(
            icon = Icons.Outlined.Tune,
            title = if (isFa) "واحدها و بروزرسانی" else "Units & Updates"
        )
        SettingsGroupCard {
            ModernSettingRow(
                icon = Icons.Filled.Straighten,
                title = prefs.t("units"),
                value = buildString {
                    append(if (prefs.uTemp == "f") "°F" else "°C")
                    append(" • ")
                    append(prefs.windLabel())
                    append(" • ")
                    append(prefs.precipitationLabel())
                },
                onClick = { dialog = "units" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.Update,
                title = prefs.t("refresh"),
                value = when (prefs.refresh) {
                    "0" -> prefs.t("off")
                    "15" -> prefs.t("min15")
                    "30" -> prefs.t("min30")
                    "60" -> prefs.t("min60")
                    "120" -> prefs.t("min120")
                    "240" -> prefs.t("min240")
                    "360" -> prefs.t("min360")
                    "720" -> prefs.t("min720")
                    "1440" -> prefs.t("min1440")
                    else -> prefs.t("min30")
                },
                onClick = { dialog = "refresh" }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Outlined.Notifications,
                title = prefs.t("alerts"),
                trailing = {
                    Switch(
                        checked = prefs.alerts == "1",
                        onCheckedChange = { prefs.changeAlerts(if (it) "1" else "0") }
                    )
                }
            )
        }

        // Group 3: System & Widget
        SettingsSectionHeader(
            icon = Icons.Outlined.Widgets,
            title = if (isFa) "سیستم و ویجت" else "System & Widget"
        )
        SettingsGroupCard {
            ModernSettingRow(
                icon = Icons.Outlined.BatteryChargingFull,
                title = prefs.t("bg_keep"),
                value = prefs.t("bg_keep_desc"),
                onClick = { openBatterySettings(ctx) }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Outlined.Power,
                title = prefs.t("autostart"),
                value = prefs.t("autostart_desc"),
                onClick = { openAutoStart(ctx) }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Outlined.LocationCity,
                title = prefs.t("notif_city"),
                value = if (prefs.notifCity.isEmpty()) prefs.t("follow_app") else prefs.notifCity.split("|").getOrNull(2) ?: prefs.t("follow_app"),
                onClick = { cityDialog = true }
            )
            SettingsDivider()
            ModernSettingRow(
                icon = Icons.Filled.Palette,
                title = prefs.t("widget_bg"),
                value = prefs.t(prefs.widgetBg),
                onClick = {
                    val nv = if (prefs.widgetBg == "trans") "solid" else "trans"
                    prefs.changeWidgetBg(nv)
                    ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putString("bg", nv).apply()

                    val ids = android.appwidget.AppWidgetManager.getInstance(ctx).getAppWidgetIds(
                        android.content.ComponentName(ctx, com.iliyateam.aseman.WeatherWidgetProvider::class.java)
                    )
                    if (ids.isNotEmpty()) {
                        ctx.sendBroadcast(
                            android.content.Intent(ctx, com.iliyateam.aseman.WeatherWidgetProvider::class.java)
                                .setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                                .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        )
                    }
                }
            )
        }

        // Group 4: About
        SettingsSectionHeader(
            icon = Icons.Outlined.Info,
            title = if (isFa) "درباره برنامه" else "About"
        )
        SettingsGroupCard {
            ModernSettingRow(
                icon = Icons.Outlined.Info,
                title = prefs.t("about_app"),
                value = prefs.t("developer"),
                onClick = { aboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    when (dialog) {
        "lang" -> {
            OptionDialog(
                prefs.t("language"),
                listOf(
                    "fa" to "فارسی",
                    "en" to "English"
                ),
                prefs.lang,
                { newLang ->
                    if (newLang != prefs.lang) {
                        prefs.changeLang(newLang)
                        restartApp(ctx)
                    }
                }
            ) {
                dialog = null
            }
        }

        "mode" -> {
            OptionDialog(
                prefs.t("mode"),
                listOf(
                    "auto" to prefs.t("auto"),
                    "light" to prefs.t("light"),
                    "dark" to prefs.t("dark"),
                    "amoled" to "amoled"
                ),
                prefs.mode,
                {
                    prefs.changeMode(it)
                }
            ) {
                dialog = null
            }
        }

        "theme_style" -> {
            OptionDialog(
                prefs.t("theme_style"),
                listOf(
                    "classic" to prefs.t("style_classic"),
                    "dynamic" to prefs.t("style_dynamic")
                ),
                prefs.themeStyle,
                {
                    prefs.changeThemeStyle(it)
                }
            ) {
                dialog = null
            }
        }

        "accent" -> {
            AccentDialog(prefs) {
                dialog = null
            }
        }

        "font" -> {
            OptionDialog(
                prefs.t("font"),
                listOf(
                    "default" to prefs.t("default_font"),
                    "vazir" to prefs.t("vazir_font")
                ),
                prefs.font,
                {
                    prefs.changeFont(it)
                }
            ) {
                dialog = null
            }
        }

        "size" -> {
            FontSizeDialog(prefs) {
                dialog = null
            }
        }

        "units" -> {
            UnitsDialog(prefs) {
                dialog = null
            }
        }

        "refresh" -> {
            OptionDialog(
                prefs.t("refresh"),
                listOf(
                    "0" to prefs.t("off"),
                    "15" to prefs.t("min15"),
                    "30" to prefs.t("min30"),
                    "60" to prefs.t("min60"),
                    "120" to prefs.t("min120"),
                    "240" to prefs.t("min240"),
                    "360" to prefs.t("min360"),
                    "720" to prefs.t("min720"),
                    "1440" to prefs.t("min1440")
                ),
                prefs.refresh,
                {
                    prefs.changeRefresh(it)
                }
            ) {
                dialog = null
            }
        }
    }

    if (cityDialog) {
        val cityList = if (cityQuery.isBlank()) {
            CityDb.defaultCities(ctx)
        } else {
            CityDb.search(ctx, cityQuery)
        }

        AlertDialog(
            onDismissRequest = {
                cityDialog = false
                cityQuery = ""
            },
            title = {
                Text(
                    prefs.t("notif_city"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = {
                            cityQuery = it
                        },
                        placeholder = {
                            Text(if (prefs.lang == "fa") "جستجوی شهر..." else "Search city...")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                prefs.changeNotifCity("")
                                cityDialog = false
                                cityQuery = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (prefs.notifCity.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "✦ " + prefs.t("follow_app"),
                                Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        cityList.forEach { c ->
                            Surface(
                                onClick = {
                                    prefs.changeNotifCity("${c.lat}|${c.lon}|${c.fa}")
                                    cityDialog = false
                                    cityQuery = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (prefs.lang == "fa") c.fa else c.en,
                                    Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    cityDialog = false
                    cityQuery = ""
                }) {
                    Text(if (prefs.lang == "fa") "انصراف" else "Cancel")
                }
            }
        )
    }

    if (aboutDialog) {
        AboutDialog(prefs) {
            aboutDialog = false
        }
    }


}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 58.dp, end = 16.dp)
    )
}

@Composable
private fun ModernSettingRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (v, label) ->
                    Surface(
                        onClick = {
                            onSelect(v)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color =
                            if (current == v) {
                                MaterialTheme.colorScheme
                                    .secondaryContainer
                            } else {
                                Color.Transparent
                            }
                    ) {
                        Row(
                            Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                Modifier.weight(1f)
                            )


                            if (current == v) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint =
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )


}

@Composable
private fun AccentDialog(
    prefs: Prefs,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                prefs.t("accent"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentDot(
                        prefs,
                        "auto",
                        0
                    )
                    AccentDot(
                        prefs,
                        "green",
                        0xFF4E9440
                    )
                    AccentDot(
                        prefs,
                        "purple",
                        0xFF7B61C4
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentDot(
                        prefs,
                        "orange",
                        0xFFC4661F
                    )
                    AccentDot(
                        prefs,
                        "blue",
                        0xFF3E7CB1
                    )
                    AccentDot(
                        prefs,
                        "pink",
                        0xFFC14E82
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (prefs.lang == "fa") {
                        "باشه"
                    } else {
                        "OK"
                    }
                )
            }
        }
    )

}

@Composable
private fun AccentDot(
    prefs: Prefs,
    value: String,
    color: Long
) {
    val selected =
        prefs.accent == value


    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color =
            if (value == "auto") {
                MaterialTheme.colorScheme.primary
            } else {
                Color(color)
            },
        border =
            BorderStroke(
                if (selected) 3.dp else 0.dp,
                MaterialTheme.colorScheme.onSurface
            ),
        onClick = {
            prefs.changeAccent(value)
        }
    ) {}

}

@Composable
private fun FontSizeDialog(
    prefs: Prefs,
    onDismiss: () -> Unit
) {
    val isFa = prefs.lang == "fa"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = prefs.t("font_size"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "آسمان ☀️" else "Aseman ☀️",
                            fontSize = (24 * prefs.fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        0.85f to "size_s",
                        1f to "size_m",
                        1.15f to "size_l",
                        1.3f to "size_xl"
                    ).forEach { (s, k) ->
                        val selected = prefs.fontScale == s
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { prefs.changeScale(s) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prefs.t(k),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "باشه" else "OK")
            }
        }
    )
}

@Composable
private fun UnitsDialog(
    prefs: Prefs,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                prefs.t("units"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    prefs.t("temp_unit"),
                    style =
                        MaterialTheme.typography.titleSmall
                )


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected =
                            prefs.uTemp == "c",
                        onClick = {
                            prefs.changeUTemp("c")
                        },
                        label = {
                            Text("°C")
                        }
                    )

                    FilterChip(
                        selected =
                            prefs.uTemp == "f",
                        onClick = {
                            prefs.changeUTemp("f")
                        },
                        label = {
                            Text("°F")
                        }
                    )
                }

                Text(
                    prefs.t("wind_unit"),
                    style =
                        MaterialTheme.typography.titleSmall
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected =
                            prefs.uWind == "kmh",
                        onClick = {
                            prefs.changeUWind("kmh")
                        },
                        label = {
                            Text("km/h")
                        }
                    )

                    FilterChip(
                        selected =
                            prefs.uWind == "ms",
                        onClick = {
                            prefs.changeUWind("ms")
                        },
                        label = {
                            Text("m/s")
                        }
                    )

                    FilterChip(
                        selected =
                            prefs.uWind == "mph",
                        onClick = {
                            prefs.changeUWind("mph")
                        },
                        label = {
                            Text("mph")
                        }
                    )
                }

                Text(
                    prefs.t("precip_unit"),
                    style =
                        MaterialTheme.typography.titleSmall
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected =
                            prefs.uPrecip == "mm",
                        onClick = {
                            prefs.changePrecip("mm")
                        },
                        label = {
                            Text("mm")
                        }
                    )

                    FilterChip(
                        selected =
                            prefs.uPrecip == "in",
                        onClick = {
                            prefs.changePrecip("in")
                        },
                        label = {
                            Text("in")
                        }
                    )
                }

                Text(
                    prefs.t("distance_unit"),
                    style =
                        MaterialTheme.typography.titleSmall
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected =
                            prefs.uDistance == "km",
                        onClick = {
                            prefs.changeDistance("km")
                        },
                        label = {
                            Text("km")
                        }
                    )

                    FilterChip(
                        selected =
                            prefs.uDistance == "mi",
                        onClick = {
                            prefs.changeDistance("mi")
                        },
                        label = {
                            Text("mi")
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (prefs.lang == "fa") {
                        "باشه"
                    } else {
                        "OK"
                    }
                )
            }
        }
    )


}

@Composable
fun AboutDialog(
    prefs: Prefs,
    onDismiss: () -> Unit
) {
    val ctx =
        androidx.compose.ui.platform.LocalContext.current


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (prefs.lang == "fa") "آسمان ☁️" else "Aseman ☁️",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${prefs.t("version")} ${BuildConfig.VERSION_NAME}",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${prefs.t("developer")}: [MightyMahdi]"
                )

                Text(
                    prefs.t("editor_line"),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                AboutButton(
                    prefs.t("rate_myket")
                ) {
                    openMyket(ctx)
                }

                AboutButton(
                    prefs.t("github")
                ) {
                    openUrl(
                        ctx,
                        "https://github.com/mahdiasdn/Aseman"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (prefs.lang == "fa") {
                        "باشه"
                    } else {
                        "OK"
                    }
                )
            }
        }
    )


}

@Composable
private fun AboutButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color =
            MaterialTheme.colorScheme
                .primaryContainer
    ) {
        Text(
            label,
            Modifier.padding(14.dp),
            color =
                MaterialTheme.colorScheme
                    .onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}
