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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpreedlyPurchaseAPIClientTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun createClient(engine: MockEngine): SpreedlyPurchaseAPIClient {
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(purchaseJson) }
            expectSuccess = false
        }
        return SpreedlyPurchaseAPIClient(client = httpClient)
    }

    // ========================================================================
    // purchase() tests
    // ========================================================================

    @Test
    fun `purchase should return response on success`() = runTest {
        // Given
        val engine = MockEngine {
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.purchase(
            gateway = SpreedlyPurchaseAPIClient.GATEWAY_PAYPAL,
            paymentMethodToken = "pm_token",
            amount = 4400,
            redirectUrl = TEST_REDIRECT_URL,
        )

        // Then
        assertNotNull(response.transaction)
        assertEquals("txn_123", response.transaction?.token)
        assertEquals("pending", response.transaction?.state)
    }

    @Test
    fun `purchase should not include Authorization header`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val authHeader = request.headers["Authorization"]
            assertNull("Should not have auth header", authHeader)
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.purchase(paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)

        // Then - assertions in MockEngine
    }

    @Test
    fun `purchase should use create-purchase endpoint`() = runTest {
        // Given
        val engine = MockEngine { request ->
            assertTrue(
                "URL should use create-purchase endpoint",
                request.url.toString().contains("/api/v1/create-purchase"),
            )
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.purchase(gateway = "paypal", paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
    }

    @Test
    fun `purchase should include gateway in request body`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain gateway field", body.contains("\"gateway\""))
            assertTrue("Should contain paypal", body.contains("\"paypal\""))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.purchase(gateway = "paypal", paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
    }

    @Test
    fun `purchase should omit gateway when null`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertFalse("Should NOT contain gateway field", body.contains("\"gateway\""))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.purchase(gateway = null, paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
    }

    @Test
    fun `purchase should throw ServerError on HTTP error`() = runTest {
        // Given
        val engine = MockEngine {
            respond(ERROR_RESPONSE, HttpStatusCode.UnprocessableEntity, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.purchase(paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
            fail("Should throw PurchaseException.ServerError")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(422, e.statusCode)
            assertEquals("Amount must be positive", e.message)
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
    // ebanxPurchase() tests
    // ========================================================================

    @Test
    fun `ebanxPurchase should serialize document in gateway_specific_fields`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain document", body.contains("\"document\""))
            assertTrue("Should contain ebanx wrapper", body.contains("\"ebanx\""))
            assertTrue("Should contain gateway_specific_fields", body.contains("gateway_specific_fields"))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.ebanxPurchase(
            paymentMethodToken = "pm",
            amount = 9900,
            currencyCode = "BRL",
            redirectUrl = TEST_REDIRECT_URL,
            document = "853.513.468-93",
        )
    }

    @Test
    fun `ebanxPurchase should include ebanx gateway in request body`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain gateway field", body.contains("\"gateway\""))
            assertTrue("Should contain ebanx", body.contains("\"ebanx\""))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.ebanxPurchase(
            paymentMethodToken = "pm",
            amount = 9900,
            currencyCode = "BRL",
            redirectUrl = TEST_REDIRECT_URL,
        )
    }

    @Test
    fun `ebanxPurchase should omit gateway_specific_fields when document is null`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertFalse("Should NOT contain gateway_specific_fields", body.contains("gateway_specific_fields"))
            respond(OFFSITE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.ebanxPurchase(
            paymentMethodToken = "pm",
            amount = 5000,
            currencyCode = "MXN",
            redirectUrl = TEST_REDIRECT_URL,
            document = null,
        )
    }

    // ========================================================================
    // stripeAPMPurchase() tests
    // ========================================================================

    @Test
    fun `stripeAPMPurchase should include payment_method with apm_types`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain stripe_apm", body.contains("stripe_apm"))
            assertTrue("Should contain ideal", body.contains("ideal"))
            respond(STRIPE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.stripeAPMPurchase(
            amount = 1000,
            currencyCode = "EUR",
            apmTypes = listOf("ideal"),
            redirectUrl = TEST_REDIRECT_URL,
        )

        // Then
        assertNotNull(response.transaction?.gatewaySpecificResponseFields?.stripePaymentIntents?.clientSecret)
    }

    @Test
    fun `stripeAPMPurchase should include stripe gateway in request body`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain gateway field", body.contains("\"gateway\""))
            assertTrue("Should contain stripe", body.contains("\"stripe\""))
            respond(STRIPE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.stripeAPMPurchase(
            amount = 1000,
            currencyCode = "EUR",
            apmTypes = listOf("ideal"),
            redirectUrl = TEST_REDIRECT_URL,
        )
    }

    @Test
    fun `stripeAPMPurchase should not include payment_method_token in request`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertFalse("Should NOT contain payment_method_token", body.contains("payment_method_token"))
            respond(STRIPE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.stripeAPMPurchase(
            amount = 1000,
            currencyCode = "EUR",
            apmTypes = listOf("bancontact"),
            redirectUrl = TEST_REDIRECT_URL,
        )
    }

    // ========================================================================
    // braintreePurchase() tests
    // ========================================================================

    @Test
    fun `braintreePurchase should include offsite_sync in payment_method`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain offsite_sync", body.contains("offsite_sync"))
            assertTrue("Should contain paypal", body.contains("paypal"))
            respond(BRAINTREE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.braintreePurchase(
            paymentMethodType = "paypal",
            amount = 1000,
        )

        // Then
        assertEquals("processing", response.transaction?.state)
    }

    @Test
    fun `braintreePurchase should include braintree gateway in request body`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain gateway field", body.contains("\"gateway\""))
            assertTrue("Should contain braintree", body.contains("\"braintree\""))
            respond(BRAINTREE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreePurchase(
            paymentMethodType = "paypal",
            amount = 1000,
        )
    }

    @Test
    fun `braintreePurchase should include gateway_specific_fields for venmo`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain venmo_flow_type", body.contains("venmo_flow_type"))
            assertTrue("Should contain multi_use", body.contains("multi_use"))
            respond(BRAINTREE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreePurchase(
            paymentMethodType = "venmo",
            amount = 1000,
            gatewaySpecificFields = BraintreeGatewaySpecificFields(
                braintree = BraintreeFields(venmoFlowType = "multi_use"),
            ),
        )
    }

    @Test
    fun `braintreePurchase should include paypal_flow_type for paypal`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain paypal_flow_type", body.contains("paypal_flow_type"))
            assertTrue("Should contain checkout", body.contains("checkout"))
            respond(BRAINTREE_PURCHASE_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreePurchase(
            paymentMethodType = "paypal",
            amount = 1000,
            gatewaySpecificFields = BraintreeGatewaySpecificFields(
                braintree = BraintreeFields(paypalFlowType = "checkout"),
            ),
        )
    }

    // ========================================================================
    // braintreeConfirm() tests
    // ========================================================================

    @Test
    fun `braintreeConfirm should use confirm endpoint`() = runTest {
        // Given
        val engine = MockEngine { request ->
            assertTrue(
                "URL should contain confirm path",
                request.url.toString().contains("/api/v1/transactions/txn_abc/confirm"),
            )
            respond(CONFIRM_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.braintreeConfirm(
            transactionToken = "txn_abc",
            state = BraintreeConfirmState.SUCCESSFUL,
            nonce = "bt_nonce_123",
            paymentMethodType = "paypal",
        )

        // Then
        assertEquals(true, response.transaction?.succeeded)
        assertEquals("succeeded", response.transaction?.state)
    }

    @Test
    fun `braintreeConfirm should send flat JSON body without transaction wrapper`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain nonce", body.contains("bt_nonce"))
            assertTrue("Should contain state Successful", body.contains("Successful"))
            assertTrue("Should contain payment_method_type at top level", body.contains("\"payment_method_type\""))
            assertFalse("Should NOT nest under payment_method", body.contains("\"payment_method\":{"))
            assertFalse("Should NOT wrap in transaction", body.contains("\"transaction\""))
            respond(CONFIRM_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreeConfirm(
            transactionToken = "txn_abc",
            state = BraintreeConfirmState.SUCCESSFUL,
            nonce = "bt_nonce",
            paymentMethodType = "venmo",
        )
    }

    @Test
    fun `braintreeConfirm should send Failed state with message and no nonce`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain state Failed", body.contains("\"Failed\""))
            assertTrue("Should contain message", body.contains("Payment error"))
            assertFalse("Should NOT contain nonce", body.contains("nonce"))
            assertTrue("Should contain payment_method_type", body.contains("\"payment_method_type\""))
            respond(CONFIRM_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreeConfirm(
            transactionToken = "txn_abc",
            state = BraintreeConfirmState.FAILED,
            message = "Payment error",
            paymentMethodType = "paypal",
        )
    }

    @Test
    fun `braintreeConfirm should send Cancelled state with message and no nonce`() = runTest {
        // Given
        val engine = MockEngine { request ->
            val body = (request.body as TextContent).text
            assertTrue("Should contain state Cancelled", body.contains("\"Cancelled\""))
            assertTrue("Should contain message", body.contains("User cancelled"))
            assertFalse("Should NOT contain nonce", body.contains("nonce"))
            respond(CONFIRM_RESPONSE, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        client.braintreeConfirm(
            transactionToken = "txn_abc",
            state = BraintreeConfirmState.CANCELLED,
            message = "User cancelled",
            paymentMethodType = "venmo",
        )
    }

    @Test
    fun `braintreeConfirm should throw ServerError on failure`() = runTest {
        // Given
        val engine = MockEngine {
            respond(ERROR_RESPONSE, HttpStatusCode.UnprocessableEntity, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.braintreeConfirm(
                transactionToken = "txn_abc",
                state = BraintreeConfirmState.SUCCESSFUL,
                nonce = "nonce",
                paymentMethodType = "paypal",
            )
            fail("Should throw PurchaseException.ServerError")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(422, e.statusCode)
        }
    }

    // ========================================================================
    // Response parsing tests
    // ========================================================================

    @Test
    fun `should parse checkout_url from response`() = runTest {
        // Given
        val engine = MockEngine {
            respond(RESPONSE_WITH_CHECKOUT_URL, HttpStatusCode.OK, jsonHeaders)
        }
        val client = createClient(engine)

        // When
        val response = client.purchase(
            paymentMethodToken = "pm",
            amount = 100,
            redirectUrl = TEST_REDIRECT_URL,
        )

        // Then
        assertEquals("https://gateway.example.com/checkout/abc", response.transaction?.checkoutUrl)
    }

    @Test
    fun `should fallback error message when response body is not JSON`() = runTest {
        // Given
        val engine = MockEngine {
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val client = createClient(engine)

        // When/Then
        try {
            client.purchase(paymentMethodToken = "pm", amount = 100, redirectUrl = TEST_REDIRECT_URL)
            fail("Should throw")
        } catch (e: PurchaseException.ServerError) {
            assertEquals(500, e.statusCode)
            assertTrue(e.message.contains("500"))
        }
    }

    companion object {
        private const val TEST_REDIRECT_URL = "com.test.app.spreedlyoffsite://checkout"

        private val OFFSITE_PURCHASE_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_123",
                    "state": "pending",
                    "succeeded": false,
                    "message": null
                }
            }
        """.trimIndent()

        private val ERROR_RESPONSE = """
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

        private val STRIPE_PURCHASE_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_stripe",
                    "state": "pending",
                    "succeeded": false,
                    "gateway_specific_response_fields": {
                        "stripe_payment_intents": {
                            "client_secret": "pi_xxx_secret_xxx"
                        }
                    }
                }
            }
        """.trimIndent()

        private val BRAINTREE_PURCHASE_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_bt",
                    "state": "processing",
                    "succeeded": false,
                    "gateway_specific_response_fields": {
                        "braintree": {
                            "client_token": "eyJ..."
                        }
                    }
                }
            }
        """.trimIndent()

        private val CONFIRM_RESPONSE = """
            {
                "transaction": {
                    "token": "txn_abc",
                    "state": "succeeded",
                    "succeeded": true,
                    "message": null
                }
            }
        """.trimIndent()

        private val RESPONSE_WITH_CHECKOUT_URL = """
            {
                "transaction": {
                    "token": "txn_offsite",
                    "state": "pending",
                    "succeeded": false,
                    "checkout_url": "https://gateway.example.com/checkout/abc"
                }
            }
        """.trimIndent()
    }
}
