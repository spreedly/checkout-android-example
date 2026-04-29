package com.spreedly.example.screens.common

/**
 * Shared product model for payment demo screens (Offsite, EBANX, Stripe APM).
 *
 * @property name Display name of the product
 * @property description Short description
 * @property price Price in smallest currency unit (e.g. cents: 4400 = $44.00)
 * @property emoji Emoji icon for display
 */
data class Product(
    val name: String,
    val description: String,
    val price: Int,
    val emoji: String,
)
