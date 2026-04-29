package com.spreedly.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from PUT /api/v1/payment_methods/{token}/retain endpoint.
 *
 * Returns success status after retaining a payment method.
 */
@Serializable
data class RetainPaymentMethodResponse(
    @SerialName("success")
    val success: Boolean = true,
    @SerialName("message")
    val message: String? = null,
    @SerialName("payment_method")
    val paymentMethod: BackendPaymentMethod? = null,
)
