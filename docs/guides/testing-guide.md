# Testing Guide

How to test your Spreedly Android SDK integration before going to production.

## Test Environment Setup

### Prerequisites

- A Spreedly test environment with a valid `environmentKey`
- Backend-signed authentication parameters (nonce, signature, timestamp, certificateToken)
- For 3DS: a Forter `siteId` configured in your Spreedly environment
- For Stripe APM: a Stripe test publishable key (`pk_test_...`) and a Stripe Payment Intents gateway
- For Braintree APM: a Braintree sandbox gateway configured in Spreedly
- For offsite payments: an offsite gateway (use SPREL for testing)

### Test vs Production

| Setting | Test | Production |
|---------|------|------------|
| Card numbers | Use test cards below | Real cards |
| Environment | Spreedly test environment | Spreedly live environment |
| Stripe key | `pk_test_...` | `pk_live_...` |
| Braintree gateway | Sandbox gateway | Production gateway |
| Offsite gateway | SPREL (test) | PayPal, EBANX, etc. |
| Forter portal | Sandbox > Mobile Events Viewer | Production dashboard |

## Test Card Numbers

### Credit Card Tokenization

| Card Number | Brand | Use Case |
|-------------|-------|----------|
| `4111 1111 1111 1111` | Visa | Successful tokenization |
| `5555 5555 5555 4444` | Mastercard | Successful tokenization |
| `3782 822463 10005` | American Express | Successful tokenization |

Use any future expiry date (e.g., `12/30`) and any 3-digit CVV (e.g., `123`).

### 3DS (Forter Global)

| Card Number | 3DS Behavior | Expected Result |
|-------------|--------------|-----------------|
| `4000 0000 0000 0002` | Requires 3DS challenge | Challenge appears, complete to succeed |
| `4000 0000 0000 0101` | 3DS authentication fails | Challenge fails with error |
| `4242 4242 4242 4242` | Frictionless 3DS | No challenge, instant success |

Contact your Spreedly account manager or Forter support for complete test card lists specific to your configuration.

### 3DS (Gateway-Specific)

Use test **amounts** (not card numbers) to trigger different 3DS scenarios:

| Amount | Cents | Scenario |
|--------|-------|----------|
| $30.03 | 3003 | Device fingerprint only (no challenge) |
| $30.04 | 3004 | Device fingerprint + challenge (retained card) |
| $30.05 | 3005 | Direct challenge (no fingerprint) |

Your purchase request must include `attempt_3dsecure: true` for 3DS to trigger.

### EBANX Test Data

| Field | Brazil (Pix/Boleto/NuPay) | Mexico (OXXO) |
|-------|--------------------------|---------------|
| **CPF/Document** | `853.513.468-93` | Not required |
| **Name** | `Ana Santos Araujo` | `Manuela E. Beyer Rocabado` |
| **Email** | `test@test.com` | `test@test.com` |
| **Phone** | `8522847035` | `(040) 577-7687` |
| **Address** | `Rua E, 1040` | `Oyono, 882` |
| **City** | `Maracanaú` | `Hermosillo` |
| **State** | `CE` | `Sonora` |
| **Zip** | `12345` | `48822` |
| **Country** | `BR` | `MX` |
| **Currency** | `BRL` | `MXN` |

## Testing Each Payment Flow

### Card Tokenization (Express Checkout)

1. Initialize the SDK with fresh backend-signed parameters
2. Call `sdk.expressCheckout()` to present the payment sheet
3. Enter a test card number, future expiry, and any CVV
4. Tap "Pay"
5. Collect the result from `sdk.paymentResultFlow`:
   - `PaymentResult.Completed` with a `token` means success
   - `PaymentResult.Failed` indicates an error — check `errorType` and `apiError`
   - `PaymentResult.Canceled` means the user dismissed the sheet

### Card Tokenization (Custom Payment Forms / Hosted Fields)

1. Initialize the SDK
2. Render hosted fields via `SpreedlyHostedField` composables
3. Fill in test card data
4. Call `sdk.createCreditCard(...)` with form fields and any additional fields
5. Verify `PaymentResult.Completed` via `paymentResultFlow`

### Recaching (CVV Update)

1. Initialize the SDK
2. Present the recache UI with a saved payment method token
3. Enter any 3-digit CVV
4. Submit and verify `PaymentResult.Completed`

See [Recaching](recaching.md) for configuration details.

### 3DS Global (Forter)

1. Initialize the SDK with `forterSiteId` in `SpreedlySDKInitOptions`
2. Tokenize a card, send the token to your backend to create a purchase
3. If the purchase response includes `sca_authentication`, call `sdk.showThreeDSChallenge(transactionToken)`
4. Complete the challenge in the Forter UI
5. Collect results from `sdk.threeDSChallengeResultFlow`
6. Verify events in the Forter Portal under **Sandbox > Mobile Events Viewer**

See [3DS Global](3ds-global.md) for the full integration.

### 3DS Gateway-Specific

1. Initialize the SDK
2. Tokenize a card, send the token to your backend
3. Create a purchase with `attempt_3dsecure: true` and a test amount (e.g., 3005 cents for a direct challenge)
4. If the response includes `required_action`, call `sdk.showThreeDSChallenge(transactionToken)` with the Activity reference
5. The challenge opens in a Chrome Custom Tab (or Auth Tab on Chrome 137+)
6. Complete the challenge and verify the result via `gatewaySpecific3DSTriggerCompletionFlow`

See [3DS Gateway-Specific](3ds-gateway-specific.md) for the full integration.

### Offsite Payments (SPREL Test Gateway)

1. Initialize the SDK
2. Configure with `OffsitePaymentMethodType.SPREL`:

```kotlin
val config = OffsitePaymentConfig(
    paymentMethodType = OffsitePaymentMethodType.SPREL,
    email = "test@example.com",
    fullName = "Test User",
    documentId = DocumentId.standard("123456789"),
    country = "BR",
)
```

3. Tokenize, purchase via your backend, then call `SpreedlyOffsiteCheckout.present(token, activity)`
4. Complete checkout on the SPREL test page in Chrome Custom Tab
5. Verify the deep link returns to your app and `PaymentResult` is received

See [Offsite Payments](offsite-payments.md) for EBANX (Pix, Boleto, NuPay, OXXO) and PayPal flows.

### Stripe APM

1. Initialize the SDK
2. Configure with Stripe test keys:

```kotlin
val config = StripeAPMConfig(
    publishableKey = "pk_test_...",
    clientSecret = clientSecret,       // From test purchase response
    transactionToken = transactionToken,
    merchantDisplayName = "Test Store",
)
```

3. Present the Stripe PaymentSheet via `SpreedlyStripeAPMCheckout.present()`
4. Select an APM (e.g., iDEAL) and complete the test payment
5. Verify `PaymentResult` received via `paymentResultFlow`

See [Stripe APM](stripe-apm.md) for the full integration.

### Braintree APM (PayPal / Venmo)

1. Configure a Braintree sandbox gateway in your Spreedly environment
2. Use sandbox credentials for PayPal and Venmo testing
3. Set `braintreeGatewayToken` in `apikeys.properties` to the sandbox gateway token
4. PayPal: use PayPal sandbox buyer accounts for the PayPal checkout flow
5. Venmo: requires the Venmo app installed on the test device, or use Braintree SDK's test mode

See [Braintree APM](braintree-apm.md) for the full integration.

### ACH Bank Account

1. Initialize the SDK
2. Use the pre-built bank account sheet or custom form
3. Enter test bank account details (routing number, account number)
4. Submit and verify `PaymentResult.Completed`

See [ACH Bank Account](ach-bank-account.md) for the full integration.

## Testing Error Scenarios

### Trigger Common Errors

```kotlin
// Test account inactive error (use real card in test environment)
// Will trigger ACCOUNT_INACTIVE if using a real card with a test gateway

// Test validation errors (use empty required fields)
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true)),
    additionalFields = mapOf(
        AdditionalField.FIRST_NAME to "",
        AdditionalField.LAST_NAME to "",
    )
)

// Test success scenario (use test card)
// Set card number to "4111111111111111" in UI first
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true)),
    additionalFields = mapOf(
        AdditionalField.FIRST_NAME to "John",
        AdditionalField.LAST_NAME to "Doe",
    )
)
```

### Expected Error Types

| Scenario | Error | API Error Enum |
|----------|-------|----------------|
| Real card in test environment | 422 with `errors.account_inactive` | `SpreedlyApiError.ACCOUNT_INACTIVE` |
| Empty required fields | Validation error | `SpreedlyApiError.VALIDATION_ERROR` |
| Invalid/expired auth params | 401 Unauthorized | `SpreedlyApiError.UNAUTHORIZED` |
| Billing / account issues | 402 Payment Required | `SpreedlyApiError.PAYMENT_REQUIRED` |
| No network | Network error | `SpreedlyNetworkError` via `PaymentResult.Failed` |

For full error handling patterns, see [Error Handling](error-handling.md).

## Verifying with Logcat

Filter SDK logs in Android Studio's Logcat panel:

```
package:mine (tag~:^Spreedly- | tag:HttpClient)
```

Flow-specific filters:

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

Key log messages to look for during offsite flows:

- `"Submitting offsite payment method"` -- Tokenization started
- `"Offsite payment method created successfully"` -- Token received
- `"Present called with transaction"` -- Checkout launch
- `"Launching Custom Tab"` -- Browser opened
- `"Return URL received: ..."` -- Deep link received
- `"Publishing payment result"` -- Final result

## Verifying Telemetry (Datadog)

If your `apikeys.properties` includes a valid `datadog.client.token`, the SDK sends telemetry automatically.

1. **Local verification** -- In Logcat, filter for `Spreedly` or `Datadog`. On SDK init you should see `"Datadog logging initialized successfully"`.
2. **Datadog console** -- Go to [Datadog Logs](https://app.datadoghq.com/logs) and query `service:checkout-android-sdk`. You should see events like `sdk_initialized`, `payment_method_created`, etc.
3. **Dashboard** -- The [Spreedly Android SDK dashboard](https://app.datadoghq.com/dashboard/e5v-vjq-7b5) shows aggregated charts for SDK init, payment success/failure, UI interactions, 3DS flows, APMs, and network errors.

Events are uploaded approximately every 5 seconds. For telemetry troubleshooting, see [Datadog Integration](../development/DATADOG_INTEGRATION.md#troubleshooting).

## Running the Demo App

The SDK includes a demo app (the `:app` module) with screens for every payment flow. This is the fastest way to verify SDK behavior end-to-end.

1. Clone the repo, create `apikeys.properties` with your credentials
2. Open in Android Studio, select the `developmentDebug` build variant
3. Run on an emulator or device

Available demo screens: Payment Bottom Sheet, Reusable Payment Bottom Sheet, Basic Form, Additional Fields, Custom Theme, Flexible Expiry, Custom Text Fields, Recaching Showcase, 3DS Gateway-Specific, 3DS Forter, Offsite Payments, EBANX, Stripe APM, Braintree, Design System Showcase.

For detailed setup instructions (including Android Studio installation and emulator creation), see the [UAT Setup Guide](../../UAT_SETUP_GUIDE.md).

## See Also

- [Error Handling](error-handling.md) -- Error types, retry logic, user-facing messages
- [Troubleshooting](troubleshooting.md) -- Symptom-based troubleshooting across all flows
- [Getting Started](getting-started.md) -- Installation and first payment
- [Security](security.md) -- Screenshot protection, field encryption, PCI compliance
