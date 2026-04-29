# Focus Management

## Overview

The Spreedly Android SDK provides programmatic focus control for text fields through the `shouldFocus` parameter on `SPLTextField` and `AppTextField`. This lets external frameworks (like React Native) or parent components control which field is focused at any given time.

The parameter defaults to `false`, so existing integrations require no changes.

Key capabilities:

- **Programmatic focus control** from parent components or external frameworks
- **React Native integration** via JavaScript-controlled focus state
- **Dynamic focus management** based on application state or validation
- **Accessibility support** alongside existing accessibility features

## How It Works

`shouldFocus` uses Compose's `FocusRequester` API internally:

- When `shouldFocus` is `true`, the field requests focus
- Focus requests run in a `LaunchedEffect` that observes the `shouldFocus` value
- State changes drive reactive focus transitions

`SPLTextField` passes `shouldFocus` through to `AppTextField`, which owns the `FocusRequester` instance.

## Usage

### Basic Usage

```kotlin
import androidx.compose.runtime.*
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType

@Composable
fun PaymentForm() {
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var focusedField by remember { mutableStateOf<String?>(null) }

    Column {
        SPLTextField(
            formFieldType = FormFieldType.CARD(),
            label = "Card Number",
            value = cardNumber,
            onChange = { cardNumber = it },
            shouldFocus = focusedField == "cardNumber"
        )

        SPLTextField(
            formFieldType = FormFieldType.CVV(),
            label = "CVV",
            value = cvv,
            onChange = { cvv = it },
            shouldFocus = focusedField == "cvv"
        )

        Button(onClick = { focusedField = "cardNumber" }) {
            Text("Focus Card Number")
        }
    }
}
```

### Auto-Focus Next Field

```kotlin
enum class PaymentFieldType {
    FULL_NAME, CARD_NUMBER, EXPIRY_DATE, CVV
}

@Composable
fun PaymentFormWithAutoFocus() {
    var fullName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    var focusedFieldType by remember {
        mutableStateOf<PaymentFieldType?>(PaymentFieldType.FULL_NAME)
    }

    var fullNameIsValid by remember { mutableStateOf(false) }
    var cardNumberIsValid by remember { mutableStateOf(false) }
    var expiryIsValid by remember { mutableStateOf(false) }
    var cvvIsValid by remember { mutableStateOf(false) }

    fun handleFieldSubmit(fieldType: PaymentFieldType) {
        focusedFieldType = when (fieldType) {
            PaymentFieldType.FULL_NAME -> if (fullNameIsValid) PaymentFieldType.CARD_NUMBER else null
            PaymentFieldType.CARD_NUMBER -> if (cardNumberIsValid) PaymentFieldType.EXPIRY_DATE else null
            PaymentFieldType.EXPIRY_DATE -> if (expiryIsValid) PaymentFieldType.CVV else null
            PaymentFieldType.CVV -> null
        }
    }

    fun getSubmitLabel(fieldType: PaymentFieldType): ImeAction {
        return when (fieldType) {
            PaymentFieldType.CVV -> ImeAction.Done
            else -> ImeAction.Next
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        SPLTextField(
            formFieldType = FormFieldType.NAME(),
            label = "Full Name",
            value = fullName,
            onChange = { fullName = it },
            onValidationChange = { valid -> fullNameIsValid = valid },
            onImeAction = { handleFieldSubmit(PaymentFieldType.FULL_NAME) },
            imeAction = getSubmitLabel(PaymentFieldType.FULL_NAME),
            shouldFocus = focusedFieldType == PaymentFieldType.FULL_NAME
        )

        SPLTextField(
            formFieldType = FormFieldType.CARD(),
            label = "Card Number",
            value = cardNumber,
            onChange = { cardNumber = it },
            onValidationChange = { valid -> cardNumberIsValid = valid },
            onImeAction = { handleFieldSubmit(PaymentFieldType.CARD_NUMBER) },
            imeAction = getSubmitLabel(PaymentFieldType.CARD_NUMBER),
            shouldFocus = focusedFieldType == PaymentFieldType.CARD_NUMBER
        )

        SPLTextField(
            formFieldType = FormFieldType.MONTH(),
            label = "Expiry Date",
            value = expiryMonth,
            onChange = { expiryMonth = it },
            onValidationChange = { valid -> expiryIsValid = valid },
            onImeAction = { handleFieldSubmit(PaymentFieldType.EXPIRY_DATE) },
            imeAction = getSubmitLabel(PaymentFieldType.EXPIRY_DATE),
            shouldFocus = focusedFieldType == PaymentFieldType.EXPIRY_DATE
        )

        SPLTextField(
            formFieldType = FormFieldType.CVV(),
            label = "CVV",
            value = cvv,
            onChange = { cvv = it },
            onValidationChange = { valid -> cvvIsValid = valid },
            onImeAction = { handleFieldSubmit(PaymentFieldType.CVV) },
            imeAction = getSubmitLabel(PaymentFieldType.CVV),
            shouldFocus = focusedFieldType == PaymentFieldType.CVV
        )
    }
}
```

### React Native Integration

Expose the `shouldFocus` parameter to JavaScript through a React Native bridge:

```kotlin
@ReactMethod
fun focusField(fieldName: String) {
    composeFocusedField.value = fieldName
}

@Composable
fun ReactNativePaymentForm(
    focusedField: State<String?>
) {
    var cardNumber by remember { mutableStateOf("") }

    SPLTextField(
        formFieldType = FormFieldType.CARD(),
        label = "Card Number",
        value = cardNumber,
        onChange = { cardNumber = it },
        shouldFocus = focusedField.value == "cardNumber"
    )
}
```

### Conditional Focus on Validation Errors

```kotlin
@Composable
fun SmartPaymentForm() {
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    var cardNumberIsValid by remember { mutableStateOf(false) }
    var cvvIsValid by remember { mutableStateOf(false) }

    var focusFirstInvalidField by remember { mutableStateOf(false) }

    val focusedField = remember(focusFirstInvalidField, cardNumberIsValid, cvvIsValid) {
        when {
            focusFirstInvalidField && !cardNumberIsValid -> "cardNumber"
            focusFirstInvalidField && !cvvIsValid -> "cvv"
            else -> null
        }
    }

    Column {
        SPLTextField(
            formFieldType = FormFieldType.CARD(),
            label = "Card Number",
            value = cardNumber,
            onChange = { cardNumber = it },
            onValidationChange = { valid -> cardNumberIsValid = valid },
            shouldFocus = focusedField == "cardNumber"
        )

        SPLTextField(
            formFieldType = FormFieldType.CVV(),
            label = "CVV",
            value = cvv,
            onChange = { cvv = it },
            onValidationChange = { valid -> cvvIsValid = valid },
            shouldFocus = focusedField == "cvv"
        )

        Button(
            onClick = {
                if (!cardNumberIsValid || !cvvIsValid) {
                    focusFirstInvalidField = true
                }
            }
        ) {
            Text("Submit")
        }
    }
}
```

## API Reference

### SPLTextField

```kotlin
@Composable
fun SPLTextField(
    formFieldType: FormFieldType,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    config: CustomFieldsConfig? = null,
    label: String = stringResource(SdkResources.Strings.form_field_card_number_label),
    value: String = "",
    onValidationChange: ((Boolean) -> Unit)? = null,
    shouldFocus: Boolean = false,
    onFocus: (() -> Unit)? = null,
)
```

`formFieldType` and `onChange` are required. `FormFieldType` is located at `com.spreedly.sdk.models.FormFieldType`.

| Parameter | Description |
|---|---|
| `shouldFocus` | When `true`, the field programmatically requests focus. Useful for external focus control from React Native or dynamic focus management. |
| `onFocus` | Callback invoked when the field gains focus (e.g., user taps the field). Use this to track which field is currently active. |

### AppTextField

```kotlin
@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    // ... other parameters ...
    shouldFocus: Boolean = false,
    onFocus: (() -> Unit)? = null,
)
```

`shouldFocus` and `onFocus` behave the same as on `SPLTextField`. `SPLTextField` passes these through to `AppTextField` internally.

### Focus Tracking with onFocus

The `onFocus` callback lets you track when users tap into fields:

```kotlin
@Composable
fun PaymentFormWithTracking() {
    var currentField by remember { mutableStateOf<FormFieldType?>(null) }
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Column {
        SPLTextField(
            formFieldType = FormFieldType.CARD(true),
            label = "Card Number",
            value = cardNumber,
            onChange = { cardNumber = it },
            onFocus = {
                currentField = FormFieldType.CARD(true)
            }
        )

        SPLTextField(
            formFieldType = FormFieldType.CVV(true),
            label = "CVV",
            value = cvv,
            onChange = { cvv = it },
            onFocus = {
                currentField = FormFieldType.CVV(true)
                showCvvHelp()
            }
        )

        Text("Currently editing: ${currentField?.javaClass?.simpleName ?: "None"}")
    }
}
```

### Combining shouldFocus and onFocus

Use both parameters for complete focus control — programmatic focus plus user-tap tracking:

```kotlin
@Composable
fun SmartFocusForm() {
    var focusedField by remember {
        mutableStateOf<FormFieldType?>(FormFieldType.CARD(true))
    }

    SPLTextField(
        formFieldType = FormFieldType.CARD(true),
        onChange = { },
        label = "Card Number",
        shouldFocus = focusedField == FormFieldType.CARD(true),
        onFocus = { focusedField = FormFieldType.CARD(true) },
        onValidationChange = { isValid ->
            if (isValid) focusedField = FormFieldType.CVV(true)
        }
    )

    SPLTextField(
        formFieldType = FormFieldType.CVV(true),
        onChange = { },
        label = "CVV",
        shouldFocus = focusedField == FormFieldType.CVV(true),
        onFocus = { focusedField = FormFieldType.CVV(true) }
    )
}
```

## Best Practices

### Use state to control focus

Always drive `shouldFocus` from a single piece of state:

```kotlin
var focusedField by remember { mutableStateOf<String?>(null) }

SPLTextField(
    formFieldType = FormFieldType.CARD(),
    onChange = { },
    shouldFocus = focusedField == "thisField"
)
```

### Keep one field focused at a time

Only one field should have `shouldFocus = true` at any moment:

```kotlin
// Good — mutual exclusion via state
val focusedField = remember { mutableStateOf<String?>("cardNumber") }

SPLTextField(
    formFieldType = FormFieldType.CARD(),
    onChange = { },
    shouldFocus = focusedField.value == "cardNumber"
)
SPLTextField(
    formFieldType = FormFieldType.CVV(),
    onChange = { },
    shouldFocus = focusedField.value == "cvv"
)
```

### Combine with validation

Use `onValidationChange` to auto-advance focus when a field becomes valid:

```kotlin
SPLTextField(
    formFieldType = FormFieldType.CARD(),
    value = cardNumber,
    onChange = { cardNumber = it },
    onValidationChange = { valid ->
        if (valid && autoAdvance) {
            focusedField = "nextField"
        }
    },
    shouldFocus = focusedField == "cardNumber"
)
```

### Reset focus state

Clear the focus state when submitting or resetting the form:

```kotlin
fun submitForm() {
    if (isValid) {
        focusedField = null
    } else {
        focusedField = "firstInvalidField"
    }
}
```

### Works with IME actions

`shouldFocus` composes naturally with IME actions:

```kotlin
SPLTextField(
    formFieldType = FormFieldType.CARD(),
    value = cardNumber,
    onChange = { cardNumber = it },
    imeAction = ImeAction.Next,
    onImeAction = { focusedField = "nextField" },
    shouldFocus = focusedField == "cardNumber"
)
```

### Works with custom themes

Focus control is independent of theme configuration. See the [Theme and Styling](theme-and-styling.md) guide.

```kotlin
SPLTextField(
    formFieldType = FormFieldType.CARD(),
    onChange = { },
    config = CustomFieldsConfig(
        primaryColor = Color.Blue,
    ),
    shouldFocus = focusedField == "cardNumber"
)
```

## Troubleshooting

### Focus not working

1. **Check state updates** — make sure the focus state is changing:
   ```kotlin
   LaunchedEffect(focusedField) {
       Log.d("FocusDebug", "Focus changed to: $focusedField")
   }
   ```
2. **Verify single focus** — only one field should have `shouldFocus = true`.
3. **Check composition** — the field must be fully composed before it can receive focus.

### Focus jumping between fields

1. **Stabilize state** — use `remember` for focus state:
   ```kotlin
   val focusedField = remember { mutableStateOf<String?>(null) }
   ```
2. **Avoid multiple writers** — don't update focus state from multiple sources simultaneously.

## Migrating from Manual FocusRequester

Before (manual):

```kotlin
val focusRequester = remember { FocusRequester() }

TextField(
    modifier = Modifier.focusRequester(focusRequester)
)

Button(onClick = { focusRequester.requestFocus() })
```

After (using `shouldFocus`):

```kotlin
var shouldFocus by remember { mutableStateOf(false) }

SPLTextField(
    formFieldType = FormFieldType.CARD(),
    onChange = { },
    shouldFocus = shouldFocus
)

Button(onClick = { shouldFocus = true })
```

## Related Documentation

- [Custom Payment Forms](custom-payment-forms.md)
- [Getting Started](getting-started.md)
- [Theme and Styling](theme-and-styling.md)
