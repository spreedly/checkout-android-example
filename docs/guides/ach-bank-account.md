# ACH Bank Account Payments

Tokenize bank accounts (routing number + account number) via the Spreedly Android SDK. Three integration paths are available:

1. **Pre-built bottom sheet** -- `SpreedlyBankAccountBottomSheet` handles the entire UI
2. **Custom layout** -- `BankAccountSheet` composable embedded in your own UI
3. **Headless** -- `sdk.createBankAccount()` with SPL text fields and your own form

All three paths produce a Spreedly payment method token through `paymentResultFlow`.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Required and Optional Fields](#required-and-optional-fields)
- [Pre-built Bottom Sheet](#pre-built-bottom-sheet)
- [Custom Layout](#custom-layout)
- [Headless Flow](#headless-flow)
- [Field Configuration](#field-configuration)
- [Handling Results](#handling-results)
- [State Management](#state-management)
- [Security](#security)

---

## Prerequisites

1. SDK installed and initialized per the [Getting Started](getting-started.md) guide.
2. Dependencies:

```kotlin
implementation("com.spreedly:checkout-payments-core:$spreedlyVersion")
implementation("com.spreedly:checkout-hostedfields:$spreedlyVersion")
implementation("com.spreedly:checkout-paymentsheet:$spreedlyVersion")
```

---

## Required and Optional Fields

The Spreedly API requires:

| Field | Required | Notes |
|-------|----------|-------|
| Routing number | Yes | 9-digit ABA routing number |
| Account number | Yes | Bank account number (4-17 digits) |
| Name | Yes | Either `full_name` OR `first_name` + `last_name` |

Optional fields (gateway-dependent):

| Field | Default |
|-------|---------|
| Account type | `checking` (alternatives: `savings`) |
| Account holder type | `personal` (alternatives: `business`) |
| Bank name | Not shown |

---

## Pre-built Bottom Sheet

The simplest integration. `SpreedlyBankAccountBottomSheet` wraps `BankAccountSheet` + `CheckoutButton` inside a `ModalBottomSheet` with screenshot protection.

```kotlin
import com.spreedly.paymentsheet.SpreedlyBankAccountBottomSheet
import com.spreedly.sdk.ui.BankAccountFieldConfig

var showSheet by remember { mutableStateOf(false) }

Button(onClick = { showSheet = true }) {
    Text("Pay with Bank Account")
}

SpreedlyBankAccountBottomSheet(
    sdk = sdk,
    show = showSheet,
    onDismiss = { showSheet = false },
    fieldConfig = BankAccountFieldConfig.Default,
)
```

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sdk` | `Spreedly` | -- | Initialized SDK instance |
| `show` | `Boolean` | -- | Controls sheet visibility |
| `onDismiss` | `() -> Unit` | -- | Called when the sheet is dismissed |
| `fieldConfig` | `BankAccountFieldConfig` | `Default` | Which optional fields to show |
| `customFieldsConfig` | `CustomFieldsConfig` | `Default` | Colors, border radius, etc. |
| `formFields` | `List<FormFieldType>` | `[ROUTING_NUMBER, ACCOUNT_NUMBER]` | Fields to validate before submission |
| `metadata` | `Map<String, Any>` | `emptyMap()` | Metadata attached to the payment method |
| `additionalFields` | `Map<AdditionalField, String>` | `emptyMap()` | Extra fields passed directly (e.g., address) |
| `onPaymentResult` | `((PaymentResult) -> Unit)?` | `null` | Inline result callback |

### With Result Callback

```kotlin
SpreedlyBankAccountBottomSheet(
    sdk = sdk,
    show = showSheet,
    onDismiss = { showSheet = false },
    fieldConfig = BankAccountFieldConfig.Default,
    onPaymentResult = { result ->
        when (result) {
            is PaymentResult.Completed -> {
                // result.token contains the payment method token
            }
            is PaymentResult.Failed -> {
                // result.message contains the error
            }
            else -> {}
        }
    },
)
```

---

## Custom Layout

Use `BankAccountSheet` to embed the bank account form in your own layout. You provide the submit button.

```kotlin
import com.spreedly.paymentsheet.BankAccountSheet
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.models.PaymentMethodType
import com.spreedly.sdk.ui.CheckoutButton

BankAccountSheet(
    state = sdk.bankAccountState.value,
    callbacks = sdk.bankAccountCallbacks,
    fieldConfig = BankAccountFieldConfig.Default,
    button = {
        CheckoutButton(
            sdk = sdk,
            formFields = listOf(
                FormFieldType.ROUTING_NUMBER(required = true),
                FormFieldType.ACCOUNT_NUMBER(required = true),
            ),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT,
        )
    },
)
```

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `state` | `BankAccountSheetState` | -- | Current form state from `sdk.bankAccountState` |
| `callbacks` | `BankAccountSheetCallbacks` | -- | Change handlers from `sdk.bankAccountCallbacks` |
| `button` | `@Composable () -> Unit` | -- | Your submit button (typically `CheckoutButton`) |
| `fieldConfig` | `BankAccountFieldConfig` | `Default` | Which optional fields to show |
| `customFieldsConfig` | `CustomFieldsConfig` | `Default` | Colors, border radius, etc. |
| `borderRadius` | `Dp` | `8.dp` | Surface corner radius |

### CustomFieldsConfig Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `primaryColor` | `Color` | `Unspecified` | Focus highlight and button color |
| `formBackgroundColor` | `Color` | `Unspecified` | Background for the form container |
| `fieldBackgroundColor` | `Color` | `Unspecified` | Background for individual input fields |
| `fieldLabelColor` | `Color` | `Unspecified` | Color for field labels |
| `placeholderColor` | `Color` | `Unspecified` | Color for placeholder/hint text (falls back to `fieldLabelColor`) |
| `textColor` | `Color` | `Unspecified` | Color for input text |
| `formBorderColor` | `Color` | `Unspecified` | Color for field borders |
| `iconColor` | `Color` | `Unspecified` | Color for trailing icons |
| `borderRadius` | `Dp` | `8.dp` | Corner radius for fields |

---

## Headless Flow

Build your own form using SPL text fields for routing/account numbers and standard text fields for names.

```kotlin
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType

val sdk = remember { Spreedly() }
var fullName by rememberSaveable { mutableStateOf("") }

// Name field (not sensitive -- use a standard text field)
OutlinedTextField(
    value = fullName,
    onValueChange = {
        fullName = it
        sdk.bankAccountCallbacks.onAccountHolderNameChange(it)
    },
    label = { Text("Account Holder Name") },
)

// Routing number (secure SPL field)
SPLTextField(
    formFieldType = FormFieldType.ROUTING_NUMBER(required = true),
    label = "Routing Number",
    value = sdk.bankAccountState.value.routingNumber.value,
    onChange = { sdk.bankAccountCallbacks.onRoutingNumberChange(it, true) },
)

// Account number (secure SPL field -- auto-clears when backgrounded)
SPLTextField(
    formFieldType = FormFieldType.ACCOUNT_NUMBER(required = true),
    label = "Account Number",
    value = sdk.bankAccountState.value.accountNumber.value,
    onChange = { sdk.bankAccountCallbacks.onAccountNumberChange(it, true) },
)
```

### Submitting

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        val result = sdk.createBankAccount(
            formFields = listOf(
                FormFieldType.ROUTING_NUMBER(required = true),
                FormFieldType.ACCOUNT_NUMBER(required = true),
            ),
        )
        when (result) {
            is PaymentProcessingResult.Processing -> { /* wait for paymentResultFlow */ }
            is PaymentProcessingResult.ValidationFailed -> {
                // result.invalidFields lists which fields failed
            }
        }
    }
}) {
    Text("Submit")
}
```

### Collecting Results

```kotlin
LaunchedEffect(Unit) {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                val token = result.token
                // Send to your backend
            }
            is PaymentResult.Failed -> {
                // Show error to user
            }
            else -> {}
        }
    }
}
```

### Using Separate First/Last Name

```kotlin
val sdk = remember { Spreedly() }
var firstName by rememberSaveable { mutableStateOf("") }
var lastName by rememberSaveable { mutableStateOf("") }

OutlinedTextField(
    value = firstName,
    onValueChange = {
        firstName = it
        sdk.bankAccountCallbacks.onFirstNameChange(it)
    },
    label = { Text("First Name") },
)

OutlinedTextField(
    value = lastName,
    onValueChange = {
        lastName = it
        sdk.bankAccountCallbacks.onLastNameChange(it)
    },
    label = { Text("Last Name") },
)
```

### Passing Name via Additional Fields

Instead of using callbacks, you can pass name values directly at submission time:

```kotlin
sdk.createBankAccount(
    formFields = listOf(
        FormFieldType.ROUTING_NUMBER(required = true),
        FormFieldType.ACCOUNT_NUMBER(required = true),
    ),
    additionalFields = mapOf(
        AdditionalField.FULL_NAME to "Jane Doe",
    ),
    allowBlankName = true,
)
```

---

## Field Configuration

`BankAccountFieldConfig` controls which optional fields appear in `BankAccountSheet` and `SpreedlyBankAccountBottomSheet`.

### Presets

| Preset | Name | Account Type | Holder Type | Bank Name |
|--------|------|-------------|-------------|-----------|
| `Default` | Full name | Shown | Shown | Hidden |
| `Minimal` | Full name | Hidden | Hidden | Hidden |
| `Full` | Full name | Shown | Shown | Shown |

### Custom Configuration

```kotlin
val config = BankAccountFieldConfig(
    nameDisplayMode = NameFieldDisplayMode.SEPARATE_FIELDS,
    showAccountType = true,
    showAccountHolderType = false,
    showBankName = true,
    bankNameRequired = true,
)
```

### Name Display Modes

- `NameFieldDisplayMode.SINGLE_FIELD` -- one "Account Holder Name" field (maps to `full_name`)
- `NameFieldDisplayMode.SEPARATE_FIELDS` -- separate "First Name" and "Last Name" fields

---

## Handling Results

Results flow through `sdk.paymentResultFlow` (same as credit card payments):

```kotlin
lifecycleScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                val token = result.token
                // Send token to your backend
            }
            is PaymentResult.Failed -> {
                when (result.errorType) {
                    PaymentResult.Failed.ErrorType.API_ERROR -> { /* Spreedly API error */ }
                    PaymentResult.Failed.ErrorType.NETWORK_ERROR -> { /* connectivity issue */ }
                    PaymentResult.Failed.ErrorType.UNKNOWN_ERROR -> { /* validation or other */ }
                }
            }
            is PaymentResult.Canceled -> { /* user dismissed */ }
            PaymentResult.Initial -> { /* waiting */ }
        }
    }
}
```

For the full error handling strategy, see [error-handling.md](error-handling.md).

---

## State Management

### Resetting State

Call `sdk.resetBankAccountState()` to clear all bank account fields. The pre-built bottom sheet does this automatically when it opens.

### Preserving State on Configuration Change

If you manage the sheet visibility yourself, preserve state across orientation changes:

```kotlin
sdk.preserveBankAccountStateOnNextShow()
```

Call this before the configuration change (e.g., in `ON_STOP`). The next time the sheet opens, it will skip the automatic reset.

---

## Security

- Account numbers are encrypted in memory and auto-cleared after 3 minutes when the app is backgrounded (same as CVV).
- Routing numbers are not auto-cleared (they are semi-public identifiers).
- `SpreedlyBankAccountBottomSheet` and `BankAccountSheet` both apply `SecureScreen` (FLAG_SECURE) to prevent screenshots.

For full security details, see [security.md](security.md).
