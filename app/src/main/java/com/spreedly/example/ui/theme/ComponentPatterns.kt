package com.spreedly.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Common component patterns and configurations using the design system.
 * These objects provide pre-configured styles for consistent UI components.
 *
 * Common button styles using the design system
 */
object ButtonStyles {
    /**
     * Primary button colors - main call-to-action buttons
     */
    val primaryColors: ButtonColors
        @Composable
        get() =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.disabled(),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.disabled(),
            )

    /**
     * Success button colors - positive actions and confirmations
     */
    val successColors: ButtonColors
        @Composable
        get() =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.success,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.success.disabled(),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.disabled(),
            )

    /**
     * Error button colors - destructive actions
     */
    val errorColors: ButtonColors
        @Composable
        get() =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.error.disabled(),
                disabledContentColor = MaterialTheme.colorScheme.onError.disabled(),
            )

    /**
     * Warning button colors - cautionary actions
     */
    val warningColors: ButtonColors
        @Composable
        get() =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.warning,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.warning.disabled(),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.disabled(),
            )

    /**
     * Standard button padding
     */
    val standardPadding: PaddingValues
        get() =
            PaddingValues(
                horizontal = Spacing.buttonPaddingHorizontal,
                vertical = Spacing.buttonPaddingVertical,
            )

    /**
     * Compact button padding for smaller buttons
     */
    val compactPadding: PaddingValues
        get() =
            PaddingValues(
                horizontal = Spacing.xs,
                vertical = Spacing.xxs,
            )

    /**
     * Large button padding for prominent CTAs
     */
    val largePadding: PaddingValues
        get() =
            PaddingValues(
                horizontal = Spacing.md,
                vertical = Spacing.sm,
            )
}

/**
 * Common card styles using the design system
 */
object CardStyles {
    /**
     * Default card colors
     */
    val defaultColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )

    /**
     * Primary card colors - emphasized cards
     */
    val primaryColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

    /**
     * Success card colors - positive status cards
     */
    val successColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.successLight,
                contentColor = MaterialTheme.colorScheme.success,
            )

    /**
     * Warning card colors - cautionary status cards
     */
    val warningColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.warningLight,
                contentColor = MaterialTheme.colorScheme.warning,
            )

    /**
     * Error card colors - error status cards
     */
    val errorColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
            )

    /**
     * Info card colors - informational cards
     */
    val infoColors: CardColors
        @Composable
        get() =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.info,
                contentColor = MaterialTheme.colorScheme.primary,
            )

    /**
     * Outlined card border
     */
    val outlinedBorder: BorderStroke
        @Composable
        get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    /**
     * Subtle outlined card border
     */
    val subtleBorder: BorderStroke
        @Composable
        get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    /**
     * Standard card padding
     */
    val standardPadding: PaddingValues
        get() = PaddingValues(Spacing.cardPadding)

    /**
     * Compact card padding
     */
    val compactPadding: PaddingValues
        get() = PaddingValues(Spacing.xs)

    /**
     * Large card padding
     */
    val largePadding: PaddingValues
        get() = PaddingValues(Spacing.md)
}

/**
 * Common text field styles using the design system
 */
object TextFieldStyles {
    /**
     * Default text field colors
     */
    val defaultColors: TextFieldColors
        @Composable
        get() =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
            )

    /**
     * Success state text field colors
     */
    val successColors: TextFieldColors
        @Composable
        get() =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.successLight,
                unfocusedContainerColor = MaterialTheme.colorScheme.successLight,
                focusedIndicatorColor = MaterialTheme.colorScheme.success,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.success,
                focusedLabelColor = MaterialTheme.colorScheme.success,
                unfocusedLabelColor = MaterialTheme.colorScheme.success,
            )
}

/**
 * Usage Examples:
 *
 * ```kotlin
 * // Button with success colors
 * Button(
 *     onClick = { },
 *     colors = ButtonStyles.successColors,
 *     contentPadding = ButtonStyles.standardPadding
 * ) {
 *     Text("Success")
 * }
 *
 * // Card with primary colors
 * Card(
 *     colors = CardStyles.primaryColors,
 *     border = CardStyles.outlinedBorder
 * ) {
 *     Column(modifier = Modifier.padding(CardStyles.standardPadding)) {
 *         Text("Title")
 *     }
 * }
 *
 * // Text field with default colors
 * OutlinedTextField(
 *     value = text,
 *     onValueChange = { text = it },
 *     colors = TextFieldStyles.defaultColors
 * )
 * ```
 */
