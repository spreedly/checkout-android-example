# Privacy Policy

This document describes what data the Spreedly Android SDK collects, how it
is processed, and which third-party services receive it. It applies to all
modules distributed under the `com.spreedly` namespace.

## Data Collected by the SDK

### Payment data

When a user submits a payment, the SDK transmits the following to the
Spreedly API (`core.spreedly.com`) over HTTPS:

| Category | Fields |
|----------|--------|
| Authentication | Environment key, nonce, timestamp, HMAC signature, certificate token |
| Card details | Card number, CVV/CVC, expiry month and year |
| Cardholder info | First name, last name, full name, company |
| Billing address | Address lines 1--2, city, state, ZIP, country, phone number |
| Shipping address | Address lines 1--2, city, state, ZIP, country, phone number |
| Optional fields | Email, custom metadata key-value pairs, `retainOnSuccess` flag |

For offsite payment methods (PayPal, Pix, Boleto, etc.), the same
authentication fields are sent along with the payment method type, email,
name, address, country, country code, and -- where required -- a document
identifier.

All payment data is used exclusively for tokenization and payment
processing. The SDK does not use it for advertising, profiling, or any
purpose other than completing the requested transaction.

### Telemetry data

When Datadog logging is enabled (the default, at `VERBOSE` level), the
SDK sends operational telemetry to Datadog for error monitoring:

| Attribute | Source | Example |
|-----------|--------|---------|
| Platform | Fixed value | `"Android"` |
| OS version | `Build.VERSION.RELEASE` | `"14"` |
| SDK version | Build-time constant | `"x.y.z"` |
| Session ID | Random UUID per SDK init | `"a1b2c3d4-..."` (ephemeral, not tied to a user) |
| Region | Mapped from network country ISO code (coarse) | `"america"` |
| Carrier | Network operator name | `"Verizon"` |
| Environment key | Masked to first 4 characters | `"AbCd****"` |
| Error code | Payment failure code | `"CLIENT_ERROR"` |
| Error message | Payment failure message | `"Failed to create payment method"` |
| Network info | Datadog SDK (connectivity type) | Wi-Fi / cellular |

The `region` attribute is a coarse geographic label (e.g. "america",
"europe") derived from the mobile network country code -- not from GPS or
fine-grained location. The `carrier` attribute is the operator name
(e.g. "Verizon"). The `session_id` is a temporary UUID generated on each
SDK init; it is not persisted and is not tied to any user identity. None
of these attributes are used for user identification.

All telemetry passes through `LogSanitizer` before transmission. Card
numbers, CVV values, tokens, secrets, email addresses, and signatures are
replaced with `[REDACTED]`. URL paths containing payment method or
transaction tokens are also sanitized. See the [Security](security.md)
guide for the full list of sanitized patterns.

Integrators can reduce or disable Datadog telemetry at any time:

```kotlin
sdk.setDatadogLogLevel(LogLevel.ERROR) // errors only
sdk.setDatadogLogLevel(LogLevel.NONE)  // disable entirely
```

See the [Datadog Integration](../development/DATADOG_INTEGRATION.md) guide
for configuration details.

## Data Not Collected

The SDK does **not** collect:

- Device IDs or advertising identifiers
- GPS or fine-grained location data (region is derived from the mobile
  carrier's country code, not from GPS)
- Browsing history or app usage analytics
- Contact lists, calendar data, or other on-device content
- Biometric data

## How Data Is Processed

### Payment data

1. Card numbers and CVV values are encrypted in memory using AES-128-GCM
   with a per-instance random key that is never persisted to disk.
2. Encrypted values are decrypted only when the SDK needs the plaintext for
   validation, card scheme detection, or API submission.
3. Data is transmitted over HTTPS to `core.spreedly.com` and is not stored
   locally in SharedPreferences, databases, or the file system.
4. Once the Spreedly API returns a payment method token, the raw card data
   is no longer needed and exists only in process memory until garbage
   collected.

### Telemetry data

1. Log messages are sanitized by `LogSanitizer` to strip any sensitive
   values that may have been included in error messages.
2. Sanitized logs are batched by the Datadog SDK and uploaded approximately
   every 5 seconds.
3. No card numbers, CVV values, cardholder names, or billing addresses are
   ever included in telemetry.

## Third-Party Services

| Service | Purpose | Data received | Optional |
|---------|---------|---------------|----------|
| **Spreedly API** (`core.spreedly.com`) | Payment tokenization | Payment data listed above | No (core function) |
| **Datadog** | Error monitoring and telemetry | Sanitized telemetry listed above | Yes (can be disabled via `LogLevel.NONE`) |
| **Braintree** (`checkout-braintree-apm`) | PayPal and Venmo payments | Braintree performs its own device data collection for fraud prevention | Yes (module not required) |
| **Stripe** (`checkout-stripe-apm`) | iDEAL, Bancontact, EPS, P24, SEPA | Stripe processes payment data through its own PaymentSheet | Yes (module not required) |
| **3DS gateway** (`checkout-threeds`) | 3D Secure authentication | For Gateway-Specific 3DS the SDK loads the gateway's device fingerprint page in a Chrome Custom Tab; fingerprinting is performed by the gateway, not the SDK. For Global 3DS (Forter) the SDK provides a `DeviceInfoCollector` utility that reads device model and OS version; the merchant calls this and sends the result to their backend. | Yes (module not required) |

Each optional module has its own third-party SDK with its own privacy
policy. Refer to [Braintree](https://www.braintreepayments.com/legal),
[Stripe](https://stripe.com/privacy), and
[Forter](https://www.forter.com/privacy-policy/) for their respective
policies.

## Data Security

The SDK implements multiple layers of protection for payment data:

- **In-memory encryption** -- AES-128-GCM for card numbers and CVV values
- **No local persistence** -- sensitive data is never written to disk
- **Clipboard blocking** -- copy/cut disabled on card and CVV fields; paste disabled on CVV only
- **Screenshot prevention** -- `FLAG_SECURE` applied to payment UI
- **CVV auto-clear** -- CVV field is cleared after 3 minutes in background
- **Log sanitization** -- card numbers, CVV, tokens, emails, and secrets
  are redacted from all log output
- **HTTPS only** -- no plaintext HTTP endpoints

For full details, see the [Security](security.md) guide.

## Data Retention

The SDK does not persist sensitive payment data (card numbers, CVV, etc.)
to disk. All card data exists only in encrypted process memory and is gone
when the app process ends.

The Braintree module (`checkout-braintree-apm`) temporarily stores an
opaque session continuation token in SharedPreferences to survive browser
switches during PayPal or Venmo flows. This token contains no payment card
data and is cleared immediately when the flow completes (success, failure,
or cancellation).

Data retention on the server side is governed by each service's own
policies:

- **Spreedly**: [spreedly.com/privacy](https://www.spreedly.com/privacy)
- **Datadog**: [datadoghq.com/legal/privacy](https://www.datadoghq.com/legal/privacy/)

## PCI Compliance

The SDK is designed to support PCI DSS compliance:

- Sensitive payment data (card numbers, CVV) is never logged
- All card data is encrypted in process memory
- No card data is persisted to disk or local storage
- Transmission uses HTTPS exclusively
- Log sanitization prevents accidental exposure in Logcat or Datadog

See the [Security](security.md) guide for the complete list of PCI
compliance controls.

## Changes to This Policy

Updates to this policy will be noted in the
[Changelog](../CHANGELOG.md). Review the changelog when upgrading to a new
SDK version.

## Contact

- **Spreedly Support**: [spreedly.com/support](https://spreedly.com/support/)
- **Spreedly Documentation**: [developer.spreedly.com](https://developer.spreedly.com/)
- **GitHub Issues**: [checkout-android-sdk/issues](https://github.com/spreedly/checkout-android-sdk/issues)
