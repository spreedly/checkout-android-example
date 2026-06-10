# Spreedly Checkout — Android Example

> **This project is for demonstration and reference purposes only.** It is not intended for use in production environments. The code, configuration, and architecture shown here illustrate SDK integration patterns — adapt them to your own security requirements, backend infrastructure, and key management practices before shipping to production.

This sample app demonstrates the [Spreedly Android Checkout SDK](https://github.com/spreedly/checkout-android-sdk) at version **1.0.1** (tag `v1.0.1`).

## Setup

1. Clone this repository
2. Add your GitHub Packages credentials to `~/.gradle/gradle.properties`. Even though [checkout-android-maven](https://github.com/spreedly/checkout-android-maven) is public, GitHub Packages still requires authentication to download Maven artifacts. Create a PAT with `read:packages` scope and add:

```properties
gpr.usr=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

3. Add your Spreedly environment key and other API keys to `apikeys.properties` (see `apikeys.properties.example`)
4. Open the project in Android Studio and sync Gradle

## Dependencies

All SDK modules are resolved from GitHub Packages:

```kotlin
implementation("com.spreedly:checkout-paymentsheet:1.0.1")
implementation("com.spreedly:checkout-braintree-apm:1.0.1")
implementation("com.spreedly:checkout-stripe-apm:1.0.1")
implementation("com.spreedly:checkout-threeds:1.0.1")
```

## SDK Documentation

- [SDK Repository](https://github.com/spreedly/checkout-android-sdk)
- [API Documentation](https://github.com/spreedly/checkout-android-sdk)

## Support

- **Spreedly Documentation**: [docs.spreedly.com](https://docs.spreedly.com/)
- **Support Portal**: [spreedly.com/support](https://spreedly.com/support/)

## Legal

- [Terms of Service](https://legal.spreedly.com/#terms)
- [Privacy Policy](https://legal.spreedly.com/#privacy-policy)
- [License](LICENSE) (Apache 2.0)
