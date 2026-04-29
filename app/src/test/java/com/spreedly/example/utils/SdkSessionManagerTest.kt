package com.spreedly.example.utils

import android.content.Context
import com.spreedly.example.AuthService
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.SpreedlySDKInitOptions
import com.spreedly.sdk.models.AuthParamsResponse
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class SdkSessionManagerTest {
    private lateinit var authService: AuthService
    private lateinit var sdk: Spreedly
    private lateinit var context: Context
    private lateinit var manager: SdkSessionManager

    @Before
    fun setup() {
        authService = mockk()
        sdk = mockk(relaxed = true)
        context = mockk()
        manager = SdkSessionManager(authService)
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `should initialize SDK when auth params are fetched successfully`() = runTest {
        val authParams = AuthParamsResponse(
            nonce = "test-nonce",
            signature = "test-sig",
            certificateToken = "test-cert",
            timestamp = 1234567890L,
        )
        coEvery { authService.getAuthParams() } returns Result.success(authParams)

        val result = manager.initializeSdk(sdk, context, "env-key")

        assertTrue(result.isSuccess)
        val optionsSlot = slot<SpreedlySDKInitOptions>()
        verify { sdk.init(capture(optionsSlot)) }
        with(optionsSlot.captured) {
            assertEquals("test-nonce", nonce)
            assertEquals("test-sig", signature)
            assertEquals("test-cert", certificateToken)
            assertEquals("1234567890", timestamp)
            assertEquals("env-key", environmentKey)
            assertEquals(context, this.context)
            assertEquals(null, forterSiteId)
        }
    }

    @Test
    fun `should pass forterSiteId when provided`() = runTest {
        val authParams = AuthParamsResponse(
            nonce = "n",
            signature = "s",
            certificateToken = "c",
            timestamp = 1L,
        )
        coEvery { authService.getAuthParams() } returns Result.success(authParams)

        manager.initializeSdk(sdk, context, "env-key", forterSiteId = "forter-123")

        val optionsSlot = slot<SpreedlySDKInitOptions>()
        verify { sdk.init(capture(optionsSlot)) }
        assertEquals("forter-123", optionsSlot.captured.forterSiteId)
    }

    @Test
    fun `should return failure when auth params fetch fails`() = runTest {
        val error = RuntimeException("Network error")
        coEvery { authService.getAuthParams() } returns Result.failure(error)

        val result = manager.initializeSdk(sdk, context, "env-key")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
        verify(exactly = 0) { sdk.init(any()) }
    }

    @Test
    fun `should return failure when sdk init throws`() = runTest {
        val authParams = AuthParamsResponse(
            nonce = "n",
            signature = "s",
            certificateToken = "c",
            timestamp = 1L,
        )
        coEvery { authService.getAuthParams() } returns Result.success(authParams)
        io.mockk.every { sdk.init(any()) } throws IllegalStateException("Already initialized")

        val result = manager.initializeSdk(sdk, context, "env-key")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
