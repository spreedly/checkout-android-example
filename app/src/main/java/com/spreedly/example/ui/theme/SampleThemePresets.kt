package com.spreedly.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.spreedly.ui.theme.SpreedlyColors
import com.spreedly.ui.theme.SpreedlyTheme

enum class SampleThemePreset {
    DEFAULT,
    BLUE,
    GREEN,
    PURPLE,
    DARK,
    ;

    val displayName: String
        get() =
            when (this) {
                DEFAULT -> "Default"
                BLUE -> "Blue"
                GREEN -> "Green"
                PURPLE -> "Purple"
                DARK -> "Dark"
            }

    val swatchColor: Color
        get() =
            when (this) {
                DEFAULT -> Color(0x73000000)
                BLUE -> Color.Blue
                GREEN -> Color(0xFF4CAF50)
                PURPLE -> Color(0xFF9C27B0)
                DARK -> Color(0xFF212121)
            }
}

object SampleThemePresets {
    fun resolveTheme(
        preset: SampleThemePreset,
        isDarkMode: Boolean,
        useCustomTheme: Boolean,
    ): SpreedlyTheme? {
        if (!useCustomTheme || preset == SampleThemePreset.DEFAULT) {
            return null
        }
        return if (isDarkMode) {
            buildDarkTheme(preset)
        } else {
            buildLightTheme(preset)
        }
    }

    fun buildLightTheme(preset: SampleThemePreset): SpreedlyTheme =
        when (preset) {
            SampleThemePreset.DEFAULT -> SpreedlyTheme.Default
            SampleThemePreset.BLUE ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color.Blue,
                            secondary = Color.Blue.copy(alpha = 0.7f),
                            background = Color.White,
                            surface = Color(0xFFE3F2FD),
                            text = Color(0xFF1976D2),
                            textSecondary = Color(0xFF424242),
                            border = Color.Blue.copy(alpha = 0.3f),
                            borderFocused = Color.Blue,
                            error = Color.Red,
                            placeholder = Color.Gray,
                        ),
                )
            SampleThemePreset.GREEN ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color(0xFF4CAF50),
                            secondary = Color(0xFF4CAF50).copy(alpha = 0.7f),
                            background = Color.White,
                            surface = Color(0xFFE8F5E9),
                            text = Color(0xFF388E3C),
                            textSecondary = Color(0xFF424242),
                            border = Color(0xFF4CAF50).copy(alpha = 0.3f),
                            borderFocused = Color(0xFF4CAF50),
                            error = Color.Red,
                            placeholder = Color.Gray,
                        ),
                )
            SampleThemePreset.PURPLE ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color(0xFF9C27B0),
                            secondary = Color(0xFF9C27B0).copy(alpha = 0.7f),
                            background = Color.White,
                            surface = Color(0xFFF3E5F5),
                            text = Color(0xFF7B1FA2),
                            textSecondary = Color(0xFF424242),
                            border = Color(0xFF9C27B0).copy(alpha = 0.3f),
                            borderFocused = Color(0xFF9C27B0),
                            error = Color.Red,
                            placeholder = Color.Gray,
                        ),
                )
            SampleThemePreset.DARK ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            text = Color.White,
                            textSecondary = Color(0xFFBDBDBD),
                            surface = Color(0xFF424242),
                            primary = Color(0xFF212121),
                            error = Color(0xFFEF5350),
                            background = Color(0xFF303030),
                            border = Color(0xFF616161),
                            placeholder = Color(0xFFBDBDBD),
                        ),
                )
        }

    fun buildDarkTheme(preset: SampleThemePreset): SpreedlyTheme =
        when (preset) {
            SampleThemePreset.DEFAULT -> SpreedlyTheme.Default
            SampleThemePreset.BLUE ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color.Blue,
                            secondary = Color.Blue.copy(alpha = 0.7f),
                            background = Color.Black,
                            surface = Color(0xFF1C1C1E),
                            text = Color.White,
                            textSecondary = Color.Gray.copy(alpha = 0.8f),
                            border = Color.Blue.copy(alpha = 0.5f),
                            borderFocused = Color.Blue,
                            error = Color.Red,
                            placeholder = Color.Gray.copy(alpha = 0.8f),
                        ),
                )
            SampleThemePreset.GREEN ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color(0xFF4CAF50),
                            secondary = Color(0xFF4CAF50).copy(alpha = 0.7f),
                            background = Color.Black,
                            surface = Color(0xFF1C1C1E),
                            text = Color.White,
                            textSecondary = Color.Gray.copy(alpha = 0.8f),
                            border = Color(0xFF4CAF50).copy(alpha = 0.5f),
                            borderFocused = Color(0xFF4CAF50),
                            error = Color.Red,
                            placeholder = Color.Gray.copy(alpha = 0.8f),
                        ),
                )
            SampleThemePreset.PURPLE ->
                SpreedlyTheme(
                    colors =
                        SpreedlyColors(
                            primary = Color(0xFF9C27B0),
                            secondary = Color(0xFF9C27B0).copy(alpha = 0.7f),
                            background = Color.Black,
                            surface = Color(0xFF1C1C1E),
                            text = Color.White,
                            textSecondary = Color.Gray.copy(alpha = 0.8f),
                            border = Color(0xFF9C27B0).copy(alpha = 0.5f),
                            borderFocused = Color(0xFF9C27B0),
                            error = Color.Red,
                            placeholder = Color.Gray.copy(alpha = 0.8f),
                        ),
                )
            SampleThemePreset.DARK -> buildLightTheme(SampleThemePreset.DARK)
        }
}
