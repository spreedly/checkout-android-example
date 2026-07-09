# Troubleshooting

Symptom-based troubleshooting for the Spreedly Android SDK, organized by area. For error types, retry logic, and error handling code patterns, see [Error Handling](error-handling.md).

## Build and Installation

### Gradle cannot resolve `com.spreedly:checkout-*`

Missing or invalid GitHub Packages credentials.

- Confirm your GitHub Personal Access Token (PAT) has the `read:packages` scope
- Verify the Maven repository URL: `https://maven.pkg.github.com/spreedly/checkout-android-maven`
- Check that `username` and `password` are set correctly in `settings.gradle.kts`

See [Getting Started -- Install](getting-started.md#1-install).

### Gradle sync fails

- Ensure the bundled JDK (jbr-21) is selected: **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**
- Run **File > Invalidate Caches > Invalidate and Restart**
- From Terminal: `./gradlew clean --refresh-dependencies`

### Forter Maven repository resolution failure

Gradle sync fails with a dependency resolution error for the Forter artifact.

- Confirm the Forter Maven repository is declared in `settings.gradle.kts` (not just `build.gradle.kts`)
- Verify `forterMavenUser` and `forterMavenPassword` are set in `gradle.properties` or `~/.gradle/gradle.properties`
- The artifact is `com.forter.mobile:forter3ds` -- Forter docs may reference different names

## SDK Initialization

### `sdk.init` fails immediately

Expired or reused signed auth params.

- Issue **fresh** nonce, signature, timestamp, and certificateToken from your backend for each payment session
- Auth parameters are single-use ("enhanced iframe security") -- never reuse them

### `UNAUTHORIZED` / auth errors

- Rotate signing keys on the server
- Confirm `environmentKey` matches your Spreedly environment
- Verify credentials haven't been revoked

### `ACCOUNT_INACTIVE` (422 with `errors.account_inactive`)

You're using real card numbers in a test environment.

- Switch to Spreedly [test card numbers](testing-guide.md#test-card-numbers)
- Or activate your environment for real transactions

### "SDK not initialized" crash

`IllegalStateException: SDK not initialized. Call init() first.`

- Ensure `sdk.init()` completes before calling any SDK method
- Don't observe `paymentResultFlow` before initialization
- Sequence: init first, then observe, then start payment

## Express Checkout (Payment Sheet)

### Bottom sheet not appearing

- Confirm `SpreedlyBottomSheet(sdk = sdk)` is in your composable tree **before** calling `sdk.expressCheckout()`
- Verify `sdk.init(options)` completed without errors
- Place `SpreedlyBottomSheet` at the top level of your composable, not nested inside `LazyColumn` or other scrolling containers

### Missing payment results

- Start collecting from `sdk.paymentResultFlow` **before** calling `sdk.expressCheckout()`. If you subscribe after, you may miss the emission.
- For Java, call `PaymentSheetJavaHelper.observePaymentResults()` in `onCreate` before the user can trigger checkout.

### Theme not applying

- `config` parameter overrides the global theme. If you pass `PaymentSheetConfig()` with all defaults, the global theme fills in. Explicit colors take precedence.
- `PaymentSheetConfig.fromTheme()` must be called inside a `@Composable` function to access `MaterialTheme` colors.

### Express autofill or initial card format ignored

- **`sdk.setConfig(PaymentSheetConfig(...))`** does not by itself change express **autofill** or **initial `CardNumberFormat`** on the payment sheet UI. Those come from **`PaymentSheetDisplayConfig`** (or legacy **`PaymentSheetConfig`** when **`displayConfig`** is null) on **`SpreedlyBottomSheet`** / **`PaymentSheet`**, resolved when the composable runs.

### Form resets unexpectedly

The bottom sheet clears form state each time it opens, unless `sdk.shouldPreserveState()` is configured. This is by design for fresh payment sessions.

### Configuration changes (rotation)

`SpreedlyBottomSheet` survives configuration changes when the `Spreedly` instance is held in a `ViewModel`. See `BottomSheetPaymentViewModel.kt` in the example app.

## 3DS (Forter Global)

### Forter SDK not initialized

Challenge doesn't appear, logs show "Forter SDK not ready."

- Verify `forterSiteId` is provided in `SpreedlySDKInitOptions`
- Check that `forterSiteId` is correctly configured in `BuildConfig`
- Verify `checkout-threeds` dependency is added (Forter is included transitively)
- Check logcat: `adb logcat | grep -E "(Spreedly|Forter)"`

### Challenge not appearing

Possible causes:

- **3DS not required** -- the transaction doesn't need authentication
- **Frictionless flow** -- authentication happened behind the scenes
- **Missing `sca_authentication`** -- backend response doesn't include it

Check the purchase response for `sca_authentication` before calling `sdk.showThreeDSChallenge()`.

### Results not received

- Ensure you're collecting from `sdk.threeDSChallengeResultFlow` **before** presenting the challenge
- Verify the collection is in a `viewModelScope` (not a short-lived scope)
- Check the coroutine isn't being canceled

### Challenge fails immediately

- Verify the transaction token is fresh
- Check the Forter Site ID matches your backend configuration
- Review the Forter Portal for error details

## 3DS (Gateway-Specific)

### Challenge not launching

- Flow collectors not set up in `init {}` before calling `showThreeDSChallenge()`
- Activity reference not passed to `showThreeDSChallenge()`
- No Chrome-compatible browser installed on the device

### Transaction succeeds without 3DS

- Not using a test amount that triggers 3DS (use 3003, 3004, or 3005 cents)
- Missing `attempt_3dsecure: true` in the purchase request
- Nested JSON structure `{ "transaction": { ... } }` instead of flat

### Completion polling timeout

Error after 2 minutes: "Timeout waiting for transaction completion."

Verify your backend processes the `/complete` call and that `gatewaySpecific3DSTriggerCompletionFlow` is subscribed.

### Complete API returns 404

The SDK waits ~10 seconds for device fingerprinting before triggering the completion callback. Verify your backend endpoint proxies to `POST /v1/transactions/{token}/complete.json` on Spreedly Core.

### Chrome Custom Tab browser redirect

On Chrome < 137 or non-Chrome browsers, the SDK falls back to Chrome Custom Tabs. A user can navigate from the Custom Tab to the full browser during a 3DS challenge, preventing automatic redirect. On Chrome 137+, Auth Tab eliminates this issue.

## Offsite Payments

### Chrome Custom Tab not launching

- Verify the SDK is initialized
- Ensure you're passing an `Activity` context, not `ApplicationContext`
- Check that the transaction has a valid `checkout_url` in its status response
- Verify `androidx.browser:browser` dependency is added

### Deep link not received (Custom Tab stays open)

- Use `SpreedlyOffsiteCheckout.redirectUrl(context)` for the `redirectUrl` in your purchase API call
- The SDK's `OffsiteReturnActivity` uses scheme `${applicationId}.spreedlyoffsite` -- ensure it's merged from the library manifest
- Verify your app's `applicationId` matches the scheme in the redirect URL

### Payment result not received

- Collect `paymentResultFlow` **before** starting the payment
- Use `viewModelScope` for collection (not a short-lived scope)
- The SDK's `OffsiteReturnActivity` calls `handleReturn()` automatically -- ensure the SDK is initialized before the redirect

### 401 Unauthorized on second payment

Auth parameters are single-use. Re-fetch auth params and call `sdk.init()` for each payment attempt.

### EBANX "Invalid Document" error

- Send the document via `gateway_specific_fields.ebanx.document` (not top-level)
- For OXXO (Mexico), do **not** include `gateway_specific_fields` -- documents are not required
- Verify the CPF format is valid (Brazilian tax ID format)

### EBANX currency mismatch

Use the correct currency: Brazil (Pix, Boleto, NuPay) = `"BRL"`, Mexico (OXXO) = `"MXN"`.

### EBANX payment returns "pending" state

Expected for offline methods (Boleto, OXXO). Treat "pending" as soft success -- the customer completes payment offline.

## Stripe APM

### PaymentSheet not appearing

- Verify `StripeAPMConfig` has all required fields populated (non-blank): `publishableKey`, `clientSecret`, `transactionToken`, `merchantDisplayName`
- Ensure the Stripe publishable key matches the key used to create the PaymentIntent on the backend
- Check that the purchase response has `state: "pending"`
- Verify `client_secret` is present in `gateway_specific_response_fields.stripe_payment_intents`

### Payment stays "pending" or "processing" after completion

In native mobile flows, the browser redirect that updates the transaction status doesn't happen naturally. For SEPA Direct Debit, the bank debit is asynchronous. The SDK mitigates this by polling up to 4 times (~7 seconds total).

Treat `pending` and `processing` as soft success and confirm via your backend webhook.

### "Missing client_secret" error

- Ensure your Spreedly gateway is a **Stripe Payment Intents** gateway (not a regular Stripe gateway)
- Verify `payment_method.payment_method_type: "stripe_apm"` and `payment_method.apm_types` are in the purchase request
- Check `state` is `"pending"` -- only pending transactions include `client_secret`

### "Invalid publishable key"

- The key must start with `pk_test_` (test) or `pk_live_` (production)
- Ensure it matches the Stripe account linked to your Spreedly Stripe Payment Intents gateway

## Braintree APM

### "Missing required Braintree payment configuration"

Ensure all required fields in `BraintreeAPMCheckoutConfig` are valid. This is handled internally by the SDK.

### "Braintree client_token not available"

The purchase was not created on a Braintree gateway, or the gateway is not properly configured. Verify the gateway token and that the purchase response state is `"processing"`.

### "gateway does not support offsite_purchase"

- The gateway token is not a Braintree gateway, or `offsite_sync: true` is missing from the purchase request
- Ensure `gateway_specific_fields.braintree` includes the required flow type (`venmo_flow_type` or `paypal_flow_type`)

### Browser switch doesn't return to app

- Verify the intent filter in your merged manifest includes `android:scheme="${applicationId}.braintree"`
- Check for manifest merge conflicts that may override the scheme

### Nonce expires before confirmation

Braintree nonces have a limited lifetime. Confirm the transaction promptly after receiving the nonce.

## Focus and Input

### Focus not working

- Verify the focus state is changing (add temporary logging)
- Only one field should have `shouldFocus = true` at a time
- The field must be fully composed before it can receive focus

### Focus jumping between fields

- Use `remember` for focus state
- Don't update focus state from multiple sources simultaneously

## Network Errors

### Timeouts / `NETWORK_ERROR`

- Check device connectivity
- Retry with backoff for transient errors
- See [Error Handling -- Error Recovery and Retry Logic](error-handling.md#error-recovery-and-retry-logic)

## Debug Logging

### General SDK logs

```
package:mine (tag~:^Spreedly- | tag:HttpClient)
```

### Flow-specific filters

```bash
# Stripe APM
adb logcat | grep -E "(Spreedly|StripeAPM|SpreedlyStripeAPMCheckout)"

# 3DS (Forter)
adb logcat | grep -E "(Spreedly|Forter3DS|ThreeDSChallenge)"

# Offsite payments
adb logcat | grep -E "(Spreedly|SpreedlyOffsiteCheckout|OffsiteReturn)"

# Braintree
adb logcat | grep -E "(Spreedly|Braintree)"
```

### Verbose 3DS logging

```kotlin
if (BuildConfig.DEBUG) {
    Forter3DS.getInstance().setLogLevel(ForterLogLevel.VERBOSE)
}
```

## Telemetry Issues (Datadog)

If logs aren't appearing in Datadog:

- Verify `datadog.client.token` is set in `apikeys.properties`
- Logs upload approximately every 5 seconds -- wait briefly
- Check Logcat for `"Datadog logging initialized successfully"` on SDK init
- Query `service:checkout-android-sdk` in [Datadog Logs](https://app.datadoghq.com/logs)

See [Datadog Integration -- Troubleshooting](../development/DATADOG_INTEGRATION.md#troubleshooting).

## Getting Help

### What to share with Spreedly Support

**OK to share:** SDK version, approximate time (UTC), masked `environment_key` (first 4 characters only), `session_id` from Datadog global attributes, `PaymentResult.Failed` `errorType` / `apiError`, HTTP status code, device OS level.

**Never share:** Full card number, CVV, full `environmentKey`, raw `rawErrorResponse` if it could contain tokens or PII, complete auth signatures or certificate tokens.

### When to contact support

Open a ticket via [Spreedly Support](https://spreedly.com/support/) after you have confirmed credentials, a fresh init payload, and a minimal repro (or merchant logs scoped as above). Include which [integration guide](../README.md) you followed.

## See Also

- [Error Handling](error-handling.md) -- Error types, retry logic, and code patterns
- [Testing Guide](testing-guide.md) -- Test cards, environment setup, flow-by-flow testing steps
- [Getting Started](getting-started.md) -- Installation and first payment
- [Datadog Integration](../development/DATADOG_INTEGRATION.md) -- Telemetry setup and verification
