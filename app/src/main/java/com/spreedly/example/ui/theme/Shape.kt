package com.spreedly.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system for consistent corner radius values throughout the app.
 */
val Shapes =
    Shapes(
        // Extra small components (chips, small buttons)
        extraSmall = RoundedCornerShape(4.dp),
        // Small components (buttons, text fields)
        small = RoundedCornerShape(8.dp),
        // Medium components (cards, dialogs)
        medium = RoundedCornerShape(12.dp),
        // Large components (bottom sheets, large cards)
        large = RoundedCornerShape(16.dp),
        // Extra large components (modal dialogs)
        extraLarge = RoundedCornerShape(24.dp),
    )

/**
 * Additional custom shapes for specific use cases
 */
object CustomShapes {
    val circle = RoundedCornerShape(50)
    val rounded = RoundedCornerShape(8.dp)
    val roundedLarge = RoundedCornerShape(16.dp)
    val topRounded = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val bottomRounded = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
}
