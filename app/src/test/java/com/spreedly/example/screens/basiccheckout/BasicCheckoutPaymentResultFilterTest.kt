package com.spreedly.example.screens.basiccheckout

import com.spreedly.sdk.ui.PaymentResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicCheckoutPaymentResultFilterTest {

    @Test
    fun `isClientSideFormValidationFailure true for exact SDK message`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE,
                originalError = Exception(CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE),
            )
        assertTrue(failed.isClientSideFormValidationFailure())
    }

    @Test
    fun `isClientSideFormValidationFailure false for trailing space`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "${CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE} ",
                originalError = Exception("x"),
            )
        assertFalse(failed.isClientSideFormValidationFailure())
    }

    @Test
    fun `isClientSideFormValidationFailure false for wrong casing`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE.uppercase(),
                originalError = Exception("x"),
            )
        assertFalse(failed.isClientSideFormValidationFailure())
    }

    @Test
    fun `isClientSideFormValidationFailure false for network style message`() {
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "Connection refused",
                originalError = Exception("x"),
            )
        assertFalse(failed.isClientSideFormValidationFailure())
    }
}
