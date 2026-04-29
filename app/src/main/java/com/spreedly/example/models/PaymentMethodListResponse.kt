package com.spreedly.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from GET /api/v1/payment_methods endpoint.
 *
 * Contains a list of retained payment methods in descending order (latest first).
 */
@Serializable
data class PaymentMethodListResponse(
    @SerialName("payment_methods")
    val paymentMethods: List<BackendPaymentMethod> = emptyList(),
    @SerialName("count")
    val count: Int? = null,
)
