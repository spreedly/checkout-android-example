# Changelog

All notable changes to the Spreedly Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2026-10-13

### Added

- **Custom additional-field validators** (`payments-core`) — Merchant public API: `Spreedly.addValidation` / `removeValidation` / `runCustomValidationBeforeSubmit` (`@JvmStatic` companion; Kotlin instance extensions). Register validators for 29 allowlisted snake_case keys (excludes PAN, CVV/CVC, expiry, and ACH bank fields; includes cardholder name), including **[AdditionalField] overloads** for typo-safe registration alongside string wire keys. Validators run after built-in checks on **hosted field blur** (SDK-internal, automatic on mounted `SPLTextField`) and again before tokenize on `createCreditCard` / `createPaymentMethod`; fail-closed on `isValid=false`, throw, or oversized fields. Blur field UI may show a sanitized merchant `errorMessage`; tokenize `PaymentResult.Failed` / logs / events stay generic. A sticky per-wire-key latch (set on blur Failed, cleared on Pass / same-field edit / `removeValidation` / form reset) makes `hasValidationError` / `areAllFieldsValid` and hosted `onValidationChange(false)` gate Pay **without** re-invoking validators on poll. PAN/CVV/date/ACH/unknown register keys are no-ops with a sanitized warn. Validator callbacks receive `fields: Map<AdditionalField, String>` — a **sparse present-field snapshot** (mounted catalogue values merged with submit `additionalFields`; keys absent from the evaluation map are omitted) plus [fieldValue] helper. Validators run only for wire keys **present** in that snapshot (iOS parity); registration alone does not validate unmounted fields omitted from `additionalFields`.
- **Submit preflight custom validation** (`payments-core`) — `runCustomValidationBeforeSubmit(formFields, additionalFields)` runs the same custom-validator tier as tokenize using the merged submit field map (not sheet state alone). `CheckoutButton` calls `createCreditCard` directly; custom validators run inside it and a failure returns `ValidationFailed`. Custom Pay controls should call `runCustomValidationBeforeSubmit` first when they want inline errors refreshed before tokenize. See [Custom payment forms — Submit preflight](guides/custom-payment-forms.md#custom-additional-field-validators).
- **Canonical hosted-fields tokenize keys** (`payments-core`) — ten Checkout Web SDK wire keys on `AdditionalField` and `CreditCard`: `house_number_or_name`, `street`, `street_line2`, shipping twins, and billing/shipping phone country/area codes. Pass via `additionalFields` on `createCreditCard` / `createPaymentMethod`; phone parts are separate keys (not concatenated into `phone_number`). Legacy `address1` / `shipping_address1` keys coexist in the same request when both are supplied.
- **Hosted-fields catalogue `FormFieldType` variants** (`payments-core`, `hostedfields`, `paymentsheet`) — `FIRST_NAME`, `LAST_NAME`, `EMAIL`, `COMPANY`, `COUNTRY`, `PHONE_NUMBER`, seven dedicated shipping types, and ten canonical address/phone types. `OptionalFieldType` expands to 26 entries for payment-sheet configuration; default `ConfigurableFormField.allOptionalFields` remains the five billing-address fields.
- **`PaymentSheetOptionalFieldCallbacks`** (`payments-core`) — new interface with default no-op methods for catalogue field changes. `SpreedlyUICallbacks` implements it alongside `PaymentSheetCallbacks` (unchanged for Java binary compat). Drop-in and custom sheets wire optional fields through `sdk.optionalCallbacks`.
- **`CardCatalogueFormFieldTypes`** (`payments-core`) — helper for card-catalogue mount tracking and SDK value-SoT types (excludes `ROUTING_NUMBER` / `ACCOUNT_NUMBER`).
- **Per-field `FieldValidationError` required messages** (`payments-core`) — every hosted/shipping/additional field (address lines, house number, street, phone number and its country/area code parts, company, country, city, state, ZIP, first/last name — plus their shipping counterparts) now has its own distinct empty-value error, e.g. `PHONE_NUMBER_REQUIRED` ("Phone number is required"), `SHIPPING_CITY_REQUIRED` ("Shipping city is required"). Previously several of these fields shared a generic `ADDRESS_REQUIRED` ("Address is required") message.
- **`ValidatorFactory.shippingCity` / `shippingState`** (`payments-core`) — new validator factory functions for the dedicated shipping-city and shipping-state fields, each defaulting to their own `SHIPPING_CITY_REQUIRED` / `SHIPPING_STATE_REQUIRED` error.
- **`ValidatorFactory.zipCode` / `name` / `limitedText` field-specific-error overloads** (`payments-core`) — each gains an additive 3-/4-arg overload taking an explicit `emptyError: FieldValidationError`, kept as a separate overload (not a defaulted parameter on the existing function) to preserve binary compatibility for callers compiled against earlier SDK versions. `StandardZipCodeValidator`, `StandardNameValidator`, and `StandardLimitedTextValidator` gain matching additive constructors.

### Changed

- **Additional-fields merge at tokenize** (`payments-core`) — matches Checkout Web SDK `pluckWhitelistedValues`: when a key is present in `additionalFields`, its value is sent unchanged (including `""` and whitespace-only). When absent, form state is used. All present name keys (`first_name`, `last_name`, `full_name`) are forwarded independently to the backend, which resolves any conflicts server-side per the Spreedly API contract. Distinct from hosted-wins merge.
- **Hosted-wins tokenize merge** (`payments-core`) — for card tokenize, owned keys (`formFields ∪` mounted catalogue field types) prefer hosted / sheet state over conflicting `additionalFields` entries (stripped at merge regardless of merchant value). Unowned keys keep additional-first merge. Neither mounts nor `formFields` → legacy additional-first (unchanged). One sanitized log line per tokenize when conflicts are stripped (key names only). Owned blank dedicated shipping (`shipping_address1` / `2` / `city` / `state` / `zip`) does **not** fall back to billing after strip; unowned blank shipping still uses the legacy billing fallback.
- **Payment sheet name tokenize** (`paymentsheet`) — drop-in sheet no longer injects name values into `additionalFields` at submit; hosted / state values win via owned-key merge.
- **Field character limits** (`payments-core`, `hostedfields`) — public `FormFieldCharLimits` aligned with the Checkout Web SDK's `DEFAULT_CHAR_LIMITS`: address/canonical text **500** (was 350), international ZIP max **15** (was 10), email max **254**, phone parts **15**, country **100**. Submit-time checks apply to `additionalFields` and hosted address/ZIP fields via validators and `FormFieldValidationDispatcher`.
- **Payment sheet name path** (`paymentsheet`) — `NameFieldDisplayMode.SEPARATE_FIELDS` mounts `FIRST_NAME` + `LAST_NAME` (not two `NAME` fields). `getFormFields` and tokenize `formFields` match the mounted types.
- **Catalogue field value SoT** (`hostedfields`, `payments-core`) — when `writeCatalogueValueToSdk = true`, non-encrypted catalogue `SPLTextField` types sync into `PaymentSheetState` via `sdk` on input; null `sdk` throws at sync time. Legacy compiled `SPLTextField` overloads default `writeCatalogueValueToSdk = false` (callback-only). ACH `ROUTING_NUMBER` / `ACCOUNT_NUMBER` carved out.
- **Custom validator present-field evaluation** (`payments-core`) — matches iOS `evaluatePresentFields`: registered validators run only when their wire key is in the submit/blur evaluation snapshot (mounted catalogue field, blur overlay, or submit `additionalFields` merge). Unmounted fields omitted from `additionalFields` are **skipped** — the SDK does not invoke validators with an empty string for a missing value. The validator `fields` callback receives a sparse map of present keys only (not a padded 29-key allowlist).
- **`createPaymentMethod` submit validation** (`payments-core`) — when `formFields` is non-empty or catalogue fields are mounted in `SPLTextField`, `createPaymentMethod()` runs the same mounted-field validation union as `createCreditCard()` before tokenize (including invalid expiry manifests and `ValidationFailed` telemetry). Empty `formFields` with no mounted catalogue fields keeps legacy no-validation behavior.
- **ACH submit validation scope** (`payments-core`) — `createBankAccount()` validates only the merchant `formFields` list on that call. The `formFields ∪ mounted` submit union and hosted-wins owned-key merge apply to **card** tokenize (`createCreditCard()` / gated `createPaymentMethod()`), not ACH in this release. ACH catalogue / union parity is deferred to a follow-up (see [ACH Bank Account](guides/ach-bank-account.md#hosted-catalogue-fields-card-vs-ach)).
- **ZIP / postal-code validation** (`payments-core`, `hostedfields`) — matches Checkout Web SDK parity: the ZIP hosted field no longer filters keystrokes to a digits/letters/space/hyphen set, and `StandardZipCodeValidator` no longer enforces a postal-code format or character set (only required-ness and `FormFieldCharLimits.ZIP` length). Any Unicode value is accepted subject only to the required/length checks — including whitespace/control characters (tab, newline, CR), zero-width and bidirectional-formatting characters, and combining-mark sequences, not only visibly-rendered ones — the `FormFieldCharLimits.ZIP` length is counted in UTF-16 code units (Kotlin `String.length`), so a supplementary-plane character such as an emoji counts as 2. `ValidationParameter.ALLOW_INTERNATIONAL_ZIP_CODES` is retained for source compatibility but is now a no-op for built-in validation. Merchants who need a specific ZIP shape enforced should register their own rule via `Spreedly.addValidation`.
- **Required-field visible-character check** (`payments-core`) — `FieldValidator.validateRequired`, shared by every required-field validator (ZIP, name, email, address/city/state, company, country, phone parts, and their shipping counterparts), now rejects a value made up only of invisible Unicode format characters (zero-width space, RTL/LTR mark) as if it were empty. Previously `String.isBlank()` alone treated these as non-blank, letting an effectively invisible value satisfy "required." `EmailFormValidator` and `LimitedTextFormValidator` (legacy catalogue-field validators) now delegate to their `Standard*` counterparts and pick up this check too, rather than duplicating the older, narrower logic inline.

### Fixed

- **Payment sheet State/Zip row spacing** (`paymentsheet`) — when STATE and ZIP_CODE are both enabled, an extra spacer was emitted for the skipped ZIP iteration, producing a visually larger gap above the next field (e.g. Country). The loop now suppresses both the spacer and the item for any field that is inlined into a sibling's row.
- **Additional-field email validation** (`payments-core`) — malformed emails in `additionalFields` at submit now classify as `invalid`, not `field_too_long` (over-length emails still use `field_too_long`).
- **Custom-validator telemetry volume** (`payments-core`, `hostedfields`) — mounted `SPLTextField`'s internal blur/keystroke/Pay-tap auto-wiring no longer emits `sdk_method_invoked` telemetry on every value change. Hosted-field auto-wiring called the same functions a direct merchant call uses, so every keystroke and blur on any mounted field — including `CARD` and `CVV` in the drop-in sheet — logged an event, regardless of whether any custom validator was registered. Merchant calls to `Spreedly.evaluateCustomValidationOnBlur` / `clearCustomValidationBlurFailure` still emit as before; only the SDK-internal auto-wiring path is now silent. No behavior change to validation itself (fail-closed gating, allowlist, and sanitized tokenize egress are unchanged).

### Breaking Changes

**Upgrade note:** Everything under **Breaking Changes** requires **recompiling** against the new `hostedfields`, `payments-core`, and `paymentsheet` AARs. This release is **not** a drop-in binary patch over compiled 1.4.x consumers — take the **1.5.0** SDK and refresh Java `createPaymentMethod` / Compose `SPLTextField` call sites.

- **`createPaymentMethod` `formFields` parameter** (`payments-core`) — both overloads accept optional `formFields: List<FormFieldType> = emptyList()` (after `metadata` / `additionalFields`) for hosted-wins ownership. Kotlin call sites omit the argument via default parameters. Hidden binary-compatible overloads without `formFields` remain for JVM callers compiled against earlier SDK versions. **`createPaymentMethod` is not annotated with `@JvmOverloads`** — new Java integrations should pass `formFields` explicitly (typically `Collections.emptyList()` / `List.of()`).
- **`PaymentSheetState` catalogue properties** (`payments-core`) — twenty-one catalogue validators append after the original fourteen properties (`component1`–`component14` order unchanged vs 1.4.x). Appending properties changes the primary constructor / `copy` / `copy$default` JVM descriptors — **recompile** against the new AAR (not a drop-in binary patch). Prefer named properties over positional `componentN()` / `copy()`.
- **`CreditCard` canonical wire fields** (`payments-core`) — ten nullable canonical wire-field properties append at the end of the primary constructor. Hidden 22-parameter constructor preserved for JVM callers compiled against 1.4.x; recompile to populate canonical keys.
- **Exhaustive `when (FormFieldType)` / `when (OptionalFieldType)`** (`payments-core`, `hostedfields`, `paymentsheet`) — new sealed variants for catalogue fields. Merchant exhaustive `when` expressions over `FormFieldType` or `OptionalFieldType` must add branches (or an `else`) when upgrading.
- **`SPLTextField.writeCatalogueValueToSdk`** (`hostedfields`) — optional trailing Compose parameter (default `true` on the new overload). Recompile `SPLTextField` call sites against the new `hostedfields` AAR; Kotlin defaults are source-compatible. When `true`, catalogue keystrokes call `syncHostedCatalogueFieldValue` into `PaymentSheetState`. Pass **`false`** when `onChange` already updates state via `sdk.callbacks` / `sdk.optionalCallbacks` (drop-in `PaymentSheet` sets `false`). The hidden pre-parameter overload defaults `writeCatalogueValueToSdk = false` for binary-compatible call sites.
- **`AdditionalFieldValidator` `fields` snapshot** (`payments-core`) — the second callback parameter is now `Map<AdditionalField, String>` (was `Map<String, String>`). Recompile custom validators; read cross-field values with `fields[AdditionalField.COUNTRY]` or `fieldValue()`. Keys are **present fields only** (sparse snapshot); absent keys are omitted, not sent as `""`.

### Security

- **Custom validator tokenize egress** (`payments-core`) — centralized generic tokenize failure message (`Payment form validation failed`); merchant `CustomValidationResult.errorMessage` never appears on `PaymentResult.Failed`, `createPaymentMethod` `Result.Error`, nested validation errors, or `validation.custom_validator_*` telemetry. Blur and submit preflight inline UI still show sanitized merchant copy. See [Security — Tokenize egress](guides/security.md#tokenize-egress-merchant-messages-never-leak).
- **Payment form state logging** (`payments-core`) — `PaymentSheetState.toString()` redacts non-blank values for the fields it prints (card number, CVV, expiry, names, email, company, phone, address1, zip) as `***`. Catalogue/shipping/canonical properties are **omitted** from that string (not printed plaintext). `BankAccountSheetState.toString()` redacts non-blank name/bank/routing/account values the same way. Crash/debug logs therefore do not expose encrypted CHD or bank account numbers via these overrides.
- **`PaymentMethod.toString`** (`payments-core`) — redacts non-blank `email` and non-empty `metadata` values; mandate remains `[REDACTED]` when present.
- **`AppNetworkError.API_ERROR.errorBody` sanitized at ingress** (`payments-core`) — previously stored the raw HTTP error-response body (capped at 8 KiB, but unredacted) on this public getter. Now passes through the same `LogSanitizer.sanitizeStoredErrorString` redaction `SpreedlyNetworkError.SpreedlyApiErrorDetail.rawErrorBody` already applied at the higher layer, closing a gap where a labeled CVV/PAN-like value in a raw server error response could reach merchant code unredacted via the lower-level type. Not guaranteed to still be valid JSON after redaction.

### Deprecated

- **`NameParsingUtils.getNameFields`** (`payments-core`) — was accidentally `public` and is no longer used by the SDK. Kept with 1.4.0 behavior for binary compatibility; scheduled for removal in 2.0.0. Remove direct calls; name resolution is handled internally during tokenize.

## [1.4.0] - 2026-09-16

### Changed

- **Kotlin 2.1.20** — SDK compiles with Kotlin **2.1.20** (was 2.3.10) so React Native 0.82–0.86 hosts no longer need to pin Kotlin 2.3.10. AGP 8.13.2, Gradle 8.14.3, and Compose BOM are unchanged.
- **Custom loggers** (`payments-core`) — `setLogger` now receives already-sanitized tag, message, and throwable (no original exception type or PAN/CVV in log text).
- **Recache CVV** (`paymentsheet`) — CVV and the in-flight spinner are not restored after rotation.

### Breaking Changes

- **API error fields** (`payments-core`) — `SpreedlyApiErrorDetail` stored strings (`rawErrorBody`, messages, validation errors) are capped and redacted; they are no longer the raw HTTP body and may not be valid JSON. Use `statusCode` / `errorKey` / `safeDescription()`. Do not parse `rawErrorBody` as wire JSON. `AppNetworkError.API_ERROR` getters are still raw.
- **ACH account numbers** (`payments-core`, `paymentsheet`) — validation is ciphertext-only. A raw account number in `BankAccountSheetCallbacks.onAccountNumberChange` or `AccountNumberValidator` fails and will not tokenize. Use `SPLTextField(FormFieldType.ACCOUNT_NUMBER)`.
- **Card scheme** (`payments-core`) — plaintext digits on `onCardNumberChange` no longer set `cardScheme`. Drive PAN through `SPLTextField`.

### Deprecated

- **`SpreedlyEncryption` / `Encryptor` / `FormFieldType.shouldEncrypt()`** (`payments-core`) — lint-deprecated and restricted to the library group. Use `SPLTextField` for card and bank fields. `KEY` is a sentinel string, not key material.

### Fixed

- **Click to Pay** (`clicktopay`) — host ingress executor is shut down when the WebView detaches and when checkout or saved-cards detector sessions are cleared, so `detectorKey` re-runs no longer leak `c2p-host-ingress` threads.
- **Click to Pay** (`clicktopay`) — checkout with a saved card could fail tokenization with `"The requested information does not exist for any available network"`. The bridge payload sanitizer was running MC's `flowId` / `correlationId` / `merchantTransactionId` through the log-text PAN heuristic and redacting legitimate IDs to `"[REDACTED]"`. PAN and CVV redaction are unaffected; a plain control-character strip still guards these fields.
- **Click to Pay** (`clicktopay`) — host and branded-button `script-src` CSP was missing the sandbox Visa, Amex, and Discover per-network SRC adapter script hosts that MC's `lib.js` loads directly, outside `*.src.mastercard.com`. Checkout with a non-Mastercard-branded card could fail to load that network's adapter.
- **Click to Pay** (`clicktopay`) — entering an invalid email or phone on the identity form ended the checkout instead of letting the shopper correct it and try again.
- **Click to Pay** (`clicktopay`) — saved-card art didn't show up for a recognized device; the CSP was missing the host MC serves it from.
- **Click to Pay** (`clicktopay`) — Visa, Discover, and Amex cards couldn't be found or added at all. The checkout WebView only allowed Mastercard's own hosts, so the other networks' identity-lookup and enrollment iframes were blocked. Now matches iOS's allowlist.
- **Click to Pay** (`clicktopay`) — Discover checkout failed even after the above fix — its fingerprinting script runs outside the iframe sandbox the other networks use, and needed a few more hosts (its own domain, ThreatMetrix, and its tracking-pixel host) allowed.
- **Click to Pay** (`clicktopay`) — adding an American Express card failed with a generic error, then with a `jQuery not defined` error once the first cause was fixed. Amex's adapter script and its jQuery/js-cookie dependencies load from hosts that weren't on the allowlist.
- **Click to Pay** (`clicktopay`) — adding a new card from an already-recognized saved-cards session failed with "Enter an email or phone number to continue," even though the device didn't need one to be recognized.
- **Click to Pay** (`clicktopay`) — saved-card art still didn't load even after the `img-src` CSP fix above: the CSP allowed the asset host, but the native WebView allowlist independently blocked it, and that layer fails silently with no CSP violation to diagnose from.
- **Click to Pay** (`clicktopay`) — same class of bug as the item above, but for production: `script-src` only allowlisted the sandbox Visa/Amex adapter hosts, so a production checkout with those cards would have hit the same blocked-script failure the sandbox fix addressed. Confirmed against MC's published production `lib.js` and validated on live production checkouts. Discover's production hosts needed no change; the existing wildcards already covered them. The production equivalent of the Mastercard saved-card-art host is still unconfirmed — it isn't referenced anywhere in `lib.js`, so it can't be found the same way.
- **Click to Pay** (`clicktopay`) — a shopper recognized by Mastercard with no cards enrolled for this merchant's DPA got stuck on "OTP validated — loading cards" indefinitely after entering their OTP. The empty `getCards` response after OTP validation retried a profile lookup that had already run and was silently skipped instead of routing to card enrollment.
- **Click to Pay** (`clicktopay`) — a shopper whose new-card checkout was declined (e.g. Amex rejecting the card) and who had no other saved cards got stuck on an empty "Select a saved card" screen with no way forward but sign out. `CHANGE_CARD` always routed to the saved-card list regardless of whether any cards existed; it now routes to card enrollment when the shopper has none.

### Security

- **Logging / `toString()`** (`payments-core`) — `PaymentResult.Failed`, `ThreeDSChallengeResult.Failed`, and API error `toString()` are log-safe (`statusCode` / `errorType` only). Prefer `getDescription()` for UI. Logs redact labeled CVV JSON and 12–19 digit PAN-like runs (JSON timestamps can over-redact).
- **Click to Pay** (`clicktopay`) — tokenize failures publish a static `Tokenize failed` (no exception text). Host WebView uses origin-checked messaging instead of `JavascriptInterface`. UAT sandbox before production.
- **Screenshot flag** (`payments-core`, `clicktopay`) — overlapping Spreedly screens keep `FLAG_SECURE` until the last one closes; merchant-set `FLAG_SECURE` is preserved.
- **Submit fail-closed** (`payments-core`) — corrupt optional card/CVV/account ciphertext returns `ValidationFailed` instead of tokenizing empty values.
- **LogSanitizer** (`payments-core`) — redacts PAN-like digit runs of 12+ via one-pass candidate scanning (contiguous or separated by any number of space/dot/underscore/hyphen characters, including glued-after-letter and 20+ digit embeddings), and escaped-JSON labeled CVV (`\"cvv\":\"123\"`). Stored error strings also redact separator-formatted PAN fragments split across the 8 KiB cap.
- **Click to Pay** (`clicktopay`) — host and branded-button HTML `script-src` CSP uses a SHA-256 hash of the bootstrap script instead of `'unsafe-inline'`, and replaces the `*.src.mastercard.com` wildcard with an explicit host list: `src.mastercard.com` and `sandbox.src.mastercard.com`; the sandbox and production Visa (`secure.checkout.visa.com`) and Amex (`aexp-static.com`) SRC adapter hosts; Discover (`discover.com`, `discovercard.com` and their subdomains, `webapp.sandbox.src.discover.com`) and its ThreatMetrix fingerprinting hosts (`online-metrix.net` and subdomains); and `code.jquery.com` / `cdn.jsdelivr.net` for Amex's checkout-window dependencies. `WebMessageListener` origin checks remain the primary control.
- **Click to Pay** (`clicktopay`) — `isAllowedMastercardOrigin`, which gates the WebMessage bridge, had widened along with the navigation/resource allowlist and would accept a `sourceOrigin` from Visa, Discover, Amex, ThreatMetrix, or the third-party CDN hosts. `WebMessageListener`'s own `allowedOriginRules` already restricted this to Mastercard before the callback ran, so this wasn't independently exploitable; it's now narrowed back to Mastercard-only so the bridge origin check doesn't silently drift with future allowlist changes. Blocked-request debug logging also switched from a heuristic string sanitizer to a scheme/host/path-only representation, dropping query and fragment data outright instead of trying to redact it.
- **Click to Pay** (`clicktopay`) — `code.jquery.com`/`cdn.jsdelivr.net` no longer count as valid main-frame navigation targets, only as script resources. They're pure library CDNs with no `frame-src` CSP entry, so there was no legitimate navigation use for them; narrowing this doesn't change what Amex's checkout window can load.
- **Click to Pay** (`clicktopay`) — that navigation restriction wasn't fully enforced: `shouldOverrideUrlLoading` isn't called for POST navigation, so a resource-only host (the two CDNs above, or the Mastercard asset host) could still have replaced the main-frame document via a POST and only faced the broader resource policy. `shouldInterceptRequest` now enforces the navigation policy for main-frame requests directly, closing that gap regardless of HTTP method.

## [1.3.0] - 2026-07-30

### Added

- **Click to Pay** (`clicktopay`) — optional `:clicktopay` artifact with Mastercard WebView checkout (not present in the `1.2.0` artifact). Entry points: `SpreedlyClickToPayCheckout` (`present`, `cancel`, `events`, `state`, `tokenize`, lookup/OTP helpers), drop-in `SpreedlyClickToPayButton` / `ClickToPayBrandedButton`, and `ClickToPaySavedCardsDetector` for pre-checkout Remember-me recognition (tear down before `present()`). Sandbox new-user enrollment via `ClickToPayCheckoutConfig.sandboxEnrollmentCard` (in-memory only; rejected in production). Default UI uses MC `src-card-list` with native SPL CVV/pay; sheet chrome follows `Spreedly.setGlobalTheme()` via `SpreedlyAdaptiveGlobalTheme` (pay actions keep Mastercard SRC branding). WebView is hardened (Mastercard host allowlist, scheme deny-list, bridge method/size/forbidden-key guards, DCF popup policies); public `CheckoutComplete` carries metadata only (no PAN/CVV). See [Click to Pay Integration Guide](guides/click-to-pay.md).
- **Mandate passthrough on tokenization** (`payments-core`, `paymentsheet`, `hostedfields`) — optional `mandate` on tokenize APIs and drop-in sheets, forwarded verbatim to Spreedly at `payment_method.mandate` and omitted when null or empty. Accepts a `Map<String, Any?>` (nested values preserved; pre-parsed `JsonObject` allowed). Spreedly owns schema validation; the SDK does not cap or validate mandate contents. Wire semantics follow ECMA-262 `JSON.stringify` (`NaN`/`Infinity` → `null`; `Date`/`Instant`/`UUID`/`URL`/`URI` → canonical string). Unrepresentable values or reference cycles fail tokenization with the offending key path. Mandate **values** are do-not-log (request `toString` redacts them); conversion diagnostics may include structural key paths. Documented in express, ACH, and custom-form guides.

### Breaking Changes

- **`SpreedlyBottomSheet` / `SpreedlyBankAccountBottomSheet` Compose signatures** (`paymentsheet`) — optional trailing `mandate` changes the Compose-generated method name. Recompile consumers against the new `paymentsheet` AAR. Kotlin callers using defaults are source-compatible; Java `PaymentSheetJavaHelper.setupContent` keeps prior overload arities.
- **Tokenize APIs gain trailing `mandate`** (`payments-core`) — `Spreedly.createCreditCard` / `createBankAccount` / `createPaymentMethod` (and matching `SpreedlyPaymentManager` methods) take optional `mandate: Map<String, Any?>?`. Kotlin defaults remain source-compatible; Java callers must pass `null` or a map.

## [1.2.0] - 2026-07-22

### Added

- **ACH bank account tokenization** (`paymentsheet`, `payments-core`) — bank account payment support with a pre-built drop-in sheet, a custom-layout composable (`BankAccountSheet(sdk, …)`), and a headless `createBankAccount()` flow. Configure name display mode and optional bank-name / account-type fields via `Spreedly.setBankAccountFieldConfig` / `setBankAccountNameDisplayMode` (and matching `SpreedlyUIController` methods). Routing, account, and name fields validate to iOS parity; in-memory routing and account data is cleared on dismiss, cancel, and API/network failure (SEC-8709), while a configuration change preserves mid-edit values.
- **`PaymentResult.Completed.paymentMethodType`** (`payments-core`) — tokenize success now reports the payment method type (e.g. `"bank_account"`) on `PaymentResult.Completed` from the payment method response.
- **Shared theming helpers** (`payments-core`, `hostedfields`) — `resolveEffectiveCustomFieldsConfig()` and `SpreedlyTheme.toCustomFieldsConfig()` give card and ACH surfaces one theme → field-config mapping (global-theme merge, `fieldShape` synced from `borderRadius`).
- **`CheckoutButton.isFormValid` / `AppTextField.maxLength` / `NameValidator` min length** (`payments-core`) — additive parameters; prior signatures retained via hidden compatibility overloads / `@JvmOverloads`.

### Breaking

- **`PaymentProcessingResult`** (`payments-core`) — adds `Rejected(ALREADY_PROCESSING)` and `Failed(UNEXPECTED_ERROR)`. `processPayment()` returns this sealed type, so exhaustive `when` expressions must handle the new branches (or add an `else`). Card flows never return the new values, but the type is shared.

### Changed

- **Express checkout guide** (`docs`) — clarifies that card sheet `borderRadius` and `fieldShape` are independent; ACH `CustomFieldsConfig` paths sync shape from radius.

## [1.1.0] - 2026-06-03

### Breaking Changes

- **`SPLTextField`** (`hostedfields`) — **CARD** and **CVV** require non-null `sdk`; display follows `Spreedly.hostedCardDisplayState`. Removed composable parameters that duplicated SDK display state (`observeHostedCardDisplayState`, `hostedCardDisplayState`, `cardNumberFormat`, `cvvDisplayMasked`). Recompile consumers and pass `sdk` on every CARD/CVV field.
- **`SPLTextField` Compose signature** (`hostedfields`) — new parameters (`trailingIcon`, `onFieldStateChange`, `onFocusChanged`, `enableAutofill`) change the Compose-generated method name. Recompile consumers against the new `hostedfields` AAR.
- **`PaymentSheet`** (`paymentsheet`) — requires `sdk: Spreedly`; removed `hostedCardDisplayState` parameter (use SDK state only).
- **`PaymentSheetConfig` data class** (`payments-core`) — new properties `enableAutofill` (`Boolean`, default `true`) and `cardNumberFormat` (`CardNumberFormat`, default `PRETTY`). Java callers using positional constructors or `copy()` must add the two new trailing arguments. Kotlin callers using named parameters are unaffected.
- **`HostedFieldsJavaHelper` / `HostedFieldsForm`** (`hostedfields`) — removed `cardNumberFormat` from hosted-fields Java setup overloads and form content; CARD/CVV use `sdk` on `SPLTextField` for display.
- **CVV recache result delivery** (`payments-core`) — Recache outcomes are delivered only on `recacheResultFlow` and from the `recachePaymentMethod` suspend return, not on `paymentResultFlow`. Removed `PaymentResult.isRecacheMirrorEvent()` and `PaymentResult.Completed.paymentMethodUpdatedAt`. Use `PaymentMethodResponse.paymentMethodUpdatedAt` from the recache `Result`. Matches iframe (separate `recache` vs `paymentMethod` events).
- **`PaymentResult.Canceled`** (`payments-core`) — now a `data class` with optional `message: String?` and `paymentMethodType: String?` (both default `null`). Kotlin `when` branches must use `is PaymentResult.Canceled`. Emit sites must use `PaymentResult.Canceled()`. Java callers use `instanceof`. Binary-incompatible change; requires recompilation.
- **`BraintreeAPMActivity.Companion.createIntent()`** (`braintree`) — removed; use `SpreedlyBraintreeAPMCheckout.present()` or `BraintreeAPMJavaHelper.presentCheckout()` instead.

### Added

- **Express core field copy** (`payments-core`, `paymentsheet`, `hostedfields`) — `PaymentSheetCoreFieldLabels` and `PaymentSheetCoreFieldCopyResolver` for optional card number, CVV, and expiration label and hint text on `SpreedlyBottomSheet` / `PaymentSheet` (iOS `DropInCoreFieldLabels` parity). `SPLTextField` supports custom hint text for express pass-through. Java: `PaymentSheetJavaHelper.createDefaultCoreFieldLabels()` and `setupContent` overloads with `coreFieldLabels`. Defaults unchanged when omitted.
- **Iframe-style PAN display** (`payments-core`, `paymentsheet`, `hostedfields`) — `CardNumberFormat` (`PRETTY`, `PLAIN`, `MASKED`), `HostedCardDisplayState`, `Spreedly.hostedCardDisplayState` (observable Compose `State`), `setNumberFormat` (enum or iframe-style string aliases `prettyFormat` / `plainFormat` / `maskedFormat`), and `toggleMask` (coupled CARD + CVV). Mask control is merchant-owned UI outside the fields (iframe parity).
- **Lifecycle mask overlay** (`hostedfields`) — `SPLTextField.forceMaskOnLifecycleStop` (default `true`): temporary visual mask on `ON_STOP` when PAN was revealed. Clipboard copy/cut disabled for CARD when revealed.
- **`SPLTextField.onFieldStateChange` / `HostedFieldState`** (`hostedfields`) — typed merchant-safe field snapshots (`INPUT`, `FOCUS`, `BLUR`, `VALIDATION`, `PAN_MASK_CHANGED`): `cardScheme`, `numberLength`, `cvvLength` (digit counts only — no raw PAN/CVV), `isValid`, `isEmpty`, `isFocused`, `isPanMasked`, `iin`. Java: `HostedFieldStateListener` overloads on `HostedFieldsJavaHelper.setupContent`.
- **`SPLTextField.onFocusChanged`** (`hostedfields`) — optional `((Boolean) -> Unit)?` for focus/blur callbacks.
- **`SPLTextField.trailingIcon`** (`hostedfields`) — optional composable slot for CARD fields (e.g., card brand logo).
- **`Spreedly.areAllFieldsValid(formFields)`** (`payments-core`) — aggregate validation gate over `FormFieldType` list. `@MainThread`.
- **`Spreedly.getRegisteredFieldCount()`** (`payments-core`) — count of mounted hosted field instances for pay-button gating.
- **`Spreedly.resetPaymentFormPreservingDisplayConfig()`** (`payments-core`) — clears payment field values and validation without resetting `hostedCardDisplayState` (preserves mask/format state).
- **`EmailValidator`** (`payments-core`) — `isValid(email)` for merchant email fields (rejects single-label domains).
- **`eligibleForCardUpdater`** — optional parameter on `createCreditCard` / `createPaymentMethod`; JSON key `eligible_for_card_updater` sent only when non-null.
- **Hosted fields autofill** (`hostedfields`, `payments-core`) — `SPLTextField(enableAutofill = true)`, `HostedFieldsJavaHelper.setupContent(..., enableAutofill)`, `PaymentSheetConfig.enableAutofill`. CARD uses `KeyboardType.Number` when autofill is on (password keyboard suppresses OS autofill).
- **`PaymentSheetDisplayConfig`** (`payments-core`) — express checkout display options (`enableAutofill`, `cardNumberFormat`). Optional `displayConfig` on `SpreedlyBottomSheet` and `PaymentSheet` (`null` falls back to `PaymentSheetConfig` via `PaymentSheetDisplayConfig.resolve`). Java: `PaymentSheetJavaHelper.createDefaultDisplayConfig()` and `setupContent` overloads with optional `displayConfig`.
- **`HostedFieldState.iin`** (`hostedfields`) — merchant-safe IIN prefix on card field snapshots (iframe `inputProperties` parity); always computed when six or more digits are present.
- **`HostedFieldState.panDisplayFormat` / `panDisplayPolicyMasked`** (`hostedfields`) — card-number snapshot of global display format and `panMasked` policy at emission time.
- **`HostedFieldsJavaHelper.setupContentWithInspector`** (`hostedfields`) — Java setup with optional inspector slot; CARD/CVV-only field-state listener on the inspector path.
- **`PaymentResult.Completed.paymentMethodType`** (`payments-core`) — optional field identifying the payment type (e.g., "paypal", "venmo") on successful Braintree APM flows.
- **`PaymentResult.Completed.venmoUsername`** (`payments-core`) — optional Venmo username returned on successful Venmo payments.
- **Braintree transaction status models** (`payments-core`) — `BraintreeLineItem`, `BraintreeShippingAddress`, and new fields on `TransactionStatus` (`enablePaylaterButton`, `billingAgreementDescription`, `lineItems`, `shippingAddress`, `locale`) for iFrame parity.
- **Braintree PayPal vault & checkout_with_vault flows** (`braintree`) — `launchPayPal()` now builds `PayPalVaultRequest` or `PayPalCheckoutRequest` with billing agreement, shipping address, line items, and locale from the transaction status. `paypal_flow_type` values `checkout`, `vault`, and `checkout_with_vault` are all supported.
- **Braintree PayPal device-data gating** (`braintree`) — device data collection is skipped for plain `checkout` flows (matching iFrame behavior). Vault and `checkout_with_vault` flows still collect device data.
- **Braintree Venmo `multi_use` / `single_use` mapping** (`braintree`) — `venmo_flow_type` from the transaction status is mapped to `VenmoPaymentMethodUsage.MULTI_USE` or `SINGLE_USE`. `multi_use` also sets `shouldVault = true`.
- **Braintree cancel messages** (`braintree`) — `PaymentResult.Canceled` now carries `message` (e.g., "User canceled PayPal payment") and `paymentMethodType` (e.g., "paypal", "venmo") on Braintree cancel events.
- **`SpreedlyBraintreeAPMCheckout.inferPaymentType()`** (`braintree`) — public helper to determine `BraintreeAPMPaymentType` from a `TransactionStatus.paymentMethodType` string, with Java-friendly helper in `BraintreeAPMJavaHelper`.
- **Client token 24h validation** (`braintree`) — `present()` now validates that the Braintree client token (`createdAt`) is within 24 hours and rejects stale tokens with `PaymentResult.Failed`.
- **Payment type mismatch validation** (`braintree`) — `present()` checks that the config's `paymentType` matches the transaction's `paymentMethodType` and fails early on mismatch.
- **Recache accessors and helpers** (`payments-core`) — `PaymentMethodTransactionTypes.RECACHE_SENSITIVE_DATA`, `PaymentMethodResponse.paymentMethodUpdatedAt`, `PaymentMethodResponse.recacheToken`.
- **Stripe APM PaymentSheet appearance** (`stripe`) — optional `StripeAPMAppearanceConfig` on `SpreedlyStripeAPMCheckout.present(config, activity, appearance)` and `StripeAPMJavaHelper.presentCheckout(config, activity, appearance)` maps to Stripe `PaymentSheet.Appearance`. See `docs/development/STRIPE_22_8_APPEARANCE_FIELD_MATRIX.md`.
- **Stripe Radar module** (`stripe-radar`) — device data collection for fraud detection via Stripe Radar sessions. See `docs/guides/stripe-radar.md`.
- **Legacy iframe migration guide** — `docs/guides/migration/from-legacy.md` with iframe-to-Android mapping tables.

### Deprecated

- **`PaymentSheetConfig.enableAutofill` / `cardNumberFormat`** (`payments-core`) — use `PaymentSheetDisplayConfig` on express checkout composables instead. Deprecated properties remain for this release (binary compatible).

### Changed

- **PAN/CVV display transforms** (`hostedfields`) — `MASKED` and `PLAIN` use full-bullet display (iframe `maskedFormat` style); `PRETTY` keeps focus-based gradual reveal (not identical to iframe `prettyFormat` — see migration guide).
- **MASKED PAN and CVV mask character** (`hostedfields`) — full-mask display now uses `*` instead of `•` for iframe parity. PRETTY gradual-reveal still uses bullets in `MaskedCardNumberVisualTransformation`.
- **Hosted card display transitions** (`payments-core`) — `setNumberFormat(PRETTY)` unmasks the PAN only; **CVV mask is preserved** from the prior state (e.g. MASKED → PRETTY keeps CVV masked). `PLAIN` and `MASKED` still couple PAN and CVV (both unmasked or both masked). `toggleMask()` toggles between masked and plain. Default / full `resetPaymentState()` uses PRETTY with `panMasked = false` and `cvvDisplayMasked = false`; successful `createCreditCard` does not reset display.
- **`resetPaymentState()` display reset** (`payments-core`) — Clears form values and resets `hostedCardDisplayState` to defaults. Re-apply `setNumberFormat` / `toggleMask` after reset if you use a non-default mask. See [migration guide](guides/migration/from-legacy.md#tokenization-reset-and-listeners).
- **Post-tokenize hosted display** (`payments-core`, `paymentsheet`) — Successful `createCreditCard` and express sheet open call `resetPaymentFormPreservingDisplayConfig()` so `hostedCardDisplayState` (mask/format) is preserved. Full `resetPaymentState()` still clears display for merchant clean-slate resets.
- **Express autofill** (`paymentsheet`) — autofill is controlled by `PaymentSheetDisplayConfig.enableAutofill` only; removed the in-sheet `showsAutofillToggle` QA switch from `PaymentSheet` / `SpreedlyBottomSheet`.
- **`AppTextField`** (`payments-core`) — when `autofillContentType` is null, the field now sets autofill semantics to `ContentDataType.None` so the OS does not treat it as a generic autofillable text target (reduces unwanted autofill UI when autofill is intentionally off).
- **`SpreedlyTheme.toPaymentSheetConfig()`** (`payments-core`) — documents and maps **colors only** for payment sheets; initial autofill / card-number format for express use `PaymentSheetDisplayConfig` or legacy `PaymentSheetConfig` fields when `displayConfig` is null.
- **Default form colors in dark mode** — Built-in `SpreedlyTheme.Dark` is now selected automatically when no global theme is set. `setGlobalTheme(SpreedlyTheme.Default)` resets to that auto-switching behavior. Payment sheets and hosted fields no longer show white field backgrounds on dark system themes.
- **Gateway-specific 3DS** (`threeds`) — Challenge and redirect polling now time out after **10 minutes** (previously 15 minutes). Braintree purchases that return `device_fingerprint` go straight to the challenge step when `challenge_url` or `challenge_form_embed_url` is on the status; if both are missing, the SDK returns **Challenge URL missing**. Device fingerprint completion is detected via status polling (up to ~10 seconds).
- **Recache suspend return** (`payments-core`) — `recachePaymentMethod` returns `Result.Error` when `transaction.succeeded` is false (including some HTTP 200 failure bodies).

### Fixed

- **Braintree APM `client_token` expiry** (`braintree`) — Parse `transaction.context` for `client_token` and `created_at`; skip expiry when `created_at` is missing or unparseable so checkout is not blocked when status fields are only in context. Fixes HC-1480.
- **Separate expiry month field** (`hostedfields`) — Month field no longer pads a lone `0` to `00`, so merchants can backspace and clear the field while typing. Live month input: digits only, max 2 characters, no zero-padding until autofill or payment submission. Months 10–12 can be typed digit-by-digit (e.g. `1` then `2` → `12`). Fixes HC-1508.
- **Validation param revalidation** (`payments-core`, `hostedfields`) — Enabling `allowExpiredDate` (or `allowBlankDate`) now clears stale expiry errors on separate month/year fields without requiring another keystroke. Fixes HC-1508.
- **Recache optional CVV** (`payments-core`, `paymentsheet`) — ROUTEX, UATP, and Tarjeta D recache now accept empty or 1–3 digit numeric CVV (hosted-fields parity). Rejects 4+ digits on optional cards. Fixes HC-1506 / HC-1504.
- **Recache Confirm button** (`paymentsheet`) — Confirm stays disabled until CVV passes card-type validation (not merely non-empty).
- **Separate expiry fields card scan** (`hostedfields`) — YY year field now keeps the last two digits when autofill supplies a four-digit year (e.g. `2030` → `30`). Single-digit months from scan are zero-padded (e.g. `4` → `04`). Fixes HC-1505.
- **Optional CVV submit validation** (`hostedfields`) — `SPLTextField` re-publishes CVV validation when the card scheme changes so ROUTEX / Tarjeta D / UATP cards accept empty CVV. Fixes HC-1496.
- **Expiry autofill** (`hostedfields`) — card scan and Wallet autofill values such as `06/30`, `06/2030`, and compact `0630` are parsed into `EXPIRY_DATE` (MMYY) and separate month/year fields; combined values route to the peer field.
- **Global theme on hosted fields** (`hostedfields`, `paymentsheet`) — `SPLTextField` and `PaymentSheet` re-read `GlobalThemeManager` when the global theme changes instead of caching an empty config in `remember(config)`.

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

- **Braintree APM client_token expiry** (`braintree`) -- Parse `transaction.context` for `client_token` and `created_at`; skip expiry when `created_at` is missing or unparseable so checkout is not blocked when status fields are only in context (HC-1480).

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

### Breaking Changes

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

- **Multi-Module Publishing**: SDK now published as three separate modules
    - `checkout-payments-core`: Core payment processing, API client, and 3DS support
    - `checkout-hostedfields`: Secure individual payment field components
    - `checkout-paymentsheet`: Pre-built payment sheet UI
- **Proper Dependency Management**: Transitive dependencies (Ktor, Compose) now correctly exposed via `api` scope
- **Bundled Core Modules**: All internal core modules now bundled into `payments-core` for cleaner architecture

### Changed

- **Artifact ID Change**: Changed from `checkout-android` to module-specific IDs
- **Build System**: Replaced fat AAR with standard multi-module publishing
- **Forter SDK**: Now required as explicit dependency (not bundled)

### Fixed

- **Resources$NotFoundException**: Fixed string resource issues by properly bundling all resources into `payments-core`
- **NoClassDefFoundError**: Fixed Ktor dependency issues with proper `api` scope
- **POM Generation**: Fixed dependency declarations in published POMs

### Removed

- **Fat AAR**: Removed single fat AAR in favor of modular approach
- **Bundled Forter**: Forter SDK no longer bundled, must be added explicitly

## [0.7.0] - 2024-12-30

### Added

- **Save Card for Future Payments**: Added `shouldRetain` property to `PaymentResult.Completed` to indicate whether users want to save their payment method
    - New `retainOnSuccess` parameter in payment creation methods (`createPaymentMethod`, `createCreditCard`)
    - `SaveCardCheckbox` composable component for allowing users to opt-in to saving cards
    - Automatic extraction of `retained` value from API response to `PaymentResult`
    - Comprehensive documentation on secure token storage best practices
- **Focus Tracking Callback**: Added `onFocus` callback parameter to `SPLTextField` and `AppTextField`
    - Triggered when user taps into a field (field gains focus)
    - Enables custom focus management and field tracking
    - Complements existing `shouldFocus` parameter for complete focus control
    - Useful for analytics, custom UI feedback, and field-specific help displays

### Security

- **Payment Token Storage**: Added guidance on secure payment token storage using encrypted SharedPreferences and Android Keystore

## [0.0.3] - 2025-08-25

### Added

- **AuthService in App Module**: Added `AuthService` class in app module to handle authentication
  parameter retrieval from backend
- **Accessibility Enhancements**: Enhanced UI components with comprehensive accessibility support
    - Added content descriptions for all interactive elements
    - Implemented semantic properties for screen readers
    - Improved navigation and focus management

### Changed

- **BREAKING CHANGE**: SDK initialization now requires authentication parameters to be fetched by
  the app
    - Removed `suspend fun init(environmentKey: String, config: PaymentSheetConfig)` from SDK
    - Apps must now implement their own `AuthService` to fetch auth parameters from backend
    - Updated all example activities to use new initialization pattern
    - Updated `SpreedlyHelper.initializeSdkWithRemoteAuth()` to handle auth parameter fetching

### Removed

- **AuthParamsService**: Removed from SDK core - authentication API calls now handled by app layer
- **Suspend init method**: Removed `suspend fun init(environmentKey, config)` - replaced with
  explicit auth parameter passing
- **Auth-related tests**: Removed SDK tests for authentication parameter fetching since it's now
  app responsibility

### Fixed

- **Card Number Visual Transformation**: Fixed cursor positioning bug in card number formatting
    - Resolved crash when typing 4th digit in unknown card types
    - Improved offset mapping for dynamic card number formatting
    - Enhanced handling of space positions in formatted card numbers
- **Code Formatting**: Applied trailing comma formatting in SPLTextField component

### Security

- **Improved Security**: Authentication parameters are now fetched by the app, giving developers
  full control over the authentication flow

## [0.0.1] - 2025-08-08

> **Historical**: This entry reflects the original single-module API (`checkout-android`). The SDK
> was restructured into multi-module architecture in 0.7.1. See 0.7.1 migration notes above.

### Added

- **Initial Release** of Spreedly Android SDK
- **Core Payment Processing** with secure tokenization
- **Modern UI Components** built with Jetpack Compose
- **Modular Architecture** with separate modules:
    - `payments-core`: Core payment processing (includes analytics, networking, validation, result types, security, and UI)
    - `paymentsheet`: Pre-built payment sheet UI components
    - `hostedfields`: Secure payment field components
    - `braintree`: Braintree APM (PayPal, Venmo)
    - `stripe`: Stripe APM (iDEAL, Bancontact, EPS, P24, SEPA)
    - `threeds`: 3D Secure authentication

### Features

- **Secure Payment Field Components**
    - Card number validation with real-time formatting
    - Expiry date validation (month/year)
    - CVV validation with card type detection
    - Address field validation

- **Payment Sheet Integration**
    - Bottom sheet payment form
    - Inline payment forms
    - Customizable styling and theming
    - Support for additional billing fields

- **Form Validation**
    - Real-time validation feedback
    - Comprehensive validator classes:
        - `CardNumberValidator`: Validates card numbers with Luhn algorithm
        - `YearValidator`: Validates expiry years (current year and future)
        - `MonthValidator`: Validates months (1-12 range)
        - `CvvValidator`: Validates CVV codes
        - `ExpiryDateValidator`: Validates expiry date formats
        - `RequiredValidator`: Validates required fields

- **Network Layer**
    - Ktor-based HTTP client
    - Robust error handling
    - Request/response logging
    - Timeout configuration

- **Security Features**
    - Secure tokenization of payment data
    - PCI-compliant payment field handling
    - Data encryption utilities

### Technical Features

- **Build System**
    - Gradle Kotlin DSL with convention plugins
    - Multi-module architecture
    - Build flavors (development/production)
    - Version catalog for dependency management

- **Testing & Quality**
    - Comprehensive unit test coverage
    - Code coverage reporting with Kover
    - Lint checks with ktlint and Android Lint
    - Compose-specific lint checks (Slack compose-lint-checks)
    - Automated code formatting with Spotless

- **CI/CD Pipeline**
    - GitHub Actions workflows for CI/CD
    - Automated testing and building
    - Code quality checks with coverage reporting
    - Manual release workflow
    - Publishing to private Maven repository

### Developer Experience

- **Documentation**
    - Comprehensive README with integration guides
    - API documentation
    - Code examples and usage patterns
    - Workflow documentation

- **Development Tools**
    - Custom lint checks and rules
    - Code generation scripts
    - Coverage report generation
    - Local development setup scripts

### Installation

> **Note**: This snippet reflects the original 0.0.1 single-module artifact. See `README.md` for current multi-module installation (0.7.1+).

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

// In app/build.gradle.kts (OBSOLETE — see README.md for current dependencies)
dependencies {
    implementation("com.spreedly:checkout-android:0.0.1")
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
- **Kotlin Version**: 2.1.20
- **KSP** (when using processors such as Hilt or Room): 2.3.6
- **Android Gradle Plugin** (reference): 8.13.2
- **Compose BOM**: 2025.10.01

For detailed integration guides and API documentation, visit
our [documentation](https://developer.spreedly.com/docs/mobile-applications).

For support or questions, please contact our support team or create an issue in the repository. 