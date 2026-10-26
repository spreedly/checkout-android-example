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
import com.spreedly.example.utils.DemoSdkAuthSession
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.SplFieldStyleOverrides
import com.spreedly.example.ui.theme.SplFieldTarget
import com.spreedly.example.ui.theme.ThemeConfigurationController
import com.spreedly.example.utils.isCvvFormRequirementMet
import com.spreedly.example.optionalfields.DemoOptionalFieldActions
import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.example.validation.DemoExpiryManifestTest
import com.spreedly.example.validation.DemoOptionalFieldValidators
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import java.util.concurrent.atomic.AtomicReference
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

    private val _fieldSelection = MutableStateFlow(DemoOptionalFieldSelection.default())
    val fieldSelection: StateFlow<DemoOptionalFieldSelection> = _fieldSelection.asStateFlow()

    private val _cardValid = MutableStateFlow(false)
    private val _cvvValid = MutableStateFlow(false)
    private val _expiryValid = MutableStateFlow(false)
    private val _monthValid = MutableStateFlow(false)
    private val _yearValid = MutableStateFlow(false)
    private val hostedOptionalValidity = linkedMapOf<OptionalFieldType, Boolean>()

    private val _demoInvalidExpiryManifest = MutableStateFlow(false)
    val demoInvalidExpiryManifest: StateFlow<Boolean> = _demoInvalidExpiryManifest.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private var lastPaymentCompletedTime = 0L
    private val checkoutValidationSnackbarDedupe = AtomicReference<String?>(null)

    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val authSession = DemoSdkAuthSession(SdkSessionManager(AuthService()))
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    init {
        syncHostedOptionalValidity(_fieldSelection.value)
        refreshInspectorAggregate()
        if (skipAutoInitializeSdk) {
            _isInitializing.value = false
        } else {
            initializeSDK()
        }
    }

    private fun registerDemoCustomValidators() {
        // No layout passed: this screen never registers core name (FULL_NAME) validators.
        DemoOptionalFieldValidators.sync(sdk, _fieldSelection.value)
    }

    private fun registerPaymentObservers() {
        registerDemoCustomValidators()
        observePaymentResults()
    }

    private fun initializeSDK() {
        initJob =
            viewModelScope.launch {
                try {
                    ensureFreshAuthBeforePayment()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Initialization error")
                    _isInitializing.value = false
                }
            }
    }

    suspend fun ensureFreshAuthBeforePayment(): Boolean =
        authSession.ensureFreshAuth(
            sdk = sdk,
            context = context,
            environmentKey = BuildConfig.ENVIRONMENT_KEY,
            isInitializing = _isInitializing,
            registerObserversIfNeeded = ::registerPaymentObservers,
            onFailure = { snackbarHostState.showSnackbar("Failed to initialize SDK") },
        )

    fun onPaymentSubmitResult(result: PaymentProcessingResult) {
        authSession.onPaymentSubmitResult(result)
    }

    private fun observePaymentResults() {
        paymentResultJob?.cancel()
        paymentResultJob =
            paymentResultHandler.observeResults(
                sdk = sdk,
                scope = viewModelScope,
                onCompleted = { result ->
                    _isProcessing.value = false
                    _paymentToken.value = result.token
                    lastPaymentCompletedTime = System.currentTimeMillis()
                    authSession.onTerminalPaymentResult(result)
                    if (result.shouldRetain) {
                        handlePaymentRetention(result)
                    } else {
                        snackbarHostState.showSnackbar("Payment successful!")
                    }
                },
                onFailed = { result ->
                    _isProcessing.value = false
                    authSession.onTerminalPaymentResult(result)
                    resolvePaymentFailedSnackbar(result)?.let { snackbarHostState.showSnackbar(it) }
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

    fun updateDemoInvalidExpiryManifest(enabled: Boolean) {
        if (_demoInvalidExpiryManifest.value == enabled) return
        _demoInvalidExpiryManifest.value = enabled
        _expiryValid.value = false
        _monthValid.value = false
        _yearValid.value = false
        refreshInspectorAggregate()
    }

    fun beginCheckoutAttempt() {
        checkoutValidationSnackbarDedupe.set(null)
    }

    /** Demo-only FA10 gate bypass — see [DemoExpiryManifestTest.skipPreSubmitFieldValidityGateForDemo]. */
    fun skipPreSubmitFieldValidityGateForDemo(): Boolean =
        DemoExpiryManifestTest.skipPreSubmitFieldValidityGateForDemo(_demoInvalidExpiryManifest.value)

    fun formatValidationFailedMessage(result: PaymentProcessingResult.ValidationFailed): String =
        DemoExpiryManifestTest.validationFailedMessage(
            invalidFields =
                result.invalidFields.ifEmpty {
                    hostedFormFields()
                },
        )

    fun resolveValidationFailedSnackbar(result: PaymentProcessingResult.ValidationFailed): String? =
        resolveCheckoutValidationSnackbar(formatValidationFailedMessage(result))

    fun resolvePaymentFailedSnackbar(result: PaymentResult.Failed): String? {
        if (
            DemoExpiryManifestTest.isDemoExpiryManifestConflictAsyncFailure(
                demoInvalidExpiryManifest = _demoInvalidExpiryManifest.value,
                formFields = hostedFormFields(),
                result = result,
            )
        ) {
            return resolveCheckoutValidationSnackbar(
                DemoExpiryManifestTest.checkoutBlockedMessage(hostedFormFields()),
            )
        }
        return "Payment failed: ${result.message ?: "Unknown error"}"
    }

    internal fun resolveCheckoutValidationSnackbar(message: String): String? {
        val previous = checkoutValidationSnackbarDedupe.getAndSet(message)
        return if (previous == message) null else message
    }

    fun updateFieldSelection(newSelection: DemoOptionalFieldSelection) {
        val previous = _fieldSelection.value
        if (previous == newSelection) return
        if (sdk.isInitialized) {
            DemoOptionalFieldActions.clearDisabledFields(sdk, previous, newSelection)
        }
        _fieldSelection.value = newSelection
        if (sdk.isInitialized) {
            DemoOptionalFieldValidators.sync(sdk, newSelection)
        }
        syncHostedOptionalValidity(newSelection, previous)
        refreshInspectorAggregate()
    }

    fun onHostedFieldValidation(fieldType: FormFieldType, isValid: Boolean) {
        when (fieldType) {
            is FormFieldType.CARD -> _cardValid.value = isValid
            is FormFieldType.CVV -> _cvvValid.value = isValid
            is FormFieldType.EXPIRY_DATE -> _expiryValid.value = isValid
            is FormFieldType.MONTH -> _monthValid.value = isValid
            is FormFieldType.YEAR -> _yearValid.value = isValid
            else -> {
                val optionalType = DemoOptionalFieldSelection.optionalFieldFrom(fieldType) ?: return
                if (_fieldSelection.value.isFieldEnabled(optionalType)) {
                    hostedOptionalValidity[optionalType] = isValid
                }
            }
        }
        refreshInspectorAggregate()
    }

    fun hostedFormFields(): List<FormFieldType> =
        _fieldSelection.value.toHostedFormFields(
            includeCoreCardFields = true,
            demoInvalidExpiryManifest = _demoInvalidExpiryManifest.value,
        )

    fun buildSubmitAdditionalFields(): Map<AdditionalField, String> {
        val merchantOwned =
            mapOf(
                AdditionalField.FULL_NAME to _nameInput.value.value,
                AdditionalField.ADDRESS_LINE_1 to _addressInput.value.value,
                AdditionalField.CITY to _cityInput.value.value,
            )
        return _fieldSelection.value.buildAdditionalFieldsMap(
            paymentState = sdk.paymentState.value,
            merchantOwned = merchantOwned,
        )
    }

    fun onCheckoutFieldsClearedBySdk() {
        _cardValid.value = false
        _cvvValid.value = false
        _expiryValid.value = false
        _monthValid.value = false
        _yearValid.value = false
        syncHostedOptionalValidity(_fieldSelection.value)
        refreshInspectorAggregate()
    }

    fun performFullPaymentReset() {
        sdk.resetPaymentState()
        fieldStateInspector.resetInspector()
        _cardValid.value = false
        _cvvValid.value = false
        _expiryValid.value = false
        _monthValid.value = false
        _yearValid.value = false
        syncHostedOptionalValidity(_fieldSelection.value)
        resetFormFields()
        refreshInspectorAggregate()
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

    fun resetFormFields() {
        _nameInput.value = NameInput("")
        _addressInput.value = AddressInput("")
        _cityInput.value = CityInput("")
        refreshInspectorAggregate()
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

    private fun syncHostedOptionalValidity(
        selection: DemoOptionalFieldSelection,
        previous: DemoOptionalFieldSelection? = null,
    ) {
        hostedOptionalValidity.keys.toList().forEach { key ->
            if (key !in selection.enabledFields) {
                hostedOptionalValidity.remove(key)
            }
        }
        selection.enabledFields.forEach { type ->
            val required = selection.isFieldRequired(type)
            val requiredChanged = previous != null && previous.isFieldRequired(type) != required
            // Reset to new default when field is new OR required flag changed — a stale
            // optional-default (true) must not silently pass the button gate when required flips on.
            if (type !in hostedOptionalValidity || requiredChanged) {
                hostedOptionalValidity[type] =
                    DemoOptionalFieldSelection.defaultValidityForField(
                        type = type,
                        required = required,
                    )
            }
        }
    }

    private fun requiredHostedFieldsValid(selection: DemoOptionalFieldSelection): Boolean =
        selection.enabledFields
            .filter { selection.isFieldRequired(it) }
            .all { hostedOptionalValidity[it] == true }

    // An optional field that received a blur event and failed custom validation keeps
    // hostedOptionalValidity[type] == false until the user corrects it. The button must
    // stay disabled while any such inline error is showing, even though the field is
    // not required for form submission.
    private fun noOptionalHostedFieldHasError(selection: DemoOptionalFieldSelection): Boolean =
        selection.enabledFields
            .filter { !selection.isFieldRequired(it) }
            .none { hostedOptionalValidity[it] == false }

    private fun computeCanAttemptPay(): Boolean {
        // FA10 demo mounts MONTH/YEAR for visual testing; submit fails on manifest config, not field values.
        val expiryValid = _expiryValid.value
        return _cardValid.value &&
            isCvvFormRequirementMet(_cvvValid.value) &&
            expiryValid &&
            _nameInput.value.isValid &&
            _addressInput.value.isValid &&
            _cityInput.value.isValid
    }

    private fun computeIsFormValid(): Boolean =
        computeCanAttemptPay() &&
            requiredHostedFieldsValid(_fieldSelection.value) &&
            noOptionalHostedFieldHasError(_fieldSelection.value)

    private fun refreshInspectorAggregate() {
        _isFormValid.value = computeIsFormValid()
        val selection = _fieldSelection.value
        val dynamicHostedFields =
            selection.enabledFields.map { type ->
                DemoOptionalFieldActions.displayLabel(type) to {
                    hostedOptionalValidity[type]
                        ?: DemoOptionalFieldSelection.defaultValidityForField(
                            type = type,
                            required = selection.isFieldRequired(type),
                        )
                }
            }
        val expiryInspectorFields =
            if (_demoInvalidExpiryManifest.value) {
                listOf(
                    "MM/YY" to { _expiryValid.value },
                    "Month" to { _monthValid.value },
                    "Year" to { _yearValid.value },
                )
            } else {
                listOf("MM/YY" to { _expiryValid.value })
            }
        fieldStateInspector.configureAggregate(
            fields =
                listOf(
                    "Card number" to { _cardValid.value },
                    "Security code (CVC)" to { isCvvFormRequirementMet(_cvvValid.value) },
                ) + expiryInspectorFields +
                    listOf(
                        "Full Name" to { _nameInput.value.isValid },
                        "Address Line 1" to { _addressInput.value.isValid },
                        "City" to { _cityInput.value.isValid },
                    ) + dynamicHostedFields,
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

    override fun onCleared() {
        DemoOptionalFieldValidators.clear(sdk)
        super.onCleared()
    }

    private companion object {
        private const val TAG = "CustomTextFieldsViewModel"
    }
}
