# Custom Payment Forms

Build payment forms that combine Spreedly's secure SPL text fields with custom-validated fields for a flexible, PCI-compliant checkout experience.

## Overview

The custom payment forms approach uses a hybrid architecture:

- **SPL Text Fields** handle sensitive card data (card number, expiry date, CVV) with full PCI compliance
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

**`SPLTextField` callbacks:** `onChange` receives **AES-encrypted** ciphertext for `FormFieldType.CARD`, `FormFieldType.CVV`, and `FormFieldType.ACCOUNT_NUMBER` only (`FormFieldType.shouldEncrypt()`); all other field types receive **raw** processed text in `onChange`. Do **not** log encrypted strings or treat them as display digits. Use **`onFieldStateChange(HostedFieldState)?`** for iframe-style observability: digit **counts** (`numberLength` / `cvvLength`), `cardScheme`, `isValid`, focus/blur via `HostedFieldEventType`, without parsing ciphertext. Use `onValidationChange` / `hasValidationError` / submit results for gating checkout. Kotlin/Java samples: [Migration from legacy](migration/from-legacy.md#hostedfieldstate--kotlin-samples-compose).

For initial SDK setup, see [getting-started.md](getting-started.md).

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

Use `SPLTextField` (from the `:hostedfields` module) with `FormFieldType` (from `com.spreedly.sdk.models`) for all sensitive card data. Each field requires a `formFieldType` and an `onChange` callback. **CARD** and **CVV** fields also require the same `sdk: Spreedly` instance used for `setNumberFormat` / `toggleMask` (display follows `sdk.hostedCardDisplayState`).

### Single Expiry Field

```kotlin
@Composable
fun CardFieldsSection(sdk: Spreedly) {
    var expiryValue by remember { mutableStateOf("") }
    var cardValue by remember { mutableStateOf("") }
    var cvvValue by remember { mutableStateOf("") }

    SPLTextField(
        formFieldType = FormFieldType.EXPIRY_DATE(),
        label = "Expiry Date (MM/YY)",
        value = expiryValue,
        onChange = { expiryValue = it },
    )

    SPLTextField(
        formFieldType = FormFieldType.CARD(true),
        sdk = sdk,
        label = "Card Number",
        value = cardValue,
        onChange = { cardValue = it },
    )

    SPLTextField(
        formFieldType = FormFieldType.CVV(true),
        sdk = sdk,
        label = "CVV",
        value = cvvValue,
        onChange = { cvvValue = it },
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

Supports US ZIP codes (12345, 12345-6789) and international formats (Canadian K1A 0B1, UK SW1A 1AA):

```kotlin
class ZipCodeInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, ZipCodeError>(value, isPristine) {
    override fun validator(value: String): ZipCodeError? =
        when {
            value.isBlank() -> ZipCodeError.Empty
            value.length > 10 -> ZipCodeError.TooLong
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

Use `sdk.createCreditCard()` to submit the form. This is a suspend function and must be called from a coroutine scope.

### Basic Submission

```kotlin
val coroutineScope = rememberCoroutineScope()

Button(
    onClick = {
        coroutineScope.launch {
            val result = sdk.createCreditCard(
                formFields = listOf(
                    FormFieldType.CARD(true),
                    FormFieldType.EXPIRY_DATE(true),
                    FormFieldType.CVV(true),
                ),
                metadata = mapOf(
                    "custom_name" to nameInput.value,
                    "custom_address" to addressInput.value,
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
    enabled = !isInitializing && !isProcessing && isCustomFormValid(),
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

fun getZipCodeErrorMessage(error: ZipCodeError?): String? =
    when (error) {
        ZipCodeError.Empty -> "Zip code is required"
        ZipCodeError.InvalidFormat -> "Invalid postal code format"
        ZipCodeError.TooLong -> "Postal code is too long (max 10 characters)"
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

        sdk.createCreditCard(
            formFields = listOf(
                FormFieldType.CARD(true),
                FormFieldType.EXPIRY_DATE(true),
                FormFieldType.CVV(true),
            ),
            metadata = _formState.value.toMetadata(),
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
