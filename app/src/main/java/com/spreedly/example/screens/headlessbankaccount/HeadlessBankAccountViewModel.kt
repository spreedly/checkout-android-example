package com.spreedly.example.screens.headlessbankaccount

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.app.R
import com.spreedly.example.AuthService
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.BankAccountFieldConfig
import com.spreedly.sdk.ui.CustomFieldsConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeadlessBankAccountViewModel(private val context: Context) : ViewModel() {
    val sdk = Spreedly()
    val snackbarHostState = SnackbarHostState()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

    private val _paymentFinished = MutableStateFlow(false)
    val paymentFinished: StateFlow<Boolean> = _paymentFinished.asStateFlow()

    private var lastPaymentCompletedTime = 0L

    private val _fieldConfig = MutableStateFlow(BankAccountFieldConfig.Default)
    val fieldConfig: StateFlow<BankAccountFieldConfig> = _fieldConfig.asStateFlow()

    private val _uiConfig = MutableStateFlow(CustomFieldsConfig())
    val uiConfig: StateFlow<CustomFieldsConfig> = _uiConfig.asStateFlow()

    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null

    init {
        viewModelScope.launch {
            initializeForPayment()
        }
    }

    /**
     * Fetch fresh auth params and re-subscribe to paymentResultFlow.
     * Must be called before each payment attempt since init() replaces the PaymentManager.
     */
    suspend fun initializeForPayment(): Boolean {
        _isInitializing.value = true
        return try {
            sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                .fold(
                    onSuccess = {
                        paymentResultJob?.cancel()
                        observePaymentResults()
                        Log.d(TAG, "SDK initialized with fresh auth")
                        _isInitializing.value = false
                        true
                    },
                    onFailure = { e ->
                        Log.d(TAG, "SDK initialization failed: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.headless_bank_account_sdk_init_failed),
                            withDismissAction = true,
                        )
                        _isInitializing.value = false
                        false
                    },
                )
        } catch (e: Exception) {
            Log.d(TAG, "SDK initialization error: ${e::class.simpleName}")
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.headless_bank_account_sdk_init_failed),
                withDismissAction = true,
            )
            _isInitializing.value = false
            false
        }
    }

    private fun observePaymentResults() {
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                Log.d(TAG, "Payment completed")
                _isProcessing.value = false
                _paymentToken.value = result.token
                _paymentFinished.value = true
                lastPaymentCompletedTime = System.currentTimeMillis()

                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = { retained ->
                        if (retained) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.headless_bank_account_saved),
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.headless_bank_account_tokenized),
                            )
                        }
                    },
                    onFailure = {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.headless_bank_account_save_failed),
                        )
                    },
                )
            },
            onFailed = { result ->
                Log.d(TAG, "Payment failed: ${result.errorType}")
                _isProcessing.value = false
                _paymentToken.value = ""
                _paymentFinished.value = true
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.headless_bank_account_payment_failed),
                    withDismissAction = true,
                )
            },
            onCanceled = {
                Log.d(TAG, "Payment canceled")
                _isProcessing.value = false
                snackbarHostState.showSnackbar(
                    context.getString(R.string.headless_bank_account_payment_canceled),
                )
            },
        )
    }

    fun setProcessing(processing: Boolean) {
        if (processing) {
            val timeSinceCompletion = System.currentTimeMillis() - lastPaymentCompletedTime
            if (timeSinceCompletion < 1000 && _paymentToken.value.isNotEmpty()) {
                Log.d(TAG, "Ignoring setProcessing(true) - payment completed ${timeSinceCompletion}ms ago")
                return
            }
        }
        _isProcessing.value = processing
    }

    fun startPaymentPolling() {
        viewModelScope.launch {
            delay(PAYMENT_TIMEOUT_MS)
            if (_isProcessing.value) {
                Log.d(TAG, "Payment timeout reached - resetting state")
                _isProcessing.value = false
                snackbarHostState.showSnackbar(
                    context.getString(R.string.headless_bank_account_timeout),
                )
            }
        }
    }

    fun clearPaymentToken() {
        _paymentToken.value = ""
        _paymentFinished.value = false
        _isProcessing.value = false
    }

    fun resetPaymentFinished() {
        _paymentFinished.value = false
    }

    fun updateFieldConfig(config: BankAccountFieldConfig) {
        _fieldConfig.value = config
    }

    fun updateUiConfig(config: CustomFieldsConfig) {
        _uiConfig.value = config
    }

    private companion object {
        private const val TAG = "HeadlessBankAccountVM"
        private const val PAYMENT_TIMEOUT_MS = 30_000L
    }
}
