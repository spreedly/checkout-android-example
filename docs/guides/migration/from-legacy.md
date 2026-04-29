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
| **Language** | Java (old SDK) / JavaScript (WebView) | Kotlin (Java helpers available for every module) |
| **UI** | XML Views (`SecureForm`, `SecureCreditCardField`) / WebView | Jetpack Compose (`SPLTextField`, `SpreedlyBottomSheet`) |
| **Async model** | RxJava `Single` / JS callbacks | Coroutines (`suspend fun`) + `SharedFlow<PaymentResult>` |
| **Authentication** | API secret stored on-device: `SpreedlyClient.newInstance("key", "secret", true)` | Server-generated signed params per session (nonce, signature, certificateToken, timestamp) -- no secret on device |
| **PCI scope** | Merchant code constructs `CreditCardInfo` with raw card data / WebView handles it in JS | Sensitive data flows exclusively through SDK secure components (`SPLTextField` / `sdk.callbacks`) -- never in merchant code |
| **Dependencies** | `com.spreedly:client` / `express` / `securewidgets` | Multi-module: `checkout-payments-core` / `checkout-hostedfields` / `checkout-paymentsheet` + optional `checkout-threeds`, `checkout-braintree-apm`, `checkout-stripe-apm` |
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
        maven { url = uri("https://mobile-sdks.forter.com/android") }
        google()
        mavenCentral()
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    // Core (required)
    implementation("com.spreedly:checkout-payments-core:$spreedlyVersion")
    implementation("com.spreedly:checkout-hostedfields:$spreedlyVersion")
    implementation("com.spreedly:checkout-paymentsheet:$spreedlyVersion")

    // 3DS (add if you need 3D Secure authentication)
    implementation("com.spreedly:checkout-threeds:$spreedlyVersion")

    // Alternative Payment Methods (add as needed)
    implementation("com.spreedly:checkout-braintree-apm:$spreedlyVersion")  // PayPal, Venmo
    implementation("com.spreedly:checkout-stripe-apm:$spreedlyVersion")     // iDEAL, Bancontact, EPS, P24, SEPA
}
```

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
| `SecureCreditCardField` (`@id/spreedly_credit_card_number`) | `SPLTextField(formFieldType = FormFieldType.CARD(true), onChange = { ... })` |
| `SecureTextField` (`@id/spreedly_cvv`) | `SPLTextField(formFieldType = FormFieldType.CVV(true), onChange = { ... })` |
| `SecureExpirationDate` (`@id/spreedly_cc_expiration_date`) | `SPLTextField(formFieldType = FormFieldType.EXPIRY_DATE(true), onChange = { ... })` or separate `MONTH()` / `YEAR()` fields |
| `TextInputLayout` (`@id/spreedly_full_name`) | Standard Compose `TextField` + `sdk.callbacks.onNameOnCardChange(value, isRequired = true)` |
| `TextInputLayout` (`@id/spreedly_first_name`) | Standard Compose `TextField` + `sdk.callbacks.onFirstNameChange(value, isRequired = true)` |
| `TextInputLayout` (`@id/spreedly_last_name`) | Standard Compose `TextField` + `sdk.callbacks.onLastNameChange(value, isRequired = true)` |
| `SecureForm.createCreditCardPaymentMethod()` (RxJava) | `sdk.createCreditCard(formFields, metadata, additionalFields)` (suspend, returns `PaymentProcessingResult`) |
| `SecureForm.createBankAccountPaymentMethod()` (RxJava) | `sdk.createBankAccount(formFields, metadata, additionalFields)` (suspend, returns `PaymentProcessingResult`) |

| WebView (Hosted Fields JS) | New SDK (Compose) |
|---|---|
| `<div id="spreedly-number">` + `inAppElements()` | `SPLTextField(formFieldType = FormFieldType.CARD(true), onChange = { ... })` |
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

The old SDK was Java. The new SDK is Kotlin-first with Jetpack Compose, but every module provides a Java helper class:

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
| Stripe APM (iDEAL, Bancontact, EPS, P24, SEPA) | [Stripe APM](../stripe-apm.md) |
| Braintree APM (PayPal, Venmo) | [Braintree APM](../braintree-apm.md) |
| CVV recaching for saved cards | [Recaching](../recaching.md) |
| Theming and dark mode | [Theme and Styling](../theme-and-styling.md) |
| Screen capture protection | [Security](../security.md) |
| Built-in telemetry (Datadog) | [Datadog Integration](../../development/DATADOG_INTEGRATION.md) |

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
| `SecureCreditCardField` | `SPLTextField(formFieldType = FormFieldType.CARD(true))` | |
| `SecureTextField` (CVV) | `SPLTextField(formFieldType = FormFieldType.CVV(true))` | |
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
