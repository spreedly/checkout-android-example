# Changelog

All notable changes to the Spreedly Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-05-08

### Added

- **SDK version attribution on API requests** -- all requests now include platform and version identifiers, enabling Spreedly support to diagnose integration issues faster.

## [1.0.0] - 2026-04-30

### Breaking Changes

- None. This major version bump signals API stability, not API change. All 0.14.x APIs remain unchanged.

### Added

- **First public release** -- Spreedly Android Checkout SDK reaches 1.0 GA. All public APIs are now covered by semantic versioning guarantees.

## [0.14.1] - 2026-04-30

### Fixed

- **Apache-2.0 LICENSE in published artifacts** -- each AAR now embeds `META-INF/LICENSE` inside `classes.jar`, ensuring compliance with Apache 2.0 Section 4 distribution requirements.

### Changed

- **Release pipeline reliability** -- GPG-signed tags on the maven distribution repository, CI runner stability guards (JVM heap limits, Gradle daemon control, job timeouts), PR-based dist-repo synchronization, and DAST security scanning integration.

## [0.14.0] - 2026-04-29

### Added

- **`placeholderColor` theming support** -- `CustomFieldsConfig`, `PaymentSheetConfig`, and `SpreedlyColors` now expose a `placeholderColor` property for explicit control over hint/placeholder text color. Defaults to `Color.Unspecified` (falls back to label color, then `onSurfaceVariant`).

### Changed

- **AppTextField default colors** -- Label fallback is now `onSurfaceVariant` (was `onSurface`), background is `surfaceContainerHigh` (was `surface`), unfocused border is `outlineVariant` (was `outline`). These follow Material 3 dark mode conventions. Merchants who explicitly set these colors are unaffected.
- **AppTextField label color transitions** -- Labels now animate between the focused border color (focused) and `onSurfaceVariant` (unfocused), matching Material 3 `OutlinedTextField` behavior.
- **Segmented button inactive state** -- Inactive segments now use transparent background, `onSurfaceVariant` text, and `outlineVariant` border for better dark mode contrast.

### Fixed

- **Bank account sheet dark mode** -- `ModalBottomSheet` popup window now inherits the parent theme via `MaterialTheme(colorScheme = parentScheme)`, fixing invisible text, white backgrounds, and black header text in dark mode.
- **Placeholder text invisible in dark mode** -- Hint text in bank account form fields is now visible in both light and dark themes.

## [0.13.0] - 2026-03-31

### Added

- **ACH Bank Account Payments**: Tokenize bank accounts via `SpreedlyBankAccountBottomSheet` (pre-built UI), `BankAccountSheet` (custom layouts), or headless `sdk.createBankAccount()`. Includes routing/account number validators, configurable field visibility via `BankAccountFieldConfig` (Default, Minimal, Full presets), separate or full name modes, and secure auto-clear for account numbers.
- **`BankAccountFieldConfig`**: Data class controlling which optional fields appear in the bank account form (account type, holder type, bank name, name display mode).
- **`FormFieldType.ROUTING_NUMBER` / `ACCOUNT_NUMBER`**: New form field types for bank account secure fields with built-in validation.
- **`PaymentMethodType` enum**: Distinguishes `CREDIT_CARD` and `BANK_ACCOUNT` payment methods.

### Changed

- **Build toolchain**: Kotlin **2.3.10** (pinned below 2.3.20 until CodeQL supports it; see [github/codeql#21484](https://github.com/github/codeql/issues/21484)), KSP **2.3.6** (KSP2 versioning), Android Gradle Plugin **8.13.2**. README compatibility table lists **AGP** and **KSP** alongside Kotlin and Gradle for integrator alignment.

## [0.12.1] - 2026-03-17

### Fixed

- **Stripe APM SEPA Direct Debit**: Payments no longer show Failed when transaction state is `"processing"`.
  The SDK now treats `"processing"` as non-terminal and continues polling; when max retries are exhausted
  with `"processing"`, it returns `PaymentResult.Completed` so merchants can display an appropriate
  success message (e.g. "Payment accepted and is being processed").

## [0.12.0] - 2026-03-13

### Added

- **`source` field on payment method creation**: All payment method creation requests (credit card and offsite/APM) now include `source` identifying the checkout SDK and platform (e.g. `"checkout-android"`, `"checkout-react-native"`).

### Changed

- **`sdkPlatform` is now a `SdkPlatform` enum**: Replaced the `String` parameter on `SpreedlySDKInitOptions` with a type-safe `SdkPlatform` enum (`ANDROID`, `REACT_NATIVE`). The enum's `value` property (`"checkout-android"` / `"checkout-react-native"`) is used for both Datadog telemetry and the Core API `source` field. **Breaking**: callers passing `sdkPlatform = "react_native"` must change to `sdkPlatform = SdkPlatform.REACT_NATIVE`.

## [0.11.3] - 2026-03-13

### Added

- **`User-Agent` header on API requests**: All outbound HTTP requests to `core.spreedly.com` now include `User-Agent: Spreedly-Android-SDK/{version}` for server-side request identification.

## [0.11.2] - 2026-03-12

### Added

- **Auth Tab for 3DS browser flows**: All gateway-specific 3DS flows (device fingerprint, challenge, redirect) now use Auth Tab on Chrome 137+. Auth Tab provides a stripped-down browser UI with no address bar, bookmarks, or "Open in Chrome" — preventing users from navigating away during 3DS authentication. On older Chrome or non-Chrome browsers, the SDK falls back to Chrome Custom Tabs automatically.
- Separate `ActivityResultLauncher` instances for fingerprint and challenge flows, preventing launcher state conflicts when both flows run sequentially.

### Changed

- `androidx.browser` dependency bumped from 1.8.0 to 1.9.0 to enable Auth Tab.
- 3DS browser flows use Auth Tab as the primary mechanism with Chrome Custom Tab as automatic fallback.
- Card number fields now allow paste (copy/cut remain blocked for PCI compliance). Pasted numbers with spaces, dashes, or dots are accepted after stripping non-digit characters.

### Fixed

- Auth Tab resolves the known issue where users could navigate from a Chrome Custom Tab to the Chrome browser during 3DS challenges, breaking the return flow. Auth Tab's stripped-down UI eliminates this navigation path entirely.

## [0.11.1] - 2026-03-11

### Added

- **`sdk_platform` global telemetry attribute**: New `sdkPlatform` field on `SpreedlySDKInitOptions` (default `"android"`). React Native bridges pass `"react_native"` to distinguish integration surface in Datadog (`@sdk_platform:android` vs `@sdk_platform:react_native`).

## [0.11.0] - 2026-03-11

### Added

- **3DS Module** (`checkout-threeds`): Extracted all 3DS functionality into a dedicated module
    - Global 3DS (Forter) integration: `SpreedlyThreeDSProvider` auto-registers via `ThreeDSInitProvider`
    - Gateway-Specific 3DS: Chrome Custom Tab challenge flow, status polling, lifecycle management
    - Shared UI: `ThreeDSChallengeSheet`, `ThreeDSChallengeContent`, `ThreeDSChallengeBottomSheet`
    - Provider interface: `ThreeDSProvider` in `payments-core` allows optional 3DS without coupling
- SDK-owned `OffsiteReturnActivity` with scheme `${applicationId}.spreedlyoffsite`
    - Merchants no longer need to create their own return activity or register intent filters
    - Helper methods: `SpreedlyOffsiteCheckout.redirectUrl(context)` and `deepLinkScheme(context)`
- **Datadog telemetry**: Structured telemetry events (`SpreedlyEvent` sealed class) covering SDK initialization, payment method creation, recache, 3DS, APM checkout, API requests, hosted field interactions, offsite payments, and validation failures.
- **Event sanitization**: `LogSanitizer` strips card numbers, CVVs, API keys, and signatures from log messages. URL paths are sanitized separately to strip payment method and transaction tokens.
- **Environment key masking**: `LogSanitizer.maskEnvironmentKey()` masks environment keys to their first 4 characters in all Datadog attributes.
- **Global attributes**: Every Datadog log includes `sdk_version`, `session_id`, masked `environment_key`, `region`, `carrier`, `platform`, and `os_version`.
- **Monotonic timing**: All performance measurements use `kotlin.time.TimeSource.Monotonic` for accurate, wall-clock-independent durations.
- **Typed telemetry attributes**: `Long`, `Boolean`, and `Int` values are preserved (not stringified) when sent to Datadog, enabling native facets and metrics.
- **`EventEmitter` singleton**: Central dispatch point for all structured telemetry events routed to `LoggerManager.emitEvent()`.
- **Configurable Datadog log level**: `sdk.setDatadogLogLevel()` and `LoggerManager.setDatadogLogLevel()` allow runtime control of which events reach Datadog.
- **`FlowDurationTracker`**: Thread-safe utility for measuring flow durations, replacing inline `ConcurrentHashMap<String, TimeMark>` pattern in APM modules.

### Changed

- **Breaking: 3DS package rename** -- `com.spreedly.sdk.threeds.*` is now `com.spreedly.threeds.*`. Update imports:
    - `com.spreedly.sdk.threeds.gatewayspecific.GatewaySpecific3DSIntegration` -> `com.spreedly.threeds.gatewayspecific.GatewaySpecific3DSIntegration`
    - `com.spreedly.sdk.ui.threeds.ThreeDSChallengeSheet` -> `com.spreedly.threeds.ui.ThreeDSChallengeSheet`
- **Breaking: Offsite deep link scheme changed** -- The SDK now owns `OffsiteReturnActivity` with scheme `${applicationId}.spreedlyoffsite`. Merchants must:
    1. Remove their custom `OffsiteReturnActivity` and its manifest entry
    2. Update backend `redirect_url` to use `SpreedlyOffsiteCheckout.redirectUrl(context)` (e.g. `com.myapp.spreedlyoffsite://checkout`)
    3. Remove old `spdlapp://` intent filter from their manifest
- **Breaking: 3DS dependency moved** -- Merchants using 3DS must add `com.spreedly:checkout-threeds` as a dependency. The Forter SDK dependency moved from `checkout-payments-core` to `checkout-threeds`.
- **Breaking: `SavedCardInfo.cardBrand` renamed to `cardholderName`** -- The field was only ever used for cardholder names; `cardType` already carries the brand. Migration:
    - Kotlin: `SavedCardInfo(cardBrand = "...")` -> `SavedCardInfo(cardholderName = "...")`
    - `RecacheJavaHelper.createRecacheConfig` / `createRecacheConfigFull`: 3rd parameter renamed from `cardBrand` to `cardholderName` (positional Java callers are unaffected)
- `Spreedly` class now delegates 3DS operations to the registered `ThreeDSProvider`. Public API (`showThreeDSChallenge()`, `hideThreeDSChallenge()`, all flows and state) remains unchanged.
- `HostedFieldInteraction` log level raised from `DEBUG` to `INFO` so field-level telemetry reaches Datadog at default settings.
- Stripe APM `ApmCheckoutCompleted` emission moved from before to after `pollForFinalStatus`, reflecting the true payment outcome.
- All `AppNetworkError` log calls use `.safeDescription()` instead of `.toString()` to prevent raw error bodies from reaching logs.

### Fixed

- APM timing race condition: replaced `@Volatile var` with `FlowDurationTracker` (backed by `ConcurrentHashMap<String, TimeMark>`) in Braintree and Stripe APM modules.
- Missing `ThreeDSCompleted` events in Forter delegate, Forter integration, ThreeDSChallengeContent (7 paths), ThreeDSTransactionCompleter, SpreedlyThreeDSProvider, and GatewaySpecific3DS lifecycle/return flows.
- Missing `ApmCheckoutCompleted` events for 3 Braintree `fetchClientTokenAndLaunch` failure paths.
- Raw payment method and transaction tokens leaked in Logcat/Datadog log messages from `RequestHandler` -- now sanitized via `LogSanitizer.sanitizeUrl()`.
- Raw 3DS activity result data removed from log messages in `ThreeDSChallengeContent`.

### Removed

- Fat AAR build task and `scripts/publish-fat-aar.sh` (deprecated since 0.7.1)
- OWASP Dependency Check scan job, Gradle plugin, and related configuration (SEC-4574). Dependabot
  and the Dependency Review Action provide equivalent coverage with faster CI times.

## [0.10.4] - 2026-03-03

> **Note:** Versions 0.10.2 and 0.10.3 were published as internal/unofficial builds and should not be used.
> Version 0.10.4 is the official release following 0.10.1.

### Added

- **Stripe APM Module** (`checkout-stripe-apm`): Stripe Alternative Payment Methods via native PaymentSheet
    - Supports iDEAL, Bancontact, EPS, P24, SEPA, and other Stripe-supported APMs
    - Dedicated `:stripe` module with `SpreedlyStripeAPMCheckout` entry point
    - `StripeAPMConfig` for configuration, `StripeAPMJavaHelper` for Java interop
    - Backend-initiated flow: merchant creates purchase, SDK presents PaymentSheet
    - Automatic redirect trigger and status polling after PaymentSheet completion
- **Braintree APM Module** (`checkout-braintree-apm`): Braintree PayPal and Venmo payments via native SDK
    - Dedicated `:braintree` module with `SpreedlyBraintreeAPMCheckout` entry point
    - `BraintreeAPMCheckoutConfig` and `BraintreeAPMPaymentType` (PAYPAL, VENMO)
    - `BraintreeAPMJavaHelper` for Java interop
    - Nonce-based flow with merchant `/confirm.json` call
- **Gateway-Specific 3DS via Chrome Custom Tabs**: 3DS challenge flow for supported gateways
    - Chrome Custom Tab-based challenge presentation (no manual WebView handling)
    - Transparent host activity for reliable CCT lifecycle management
    - Unified `ThreeDSChallengeSheet` component for both Global and Gateway-Specific flows
    - Device fingerprint and challenge handling with automatic status polling
    - `GatewaySpecific3DSReturnActivity` with singleton fallback for `callerClassName`
- `TransactionStatus.isTerminal` convenience property for checking terminal state (succeeded or failed)
- Offsite payment message alignment with iOS SDK for cross-platform consistency
- Unified purchase API clients with merchant backend routing
- Expanded CI workflow to include Stripe and Braintree modules with parallel jobs

### Changed

- `GatewaySpecific3DSIntegration.returnActivityClassName()` visibility reduced from `public` to `internal` (not part of documented public API)
- Consolidated CI into single code-quality workflow with parallel jobs
- Stripe and Braintree modules added to publish script, coverage reporting, and documentation generation

### Fixed

- Expanded terminal state detection to include `gateway_processing_result_unknown` and `gateway_setup_failed` in `TransactionStatus.failed`, preventing unnecessary polling timeouts
- Thread safety in `GatewaySpecific3DSIntegration`: `pendingFlowData` now uses `AtomicReference` for atomic consume-once semantics; `callerActivityClassName` is `@Volatile`
- Chrome Custom Tab dismissal: added `REORDER_TASKS` permission for reliable `ActivityManager.moveTaskToFront()` when 3DS flow completes while CCT is still on screen
- `GatewaySpecific3DSReturnActivity` now retrieves `callerClassName` from the `GatewaySpecific3DSIntegration` singleton as fallback, removing dependency on Intent extras
- Consistent use of `TransactionStatus.isTerminal` in `GatewaySpecific3DSLifecycle` device fingerprint polling callback
- GitHub workflow validation errors
- Hardcoded version references in publish script

### Removed

- Dead `continueChallengePolling()` method from `GatewaySpecific3DSLifecycle`
- 3DS fallback path from `OffsiteReturnActivity` (3DS now uses dedicated return activity)

### Known Issues

- **Gateway-Specific 3DS Chrome Custom Tab Redirect**: Resolved in the next release via Auth Tab (Chrome 137+). On older browsers using the CCT fallback, a user can still navigate from the Custom Tab to the full Chrome browser during a 3DS challenge, preventing automatic redirect back to the app.

## [0.10.1] - 2026-02-25

### Added

- **Stripe APM Integration**: Initial Stripe APM offsite payment integration with native PaymentSheet (HC-1179)
- **Braintree PayPal/Venmo Integration**: Braintree PayPal and Venmo payment support (HC-1180)
- **Module Extraction**: Extracted Stripe APM into dedicated `:stripe` module and renamed Braintree APIs with APM suffix (HC-1182)
- **Gateway-Specific 3DS Chrome Custom Tabs**: Transitioned Gateway-Specific 3DS from WebView to Chrome Custom Tabs (HC-1183)
- Build configuration for Braintree and Stripe modules in coverage and documentation generation (HC-1184)
- Updated README for version 0.10.1 with APM module documentation (HC-1186)

## [0.10.0] - 2026-02-20

### Added

- **Offsite Payment Integration (SPREL)**: Chrome Custom Tabs-based offsite payment flow for SPREL and PayPal (HC-1174)
    - Deep link return handling via `OffsiteReturnActivity`
    - Automatic Chrome Custom Tab dismissal
    - `paymentResultFlow` for reactive result delivery
- **EBANX Payment Integration**: Support for Pix, Boleto, OXXO, and NuPay via EBANX (HC-1177)
- **Allow Blank Date Validation Parameter**: `ALLOW_BLANK_DATE` validation parameter for optional expiration date fields (HC-1140)
    - `ValidationParameter.ALLOW_BLANK_DATE` enum entry for type-safe parameter setting
    - `allowBlankDate` property in `SpreedlyParamsManager` for global configuration
    - `allowBlankDate` parameter in `createPaymentMethod()` and recaching API methods
    - `allowBlankDate` parameter in `SpreedlyBottomSheet` composable
    - "All-or-nothing" validation logic: blank dates are valid, but partial dates require completion
- **Dark Mode Support**: Full dark mode support with `null` for empty fields (HC-609)
- **Gateway-Specific 3DS**: Initial Gateway-Specific 3DS feature (HC-1152)

### Fixed

- CVV clear behavior (HC-628)
- Combined expiry date validation for 2-digit years (HC-633)
- 3DS bottom sheet issues (HC-1129)
- Recaching validation improvements and enhanced test coverage (HC-1146)
- Status API fixes (HC-1150)
- 3DS authentication fixes (HC-1165)
- CodeQL security findings (HC-1164)

## [0.7.1] - 2024-12-30

> **Note**: As of SDK 0.10.4, the Forter 3DS SDK is included as a transitive dependency of
> `checkout-threeds`. Merchants need to add `com.spreedly:checkout-threeds` for 3DS support.
> See [README.md](../README.md) for current integration instructions.

### 🚨 BREAKING CHANGES

This release introduces a new multi-module publishing structure that replaces the single fat AAR. This is a **breaking change** that requires dependency updates.

#### Migration Required

**Old (0.7.0 and earlier):**
```kotlin
implementation("com.spreedly:checkout-android:0.7.0")
```

**New (0.7.1+):**
```kotlin
implementation("com.spreedly:checkout-payments-core:0.7.1")
implementation("com.spreedly:checkout-hostedfields:0.7.1")
implementation("com.spreedly:checkout-paymentsheet:0.7.1")
// Forter 3DS SDK: add explicitly at 0.7.1; transitive as of 0.10.4 (see note above)
```

### Added

- 📦 **Multi-Module Publishing**: SDK now published as three separate modules
    - `checkout-payments-core`: Core payment processing, API client, and 3DS support
    - `checkout-hostedfields`: Secure individual payment field components
    - `checkout-paymentsheet`: Pre-built payment sheet UI
- 🔗 **Proper Dependency Management**: Transitive dependencies (Ktor, Compose) now correctly exposed via `api` scope
- 📚 **Bundled Core Modules**: All internal core modules now bundled into `payments-core` for cleaner architecture

### Changed

- ⚡ **Artifact ID Change**: Changed from `checkout-android` to module-specific IDs
- 🏗️ **Build System**: Replaced fat AAR with standard multi-module publishing
- 📦 **Forter SDK**: Now required as explicit dependency (not bundled)

### Fixed

- 🐛 **Resources$NotFoundException**: Fixed string resource issues by properly bundling all resources into `payments-core`
- 🔧 **NoClassDefFoundError**: Fixed Ktor dependency issues with proper `api` scope
- 📦 **POM Generation**: Fixed dependency declarations in published POMs

### Removed

- 🗑️ **Fat AAR**: Removed single fat AAR in favor of modular approach
- 🗑️ **Bundled Forter**: Forter SDK no longer bundled, must be added explicitly

## [0.7.0] - 2024-12-30

### Added

- 💾 **Save Card for Future Payments**: Added `shouldRetain` property to `PaymentResult.Completed` to indicate whether users want to save their payment method
    - New `retainOnSuccess` parameter in payment creation methods (`createPaymentMethod`, `createCreditCard`)
    - `SaveCardCheckbox` composable component for allowing users to opt-in to saving cards
    - Automatic extraction of `retained` value from API response to `PaymentResult`
    - Comprehensive documentation on secure token storage best practices
- 🎯 **Focus Tracking Callback**: Added `onFocus` callback parameter to `SPLTextField` and `AppTextField`
    - Triggered when user taps into a field (field gains focus)
    - Enables custom focus management and field tracking
    - Complements existing `shouldFocus` parameter for complete focus control
    - Useful for analytics, custom UI feedback, and field-specific help displays

### Security

- 🔒 **Payment Token Storage**: Added guidance on secure payment token storage using encrypted SharedPreferences and Android Keystore

## [0.0.3] - 2025-08-25

### Added

- 🔧 **AuthService in App Module**: Added `AuthService` class in app module to handle authentication
  parameter retrieval from backend
- ♿ **Accessibility Enhancements**: Enhanced UI components with comprehensive accessibility support
    - Added content descriptions for all interactive elements
    - Implemented semantic properties for screen readers
    - Improved navigation and focus management

### Changed

- 🔄 **BREAKING CHANGE**: SDK initialization now requires authentication parameters to be fetched by
  the app
    - Removed `suspend fun init(environmentKey: String, config: PaymentSheetConfig)` from SDK
    - Apps must now implement their own `AuthService` to fetch auth parameters from backend
    - Updated all example activities to use new initialization pattern
    - Updated `SpreedlyHelper.initializeSdkWithRemoteAuth()` to handle auth parameter fetching

### Deprecated

### Removed

- ❌ **AuthParamsService**: Removed from SDK core - authentication API calls now handled by app layer
- ❌ **Suspend init method**: Removed `suspend fun init(environmentKey, config)` - replaced with
  explicit auth parameter passing
- ❌ **Auth-related tests**: Removed SDK tests for authentication parameter fetching since it's now
  app responsibility

### Fixed

- 🐛 **Card Number Visual Transformation**: Fixed cursor positioning bug in card number formatting
    - Resolved crash when typing 4th digit in unknown card types
    - Improved offset mapping for dynamic card number formatting
    - Enhanced handling of space positions in formatted card numbers
- 🎨 **Code Formatting**: Applied trailing comma formatting in SPLTextField component

### Security

- 🔒 **Improved Security**: Authentication parameters are now fetched by the app, giving developers
  full control over the authentication flow

## [0.0.1] - 2025-08-08

> **Historical**: This entry reflects the original single-module API (`checkout-android`). The SDK
> was restructured into multi-module architecture in 0.7.1. See 0.7.1 migration notes above.

### Added

- 🎉 **Initial Release** of Spreedly Android SDK
- 💳 **Core Payment Processing** with secure tokenization
- 🎨 **Modern UI Components** built with Jetpack Compose
- 🏗️ **Modular Architecture** with separate modules:
    - `payments-core`: Core payment processing (includes analytics, networking, validation, result types, security, and UI)
    - `paymentsheet`: Pre-built payment sheet UI components
    - `hostedfields`: Secure payment field components
    - `braintree`: Braintree APM (PayPal, Venmo)
    - `stripe`: Stripe APM (iDEAL, Bancontact, EPS, P24, SEPA)
    - `threeds`: 3D Secure authentication

### Features

- ✅ **Secure Payment Field Components**
    - Card number validation with real-time formatting
    - Expiry date validation (month/year)
    - CVV validation with card type detection
    - Address field validation

- ✅ **Payment Sheet Integration**
    - Bottom sheet payment form
    - Inline payment forms
    - Customizable styling and theming
    - Support for additional billing fields

- ✅ **Form Validation**
    - Real-time validation feedback
    - Comprehensive validator classes:
        - `CardNumberValidator`: Validates card numbers with Luhn algorithm
        - `YearValidator`: Validates expiry years (current year and future)
        - `MonthValidator`: Validates months (1-12 range)
        - `CvvValidator`: Validates CVV codes
        - `ExpiryDateValidator`: Validates expiry date formats
        - `RequiredValidator`: Validates required fields

- ✅ **Network Layer**
    - Ktor-based HTTP client
    - Robust error handling
    - Request/response logging
    - Timeout configuration

- ✅ **Security Features**
    - Secure tokenization of payment data
    - PCI-compliant payment field handling
    - Data encryption utilities

### Technical Features

- 🔧 **Build System**
    - Gradle Kotlin DSL with convention plugins
    - Multi-module architecture
    - Build flavors (development/production)
    - Version catalog for dependency management

- 🧪 **Testing & Quality**
    - Comprehensive unit test coverage
    - Code coverage reporting with Kover
    - Lint checks with ktlint and Android Lint
    - Compose-specific lint checks (Slack compose-lint-checks)
    - Automated code formatting with Spotless

- 🚀 **CI/CD Pipeline**
    - GitHub Actions workflows for CI/CD
    - Automated testing and building
    - Code quality checks with coverage reporting
    - Manual release workflow
    - Publishing to private Maven repository

### Developer Experience

- 📖 **Documentation**
    - Comprehensive README with integration guides
    - API documentation
    - Code examples and usage patterns
    - Workflow documentation

- 🛠️ **Development Tools**
    - Custom lint checks and rules
    - Code generation scripts
    - Coverage report generation
    - Local development setup scripts

### Installation

```kotlin
// In settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/spreedly/checkout-android-maven")
            credentials {
                username =
                    providers.gradleProperty("gpr.usr").orNull ?: System.getenv("GITHUB_USERNAME")
                password =
                    providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        google()
        mavenCentral()
    }
}

// In app/build.gradle.kts
dependencies {
    implementation("com.spreedly:checkout-android:0.0.1") {
        exclude(group = "checkout-android-sdk.core")
    }
}
```

### Usage Example

```kotlin
// Initialize SDK
val sdk = Spreedly()
val options = SpreedlySDKInitOptions(
    token = "your_token",
    nonce = "your_nonce",
    signature = "your_signature",
    certificateToken = "your_certificate_token",
    timestamp = "your_timestamp",
    environmentKey = "your_environment_key"
)
sdk.init(options)

// Show payment sheet
sdk.expressCheckout()

// Handle results
lifecycleScope.launch {
    sdk.paymentResultFlow.collect { result ->
        when (result) {
            is PaymentResult.Completed -> {
                // Handle successful payment
                val token = result.token
            }
            is PaymentResult.Failed -> {
                // Handle error
                val error = result.message
            }
            is PaymentResult.Canceled -> {
                // Handle cancellation
            }
        }
    }
}
```

---

## Release Notes

### Versioning

This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html):

- **MAJOR** version for incompatible API changes
- **MINOR** version for backwards-compatible functionality additions
- **PATCH** version for backwards-compatible bug fixes

### Support

- **Minimum Android API**: 26
- **Target Android API**: 35 (Android 15)
- **Kotlin Version**: 2.3.10
- **KSP** (when using processors such as Hilt or Room): 2.3.6
- **Android Gradle Plugin** (reference): 8.13.2
- **Compose BOM**: 2025.10.01

For detailed integration guides and API documentation, visit
our [documentation](https://developer.spreedly.com/docs/mobile-applications).

For support or questions, please contact our support team or create an issue in the repository. 