package com.spreedly.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing system for consistent margins, padding, and gaps throughout the app.
 * Based on an 4dp base unit system.
 */
object Spacing {
    /** 0dp - No spacing */
    val none: Dp = 0.dp

    /** 2dp - Minimal spacing */
    val xxxs: Dp = 2.dp

    /** 4dp - Extra extra small spacing */
    val xxs: Dp = 4.dp

    /** 8dp - Extra small spacing */
    val xs: Dp = 8.dp

    /** 12dp - Small spacing */
    val sm: Dp = 12.dp

    /** 16dp - Medium spacing (default) */
    val md: Dp = 16.dp

    /** 20dp - Medium-large spacing */
    val mlg: Dp = 20.dp

    /** 24dp - Large spacing */
    val lg: Dp = 24.dp

    /** 32dp - Extra large spacing */
    val xl: Dp = 32.dp

    /** 40dp - Extra extra large spacing */
    val xxl: Dp = 40.dp

    /** 48dp - Extra extra extra large spacing */
    val xxxl: Dp = 48.dp

    /** 64dp - Massive spacing */
    val massive: Dp = 64.dp

    // Semantic spacing aliases for common use cases

    /** 16dp - Default padding for screen content */
    val screenPadding: Dp = md

    /** 8dp - Default spacing between elements */
    val elementSpacing: Dp = xs

    /** 24dp - Section spacing */
    val sectionSpacing: Dp = lg

    /** 12dp - Card padding */
    val cardPadding: Dp = sm

    /** 8dp - Button padding horizontal */
    val buttonPaddingHorizontal: Dp = xs

    /** 12dp - Button padding vertical */
    val buttonPaddingVertical: Dp = sm

    /** 4dp - Icon spacing from text */
    val iconSpacing: Dp = xxs
}
