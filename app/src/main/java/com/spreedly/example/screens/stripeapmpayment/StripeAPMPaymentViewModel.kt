package com.spreedly.example.screens.stripeapmpayment

import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.screens.common.Product
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.stripe.SpreedlyStripeAPMCheckout
import com.spreedly.stripe.StripeAPMAppearanceConfig
import com.spreedly.stripe.StripeAPMConfig
import com.spreedly.striperadar.SpreedlyStripeRadar
import com.spreedly.striperadar.StripeRadarConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * APM type option for Stripe (iDEAL, Bancontact, etc.).
 */
data class StripeAPMType(
    val id: String,
    val displayName: String,
)

/**
 * ViewModel for the Stripe APM payment screen.
 *
 * Uses a 3-stage flow: Idle -> Purchasing -> Checkout (no tokenization step).
 * Backend creates a pending purchase with payment_method_type "stripe_apm";
 * the SDK presents the Stripe PaymentSheet with the returned client_secret.
 */
class StripeAPMPaymentViewModel(
    private val context: Context,
    private val sdk: Spreedly = Spreedly(),
    private val purchaseClient: SpreedlyPurchaseAPIClient = SpreedlyPurchaseAPIClient(),
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    // Shared helpers
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(PaymentMethodRepository(context))

    enum class Stage {
        IDLE,
        PURCHASING,
        CHECKOUT,
    }

    private val _stage = MutableStateFlow(Stage.IDLE)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _selectedApmTypes = MutableStateFlow(setOf(apmTypes.first()))
    val selectedApmTypes: StateFlow<Set<StripeAPMType>> = _selectedApmTypes.asStateFlow()

    private val _radarEnabled = MutableStateFlow(false)
    val radarEnabled: StateFlow<Boolean> = _radarEnabled.asStateFlow()

    private val _radarSessionId = MutableStateFlow<String?>(null)
    val radarSessionId: StateFlow<String?> = _radarSessionId.asStateFlow()

    private val _radarState = MutableStateFlow(RadarState.IDLE)
    val radarState: StateFlow<RadarState> = _radarState.asStateFlow()

    enum class RadarState { IDLE, COLLECTING, SUCCESS, FAILED }

    val products = listOf(
        Product("Sunglasses", "Premium UV protection", 4400, "🕶️"),
        Product("Watch", "Swiss precision", 19900, "⌚"),
        Product("Headphones", "Noise cancelling", 29900, "🎧"),
        Product("Camera", "Professional grade", 89900, "📷"),
        Product("Laptop", "Ultra portable", 129900, "💻"),
        Product("Phone", "Latest model", 99900, "📱"),
    )

    val apmTypesList = apmTypes

    private val selectedAPMDisplayName: String
        get() = _selectedApmTypes.value.firstOrNull()?.displayName ?: "APM"

    private var paymentResultJob: kotlinx.coroutines.Job? = null

    init {
        initializeSdkOnScreenLoad()
    }

    private fun initializeSdkOnScreenLoad() {
        viewModelScope.launch {
            val initialized = initializeSdkIfNeeded()
            if (initialized) {
                startPaymentResultObserver()
            }
        }
    }

    fun toggleRadar(enabled: Boolean) {
        _radarEnabled.value = enabled
        if (enabled) {
            collectRadarSession()
        } else {
            _radarSessionId.value = null
            _radarState.value = RadarState.IDLE
        }
    }

    private fun collectRadarSession() {
        val publishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY
        if (publishableKey.isBlank()) return

        _radarState.value = RadarState.COLLECTING
        viewModelScope.launch {
            val config = StripeRadarConfig(publishableKey = publishableKey)
            val sessionId = SpreedlyStripeRadar.createRadarSession(config, context)
            _radarSessionId.value = sessionId
            _radarState.value = if (sessionId != null) RadarState.SUCCESS else RadarState.FAILED
        }
    }

    /**
     * Starts collecting from paymentResultFlow. Must only be called after the SDK is initialized,
     * since paymentResultFlow delegates to paymentManager which throws if not initialized.
     */
    private fun startPaymentResultObserver() {
        if (!sdk.isInitialized) return
        paymentResultJob?.cancel()
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                val message = when (result.state) {
                    "succeeded" ->
                        "Payment successful. The transaction has been completed successfully."

                    "processing" ->
                        "Payment accepted and is being processed. " +
                            "Final confirmation may take a few days."

                    "pending" ->
                        "Payment submitted. " +
                            "Awaiting final confirmation from the payment provider."

                    else ->
                        "Payment successful. The transaction has been completed successfully."
                }
                _successMessage.value = message
                _stage.value = Stage.IDLE
            },
            onFailed = { result ->
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                _errorMessage.value =
                    result.message ?: "$selectedAPMDisplayName payment failed."
                _stage.value = Stage.IDLE
            },
            onCanceled = {
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                _errorMessage.value = "$selectedAPMDisplayName payment was canceled."
                _stage.value = Stage.IDLE
            },
        )
    }

    fun startPayment(
        activity: Activity,
        appearance: StripeAPMAppearanceConfig? = null,
    ) {
        val product = _selectedProduct.value
        if (product == null) {
            _errorMessage.value = "Please select a product"
            return
        }

        val selectedTypes = _selectedApmTypes.value
        if (selectedTypes.isEmpty()) {
            _errorMessage.value = "Please select at least one payment method"
            return
        }

        if (!sdk.isInitialized) {
            _errorMessage.value = "SDK is still initializing, please wait..."
            return
        }

        _errorMessage.value = null
        _successMessage.value = null
        _stage.value = Stage.PURCHASING

        viewModelScope.launch {
            try {
                val apmTypeIds = selectedTypes.map { it.id }
                val response = purchaseClient.stripeAPMPurchase(
                    amount = product.price,
                    currencyCode = "EUR",
                    apmTypes = apmTypeIds,
                    redirectUrl = SpreedlyPurchaseAPIClient.redirectUrl(context, "stripe/checkout"),
                    radarSessionId = if (_radarEnabled.value) _radarSessionId.value else null,
                )

                val transaction = response.transaction
                if (transaction == null) {
                    _errorMessage.value = "Failed to create pending purchase"
                    _stage.value = Stage.IDLE
                    return@launch
                }
                if (transaction.state != "pending") {
                    _errorMessage.value =
                        "Transaction not in pending state: ${transaction.state}." +
                            " Message: ${transaction.message}"
                    _stage.value = Stage.IDLE
                    return@launch
                }

                val clientSecret = transaction.gatewaySpecificResponseFields
                    ?.stripePaymentIntents
                    ?.clientSecret
                if (clientSecret.isNullOrBlank()) {
                    _errorMessage.value = "Missing client_secret in pending purchase response"
                    _stage.value = Stage.IDLE
                    return@launch
                }

                val publishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY
                if (publishableKey.isBlank()) {
                    _errorMessage.value = "Stripe publishable key not configured"
                    _stage.value = Stage.IDLE
                    return@launch
                }

                val config = StripeAPMConfig(
                    publishableKey = publishableKey,
                    clientSecret = clientSecret,
                    transactionToken = transaction.token,
                    merchantDisplayName = "Example Store",
                    returnURL = SpreedlyPurchaseAPIClient.redirectUrl(context, "stripe/checkout"),
                )

                _stage.value = Stage.CHECKOUT
                SpreedlyStripeAPMCheckout.present(config, activity, appearance)
            } catch (e: Exception) {
                _errorMessage.value = "Pending purchase failed"
                _stage.value = Stage.IDLE
            }
        }
    }

    private suspend fun initializeSdkIfNeeded(): Boolean {
        if (sdk.isInitialized) {
            return true
        }
        _isInitializing.value = true
        return try {
            sdkSessionManager.initializeSdk(
                sdk = sdk,
                context = context.applicationContext,
                environmentKey = BuildConfig.ENVIRONMENT_KEY,
            ).fold(
                onSuccess = { true },
                onFailure = { e ->
                    _errorMessage.value = "Failed to get auth params"
                    false
                },
            )
        } finally {
            _isInitializing.value = false
        }
    }

    fun onResumeFromCheckout() {
        if (_stage.value == Stage.CHECKOUT) {
            SpreedlyStripeAPMCheckout.finalizeIfActive()
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun toggleApmType(apmType: StripeAPMType) {
        val current = _selectedApmTypes.value.toMutableSet()
        if (current.contains(apmType)) {
            current.remove(apmType)
        } else {
            current.add(apmType)
        }
        _selectedApmTypes.value = current
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        paymentResultJob?.cancel()
        purchaseClient.close()
    }

    companion object {
        private val apmTypes = listOf(
            StripeAPMType("ideal", "iDEAL"),
            StripeAPMType("bancontact", "Bancontact"),
            StripeAPMType("eps", "EPS"),
            StripeAPMType("p24", "Przelewy24"),
            StripeAPMType("sepa_debit", "SEPA Direct Debit"),
        )
    }
}
