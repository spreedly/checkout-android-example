package com.spreedly.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payment method structure from backend API.
 *
 * Represents a payment method as returned by the GET /api/v1/payment_methods endpoint.
 * Supports multiple payment method types (credit cards, bank accounts, etc.).
 */
@Serializable
data class BackendPaymentMethod(
    @SerialName("token")
    val token: String,
    @SerialName("payment_method_type")
    val paymentMethodType: String,
    @SerialName("last_four_digits")
    val lastFourDigits: String? = null,
    @SerialName("card_type")
    val cardType: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("month")
    val month: Int? = null,
    @SerialName("year")
    val year: Int? = null,
    @SerialName("address1")
    val address1: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("zip")
    val zip: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("verification_value_length")
    val verificationValueLength: Int? = null,
) {
    /**
     * Check if this is a credit card payment method.
     */
    fun isCreditCard(): Boolean = paymentMethodType == "credit_card"

    /**
     * Convert backend payment method to SavedPaymentMethod model.
     * Only supports credit card payment methods.
     *
     * @throws IllegalStateException if called on a non-credit-card payment method
     */
    fun toSavedPaymentMethod(): SavedPaymentMethod {
        require(isCreditCard()) {
            "Cannot convert non-credit-card payment method (type: $paymentMethodType) to SavedPaymentMethod"
        }
        requireNotNull(lastFourDigits) { "lastFourDigits is required for credit card payment methods" }
        requireNotNull(cardType) { "cardType is required for credit card payment methods" }

        return SavedPaymentMethod(
            token = token,
            lastFourDigits = lastFourDigits,
            cardType = cardType,
            cardholderName = fullName,
            expiryMonth = month,
            expiryYear = year,
            addressLine1 = address1,
            city = city,
            state = state,
            zip = zip,
            savedAt = System.currentTimeMillis(),
        )
    }
}
