package com.iliyateam.aseman.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import com.iliyateam.aseman.openAutoStart
import com.iliyateam.aseman.openBatterySettings
import com.iliyateam.aseman.openMyket
import com.iliyateam.aseman.openUrl
import com.iliyateam.aseman.t

@Composable
fun SettingsScreen(
    prefs: Prefs,
    pad: PaddingValues
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current


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
        Modifier
            .fillMaxSize()
            .padding(pad)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingRow(
            Icons.Filled.Language,
            prefs.t("language"),
            if (prefs.lang == "fa") "فارسی" else "English"
        ) {
            dialog = "lang"
        }

        SettingRow(
            Icons.Filled.DarkMode,
            prefs.t("mode"),
            prefs.t(prefs.mode)
        ) {
            dialog = "mode"
        }

        SettingRow(
            Icons.Filled.Palette,
            prefs.t("accent"),
            prefs.t(prefs.accent)
        ) {
            dialog = "accent"
        }

        SettingRow(
            Icons.Filled.TextFields,
            prefs.t("font"),
            if (prefs.font == "vazir") {
                prefs.t("vazir_font")
            } else {
                prefs.t("default_font")
            }
        ) {
            dialog = "font"
        }

        SettingRow(
            Icons.Filled.FormatSize,
            prefs.t("font_size"),
            "×${prefs.fontScale.toString().take(4)}"
        ) {
            dialog = "size"
        }

        SettingRow(
            Icons.Filled.Straighten,
            prefs.t("units"),
            buildString {
                append(
                    if (prefs.uTemp == "f") {
                        "°F"
                    } else {
                        "°C"
                    }
                )
                append(" • ")
                append(prefs.windLabel())
                append(" • ")
                append(prefs.precipitationLabel())
                append(" • ")
                append(prefs.distanceLabel())
            }
        ) {
            dialog = "units"
        }

        SettingRow(
            Icons.Filled.Update,
            prefs.t("refresh"),
            when (prefs.refresh) {
                "0" -> prefs.t("off")
                "15" -> prefs.t("min15")
                "60" -> prefs.t("min60")
                else -> prefs.t("min30")
            }
        ) {
            dialog = "refresh"
        }

        ElevatedCard(
            Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    prefs.t("alerts"),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = prefs.alerts == "1",
                    onCheckedChange = {
                        prefs.changeAlerts(
                            if (it) "1" else "0"
                        )
                    }
                )
            }
        }

        SettingRow(
            Icons.Outlined.BatteryChargingFull,
            prefs.t("bg_keep"),
            prefs.t("bg_keep_desc")
        ) {
            openBatterySettings(ctx)
        }

        SettingRow(
            Icons.Outlined.Power,
            prefs.t("autostart"),
            prefs.t("autostart_desc")
        ) {
            openAutoStart(ctx)
        }

        SettingRow(
            Icons.Outlined.LocationCity,
            prefs.t("notif_city"),
            if (prefs.notifCity.isEmpty()) {
                prefs.t("follow_app")
            } else {
                prefs.notifCity
                    .split("|")
                    .getOrNull(2)
                    ?: prefs.t("follow_app")
            }
        ) {
            cityDialog = true
        }

        SettingRow(
            Icons.Filled.Palette,
            prefs.t("widget_bg"),
            prefs.t(prefs.widgetBg)
        ) {
            val nv =
                if (prefs.widgetBg == "trans") {
                    "solid"
                } else {
                    "trans"
                }

            prefs.changeWidgetBg(nv)

            ctx.getSharedPreferences(
                "widget",
                Context.MODE_PRIVATE
            )
                .edit()
                .putString("bg", nv)
                .apply()

            val ids =
                android.appwidget.AppWidgetManager
                    .getInstance(ctx)
                    .getAppWidgetIds(
                        android.content.ComponentName(
                            ctx,
                            com.iliyateam.aseman.WeatherWidgetProvider::class.java
                        )
                    )

            if (ids.isNotEmpty()) {
                ctx.sendBroadcast(
                    android.content.Intent(
                        ctx,
                        com.iliyateam.aseman.WeatherWidgetProvider::class.java
                    )
                        .setAction(
                            android.appwidget.AppWidgetManager
                                .ACTION_APPWIDGET_UPDATE
                        )
                        .putExtra(
                            android.appwidget.AppWidgetManager
                                .EXTRA_APPWIDGET_IDS,
                            ids
                        )
                )
            }
        }

        SettingRow(
            Icons.Outlined.Info,
            prefs.t("about_app"),
            prefs.t("developer")
        ) {
            aboutDialog = true
        }
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
                {
                    prefs.changeLang(it)
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
                    "60" to prefs.t("min60")
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
        AlertDialog(
            onDismissRequest = {
                cityDialog = false
            },
            title = {
                Text(
                    prefs.t("notif_city"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = {
                            cityQuery = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        Modifier.verticalScroll(
                            rememberScrollState()
                        )
                    ) {
                        CityDb.search(
                            ctx,
                            cityQuery
                        ).forEach { c ->
                            Surface(
                                onClick = {
                                    prefs.changeNotifCity(
                                        "${c.lat}|${c.lon}|${c.fa}"
                                    )
                                    cityDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (prefs.lang == "fa") {
                                        c.fa
                                    } else {
                                        c.en
                                    },
                                    Modifier.padding(12.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                prefs.changeNotifCity("")
                                cityDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                prefs.t("follow_app"),
                                Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (aboutDialog) {
        AboutDialog(prefs) {
            aboutDialog = false
        }
    }


}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary
            )


            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                prefs.t("font_size"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "آسمان ☀️",
                    fontSize =
                        (26 * prefs.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0.85f to "size_s",
                        1f to "size_m",
                        1.15f to "size_l",
                        1.3f to "size_xl"
                    ).forEach { (s, k) ->
                        FilterChip(
                            selected =
                                prefs.fontScale == s,
                            onClick = {
                                prefs.changeScale(s)
                            },
                            label = {
                                Text(
                                    prefs.t(k)
                                )
                            }
                        )
                    }
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
                "آسمان ☁️",
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
