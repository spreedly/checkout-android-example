package com.spreedly.example.models

/**
 * Saved card model for payment method selection
 */
data class SavedCard(
    val id: String,
    val paymentMethodToken: String,
    val lastFourDigits: String,
    val cardType: String, // e.g., "Visa", "Mastercard"
    val cardBrand: String, // e.g., "visa", "mastercard" (for icon mapping)
    val expiryMonth: String?,
    val expiryYear: String?,
) {
    /**
     * Display name for the card (e.g., "Visa ****1234")
     */
    val displayName: String
        get() = "$cardType ****$lastFourDigits"

    /**
     * Display expiry (e.g., "12/25")
     */
    val displayExpiry: String?
        get() = if (expiryMonth != null && expiryYear != null) {
            "$expiryMonth/${expiryYear.takeLast(2)}"
        } else {
            null
        }
}
