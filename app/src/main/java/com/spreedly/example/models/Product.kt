package com.spreedly.example.models

/**
 * Product model for 3DS payment flow demo
 */
data class Product(
    val id: String,
    val name: String,
    val displayPrice: Int, // Price in cents for UI display (e.g., 300100 = $3,001.00)
    val apiAmount: Int, // Actual amount to send to Spreedly API (e.g., 3001 = $30.01)
    val description: String,
    val iconName: String, // Material icon name
) {
    /**
     * Format display price as dollars for UI
     */
    fun formattedPrice(): String {
        val dollars = displayPrice / 100.0
        return "$${"%.2f".format(dollars)}"
    }
}

/**
 * Demo products for 3DS payment flow
 *
 * These use specific amounts (in cents) that trigger different Gateway-Specific 3DS2 flows
 * when used with Spreedly Test Gateway.
 *
 * See: https://developer.spreedly.com/docs/gateway-specific-3ds2-guide#test-data-for-spreedly-3ds2-gateway-specific
 */
object DemoProducts {
    val globalProducts = listOf(
        Product(
            id = "prod_1",
            name = "Wireless Earbuds",
            displayPrice = 12900, // UI shows $129.00
            apiAmount = 12900, // API sends 12900 cents ($129.00)
            description = "Premium wireless earbuds with active noise cancellation",
            iconName = "headphones",
        ),
        Product(
            id = "prod_2",
            name = "Smart Watch",
            displayPrice = 39900, // UI shows $399.00
            apiAmount = 39900, // API sends 39900 cents ($399.00)
            description = "Feature-rich smartwatch with health tracking and fitness monitoring",
            iconName = "watch",
        ),
        Product(
            id = "prod_3",
            name = "Tablet",
            displayPrice = 49900, // UI shows $499.00
            apiAmount = 49900, // API sends 49900 cents ($499.00)
            description = "High-performance tablet with stunning display",
            iconName = "tablet",
        ),
        Product(
            id = "prod_4",
            name = "Laptop",
            displayPrice = 129900, // UI shows $1,299.00
            apiAmount = 129900, // API sends 129900 cents ($1,299.00)
            description = "Powerful laptop for work and creativity",
            iconName = "laptop",
        ),
        Product(
            id = "prod_5",
            name = "Smart Speaker",
            displayPrice = 9900, // UI shows $99.00
            apiAmount = 9900, // API sends 9900 cents ($99.00)
            description = "Voice-controlled smart speaker with premium sound",
            iconName = "speaker",
        ),
        Product(
            id = "prod_6",
            name = "Gaming Console",
            displayPrice = 49900, // UI shows $499.00
            apiAmount = 49900, // API sends 49900 cents ($499.00)
            description = "Next-generation gaming console with immersive gameplay",
            iconName = "videogame_asset",
        ),
    )

    val products = listOf(
        Product(
            id = "prod_3001",
            name = "Frictionless",
            displayPrice = 300100, // UI shows $3,001.00
            apiAmount = 3001, // API sends 3001 cents ($30.01) - 3DS2 full frictionless
            description = "Tests frictionless 3DS flow - succeeds immediately without challenges",
            iconName = "check_circle",
        ),
        Product(
            id = "prod_3003",
            name = "Fingerprint + Direct Auth",
            displayPrice = 300300, // UI shows $3,003.00
            apiAmount = 3003, // API sends 3003 cents ($30.03) - Device fingerprint → direct authorize
            description = "Tests device fingerprint flow with direct authorization (requires lifecycle & completion)",
            iconName = "fingerprint",
        ),
        Product(
            id = "prod_3004",
            name = "Fingerprint + Challenge",
            displayPrice = 300400, // UI shows $3,004.00
            apiAmount = 3004, // API sends 3004 cents ($30.04) - Device fingerprint → challenge
            description = "Tests device fingerprint flow leading to challenge (requires lifecycle & completion)",
            iconName = "security",
        ),
        Product(
            id = "prod_3005",
            name = "Direct Challenge",
            displayPrice = 300500, // UI shows $3,005.00
            apiAmount = 3005, // API sends 3005 cents ($30.05) - Direct challenge
            description = "Tests direct challenge flow without device fingerprint (requires lifecycle)",
            iconName = "shield",
        ),
        Product(
            id = "prod_3103",
            name = "Fingerprint + Forced Failure",
            displayPrice = 310300, // UI shows $3,103.00
            apiAmount = 3103, // API sends 3103 cents ($31.03) - Device fingerprint with forced failure
            description = "Tests device fingerprint flow with forced failure (requires lifecycle & completion)",
            iconName = "error",
        ),
        Product(
            id = "prod_3104",
            name = "Challenge + Forced Failure",
            displayPrice = 310400, // UI shows $3,104.00
            apiAmount = 3104, // API sends 3104 cents ($31.04) - Challenge with forced failure
            description = "Tests challenge flow with forced failure (requires lifecycle)",
            iconName = "cancel",
        ),
    )
}
