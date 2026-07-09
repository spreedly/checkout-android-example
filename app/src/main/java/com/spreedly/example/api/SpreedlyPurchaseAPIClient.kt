package com.spreedly.example.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString

/**
 * API client for purchase operations through the merchant backend.
 *
 * Routes all purchase and confirm requests through the merchant backend
 * which handles Spreedly Core API authentication server-side.
 *
 * ## Supported Gateways
 *
 * The `gateway` parameter selects which payment gateway to route through:
 * - [GATEWAY_PAYPAL] -- PayPal offsite payments
 * - [GATEWAY_EBANX] -- EBANX (Pix, Boleto, OXXO, NuPay)
 * - [GATEWAY_STRIPE] -- Stripe APM (iDEAL, Bancontact, etc.)
 * - [GATEWAY_BRAINTREE] -- Braintree (PayPal, Venmo)
 * - `null` -- Spreedly test gateway (Sprel)
 *
 * ## Example Usage
 *
 * ```kotlin
 * val client = SpreedlyPurchaseAPIClient()
 *
 * val response = client.purchase(
 *     gateway = SpreedlyPurchaseAPIClient.GATEWAY_PAYPAL,
 *     paymentMethodToken = "payment_token_from_sdk",
 *     amount = 4400,
 *     redirectUrl = SpreedlyPurchaseAPIClient.redirectUrl(context, "sprel/checkout")
 * )
 *
 * SpreedlyOffsiteCheckout.present(response.transaction.token, activity)
 * ```
 */
class SpreedlyPurchaseAPIClient
@JvmOverloads
constructor(
    private val client: HttpClient = createPurchaseHttpClient("SpreedlyPurchaseAPI"),
) {
    companion object {
        private const val TAG = "SpreedlyPurchaseAPI"

        /** Merchant backend base URL. */
        const val BASE_URL = "https://checkout-web-sample-app-049a3c617015.herokuapp.com"

        /** Gateway identifier for PayPal offsite payments. */
        const val GATEWAY_PAYPAL = "paypal"

        /** Gateway identifier for EBANX payments (Pix, Boleto, OXXO, NuPay). */
        const val GATEWAY_EBANX = "ebanx"

        /** Gateway identifier for Stripe APM (iDEAL, Bancontact, etc.). */
        const val GATEWAY_STRIPE = "stripe"

        /** Gateway identifier for Braintree (PayPal, Venmo). */
        const val GATEWAY_BRAINTREE = "braintree"

        /**
         * Build a redirect URL using the SDK-owned offsite return scheme.
         *
         * Uses [SpreedlyOffsiteCheckout.redirectUrl] so the SDK's own
         * [com.spreedly.sdk.ui.offsite.OffsiteReturnActivity] receives the deep link.
         */
        fun redirectUrl(context: android.content.Context, path: String = "checkout"): String =
            com.spreedly.sdk.ui.offsite.SpreedlyOffsiteCheckout.redirectUrl(context, path)

        /**
         * Re-exported from [com.spreedly.example.api.DEFAULT_CALLBACK_URL] for Java interop.
         */
        @Suppress("ConstPropertyName")
        const val DEFAULT_CALLBACK_URL = com.spreedly.example.api.DEFAULT_CALLBACK_URL

        private const val PURCHASE_PATH = "/api/v1/create-purchase"

        private fun confirmUrl(transactionToken: String): String =
            "$BASE_URL/api/v1/transactions/$transactionToken/confirm"
    }

    /**
     * Execute a purchase request, parse the response, and wrap
     * all errors (HTTP + network) into [PurchaseException].
     *
     * Every public method delegates here so error handling is defined once.
     */
    private suspend inline fun <reified T> executePurchaseRequest(
        url: String,
        request: T,
        errorContext: String = "Purchase",
    ): SpreedlyPurchaseResponse = try {
        val requestBody = purchaseJson.encodeToString(request)
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val errorMessage = try {
                val errorResponse = purchaseJson.decodeFromString<SpreedlyPurchaseResponse>(responseBody)
                errorResponse.errors?.firstOrNull()?.message
                    ?: errorResponse.transaction?.message
                    ?: "$errorContext failed with status ${response.status.value}"
            } catch (_: Exception) {
                "$errorContext failed with status ${response.status.value}"
            }
            throw PurchaseException.ServerError(
                statusCode = response.status.value,
                message = errorMessage,
                rawResponse = responseBody,
            )
        }
        purchaseJson.decodeFromString(responseBody)
    } catch (e: PurchaseException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$errorContext network error", e)
        throw PurchaseException.NetworkError(e)
    }

    /**
     * Execute a standard offsite purchase through the merchant backend.
     *
     * **Endpoint:** `POST {BASE_URL}/api/v1/create-purchase`
     *
     * @param gateway The gateway identifier (e.g. [GATEWAY_PAYPAL]), or null for test gateway
     * @param paymentMethodToken The payment method token from SDK tokenization
     * @param amount The amount in cents (e.g., 4400 = $44.00)
     * @param currencyCode The 3-letter currency code (default: "USD")
     * @param redirectUrl The URL for gateway to redirect after checkout
     * @param callbackUrl The webhook callback URL
     * @return [SpreedlyPurchaseResponse] containing the transaction token
     * @throws PurchaseException on error
     */
    suspend fun purchase(
        gateway: String? = null,
        paymentMethodToken: String,
        amount: Int,
        currencyCode: String = "USD",
        redirectUrl: String,
        callbackUrl: String = DEFAULT_CALLBACK_URL,
    ): SpreedlyPurchaseResponse = executePurchaseRequest(
        url = "$BASE_URL$PURCHASE_PATH",
        request = SpreedlyPurchaseTransactionRequest(
            gateway = gateway,
            transaction = SpreedlyPurchaseTransactionBody(
                amount = amount,
                currencyCode = currencyCode,
                paymentMethodToken = paymentMethodToken,
                redirectUrl = redirectUrl,
                callbackUrl = callbackUrl,
            ),
        ),
        errorContext = "Purchase",
    )

    /**
     * Execute an EBANX purchase through the merchant backend.
     *
     * EBANX has different requirements than standard offsite payments:
     * - No `browser_info` or `ip` fields
     * - Uses `gateway_specific_fields.ebanx.document` for CPF (except OXXO)
     * - Currency is BRL (Brazil) or MXN (Mexico/OXXO)
     *
     * **Endpoint:** `POST {BASE_URL}/api/v1/create-purchase`
     *
     * @param paymentMethodToken The payment method token from SDK tokenization
     * @param amount The amount in cents (e.g., 9900 = R$99.00 or MX$99.00)
     * @param currencyCode "BRL" for Pix/Boleto/NuPay, "MXN" for OXXO
     * @param redirectUrl The deep link URL for app return
     * @param callbackUrl The webhook callback URL
     * @param document The customer's CPF/taxpayer ID. Required for Pix/Boleto/NuPay, null for OXXO
     * @return [SpreedlyPurchaseResponse] containing the transaction token
     * @throws PurchaseException on error
     */
    suspend fun ebanxPurchase(
        paymentMethodToken: String,
        amount: Int,
        currencyCode: String,
        redirectUrl: String,
        callbackUrl: String = DEFAULT_CALLBACK_URL,
        document: String? = null,
    ): SpreedlyPurchaseResponse {
        val gatewaySpecificFields = document?.let {
            EbanxGatewaySpecificFields(ebanx = EbanxFields(document = it))
        }
        return executePurchaseRequest(
            url = "$BASE_URL$PURCHASE_PATH",
            request = EbanxPurchaseTransactionRequest(
                gateway = GATEWAY_EBANX,
                transaction = EbanxPurchaseTransactionBody(
                    amount = amount,
                    currencyCode = currencyCode,
                    paymentMethodToken = paymentMethodToken,
                    redirectUrl = redirectUrl,
                    callbackUrl = callbackUrl,
                    gatewaySpecificFields = gatewaySpecificFields,
                ),
            ),
            errorContext = "EBANX purchase",
        )
    }

    /**
     * Create a pending Stripe APM purchase through the merchant backend.
     *
     * Stripe APM has no tokenization step; the backend creates a pending purchase with
     * payment_method_type "stripe_apm" and apm_types. The response includes
     * transaction_token and gateway_specific_response_fields.stripe_payment_intents.client_secret
     * for presenting the Stripe PaymentSheet.
     *
     * **Endpoint:** `POST {BASE_URL}/api/v1/create-purchase`
     *
     * @param amount The amount in smallest currency unit (e.g. cents)
     * @param currencyCode e.g. "EUR" (iDEAL requires EUR)
     * @param apmTypes List of APM identifiers (e.g. ["ideal", "bancontact"])
     * @param redirectUrl URL for redirect-based APMs; user returns to app via this
     * @param callbackUrl Server-side webhook URL
     * @return [SpreedlyPurchaseResponse] with transaction.token and client_secret when state is "pending"
     * @throws PurchaseException on error
     */
    suspend fun stripeAPMPurchase(
        amount: Int,
        currencyCode: String,
        apmTypes: List<String>,
        redirectUrl: String,
        callbackUrl: String = DEFAULT_CALLBACK_URL,
        radarSessionId: String? = null,
    ): SpreedlyPurchaseResponse = executePurchaseRequest(
        url = "$BASE_URL$PURCHASE_PATH",
        request = StripeAPMPurchaseTransactionRequest(
            gateway = GATEWAY_STRIPE,
            transaction = StripeAPMPurchaseTransactionBody(
                amount = amount,
                currencyCode = currencyCode,
                redirectUrl = redirectUrl,
                callbackUrl = callbackUrl,
                paymentMethod = StripeAPMPaymentMethod(apmTypes = apmTypes),
                gatewaySpecificFields = radarSessionId?.let {
                    StripeGatewaySpecificFields(
                        stripePaymentIntents = StripeRadarFields(radarSessionId = it),
                    )
                },
            ),
        ),
        errorContext = "Stripe APM purchase",
    )

    /**
     * Create a pending Braintree purchase through the merchant backend.
     *
     * Braintree has no tokenization step; the backend creates a pending purchase with
     * payment_method_type "paypal" or "venmo" on the Braintree gateway. The status API
     * response includes a Braintree client_token for initializing the native SDK.
     *
     * **Endpoint:** `POST {BASE_URL}/api/v1/create-purchase`
     *
     * @param paymentMethodType "paypal" or "venmo"
     * @param amount The amount in cents (e.g., 1000 = $10.00)
     * @param currencyCode e.g. "USD"
     * @param gatewaySpecificFields Optional Braintree-specific fields (e.g. venmo_flow_type)
     * @return [SpreedlyPurchaseResponse] with transaction.token when state is "pending"
     * @throws PurchaseException on error
     */
    suspend fun braintreePurchase(
        paymentMethodType: String,
        amount: Int,
        currencyCode: String = "USD",
        gatewaySpecificFields: BraintreeGatewaySpecificFields? = null,
    ): SpreedlyPurchaseResponse = executePurchaseRequest(
        url = "$BASE_URL$PURCHASE_PATH",
        request = BraintreePurchaseTransactionRequest(
            gateway = GATEWAY_BRAINTREE,
            transaction = BraintreePurchaseTransactionBody(
                amount = amount,
                currencyCode = currencyCode,
                paymentMethod = BraintreePaymentMethod(
                    paymentMethodType = paymentMethodType,
                ),
                gatewaySpecificFields = gatewaySpecificFields,
            ),
        ),
        errorContext = "Braintree purchase",
    )

    /**
     * Confirm a Braintree offsite transaction for any callback state.
     *
     * Must be called for **all** Braintree callback outcomes (Successful, Failed, Cancelled)
     * so the Spreedly backend can finalize the transaction.
     *
     * **Endpoint:** `POST {BASE_URL}/api/v1/transactions/{token}/confirm`
     *
     * @param transactionToken The Spreedly transaction token from the pending purchase
     * @param state The Braintree callback outcome
     * @param nonce The Braintree payment nonce (only present for [BraintreeConfirmState.SUCCESSFUL])
     * @param message Error/cancel description (only for [BraintreeConfirmState.FAILED]/[BraintreeConfirmState.CANCELLED])
     * @param paymentMethodType "paypal" or "venmo"
     * @return [SpreedlyPurchaseResponse] with the confirmed transaction details
     * @throws PurchaseException on error
     */
    suspend fun braintreeConfirm(
        transactionToken: String,
        state: BraintreeConfirmState,
        nonce: String? = null,
        message: String? = null,
        paymentMethodType: String,
    ): SpreedlyPurchaseResponse = executePurchaseRequest(
        url = confirmUrl(transactionToken),
        request = BraintreeConfirmRequest(
            state = state,
            nonce = nonce,
            message = message,
            paymentMethodType = paymentMethodType,
        ),
        errorContext = "Braintree confirm",
    )

    fun close() {
        client.close()
    }
}
