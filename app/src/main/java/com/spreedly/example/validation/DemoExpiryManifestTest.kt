package com.spreedly.example.validation

import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult

/**
 * Demo-only FA10 mutual-exclusion testing for Custom Text Fields.
 *
 * When enabled, the screen mounts [FormFieldType.EXPIRY_DATE] together with
 * [FormFieldType.MONTH] and [FormFieldType.YEAR]. Submit validation must fail before tokenize.
 */
object DemoExpiryManifestTest {
    /** Compose UI test tag for the FA10 demo switch row. */
    const val SWITCH_TEST_TAG = "demo_fa10_expiry_conflict_switch"

    const val SWITCH_LABEL = "Demo: conflicting expiry fields"
    const val SWITCH_DESCRIPTION =
        "Shows MM/YY and separate Month + Year together. Checkout is blocked — only one expiry style is allowed."
    /** Matches [com.spreedly.sdk.SpreedlyPaymentOperations] client-validation failure copy. */
    const val SDK_CLIENT_VALIDATION_FAILED_MESSAGE = "Payment form validation failed"

    const val CHECKOUT_BLOCKED_MESSAGE =
        "Checkout blocked: MM/YY cannot be used with separate Month and Year fields. " +
            "Choose one expiry style. Payment was not submitted."

    fun defaultCoreExpiryFields(required: Boolean = true): List<FormFieldType> =
        listOf(FormFieldType.EXPIRY_DATE(required))

    fun invalidDualManifestFields(required: Boolean = true): List<FormFieldType> =
        listOf(
            FormFieldType.EXPIRY_DATE(required),
            FormFieldType.MONTH(required),
            FormFieldType.YEAR(required),
        )

    /** Full Custom Text Fields submit manifest when the FA10 demo switch is on (CARD + triple expiry + CVV). */
    fun demoSubmitFormFields(required: Boolean = true): List<FormFieldType> =
        listOf(FormFieldType.CARD(required)) +
            invalidDualManifestFields(required) +
            listOf(FormFieldType.CVV(required))

    fun coreExpiryFields(
        demoInvalidExpiryManifest: Boolean,
        required: Boolean = true,
    ): List<FormFieldType> =
        if (demoInvalidExpiryManifest) {
            invalidDualManifestFields(required)
        } else {
            defaultCoreExpiryFields(required)
        }

    fun isInvalidDualManifest(formFields: List<FormFieldType>): Boolean {
        val hasExpiryDate = formFields.any { it is FormFieldType.EXPIRY_DATE }
        val hasMonth = formFields.any { it is FormFieldType.MONTH }
        val hasYear = formFields.any { it is FormFieldType.YEAR }
        val hasYearSecondary = formFields.any { it is FormFieldType.YEAR_SECONDARY }
        return hasExpiryDate && (hasMonth || hasYear || hasYearSecondary)
    }

    fun checkoutBlockedMessage(formFields: List<FormFieldType>): String =
        if (isInvalidDualManifest(formFields)) {
            CHECKOUT_BLOCKED_MESSAGE
        } else {
            SDK_CLIENT_VALIDATION_FAILED_MESSAGE
        }

    fun validationFailedMessage(invalidFields: List<FormFieldType>): String =
        checkoutBlockedMessage(invalidFields)

    /**
     * Demo-only: when the FA10 switch is on, skip [Spreedly.areAllFieldsValid] before submit so
     * [createCreditCard] receives the invalid manifest and the SDK's submit-time
     * [ExpiryFieldConfigurationValidator] can reject it. Never use outside this Example screen.
     */
    fun skipPreSubmitFieldValidityGateForDemo(demoInvalidExpiryManifest: Boolean): Boolean =
        demoInvalidExpiryManifest

    /**
     * Typed async failure classification for the FA10 demo.
     *
     * Expiry manifest rejection uses [SpreedlyPaymentManager.notifyPaymentFailed] with a plain [Exception],
     * so [PaymentResult.Failed.validationErrors] stays empty and the message matches
     * [SDK_CLIENT_VALIDATION_FAILED_MESSAGE]. Requiring the exact message avoids misclassifying an
     * unrelated [PaymentResult.Failed.ErrorType.UNKNOWN_ERROR] that happens while the switch is on.
     */
    fun isDemoExpiryManifestConflictAsyncFailure(
        demoInvalidExpiryManifest: Boolean,
        formFields: List<FormFieldType>,
        result: PaymentResult.Failed,
    ): Boolean {
        if (!demoInvalidExpiryManifest || !isInvalidDualManifest(formFields)) {
            return false
        }
        return result.errorType == PaymentResult.Failed.ErrorType.UNKNOWN_ERROR &&
            result.message == SDK_CLIENT_VALIDATION_FAILED_MESSAGE &&
            result.apiError == null &&
            result.validationErrors.isEmpty()
    }
}
