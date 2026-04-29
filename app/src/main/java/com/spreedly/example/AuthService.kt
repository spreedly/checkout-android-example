package com.spreedly.example

import com.spreedly.example.models.PaymentMethodListResponse
import com.spreedly.example.models.RetainPaymentMethodResponse
import com.spreedly.sdk.models.AuthParamsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Service for backend API calls (auth params, payment methods, retention).
 *
 * Accepts [HttpClient] via constructor for testability -- pass a client
 * backed by `MockEngine` in unit tests.
 *
 * @param httpClient Ktor HTTP client; defaults to the production client.
 */
class AuthService(
    private val httpClient: HttpClient = createDefaultClient(),
) {
    suspend fun getAuthParams(): kotlin.Result<AuthParamsResponse> = runCatching {
        httpClient.get("/api/v1/auth/params").body<AuthParamsResponse>()
    }

    suspend fun getPaymentMethods(): kotlin.Result<PaymentMethodListResponse> = runCatching {
        httpClient.get("/api/v1/payment_methods").body<PaymentMethodListResponse>()
    }

    suspend fun retainPaymentMethod(
        paymentMethodToken: String,
    ): kotlin.Result<RetainPaymentMethodResponse> = runCatching {
        httpClient.put("/api/v1/payment_methods/$paymentMethodToken/retain").body<RetainPaymentMethodResponse>()
    }

    companion object {
        private const val DEFAULT_HOST = "checkout-web-sample-app-049a3c617015.herokuapp.com"

        fun createDefaultClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = DEFAULT_HOST
                }
                contentType(ContentType.Application.Json)
            }
        }
    }
}
