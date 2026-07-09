# CVV Recaching Guide - Spreedly Android SDK

## Overview

CVV Recaching allows users to update the CVV for a previously saved payment method without re-entering complete card details. The SDK provides a secure, customizable UI (bottom sheet or dialog) for CVV entry, ensuring PCI compliance and a seamless user experience.

## Table of Contents

- [Why Use Recaching?](#why-use-recaching)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [UI Customization](#ui-customization)
- [Complete Examples](#complete-examples)
- [Error Handling](#error-handling)
- [Security](#security)
- [Best Practices](#best-practices)

---

## Why Use Recaching?

**Problem**: CVV values cannot be stored long-term due to PCI compliance requirements, but many payment processors require a fresh CVV for repeat transactions.

**Solution**: Recaching lets customers update the CVV for saved cards without re-entering full card details.

### When to Use
- Customer returns to make a purchase with a saved card
- Payment processor requires CVV for repeat transactions
- Card is already tokenized and retained in Spreedly
- You want SDK-managed secure CVV input

### Benefits
- **PCI Compliant**: CVV never passes through your application code
- **Better UX**: No need to re-enter full card details
- **Secure**: SDK handles all sensitive data
- **Customizable**: Match your brand's look and feel

---

## Quick Start

### 1. Add SDK UI Component

Add `SpreedlyRecacheUI` to your screen (required for recaching to work):

```kotlin
import com.spreedly.paymentsheet.recache.SpreedlyRecacheUI

@Composable
fun CheckoutScreen() {
    val sdk = Spreedly()
    
    // Your screen content
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { /* trigger recache */ }) {
            Text("Pay with Saved Card")
        }
    }
    
    // Required for recaching - with optional custom theming
    SpreedlyRecacheUI(
        sdk = sdk,
        theme = customLightTheme,  // Optional: customize theme at view level
        darkTheme = customDarkTheme // Optional: customize dark theme
    )
}
```

Before calling `recachePaymentMethod`, initialize the SDK with `Spreedly.init(SpreedlySDKInitOptions(...))`. Keep `SpreedlyRecacheUI` in the composition tree whenever recache may run so the CVV UI can be presented.

### 2. Trigger Recaching

```kotlin
viewModelScope.launch {
    // Optional: Configure validation parameters globally
    spreedly.setParam(ValidationParameter.ALLOW_BLANK_NAME, true)
    spreedly.setParam(ValidationParameter.ALLOW_BLANK_DATE, true)
    spreedly.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, true)
    
    val config = RecacheConfig(
        recachePresentationMode = ScreenPresentationMode.bottomSheet,
        cardInfo = SavedCardInfo(
            lastFourDigits = "4242",
            cardType = "Visa"
        )
    )
    
    val result = spreedly.recachePaymentMethod(
        paymentMethodToken = "saved_token_here",
        config = config
    )
    
    when (result) {
        is Result.Success -> {
            val newToken = result.data.transaction.paymentMethod.token
            // Use updated token for transaction
        }
        is Result.Error -> {
            // Handle error
        }
    }
}
```

---

## API Reference

### Main Method

#### `recachePaymentMethod()`

Displays SDK-managed CVV input UI and updates the payment method.

```kotlin
suspend fun recachePaymentMethod(
    paymentMethodToken: String,
    config: RecacheConfig
): Result<PaymentMethodResponse, SpreedlyNetworkError>
```

**Parameters:**
- `paymentMethodToken`: Token of the saved payment method
- `config`: UI and display configuration

**Returns:**
- `Result.Success<PaymentMethodResponse>`: Contains the updated payment method; use `transaction.paymentMethod.token` or `recacheToken` / `paymentMethodUpdatedAt` extensions on the response
- `Result.Error<SpreedlyNetworkError>`: Validation, network, or API failure — including when the API returns HTTP 200 with `transaction.succeeded == false` (normalized to `Error`)

**Throws:**
- `IllegalStateException`: If SDK is not initialized

---

### RecacheConfig

Configuration for the CVV input UI.

```kotlin
data class RecacheConfig(
    val recachePresentationMode: ScreenPresentationMode = ScreenPresentationMode.bottomSheet,
    val cardInfo: SavedCardInfo,
    val labelText: String = "CVV",
    val placeholderText: String = "123",
    val buttonText: String = "Confirm",
    val cancelButtonText: String = "Cancel",
)
```

**Note:** Theming is **NOT** part of `RecacheConfig`. Themes are passed to `SpreedlyRecacheUI` at the view level for better separation of concerns.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `recachePresentationMode` | `ScreenPresentationMode` | `bottomSheet` | Display mode (bottom sheet or dialog) |
| `cardInfo` | `SavedCardInfo` | - | **Required**: Saved card information to display |
| `labelText` | `String` | `"CVV"` | Label for CVV input field |
| `placeholderText` | `String` | `"123"` | Placeholder text |
| `buttonText` | `String` | `"Confirm"` | Submit button text |
| `cancelButtonText` | `String` | `"Cancel"` | Cancel button text |

---

### ScreenPresentationMode

Defines how the CVV UI is displayed.

```kotlin
enum class ScreenPresentationMode {
    bottomSheet,  // Slides up from bottom (recommended for mobile)
    dialog,       // Centered dialog overlay
}
```

---

### SavedCardInfo

Information about the saved card to display.

```kotlin
data class SavedCardInfo(
    val lastFourDigits: String,     // Last 4 digits (e.g., "4242")
    val cardType: String,            // Card type (e.g., "Visa", "Mastercard")
    val cardholderName: String? = null, // Optional: Cardholder name
)
```

---

### Reactive Result Flow

Subscribe to recaching results reactively:

```kotlin
val recacheResultFlow: SharedFlow<Result<PaymentMethodResponse, SpreedlyNetworkError>>
```

**Example:**
```kotlin
viewModelScope.launch {
    spreedly.recacheResultFlow.collect { result ->
        when (result) {
            is Result.Success -> {
                // Handle success
            }
            is Result.Error -> {
                // Handle error
            }
        }
    }
}
```

If you also collect `paymentResultFlow` for tokenization on the same `Spreedly` instance, recache outcomes **do not** appear there. Use **`recacheResultFlow`** or the **`recachePaymentMethod`** return value for recache only (iframe `recache` event parity). `paymentResultFlow` remains for tokenization, APM, and offsite flows.

### PaymentMethodResponse accessors

Extensions on `PaymentMethodResponse` after a successful recache:

| API | Description |
|-----|-------------|
| `paymentMethodUpdatedAt` | ISO-8601 timestamp from `transaction.paymentMethod.updatedAt` |
| `recacheToken` | Payment method token after recache (same value as `transaction.paymentMethod.token`) |
| `PaymentMethodTransactionTypes.RECACHE_SENSITIVE_DATA` | String constant `RecacheSensitiveData` — typical `transaction.transactionType` for recache responses |

```kotlin
when (val result = spreedly.recachePaymentMethod(token, config)) {
    is Result.Success -> {
        val updatedAt = result.data.paymentMethodUpdatedAt
        val pmToken = result.data.recacheToken
    }
    is Result.Error -> { /* handle SpreedlyNetworkError */ }
}
```

---

### Recache and paymentResultFlow

CVV recache is delivered **only** on **`recacheResultFlow`** and from the **`recachePaymentMethod`** suspend return as `Result<PaymentMethodResponse, SpreedlyNetworkError>`. It is **not** mirrored to **`paymentResultFlow`** (unlike tokenization, which completes on `paymentResultFlow` as `PaymentResult`).

---

## Validation Parameters

The recaching API supports optional validation parameters that can be configured globally to control validation behavior.

### Available Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ALLOW_BLANK_NAME` | Allows recaching without requiring name fields | `false` |
| `ALLOW_BLANK_DATE` | Allows recaching without requiring expiration date fields | `false` |
| `ALLOW_EXPIRED_DATE` | Allows recaching with expired card dates | `false` |

### Setting Validation Parameters

Use `SpreedlyParamsManager` to configure validation behavior:

```kotlin
// Allow blank name fields
spreedly.setParam(ValidationParameter.ALLOW_BLANK_NAME, true)

// Allow blank date fields
spreedly.setParam(ValidationParameter.ALLOW_BLANK_DATE, true)

// Allow expired dates
spreedly.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, true)

// All parameters
spreedly.setParam(ValidationParameter.ALLOW_BLANK_NAME, true)
spreedly.setParam(ValidationParameter.ALLOW_BLANK_DATE, true)
spreedly.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, true)
```

### Example with Validation Parameters

```kotlin
viewModelScope.launch {
    // Configure validation parameters before recaching
    spreedly.setParam(ValidationParameter.ALLOW_BLANK_DATE, true)
    spreedly.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, true)
    
    val config = RecacheConfig(
        recachePresentationMode = ScreenPresentationMode.bottomSheet,
        cardInfo = SavedCardInfo(
            lastFourDigits = "4242",
            cardType = "Visa"
        )
    )
    
    val result = spreedly.recachePaymentMethod(
        paymentMethodToken = "saved_token_here",
        config = config
    )
    
    when (result) {
        is Result.Success -> {
            // Card recached successfully, even if expired
            val newToken = result.data.transaction.paymentMethod.token
        }
        is Result.Error -> {
            // Handle error
        }
    }
}
```

**Note:** When validation parameters are set to `true`, they are automatically included in the recaching API request. When set to `false` (default), they are omitted from the request.

For comprehensive information on validation parameters, see the `ALLOW_BLANK_DATE` entry in the [CHANGELOG](../CHANGELOG.md).

---

## UI Customization

Theming for recaching is passed at the **view level** (`SpreedlyRecacheUI` composable), not in the `RecacheConfig`. This provides better separation of concerns between configuration and presentation.

### Using Global Theme

Set a global theme that applies to all SDK UI components (including recaching):

```kotlin
val globalTheme = SpreedlyTheme(
    colors = SpreedlyColors(
        text = Color(0xFF1A1A1A),
        textSecondary = Color(0xFF666666),
        background = Color(0xFFF5F5F5),
        primary = Color(0xFF007AFF),
        primaryText = Color.White,
        error = Color(0xFFFF3B30),
        surface = Color.White,
        border = Color(0xFFE0E0E0),
    ),
    typography = SpreedlyTypography(
        titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        bodyMedium = TextStyle(fontSize = 16.sp),
    ),
    spacing = SpreedlySpacing(
        small = 8.dp,
        medium = 16.dp,
        large = 24.dp,
    ),
    borderRadius = SpreedlyBorderRadius(
        small = 8.dp,
        medium = 12.dp,
        large = 16.dp,
    ),
)

spreedly.setGlobalTheme(globalTheme)

// In your @Composable - no theme parameter means it uses global theme
@Composable
fun MyScreen() {
    // Your screen content
    
    SpreedlyRecacheUI(sdk = spreedly)  // Uses global theme
}
```

### Using Custom Theme for Recaching

Pass a custom theme to `SpreedlyRecacheUI` to override the global theme:

```kotlin
val customTheme = SpreedlyTheme(
    colors = SpreedlyColors(
        text = Color(0xFF1976D2),
        background = Color(0xFFE3F2FD),
        primary = Color(0xFF1976D2),
        primaryText = Color.White,
        error = Color(0xFFD32F2F),
        surface = Color.White,
        border = Color(0xFFBDBDBD),
    )
)

@Composable
fun MyScreen() {
    // Your screen content
    
    // Pass custom theme at view level
    SpreedlyRecacheUI(
        sdk = spreedly,
        theme = customTheme  // Custom theme for recaching UI
    )
}

// Config remains simple - no theming
val config = RecacheConfig(
    cardInfo = SavedCardInfo(
        lastFourDigits = "4242",
        cardType = "Visa"
    )
)
```

### Theme Priority

1. **Explicit theme** passed to `SpreedlyRecacheUI(theme = ...)` (highest)
2. **SDK global theme** via `Spreedly.setGlobalTheme()` (fallback)
3. **Default theme** (lowest)

---

## Complete Examples

### Example 1: Basic Integration

```kotlin
// ViewModel
class CheckoutViewModel : ViewModel() {
    val spreedly = Spreedly(LoggerConfiguration.PRODUCTION)
    
    private val _state = MutableStateFlow<RecacheState>(RecacheState.Idle)
    val state: StateFlow<RecacheState> = _state
    
    init {
        // Observe recache results
        viewModelScope.launch {
            spreedly.recacheResultFlow.collect { result ->
                _state.value = when (result) {
                    is Result.Success -> RecacheState.Success(
                        result.data.transaction.paymentMethod.token
                    )
                    is Result.Error -> RecacheState.Error(
                        result.error::class.simpleName ?: "Unknown error"
                    )
                }
            }
        }
    }
    
    fun recacheSavedCard(token: String, cardInfo: SavedCardInfo) {
        viewModelScope.launch {
            _state.value = RecacheState.Loading
            
            val config = RecacheConfig(
                recachePresentationMode = ScreenPresentationMode.bottomSheet,
                cardInfo = cardInfo,
                buttonText = "Confirm Payment"
            )
            
            spreedly.recachePaymentMethod(token, config)
        }
    }

    fun getSavedCards(): List<SavedCard> {
        // Replace with your saved cards data source
        return listOf(
            SavedCard("tok_abc123", "4242", "visa", cardholderName = "Jane Doe"),
            SavedCard("tok_def456", "5555", "mastercard", cardholderName = "John Smith"),
        )
    }
}

data class SavedCard(
    val token: String,
    val lastFour: String,
    val cardType: String,
    val cardholderName: String? = null,
)

sealed class RecacheState {
    object Idle : RecacheState()
    object Loading : RecacheState()
    data class Success(val token: String) : RecacheState()
    data class Error(val message: String) : RecacheState()
}

// Screen
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is RecacheState.Loading -> CircularProgressIndicator()
            is RecacheState.Success -> Text("Card updated successfully")
            is RecacheState.Error -> Text("Error: ${currentState.message}", color = Color.Red)
            else -> Button(
                onClick = {
                    viewModel.recacheSavedCard(
                        token = "saved_token",
                        cardInfo = SavedCardInfo("4242", "Visa")
                    )
                }
            ) {
                Text("Pay with Saved Card")
            }
        }
    }
    
    // Required for recaching
    SpreedlyRecacheUI(sdk = viewModel.spreedly)
}
```

### Example 2: Saved Cards List

```kotlin
@Composable
fun SavedCardsScreen(viewModel: CheckoutViewModel) {
    val savedCards = remember { viewModel.getSavedCards() }
    
    LazyColumn {
        items(savedCards) { card ->
            SavedCardItem(
                card = card,
                onClick = {
                    viewModel.recacheSavedCard(
                        token = card.token,
                        cardInfo = SavedCardInfo(
                            lastFourDigits = card.lastFour,
                            cardType = card.cardType,
                            cardholderName = card.cardholderName,
                        )
                    )
                }
            )
        }
    }
    
    SpreedlyRecacheUI(sdk = viewModel.spreedly)
}

@Composable
fun SavedCardItem(card: SavedCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(getCardIcon(card.cardType)),
                contentDescription = card.cardType
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${card.cardType.uppercase()} •••• ${card.lastFour}",
                    style = MaterialTheme.typography.bodyLarge
                )
                card.cardholderName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
```

### Example 3: Dialog Mode with Custom Theme

```kotlin
// ViewModel
fun recacheWithCustomDialog(token: String) {
    viewModelScope.launch {
        val config = RecacheConfig(
            recachePresentationMode = ScreenPresentationMode.dialog,
            cardInfo = SavedCardInfo(
                lastFourDigits = "1234",
                cardType = "Mastercard",
                cardholderName = "John Doe"
            ),
            labelText = "Security Code",
            placeholderText = "XXX",
            buttonText = "Verify & Pay",
            cancelButtonText = "Not Now"
        )
        
        val result = spreedly.recachePaymentMethod(token, config)
    }
}

// Screen - Pass theme at view level
@Composable
fun MyScreen(viewModel: MyViewModel) {
    // Define custom theme
    val customTheme = remember {
        SpreedlyTheme(
            colors = SpreedlyColors(
                text = Color(0xFF2C3E50),
                background = Color.White,
                primary = Color(0xFF3498DB),
                primaryText = Color.White,
                error = Color(0xFFE74C3C),
                surface = Color(0xFFF8F9FA),
                border = Color(0xFFDEE2E6),
            )
        )
    }
    
    // Your screen content
    Column {
        Button(onClick = { viewModel.recacheWithCustomDialog("token") }) {
            Text("Pay with Saved Card")
        }
    }
    
    // Theme passed at view level
    SpreedlyRecacheUI(
        sdk = viewModel.spreedly,
        theme = customTheme
    )
}
```

---

## Error Handling

### Common Error Scenarios

#### 1. SDK Not Initialized

```kotlin
try {
    val result = spreedly.recachePaymentMethod(token, config)
} catch (e: IllegalStateException) {
    Log.e("Recache", "SDK not initialized: ${e.message}")
    // Initialize SDK first
    spreedly.init(options)
}
```

#### 2. Invalid CVV

```kotlin
when (result) {
    is Result.Error -> {
        val error = result.error as? SpreedlyNetworkError.SpreedlyApiErrorDetail
        if (error?.errorKey == "errors.invalid_cvv") {
            showError("Invalid CVV format. Please try again.")
        }
    }
}
```

#### 3. Payment Method Not Found

```kotlin
when (result) {
    is Result.Error -> {
        val error = result.error as? SpreedlyNetworkError.SpreedlyApiErrorDetail
        if (error?.statusCode == 404) {
            // Payment method deleted or doesn't exist
            removeCardFromSavedList(token)
            showError("This card is no longer available.")
        }
    }
}
```

#### 4. Network Issues

```kotlin
when (result) {
    is Result.Error -> {
        when (result.error) {
            is SpreedlyNetworkError.NO_INTERNET -> {
                showError("No internet connection. Please try again.")
                showRetryButton()
            }
            is SpreedlyNetworkError.IO_ERROR -> {
                showError("Request timed out. Please try again.")
                showRetryButton()
            }
        }
    }
}
```

### Error Types

| Error Type | Description | Handling |
|------------|-------------|----------|
| `SpreedlyApiErrorDetail` | API validation or authentication errors | Show user-friendly message, check error key |
| `NO_INTERNET` | No internet connection | Show retry option |
| `IO_ERROR` | I/O or timeout error during network call | Show retry option |
| `SERVER_ERROR` | Server-side error (5xx) | Log and show retry option |
| `CLIENT_ERROR` | Client-side error (4xx) | Check request parameters |
| `SERIALIZATION` | Failed to parse response | Log and report to support |
| `UNKNOWN` | Unexpected error | Log and show generic message |

### Complete Error Handling Example

```kotlin
suspend fun recacheWithErrorHandling(
    token: String,
    config: RecacheConfig
): String? {
    try {
        val result = spreedly.recachePaymentMethod(token, config)
        
        return when (result) {
            is Result.Success -> {
                result.data.transaction.paymentMethod.token
            }
            is Result.Error -> {
                val errorMessage = when (val error = result.error) {
                    is SpreedlyNetworkError.SpreedlyApiErrorDetail -> {
                        when {
                            error.statusCode == 404 -> "Card not found. It may have been deleted."
                            error.errorKey?.contains("cvv") == true -> "Invalid CVV. Please try again."
                            error.statusCode == 401 -> "Authentication failed. Please contact support."
                            else -> error.errorMessage ?: "Payment processing failed."
                        }
                    }
                    is SpreedlyNetworkError.NO_INTERNET -> "No internet connection. Please check your network."
                    is SpreedlyNetworkError.IO_ERROR -> "Request timed out. Please try again."
                    else -> "An unexpected error occurred. Please try again."
                }
                
                showError(errorMessage)
                logError("Recache failed", error)
                null
            }
        }
    } catch (e: IllegalStateException) {
        showError("SDK not initialized. Please restart the app.")
        logError("Recache exception", e)
        return null
    }
}
```

---

## Security

For the full list of SDK security features, see the [Security Guide](security.md).

### Built-in Security Features

1. **Screenshot Prevention**: Automatically prevents screenshots during CVV entry
2. **No CVV Storage**: CVV values are never stored or logged
3. **SDK-Managed Input**: CVV never passes through your application code
4. **HTTPS Only**: All API calls use secure connections
5. **PCI Compliant**: Designed to maintain PCI DSS compliance

### Security Best Practices

```kotlin
// ✅ GOOD: SDK handles CVV securely
spreedly.recachePaymentMethod(token, config)

// ❌ BAD: Never build your own CVV input
// Don't create custom CVV fields - use SDK UI

// ✅ GOOD: Use SDK's secure UI components
SpreedlyRecacheUI(sdk = spreedly)

// ✅ GOOD: Don't log sensitive data
Log.d("Recache", "Starting recache for token ending in ${token.takeLast(4)}")

// ❌ BAD: Never log CVV or full tokens
Log.d("Recache", "CVV: $cvv, Token: $fullToken")  // NEVER DO THIS
```

---

## Best Practices

### 1. Always Include SDK UI Component

```kotlin
@Composable
fun MyScreen() {
    // Your content
    
    // Required for recaching to work
    SpreedlyRecacheUI(sdk = spreedly)
}
```

### 2. Check SDK Initialization

```kotlin
if (!spreedly.isInitialized) {
    spreedly.init(options)
}
```

### 3. Update Token After Success

```kotlin
when (result) {
    is Result.Success -> {
        val newToken = result.data.transaction.paymentMethod.token
        // Always update stored token
        paymentRepository.updateToken(oldToken, newToken)
    }
}
```

### 4. Provide Clear User Feedback

```kotlin
when (result) {
    is Result.Success -> showSuccess("Card verified successfully!")
    is Result.Error -> when (result.error) {
        is SpreedlyNetworkError.SpreedlyApiErrorDetail -> 
            showError("Invalid CVV. Please check and try again.")
        is SpreedlyNetworkError.NO_INTERNET -> 
            showError("No internet connection. Please check your network.")
        else -> 
            showError("Something went wrong. Please try again.")
    }
}
```

### 5. Use Lifecycle-Aware Collection

```kotlin
// In Composable
LaunchedEffect(Unit) {
    spreedly.recacheResultFlow.collect { result ->
        // Handle result
    }
}

// In ViewModel
viewModelScope.launch {
    spreedly.recacheResultFlow.collect { result ->
        // Handle result
    }
}
```

### 6. Handle Both Result Methods

You can get results in two ways:

```kotlin
// Method 1: Direct result (suspend function)
val result = spreedly.recachePaymentMethod(token, config)

// Method 2: Reactive flow (for observing updates)
spreedly.recacheResultFlow.collect { result -> }
```

Choose based on your use case:
- Use **direct result** for simple, one-time operations
- Use **`recacheResultFlow`** for reactive UI updates or when you need to observe results in multiple places
- **`paymentResultFlow`** is for tokenization and other payment flows only — not for recache outcomes

---

## Requirements

- **SDK Version**: Latest (see [GitHub Packages](https://github.com/spreedly/checkout-android-maven/packages))
- See the [Compatibility table](../../README.md#compatibility) in the README for Android API, Kotlin, Gradle, and JDK requirements.

---

## Java Integration

For Java projects, use `RecacheJavaHelper` from the `paymentsheet` module:

```java
import com.spreedly.paymentsheet.recache.RecacheJavaHelper;
import com.spreedly.sdk.models.RecacheConfig;
import com.spreedly.sdk.models.ScreenPresentationMode;

// 1. Setup the recache UI in your ComposeView
ComposeView composeView = findViewById(R.id.recache_compose_view);
RecacheJavaHelper.setupRecacheUI(composeView, sdk);

// 2. Create config and trigger recache
RecacheConfig config = RecacheJavaHelper.createRecacheConfig(
    "4242", "visa", "John Doe", ScreenPresentationMode.bottomSheet
);
RecacheJavaHelper.recachePaymentMethod(
    this, sdk, paymentMethodToken, config,
    (token, updatedAt) -> Log.d("Recache", "token=" + token + " updatedAt=" + updatedAt),
    error -> Log.e("Recache", "Recache failed")
);
```

The `BiConsumer` second argument is the ISO-8601 `updatedAt` timestamp when present (same value as `PaymentMethodResponse.paymentMethodUpdatedAt` in Kotlin).

See `RecacheJavaHelper` for the `Consumer<String>` token-only overload, full parameter options, and `createRecacheConfigFull()` for custom button text and labels.

---

## Related Documentation

- [Error Handling Guide](error-handling.md) - Comprehensive error handling
- [README](../../README.md) - SDK setup and integration

---

## Support

For questions or issues:
- Review [GitHub Issues](https://github.com/spreedly/checkout-android-sdk/issues)
- Contact Spreedly Support

---

**Last Updated**: March 4, 2026  
**SDK Version**: Latest (
see [GitHub Packages](https://github.com/spreedly/checkout-android-maven/packages))  
**Status**: Production Ready

