# Custom Payment Forms

Build payment forms that combine Spreedly's secure SPL text fields with custom-validated fields. The SDK is designed to **minimize PAN/CVV exposure** in your app process; merchants confirm PCI scope with their QSA/acquirer.

## Overview

The custom payment forms approach uses a hybrid architecture:

- **SPL Text Fields** handle sensitive card data (card number, expiry date, CVV) so PAN/CVV stay in SDK-managed fields
- **Custom Text Fields** handle personal and billing information with your own validation logic
- **Custom Button** calls `sdk.createCreditCard()` to submit the payment

```
SPL Fields (Secure)              Custom Fields (Flexible)
├── Card Number                  ├── Name (letters, spaces, hyphens)
├── Expiry Date                  ├── Address (min length)
└── CVV                          ├── City (letters only)
                                 ├── State (2-3 uppercase)
                                 └── Zip (format validation)

                    Custom Button (sdk.createCreditCard())
```

SPL fields enforce security automatically:
- CVV fields clear after 3 minutes when the app is backgrounded
- Card number fields block copy, cut, and text selection (paste is allowed)
- CVV fields block all clipboard operations and text selection

**`SPLTextField` callbacks:** `onChange` receives **AES-encrypted** ciphertext for `FormFieldType.CARD`, `FormFieldType.CVV`, and `FormFieldType.ACCOUNT_NUMBER` only (`FormFieldType.shouldEncrypt()` is deprecated / LIBRARY_GROUP-restricted — behavior unchanged); all other field types receive **raw** processed text in `onChange`. Do **not** log encrypted strings or treat them as display digits. Use **`onFieldStateChange(HostedFieldState)?`** for iframe-style observability: digit **counts** (`numberLength` / `cvvLength`), `cardScheme`, `isValid`, focus/blur via `HostedFieldEventType`, without parsing ciphertext. Use `onValidationChange` / `hasValidationError` / submit results for gating checkout. Kotlin/Java samples: [Migration from legacy](migration/from-legacy.md#hostedfieldstate--kotlin-samples-compose).

**Card scheme detection (CARD field):** Scheme detection and CVV-length coupling run on **ciphertext stored by the SDK**, not on plaintext you pass from a custom `EditText`. Wire the PAN through **`SPLTextField(FormFieldType.CARD(...))`** so `onChange` delivers encrypted values to `sdk.callbacks.onCardNumberChange`. If you call `onCardNumberChange` with raw digit strings (headless demos, manual callback wiring), **`HostedFieldState.cardScheme` stays `null`** and CVV rules fall back to the unknown-scheme default until valid SDK ciphertext is stored — this is intentional after field-encryption hardening. See [security.md](security.md#card-scheme-only-storage).

### Character limits (`FormFieldCharLimits`)

Address, name, email, phone, country, and canonical hosted-fields keys use limits aligned with the web SDK (`FormFieldCharLimits` in `payments-core`). Examples: address/canonical text **500**, combined name **350**, email **254**, international ZIP **15**, country **100**. Limits apply at typing time in `SPLTextField` and at submit for `additionalFields` / address fields.

### Catalogue `SPLTextField` and `sdk`

**CARD** and **CVV** always require a non-null **`sdk`** (hosted PAN/CVV display + encrypted tokenize state).

For **catalogue** types (`EMAIL`, name, address, shipping, canonical keys, etc.), pass the same **`sdk`** instance you use for `createCreditCard()` / `createPaymentMethod()` whenever you rely on any of:

| Feature | Requires non-null `sdk` on catalogue `SPLTextField` |
|---------|-----------------------------------------------------|
| Mount tracking (`formFields ∪ mounted` submit union) | **Yes** — `DisposableEffect` registers mounts only when `sdk != null` |
| Hosted-wins owned-key merge at tokenize | **Yes** — ownership comes from mounted types + merchant `formFields` |
| `syncHostedCatalogueFieldValue` → `PaymentSheetState` | **Yes** when `writeCatalogueValueToSdk = true` (throws on input if `sdk` is null) |

**Without `sdk`**, a catalogue field may still **compose** (legacy binary-compatible overload defaults `writeCatalogueValueToSdk = false`), but it does **not** register as mounted, does **not** join hosted-wins or the submit union, and does **not** sync into `PaymentSheetState`. You must list the type in `formFields` and update state yourself via `sdk.callbacks` / `sdk.optionalCallbacks` (or `additionalFields` at submit). That path is for callback-owned layouts only — not for hosted-wins catalogue flows.

ACH `ROUTING_NUMBER` / `ACCOUNT_NUMBER` are carved out of catalogue mount/sync (encrypt types).

**Callback-owned state (drop-in sheet, Java helper, or headless layouts mirroring callbacks):**

```kotlin
SPLTextField(
    formFieldType = FormFieldType.EMAIL(required = true),
    sdk = sdk,
    label = "Email",
    value = sdk.paymentState.value.email.value,
    onChange = { sdk.optionalCallbacks.onEmailChange(it, isRequired = true) },
    writeCatalogueValueToSdk = false,
)
```

**Sync-only headless layout (SDK writes `PaymentSheetState`; omit callbacks or use `onChange` only for local UI):**

```kotlin
SPLTextField(
    formFieldType = FormFieldType.EMAIL(required = true),
    sdk = sdk,
    label = "Email",
    onChange = { /* optional local mirror */ },
    // writeCatalogueValueToSdk defaults to true
)
```

### `writeCatalogueValueToSdk` (default `true`)

When **`true`**, each catalogue keystroke also calls `syncHostedCatalogueFieldValue` → `PaymentSheetState`. When **`false`**, only your `onChange` / callbacks update state (drop-in `PaymentSheet` sets `false` on every catalogue field it owns).

| Layout | Set `writeCatalogueValueToSdk` |
|--------|-------------------------------|
| Drop-in `PaymentSheet` / callback wiring to `sdk.callbacks` | **`false`** |
| Headless sync-only (no duplicate callback writes) | **`true`** (default) |
| Headless with `onChange` → `sdk.optionalCallbacks` | **`false`** |

Default **`true`** is correct for sync-only headless forms. Using callbacks **and** leaving the default **`true`** double-updates state on every keystroke — set **`false`** when callbacks own the value.

**Footgun checklist**

| You wire | Set `writeCatalogueValueToSdk` |
|----------|-------------------------------|
| `onChange` → `sdk.callbacks` / `sdk.optionalCallbacks` only | **`false`** |
| No callbacks; SDK should own `PaymentSheetState` | **`true`** (default) |
| Drop-in `PaymentSheet` / Express sheet | **`false`** (SDK sets this for you) |

CARD, CVV, expiry, and ACH encrypt types never sync catalogue plaintext/ciphertext into `PaymentSheetState` through this path — only non-encrypted catalogue types use `syncHostedCatalogueFieldValue`.

### Additional-fields pluck at tokenize (Phase 1)

Matches Checkout Web SDK `pluckWhitelistedValues`:

| Key in `additionalFields`? | Tokenize value |
|----------------------------|----------------|
| **Present** (including `""` or whitespace-only) | That map value unchanged |
| **Absent** | `PaymentSheetState` / callback-populated state for that wire key |

All present name keys (`first_name`, `last_name`, `full_name`) are forwarded independently to the backend — no key suppresses another. The Spreedly API resolves conflicts server-side: when `full_name` is present, it is parsed to derive `first_name` and `last_name` if those are missing or incomplete. This means if you supply all three, the explicitly provided `first_name` and `last_name` values are preserved and sent alongside `full_name`.

> **Key aliases:** `"firstName"` and `"lastName"` (camelCase) are accepted as aliases for `"first_name"` and `"last_name"`. Snake_case takes precedence when both forms are present for the same field.

This is separate from hosted-wins below.

#### Canonical hosted-fields wire keys

Ten keys aligned with the Checkout Web SDK on `AdditionalField` / `CreditCard` (pass via `additionalFields` or mount the matching catalogue `FormFieldType`). Phone country/area parts are **separate** keys — they are not concatenated into `phone_number`. Legacy `address1` / `shipping_address1` (and related legacy shipping keys) may appear in the **same** request when both families are supplied; the backend owns conflict resolution.

| Wire key | `AdditionalField` |
|----------|-------------------|
| `house_number_or_name` | `HOUSE_NUMBER_OR_NAME` |
| `street` | `STREET` |
| `street_line2` | `STREET_LINE_2` |
| `shipping_house_number_or_name` | `SHIPPING_HOUSE_NUMBER_OR_NAME` |
| `shipping_street` | `SHIPPING_STREET` |
| `shipping_street_line2` | `SHIPPING_STREET_LINE_2` |
| `phone_number_country_code` | `PHONE_NUMBER_COUNTRY_CODE` |
| `phone_number_area_code` | `PHONE_NUMBER_AREA_CODE` |
| `shipping_phone_number_country_code` | `SHIPPING_PHONE_NUMBER_COUNTRY_CODE` |
| `shipping_phone_number_area_code` | `SHIPPING_PHONE_NUMBER_AREA_CODE` |

```kotlin
// Key present → sends "" even if the UI field shows a name
sdk.createCreditCard(
    formFields = listOf(FormFieldType.CARD(true), FormFieldType.CVV(true), FormFieldType.EXPIRY_DATE(true)),
    additionalFields = mapOf(AdditionalField.FULL_NAME to "   "),
)
```

### Hosted-wins merge at tokenize

For card tokenize, **owned keys** = `formFields` you pass to `createCreditCard()` **plus** catalogue field types currently mounted in `SPLTextField` (with `sdk`). For owned keys, hosted / sheet state wins over conflicting `additionalFields` entries — the merge strips the merchant key regardless of value (`""`, whitespace-only, or non-blank). One sanitized log line lists stripped key names only.

**Owned vs unowned at tokenize:**

| Ownership | Merchant `additionalFields` key present | Tokenize value source |
|-----------|----------------------------------------|------------------------|
| **Owned** | Any value (including blank / whitespace) | Hosted / sheet state for that wire key |
| **Unowned** | Present (including blank / whitespace) | Merchant pluck value unchanged |
| **Unowned** | Absent | `PaymentSheetState` / callback state |

**Blank owned state:** If the owned hosted/sheet value is blank, tokenize sends blank for that wire key even when `additionalFields` carried a non-blank value (the additional entry is stripped). Fill the mounted field / sheet callbacks before submit, or do not list the type in `formFields` and do not mount it when you intend `additionalFields` alone to supply the value.

**Owned shipping vs billing fallback:** For `shipping_address1` / `shipping_address2` / `shipping_city` / `shipping_state` / `shipping_zip`, unowned blank dedicated shipping still falls back to the matching billing sheet value (legacy). When that shipping key is **owned**, blank hosted/sheet shipping wins — billing is not substituted after hosted-wins strips conflicting `additionalFields`.

Unowned keys keep additional-first pluck merge.

**Submit validation union (card tokenize only):** `createCreditCard()` and card-path `createPaymentMethod()` (when you pass non-empty `formFields` **or** have mounted catalogue `SPLTextField` types with `sdk`) validate merchant `formFields` **plus** mounted catalogue types not already listed. **`createBankAccount()` does not use this union** — it validates only the `formFields` you pass. See [ACH Bank Account — card vs ACH](ach-bank-account.md#hosted-catalogue-fields-card-vs-ach). **Mounted required** fields block card submit even when omitted from `formFields`. **Mounted optional** non-encrypted fields are unioned but follow the same optional non-encrypted rule — they do **not** block submit when blank/invalid (same as optional fields explicitly listed in `formFields`). List types in `formFields` when you want pre-submit UI gating via `areAllFieldsValid()`.

When neither `formFields` nor mounted catalogue fields are present, `createPaymentMethod()` keeps legacy behavior (no client-side field validation union).

### Custom additional-field validators

Custom validation is **PCI-conscious** (sanitized blur UI, PCI-stripped validator snapshots,
generic tokenize egress) — not a claim of PCI SSC Secure Software assessment or merchant PCI
compliance. Confirm scope with your QSA/acquirer.

Register cross-field rules on the **29 allowlisted wire keys** (excludes PAN, CVV/CVC, expiry,
and ACH bank fields; includes cardholder name) with the merchant public API below. Prefer the **[AdditionalField] overload** to avoid wire-key typos; string snake_case keys remain supported for web parity.

**Merchant public API (custom validators):**

| Method | Purpose |
|--------|---------|
| `addValidation` | Register a validator for one allowlisted field |
| `removeValidation` | Unregister |
| `runCustomValidationBeforeSubmit(formFields, additionalFields)` | Pre-submit custom validation with the same merged map as tokenize; returns `true` when any validator failed — block Pay and do not call tokenize |

Hosted blur evaluation and sticky-latch clearing on field edit run automatically inside mounted `SPLTextField` — merchants do not call blur helpers directly. For cross-field dependency changes (for example country → zip), call **`runCustomValidationBeforeSubmit`** on Pay instead of clearing sticky state manually.

| Trigger | Behavior |
|---------|----------|
| **Hosted blur** | After built-in validation passes, the SDK runs the registered validator on blur; blur UI may show a **sanitized** `CustomValidationResult.errorMessage` |
| **Tokenize** | After built-in checks and hosted-wins merge, before the API call; **fail-closed** on `isValid = false`, throw, or malformed return |

Validators do **not** run on ACH (`createBankAccount`), recache, or `areAllFieldsValid()` polling alone.

**Registry lifetime:** Calling `init()` on the **same** `Spreedly` instance (for example refreshing signed auth) **retains** registered validators. A **new** `Spreedly(...)` instance starts with an empty registry.

**Submit snapshot:** Custom validators run inside `createCreditCard` using the same **present-field snapshot** as tokenize (mounted catalogue fields merged with `additionalFields`). Pass the same `additionalFields` to tokenize as you use for blur so cross-field rules evaluate consistently; `formFields` controls which hosted fields are included in the snapshot.

**Present-field edge cases (iOS parity):**

| Situation | Validator runs? |
|-----------|-----------------|
| Field mounted in `SPLTextField` (even if value is blank) | Yes |
| Key in submit `additionalFields` (even if value is `""`) | Yes |
| Key only in `PaymentSheetState` / prefilled, not mounted | **No** |
| Registered but unmounted and omitted from `additionalFields` | **No** |
| Prior sticky failure on a field later omitted from snapshot | Sticky **cleared** on next preflight |

Cross-field rules (for example zip keyed off country) need the dependency field **present** in the same snapshot — mount it or pass it in `additionalFields` at submit.

**CheckoutButton click order:** On Pay, `CheckoutButton` runs **`beforeSubmit()`** (optional auth refresh hook) → **`createCreditCard`**. It does **not** call `areAllFieldsValid()` or `runCustomValidationBeforeSubmit()` inside `onClick` — `isFormValid` only controls whether the button is enabled before tap; custom validators run inside `createCreditCard` and a failure returns `ValidationFailed`. If you build a custom Pay button and want to refresh inline errors before tokenize, use **`runCustomValidationBeforeSubmit` → `areAllFieldsValid` → tokenize** (sample below).

**Return shape:** `CustomValidationResult(isValid, errorMessage?)` only. On tokenize failure, `PaymentResult.Failed`, logs, and telemetry use a **generic SDK message** — merchant `errorMessage` is never forwarded there. Custom-validator error telemetry uses `reason` values `threw`, `malformed`, or `fields_oversized` (metadata only; no merchant text).

The `fields` callback argument is a **sparse present-field snapshot** (iOS parity): only allowlisted [AdditionalField] keys in the current evaluation map are included — mounted catalogue values merged with submit `additionalFields` at tokenize/preflight, or mounted values plus the blurred field overlay at blur. Keys not on the form and not in submit `additionalFields` are **omitted** (validators for those keys are not invoked). Use `fields[AdditionalField.COUNTRY].orEmpty()` or `fieldValue()`. **Breaking (Unreleased):** recompile validators after upgrading — the map type changed from `Map<String, String>` to `Map<AdditionalField, String>` (see [CHANGELOG](../CHANGELOG.md)).

**Snapshot merge precedence (tokenize / preflight):** Mounted catalogue field values always win over `additionalFields` for the same key — even when the hosted value is blank. This mirrors tokenize hosted-wins semantics: if a field is mounted, its value is authoritative and `additionalFields` cannot override it. If a key is **not** mounted, `additionalFields` supplies the value. For the combined `NAME` field (`NameFieldDisplayMode.SINGLE_FIELD`), the snapshot derives `first_name` and `last_name` by parsing `nameOnCard` — the same logic tokenize uses — so custom validators always see a consistent split regardless of how the name was entered. Pass `additionalFields` to supply values for fields that are not mounted (e.g. a country picker rendered outside the payment sheet); do not rely on it to override a mounted field.

> **Blank NAME field:** When the NAME `SPLTextField` is empty, `full_name` validators receive `" "` (a single space), not `""` — this sentinel matches what tokenize sends and is consistent between blur and submit. Check with `value.isBlank()` rather than `value.isEmpty()` to handle it correctly.
>
> **Single-word NAME field:** When the cardholder enters only one word (e.g. `"Alice"`), `last_name` validators receive `" - "` — the same sentinel value that tokenize sends in the request. `full_name` validators receive the raw single word unchanged. Check `last_name` with `value.trim() == "-"` or `value.isBlank()` to detect the no-last-name case.

**Blur snapshot:** At blur time `additionalFields` is not available — blur fires on a field event before the merchant calls `createCreditCard`. The blur snapshot contains only the currently mounted catalogue field values; cross-field validation at blur is limited to those mounted fields.

**Allowlist (29 snake_case keys):** `first_name`, `last_name`, `full_name`, `address1`, `address2`, `city`, `state`, `zip`, `country`, `phone_number`, `company`, `email`, shipping twins (`shipping_address1` … `shipping_phone_number`), and the ten canonical keys from [Canonical hosted-fields wire keys](#canonical-hosted-fields-wire-keys). Keys such as `number`, `cvv`, `month`, `year`, `expiry`, and ACH bank fields are rejected (no-op + sanitized warn).

```kotlin
Spreedly.addValidation(AdditionalField.ZIP_CODE) { value, fields ->
    val country = fields[AdditionalField.COUNTRY].orEmpty()
    val valid = when (country.uppercase()) {
        "US" -> value.matches(Regex("^\\d{5}(-\\d{4})?$"))
        else -> value.length in 1..15
    }
    CustomValidationResult(valid, if (valid) null else "Invalid postal code")
}

// Instance-scoped (preferred when you hold a Spreedly reference):
sdk.addValidation(AdditionalField.STATE, ::validateState)

// String wire keys still work (web parity):
// sdk.addValidation("zip") { value, fields -> ... }
```

Mount the field in `SPLTextField` with **`sdk = sdk`** so blur evaluation and submit gating see the same registry. Custom validators run inside `createCreditCard`, which also refreshes inline error state — so inline errors appear even on fields the user never blurred before tapping Pay. Inline error state is also set on blur and preserved until the field value changes.

**Performance:** Keep merchant validators fast, deterministic, side-effect-free, non-blocking, and
thread-safe. `CheckoutButton` runs them once, inside `createCreditCard`. A custom Pay button that
calls `runCustomValidationBeforeSubmit` and then `createCreditCard` runs them twice (preflight,
then tokenize). Blur and `runCustomValidationBeforeSubmit` run on the main thread; direct tokenize
follows the caller coroutine context. Blur UI messages are capped at 256 characters with
boundary-aware sanitization via `LogSanitizer.sanitizeStoredErrorString`.

> **Do not block the main thread inside a validator.** Blur and submit preflight run
> validators synchronously on the main thread. A blocking network call or I/O inside a
> validator freezes the UI for its entire duration.
>
> ```kotlin
> // ❌ WRONG — blocks the main thread, freezes the UI during blur/preflight
> sdk.addValidation("email") { value, _ ->
>     val ok = myApi.validateEmailSync(value)   // synchronous network call!
>     CustomValidationResult(isValid = ok)
> }
>
> // ✅ RIGHT — keep the validator fast; do async work outside the callback.
> // Fail-closed until a check for THIS generation's value has returned, including when
> // the field is edited away and back to a value that was already confirmed valid:
> // onEmailChanged clears the cache synchronously before launching, so a Pay tap during
> // the resulting re-check sees no confirmed result yet, not the stale confirmation from
> // the earlier check of the same string. A single immutable EmailCheck published through
> // one @Volatile reference also avoids a torn read: two separate @Volatile fields (an
> // email string and a boolean) written as two separate statements let a reader on another
> // dispatcher — direct createCreditCard follows the caller coroutine context, so this can
> // be a different thread than the one running the validator — observe the new email
> // paired with the previous boolean, a fail-open window if that previous boolean was
> // `true`. A single object reference is replaced atomically. cancel() on the previous job
> // also stops a stale request from doing work once it's superseded — defense in depth,
> // since myApi.validateEmail must itself be cooperatively cancellable for cancel() to
> // take effect immediately. Call onEmailChanged from the field's onChange.
> private data class EmailCheck(val email: String, val valid: Boolean)
>
> private val validationGeneration = AtomicInteger(0)
> private var validationJob: Job? = null
> @Volatile private var lastConfirmed: EmailCheck? = null
>
> fun onEmailChanged(email: String) {
>     val generation = validationGeneration.incrementAndGet()
>     lastConfirmed = null // fail closed immediately, even if this exact value was valid before
>     validationJob?.cancel()
>     validationJob = viewModelScope.launch {
>         val ok = myApi.validateEmail(email)
>         // Superseded by a newer onEmailChanged call while this was in flight — drop it.
>         if (generation != validationGeneration.get()) return@launch
>         lastConfirmed = EmailCheck(email, ok)
>     }
> }
> sdk.addValidation("email") { value, _ ->
>     val confirmed = lastConfirmed
>     CustomValidationResult(isValid = confirmed != null && confirmed.email == value && confirmed.valid)
> }
> ```
>
> Revalidate before submit, not just on keystroke: if the user taps Pay while a check for the
> current value is still in flight — including a re-check after editing away and back —
> `lastConfirmed` is `null` and the validator correctly fails closed, blocking tokenize until
> it resolves. Disable Pay while a check is pending for better UX, rather than relying on
> fail-closed alone to explain the rejection.
>
> `runCustomValidationBeforeSubmit` is a `@MainThread` call — invoke it on the main thread,
> then launch tokenize in a coroutine:
>
> ```kotlin
> Button(onClick = {
>     // runCustomValidationBeforeSubmit is @MainThread — call it here, not on Dispatchers.IO
>     val failed = sdk.runCustomValidationBeforeSubmit(formFields, additionalFields)
>     if (!failed) {
>         viewModelScope.launch {
>             sdk.createCreditCard(formFields, additionalFields)
>         }
>     }
> })
> ```

**Pay-path evaluation:** `CheckoutButton` calls `createCreditCard` directly — custom validators
run inside it and a failure returns `PaymentProcessingResult.ValidationFailed`. If you build a
custom Pay button and want to refresh inline errors on all fields before tokenize, call
`runCustomValidationBeforeSubmit` first; if it returns `true` (at least one validator failed),
do not call `createCreditCard` — inline errors are already set and the button stays active.

**Process death:** Custom validator registrations and sticky blur failure latches live in the `Spreedly` instance process memory only. They are not restored after the app process is killed; re-register validators after recreating the SDK.

**CheckoutButton vs web `submitClick`:** Android has no iframe submit protocol. Use `CheckoutButton` (or your own Pay control calling `createCreditCard` / `runCustomValidationBeforeSubmit`) instead of web `submitClick` events. When `runCustomValidationBeforeSubmit` returns `true`, at least one validator failed — **stop** and do not call tokenize.

**Custom Pay button:** Build `additionalFields` once and pass the **same** `formFields` and `additionalFields` to preflight and tokenize (do not call preflight with default empty args):

**Pay button enablement:** `CheckoutButton` defaults `isFormValid = true` so Pay stays clickable and custom validators run inside `createCreditCard` on tap (not as a separate preflight call). If you disable Pay when `areAllFieldsValid()` or per-field `onValidationChange` reports invalid, a **cross-field** custom failure can leave Pay disabled after the user fixes a dependency field (for example changing country without re-blurring zip) — **`runCustomValidationBeforeSubmit` never runs**. Prefer keeping Pay enabled once **core** fields (card, merchant-owned text) are filled and gating inside `onClick` with **`runCustomValidationBeforeSubmit` → `areAllFieldsValid` → tokenize** on a custom Pay button. Do not add a second aggregate `isFormValid` check after preflight — hosted `onValidationChange` can lag preflight by a frame and block tokenize incorrectly.

```kotlin
Button(
    onClick = {
        val additionalFields = mapOf(
            AdditionalField.ZIP_CODE to zipInput.value,
            // …same map you pass to createCreditCard
        )
        // Preflight before areAllFieldsValid when validators depend on other fields (e.g. zip + country).
        if (sdk.runCustomValidationBeforeSubmit(formFields, additionalFields)) return@Button
        if (!sdk.areAllFieldsValid(formFields)) return@Button
        coroutineScope.launch {
            sdk.createCreditCard(
                formFields = formFields,
                additionalFields = additionalFields,
            )
        }
    },
)
```

#### Field limits and value sources

Built-in maxlength and typing validation use public `FormFieldCharLimits` (aligned with the Checkout Web SDK). Custom validators receive the present-field map at **submit preflight and tokenize**. Blur uses mounted catalogue values plus the current field’s display overlay — not submit `additionalFields` passed later at Pay.

| Value source | Used at blur | Used at submit preflight / tokenize |
|--------------|--------------|-------------------------------------|
| Mounted `SPLTextField` + `sdk` | Field display value overlaid on SDK state | Included in evaluation snapshot (hosted-wins for owned keys) |
| Submit `additionalFields` | **Not** read at blur | Merged when key is **not mounted** (hosted-wins: mounted blank value takes priority over a conflicting `additionalFields` entry) |
| Unmounted, not in `additionalFields` | Validator **not invoked** on blur | Validator **not invoked** on submit/preflight/tokenize |

Cross-field validators may therefore see different snapshots at blur vs submit when you pass submit-only `additionalFields`.

**29-key custom-validator allowlist and character limits** (parity with the Checkout Web SDK; bank ACH keys excluded from custom validators):

| Wire key | Max length | Hosted `FormFieldType` / sheet `OptionalFieldType` |
|----------|------------|-----------------------------------------------------|
| `first_name` | 350 | `FIRST_NAME` |
| `last_name` | 350 | `LAST_NAME` |
| `full_name` | 350 | `NAME` |
| `email` | 254 | `EMAIL` |
| `company` | 500 | `COMPANY` |
| `address1` | 500 | `ADDRESS_LINE_1` |
| `address2` | 500 | `ADDRESS_LINE_2` |
| `house_number_or_name` | 500 | `HOUSE_NUMBER_OR_NAME` |
| `street` | 500 | `STREET` |
| `street_line2` | 500 | `STREET_LINE_2` |
| `city` | 500 | `CITY` |
| `state` | 500 | `STATE` |
| `zip` | 15 | `ZIP` / `ZIP_CODE` |
| `country` | 100 | `COUNTRY` |
| `phone_number` | 15 | `PHONE_NUMBER` |
| `phone_number_country_code` | 15 | `PHONE_NUMBER_COUNTRY_CODE` |
| `phone_number_area_code` | 15 | `PHONE_NUMBER_AREA_CODE` |
| `shipping_address1` | 500 | `SHIPPING_ADDRESS_LINE_1` / `SHIPPING_ADDRESS_1` |
| `shipping_address2` | 500 | `SHIPPING_ADDRESS_LINE_2` / `SHIPPING_ADDRESS_2` |
| `shipping_house_number_or_name` | 500 | `SHIPPING_HOUSE_NUMBER_OR_NAME` |
| `shipping_street` | 500 | `SHIPPING_STREET` |
| `shipping_street_line2` | 500 | `SHIPPING_STREET_LINE_2` |
| `shipping_city` | 500 | `SHIPPING_CITY` |
| `shipping_state` | 500 | `SHIPPING_STATE` |
| `shipping_zip` | 15 | `SHIPPING_ZIP` |
| `shipping_country` | 100 | `SHIPPING_COUNTRY` |
| `shipping_phone_number` | 15 | `SHIPPING_PHONE_NUMBER` |
| `shipping_phone_number_country_code` | 15 | `SHIPPING_PHONE_NUMBER_COUNTRY_CODE` |
| `shipping_phone_number_area_code` | 15 | `SHIPPING_PHONE_NUMBER_AREA_CODE` |

Required vs optional at submit follows each field’s `required` flag (hosted mount or `formFields` / sheet config). PAN, CVV, expiry, and ACH bank keys cannot be registered — see [Custom additional-field validators](#custom-additional-field-validators).

**Java:**

```java
Spreedly.addValidation(AdditionalField.ZIP_CODE, (value, fields) -> {
    String country = fields.getOrDefault(AdditionalField.COUNTRY, "");
    if (!"US".equalsIgnoreCase(country)) {
        return new CustomValidationResult(true, null);
    }
    boolean valid = value.matches("^\\d{5}(-\\d{4})?$");
    return new CustomValidationResult(valid, valid ? null : "Invalid postal code");
});

// Later, on the same Spreedly instance:
Spreedly.removeValidation(AdditionalField.ZIP_CODE);
```

#### Android vs web divergence

| Topic | Web | Android |
|-------|---------------|---------|
| Validator throw / malformed return | Fail-open (treat as valid) | **Fail-closed** (block tokenize) |
| `errorMessage` on tokenize Failed | May surface to merchant | **Ignored** — generic Failed message only |
| When validators run | Blur + tokenize (+ web `validate()` snapshot API) | Blur + submit preflight + tokenize (no standalone `validate()` API) |
| Name typing pattern | Latin pattern optional on web | UTF-8 `NameValidator`; no Latin typing-time pattern |
| `HostedFieldState` value | Web `fieldStateChange` may include raw value | **No raw typed value** on catalogue state (counts/scheme only) |
| ACH address catalogue | N/A on web card form | **Out of scope** on Android ACH path |

Merchants confirm PCI scope with their QSA/acquirer; this table makes no SAQ eligibility claim.

Do not use `PaymentResult.Failed.message` for custom-validator UX — use inline field errors from preflight/blur. Full tokenize-egress table: [Security — Tokenize egress](security.md#tokenize-egress-merchant-messages-never-leak).

### Custom `PaymentSheet` and optional catalogue fields

Drop-in `PaymentSheet` / `SpreedlyPaymentBottomSheet` wire core card fields through your `callbacks` parameter and catalogue optional fields (email, shipping, phone splits, etc.) through **`sdk.optionalCallbacks`**. Legacy billing optional fields still use `callbacks`.

If you embed `PaymentSheet` with **merchant-owned** `state` and `callbacks` but leave catalogue optional fields on default `sdk.optionalCallbacks`, keystrokes update **`sdk.paymentState`** while your composable reads **merchant `state`** — the UI and submit path desync. Fix: route optional catalogue `onChange` handlers to the same `state`/`callbacks` you pass in, or read `val paymentState by sdk.paymentState` like `SpreedlyPaymentBottomSheet`.

**Default optional fields:** inline `PaymentSheet` defaults `additionalFields` to `ConfigurableFormField.allOptionalFields` (five billing fields, all optional). `SpreedlyPaymentBottomSheet` defaults to `emptyList()` — pass `additionalFields` explicitly when you want optional address fields in the modal sheet.

For initial SDK setup, see [getting-started.md](getting-started.md).

### Screen capture protection

Any screen showing payment fields must use `SecureScreen()` or `Modifier.secureScreen()` to block PAN/CVV from screenshots and the recent-apps thumbnail. This is not a guarantee on every device or capture path — see [Security — Limitations](security.md#limitations). Wrap your checkout column or activity content:

```kotlin
import com.spreedly.security.secureScreen

Column(
    modifier = Modifier
        .fillMaxSize()
        .secureScreen(),
) {
    // SPL + custom fields
}
```

See [security.md](security.md) for PCI scope notes.

## Setup

### FormInput Base Class

Define validation logic for each custom field by extending `FormInput`. Each subclass provides a `validator` function that returns an error or `null` for valid input.

```kotlin
class NameInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, NameError>(value, isPristine) {
    override fun validator(value: String): NameError? =
        when {
            value.isBlank() -> NameError.Empty
            value.length < 2 -> NameError.TooShort
            !value.matches(Regex("^[a-zA-Z\\s'-]+$")) -> NameError.InvalidCharacters
            else -> null
        }
}

enum class NameError {
    Empty,
    TooShort,
    InvalidCharacters,
}
```

### CustomTextField Component

Create a reusable composable for non-sensitive fields:

```kotlin
@Composable
fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) Color(0xFFDC2626) else Color(0xFF374151),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (isError) Color(0xFFDC2626) else Color(0xFFD1D5DB),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
```

For styling guidance, see [theme-and-styling.md](theme-and-styling.md).

## SPL Text Fields

Use `SPLTextField` (from the `:hostedfields` module) with `FormFieldType` (from `com.spreedly.sdk.models`) for all sensitive card data. Each field requires a `formFieldType` and an `onChange` callback. **CARD** and **CVV** require `sdk` at compose time. For **catalogue** fields, pass **`sdk`** whenever you need mount tracking, hosted-wins merge, or `writeCatalogueValueToSdk = true` (see [Catalogue `SPLTextField` and `sdk`](#catalogue-spltextfield-and-sdk) above). Callback-only catalogue fields may omit `sdk` only when you set `writeCatalogueValueToSdk = false` and you are **not** relying on mounted submit union or hosted-wins for that field.

### Single Expiry Field

```kotlin
@Composable
fun CardFieldsSection(sdk: Spreedly) {
    var expiryValue by remember { mutableStateOf("") }

    SPLTextField(
        formFieldType = FormFieldType.EXPIRY_DATE(),
        label = "Expiry Date (MM/YY)",
        value = expiryValue,
        onChange = { encrypted ->
            expiryValue = encrypted
            sdk.callbacks.onExpirationDateChange(encrypted, true)
        },
    )

    SPLTextField(
        formFieldType = FormFieldType.CARD(true),
        sdk = sdk,
        label = "Card Number",
        onChange = sdk.callbacks::onCardNumberChange,
    )

    SPLTextField(
        formFieldType = FormFieldType.CVV(true),
        sdk = sdk,
        label = "CVV",
        onChange = sdk.callbacks::onSecurityCodeChange,
    )
}
```

### Separate Month and Year Fields

If your design calls for separate inputs, use `FormFieldType.MONTH()` and `FormFieldType.YEAR()`:

```kotlin
SPLTextField(
    formFieldType = FormFieldType.MONTH(),
    label = "Month",
    value = monthValue,
    onChange = { monthValue = it },
)

SPLTextField(
    formFieldType = FormFieldType.YEAR(),
    label = "Year",
    value = yearValue,
    onChange = { yearValue = it },
)
```

Separate month/year fields require combined validation — see [Combined Expiry Validation](#combined-expiry-validation) below.

For field focus and navigation, see [focus-management.md](focus-management.md).

## Custom Fields

Each custom field follows the same pattern: define a `FormInput` subclass, wire it to a `CustomTextField`, and sync the value with the SDK via callbacks.

### Name Field

```kotlin
CustomTextField(
    label = "Full Name",
    value = nameInput.value,
    onValueChange = { value ->
        nameInput = NameInput(value, false)
        sdk.callbacks.onNameOnCardChange(value, true)
    },
    isError = nameInput.isNotValid && !nameInput.isPristine,
    errorMessage = when (nameInput.error) {
        NameError.Empty -> "Name is required"
        NameError.TooShort -> "Name must be at least 2 characters"
        NameError.InvalidCharacters ->
            "Name can only contain letters, spaces, hyphens, and apostrophes"
        null -> null
    },
)
```

### Address Field

```kotlin
class AddressInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, AddressError>(value, isPristine) {
    override fun validator(value: String): AddressError? =
        when {
            value.isBlank() -> AddressError.Empty
            value.length < 5 -> AddressError.TooShort
            else -> null
        }
}

enum class AddressError { Empty, TooShort }
```

### State Code Field

Automatically converts input to uppercase and validates 2-3 letter codes:

```kotlin
class StateInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, StateError>(value, isPristine) {
    override fun validator(value: String): StateError? =
        when {
            value.isBlank() -> StateError.Empty
            value.length !in 2..3 -> StateError.InvalidLength
            !value.matches(Regex("^[A-Z]{2,3}$")) -> StateError.InvalidFormat
            else -> null
        }
}

CustomTextField(
    label = "State",
    value = stateInput.value,
    onValueChange = { value ->
        val upperValue = value.uppercase()
        stateInput = StateInput(upperValue, false)
        sdk.callbacks.onStateChange(upperValue, true)
    },
    capitalization = KeyboardCapitalization.Characters,
)
```

### Zip / Postal Code Field

This is a fully custom `CustomTextField`, not `SPLTextField` — the filtering and format regex
below are the merchant's own choice, not SDK behavior. The built-in `SPLTextField` for
`FormFieldType.ZIP` / `SHIPPING_ZIP` does not filter keystrokes or enforce a format; see
[Character limits](#character-limits-formfieldcharlimits) and the `StandardZipCodeValidator` note under
[Error Message Helpers](#error-message-helpers). Use a pattern like this only if you're building
your own non-hosted ZIP field and want to enforce US ZIP codes (12345, 12345-6789) or international
formats (Canadian K1A 0B1, UK SW1A 1AA) yourself:

```kotlin
class ZipCodeInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, ZipCodeError>(value, isPristine) {
    override fun validator(value: String): ZipCodeError? =
        when {
            value.isBlank() -> ZipCodeError.Empty
            value.length > 15 -> ZipCodeError.TooLong
            !value.matches(Regex("^[A-Za-z0-9\\s-]+$")) -> ZipCodeError.InvalidFormat
            else -> null
        }
}

enum class ZipCodeError { Empty, InvalidFormat, TooLong }

CustomTextField(
    label = "Zip Code",
    value = zipCodeInput.value,
    onValueChange = { value ->
        val filtered = value
            .filter { it in 'A'..'Z' || it in 'a'..'z' || it.isDigit() || it == ' ' || it == '-' }
            .uppercase()
        zipCodeInput = ZipCodeInput(filtered, false)
        sdk.callbacks.onZipCodeChange(filtered, true)
    },
    keyboardType = KeyboardType.Text,
    capitalization = KeyboardCapitalization.Characters,
)
```

## Bank Account Fields

The same SPL text field pattern works for ACH bank account tokenization. Use `FormFieldType.ROUTING_NUMBER` and `FormFieldType.ACCOUNT_NUMBER` for the secure fields, and standard text fields or `sdk.bankAccountCallbacks` for name entry.

```kotlin
// Routing number (secure SPL field)
SPLTextField(
    formFieldType = FormFieldType.ROUTING_NUMBER(required = true),
    label = "Routing Number",
    value = sdk.bankAccountState.value.routingNumber.value,
    onChange = { sdk.bankAccountCallbacks.onRoutingNumberChange(it, true) },
)

// Account number (secure SPL field -- auto-clears when backgrounded)
SPLTextField(
    formFieldType = FormFieldType.ACCOUNT_NUMBER(required = true),
    label = "Account Number",
    value = sdk.bankAccountState.value.accountNumber.value,
    onChange = { sdk.bankAccountCallbacks.onAccountNumberChange(it, true) },
)
```

Call `sdk.resetBankAccountState()` when the custom ACH form leaves composition (navigation back, dismiss), unless the Activity is changing configurations — then leave in-memory SDK state as-is. Do **not** call `preserveBankAccountStateOnNextShow()` from custom layouts; that latch is only for the drop-in bottom sheet open path. Prefer `BankAccountSheet(sdk, …)` from [ach-bank-account.md](ach-bank-account.md) when you want leave-clear built in.

### Bank Account Submission

Use `sdk.createBankAccount()` instead of `sdk.createCreditCard()`:

```kotlin
val result = sdk.createBankAccount(
    formFields = listOf(
        FormFieldType.ROUTING_NUMBER(required = true),
        FormFieldType.ACCOUNT_NUMBER(required = true),
    ),
    additionalFields = mapOf(
        AdditionalField.FULL_NAME to nameInput.value,
    ),
)
```

For the full ACH integration guide (including pre-built bottom sheet and field configuration), see [ach-bank-account.md](ach-bank-account.md).

## Combined Expiry Validation

When using separate `FormFieldType.MONTH` and `FormFieldType.YEAR` fields, you need `ExpiryValidationUtils` to validate that the month/year combination is not expired.

```kotlin
import com.spreedly.hostedfields.utils.ExpiryValidationUtils
import com.spreedly.hostedfields.utils.getDisplayValue
```

### Validation Logic

`ExpiryValidationUtils` performs these checks in order:

1. **Individual validation** — month is 01-12, year is a valid YY or YYYY format
2. **Year conversion** — two-digit years are treated as 20XX (e.g., 25 becomes 2025, 99 becomes 2099)
3. **Expiry check** — the month/year combination is not in the past
4. **SDK parameters** — honors `allowExpiredDate` and `isExpiredDateValidationEnabled` settings

### API

```kotlin
ExpiryValidationUtils.isValidCombinedExpiry(month: String, year: String): Boolean
ExpiryValidationUtils.areValidIndividualAndCombinedExpiry(month: String, year: String): Boolean
ExpiryValidationUtils.getExpiryValidationDescription(month: String, year: String): String
ExpiryValidationUtils.isValidMonth(month: String): Boolean
ExpiryValidationUtils.isValidYear(year: String): Boolean
```

### Real-time Validation

```kotlin
@Composable
fun ExpiryFields(
    monthValue: String,
    yearValue: String,
    onMonthChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
) {
    var showExpiryError by remember { mutableStateOf(false) }

    SPLTextField(
        formFieldType = FormFieldType.MONTH(),
        label = "Month",
        value = monthValue,
        onChange = onMonthChange,
    )

    SPLTextField(
        formFieldType = FormFieldType.YEAR(),
        label = "Year",
        value = yearValue,
        onChange = onYearChange,
    )

    LaunchedEffect(monthValue, yearValue) {
        if (monthValue.isNotEmpty() && yearValue.isNotEmpty()) {
            val monthPlain = getDisplayValue(monthValue, FormFieldType.MONTH())
            val yearPlain = getDisplayValue(yearValue, FormFieldType.YEAR())
            showExpiryError = !ExpiryValidationUtils.isValidCombinedExpiry(monthPlain, yearPlain)
        }
    }

    if (showExpiryError) {
        val monthPlain = getDisplayValue(monthValue, FormFieldType.MONTH())
        val yearPlain = getDisplayValue(yearValue, FormFieldType.YEAR())
        Text(
            text = ExpiryValidationUtils.getExpiryValidationDescription(monthPlain, yearPlain),
            color = MaterialTheme.colorScheme.error,
        )
    }
}
```

Both YY and YYYY year formats are accepted:

```kotlin
ExpiryValidationUtils.isValidCombinedExpiry("12", "25")    // December 2025
ExpiryValidationUtils.isValidCombinedExpiry("06", "2030")  // June 2030
```

### SDK Validation Parameters

Combined validation respects global SDK settings:

```kotlin
SpreedlyParamsManager.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, true)
ExpiryValidationUtils.isValidCombinedExpiry("01", "20") // true — expired dates allowed

SpreedlyParamsManager.setParam(ValidationParameter.ALLOW_BLANK_DATE, true)
ExpiryValidationUtils.isValidCombinedExpiry("", "") // true — blank dates allowed
```

## Payment Submission

Use `sdk.createCreditCard()` or `sdk.createPaymentMethod()` to submit the form. Both are suspend functions and must be called from a coroutine scope. Prefer `createCreditCard()` for checkout flows that need `PaymentProcessingResult` and payment-result events; use `createPaymentMethod()` when you already work with `Result<PaymentMethodResponse, SpreedlyNetworkError>`.

### Submission validation (`formFields` + mounted catalogue)

The `formFields` argument is the **merchant-declared** manifest. For **card** tokenize, when `formFields` is non-empty **or** catalogue fields are mounted in `SPLTextField` (with `sdk`), **`createCreditCard()` and `createPaymentMethod()`** validate merchant `formFields` **plus** mounted catalogue types not already listed — see [Hosted-wins merge](#hosted-wins-merge-at-tokenize). **`createBankAccount()` validates only the list you pass** (no mounted submit union). When both card lists are empty, `createPaymentMethod()` keeps legacy behavior (no client-side field validation union). Payment sheet / Express checkout builds `formFields` via `getFormFields()` (card, expiry, name mode, configured optional fields).

| Rule | Behavior |
|------|----------|
| **Required** (`required = true`) | Always validated before tokenization. |
| **Optional encrypted** (`CARD`, `CVV`, `ACCOUNT_NUMBER` with `required = false`) | Validated only when the SDK has **non-blank stored ciphertext** for that field. Corrupt or invalid ciphertext returns validation failure and does **not** tokenize with empty decrypted values. |
| **Optional non-encrypted** (for example `NAME`, `ZIP`, address fields with `required = false`) | **Not** submission-gated by `createCreditCard()` / `createBankAccount()` / gated `createPaymentMethod()` — same as pre–field-encryption-hardening behavior. |
| **Mounted catalogue** (not in your `formFields` list) | **Card tokenize only** — unioned into submit validation for `createCreditCard()` / gated `createPaymentMethod()`; **not** `createBankAccount()`. **Mounted required** types block card submit when invalid/empty. |
| **Invalid expiry manifest** | Mutually exclusive expiry types (for example `EXPIRY_DATE` + `MONTH`) fail before tokenize on both `createCreditCard()` and gated `createPaymentMethod()`. |

`createCreditCard()` returns `PaymentProcessingResult.ValidationFailed` with `invalidFields` when submit validation fails. Gated `createPaymentMethod()` returns `Result.Error` with `SpreedlyApiErrorDetail` (422-style validation detail). Both paths emit `ValidationFailed` telemetry when gating runs.

**Pre-submit UI vs submit:** `areAllFieldsValid(formFields)` runs validation on **every listed field only** (not mounted-only fields). That can disable a Pay button even when `createCreditCard()` would proceed. With registered custom validators that depend on other fields (for example zip rules keyed off country), call `runCustomValidationBeforeSubmit(formFields, additionalFields)` **before** `areAllFieldsValid` on Pay so sticky failures re-evaluate when dependencies change — see [Custom additional-field validators](#custom-additional-field-validators). For optional non-encrypted fields in `formFields`, prefer field-level `HostedFieldState.isValid` / `onValidationChange` for UI, and treat `createCreditCard()` / gated `createPaymentMethod()` as the authoritative submit gate. See [ACH Bank Account](ach-bank-account.md) for the same `formFields` pattern on ACH.

### Basic Submission

Prefer **`additionalFields`** for billing/name/address wire keys (primary path). Use `metadata` only for non-payment merchant data. The sample below uses legacy `address1` / city / state / zip; for the ten canonical keys (`house_number_or_name`, `street`, …) see [Canonical hosted-fields wire keys](#canonical-hosted-fields-wire-keys).

When you register custom validators with `addValidation`, validators run automatically inside `createCreditCard` — no separate preflight call is required. `CheckoutButton` calls `createCreditCard` directly; a custom-validator failure returns `PaymentProcessingResult.ValidationFailed` and the button re-enables. If you build your own Pay button and want inline errors refreshed on all fields before tokenize, call `runCustomValidationBeforeSubmit` first and abort if it returns `true`.

```kotlin
import com.spreedly.sdk.AdditionalField

val coroutineScope = rememberCoroutineScope()

val formFields =
    listOf(
        FormFieldType.CARD(true),
        FormFieldType.EXPIRY_DATE(true),
        FormFieldType.CVV(true),
    )

Button(
    onClick = {
        val additionalFields =
            mapOf(
                AdditionalField.FULL_NAME to nameInput.value,
                AdditionalField.ADDRESS_LINE_1 to addressInput.value,
                AdditionalField.CITY to cityInput.value,
                AdditionalField.STATE to stateInput.value,
                AdditionalField.ZIP_CODE to zipInput.value,
                // Optional canonical keys (coexist with legacy address1 when both set):
                // AdditionalField.HOUSE_NUMBER_OR_NAME to houseInput.value,
                // AdditionalField.STREET to streetInput.value,
            )
        if (sdk.runCustomValidationBeforeSubmit(formFields, additionalFields)) return@Button
        if (!sdk.areAllFieldsValid(formFields)) return@Button
        coroutineScope.launch {
            val result =
                sdk.createCreditCard(
                    formFields = formFields,
                    additionalFields = additionalFields,
                    metadata = mapOf(
                        "order_id" to orderId,
                    ),
                    // Optional. Forwarded verbatim to Spreedly at `payment_method.mandate`;
                    // omitted from the request when null or empty. Nested objects, arrays,
                    // numbers, and booleans are preserved. Spreedly owns the schema and
                    // validates it: `source_version` is "1.0", and rules are generated
                    // server-side from `raw_mandate` -- do not send a `rules` array.
                    mandate = mapOf(
                        "source" to "acp",
                        "source_version" to "1.0",
                        "valid_from" to "2026-07-21T00:00:00Z",
                        "valid_until" to "2026-08-21T00:00:00Z",
                        "raw_mandate" to mapOf(
                            "reason" to "one_time",
                            "max_amount" to 5000,
                            "currency" to "usd",
                            "checkout_session_id" to "cs_test_abc123",
                            "merchant_id" to "merch_abc123",
                            "expires_at" to "2026-08-21T00:00:00Z",
                        ),
                    ),
                )
            when (result) {
                is PaymentProcessingResult.Processing -> {
                    isProcessing = true
                }
                is PaymentProcessingResult.ValidationFailed -> {
                    // Handle validation errors
                }
            }
        }
    },
    // Keep Pay enabled; gate in onClick via preflight + areAllFieldsValid (see Pay enablement above).
    enabled = !isInitializing && !isProcessing,
) {
    Text(
        text = when {
            isInitializing -> "Initializing..."
            isProcessing -> "Processing Payment..."
            !isCustomFormValid() -> "Complete All Fields"
            else -> "Pay Now"
        },
    )
}
```

### Combined Form Validation

Gate the submit button on both SPL and custom field validity:

```kotlin
fun isCustomFormValid(): Boolean =
    nameInput.isValid &&
        addressInput.isValid &&
        cityInput.isValid &&
        stateInput.isValid &&
        zipCodeInput.isValid
```

When using separate month/year fields, add the combined check:

```kotlin
fun validateForm(): Boolean {
    val monthPlain = getDisplayValue(monthValue, FormFieldType.MONTH())
    val yearPlain = getDisplayValue(yearValue, FormFieldType.YEAR())
    val isExpiryValid = ExpiryValidationUtils.isValidCombinedExpiry(monthPlain, yearPlain)

    return isCustomFormValid() && isExpiryValid
}
```

## Error Handling

### Error Message Helpers

Create a helper function per field type for consistent messaging:

```kotlin
fun getNameErrorMessage(error: NameError?): String? =
    when (error) {
        NameError.Empty -> "Name is required"
        NameError.TooShort -> "Name must be at least 2 characters"
        NameError.InvalidCharacters ->
            "Name can only contain letters, spaces, hyphens, and apostrophes"
        null -> null
    }

// This maps the merchant-owned custom ZipCodeError from the example above, not the SDK's
// built-in com.spreedly.validation.validators.ZipCodeError. The SDK's own StandardZipCodeValidator
// doesn't enforce a format (see note under Field Character Limits) — this InvalidFormat case only
// exists because the custom validator above chooses to enforce one.
fun getZipCodeErrorMessage(error: ZipCodeError?): String? =
    when (error) {
        ZipCodeError.Empty -> "Zip code is required"
        ZipCodeError.InvalidFormat -> "Enter a valid postal code"
        ZipCodeError.TooLong -> "Postal code is too long (max 15 characters)"
        null -> null
    }
```

### Handling Submission Failures

Use `PaymentResult.Failed.fromThrowable()` to wrap exceptions into the SDK's result type:

```kotlin
coroutineScope.launch {
    try {
        val result = sdk.createCreditCard(
            formFields = listOf(
                FormFieldType.CARD(true),
                FormFieldType.EXPIRY_DATE(true),
                FormFieldType.CVV(true),
            ),
            metadata = formState.toMetadata(),
        )
    } catch (e: Exception) {
        val failedResult = PaymentResult.Failed.fromThrowable(e)
        // Surface failedResult to the UI
    }
}
```

For the full error handling strategy, see [error-handling.md](error-handling.md).

## Complete Examples

### ViewModel-Driven Form

Manage all form state and validation in a `ViewModel`, keeping the composable layer thin:

```kotlin
class CustomFormViewModel : ViewModel() {
    private val _formState = MutableStateFlow(CustomFormState())
    val formState: StateFlow<CustomFormState> = _formState.asStateFlow()

    fun updateName(name: String) {
        _formState.update { it.copy(name = NameInput(name, false)) }
    }

    fun updateEmail(email: String) {
        _formState.update { it.copy(email = EmailInput(email, false)) }
    }

    fun isFormValid(): Boolean = _formState.value.isValid

    fun submitForm(sdk: Spreedly): Flow<PaymentResult> = flow {
        if (!isFormValid()) {
            emit(PaymentResult.Failed.fromThrowable(IllegalStateException("Form is not valid")))
            return@flow
        }

        val formFields =
            listOf(
                FormFieldType.CARD(true),
                FormFieldType.EXPIRY_DATE(true),
                FormFieldType.CVV(true),
            )
        val additionalFields = _formState.value.toAdditionalFields()
        if (sdk.runCustomValidationBeforeSubmit(formFields, additionalFields)) {
            emit(
                PaymentResult.Failed(
                    errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                    message = "Payment form validation failed",
                    originalError = null,
                ),
            )
            return@flow
        }

        sdk.createCreditCard(
            formFields = formFields,
            metadata = _formState.value.toMetadata(),
            additionalFields = additionalFields,
        )
    }
}

data class CustomFormState(
    val name: NameInput = NameInput(""),
    val email: EmailInput = EmailInput(""),
    val address: AddressInput = AddressInput(""),
) : Form {
    override val inputs = listOf(name, email, address)

    fun toMetadata(): Map<String, String> = mapOf(
        "customer_name" to name.value,
        "customer_email" to email.value,
        "billing_address" to address.value,
    )

    fun toAdditionalFields(): Map<AdditionalField, String> =
        mapOf(
            AdditionalField.EMAIL to email.value,
            AdditionalField.ADDRESS_LINE_1 to address.value,
        )
}
```

### Multi-Step Form

Split collection across steps while keeping a single payment submission:

```kotlin
@Composable
fun MultiStepCustomForm(sdk: Spreedly) {
    var currentStep by remember { mutableStateOf(1) }
    var personalInfo by remember { mutableStateOf(PersonalInfoState()) }
    var addressInfo by remember { mutableStateOf(AddressInfoState()) }

    when (currentStep) {
        1 -> PersonalInfoStep(
            state = personalInfo,
            onUpdate = { personalInfo = it },
            onNext = { currentStep = 2 },
        )
        2 -> AddressInfoStep(
            state = addressInfo,
            onUpdate = { addressInfo = it },
            onBack = { currentStep = 1 },
            onNext = { currentStep = 3 },
        )
        3 -> PaymentStep(
            personalInfo = personalInfo,
            addressInfo = addressInfo,
            sdk = sdk,
            onBack = { currentStep = 2 },
        )
    }
}

data class PersonalInfoState(
    val firstName: NameInput = NameInput(""),
    val lastName: NameInput = NameInput(""),
    val email: EmailInput = EmailInput(""),
    val phone: PhoneInput = PhoneInput(""),
) : Form {
    override val inputs = listOf(firstName, lastName, email, phone)
}
```

### Testing Validation Logic

```kotlin
class NameInputTest {
    @Test
    fun `should reject empty name`() {
        val input = NameInput("")
        assertFalse(input.isValid)
        assertEquals(NameError.Empty, input.error)
    }

    @Test
    fun `should reject name with numbers`() {
        val input = NameInput("John123")
        assertFalse(input.isValid)
        assertEquals(NameError.InvalidCharacters, input.error)
    }

    @Test
    fun `should accept name with hyphens and apostrophes`() {
        val input = NameInput("John O'Connor-Smith")
        assertTrue(input.isValid)
        assertNull(input.error)
    }
}
```
