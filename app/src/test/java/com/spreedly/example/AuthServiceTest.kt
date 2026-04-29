package com.spreedly.example

import com.spreedly.sdk.models.AuthParamsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {
    private fun mockClient(respondJson: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = respondJson,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = true
        }

    @Test
    fun `getAuthParams should parse successful response`() = runTest {
        val json = """{"nonce":"abc","signature":"sig","certificateToken":"cert","timestamp":123}"""
        val service = AuthService(mockClient(json))

        val result = service.getAuthParams()

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("abc", data.nonce)
        assertEquals("sig", data.signature)
        assertEquals("cert", data.certificateToken)
        assertEquals(123L, data.timestamp)
    }

    @Test
    fun `getAuthParams should return failure on server error`() = runTest {
        val service = AuthService(mockClient("{}", HttpStatusCode.InternalServerError))

        val result = service.getAuthParams()

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `getPaymentMethods should parse successful response`() = runTest {
        val json = """{"payment_methods":[],"count":0}"""
        val service = AuthService(mockClient(json))

        val result = service.getPaymentMethods()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().paymentMethods.isEmpty())
    }

    @Test
    fun `retainPaymentMethod should parse successful response`() = runTest {
        val json = """{"success":true}"""
        val service = AuthService(mockClient(json))

        val result = service.retainPaymentMethod("tok_123")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().success)
    }

    @Test
    fun `retainPaymentMethod should return failure on server error`() = runTest {
        val service = AuthService(mockClient("{}", HttpStatusCode.BadRequest))

        val result = service.retainPaymentMethod("tok_bad")

        assertTrue(result.isFailure)
    }

    @Test
    fun `AuthService should be instantiable with default client`() {
        val authService = AuthService()
        assertNotNull(authService)
    }
}
