package com.spreedly.example.screens.customtextfields

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
 * ViewModel for managing CustomTextFieldsScreen state.
 * This ViewModel handles both the Spreedly SDK instance and the custom form field states
 * that cannot be saved using rememberSaveable due to their complex nature.
 */
class CustomTextFieldsViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

    // Custom form field states - managed in ViewModel to survive configuration changes
    private val _nameInput = MutableStateFlow(NameInput(""))
    val nameInput: StateFlow<NameInput> = _nameInput.asStateFlow()

    private val _addressInput = MutableStateFlow(AddressInput(""))
    val addressInput: StateFlow<AddressInput> = _addressInput.asStateFlow()

    private val _cityInput = MutableStateFlow(CityInput(""))
    val cityInput: StateFlow<CityInput> = _cityInput.asStateFlow()

    private val _stateInput = MutableStateFlow(StateInput(""))
    val stateInput: StateFlow<StateInput> = _stateInput.asStateFlow()

    private val _zipCodeInput = MutableStateFlow(ZipCodeInput(""))
    val zipCodeInput: StateFlow<ZipCodeInput> = _zipCodeInput.asStateFlow()

    // Track when payment completed to prevent race conditions
    private var lastPaymentCompletedTime = 0L

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

    private fun initializeSDK() {
        initJob = viewModelScope.launch {
            try {
                _isInitializing.value = true
                sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                    .fold(
                        onSuccess = {
                            observePaymentResults()
                            _isInitializing.value = false
                        },
                        onFailure = { e ->
                            snackbarHostState.showSnackbar("Failed to initialize SDK")
                            _isInitializing.value = false
                        },
                    )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Initialization error")
                _isInitializing.value = false
            }
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
                } else {
                    snackbarHostState.showSnackbar("Payment successful!")
                }
            },
            onFailed = { result ->
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment failed: ${result.message}")
            },
            onCanceled = {
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment canceled")
            },
        )
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
        _paymentToken.value = ""
        lastPaymentCompletedTime = 0L // Reset completion tracking
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
                Log.d(TAG, "CustomTextFieldsViewModel: Retaining payment method")
                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = {
                        Log.d(TAG, "CustomTextFieldsViewModel: Payment method retained successfully")
                        snackbarHostState.showSnackbar("Payment method saved for future use!")
                    },
                    onFailure = { e ->
                        Log.d(TAG, "CustomTextFieldsViewModel: Error retaining payment method: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "CustomTextFieldsViewModel: Exception during retention: ${e::class.simpleName}")
            }
        }
    }

    // Form field update methods
    fun updateNameInput(value: String) {
        _nameInput.value = NameInput(value, false)
    }

    fun updateAddressInput(value: String) {
        _addressInput.value = AddressInput(value, false)
    }

    fun updateCityInput(value: String) {
        _cityInput.value = CityInput(value, false)
    }

    fun updateStateInput(value: String) {
        _stateInput.value = StateInput(value, false)
    }

    fun updateZipCodeInput(value: String) {
        _zipCodeInput.value = ZipCodeInput(value, false)
    }

    // Reset all form fields
    fun resetFormFields() {
        _nameInput.value = NameInput("")
        _addressInput.value = AddressInput("")
        _cityInput.value = CityInput("")
        _stateInput.value = StateInput("")
        _zipCodeInput.value = ZipCodeInput("")
    }

    // Backup timeout mechanism in case payment result flow fails
    fun startPaymentPolling() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000) // 30-second timeout
            if (_isProcessing.value) {
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment processing timeout")
            }
        }
    }

    private companion object {
        private const val TAG = "CustomTextFieldsViewModel"
    }
}
