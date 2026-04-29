package com.spreedly.example.screens.bottomsheet

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel specifically for BottomSheetPaymentScreen that includes payment sheet configuration.
 * Now includes configuration change handling to preserve payment form data during orientation changes.
 */
class BottomSheetPaymentViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    // Configuration change tracking
    private var isConfigurationChanging = false

    // Shared helpers
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)

    // Initialize SDK on ViewModel creation
    init {
        initializeSDK()
    }

    private fun initializeSDK() {
        viewModelScope.launch {
            _isInitializing.value = true
            sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "SDK initialized successfully with remote auth")
                        observePaymentResults()
                    },
                    onFailure = { e ->
                        Log.d(TAG, "SDK initialization failed: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("SDK initialization failed")
                    },
                )
            _isInitializing.value = false
        }
    }

    private fun observePaymentResults() {
        paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                Log.d(TAG, "BottomSheetPaymentViewModel: Payment completed")
                _isProcessing.value = false
                _token.value = result.token
                Log.d(TAG, "BottomSheetPaymentViewModel: Token set in view model state")

                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = { retained ->
                        if (retained && !isConfigurationChanging) {
                            snackbarHostState.showSnackbar("Payment method saved for future use!")
                        } else if (!retained && !isConfigurationChanging) {
                            snackbarHostState.showSnackbar("Payment completed successfully!")
                        }
                    },
                    onFailure = {
                        if (!isConfigurationChanging) {
                            snackbarHostState.showSnackbar("Payment created but failed to save")
                        }
                    },
                )
            },
            onFailed = { result ->
                Log.d(TAG, "BottomSheetPaymentViewModel: Payment failed: ${result.errorType}")
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment failed")
                }
            },
            onCanceled = {
                Log.d(TAG, "BottomSheetPaymentViewModel: Payment canceled")
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment canceled")
                }
            },
        )
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    fun clearToken() {
        _token.value = ""
    }

    fun expressCheckout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "BottomSheetPaymentViewModel: Starting express checkout...")
                clearToken()
                setProcessing(true)
                sdk.expressCheckout()

                // Safety mechanism: reset processing state after 30 seconds if no result
                viewModelScope.launch {
                    delay(30000)
                    if (_isProcessing.value) {
                        Log.d(TAG, "BottomSheetPaymentViewModel: Timeout - resetting processing state")
                        setProcessing(false)
                        if (!isConfigurationChanging) {
                            snackbarHostState.showSnackbar("Payment processing timeout. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "BottomSheetPaymentViewModel: Error during express checkout: ${e::class.simpleName}")
                setProcessing(false)
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Checkout error")
                }
            }
        }
    }

    /**
     * Call this method when configuration change is about to happen.
     * This preserves the payment form state during orientation changes.
     */
    fun onConfigurationChanging() {
        isConfigurationChanging = true
        if (sdk.isInitialized && sdk.showBottomSheet.value) {
            // Preserve state if bottom sheet is currently shown
            sdk.preservePaymentStateOnNextShow()
            Log.d(TAG, "BottomSheetPaymentViewModel: Preserving payment state for configuration change")
        }
    }

    /**
     * Call this method after configuration change is complete.
     */
    fun onConfigurationChangeComplete() {
        isConfigurationChanging = false
        Log.d(TAG, "BottomSheetPaymentViewModel: Configuration change complete")
    }

    /**
     * Clear snackbar state when returning from child activities
     * This prevents stale error messages from appearing
     */
    fun clearSnackbarState() {
        viewModelScope.launch {
            try {
                snackbarHostState.currentSnackbarData?.dismiss()
            } catch (e: Exception) {
                // Ignore any exceptions during cleanup
            }
        }
    }

    /**
     * Reset the SDK state and reinitialize for a fresh start.
     * This should be called when navigating to a new demo screen to ensure
     * form data doesn't persist between different examples.
     */
    fun resetAndReinitialize() {
        viewModelScope.launch {
            try {
                // Clear any existing token
                clearToken()

                // Only reset the SDK payment state if it's not during a configuration change
                // and we want to start fresh
                if (sdk.isInitialized && !isConfigurationChanging) {
                    sdk.resetPaymentState()
                    Log.d(TAG, "BottomSheetPaymentViewModel: SDK payment state reset")
                } else if (sdk.isInitialized && isConfigurationChanging) {
                    // Preserve state during configuration changes
                    sdk.preservePaymentStateOnNextShow()
                    Log.d(TAG, "BottomSheetPaymentViewModel: Preserving payment state during configuration change")
                }

                // Only reinitialize if not already initialized
                if (!sdk.isInitialized) {
                    initializeSDK()
                    Log.d(TAG, "BottomSheetPaymentViewModel: SDK initialized")
                } else {
                    Log.d(TAG, "BottomSheetPaymentViewModel: SDK already initialized, skipping reinitialization")
                }
            } catch (e: Exception) {
                Log.d(TAG, "BottomSheetPaymentViewModel: Error during reset and reinitialize: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error resetting SDK")
            }
        }
    }

    private companion object {
        private const val TAG = "BottomSheetPaymentViewModel"
    }
}
