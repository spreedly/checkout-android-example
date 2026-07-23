# Express Checkout

Add a complete, pre-built payment form to your Android app with minimal code.

**Estimated time:** ~15 minutes (assumes backend signature endpoint is already set up)

> **Example App:** See `BottomSheetPaymentScreen.kt` and `BottomSheetPaymentViewModel.kt` in the example project for a working Compose implementation. For Java/XML, see `JavaPaymentActivity.java` and `TraditionalActivity.kt`.

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Step-by-Step Integration](#step-by-step-integration)
5. [Result Handling](#result-handling)
6. [Configuration Options](#configuration-options)
7. [Advanced Configuration](#advanced-configuration)
8. [Save Card for Future Payments](#save-card-for-future-payments)
9. [Error Handling](#error-handling)
10. [Troubleshooting](#troubleshooting)
11. [Related Documentation](#related-documentation)

---

## Introduction

Express Checkout provides a complete, pre-built payment form presented as a bottom sheet. It handles all UI rendering, field validation, encryption, and tokenization. You call `sdk.expressCheckout()`, collect the result from `paymentResultFlow`, and send the token to your backend.

### When to Use

Choose Express Checkout when you need:

- Quick integration with minimal code
- A full payment form without building custom UI
- Automatic validation and error display
- Built-in save card checkbox
- Automatic screenshot protection (`SecureScreen()` applied internally)

### Express vs Custom vs Headless

| Feature | Express (`SpreedlyBottomSheet`) | Custom (`SPLTextField`) | Headless (`createCreditCard`) |
|---------|--------------------------------|-------------------------|-------------------------------|
| UI | Built-in bottom sheet | Manual layout per field | No UI -- you build everything |
| Validation | Automatic | Per-field callbacks | Manual |
| Save card checkbox | Built-in | Implement yourself | Implement yourself |
| Screenshot protection | Automatic | Must add `SecureScreen()` | Must add `SecureScreen()` |
| Integration effort | Low | Medium | High |
| Customization | Theming, additional fields | Full control | Full control |

---

## Prerequisites

1. Complete [Getting Started](getting-started.md) (installation and SDK initialization).
2. Ensure `Spreedly().init(options)` is called before presenting the form. Without valid credentials, tokenization will fail.
3. Your backend must generate fresh signature parameters (`nonce`, `signature`, `certificateToken`, `timestamp`) per payment session.

---

## Quick Start

Minimal Kotlin/Compose implementation:

```kotlin
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.SpreedlySDKInitOptions
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.paymentsheet.SpreedlyBottomSheet

// In your ViewModel
val sdk = Spreedly()

fun initialize(authParams: AuthParams) {
    sdk.init(
        SpreedlySDKInitOptions(
            nonce = authParams.nonce,
            signature = authParams.signature,
            certificateToken = authParams.certificateToken,
            timestamp = authParams.timestamp,
            environmentKey = "your_environment_key",
            context = applicationContext,
        )
    )
}

fun startPayment() {
    sdk.expressCheckout()
}

// Collect results
viewModelScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> sendTokenToBackend(result.token)
            is PaymentResult.Failed -> showError(result.message)
            is PaymentResult.Canceled -> { /* user dismissed */ }
            PaymentResult.Initial -> { /* waiting */ }
        }
    }
}
```

```kotlin
// In your Composable
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel) {
    SpreedlyBottomSheet(sdk = viewModel.sdk)

    Button(onClick = { viewModel.startPayment() }) {
        Text("Pay")
    }
}
```

That's it. `SpreedlyBottomSheet` renders the form when `sdk.expressCheckout()` is called, and hides automatically on success or cancellation.

---

## Step-by-Step Integration

### Kotlin / Jetpack Compose

**1. Create a ViewModel**

```kotlin
class PaymentViewModel(private val context: Context) : ViewModel() {
    val sdk = Spreedly()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    init {
        observePaymentResults()
    }

    fun initialize(authParams: AuthParams) {
        sdk.init(
            SpreedlySDKInitOptions(
                nonce = authParams.nonce,
                signature = authParams.signature,
                certificateToken = authParams.certificateToken,
                timestamp = authParams.timestamp,
                environmentKey = "your_environment_key",
                context = context,
            )
        )
    }

    fun startPayment() {
        _isProcessing.value = true
        _token.value = ""
        sdk.expressCheckout()
    }

    private fun observePaymentResults() {
        viewModelScope.launch {
            sdk.paymentResultFlow.collect { result ->
                when (result) {
                    is PaymentResult.Completed -> {
                        _token.value = result.token
                        _isProcessing.value = false
                        // Send token to your backend
                    }
                    is PaymentResult.Failed -> {
                        _isProcessing.value = false
                        // Handle error -- see Error Handling section
                    }
                    is PaymentResult.Canceled -> {
                        _isProcessing.value = false
                    }
                    PaymentResult.Initial -> {}
                }
            }
        }
    }
}
```

**2. Compose the UI**

Place `SpreedlyBottomSheet` in your composable tree. It renders nothing until `sdk.expressCheckout()` is called, then presents a modal bottom sheet.

```kotlin
@Composable
fun PaymentScreen(viewModel: PaymentViewModel) {
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()

    // The bottom sheet -- place once in the composable tree
    SpreedlyBottomSheet(sdk = viewModel.sdk)

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = { viewModel.startPayment() },
            enabled = !isProcessing,
        ) {
            Text(if (isProcessing) "Processing..." else "Pay Now")
        }

        if (token.isNotEmpty()) {
            Text("Token: $token")
        }
    }
}
```

**3. Create a new SDK instance per payment flow**

Always create a fresh `Spreedly()` instance when users enter a payment flow. This ensures fresh authentication and clean state.

### Java / XML

Use `PaymentSheetJavaHelper` to embed the bottom sheet in a `ComposeView` from an XML layout.

**1. Add a `ComposeView` to your XML layout**

```xml
<!-- activity_payment.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <Button
        android:id="@+id/payButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Pay Now" />

    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/composeBottomSheet"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>
```

**2. Wire up the Activity**

```java
import com.spreedly.sdk.Spreedly;
import com.spreedly.sdk.SpreedlySDKInitOptions;
import com.spreedly.sdk.ui.PaymentResult;
import com.spreedly.paymentsheet.PaymentSheetJavaHelper;

public class PaymentActivity extends AppCompatActivity {
    private Spreedly sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        sdk = new Spreedly();
        sdk.init(new SpreedlySDKInitOptions(
            /* token */ "",
            authParams.getNonce(),
            authParams.getSignature(),
            authParams.getCertificateToken(),
            authParams.getTimestamp(),
            "your_environment_key",
            getApplicationContext(),
            /* forterSiteId */ null,
            /* sdkPlatform */ SdkPlatform.ANDROID
        ));

        ComposeView composeView = findViewById(R.id.composeBottomSheet);
        PaymentSheetJavaHelper.setupContent(composeView, sdk);

        PaymentSheetJavaHelper.observePaymentResults(sdk, this, result -> {
            if (result instanceof PaymentResult.Completed) {
                String token = ((PaymentResult.Completed) result).getToken();
                // Send token to backend
            } else if (result instanceof PaymentResult.Failed) {
                String message = ((PaymentResult.Failed) result).getMessage();
                // Show error
            } else if (result instanceof PaymentResult.Canceled) {
                // User dismissed
            }
        });

        findViewById(R.id.payButton).setOnClickListener(v -> sdk.expressCheckout());
    }
}
```

`PaymentSheetJavaHelper.observePaymentResults` is lifecycle-aware -- it collects from `paymentResultFlow` and dispatches to the callback while the `LifecycleOwner` is active.

---

## Result Handling

Express Checkout uses two result channels, matching the pattern across all Spreedly SDKs:

### 1. `PaymentProcessingResult` -- validation status

Returned internally by `CheckoutButton` when the user taps the submit button. In Express Checkout this is handled automatically by the built-in form -- invalid fields are highlighted and the form stays open. You don't need to handle this directly unless building a custom form.

| Variant | Description |
|---------|-------------|
| `Processing` | Validation passed, network request started |
| `ValidationFailed(invalidFields)` | Client-side validation failed; lists the invalid `FormFieldType` entries |

### 2. `PaymentResult` via `paymentResultFlow` -- final outcome

This is what you collect. Emitted after the tokenization API call completes (or the user dismisses the sheet).

| Variant | Properties | Description |
|---------|-----------|-------------|
| `Initial` | -- | Default state before any payment |
| `Completed` | `token`, `paymentMethodResponse`, `shouldRetain`, `state`, `nonce`, `deviceData` | Tokenization succeeded |
| `Canceled` | -- | User dismissed the bottom sheet |
| `Failed` | `errorType`, `message`, `state`, `apiError`, `statusCode`, `validationErrors`, `rawErrorResponse` | Tokenization failed |

```kotlin
sdk.paymentResultFlow.collect { result ->
    when (result) {
        is PaymentResult.Completed -> {
            val token = result.token
            val shouldRetain = result.shouldRetain
            // Send token to your backend
        }
        is PaymentResult.Failed -> {
            val description = result.getDescription()
            // Show user-friendly error
        }
        is PaymentResult.Canceled -> {
            // User dismissed the sheet
        }
        PaymentResult.Initial -> { /* no-op */ }
    }
}
```

The bottom sheet auto-dismisses on `Completed`, `Canceled`, and API/network `Failed` results. It stays open on `Failed` with `UNKNOWN_ERROR` to allow retry.

---

## Configuration Options

`SpreedlyBottomSheet` accepts the following parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sdk` | `Spreedly` | *(required)* | SDK instance |
| `modifier` | `Modifier` | `Modifier` | Compose modifier |
| `config` | `PaymentSheetConfig?` | `null` | Color and styling configuration. Falls back to global theme if null |
| `displayConfig` | `PaymentSheetDisplayConfig?` | `null` | Express display (`enableAutofill`, initial `cardNumberFormat`). `null` reads legacy fields from the resolved `PaymentSheetConfig` (same as 1.1.0). Non-null uses that object as-is (no field-level merge). iOS: `CardFormDropInDisplayConfig` |
| `borderRadius` | `Dp` | `8.dp` | Corner radius for form elements |
| `fieldShape` | `Shape` | `RoundedCornerShape(8.dp)` | Shape for input fields. Independent of `borderRadius` on this API — set both explicitly if they must match. ACH `CustomFieldsConfig` resolution paths sync `fieldShape` from `borderRadius` instead; see [ACH Bank Account](ach-bank-account.md#theme-resolution). |
| `nameFieldDisplayMode` | `NameFieldDisplayMode` | `SINGLE_FIELD` | How cardholder name is displayed |
| `allowBlankName` | `Boolean` | `false` | Allow empty cardholder name |
| `allowBlankDate` | `Boolean` | `false` | Allow empty expiration date |
| `allowExpiredDate` | `Boolean` | `false` | Allow expired cards (useful for testing) |
| `yearFormat` | `YearFormat` | `MM_YY` | Expiration date format |
| `additionalFields` | `List<ConfigurableFormField>` | `emptyList()` | Optional address fields to display |
| `showSavePaymentCheckbox` | `Boolean` | `true` | Show "Save card" checkbox |
| `savePaymentCheckboxLabel` | `String` | `"Save payment information for future use"` | Checkbox label text |
| `savePaymentCheckboxDefaultChecked` | `Boolean` | `false` | Whether the checkbox starts checked |
| `coreFieldLabels` | `PaymentSheetCoreFieldLabels?` | `null` | Optional core card field label and placeholder overrides (iOS `DropInCoreFieldLabels` parity). `null` keeps SDK defaults |

### Core field copy (`PaymentSheetCoreFieldLabels`)

Override labels and placeholders for card number, CVV, and expiration fields only. Name and address labels use existing `additionalFields` / name-mode behavior and are not part of this config.

```kotlin
import com.spreedly.sdk.ui.PaymentSheetCoreFieldLabels

SpreedlyBottomSheet(
    sdk = sdk,
    coreFieldLabels = PaymentSheetCoreFieldLabels(
        cardNumberTitle = "Card number",
        cardNumberPlaceholder = "1234 5678 9012 3456",
        cvcTitle = "CVC",
        expirationDateTitle = "Expiry",
        expirationDatePlaceholder = "MM/YY",
    ),
)
```

Java:

```java
PaymentSheetJavaHelper.setupContent(
    composeView,
    sdk,
    PaymentSheetJavaHelper.createDefaultCoreFieldLabels(), // or your overrides
    PaymentSheetJavaHelper.createDefaultDisplayConfig()
);
```

Blank or null properties fall back to SDK defaults. For CVV, leave `cvcPlaceholder` unset to keep optional-CVV hint behavior when applicable.

### NameFieldDisplayMode

| Value | Description |
|-------|-------------|
| `SINGLE_FIELD` | One "Full Name" field |
| `SEPARATE_FIELDS` | Separate "First Name" and "Last Name" fields |

### YearFormat

| Value | Description |
|-------|-------------|
| `MM_YY` | Single combined field, MM/YY format |
| `SEPARATE_FIELDS_YYYY` | Separate month and year fields, 4-digit year |
| `SEPARATE_FIELDS_YY` | Separate month and year fields, 2-digit year |

---

## Advanced Configuration

### Theming

The bottom sheet resolves colors using a three-tier priority system:

1. **`config` parameter** (highest) -- passed directly to `SpreedlyBottomSheet`
2. **Global theme** -- set via `sdk.setGlobalTheme(theme)`
3. **SDK defaults** (lowest)

**Option A: Pass a `PaymentSheetConfig` directly**

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    config = PaymentSheetConfig(
        primaryColor = Color(0xFF6366F1),
        secondaryColor = Color(0xFF8B5CF6),
        formBackgroundColor = Color.White,
        fieldBackgroundColor = Color(0xFFF9FAFB),
        fieldLabelColor = Color(0xFF6B7280),
        formBorderColor = Color(0xFFD1D5DB),
        textColor = Color.Black,
        iconColor = Color(0xFF9CA3AF),
    ),
)
```

**Option B: Derive from the current MaterialTheme**

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    config = PaymentSheetConfig.fromTheme(), // adapts to light/dark automatically
)
```

**Option C: Set a global theme once**

```kotlin
sdk.setGlobalTheme(
    SpreedlyTheme(
        colors = SpreedlyColors(
            primary = Color(0xFF6366F1),
            // ...
        ),
    )
)

// All SpreedlyBottomSheet instances without an explicit config will use this
SpreedlyBottomSheet(sdk = sdk)
```

`PaymentSheetConfig` properties:

| Property | Description |
|----------|-------------|
| `primaryColor` | Buttons and highlights |
| `secondaryColor` | Accents and borders |
| `formBorderColor` | Form field borders |
| `formBackgroundColor` | Background of the entire form |
| `fieldBackgroundColor` | Background of individual input fields |
| `fieldLabelColor` | Labels and placeholder text |
| `textColor` | Input text content |
| `disabledTextColor` | Text when fields are disabled |
| `iconColor` | Icons and trailing elements |
| `placeholderColor` | Hint / placeholder tint in input fields |
| `enableAutofill` | *(deprecated)* — prefer **`PaymentSheetDisplayConfig`** on **`SpreedlyBottomSheet`** / **`PaymentSheet`**. When `displayConfig` is `null`, this property still applies (1.1.0 path) |
| `cardNumberFormat` | *(deprecated)* — same migration as `enableAutofill` |

**Express display (preferred):** pass **`displayConfig = PaymentSheetDisplayConfig(...)`** on **`SpreedlyBottomSheet`** (or **`PaymentSheet`**). **`SpreedlyTheme.toPaymentSheetConfig()`** maps colors only; it does not set express autofill or initial card format — use **`displayConfig`** or legacy **`PaymentSheetConfig`** fields when **`displayConfig`** is null.

**Mask / reveal outside the sheet:** `SpreedlyBottomSheet` does not include a mask control. To match the iframe, add a Switch or button **outside** the sheet and call **`sdk.setNumberFormat(CardNumberFormat.PLAIN)`** / **`PRETTY`** or **`sdk.toggleMask()`** while the sheet is open. CARD and CVV inside the sheet already observe **`sdk.hostedCardDisplayState`**. Opening a fresh sheet or **`resetPaymentState()`** resets display state to defaults (`PRETTY`, masked PAN/CVV). Re-apply **`setNumberFormat`** / **`toggleMask`** after reset or successful tokenization if you use a non-default mask.

For full design system documentation, see [Theme & Styling](theme-and-styling.md).

### Additional Fields

Add address fields using `ConfigurableFormField`:

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    additionalFields = listOf(
        ConfigurableFormField(type = OptionalFieldType.ADDRESS_LINE_1, isRequired = true),
        ConfigurableFormField(type = OptionalFieldType.ADDRESS_LINE_2, isRequired = false),
        ConfigurableFormField(type = OptionalFieldType.CITY, isRequired = true),
        ConfigurableFormField(type = OptionalFieldType.STATE, isRequired = true),
        ConfigurableFormField(type = OptionalFieldType.ZIP_CODE, isRequired = true),
    ),
)
```

Pre-built field lists are available:

| Preset | Fields | Required |
|--------|--------|----------|
| `ConfigurableFormField.allOptionalFields` | All 5 address fields | All optional |
| `ConfigurableFormField.commonRequiredFields` | All 5 address fields | Line 1, city, state, zip required |
| `ConfigurableFormField.minimalFields` | Address line 1 + zip | Both required |

### Validation Parameters

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    allowBlankName = false,     // require cardholder name
    allowExpiredDate = false,   // reject expired cards
    allowBlankDate = false,     // require expiration date
)
```

### Name Display Mode

```kotlin
// Single "Full Name" field (default)
SpreedlyBottomSheet(
    sdk = sdk,
    nameFieldDisplayMode = NameFieldDisplayMode.SINGLE_FIELD,
)

// Separate "First Name" and "Last Name"
SpreedlyBottomSheet(
    sdk = sdk,
    nameFieldDisplayMode = NameFieldDisplayMode.SEPARATE_FIELDS,
)
```

### Year Format

```kotlin
// Combined MM/YY (default)
SpreedlyBottomSheet(sdk = sdk, yearFormat = YearFormat.MM_YY)

// Separate month and 4-digit year fields
SpreedlyBottomSheet(sdk = sdk, yearFormat = YearFormat.SEPARATE_FIELDS_YYYY)

// Separate month and 2-digit year fields
SpreedlyBottomSheet(sdk = sdk, yearFormat = YearFormat.SEPARATE_FIELDS_YY)
```

---

## Save Card for Future Payments

The bottom sheet includes a built-in "Save card for future payments" checkbox, shown by default. The user's choice is available in `PaymentResult.Completed.shouldRetain`.

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    showSavePaymentCheckbox = true,                         // default: true
    savePaymentCheckboxLabel = "Save for next time",        // custom label
    savePaymentCheckboxDefaultChecked = false,               // default: unchecked
)

// In your result handler
sdk.paymentResultFlow.collect { result ->
    if (result is PaymentResult.Completed) {
        if (result.shouldRetain) {
            // Token will be retained in Spreedly vault for future use
        }
    }
}
```

To hide the checkbox entirely:

```kotlin
SpreedlyBottomSheet(
    sdk = sdk,
    showSavePaymentCheckbox = false,
)
```

---

## Error Handling

### Validation Errors

The bottom sheet handles validation automatically -- invalid fields are highlighted inline and the form stays open. No action needed from your code.

### API and Network Errors

These are delivered via `paymentResultFlow` as `PaymentResult.Failed`:

```kotlin
is PaymentResult.Failed -> {
    when (result.errorType) {
        PaymentResult.Failed.ErrorType.API_ERROR -> {
            // Spreedly API rejected the request
            val description = result.getDescription()
            val apiError = result.apiError  // e.g., SpreedlyApiError.VALIDATION_ERROR

            // Field-level validation errors from the API
            result.validationErrors.forEach { error ->
                Log.d("Payment", "${error.fieldName}: ${error.errorMessage}")
            }
        }
        PaymentResult.Failed.ErrorType.NETWORK_ERROR -> {
            // Connection issue -- show retry option
        }
        PaymentResult.Failed.ErrorType.UNKNOWN_ERROR -> {
            // Unexpected error -- sheet stays open for retry
        }
    }
}
```

The bottom sheet auto-dismisses on `API_ERROR` and `NETWORK_ERROR`. It stays open on `UNKNOWN_ERROR` so the user can retry without re-entering data.

For detailed error handling patterns, see [Error Handling](error-handling.md).

---

## Troubleshooting

### Bottom sheet not appearing

- Confirm `SpreedlyBottomSheet(sdk = sdk)` is in your composable tree *before* calling `sdk.expressCheckout()`.
- Verify `sdk.init(options)` completed without errors.
- Check that `SpreedlyBottomSheet` is placed at the top level of your composable, not nested inside a `LazyColumn` or other scrolling container.

### Missing payment results

- Start collecting from `sdk.paymentResultFlow` *before* calling `sdk.expressCheckout()`. If you subscribe after, you may miss the emission.
- For Java, call `PaymentSheetJavaHelper.observePaymentResults()` in `onCreate` before the user can trigger checkout.

### Theming not applying

- `config` parameter overrides the global theme. If you pass `PaymentSheetConfig()` (all defaults / `Color.Unspecified`), the global theme fills in. If you pass explicit colors, those take precedence.
- `PaymentSheetConfig.fromTheme()` must be called inside a `@Composable` function to access `MaterialTheme` colors.

### Express autofill / card format not updating

- **`sdk.setConfig(PaymentSheetConfig(...))`** updates colors and validation wiring used by the sheet, but express **field autofill** and **initial card number format** are driven by the composable **`displayConfig`** / legacy **`PaymentSheetConfig`** fields resolved when **`SpreedlyBottomSheet`** / **`PaymentSheet`** run — not by `setConfig` alone. Pass **`displayConfig`** on the composable (or keep using deprecated **`PaymentSheetConfig.enableAutofill`** / **`cardNumberFormat`** with **`displayConfig = null`**).

### Form resets unexpectedly

- The bottom sheet clears form state each time it opens, unless `sdk.shouldPreserveState()` is configured. This is by design for fresh payment sessions.

### Configuration changes (rotation)

- `SpreedlyBottomSheet` survives configuration changes when the `Spreedly` instance is held in a `ViewModel`. See `BottomSheetPaymentViewModel.kt` in the example app for the pattern.

---

## Related Documentation

- [Getting Started](getting-started.md) -- Installation, initialization, first payment
- [Custom Payment Forms](custom-payment-forms.md) -- Build custom forms with `SPLTextField` for full layout control
- [Theme & Styling](theme-and-styling.md) -- Design tokens, colors, typography, dark mode
- [Error Handling](error-handling.md) -- Error types and handling patterns
- [Security](security.md) -- Screenshot protection, encryption, PCI compliance
- [Recaching](recaching.md) -- Update CVV for saved payment methods
