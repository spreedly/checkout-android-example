package com.spreedly.example.repository

import android.content.Context
import android.content.SharedPreferences
import com.spreedly.example.AuthService
import com.spreedly.example.models.SavedPaymentMethod
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing saved payment methods.
 *
 * Fetches payment methods from the backend API and caches them locally
 * in [SharedPreferences]. Both collaborators are constructor-injected
 * for testability (pass mocks in unit tests).
 *
 * @param authService Backend API client.
 * @param prefs SharedPreferences instance for local caching.
 */
class PaymentMethodRepository(
    private val authService: AuthService,
    private val prefs: SharedPreferences,
) {
    /** Convenience constructor used from Java callers and ViewModels that only have [Context]. */
    constructor(context: Context) : this(
        authService = AuthService(),
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    companion object {
        private const val PREFS_NAME = "spreedly_saved_payment_methods"
        private const val KEY_PAYMENT_METHODS = "saved_payment_methods"
    }

    fun getSavedPaymentMethods(): List<SavedPaymentMethod> {
        val jsonString = prefs.getString(KEY_PAYMENT_METHODS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedPaymentMethod>>(jsonString)
        } catch (e: Exception) {
            Log.d("PaymentMethodRepository", "Error decoding saved payment methods: ${e::class.simpleName}")
            emptyList()
        }
    }

    suspend fun fetchAndSyncPaymentMethods(): kotlin.Result<List<SavedPaymentMethod>> {
        val apiResult = authService.getPaymentMethods()

        return apiResult.fold(
            onSuccess = { response ->
                val savedMethods = response.paymentMethods
                    .filter { it.isCreditCard() }
                    .map { it.toSavedPaymentMethod() }
                    .take(9)

                val jsonString = json.encodeToString(savedMethods)
                prefs.edit().putString(KEY_PAYMENT_METHODS, jsonString).apply()

                kotlin.Result.success(savedMethods)
            },
            onFailure = { error ->
                val cached = getSavedPaymentMethods()
                if (cached.isNotEmpty()) {
                    kotlin.Result.success(cached)
                } else {
                    kotlin.Result.failure(error)
                }
            },
        )
    }

    fun savePaymentMethod(paymentMethod: SavedPaymentMethod) {
        val currentMethods = getSavedPaymentMethods().toMutableList()

        val existingIndex = currentMethods.indexOfFirst { it.token == paymentMethod.token }
        if (existingIndex >= 0) {
            currentMethods[existingIndex] = paymentMethod
        } else {
            currentMethods.add(0, paymentMethod)
        }

        val jsonString = json.encodeToString(currentMethods)
        prefs.edit().putString(KEY_PAYMENT_METHODS, jsonString).apply()
    }

    suspend fun retainPaymentMethod(paymentMethodToken: String): kotlin.Result<Boolean> =
        authService.retainPaymentMethod(paymentMethodToken).map { true }

    /**
     * Delete a payment method by token.
     *
     * @param token The token of the payment method to delete
     */
    fun deletePaymentMethod(token: String) {
        val currentMethods = getSavedPaymentMethods().toMutableList()
        currentMethods.removeAll { it.token == token }

        val jsonString = json.encodeToString(currentMethods)
        prefs.edit().putString(KEY_PAYMENT_METHODS, jsonString).apply()
    }

    /**
     * Clear all saved payment methods.
     *
     * Useful for testing and debugging.
     */
    fun clearAllSavedPaymentMethods() {
        prefs.edit().remove(KEY_PAYMENT_METHODS).apply()
    }

    /**
     * Initialize with mock data if no data exists.
     *
     * This is helpful for testing the recaching feature.
     */
    fun initializeMockDataIfEmpty() {
        if (getSavedPaymentMethods().isEmpty()) {
            val mockMethods = getMockPaymentMethods()
            mockMethods.forEach { savePaymentMethod(it) }
        }
    }

    /**
     * Generate mock payment methods for testing.
     *
     * @return List of mock saved payment methods
     */
    private fun getMockPaymentMethods(): List<SavedPaymentMethod> = listOf(
            SavedPaymentMethod(
                token = "mock_token_visa_4242",
                lastFourDigits = "4242",
                cardType = "visa",
                cardholderName = "John Doe",
                expiryMonth = 12,
                expiryYear = 2025,
                addressLine1 = "123 Main St",
                city = "San Francisco",
                state = "CA",
                zip = "94102",
                savedAt = System.currentTimeMillis() - 86400000, // 1 day ago
            ),
            SavedPaymentMethod(
                token = "mock_token_mastercard_5555",
                lastFourDigits = "5555",
                cardType = "mastercard",
                cardholderName = "Jane Smith",
                expiryMonth = 8,
                expiryYear = 2026,
                addressLine1 = "456 Oak Ave",
                city = "New York",
                state = "NY",
                zip = "10001",
                savedAt = System.currentTimeMillis() - 172800000, // 2 days ago
            ),
            SavedPaymentMethod(
                token = "mock_token_amex_0005",
                lastFourDigits = "0005",
                cardType = "american_express",
                cardholderName = "Bob Johnson",
                expiryMonth = 3,
                expiryYear = 2027,
                addressLine1 = "789 Pine St",
                city = "Los Angeles",
                state = "CA",
                zip = "90001",
                savedAt = System.currentTimeMillis() - 259200000, // 3 days ago
            ),
            SavedPaymentMethod(
                token = "mock_token_passcard_0008",
                lastFourDigits = "0008",
                cardType = "passcard",
                cardholderName = "Test Passcard",
                expiryMonth = 6,
                expiryYear = 2028,
                addressLine1 = "100 Test Ave",
                city = "Test City",
                state = "CA",
                zip = "90210",
                savedAt = System.currentTimeMillis() - 345600000, // 4 days ago
            ),
            SavedPaymentMethod(
                token = "mock_token_routex_0006",
                lastFourDigits = "0006",
                cardType = "routex",
                cardholderName = "Zero CVV Test",
                expiryMonth = 9,
                expiryYear = 2029,
                addressLine1 = "500 Zero CVV Lane",
                city = "Test City",
                state = "CA",
                zip = "94105",
                savedAt = System.currentTimeMillis() - 432000000, // 5 days ago
            ),
        )
}
