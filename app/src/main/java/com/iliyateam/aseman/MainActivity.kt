package com.iliyateam.aseman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iliyateam.aseman.ui.AboutDialog
import com.iliyateam.aseman.ui.CitiesTab
import com.iliyateam.aseman.ui.SettingsScreen
import com.iliyateam.aseman.ui.WeatherTab
import com.iliyateam.aseman.ui.theme.AsanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val high = android.view.WindowManager::class.java
                    .getField("FRAME_RATE_CATEGORY_HIGH").getInt(null)
                android.view.Window::class.java
                    .getMethod("setFrameRateCategory", Int::class.java)
                    .invoke(window, high)
            } catch (_: Exception) { }
        }
        enableEdgeToEdge()
        setContent { Root() }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var loadedLang by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        prefs.load()
        loadedLang = prefs.lang
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        try {
            val i = Intent(ctx, WeatherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ContextCompat.startForegroundService(ctx, i)
            else ctx.startService(i)
        } catch (_: Exception) { }
    }

    LaunchedEffect(prefs.lang, loadedLang) {
        if (loadedLang != null && prefs.lang != loadedLang) {
            delay(250)
            (ctx as? ComponentActivity)?.recreate()
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, prefs.fontScale)
    ) {
        AsanTheme(prefs) {
            MainScaffold(prefs)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScaffold(prefs: Prefs) {
    val ctx = LocalContext.current
    val vm: WeatherViewModel = viewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var showAboutDialog by remember { mutableStateOf(false) }
    val gpsPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        if (res.values.all { it }) {
            vm.useGps(ctx) { msg ->
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    if (prefs.lang == "fa") "آسمان" else "Aseman",
                    modifier = Modifier.padding(24.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.WbSunny, null) },
                    label = { Text(prefs.t("tab_weather")) },
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            pagerState.animateScrollToPage(0)
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.LocationCity, null) },
                    label = { Text(prefs.t("tab_cities")) },
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text(prefs.t("tab_settings")) },
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            pagerState.animateScrollToPage(2)
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, null) },
                    label = { Text(prefs.t("about_app")) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                    label = { Text(prefs.t("exit")) },
                    selected = false,
                    onClick = {
                        (ctx as? ComponentActivity)?.finish()
                    }
                )
            }
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (prefs.lang == "fa") "آسمان" else "Aseman",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "منو")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                vm.useGps(ctx) { msg ->
                                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                gpsPerm.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }) {
                            Icon(Icons.Filled.MyLocation, contentDescription = prefs.t("my_location"))
                        }
                        IconButton(onClick = { vm.retry() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = prefs.t("retry"))
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Outlined.WbSunny, null) },
                        label = { Text(prefs.t("tab_weather")) }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Outlined.Cloud, null) },
                        label = { Text(prefs.t("tab_cities")) }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        icon = { Icon(Icons.Outlined.Speed, null) },
                        label = { Text(prefs.t("tab_settings")) }
                    )
                }
            }
        ) { pad ->

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(pad)
            ) { page ->
                when (page) {
                    0 -> WeatherTab(prefs, androidx.compose.foundation.layout.PaddingValues())
                    1 -> CitiesTab(prefs, androidx.compose.foundation.layout.PaddingValues(), onCitySelected = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    })
                    else -> SettingsScreen(prefs, androidx.compose.foundation.layout.PaddingValues())
                }
            }
        }
    }

    /* پنجرهٔ مشترک درباره — همانی که تنظیمات هم استفاده می‌کند */
    if (showAboutDialog) AboutDialog(prefs) { showAboutDialog = false }
}