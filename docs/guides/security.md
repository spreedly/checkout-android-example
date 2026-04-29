# Security

The Spreedly Android SDK is designed to handle sensitive payment data safely.
It provides multiple layers of protection -- screenshot prevention, in-memory
field encryption, clipboard blocking, automatic CVV expiry, and log
sanitization -- so that card details are never exposed through your
application code.

## Screenshot and Screen Recording Prevention

The SDK sets `FLAG_SECURE` on the host window whenever payment UI is visible.
This blocks screenshots and, on most Android versions, screen recording.

The flag is applied when the composable enters composition and cleared
automatically when it leaves, so the rest of your app is unaffected.

### Built-in coverage

`SecureScreen()` is called internally by:

- `SpreedlyPaymentBottomSheet`
- `PaymentSheet`
- `SpreedlyBankAccountBottomSheet` / `BankAccountSheet`
- `SpreedlyRecacheUI`
- `ThreeDSChallengeBottomSheet` / `ThreeDSChallengeSheet`
- `HostedFieldsJavaHelper`

No extra work is needed if you use these components.

### Custom screens

If you build your own payment UI, apply one of the two public APIs from
`com.spreedly.security`:

```kotlin
// Option 1: Side-effect composable (call anywhere in the composition)
@Composable
fun MyPaymentScreen() {
    SecureScreen()

    Column {
        CreditCardField()
        CVVField()
    }
}

// Option 2: Modifier (attach to any composable)
@Composable
fun MyPaymentScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .secureScreen()
    ) {
        CreditCardField()
        CVVField()
    }
}
```

### Limitations

| Android version | Screenshots | Screen recording |
|-----------------|-------------|------------------|
| 5 -- 9          | Blocked     | Blocked by most recorders |
| 10+             | Blocked     | System recorder **not** blocked (Android limitation) |
| Rooted devices  | Can be bypassed | Can be bypassed |

For additional protection, consider server-side checks via the
Play Integrity API.

## Field Encryption

Card numbers and CVV values are encrypted in memory using **AES-128-GCM**
(via `SpreedlyEncryption`). Each app instance generates a random 128-bit key
with `SecureRandom`; the key lives only in process memory and is never
persisted.

Encryption is applied automatically through the `Encryptor` interface:

- `DefaultEncryptor.encryptValue()` encrypts CARD and CVV field types on
  every keystroke.
- `DefaultEncryptor.decryptValue()` decrypts only when the SDK itself needs
  the plaintext (validation, scheme detection, API submission).
- All other field types (name, expiry, ZIP) pass through unencrypted.

This means your application code never has access to raw card numbers or
CVV values, even if you inspect the field state objects.

## PCI Compliance Controls

### Clipboard blocking

Copy and cut are disabled on card number and CVV fields. Paste is allowed
on card number fields but blocked on CVV. The SDK uses a paste-only text
toolbar for card fields and an empty toolbar for CVV, controlled via
`disableClipboard` and `allowPaste` on `AppTextField`.

### Sensitive field auto-clear

When the app moves to the background, a timer starts. If the app stays
backgrounded for **3 minutes**, the following fields are automatically cleared:

- **CVV** -- via `SPLTextFieldSensitiveAutoClearEffect` and recache bottom sheet/dialog
- **Account Number** (ACH) -- via `SPLTextFieldSensitiveAutoClearEffect`

Routing numbers are not auto-cleared because they are semi-public bank identifiers.

### Card scheme-only storage

`CardNumberContext` stores only the detected card scheme (e.g. Visa, Amex) --
never the full PAN. Downstream components like `CardSchemeAwareCvvValidator`
read the scheme without ever seeing the card number.

### No local persistence

The SDK does not write card data to disk, SharedPreferences, or any local
database. All sensitive values exist only in encrypted process memory and are
transmitted directly to the Spreedly API over HTTPS.

## Log Sanitization

All SDK log output passes through `LogSanitizer` before reaching Logcat or
Datadog:

| Pattern | Action |
|---------|--------|
| 16-digit card numbers (with or without separators) | Replaced with `[REDACTED]` |
| CVV / CVC / security code values | Replaced with `[REDACTED]` |
| API keys, tokens, secrets, passwords | Replaced with `[REDACTED]` |
| Signatures and HMACs | Replaced with `[REDACTED]` |
| Email addresses | Replaced with `[REDACTED]` |
| Payment method / transaction tokens in URL paths | Replaced with `[REDACTED]` |
| Log-forging control characters (`\r`, `\n`, null bytes) | Stripped |

Additional protections:

- `PaymentMethodRequest.toString()` redacts `environmentKey`, `nonce`,
  `signature`, and `certificateToken`.
- `ApiClientBuilder` sanitizes the `Authorization` header in HTTP logs.
- `DatadogSpreedlyLogger` masks the environment key to its first 4
  characters (`AbCd****`) in all Datadog attributes via `LogSanitizer.maskEnvironmentKey()`.

## Telemetry Security

Structured telemetry events (SDK init, payment creation, 3DS flows, API
latency, etc.) are sent to Datadog for observability. These events never
contain PCI-sensitive data:

- Card numbers, CVV values, cardholder names, and billing addresses are
  never included in any telemetry event attribute.
- URL paths logged in API request events are sanitized: payment method
  tokens and transaction tokens in path segments are replaced with
  `[REDACTED]` before emission.
- All string attribute values pass through `LogSanitizer` before being
  sent to Datadog.
- Numeric and boolean attributes (durations, success flags) are sent as
  typed values for Datadog facet queries.
- The environment key is masked to `take(4) + "****"` in all Datadog
  log attributes.

Merchants can control telemetry via `sdk.setDatadogLogLevel(LogLevel.NONE)`
to disable Datadog logging entirely, or set a higher threshold (e.g.
`LogLevel.ERROR`) to reduce volume.

## Network Security

- All API calls use HTTPS via the Ktor CIO engine.
- The SDK targets `core.spreedly.com` exclusively.
- No plaintext HTTP endpoints are used or supported.

### ProGuard / R8

The SDK ships with consumer ProGuard rules. If you use custom rules and
encounter issues, add:

```proguard
-keep class com.spreedly.** { *; }
-keepclassmembers class com.spreedly.** { *; }
-keep class io.ktor.** { *; }
```

## Best Practices for Integrators

1. **Use SDK-managed input fields.** Never build custom card number or CVV
   fields. The SDK's hosted fields and payment sheet handle encryption,
   clipboard blocking, and auto-clear automatically.

2. **Apply `SecureScreen` on custom payment screens.** If you are not using
   `PaymentSheet` or `SpreedlyRecacheUI`, call `SecureScreen()` or use
   `Modifier.secureScreen()` to enable screenshot protection.

3. **Never log sensitive values.**

   ```kotlin
   // Good -- log only a truncated identifier
   Log.d("Payment", "Processing token ending in ${token.takeLast(4)}")

   // Bad -- leaks the full token and CVV
   Log.d("Payment", "CVV: $cvv, Token: $fullToken")
   ```

4. **Do not store card data yourself.** Let the SDK tokenize the card and
   store only the Spreedly payment method token on your backend.

5. **Keep the SDK up to date.** Security patches are included in regular
   releases. Check the [Changelog](../CHANGELOG.md) for details.

## Related Documentation

- [Privacy Policy](privacy-policy.md) -- Data collection, processing, and third-party services
- [Getting Started](getting-started.md) -- Installation and first payment
- [Custom Payment Forms](custom-payment-forms.md) -- Building payment UI with hosted fields
- [Recaching](recaching.md) -- CVV recaching security details
- [Vulnerability Scanning](../development/VULNERABILITY_SCANNING.md) -- Dependency scanning and incident response
- [Secret Scanning](../development/SECRET_SCANNING.md) -- Preventing secret leaks in commits
- [Signature Verification](../development/SIGNATURE_VERIFICATION.md) -- Artifact signing and supply chain security
