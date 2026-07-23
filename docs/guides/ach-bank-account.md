# ACH Bank Account Payments

Tokenize bank accounts (routing number + account number) via the Spreedly Android SDK. Three integration paths are available:

1. **Pre-built bottom sheet** -- `SpreedlyBankAccountBottomSheet` handles the entire UI
2. **Custom layout** -- `BankAccountSheet` composable embedded in your own UI
3. **Headless** -- `sdk.createBankAccount()` with SPL text fields and your own form

All three paths produce a Spreedly payment method token. The drop-in bottom sheet delivers its result on the `onPaymentResult` callback; the custom-layout and headless paths deliver on `paymentResultFlow`. See [Handling Results](#handling-results).

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
            is PaymentResult.Canceled -> {
                // user dismissed the sheet (swipe / back)
            }
            PaymentResult.Initial -> {}
        }
    },
)
```

---

## Custom Layout

Use `BankAccountSheet` with an `sdk` parameter so the same `fieldConfig` drives both the form UI and `createBankAccount` validation. Pass `isFormValid` into `CheckoutButton` (or otherwise gate submit until the form is valid).

Passing `fieldConfig` alone to the state/callbacks overload does **not** configure SDK submission validation.

```kotlin
import com.spreedly.paymentsheet.BankAccountSheet
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.models.PaymentMethodType
import com.spreedly.sdk.ui.BankAccountFieldConfig
import com.spreedly.sdk.ui.CheckoutButton

BankAccountSheet(
    sdk = sdk,
    fieldConfig = BankAccountFieldConfig.Default,
    button = { isFormValid ->
        CheckoutButton(
            sdk = sdk,
            formFields = listOf(
                FormFieldType.ROUTING_NUMBER(required = true),
                FormFieldType.ACCOUNT_NUMBER(required = true),
            ),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT,
            isFormValid = isFormValid,
        )
    },
)
```

This overload clears in-memory ACH fields when it leaves composition (navigation back, parent `if (show)` becoming false, etc.). Mid-edit values are preserved across configuration change only when a host Activity can be resolved (or is supplied by the drop-in bottom sheet) and reports `isChangingConfigurations`. If no Activity is available, leave-clear is fail-closed (state is reset). Process death does not preserve SDK bank-account state.

Use the same `allowBlankName` setting for the rendered form and for `createBankAccount` (global `SpreedlyParamsManager` / `setParam`, or the per-call override on submit).

Blank names may be accepted when `allowBlankName` is enabled. Nonblank names must still satisfy ACH minimum length (2), maximum length (350), and combined first+last length rules.

### Parameters (`BankAccountSheet(sdk, …)`)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sdk` | `Spreedly` | -- | SDK instance; syncs `fieldConfig` and supplies state/callbacks |
| `button` | `@Composable (isFormValid: Boolean) -> Unit` | -- | Your submit button (typically `CheckoutButton`); pass `isFormValid` into `CheckoutButton` |
| `fieldConfig` | `BankAccountFieldConfig` | `Default` | Which optional fields to show (also applied to submission validation) |
| `customFieldsConfig` | `CustomFieldsConfig` | `Default` | Colors, border radius, etc. |
| `borderRadius` | `Dp` | `8.dp` | Surface corner radius |
| `modifier` | `Modifier` | -- | Outer surface modifier |

### State/callbacks overload (manual config sync)

If you embed `BankAccountSheet(state, callbacks, fieldConfig, …)` yourself (for example a headless dual-write UI), you **must** sync config **and** clear ACH state on leave (this overload does not auto-clear):

```kotlin
// Resolve the Activity by unwrapping ContextWrapper layers. `LocalContext.current as? Activity`
// is not reliable — Compose often runs under a ContextThemeWrapper, so the direct cast returns null.
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

val activity = LocalContext.current.findActivity()

SideEffect {
    sdk.setBankAccountFieldConfig(fieldConfig)
}

DisposableEffect(Unit) {
    onDispose {
        // Fail-closed when Activity is missing (treat as true leave).
        // Do not call preserveBankAccountStateOnNextShow() from custom layouts —
        // that latch is for the drop-in bottom sheet open path only.
        if (activity?.isChangingConfigurations != true) {
            sdk.resetBankAccountState()
        }
        // On configuration change, leave SDK in-memory state as-is (no preserve latch).
    }
}

BankAccountSheet(
    state = sdk.bankAccountState.value,
    callbacks = sdk.bankAccountCallbacks,
    fieldConfig = fieldConfig,
    button = { isFormValid ->
        CheckoutButton(
            sdk = sdk,
            formFields = listOf(
                FormFieldType.ROUTING_NUMBER(required = true),
                FormFieldType.ACCOUNT_NUMBER(required = true),
            ),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT,
            isFormValid = isFormValid,
        )
    },
)
```

Prefer `BankAccountSheet(sdk, …)` when you can — it syncs `fieldConfig` and clears on leave for you.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `state` | `BankAccountSheetState` | -- | Current form state from `sdk.bankAccountState` |
| `callbacks` | `BankAccountSheetCallbacks` | -- | Change handlers from `sdk.bankAccountCallbacks` |
| `button` | `@Composable (isFormValid: Boolean) -> Unit` | -- | Your submit button; pass `isFormValid` into `CheckoutButton` |
| `fieldConfig` | `BankAccountFieldConfig` | `Default` | Which optional fields to show (must match `setBankAccountFieldConfig`) |
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
| `fieldShape` | `Shape` | `RoundedCornerShape(8.dp)` | Field corner shape; synced from `borderRadius` on SDK resolution paths |

### Theme resolution

`BankAccountSheet` and `SPLTextField` merge your `customFieldsConfig` with the global theme from `Spreedly.setGlobalTheme()` at render time. For each color, an explicit value on `customFieldsConfig` wins; `Color.Unspecified` falls back to the global theme.

`borderRadius` is the authoritative control for field corners on all ACH fields (name `AppTextField` and routing/account `SPLTextField`). SDK resolution paths derive `fieldShape` from `borderRadius`, so a mismatched explicit `fieldShape` is overridden — set `borderRadius` instead.

For headless layouts that mix `AppTextField` with `SPLTextField`, call `resolveEffectiveCustomFieldsConfig(config)` inside a `@Composable` to style name fields the same way the SDK resolves SPL fields:

```kotlin
import com.spreedly.ui.theme.resolveEffectiveCustomFieldsConfig

val resolvedConfig = resolveEffectiveCustomFieldsConfig(customFieldsConfig)

AppTextField(
    backgroundColor = resolvedConfig.fieldBackgroundColor,
    focusedBorderColor = resolvedConfig.primaryColor,
    shape = resolvedConfig.fieldShape,
    // ...
)
```

---

## Headless Flow

Build your own form using SPL text fields for routing/account numbers and standard text fields for names.

Call `sdk.setBankAccountFieldConfig(fieldConfig)` whenever your headless form’s field visibility differs from `BankAccountFieldConfig.Default`, and keep `allowBlankName` consistent between UI gating and `createBankAccount`.

> **PCI:** Any screen that renders cardholder or bank-account fields must block screenshots and screen recording. Call `SecureScreen()` (or apply `Modifier.secureScreen()` to the container) in your headless form — the drop-in `SpreedlyBankAccountBottomSheet` and `BankAccountSheet` already do this for you.

```kotlin
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.security.SecureScreen

val sdk = remember { Spreedly() }
var fullName by remember { mutableStateOf("") }

// Prevent screenshots / screen recording of the bank-account fields (FLAG_SECURE).
SecureScreen()

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
var firstName by remember { mutableStateOf("") }
var lastName by remember { mutableStateOf("") }

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

Results are delivered by different channels depending on the integration path.

**One ACH session per `Spreedly` instance.** Do not run two ACH UIs (drop-in + custom, or two drop-ins) against the same SDK at once. A second `createBankAccount()` / submit while another operation is in flight, in the post-claim cleanup window, or while a terminal awaits acknowledgement returns **`PaymentProcessingResult.Rejected(ALREADY_PROCESSING)`** before any network dispatch — parameters from the rejected call are ignored. Prefer one ACH UI, or separate `Spreedly` instances for concurrent flows.

| Path | Result delivery | Submitting / duplicate-submit |
|---|---|---|
| Drop-in `SpreedlyBankAccountBottomSheet` | Terminals on `onPaymentResult` (surface-scoped). `Completed`/`Failed` also on `paymentResultFlow`. `Canceled` is surface-only (not on the shared flow). | SDK-owned submitting latch; duplicate taps return `Rejected(ALREADY_PROCESSING)` |
| Custom `BankAccountSheet(sdk)` + `CheckoutButton` | Same as headless for tokenize: `paymentResultFlow` for `Completed`/`Failed` | Same latch and rejection semantics when using SDK `CheckoutButton` |
| Headless / direct `createBankAccount()` | `paymentResultFlow` only (`Completed`/`Failed`) | Second concurrent call returns `Rejected(ALREADY_PROCESSING)`; handle in your submit handler |

- **Drop-in** — every terminal (`Completed`, `Failed`, `Canceled`) is delivered to `onPaymentResult`, scoped to that sheet’s surface id. Shared-flow collectors still see `Completed`/`Failed`. **Cancellation is surface-only.**
- **Headless / direct** — `Completed` and `Failed` on `paymentResultFlow`; no user-cancel concept. Unexpected SDK failures return immediate `PaymentProcessingResult.Failed(UNEXPECTED_ERROR)` while the sanitized `PaymentResult.Failed` is delivered asynchronously. ACH network/API failures never expose `originalError` or `rawErrorResponse` on public results.

**Programmatic hide vs user-cancel**: setting `show = false` from the parent hides the sheet and clears SDK state, but does **not** call `onPaymentResult(Canceled)`. Only a swipe/back dismiss triggered by the user produces a `Canceled` result. If your UI needs to distinguish "user abandoned" from "parent closed the sheet", track that distinction in your own state rather than relying on `Canceled` alone.

**Single ACH surface per `Spreedly` instance**: the form state (`bankAccountState`) is a single global bucket per SDK instance. While terminal routing is surface-scoped, the form data is shared. Do not render two ACH UIs against the same `Spreedly` at the same time; use separate instances if you need concurrent flows.

**Swipe/back dismiss when another session owns the latch**: `cancelBankAccountAttempt` is fail-closed — if a headless or anonymous attempt owns the in-flight session when the user swipes to dismiss the drop-in sheet, the cancel is a no-op (no `Canceled` terminal). The drop-in sheet still hides, but `onPaymentResult(Canceled)` is **not** delivered. Sensitive form fields are still cleared when the sheet leaves composition (leave-clear), independent of the cancel result. This is an unsupported configuration; it occurs only when you violate the "single ACH surface per `Spreedly`" constraint above. Use separate `Spreedly` instances for concurrent flows.

**Callback threading**: drop-in `onPaymentResult` runs on the **main thread** after sensitive bank-account state is cleared. Keep callbacks fast — avoid blocking work or starting another payment while the sheet is dismissing.

Prefer the drop-in `onPaymentResult` callback for the sheet; use `paymentResultFlow` for the headless/custom path. If you observe both, expect `Completed`/`Failed` on each — deduplicate by your own request state.

Client-side validation failures emit a generic `Failed` on `paymentResultFlow` and keep the form open for correction (not a dismiss terminal).

### Cancellation, in-flight tokenization, and retry

Once client validation passes, the SDK **always runs the tokenization HTTP request to completion**, even if the merchant coroutine is cancelled (sheet dismiss, navigation away, or `Job.cancel()`). User cancel or leave during an in-flight request enters **CanceledDraining**: `isAchSubmitting` stays true and `tryBeginAttempt` returns `Rejected(ALREADY_PROCESSING)` until the abandoned request returns (result dropped) or the attempt is abandoned before the network call starts.

A surface-owned `Canceled` terminal may be delivered and acknowledged **while the latch is still held** through drain. Do not treat ack of `Canceled` as permission to submit again until `isAchSubmitting` is false.

Once a surface retains `Completed` or `Failed`, cancellation **cannot** replace that terminal with `Canceled`. Drop-in swipe/back waits for the collector to deliver the definitive result (then dismisses). Global publish and surface retain happen in one Main transaction — an attempt is never both completed/failed on `paymentResultFlow` and canceled on the drop-in surface.

| Phase | Coroutine cancelled | Latch | Merchant retry |
|-------|---------------------|-------|----------------|
| Before network dispatch (validation) | `abandonBankAccountAttempt` | Released immediately | Allowed |
| During / after network dispatch | NonCancellable completion + CanceledDraining | Held until abandoned request returns | **Rejected** until drain completes (even after `Canceled` ack) |

**Client cancellation is not server revocation.** Aborting the coroutine or closing the UI cancels the Ktor request on a best-effort basis only. Spreedly may still create a payment method if the request was already accepted (orphan token). The SDK does **not** send idempotency keys today.

**Safe retry guidance for merchants:**

1. Wait until `isAchSubmitting` is false (and no unacknowledged surface terminal) before calling `createBankAccount()` again.
2. If the user dismissed during an in-flight submit, treat the operation as **pending** until the latch clears or your backend confirms no payment method was created.
3. For backend deduplication, include your own correlation id in `metadata` (for example `metadata = mapOf("client_attempt_id" to yourUuid)`).

Do not call `createBankAccount()` again while `isAchSubmitting` is true or a surface terminal awaits acknowledgement.

### Drop-in callback

```kotlin
SpreedlyBankAccountBottomSheet(
    sdk = sdk,
    show = showSheet,
    onDismiss = { showSheet = false },
    onPaymentResult = { result ->
        when (result) {
            is PaymentResult.Completed -> { /* result.token */ }
            is PaymentResult.Failed -> { /* result.message */ }
            is PaymentResult.Canceled -> { /* user dismissed the sheet */ }
            PaymentResult.Initial -> {}
        }
    },
)
```

### Headless flow

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
            is PaymentResult.Canceled,
            PaymentResult.Initial,
            -> {}
        }
    }
}
```

For the full error handling strategy, see [error-handling.md](error-handling.md).

---

## State Management

### Resetting State

Call `sdk.resetBankAccountState()` to clear all bank account fields.

`BankAccountSheet(sdk, …)` and the pre-built bottom sheet clear state when the form leaves composition (swipe/back dismiss, cancel-driven close, or navigation away) and again when the bottom sheet opens, unless preserve-on-next-show is set. After success or API/network failure, `createBankAccount()` resets form state **before** publishing the payment result; the drop-in sheet dismisses only on a terminal retained for **its** surface id (`Completed`, `Failed` with `API_ERROR` / `NETWORK_ERROR` / `UNKNOWN_ERROR`, or `Canceled`), delivered through `onPaymentResult`. An untyped result on the shared flow never dismisses the drop-in. The state/callbacks overload does **not** auto-clear on leave — call `resetBankAccountState()` yourself, or prefer the `sdk` overload.

`createBankAccount()` also calls `resetBankAccountState()` after a successful tokenize and after API/network failures; client-side validation failures leave the form filled so the user can correct input. Merchants observing success or failure callbacks should expect ACH UI state to already be empty after tokenize success and network/API errors.

### Preserving State on Configuration Change

`BankAccountSheet(sdk, …)` leaves in-memory values in place across configuration change when a host Activity reports `isChangingConfigurations` (fail-closed clear if Activity cannot be resolved). The drop-in bottom sheet additionally sets `preserveBankAccountStateOnNextShow()` so its open-path reset is skipped after recreation. Standalone `BankAccountSheet(sdk)` and custom state/callbacks layouts should **not** call that latch — a later drop-in open must still reset. Process death is not a configuration change: in-memory SDK state is not restored. If you manage visibility yourself with the state/callbacks overload (or a custom wrapper):

```kotlin
// Only when the Activity is actually recreating (rotation), not on every ON_STOP
if (activity?.isChangingConfigurations != true) {
    sdk.resetBankAccountState()
}
// Else leave SDK in-memory state as-is (do not call preserveBankAccountStateOnNextShow)
```

Card payment sheet still resets only on open (intentional difference).

---

## Security

- Account numbers are encrypted in memory and auto-cleared after 3 minutes when the app is backgrounded (same as CVV).
- Routing numbers are not auto-cleared (they are semi-public identifiers).
- `SpreedlyBankAccountBottomSheet` and `BankAccountSheet` both apply `SecureScreen` (FLAG_SECURE) to prevent screenshots.

For full security details, see [security.md](security.md).
