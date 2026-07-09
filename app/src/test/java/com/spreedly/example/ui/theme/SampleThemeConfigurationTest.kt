package com.spreedly.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.ui.theme.SpreedlyColors
import com.spreedly.ui.theme.SpreedlyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SampleThemePresetsTest {
    @Test
    fun `should return null when custom theme is disabled`() {
        val theme =
            SampleThemePresets.resolveTheme(
                preset = SampleThemePreset.BLUE,
                isDarkMode = false,
                useCustomTheme = false,
            )

        assertNull(theme)
    }

    @Test
    fun `should return blue light theme when blue preset selected in light mode`() {
        val theme =
            SampleThemePresets.resolveTheme(
                preset = SampleThemePreset.BLUE,
                isDarkMode = false,
                useCustomTheme = true,
            )

        assertEquals(Color.Blue, theme?.colors?.primary)
        assertEquals(Color(0xFFE3F2FD), theme?.colors?.surface)
    }

    @Test
    fun `should return blue dark theme when blue preset selected in dark mode`() {
        val theme =
            SampleThemePresets.resolveTheme(
                preset = SampleThemePreset.BLUE,
                isDarkMode = true,
                useCustomTheme = true,
            )

        assertEquals(Color(0xFF1C1C1E), theme?.colors?.surface)
        assertEquals(Color.White, theme?.colors?.text)
    }
}

class SplFieldConfigResolverTest {
    private val globalTheme =
        SpreedlyTheme(
            colors =
                SpreedlyColors(
                    primary = Color.Blue,
                    surface = Color.White,
                    text = Color.Black,
                    border = Color.Gray,
                    placeholder = Color.LightGray,
                ),
        )

    @Test
    fun `should return global config for non-target fields`() {
        val config =
            SplFieldConfigResolver.resolve(
                formFieldType = FormFieldType.EXPIRY_DATE(true),
                globalTheme = globalTheme,
                overrideTarget = SplFieldTarget.CARD_NUMBER,
                overrides = SplFieldStyleOverrides(primaryColor = Color.Red),
            )

        assertEquals(Color.Blue, config?.primaryColor)
    }

    @Test
    fun `should apply overrides on target field while keeping other properties`() {
        val config =
            SplFieldConfigResolver.resolve(
                formFieldType = FormFieldType.CARD(true),
                globalTheme = globalTheme,
                overrideTarget = SplFieldTarget.CARD_NUMBER,
                overrides =
                    SplFieldStyleOverrides(
                        primaryColor = Color.Red,
                        placeholderColor = Color.Magenta,
                    ),
            )

        assertEquals(Color.Red, config?.primaryColor)
        assertEquals(Color.Magenta, config?.placeholderColor)
        assertEquals(Color.White, config?.fieldBackgroundColor)
    }

    @Test
    fun `should use default base when only field override is enabled`() {
        val config =
            SplFieldConfigResolver.resolve(
                formFieldType = FormFieldType.CVV(true),
                globalTheme = null,
                overrideTarget = SplFieldTarget.CVV,
                overrides = SplFieldStyleOverrides(textColor = Color.Red),
            )

        assertEquals(Color.Red, config?.textColor)
    }
}

class ThemeConfigurationControllerTest {
    @Test
    fun `should clear field overrides when custom theme disabled`() {
        val controller = ThemeConfigurationController()
        controller.setFieldOverrideTarget(SplFieldTarget.CVV)
        controller.updateFieldOverrides(SplFieldStyleOverrides(textColor = Color.Red))

        controller.setUseCustomTheme(false)

        assertEquals(SplFieldTarget.NONE, controller.fieldOverrideTarget.value)
        assertEquals(SplFieldStyleOverrides(), controller.fieldOverrides.value)
    }

    @Test
    fun `should resolve payment sheet config only when custom theme enabled`() {
        val controller = ThemeConfigurationController()

        assertNull(controller.resolvePaymentSheetConfig(isDarkMode = false))

        controller.setUseCustomTheme(true)

        assertNotNull(controller.resolvePaymentSheetConfig(isDarkMode = false))
    }
}
