package com.convex.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.convex.app.domain.model.ThemeMode

// ── Palette ────────────────────────────────────────────────────────────────────
// Deep violet + teal accent — premium feel, media-tool aesthetic

private val Purple10 = Color(0xFF1C0036)
private val Purple20 = Color(0xFF370067)
private val Purple40 = Color(0xFF7C43BD)
private val Purple80 = Color(0xFFD0BCFF)
private val Purple90 = Color(0xFFEADDFF)

private val Teal10 = Color(0xFF001F26)
private val Teal20 = Color(0xFF003640)
private val Teal40 = Color(0xFF00677F)
private val Teal80 = Color(0xFF5DD5F3)
private val Teal90 = Color(0xFFB8EAFF)

private val Neutral10 = Color(0xFF191C1D)
private val Neutral20 = Color(0xFF2D3133)
private val Neutral90 = Color(0xFFE1E3E5)
private val Neutral95 = Color(0xFFEFF1F3)

private val Error10 = Color(0xFF410002)
private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)

// ── Light Scheme ───────────────────────────────────────────────────────────────
private val LightColors =
    lightColorScheme(
        primary = Purple40,
        onPrimary = Color.White,
        primaryContainer = Purple90,
        onPrimaryContainer = Purple10,
        secondary = Teal40,
        onSecondary = Color.White,
        secondaryContainer = Teal90,
        onSecondaryContainer = Teal10,
        error = Error40,
        onError = Color.White,
        errorContainer = Error90,
        onErrorContainer = Error10,
        background = Neutral95,
        onBackground = Neutral10,
        surface = Neutral95,
        onSurface = Neutral10,
        surfaceVariant = Color(0xFFECE6F0),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF7A757F),
    )

// ── Dark Scheme ────────────────────────────────────────────────────────────────
private val DarkColors =
    darkColorScheme(
        primary = Purple80,
        onPrimary = Purple20,
        primaryContainer = Purple10,
        onPrimaryContainer = Purple90,
        secondary = Teal80,
        onSecondary = Teal20,
        secondaryContainer = Teal10,
        onSecondaryContainer = Teal90,
        error = Error80,
        onError = Error10,
        errorContainer = Error40,
        onErrorContainer = Error90,
        background = Neutral10,
        onBackground = Neutral90,
        surface = Neutral10,
        onSurface = Neutral90,
        surfaceVariant = Neutral20,
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
    )

// ── appTheme ───────────────────────────────────────────────────────────────────
@Composable
fun appTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark =
        when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }

    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark ->
                dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isDark ->
                dynamicLightColorScheme(LocalContext.current)
            isDark -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
