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

Generating backend auth details is not enough on its own. Until **Enable secure tokenization** is
turned on for your Spreedly environment, Create payment method calls from the SDK are not
authenticated.

1. Sign in to [app.spreedly.com](https://app.spreedly.com) and open **Environment settings** for
   the environment matching your `apikeys.properties` `environmentKey`.
2. Under **Security → Enhanced Security**, check **Enable secure tokenization**, then click
   **Save changes**.
3. Under **Payment method submission**, we recommend **iFrame or Spreedly Express** so payment
   methods cannot be created through the direct API.

This setting is per environment. Once enabled, every Create payment method call requires
authentication: SDK requests need valid signed auth details, and direct API calls need HTTP basic
auth. Confirm the sample app (via your backend) sends valid auth details before you save the setting.
Non-secure tokenization will be deprecated on October 31,
2026, with end of life on December 31, 2028. See
[Securing iFrame or Checkout SDK](https://developer.spreedly.com/docs/securing-iframe).

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
