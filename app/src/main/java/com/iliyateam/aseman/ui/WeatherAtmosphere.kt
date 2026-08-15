package com.iliyateam.aseman.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Container for modern cards.
 */
@Composable
fun PixelGlassCard(
    isDynamicTheme: Boolean,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    if (isDynamicTheme) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(shapeRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            content()
        }
    } else {
        ElevatedCard(
            modifier = modifier,
            shape = RoundedCornerShape(shapeRadius)
        ) {
            content()
        }
    }
}
