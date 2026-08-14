package com.iliyateam.aseman.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.WeatherViewModel
import com.iliyateam.aseman.t
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CityDef(val lat: Double, val lon: Double, val fa: String, val en: String)

private val DEFAULT_CITIES = listOf(
    CityDef(35.6892, 51.3890, "تهران", "Tehran"),
    CityDef(36.2605, 59.6168, "مشهد", "Mashhad"),
    CityDef(32.6546, 51.6680, "اصفهان", "Isfahan"),
    CityDef(29.5918, 52.5837, "شیراز", "Shiraz"),
    CityDef(38.08, 46.2919, "تبریز", "Tabriz"),
    CityDef(37.2808, 49.5832, "رشت", "Rasht")
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
    var job by remember { mutableStateOf<Job?>(null) }
    val faLang = prefs.lang == "fa"

    LaunchedEffect(Unit) {
        vm.prefs = prefs
        vm.loadFavs(ctx)
        vm.loadHidden(ctx)
    }

    Column(
        Modifier.fillMaxSize().padding(pad).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { q ->
                query = q
                job?.cancel()
                job = scope.launch {
                    delay(400)
                    vm.search(q, if (faLang) "fa" else "en")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(prefs.t("search_ph")) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (query.trim().length >= 2) {
            if (results.isEmpty()) {
                Text(if (vpn) prefs.t("vpn_hint") else prefs.t("no_result"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { r ->
                        CityRow(r.name, onClick = {
                            vm.load(r.latitude, r.longitude, r.name)
                            query = ""
                            onCitySelected()
                        })
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(prefs.t("favorites"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                if (favs.isEmpty()) {
                    item { Text(prefs.t("no_fav"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(favs) { f ->
                        CityRow(
                            name = f.third,
                            onClick = { vm.load(f.first, f.second, f.third); onCitySelected() },
                            onDelete = {
                                vm.toggleFav(f.first, f.second, f.third) { msg ->
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        prefs.t("all_cities"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(DEFAULT_CITIES.filterNot { hidden.contains(it.fa) }) { c ->
                    CityRow(
                        name = if (faLang) c.fa else c.en,
                        onClick = { vm.load(c.lat, c.lon, if (faLang) c.fa else c.en); onCitySelected() },
                        onDelete = { vm.hideDefault(c.fa) }
                    )
                }
                if (hidden.isNotEmpty()) {
                    item {
                        TextButton(onClick = { vm.restoreDefaults() }) {
                            Text(prefs.t("restore"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(name: String, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Text(name, fontWeight = FontWeight.Bold)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Remove, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}