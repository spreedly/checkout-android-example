# Spreedly Checkout — Android Example

This sample app demonstrates the [Spreedly Android Checkout SDK](https://github.com/spreedly/checkout-android-sdk) at version **1.5.0** (tag `v1.5.0`).

## Setup

1. Clone this repository
2. Add your GitHub Packages credentials to `~/.gradle/gradle.properties`:

```properties
gpr.usr=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

3. Add your Spreedly environment key and other API keys to `apikeys.properties` (see `apikeys.properties.example`)
4. In [app.spreedly.com](https://app.spreedly.com), open **Environment settings** for the environment you use with this app. Under **Security → Enhanced Security**, check **Enable secure tokenization**, then **Save changes**. Repeat for each environment (for example, staging and production). See [Enable secure tokenization (required)](https://github.com/spreedly/checkout-android-sdk/blob/main/docs/guides/getting-started.md#enable-secure-tokenization-required) in the SDK docs.
5. Open the project in Android Studio and sync Gradle

### Enable secure tokenization (required)

Follow the canonical steps in the SDK docs (dashboard paths, payment-method submission, staging
checks, and platform behavior):
[Enable secure tokenization (required)](https://github.com/spreedly/checkout-android-sdk/blob/main/docs/guides/getting-started.md#enable-secure-tokenization-required).
Enable the setting for each Spreedly environment that matches your `apikeys.properties`
`environmentKey` before you tokenize from this sample.

## Dependencies

All SDK modules are resolved from GitHub Packages:

```kotlin
implementation("com.spreedly:checkout-paymentsheet:1.5.0")
implementation("com.spreedly:checkout-braintree-apm:1.5.0")
implementation("com.spreedly:checkout-stripe-apm:1.5.0")
implementation("com.spreedly:checkout-threeds:1.5.0")
```

## SDK Documentation

- [SDK Repository](https://github.com/spreedly/checkout-android-sdk)
- [Getting Started — secure tokenization](https://github.com/spreedly/checkout-android-sdk/blob/main/docs/guides/getting-started.md#enable-secure-tokenization-required)
- [API Documentation](https://github.com/spreedly/checkout-android-sdk)

## Support

For questions or issues, please contact the Spreedly team or create an issue in the [SDK repository](https://github.com/spreedly/checkout-android-sdk/issues).
