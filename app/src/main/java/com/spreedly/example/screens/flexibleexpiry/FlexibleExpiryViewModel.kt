package com.spreedly.example.screens.flexibleexpiry

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for FlexibleExpiryScreen managing payment state and SDK initialization.
 */
class FlexibleExpiryViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

    // Shared helpers
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    // Initialize SDK on ViewModel creation
    init {
        initializeSDK()
    }

    private fun initializeSDK() {
        initJob = viewModelScope.launch {
            _isInitializing.value = true
            sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "FlexibleExpiryViewModel: SDK initialized successfully")
                        observePaymentResults()
                    },
                    onFailure = { e ->
                        Log.d(TAG, "FlexibleExpiryViewModel: SDK initialization failed: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("SDK initialization failed")
                    },
                )
            _isInitializing.value = false
        }
    }

    private fun observePaymentResults() {
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                Log.d(TAG, "FlexibleExpiryViewModel: Payment completed")
                _paymentToken.value = result.token

                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = { retained ->
                        if (retained) {
                            snackbarHostState.showSnackbar("Payment method saved for future use!")
                        } else {
                            snackbarHostState.showSnackbar("Payment method created successfully!")
                        }
                    },
                    onFailure = {
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    },
                )
            },
            onFailed = { result ->
                Log.d(TAG, "FlexibleExpiryViewModel: Payment failed: ${result.errorType}")
                _paymentToken.value = ""
                snackbarHostState.showSnackbar("Error creating payment method")
            },
            onCanceled = {
                Log.d(TAG, "FlexibleExpiryViewModel: Payment canceled")
                snackbarHostState.showSnackbar("Payment canceled")
            },
        )
    }

    fun clearToken() {
        _paymentToken.value = ""
    }

    fun reinitialize() {
        initJob?.cancel()
        paymentResultJob?.cancel()
        clearToken()
        initializeSDK()
    }

    private companion object {
        private const val TAG = "FlexibleExpiryViewModel"
    }
}
