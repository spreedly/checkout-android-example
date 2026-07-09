package com.spreedly.example.screens.reusablebottomsheet

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
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.ThemeConfigurationController
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.BinMetadata
import com.spreedly.sdk.models.Metadata
import com.spreedly.sdk.models.PaymentMethodDetails
import com.spreedly.sdk.models.PaymentMethodResponse
import com.spreedly.sdk.models.Transaction
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.sdk.ui.PaymentSheetConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for ReusableBottomSheetPaymentScreen that reinitializes the SDK
 * each time the bottom sheet is opened, allowing for multiple payments.
 *
 * This example demonstrates how to handle multiple payment flows by fetching
 * fresh authentication parameters (nonce, signature, etc.) for each payment attempt.
 */
class ReusableBottomSheetPaymentViewModel(private val context: Context) : ViewModel() {
    // Spreedly SDK instance - survives configuration changes via ViewModel
    val sdk = Spreedly()

    // SnackbarHostState for showing messages
    val snackbarHostState = SnackbarHostState()

    // UI State
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    private val _paymentCount = MutableStateFlow(0)
    val paymentCount: StateFlow<Int> = _paymentCount.asStateFlow()

    private val _tokenHistory = MutableStateFlow<List<String>>(emptyList())
    val tokenHistory: StateFlow<List<String>> = _tokenHistory.asStateFlow()

    // Configuration change tracking
    private var isConfigurationChanging = false

    // Payment method repository for retention
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    val themeConfiguration = ThemeConfigurationController()
    val useCustomTheme = themeConfiguration.useCustomTheme
    val selectedThemePreset = themeConfiguration.selectedPreset

    // Track if we've initialized the SDK at least once
    private var hasInitialized = false

    /**
     * Initialize SDK with fresh auth parameters for each payment.
     */
    private suspend fun initializeSDKForPayment(): Boolean {
        _isInitializing.value = true
        return try {
            sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                .fold(
                    onSuccess = {
                        if (!hasInitialized) {
                            observePaymentResults()
                            hasInitialized = true
                            Log.d(TAG, "ReusableBottomSheetViewModel: First SDK initialization with observers")
                        } else {
                            Log.d(TAG, "ReusableBottomSheetViewModel: SDK reinitialized with fresh auth")
                        }
                        _isInitializing.value = false
                        true
                    },
                    onFailure = { e ->
                        Log.d(TAG, "ReusableBottomSheetViewModel: Failed to fetch auth parameters: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("Failed to fetch auth parameters")
                        _isInitializing.value = false
                        false
                    },
                )
        } catch (e: Exception) {
            Log.d(TAG, "ReusableBottomSheetViewModel: SDK initialization failed: ${e::class.simpleName}")
            snackbarHostState.showSnackbar("SDK initialization failed")
            _isInitializing.value = false
            false
        }
    }

    private fun observePaymentResults() {
        paymentResultHandler.observeResults(
            sdk = sdk,
            scope = viewModelScope,
            onCompleted = { result ->
                _isProcessing.value = false
                _token.value = result.token
                _paymentCount.value = _paymentCount.value + 1
                val currentHistory = _tokenHistory.value.toMutableList()
                currentHistory.add(result.token)
                _tokenHistory.value = currentHistory
                if (result.shouldRetain) {
                    handlePaymentRetention(result)
                } else if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment #${_paymentCount.value} completed successfully!")
                }
            },
            onFailed = { result ->
                Log.d(TAG, "ReusableBottomSheetViewModel: Payment failed: ${result.errorType}")
                _isProcessing.value = false
                if (!isConfigurationChanging) {
                    snackbarHostState.showSnackbar("Payment failed: ${result.message}")
                }
            },
            onCanceled = {
                Log.d(TAG, "ReusableBottomSheetViewModel: Payment canceled")
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

    fun clearTokenHistory() {
        _tokenHistory.value = emptyList()
    }

    /**
     * Start a new payment flow with fresh auth parameters.
     */
    fun startNewPayment() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ReusableBottomSheetViewModel: Starting new payment flow...")

                // Clear previous token and set processing
                clearToken()
                setProcessing(true)

                // Initialize SDK with fresh auth parameters
                val initSuccess = initializeSDKForPayment()

                if (initSuccess && sdk.isInitialized) {
                    Log.d(TAG, "ReusableBottomSheetViewModel: SDK initialized successfully")

                    // Small delay to ensure initialization is complete
                    delay(100)

                    // Open the bottom sheet
                    sdk.expressCheckout()

                    Log.d(TAG, "ReusableBottomSheetViewModel: Express checkout started")
                } else {
                    setProcessing(false)
                    snackbarHostState.showSnackbar("Failed to initialize payment. Please try again.")
                }
            } catch (e: Exception) {
                Log.d(TAG, "ReusableBottomSheetViewModel: Error during new payment: ${e::class.simpleName}")
                setProcessing(false)
                snackbarHostState.showSnackbar("Payment error")
            }
        }
    }

    /**
     * Handle payment retention after successful payment creation.
     * Calls the backend retain API.
     */
    private fun handlePaymentRetention(result: PaymentResult.Completed) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ReusableBottomSheetPaymentViewModel: Retaining payment method")
                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = {
                        Log.d(TAG, "ReusableBottomSheetPaymentViewModel: Payment method retained successfully")
                        if (!isConfigurationChanging) {
                            snackbarHostState
                                .showSnackbar("Payment #${_paymentCount.value} saved for future use!")
                        }
                    },
                    onFailure = { e ->
                        Log.d(TAG, "ReusableBottomSheetPaymentViewModel: Error retaining payment method: ${e::class.simpleName}")
                        if (!isConfigurationChanging) {
                            snackbarHostState.showSnackbar("Payment created but failed to save")
                        }
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG, "ReusableBottomSheetPaymentViewModel: Exception during retention: ${e::class.simpleName}")
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
            Log.d(TAG, "ReusableBottomSheetViewModel: Preserving payment state for configuration change")
        }
    }

    /**
     * Call this method after configuration change is complete.
     */
    fun onConfigurationChangeComplete() {
        isConfigurationChanging = false
        Log.d(TAG, "ReusableBottomSheetViewModel: Configuration change complete")
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
     * Reset payment counter for demo purposes
     */
    fun resetPaymentCounter() {
        _paymentCount.value = 0
        clearToken()
        clearTokenHistory()
    }

    /**
     * Force reset all state - use this if the UI gets stuck
     */
    fun forceReset() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "ReusableBottomSheetViewModel: Force resetting all state")

                // Reset all our state
                _isProcessing.value = false
                _isInitializing.value = false
                clearToken()

                // Reset SDK state if initialized
                if (sdk.isInitialized) {
                    sdk.resetPaymentState()
                    // Try to hide bottom sheet if it's stuck open
                    if (sdk.showBottomSheet.value) {
                        sdk.hideBottomSheet()
                    }
                }

                snackbarHostState.showSnackbar("State reset successfully")
                Log.d(TAG, "ReusableBottomSheetViewModel: Force reset completed")
            } catch (e: Exception) {
                Log.d(TAG, "ReusableBottomSheetViewModel: Error during force reset: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Reset failed")
            }
        }
    }

    /**
     * Manually cancel any ongoing payment processing.
     * This can be called when the user explicitly cancels or if we need to reset state.
     */
    fun cancelPayment() {
        Log.d(TAG, "ReusableBottomSheetViewModel: Manually canceling payment")
        setProcessing(false)
        if (sdk.isInitialized && sdk.showBottomSheet.value) {
            // Try to hide the bottom sheet if it's still shown
            try {
                sdk.hideBottomSheet()
            } catch (e: Exception) {
                Log.d(TAG, "ReusableBottomSheetViewModel: Error hiding bottom sheet: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Get current SDK state for debugging purposes
     */
    fun getSDKState(): String = "SDK Initialized: ${sdk.isInitialized}, " +
            "Bottom Sheet Shown: ${sdk.showBottomSheet.value}, " +
            "Processing: ${_isProcessing.value}, " +
            "Initializing: ${_isInitializing.value}"

    /**
     * Test method to manually trigger payment success (for debugging)
     */
    fun testManualPaymentSuccess() {
        Log.d(TAG, "=== TESTING MANUAL PAYMENT SUCCESS ===")
        if (sdk.isInitialized) {
            // Create mock response for testing
            val testToken = "test_token_${System.currentTimeMillis()}"
            val mockResponse = createMockPaymentMethodResponse(testToken)
            sdk.notifyPaymentSuccess(mockResponse)
            Log.d(TAG, "Manual payment success notification sent")
        } else {
            Log.d(TAG, "SDK not initialized - cannot send manual payment success")
        }
    }

    /**
     * Helper method to create a mock PaymentMethodResponse for testing
     */
    private fun createMockPaymentMethodResponse(token: String): PaymentMethodResponse {
        val paymentMethodDetails = PaymentMethodDetails(
            token = token,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            email = "test@example.com",
            data = null,
            storageState = "cached",
            test = true,
            metadata = Metadata(emptyMap()),
            callbackUrl = null,
            lastFourDigits = "4242",
            firstSixDigits = "424242",
            cardType = "visa",
            firstName = "Test",
            lastName = "User",
            month = 12,
            year = 2025,
            address1 = "123 Test St",
            address2 = "",
            city = "Test City",
            state = "CA",
            zip = "12345",
            country = "US",
            phoneNumber = "",
            company = "",
            fullName = "Test User",
            eligibleForCardUpdater = false,
            shippingAddress1 = "",
            shippingAddress2 = "",
            shippingCity = "",
            shippingState = "",
            shippingZip = "",
            shippingCountry = "",
            shippingPhoneNumber = "",
            issuerIdentificationNumber = "424242",
            clickToPay = false,
            managed = false,
            binMetadata = BinMetadata(
                message = null,
                cardBrand = "VISA",
                issuingBank = "Test Bank",
                cardType = "CREDIT",
                cardCategory = "CLASSIC",
                issuingCountryIsoNumber = "840",
                issuingBankWebsite = null,
                issuingBankPhoneNumber = null,
                maxPanLength = 16,
                binType = "PERSONAL",
                regulated = "N",
                issuingCountryIsoA2Code = "US",
                issuingCountryIsoA3Code = "USA",
                issuingCountryIsoName = "United States",
            ),
            subscribedToMastercardAbu = false,
            paymentMethodType = "credit_card",
            errors = emptyList(),
            fingerprint = "test_fingerprint",
            verificationValue = "",
            number = "XXXX-XXXX-XXXX-4242",
        )

        val transaction = Transaction(
            token = "txn_$token",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            succeeded = true,
            transactionType = "StorePaymentMethod",
            retained = true,
            state = "succeeded",
            messageKey = "messages.transaction_succeeded",
            message = "Succeeded!",
            paymentMethod = paymentMethodDetails,
        )

        return PaymentMethodResponse(transaction = transaction)
    }

    /**
     * Test method to manually close bottom sheet (for debugging)
     */
    fun testManualCloseBottomSheet() {
        Log.d(TAG, "=== TESTING MANUAL BOTTOM SHEET CLOSE ===")
        if (sdk.isInitialized && sdk.showBottomSheet.value) {
            sdk.hideBottomSheet()
            Log.d(TAG, "Manual bottom sheet close called")
        } else {
            Log.d(TAG, "Bottom sheet not open or SDK not initialized")
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

    fun resolvePaymentSheetConfig(
        isDarkMode: Boolean,
        fallbackConfig: PaymentSheetConfig,
    ): PaymentSheetConfig {
        val themedConfig = themeConfiguration.resolvePaymentSheetConfig(isDarkMode)
        return themedConfig ?: fallbackConfig
    }

    fun applyThemeToSdk(isDarkMode: Boolean) {
        themeConfiguration.applyGlobalTheme(sdk, isDarkMode)
    }

    private companion object {
        private const val TAG = "ReusableBottomSheetPaymentViewModel"
    }
}
