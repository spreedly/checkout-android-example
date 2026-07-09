package com.spreedly.example.ui.theme

import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.PaymentSheetConfig
import com.spreedly.ui.theme.SpreedlyTheme
import com.spreedly.ui.theme.toPaymentSheetConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeConfigurationController {
    private val _useCustomTheme = MutableStateFlow(false)
    val useCustomTheme: StateFlow<Boolean> = _useCustomTheme.asStateFlow()

    private val _selectedPreset = MutableStateFlow(SampleThemePreset.DEFAULT)
    val selectedPreset: StateFlow<SampleThemePreset> = _selectedPreset.asStateFlow()

    private val _fieldOverrideTarget = MutableStateFlow(SplFieldTarget.NONE)
    val fieldOverrideTarget: StateFlow<SplFieldTarget> = _fieldOverrideTarget.asStateFlow()

    private val _fieldOverrides = MutableStateFlow(SplFieldStyleOverrides())
    val fieldOverrides: StateFlow<SplFieldStyleOverrides> = _fieldOverrides.asStateFlow()

    fun setUseCustomTheme(enabled: Boolean) {
        _useCustomTheme.value = enabled
        if (!enabled) {
            _selectedPreset.value = SampleThemePreset.DEFAULT
            clearFieldOverrides()
        } else if (_selectedPreset.value == SampleThemePreset.DEFAULT) {
            _selectedPreset.value = SampleThemePreset.BLUE
        }
    }

    fun setPreset(preset: SampleThemePreset) {
        _selectedPreset.value = preset
        _useCustomTheme.value = true
    }

    fun setFieldOverrideTarget(target: SplFieldTarget) {
        _fieldOverrideTarget.value = target
        if (target == SplFieldTarget.NONE) {
            _fieldOverrides.value = SplFieldStyleOverrides()
        }
    }

    fun updateFieldOverrides(overrides: SplFieldStyleOverrides) {
        _fieldOverrides.value = overrides
    }

    fun clearFieldOverrides() {
        _fieldOverrideTarget.value = SplFieldTarget.NONE
        _fieldOverrides.value = SplFieldStyleOverrides()
    }

    fun resolveTheme(isDarkMode: Boolean): SpreedlyTheme? =
        SampleThemePresets.resolveTheme(
            preset = _selectedPreset.value,
            isDarkMode = isDarkMode,
            useCustomTheme = _useCustomTheme.value,
        )

    fun resolveFieldConfig(
        formFieldType: FormFieldType,
        isDarkMode: Boolean,
    ): CustomFieldsConfig? {
        if (!_useCustomTheme.value && _fieldOverrideTarget.value == SplFieldTarget.NONE) {
            return null
        }
        return SplFieldConfigResolver.resolve(
            formFieldType = formFieldType,
            globalTheme = resolveTheme(isDarkMode),
            overrideTarget = _fieldOverrideTarget.value,
            overrides = _fieldOverrides.value,
        )
    }

    fun resolvePaymentSheetConfig(isDarkMode: Boolean): PaymentSheetConfig? {
        val theme = resolveTheme(isDarkMode)
        return theme?.toPaymentSheetConfig()
    }

    fun applyGlobalTheme(
        sdk: Spreedly,
        isDarkMode: Boolean,
    ) {
        if (!_useCustomTheme.value) {
            sdk.setGlobalTheme(SpreedlyTheme.Default)
            return
        }
        resolveTheme(isDarkMode)?.let { sdk.setGlobalTheme(it) }
    }
}
