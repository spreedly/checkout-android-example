package com.spreedly.example.screens.recachingshowcase

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spreedly.app.R
import com.spreedly.example.ui.components.RetokenizeCard
import com.spreedly.example.screens.basiccheckout.SimpleInputField
import com.spreedly.example.ui.components.SavedPaymentMethodsList
import com.spreedly.example.ui.theme.SampleThemePreset
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.recachingShowcaseViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.models.ScreenPresentationMode
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.paymentsheet.recache.SpreedlyRecacheUI
import com.spreedly.security.secureScreen
import kotlinx.coroutines.launch

private const val TAG = "RecachingShowcaseScreen"

/**
 * Screen showcasing the recaching feature with customizable presentation mode and theme.
 */
@Composable
@SuppressLint("ComposeModifierMissing")
fun RecachingShowcaseScreen(
    viewModel: RecachingShowcaseViewModel = recachingShowcaseViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    val savedPaymentMethods by viewModel.savedPaymentMethods.collectAsState()
    val presentationMode by viewModel.presentationMode.collectAsState()
    val customThemeEnabled by viewModel.customThemeEnabled.collectAsState()
    val selectedThemePreset by viewModel.selectedThemePreset.collectAsState()

    // Custom fields for payment form
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    // Validation parameters for recaching
    var allowBlankName by rememberSaveable { mutableStateOf(false) }
    var allowBlankDate by rememberSaveable { mutableStateOf(false) }
    var allowExpiredDate by rememberSaveable { mutableStateOf(false) }

    // Simple validation states
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }

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
                    .secureScreen()
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
                        imageVector = Icons.Default.Security,
                        contentDescription = "Recaching",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(
                text = stringResource(R.string.recaching_showcase_title),
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.recaching_showcase_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            // Customization Section
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.recaching_customization),
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                        )
                    }

                    // Presentation Mode Selector
                    Text(
                        text = stringResource(R.string.presentation_mode),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PresentationModeButton(
                            text = stringResource(R.string.bottom_sheet),
                            isSelected = presentationMode == ScreenPresentationMode.bottomSheet,
                            onClick = { viewModel.updatePresentationMode(ScreenPresentationMode.bottomSheet) },
                            modifier = Modifier.weight(1f),
                        )
                        PresentationModeButton(
                            text = stringResource(R.string.dialog),
                            isSelected = presentationMode == ScreenPresentationMode.dialog,
                            onClick = { viewModel.updatePresentationMode(ScreenPresentationMode.dialog) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // Custom Theme Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.custom_theme),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = customThemeEnabled,
                            onCheckedChange = { viewModel.toggleCustomTheme() },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }

                    // Theme Preset Selector (shown when custom theme is enabled)
                    if (customThemeEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.theme_preset),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemePresetChip(
                                text = stringResource(R.string.theme_default),
                                isSelected = selectedThemePreset == SampleThemePreset.DEFAULT,
                                onClick = { viewModel.updateThemePreset(SampleThemePreset.DEFAULT) },
                                color = Color(0xFF757575),
                            )
                            ThemePresetChip(
                                text = stringResource(R.string.theme_blue),
                                isSelected = selectedThemePreset == SampleThemePreset.BLUE,
                                onClick = { viewModel.updateThemePreset(SampleThemePreset.BLUE) },
                                color = Color(0xFF1976D2),
                            )
                            ThemePresetChip(
                                text = stringResource(R.string.theme_green),
                                isSelected = selectedThemePreset == SampleThemePreset.GREEN,
                                onClick = { viewModel.updateThemePreset(SampleThemePreset.GREEN) },
                                color = Color(0xFF388E3C),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemePresetChip(
                                text = stringResource(R.string.theme_purple),
                                isSelected = selectedThemePreset == SampleThemePreset.PURPLE,
                                onClick = { viewModel.updateThemePreset(SampleThemePreset.PURPLE) },
                                color = Color(0xFF7B1FA2),
                            )
                            ThemePresetChip(
                                text = stringResource(R.string.theme_dark),
                                isSelected = selectedThemePreset == SampleThemePreset.DARK,
                                onClick = { viewModel.updateThemePreset(SampleThemePreset.DARK) },
                                color = Color(0xFF212121),
                            )
                            // Spacer to balance the row
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // Validation Parameters Section
                    Text(
                        text = "Validation Parameters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    // Allow Blank Name Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Allow Blank Name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = allowBlankName,
                            onCheckedChange = { allowBlankName = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }

                    // Allow Blank Date Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Allow Blank Date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = allowBlankDate,
                            onCheckedChange = { allowBlankDate = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }

                    // Allow Expired Date Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Allow Expired Date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = allowExpiredDate,
                            onCheckedChange = { allowExpiredDate = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Payment Methods Section
            if (savedPaymentMethods.isNotEmpty()) {
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
                                .padding(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.saved_cards_section),
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            text = stringResource(R.string.click_card_to_recache),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        SavedPaymentMethodsList(
                            savedPaymentMethods = savedPaymentMethods,
                            onCardClick = { savedCard ->
                                viewModel.recacheSavedPaymentMethod(savedCard)
                            },
                            onDeleteClick = { savedCard ->
                                viewModel.deleteSavedPaymentMethod(savedCard.token)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.or_create_new),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Form Section
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
                    // Card Information Section
                    Text(
                        text = "Card Information",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    )

                    // SPL Card Number Field
                    SPLTextField(
                        label = "Card Number",
                        formFieldType = FormFieldType.CARD(true),
                        config = CustomFieldsConfig.Default,
                        value = sdk.paymentState.value.cardNumber.value,
                        onChange = { sdk.callbacks.onCardNumberChange(it, true) },
                        sdk = sdk,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // SPL Expiry Date Field
                        SPLTextField(
                            label = "MM/YY",
                            formFieldType = FormFieldType.EXPIRY_DATE(true),
                            config = CustomFieldsConfig.Default,
                            value = sdk.paymentState.value.expirationDate.value,
                            onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next,
                        )

                        // SPL CVV Field
                        SPLTextField(
                            label = "CVV",
                            formFieldType = FormFieldType.CVV(true),
                            config = CustomFieldsConfig.Default,
                            value = sdk.paymentState.value.securityCode.value,
                            onChange = { sdk.callbacks.onSecurityCodeChange(it, true) },
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next,
                            sdk = sdk,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Personal Information Section
                    Text(
                        text = "Personal Information",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

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

                    val isButtonDisabled = isInitializing || isProcessing || !isFormFilledOut()

                    // Create Payment Method Button
                    Button(
                        onClick = {
                            if (isFormValid()) {
                                coroutineScope.launch {
                                    try {
                                        Log.d(TAG, "Starting payment processing...")

                                        // Create additional fields map
                                        val additionalFields =
                                            mapOf(
                                                AdditionalField.FULL_NAME to fullName
                                                    .split(" ")
                                                    .firstOrNull()
                                                    .orEmpty(),
                                                AdditionalField.EMAIL to email,
                                            )

                                        // Use createCreditCard method
                                        val result =
                                            sdk.createCreditCard(
                                                formFields = formFields,
                                                additionalFields = additionalFields,
                                                metadata =
                                                    mapOf(
                                                        "checkout_type" to "recaching_showcase",
                                                        "timestamp" to "${System.currentTimeMillis()}",
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
                                                viewModel.setProcessing(false)
                                                snackbarHostState
                                                    .showSnackbar(
                                                        "Validation failed. Please " +
                                                        "check your information.",
                                                    )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d(
                                            TAG,
                                            "Error during payment processing: ${e::class.simpleName}",
                                        )
                                        viewModel.setProcessing(false)
                                        snackbarHostState.showSnackbar(
                                            "Payment processing error",
                                        )
                                    }
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        enabled = !isButtonDisabled,
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
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
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text =
                                    when {
                                        isInitializing -> "Initializing..."
                                        isProcessing -> "Processing Payment..."
                                        !isFormFilledOut() -> "Complete All Fields"
                                        else -> stringResource(R.string.create_payment_method)
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
                    "Customize recaching presentation and test with saved cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val spreedlyTheme = remember(customThemeEnabled, selectedThemePreset) {
            viewModel.buildSpreedlyTheme()
        }

        SpreedlyRecacheUI(
            sdk = sdk,
            theme = spreedlyTheme,
            allowBlankName = allowBlankName,
            allowBlankDate = allowBlankDate,
            allowExpiredDate = allowExpiredDate,
        )
    }
}

/**
 * Button for selecting presentation mode.
 */
@Composable
private fun PresentationModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ).border(
                    width = 1.dp,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Chip for selecting theme preset.
 */
@Composable
private fun ThemePresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isSelected) {
                        color
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ).border(
                    width = 1.dp,
                    color =
                        if (isSelected) {
                            color
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = RoundedCornerShape(18.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
