package com.spreedly.example.viewmodel

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Configuration-change aware ViewModel for managing Spreedly SDK and preserving state.
 *
 * This ViewModel properly handles orientation changes by:
 * - Preserving SDK instance across configuration changes
 * - Maintaining form data during orientation changes
 * - Preventing race conditions during state transitions
 */
class ConfigurationChangeAwareViewModel(private val context: Context) : ViewModel() {
    val sdk = Spreedly()

    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

    // Track when payment completed to prevent race conditions
    private var lastPaymentCompletedTime = 0L

    // Configuration change tracking
    private var isConfigurationChanging = false

    // Initialize SDK on ViewModel creation
    init {
        initializeSDK()
    }

    private fun initializeSDK() {
        initJob = viewModelScope.launch {
            _isInitializing.value = true
            sdkSessionManager.initializeSdk(
                sdk,
                context.applicationContext,
                BuildConfig.ENVIRONMENT_KEY,
            ).fold(
                onSuccess = {
                    while (!sdk.isInitialized) { delay(50) }
                    observePaymentResults()
                    _isInitializing.value = false
                },
                onFailure = { e ->
                    _isInitializing.value = false
                    snackbarHostState.showSnackbar("Failed to initialize SDK")
                },
            )
        }
    }

    private fun observePaymentResults() {
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                _isProcessing.value = false
                _paymentToken.value = result.token
                lastPaymentCompletedTime = System.currentTimeMillis()

                if (result.shouldRetain) {
                    handlePaymentRetention(result)
                } else if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment successful!")
                }
            },
            onFailed = { result ->
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment failed: ${result.message}")
                }
            },
            onCanceled = {
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment canceled")
                }
            },
        )
    }

    private fun handlePaymentRetention(result: PaymentResult.Completed) {
        viewModelScope.launch {
            paymentResultHandler.retainIfNeeded(result).fold(
                onSuccess = {
                    fetchPaymentMethodsFromBackend()
                    if (!isConfigurationChanging) {
                        snackbarHostState.showSnackbar("Payment method saved for future use!")
                    }
                },
                onFailure = { e ->
                    if (!isConfigurationChanging) {
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    }
                },
            )
        }
    }

    private fun fetchPaymentMethodsFromBackend() {
        viewModelScope.launch {
            paymentMethodRepository.fetchAndSyncPaymentMethods()
        }
    }

    fun setProcessing(processing: Boolean) {
        if (processing) {
            // Check if payment completed recently (within last 1000ms) - race condition protection
            val timeSinceCompletion = System.currentTimeMillis() - lastPaymentCompletedTime
            if (timeSinceCompletion < 1000 && _paymentToken.value.isNotEmpty()) {
                return
            }
        }
        _isProcessing.value = processing
    }

    fun clearToken() {
        Log.d(TAG, "Clearing token - previous value: '${_paymentToken.value}'")
        _paymentToken.value = ""
        lastPaymentCompletedTime = 0L // Reset completion tracking
        Log.d(TAG, "Token cleared - new value: '${_paymentToken.value}'")
    }

    fun reinitialize() {
        initJob?.cancel()
        paymentResultJob?.cancel()
        clearToken()
        initializeSDK()
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
        }
    }

    /**
     * Call this method after configuration change is complete.
     */
    fun onConfigurationChangeComplete() {
        isConfigurationChanging = false
    }

    // Backup timeout mechanism in case payment result flow fails
    fun startPaymentPolling() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000) // 30-second timeout
            if (_isProcessing.value) {
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment processing timeout")
                }
            }
        }
    }

    /**
     * Clear snackbar state and reset UI state when activity is finishing
     * This prevents stale error messages from appearing when returning to form list
     */
    fun clearStateOnFinish() {
        viewModelScope.launch {
            // Clear any pending snackbar messages
            try {
                snackbarHostState.currentSnackbarData?.dismiss()
            } catch (e: Exception) {
                // Ignore any exceptions during cleanup
            }
        }
        // Reset processing state to prevent UI inconsistencies
        _isProcessing.value = false
    }

    // Java-compatible getter methods
    fun getInitializingState(): StateFlow<Boolean> = isInitializing

    fun getProcessingState(): StateFlow<Boolean> = isProcessing

    fun getTokenState(): StateFlow<String> = paymentToken

    private companion object {
        private const val TAG = "ConfigurationChangeAwareViewModel"
    }
}
