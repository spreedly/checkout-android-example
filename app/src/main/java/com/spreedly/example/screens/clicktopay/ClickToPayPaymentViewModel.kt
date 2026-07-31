package com.spreedly.example.screens.clicktopay

import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.clicktopay.ClickToPaySavedCardsDetectorResult
import com.spreedly.clicktopay.ClickToPayEvent
import com.spreedly.clicktopay.ClickToPayCheckoutConfig
import com.spreedly.clicktopay.ClickToPayCustomer
import com.spreedly.clicktopay.ClickToPayFlowPhase
import com.spreedly.clicktopay.SpreedlyClickToPayCheckout
import com.spreedly.clicktopay.tokenize.ClickToPayMetadata
import com.spreedly.example.AuthService
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.screens.common.Product
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.ThemeConfigurationController
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.hostedfields.utils.getDisplayValue
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.clicktopay.ui.ClickToPaySavedCardsDetectorController
import com.spreedly.validation.EmailValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DeviceRecognitionState {
    Checking,
    NotRecognized,
    Recognized,
    DetectionFailed,
    UsingDifferentEmail,
}

class ClickToPayPaymentViewModel(
    private val context: Context,
    val spreedlySdk: Spreedly = Spreedly(),
) : ViewModel() {
    val snackbarHostState = SnackbarHostState()

    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(PaymentMethodRepository(context))
    val themeConfiguration = ThemeConfigurationController()
    val useCustomTheme = themeConfiguration.useCustomTheme
    val selectedThemePreset = themeConfiguration.selectedPreset

    enum class Stage {
        IDLE,
        CHECKOUT,
        TOKENIZING,
    }

    private val _stage = MutableStateFlow(Stage.IDLE)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _doLookup = MutableStateFlow(true)
    val doLookup: StateFlow<Boolean> = _doLookup.asStateFlow()

    private val _deviceRecognition = MutableStateFlow(DeviceRecognitionState.Checking)
    val deviceRecognition: StateFlow<DeviceRecognitionState> = _deviceRecognition.asStateFlow()

    private val _recognizedCardLabels = MutableStateFlow(RecognizedCardLabelList())
    val recognizedCardLabels: StateFlow<RecognizedCardLabelList> = _recognizedCardLabels.asStateFlow()

    private val _savedCardsDetectorKey = MutableStateFlow(-1)
    val savedCardsDetectorKey: StateFlow<Int> = _savedCardsDetectorKey.asStateFlow()

    val savedCardsDetectorConfig: ClickToPayCheckoutConfig =
        ClickToPaySandboxCatalog.buildSavedCardsDetectorConfig(transactionAmount = 100.0)

    private var savedCardsDetectorController: ClickToPaySavedCardsDetectorController? = null
    private val savedCardsDetectorTearDownAwaiter = SavedCardsDetectorTearDownAwaiter()

    /** Bumped in [prepareForCheckout]; remount runs at most once per checkout session. */
    private var checkoutSessionGeneration = 0
    private var remountedDetectorForCheckoutGeneration = -1
    private var remountJob: Job? = null

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    private val _merchantPrefill = MutableStateFlow(ClickToPayMerchantPrefill())
    val merchantPrefill: StateFlow<ClickToPayMerchantPrefill> = _merchantPrefill.asStateFlow()

    private val _firstNameError = MutableStateFlow<String?>(null)
    val firstNameError: StateFlow<String?> = _firstNameError.asStateFlow()

    private val _lastNameError = MutableStateFlow<String?>(null)
    val lastNameError: StateFlow<String?> = _lastNameError.asStateFlow()

    private val _flowPhase = MutableStateFlow(ClickToPayFlowPhase.IDLE)
    val flowPhase: StateFlow<ClickToPayFlowPhase> = _flowPhase.asStateFlow()

    private val _eventLog = MutableStateFlow(ClickToPayEventLog())
    val eventLog: StateFlow<ClickToPayEventLog> = _eventLog.asStateFlow()

    private val _pendingMetadata = MutableStateFlow<ClickToPayMetadata?>(null)
    val pendingMetadata: StateFlow<ClickToPayMetadata?> = _pendingMetadata.asStateFlow()

    private val _pendingVerificationValue = MutableStateFlow<String?>(null)
    val pendingVerificationValue: StateFlow<String?> = _pendingVerificationValue.asStateFlow()

    val products = listOf(
        Product("Sunglasses", "Premium UV protection", 4400, "🕶️"),
        Product("Watch", "Swiss precision", 19900, "⌚"),
        Product("Headphones", "Noise cancelling", 29900, "🎧"),
        Product("Camera", "Professional grade", 89900, "📷"),
        Product("Laptop", "Ultra portable", 129900, "💻"),
        Product("Phone", "Latest model", 99900, "📱"),
    )

    private var paymentResultJob: Job? = null
    private var eventsJob: Job? = null
    private var stateJob: Job? = null

    init {
        SpreedlyClickToPayCheckout.setAutoTokenizeAuthRefresher {
            refreshSdkAuth()
        }
        initializeSdkOnScreenLoad()
    }

    private suspend fun refreshSdkAuth(): Boolean {
        val ok =
            sdkSessionManager.initializeSdk(
                sdk = spreedlySdk,
                context = context.applicationContext,
                environmentKey = BuildConfig.ENVIRONMENT_KEY,
            ).isSuccess
        if (ok) {
            startPaymentResultObserver()
        }
        return ok
    }

    private fun initializeSdkOnScreenLoad() {
        viewModelScope.launch {
            val initialized = initializeSdkIfNeeded()
            if (initialized) {
                startPaymentResultObserver()
                startClickToPayObservers()
            }
        }
    }

    private fun startClickToPayObservers() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            SpreedlyClickToPayCheckout.events.collect { event ->
                handleClickToPayEvent(event)
            }
        }
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            SpreedlyClickToPayCheckout.state.collect { state ->
                _flowPhase.value = state.phase
            }
        }
    }

    private fun handleClickToPayEvent(event: ClickToPayEvent) {
        when (event) {
            is ClickToPayEvent.CheckoutStarted ->
                appendEvent("CheckoutStarted(${event.checkoutId.take(8)}…)")

            is ClickToPayEvent.StateChanged ->
                appendEvent("StateChanged(${event.state.phase})")

            is ClickToPayEvent.DisplayCardsReady ->
                appendEvent("DisplayCardsReady(${event.cards.size} cards)")

            is ClickToPayEvent.NewUserEnrollmentRequired ->
                appendEvent("NewUserEnrollmentRequired")

            is ClickToPayEvent.CheckoutComplete -> {
                appendEvent("CheckoutComplete")
                _stage.value = Stage.TOKENIZING
            }

            is ClickToPayEvent.CheckoutCancelled -> {
                appendEvent("CheckoutCancelled")
                if (_stage.value == Stage.CHECKOUT) {
                    _errorMessage.value = "Click to Pay checkout was canceled."
                    _stage.value = Stage.IDLE
                    remountSavedCardsDetectorAfterCheckoutInterrupted()
                }
            }

            is ClickToPayEvent.Error -> {
                appendEvent("Error(${event.code})")
                if (_stage.value != Stage.IDLE) {
                    _errorMessage.value = event.message
                    _stage.value = Stage.IDLE
                    _pendingMetadata.value = null
                    _pendingVerificationValue.value = null
                    remountSavedCardsDetectorAfterCheckoutInterrupted()
                }
            }

            is ClickToPayEvent.Initialized ->
                appendEvent("Initialized(success=${event.success})")

            is ClickToPayEvent.VerifiedUser ->
                appendEvent("VerifiedUser")

            is ClickToPayEvent.ExistingUser ->
                appendEvent("ExistingUser")

            is ClickToPayEvent.AddNewCard ->
                appendEvent("AddNewCard(${event.availableCardBrands.joinToString()})")

            is ClickToPayEvent.OtpInitiated ->
                appendEvent("OtpInitiated(${event.maskedValidationChannel ?: "?"})")

            is ClickToPayEvent.OtpResponse ->
                appendEvent("OtpResponse(success=${event.success})")

            is ClickToPayEvent.OtpResend ->
                appendEvent("OtpResend")

            is ClickToPayEvent.OtpNotYou ->
                appendEvent("OtpNotYou")

            is ClickToPayEvent.SessionDeleted -> {
                appendEvent("SessionDeleted")
                if (_stage.value == Stage.CHECKOUT) {
                    _stage.value = Stage.IDLE
                    remountSavedCardsDetectorAfterCheckoutInterrupted()
                }
            }

            is ClickToPayEvent.CheckoutDifferentPaymentMethod ->
                appendEvent("CheckoutDifferentPaymentMethod")

            is ClickToPayEvent.OtpChannelSelectionRequired ->
                appendEvent("OtpChannelSelectionRequired(${event.channels.size})")

            is ClickToPayEvent.PaymentMethodTokenized -> {
                appendEvent("PaymentMethodTokenized([redacted])")
                _stage.value = Stage.IDLE
                _pendingMetadata.value = null
                _pendingVerificationValue.value = null
                remountSavedCardsDetectorAfterCheckoutInterrupted()
            }

            is ClickToPayEvent.CheckoutWindowOpen ->
                appendEvent("CheckoutWindowOpen")

            is ClickToPayEvent.CheckoutWindowClose ->
                appendEvent("CheckoutWindowClose")

            is ClickToPayEvent.ValidationErrors ->
                appendEvent("ValidationErrors(${event.errors.size})")
        }
    }

    private fun startPaymentResultObserver() {
        if (!spreedlySdk.isInitialized) return
        paymentResultJob?.cancel()
        paymentResultJob = paymentResultHandler.observeResults(
            sdk = spreedlySdk,
            scope = viewModelScope,
            onCompleted = { result ->
                if (_stage.value != Stage.CHECKOUT && _stage.value != Stage.TOKENIZING) {
                    return@observeResults
                }
                val token = result.token
                _successMessage.value =
                    "Payment method tokenized${if (token.isNotBlank()) ": $token" else "."}"
                _stage.value = Stage.IDLE
                _pendingMetadata.value = null
                _pendingVerificationValue.value = null
                remountSavedCardsDetectorAfterCheckoutInterrupted()
            },
            onFailed = { result ->
                if (_stage.value != Stage.CHECKOUT && _stage.value != Stage.TOKENIZING) {
                    return@observeResults
                }
                _errorMessage.value = result.message ?: "Click to Pay tokenization failed."
                _stage.value = Stage.IDLE
                _pendingMetadata.value = null
                _pendingVerificationValue.value = null
                remountSavedCardsDetectorAfterCheckoutInterrupted()
            },
            onCanceled = {
                if (_stage.value != Stage.CHECKOUT && _stage.value != Stage.TOKENIZING) {
                    return@observeResults
                }
                _errorMessage.value = "Click to Pay tokenization was canceled."
                _stage.value = Stage.IDLE
                _pendingMetadata.value = null
                _pendingVerificationValue.value = null
                remountSavedCardsDetectorAfterCheckoutInterrupted()
            },
        )
    }

    suspend fun prepareForCheckout(isDarkMode: Boolean): Boolean {
        applyThemeToSdk(isDarkMode)
        val product = _selectedProduct.value
        if (product == null) {
            _errorMessage.value = "Please select a product"
            return false
        }
        if (product.price <= 0) {
            _errorMessage.value = "Invalid product price"
            return false
        }
        if (!validateMerchantContactOnPay()) {
            return false
        }
        if (!refreshSdkAuth()) {
            _errorMessage.value = "Failed to refresh auth params"
            return false
        }

        if (!tearDownSavedCardsDetectorAndAwait()) {
            _errorMessage.value =
                "Click to Pay is still closing the saved-cards detector. Wait a moment and try again."
            return false
        }
        remountJob?.cancel()
        remountJob = null
        checkoutSessionGeneration++
        remountedDetectorForCheckoutGeneration = -1
        _errorMessage.value = null
        _successMessage.value = null
        _pendingMetadata.value = null
        _pendingVerificationValue.value = null
        _eventLog.value = ClickToPayEventLog()
        _stage.value = Stage.CHECKOUT
        return true
    }

    fun merchantCheckoutConfig(): ClickToPayCheckoutConfig? {
        val product = _selectedProduct.value ?: return null
        if (product.price <= 0) return null
        val amount = product.price / 100.0
        val prefill = _merchantPrefill.value
        return ClickToPaySandboxCatalog.buildCheckoutConfig(
            email = _email.value,
            doLookup = _doLookup.value,
            transactionAmount = amount,
            merchantPrefill = prefill,
            currencyCode = "USD",
        )
    }

    fun startPayment(activity: Activity, isDarkMode: Boolean) {
        applyThemeToSdk(isDarkMode)
        val product = _selectedProduct.value
        if (product == null) {
            _errorMessage.value = "Please select a product"
            return
        }
        if (product.price <= 0) {
            _errorMessage.value = "Invalid product price"
            return
        }
        if (!validateMerchantContactOnPay()) {
            return
        }

        viewModelScope.launch {
            if (!prepareForCheckout(isDarkMode)) {
                return@launch
            }

            val config = merchantCheckoutConfig() ?: return@launch
            SpreedlyClickToPayCheckout.present(config, activity)
        }
    }

    fun tokenizeWithCvv(encryptedCvv: String) {
        val metadata = _pendingMetadata.value
        if (metadata == null) {
            _errorMessage.value = "No checkout metadata — complete Click to Pay checkout first"
            return
        }
        val cvv = getDisplayValue(encryptedCvv, FormFieldType.CVV(true))
        if (cvv.isBlank()) {
            _errorMessage.value = "CVV is required"
            return
        }

        viewModelScope.launch {
            if (!refreshSdkAuth()) {
                _errorMessage.value = "Failed to refresh auth params"
                return@launch
            }
            if (!spreedlySdk.isInitialized) {
                _errorMessage.value = "SDK is still initializing, please wait..."
                return@launch
            }

            _errorMessage.value = null
            val prefill = _merchantPrefill.value
            when (
                SpreedlyClickToPayCheckout.tokenize(
                    metadata = metadata,
                    verificationValue = cvv,
                    billing = prefill.makeTokenizeBilling(_email.value).toBillingFields(_email.value),
                )
            ) {
                is PaymentProcessingResult.ValidationFailed ->
                    _errorMessage.value = "CVV validation failed"

                else -> Unit
            }
        }
    }

    fun cancelCheckout() {
        SpreedlyClickToPayCheckout.cancel()
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun setDoLookup(enabled: Boolean) {
        _doLookup.value = enabled
    }

    fun onMerchantScreenDisplayed() {
        _deviceRecognition.value = DeviceRecognitionState.Checking
        _recognizedCardLabels.value = RecognizedCardLabelList()
        _savedCardsDetectorKey.value++
    }

    fun onSavedCardsDetectorControllerReady(controller: ClickToPaySavedCardsDetectorController) {
        savedCardsDetectorController = controller
    }

    fun onSavedCardsDetectorDisposed() {
        savedCardsDetectorTearDownAwaiter.signalDisposed()
    }

    fun onSavedCardsDetectorResult(result: ClickToPaySavedCardsDetectorResult) {
        if (_deviceRecognition.value == DeviceRecognitionState.UsingDifferentEmail) return
        val recognition = deviceRecognitionForDetectorResult(result)
        if (recognition == DeviceRecognitionState.Recognized) {
            _recognizedCardLabels.value =
                RecognizedCardLabelList(
                    result.savedCards.map { ClickToPaySandboxCatalog.labelForMaskedCard(it) },
                )
        } else {
            _recognizedCardLabels.value = RecognizedCardLabelList()
        }
        _deviceRecognition.value = recognition
    }

    fun useDifferentEmail() {
        _deviceRecognition.value = DeviceRecognitionState.UsingDifferentEmail
        _recognizedCardLabels.value = RecognizedCardLabelList()
        _email.value = ""
        _emailError.value = null
        _phoneError.value = null
        updateMerchantPrefill {
            it.copy(phoneCountryCode = "", phoneNumber = "")
        }
        savedCardsDetectorController?.signOut()
    }

    private suspend fun tearDownSavedCardsDetectorAndAwait(): Boolean {
        val hadActiveDetector = _savedCardsDetectorKey.value >= 0
        val controller = savedCardsDetectorController
        savedCardsDetectorController = null
        if (hadActiveDetector) {
            _savedCardsDetectorKey.value = -1
        }
        return awaitSavedCardsDetectorTearDown(
            hadActiveDetector = hadActiveDetector,
            controllerAwaitTearDown = controller?.let { c -> suspend { c.awaitTearDown() } },
            awaiter = savedCardsDetectorTearDownAwaiter,
        )
    }

    private fun remountSavedCardsDetectorAfterCheckoutInterrupted() {
        remountJob?.cancel()
        remountJob =
            viewModelScope.launch {
                remountSavedCardsDetectorAfterCheckoutInterruptedSuspending()
            }
    }

    private suspend fun remountSavedCardsDetectorAfterCheckoutInterruptedSuspending() {
        if (
            !shouldRemountSavedCardsDetector(
                _deviceRecognition.value,
                remountedDetectorForCheckoutGeneration,
                checkoutSessionGeneration,
            )
        ) {
            return
        }
        awaitCheckoutInactiveUntilRemount(
            isCheckoutActive = { SpreedlyClickToPayCheckout.isActive },
            delay = { delay(it) },
        )
        if (
            !shouldRemountSavedCardsDetector(
                _deviceRecognition.value,
                remountedDetectorForCheckoutGeneration,
                checkoutSessionGeneration,
            )
        ) {
            return
        }
        remountedDetectorForCheckoutGeneration = checkoutSessionGeneration
        _deviceRecognition.value = DeviceRecognitionState.Checking
        _recognizedCardLabels.value = RecognizedCardLabelList()
        _savedCardsDetectorKey.value = (_savedCardsDetectorKey.value.coerceAtLeast(0)) + 1
    }

    private fun treatsDeviceAsRecognized(): Boolean =
        _deviceRecognition.value == DeviceRecognitionState.Recognized

    fun updateEmail(value: String) {
        if (treatsDeviceAsRecognized() && value != _email.value) {
            useDifferentEmail()
        }
        _email.value = value
        refreshCustomerIdentityValidity()
    }

    fun updateMerchantPrefill(transform: (ClickToPayMerchantPrefill) -> ClickToPayMerchantPrefill) {
        _merchantPrefill.value = transform(_merchantPrefill.value)
    }

    fun updateFirstName(value: String) {
        updateMerchantPrefill { it.copy(firstName = value) }
        if (_firstNameError.value != null) {
            validateBillingNames()
        }
    }

    fun updateLastName(value: String) {
        updateMerchantPrefill { it.copy(lastName = value) }
        if (_lastNameError.value != null) {
            validateBillingNames()
        }
    }

    fun updatePhoneCountryCode(value: String) {
        if (treatsDeviceAsRecognized()) {
            useDifferentEmail()
        }
        updateMerchantPrefill { it.copy(phoneCountryCode = value.filter { it.isDigit() }) }
        refreshCustomerIdentityValidity()
    }

    fun updatePhoneNumber(value: String) {
        if (treatsDeviceAsRecognized()) {
            useDifferentEmail()
        }
        updateMerchantPrefill { it.copy(phoneNumber = value.filter { it.isDigit() }) }
        refreshCustomerIdentityValidity()
    }

    fun updateAddressLine1(value: String) {
        updateMerchantPrefill { it.copy(addressLine1 = value) }
    }

    fun updateAddressLine2(value: String) {
        updateMerchantPrefill { it.copy(addressLine2 = value) }
    }

    fun updateCity(value: String) {
        updateMerchantPrefill { it.copy(city = value) }
    }

    fun updateState(value: String) {
        updateMerchantPrefill { it.copy(state = value) }
    }

    fun updateZip(value: String) {
        updateMerchantPrefill { it.copy(zip = value) }
    }

    fun updateCountry(value: String) {
        updateMerchantPrefill { it.copy(country = value) }
    }

    fun setCopyBillingToShipping(enabled: Boolean) {
        updateMerchantPrefill { it.copy(copyBillingToShipping = enabled) }
    }

    fun validateBillingNames(): Boolean {
        val first = _merchantPrefill.value.firstName.trim()
        val last = _merchantPrefill.value.lastName.trim()
        _firstNameError.value = if (first.isEmpty()) "First name is required" else null
        _lastNameError.value = if (last.isEmpty()) "Last name is required" else null
        return first.isNotEmpty() && last.isNotEmpty()
    }

    fun validateMerchantContactOnPay(): Boolean {
        val namesValid = validateBillingNames()
        val identityValid = validateCustomerIdentity()
        return identityValid && namesValid
    }

    fun clearEmailError() {
        _emailError.value = null
    }

    fun clearPhoneError() {
        _phoneError.value = null
    }

    fun validateCustomerIdentity(): Boolean {
        val recognizedDevice = treatsDeviceAsRecognized()
        val email = _email.value.trim()
        val prefill = _merchantPrefill.value
        val phone = prefill.phoneNumber.trim()
        val countryCode = prefill.phoneCountryCode.trim()

        if (recognizedDevice) {
            _phoneError.value = null
            _emailError.value =
                if (email.isNotEmpty() && !EmailValidator.isValid(email)) {
                    "Invalid email format"
                } else {
                    null
                }
            return _emailError.value == null
        }

        val hasValidEmail = email.isNotEmpty() && EmailValidator.isValid(email)
        val hasPhoneLookup = phone.isNotEmpty() && countryCode.isNotEmpty()
        val partialPhone = phone.isNotEmpty() xor countryCode.isNotEmpty()

        if (hasValidEmail || hasPhoneLookup) {
            _emailError.value = null
            _phoneError.value = null
            return true
        }

        _emailError.value =
            when {
                email.isNotEmpty() && !EmailValidator.isValid(email) -> "Invalid email format"
                else -> null
            }
        _phoneError.value =
            when {
                partialPhone -> "Country code and mobile number are both required for phone lookup"
                else -> "Email or phone with country code is required"
            }
        return false
    }

    private fun refreshCustomerIdentityValidity() {
        if (_emailError.value != null || _phoneError.value != null) {
            validateCustomerIdentity()
        }
    }

    fun canStartPayment(): Boolean =
        _stage.value == Stage.IDLE && _selectedProduct.value != null

    companion object {
        fun canStartPayment(
            stage: Stage,
            selectedProduct: Product?,
            deviceRecognition: DeviceRecognitionState,
        ): Boolean =
            stage == Stage.IDLE &&
                selectedProduct != null &&
                deviceRecognition != DeviceRecognitionState.Checking

        fun hasValidCustomerIdentity(
            deviceRecognition: DeviceRecognitionState,
            email: String,
            phoneNumber: String,
            phoneCountryCode: String,
        ): Boolean {
            val recognizedDevice = deviceRecognition == DeviceRecognitionState.Recognized
            if (recognizedDevice) {
                return email.isBlank() || EmailValidator.isValid(email)
            }
            return ClickToPayCustomer(
                email = email.trim().ifBlank { null },
                phoneNumber = phoneNumber.trim().ifBlank { null },
                countryCode = phoneCountryCode.trim().ifBlank { null },
            ).let { customer ->
                (email.isBlank() || EmailValidator.isValid(email)) && customer.isValidForLookup()
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private suspend fun initializeSdkIfNeeded(): Boolean {
        if (spreedlySdk.isInitialized) return true
        _isInitializing.value = true
        return try {
            sdkSessionManager.initializeSdk(
                sdk = spreedlySdk,
                context = context.applicationContext,
                environmentKey = BuildConfig.ENVIRONMENT_KEY,
            ).fold(
                onSuccess = { true },
                onFailure = {
                    _errorMessage.value = "Failed to get auth params"
                    false
                },
            )
        } finally {
            _isInitializing.value = false
        }
    }

    private fun appendEvent(message: String) {
        val updated = (_eventLog.value.lines + message).takeLast(6)
        _eventLog.value = ClickToPayEventLog(updated)
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

    fun applyThemeToSdk(isDarkMode: Boolean) {
        themeConfiguration.applyGlobalTheme(spreedlySdk, isDarkMode)
    }

    override fun onCleared() {
        super.onCleared()
        paymentResultJob?.cancel()
        eventsJob?.cancel()
        stateJob?.cancel()
        remountJob?.cancel()
    }
}

@Immutable
data class ClickToPayEventLog(val lines: List<String> = emptyList())

@Immutable
data class RecognizedCardLabelList(val items: List<String> = emptyList())
