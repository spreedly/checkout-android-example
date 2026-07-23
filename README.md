# Spreedly Checkout — Android Example

This sample app demonstrates the [Spreedly Android Checkout SDK](https://github.com/spreedly/checkout-android-sdk) at version **1.2.0** (tag `v1.2.0`).

## Setup

1. Clone this repository
2. Add your GitHub Packages credentials to `~/.gradle/gradle.properties`:

```properties
gpr.usr=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

3. Add your Spreedly environment key and other API keys to `apikeys.properties` (see `apikeys.properties.example`)
4. Open the project in Android Studio and sync Gradle

## Dependencies

All SDK modules are resolved from GitHub Packages:

```kotlin
implementation("com.spreedly:checkout-paymentsheet:1.2.0")
implementation("com.spreedly:checkout-braintree-apm:1.2.0")
implementation("com.spreedly:checkout-stripe-apm:1.2.0")
implementation("com.spreedly:checkout-threeds:1.2.0")
```

## SDK Documentation

- [SDK Repository](https://github.com/spreedly/checkout-android-sdk)
- [API Documentation](https://github.com/spreedly/checkout-android-sdk)

## Support

For questions or issues, please contact the Spreedly team or create an issue in the [SDK repository](https://github.com/spreedly/checkout-android-sdk/issues).
