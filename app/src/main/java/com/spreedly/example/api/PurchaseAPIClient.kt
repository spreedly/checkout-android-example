package com.spreedly.example.api

import android.util.Log
import com.spreedly.sdk.models.TransactionStatusResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * API client for the merchant backend purchase API.
 *
 * Calls the merchant backend which proxies requests to Spreedly Core.
 * Used for 3DS purchase flows and offsite payment purchases.
 */
class PurchaseAPIClient(
    private val config: ServerConfig,
    private val client: HttpClient = createPurchaseHttpClient("PurchaseAPI"),
) {
    data class ServerConfig(
        val baseURL: String,
    )

    companion object {
        private const val TAG = "PurchaseAPI"
    }

    /**
     * Execute a 3DS purchase through the merchant backend.
     *
     * **Endpoint:** `POST {baseURL}/api/v1/purchase`
     *
     * @param paymentMethodToken The payment method token from SDK tokenization
     * @param amount The amount in cents
     * @param currencyCode The 3-letter currency code (default: "USD")
     * @param redirectUrl The deep link URL for app return after 3DS
     * @param attempt3dsecure Gateway-specific: true, Global: null (omitted from request)
     * @return [PurchaseResponse] containing the transaction details
     * @throws PurchaseException on error
     */
    suspend fun purchase(
        paymentMethodToken: String,
        amount: Int,
        currencyCode: String = "USD",
        redirectUrl: String,
        attempt3dsecure: Boolean? = null,
    ): PurchaseResponse {
        val url = "${config.baseURL}/api/v1/purchase"
        val request = TransactionData(
            amount = amount,
            currencyCode = currencyCode,
            paymentMethodToken = paymentMethodToken,
            redirectUrl = redirectUrl,
            attempt3dsecure = attempt3dsecure,
        )

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                val errorMessage = parseErrorMessage(responseBody, response.status.value, "Purchase")
                throw PurchaseException.ServerError(
                    statusCode = response.status.value,
                    message = errorMessage,
                    rawResponse = responseBody,
                )
            }

            purchaseJson.decodeFromString<PurchaseResponse>(responseBody)
        } catch (e: PurchaseException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Purchase network error", e)
            throw PurchaseException.NetworkError(e)
        }
    }

    /**
     * Execute an offsite purchase through the merchant backend.
     *
     * **Endpoint:** `POST {baseURL}/offsite-purchase`
     *
     * @param gateway The Spreedly gateway token for the offsite gateway
     * @param paymentMethodToken The payment method token from SDK tokenization
     * @param amount The amount in smallest currency unit (e.g. cents)
     * @param currencyCode ISO 4217 currency code (e.g. "USD")
     * @param redirectUrl URL to return to app after checkout
     * @param callbackUrl Webhook URL for server-side callback
     * @param channel Typically "app" for mobile
     * @return [SpreedlyPurchaseResponse] containing the transaction token and checkout_url
     * @throws PurchaseException on error
     */
    suspend fun offsitePurchase(
        gateway: String,
        paymentMethodToken: String,
        amount: Int,
        currencyCode: String = "USD",
        redirectUrl: String,
        callbackUrl: String = DEFAULT_CALLBACK_URL,
        channel: String = DEFAULT_CHANNEL,
    ): SpreedlyPurchaseResponse {
        val url = "${config.baseURL}/offsite-purchase"
        val request = MerchantOffsitePurchaseRequest(
            gateway = gateway,
            paymentMethodToken = paymentMethodToken,
            amount = amount,
            currencyCode = currencyCode,
            redirectUrl = redirectUrl,
            callbackUrl = callbackUrl,
            channel = channel,
        )

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                val errorMessage = parseSpreedlyErrorMessage(responseBody, response.status.value, "Offsite purchase")
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
            Log.w(TAG, "Offsite purchase network error", e)
            throw PurchaseException.NetworkError(e)
        }
    }

    /**
     * Complete a gateway-specific 3DS transaction after device fingerprint phase.
     *
     * This endpoint returns the same format as /status.json (TransactionStatusResponse)
     * which can be directly passed to GatewaySpecific3DSIntegration.finalizeTransaction().
     *
     * **Endpoint:** `POST {baseURL}/api/v1/transactions/{token}/complete`
     *
     * @param transactionToken Transaction token from purchase response
     * @return TransactionStatusResponse from Spreedly Core API
     * @throws PurchaseException on error
     */
    suspend fun complete(transactionToken: String): TransactionStatusResponse {
        val url = "${config.baseURL}/api/v1/transactions/$transactionToken/complete"

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(emptyMap<String, String>())
            }

            val responseBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                val errorMessage = parseErrorMessage(responseBody, response.status.value, "Complete")
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
            Log.w(TAG, "Complete network error", e)
            throw PurchaseException.NetworkError(e)
        }
    }

    /**
     * Parse error message from the merchant backend's error response formats.
     * Tries `PurchaseErrorResponse` first, then falls back to `PurchaseResponse`.
     */
    private fun parseErrorMessage(responseBody: String, statusCode: Int, context: String): String = try {
        val errorResponse = purchaseJson.decodeFromString<PurchaseErrorResponse>(responseBody)
        errorResponse.details?.transaction?.message
            ?: errorResponse.error
            ?: "$context failed with status $statusCode"
    } catch (_: Exception) {
        try {
            val response = purchaseJson.decodeFromString<PurchaseResponse>(responseBody)
            response.transaction?.message
                ?: response.errors?.firstOrNull()?.message
                ?: "$context failed with status $statusCode"
        } catch (_: Exception) {
            "$context failed with status $statusCode"
        }
    }

    /**
     * Parse error message from a Spreedly Core-shaped error response.
     */
    private fun parseSpreedlyErrorMessage(responseBody: String, statusCode: Int, context: String): String = try {
        val response = purchaseJson.decodeFromString<SpreedlyPurchaseResponse>(responseBody)
        response.errors?.firstOrNull()?.message
            ?: response.transaction?.message
            ?: "$context failed with status $statusCode"
    } catch (_: Exception) {
        "$context failed with status $statusCode"
    }

    fun close() {
        client.close()
    }
}
