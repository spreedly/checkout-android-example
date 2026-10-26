package com.spreedly.example.utils

import android.content.Context
import com.spreedly.example.AuthService
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.AuthParamsResponse
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DemoSdkAuthSessionTest {
    private lateinit var authService: AuthService
    private lateinit var session: DemoSdkAuthSession
    private lateinit var sdk: Spreedly
    private lateinit var context: Context
    private val isInitializing = MutableStateFlow(false)

    private val authParams =
        AuthParamsResponse(
            nonce = "nonce",
            signature = "sig",
            certificateToken = "cert",
            timestamp = 123L,
        )

    @Before
    fun setUp() {
        authService = mockk()
        session = DemoSdkAuthSession(SdkSessionManager(authService))
        sdk = mockk(relaxed = true)
        context = mockk(relaxed = true)
        coEvery { authService.getAuthParams() } returns Result.success(authParams)
        every { sdk.isInitialized } returnsMany listOf(false, true, true, true, true, true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `ensureFreshAuth clears initializing flag when auth fetch fails`() = runTest {
        coEvery { authService.getAuthParams() } returns Result.failure(IllegalStateException("network down"))
        every { sdk.isInitialized } returns false

        assertFalse(
            session.ensureFreshAuth(
                sdk = sdk,
                context = context,
                environmentKey = "env",
                isInitializing = isInitializing,
                registerObserversIfNeeded = {},
            ),
        )

        assertFalse(isInitializing.value)
    }

    @Test
    fun `ensureFreshAuth registers observers only once until manual reinitialize`() = runTest {
        var observerCalls = 0
        val registerObservers: () -> Unit = {
            observerCalls++
        }

        assertTrue(
            session.ensureFreshAuth(
                sdk = sdk,
                context = context,
                environmentKey = "env",
                isInitializing = isInitializing,
                registerObserversIfNeeded = registerObservers,
            ),
        )
        assertTrue(
            session.ensureFreshAuth(
                sdk = sdk,
                context = context,
                environmentKey = "env",
                isInitializing = isInitializing,
                registerObserversIfNeeded = registerObservers,
            ),
        )

        assertFalse(isInitializing.value)
        assertEquals(1, observerCalls)
        coVerify(exactly = 1) { authService.getAuthParams() }

        session.prepareForManualReinitialize()
        every { sdk.isInitialized } returns true
        session.markAuthConsumedViaSession()

        assertTrue(
            session.ensureFreshAuth(
                sdk = sdk,
                context = context,
                environmentKey = "env",
                isInitializing = isInitializing,
                registerObserversIfNeeded = registerObservers,
            ),
        )
        assertEquals(2, observerCalls)
        coVerify(exactly = 2) { authService.getAuthParams() }
    }

    @Test
    fun `onPaymentSubmitResult marks auth consumed only when processing starts`() {
        session.onPaymentSubmitResult(PaymentProcessingResult.ValidationFailed(emptyList()))
        assertFalse(session.isAuthConsumedForTests())

        session.onPaymentSubmitResult(PaymentProcessingResult.Processing)
        assertTrue(session.isAuthConsumedForTests())
    }

    @Test
    fun `onTerminalPaymentResult marks auth consumed on API failure not cancel`() {
        session.onTerminalPaymentResult(PaymentResult.Canceled())
        assertFalse(session.isAuthConsumedForTests())

        session.onTerminalPaymentResult(
            PaymentResult.Failed(message = "bad", errorType = PaymentResult.Failed.ErrorType.API_ERROR),
        )
        assertTrue(session.isAuthConsumedForTests())
    }

    private fun DemoSdkAuthSession.markAuthConsumedViaSession() {
        onPaymentSubmitResult(PaymentProcessingResult.Processing)
    }
}
