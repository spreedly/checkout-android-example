package com.spreedly.example.screens.customtextfields

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.qa.FieldStateInspectorController
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.SplFieldStyleOverrides
import com.spreedly.example.ui.theme.SplFieldTarget
import com.spreedly.example.ui.theme.ThemeConfigurationController
import com.spreedly.example.utils.isCvvFormRequirementMet
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
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
class CustomTextFieldsViewModel(
    private val context: Context,
    private val skipAutoInitializeSdk: Boolean = false,
) : ViewModel() {
    val sdk = Spreedly()

    val snackbarHostState = SnackbarHostState()

    val fieldStateInspector = FieldStateInspectorController(sdk)
    val inspectorUiState = fieldStateInspector.uiState
    val themeConfiguration = ThemeConfigurationController()
    val useCustomTheme = themeConfiguration.useCustomTheme
    val selectedThemePreset = themeConfiguration.selectedPreset
    val fieldOverrideTarget = themeConfiguration.fieldOverrideTarget
    val fieldStyleOverrides = themeConfiguration.fieldOverrides

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

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

    private val _cardValid = MutableStateFlow(false)
    private val _cvvValid = MutableStateFlow(false)
    private val _expiryValid = MutableStateFlow(false)

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private var lastPaymentCompletedTime = 0L

    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    init {
        refreshInspectorAggregate()
        if (skipAutoInitializeSdk) {
            _isInitializing.value = false
        } else {
            initializeSDK()
        }
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
                        onFailure = {
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
            val timeSinceCompletion = System.currentTimeMillis() - lastPaymentCompletedTime
            if (timeSinceCompletion < 1000 && _paymentToken.value.isNotEmpty()) {
                return
            }
        }
        _isProcessing.value = processing
    }

    fun clearToken() {
        _paymentToken.value = ""
        lastPaymentCompletedTime = 0L
    }

    fun onFieldStateUpdate(state: HostedFieldState) {
        fieldStateInspector.onFieldStateChanged(state)
    }

    fun onHostedFieldValidation(fieldType: FormFieldType, isValid: Boolean) {
        when (fieldType) {
            is FormFieldType.CARD -> _cardValid.value = isValid
            is FormFieldType.CVV -> _cvvValid.value = isValid
            is FormFieldType.EXPIRY_DATE -> _expiryValid.value = isValid
            else -> Unit
        }
        refreshInspectorAggregate()
    }

    fun onCheckoutFieldsClearedBySdk() {
        _cardValid.value = false
        _cvvValid.value = false
        _expiryValid.value = false
        refreshInspectorAggregate()
    }

    fun performFullPaymentReset() {
        sdk.resetPaymentState()
        fieldStateInspector.resetInspector()
        _cardValid.value = false
        _cvvValid.value = false
        _expiryValid.value = false
        resetFormFields()
        refreshInspectorAggregate()
    }

    fun reinitialize() {
        initJob?.cancel()
        paymentResultJob?.cancel()
        clearToken()
        initializeSDK()
    }

    private fun handlePaymentRetention(result: PaymentResult.Completed) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "CustomTextFieldsViewModel: Retaining payment method")
                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = {
                        Log.d(TAG, "CustomTextFieldsViewModel: Payment method retained successfully")
                        snackbarHostState.showSnackbar("Payment method saved for future use!")
                    },
                    onFailure = {
                        Log.d(TAG, "CustomTextFieldsViewModel: Error retaining payment method")
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "CustomTextFieldsViewModel: Exception during retention: ${e::class.simpleName}")
            }
        }
    }

    fun updateNameInput(value: String) {
        _nameInput.value = NameInput(value, false)
        fieldStateInspector.logOnChangeReadout("Full Name", value)
        refreshInspectorAggregate()
    }

    fun updateAddressInput(value: String) {
        _addressInput.value = AddressInput(value, false)
        fieldStateInspector.logOnChangeReadout("Address Line 1", value)
        refreshInspectorAggregate()
    }

    fun updateCityInput(value: String) {
        _cityInput.value = CityInput(value, false)
        fieldStateInspector.logOnChangeReadout("City", value)
        refreshInspectorAggregate()
    }

    fun updateStateInput(value: String) {
        _stateInput.value = StateInput(value, false)
        fieldStateInspector.logOnChangeReadout("State", value)
        refreshInspectorAggregate()
    }

    fun updateZipCodeInput(value: String) {
        _zipCodeInput.value = ZipCodeInput(value, false)
        fieldStateInspector.logOnChangeReadout("Zip Code", value)
        refreshInspectorAggregate()
    }

    fun resetFormFields() {
        _nameInput.value = NameInput("")
        _addressInput.value = AddressInput("")
        _cityInput.value = CityInput("")
        _stateInput.value = StateInput("")
        _zipCodeInput.value = ZipCodeInput("")
    }

    fun startPaymentPolling() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000)
            if (_isProcessing.value) {
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment processing timeout")
            }
        }
    }

    private fun computeIsFormValid(): Boolean =
        _cardValid.value &&
            isCvvFormRequirementMet(_cvvValid.value) &&
            _expiryValid.value &&
            _nameInput.value.isValid &&
            _addressInput.value.isValid &&
            _cityInput.value.isValid &&
            _stateInput.value.isValid &&
            _zipCodeInput.value.isValid

    private fun refreshInspectorAggregate() {
        _isFormValid.value = computeIsFormValid()
        fieldStateInspector.configureAggregate(
            fields =
                listOf(
                    "Card number" to { _cardValid.value },
                    "Security code (CVC)" to { isCvvFormRequirementMet(_cvvValid.value) },
                    "MM/YY" to { _expiryValid.value },
                    "Full Name" to { _nameInput.value.isValid },
                    "Address Line 1" to { _addressInput.value.isValid },
                    "City" to { _cityInput.value.isValid },
                    "State" to { _stateInput.value.isValid },
                    "Zip Code" to { _zipCodeInput.value.isValid },
                ),
            isFormValid = ::computeIsFormValid,
        )
        fieldStateInspector.refreshMismatch(sdk.hostedCardDisplayState.value)
    }

    fun setUseCustomTheme(enabled: Boolean) {
        themeConfiguration.setUseCustomTheme(enabled)
    }

    fun setThemePreset(preset: SampleThemePreset) {
        themeConfiguration.setPreset(preset)
    }

    fun resetThemeConfiguration() {
        themeConfiguration.setUseCustomTheme(false)
    }

    fun setFieldOverrideTarget(target: SplFieldTarget) {
        themeConfiguration.setFieldOverrideTarget(target)
    }

    fun updateFieldStyleOverrides(overrides: SplFieldStyleOverrides) {
        themeConfiguration.updateFieldOverrides(overrides)
    }

    fun clearFieldStyleOverrides() {
        themeConfiguration.clearFieldOverrides()
    }

    fun resolveSplFieldConfig(
        formFieldType: FormFieldType,
        isDarkMode: Boolean,
    ) = themeConfiguration.resolveFieldConfig(formFieldType, isDarkMode)

    fun applyThemeToSdk(isDarkMode: Boolean) {
        themeConfiguration.applyGlobalTheme(sdk, isDarkMode)
    }

    private companion object {
        private const val TAG = "CustomTextFieldsViewModel"
    }
}
