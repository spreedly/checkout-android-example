package com.spreedly.example.screens.customcheckout

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.spreedly.example.ui.components.RetokenizeCard
import com.spreedly.example.ui.components.SavedPaymentMethodsList
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.checkoutWithAdditionalFieldsViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CheckoutButton
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.paymentsheet.recache.SpreedlyRecacheUI
import com.spreedly.security.secureScreen

private const val TAG = "CheckoutWithAdditionalFieldsScreen"

/**
 * Enum representing the different field types for focus management
 */
enum class AdditionalFieldType {
    CARD_NUMBER,
    EXPIRY_DATE,
    CVV,
    NAME,
    CITY,
    STATE,
    ADDRESS_LINE_1,
    ADDRESS_LINE_2,
    ZIP,
}

@Composable
@SuppressLint("ComposeModifierMissing")
fun CheckoutWithAdditionalFieldsScreen(
    viewModel: CheckoutWithAdditionalFieldsViewModel = checkoutWithAdditionalFieldsViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val paymentToken by viewModel.token.collectAsState()
    val savedPaymentMethods by viewModel.savedPaymentMethods.collectAsState()

    // Create theme-aware config for SPL text fields
    val splFieldConfig = CustomFieldsConfig(
        fieldBackgroundColor = MaterialTheme.colorScheme.surface,
        textColor = MaterialTheme.colorScheme.onSurface,
        fieldLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        primaryColor = MaterialTheme.colorScheme.primary,
        formBorderColor = MaterialTheme.colorScheme.outline,
    )

    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    // Focus management state
    var focusedFieldType by remember { mutableStateOf<AdditionalFieldType?>(AdditionalFieldType.CARD_NUMBER) }
    var autoAdvanceEnabled by remember { mutableStateOf(true) }

    // Validation states for each field
    var cardNumberIsValid by remember { mutableStateOf(false) }
    var expiryIsValid by remember { mutableStateOf(false) }
    var cvvIsValid by remember { mutableStateOf(false) }
    var nameIsValid by remember { mutableStateOf(false) }

    /**
     * Handles field submission and auto-advances to the next field if enabled
     */
    fun handleFieldSubmit(fieldType: AdditionalFieldType) {
        if (!autoAdvanceEnabled) return

        focusedFieldType = when (fieldType) {
            AdditionalFieldType.CARD_NUMBER -> if (cardNumberIsValid) AdditionalFieldType.EXPIRY_DATE else null
            AdditionalFieldType.EXPIRY_DATE -> if (expiryIsValid) AdditionalFieldType.CVV else null
            AdditionalFieldType.CVV -> if (cvvIsValid) AdditionalFieldType.NAME else null
            AdditionalFieldType.NAME -> if (nameIsValid) AdditionalFieldType.CITY else null
            AdditionalFieldType.CITY -> AdditionalFieldType.STATE
            AdditionalFieldType.STATE -> AdditionalFieldType.ADDRESS_LINE_1
            AdditionalFieldType.ADDRESS_LINE_1 -> AdditionalFieldType.ADDRESS_LINE_2
            AdditionalFieldType.ADDRESS_LINE_2 -> AdditionalFieldType.ZIP
            AdditionalFieldType.ZIP -> null // Last field
        }
    }

    /**
     * Gets the appropriate IME action for each field
     */
    fun getImeAction(fieldType: AdditionalFieldType): ImeAction = when (fieldType) {
        AdditionalFieldType.ZIP -> ImeAction.Done
        else -> ImeAction.Next
    }

    // Initialize SDK only once when screen is first created
    LaunchedEffect(Unit) {
        if (!sdk.isInitialized) {
            viewModel.initializeSDKIfNeeded()
        }
    }

    // SDK initialization and payment result handling is now managed by ViewModel
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
                text = "Checkout with additional fields",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Enter your payment details",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Focus Control Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-advance on valid input",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Text(
                            text = "Currently: ${focusedFieldType?.name?.replace("_", " ") ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoAdvanceEnabled,
                        onCheckedChange = { autoAdvanceEnabled = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main Card
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SPLTextField(
                        label = "Card Number",
                        formFieldType = FormFieldType.CARD(true),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.cardNumber.value,
                        onChange = { sdk.callbacks.onCardNumberChange(it, true) },
                        onValidationChange = { valid -> cardNumberIsValid = valid },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.CARD_NUMBER) },
                        imeAction = getImeAction(AdditionalFieldType.CARD_NUMBER),
                        shouldFocus = focusedFieldType == AdditionalFieldType.CARD_NUMBER,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Expiry Date",
                        formFieldType = FormFieldType.EXPIRY_DATE(true),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.expirationDate.value,
                        onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                        onValidationChange = { valid -> expiryIsValid = valid },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.EXPIRY_DATE) },
                        imeAction = getImeAction(AdditionalFieldType.EXPIRY_DATE),
                        shouldFocus = focusedFieldType == AdditionalFieldType.EXPIRY_DATE,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Security Code",
                        formFieldType = FormFieldType.CVV(true),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.securityCode.value,
                        onChange = { sdk.callbacks.onSecurityCodeChange(it, true) },
                        onValidationChange = { valid -> cvvIsValid = valid },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.CVV) },
                        imeAction = getImeAction(AdditionalFieldType.CVV),
                        shouldFocus = focusedFieldType == AdditionalFieldType.CVV,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Name",
                        formFieldType = FormFieldType.NAME(true),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.nameOnCard.value,
                        onChange = { sdk.callbacks.onNameOnCardChange(it, true) },
                        onValidationChange = { valid -> nameIsValid = valid },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.NAME) },
                        imeAction = getImeAction(AdditionalFieldType.NAME),
                        shouldFocus = focusedFieldType == AdditionalFieldType.NAME,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "City",
                        formFieldType = FormFieldType.CITY(false),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.city.value,
                        onChange = { sdk.callbacks.onCityChange(it, false) },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.CITY) },
                        imeAction = getImeAction(AdditionalFieldType.CITY),
                        shouldFocus = focusedFieldType == AdditionalFieldType.CITY,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "State",
                        formFieldType = FormFieldType.STATE(false),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.state.value,
                        onChange = { sdk.callbacks.onStateChange(it) },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.STATE) },
                        imeAction = getImeAction(AdditionalFieldType.STATE),
                        shouldFocus = focusedFieldType == AdditionalFieldType.STATE,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Address Line 1",
                        formFieldType = FormFieldType.ADDRESS_LINE_1(false),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.addressLine1.value,
                        onChange = { sdk.callbacks.onAddressLine1Change(it) },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.ADDRESS_LINE_1) },
                        imeAction = getImeAction(AdditionalFieldType.ADDRESS_LINE_1),
                        shouldFocus = focusedFieldType == AdditionalFieldType.ADDRESS_LINE_1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Address Line 2",
                        formFieldType = FormFieldType.ADDRESS_LINE_2(false),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.addressLine2.value,
                        onChange = { sdk.callbacks.onAddressLine2Change(it) },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.ADDRESS_LINE_2) },
                        imeAction = getImeAction(AdditionalFieldType.ADDRESS_LINE_2),
                        shouldFocus = focusedFieldType == AdditionalFieldType.ADDRESS_LINE_2,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Zip Code",
                        formFieldType = FormFieldType.ZIP(false),
                        config = splFieldConfig,
                        value = sdk.paymentState.value.zipCode.value,
                        onChange = { sdk.callbacks.onZipCodeChange(it) },
                        onImeAction = { handleFieldSubmit(AdditionalFieldType.ZIP) },
                        imeAction = getImeAction(AdditionalFieldType.ZIP),
                        shouldFocus = focusedFieldType == AdditionalFieldType.ZIP,
                    )

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

                    CheckoutButton(
                        formFields =
                            listOf(
                                FormFieldType.CARD(true),
                                FormFieldType.EXPIRY_DATE(true),
                                FormFieldType.CVV(true),
                                FormFieldType.NAME(true),
                                FormFieldType.CITY(false),
                                FormFieldType.STATE(false),
                                FormFieldType.ADDRESS_LINE_1(false),
                                FormFieldType.ADDRESS_LINE_2(false),
                                FormFieldType.ZIP(false),
                            ),
                        sdk = sdk,
                        isInitializing = isInitializing,
                        retainOnSuccess = shouldRetainPaymentMethod,
                    )

                    RetokenizeCard(
                        paymentToken = paymentToken,
                        onRetokenize = { viewModel.reinitialize() },
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
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Secure tokenized payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Required for recaching to work - observes SDK state and shows recache UI
        SpreedlyRecacheUI(sdk = sdk)
    }
}
