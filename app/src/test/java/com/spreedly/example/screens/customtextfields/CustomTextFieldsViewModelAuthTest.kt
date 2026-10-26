package com.spreedly.example.screens.customtextfields

import com.spreedly.example.utils.DemoSdkAuthSession
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTextFieldsViewModelAuthTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `auth session marks consumed when tokenization starts`() {
        val session = DemoSdkAuthSession(SdkSessionManager(mockk(relaxed = true)))
        assertFalse(session.isAuthConsumedForTests())

        session.onPaymentSubmitResult(PaymentProcessingResult.Processing)

        assertTrue(session.isAuthConsumedForTests())
        assertTrue(session.shouldRefreshForTests(isInitialized = true))
    }

    @Test
    fun `auth session does not mark consumed on validation failure before submit`() {
        val session = DemoSdkAuthSession(SdkSessionManager(mockk(relaxed = true)))

        session.onPaymentSubmitResult(PaymentProcessingResult.ValidationFailed(emptyList()))

        assertFalse(session.isAuthConsumedForTests())
        assertFalse(session.shouldRefreshForTests(isInitialized = true))
    }

    @Test
    fun `auth session marks consumed on terminal API failure`() {
        val session = DemoSdkAuthSession(SdkSessionManager(mockk(relaxed = true)))

        session.onTerminalPaymentResult(
            PaymentResult.Failed(message = "fail", errorType = PaymentResult.Failed.ErrorType.API_ERROR),
        )

        assertTrue(session.isAuthConsumedForTests())
    }
}
