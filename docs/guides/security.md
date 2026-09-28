# Security

The Spreedly Android SDK is designed to handle sensitive payment data safely.
It provides multiple layers of protection -- screenshot prevention, in-memory
field encryption, clipboard blocking, automatic CVV expiry, and log
sanitization -- to **minimize** cardholder data exposure in merchant code.
Sensitive values still exist transiently on SDK-managed paths (validation,
tokenization, and residual public crypto helpers documented below); follow
this guide and [error-handling.md](error-handling.md) for safe integration.

## Screenshot and Screen Recording Prevention

The SDK sets `FLAG_SECURE` on the host window whenever payment UI is visible.
`FLAG_SECURE` prevents secure window content from appearing in screenshots
and non-secure displays.

Ownership is reference-counted per window: overlapping secure surfaces (for
example a payment sheet under a 3-D Secure challenge) keep the flag until the
last Spreedly owner leaves. If the host app already had `FLAG_SECURE` set,
Spreedly leaves it set after release. The rest of your app is otherwise
unaffected.

Do not set or clear `FLAG_SECURE` yourself on a window while a Spreedly secure
surface is active on that window. Spreedly restores the flag state from the
first Spreedly acquire; concurrent merchant changes can race with that restore.

### Built-in coverage

`SecureScreen()` is called internally by:

- `SpreedlyPaymentBottomSheet`
- `PaymentSheet`
- `SpreedlyBankAccountBottomSheet` / `BankAccountSheet`
- `SpreedlyRecacheUI`
- `ThreeDSChallengeBottomSheet` / `ThreeDSChallengeSheet`
- `HostedFieldsJavaHelper`
- `ClickToPaySavedCardsDetector` (held for the detector mount duration)

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

`FLAG_SECURE` prevents secure window content from appearing in screenshots
and non-secure displays. Protection against screen recording, screen sharing,
overlays, rooted devices, and OEM-specific capture mechanisms can vary by
Android version, device, and capture mechanism.

For additional protection, consider server-side checks via Play Integrity
App Access Risk.

## Field Encryption

Card numbers, CVV, and bank account numbers (**CARD**, **CVV**, and **ACCOUNT_NUMBER**)
are encrypted in memory using **AES-128-GCM** (via `SpreedlyEncryption`). Encryption uses
a **per-process** lifetime key: each app process generates a random **128-bit** key with
`SecureRandom` (one `keyBytes` for the `SpreedlyEncryption` object, not per SDK instance);
the key lives only in process memory and is never persisted. `SpreedlyEncryption.KEY` is a
sentinel string (not key material) used to select that process key.

Encryption is applied automatically through the `Encryptor` interface:

- `DefaultEncryptor.encryptValue()` encrypts **CARD, CVV, and ACCOUNT_NUMBER** field types on
  every keystroke.
- `DefaultEncryptor.decryptValue()` decrypts only when the SDK itself needs
  the plaintext (validation, scheme detection, API submission).
- All other field types (name, expiry, ZIP) pass through unencrypted.

`@RestrictTo(LIBRARY_GROUP)` and `@Deprecated` on the encryption surfaces are
**lint-only** — symbols remain in the published ABI. Public `decryptAES` / `KEY`
still accept CHD for SDK field ciphertext; this release improves entropy and
fail-closed decrypt behavior. It is **not** an egress reduction for every API
capability (`getDisplayValue`, public `decryptAES`, and residual FieldUtils paths).

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

The SDK sanitizes log tags, messages, and throwables in `LoggerManager` before
any `SpreedlyLogger` implementation runs on the standard log APIs
(`verbose` / `debug` / `info` / `warn` / `error`) — including custom loggers
installed via `LoggerManager.setLogger(...)`. Structured events via
`LoggerManager.emitEvent` sanitize string attributes before Datadog and before
the base-logger copy (that path does not go through the log-API facade).
Implementations must not expect raw card data, CVV, or the original exception
instance. `LoggerManager.logger` is the log-API facade; it is not the same
instance passed to `setLogger`.

Built-in sanitization covers:

| Pattern | Action |
|---------|--------|
| ISO 7812 card numbers / PAN-like digit runs of **12 or more** digits, including contiguous runs and digits separated by any number of spaces, hyphens, dots, or underscores (covers double-/triple-/wide-spaced formatting, glued-after-letter forms such as `cardNumber4111…`, and 20+ digit embeddings). Candidates are detected in a single linear pass with no per-candidate character cap. 12-digit dotted IPv4 addresses and long numeric IDs may over-redact | Replaced with `[REDACTED]` |
| Labeled CVV / CVC / security code values (JSON `"cvv":"123"`, escaped-JSON `\"cvv\":\"123\"`, `verification_value`, prose `cvv: 123`; unlabeled 3–4 digits are not redacted) | Replaced with `[REDACTED]` |
| API keys, tokens, secrets, passwords | Replaced with `[REDACTED]` |
| Signatures and HMACs | Replaced with `[REDACTED]` |
| Email addresses | Replaced with `[REDACTED]` |
| Payment method / transaction tokens in URL paths | Replaced with `[REDACTED]` |
| Log-forging control characters (`\r`, `\n`, null bytes) | Stripped |

`LogSanitizer` scrubs supported PAN/CVV/token/credential/value patterns and URL
tokens. Bare identifier-shaped path components (for example structural mandate
key paths in conversion diagnostics) are **not** automatically scrubbed.

Mandate **values** are do-not-log; request `toString` redacts them as
`mandate=[REDACTED]`. Conversion diagnostics may contain structural mandate key
paths. Click to Pay unexpected local conversion failures publish only a static
public failure message (`Tokenize failed`). The SDK default `sdkScope`
`CoroutineExceptionHandler` logs only the exception class name (no throwable
payload) so custom loggers cannot receive raw source exception content from
that last-resort path.

Additional protections:

- `PaymentMethodRequest.toString()` redacts `environmentKey`, `nonce`,
  `signature`, and `certificateToken`.
- `SpreedlyNetworkError.SpreedlyApiErrorDetail.toString()` matches
  `safeDescription()` (`statusCode` and `errorKey` only). It does not
  include `rawErrorBody`, `errorMessage`, or `validationErrors`. Getters
  return values stored after `LogSanitizer.sanitizeStoredErrorString` (pattern
  redaction, trailing separator-optional partial-digit redaction after the 8 KiB cap, log-forge
  control characters → space, then `trim()`), including nested
  `validationErrors` `fieldName` / `errorKey` / `errorMessage`, after an 8 KiB
  length cap. That is not the verbatim HTTP body and is not a guarantee every
  secret shape is removed. Prefer `safeDescription()` for logs; do **not** parse
  `rawErrorBody` / `rawErrorResponse` as wire JSON (over-redaction can break
  JSON, e.g. unlabeled 13-digit timestamps). Do not treat stored strings as a
  source of truth for analytics or retries — use `statusCode` / `errorKey` /
  `safeDescription()` instead.
  `AppNetworkError.API_ERROR.toString()` also matches `safeDescription()`;
  public `API_ERROR` getters remain raw. Direct `AppNetworkError` consumers are not
  covered by this tokenize-path hardening. `RequestHandler` uses streaming
  `HttpStatement.execute { }` (not the no-arg overload that buffers the full
  body via `call.save()`), reads at most 8 KiB **bytes** from the HTTP error
  body channel, and cancels the rest; success (2xx) bodies are not capped.
  Cancel stops further channel reads on that path; it is not a socket-level
  cutoff if the engine already buffered data.
- `PaymentResult.Failed.toString()` is log-safe (`errorType`,
  `statusCode`, `apiError`, and `state` when present). It does not include
  `message`, `rawErrorResponse`, `validationErrors`, or `originalError`. Prefer
  `toString()` for logs and `getDescription()` for UI; never log getters
  directly on merchant-constructed failures.
- `ThreeDSChallengeResult.Failed.toString()` is log-safe
  (`errorType` only). It does not include `message` or `originalError`.
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
