package com.spreedly.example.ui.theme

import com.spreedly.hostedfields.ui.toCustomFieldsConfig
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.ui.theme.SpreedlyTheme

object SplFieldConfigResolver {
    fun resolve(
        formFieldType: FormFieldType,
        globalTheme: SpreedlyTheme?,
        overrideTarget: SplFieldTarget,
        overrides: SplFieldStyleOverrides,
    ): CustomFieldsConfig? {
        val targetType = overrideTarget.formFieldType
        val baseConfig = globalTheme?.toCustomFieldsConfig()

        if (targetType == null || !formFieldType.matchesSplFieldTarget(targetType)) {
            return baseConfig
        }

        val resolvedBase = baseConfig ?: SpreedlyTheme.Default.toCustomFieldsConfig()
        return resolvedBase.copy(
            primaryColor = overrides.primaryColor ?: resolvedBase.primaryColor,
            fieldBackgroundColor = overrides.fieldBackgroundColor ?: resolvedBase.fieldBackgroundColor,
            textColor = overrides.textColor ?: resolvedBase.textColor,
            placeholderColor = overrides.placeholderColor ?: resolvedBase.placeholderColor,
            formBorderColor = overrides.borderColor ?: resolvedBase.formBorderColor,
        )
    }
}
