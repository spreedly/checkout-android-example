package com.spreedly.example.screens.offsitepayment

import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.offsite.OffsitePaymentConfig
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.sdk.ui.offsite.SpreedlyOffsiteCheckout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for offsite payment flows (Sprel/PayPal, EBANX, etc.).
 *
 * Encapsulates the shared stage machine, SDK initialization, payment result observation,
 * and checkout launch. Subclasses provide provider-specific config and purchase logic.
 *
 * ## Stage Machine
 *
 * ```
 * Idle --> CreatingPaymentMethod --> Purchasing --> Checkout --> Idle
 *           |                          |                |
 *           +--(failure)-> Idle        +--(failure)-> Idle
 * ```
 *
 * ## Activity handling
 *
 * Activity is not stored; it is passed only into [startPayment] and [proceedToCheckout]
 * to avoid holding a lifecycle-sensitive reference and potential leaks.
 */
abstract class BaseOffsitePaymentViewModel(
    protected val context: Context,
    protected val sdk: Spreedly,
    protected val purchaseClient: SpreedlyPurchaseAPIClient,
    private val environmentKey: String = BuildConfig.ENVIRONMENT_KEY,
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    // Shared helpers
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)

    enum class Stage {
        IDLE,
        CREATING_PAYMENT_METHOD,
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

    private var currentPaymentMethodToken: String? = null
    private var paymentResultObserverJob: Job? = null

    internal fun updateStage(stage: Stage) {
        _stage.value = stage
    }

    internal fun updateError(message: String?) {
        _errorMessage.value = message
    }

    internal fun updateSuccess(message: String?) {
        _successMessage.value = message
    }

    protected abstract fun hasSelectedProduct(): Boolean

    protected abstract suspend fun getOffsitePaymentConfig(): OffsitePaymentConfig

    protected abstract suspend fun performPurchase(paymentMethodToken: String): String?

    protected abstract fun handlePaymentCompleted(result: PaymentResult.Completed)

    protected abstract fun handlePaymentFailed(result: PaymentResult.Failed)

    protected abstract fun handlePaymentCanceled()

    protected abstract fun resetSelection()

    protected suspend fun initializeSDKWithFreshAuth(): Boolean = try {
            sdkSessionManager.initializeSdk(
                sdk = sdk,
                context = context.applicationContext,
                environmentKey = environmentKey,
            ).fold(
                onSuccess = {
                    restartPaymentResultObserver()
                    true
                },
                onFailure = {
                    updateError("Failed to get authentication parameters")
                    false
                },
            )
        } catch (e: Exception) {
            updateError("Initialization error")
            false
        }

    private fun restartPaymentResultObserver() {
        paymentResultObserverJob?.cancel()
        paymentResultObserverJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                if (_stage.value == Stage.CHECKOUT) handlePaymentCompleted(result)
            },
            onFailed = { result ->
                if (_stage.value == Stage.CHECKOUT) handlePaymentFailed(result)
            },
            onCanceled = {
                if (_stage.value == Stage.CHECKOUT) handlePaymentCanceled()
            },
        )
    }

    /**
     * Start the offsite payment flow.
     *
     * Activity is passed in here (not stored) so the ViewModel does not hold
     * a lifecycle-sensitive reference.
     */
    fun startPayment(activity: Activity) {
        if (!hasSelectedProduct()) {
            updateError("Please select a product")
            return
        }

        clearMessages()
        updateStage(Stage.CREATING_PAYMENT_METHOD)

        viewModelScope.launch {
            try {
                val initialized = initializeSDKWithFreshAuth()
                if (!initialized) {
                    updateStage(Stage.IDLE)
                    return@launch
                }

                val config = getOffsitePaymentConfig()
                val result = sdk.submitOffsitePayment(config)

                when (result) {
                    is com.spreedly.result.Result.Success -> {
                        val token = result.data.transaction.paymentMethod.token
                        currentPaymentMethodToken = token
                        val transactionToken = performPurchase(token)
                        if (transactionToken != null) {
                            proceedToCheckout(transactionToken, activity)
                        }
                    }

                    is com.spreedly.result.Result.Error -> {
                        updateError("Failed to create payment method")
                        updateStage(Stage.IDLE)
                    }
                }
            } catch (e: Exception) {
                updateError("An error occurred")
                updateStage(Stage.IDLE)
            }
        }
    }

    protected fun proceedToCheckout(transactionToken: String, activity: Activity) {
        updateStage(Stage.CHECKOUT)
        try {
            SpreedlyOffsiteCheckout.present(transactionToken, activity)
        } catch (e: Exception) {
            updateError("Checkout error")
            updateStage(Stage.IDLE)
        }
    }

    fun onResumeFromCheckout() {
        if (_stage.value == Stage.CHECKOUT) {
            SpreedlyOffsiteCheckout.finalizeIfActive()
        }
    }

    fun clearMessages() {
        updateError(null)
        updateSuccess(null)
    }

    fun reset() {
        updateStage(Stage.IDLE)
        currentPaymentMethodToken = null
        resetSelection()
        clearMessages()
    }

    override fun onCleared() {
        super.onCleared()
        paymentResultObserverJob?.cancel()
        purchaseClient.close()
    }
}
