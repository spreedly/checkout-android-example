package com.spreedly.example.repository

import android.content.SharedPreferences
import com.spreedly.example.AuthService
import com.spreedly.example.models.PaymentMethodListResponse
import com.spreedly.example.models.SavedPaymentMethod
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentMethodRepositoryTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var authService: AuthService
    private lateinit var repository: PaymentMethodRepository

    @Before
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        authService = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        repository = PaymentMethodRepository(authService, sharedPreferences)
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `getSavedPaymentMethods should return empty list when no cached data`() {
        // Given - No cached data in SharedPreferences
        every { sharedPreferences.getString(any(), any()) } returns null

        // When
        val result = repository.getSavedPaymentMethods()

        // Then - Repository returns empty list (data comes from backend API now)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `initializeMockDataIfEmpty should add mock data when cache is empty`() {
        // Given - No cached data in SharedPreferences
        every { sharedPreferences.getString(any(), any()) } returns null

        // When
        repository.initializeMockDataIfEmpty()

        // Then - Should save mock data to SharedPreferences
        verify(atLeast = 1) { editor.putString(any(), any()) }
        verify(atLeast = 1) { editor.apply() }
    }

    @Test
    fun `deletePaymentMethod should remove payment method by token`() {
        // Given
        val token = "test-token"
        val savedMethods = listOf(
            SavedPaymentMethod(
                token = token,
                lastFourDigits = "4242",
                cardType = "visa",
                cardholderName = "John Doe",
                expiryMonth = 12,
                expiryYear = 25,
            ),
        )
        every { sharedPreferences.getString(any(), any()) } returns
            """[{"token":"$token","lastFourDigits":"4242","cardType":"visa"}]"""

        // When
        repository.deletePaymentMethod(token)

        // Then
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }

    @Test
    fun `SavedPaymentMethod getFormattedCardType should capitalize card type`() {
        // Given
        val savedMethod = SavedPaymentMethod(
            token = "test-token",
            lastFourDigits = "4242",
            cardType = "visa",
        )

        // When
        val result = savedMethod.getFormattedCardType()

        // Then
        assertEquals("Visa", result)
    }

    @Test
    fun `SavedPaymentMethod getFormattedExpiry should format expiry date correctly`() {
        // Given
        val savedMethod = SavedPaymentMethod(
            token = "test-token",
            lastFourDigits = "4242",
            cardType = "visa",
            expiryMonth = 12,
            expiryYear = 25,
        )

        // When
        val result = savedMethod.getFormattedExpiry()

        // Then
        assertEquals("12/25", result)
    }

    @Test
    fun `SavedPaymentMethod getFormattedExpiry should return null when expiry is missing`() {
        // Given
        val savedMethod = SavedPaymentMethod(
            token = "test-token",
            lastFourDigits = "4242",
            cardType = "visa",
        )

        // When
        val result = savedMethod.getFormattedExpiry()

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `fetchAndSyncPaymentMethods should return cached data on network failure`() = runTest {
        // Given -- cache has data, network fails
        every { sharedPreferences.getString(any(), any()) } returns
            """[{"token":"t1","lastFourDigits":"4242","cardType":"visa"}]"""
        coEvery { authService.getPaymentMethods() } returns
            kotlin.Result.failure(RuntimeException("network"))

        // When
        val result = repository.fetchAndSyncPaymentMethods()

        // Then -- falls back to cache
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `fetchAndSyncPaymentMethods should propagate failure when cache is empty`() = runTest {
        // Given
        every { sharedPreferences.getString(any(), any()) } returns null
        coEvery { authService.getPaymentMethods() } returns
            kotlin.Result.failure(RuntimeException("offline"))

        // When
        val result = repository.fetchAndSyncPaymentMethods()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `retainPaymentMethod should return success on API success`() = runTest {
        // Given
        coEvery { authService.retainPaymentMethod("tok_1") } returns
            kotlin.Result.success(mockk(relaxed = true))

        // When
        val result = repository.retainPaymentMethod("tok_1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `retainPaymentMethod should return failure on API error`() = runTest {
        // Given
        coEvery { authService.retainPaymentMethod("tok_bad") } returns
            kotlin.Result.failure(RuntimeException("server error"))

        // When
        val result = repository.retainPaymentMethod("tok_bad")

        // Then
        assertTrue(result.isFailure)
    }
}
