package com.spreedly.example.utils

import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAuthRefreshPolicyTest {

    @Test
    fun `should refresh when SDK is not initialized`() {
        assertTrue(shouldRefreshAuthBeforePayment(isInitialized = false, authConsumed = false))
    }

    @Test
    fun `should not refresh on first payment when auth is unused`() {
        assertFalse(shouldRefreshAuthBeforePayment(isInitialized = true, authConsumed = false))
    }

    @Test
    fun `should refresh after auth was consumed`() {
        assertTrue(shouldRefreshAuthBeforePayment(isInitialized = true, authConsumed = true))
    }

    @Test
    fun `Processing submit result consumes auth`() {
        assertTrue(shouldMarkAuthConsumedAfterSubmit(PaymentProcessingResult.Processing))
    }

    @Test
    fun `ValidationFailed submit result does not consume auth`() {
        assertFalse(
            shouldMarkAuthConsumedAfterSubmit(
                PaymentProcessingResult.ValidationFailed(invalidFields = emptyList()),
            ),
        )
    }

    @Test
    fun `Completed payment result consumes auth`() {
        assertTrue(shouldMarkAuthConsumedAfterPaymentResult(PaymentResult.Completed(token = "tok")))
    }

    @Test
    fun `API failed payment result consumes auth`() {
        assertTrue(
            shouldMarkAuthConsumedAfterPaymentResult(
                PaymentResult.Failed(
                    message = "error",
                    errorType = PaymentResult.Failed.ErrorType.API_ERROR,
                ),
            ),
        )
    }

    @Test
    fun `validation failed payment result does not consume auth`() {
        assertFalse(
            shouldMarkAuthConsumedAfterPaymentResult(
                PaymentResult.Failed(
                    message = "bad zip",
                    errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                ),
            ),
        )
    }

    @Test
    fun `Canceled payment result does not consume auth`() {
        assertFalse(shouldMarkAuthConsumedAfterPaymentResult(PaymentResult.Canceled()))
    }
}
