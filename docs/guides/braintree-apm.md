# Braintree APM Integration Guide

A practical guide for integrating Braintree APM PayPal and Venmo payments into your Android app
using the Spreedly SDK's `:braintree` module.

## Table of Contents

- [Introduction](#introduction)
- [Braintree vs Other Payment Flows](#braintree-vs-other-payment-flows)
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [How Braintree Payments Work](#how-braintree-payments-work)
- [Backend Requirements](#backend-requirements)
- [Kotlin Integration](#kotlin-integration)
- [Java Integration](#java-integration)
- [onResume Handling](#onresume-handling)
- [PaymentResult Fields](#paymentresult-fields)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [API Reference](#api-reference)

---

## Introduction

### What are Braintree APM Payments?

Braintree APM payments allow customers to pay using their **PayPal** or **Venmo** accounts through
the native Braintree SDK. Unlike standard offsite payments that use Chrome Custom Tabs, Braintree
APM payments use the native PayPal/Venmo apps or a browser-based flow managed by the Braintree SDK.

### Key Differences from Other Flows

- **No tokenization step** — the merchant backend creates a pending purchase directly on the
  Braintree gateway with `offsite_sync: true`
- **Returns a nonce** — the SDK returns a payment nonce that the merchant must send to their
  backend to call Spreedly's `/confirm.json` endpoint
- **Native UI** — uses Braintree's native PayPal/Venmo SDK instead of Chrome Custom Tabs or
  Stripe PaymentSheet
- **Separate module** — Braintree SDK dependencies are isolated in the `:braintree` module, keeping
  `payments-core` lightweight

---

## Braintree vs Other Payment Flows

| Feature                | Offsite (CCT)       | Stripe APM                       | Braintree                                |
|------------------------|---------------------|----------------------------------|------------------------------------------|
| **Tokenization**       | SDK tokenizes first | None (backend creates purchase)  | None (backend creates purchase)          |
| **Checkout UI**        | Chrome Custom Tab   | Stripe PaymentSheet              | Native PayPal/Venmo SDK                  |
| **Return mechanism**   | Deep link redirect  | Activity result                  | Browser switch + deep link               |
| **Result**             | Transaction state   | Transaction state (polled)       | Payment nonce                            |
| **Confirmation**       | Automatic           | Automatic (via redirect trigger) | Merchant sends nonce via `/confirm.json` |
| **Module**             | `payments-core`     | `:stripe` (separate)             | `:braintree` (separate)                  |
| **Gateway dependency** | None                | Stripe SDK                       | Braintree SDK v5                         |

---

## Prerequisites

1. **Spreedly account** with a Braintree gateway configured
2. **Braintree merchant account** with PayPal and/or Venmo enabled
3. **Spreedly SDK initialized** via `sdk.init(options)`
4. See the [Compatibility table](../../README.md#compatibility) in the README for Android API level and other version requirements.

---

## Project Setup

### 1. Configure the Maven Repository

The Spreedly SDK is hosted on GitHub Packages. If you haven't already, add the repository to your
`settings.gradle.kts` as described in [Getting Started — Install](getting-started.md#1-install).

### 2. Add the Braintree Module Dependency

Add the `:braintree` module to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Includes payments-core transitively, plus Braintree SDK v5
    implementation("com.spreedly:checkout-braintree-apm:$spreedlyVersion")
}
```

This single dependency provides:

- All `payments-core` classes (via `api` dependency)
- `SpreedlyBraintreeAPMCheckout`, `BraintreeAPMCheckoutConfig`, `BraintreeAPMPaymentType`
- Braintree SDK v5 (PayPal, Venmo, DataCollector)

### 3. AndroidManifest (Automatic)

The `:braintree` module's manifest is merged automatically. It registers `BraintreeAPMActivity` with
the required intent filter for browser switch returns:

```xml
<activity
    android:name="com.spreedly.braintree.BraintreeAPMActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:theme="@style/Theme.Spreedly.Transparent">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="${applicationId}.braintree" />
    </intent-filter>
</activity>
```

The `${applicationId}` placeholder resolves to your app's package name at build time.

### 4. No Braintree Module = Zero Impact

If your app does **not** include the `:braintree` dependency:

- No Braintree SDK classes, Activity, or AAR in your APK
- `payments-core` has no Braintree dependency at all
- Compile-time safety: you can't even reference `SpreedlyBraintreeAPMCheckout`

---

## How Braintree Payments Work

```
┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────┐    ┌──────────┐
│ Merchant App │    │  SDK        │    │ BraintreeSDK │    │ Spreedly │    │ Backend  │
└──────┬───────┘    └──────┬──────┘    └──────┬───────┘    └────┬─────┘    └────┬─────┘
       │                    │                  │                 │               │
       │ 1. Backend creates purchase on Braintree gateway       │               │
       │────────────────────────────────────────────────────────►│               │
       │    (offsite_sync: true + gateway_specific_fields)       │               │
       │◄────────────────────────────────────────────────────────│               │
       │    (state: "processing", client_token in response)      │               │
       │                    │                  │                 │               │
       │ 2. present(config, activity)          │                 │               │
       │   (pass client_token from purchase response)            │               │
       │───────────────────►│                  │                 │               │
       │                    │                  │                 │               │
       │                    │ 3. Launch BraintreeAPMActivity        │               │
       │                    │─────────────────►│                 │               │
       │                    │                  │ 4. PayPal/Venmo │               │
       │                    │                  │    flow         │               │
       │                    │                  │◄────────────────│               │
       │                    │ 5. nonce + deviceData              │               │
       │                    │◄─────────────────│                 │               │
       │                    │                  │                 │               │
       │ 6. PaymentResult.Completed(nonce=...)  │                │               │
       │◄───────────────────│                  │                 │               │
       │                    │                  │                 │               │
       │ 7. Send nonce to backend              │                 │               │
       │────────────────────────────────────────────────────────────────────────►│
       │                    │                  │                 │               │
       │                    │                  │    8. POST /confirm.json        │
       │                    │                  │                 │◄──────────────│
       │                    │                  │                 │──────────────►│
       │ 9. Confirmation                       │                 │               │
       │◄───────────────────────────────────────────────────────────────────────│
```

### Step-by-Step

1. **Merchant backend** creates a purchase on the Braintree gateway via Spreedly API
   (with `offsite_sync: true` and `gateway_specific_fields`)
2. Purchase response returns `state: "processing"` and a `client_token` inside
   `gateway_specific_response_fields.braintree`
3. **App** passes the `client_token` from the purchase response into
   `BraintreeAPMCheckoutConfig` and calls `SpreedlyBraintreeAPMCheckout.present(config, activity)`
4. **SDK** launches `BraintreeAPMActivity` with the client token and payment config
5. **BraintreeAPMActivity** initializes the native PayPal/Venmo flow via the Braintree SDK
6. **User** completes payment; Braintree SDK returns a payment nonce + device data
7. **SDK** emits `PaymentResult.Completed` with the nonce via `paymentResultFlow`
8. **App** sends the nonce to the merchant backend
9. **Backend** calls Spreedly's `POST /v1/transactions/{token}/confirm.json` with the nonce
10. **Backend** returns confirmation to the app

---

## Backend Requirements

### Creating a Purchase

Your backend creates a purchase on the Braintree gateway. The `offsite_sync: true` flag and
`gateway_specific_fields` are required.

**PayPal example:**

```
POST https://core.spreedly.com/v1/gateways/{braintree_gateway_token}/purchase.json
Authorization: Basic base64(environment_key:access_secret)

{
  "transaction": {
    "amount": 1000,
    "currency_code": "USD",
    "payment_method": {
      "payment_method_type": "paypal",
      "offsite_sync": true
    },
    "gateway_specific_fields": {
      "braintree": {
        "paypal_flow_type": "checkout"
      }
    }
  }
}
```

**Venmo example:**

```
POST https://core.spreedly.com/v1/gateways/{braintree_gateway_token}/purchase.json
Authorization: Basic base64(environment_key:access_secret)

{
  "transaction": {
    "amount": 1000,
    "currency_code": "USD",
    "payment_method": {
      "payment_method_type": "venmo",
      "offsite_sync": true
    },
    "gateway_specific_fields": {
      "braintree": {
        "venmo_flow_type": "multi_use",
        "venmo_profile_id": "12345"
      }
    }
  }
}
```

**Key fields in the response:**

| Field               | Location                                                  | Description                                            |
|---------------------|-----------------------------------------------------------|--------------------------------------------------------|
| `transaction.token` | Top-level                                                 | Spreedly transaction token for the confirm call        |
| `transaction.state` | Top-level                                                 | `"processing"` for Braintree offsite purchases         |
| `client_token`      | `gateway_specific_response_fields.braintree.client_token` | Braintree client token for initializing the native SDK |

> **Important:** The transaction state is `"processing"` (not `"pending"`). The `client_token` is
> inside `gateway_specific_response_fields.braintree`, not at the top level of the transaction.

### Confirming with Nonce

After receiving the nonce from the SDK, your backend confirms the transaction using the
**`/confirm.json`** endpoint (not `/complete.json` — that's for 3DS):

```
POST https://core.spreedly.com/v1/transactions/{transaction_token}/confirm.json
Authorization: Basic base64(environment_key:access_secret)

{
  "state": "Successful",
  "nonce": "the-braintree-nonce",
  "device_data": "{\"correlation_id\":\"...\"}",
  "payment_method": {
    "payment_method_type": "paypal"
  }
}
```

> **Note:** The body is sent as a flat JSON object — it is **not** wrapped in `"transaction"` or
> `"context"`. Include `payment_method.payment_method_type` matching the type used in the purchase
> so Spreedly can update the payment method if the user switched (e.g., started PayPal but paid
> with Venmo).

See
the [Spreedly Braintree APM docs](https://docs.spreedly.com/payment-gateways/braintree#alternative-payment-methods)
for full details.

---

## Kotlin Integration

### 1. Subscribe to Payment Results

```kotlin
lifecycleScope.launch {
    spreedly.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                if (result.nonce != null) {
                    // Braintree flow: send nonce to your backend
                    confirmPayment(
                        transactionToken = result.token,
                        nonce = result.nonce,
                        deviceData = result.deviceData,
                        paymentMethodType = "paypal", // or "venmo"
                    )
                } else {
                    // Other flows (offsite, Stripe APM)
                    handleStandardCompletion(result)
                }
            }
            is PaymentResult.Failed -> handleFailure(result)
            is PaymentResult.Canceled -> handleCanceled()
            PaymentResult.Initial -> { /* ignore */ }
        }
    }
}
```

### 2. Build Configuration and Present

The `client_token` from the purchase response can be passed directly in the config, allowing the
SDK to skip an extra status API call:

```kotlin
// After your backend creates the purchase:
val purchaseResponse = myBackendApi.createBraintreePurchase(amount, "paypal")

val clientToken = purchaseResponse.transaction
    ?.gatewaySpecificResponseFields?.braintree?.clientToken
    ?: throw Exception("Missing client token")

val config = BraintreeAPMCheckoutConfig(
    transactionToken = purchaseResponse.transaction?.token
        ?: throw Exception("Missing transaction token"),
    paymentType = BraintreeAPMPaymentType.PAYPAL, // or VENMO
    merchantDisplayName = "My Store",
    clientToken = clientToken,        // from purchase response
    amount = "10.00",                 // human-readable amount
    currencyCode = "USD",
)

SpreedlyBraintreeAPMCheckout.present(config, activity)
```

> **Tip:** If `clientToken` is omitted, the SDK will fall back to fetching it from Spreedly's
> internal status API. Providing it directly is recommended for faster checkout.
>
> **Venmo note:** When `clientToken` is provided directly, the SDK does not query the status API
> and therefore does not automatically pick up `venmo_profile_id` from the purchase response.
> If your Venmo integration requires a specific profile ID, omit `clientToken` from the config so
> the SDK fetches it (along with the profile ID) from the status API.

### 3. Handle the Nonce

```kotlin
private fun confirmPayment(
    transactionToken: String,
    nonce: String,
    deviceData: String?,
    paymentMethodType: String,
) {
    lifecycleScope.launch {
        // Send to YOUR backend — never call Spreedly directly from the app in production
        val result = myBackendApi.confirmBraintreePayment(
            transactionToken = transactionToken,
            nonce = nonce,
            deviceData = deviceData,
            paymentMethodType = paymentMethodType,
        )
        // Handle confirmation result
    }
}
```

---

## Java Integration

The `:braintree` module provides `BraintreeAPMJavaHelper` with `@JvmStatic` methods and
`Consumer`/`Runnable` callbacks for Java consumers:

```java
import com.spreedly.braintree.BraintreeAPMJavaHelper;
import com.spreedly.braintree.BraintreeAPMCheckoutConfig;
import com.spreedly.braintree.BraintreeAPMPaymentType;

// Start monitoring before presenting checkout
BraintreeAPMJavaHelper.startPaymentResultMonitoring(
    this, // LifecycleOwner
    sdk,
    result -> {
        if (result.getNonce() != null) {
            // Braintree flow: send nonce to your backend for confirmation
            confirmPayment(result.getToken(), result.getNonce(), result.getDeviceData());
        } else {
            Log.d("Payment", "Completed: " + result.getState());
        }
    },
    result -> {
        Log.e("Payment", "Failed: " + result.getDescription());
    },
    () -> {
        Log.d("Payment", "Canceled by user");
    }
);

// Build config using the Java-friendly factory method
BraintreeAPMCheckoutConfig config = BraintreeAPMJavaHelper.createConfig(
    transactionToken,                    // from purchase response
    BraintreeAPMPaymentType.PAYPAL,      // or VENMO
    "My Store",                          // merchant display name
    clientToken,                         // from purchase response (nullable)
    "10.00",                             // amount (nullable)
    "USD"                                // currency code (nullable)
);

// Present the Braintree checkout
BraintreeAPMJavaHelper.presentCheckout(config, this);

// Check status
boolean active = BraintreeAPMJavaHelper.isCheckoutActive();
boolean ready = BraintreeAPMJavaHelper.isInitialized(sdk);
```

**Java Notes:**

- `BraintreeAPMJavaHelper` lives in the `:braintree` module (`com.spreedly.braintree`)
- All methods are `@JvmStatic` — call directly on the class
- Uses `Consumer<PaymentResult.Completed>`, `Consumer<PaymentResult.Failed>`, and `Runnable` for
  callbacks
- When `result.getNonce()` is non-null, the merchant must complete the transaction by sending
  the nonce to their backend
- See [`BraintreeAPMJavaHelper.kt`](../../braintree/src/main/java/com/spreedly/braintree/BraintreeAPMJavaHelper.kt) for the full implementation

---

## onResume Handling

The dedicated `BraintreeAPMActivity` manages the Braintree SDK lifecycle internally, so
`finalizeIfActive()` is effectively a no-op for Braintree flows. It is provided for API
consistency with `SpreedlyOffsiteCheckout`, which does require `onResume` handling.

You may still call it in `onResume` for forward-compatibility:

```kotlin
override fun onResume() {
    super.onResume()
    SpreedlyBraintreeAPMCheckout.finalizeIfActive()
}
```

---

## PaymentResult Fields

When `PaymentResult.Completed` is received from a Braintree flow:

| Field                   | Type      | Description                                               |
|-------------------------|-----------|-----------------------------------------------------------|
| `token`                 | `String`  | Spreedly transaction token (use for `/confirm.json` call) |
| `nonce`                 | `String?` | Braintree payment nonce (non-null for Braintree flows)    |
| `deviceData`            | `String?` | Braintree device fingerprint data for fraud detection     |
| `state`                 | `String?` | `"pending"` — transaction awaits nonce confirmation       |
| `paymentMethodResponse` | `null`    | Not populated for Braintree flows                         |

**Key indicator:** When `nonce` is non-null, the merchant must complete the transaction by sending
the nonce to their backend, which calls Spreedly's `/confirm.json` endpoint.

> **Note on `state`:** The purchase response from Spreedly returns `state: "processing"`, but the
> SDK emits `PaymentResult.Completed` with `state = "pending"`. The `"pending"` state indicates
> the transaction awaits nonce confirmation via `/confirm.json` and does not reflect an error.

---

## Error Handling

### Configuration Validation Errors

`SpreedlyBraintreeAPMCheckout.present()` validates the config before proceeding:

- Empty `transactionToken` → `PaymentResult.Failed` with descriptive message

### Braintree SDK Errors

Errors from the Braintree SDK (PayPal/Venmo flow failures) are surfaced as
`PaymentResult.Failed` with `errorType = UNKNOWN_ERROR` and the original error in `message`.

### Network Errors

If the SDK fails to fetch the `client_token` from the transaction status API (only when
`clientToken` is not provided in the config):

- `PaymentResult.Failed` with `message = "Failed to fetch Braintree client token"`

### Missing Client Token

If the transaction status response doesn't contain a `client_token`:

- `PaymentResult.Failed` with
  `message = "Braintree client_token not available in transaction status"`

### Common Confirm Errors

| Error                                                  | Cause                                               | Fix                                                                        |
|--------------------------------------------------------|-----------------------------------------------------|----------------------------------------------------------------------------|
| "gateway does not support offsite_purchase"            | Wrong gateway token or missing `offsite_sync: true` | Use a Braintree gateway and include `offsite_sync: true` in payment_method |
| "transaction can only be completed in 'pending' state" | Called `/complete.json` instead of `/confirm.json`  | Use `POST /v1/transactions/{token}/confirm.json`                           |

---

## Testing

### Test Gateway Setup

1. Configure a **Braintree sandbox** gateway in your Spreedly environment
2. Use sandbox credentials for PayPal and Venmo testing
3. Set `braintreeGatewayToken` in your `apikeys.properties` to the sandbox gateway token

### PayPal Sandbox

Use PayPal sandbox buyer accounts to test the PayPal checkout flow. The Braintree sandbox
automatically connects to PayPal's sandbox environment.

### Venmo Test Flows

Venmo testing requires the Venmo app installed on the test device (or use the Braintree SDK's
test mode which simulates the Venmo flow).

---

## Troubleshooting

### Common Issues

**"Missing required Braintree payment configuration"**

- Ensure all required intent extras are passed. This is handled internally by the SDK — if you
  see this, the `BraintreeAPMCheckoutConfig` fields may be invalid.

**"Braintree client_token not available in transaction status"**

- The purchase was not created on a Braintree gateway, or the gateway is not properly configured.
- Verify the gateway token and that the purchase response state is `"processing"`.

**"gateway does not support offsite_purchase"**

- The gateway token is not a Braintree gateway, or `offsite_sync: true` is missing from the
  purchase request's `payment_method`.
- Ensure `gateway_specific_fields.braintree` includes the required flow type
  (e.g., `venmo_flow_type` or `paypal_flow_type`).

**"transaction can only be completed in 'pending' state and have a required_action of
'device_fingerprint' or 'challenge'"**

- You are calling the wrong endpoint. Use `/confirm.json` for Braintree offsite transactions,
  not `/complete.json` (which is for 3DS).

**Browser switch doesn't return to app**

- Verify the intent filter in your merged manifest includes
  `android:scheme="${applicationId}.braintree"`
- Check for manifest merge conflicts that may override the scheme

**Nonce expires before confirmation**

- Braintree nonces have a limited lifetime. Confirm the transaction promptly after receiving
  the nonce.

**Manifest merge conflicts**

- If another library registers an Activity with the same intent filter scheme, use manifest
  merger rules (`tools:replace`, `tools:node`) to resolve conflicts.

---

## API Reference

### BraintreeAPMCheckoutConfig

```kotlin
data class BraintreeAPMCheckoutConfig(
    val transactionToken: String,          // Spreedly transaction token
    val paymentType: BraintreeAPMPaymentType, // PAYPAL or VENMO
    val merchantDisplayName: String = "",  // Name shown in payment UI
    val clientToken: String? = null,       // Braintree client_token (recommended)
    val amount: String? = null,            // e.g. "10.00"
    val currencyCode: String? = null,      // e.g. "USD"
)
```

> When `clientToken` is provided, the SDK launches the Braintree flow immediately.
> When omitted, the SDK fetches it from the Spreedly status API (slower, may fail for some
> transaction types).

### BraintreeAPMPaymentType

```kotlin
enum class BraintreeAPMPaymentType(val value: String) {
    PAYPAL("paypal"),
    VENMO("venmo"),
}
```

### SpreedlyBraintreeAPMCheckout

| Method                      | Description                                              |
|-----------------------------|----------------------------------------------------------|
| `present(config, activity)` | Launch the Braintree payment flow                        |
| `finalizeIfActive()`        | No-op; kept for API consistency with offsite checkout    |
| `isActive()`                | Check if a checkout is in progress                       |
| `currentTransactionToken()` | Get the current transaction token (if active)            |
| `callerActivityClassName()` | Get the class name of the Activity that started checkout |

### PaymentResult.Completed (Braintree-specific fields)

| Field        | Type      | Description                            |
|--------------|-----------|----------------------------------------|
| `nonce`      | `String?` | Braintree payment nonce                |
| `deviceData` | `String?` | Device fingerprint for fraud detection |

### Spreedly API Endpoints

| Step     | Endpoint                                                | Purpose                                          |
|----------|---------------------------------------------------------|--------------------------------------------------|
| Purchase | `POST /v1/gateways/{token}/purchase.json`               | Create a pending Braintree offsite purchase      |
| Confirm  | `POST /v1/transactions/{token}/confirm.json`            | Complete the transaction with the nonce          |
| Status   | SDK-internal endpoint (not for merchant use)             | Fetch client_token (SDK internal, fallback only) |
