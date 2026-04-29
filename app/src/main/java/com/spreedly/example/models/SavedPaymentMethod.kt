package com.spreedly.example.models

import kotlinx.serialization.Serializable

/**
 * Data model representing a saved payment method.
 *
 * This is used in the sample app to demonstrate the recaching functionality.
 * In a production app, this data would come from your backend API.
 *
 * The SDK automatically calculates CVV length from the card type, so it's not
 * stored in this model.
 *
 * @property token The payment method token from Spreedly
 * @property lastFourDigits Last four digits of the card number
 * @property cardType Type of card (e.g., "visa", "mastercard", "amex").
 *                    SDK uses this to determine CVV length automatically.
 * @property cardholderName Name on the card
 * @property expiryMonth Expiration month (1-12)
 * @property expiryYear Expiration year (4-digit year)
 * @property addressLine1 Billing address line 1
 * @property city Billing city
 * @property state Billing state/province
 * @property zip Billing ZIP/postal code
 * @property savedAt Timestamp when the payment method was saved
 */
@Serializable
data class SavedPaymentMethod(
    val token: String,
    val lastFourDigits: String,
    val cardType: String,
    val cardholderName: String? = null,
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val addressLine1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
) {
    /**
     * Get formatted expiry date string (MM/YY).
     */
    fun getFormattedExpiry(): String? = if (expiryMonth != null && expiryYear != null) {
            val monthStr = expiryMonth.toString().padStart(2, '0')
            val yearStr = (expiryYear % 100).toString().padStart(2, '0')
            "$monthStr/$yearStr"
        } else {
            null
        }

    /**
     * Get formatted card type (capitalized).
     */
    fun getFormattedCardType(): String = cardType.replaceFirstChar { it.uppercase() }
}
