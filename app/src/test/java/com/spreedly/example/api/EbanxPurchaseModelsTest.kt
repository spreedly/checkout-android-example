package com.spreedly.example.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for EBANX purchase models.
 *
 * Tests serialization behavior for EBANX-specific request models,
 * ensuring proper handling of optional fields like gateway_specific_fields.
 */
class EbanxPurchaseModelsTest {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `should serialize EbanxPurchaseTransactionBody with document`() {
        // Given
        val document = "853.513.468-93"
        val body = EbanxPurchaseTransactionBody(
            amount = 9900,
            currencyCode = "BRL",
            paymentMethodToken = "test_token_123",
            redirectUrl = "com.test.app.spreedlyoffsite://ebanx/checkout",
            callbackUrl = "https://callback.example.com/",
            gatewaySpecificFields = EbanxGatewaySpecificFields(
                ebanx = EbanxFields(document = document),
            ),
        )

        // When
        val serialized = json.encodeToString(body)

        // Then
        assertTrue("Should contain gateway_specific_fields", serialized.contains("gateway_specific_fields"))
        assertTrue("Should contain ebanx object", serialized.contains("\"ebanx\""))
        assertTrue("Should contain document", serialized.contains("\"document\":\"$document\""))
        assertTrue("Should contain currency_code BRL", serialized.contains("\"currency_code\":\"BRL\""))
        assertTrue("Should contain amount", serialized.contains("\"amount\":9900"))
    }

    @Test
    fun `should serialize EbanxPurchaseTransactionBody without gateway_specific_fields when document is null`() {
        // Given - OXXO scenario where document is not required
        val body = EbanxPurchaseTransactionBody(
            amount = 5000,
            currencyCode = "MXN",
            paymentMethodToken = "oxxo_token_456",
            redirectUrl = "com.test.app.spreedlyoffsite://ebanx/checkout",
            callbackUrl = "https://callback.example.com/",
            gatewaySpecificFields = null,
        )

        // When
        val serialized = json.encodeToString(body)

        // Then
        assertTrue("Should NOT contain gateway_specific_fields", !serialized.contains("gateway_specific_fields"))
        assertTrue("Should contain currency_code MXN", serialized.contains("\"currency_code\":\"MXN\""))
        assertTrue("Should contain channel app", serialized.contains("\"channel\":\"app\""))
    }

    @Test
    fun `should correctly set currency_code BRL in serialized output`() {
        // Given - Pix/Boleto/NuPay use BRL
        val body = EbanxPurchaseTransactionBody(
            amount = 4400,
            currencyCode = "BRL",
            paymentMethodToken = "pix_token",
            redirectUrl = "com.test.app.spreedlyoffsite://test",
        )

        // When
        val serialized = json.encodeToString(body)

        // Then
        assertTrue(serialized.contains("\"currency_code\":\"BRL\""))
    }

    @Test
    fun `should correctly set currency_code MXN in serialized output`() {
        // Given - OXXO uses MXN
        val body = EbanxPurchaseTransactionBody(
            amount = 10000,
            currencyCode = "MXN",
            paymentMethodToken = "oxxo_token",
            redirectUrl = "com.test.app.spreedlyoffsite://test",
        )

        // When
        val serialized = json.encodeToString(body)

        // Then
        assertTrue(serialized.contains("\"currency_code\":\"MXN\""))
    }

    @Test
    fun `should serialize full EbanxPurchaseTransactionRequest`() {
        // Given
        val request = EbanxPurchaseTransactionRequest(
            transaction = EbanxPurchaseTransactionBody(
                amount = 19900,
                currencyCode = "BRL",
                paymentMethodToken = "pm_token_789",
                redirectUrl = "com.test.app.spreedlyoffsite://redirect",
                callbackUrl = "https://callback.url",
                gatewaySpecificFields = EbanxGatewaySpecificFields(
                    ebanx = EbanxFields(document = "123.456.789-00"),
                ),
            ),
        )

        // When
        val serialized = json.encodeToString(request)

        // Then
        assertTrue("Should contain transaction wrapper", serialized.contains("\"transaction\""))
        assertTrue("Should contain all nested fields", serialized.contains("payment_method_token"))
    }

    @Test
    fun `should deserialize SpreedlyPurchaseResponse with EBANX transaction`() {
        // Given
        val responseJson = """
            {
                "transaction": {
                    "token": "txn_ebanx_123",
                    "state": "pending",
                    "succeeded": false,
                    "message": "EBANX transaction pending",
                    "payment_method": {
                        "token": "pm_ebanx_456",
                        "payment_method_type": "pix"
                    }
                }
            }
        """.trimIndent()

        // When
        val response = json.decodeFromString<SpreedlyPurchaseResponse>(responseJson)

        // Then
        assertNotNull(response.transaction)
        assertEquals("txn_ebanx_123", response.transaction?.token)
        assertEquals("pending", response.transaction?.state)
        assertEquals(false, response.transaction?.succeeded)
        assertEquals("EBANX transaction pending", response.transaction?.message)
        assertEquals("pm_ebanx_456", response.transaction?.paymentMethod?.token)
        assertEquals("pix", response.transaction?.paymentMethod?.paymentMethodType)
    }

    @Test
    fun `should deserialize SpreedlyPurchaseResponse with errors`() {
        // Given
        val responseJson = """
            {
                "errors": [
                    {
                        "attribute": "amount",
                        "key": "errors.invalid",
                        "message": "Amount must be positive"
                    }
                ]
            }
        """.trimIndent()

        // When
        val response = json.decodeFromString<SpreedlyPurchaseResponse>(responseJson)

        // Then
        assertNull(response.transaction)
        assertNotNull(response.errors)
        assertEquals(1, response.errors?.size)
        assertEquals("Amount must be positive", response.errors?.first()?.message)
    }

    @Test
    fun `should use default values for optional fields`() {
        // Given
        val body = EbanxPurchaseTransactionBody(
            amount = 100,
            currencyCode = "BRL",
            paymentMethodToken = "token",
            redirectUrl = "url",
        )

        // Then
        assertEquals("app", body.channel)
        assertEquals("https://example.com/webhook/callback", body.callbackUrl)
        assertNull(body.gatewaySpecificFields)
    }

    @Test
    fun `should create EbanxFields with document`() {
        // Given
        val cpf = "853.513.468-93"

        // When
        val fields = EbanxFields(document = cpf)

        // Then
        assertEquals(cpf, fields.document)
    }

    @Test
    fun `should create EbanxGatewaySpecificFields wrapping EbanxFields`() {
        // Given
        val cpf = "123.456.789-00"

        // When
        val gatewayFields = EbanxGatewaySpecificFields(
            ebanx = EbanxFields(document = cpf),
        )

        // Then
        assertEquals(cpf, gatewayFields.ebanx.document)
    }

    @Test
    fun `should deserialize SpreedlyPurchaseResponse with checkout_url`() {
        // Given
        val responseJson = """
            {
                "transaction": {
                    "token": "txn_offsite",
                    "state": "pending",
                    "succeeded": false,
                    "checkout_url": "https://gateway.example.com/checkout/abc"
                }
            }
        """.trimIndent()

        // When
        val response = json.decodeFromString<SpreedlyPurchaseResponse>(responseJson)

        // Then
        assertEquals("https://gateway.example.com/checkout/abc", response.transaction?.checkoutUrl)
    }

    @Test
    fun `should serialize BraintreeFields with paypal_flow_type`() {
        // Given
        val fields = BraintreeFields(paypalFlowType = "checkout")

        // When
        val serialized = json.encodeToString(fields)

        // Then
        assertTrue("Should contain paypal_flow_type", serialized.contains("paypal_flow_type"))
        assertTrue("Should contain checkout", serialized.contains("checkout"))
    }

    @Test
    fun `should use shared constants for default values`() {
        // Given
        val body = EbanxPurchaseTransactionBody(
            amount = 100,
            currencyCode = "BRL",
            paymentMethodToken = "token",
            redirectUrl = "url",
        )

        // Then
        assertEquals(DEFAULT_CALLBACK_URL, body.callbackUrl)
        assertEquals(DEFAULT_CHANNEL, body.channel)
    }
}
