package com.spreedly.example.validation

import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.CustomValidationResult
import com.spreedly.sdk.fieldValue
import com.spreedly.sdk.ui.OptionalFieldType

internal object DemoPresetFieldValidators {
    private val noSpecialCharactersPattern = Regex("""[^a-zA-Z0-9\s]""")

    private val onlyWhenUsFieldTypes =
        setOf(
            OptionalFieldType.ZIP_CODE,
            OptionalFieldType.STATE,
            OptionalFieldType.SHIPPING_ZIP,
            OptionalFieldType.SHIPPING_STATE,
        )

    fun onlyWhenUsAppliesTo(fieldType: OptionalFieldType): Boolean = fieldType in onlyWhenUsFieldTypes

    fun onlyWhenUsAppliesTo(field: DemoCoreNameField): Boolean = false

    fun shouldRegisterPreset(
        preset: DemoCustomValidationPreset,
        fieldType: OptionalFieldType,
    ): Boolean =
        shouldRegisterPresetInternal(preset) {
            onlyWhenUsAppliesTo(fieldType)
        }

    fun shouldRegisterPreset(
        preset: DemoCustomValidationPreset,
        field: DemoCoreNameField,
    ): Boolean =
        shouldRegisterPresetInternal(preset) {
            onlyWhenUsAppliesTo(field)
        }

    private fun shouldRegisterPresetInternal(
        preset: DemoCustomValidationPreset,
        onlyWhenUsApplies: () -> Boolean,
    ): Boolean =
        when {
            preset == DemoCustomValidationPreset.NONE -> false
            preset == DemoCustomValidationPreset.ONLY_WHEN_US -> onlyWhenUsApplies()
            else -> true
        }

    fun validate(
        preset: DemoCustomValidationPreset,
        fieldType: OptionalFieldType,
        value: String,
        fields: Map<AdditionalField, String>,
    ): CustomValidationResult =
        validateInternal(preset, value, fields) {
            onlyWhenUs(fieldType, value, fields)
        }

    fun validate(
        preset: DemoCustomValidationPreset,
        field: DemoCoreNameField,
        value: String,
        fields: Map<AdditionalField, String>,
    ): CustomValidationResult =
        validateInternal(preset, value, fields) {
            CustomValidationResult(isValid = true)
        }

    private fun validateInternal(
        preset: DemoCustomValidationPreset,
        value: String,
        fields: Map<AdditionalField, String>,
        onlyWhenUs: () -> CustomValidationResult,
    ): CustomValidationResult =
        when (preset) {
            DemoCustomValidationPreset.NONE -> CustomValidationResult(isValid = true)
            DemoCustomValidationPreset.MIN_LENGTH_3 -> minLength(value, min = 3)
            DemoCustomValidationPreset.NO_DIGITS -> noDigits(value)
            DemoCustomValidationPreset.DIGITS_ONLY -> digitsOnly(value)
            DemoCustomValidationPreset.NO_SPECIAL_CHARACTERS -> noSpecialCharacters(value)
            DemoCustomValidationPreset.ONLY_WHEN_US -> onlyWhenUs()
            DemoCustomValidationPreset.ALWAYS_INVALID ->
                CustomValidationResult(isValid = false, errorMessage = "This field is always invalid")
        }

    private fun onlyWhenUs(
        fieldType: OptionalFieldType,
        value: String,
        fields: Map<AdditionalField, String>,
    ): CustomValidationResult {
        val country = fields.fieldValue(countryFieldFor(fieldType)).trim().uppercase()
        if (country != "US") {
            return CustomValidationResult(isValid = true)
        }
        if (!onlyWhenUsAppliesTo(fieldType)) {
            return CustomValidationResult(isValid = true)
        }
        return when (fieldType) {
            OptionalFieldType.ZIP_CODE ->
                DemoHostedFieldValidators.validateZip(value, fields)
            OptionalFieldType.STATE ->
                DemoHostedFieldValidators.validateState(value, fields)
            OptionalFieldType.SHIPPING_ZIP ->
                DemoHostedFieldValidators.validateShippingZip(value, fields)
            OptionalFieldType.SHIPPING_STATE ->
                DemoHostedFieldValidators.validateShippingState(value, fields)
            else -> CustomValidationResult(isValid = true)
        }
    }

    private fun countryFieldFor(fieldType: OptionalFieldType): AdditionalField =
        when (fieldType) {
            OptionalFieldType.SHIPPING_ZIP,
            OptionalFieldType.SHIPPING_STATE,
            OptionalFieldType.SHIPPING_COUNTRY,
            OptionalFieldType.SHIPPING_CITY,
            OptionalFieldType.SHIPPING_ADDRESS_1,
            OptionalFieldType.SHIPPING_ADDRESS_2,
            OptionalFieldType.SHIPPING_PHONE_NUMBER,
            OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME,
            OptionalFieldType.SHIPPING_STREET,
            OptionalFieldType.SHIPPING_STREET_LINE_2,
            OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE,
            OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE,
            -> AdditionalField.SHIPPING_COUNTRY
            else -> AdditionalField.COUNTRY
        }

    private fun minLength(value: String, min: Int): CustomValidationResult {
        // Untouched field (raw value empty) is skipped — required-ness is a separate, built-in
        // SDK concern. A whitespace-only value is NOT untouched (HC-1901), so it must still be
        // measured against min: checking value.trim().isEmpty() here would conflate the two.
        if (value.isEmpty()) {
            return CustomValidationResult(isValid = true)
        }
        val trimmed = value.trim()
        val ok = trimmed.length >= min
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Must be at least $min characters",
        )
    }

    private fun noDigits(value: String): CustomValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return CustomValidationResult(isValid = true)
        }
        val ok = trimmed.none { it.isDigit() }
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Must not contain digits",
        )
    }

    private fun digitsOnly(value: String): CustomValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return CustomValidationResult(isValid = true)
        }
        val ok = trimmed.all { it.isDigit() }
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Must contain only digits",
        )
    }

    private fun noSpecialCharacters(value: String): CustomValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return CustomValidationResult(isValid = true)
        }
        val ok = !noSpecialCharactersPattern.containsMatchIn(trimmed)
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Must not contain special characters",
        )
    }
}
