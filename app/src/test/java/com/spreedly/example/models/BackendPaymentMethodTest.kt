package com.spreedly.example.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BackendPaymentMethod model.
 *
 * Tests the handling of different payment method types and conversion to SavedPaymentMethod.
 */
class BackendPaymentMethodTest {
    @Test
    fun `isCreditCard should return true for credit card payment methods`() {
        // Given
        val creditCardPaymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "credit_card",
            lastFourDigits = "4242",
            cardType = "visa",
            fullName = "John Doe",
        )

        // When
        val result = creditCardPaymentMethod.isCreditCard()

        // Then
        assertTrue("Credit card payment method should return true", result)
    }

    @Test
    fun `isCreditCard should return false for non-credit card payment methods`() {
        // Given
        val nonCreditCardPaymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "sprel",
            fullName = "Elon Musk",
        )

        // When
        val result = nonCreditCardPaymentMethod.isCreditCard()

        // Then
        assertFalse("Non-credit card payment method should return false", result)
    }

    @Test
    fun `toSavedPaymentMethod should convert credit card payment method successfully`() {
        // Given
        val creditCardPaymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "credit_card",
            lastFourDigits = "4242",
            cardType = "visa",
            fullName = "John Doe",
            month = 12,
            year = 2025,
            address1 = "123 Main St",
            city = "San Francisco",
            state = "CA",
            zip = "94102",
        )

        // When
        val savedPaymentMethod = creditCardPaymentMethod.toSavedPaymentMethod()

        // Then
        assertEquals("test-token", savedPaymentMethod.token)
        assertEquals("4242", savedPaymentMethod.lastFourDigits)
        assertEquals("visa", savedPaymentMethod.cardType)
        assertEquals("John Doe", savedPaymentMethod.cardholderName)
        assertEquals(12, savedPaymentMethod.expiryMonth)
        assertEquals(2025, savedPaymentMethod.expiryYear)
        assertEquals("123 Main St", savedPaymentMethod.addressLine1)
        assertEquals("San Francisco", savedPaymentMethod.city)
        assertEquals("CA", savedPaymentMethod.state)
        assertEquals("94102", savedPaymentMethod.zip)
    }

    @Test
    fun `toSavedPaymentMethod should throw exception for non-credit card payment methods`() {
        // Given
        val nonCreditCardPaymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "sprel",
            fullName = "Elon Musk",
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            nonCreditCardPaymentMethod.toSavedPaymentMethod()
        }

        assertTrue(
            "Exception message should mention payment method type",
            exception.message?.contains("sprel") == true,
        )
    }

    @Test
    fun `toSavedPaymentMethod should throw exception when lastFourDigits is missing`() {
        // Given
        val paymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "credit_card",
            lastFourDigits = null,
            cardType = "visa",
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            paymentMethod.toSavedPaymentMethod()
        }

        assertTrue(
            "Exception message should mention lastFourDigits",
            exception.message?.contains("lastFourDigits") == true,
        )
    }

    @Test
    fun `toSavedPaymentMethod should throw exception when cardType is missing`() {
        // Given
        val paymentMethod = BackendPaymentMethod(
            token = "test-token",
            paymentMethodType = "credit_card",
            lastFourDigits = "4242",
            cardType = null,
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            paymentMethod.toSavedPaymentMethod()
        }

        assertTrue(
            "Exception message should mention cardType",
            exception.message?.contains("cardType") == true,
        )
    }
}
