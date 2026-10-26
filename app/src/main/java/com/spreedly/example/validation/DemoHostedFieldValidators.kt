package com.spreedly.example.validation

import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.CustomValidationResult
import com.spreedly.sdk.fieldValue

/**
 * Demo custom validators for US zip/state when country is US.
 *
 * Zip: `12345` or `12345-6789`. State: exactly 2 letters (US abbreviation).
 */
internal object DemoHostedFieldValidators {
    private val usZipPattern = Regex("""^\d{5}(-\d{4})?$""")

    internal fun validateShippingZip(value: String, fields: Map<AdditionalField, String>): CustomValidationResult =
        validateZip(
            value = value,
            fields = mapOf(AdditionalField.COUNTRY to fields.fieldValue(AdditionalField.SHIPPING_COUNTRY)),
        )

    internal fun validateShippingState(value: String, fields: Map<AdditionalField, String>): CustomValidationResult =
        validateState(
            value = value,
            fields = mapOf(AdditionalField.COUNTRY to fields.fieldValue(AdditionalField.SHIPPING_COUNTRY)),
        )

    internal fun validateZip(value: String, fields: Map<AdditionalField, String>): CustomValidationResult {
        val zip = value.trim()
        val country = fields.fieldValue(AdditionalField.COUNTRY).trim().uppercase()
        if (country != "US") return CustomValidationResult(isValid = true)
        if (zip.isEmpty()) {
            return CustomValidationResult(
                isValid = false,
                errorMessage = "ZIP is required for US addresses",
            )
        }
        val ok = usZipPattern.matches(zip)
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Enter a valid US ZIP code",
        )
    }

    internal fun validateState(value: String, fields: Map<AdditionalField, String>): CustomValidationResult {
        val country = fields.fieldValue(AdditionalField.COUNTRY).trim().uppercase()
        if (country != "US") return CustomValidationResult(isValid = true)
        val state = value.trim().uppercase()
        val ok = state.length == 2 && state.all { it.isLetter() }
        return CustomValidationResult(
            isValid = ok,
            errorMessage = if (ok) null else "Enter a valid 2 letter US state code",
        )
    }
}
