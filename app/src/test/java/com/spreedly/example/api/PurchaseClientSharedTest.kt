package com.spreedly.example.api

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseClientSharedTest {

    // ========================================================================
    // purchaseJson configuration tests
    // ========================================================================

    @Test
    fun `purchaseJson should ignore unknown keys`() {
        // Given
        val json = """{"attribute": "amount", "message": "Invalid", "unknown_field": "ignored"}"""

        // When
        val error = purchaseJson.decodeFromString<PurchaseApiError>(json)

        // Then
        assertEquals("amount", error.attribute)
        assertEquals("Invalid", error.message)
    }

    @Test
    fun `purchaseJson should omit null values`() {
        // Given
        val error = PurchaseApiError(attribute = "amount", key = null, message = "Invalid")

        // When
        val json = purchaseJson.encodeToString(error)

        // Then
        assertFalse("Should omit null 'key' field", json.contains("\"key\""))
    }

    @Test
    fun `purchaseJson should encode default values`() {
        // Given
        val request = MerchantOffsitePurchaseRequest(
            gateway = "gw",
            paymentMethodToken = "pm",
            amount = 100,
            currencyCode = "USD",
            redirectUrl = "app://checkout",
        )

        // When
        val json = purchaseJson.encodeToString(request)

        // Then
        assertTrue("Should encode default callback_url", json.contains(DEFAULT_CALLBACK_URL))
        assertTrue("Should encode default channel", json.contains(DEFAULT_CHANNEL))
    }

    // ========================================================================
    // createPurchaseHttpClient() tests
    // ========================================================================

    @Test
    fun `createPurchaseHttpClient should create a closeable client`() {
        // Given/When
        val client = createPurchaseHttpClient("TestTag")

        // Then
        assertNotNull(client)
        client.close()
    }

    // ========================================================================
    // PurchaseApiError serialization tests
    // ========================================================================

    @Test
    fun `PurchaseApiError should serialize and deserialize round-trip`() {
        // Given
        val original = PurchaseApiError(
            attribute = "amount",
            key = "errors.invalid",
            message = "Amount must be positive",
        )

        // When
        val json = purchaseJson.encodeToString(original)
        val deserialized = purchaseJson.decodeFromString<PurchaseApiError>(json)

        // Then
        assertEquals(original, deserialized)
    }

    @Test
    fun `PurchaseApiError should deserialize from minimal JSON`() {
        // Given
        val json = """{"message": "Something went wrong"}"""

        // When
        val error = purchaseJson.decodeFromString<PurchaseApiError>(json)

        // Then
        assertEquals("Something went wrong", error.message)
        assertNull(error.attribute)
        assertNull(error.key)
    }

    @Test
    fun `PurchaseApiError should use snake_case SerialNames`() {
        // Given
        val json = """{"attribute": "payment_method_token", "key": "errors.blank", "message": "Can't be blank"}"""

        // When
        val error = purchaseJson.decodeFromString<PurchaseApiError>(json)

        // Then
        assertEquals("payment_method_token", error.attribute)
        assertEquals("errors.blank", error.key)
        assertEquals("Can't be blank", error.message)
    }

    // ========================================================================
    // PurchaseException hierarchy tests
    // ========================================================================

    @Test
    fun `ServerError should retain status code message and raw response`() {
        // Given/When
        val error = PurchaseException.ServerError(
            statusCode = 422,
            message = "Amount must be positive",
            rawResponse = """{"error": "bad"}""",
        )

        // Then
        assertEquals(422, error.statusCode)
        assertEquals("Amount must be positive", error.message)
        assertEquals("""{"error": "bad"}""", error.rawResponse)
    }

    @Test
    fun `NetworkError should preserve original exception as cause`() {
        // Given
        val cause = java.io.IOException("Connection refused")

        // When
        val error = PurchaseException.NetworkError(cause)

        // Then
        assertEquals(cause, error.originalError)
        assertEquals(cause, error.cause)
        assertTrue(error.message!!.contains("Connection refused"))
    }

    @Test
    fun `AuthenticationError should use provided message`() {
        // Given/When
        val error = PurchaseException.AuthenticationError("Missing credentials")

        // Then
        assertEquals("Missing credentials", error.message)
    }

    @Test
    fun `InvalidURL should include the URL in message`() {
        // Given/When
        val error = PurchaseException.InvalidURL("not://valid")

        // Then
        assertTrue(error.message!!.contains("not://valid"))
        assertEquals("not://valid", error.url)
    }

    @Test
    fun `EncodingError should preserve original exception as cause`() {
        // Given
        val cause = RuntimeException("Bad encoding")

        // When
        val error = PurchaseException.EncodingError(cause)

        // Then
        assertEquals(cause, error.originalError)
        assertEquals(cause, error.cause)
        assertTrue(error.message!!.contains("Bad encoding"))
    }

    @Test
    fun `all PurchaseException subtypes should have non-null messages`() {
        // Given
        val exceptions: List<PurchaseException> = listOf(
            PurchaseException.ServerError(500, "fail"),
            PurchaseException.NetworkError(RuntimeException("net")),
            PurchaseException.AuthenticationError("auth"),
            PurchaseException.InvalidURL("url"),
            PurchaseException.EncodingError(RuntimeException("enc")),
        )

        // Then
        exceptions.forEach { e ->
            assertNotNull("${e::class.simpleName} should have a non-null message", e.message)
        }
        assertEquals(5, exceptions.size)
    }

    // ========================================================================
    // Constants tests
    // ========================================================================

    @Test
    fun `DEFAULT_CALLBACK_URL should be a valid URL`() {
        assertTrue(DEFAULT_CALLBACK_URL.startsWith("https://"))
    }

    @Test
    fun `DEFAULT_CHANNEL should be app`() {
        assertEquals("app", DEFAULT_CHANNEL)
    }
}
