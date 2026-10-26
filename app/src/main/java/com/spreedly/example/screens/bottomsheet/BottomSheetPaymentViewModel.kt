package com.spreedly.example.screens.bottomsheet

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.DemoSdkAuthSession
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.ThemeConfigurationController
import com.spreedly.example.optionalfields.DemoOptionalFieldActions
import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.example.validation.DemoCoreNameField
import com.spreedly.example.validation.DemoCoreNameFieldLayout
import com.spreedly.example.validation.DemoOptionalFieldValidators
import com.spreedly.sdk.Spreedly
import kotlinx.coroutines.Job
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
    val sdk = Spreedly()

    val snackbarHostState = SnackbarHostState()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    private var isConfigurationChanging = false

    private val authSession = DemoSdkAuthSession(SdkSessionManager(AuthService()))
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    val themeConfiguration = ThemeConfigurationController()
    val useCustomTheme = themeConfiguration.useCustomTheme
    val selectedThemePreset = themeConfiguration.selectedPreset

    private val _fieldSelection = MutableStateFlow(DemoOptionalFieldSelection.default())
    val fieldSelection: StateFlow<DemoOptionalFieldSelection> = _fieldSelection.asStateFlow()

    val availableCoreNameFields: DemoCoreNameFieldLayout = DemoCoreNameField.forBottomSheetScreen()

    private var paymentResultJob: Job? = null

    init {
        viewModelScope.launch {
            ensureFreshAuthBeforePayment()
        }
    }

    private fun registerPaymentObservers() {
        DemoOptionalFieldValidators.sync(sdk, _fieldSelection.value, availableCoreNameFields)
        observePaymentResults()
    }

    suspend fun ensureFreshAuthBeforePayment(): Boolean =
        authSession.ensureFreshAuth(
            sdk = sdk,
            context = context,
            environmentKey = BuildConfig.ENVIRONMENT_KEY,
            isInitializing = _isInitializing,
            registerObserversIfNeeded = ::registerPaymentObservers,
            onFailure = {
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Failed to fetch auth parameters")
                }
            },
        )

    private fun observePaymentResults() {
        paymentResultJob?.cancel()
        paymentResultJob =
            paymentResultHandler.observeResults(
                sdk = sdk,
                scope = viewModelScope,
                onCompleted = { result ->
                    Log.d(TAG, "BottomSheetPaymentViewModel: Payment completed")
                    _isProcessing.value = false
                    _token.value = result.token
                    authSession.onTerminalPaymentResult(result)

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
                    authSession.onTerminalPaymentResult(result)
                    if (!isConfigurationChanging) {
                        snackbarHostState.showSnackbar("Payment failed: ${result.message}")
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

                if (!ensureFreshAuthBeforePayment()) {
                    setProcessing(false)
                    if (!isConfigurationChanging) {
                        snackbarHostState.showSnackbar("Failed to initialize payment. Please try again.")
                    }
                    return@launch
                }

                delay(100)
                sdk.expressCheckout()

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

    fun onConfigurationChanging() {
        isConfigurationChanging = true
        if (sdk.isInitialized && sdk.showBottomSheet.value) {
            sdk.preservePaymentStateOnNextShow()
            Log.d(TAG, "BottomSheetPaymentViewModel: Preserving payment state for configuration change")
        }
    }

    fun onConfigurationChangeComplete() {
        isConfigurationChanging = false
        Log.d(TAG, "BottomSheetPaymentViewModel: Configuration change complete")
    }

    fun clearSnackbarState() {
        viewModelScope.launch {
            try {
                snackbarHostState.currentSnackbarData?.dismiss()
            } catch (e: Exception) {
                // Ignore any exceptions during cleanup
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

    fun resolvePaymentSheetConfig(isDarkMode: Boolean) =
        themeConfiguration.resolvePaymentSheetConfig(isDarkMode)

    fun applyThemeToSdk(isDarkMode: Boolean) {
        themeConfiguration.applyGlobalTheme(sdk, isDarkMode)
    }

    fun updateFieldSelection(newSelection: DemoOptionalFieldSelection) {
        val previous = _fieldSelection.value
        if (previous == newSelection) return
        if (sdk.isInitialized) {
            DemoOptionalFieldActions.clearDisabledFields(sdk, previous, newSelection)
            if (sdk.showBottomSheet.value) {
                sdk.hideBottomSheet()
            }
        }
        _fieldSelection.value = newSelection
        if (sdk.isInitialized) {
            DemoOptionalFieldValidators.sync(sdk, newSelection, availableCoreNameFields)
        }
    }

    override fun onCleared() {
        DemoOptionalFieldValidators.clear(sdk)
        super.onCleared()
    }

    private companion object {
        private const val TAG = "BottomSheetPaymentViewModel"
    }
}
