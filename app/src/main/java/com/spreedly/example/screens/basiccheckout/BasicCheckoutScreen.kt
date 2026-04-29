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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.sp
import com.spreedly.example.ui.components.SavedPaymentMethodsList
import com.spreedly.example.ui.components.RetokenizeCard
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.basicCheckoutViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CustomFieldsConfig
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
                .onFocusChanged { isFocused = it.isFocused },
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
    val snackbarHostState = viewModel.snackbarHostState
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    val savedPaymentMethods by viewModel.savedPaymentMethods.collectAsState()

    // Custom fields for the new pattern - handled manually
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    // Simple validation states
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }

    // SDK initialization and payment result handling is now managed by ViewModel

    // Simple validation functions
    fun validateName(): Boolean {
        nameError = when {
            fullName.isBlank() -> "Name is required"
            fullName.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
        return nameError == null
    }

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "Email is required"
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$")) -> "Invalid email format"
            else -> null
        }
        return emailError == null
    }

    fun isFormValid(): Boolean = validateName() && validateEmail()

    // Pure check without side effects -- safe to call during composition
    fun isFormFilledOut(): Boolean =
        fullName.isNotBlank() && fullName.length >= 2 &&
            email.isNotBlank() && email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))

    // Only sensitive fields require validation via SDK
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

                    // SPL Card Number Field
                    SPLTextField(
                        label = "Card Number",
                        formFieldType = FormFieldType.CARD(true),
                        config = CustomFieldsConfig.Default,
                        value = sdk.paymentState.value.cardNumber.value,
                        onChange = { sdk.callbacks.onCardNumberChange(it, true) },
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // SPL Expiry Date Field
                        SPLTextField(
                            label = "MM/YY",
                            formFieldType = FormFieldType.EXPIRY_DATE(true),
                            config = CustomFieldsConfig.Default,
                            value = sdk.paymentState.value.expirationDate.value,
                            onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                            modifier = Modifier.weight(1f),
                        )

                        // SPL CVV Field
                        SPLTextField(
                            label = "CVV",
                            formFieldType = FormFieldType.CVV(true),
                            config = CustomFieldsConfig.Default,
                            value = sdk.paymentState.value.securityCode.value,
                            onChange = { sdk.callbacks.onSecurityCodeChange(it, true) },
                            modifier = Modifier.weight(1f),
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
                        onValueChange = { fullName = it },
                        isRequired = true,
                        isError = nameError != null,
                        errorMessage = nameError,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Custom Email Field
                    SimpleInputField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        isRequired = true,
                        isError = emailError != null,
                        errorMessage = emailError,
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
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

                    val isButtonDisabled = isInitializing || isProcessing || !isFormFilledOut()

                    // Custom Payment Button using new pattern
                    Button(
                        onClick = {
                            if (isFormValid()) {
                                coroutineScope.launch {
                                    try {
                                        Log.d(TAG, "Starting payment processing...")

                                        // Create additional fields map using new pattern
                                        val additionalFields = mapOf(
                                            AdditionalField.FULL_NAME to fullName.split(" ").firstOrNull().orEmpty(),
                                            AdditionalField.EMAIL to email,
                                        )

                                        // Use the new iOS-style createCreditCard method
                                        val result = sdk.createCreditCard(
                                            formFields = formFields, // Only sensitive fields
                                            additionalFields = additionalFields, // Custom fields passed directly
                                            metadata = mapOf(
                                                "checkout_type" to "basic_with_custom_fields",
                                                "timestamp" to "${System.currentTimeMillis()}",
                                                "pattern" to "ios_style",
                                            ),
                                            retainOnSuccess = shouldRetainPaymentMethod,
                                        )

                                        Log.d(TAG, "Payment processing result received")

                                        when (result) {
                                            is PaymentProcessingResult.Processing -> {
                                                Log.d(TAG, "Payment processing started")
                                                fullName = ""
                                                email = ""
                                                nameError = null
                                                emailError = null
                                                viewModel.setProcessing(true)
                                                viewModel.startPaymentPolling()
                                            }

                                            is PaymentProcessingResult.ValidationFailed -> {
                                                Log.d(
                                                    TAG,
                                                    "Validation failed for fields: ${result.invalidFields}",
                                                )
                                                viewModel.setProcessing(false) // Ensure processing state is reset
                                                snackbarHostState
                                                    .showSnackbar("Validation failed. Please check your information.")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d(TAG, "Error during payment processing: ${e::class.simpleName}")
                                        viewModel.setProcessing(false)
                                        snackbarHostState.showSnackbar("Payment processing error")
                                    }
                                }
                            }
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
                                    !isFormFilledOut() -> "Complete All Fields"
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
