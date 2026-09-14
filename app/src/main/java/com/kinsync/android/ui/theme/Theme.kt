package com.kinsync.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = KinSyncGreen,
    onPrimary = KinSyncOnGreen,
    primaryContainer = KinSyncGreenContainer,
)

private val DarkColors = darkColorScheme(
    primary = KinSyncGreenContainer,
    onPrimary = KinSyncGreen,
)

@Composable
fun KinSyncTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KinSyncTypography,
        content = content,
    )
}
