# Getting Started

Integrate the Spreedly Android SDK and process your first payment in under 5 minutes.

## 1. Install

Add the Spreedly Maven repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/spreedly/checkout-android-maven")
            credentials {
                username = "your_github_username"
                password = "your_github_personal_access_token"
            }
        }
        maven { url = uri("https://mobile-sdks.forter.com/android") }
        google()
        mavenCentral()
    }
}
```

Add the SDK dependencies to your `app/build.gradle.kts`:

```kotlin
// Replace $spreedlyVersion with the latest version
// from https://github.com/spreedly/checkout-android-maven/packages
dependencies {
    implementation("com.spreedly:checkout-payments-core:$spreedlyVersion")
    implementation("com.spreedly:checkout-hostedfields:$spreedlyVersion")
    implementation("com.spreedly:checkout-paymentsheet:$spreedlyVersion")
}
```

## 2. Initialize the SDK

Obtain authentication parameters from your backend, then initialize:

```kotlin
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.SpreedlySDKInitOptions

val sdk = Spreedly()

val options = SpreedlySDKInitOptions(
    nonce = authParams.nonce,
    signature = authParams.signature,
    certificateToken = authParams.certificateToken,
    timestamp = authParams.timestamp.toString(),
    environmentKey = "your_environment_key",
    context = applicationContext,
    // sdkPlatform = SdkPlatform.ANDROID,  // default; pass SdkPlatform.REACT_NATIVE for RN bridges
)

sdk.init(options)
```

> **Important**: Create a new SDK instance each time users enter a payment flow. This ensures
> fresh authentication and optimal security.

## 3. Show the Payment Sheet

Launch the Express Checkout payment sheet:

```kotlin
sdk.expressCheckout()
```

This presents a pre-built bottom sheet with card fields, validation, and a save card checkbox.
For configuration options (theming, additional fields, name display, year format), see the
[Express Checkout](express-checkout.md) guide.

Or use Hosted Fields for a custom payment form. See the
[Custom Payment Forms](custom-payment-forms.md) guide.

## 4. Handle the Result

Collect payment results via `paymentResultFlow`:

```kotlin
import com.spreedly.sdk.ui.PaymentResult

lifecycleScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                val token = result.token
                // Send token to your backend to complete the transaction
            }
            is PaymentResult.Failed -> {
                val error = result.message
                // Show error to user
            }
            is PaymentResult.Canceled -> {
                // User dismissed the payment sheet
            }
            PaymentResult.Initial -> { /* Waiting for payment */ }
        }
    }
}
```

## Production Integration Checklist

Use this before shipping to production users. Details live in the linked guides — this section does not repeat install or init steps.

| Item | What to verify | Reference |
|------|----------------|-----------|
| Maven access | GitHub username + PAT with `read:packages` for `checkout-android-maven` | [§ 1. Install](#1-install) above |
| Backend signing | Your server mints **new** signed init params per session / flow | [§ 2. Initialize](#2-initialize-the-sdk) above |
| ProGuard / R8 | Keep rules for `com.spreedly.**` and Ktor if you customize rules | [README — ProGuard / R8](../../README.md#proguard--r8) |
| Errors & UX | Handle `PaymentResult.Failed` and validation cases | [Error Handling](error-handling.md) |
| Security | Use SDK fields; add `SecureScreen` on custom payment screens | [Security](security.md) |
| Privacy & telemetry | Understand Datadog and data handling | [Privacy Policy](privacy-policy.md), [Datadog Integration](../development/DATADOG_INTEGRATION.md) |
| Minimum SDK | Your `minSdk` is at least **26** | [README — Compatibility](../../README.md#compatibility) |

## Next Steps

- **[Error Handling](error-handling.md)** -- Handle all error scenarios
- **[Recaching](recaching.md)** -- Update CVV for saved cards
- **[Offsite Payments](offsite-payments.md)** -- PayPal, Pix, Boleto via Chrome Custom Tabs
- **[Stripe APM](stripe-apm.md)** -- iDEAL, Bancontact, EPS, P24, SEPA
- **[Braintree APM](braintree-apm.md)** -- PayPal and Venmo via Braintree
- **[ACH Bank Account](ach-bank-account.md)** -- Tokenize bank accounts via ACH
- **[3DS Authentication](3ds-global.md)** -- Add 3D Secure to your flow
- **[Theme & Styling](theme-and-styling.md)** -- Customize the SDK's appearance
