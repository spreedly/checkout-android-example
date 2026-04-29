package com.spreedly.example.screens.customizedcheckout

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CustomisedCheckoutScreen managing payment state and SDK initialization.
 */
class CustomisedCheckoutViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    // Payment method repository for retention
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    // Initialize SDK on ViewModel creation
    init {
        initializeSDK()
    }

    /**
     * Initialize SDK only if it's not already initialized.
     */
    fun initializeSDKIfNeeded() {
        if (!sdk.isInitialized) {
            initializeSDK()
        }
    }

    private fun initializeSDK() {
        initJob = viewModelScope.launch {
            _isInitializing.value = true
            try {
                sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                    .fold(
                        onSuccess = {
                            Log.d(TAG, "CustomisedCheckoutViewModel: SDK initialized successfully")
                            observePaymentResults()
                        },
                        onFailure = { e ->
                            Log.d(TAG, "CustomisedCheckoutViewModel: Failed to fetch auth parameters: ${e::class.simpleName}")
                            snackbarHostState.showSnackbar("Failed to fetch auth parameters")
                        },
                    )
            } catch (e: Exception) {
                Log.d(TAG, "CustomisedCheckoutViewModel: SDK initialization failed: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("SDK initialization failed")
            }
            _isInitializing.value = false
        }
    }

    private fun observePaymentResults() {
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                _token.value = result.token
                Log.d(TAG, "CustomisedCheckoutViewModel: Token set")
                if (result.shouldRetain) {
                    handlePaymentRetention(result)
                } else {
                    snackbarHostState.showSnackbar("Payment completed successfully!")
                }
            },
            onFailed = {
                Log.d(TAG, "CustomisedCheckoutViewModel: Payment failed: ${it.errorType}")
                snackbarHostState.showSnackbar("Payment failed")
            },
            onCanceled = {
                Log.d(TAG, "CustomisedCheckoutViewModel: Payment canceled")
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
        initializeSDK()
    }

    /**
     * Handle payment retention after successful payment creation.
     * Calls the backend retain API.
     */
    private fun handlePaymentRetention(result: PaymentResult.Completed) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "CustomisedCheckoutViewModel: Retaining payment method")
                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = {
                        Log.d(TAG, "CustomisedCheckoutViewModel: Payment method retained successfully")
                        snackbarHostState.showSnackbar("Payment method saved for future use!")
                    },
                    onFailure = { e ->
                        Log.d(TAG, "CustomisedCheckoutViewModel: Error retaining payment method: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "CustomisedCheckoutViewModel: Exception during retention: ${e::class.simpleName}")
            }
        }
    }

    private companion object {
        private const val TAG = "CustomisedCheckoutViewModel"
    }
}
