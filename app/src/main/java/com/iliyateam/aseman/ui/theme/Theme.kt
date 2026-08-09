package com.iliyateam.aseman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.iliyateam.aseman.Prefs

private fun light(p: Long, pc: Long, onpc: Long) = lightColorScheme(
    primary = Color(p), primaryContainer = Color(pc), onPrimaryContainer = Color(onpc),
    secondaryContainer = Color(pc), onSecondaryContainer = Color(onpc)
)

private fun dark(p: Long, pc: Long, onpc: Long) = darkColorScheme(
    primary = Color(p), primaryContainer = Color(pc), onPrimaryContainer = Color(onpc),
    secondaryContainer = Color(pc), onSecondaryContainer = Color(onpc)
)

@Composable
fun AsanTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.mode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }

    val scheme = when (prefs.accent) {
        "green" -> if (dark) dark(0xFFA6D88A, 0xFF2C4A16, 0xFFD5F2BC) else light(0xFF3B7A2C, 0xFFC8F0B4, 0xFF0C2A04)
        "purple" -> if (dark) dark(0xFFCDB6FF, 0xFF3F2E6E, 0xFFE8DEFF) else light(0xFF6B4FA8, 0xFFEBDDFF, 0xFF251447)
        "orange" -> if (dark) dark(0xFFFFB787, 0xFF55300F, 0xFFFFDCC5) else light(0xFFA8571A, 0xFFFFDBC4, 0xFF381500)
        "blue" -> if (dark) dark(0xFFA9C8FF, 0xFF1F4585, 0xFFD6E4FF) else light(0xFF2E62A6, 0xFFD6E5FF, 0xFF002E6B)
        "pink" -> if (dark) dark(0xFFFFAEDC, 0xFF611D55, 0xFFFFD8EA) else light(0xFFA8467C, 0xFFFFD8EA, 0xFF3E0032)
        else -> if (dark) darkColorScheme() else lightColorScheme()
    }

    /* فونت وزیرمتن اگر فایلش در res/font باشد فعال می‌شود */
    val ctx = LocalContext.current
    val fontId = ctx.resources.getIdentifier("vazirmatn", "font", ctx.packageName)
    val family = if (prefs.font == "vazir" && fontId != 0) FontFamily(Font(fontId)) else FontFamily.Default

    val base = Typography()
    val typo = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
    val finalScheme = if (prefs.mode == "amoled") scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerHigh = Color(0xFF111111),
        surfaceContainerHighest = Color(0xFF161616)
    ) else scheme

    MaterialTheme(
        colorScheme = finalScheme,
        typography = typo,
        content = content
    )
}