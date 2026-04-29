package com.spreedly.example.utils

import android.util.Log
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.sdk.ui.PaymentResult.Failed.ErrorType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentResultHandlerTest {
    private lateinit var repository: PaymentMethodRepository
    private lateinit var sdk: Spreedly
    private lateinit var handler: PaymentResultHandler
    private lateinit var resultFlow: MutableSharedFlow<PaymentResult>

    @After
    fun tearDown() = unmockkAll()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        repository = mockk(relaxed = true)
        sdk = mockk()
        resultFlow = MutableSharedFlow()
        every { sdk.paymentResultFlow } returns resultFlow
        handler = PaymentResultHandler(repository)
    }

    @Test
    fun `should invoke onCompleted when Completed result is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            var receivedToken: String? = null
            val job = handler.observeResults(
                sdk = sdk,
                scope = this,
                onCompleted = { receivedToken = it.token },
                onFailed = {},
                onCanceled = {},
            )

            resultFlow.emit(PaymentResult.Completed(token = "tok_abc"))
            assertEquals("tok_abc", receivedToken)
            job.cancel()
        }

    @Test
    fun `should invoke onFailed when Failed result is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            var receivedMessage: String? = null
            val job = handler.observeResults(
                sdk = sdk,
                scope = this,
                onCompleted = {},
                onFailed = { receivedMessage = it.message },
                onCanceled = {},
            )

            resultFlow.emit(PaymentResult.Failed(errorType = ErrorType.API_ERROR, message = "card declined"))
            assertEquals("card declined", receivedMessage)
            job.cancel()
        }

    @Test
    fun `should invoke onCanceled when Canceled result is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            var canceled = false
            val job = handler.observeResults(
                sdk = sdk,
                scope = this,
                onCompleted = {},
                onFailed = {},
                onCanceled = { canceled = true },
            )

            resultFlow.emit(PaymentResult.Canceled)
            assertTrue(canceled)
            job.cancel()
        }

    @Test
    fun `should ignore Initial result`() =
        runTest(UnconfinedTestDispatcher()) {
            var callbackInvoked = false
            val job = handler.observeResults(
                sdk = sdk,
                scope = this,
                onCompleted = { callbackInvoked = true },
                onFailed = { callbackInvoked = true },
                onCanceled = { callbackInvoked = true },
            )

            resultFlow.emit(PaymentResult.Initial)
            assertTrue(!callbackInvoked)
            job.cancel()
        }

    @Test
    fun `should handle multiple rapid emissions without dropping`() =
        runTest(UnconfinedTestDispatcher()) {
            val tokens = mutableListOf<String>()
            val job = handler.observeResults(
                sdk = sdk,
                scope = this,
                onCompleted = { tokens.add(it.token) },
                onFailed = {},
                onCanceled = {},
            )

            resultFlow.emit(PaymentResult.Completed(token = "tok_1"))
            resultFlow.emit(PaymentResult.Completed(token = "tok_2"))

            assertEquals(listOf("tok_1", "tok_2"), tokens)
            job.cancel()
        }

    @Test
    fun `retainIfNeeded should skip retain when shouldRetain is false`() = runTest {
        val completed = PaymentResult.Completed(token = "tok_123", shouldRetain = false)

        val result = handler.retainIfNeeded(completed)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
        coVerify(exactly = 0) { repository.retainPaymentMethod(any()) }
    }

    @Test
    fun `retainIfNeeded should call retain when shouldRetain is true`() = runTest {
        coEvery { repository.retainPaymentMethod("tok_123") } returns Result.success(true)
        val completed = PaymentResult.Completed(token = "tok_123", shouldRetain = true)

        val result = handler.retainIfNeeded(completed)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        coVerify { repository.retainPaymentMethod("tok_123") }
    }

    @Test
    fun `retainIfNeeded should propagate failure from repository`() = runTest {
        coEvery { repository.retainPaymentMethod(any()) } returns Result.failure(RuntimeException("API error"))
        val completed = PaymentResult.Completed(token = "tok_fail", shouldRetain = true)

        val result = handler.retainIfNeeded(completed)

        assertTrue(result.isFailure)
        assertEquals("API error", result.exceptionOrNull()?.message)
    }
}
