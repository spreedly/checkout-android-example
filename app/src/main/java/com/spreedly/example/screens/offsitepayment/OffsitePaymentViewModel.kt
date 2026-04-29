package com.spreedly.example.screens.offsitepayment

import android.content.Context
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.screens.common.Product
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.offsite.DocumentId
import com.spreedly.sdk.models.offsite.OffsitePaymentConfig
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the OffsitePaymentScreen demonstrating the offsite payment flow (Sprel, PayPal).
 *
 * ## Flow
 *
 * 1. User selects product and provider
 * 2. User taps "Start Payment"
 * 3. SDK creates offsite payment method token (CreatingPaymentMethod)
 * 4. App calls purchase API with token (Purchasing)
 * 5. Checkout presenter launches Custom Tab with checkout_url (Checkout)
 * 6. User completes payment on provider's page
 * 7. SDK verifies status and emits PaymentResult
 * 8. Return to Idle with success/failure message
 */
class OffsitePaymentViewModel(
    context: Context,
    sdk: Spreedly = Spreedly(),
    purchaseClient: SpreedlyPurchaseAPIClient = SpreedlyPurchaseAPIClient(),
) : BaseOffsitePaymentViewModel(
    context = context,
    sdk = sdk,
    purchaseClient = purchaseClient,
) {
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _selectedProvider = MutableStateFlow(OffsitePaymentMethodType.SPREL)
    val selectedProvider: StateFlow<OffsitePaymentMethodType> = _selectedProvider.asStateFlow()

    val products = listOf(
        Product("Sunglasses", "Premium UV protection", 4400, "🕶️"),
        Product("Watch", "Swiss precision", 19900, "⌚"),
        Product("Headphones", "Noise cancelling", 29900, "🎧"),
        Product("Camera", "Professional grade", 89900, "📷"),
        Product("Laptop", "Ultra portable", 129900, "💻"),
        Product("Phone", "Latest model", 99900, "📱"),
    )

    val providers = listOf(
        OffsitePaymentMethodType.SPREL,
        OffsitePaymentMethodType.PAYPAL,
    )

    private val providerDisplayName: String
        get() = when (_selectedProvider.value) {
            OffsitePaymentMethodType.PAYPAL -> "PayPal"
            OffsitePaymentMethodType.SPREL -> "Sprel"
            else -> _selectedProvider.value.rawValue
        }

    override fun hasSelectedProduct(): Boolean = _selectedProduct.value != null

    override suspend fun getOffsitePaymentConfig(): OffsitePaymentConfig = OffsitePaymentConfig(
            paymentMethodType = _selectedProvider.value,
            redirectUrl = SpreedlyPurchaseAPIClient.redirectUrl(context, "sprel/checkout"),
            email = "customer@example.com",
            fullName = "Ana Santos Araujo",
            documentId = DocumentId.standard("853.513.468-93"),
            country = "BR",
            phoneNumber = "8522847035",
            address1 = "Rua E, 1040",
            city = "Maracanaú",
            state = "CE",
            zip = "12345",
        )

    override suspend fun performPurchase(paymentMethodToken: String): String? {
        updateStage(Stage.PURCHASING)
        val product = _selectedProduct.value ?: return null
        val gateway = when (_selectedProvider.value) {
            OffsitePaymentMethodType.PAYPAL -> SpreedlyPurchaseAPIClient.GATEWAY_PAYPAL
            else -> null
        }
        return try {
            val response = purchaseClient.purchase(
                gateway = gateway,
                paymentMethodToken = paymentMethodToken,
                amount = product.price,
                redirectUrl = SpreedlyPurchaseAPIClient.redirectUrl(context, "sprel/checkout"),
            )
            if (response.transaction?.token == null) {
                updateError("Purchase failed while setting up a transaction")
                updateStage(Stage.IDLE)
                null
            } else {
                response.transaction?.token
            }
        } catch (e: Exception) {
            updateError("Purchase failed")
            updateStage(Stage.IDLE)
            null
        }
    }

    override fun handlePaymentCompleted(result: PaymentResult.Completed) {
        updateSuccess("Payment successful. The transaction has been completed successfully.")
        updateStage(Stage.IDLE)
    }

    override fun handlePaymentFailed(result: PaymentResult.Failed) {
        updateError(getErrorMessage(result))
        updateStage(Stage.IDLE)
    }

    override fun handlePaymentCanceled() {
        updateError("$providerDisplayName checkout failed.")
        updateStage(Stage.IDLE)
    }

    private fun getErrorMessage(result: PaymentResult.Failed): String = when {
        result.state == "gateway_processing_failed" ->
            "We couldn't complete your $providerDisplayName payment. Please try again."

        result.state == "processing" ->
            "Your $providerDisplayName payment is currently being processed. Please wait a moment."

        result.state == "pending" ->
            "Your payment is pending. Please try again shortly."

        result.message != null -> result.message!!
        else -> "$providerDisplayName checkout failed."
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        clearMessages()
    }

    fun selectProvider(provider: OffsitePaymentMethodType) {
        _selectedProvider.value = provider
        clearMessages()
    }

    override fun resetSelection() {
        _selectedProduct.value = null
    }
}
