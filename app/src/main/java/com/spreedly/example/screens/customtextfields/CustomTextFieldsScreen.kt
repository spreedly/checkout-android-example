package com.spreedly.example.screens.customtextfields

/**
 * CustomTextFieldsScreen demonstrates how to integrate custom text fields with Spreedly's secure SPL fields.
 *
 * This example shows a hybrid approach where:
 * - SPLTextField (hosted fields) handle card data and additional fields registered with sdk.addValidation()
 * - CustomTextField handles merchant-owned inputs with your own validation (FormInput pattern)
 * - A custom payment button validates both field types before createCreditCard
 *
 * Key features:
 * - Real-time validation for custom fields using the FormInput pattern
 * - sdk.addValidation() on hosted SPLTextField optional fields (inline errors on blur)
 * - Custom validator failures surface as a ValidationFailed snackbar via createCreditCard,
 *   not through a pre-submit preflight call; inline errors are set on blur only
 * - Core name fields (Full Name) are not exposed in the field configuration card; custom
 *   name validators are not registered from this screen
 * - SDK callbacks keep field values in sync with payment state
 * - Combined validation before payment processing: areAllFieldsValid gates the button,
 *   createCreditCard handles custom validation internally and returns ValidationFailed
 */

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spreedly.example.ui.components.CardBrandTrailingIcon
import com.spreedly.example.ui.components.FieldStyleOverrideCard
import com.spreedly.example.ui.components.RetokenizeCard
import com.spreedly.example.ui.components.ThemeConfigurationCard
import com.spreedly.example.ui.components.ThemeConfigurationStyle
import com.spreedly.example.MerchantMaskToggleBar
import com.spreedly.example.optionalfields.DemoOptionalFieldConfigCard
import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.example.optionalfields.DemoOptionalHostedFieldsSection
import com.spreedly.example.qa.FieldStateInspectorCard
import com.spreedly.example.qa.HeadlessHostedFieldsConfigCard
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.validation.DemoCoreNameFieldLayout
import com.spreedly.example.validation.DemoExpiryManifestTest
import com.spreedly.example.viewmodel.customTextFieldsViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.security.secureScreen
import com.spreedly.validation.formx.FormInput
import kotlinx.coroutines.launch

/**
 * Custom form input for name validation.
 *
 * Validates that the name contains only valid characters and meets minimum length requirements.
 * This demonstrates how to create custom validation logic that integrates with the FormInput pattern.
 *
 * @param value The current name value
 * @param isPristine Whether the field has been modified by the user
 */
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

/**
 * Validation errors for name input.
 */
enum class NameError {
    /** Name field is empty */
    Empty,

    /** Name is shorter than minimum required length */
    TooShort,

    /** Name contains invalid characters */
    InvalidCharacters,
}

/**
 * Custom form input for address validation.
 *
 * Validates address format with minimum length requirements.
 *
 * @param value The current address value
 * @param isPristine Whether the field has been modified by the user
 */
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

enum class AddressError {
    Empty,
    TooShort,
}

// Custom form input for city validation
class CityInput(
    value: String,
    isPristine: Boolean = true,
) : FormInput<String, CityError>(value, isPristine) {
    override fun validator(value: String): CityError? =
        when {
            value.isBlank() -> CityError.Empty
            value.length < 2 -> CityError.TooShort
            !value.matches(Regex("^[a-zA-Z\\s'-]+$")) -> CityError.InvalidCharacters
            else -> null
        }
}

enum class CityError {
    Empty,
    TooShort,
    InvalidCharacters,
}

/**
 * A reusable custom text field component that integrates with the validation system.
 *
 * This component demonstrates how to create custom input fields that:
 * - Provide consistent styling and behavior
 * - Support validation error display
 * - Integrate with different keyboard types and input modes
 * - Can be customized for specific use cases
 *
 * @param label The field label to display
 * @param value The current field value
 * @param onValueChange Callback when the value changes
 * @param modifier Modifier for styling the component
 * @param isError Whether the field is in an error state
 * @param errorMessage The error message to display (if any)
 * @param keyboardType The keyboard type to show
 * @param capitalization The text capitalization mode
 */
@Composable
fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    imeAction: ImeAction = ImeAction.Default,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Column(modifier = modifier) {
        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (isRequired) {
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp),
                ).border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                ).padding(12.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                onDone = { focusManager.clearFocus() },
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter $label",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Main screen demonstrating custom text fields integration with Spreedly SDK.
 *
 * This screen showcases a hybrid approach where:
 * 1. Card payment information uses secure SPLTextField hosted fields
 * 2. Country, state, and zip use SPLTextField with sdk.addValidation() custom rules
 * 3. Name, address, and city use CustomTextField with independent FormInput validation
 * 4. A custom payment button handles validation and processing
 *
 * Key implementation details:
 * - Custom validation classes extend FormInput for merchant-owned fields
 * - sdk.addValidation() applies only to hosted SPLTextField fields (pass sdk = sdk)
 * - SDK callbacks sync field values with payment state
 * - Combined validation ensures SPL and custom fields are valid before payment
 */
@Composable
@SuppressLint("ComposeModifierMissing")
fun CustomTextFieldsScreen(
    viewModel: CustomTextFieldsViewModel = customTextFieldsViewModel(),
) {
    val sdk = viewModel.sdk
    val hostedCardDisplayState by sdk.hostedCardDisplayState
    val inspectorUiState by viewModel.inspectorUiState.collectAsStateWithLifecycle()
    var enableAutofill by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(hostedCardDisplayState) {
        viewModel.fieldStateInspector.refreshMismatch(hostedCardDisplayState)
    }
    val snackbarHostState = viewModel.snackbarHostState
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val paymentToken by viewModel.paymentToken.collectAsStateWithLifecycle()

    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    // Custom form field states - managed by ViewModel to survive configuration changes
    // These are separate from SDK state to demonstrate independent validation logic
    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val addressInput by viewModel.addressInput.collectAsStateWithLifecycle()
    val cityInput by viewModel.cityInput.collectAsStateWithLifecycle()
    val fieldSelection by viewModel.fieldSelection.collectAsStateWithLifecycle()
    val demoInvalidExpiryManifest by viewModel.demoInvalidExpiryManifest.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val useCustomTheme by viewModel.useCustomTheme.collectAsStateWithLifecycle()
    val selectedThemePreset by viewModel.selectedThemePreset.collectAsStateWithLifecycle()
    val fieldOverrideTarget by viewModel.fieldOverrideTarget.collectAsStateWithLifecycle()
    val fieldStyleOverrides by viewModel.fieldStyleOverrides.collectAsStateWithLifecycle()
    val isDarkMode = isSystemInDarkTheme()

    LaunchedEffect(useCustomTheme, selectedThemePreset, isDarkMode) {
        viewModel.applyThemeToSdk(isDarkMode)
    }

    val cardFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.CARD(true), isDarkMode)
        }
    val expiryFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.EXPIRY_DATE(true), isDarkMode)
        }
    val monthFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.MONTH(true), isDarkMode)
        }
    val yearFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.YEAR(true), isDarkMode)
        }
    val cvvFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.CVV(true), isDarkMode)
        }

    val formFields =
        remember(fieldSelection, demoInvalidExpiryManifest) {
            fieldSelection.toHostedFormFields(
                includeCoreCardFields = true,
                demoInvalidExpiryManifest = demoInvalidExpiryManifest,
            )
        }

    // SDK initialization and payment result handling is now managed by ViewModel

    fun getNameErrorMessage(error: NameError?): String? =
        when (error) {
            NameError.Empty -> "Name is required"
            NameError.TooShort -> "Name must be at least 2 characters"
            NameError.InvalidCharacters -> "Name can only contain letters, spaces, hyphens, and apostrophes"
            null -> null
        }

    fun getAddressErrorMessage(error: AddressError?): String? =
        when (error) {
            AddressError.Empty -> "Address is required"
            AddressError.TooShort -> "Address must be at least 5 characters"
            null -> null
        }

    fun getCityErrorMessage(error: CityError?): String? =
        when (error) {
            CityError.Empty -> "City is required"
            CityError.TooShort -> "City must be at least 2 characters"
            CityError.InvalidCharacters -> "City can only contain letters, spaces, hyphens, and apostrophes"
            null -> null
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .secureScreen() // Apply security to prevent screenshots
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section
            Card(
                modifier =
                    Modifier
                        .size(80.dp)
                        .padding(bottom = 24.dp),
                shape = CircleShape,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Custom Text Fields Example",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(
                text = "Custom Text Fields Example",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "SPL hosted fields for card data, custom fields for personal info",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 48.dp),
            )

            ThemeConfigurationCard(
                useCustomTheme = useCustomTheme,
                selectedPreset = selectedThemePreset,
                onUseCustomThemeChange = viewModel::setUseCustomTheme,
                onPresetSelected = viewModel::setThemePreset,
                onResetTheme = viewModel::resetThemeConfiguration,
                style = ThemeConfigurationStyle.SWATCH,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
            )

            FieldStyleOverrideCard(
                selectedTarget = fieldOverrideTarget,
                overrides = fieldStyleOverrides,
                onTargetSelected = viewModel::setFieldOverrideTarget,
                onOverridesChange = viewModel::updateFieldStyleOverrides,
                onClearOverrides = viewModel::clearFieldStyleOverrides,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
            )

            // Empty layout: this screen never exposes core name fields in the config card
            // and never registers core name custom validators.
            DemoOptionalFieldConfigCard(
                selection = fieldSelection,
                onSelectionChange = viewModel::updateFieldSelection,
                availableCoreNameFields = DemoCoreNameFieldLayout(emptyList()),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
            )

            // Main Card
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Card Payment Section
                    Text(
                        text = "Card Information",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )

                    MerchantMaskToggleBar(
                        sdk = sdk,
                        hostedCardDisplayState = hostedCardDisplayState,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    HeadlessHostedFieldsConfigCard(
                        enableAutofill = enableAutofill,
                        onEnableAutofillChange = { enableAutofill = it },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DemoExpiryManifestTest.SWITCH_LABEL,
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                )
                                Text(
                                    text = DemoExpiryManifestTest.SWITCH_DESCRIPTION,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                modifier = Modifier.testTag(DemoExpiryManifestTest.SWITCH_TEST_TAG),
                                checked = demoInvalidExpiryManifest,
                                onCheckedChange = viewModel::updateDemoInvalidExpiryManifest,
                            )
                        }
                    }

                    // Card hosted fields — SPLTextField for PCI-sensitive data
                    key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                        SPLTextField(
                            label = "Card Number",
                            formFieldType = FormFieldType.CARD(true),
                            config = cardFieldConfig,
                            value = sdk.paymentState.value.cardNumber.value,
                            onChange = {
                                sdk.callbacks.onCardNumberChange(it, true)
                                viewModel.fieldStateInspector.logOpaqueFieldChange(FormFieldType.CARD(true))
                            },
                            trailingIcon = { scheme -> CardBrandTrailingIcon(scheme) },
                            onFieldStateChange = { viewModel.onFieldStateUpdate(it) },
                            onValidationChange = {
                                viewModel.onHostedFieldValidation(FormFieldType.CARD(true), it)
                            },
                            enableAutofill = enableAutofill,
                            sdk = sdk,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (demoInvalidExpiryManifest) {
                        key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                            SPLTextField(
                                label = "MM/YY",
                                formFieldType = FormFieldType.EXPIRY_DATE(true),
                                config = expiryFieldConfig,
                                value = sdk.paymentState.value.expirationDate.value,
                                onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                                onValidationChange = {
                                    viewModel.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), it)
                                },
                                imeAction = ImeAction.Next,
                                sdk = sdk,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                                SPLTextField(
                                    label = "Month",
                                    formFieldType = FormFieldType.MONTH(true),
                                    config = monthFieldConfig,
                                    value = sdk.paymentState.value.month.value,
                                    onChange = { sdk.callbacks.onMonthChange(it, true) },
                                    onValidationChange = {
                                        viewModel.onHostedFieldValidation(FormFieldType.MONTH(true), it)
                                    },
                                    modifier = Modifier.weight(1f),
                                    imeAction = ImeAction.Next,
                                    sdk = sdk,
                                )
                            }

                            key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                                SPLTextField(
                                    label = "Year (YYYY)",
                                    formFieldType = FormFieldType.YEAR(true),
                                    config = yearFieldConfig,
                                    value = sdk.paymentState.value.year.value,
                                    onChange = { sdk.callbacks.onYearChange(it, true) },
                                    onValidationChange = {
                                        viewModel.onHostedFieldValidation(FormFieldType.YEAR(true), it)
                                    },
                                    modifier = Modifier.weight(1f),
                                    imeAction = ImeAction.Next,
                                    sdk = sdk,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                            SPLTextField(
                                label = "Security Code (CVC)",
                                formFieldType = FormFieldType.CVV(true),
                                config = cvvFieldConfig,
                                value = sdk.paymentState.value.securityCode.value,
                                onChange = {
                                    sdk.callbacks.onSecurityCodeChange(it, true)
                                    viewModel.fieldStateInspector.logOpaqueFieldChange(FormFieldType.CVV(true))
                                },
                                onFieldStateChange = { viewModel.onFieldStateUpdate(it) },
                                onValidationChange = {
                                    viewModel.onHostedFieldValidation(FormFieldType.CVV(true), it)
                                },
                                imeAction = ImeAction.Next,
                                enableAutofill = enableAutofill,
                                sdk = sdk,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                                SPLTextField(
                                    label = "MM/YY",
                                    formFieldType = FormFieldType.EXPIRY_DATE(true),
                                    config = expiryFieldConfig,
                                    value = sdk.paymentState.value.expirationDate.value,
                                    onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                                    onValidationChange = {
                                        viewModel.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), it)
                                    },
                                    modifier = Modifier.weight(1f),
                                    imeAction = ImeAction.Next,
                                    sdk = sdk,
                                )
                            }

                            key(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                                SPLTextField(
                                    label = "Security Code (CVC)",
                                    formFieldType = FormFieldType.CVV(true),
                                    config = cvvFieldConfig,
                                    value = sdk.paymentState.value.securityCode.value,
                                    onChange = {
                                        sdk.callbacks.onSecurityCodeChange(it, true)
                                        viewModel.fieldStateInspector.logOpaqueFieldChange(FormFieldType.CVV(true))
                                    },
                                    onFieldStateChange = { viewModel.onFieldStateUpdate(it) },
                                    onValidationChange = {
                                        viewModel.onHostedFieldValidation(FormFieldType.CVV(true), it)
                                    },
                                    modifier = Modifier.weight(1f),
                                    imeAction = ImeAction.Next,
                                    enableAutofill = enableAutofill,
                                    sdk = sdk,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Personal Information Section — merchant-owned CustomTextField inputs
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )

                    // Custom Name Field — FormInput validation with SDK callback sync
                    CustomTextField(
                        label = "Full Name",
                        value = nameInput.value,
                        onValueChange = { value ->
                            viewModel.updateNameInput(value)
                            sdk.callbacks.onNameOnCardChange(value, true)
                        },
                        isRequired = true,
                        isError = nameInput.isNotValid && !nameInput.isPristine,
                        errorMessage = if (!nameInput.isPristine) getNameErrorMessage(nameInput.error) else null,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Address Field — merchant-owned validation; sync value via SDK callback
                    CustomTextField(
                        label = "Address Line 1",
                        value = addressInput.value,
                        onValueChange = { value ->
                            viewModel.updateAddressInput(value)
                            sdk.callbacks.onAddressLine1Change(value, true)
                        },
                        isRequired = true,
                        isError = addressInput.isNotValid && !addressInput.isPristine,
                        errorMessage = if (!addressInput.isPristine) getAddressErrorMessage(addressInput.error) else null,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom City Field — merchant-owned validation; sync value via SDK callback
                    CustomTextField(
                        label = "City",
                        value = cityInput.value,
                        onValueChange = { value ->
                            viewModel.updateCityInput(value)
                            sdk.callbacks.onCityChange(value, true)
                        },
                        isRequired = true,
                        isError = cityInput.isNotValid && !cityInput.isPristine,
                        errorMessage = if (!cityInput.isPristine) getCityErrorMessage(cityInput.error) else null,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    key(fieldSelection, useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides) {
                        DemoOptionalHostedFieldsSection(
                            sdk = sdk,
                            selection = fieldSelection,
                            fieldConfigFor = { formFieldType ->
                                viewModel.resolveSplFieldConfig(formFieldType, isDarkMode)
                            },
                            onValidationChange = viewModel::onHostedFieldValidation,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Payment Method Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shouldRetainPaymentMethod = !shouldRetainPaymentMethod }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = shouldRetainPaymentMethod,
                            onCheckedChange = { shouldRetainPaymentMethod = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save payment information for future use",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // isFormValid = core fields valid && required optional hosted fields valid.
                    val isButtonDisabled = isInitializing || isProcessing || !isFormValid

                    // Custom Payment Button submit flow:
                    // 1. areAllFieldsValid — built-in hosted-field gate (+ sticky custom blur latch)
                    // 2. createCreditCard — custom validators run internally; ValidationFailed
                    //    is returned when any registered validator rejects the field value
                    Button(
                        onClick = {
                            viewModel.beginCheckoutAttempt()
                            val additionalFields = viewModel.buildSubmitAdditionalFields()
                            if (
                                // Demo-only: FA10 invalid manifest must reach SDK submit validator.
                                !viewModel.skipPreSubmitFieldValidityGateForDemo() &&
                                !sdk.areAllFieldsValid(formFields)
                            ) {
                                return@Button
                            }

                            coroutineScope.launch {
                                if (!viewModel.ensureFreshAuthBeforePayment()) {
                                    return@launch
                                }

                                val result = sdk.createCreditCard(
                                    formFields = formFields,
                                    additionalFields = additionalFields,
                                    metadata = mapOf(
                                        "example_type" to "custom_text_fields_new_pattern",
                                        "order_id" to "ORD-${System.currentTimeMillis()}",
                                        "amount" to "49.99",
                                        "currency" to "USD",
                                    ),
                                    retainOnSuccess = shouldRetainPaymentMethod,
                                )
                                when (result) {
                                    is PaymentProcessingResult.Processing -> {
                                        viewModel.onPaymentSubmitResult(result)
                                        viewModel.resetFormFields()
                                        viewModel.onCheckoutFieldsClearedBySdk()
                                        viewModel.setProcessing(true)
                                        viewModel.startPaymentPolling()
                                    }

                                    is PaymentProcessingResult.ValidationFailed -> {
                                        viewModel.resolveValidationFailedSnackbar(result)?.let { message ->
                                            viewModel.snackbarHostState.showSnackbar(message)
                                        }
                                    }

                                    is PaymentProcessingResult.Rejected,
                                    is PaymentProcessingResult.Failed,
                                    -> Unit
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isButtonDisabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isInitializing || isProcessing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = when {
                                    isInitializing -> "Initializing..."
                                    isProcessing -> "Processing Payment..."
                                    !isFormValid -> "Complete All Fields"
                                    else -> "Pay Now - Custom Fields"
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    RetokenizeCard(
                        paymentToken = paymentToken,
                        onRetokenize = { viewModel.reinitialize() },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.performFullPaymentReset() },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("custom-text-fields-reset-payment-state-button"),
                    ) {
                        Text("resetPaymentState()")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FieldStateInspectorCard(
                        uiState = inspectorUiState,
                        hostedCardDisplayState = hostedCardDisplayState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Secure",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "SPL secure fields + custom validation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
