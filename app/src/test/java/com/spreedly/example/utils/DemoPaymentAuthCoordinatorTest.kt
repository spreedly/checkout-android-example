package com.spreedly.example.utils

import android.content.Context
import com.spreedly.example.AuthService
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.AuthParamsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DemoPaymentAuthCoordinatorTest {
    private lateinit var authService: AuthService
    private lateinit var sdkSessionManager: SdkSessionManager
    private lateinit var coordinator: DemoPaymentAuthCoordinator
    private lateinit var sdk: Spreedly
    private lateinit var context: Context

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
        sdkSessionManager = SdkSessionManager(authService)
        coordinator = DemoPaymentAuthCoordinator(sdkSessionManager)
        sdk = mockk(relaxed = true)
        context = mockk(relaxed = true)
        coEvery { authService.getAuthParams() } returns Result.success(authParams)
        every { sdk.isInitialized } returnsMany listOf(false, true, true, true, true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `refreshAuthIfNeeded fetches auth when SDK is not initialized`() = runTest {
        val result = coordinator.refreshAuthIfNeeded(sdk, context, "env-key")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authService.getAuthParams() }
        assertFalse(coordinator.isAuthConsumedForTests())
    }

    @Test
    fun `refreshAuthIfNeeded skips network when auth is still fresh`() = runTest {
        every { sdk.isInitialized } returns true

        val result = coordinator.refreshAuthIfNeeded(sdk, context, "env-key")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { authService.getAuthParams() }
    }

    @Test
    fun `refreshAuthIfNeeded fetches auth after previous payment consumed it`() = runTest {
        every { sdk.isInitialized } returns true
        coordinator.markAuthConsumed()

        val result = coordinator.refreshAuthIfNeeded(sdk, context, "env-key")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authService.getAuthParams() }
    }

    @Test
    fun `concurrent refresh calls only fetch auth once`() = runTest {
        var initialized = false
        every { sdk.isInitialized } answers { initialized }
        coEvery { authService.getAuthParams() } answers {
            initialized = true
            Result.success(authParams)
        }

        awaitAll(
            async { coordinator.refreshAuthIfNeeded(sdk, context, "env-key") },
            async { coordinator.refreshAuthIfNeeded(sdk, context, "env-key") },
        )

        coVerify(exactly = 1) { authService.getAuthParams() }
    }

    @Test
    fun `concurrent refresh waits for in-flight auth fetch`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var initialized = false
        every { sdk.isInitialized } answers { initialized }
        coEvery { authService.getAuthParams() } coAnswers {
            gate.await()
            initialized = true
            Result.success(authParams)
        }

        val first = async { coordinator.refreshAuthIfNeeded(sdk, context, "env-key") }
        delay(20)
        val second = async { coordinator.refreshAuthIfNeeded(sdk, context, "env-key") }
        gate.complete(Unit)
        awaitAll(first, second)

        coVerify(exactly = 1) { authService.getAuthParams() }
    }
}
