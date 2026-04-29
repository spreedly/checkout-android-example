package com.spreedly.example.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Default webhook callback URL placeholder.
 * Production apps should use the merchant's actual webhook URL.
 */
const val DEFAULT_CALLBACK_URL = "https://example.com/webhook/callback"

/**
 * Default channel identifier for mobile purchase requests.
 */
const val DEFAULT_CHANNEL = "app"

/**
 * Shared [Json] configuration for purchase API serialization.
 *
 * Used by both [PurchaseAPIClient] and [SpreedlyPurchaseAPIClient] to ensure
 * consistent JSON encoding/decoding behavior.
 */
val purchaseJson: Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Creates an [HttpClient] configured for purchase API usage.
 *
 * Provides shared ContentNegotiation (JSON) and Logging plugins so that
 * both [PurchaseAPIClient] and [SpreedlyPurchaseAPIClient] use the same
 * HTTP configuration without duplicating setup code.
 *
 * @param logTag Prefix for log messages (e.g. "PurchaseAPI", "SpreedlyPurchaseAPI")
 */
fun createPurchaseHttpClient(logTag: String): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(purchaseJson)
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Log.d(logTag, message)
            }
        }
        level = LogLevel.INFO
    }

    expectSuccess = false
}

/**
 * Unified error model for purchase API responses.
 *
 * Both the merchant backend and direct Spreedly Core APIs return errors
 * in the same structure. This single model replaces the previously
 * duplicated `PurchaseError` and `SpreedlyPurchaseError` classes.
 */
@Serializable
data class PurchaseApiError(
    @SerialName("attribute")
    val attribute: String? = null,
    @SerialName("key")
    val key: String? = null,
    @SerialName("message")
    val message: String? = null,
)

/**
 * Unified exception hierarchy for purchase API errors.
 *
 * Replaces the previously duplicated `PurchaseAPIException` and
 * `SpreedlyPurchaseAPIException` sealed classes, providing a single
 * error type for all purchase-related API failures.
 */
sealed class PurchaseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class ServerError(
        val statusCode: Int,
        override val message: String,
        val rawResponse: String? = null,
    ) : PurchaseException("Purchase API error ($statusCode): $message")

    data class NetworkError(
        val originalError: Throwable,
    ) : PurchaseException("Network error: ${originalError.message}", originalError)

    data class AuthenticationError(
        override val message: String,
    ) : PurchaseException(message)

    data class InvalidURL(val url: String) :
        PurchaseException("Invalid purchase API URL: $url")

    data class EncodingError(
        val originalError: Throwable,
    ) : PurchaseException("Encoding error: ${originalError.message}", originalError)
}
