package com.spreedly.example.screens.braintreepayment

import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.braintree.BraintreeAPMCheckoutConfig
import com.spreedly.braintree.BraintreeAPMPaymentType
import com.spreedly.braintree.SpreedlyBraintreeAPMCheckout
import com.spreedly.example.AuthService
import com.spreedly.example.api.BraintreeConfirmState
import com.spreedly.example.api.BraintreeFields
import com.spreedly.example.api.BraintreeGatewaySpecificFields
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.screens.common.Product
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Braintree payment screen.
 *
 * Uses a 4-stage flow: Idle -> Purchasing -> Checkout -> Confirming.
 * Unlike Stripe APM, Braintree returns a nonce that requires a separate
 * confirmation step via the merchant's backend (Spreedly `/confirm` endpoint).
 */
class BraintreePaymentViewModel(
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
        CONFIRMING,
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

    private val _selectedPaymentType = MutableStateFlow(BraintreeAPMPaymentType.PAYPAL)
    val selectedPaymentType: StateFlow<BraintreeAPMPaymentType> = _selectedPaymentType.asStateFlow()

    val products = listOf(
        Product("Sunglasses", "Premium UV protection", 4400, "🕶️"),
        Product("Watch", "Swiss precision", 19900, "⌚"),
        Product("Headphones", "Noise cancelling", 29900, "🎧"),
        Product("Camera", "Professional grade", 89900, "📷"),
        Product("Laptop", "Ultra portable", 129900, "💻"),
        Product("Phone", "Latest model", 99900, "📱"),
    )

    val paymentTypes = BraintreeAPMPaymentType.entries

    private val braintreeMethodDisplayName: String
        get() = when (_selectedPaymentType.value) {
            BraintreeAPMPaymentType.PAYPAL -> "PayPal"
            BraintreeAPMPaymentType.VENMO -> "Venmo"
        }

    private var paymentResultJob: kotlinx.coroutines.Job? = null
    private var currentTransactionToken: String? = null

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

    private fun startPaymentResultObserver() {
        if (!sdk.isInitialized) return
        paymentResultJob?.cancel()
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                val nonce = result.nonce
                if (nonce != null) {
                    confirmTransaction(result.token, nonce)
                } else {
                    val message = when (result.state) {
                        "succeeded" ->
                            "Payment successful. " +
                                "The transaction has been completed successfully."

                        else ->
                            "Payment is being processed. " +
                                "Final confirmation may take a moment."
                    }
                    _successMessage.value = message
                    _stage.value = Stage.IDLE
                }
            },
            onFailed = { result ->
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                val errorMsg =
                    result.message ?: "$braintreeMethodDisplayName payment failed."
                _errorMessage.value = errorMsg
                _stage.value = Stage.IDLE
                confirmNonSuccessful(state = BraintreeConfirmState.FAILED, message = errorMsg)
            },
            onCanceled = {
                if (_stage.value != Stage.CHECKOUT) return@observeResults
                val cancelMsg =
                    "$braintreeMethodDisplayName payment was canceled."
                _errorMessage.value = cancelMsg
                _stage.value = Stage.IDLE
                confirmNonSuccessful(state = BraintreeConfirmState.CANCELLED, message = cancelMsg)
            },
        )
    }

    private fun confirmTransaction(transactionToken: String, nonce: String) {
        _stage.value = Stage.CONFIRMING
        viewModelScope.launch {
            try {
                val response = purchaseClient.braintreeConfirm(
                    transactionToken = transactionToken,
                    state = BraintreeConfirmState.SUCCESSFUL,
                    nonce = nonce,
                    paymentMethodType = _selectedPaymentType.value.value,
                )
                val transaction = response.transaction
                if (transaction == null) {
                    _errorMessage.value = "Confirmation response missing transaction data."
                    _stage.value = Stage.IDLE
                    return@launch
                }
                val state = transaction.state
                when {
                    transaction.succeeded == true || state == "succeeded" -> {
                        _successMessage.value =
                            "Payment successful. " +
                                "The transaction has been completed successfully."
                    }

                    state == "pending" || state == "processing" -> {
                        _successMessage.value =
                            "Payment is being processed. " +
                                "Final confirmation may take a moment."
                    }

                    else -> {
                        _errorMessage.value =
                            "Confirmation returned state: $state." +
                                " Message: ${transaction.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Confirmation failed: ${e.message}"
            } finally {
                _stage.value = Stage.IDLE
            }
        }
    }

    private fun confirmNonSuccessful(state: BraintreeConfirmState, message: String) {
        val token = currentTransactionToken ?: return
        viewModelScope.launch {
            try {
                purchaseClient.braintreeConfirm(
                    transactionToken = token,
                    state = state,
                    message = message,
                    paymentMethodType = _selectedPaymentType.value.value,
                )
            } catch (_: Exception) {
                // Best-effort: backend notification failure doesn't affect user experience
            }
        }
    }

    fun startPayment(activity: Activity) {
        val product = _selectedProduct.value
        if (product == null) {
            _errorMessage.value = "Please select a product"
            return
        }
        if (product.price <= 0) {
            _errorMessage.value = "Invalid product price"
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
                val paymentType = _selectedPaymentType.value
                val gatewayFields = when (paymentType) {
                    BraintreeAPMPaymentType.VENMO -> BraintreeGatewaySpecificFields(
                        braintree = BraintreeFields(
                            venmoFlowType = "multi_use",
                            venmoProfileId = "12345",
                        ),
                    )

                    BraintreeAPMPaymentType.PAYPAL -> BraintreeGatewaySpecificFields(
                        braintree = BraintreeFields(paypalFlowType = "checkout"),
                    )
                }
                val response = purchaseClient.braintreePurchase(
                    paymentMethodType = paymentType.value,
                    amount = product.price,
                    currencyCode = "USD",
                    gatewaySpecificFields = gatewayFields,
                )

                val transaction = response.transaction
                if (transaction == null) {
                    _errorMessage.value = "Failed to create purchase"
                    _stage.value = Stage.IDLE
                    return@launch
                }
                val validStates = setOf("pending", "processing")
                if (transaction.state !in validStates) {
                    _errorMessage.value =
                        "Transaction not in expected state: ${transaction.state}." +
                            " Message: ${transaction.message}"
                    _stage.value = Stage.IDLE
                    return@launch
                }

                currentTransactionToken = transaction.token

                val clientToken = response.transaction
                    ?.gatewaySpecificResponseFields
                    ?.braintree
                    ?.clientToken

                val amountStr = "%.2f".format(product.price / 100.0)

                val config = BraintreeAPMCheckoutConfig(
                    transactionToken = transaction.token,
                    paymentType = paymentType,
                    merchantDisplayName = "Example Store",
                    clientToken = clientToken,
                    amount = amountStr,
                    currencyCode = "USD",
                )

                _stage.value = Stage.CHECKOUT
                SpreedlyBraintreeAPMCheckout.present(config, activity)
            } catch (e: Exception) {
                _errorMessage.value = "Error occurred"
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
            SpreedlyBraintreeAPMCheckout.finalizeIfActive()
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun selectPaymentType(type: BraintreeAPMPaymentType) {
        _selectedPaymentType.value = type
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
}
