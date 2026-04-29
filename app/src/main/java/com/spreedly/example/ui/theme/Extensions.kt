package com.spreedly.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Extension properties for accessing semantic colors from the color scheme
 * Success color for positive actions and states */
val ColorScheme.success: Color
    @Composable
    get() = SuccessGreen

/** Success light color for backgrounds */
val ColorScheme.successLight: Color
    @Composable
    get() = SuccessGreenLight

/** Success message color for success messages */
val ColorScheme.successMessage: Color
    @Composable
    get() = SuccessMessageGreen

/** Warning color for cautionary actions and states */
val ColorScheme.warning: Color
    @Composable
    get() = WarningOrange

/** Warning light color for backgrounds */
val ColorScheme.warningLight: Color
    @Composable
    get() = WarningOrangeLight

/** Info color for informational messages */
val ColorScheme.info: Color
    @Composable
    get() = InfoBlue

/** Spreedly brand red color */
val ColorScheme.spreedlyRed: Color
    @Composable
    get() = SpreedlyRed

/** Spreedly brand dark red color */
val ColorScheme.spreedlyDarkRed: Color
    @Composable
    get() = SpreedlyDarkRed

/** Spreedly brand orange color */
val ColorScheme.spreedlyOrange: Color
    @Composable
    get() = SpreedlyOrange

/** Spreedly brand yellow color */
val ColorScheme.spreedlyYellow: Color
    @Composable
    get() = SpreedlyYellow

/** Default blue color */
val ColorScheme.defaultBlue: Color
    @Composable
    get() = DefaultBlue

/**
 * Extension functions for color manipulation
 *
 * Returns a copy of this color with the specified alpha value
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

/**
 * Returns a disabled version of this color (30% opacity)
 */
fun Color.disabled(): Color = this.copy(alpha = 0.3f)

/**
 * Returns a hovered version of this color (90% opacity)
 */
fun Color.hovered(): Color = this.copy(alpha = 0.9f)

/**
 * Returns a pressed version of this color (80% opacity)
 */
fun Color.pressed(): Color = this.copy(alpha = 0.8f)
