package com.spreedly.example.screens.basiccheckout

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.models.SavedPaymentMethod
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
import com.spreedly.result.Result
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.SpreedlyErrorMessages
import com.spreedly.sdk.SpreedlyNetworkError
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.models.RecacheConfig
import com.spreedly.sdk.models.SavedCardInfo
import com.spreedly.sdk.models.ScreenPresentationMode
import com.spreedly.sdk.models.paymentMethodUpdatedAt
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.validation.EmailValidator
import com.spreedly.validation.ValidationParameter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for BasicCheckoutScreen managing payment state and SDK initialization.
 */
class BasicCheckoutViewModel(
    private val context: Context,
    val sdk: Spreedly = Spreedly(),
    private val skipAutoInitializeSdk: Boolean = false,
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentToken = MutableStateFlow("")
    val paymentToken: StateFlow<String> = _paymentToken.asStateFlow()

    private var lastPaymentCompletedTime = 0L

    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    private val _savedPaymentMethods = MutableStateFlow<List<SavedPaymentMethod>>(emptyList())
    val savedPaymentMethods: StateFlow<List<SavedPaymentMethod>> = _savedPaymentMethods.asStateFlow()

    val fieldStateInspector = FieldStateInspectorController(sdk)
    val inspectorUiState = fieldStateInspector.uiState
    val themeConfiguration = ThemeConfigurationController()
    val useCustomTheme = themeConfiguration.useCustomTheme
    val selectedThemePreset = themeConfiguration.selectedPreset
    val fieldOverrideTarget = themeConfiguration.fieldOverrideTarget
    val fieldStyleOverrides = themeConfiguration.fieldOverrides

    private val _cardValid = MutableStateFlow(false)
    private val _cvvValid = MutableStateFlow(false)
    private val _expiryValid = MutableStateFlow(false)
    private val _nameValid = MutableStateFlow(false)
    private val _emailValid = MutableStateFlow(false)

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private val _checkoutEvent =
        MutableSharedFlow<PaymentProcessingResult>(
            replay = 0,
            extraBufferCapacity = 1,
        )
    val checkoutEvent: SharedFlow<PaymentProcessingResult> = _checkoutEvent

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
        _nameValid.value = false
        _emailValid.value = false
        clearCustomFieldInputErrors()
        refreshInspectorAggregate()
    }

    private fun computeIsFormValid(): Boolean =
        _cardValid.value &&
            isCvvFormRequirementMet(_cvvValid.value) &&
            _expiryValid.value &&
            _nameValid.value &&
            _emailValid.value

    private fun refreshInspectorAggregate() {
        _isFormValid.value = computeIsFormValid()
        fieldStateInspector.configureAggregate(
            fields =
                listOf(
                    "Card number" to { _cardValid.value },
                    "Security code (CVC)" to { isCvvFormRequirementMet(_cvvValid.value) },
                    "MM/YY" to { _expiryValid.value },
                    "Full Name" to { _nameValid.value },
                    "Email" to { _emailValid.value },
                ),
            isFormValid = ::computeIsFormValid,
        )
        fieldStateInspector.refreshMismatch(sdk.hostedCardDisplayState.value)
    }

    fun updateNameValidity(name: String) {
        _nameValid.value = isNameValueValid(name)
        refreshInspectorAggregate()
    }

    fun updateEmailValidity(email: String) {
        _emailValid.value = isEmailValueValid(email)
        refreshInspectorAggregate()
    }

    private fun isNameValueValid(name: String): Boolean =
        name.isNotBlank() && name.length >= 2

    private fun isEmailValueValid(email: String): Boolean =
        email.isNotBlank() && EmailValidator.isValid(email)

    fun clearCustomFieldInputErrors() {
        _nameError.value = null
        _emailError.value = null
    }

    fun clearNameError() {
        _nameError.value = null
    }

    fun clearEmailError() {
        _emailError.value = null
    }

    fun validateName(name: String): Boolean {
        _nameError.value =
            when {
                name.isBlank() -> "Name is required"
                name.length < 2 -> "Name must be at least 2 characters"
                else -> null
            }
        _nameValid.value = isNameValueValid(name)
        fieldStateInspector.logNonSensitiveFieldChange(FormFieldType.NAME(true), name)
        refreshInspectorAggregate()
        return _nameValid.value
    }

    fun validateEmail(email: String): Boolean {
        _emailError.value =
            when {
                email.isBlank() -> "Email is required"
                !EmailValidator.isValid(email) -> "Invalid email format"
                else -> null
            }
        _emailValid.value = isEmailValueValid(email)
        fieldStateInspector.logOnChangeReadout("Email", email)
        refreshInspectorAggregate()
        return _emailValid.value
    }

    suspend fun submitCheckout(params: SubmitCheckoutParams): PaymentProcessingResult {
        val additionalFields =
            mapOf(
                AdditionalField.FULL_NAME to params.fullName.split(" ").firstOrNull().orEmpty(),
                AdditionalField.EMAIL to params.email,
            )
        return sdk.createCreditCard(
            formFields = params.formFields,
            additionalFields = additionalFields,
            metadata =
                mapOf(
                    "checkout_type" to "basic_with_custom_fields",
                    "timestamp" to "${System.currentTimeMillis()}",
                    "pattern" to "ios_style",
                ),
            retainOnSuccess = params.shouldRetainPaymentMethod,
            eligibleForCardUpdater = params.eligibleForCardUpdater?.takeIf { it },
        )
    }

    fun launchSubmitCheckout(params: SubmitCheckoutParams) {
        viewModelScope.launch {
            try {
                val result = submitCheckout(params)
                _checkoutEvent.emit(result)
            } catch (e: Exception) {
                Log.d(TAG, "BasicCheckoutViewModel: Error during payment processing: ${e::class.simpleName}")
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment processing error")
            }
        }
    }

    init {
        refreshInspectorAggregate()
        if (skipAutoInitializeSdk) {
            _isInitializing.value = false
        } else {
            initializeSDK()
            loadSavedPaymentMethods()
            fetchPaymentMethodsFromBackend()
        }
    }

    private fun initializeSDK() {
        initJob =
            viewModelScope.launch {
                _isInitializing.value = true
                sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                    .fold(
                        onSuccess = {
                            Log.d(TAG, "BasicCheckoutViewModel: SDK initialized successfully")
                            observePaymentResults()
                        },
                        onFailure = { e ->
                            Log.d(TAG, "BasicCheckoutViewModel: SDK initialization failed: ${e::class.simpleName}")
                            snackbarHostState.showSnackbar("SDK initialization failed")
                        },
                    )
                _isInitializing.value = false
            }
    }

    private fun observePaymentResults() {
        paymentResultJob =
            paymentResultHandler.observeResults(
                sdk = sdk,
                scope = viewModelScope,
                onCompleted = { result ->
                    Log.d(TAG, "BasicCheckoutViewModel: Payment completed")
                    _isProcessing.value = false
                    _paymentToken.value = result.token
                    lastPaymentCompletedTime = System.currentTimeMillis()
                    paymentResultHandler.retainIfNeeded(result).fold(
                        onSuccess = { retained ->
                            if (retained) {
                                fetchPaymentMethodsFromBackend()
                                snackbarHostState.showSnackbar("Payment method saved for future use!")
                            } else {
                                snackbarHostState.showSnackbar("Payment method created successfully!")
                            }
                        },
                        onFailure = { e ->
                            snackbarHostState.showSnackbar("Payment created but failed to save")
                        },
                    )
                },
                onFailed = { result ->
                    Log.d(TAG, "BasicCheckoutViewModel: Payment failed: ${result.errorType}")
                    _isProcessing.value = false
                    _paymentToken.value = ""
                    if (!result.isClientSideFormValidationFailure()) {
                        snackbarHostState.showSnackbar("Error creating payment method")
                    }
                },
                onCanceled = {
                    Log.d(TAG, "BasicCheckoutViewModel: Payment canceled")
                    _isProcessing.value = false
                    snackbarHostState.showSnackbar("Payment canceled")
                },
            )
    }

    fun setProcessing(processing: Boolean) {
        Log.d(TAG, "BasicCheckoutViewModel: setProcessing called with: $processing")

        if (processing) {
            val timeSinceCompletion = System.currentTimeMillis() - lastPaymentCompletedTime
            if (timeSinceCompletion < 1000 && _paymentToken.value.isNotEmpty()) {
                Log.d(
                    TAG,
                    "BasicCheckoutViewModel: Ignoring setProcessing(true) - " +
                        "payment completed ${timeSinceCompletion}ms ago",
                )
                return
            }
        }

        _isProcessing.value = processing
    }

    fun clearToken() {
        _paymentToken.value = ""
        lastPaymentCompletedTime = 0L
    }

    fun reinitialize() {
        initJob?.cancel()
        paymentResultJob?.cancel()
        clearToken()
        initializeSDK()
    }

    fun startPaymentPolling() {
        Log.d(TAG, "BasicCheckoutViewModel: Starting payment timeout protection")
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000)
            if (_isProcessing.value) {
                Log.d(TAG, "BasicCheckoutViewModel: Payment timeout reached - resetting state")
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment processing timeout")
            }
        }
    }

    fun loadSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                val methods = paymentMethodRepository.getSavedPaymentMethods()
                _savedPaymentMethods.value = methods
                Log.d(TAG, "BasicCheckoutViewModel: Loaded ${methods.size} saved payment methods")
            } catch (e: Exception) {
                Log.d(TAG, "BasicCheckoutViewModel: Error loading saved payment methods: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error loading saved payment methods")
            }
        }
    }

    private fun fetchPaymentMethodsFromBackend() {
        viewModelScope.launch {
            try {
                paymentMethodRepository.fetchAndSyncPaymentMethods().fold(
                    onSuccess = { methods ->
                        _savedPaymentMethods.value = methods
                        Log.d(TAG, "BasicCheckoutViewModel: Fetched ${methods.size} payment methods from backend")
                    },
                    onFailure = { e ->
                        Log.d(TAG, "BasicCheckoutViewModel: Error fetching payment methods: ${e::class.simpleName}")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "BasicCheckoutViewModel: Exception fetching payment methods: ${e::class.simpleName}")
            }
        }
    }

    fun recacheSavedPaymentMethod(savedCard: SavedPaymentMethod) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true

                val config =
                    RecacheConfig(
                        recachePresentationMode = ScreenPresentationMode.bottomSheet,
                        cardInfo =
                            SavedCardInfo(
                                lastFourDigits = savedCard.lastFourDigits,
                                cardType = savedCard.cardType,
                                cardholderName = savedCard.cardholderName,
                            ),
                        labelText = "CVV",
                        placeholderText = "123",
                        buttonText = "Confirm",
                        cancelButtonText = "Cancel",
                    )

                sdk.setParam(ValidationParameter.ALLOW_BLANK_NAME, true)

                val result =
                    sdk.recachePaymentMethod(
                        paymentMethodToken = savedCard.token,
                        config = config,
                    )

                _isProcessing.value = false

                when (val recacheResult = result) {
                    is Result.Success -> {
                        val response = recacheResult.data
                        _paymentToken.value = response.transaction.paymentMethod.token
                        Log.d(
                            TAG,
                            "BasicCheckoutViewModel: Recached " +
                                "token: ${response.transaction.paymentMethod.token}",
                        )
                        snackbarHostState.showSnackbar(
                            "CVV updated. Updated at: ${response.paymentMethodUpdatedAt}",
                        )
                        retainAfterRecache(response.transaction.paymentMethod.token)
                    }

                    is Result.Error -> {
                        if (recacheResult.error != SpreedlyNetworkError.USER_CANCELLED) {
                            val errorMessage =
                                SpreedlyErrorMessages.getUserFriendlyMessage(
                                    error = recacheResult.error,
                                    defaultMessage = "Failed to update payment method. Please try again.",
                                )
                            snackbarHostState.showSnackbar(errorMessage)
                            Log.d(TAG, "BasicCheckoutViewModel: Recaching error: ${recacheResult.error.safeDescription()}")
                        } else {
                            Log.d(TAG, "BasicCheckoutViewModel: Recaching cancelled by user")
                        }
                    }
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                Log.d(TAG, "BasicCheckoutViewModel: Exception during recaching: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Recaching error")
            }
        }
    }

    private fun retainAfterRecache(token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "BasicCheckoutViewModel: Retaining after recache")
                paymentMethodRepository.retainPaymentMethod(token).fold(
                    onSuccess = {
                        Log.d(TAG, "BasicCheckoutViewModel: Successfully retained after recache")
                        snackbarHostState.showSnackbar("Payment method recached and retained!")
                    },
                    onFailure = {
                        Log.d(TAG, "BasicCheckoutViewModel: Error retaining after recache: ${it::class.simpleName}")
                        snackbarHostState.showSnackbar("Recached successfully!")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "BasicCheckoutViewModel: Exception retaining after recache: ${e::class.simpleName}")
            }
        }
    }

    fun deleteSavedPaymentMethod(token: String) {
        viewModelScope.launch {
            try {
                paymentMethodRepository.deletePaymentMethod(token)
                loadSavedPaymentMethods()
                snackbarHostState.showSnackbar("Payment method deleted")
            } catch (e: Exception) {
                Log.d(TAG, "BasicCheckoutViewModel: Error deleting payment method: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error deleting payment method")
            }
        }
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
        private const val TAG = "BasicCheckoutViewModel"
    }
}
