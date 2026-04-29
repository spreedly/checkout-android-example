package com.spreedly.example.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PurchaseAPIClientTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun createClient(engine: MockEngine): PurchaseAPIClient {
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(purchaseJson) }
            expectSuccess = false
        }
        return PurchaseAPIClient(
            config = PurchaseAPIClient.ServerConfig(baseURL = "https://merchant.example.com"),
            client = httpClient,
        )
    }

    // ========================================================================
    // purchase() tests
    // ========================================================================

    @Test
    fun `purchase should call correct endpoint`() = runTest {
        // Given
        val engine = MockEngine { request ->
            assertEquals(
                "https://merchant.example.com/api/v1/purchase",
                request.url.toString(),
            )
            respond(PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.purchase(
            paymentMethodToken = "pm_token",
            amount = 4400,
            redirectUrl = TEST_REDIRECT_URL,
        )

        // Then
        assertNotNull(response.transaction)
        assertEquals("txn_3ds", response.transaction?.token)
    }

    @Test
    fun `purchase should include attempt_3dsecure when provided`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain attempt_3dsecure", body.contains("attempt_3dsecure"))
            respond(PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.purchase(
            paymentMethodToken = "pm_token",
            amount = 4400,
            redirectUrl = TEST_REDIRECT_URL,
            attempt3dsecure = true,
        )
    }

    @Test
    fun `purchase should throw ServerError on HTTP error`() = runTest {
        // Given
        val engine = MockEngine {
            respond(MERCHANT_ERROR_RESPONSE, HttpStatusCode.BadRequest, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.purchase(paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
            fail("Should throw PurchaseException.ServerError")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(400, e.statusCode)
        }
    }

    @Test
    fun `purchase should throw NetworkError on exception`() = runTest {
        // Given
        val engine = MockEngine {
            throw java.io.IOException("Connection refused")
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.purchase(paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
            fail("Should throw PurchaseException.NetworkError")
        } catch (e: PurchaseException.NetworkError) {
            assertNotNull(e.originalError)
        }
    }

    // ========================================================================
    // offsitePurchase() tests
    // ========================================================================

    @Test
    fun `offsitePurchase should call correct endpoint`() = runTest {
        // Given
        val engine = MockEngine { request ->
            assertEquals(
                "https://merchant.example.com/offsite-purchase",
                request.url.toString(),
            )
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.offsitePurchase(
            gateway = "gw_paypal",
            paymentMethodToken = "pm_token",
            amount = 4400,
            redirectUrl = "com.test.app.spreedlyoffsite://checkout",
        )

        // Then
        assertNotNull(response.transaction)
        assertEquals("txn_offsite", response.transaction?.token)
        assertEquals("https://gateway.example.com/checkout/abc", response.transaction?.checkoutUrl)
    }

    @Test
    fun `offsitePurchase should serialize flat JSON request`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain gateway", body.contains("\"gateway\""))
            assertTrue("Should contain payment_method_token", body.contains("payment_method_token"))
            assertTrue("Should contain redirect_url", body.contains("redirect_url"))
            assertTrue("Should contain callback_url", body.contains("callback_url"))
            assertTrue("Should NOT wrap in transaction", !body.contains("\"transaction\""))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.offsitePurchase(
            gateway = "gw_sprel",
            paymentMethodToken = "pm",
            amount = 100,
            redirectUrl = "app://checkout",
        )
    }

    @Test
    fun `offsitePurchase should throw ServerError on HTTP error`() = runTest {
        // Given
        val errorBody = """
            {
                "errors": [{"key": "errors.invalid", "message": "Invalid gateway"}]
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(errorBody, HttpStatusCode.UnprocessableEntity, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.offsitePurchase(
                gateway = "bad_gw",
                paymentMethodToken = "pm",
                amount = 100,
                redirectUrl = "app://checkout",
            )
            fail("Should throw PurchaseException.ServerError")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(422, e.statusCode)
            assertEquals("Invalid gateway", e.message)
        }
    }

    // ========================================================================
    // complete() tests
    // ========================================================================

    @Test
    fun `complete should call correct endpoint`() = runTest {
        // Given
        val engine = MockEngine { request ->
            assertEquals(
                "https://merchant.example.com/api/v1/transactions/txn_123/complete",
                request.url.toString(),
            )
            respond(STATUS_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.complete("txn_123")

        // Then
        assertNotNull(response)
    }

    @Test
    fun `complete should throw ServerError on HTTP error`() = runTest {
        // Given
        val engine = MockEngine {
            respond("error", HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.complete("txn_123")
            fail("Should throw PurchaseException.ServerError")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(500, e.statusCode)
        }
    }

    @Test
    fun `complete should throw NetworkError on exception`() = runTest {
        // Given
        val engine = MockEngine {
            throw java.io.IOException("Timeout")
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.complete("txn_123")
            fail("Should throw PurchaseException.NetworkError")
        } catch (e: PurchaseException.NetworkError) {
            assertNotNull(e.originalError)
        }
    }

    companion object {
        private const val TEST_REDIRECT_URL = "com.test.app.spreedlyoffsite://checkout"

        private val PURCHASE_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_3ds",
                    "succeeded": false,
                    "state": "pending",
                    "required_action": "device_fingerprint",
                    "message": null
                }
            }
        """.trimIndent()

        private val MERCHANT_ERROR_RESPONSE = """
            {
                "error": "Bad Request",
                "details": {
                    "error": "Invalid payment method"
                }
            }
        """.trimIndent()

        private val OFFSITE_PURCHASE_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_offsite",
                    "state": "pending",
                    "succeeded": false,
                    "checkout_url": "https://gateway.example.com/checkout/abc"
                }
            }
        """.trimIndent()

        private val STATUS_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_123",
                    "state": "pending",
                    "succeeded": false
                }
            }
        """.trimIndent()
    }
}
