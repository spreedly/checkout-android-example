package com.spreedly.example.screens.threedschallenge

import android.annotation.SuppressLint
import android.util.Log
import android.app.Activity
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreedly.app.BuildConfig
import com.spreedly.example.AuthService
import com.spreedly.example.api.PurchaseAPIClient
import com.spreedly.example.api.PurchaseException
import com.spreedly.example.api.PurchaseTransaction
import com.spreedly.example.models.DemoProducts
import com.spreedly.example.models.Product
import com.spreedly.example.models.SavedCard
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.ThreeDSChallengeResult
import com.spreedly.threeds.SpreedlyThreeDSProvider
import com.spreedly.threeds.gatewayspecific.GatewaySpecific3DSIntegration
import kotlinx.coroutines.launch

/**
 * ViewModel for 3DS Payment Flow Demo
 *
 * Matches iOS implementation: ThreeDSPaymentFlowView.swift
 *
 * Flow:
 * 1. Load payment methods from API
 * 2. User selects product and payment method
 * 3. Call purchase API with payment method token
 * 4. If 3DS required, show challenge
 * 5. Handle challenge result (SDK calls backend APIs internally)
 * 6. Show success/failure message
 */
class ThreeDSExampleViewModel(
    private val context: Context,
    private val activity: Activity,
) : ViewModel() {
    // Spreedly SDK instance
    val sdk = Spreedly()

    // SnackbarHostState for messages
    val snackbarHostState = SnackbarHostState()

    // Payment method repository
    private val paymentMethodRepository = PaymentMethodRepository(context)

    // Shared helpers
    private val sdkSessionManager = SdkSessionManager(AuthService())

    // Products (mocked - same as iOS)
    @SuppressLint("ComposeUnstableCollections")
    val products = DemoProducts.products

    // Selected product
    private val _selectedProduct = mutableStateOf<Product?>(null)
    val selectedProduct: State<Product?> = _selectedProduct

    // Payment methods loading
    private val _isLoadingCards = mutableStateOf(false)
    val isLoadingCards: State<Boolean> = _isLoadingCards

    // Saved payment methods
    private val _savedCards = mutableStateOf<List<SavedCard>>(emptyList())

    @SuppressLint("ComposeUnstableCollections")
    val savedCards: State<List<SavedCard>> = _savedCards

    // Selected payment method
    private val _selectedCard = mutableStateOf<SavedCard?>(null)
    val selectedCard: State<SavedCard?> = _selectedCard

    // Processing state (purchase/3DS)
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // Success message
    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    // Error message
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // 3DS Challenge token
    private val transactionToken = mutableStateOf<String?>(null)

    // Challenge result
    private val _challengeResult = mutableStateOf<ThreeDSChallengeResult?>(null)
    val challengeResult: State<ThreeDSChallengeResult?> = _challengeResult

    // SDK initialization state
    private val _isInitializing = mutableStateOf(true)
    val isInitializing: State<Boolean> = _isInitializing

    // Computed property - is pay button enabled?
    val isPayButtonEnabled: Boolean
        get() = selectedProduct.value != null && selectedCard.value != null && !isLoading.value

    init {
        // Initialize SDK
        initializeSdk()

        // Subscribe to 3DS challenge results (BEFORE presenting challenge)
        setupSubscriptions()

        // Fetch payment methods
        fetchPaymentMethods()
    }

    /**
     * Initialize the Spreedly SDK with authentication parameters.
     */
    private fun initializeSdk() {
        viewModelScope.launch {
            _isInitializing.value = true
            sdkSessionManager.initializeSdk(
                sdk = sdk,
                context = context.applicationContext,
                environmentKey = BuildConfig.ENVIRONMENT_KEY,
                forterSiteId = BuildConfig.FORTER_SITE_ID.takeIf { it.isNotEmpty() },
            ).fold(
                onSuccess = {
                    Log.d(TAG, "ThreeDSPaymentFlow: ForterSiteId configured: ${BuildConfig.FORTER_SITE_ID.isNotEmpty()}")
                    Log.d(TAG, "ThreeDSPaymentFlow: SDK initialized successfully")
                },
                onFailure = { e ->
                    Log.d(TAG, "ThreeDSPaymentFlow: Failed to fetch auth parameters: ${e::class.simpleName}")
                    _errorMessage.value = "Failed to initialize SDK"
                },
            )
            _isInitializing.value = false
        }
    }

    /**
     * Setup subscriptions for 3DS challenge results.
     * Matches iOS: setupSubscriptions()
     *
     * NOTE: We do NOT subscribe to gatewaySpecific3DSChallengeReadyFlow because
     * the unified ThreeDSChallengeSheet component handles it internally.
     * This matches iOS SDK's DoChallengeIfNeeded behavior.
     */
    private fun setupSubscriptions() {
        // NO subscription to gatewaySpecific3DSChallengeReadyFlow needed!
        // The ThreeDSChallengeSheet component handles challenge presentation internally

        // Subscribe to Gateway-Specific 3DS Trigger Completion
        // NOTE: Only emitted for device_fingerprint flows (3003, 3004), NOT for direct challenge (3005)
        //
        // OPTION 1: Explicit Finalization (iOS-style) - Recommended
        // Call backend /complete endpoint and pass response to SDK
        viewModelScope.launch {
            sdk.gatewaySpecific3DSTriggerCompletionFlow.collect { event ->
                Log.d(TAG,
                    "ThreeDSPaymentFlow: [Gateway-Specific] Trigger completion - " +
                    "calling backend /complete endpoint",
                )

                try {
                    val purchaseClient = PurchaseAPIClient(
                        config = PurchaseAPIClient.ServerConfig(
                            baseURL = "https://checkout-web-sample-app-049a3c617015.herokuapp.com",
                        ),
                    )

                    // Call backend /complete endpoint
                    val completeResponse = purchaseClient.complete(event.token)
                    purchaseClient.close()

                    Log.d(TAG, "ThreeDSPaymentFlow: Backend /complete call successful")

                    // OPTION 1: Explicit finalization (iOS-style) - Pass response to SDK ✅ RECOMMENDED
                    // This gives merchant explicit control over the flow and matches iOS SDK
                    completeResponse.transaction?.let { transaction ->
                        Log.d(TAG, "ThreeDSPaymentFlow: Using explicit finalization (iOS-style)")
                        Log.d(TAG,
                            "ThreeDSPaymentFlow: Transaction state: ${transaction.state}, " +
                            "Required action: ${transaction.requiredAction}",
                        )

                        GatewaySpecific3DSIntegration.finalizeTransaction(
                            transactionToken = event.token,
                            transactionStatus = transaction,
                        )
                    } ?: run {
                        Log.d(TAG,
                            "ThreeDSPaymentFlow: ⚠️ No transaction in response, SDK will " +
                            "fall back to automatic polling",
                        )
                        // SDK will continue with automatic polling (fallback)
                    }

                    // OPTION 2: Automatic polling (fallback)
                    // If you prefer, you can omit the finalizeTransaction() call above
                    // and the SDK will automatically poll for status updates.
                    // To use this approach, simply call complete() and don't call finalizeTransaction().
                } catch (e: Exception) {
                    Log.d(TAG, "ThreeDSPaymentFlow: Complete API failed: ${e::class.simpleName}")
                    _errorMessage.value = "Failed to complete 3DS flow"
                    // SDK will fall back to automatic polling on error
                }
            }
        }

        // Subscribe to final challenge results (both Forter and Gateway-Specific)
        viewModelScope.launch {
            sdk.threeDSChallengeResultFlow.collect { result ->
                _challengeResult.value = result
                _isLoading.value = false

                when (result) {
                    is ThreeDSChallengeResult.Success -> {
                        // 3DS Challenge completed successfully
                        Log.d(TAG, "ThreeDSPaymentFlow: 3DS challenge completed successfully")
                        _errorMessage.value = null

                        sdk.hideThreeDSChallenge()

                        _successMessage.value = "Your payment has been securely authenticated and processed."
                    }

                    is ThreeDSChallengeResult.Failed -> {
                        // 3DS Challenge failed
                        // Check for specific error codes and show human-readable messages
                        val errorMsg = when {
                            result.message?.contains("messages.failed_sca_authentication") == true ||
                                result.message?.contains("Forter3DS") == true -> {
                                "Transaction failed due to failed authentication. Please try again."
                            }
                            result.message != null -> {
                                "Payment failed: ${result.message}"
                            }
                            else -> {
                                "Payment failed"
                            }
                        }
                        _errorMessage.value = errorMsg
                        Log.d(TAG, "ThreeDSPaymentFlow: 3DS challenge failed: $errorMsg")

                        // Hide challenge views
                        sdk.hideThreeDSChallenge()
                    }

                    is ThreeDSChallengeResult.Canceled -> {
                        // User canceled 3DS challenge
                        _errorMessage.value = "Payment canceled by user"
                        Log.d(TAG, "ThreeDSPaymentFlow: 3DS challenge canceled by user")

                        // Hide challenge views
                        sdk.hideThreeDSChallenge()
                    }

                    is ThreeDSChallengeResult.Initial -> {
                        // Initial state - no action needed
                    }
                }
            }
        }
    }

    /**
     * Fetch payment methods from API.
     * Matches iOS: fetchPaymentMethods()
     */
    private fun fetchPaymentMethods() {
        viewModelScope.launch {
            _isLoadingCards.value = true
            _errorMessage.value = null

            try {
                // Fetch and sync payment methods from backend
                paymentMethodRepository.fetchAndSyncPaymentMethods().fold(
                    onSuccess = { savedMethods ->
                        if (savedMethods.isEmpty()) {
                            // No saved methods - initialize with mock data for testing
                            paymentMethodRepository.initializeMockDataIfEmpty()
                            val mockMethods = paymentMethodRepository.getSavedPaymentMethods()

                            if (mockMethods.isEmpty()) {
                                _errorMessage.value = "No saved payment methods. Please add cards first."
                                _savedCards.value = emptyList()
                            } else {
                                // Convert mock methods to SavedCard models (take first 6 like iOS)
                                val cards = mockMethods.take(6).map { method ->
                                    SavedCard(
                                        id = method.token,
                                        paymentMethodToken = method.token ?: "",
                                        lastFourDigits = method.lastFourDigits ?: "****",
                                        cardType = method.cardType?.replaceFirstChar { it.uppercase() } ?: "Card",
                                        cardBrand = method.cardType?.lowercase() ?: "unknown",
                                        expiryMonth = method.expiryMonth?.toString()?.padStart(2, '0'),
                                        expiryYear = method.expiryYear?.toString(),
                                    )
                                }
                                _savedCards.value = cards
                                Log.d(TAG, "ThreeDSPaymentFlow: Loaded ${cards.size} mock payment methods")
                            }
                        } else {
                            // Convert to SavedCard models (take first 6 like iOS)
                            val cards = savedMethods.take(6).map { method ->
                                SavedCard(
                                    id = method.token ?: "",
                                    paymentMethodToken = method.token ?: "",
                                    lastFourDigits = method.lastFourDigits ?: "****",
                                    cardType = method.cardType?.replaceFirstChar { it.uppercase() } ?: "Card",
                                    cardBrand = method.cardType?.lowercase() ?: "unknown",
                                    expiryMonth = method.expiryMonth?.toString()?.padStart(2, '0'),
                                    expiryYear = method.expiryYear?.toString(),
                                )
                            }
                            _savedCards.value = cards
                            Log.d(TAG, "ThreeDSPaymentFlow: Loaded ${cards.size} payment methods from API")
                        }
                    },
                    onFailure = { error ->
                        // Try to use cached data
                        val cachedMethods = paymentMethodRepository.getSavedPaymentMethods()

                        if (cachedMethods.isEmpty()) {
                            // No cached data - initialize with mock data for testing
                            paymentMethodRepository.initializeMockDataIfEmpty()
                            val mockMethods = paymentMethodRepository.getSavedPaymentMethods()

                            if (mockMethods.isNotEmpty()) {
                                val cards = mockMethods.take(6).map { method ->
                                    SavedCard(
                                        id = method.token ?: "",
                                        paymentMethodToken = method.token ?: "",
                                        lastFourDigits = method.lastFourDigits ?: "****",
                                        cardType = method.cardType?.replaceFirstChar { it.uppercase() } ?: "Card",
                                        cardBrand = method.cardType?.lowercase() ?: "unknown",
                                        expiryMonth = method.expiryMonth?.toString()?.padStart(2, '0'),
                                        expiryYear = method.expiryYear?.toString(),
                                    )
                                }
                                _savedCards.value = cards
                                Log.d(TAG, "ThreeDSPaymentFlow: Using ${cards.size} mock payment methods (API error)")
                            } else {
                                _errorMessage.value = "Failed to load payment methods"
                                _savedCards.value = emptyList()
                            }
                        } else {
                            // Use cached data
                            val cards = cachedMethods.take(6).map { method ->
                                SavedCard(
                                    id = method.token ?: "",
                                    paymentMethodToken = method.token ?: "",
                                    lastFourDigits = method.lastFourDigits ?: "****",
                                    cardType = method.cardType?.replaceFirstChar { it.uppercase() } ?: "Card",
                                    cardBrand = method.cardType?.lowercase() ?: "unknown",
                                    expiryMonth = method.expiryMonth?.toString()?.padStart(2, '0'),
                                    expiryYear = method.expiryYear?.toString(),
                                )
                            }
                            _savedCards.value = cards
                            Log.d(TAG, "ThreeDSPaymentFlow: Using ${cards.size} cached payment methods (API error)")
                        }
                    },
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load payment methods"
                Log.d(TAG, "ThreeDSPaymentFlow: Failed to fetch payment methods: ${e::class.simpleName}")
            }

            _isLoadingCards.value = false
        }
    }

    /**
     * Handle pay button tap.
     * Matches iOS: handlePayButtonTap()
     */
    fun handlePayButtonTap() {
        val product = selectedProduct.value ?: return
        val card = selectedCard.value ?: return

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                // Use the API amount (Spreedly test gateway amounts)
                // UI displays: ${product.formattedPrice()} (e.g., $3,001.00)
                // API sends: product.apiAmount (e.g., 3001 cents = $30.01 for test gateway)
                val amountInCents = product.apiAmount

                Log.d(TAG, "ThreeDSPaymentFlow: Calling purchase API")
                Log.d(TAG, "ThreeDSPaymentFlow: UI Display: ${product.formattedPrice()}")
                Log.d(TAG, "ThreeDSPaymentFlow: API Amount: $amountInCents cents")
                Log.d(TAG, "ThreeDSPaymentFlow: Payment method token received")

                // Create purchase API client (calling Spreedly Core API directly like iOS)
                val purchaseClient = PurchaseAPIClient(
                    config = PurchaseAPIClient.ServerConfig(
                        baseURL = "https://checkout-web-sample-app-049a3c617015.herokuapp.com",
                    ),
                )

                try {
                    // Call purchase API - Gateway-specific includes attempt_3dsecure
                    val response = purchaseClient.purchase(
                        paymentMethodToken = card.paymentMethodToken,
                        amount = amountInCents,
                        currencyCode = "USD",
                        redirectUrl = SpreedlyThreeDSProvider.redirectUrl(context, "threeds/complete"),
                        attempt3dsecure = true,
                    )

                    // Check for errors in response
                    if (response.errors?.isNotEmpty() == true) {
                        _isLoading.value = false
                        val errorMessages = response.errors.mapNotNull { it.message }.joinToString(", ")
                        _errorMessage.value = errorMessages.ifEmpty { "Purchase failed" }
                        return@launch
                    }

                    // Extract transaction data
                    val transaction: PurchaseTransaction? = response.transaction
                    if (transaction == null) {
                        _isLoading.value = false
                        _errorMessage.value = "No transaction data received"
                        return@launch
                    }

                    transactionToken.value = transaction.token
                    Log.d(TAG, "ThreeDSPaymentFlow: Transaction token received")

                    // Check if 3DS is required (simplified - SDK auto-detects flow type)
                    if (transaction.requiredAction != null || transaction.scaAuthentication != null) {
                        // 3DS required - SDK will automatically detect and handle the appropriate flow
                        // (Gateway-Specific or Global/Forter)
                        // Keep loader visible until 3DS challenge completes (handled in threeDSChallengeResultFlow)
                        Log.d(TAG, "ThreeDSPaymentFlow: 3DS required - showing challenge (loader stays visible)")
                        sdk.showThreeDSChallenge(transaction.token, activity)
                    } else {
                        // No 3DS required - transaction complete
                        _isLoading.value = false
                        Log.d(TAG, "ThreeDSPaymentFlow: No 3DS required - transaction complete")
                        _successMessage.value = "Transaction completed successfully!"
                    }
                } finally {
                    purchaseClient.close()
                }
            } catch (e: PurchaseException.ServerError) {
                _isLoading.value = false
                _errorMessage.value = "Server error"
                Log.d(TAG, "ThreeDSPaymentFlow: Server error: ${e::class.simpleName}")
            } catch (e: PurchaseException.NetworkError) {
                _isLoading.value = false
                _errorMessage.value = "Network error"
                Log.d(TAG, "ThreeDSPaymentFlow: Network error: ${e.originalError::class.simpleName}")
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Purchase failed"
                Log.d(TAG, "ThreeDSPaymentFlow: Purchase failed: ${e::class.simpleName}")
            }
        }
    }

    /**
     * Select a product.
     */
    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        Log.d(TAG, "ThreeDSPaymentFlow: Selected product: ${product.name}")
    }

    /**
     * Select a payment method.
     */
    fun selectCard(card: SavedCard) {
        _selectedCard.value = card
        Log.d(TAG, "ThreeDSPaymentFlow: Selected card: ${card.displayName}")
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message.
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    private companion object {
        private const val TAG = "ThreeDSExampleViewModel"
    }
}
