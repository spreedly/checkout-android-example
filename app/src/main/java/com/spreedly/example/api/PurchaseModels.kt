package com.spreedly.example.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Purchase API Models
 *
 * Matches iOS implementation: PurchaseModels.swift
 *
 * This wraps the transaction data in a "transaction" object for Spreedly Core API
 *
 * Device fingerprint form data (matches iOS DeviceFingerprintForm).
 */
@Serializable
data class DeviceFingerprintForm(
    @SerialName("cdata")
    val cdata: String? = null,
)

@Serializable
data class PurchaseTransactionRequest(
    @SerialName("transaction")
    val transaction: TransactionData,
)

@Serializable
data class TransactionData(
    @SerialName("amount")
    val amount: Int, // Amount in cents
    @SerialName("currency_code")
    val currencyCode: String,
    @SerialName("payment_method_token")
    val paymentMethodToken: String,
    @SerialName("ip")
    val ip: String = "127.0.0.1",
    @SerialName("browser_info")
    val browserInfo: String = "eyJ3aWR0aCI6MTUxMiwiaGVpZ2h0Ijo5ODIsImRlcHRoIjozMCwidGltZXpvbmUiOi" +
        "0zMzAsInVzZXJfYWdlbnQiOiJNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMF8xNV83KSBBcHBsZ" +
        "VdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTQzLjAuMC4wIFNhZmFyaS81MzcuMzYiLCJqYXZh" +
        "IjpmYWxzZSwibGFuZ3VhZ2UiOiJlbi1VUyIsImJyb3dzZXJfc2l6ZSI6IjA0IiwiYWNjZXB0X2hlYWRlciI6InRleHQvaHRtbC" +
        "xhcHBsaWNhdGlvbi94aHRtbCt4bWw7cT0wLjksKi8qO3E9MC44In0=",
    @SerialName("three_ds_version")
    val threeDsVersion: String = "2",
    @SerialName("attempt_3dsecure")
    val attempt3dsecure: Boolean? = null, // Gateway-specific: true, Global: null (omitted)
    @SerialName("callback_url")
    val callbackUrl: String = "https://example.com/spreedly/callback",
    @SerialName("redirect_url")
    val redirectUrl: String,
)

// MARK: - Purchase Response Model
@Serializable
data class PurchaseResponse(
    @SerialName("transaction")
    val transaction: PurchaseTransaction? = null,
    @SerialName("errors")
    val errors: List<PurchaseApiError>? = null,
)

// MARK: - Purchase Transaction (Response)
@Serializable
data class PurchaseTransaction(
    @SerialName("on_test_gateway")
    val onTestGateway: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("succeeded")
    val succeeded: Boolean,
    @SerialName("state")
    val state: String? = null,
    @SerialName("token")
    val token: String,
    @SerialName("transaction_type")
    val transactionType: String? = null,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("ip")
    val ip: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("merchant_name_descriptor")
    val merchantNameDescriptor: String? = null,
    @SerialName("merchant_location_descriptor")
    val merchantLocationDescriptor: String? = null,
    @SerialName("merchant_profile_key")
    val merchantProfileKey: String? = null,
    @SerialName("gateway_transaction_id")
    val gatewayTransactionId: String? = null,
    @SerialName("sub_merchant_key")
    val subMerchantKey: String? = null,
    @SerialName("gateway_latency_ms")
    val gatewayLatencyMs: Int? = null,
    @SerialName("warning")
    val warning: String? = null,
    @SerialName("application_id")
    val applicationId: String? = null,
    @SerialName("amount")
    val amount: Int? = null,
    @SerialName("local_amount")
    val localAmount: Int? = null,
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("retain_on_success")
    val retainOnSuccess: Boolean? = null,
    @SerialName("payment_method_added")
    val paymentMethodAdded: Boolean? = null,
    @SerialName("smart_routed")
    val smartRouted: Boolean? = null,
    @SerialName("stored_credential_initiator")
    val storedCredentialInitiator: String? = null,
    @SerialName("stored_credential_reason_type")
    val storedCredentialReasonType: String? = null,
    @SerialName("stored_credential_alternate_gateway")
    val storedCredentialAlternateGateway: String? = null,
    @SerialName("stored_credential_final_payment")
    val storedCredentialFinalPayment: Boolean? = null,
    @SerialName("message_key")
    val messageKey: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("gateway_token")
    val gatewayToken: String? = null,
    @SerialName("gateway_type")
    val gatewayType: String? = null,
    @SerialName("shipping_address")
    val shippingAddress: ShippingAddress? = null,
    @SerialName("api_urls")
    val apiUrls: List<ApiUrl>? = null,
    @SerialName("attempt_3dsecure")
    val attempt3dsecure: Boolean? = null,
    @SerialName("payment_method")
    val paymentMethod: PaymentMethod? = null,
    @SerialName("sca_authentication")
    val scaAuthentication: SCAAuthentication? = null,
    @SerialName("required_action")
    val requiredAction: String? = null,
    @SerialName("challenge_url")
    val challengeUrl: String? = null,
    @SerialName("device_fingerprint_form")
    val deviceFingerprintForm: DeviceFingerprintForm? = null,
    @SerialName("device_fingerprint_form_embed_url")
    val deviceFingerprintFormEmbedUrl: String? = null,
)

// MARK: - SCA Authentication
@Serializable
data class SCAAuthentication(
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("succeeded")
    val succeeded: Boolean? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("token")
    val token: String? = null,
    @SerialName("flow_performed")
    val flowPerformed: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("sca_provider_key")
    val scaProviderKey: String? = null,
    @SerialName("amount")
    val amount: Int? = null,
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("ip")
    val ip: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("three_ds_version")
    val threeDsVersion: String? = null,
    @SerialName("ecommerce_indicator")
    val ecommerceIndicator: String? = null,
    @SerialName("authentication_value")
    val authenticationValue: String? = null,
    @SerialName("directory_server_transaction_id")
    val directoryServerTransactionId: String? = null,
    @SerialName("authentication_value_algorithm")
    val authenticationValueAlgorithm: String? = null,
    @SerialName("directory_response_status")
    val directoryResponseStatus: String? = null,
    @SerialName("authentication_response_status")
    val authenticationResponseStatus: String? = null,
    @SerialName("required_action")
    val requiredAction: String? = null,
    @SerialName("acs_reference_number")
    val acsReferenceNumber: String? = null,
    @SerialName("acs_rendering_type")
    val acsRenderingType: String? = null,
    @SerialName("acs_signed_content")
    val acsSignedContent: String? = null,
    @SerialName("acs_transaction_id")
    val acsTransactionId: String? = null,
    @SerialName("sdk_transaction_id")
    val sdkTransactionId: String? = null,
    @SerialName("challenge_form")
    val challengeForm: String? = null,
    @SerialName("challenge_form_embed_url")
    val challengeFormEmbedUrl: String? = null,
    @SerialName("three_ds_server_trans_id")
    val threeDsServerTransId: String? = null,
    @SerialName("xid")
    val xid: String? = null,
    @SerialName("enrolled")
    val enrolled: String? = null,
    @SerialName("transaction_type")
    val transactionType: String? = null,
    @SerialName("gateway_transaction_key")
    val gatewayTransactionKey: String? = null,
    @SerialName("callback_url")
    val callbackUrl: String? = null,
    @SerialName("test_scenario")
    val testScenario: String? = null,
    @SerialName("three_ds_requestor_challenge_ind")
    val threeDsRequestorChallengeInd: String? = null,
    @SerialName("trans_status_reason")
    val transStatusReason: String? = null,
    @SerialName("exemption_type")
    val exemptionType: String? = null,
    @SerialName("acquiring_bank_fraud_rate")
    val acquiringBankFraudRate: String? = null,
    @SerialName("warning")
    val warning: String? = null,
    @SerialName("daf")
    val daf: Boolean? = null,
    @SerialName("force_daf")
    val forceDaf: Boolean? = null,
    @SerialName("managed_order_token")
    val managedOrderToken: String? = null,
    @SerialName("payment_method_key")
    val paymentMethodKey: String? = null,
)

// MARK: - Shipping Address
@Serializable
data class ShippingAddress(
    @SerialName("name")
    val name: String? = null,
    @SerialName("address1")
    val address1: String? = null,
    @SerialName("address2")
    val address2: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("zip")
    val zip: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
)

// MARK: - API URL
@Serializable
data class ApiUrl(
    @SerialName("referencing_transaction")
    val referencingTransaction: List<String>? = null,
    @SerialName("failover_transaction")
    val failoverTransaction: List<String>? = null,
)

// MARK: - Payment Method (minimal for response)
@Serializable
data class PaymentMethod(
    @SerialName("token")
    val token: String? = null,
    @SerialName("card_type")
    val cardType: String? = null,
    @SerialName("last_four_digits")
    val lastFourDigits: String? = null,
)

// MARK: - Transaction Complete Response

/**
 * Response from /complete.json endpoint.
 * Uses PurchaseTransaction (not TransactionStatus) because complete API
 * returns device_fingerprint_form as an object, not a string.
 */
@Serializable
data class TransactionCompleteResponse(
    @SerialName("transaction")
    val transaction: PurchaseTransaction? = null,
    @SerialName("errors")
    val errors: List<PurchaseApiError>? = null,
)

// MARK: - Error Response with Details
@Serializable
data class PurchaseErrorResponse(
    @SerialName("error")
    val error: String? = null,
    @SerialName("details")
    val details: ErrorDetails? = null,
)

@Serializable
data class ErrorDetails(
    @SerialName("transaction")
    val transaction: PurchaseTransaction? = null,
    @SerialName("error")
    val error: String? = null,
)

/**
 * Request model for the merchant backend offsite-purchase endpoint.
 *
 * Flat JSON structure (no "transaction" wrapper).
 *
 * **Endpoint:** `POST {merchant_base_url}/offsite-purchase`
 */
@Serializable
data class MerchantOffsitePurchaseRequest(
    val gateway: String,
    @SerialName("payment_method_token")
    val paymentMethodToken: String,
    val amount: Int,
    @SerialName("currency_code")
    val currencyCode: String,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("callback_url")
    val callbackUrl: String = DEFAULT_CALLBACK_URL,
    val channel: String = DEFAULT_CHANNEL,
)
