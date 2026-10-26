package com.spreedly.example.screens.customcheckout

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.models.SavedPaymentMethod
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.DemoSdkAuthSession
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.result.Result
import com.spreedly.sdk.SpreedlyErrorMessages
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.paymentMethodUpdatedAt
import com.spreedly.sdk.models.RecacheConfig
import com.spreedly.sdk.models.SavedCardInfo
import com.spreedly.sdk.models.ScreenPresentationMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CheckoutWithAdditionalFieldsScreen managing payment state and SDK initialization.
 */
class CheckoutWithAdditionalFieldsViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    // Shared helpers
    private val authSession = DemoSdkAuthSession(SdkSessionManager(AuthService()))
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    // Saved payment methods
    private val _savedPaymentMethods = MutableStateFlow<List<SavedPaymentMethod>>(emptyList())
    val savedPaymentMethods: StateFlow<List<SavedPaymentMethod>> = _savedPaymentMethods.asStateFlow()

    // Initialize SDK on ViewModel creation
    init {
        initJob =
            viewModelScope.launch {
                ensureFreshAuthBeforePayment()
            }
        loadSavedPaymentMethods()
        fetchPaymentMethodsFromBackend()
    }

    suspend fun ensureFreshAuthBeforePayment(): Boolean =
        authSession.ensureFreshAuth(
            sdk = sdk,
            context = context,
            environmentKey = BuildConfig.ENVIRONMENT_KEY,
            isInitializing = _isInitializing,
            registerObserversIfNeeded = ::observePaymentResults,
            onFailure = { snackbarHostState.showSnackbar("SDK initialization failed") },
        )

    private fun observePaymentResults() {
        paymentResultJob?.cancel()
        paymentResultJob =
            paymentResultHandler.observeResults(
                sdk = sdk,
                scope = viewModelScope,
                onCompleted = { result ->
                    Log.d(TAG, "CheckoutWithAdditionalFieldsViewModel: Payment completed")
                    _token.value = result.token
                    authSession.onTerminalPaymentResult(result)
                    Log.d(TAG, "CheckoutWithAdditionalFieldsViewModel: Token set")

                    paymentResultHandler.retainIfNeeded(result).fold(
                        onSuccess = { retained ->
                            if (retained) {
                                snackbarHostState.showSnackbar("Payment method saved for future use!")
                            } else {
                                snackbarHostState.showSnackbar("Payment completed successfully!")
                            }
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar("Payment created but failed to save")
                        },
                    )
                },
                onFailed = { result ->
                    Log.d(TAG, "CheckoutWithAdditionalFieldsViewModel: Payment failed: ${result.errorType}")
                    authSession.onTerminalPaymentResult(result)
                    snackbarHostState.showSnackbar("Payment failed")
                },
                onCanceled = {
                    Log.d(TAG, "CheckoutWithAdditionalFieldsViewModel: Payment canceled")
                    snackbarHostState.showSnackbar("Payment canceled")
                },
            )
    }

    fun clearToken() {
        _token.value = ""
    }

    fun reinitialize() {
        initJob?.cancel()
        paymentResultJob?.cancel()
        clearToken()
        authSession.prepareForManualReinitialize()
        initJob =
            viewModelScope.launch {
                ensureFreshAuthBeforePayment()
            }
    }

    /**
     * Load saved payment methods from the repository.
     */
    fun loadSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                // Load saved payment methods (no mock data)
                val methods = paymentMethodRepository.getSavedPaymentMethods()
                _savedPaymentMethods.value = methods
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Loaded ${methods.size} saved payment methods")
            } catch (e: Exception) {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Error loading saved payment methods: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error loading saved payment methods")
            }
        }
    }

    /**
     * Fetch payment methods from backend and update local cache.
     */
    private fun fetchPaymentMethodsFromBackend() {
        viewModelScope.launch {
            try {
                paymentMethodRepository.fetchAndSyncPaymentMethods().fold(
                    onSuccess = { methods ->
                        _savedPaymentMethods.value = methods
                        Log.d(TAG,
                            "CheckoutWithAdditionalFieldsViewModel: " +
                            "Fetched ${methods.size} payment methods from backend",
                        )
                    },
                    onFailure = { e ->
                        Log.d(TAG,
                            "CheckoutWithAdditionalFieldsViewModel: Error " +
                            "fetching payment methods: ${e::class.simpleName}",
                        )
                        // Keep using cached data
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Exception fetching payment methods: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Recache a saved payment method by showing the SDK's CVV input UI.
     *
     * @param savedCard The saved payment method to recache
     */
    fun recacheSavedPaymentMethod(savedCard: SavedPaymentMethod) {
        viewModelScope.launch {
            try {
                // Create recaching configuration
                val config = RecacheConfig(
                    recachePresentationMode = ScreenPresentationMode.bottomSheet,
                    cardInfo = SavedCardInfo(
                        lastFourDigits = savedCard.lastFourDigits,
                        cardType = savedCard.cardType,
                        cardholderName = savedCard.cardholderName,
                    ),
                    labelText = "CVV",
                    placeholderText = "123",
                    buttonText = "Confirm",
                    cancelButtonText = "Cancel",
                )

                // Call SDK's recaching method
                val result = sdk.recachePaymentMethod(
                    paymentMethodToken = savedCard.token,
                    config = config,
                )

                when (val recacheResult = result) {
                    is Result.Success -> {
                        val response = recacheResult.data
                        _token.value = response.transaction.paymentMethod.token
                        Log.d(TAG,
                            "CheckoutWithAdditionalFieldsViewModel: " +
                                "Recached token: ${response.transaction.paymentMethod.token}",
                        )
                        snackbarHostState.showSnackbar(
                            "CVV updated. Updated at: ${response.paymentMethodUpdatedAt}",
                        )
                        retainAfterRecache(response.transaction.paymentMethod.token)
                    }
                    is Result.Error -> {
                        // Don't show error message if user cancelled
                        if (recacheResult.error != com.spreedly.sdk.SpreedlyNetworkError.USER_CANCELLED) {
                            val errorMessage = SpreedlyErrorMessages.getUserFriendlyMessage(
                                error = recacheResult.error,
                                defaultMessage = "Failed to update payment method. Please try again.",
                            )
                            snackbarHostState.showSnackbar(errorMessage)
                            Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Recaching error: ${recacheResult.error.safeDescription()}")
                        } else {
                            Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Recaching cancelled by user")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Exception during recaching: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("An error occurred during recaching")
            }
        }
    }

    /**
     * Call retain API after successful recache to refresh retention period.
     */
    private fun retainAfterRecache(token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Retaining after recache")
                paymentMethodRepository.retainPaymentMethod(token).fold(
                    onSuccess = {
                        Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Successfully retained after recache")
                        snackbarHostState.showSnackbar("Payment method recached and retained!")
                    },
                    onFailure = {
                        Log.d(TAG,
                            "CheckoutWithAdditionalFieldsViewModel: Error " +
                            "retaining after recache: ${it::class.simpleName}",
                        )
                        snackbarHostState.showSnackbar("Recached successfully!")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Exception retaining after recache: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Delete a saved payment method.
     *
     * @param token The token of the payment method to delete
     */
    fun deleteSavedPaymentMethod(token: String) {
        viewModelScope.launch {
            try {
                paymentMethodRepository.deletePaymentMethod(token)
                loadSavedPaymentMethods()
                snackbarHostState.showSnackbar("Payment method deleted")
            } catch (e: Exception) {
                Log.d(TAG,"CheckoutWithAdditionalFieldsViewModel: Error deleting payment method: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error deleting payment method")
            }
        }
    }

    private companion object {
        private const val TAG = "CheckoutWithAdditionalFieldsViewModel"
    }
}
