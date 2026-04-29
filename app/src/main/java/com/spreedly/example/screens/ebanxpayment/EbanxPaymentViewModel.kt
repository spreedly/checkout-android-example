package com.spreedly.example.screens.ebanxpayment

import android.content.Context
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.screens.common.Product
import com.spreedly.example.screens.offsitepayment.BaseOffsitePaymentViewModel
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.offsite.DocumentId
import com.spreedly.sdk.models.offsite.OffsitePaymentConfig
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the EbanxPaymentScreen demonstrating the EBANX payment flow.
 *
 * Supports Pix, Boleto Bancario, OXXO, and NuPay payment methods.
 *
 * ## EBANX-Specific Behavior
 *
 * - **Currency**: OXXO uses MXN, all others use BRL
 * - **Document**: OXXO does NOT send document; Pix/Boleto/NuPay send CPF
 * - **Pending state**: Treated as soft success (offline payment methods like Boleto/OXXO)
 */
class EbanxPaymentViewModel(
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

    private val _selectedProvider = MutableStateFlow(OffsitePaymentMethodType.PIX)
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
        OffsitePaymentMethodType.PIX,
        OffsitePaymentMethodType.BOLETO_BANCARIO,
        OffsitePaymentMethodType.OXXO,
        OffsitePaymentMethodType.NUPAY,
    )

    private val ebanxMethodDisplayName: String
        get() = when (_selectedProvider.value) {
            OffsitePaymentMethodType.PIX -> "Pix"
            OffsitePaymentMethodType.BOLETO_BANCARIO -> "Boleto Bancario"
            OffsitePaymentMethodType.OXXO -> "OXXO"
            OffsitePaymentMethodType.NUPAY -> "NuPay"
            else -> _selectedProvider.value.rawValue
        }

    override fun hasSelectedProduct(): Boolean = _selectedProduct.value != null

    override suspend fun getOffsitePaymentConfig(): OffsitePaymentConfig = EbanxConfigBuilder
        .buildConfig(
            _selectedProvider.value,
            SpreedlyPurchaseAPIClient.redirectUrl(context, "ebanx/checkout"),
        )

    override suspend fun performPurchase(paymentMethodToken: String): String? {
        updateStage(Stage.PURCHASING)
        val product = _selectedProduct.value ?: return null
        val provider = _selectedProvider.value
        val currencyCode = EbanxConfigBuilder.currencyCode(provider)
        val document = EbanxConfigBuilder.document(provider)
        return try {
            val response = purchaseClient.ebanxPurchase(
                paymentMethodToken = paymentMethodToken,
                amount = product.price,
                currencyCode = currencyCode,
                redirectUrl = SpreedlyPurchaseAPIClient.redirectUrl(context, "ebanx/checkout"),
                document = document,
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
        val message = when (result.state) {
            "succeeded" ->
                "Payment successful. The transaction has been completed successfully."

            "processing" ->
                "Payment accepted and is being processed. Final confirmation may take a few days."

            "pending" ->
                "Payment submitted. Awaiting final confirmation from the payment provider."

            else ->
                "Payment successful. The transaction has been completed successfully."
        }
        updateSuccess(message)
        updateStage(Stage.IDLE)
    }

    override fun handlePaymentFailed(result: PaymentResult.Failed) {
        val message = when (result.state) {
            "gateway_processing_failed" ->
                "We couldn't complete your $ebanxMethodDisplayName payment. Please try again."

            "pending" ->
                "Your payment is pending. Please try again shortly."

            "canceled" ->
                "$ebanxMethodDisplayName payment was canceled."

            else ->
                result.message ?: "$ebanxMethodDisplayName payment failed."
        }
        updateError(message)
        updateStage(Stage.IDLE)
    }

    override fun handlePaymentCanceled() {
        updateError("$ebanxMethodDisplayName payment was canceled.")
        updateStage(Stage.IDLE)
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
        _selectedProvider.value = OffsitePaymentMethodType.PIX
    }
}

/**
 * Builder object for creating EBANX-specific OffsitePaymentConfig.
 *
 * Encapsulates the per-provider configuration logic for testability.
 */
internal object EbanxConfigBuilder {
    private const val TEST_EMAIL = "test@test.com"
    private const val TEST_CPF = "853.513.468-93"

    // Brazil test data
    private const val BR_NAME = "Ana Santos Araujo"
    private const val BR_PHONE = "8522847035"
    private const val BR_ADDRESS = "Rua E, 1040"
    private const val BR_CITY = "Maracanaú"
    private const val BR_STATE = "CE"
    private const val BR_ZIP = "12345"

    // Mexico test data (for OXXO)
    private const val MX_NAME = "Manuela E. Beyer Rocabado"
    private const val MX_PHONE = "(040) 577-7687"
    private const val MX_ADDRESS = "Oyono, 882"
    private const val MX_CITY = "Hermosillo"
    private const val MX_STATE = "Sonora"
    private const val MX_ZIP = "48822"

    /**
     * Build OffsitePaymentConfig for the given EBANX provider.
     */
    fun buildConfig(
        provider: OffsitePaymentMethodType,
        redirectUrl: String,
    ): OffsitePaymentConfig = when (provider) {
            OffsitePaymentMethodType.PIX -> buildPixConfig(redirectUrl)
            OffsitePaymentMethodType.BOLETO_BANCARIO -> buildBoletoConfig(redirectUrl)
            OffsitePaymentMethodType.OXXO -> buildOxxoConfig(redirectUrl)
            OffsitePaymentMethodType.NUPAY -> buildNupayConfig(redirectUrl)
            else -> throw IllegalArgumentException("Unsupported EBANX provider: $provider")
        }

    /**
     * Get the currency code for the given provider.
     */
    fun currencyCode(provider: OffsitePaymentMethodType): String = when (provider) {
            OffsitePaymentMethodType.OXXO -> "MXN"
            else -> "BRL"
        }

    /**
     * Get the document (CPF) for the given provider.
     * Returns null for OXXO since it doesn't require a document.
     */
    fun document(provider: OffsitePaymentMethodType): String? = when (provider) {
            OffsitePaymentMethodType.OXXO -> null
            else -> TEST_CPF
        }

    private fun buildPixConfig(redirectUrl: String): OffsitePaymentConfig = OffsitePaymentConfig(
            paymentMethodType = OffsitePaymentMethodType.PIX,
            redirectUrl = redirectUrl,
            email = TEST_EMAIL,
            fullName = BR_NAME,
            documentId = DocumentId.standard(TEST_CPF),
            country = "BR",
            phoneNumber = BR_PHONE,
            address1 = BR_ADDRESS,
            city = BR_CITY,
            state = BR_STATE,
            zip = BR_ZIP,
        )

    private fun buildBoletoConfig(redirectUrl: String): OffsitePaymentConfig = OffsitePaymentConfig(
            paymentMethodType = OffsitePaymentMethodType.BOLETO_BANCARIO,
            redirectUrl = redirectUrl,
            email = TEST_EMAIL,
            fullName = BR_NAME,
            documentId = DocumentId.standard(TEST_CPF),
            country = "BR",
            phoneNumber = BR_PHONE,
            address1 = BR_ADDRESS,
            city = BR_CITY,
            state = BR_STATE,
            zip = BR_ZIP,
        )

    private fun buildOxxoConfig(redirectUrl: String): OffsitePaymentConfig = OffsitePaymentConfig(
            paymentMethodType = OffsitePaymentMethodType.OXXO,
            redirectUrl = redirectUrl,
            email = TEST_EMAIL,
            fullName = MX_NAME,
            country = "MX",
            phoneNumber = MX_PHONE,
            address1 = MX_ADDRESS,
            city = MX_CITY,
            state = MX_STATE,
            zip = MX_ZIP,
        )

    private fun buildNupayConfig(redirectUrl: String): OffsitePaymentConfig = OffsitePaymentConfig(
            paymentMethodType = OffsitePaymentMethodType.NUPAY,
            redirectUrl = redirectUrl,
            email = TEST_EMAIL,
            fullName = BR_NAME,
            documentId = DocumentId.standard(TEST_CPF),
            country = "BR",
            phoneNumber = BR_PHONE,
        )
}
