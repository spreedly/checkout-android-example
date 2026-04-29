# Enhanced Error Handling Guide

The Spreedly Android SDK provides comprehensive error handling with detailed categorization of API errors. This guide shows you how to handle different types of errors that can occur during payment processing.

## Overview

The SDK categorizes errors into three main types:

1. **API Errors** - Specific errors from the Spreedly API with detailed information
2. **Network Errors** - Connection, timeout, and other network-related issues  
3. **Unknown Errors** - Unexpected errors or exceptions

## Error Structure

### PaymentResult.Failed

All payment errors are emitted as `PaymentResult.Failed` objects containing:

```kotlin
data class Failed(
    val errorType: ErrorType,           // Type of error (API, Network, Unknown)
    val message: String?,               // Primary error message
    val state: String?,                 // Transaction state (offsite payments)
    val originalError: Throwable?,      // Original exception (for debugging)
    val apiError: SpreedlyApiError?,    // Specific API error type
    val statusCode: Int?,               // HTTP status code
    val validationErrors: List<ValidationError>, // Field-specific errors
    val rawErrorResponse: String?       // Complete error response (debugging)
)
```

### SpreedlyApiError Types

The SDK categorizes API errors into specific types:

- `ACCOUNT_INACTIVE` - Environment not activated for real transactions
- `VALIDATION_ERROR` - Field validation failures
- `PAYMENT_REQUIRED` - Account payment/billing issues
- `UNPROCESSABLE_ENTITY` - Business logic validation failures
- `UNAUTHORIZED` - Invalid credentials
- `FORBIDDEN` - Insufficient permissions
- `NOT_FOUND` - Resource not found
- `RATE_LIMITED` - Too many requests
- `SERVER_ERROR` - Server-side errors
- `UNKNOWN` - Unrecognized errors

## Handling Errors

### Basic Error Handling

```kotlin
sdk.paymentResultFlow.collect { result ->
    when (result) {
        is PaymentResult.Failed -> {
            // Simple error handling
            showErrorMessage(result.getDescription())
        }
        // ... other cases
    }
}
```

### Detailed Error Handling

```kotlin
sdk.paymentResultFlow.collect { result ->
    when (result) {
        is PaymentResult.Failed -> {
            when (result.errorType) {
                PaymentResult.Failed.ErrorType.API_ERROR -> {
                    handleApiError(result)
                }
                PaymentResult.Failed.ErrorType.NETWORK_ERROR -> {
                    handleNetworkError(result)
                }
                PaymentResult.Failed.ErrorType.UNKNOWN_ERROR -> {
                    handleUnknownError(result)
                }
            }
        }
        // ... other cases
    }
}

private fun handleApiError(error: PaymentResult.Failed) {
    when (error.apiError) {
        SpreedlyApiError.ACCOUNT_INACTIVE -> {
            showMessage("Please use test card numbers in test environment")
            // Guide user to use test cards or activate environment
        }
        
        SpreedlyApiError.VALIDATION_ERROR -> {
            if (error.hasValidationErrors()) {
                // Handle field-specific validation errors
                error.validationErrors.forEach { validationError ->
                    showFieldError(
                        fieldName = validationError.fieldName,
                        message = validationError.errorMessage ?: "Invalid value"
                    )
                }
            }
        }
        
        SpreedlyApiError.PAYMENT_REQUIRED -> {
            showMessage("Account payment required. Please contact support.")
            // Direct user to account billing or support
        }
        
        SpreedlyApiError.UNAUTHORIZED -> {
            showMessage("Authentication failed. Please check your API keys.")
            // Handle authentication issues
        }
        
        SpreedlyApiError.RATE_LIMITED -> {
            showMessage("Too many requests. Please try again in a moment.")
            // Implement retry logic with backoff
        }
        
        // ... handle other specific errors
        
        else -> {
            showMessage(error.getDescription())
        }
    }
}
```

## Common Error Scenarios

### 1. Account Inactive Error (402 Payment Required)

**What it means:** You're trying to use real card numbers with a test gateway.

**Error details:**
```json
{
  "errors": [
    {
      "key": "errors.account_inactive",
      "message": "Your environment has not been activated for real transactions..."
    }
  ]
}
```

**How to handle:**
```kotlin
SpreedlyApiError.ACCOUNT_INACTIVE -> {
    showMessage("""
        Test Environment Detected
        
        Please use test card numbers:
        • Visa: 4111111111111111
        • MasterCard: 5555555555554444
        • American Express: 378282246310005
        
        Or activate your environment for real transactions.
    """.trimIndent())
}
```

### 2. Validation Errors (422 Unprocessable Entity)

**What it means:** Required fields are missing or contain invalid data.

**Error details:**
```json
{
  "errors": [
    {
      "attribute": "last_name",
      "key": "errors.blank",
      "message": "Last name can't be blank"
    },
    {
      "attribute": "email",
      "key": "errors.invalid",
      "message": "Email is invalid"
    }
  ]
}
```

**How to handle:**
```kotlin
SpreedlyApiError.VALIDATION_ERROR -> {
    if (error.hasValidationErrors()) {
        error.validationErrors.forEach { validationError ->
            when (validationError.fieldName) {
                "first_name", "last_name" -> {
                    highlightNameField(validationError.errorMessage)
                }
                "email" -> {
                    highlightEmailField(validationError.errorMessage)
                }
                "number" -> {
                    highlightCardNumberField(validationError.errorMessage)
                }
                // ... handle other fields
            }
        }
    }
}
```

### 3. Network Errors

**What it means:** Connection issues, timeouts, or other network problems.

**How to handle:**
```kotlin
PaymentResult.Failed.ErrorType.NETWORK_ERROR -> {
    when {
        error.message?.contains("timeout") == true -> {
            showRetryableError("Request timed out. Please try again.")
        }
        error.message?.contains("connection") == true -> {
            showRetryableError("Connection failed. Please check your internet.")
        }
        else -> {
            showRetryableError("Network error occurred. Please try again.")
        }
    }
}
```

## Field-Specific Error Handling

### Map Validation Errors to UI Fields

```kotlin
private fun showFieldErrors(validationErrors: List<PaymentResult.Failed.ValidationError>) {
    // Clear previous errors
    clearAllFieldErrors()
    
    validationErrors.forEach { error ->
        when (error.fieldName) {
            "number" -> cardNumberField.showError(error.errorMessage)
            "verification_value" -> cvvField.showError(error.errorMessage)
            "month", "year" -> expiryField.showError(error.errorMessage)
            "first_name" -> firstNameField.showError(error.errorMessage)
            "last_name" -> lastNameField.showError(error.errorMessage)
            "email" -> emailField.showError(error.errorMessage)
            "address1" -> addressField.showError(error.errorMessage)
            "city" -> cityField.showError(error.errorMessage)
            "state" -> stateField.showError(error.errorMessage)
            "zip" -> zipField.showError(error.errorMessage)
            // Add other fields as needed
        }
    }
}
```

### Get Errors for Specific Fields

```kotlin
// Get all validation errors for a specific field
val emailErrors = error.getValidationErrors("email")
if (emailErrors.isNotEmpty()) {
    val errorMessage = emailErrors.joinToString("; ") { it.errorMessage ?: "Invalid" }
    emailField.showError(errorMessage)
}
```

## Error Recovery and Retry Logic

### Implement Smart Retry

```kotlin
private suspend fun handleRetryableError(error: PaymentResult.Failed) {
    when (error.apiError) {
        SpreedlyApiError.RATE_LIMITED -> {
            // Exponential backoff for rate limiting
            delay(2000) // Wait 2 seconds
            retryPayment()
        }
        
        SpreedlyApiError.SERVER_ERROR -> {
            // Retry once for server errors
            if (retryCount < 1) {
                retryCount++
                delay(1000)
                retryPayment()
            } else {
                showError("Server error persists. Please try again later.")
            }
        }
        
        else -> {
            // Don't retry for validation or authentication errors
            showError(error.getDescription())
        }
    }
}
```

### Progressive Error Messages

```kotlin
private fun getProgressiveErrorMessage(error: PaymentResult.Failed, attemptCount: Int): String {
    return when (attemptCount) {
        1 -> error.getDescription()
        2 -> "${error.getDescription()}\n\nIf this continues, please check your internet connection."
        else -> "${error.getDescription()}\n\nPersistent issue detected. Please contact support."
    }
}
```

## Testing Error Scenarios

### Test Different Error Types

```kotlin
// Inside a coroutine scope (e.g., viewModelScope.launch { ... })

// Test account inactive error (use real card in test environment)
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true)),
    // Will trigger account_inactive if using real card with test gateway
)

// Test validation errors (use empty required fields)
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true)),
    additionalFields = mapOf(
        AdditionalField.FIRST_NAME to "",  // Will trigger validation error
        AdditionalField.LAST_NAME to "",   // Will trigger validation error
    )
)

// Test success scenario (use test card)
// Set card number to "4111111111111111" in UI first
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true)),
    additionalFields = mapOf(
        AdditionalField.FIRST_NAME to "John",
        AdditionalField.LAST_NAME to "Doe",
    )
)
```

## Best Practices

### 1. User-Friendly Error Messages

The SDK provides `SpreedlyErrorMessages` for converting `SpreedlyNetworkError` values into user-facing strings. This is most commonly used with recache results:

```kotlin
import com.spreedly.sdk.SpreedlyErrorMessages

// Recache results return Result<PaymentMethodResponse, SpreedlyNetworkError>
when (result) {
    is Result.Error -> {
        val message = SpreedlyErrorMessages.getUserFriendlyMessage(result.error)
    }
}

// With a custom default fallback:
val message = SpreedlyErrorMessages.getUserFriendlyMessage(
    error = networkError,
    defaultMessage = "Payment failed. Please try again later."
)
```

For `PaymentResult.Failed` (from `paymentResultFlow`), use the built-in properties directly:

```kotlin
when (val result = paymentResult) {
    is PaymentResult.Failed -> {
        val message = result.message ?: "Payment failed"
        val apiError = result.apiError // SpreedlyApiError? for fine-grained handling
    }
}
```

From Java (recache errors):

```java
String message = SpreedlyErrorMessages.getUserFriendlyMessage(networkError);
```

For finer control over API-specific errors, you can also map `SpreedlyApiError` values directly:

```kotlin
private fun getHumanReadableError(apiError: SpreedlyApiError): String = when (apiError) {
    SpreedlyApiError.ACCOUNT_INACTIVE -> 
        "Please use test card numbers in the test environment"
    SpreedlyApiError.VALIDATION_ERROR -> 
        "Please check the highlighted fields and try again"
    SpreedlyApiError.PAYMENT_REQUIRED -> 
        "Account billing issue. Please contact support"
    SpreedlyApiError.UNAUTHORIZED -> 
        "Authentication failed. Please contact support"
    SpreedlyApiError.RATE_LIMITED -> 
        "Too many attempts. Please wait a moment and try again"
    else -> 
        "Payment could not be processed. Please try again"
}
```

### 2. Error Logging for Debugging

```kotlin
private fun logErrorForDebugging(error: PaymentResult.Failed) {
    Log.e("PaymentError", buildString {
        appendLine("Error Type: ${error.errorType}")
        appendLine("API Error: ${error.apiError}")
        appendLine("Status Code: ${error.statusCode}")
        appendLine("Message: ${error.message}")
        
        if (error.hasValidationErrors()) {
            appendLine("Validation Errors:")
            error.validationErrors.forEach { validationError ->
                appendLine("  ${validationError.fieldName}: ${validationError.errorMessage}")
            }
        }
        
        // rawErrorResponse is sanitized by the SDK but should only be logged in debug builds
        if (BuildConfig.DEBUG) {
            error.rawErrorResponse?.let { response ->
                appendLine("Raw Response: $response")
            }
            error.originalError?.let { throwable ->
                appendLine("Original Exception: ${throwable.message}")
            }
        }
    })
}
```

### 3. Graceful Degradation

```kotlin
private fun handlePaymentFailure(error: PaymentResult.Failed) {
    // Always log for debugging
    logErrorForDebugging(error)
    
    // Show user-friendly message
    val userMessage = when (error.errorType) {
        PaymentResult.Failed.ErrorType.API_ERROR -> {
            when (error.apiError) {
                SpreedlyApiError.VALIDATION_ERROR -> {
                    // Handle validation errors by highlighting fields
                    showFieldValidationErrors(error.validationErrors)
                    "Please check the highlighted fields"
                }
                else -> error.apiError?.let { getHumanReadableError(it) } ?: error.getDescription()
            }
        }
        else -> error.getDescription()
    }
    
    showUserMessage(userMessage)
    
    // Enable retry for appropriate error types
    if (isRetryable(error)) {
        showRetryButton()
    }
}

private fun isRetryable(error: PaymentResult.Failed): Boolean = when (error.apiError) {
    SpreedlyApiError.RATE_LIMITED,
    SpreedlyApiError.SERVER_ERROR -> true
    else -> error.errorType == PaymentResult.Failed.ErrorType.NETWORK_ERROR
}
```

## Migration from Legacy Error Handling

If you're upgrading from the previous error handling system:

### Before (Legacy)
```kotlin
is PaymentResult.Failed -> {
    Log.e("Payment", "Payment failed: ${result.getDescription()}")
    showError(result.getDescription())
}
```

### After (Enhanced)
```kotlin
is PaymentResult.Failed -> {
    when (result.errorType) {
        PaymentResult.Failed.ErrorType.API_ERROR -> {
            // Handle specific API errors
            handleApiError(result)
        }
        else -> {
            // Fallback to simple error handling
            showError(result.getDescription())
        }
    }
}
```

The new system is backward compatible, so you can migrate gradually by first using the simple `result.getDescription()` method and then adding specific error handling as needed.

## Customer Troubleshooting

### Common issues

| Symptom | Likely cause | What to try |
|---------|--------------|-------------|
| Gradle cannot resolve `com.spreedly:checkout-*` | Missing or invalid GitHub Packages credentials | Confirm PAT has `read:packages`; see [Getting Started § Install](getting-started.md#1-install) |
| `sdk.init` fails immediately | Expired or reused signed auth params | Issue **fresh** nonce/signature/timestamp/certificate from your backend per payment session |
| `UNAUTHORIZED` / auth errors | Wrong or revoked credentials | Rotate signing keys on the server; confirm `environmentKey` matches Spreedly |
| `ACCOUNT_INACTIVE` | Live card in test environment | Use Spreedly test cards or activate the environment |
| Timeouts / `NETWORK_ERROR` | Device or API connectivity | Retry with backoff for transient errors; see [Error Recovery and Retry Logic](#error-recovery-and-retry-logic) |

For SDK log delivery issues (Datadog), see [DATADOG_INTEGRATION.md § Troubleshooting](../development/DATADOG_INTEGRATION.md#troubleshooting).

### What you can share with Spreedly Support

**OK to share:** SDK version, approximate time (UTC), masked `environment_key` (first 4 chars only if you log it), `session_id` from Datadog global attributes (if you use SDK telemetry), `PaymentResult.Failed` `errorType` / `apiError`, HTTP status if shown, device OS level.

**Never share:** Full card number, CVV, full `environmentKey`, raw `rawErrorResponse` if it could contain tokens or PII, complete auth signatures or certificate tokens.

### When to contact support

Open a ticket via [Spreedly Support](https://spreedly.com/support/) after you have confirmed credentials, a fresh init payload, and a minimal repro (or merchant logs scoped as above). Include integration guide references you followed ([Getting Started](getting-started.md), [Security](security.md)).
