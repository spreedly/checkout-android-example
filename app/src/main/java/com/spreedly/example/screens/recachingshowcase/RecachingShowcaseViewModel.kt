package com.spreedly.example.screens.recachingshowcase

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.models.SavedPaymentMethod
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.PaymentResultHandler
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.SpreedlyErrorMessages
import com.spreedly.result.Result
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.RecacheConfig
import com.spreedly.sdk.models.SavedCardInfo
import com.spreedly.sdk.models.ScreenPresentationMode
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for RecachingShowcaseScreen managing recaching customization and payment state.
 */
class RecachingShowcaseViewModel(private val context: Context) : ViewModel() {
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

    // Track when payment completed to prevent race conditions
    private var lastPaymentCompletedTime = 0L

    // Saved payment methods
    private val paymentMethodRepository = PaymentMethodRepository(context)
    private val sdkSessionManager = SdkSessionManager(AuthService())
    private val paymentResultHandler = PaymentResultHandler(paymentMethodRepository)
    private val _savedPaymentMethods = MutableStateFlow<List<SavedPaymentMethod>>(emptyList())
    val savedPaymentMethods: StateFlow<List<SavedPaymentMethod>> = _savedPaymentMethods.asStateFlow()
    private var paymentResultJob: Job? = null
    private var initJob: Job? = null

    // Recaching customization state
    private val _presentationMode = MutableStateFlow(ScreenPresentationMode.bottomSheet)
    val presentationMode: StateFlow<ScreenPresentationMode> = _presentationMode.asStateFlow()

    private val _customThemeEnabled = MutableStateFlow(false)
    val customThemeEnabled: StateFlow<Boolean> = _customThemeEnabled.asStateFlow()

    private val _selectedThemePreset = MutableStateFlow(ThemePreset.DEFAULT)
    val selectedThemePreset: StateFlow<ThemePreset> = _selectedThemePreset.asStateFlow()

    // Initialize SDK on ViewModel creation
    init {
        initializeSDK()
        // Load saved payment methods without initializing mock data
        viewModelScope.launch {
            try {
                val methods = paymentMethodRepository.getSavedPaymentMethods()
                _savedPaymentMethods.value = methods
                Log.d(TAG,"RecachingShowcaseViewModel: Loaded ${methods.size} saved payment methods from cache")
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Error loading saved payment methods: ${e::class.simpleName}")
            }
        }
        fetchPaymentMethodsFromBackend()
    }

    private fun initializeSDK() {
        initJob = viewModelScope.launch {
            _isInitializing.value = true
            try {
                sdkSessionManager.initializeSdk(sdk, context.applicationContext, BuildConfig.ENVIRONMENT_KEY)
                    .fold(
                        onSuccess = {
                            Log.d(TAG,"RecachingShowcaseViewModel: SDK initialized successfully")
                            observePaymentResults()
                        },
                        onFailure = { e ->
                            Log.d(TAG,"RecachingShowcaseViewModel: Failed to fetch auth parameters: ${e::class.simpleName}")
                            snackbarHostState.showSnackbar("Failed to fetch auth parameters")
                        },
                    )
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: SDK initialization failed: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("SDK initialization failed")
            }
            _isInitializing.value = false
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
                    snackbarHostState.showSnackbar("Payment method created successfully!")
                }
            },
            onFailed = {
                Log.d(TAG, "RecachingShowcaseViewModel: Payment failed: ${it.errorType}")
                _isProcessing.value = false
                _paymentToken.value = ""
                snackbarHostState.showSnackbar("Error creating payment method")
            },
            onCanceled = {
                Log.d(TAG,"RecachingShowcaseViewModel: Payment canceled")
                _isProcessing.value = false
                // Don't show snackbar - user cancellation is a normal action, not an error
            },
        )
    }

    fun setProcessing(processing: Boolean) {
        Log.d(TAG,"RecachingShowcaseViewModel: setProcessing called with: $processing")

        if (processing) {
            // Check if payment completed recently (within last 1000ms) - race condition protection
            val timeSinceCompletion = System.currentTimeMillis() - lastPaymentCompletedTime
            if (timeSinceCompletion < 1000 && _paymentToken.value.isNotEmpty()) {
                Log.d(TAG,
                    "RecachingShowcaseViewModel: Ignoring setProcessing(true) - " +
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

    // Backup timeout mechanism in case payment result flow fails
    fun startPaymentPolling() {
        Log.d(TAG,"RecachingShowcaseViewModel: Starting payment timeout protection")
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000) // 30-second timeout
            if (_isProcessing.value) {
                Log.d(TAG,"RecachingShowcaseViewModel: Payment timeout reached - resetting state")
                _isProcessing.value = false
                snackbarHostState.showSnackbar("Payment processing timeout")
            }
        }
    }

    /**
     * Load saved payment methods from the repository.
     */
    private fun loadSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                // Load saved payment methods without mock data
                val methods = paymentMethodRepository.getSavedPaymentMethods()
                _savedPaymentMethods.value = methods
                Log.d(TAG,"RecachingShowcaseViewModel: Loaded ${methods.size} saved payment methods")
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Error loading saved payment methods: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error loading saved payment methods")
            }
        }
    }

    /**
     * Fetch payment methods from backend and update local cache.
     */
    private fun fetchPaymentMethodsFromBackend() {
        viewModelScope.launch {
            try {
                paymentMethodRepository.fetchAndSyncPaymentMethods().fold(
                    onSuccess = { methods ->
                        _savedPaymentMethods.value = methods
                        Log.d(TAG,"RecachingShowcaseViewModel: Fetched ${methods.size} payment methods from backend")
                    },
                    onFailure = { e ->
                        Log.d(TAG,"RecachingShowcaseViewModel: Error fetching payment methods: ${e::class.simpleName}")
                        // Keep using cached data
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Exception fetching payment methods: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Handle payment retention after successful payment creation.
     * Calls the backend retain API and refreshes the payment methods list.
     */
    private fun handlePaymentRetention(result: PaymentResult.Completed) {
        viewModelScope.launch {
            try {
                Log.d(TAG,"RecachingShowcaseViewModel: Retaining payment method")
                paymentResultHandler.retainIfNeeded(result).fold(
                    onSuccess = {
                        Log.d(TAG,"RecachingShowcaseViewModel: Payment method retained successfully")
                        fetchPaymentMethodsFromBackend()
                        snackbarHostState.showSnackbar("Payment method created and saved!")
                    },
                    onFailure = { e ->
                        Log.d(TAG,"RecachingShowcaseViewModel: Error retaining payment method: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("Payment created but failed to save")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Exception during retention: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Update the presentation mode for recaching UI.
     */
    fun updatePresentationMode(mode: ScreenPresentationMode) {
        _presentationMode.value = mode
        Log.d(TAG,"RecachingShowcaseViewModel: Presentation mode updated to $mode")
    }

    /**
     * Toggle custom theme enabled/disabled.
     */
    fun toggleCustomTheme() {
        _customThemeEnabled.value = !_customThemeEnabled.value
        Log.d(TAG,"RecachingShowcaseViewModel: Custom theme enabled: ${_customThemeEnabled.value}")
    }

    /**
     * Update the selected theme preset.
     */
    fun updateThemePreset(preset: ThemePreset) {
        _selectedThemePreset.value = preset
        Log.d(TAG,"RecachingShowcaseViewModel: Theme preset updated to $preset")
    }

    /**
     * Build the SpreedlyTheme based on the selected preset.
     * Used to pass theme to SpreedlyRecacheUI at the view level.
     */
    fun buildSpreedlyTheme(): com.spreedly.ui.theme.SpreedlyTheme? {
        if (!_customThemeEnabled.value) return null

        return when (_selectedThemePreset.value) {
            ThemePreset.DEFAULT -> null
            ThemePreset.BLUE -> com.spreedly.ui.theme.SpreedlyTheme(
                colors = com.spreedly.ui.theme.SpreedlyColors(
                    text = Color(0xFF1976D2),
                    textSecondary = Color(0xFF424242),
                    surface = Color(0xFFE3F2FD),
                    primary = Color(0xFF1976D2),
                    error = Color(0xFFD32F2F),
                    background = Color.White,
                    border = Color(0xFFBDBDBD),
                ),
            )
            ThemePreset.GREEN -> com.spreedly.ui.theme.SpreedlyTheme(
                colors = com.spreedly.ui.theme.SpreedlyColors(
                    text = Color(0xFF388E3C),
                    textSecondary = Color(0xFF424242),
                    surface = Color(0xFFE8F5E9),
                    primary = Color(0xFF388E3C),
                    error = Color(0xFFD32F2F),
                    background = Color.White,
                    border = Color(0xFFBDBDBD),
                ),
            )
            ThemePreset.PURPLE -> com.spreedly.ui.theme.SpreedlyTheme(
                colors = com.spreedly.ui.theme.SpreedlyColors(
                    text = Color(0xFF7B1FA2),
                    textSecondary = Color(0xFF424242),
                    surface = Color(0xFFF3E5F5),
                    primary = Color(0xFF7B1FA2),
                    error = Color(0xFFD32F2F),
                    background = Color.White,
                    border = Color(0xFFBDBDBD),
                ),
            )
            ThemePreset.DARK -> com.spreedly.ui.theme.SpreedlyTheme(
                colors = com.spreedly.ui.theme.SpreedlyColors(
                    text = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFBDBDBD),
                    surface = Color(0xFF424242),
                    primary = Color(0xFF212121),
                    error = Color(0xFFEF5350),
                    background = Color(0xFF303030),
                    border = Color(0xFF616161),
                ),
            )
        }
    }

    /**
     * Recache a saved payment method by showing the SDK's CVV input UI with custom configuration.
     *
     * @param savedCard The saved payment method to recache
     */
    fun recacheSavedPaymentMethod(savedCard: SavedPaymentMethod) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true

                // Create recaching configuration with customizations
                // Note: Theme is now passed to SpreedlyRecacheUI at the view level
                val config = RecacheConfig(
                    recachePresentationMode = _presentationMode.value,
                    cardInfo = SavedCardInfo(
                        lastFourDigits = savedCard.lastFourDigits,
                        cardType = savedCard.cardType,
                        cardholderName = savedCard.cardholderName,
                    ),
                    labelText = "CVV",
                    placeholderText = "123",
                    buttonText = "Confirm",
                    cancelButtonText = "Cancel",
                )

                Log.d(TAG, "RecachingShowcaseViewModel: Recaching with presentationMode=${config.recachePresentationMode}")

                // Call SDK's recaching method
                val result = sdk.recachePaymentMethod(
                    paymentMethodToken = savedCard.token,
                    config = config,
                )

                _isProcessing.value = false

                when (result) {
                    is Result.Success -> {
                        val response = result.data
                        if (response.transaction.succeeded) {
                            _paymentToken.value = response.transaction.paymentMethod.token
                            Log.d(TAG,
                                "RecachingShowcaseViewModel: Recached " +
                                    "token: ${response.transaction.paymentMethod.token.take(8)}...",
                            )

                            // Call retain API after successful recache to refresh retention
                            retainAfterRecache(response.transaction.paymentMethod.token)
                        } else {
                            snackbarHostState.showSnackbar("Recaching failed: ${response.transaction.message}")
                        }
                    }
                    is Result.Error -> {
                        // Don't show error message if user cancelled
                        if (result.error != com.spreedly.sdk.SpreedlyNetworkError.USER_CANCELLED) {
                            val errorMessage = SpreedlyErrorMessages.getUserFriendlyMessage(
                                error = result.error,
                                defaultMessage = "Failed to update payment method. Please try again.",
                            )
                            snackbarHostState.showSnackbar(errorMessage)
                            Log.d(TAG,"RecachingShowcaseViewModel: Recaching error: ${result.error.safeDescription()}")
                        } else {
                            Log.d(TAG,"RecachingShowcaseViewModel: Recaching cancelled by user")
                        }
                    }
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                Log.d(TAG,"RecachingShowcaseViewModel: Exception during recaching: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Recaching error")
            }
        }
    }

    /**
     * Call retain API after successful recache to refresh retention period.
     */
    private fun retainAfterRecache(token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG,"RecachingShowcaseViewModel: Retaining after recache")
                paymentMethodRepository.retainPaymentMethod(token).fold(
                    onSuccess = {
                        Log.d(TAG,"RecachingShowcaseViewModel: Successfully retained after recache")
                        snackbarHostState.showSnackbar("Payment method recached and retained!")
                    },
                    onFailure = { e ->
                        Log.d(TAG,"RecachingShowcaseViewModel: Error retaining after recache: ${e::class.simpleName}")
                        snackbarHostState.showSnackbar("Recached successfully!")
                    },
                )
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Exception retaining after recache: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Delete a saved payment method.
     *
     * @param token The token of the payment method to delete
     */
    fun deleteSavedPaymentMethod(token: String) {
        viewModelScope.launch {
            try {
                paymentMethodRepository.deletePaymentMethod(token)
                loadSavedPaymentMethods()
                snackbarHostState.showSnackbar("Payment method deleted")
            } catch (e: Exception) {
                Log.d(TAG,"RecachingShowcaseViewModel: Error deleting payment method: ${e::class.simpleName}")
                snackbarHostState.showSnackbar("Error deleting payment method")
            }
        }
    }

    private companion object {
        private const val TAG = "RecachingShowcaseViewModel"
    }
}

/**
 * Theme presets for recaching UI customization.
 */
enum class ThemePreset {
    DEFAULT,
    BLUE,
    GREEN,
    PURPLE,
    DARK,
}
