package com.spreedly.example.screens.ebanxpayment

import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for EbanxConfigBuilder.
 *
 * Tests the per-provider configuration logic for EBANX payment methods.
 */
class EbanxConfigBuilderTest {
    private val testRedirectUrl = "com.test.app.spreedlyoffsite://ebanx/checkout"

    // ========================================================================
    // Currency Code Tests
    // ========================================================================

    @Test
    fun `should return BRL currency for PIX`() {
        // When
        val currency = EbanxConfigBuilder.currencyCode(OffsitePaymentMethodType.PIX)

        // Then
        assertEquals("BRL", currency)
    }

    @Test
    fun `should return BRL currency for BOLETO_BANCARIO`() {
        // When
        val currency = EbanxConfigBuilder.currencyCode(OffsitePaymentMethodType.BOLETO_BANCARIO)

        // Then
        assertEquals("BRL", currency)
    }

    @Test
    fun `should return BRL currency for NUPAY`() {
        // When
        val currency = EbanxConfigBuilder.currencyCode(OffsitePaymentMethodType.NUPAY)

        // Then
        assertEquals("BRL", currency)
    }

    @Test
    fun `should return MXN currency for OXXO`() {
        // When
        val currency = EbanxConfigBuilder.currencyCode(OffsitePaymentMethodType.OXXO)

        // Then
        assertEquals("MXN", currency)
    }

    // ========================================================================
    // Document Tests
    // ========================================================================

    @Test
    fun `should return document for PIX`() {
        // When
        val document = EbanxConfigBuilder.document(OffsitePaymentMethodType.PIX)

        // Then
        assertNotNull(document)
        assertEquals("853.513.468-93", document)
    }

    @Test
    fun `should return document for BOLETO_BANCARIO`() {
        // When
        val document = EbanxConfigBuilder.document(OffsitePaymentMethodType.BOLETO_BANCARIO)

        // Then
        assertNotNull(document)
        assertEquals("853.513.468-93", document)
    }

    @Test
    fun `should return document for NUPAY`() {
        // When
        val document = EbanxConfigBuilder.document(OffsitePaymentMethodType.NUPAY)

        // Then
        assertNotNull(document)
        assertEquals("853.513.468-93", document)
    }

    @Test
    fun `should return null document for OXXO`() {
        // When
        val document = EbanxConfigBuilder.document(OffsitePaymentMethodType.OXXO)

        // Then
        assertNull(document)
    }

    // ========================================================================
    // Build Config Tests - PIX
    // ========================================================================

    @Test
    fun `should build PIX config with BR country and document ID`() {
        // When
        val config = EbanxConfigBuilder.buildConfig(OffsitePaymentMethodType.PIX, testRedirectUrl)

        // Then
        assertEquals(OffsitePaymentMethodType.PIX, config.paymentMethodType)
        assertEquals("BR", config.country)
        assertNotNull(config.documentId)
        assertNotNull(config.address1)
        assertNotNull(config.city)
        assertNotNull(config.state)
        assertNotNull(config.zip)
    }

    // ========================================================================
    // Build Config Tests - BOLETO
    // ========================================================================

    @Test
    fun `should build BOLETO config with full address and document ID`() {
        // When
        val config = EbanxConfigBuilder.buildConfig(OffsitePaymentMethodType.BOLETO_BANCARIO, testRedirectUrl)

        // Then
        assertEquals(OffsitePaymentMethodType.BOLETO_BANCARIO, config.paymentMethodType)
        assertEquals("BR", config.country)
        assertNotNull(config.documentId)
        assertNotNull(config.address1)
        assertNotNull(config.city)
        assertNotNull(config.state)
        assertNotNull(config.zip)
        assertNotNull(config.email)
        assertNotNull(config.fullName)
        assertNotNull(config.phoneNumber)
    }

    // ========================================================================
    // Build Config Tests - OXXO
    // ========================================================================

    @Test
    fun `should build OXXO config with MX country and no document ID`() {
        // When
        val config = EbanxConfigBuilder.buildConfig(OffsitePaymentMethodType.OXXO, testRedirectUrl)

        // Then
        assertEquals(OffsitePaymentMethodType.OXXO, config.paymentMethodType)
        assertEquals("MX", config.country)
        assertNull(config.documentId)
        assertNotNull(config.address1)
        assertNotNull(config.city)
        assertNotNull(config.state)
        assertNotNull(config.zip)
    }

    // ========================================================================
    // Build Config Tests - NUPAY
    // ========================================================================

    @Test
    fun `should build NUPAY config with BR country and document ID but no address`() {
        // When
        val config = EbanxConfigBuilder.buildConfig(OffsitePaymentMethodType.NUPAY, testRedirectUrl)

        // Then
        assertEquals(OffsitePaymentMethodType.NUPAY, config.paymentMethodType)
        assertEquals("BR", config.country)
        assertNotNull(config.documentId)
        assertNotNull(config.email)
        assertNotNull(config.fullName)
        assertNotNull(config.phoneNumber)
        // NuPay doesn't require address fields
        assertNull(config.address1)
        assertNull(config.city)
        assertNull(config.state)
        assertNull(config.zip)
    }

    // ========================================================================
    // Redirect URL Tests
    // ========================================================================

    @Test
    fun `should set redirect URL for all providers`() {
        // Given
        val providers = listOf(
            OffsitePaymentMethodType.PIX,
            OffsitePaymentMethodType.BOLETO_BANCARIO,
            OffsitePaymentMethodType.OXXO,
            OffsitePaymentMethodType.NUPAY,
        )

        // When/Then
        providers.forEach { provider ->
            val config = EbanxConfigBuilder.buildConfig(provider, testRedirectUrl)
            assertEquals(testRedirectUrl, config.redirectUrl)
        }
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception for unsupported provider`() {
        // When - using a non-EBANX provider
        EbanxConfigBuilder.buildConfig(OffsitePaymentMethodType.PAYPAL, testRedirectUrl)
    }
}
