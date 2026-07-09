package com.spreedly.example.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for the merchant backend purchase API.
 *
 * **Endpoint:** `POST {merchant_base_url}/api/v1/create-purchase`
 *
 * @property gateway Gateway identifier (e.g. "paypal", "stripe"), or null for default test gateway
 * @property transaction The transaction body containing payment details
 */
@Serializable
data class SpreedlyPurchaseTransactionRequest(
    val gateway: String? = null,
    val transaction: SpreedlyPurchaseTransactionBody,
)

/**
 * Transaction body for the purchase request.
 *
 * Fields are based on the iOS reference document for offsite payments.
 *
 * @property amount The amount in cents (e.g., 4400 = $44.00)
 * @property browserInfo Base64-encoded browser info (can be empty for mobile)
 * @property currencyCode The 3-letter currency code (e.g., "USD")
 * @property paymentMethodToken The payment method token from tokenization step
 * @property ip The customer's IP address (can use "127.0.0.1" for testing)
 * @property redirectUrl The URL to redirect to after checkout completion
 * @property channel The channel identifier (typically "app" for mobile)
 * @property callbackUrl The callback URL for webhooks (can be any valid URL)
 */
@Serializable
data class SpreedlyPurchaseTransactionBody(
    val amount: Int,
    @SerialName("browser_info")
    val browserInfo: String = "",
    @SerialName("currency_code")
    val currencyCode: String = "USD",
    @SerialName("payment_method_token")
    val paymentMethodToken: String,
    val ip: String = "127.0.0.1",
    @SerialName("redirect_url")
    val redirectUrl: String,
    val channel: String = DEFAULT_CHANNEL,
    @SerialName("callback_url")
    val callbackUrl: String = DEFAULT_CALLBACK_URL,
)

/**
 * Response model for direct Spreedly Core purchase API.
 *
 * The response contains a transaction object with the state and token needed
 * for the checkout flow.
 */
@Serializable
data class SpreedlyPurchaseResponse(
    val transaction: SpreedlyPurchaseTransaction? = null,
    val errors: List<PurchaseApiError>? = null,
)

/**
 * Transaction details from the purchase response.
 *
 * Key fields:
 * - [token]: Used to check status and launch checkout
 * - [state]: Current transaction state (e.g., "pending", "gateway_processing_result_unknown")
 * - [succeeded]: Whether the purchase succeeded
 * - [checkoutUrl]: URL for offsite/EBANX flows to open in browser/Custom Tab
 * - [message]: Status message from the gateway
 */
@Serializable
data class SpreedlyPurchaseTransaction(
    val token: String,
    val state: String? = null,
    val succeeded: Boolean? = null,
    val message: String? = null,
    @SerialName("checkout_url")
    val checkoutUrl: String? = null,
    @SerialName("payment_method")
    val paymentMethod: SpreedlyPurchasePaymentMethod? = null,
    @SerialName("gateway_specific_response_fields")
    val gatewaySpecificResponseFields: GatewaySpecificResponseFields? = null,
)

/**
 * Gateway-specific response fields from Spreedly purchase response.
 */
@Serializable
data class GatewaySpecificResponseFields(
    @SerialName("stripe_payment_intents")
    val stripePaymentIntents: StripePaymentIntents? = null,
    val braintree: BraintreeResponseFields? = null,
)

@Serializable
data class StripePaymentIntents(
    @SerialName("client_secret")
    val clientSecret: String? = null,
)

@Serializable
data class BraintreeResponseFields(
    @SerialName("client_token")
    val clientToken: String? = null,
)

/**
 * Payment method details in the purchase response.
 */
@Serializable
data class SpreedlyPurchasePaymentMethod(
    val token: String,
    @SerialName("payment_method_type")
    val paymentMethodType: String? = null,
)

// ============================================================================
// EBANX-specific models
// ============================================================================

/**
 * Request model for EBANX purchase via the merchant backend.
 *
 * EBANX requires different fields than standard offsite payments:
 * - No `browser_info` or `ip` required
 * - Uses `gateway_specific_fields.ebanx.document` for CPF (except OXXO)
 * - Currency is BRL (Brazil) or MXN (Mexico/OXXO)
 *
 * **Endpoint:** `POST {merchant_base_url}/api/v1/create-purchase`
 */
@Serializable
data class EbanxPurchaseTransactionRequest(
    val gateway: String? = null,
    val transaction: EbanxPurchaseTransactionBody,
)

/**
 * Transaction body for EBANX purchase request.
 *
 * @property amount The amount in cents (e.g., 9900 = R$99.00 or MX$99.00)
 * @property currencyCode "BRL" for Pix/Boleto/NuPay, "MXN" for OXXO
 * @property paymentMethodToken The payment method token from tokenization step
 * @property redirectUrl The deep link URL for app return (e.g., "{applicationId}.spreedlyoffsite://...")
 * @property callbackUrl Server-side webhook URL
 * @property channel Always "app" for mobile
 * @property gatewaySpecificFields Contains document for Pix/Boleto/NuPay; null for OXXO
 */
@Serializable
data class EbanxPurchaseTransactionBody(
    val amount: Int,
    @SerialName("currency_code")
    val currencyCode: String,
    @SerialName("payment_method_token")
    val paymentMethodToken: String,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("callback_url")
    val callbackUrl: String = DEFAULT_CALLBACK_URL,
    val channel: String = DEFAULT_CHANNEL,
    @SerialName("gateway_specific_fields")
    val gatewaySpecificFields: EbanxGatewaySpecificFields? = null,
)

/**
 * Wrapper for EBANX gateway-specific fields.
 *
 * This is only included when a document (CPF) is required.
 * OXXO does NOT require this field.
 */
@Serializable
data class EbanxGatewaySpecificFields(
    val ebanx: EbanxFields,
)

/**
 * EBANX-specific fields containing the customer's document (CPF).
 *
 * @property document The customer's CPF/taxpayer ID (e.g., "853.513.468-93")
 *                    Required for Pix, Boleto, NuPay. NOT sent for OXXO.
 */
@Serializable
data class EbanxFields(
    val document: String,
)

// ============================================================================
// Stripe APM-specific models
// ============================================================================

/**
 * Request model for Stripe APM purchase via the merchant backend.
 *
 * Stripe APM creates a pending purchase directly (no tokenization step).
 * Uses payment_method with payment_method_type "stripe_apm" and apm_types.
 *
 * **Endpoint:** `POST {merchant_base_url}/api/v1/create-purchase`
 */
@Serializable
data class StripeAPMPurchaseTransactionRequest(
    val gateway: String? = null,
    val transaction: StripeAPMPurchaseTransactionBody,
)

/**
 * Transaction body for Stripe APM purchase request.
 *
 * @property amount Amount in smallest currency unit (e.g. cents)
 * @property currencyCode e.g. "EUR" (determines which APMs are available)
 * @property channel Use "app" for mobile
 * @property redirectUrl Used by Stripe for redirect-based APMs; user returns to app via this
 * @property callbackUrl Server-to-server notification of transaction result
 * @property paymentMethod Must contain payment_method_type "stripe_apm" and apm_types
 */
@Serializable
data class StripeAPMPurchaseTransactionBody(
    val amount: Int,
    @SerialName("currency_code")
    val currencyCode: String,
    val channel: String = DEFAULT_CHANNEL,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("callback_url")
    val callbackUrl: String = DEFAULT_CALLBACK_URL,
    @SerialName("payment_method")
    val paymentMethod: StripeAPMPaymentMethod,
    @SerialName("gateway_specific_fields")
    val gatewaySpecificFields: StripeGatewaySpecificFields? = null,
)

@Serializable
data class StripeAPMPaymentMethod(
    @SerialName("payment_method_type")
    val paymentMethodType: String = "stripe_apm",
    @SerialName("apm_types")
    val apmTypes: List<String>,
)

@Serializable
data class StripeGatewaySpecificFields(
    @SerialName("stripe_payment_intents")
    val stripePaymentIntents: StripeRadarFields,
)

@Serializable
data class StripeRadarFields(
    @SerialName("radar_session_id")
    val radarSessionId: String,
)

// ============================================================================
// Braintree-specific models
// ============================================================================

/**
 * Request model for Braintree purchase via the merchant backend.
 *
 * Creates a pending purchase with payment_method_type "paypal" or "venmo"
 * on the Braintree gateway. No prior tokenization step is needed.
 *
 * **Endpoint:** `POST {merchant_base_url}/api/v1/create-purchase`
 */
@Serializable
data class BraintreePurchaseTransactionRequest(
    val gateway: String? = null,
    val transaction: BraintreePurchaseTransactionBody,
)

/**
 * Transaction body for Braintree purchase request.
 *
 * @property amount Amount in smallest currency unit (e.g. cents)
 * @property currencyCode e.g. "USD"
 * @property paymentMethod Must contain payment_method_type "paypal" or "venmo" with offsite_sync
 * @property gatewaySpecificFields Braintree-specific fields (e.g. venmo_flow_type, paypal_flow_type)
 */
@Serializable
data class BraintreePurchaseTransactionBody(
    val amount: Int,
    @SerialName("currency_code")
    val currencyCode: String = "USD",
    @SerialName("payment_method")
    val paymentMethod: BraintreePaymentMethod,
    @SerialName("gateway_specific_fields")
    val gatewaySpecificFields: BraintreeGatewaySpecificFields? = null,
)

@Serializable
data class BraintreePaymentMethod(
    @SerialName("payment_method_type")
    val paymentMethodType: String,
    @SerialName("offsite_sync")
    val offsiteSync: Boolean = true,
)

@Serializable
data class BraintreeGatewaySpecificFields(
    val braintree: BraintreeFields,
)

@Serializable
data class BraintreeFields(
    @SerialName("paypal_flow_type")
    val paypalFlowType: String? = null,
    @SerialName("venmo_flow_type")
    val venmoFlowType: String? = null,
    @SerialName("venmo_profile_id")
    val venmoProfileId: String? = null,
)

/**
 * Braintree callback state sent when confirming a transaction.
 *
 * Maps to the `state` field in the Spreedly `/confirm` request body.
 * Serialized via `@SerialName` (e.g. `SUCCESSFUL` -> `"Successful"`).
 */
@Serializable
enum class BraintreeConfirmState {
    @SerialName("Successful")
    SUCCESSFUL,

    @SerialName("Failed")
    FAILED,

    @SerialName("Cancelled")
    CANCELLED,
}

/**
 * Request model for confirming a Braintree offsite transaction.
 *
 * Must be sent for **all** callback states (Successful, Failed, Cancelled).
 * Body is sent as a flat JSON object (NOT wrapped in "transaction" or nested objects).
 *
 * - **[BraintreeConfirmState.SUCCESSFUL]**: includes [nonce] and [paymentMethodType]
 * - **[BraintreeConfirmState.FAILED] / [BraintreeConfirmState.CANCELLED]**: includes [message]
 *   and [paymentMethodType] (no nonce)
 *
 * **Endpoint:** `POST {merchant_base_url}/api/v1/transactions/{token}/confirm`
 */
@Serializable
data class BraintreeConfirmRequest(
    val state: BraintreeConfirmState,
    val nonce: String? = null,
    val message: String? = null,
    @SerialName("payment_method_type")
    val paymentMethodType: String,
)
