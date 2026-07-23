# Migrating to the Spreedly Checkout Android SDK

Move from the legacy `mobile-sdk-android` or a WebView-based iFrame/Express integration to the modern Checkout Android SDK.

This guide covers two migration paths:

- **Path A** -- From the old native `mobile-sdk-android` (`com.spreedly:client`, `com.spreedly:express`, `com.spreedly:securewidgets`)
- **Path B** -- From Spreedly iFrame or Express running inside an Android WebView

Both paths converge on the same modern SDK. If you are already on the Checkout Android SDK and upgrading between versions, see the [version-specific migration guides](../migration/) instead.

---

## What you gain

The Checkout Android SDK replaces both the old native SDK and WebView-based approaches with:

- Jetpack Compose UI with full theming and dark mode support
- Server-side authentication (no secrets on-device)
- PCI scope reduction (sensitive data never touches merchant code)
- ACH bank account tokenization
- 3D Secure authentication (Forter global and gateway-specific)
- Alternative payment methods (Stripe APM, Braintree APM)
- Offsite payments (PayPal, Pix, Boleto, OXXO, NuPay)
- CVV recaching for saved cards
- Screen capture protection (`FLAG_SECURE`)
- Built-in telemetry via Datadog

---

## Compatibility

See the [README compatibility table](../../../README.md#compatibility) for minimum and recommended versions of Android API, Kotlin, AGP, KSP, Gradle, and Android Studio.

---

## Architectural differences

| Area | Old SDK / WebView | Checkout SDK |
|------|-------------------|--------------|
| **Language** | Java (old SDK) / JavaScript (WebView) | Kotlin (Java helpers for most modules; `stripe-radar` uses `SpreedlyStripeRadar`) |
| **UI** | XML Views (`SecureForm`, `SecureCreditCardField`) / WebView | Jetpack Compose (`SPLTextField`, `SpreedlyBottomSheet`) |
| **Async model** | RxJava `Single` / JS callbacks | Coroutines (`suspend fun`) + `SharedFlow<PaymentResult>` |
| **Authentication** | API secret stored on-device: `SpreedlyClient.newInstance("key", "secret", true)` | Server-generated signed params per session (nonce, signature, certificateToken, timestamp) -- no secret on device |
| **PCI scope** | Merchant code constructs `CreditCardInfo` with raw card data / WebView handles it in JS | Sensitive data flows exclusively through SDK secure components (`SPLTextField` / `sdk.callbacks`) -- never in merchant code |
| **Dependencies** | `com.spreedly:client` / `express` / `securewidgets` | Multi-module: `checkout-payments-core` / `checkout-hostedfields` / `checkout-paymentsheet` + optional `checkout-threeds`, `checkout-braintree-apm`, `checkout-stripe-apm`, `checkout-stripe-radar` |
| **Result delivery** | `onActivityResult` with `EXTRA_PAYMENT_METHOD_TOKEN` / JS bridge | `sdk.paymentResultFlow: SharedFlow<PaymentResult>` |

---

## Step 1: Implement server-side authentication

This is the biggest change in both paths. The old SDK stored your API secret directly in the app. The new SDK requires a server endpoint that generates signed authentication parameters for each checkout session.

### Old SDK (Path A)

```java
SpreedlyClient client = SpreedlyClient.newInstance("envKey", "envSecret", true);
```

### WebView (Path B)

Auth was either embedded in the iFrame configuration or passed through `addJavascriptInterface`.

### New SDK (both paths)

Your backend generates signed params per session. The app fetches them and passes them to the SDK:

```kotlin
val sdk = Spreedly()
sdk.init(
    SpreedlySDKInitOptions(
        nonce = authParams.nonce,
        signature = authParams.signature,
        certificateToken = authParams.certificateToken,
        timestamp = authParams.timestamp,
        environmentKey = "your_environment_key",
        context = applicationContext,
    ),
)
```

### Backend endpoint (pseudocode)

Your server needs an endpoint that generates these parameters. At a high level:

```
POST /api/v1/auth/params

1. Generate a unique nonce (UUID or similar)
2. Get the current UTC timestamp
3. Retrieve your certificate_token from Spreedly
4. Create an HMAC-SHA256 signature using your Spreedly API secret:
     signature = HMAC-SHA256(secret, nonce + timestamp + certificate_token)
5. Return JSON: { nonce, signature, certificate_token, timestamp }
```

Never generate these values in client-side code. The API secret must stay on your server.

---

## Step 2: Remove old dependencies

### Path A -- Old native SDK

Remove the old Maven repository and artifacts from your Gradle files:

```diff
 // settings.gradle(.kts) or build.gradle(.kts)
 repositories {
-    maven { url = uri('https://raw.githubusercontent.com/spreedly/mobile-sdk-android/maven/') }
 }

 // app/build.gradle(.kts)
 dependencies {
-    implementation 'com.spreedly:client:0.1-beta'
-    implementation 'com.spreedly:express:0.1-beta'
-    implementation 'com.spreedly:securewidgets:0.1-beta'
 }
```

If RxJava was only used for the Spreedly SDK, remove those dependencies too:

```diff
-    implementation 'io.reactivex.rxjava2:rxjava:2.x.x'
-    implementation 'io.reactivex.rxjava2:rxandroid:2.x.x'
```

### Path B -- WebView integration

Remove all WebView-based payment code:

- The `WebView` component and its configuration (`WebSettings`, `setJavaScriptEnabled`)
- Any `@JavascriptInterface` bridge class used to communicate with the iFrame/Express form
- `addJavascriptInterface` calls
- The iFrame or Express `<script>` tag references (`spreedly.js`, `express-checkout.js`)
- CSP or `WebViewClient` overrides related to Spreedly domains
- JavaScript evaluation calls (`evaluateJavascript`) for tokenization

---

## Step 3: Add new SDK dependencies

Add the Spreedly Maven repository (GitHub Packages) and the SDK artifacts:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/spreedly/checkout-android-maven")
            credentials {
                username = "your_github_username"
                password = "your_github_personal_access_token" // needs read:packages scope
            }
        }
        // When using checkout-threeds (Global or Gateway-Specific 3DS):
        // maven {
        //     url = uri("https://mobile-sdks.forter.com/android")
        //     credentials { ... }  // see ../3ds-global.md#step-1-add-maven-repository
        // }
        google()
        mavenCentral()
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    // Typical card UI (payments-core is always required)
    implementation("com.spreedly:checkout-payments-core:$spreedlyVersion")
    implementation("com.spreedly:checkout-hostedfields:$spreedlyVersion")   // SPLTextField / custom forms
    implementation("com.spreedly:checkout-paymentsheet:$spreedlyVersion")   // pre-built bottom sheet

    // 3DS — uncomment when using Global or Gateway-Specific 3DS
    // implementation("com.spreedly:checkout-threeds:$spreedlyVersion")

    // Optional — add only the flows you use
    // implementation("com.spreedly:checkout-braintree-apm:$spreedlyVersion")  // PayPal, Venmo
    // implementation("com.spreedly:checkout-stripe-apm:$spreedlyVersion")     // iDEAL, Bancontact, EPS, P24, SEPA
    // implementation("com.spreedly:checkout-stripe-radar:$spreedlyVersion")   // Stripe Radar fraud sessions
}
```

See the [README installation notes](../../../README.md#installation-notes) for when each module and repository is required.

### ProGuard / R8

If you use custom ProGuard rules, add:

```proguard
-keep class com.spreedly.** { *; }
-keepclassmembers class com.spreedly.** { *; }
-keep class io.ktor.** { *; }
```

---

## Step 4: Migrate payment flows

### Key architectural change: no raw card data in merchant code

The old SDK let merchant code construct `CreditCardInfo` objects containing raw card numbers and CVVs. The new SDK does not have an equivalent. All sensitive data flows through SDK-owned secure components:

- **`SPLTextField`** composables write card data to internal encrypted state
- **`sdk.callbacks`** methods update internal `PaymentSheetState`

The `createCreditCard()` and `createPaymentMethod()` methods read from that internal state. Your code never handles raw card numbers, CVVs, or account numbers directly. This is a PCI scope reduction by design.

### Option A: Express Checkout (pre-built bottom sheet)

The fastest migration path. Replaces `ExpressBuilder` (old SDK) or the Express JS form (WebView) with a pre-built Compose bottom sheet.

#### Kotlin (Compose)

```kotlin
// 1. Initialize
val sdk = Spreedly()
sdk.init(initOptions)

// 2. Place the bottom sheet in your Compose tree
@Composable
fun CheckoutScreen() {
    SpreedlyBottomSheet(sdk = sdk)
}

// 3. Collect results BEFORE triggering the sheet
lifecycleScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                val token = result.token
                // Send token to your backend
            }
            is PaymentResult.Failed -> {
                val error = result.getDescription()
                // Show error to user
            }
            is PaymentResult.Canceled -> {
                // User dismissed the sheet
            }
            PaymentResult.Initial -> { /* Waiting */ }
        }
    }
}

// 4. Show the sheet
sdk.expressCheckout()
```

`SpreedlyBottomSheet` accepts parameters for customization: `nameFieldDisplayMode`, `yearFormat`, `additionalFields`, `showSavePaymentCheckbox`, `savePaymentCheckboxLabel`, `allowBlankName`, `allowBlankDate`, `allowExpiredDate`, and `config: PaymentSheetConfig` for color overrides.

See the [Express Checkout guide](../express-checkout.md) for the full parameter reference.

#### Java (using PaymentSheetJavaHelper)

If you are not ready to adopt Kotlin, use `PaymentSheetJavaHelper` to mount the bottom sheet inside a `ComposeView` from a traditional Activity or Fragment:

```java
// In your Activity's onCreate or Fragment's onCreateView
ComposeView composeView = findViewById(R.id.compose_view);
PaymentSheetJavaHelper.setupContent(composeView, sdk);

// Observe results
PaymentSheetJavaHelper.observePaymentResults(sdk, lifecycleOwner, result -> {
    if (result instanceof PaymentResult.Completed) {
        String token = ((PaymentResult.Completed) result).getToken();
        // Send token to your backend
    }
});

// Show the sheet
sdk.expressCheckout();
```

#### What maps to what

| Old SDK (`ExpressBuilder`) | New SDK |
|---|---|
| `SpreedlyClient.newInstance(key, secret, test)` | `Spreedly()` + `sdk.init(SpreedlySDKInitOptions(...))` |
| `ExpressBuilder(client, paymentOptions)` | `SpreedlyBottomSheet(sdk = sdk)` composable |
| `builder.showDialog(fm, tag, target, requestCode)` | `sdk.expressCheckout()` |
| `onActivityResult` + `EXTRA_PAYMENT_METHOD_TOKEN` | `sdk.paymentResultFlow` collecting `PaymentResult.Completed.token` |
| `paymentOptions.setButtonText("Pay")` | `SpreedlyBottomSheet` parameters or `PaymentSheetConfig` |
| `paymentOptions.setPaymentType(PaymentType.ALL)` | Card vs bank account handled by separate composables (`SpreedlyBottomSheet` for cards, `SpreedlyBankAccountBottomSheet` for ACH) |
| `paymentOptions.setStoredCardList(cards)` | Use [Recaching](../recaching.md) for saved card CVV updates |

| WebView (Express JS) | New SDK |
|---|---|
| `new SpreedlyExpressCheckout(authDetails)` | `SpreedlyBottomSheet(sdk = sdk)` composable |
| `expressCheckout()` JS call | `sdk.expressCheckout()` |
| `tokenGenerated` JS event | `PaymentResult.Completed` on `sdk.paymentResultFlow` |

### Option B: Custom payment forms (hosted fields)

Build your own payment form layout using secure `SPLTextField` composables for sensitive fields and standard Compose components for everything else.

#### Old SDK mapping

| Old SDK (SecureForm / XML) | New SDK (Compose) |
|---|---|
| `SecureCreditCardField` (`@id/spreedly_credit_card_number`) | `SPLTextField(formFieldType = FormFieldType.CARD(true), sdk = sdk, onChange = { ... })` |
| `SecureTextField` (`@id/spreedly_cvv`) | `SPLTextField(formFieldType = FormFieldType.CVV(true), sdk = sdk, onChange = { ... })` |
| `SecureExpirationDate` (`@id/spreedly_cc_expiration_date`) | `SPLTextField(formFieldType = FormFieldType.EXPIRY_DATE(true), onChange = { ... })` or separate `MONTH()` / `YEAR()` fields |
| `TextInputLayout` (`@id/spreedly_full_name`) | Standard Compose `TextField` + `sdk.callbacks.onNameOnCardChange(value, isRequired = true)` |
| `TextInputLayout` (`@id/spreedly_first_name`) | Standard Compose `TextField` + `sdk.callbacks.onFirstNameChange(value, isRequired = true)` |
| `TextInputLayout` (`@id/spreedly_last_name`) | Standard Compose `TextField` + `sdk.callbacks.onLastNameChange(value, isRequired = true)` |
| `SecureForm.createCreditCardPaymentMethod()` (RxJava) | `sdk.createCreditCard(formFields, metadata, additionalFields)` (suspend, returns `PaymentProcessingResult`) |
| `SecureForm.createBankAccountPaymentMethod()` (RxJava) | `sdk.createBankAccount(formFields, metadata, additionalFields)` (suspend, returns `PaymentProcessingResult`) |

| WebView (Hosted Fields JS) | New SDK (Compose) |
|---|---|
| `<div id="spreedly-number">` + `inAppElements()` | `SPLTextField(formFieldType = FormFieldType.CARD(true), sdk = sdk, onChange = { ... })` |
| JS field change callbacks | `SPLTextField`'s `onChange: (String) -> Unit` lambda |

#### Kotlin example

```kotlin
@Composable
fun CustomPaymentForm(sdk: Spreedly) {
    val coroutineScope = rememberCoroutineScope()
    var cardValue by remember { mutableStateOf("") }
    var expiryValue by remember { mutableStateOf("") }
    var cvvValue by remember { mutableStateOf("") }

    Column {
        SPLTextField(
            formFieldType = FormFieldType.CARD(true),
            sdk = sdk,
            label = "Card Number",
            value = cardValue,
            onChange = { cardValue = it },
        )
        SPLTextField(
            formFieldType = FormFieldType.EXPIRY_DATE(true),
            label = "Expiry Date (MM/YY)",
            value = expiryValue,
            onChange = { expiryValue = it },
        )
        SPLTextField(
            formFieldType = FormFieldType.CVV(true),
            sdk = sdk,
            label = "CVV",
            value = cvvValue,
            onChange = { cvvValue = it },
        )

        Button(onClick = {
            coroutineScope.launch {
                val result = sdk.createCreditCard(
                    formFields = listOf(
                        FormFieldType.CARD(true),
                        FormFieldType.EXPIRY_DATE(true),
                        FormFieldType.CVV(true),
                    ),
                    additionalFields = mapOf(
                        AdditionalField.FULL_NAME to "Jane Doe",
                    ),
                )
                when (result) {
                    is PaymentProcessingResult.Processing -> {
                        // Validation passed, request sent.
                        // Final result arrives on sdk.paymentResultFlow.
                    }
                    is PaymentProcessingResult.ValidationFailed -> {
                        // result.invalidFields lists which fields failed
                    }
                }
            }
        }) {
            Text("Pay")
        }
    }
}
```

**Java callers**: Use `HostedFieldsJavaHelper` to mount a default card form inside a `ComposeView`.

See the [Custom Payment Forms guide](../custom-payment-forms.md) for the full field reference, validation patterns, and bank account fields.

### Understanding the "no raw card data" change

If you used the old SDK's headless `SpreedlyClient` API, you may have constructed card data directly:

```java
// OLD SDK -- merchant code touches raw card data
CreditCardInfo info = new CreditCardInfo(
    "Full Name",
    new SpreedlySecureOpaqueString("4111111111111111"),
    new SpreedlySecureOpaqueString("432"),
    2025, 12
);
client.createCreditCardPaymentMethod(info, null, null).subscribe(...);
```

The new SDK has no equivalent to this pattern. Card data must flow through `SPLTextField` composables or `sdk.callbacks`, which populate internal encrypted state. You then call `sdk.createCreditCard()` or `sdk.createPaymentMethod()` to trigger tokenization from that internal state.

For the lower-level `createPaymentMethod()` overload (returns `Result<PaymentMethodResponse, SpreedlyNetworkError>` directly), card state must still have been populated via callbacks or SPL fields first.

---

## Step 5: Migrate result handling

All integration tiers (express, custom, headless) deliver results through the same `SharedFlow`.

### Old SDK (Express) -- `onActivityResult`

```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
        String token = data.getStringExtra(ExpressBuilder.EXTRA_PAYMENT_METHOD_TOKEN);
        // Send token to backend
    }
}
```

### Old SDK (headless) -- RxJava `subscribe()`

```java
client.createCreditCardPaymentMethod(info, null, null)
    .subscribe(new SingleObserver<TransactionResult<PaymentMethodResult>>() {
        @Override
        public void onSubscribe(Disposable d) {}

        @Override
        public void onSuccess(TransactionResult<PaymentMethodResult> trans) {
            if (trans.succeeded) {
                String token = trans.result.token;
                // Send token to backend
            } else {
                String error = trans.message;
                // Handle error
            }
        }

        @Override
        public void onError(Throwable e) {
            // Network or unexpected error
        }
    });
```

### New SDK -- `paymentResultFlow`

```kotlin
lifecycleScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                val token = result.token
                // Send token to your backend to complete the transaction
            }
            is PaymentResult.Failed -> {
                handleError(result)
            }
            is PaymentResult.Canceled -> {
                // User dismissed the payment sheet
            }
            PaymentResult.Initial -> {
                // Waiting for payment
            }
        }
    }
}
```

Subscribe to `paymentResultFlow` **before** calling `sdk.expressCheckout()` or `sdk.createCreditCard()` to avoid missing emissions.

---

## Step 6: Migrate error handling

### Old SDK

```java
if (trans.succeeded) {
    // success
} else {
    String errorMessage = trans.message;
    // single error string, no categorization
}
```

### New SDK

`PaymentResult.Failed` provides structured error information:

```kotlin
is PaymentResult.Failed -> {
    when (result.errorType) {
        PaymentResult.Failed.ErrorType.API_ERROR -> {
            // Specific API error with categorization
            when (result.apiError) {
                SpreedlyApiError.VALIDATION_ERROR -> {
                    // Field-level validation errors available
                    result.validationErrors.forEach { error ->
                        // error.fieldName, error.errorMessage
                    }
                }
                SpreedlyApiError.ACCOUNT_INACTIVE -> { /* Test env with real card */ }
                SpreedlyApiError.UNAUTHORIZED -> { /* Auth issue */ }
                SpreedlyApiError.RATE_LIMITED -> { /* Back off and retry */ }
                else -> { /* result.getDescription() for user message */ }
            }
        }
        PaymentResult.Failed.ErrorType.NETWORK_ERROR -> {
            // Connection, timeout, IO errors
        }
        PaymentResult.Failed.ErrorType.UNKNOWN_ERROR -> {
            // Unexpected -- result.originalError has the Throwable
        }
    }
}
```

Key properties on `PaymentResult.Failed`:
- `errorType` -- `API_ERROR`, `NETWORK_ERROR`, or `UNKNOWN_ERROR`
- `apiError: SpreedlyApiError?` -- categorized API error type
- `validationErrors: List<ValidationError>` -- field-level errors with `fieldName`, `errorKey`, `errorMessage`
- `message: String?` -- primary error message
- `statusCode: Int?` -- HTTP status code
- `getDescription()` -- user-friendly error string
- `hasValidationErrors()` -- whether field-level errors are present
- `getValidationErrors(fieldName)` -- errors for a specific field

For recache results (`Result<PaymentMethodResponse, SpreedlyNetworkError>`), use `SpreedlyErrorMessages.getUserFriendlyMessage(error)` to get a display-ready string.

See the [Error Handling guide](../error-handling.md) for the full list of `SpreedlyApiError` values, field-specific error mapping, retry logic, and user-friendly message patterns.

---

## Java interop

The old SDK was Java. The new SDK is Kotlin-first with Jetpack Compose; most payment modules provide a Java helper class (`stripe-radar` uses the `SpreedlyStripeRadar` Kotlin API instead):

| Module | Java Helper | Purpose |
|--------|-------------|---------|
| paymentsheet | `PaymentSheetJavaHelper` | Mount `SpreedlyBottomSheet` in a `ComposeView` |
| hostedfields | `HostedFieldsJavaHelper` | Mount default card form in a `ComposeView` |
| paymentsheet (recache) | `RecacheJavaHelper` | Recache UI and `RecacheConfig` creation |
| threeds | `ThreeDSJavaHelper` | 3DS challenge UI |
| braintree | `BraintreeAPMJavaHelper` | Braintree APM launch |
| stripe | `StripeAPMJavaHelper` | Stripe APM launch |
| payments-core (offsite) | `OffsitePaymentJavaHelper` | Offsite payment launch |

These helpers bridge between Java Activities/Fragments and the Compose-based SDK. You still need to add `ComposeView` elements to your XML layouts. Over time, migrating to Kotlin and Compose directly is recommended for the best experience.

---

## New capabilities

The Checkout SDK includes features not available in the old SDK or WebView approach:

| Feature | Guide |
|---------|-------|
| ACH bank account tokenization | [ACH Bank Account](../ach-bank-account.md) |
| 3D Secure (Forter global) | [3DS Global](../3ds-global.md) |
| 3D Secure (gateway-specific) | [3DS Gateway-Specific](../3ds-gateway-specific.md) |
| Offsite payments (PayPal, Pix, Boleto, OXXO, NuPay) | [Offsite Payments](../offsite-payments.md) |
| Stripe APM (iDEAL, Bancontact, EPS, P24, SEPA) and PaymentSheet appearance | [Stripe APM](../stripe-apm.md) |
| Braintree APM (PayPal, Venmo) | [Braintree APM](../braintree-apm.md) |
| CVV recaching for saved cards | [Recaching](../recaching.md) |
| Stripe Radar fraud sessions | [Stripe Radar](../stripe-radar.md) |
| Theming and dark mode | [Theme and Styling](../theme-and-styling.md) |
| Screen capture protection | [Security](../security.md) |
| Built-in telemetry (Datadog) | [Datadog Integration](../../development/DATADOG_INTEGRATION.md) |

For gateway-specific 3DS timing, Braintree behavior, and `/complete` handling, see [3DS Gateway-Specific](../3ds-gateway-specific.md).

---

## Concept mapping reference

Quick-reference table for translating between old and new APIs.

### Path A: Old native SDK to Checkout SDK

| Old SDK | Checkout SDK | Notes |
|---------|-------------|-------|
| `SpreedlyClient` | `Spreedly` | `Spreedly()` constructor, then `sdk.init(SpreedlySDKInitOptions(...))` |
| `SpreedlyClient.newInstance(key, secret, test)` | `Spreedly()` + `sdk.init(...)` | Secret no longer on device |
| `CreditCardInfo(name, number, cvv, year, month)` | No equivalent | Card data flows through `SPLTextField` / `sdk.callbacks` |
| `SpreedlySecureOpaqueString` | Not needed | `SPLTextField` handles encryption internally |
| `TransactionResult<PaymentMethodResult>` | `PaymentResult` sealed class | `Completed`, `Failed`, `Canceled`, `Initial` |
| `trans.succeeded` / `trans.result.token` | `PaymentResult.Completed.token` | |
| `trans.message` | `PaymentResult.Failed.message` / `.getDescription()` | Structured error types available |
| `ExpressBuilder` | `SpreedlyBottomSheet` composable | Or `PaymentSheetJavaHelper` for Java |
| `ExpressBuilder.showDialog(...)` | `sdk.expressCheckout()` | No params, returns Unit |
| `EXTRA_PAYMENT_METHOD_TOKEN` | `PaymentResult.Completed.token` | Via `paymentResultFlow` |
| `PaymentOptions` | `SpreedlyBottomSheet` parameters + `PaymentSheetConfig` | |
| `SecureForm` | `SPLTextField` composables + `sdk.callbacks` | Or `HostedFieldsJavaHelper` |
| `SecureCreditCardField` | `SPLTextField(formFieldType = FormFieldType.CARD(true), sdk = sdk)` | |
| `SecureTextField` (CVV) | `SPLTextField(formFieldType = FormFieldType.CVV(true), sdk = sdk)` | |
| `SecureExpirationDate` | `SPLTextField(formFieldType = FormFieldType.EXPIRY_DATE(true))` | Or separate `MONTH()` / `YEAR()` |
| `createCreditCardPaymentMethod()` (RxJava) | `sdk.createCreditCard(formFields, ...)` (suspend) | Returns `PaymentProcessingResult`; final result on `paymentResultFlow` |
| `createBankAccountPaymentMethod()` (RxJava) | `sdk.createBankAccount(formFields, ...)` (suspend) | Returns `PaymentProcessingResult`; final result on `paymentResultFlow` |
| `StoredCard` | `SavedCardInfo` | For recache: `lastFourDigits`, `cardType`, `cardholderName` |

### Path B: WebView to Checkout SDK

| WebView (iFrame / Express JS) | Checkout SDK | Notes |
|-------------------------------|-------------|-------|
| `new SpreedlyExpressCheckout(authDetails)` | `SpreedlyBottomSheet(sdk = sdk)` | Compose composable |
| `expressCheckout()` JS | `sdk.expressCheckout()` | |
| `new SpreedlyHostedFields(authDetails)` | `SPLTextField` composables | One per field type |
| `inAppElements()` JS | Not needed | `SPLTextField` renders natively |
| `tokenGenerated` JS event | `PaymentResult.Completed` on `sdk.paymentResultFlow` | |
| `paymentMethodFailed` JS event | `PaymentResult.Failed` on `sdk.paymentResultFlow` | |
| `addJavascriptInterface` bridge | Not needed | Native Kotlin API |
| `WebView` + CSP configuration | Not needed | Native UI, no iframes |

---

## Public API inventory (merchant-facing)

Complete checklist of SDK APIs available to merchants. Use this to confirm you've wired everything your integration needs.

### SDK-level (`Spreedly` instance)

| API | Purpose |
|-----|---------|
| `sdk.init(SpreedlySDKInitOptions(…))` | Configure signed auth for the session |
| `sdk.isInitialized` | `true` after `init` succeeds — not proof of credentials alone |
| `sdk.expressCheckout()` | Show the pre-built payment bottom sheet |
| `sdk.createCreditCard(formFields, metadata, additionalFields)` | Tokenize card (suspend) — final result on `paymentResultFlow` |
| `sdk.createBankAccount(formFields, metadata, additionalFields)` | Tokenize bank account (suspend) |
| `sdk.createPaymentMethod(formFields, metadata, additionalFields)` | Generic tokenize (suspend) |
| `sdk.recachePaymentMethod(token, config)` | CVV recache — result on `recacheResultFlow` and suspend return |
| `sdk.paymentResultFlow` | `SharedFlow<PaymentResult>` — tokenization, APM, offsite results |
| `sdk.recacheResultFlow` | `SharedFlow<Result<PaymentMethodResponse, SpreedlyNetworkError>>` — recache only (not mirrored on `paymentResultFlow`) |
| `sdk.threeDSChallengeResultFlow` | `SharedFlow<ThreeDSChallengeResult>` — 3DS challenge outcomes |
| `sdk.hasValidationError(FormFieldType)` | Per-field pre-submit check |
| `sdk.areAllFieldsValid(List<FormFieldType>)` | Aggregate validation gate (`@MainThread`) |
| `sdk.getRegisteredFieldCount()` | Count of mounted `SPLTextField` instances — use for pay-button gating (1.1.0+) |
| `sdk.setNumberFormat(CardNumberFormat)` | Set PAN layout: `PRETTY` / `PLAIN` / `MASKED` — coupled CVV |
| `sdk.setNumberFormat(type: String)` | Iframe-style string aliases: `"prettyFormat"` / `"plainFormat"` / `"maskedFormat"` |
| `sdk.toggleMask()` | Toggle plain ↔ masked (PAN + CVV coupled; main thread) |
| `sdk.hostedCardDisplayState` | Read-only `State<HostedCardDisplayState>` — observe `cardNumberFormat`, `panMasked`, `cvvDisplayMasked` |
| `sdk.resetPaymentState()` | Full reset: form values, validation, scheme context, and `hostedCardDisplayState` defaults |
| `sdk.resetPaymentFormPreservingDisplayConfig()` | Form-only reset: clears values + validation but keeps mask/format (called automatically on `createCreditCard` success) |
| `sdk.preservePaymentStateOnNextShow()` | Skip the next bottom-sheet form reset on open (use for config change / rotation) |
| `sdk.shouldPreserveState()` | Consume one-shot preserve flag (advanced; sheet uses internally) |
| `sdk.setParam(ValidationParameter, Boolean)` | Set validation behavior flags (see table below) |
| `sdk.callbacks` | Field change handlers for custom forms: `onNameOnCardChange`, `onFirstNameChange`, `onLastNameChange`, etc. |

#### `ValidationParameter` values

| Parameter | Default | Effect |
|-----------|---------|--------|
| `ALLOW_BLANK_NAME` | `false` | Skip name field validation |
| `ALLOW_BLANK_DATE` | `false` | Skip expiry date validation |
| `ALLOW_EXPIRED_DATE` | `false` | Accept past expiry dates |
| `ALLOW_INTERNATIONAL_ZIP_CODES` | `true` | `true` = international postal codes; `false` = US numeric ZIP only |

### Headless `SPLTextField` (Compose)

| Parameter / Callback | Purpose |
|----------------------|---------|
| `formFieldType` | Which field: `CARD(required)`, `CVV(required)`, `EXPIRY_DATE(required)`, `MONTH()`, `YEAR()`, etc. |
| `sdk` | **Required** for `CARD` and `CVV` — display follows `sdk.hostedCardDisplayState`. Optional for other field types. |
| `onChange` | Value updates (AES-encrypted for CARD/CVV/ACCOUNT_NUMBER; raw text for others) |
| `onFieldStateChange` | `HostedFieldState` snapshots — scheme, `iin`, digit lengths, validity, focus, mask state (1.1.0+) |
| `onValidationChange` | Per-field `Boolean` validity |
| `onFocus` | Field gained focus |
| `onFocusChanged` | Focus enter (`true`) / blur (`false`) (1.1.0+) |
| `shouldFocus` | Programmatic focus request |
| `trailingIcon` | Custom composable for CARD brand artwork (e.g., card logo drawable) |
| `forceMaskOnLifecycleStop` | Lifecycle mask overlay on `ON_STOP` (default `true`) |
| `enableAutofill` | OS autofill hints (default `true`; set `false` for legacy `toggleAutoComplete` off) |
| `imeAction` / `onImeAction` | IME "Next" / "Done" and keyboard submit |
| `config: CustomFieldsConfig` | Per-field visual overrides (colors, borders). On SDK resolution paths, `borderRadius` drives field corners — `fieldShape` is synced from `borderRadius` (set radius, not a mismatched shape). Use `resolveEffectiveCustomFieldsConfig()` for custom ACH layouts mixing `AppTextField` with `SPLTextField`. See [ACH Bank Account](../ach-bank-account.md#theme-resolution). |

#### `HostedFieldState` properties (on `onFieldStateChange`)

| Property | Type | Notes |
|----------|------|-------|
| `fieldType` | `FormFieldType` | Which field emitted |
| `eventType` | `HostedFieldEventType` | `INPUT`, `FOCUS`, `BLUR`, `VALIDATION`, `PAN_MASK_CHANGED` |
| `isFocused` | `Boolean` | |
| `isValid` | `Boolean` | Current validation (includes combined expiry) |
| `isEmpty` | `Boolean` | |
| `cardScheme` | `CardScheme?` | Detected brand (CARD fields) |
| `iin` | `String?` | 6–8 digit issuer prefix (CARD; `null` when <6 digits; omitted from `toString()`) |
| `numberLength` | `Int?` | Digit count for CARD |
| `cvvLength` | `Int?` | Digit count for CVV |
| `isPanMasked` | `Boolean` | Whether PAN is visually masked |
| `panDisplayFormat` | `CardNumberFormat?` | Snapshot of global format at emission (CARD only) |
| `panDisplayPolicyMasked` | `Boolean?` | Snapshot of global `panMasked` policy (CARD only) |

### Express Checkout (`SpreedlyBottomSheet` / `PaymentSheet`)

| Parameter | Purpose |
|-----------|---------|
| `sdk` | SDK instance (required) |
| `nameFieldDisplayMode` | `SINGLE_FIELD` / `SEPARATE_FIELDS` |
| `yearFormat` | Expiry year: 2-digit or 4-digit |
| `additionalFields` | Optional address fields (`ConfigurableFormField` list) |
| `showSavePaymentCheckbox` / `savePaymentCheckboxLabel` | Save card opt-in UI |
| `allowBlankName` / `allowBlankDate` / `allowExpiredDate` | Validation relaxation |
| `config: PaymentSheetConfig` | Colors and legacy display (deprecated for `displayConfig`) |
| `displayConfig: PaymentSheetDisplayConfig` | `enableAutofill`, `cardNumberFormat` (seeds sheet on open) |
| `coreFieldLabels: PaymentSheetCoreFieldLabels?` | Core card field labels/placeholders (iOS `DropInCoreFieldLabels` parity) |

**Not available on Express** (use headless `SPLTextField` if needed):
- `onFieldStateChange` per-field callbacks
- Custom `trailingIcon` on CARD
- Per-field `onFocusChanged`
- Granular `onInputLength` / digit counts during typing

### `EmailValidator`

| API | Purpose |
|-----|---------|
| `EmailValidator.isValid(String)` | JVM-safe email format check (not auto-wired into fields) |

---

## Legacy iFrame (JavaScript) 1:1 mapping

If you integrated **Spreedly iFrame v1** (`iframe-v1.min.js`) inside a WebView or a mobile browser, use this table to find the Checkout **Android** pattern. Official legacy references: [iFrame UI](https://developer.spreedly.com/docs/iframe-ui), [iFrame events](https://developer.spreedly.com/docs/iframe-events).

**Status** in the third column:

- **Parity** — Supported with an Android-first API or pattern.
- **Platform** — Same outcome via Android/Compose idioms (not a JS-shaped API).
- **Gap** — No equivalent today; read **Notes** for what to do instead.
- **N/A** — Web-only or not meaningful on Android.

### Card-brand display during typing

Legacy `fieldEvent` / `inputProperties.cardType` let merchants swap **icons** and drive BIN UX while typing.

**Android today**

- Use **`SPLTextField.onFieldStateChange`** with **`com.spreedly.hostedfields.models.HostedFieldState`** — `cardScheme` is populated for `FormFieldType.CARD` on **`INPUT`**, **`VALIDATION`**, and focus-related events (typed `CardScheme`; map to your drawable assets).
- **`HostedFieldState.iin`** — merchant-safe issuer prefix (6 or 8 digits per scheme; `null` when fewer than six digits on the card field). Use for BIN-level UX while typing; it is omitted from `HostedFieldState.toString()` for logging safety.
- The SDK **does not** ship drawable assets for brands. By default, `CardScheme.name` is shown as **Text** in the trailing slot when the scheme is known. To replace that with an icon, pass **`trailingIcon = { scheme -> … }`** on **`SPLTextField`** (CARD fields only; `null` keeps the default text).
- `CardNumberContext` exists for **internal** CVV-length coupling and is **not** a supported merchant subscription API (`payments-core/.../CardNumberContext.kt`).

### Safe field-state observability

| Signal | Merchant-visible? | Notes |
|--------|--------------------|--------|
| `SPLTextField.onFieldStateChange(HostedFieldState)?` | Yes (**1.1.0+**) | **Preferred** iframe-style channel: `INPUT`, `FOCUS`, `BLUR`, `VALIDATION`, `PAN_MASK_CHANGED`; includes `cardScheme`, `numberLength` / `cvvLength` (**digit counts only**), `iin` (**issuer prefix**, 1.1.0+), `isValid`, `isEmpty`, `isFocused`, `isPanMasked`. |
| `SPLTextField.onChange` | Yes, but value is **AES-encrypted** only for `CARD`, `CVV`, and `ACCOUNT_NUMBER` (`FormFieldType.shouldEncrypt()`); other types pass **raw** text | Do **not** log ciphertext or treat it as display digits; use `onFieldStateChange` for lengths, not ciphertext parsing. |
| `onValidationChange(Boolean)?` | Yes | Per-field validity; also reflected in `HostedFieldState.isValid` on **`VALIDATION`** (and other events). |
| `onFocus` | Yes | Fires when the field **gains** focus. |
| `onFocusChanged(Boolean)?` | Yes (**1.1.0+**) | `true`/`false` for focus and blur (blur is `false`). |
| `hasValidationError(FormFieldType)` | Yes | Pre-submit validation helper on `Spreedly`. |
| `areAllFieldsValid(List<FormFieldType>)` | Yes | Aggregate helper: `true` when none of the listed fields report `hasValidationError`. Must run on the **main thread** (`@MainThread`); avoid calling every frame during recomposition because **CVV** checks decrypt ciphertext — prefer `onValidationChange` for live UI. |
| `com.spreedly.validation.EmailValidator` | Yes | JVM-safe `isValid(String)` for merchant-collected email before tokenize (not auto-wired into `SPLTextField`). |
| `PaymentProcessingResult.ValidationFailed` | Yes | Submit-time invalid field list. |
| `SpreedlyEvent.HostedFieldInteraction` (Datadog) | **Internal telemetry** | **Not** a supported merchant integration API unless product explicitly documents it. |

### HostedFieldState — Kotlin samples (Compose)

Merchant-owned **icons** (legacy iframe used `cardType` metadata; same pattern on Android):

```kotlin
var cardBrandIcon by remember { mutableStateOf<CardScheme?>(null) }

SPLTextField(
    formFieldType = FormFieldType.CARD(true),
    sdk = sdk,
    value = cardNumber,
    onChange = { cardNumber = it },
    onFieldStateChange = { state ->
        if (state.fieldType is FormFieldType.CARD) {
            cardBrandIcon = state.cardScheme
        }
    },
)
// Map cardBrandIcon to painterResource(R.drawable.ic_visa) etc. — do not log state if it could
// ever include sensitive fields; HostedFieldState is PAN-safe by design.
```

Safe **length** gating (do not parse ciphertext from `onChange`):

```kotlin
var digitCount by remember { mutableStateOf(0) }

SPLTextField(
    formFieldType = FormFieldType.CARD(true),
    sdk = sdk,
    value = cardNumber,
    onChange = { cardNumber = it },
    onFieldStateChange = { state ->
        state.numberLength?.let { digitCount = it }
    },
)
```

Pre-submit **block** using `HostedFieldState.isValid` (mirrors `onValidationChange`; use either or both):

```kotlin
var hostedCardValid by remember { mutableStateOf(false) }

SPLTextField(
    formFieldType = FormFieldType.CARD(true),
    sdk = sdk,
    value = cardNumber,
    onChange = { cardNumber = it },
    onFieldStateChange = { state ->
        if (state.eventType == HostedFieldEventType.VALIDATION ||
            state.eventType == HostedFieldEventType.INPUT
        ) {
            hostedCardValid = state.isValid
        }
    },
)
// Gate checkout: if (!hostedCardValid || sdk.hasValidationError(...)) return
```

**Java** — use non-null listener overloads so overload resolution is unambiguous (`setupContent(composeView, sdk, listener)` or `setupContent(composeView, sdk, config, listener)`):

```java
HostedFieldsJavaHelper.setupContent(composeView, sdk, state -> {
    CardScheme scheme = state.getCardScheme();
    Integer len = state.getNumberLength();
    // Drive UI from getters; do not log raw payment data.
});
```

### External mask toggle (iframe / iOS pattern)

The iframe has **no eye icon** inside the number or CVV iframes. Mask/reveal is **merchant-owned UI** outside the fields, calling **`Spreedly.setNumberFormat(...)`** or **`Spreedly.toggleMask()`** on the main thread. The Spreedly example app uses this pattern (not the in-field SDK toggle).

> **PRETTY is not iframe `prettyFormat`.** On the web, `prettyFormat` keeps grouped digits visible when the field loses focus. On Android, `CardNumberFormat.PRETTY` applies **blur masking** (last-four style) when unfocused while `panMasked` is true. For digits that stay visible like iframe `prettyFormat`, use `CardNumberFormat.PLAIN`. For full bullets on every digit, use `MASKED` or `toggleMask()`.

> **Multiple CARD fields:** One **`Spreedly.hostedCardDisplayState`** applies to the whole SDK instance. Every **CARD** and **CVV** `SPLTextField` that receives the same **`sdk`** instance shares that snapshot (mask/format), unlike separate iframes with per-embed state.

**Internal:** `SpreedlyUIController.setHostedCardDisplayState` is SDK-internal and unused by merchant UIs — use **`Spreedly.setNumberFormat`** / **`toggleMask`** and collect **`hostedCardDisplayState`** instead.

**Required wiring** — pass the same **`sdk`** on **CARD** and **CVV** fields (display follows **`sdk.hostedCardDisplayState`** automatically):

```kotlin
SPLTextField(
    formFieldType = FormFieldType.CARD(true),
    sdk = sdk,
    label = "Card Number",
    value = cardNumber,
    onChange = { cardNumber = it },
)

SPLTextField(
    formFieldType = FormFieldType.CVV(true),
    sdk = sdk,
    label = "CVV",
    value = cvv,
    onChange = { cvv = it },
)

// Merchant UI: all three iframe formats (see sample app MerchantMaskToggleBar)
sdk.setNumberFormat(CardNumberFormat.PRETTY)   // prettyFormat — grouped digits
sdk.setNumberFormat(CardNumberFormat.PLAIN)    // plainFormat — reveal PAN + CVV
sdk.setNumberFormat(CardNumberFormat.MASKED)   // maskedFormat — full * on every digit

// iframe toggleMask() — coupled PAN + CVV; from PRETTY, first call → MASKED; second → PLAIN
Button(onClick = { sdk.toggleMask() }) { Text("toggleMask()") }
```

**Java `HostedFieldState`:** positional constructor includes **`isPanMasked`** as the **9th** parameter; handle **`PAN_MASK_CHANGED`** in `switch` statements.

**`SPLTextField.forceMaskOnLifecycleStop`** (default `true`) — Android lifecycle overlay on `ON_STOP` when PAN was revealed; does not replace `toggleMask()` / controller state. See CHANGELOG.

### Initialization and configuration

| If you previously used… | Use this in Android SDK… | Notes | Status |
|---------------------------|---------------------------|-------|--------|
| `Spreedly.init(environmentKey, { nonce, signature, certificateToken, timestamp, numberEl, cvvEl, … })` | `Spreedly()` + `sdk.init(SpreedlySDKInitOptions(…))` | No DOM ids — add `SPLTextField` composables instead of iframe containers. | Platform |
| `Spreedly.on('ready', fn)` | Run field setup **after** `init` succeeds and your composable is active | There is no global `ready` callback; `sdk.isInitialized` reflects completion. | Platform |
| `Spreedly.setFieldType('number'\|'cvv', 'text'\|'tel'\|'number')` | Default keyboards from `SPLTextField` / `FormFieldType` | Card/CVV use digit-safe keyboards internally. | Platform |
| `Spreedly.setLabel(field, text)` | `label = "…"` on `SPLTextField` | Label drives visible text and accessibility on native fields. | Parity |
| `Spreedly.setTitle(field, text)` | Optional helper text / custom semantics | No iframe `title` attribute — use native patterns. | N/A — Android native |
| `Spreedly.setPlaceholder(field, text)` | Hint/placeholder via field config + `placeholderColor` in theme (`CustomFieldsConfig` / `SpreedlyColors`) | See [Theme and Styling](../theme-and-styling.md) and [CHANGELOG](../../CHANGELOG.md) for `placeholderColor`. | Parity |
| `Spreedly.setStyle(field, css)` / `setStyle('placeholder', css)` | `SpreedlyTheme` / `MaterialTheme` + `CustomFieldsConfig` | Map CSS to Compose tokens; placeholder color has first-class support. | Platform |
| `Spreedly.setNumberFormat('prettyFormat' \| 'plainFormat' \| 'maskedFormat')` | Prefer **`Spreedly.setNumberFormat(CardNumberFormat)`** (or the string overload above). **`PaymentSheetDisplayConfig.cardNumberFormat`** on **`SpreedlyBottomSheet`** / **`PaymentSheet`** (or deprecated **`PaymentSheetConfig.cardNumberFormat`** when **`displayConfig`** is null) seeds the controller when it is still default after the sheet opens. Headless **`SPLTextField`**: pass **`sdk`** on CARD/CVV — display follows **`sdk.hostedCardDisplayState`** | See **PRETTY vs iframe prettyFormat** above. | Partial |
| `Spreedly.toggleMask()` | **`Spreedly.toggleMask()`** on the main thread (coupled CARD + CVV when both fields use the same **`sdk`**). Prefer merchant UI outside fields (see **External mask toggle** above). From default **`PRETTY`**, first **`toggleMask()`** applies **`MASKED`**; second reveals **`PLAIN`**. | | Partial |
| `Spreedly.transferFocus(field)` | `shouldFocus = true` on the target `SPLTextField` | See [Focus management](../focus-management.md). | Parity |
| `Spreedly.toggleAutoComplete()` | **`PaymentSheetDisplayConfig(enableAutofill = false)`** on **`SpreedlyBottomSheet`** / **`PaymentSheet`** (or deprecated **`PaymentSheetConfig(enableAutofill = false)`** when **`displayConfig`** is null) / `SPLTextField(..., enableAutofill = false)` / `HostedFieldsJavaHelper.setupContent(..., enableAutofill = false)` | Default is on (`true`). Set `false` to suppress autofill hints on hosted fields. | N/A |
| `Spreedly.setRequiredAttribute(field)` | `FormFieldType.CARD(true)`, `CVV(true)`, etc. | `required` flags on `FormFieldType`. | Parity |
| `Spreedly.setParam(name, value)` | `sdk.setParam(ValidationParameter.…, value)` | Typed `ValidationParameter` enum: `ALLOW_BLANK_NAME`, `ALLOW_BLANK_DATE`, `ALLOW_EXPIRED_DATE`, `ALLOW_INTERNATIONAL_ZIP_CODES`. | Parity |

### Validation and field state

| If you previously used… | Use this in Android SDK… | Notes | Status |
|---------------------------|---------------------------|-------|--------|
| `Spreedly.validate()` + `validation` event (`inputProperties`) | `sdk.hasValidationError` / `sdk.areAllFieldsValid` before submit; `onValidationChange` on each `SPLTextField`; `createCreditCard` → `PaymentProcessingResult.ValidationFailed` | Per-field booleans + optional aggregate; **no** single `inputProperties` object. Use `HostedFieldState` for typing-time scheme, lengths, and `iin`. | Parity |
| `fieldEvent` with `input` + `inputProperties.cardType` / `validNumber` / `validCvv` | `onFieldStateChange { HostedFieldState }` on `SPLTextField` (plus `onChange` / `onValidationChange` if you split concerns) | **`HostedFieldState`** bundles scheme, digit **lengths**, `iin`, validity, focus, mask state, and event type — **no** separate `luhnValid` bit. Built-in **icons** are still merchant assets. | Platform |
| `inputProperties.iin` (6–8 digits while typing) | `HostedFieldState.iin` on CARD field snapshots | Merchant-safe prefix only (not full PAN); `null` when fewer than six digits. Omitted from `toString()`. | Parity |
| `inputProperties.numberLength` / `cvvLength` | `HostedFieldState.numberLength` / `cvvLength` (**int digit counts**, from `handleValueChange` pre-encrypt) | Do **not** confuse with ciphertext in `onChange`. | Platform |
| `fieldEvent` `focus` / `blur` | `HostedFieldState` with `HostedFieldEventType.FOCUS` / `BLUR`, or `onFocus` / `onFocusChanged` | Same semantics; blur-driven errors still use “user edited” + forced validation. | Platform |
| `fieldEvent` `mouseover` / `mouseout` | (skip on phone) | Desktop-only hover. | N/A |
| `inputProperties.luhnValid` | Use `HostedFieldState.isValid` | No separate Luhn bit; `isValid` covers format + Luhn + length. | Platform |
| `setValue('number' \| 'cvv', …)` | Not exposed | Sensitive fields accept user input and autofill only — no programmatic `setValue`. | Gap |
| Enter / Tab / Escape from iframe | `ImeAction` + `onImeAction`; **`FocusRequester`** / `focusProperties` / `Modifier.focusOrder` between fields; `onFocusChanged` | Tab order across fields is **merchant-owned** in Compose — wire explicit focus chains (e.g. card → expiry → CVV) and test with a hardware keyboard. Escape is not a mobile soft-keyboard concept. | Platform |

### Tokenization, reset, and listeners

| If you previously used… | Use this in Android SDK… | Notes | Status |
|---------------------------|---------------------------|-------|--------|
| `Spreedly.tokenizeCreditCard()` | `sdk.createCreditCard(…)` / `createPaymentMethod(…)` + collect `sdk.paymentResultFlow` | Optional **`eligibleForCardUpdater`** argument on `createCreditCard` / `createPaymentMethod` maps to JSON `eligible_for_card_updater` when non-null; omitted when `null` (matches API `encodeDefaults = false`). Alternatively set `"eligible_for_card_updater"` in **`metadata`** as `"true"` / `"false"` if you prefer the metadata-only path. | Parity |
| `Spreedly.reload()` | `sdk.resetPaymentState()` clears form values + validation display + scheme context + **`hostedCardDisplayState`** defaults; fetch **new** signed auth and call **`sdk.init(SpreedlySDKInitOptions(...))`** again for a fresh signing bundle. | Unlike iframe `reload()` (DOM remount), Android keeps one `Spreedly` instance — **init** rotates signing; `resetPaymentState` alone does not. Successful **`createCreditCard`** calls **`resetPaymentFormPreservingDisplayConfig()`** (fields only, mask/format kept — iOS parity). **`createPaymentMethod`** does not auto-reset. Use **`resetPaymentState()`** when you need display wiped. For rotation / config changes on express sheets, call **`preservePaymentStateOnNextShow()`** before the sheet re-opens to skip the next form reset. | Platform |
| `Spreedly.removeHandlers()` / `off` | Cancel coroutine collectors (`viewModelScope` / `lifecycleScope`) | No global event bus on Android. | Platform |

### Recache, errors, 3DS, telemetry

| If you previously used… | Use this in Android SDK… | Notes | Status |
|---------------------------|---------------------------|-------|--------|
| `recacheReady` | Add `SpreedlyRecacheUI` to composition + `init` before calling `recachePaymentMethod` | There is **no** `recacheReady` event name — readiness is structural. | Platform |
| `recache` / `recache` success | `recachePaymentMethod`, `recacheResultFlow`, and suspend `Result` only (not `paymentResultFlow`) | See [Recaching](../recaching.md). | Parity |
| `errors` (array of `{ attribute, key, message }`) | `PaymentResult.Failed` + `validationErrors` / `SpreedlyNetworkError` | Map `errorKey` to UI. See [Error handling](../error-handling.md). | Parity |
| `paymentMethod` | `PaymentResult.Completed` on `paymentResultFlow` | Tokenization and other payment flows only — not CVV recache. Recache uses `recacheResultFlow` / suspend return. | Parity |
| `3ds:status` | `threeDSChallengeResultFlow` + gateway-specific APIs | Typed results instead of one string event. See [3DS Global](../3ds-global.md). | Platform |
| `fraud:token` | Stripe Radar (`checkout-stripe-radar`) / Braintree device data | Different fraud integrations; not a drop-in `fraud:token`. | Gap |
| `consoleError` | (no merchant hook) | Use Spreedly support + your own crash reporting for **app** code. | N/A |

### Release notes and docs

| If you previously used… | Use this in Android SDK… | Notes | Status |
|---------------------------|---------------------------|-------|--------|
| iframe `feed.xml` / web changelog habits | [CHANGELOG](../../CHANGELOG.md) + GitHub Packages release | Optional RSS/XML feed is **not** published from this repo today. | Gap |
| TypeScript / `@types` | Kotlin sources + Dokka (`./gradlew generateApiDocs`) | Types ship with the library; use IDE or generated HTML. | Platform |

---

## Verification checklist

- [ ] Build succeeds: `./gradlew assembleRelease`
- [ ] No old dependency references: search for `com.spreedly:client`, `com.spreedly:express`, `com.spreedly:securewidgets` -- zero results
- [ ] No old Maven repo: search for `raw.githubusercontent.com/spreedly/mobile-sdk-android` -- zero results
- [ ] No WebView payment code remaining (Path B): search for `addJavascriptInterface` and `spreedly` in WebView-related files -- zero results
- [ ] No `SpreedlyClient` references: search for `SpreedlyClient` -- zero results
- [ ] No `CreditCardInfo` or `SpreedlySecureOpaqueString` references -- zero results
- [ ] Auth params fetched from backend, not hardcoded: no API secret in app code
- [ ] ProGuard/R8 rules added (if using custom ProGuard config)
- [ ] End-to-end payment flow works with test card `4111111111111111`
- [ ] Token received and processed on backend

---

## Getting help

- [Testing Guide](../testing-guide.md) -- Test cards, environment setup, flow-by-flow procedures
- [Troubleshooting](../troubleshooting.md) -- Symptom-based debugging across all payment flows
- [Getting Started](../getting-started.md) -- Full setup walkthrough for new integrations
- [Spreedly Support](https://spreedly.com/support/) -- Open a ticket if you need migration assistance
