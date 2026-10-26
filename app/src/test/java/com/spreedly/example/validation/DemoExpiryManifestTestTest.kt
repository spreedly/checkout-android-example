package com.spreedly.example.validation

import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoExpiryManifestTestTest {
    @Test
    fun `invalid dual manifest fields include all expiry types`() {
        val fields = DemoExpiryManifestTest.invalidDualManifestFields()
        assertTrue(fields.any { it is FormFieldType.EXPIRY_DATE })
        assertTrue(fields.any { it is FormFieldType.MONTH })
        assertTrue(fields.any { it is FormFieldType.YEAR })
        assertTrue(DemoExpiryManifestTest.isInvalidDualManifest(fields))
    }

    @Test
    fun `default core expiry is valid manifest`() {
        val fields = DemoExpiryManifestTest.defaultCoreExpiryFields()
        assertFalse(DemoExpiryManifestTest.isInvalidDualManifest(fields))
    }

    @Test
    fun `checkout blocked message explains FA10 dual manifest`() {
        val message =
            DemoExpiryManifestTest.checkoutBlockedMessage(
                DemoExpiryManifestTest.invalidDualManifestFields(),
            )
        assertEquals(DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE, message)
        assertTrue(message.contains("Payment was not submitted"))
    }

    @Test
    fun `checkout blocked message uses generic copy for non dual manifest fields`() {
        assertEquals(
            DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            DemoExpiryManifestTest.checkoutBlockedMessage(listOf(FormFieldType.CARD(true))),
        )
    }

    @Test
    fun `validation failed message maps dual manifest failures`() {
        val result =
            PaymentProcessingResult.ValidationFailed(
                invalidFields = DemoExpiryManifestTest.invalidDualManifestFields(),
            )
        assertEquals(
            DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE,
            DemoExpiryManifestTest.validationFailedMessage(result.invalidFields),
        )
    }

    @Test
    fun `demo submit form fields match payments core FA10 integration fixture`() {
        val expected = DemoExpiryManifestTest.demoSubmitFormFields()
        assertEquals(
            expected.filter { it is FormFieldType.CARD || it is FormFieldType.CVV || it is FormFieldType.EXPIRY_DATE || it is FormFieldType.MONTH || it is FormFieldType.YEAR },
            expected,
        )
        assertTrue(DemoExpiryManifestTest.isInvalidDualManifest(expected))
    }

    @Test
    fun `skip pre submit field gate only when demo switch enabled`() {
        assertFalse(DemoExpiryManifestTest.skipPreSubmitFieldValidityGateForDemo(false))
        assertTrue(DemoExpiryManifestTest.skipPreSubmitFieldValidityGateForDemo(true))
    }

    @Test
    fun `isDemoExpiryManifestConflictAsyncFailure matches throwable-only client validation`() {
        val formFields = DemoExpiryManifestTest.invalidDualManifestFields()
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )
        assertTrue(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = true,
                formFields = formFields,
                result = failed,
            ),
        )
    }

    @Test
    fun `isDemoExpiryManifestConflictAsyncFailure rejects unrelated UNKNOWN_ERROR with different message`() {
        // An unrelated failure (e.g. network timeout wrapped as UNKNOWN_ERROR) must NOT be
        // misclassified as the FA10 checkout-blocked scenario even when the demo switch is on.
        val formFields = DemoExpiryManifestTest.invalidDualManifestFields()
        val unrelatedFailure =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "Some unrelated runtime failure",
            )
        assertFalse(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = true,
                formFields = formFields,
                result = unrelatedFailure,
            ),
        )
    }

    @Test
    fun `isDemoExpiryManifestConflictAsyncFailure rejects UNKNOWN_ERROR with null message`() {
        val formFields = DemoExpiryManifestTest.invalidDualManifestFields()
        val nullMessageFailure =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = null,
            )
        assertFalse(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = true,
                formFields = formFields,
                result = nullMessageFailure,
            ),
        )
    }

    @Test
    fun `isDemoExpiryManifestConflictAsyncFailure rejects custom validator failures with validationErrors`() {
        val formFields = DemoExpiryManifestTest.invalidDualManifestFields()
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
                validationErrors =
                    listOf(
                        PaymentResult.Failed.ValidationError(
                            fieldName = "zip",
                            errorKey = "invalid",
                            errorMessage = null,
                        ),
                    ),
            )
        assertFalse(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = true,
                formFields = formFields,
                result = failed,
            ),
        )
    }

    @Test
    fun `isDemoExpiryManifestConflictAsyncFailure requires demo switch and invalid manifest`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )
        assertFalse(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = false,
                formFields = DemoExpiryManifestTest.invalidDualManifestFields(),
                result = failed,
            ),
        )
        assertFalse(
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = true,
                formFields = DemoExpiryManifestTest.defaultCoreExpiryFields(),
                result = failed,
            ),
        )
    }

    @Test
    fun `payment failed companion message matches sdk client validation copy`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )
        assertEquals(
            DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            failed.message,
        )
    }
}
