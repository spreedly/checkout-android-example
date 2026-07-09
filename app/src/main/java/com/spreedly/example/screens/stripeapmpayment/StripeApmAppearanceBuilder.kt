package com.spreedly.example.screens.stripeapmpayment

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.spreedly.stripe.StripeAPMAppearanceConfig

/**
 * Builds [StripeAPMAppearanceConfig] from the sample app's PaymentSheet appearance QA controls.
 * Mirrors iOS `makeStripeAppearance()` in `StripeAPMPaymentFlowView`.
 */
fun buildStripeAppearanceConfig(
    useCustomAppearance: Boolean,
    primaryColor: Color,
    backgroundColor: Color,
    buttonBackgroundColor: Color,
    buttonTextColor: Color,
    cornerRadiusDp: Float,
): StripeAPMAppearanceConfig? {
    if (!useCustomAppearance) {
        return null
    }
    return StripeAPMAppearanceConfig(
        shapes = StripeAPMAppearanceConfig.Shapes(
            cornerRadiusDp = cornerRadiusDp,
            borderStrokeWidthDp = 1f,
            selectedBorderStrokeWidthDp = null,
        ),
        colors = StripeAPMAppearanceConfig.Colors(
            primary = primaryColor.toArgb(),
            surface = backgroundColor.toArgb(),
        ),
        primaryButton = StripeAPMAppearanceConfig.PrimaryButton(
            background = buttonBackgroundColor.toArgb(),
            onBackground = buttonTextColor.toArgb(),
            cornerRadiusDp = cornerRadiusDp,
            heightDp = 52f,
        ),
    )
}

/** Default primary / pay-button color (iOS `Color(.systemIndigo)`). */
val DefaultStripeApmPrimaryColor: Color = Color(0xFF5856D6)

/** Default PaymentSheet background (iOS `Color(.systemBackground)` on light). */
val DefaultStripeApmBackgroundColor: Color = Color(0xFFFFFBFE)
