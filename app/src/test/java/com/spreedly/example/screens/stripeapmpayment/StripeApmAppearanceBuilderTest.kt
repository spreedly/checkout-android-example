package com.spreedly.example.screens.stripeapmpayment

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StripeApmAppearanceBuilderTest {

    @Test
    fun `buildStripeAppearanceConfig returns null when custom appearance disabled`() {
        val result = buildStripeAppearanceConfig(
            useCustomAppearance = false,
            primaryColor = Color.Red,
            backgroundColor = Color.White,
            buttonBackgroundColor = Color.Blue,
            buttonTextColor = Color.White,
            cornerRadiusDp = 10f,
        )
        assertNull(result)
    }

    @Test
    fun `buildStripeAppearanceConfig maps colors shapes and primary button`() {
        val primary = Color(0xFF5856D6)
        val surface = Color(0xFFFFFBFE)
        val buttonBg = Color(0xFF5856D6)
        val buttonText = Color.White

        val result = buildStripeAppearanceConfig(
            useCustomAppearance = true,
            primaryColor = primary,
            backgroundColor = surface,
            buttonBackgroundColor = buttonBg,
            buttonTextColor = buttonText,
            cornerRadiusDp = 10f,
        )

        requireNotNull(result)
        assertEquals(10f, result.shapes?.cornerRadiusDp)
        assertEquals(1f, result.shapes?.borderStrokeWidthDp)
        assertEquals(primary.toArgb(), result.colors?.primary)
        assertEquals(surface.toArgb(), result.colors?.surface)
        assertEquals(buttonBg.toArgb(), result.primaryButton?.background)
        assertEquals(buttonText.toArgb(), result.primaryButton?.onBackground)
        assertEquals(10f, result.primaryButton?.cornerRadiusDp)
        assertEquals(52f, result.primaryButton?.heightDp)
    }
}
