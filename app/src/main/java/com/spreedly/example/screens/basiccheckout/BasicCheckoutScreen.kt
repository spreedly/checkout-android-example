package com.spreedly.example.screens.basiccheckout

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.spreedly.example.MerchantMaskToggleBar
import com.spreedly.example.qa.FieldStateInspectorCard
import com.spreedly.example.qa.HeadlessHostedFieldsConfigCard
import com.spreedly.example.ui.components.CardBrandTrailingIcon
import com.spreedly.example.ui.components.FieldStyleOverrideCard
import com.spreedly.example.ui.components.SavedPaymentMethodsList
import com.spreedly.example.ui.components.RetokenizeCard
import com.spreedly.example.ui.components.ThemeConfigurationCard
import com.spreedly.example.ui.components.ThemeConfigurationStyle
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.basicCheckoutViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.paymentsheet.recache.SpreedlyRecacheUI
import com.spreedly.security.secureScreen
import kotlinx.coroutines.launch

private const val TAG = "BasicCheckoutScreen"

/**
 * Simple text field component for the basic checkout with custom fields.
 */
@Composable
fun SimpleInputField(
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
    onFocusChanged: ((Boolean) -> Unit)? = null,
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
        Row(modifier = Modifier.padding(bottom = Spacing.xxs)) {
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
                    MaterialTheme.shapes.small,
                ).border(
                    width = borderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.small,
                ).padding(Spacing.sm)
                .onFocusChanged { fc ->
                    isFocused = fc.isFocused
                    onFocusChanged?.invoke(fc.isFocused)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                modifier = Modifier.padding(top = Spacing.xxs),
            )
        }
    }
}

@Composable
@SuppressLint("ComposeModifierMissing")
fun BasicCheckoutScreen(
    viewModel: BasicCheckoutViewModel = basicCheckoutViewModel(),
) {
    val sdk = viewModel.sdk
    val hostedCardDisplayState by sdk.hostedCardDisplayState
    val inspectorUiState by viewModel.inspectorUiState.collectAsState()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    val savedPaymentMethods by viewModel.savedPaymentMethods.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val useCustomTheme by viewModel.useCustomTheme.collectAsState()
    val selectedThemePreset by viewModel.selectedThemePreset.collectAsState()
    val fieldOverrideTarget by viewModel.fieldOverrideTarget.collectAsState()
    val fieldStyleOverrides by viewModel.fieldStyleOverrides.collectAsState()
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
    val cvvFieldConfig =
        remember(useCustomTheme, selectedThemePreset, fieldOverrideTarget, fieldStyleOverrides, isDarkMode) {
            viewModel.resolveSplFieldConfig(FormFieldType.CVV(true), isDarkMode)
        }

    val coroutineScope = rememberCoroutineScope()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var shouldRetainPaymentMethod by rememberSaveable { mutableStateOf(false) }
    var eligibleForCardUpdater by rememberSaveable { mutableStateOf(false) }
    var enableAutofill by rememberSaveable { mutableStateOf(true) }
    var forceMaskOnLifecycleStop by rememberSaveable { mutableStateOf(true) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    var nameWasFocused by rememberSaveable { mutableStateOf(false) }
    var emailWasFocused by rememberSaveable { mutableStateOf(false) }
    var nameHasEverChanged by rememberSaveable { mutableStateOf(false) }
    var emailHasEverChanged by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hostedCardDisplayState) {
        viewModel.fieldStateInspector.refreshMismatch(hostedCardDisplayState)
    }

    LaunchedEffect(viewModel) {
        viewModel.checkoutEvent.collect { result ->
            when (result) {
                is PaymentProcessingResult.Processing -> {
                    fullName = ""
                    email = ""
                    viewModel.updateNameValidity("")
                    viewModel.updateEmailValidity("")
                    viewModel.onCheckoutFieldsClearedBySdk()
                    viewModel.clearCustomFieldInputErrors()
                    viewModel.setProcessing(true)
                    viewModel.startPaymentPolling()
                }
                is PaymentProcessingResult.ValidationFailed -> Unit
            }
        }
    }

    val formFields = listOf(
        FormFieldType.CARD(true),
        FormFieldType.EXPIRY_DATE(true),
        FormFieldType.CVV(true),
    )
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
                    .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section
            Card(
                modifier =
                    Modifier
                        .size(80.dp)
                        .padding(bottom = Spacing.lg),
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
                        imageVector = Icons.Default.Payment,
                        contentDescription = "Payment SDK",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(
                text = "Basic Checkout with Custom Fields",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
            Text(
                text = "SPL fields for sensitive data, custom fields with custom button",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xxxl),
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
                        .padding(horizontal = Spacing.md)
                        .padding(bottom = Spacing.md),
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
                        .padding(horizontal = Spacing.md)
                        .padding(bottom = Spacing.lg),
            )

            // Saved Payment Methods Section
            if (savedPaymentMethods.isNotEmpty()) {
                SavedPaymentMethodsList(
                    savedPaymentMethods = savedPaymentMethods,
                    onCardClick = { savedCard ->
                        viewModel.recacheSavedPaymentMethod(savedCard)
                    },
                    onDeleteClick = { savedCard ->
                        viewModel.deleteSavedPaymentMethod(savedCard.token)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            // Main Card
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                shape = MaterialTheme.shapes.large,
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
                            .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Sensitive Card Information Section - Uses SPL fields
                    Text(
                        text = "Card Information (Secure)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                    )

                    MerchantMaskToggleBar(
                        sdk = sdk,
                        hostedCardDisplayState = hostedCardDisplayState,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )

                    HeadlessHostedFieldsConfigCard(
                        enableAutofill = enableAutofill,
                        onEnableAutofillChange = { enableAutofill = it },
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "forceMaskOnLifecycleStop (CARD)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        androidx.compose.material3.Switch(
                            checked = forceMaskOnLifecycleStop,
                            onCheckedChange = { forceMaskOnLifecycleStop = it },
                        )
                    }

                    // SPL Card Number Field
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
                        forceMaskOnLifecycleStop = forceMaskOnLifecycleStop,
                        sdk = sdk,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

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
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

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
                            enableAutofill = enableAutofill,
                            sdk = sdk,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Personal Information Section - Custom handling
                    Text(
                        text = "Personal Information (Custom Handled)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                    )

                    // Custom Name Field
                    SimpleInputField(
                        label = "Full Name",
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (it.isNotEmpty()) nameHasEverChanged = true
                            if (nameHasEverChanged || attemptedSubmit) {
                                viewModel.validateName(it)
                            } else {
                                viewModel.clearNameError()
                                viewModel.updateNameValidity(it)
                            }
                        },
                        isRequired = true,
                        isError = nameError != null,
                        errorMessage = nameError,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                        onFocusChanged = { focused ->
                            if (focused) {
                                nameWasFocused = true
                            } else if (nameWasFocused) {
                                viewModel.validateName(fullName)
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Custom Email Field
                    SimpleInputField(
                        label = "Email",
                        value = email,
                        onValueChange = {
                            email = it
                            if (it.isNotEmpty()) emailHasEverChanged = true
                            if (emailHasEverChanged || attemptedSubmit) {
                                viewModel.validateEmail(it)
                            } else {
                                viewModel.clearEmailError()
                                viewModel.updateEmailValidity(it)
                            }
                        },
                        isRequired = true,
                        isError = emailError != null,
                        errorMessage = emailError,
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                        onFocusChanged = { focused ->
                            if (focused) {
                                emailWasFocused = true
                            } else if (emailWasFocused) {
                                viewModel.validateEmail(email)
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Save Payment Method Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shouldRetainPaymentMethod = !shouldRetainPaymentMethod }
                            .padding(vertical = Spacing.xs),
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
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Save payment information for future use",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { eligibleForCardUpdater = !eligibleForCardUpdater }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = eligibleForCardUpdater,
                            onCheckedChange = { eligibleForCardUpdater = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Eligible for card updater",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    val isButtonDisabled = isInitializing || isProcessing || !isFormValid

                    // Custom Payment Button using new pattern
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            val nameOk = viewModel.validateName(fullName)
                            val emailOk = viewModel.validateEmail(email)
                            if (!nameOk || !emailOk) {
                                return@Button
                            }
                            if (!isFormValid) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Fix invalid fields before paying.")
                                }
                                return@Button
                            }
                            if (!sdk.areAllFieldsValid(formFields)) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Please fix card field errors before proceeding",
                                    )
                                }
                                return@Button
                            }
                            Log.d(TAG, "Starting payment processing...")
                            viewModel.launchSubmitCheckout(
                                SubmitCheckoutParams(
                                    formFields = formFields,
                                    fullName = fullName,
                                    email = email,
                                    shouldRetainPaymentMethod = shouldRetainPaymentMethod,
                                    eligibleForCardUpdater = eligibleForCardUpdater.takeIf { it },
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isButtonDisabled,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
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
                                Spacer(modifier = Modifier.width(Spacing.xs))
                            }
                            Text(
                                text = when {
                                    isInitializing -> "Initializing..."
                                    isProcessing -> "Processing Payment..."
                                    !isFormValid -> "Complete All Fields"
                                    else -> "Pay with Custom Button"
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

                    Spacer(modifier = Modifier.height(Spacing.md))

                    TextButton(
                        onClick = { viewModel.performFullPaymentReset() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("basic-checkout-reset-payment-state-button"),
                    ) {
                        Text("resetPaymentState()")
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    FieldStateInspectorCard(
                        uiState = inspectorUiState,
                        hostedCardDisplayState = hostedCardDisplayState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
            // Info Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Secure",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    "SPL secure fields + custom button with additionalFields",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Required for recaching to work - observes SDK state and shows recache UI
        SpreedlyRecacheUI(sdk = sdk)
    }
}
