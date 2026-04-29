package com.spreedly.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Blue500,
        onPrimary = Gray900,
        primaryContainer = Blue700,
        onPrimaryContainer = Blue100,
        secondary = Blue400,
        onSecondary = Gray900,
        secondaryContainer = Blue500,
        onSecondaryContainer = Gray900,
        tertiary = SpreedlyOrange,
        onTertiary = Gray900,
        tertiaryContainer = Orange700,
        onTertiaryContainer = Orange100,
        error = Red500,
        onError = Gray900,
        errorContainer = Red800,
        onErrorContainer = Red100,
        background = Gray900,
        onBackground = Gray100,
        surface = Gray800,
        onSurface = Gray100,
        surfaceVariant = Gray700,
        onSurfaceVariant = Gray300,
        outline = Gray600,
        outlineVariant = Gray700,
        scrim = Gray950,
        inverseSurface = Gray100,
        inverseOnSurface = Gray900,
        inversePrimary = Blue600,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Blue600,
        onPrimary = Gray50,
        primaryContainer = Blue200,
        onPrimaryContainer = Blue800,
        secondary = Blue500,
        onSecondary = Gray50,
        secondaryContainer = Blue200,
        onSecondaryContainer = Gray900,
        tertiary = SpreedlyOrange,
        onTertiary = Gray50,
        tertiaryContainer = Orange200,
        onTertiaryContainer = Orange900,
        error = Red600,
        onError = Gray50,
        errorContainer = Red100,
        onErrorContainer = Red900,
        background = Gray50,
        onBackground = Gray900,
        surface = Gray50,
        onSurface = Gray900,
        surfaceVariant = Gray200,
        onSurfaceVariant = Gray700,
        outline = Gray400,
        outlineVariant = Gray300,
        scrim = Gray950,
        inverseSurface = Gray800,
        inverseOnSurface = Gray100,
        inversePrimary = Blue400,
    )

@Composable
fun SpreedlyExampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
