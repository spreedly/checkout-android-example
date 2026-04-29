package com.spreedly.example.screens.flexibleexpiry

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.flexibleExpiryViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CheckoutButton
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.security.secureScreen
import com.spreedly.validation.ValidationParameter

@Composable
@SuppressLint("ComposeModifierMissing")
fun FlexibleExpiryScreen(
    viewModel: FlexibleExpiryViewModel = flexibleExpiryViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    var useSeparateFields by rememberSaveable { mutableStateOf(true) }
    var allowBlankName by rememberSaveable { mutableStateOf(false) }
    var allowBlankDate by rememberSaveable { mutableStateOf(false) }
    var useTwoDigitFields by rememberSaveable { mutableStateOf(false) }
    var allowExpiredDate by rememberSaveable { mutableStateOf(false) }
    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    fun setParamSafely(param: ValidationParameter, value: Boolean) {
        try {
            sdk.setParam(param, value)
        } catch (_: IllegalStateException) {
            // SDK not ready yet
        }
    }

    // Sync SpreedlyParamsManager during composition so that isFieldRequired()
    // inside SPLTextField reads the correct values in the same frame.
    // Re-runs whenever the SDK readiness or any toggle changes.
    @Suppress("UNUSED_VARIABLE")
    val paramsSynced = remember(isInitializing, allowBlankName, allowBlankDate, allowExpiredDate) {
        if (!isInitializing) {
            setParamSafely(ValidationParameter.ALLOW_BLANK_NAME, allowBlankName)
            setParamSafely(ValidationParameter.ALLOW_BLANK_DATE, allowBlankDate)
            setParamSafely(ValidationParameter.ALLOW_EXPIRED_DATE, allowExpiredDate)
        }
        true
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
                    .padding(6.dp),
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
                        imageVector = Icons.Default.Payment,
                        contentDescription = "Payment SDK",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = "Validation Parameters Demo",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Configure validation behavior and field layout",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // Field Layout Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            .padding(16.dp)
                            .clickable { useSeparateFields = !useSeparateFields },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (useSeparateFields) "Separate Month/Year Fields" else "Combined Expiry Date Field",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                    Switch(
                        checked = useSeparateFields,
                        onCheckedChange = { useSeparateFields = it },
                    )
                }
            }

            // Allow Blank Name Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                val newValue = !allowBlankName
                                allowBlankName = newValue
                                setParamSafely(ValidationParameter.ALLOW_BLANK_NAME, newValue)
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Blank Name",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                        Text(
                            text = "Skip name field validation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowBlankName,
                        onCheckedChange = {
                            allowBlankName = it
                            setParamSafely(ValidationParameter.ALLOW_BLANK_NAME, it)
                        },
                    )
                }
            }

            // Allow Blank Date Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                val newValue = !allowBlankDate
                                allowBlankDate = newValue
                                setParamSafely(ValidationParameter.ALLOW_BLANK_DATE, newValue)
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Blank Date",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                        Text(
                            text = "Skip date field validation when empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowBlankDate,
                        onCheckedChange = {
                            allowBlankDate = it
                            setParamSafely(ValidationParameter.ALLOW_BLANK_DATE, it)
                        },
                    )
                }
            }

            // Allow Expired Date Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                val newValue = !allowExpiredDate
                                allowExpiredDate = newValue
                                setParamSafely(ValidationParameter.ALLOW_EXPIRED_DATE, newValue)
                            },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Expired Date",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                        Text(
                            text = "Accept expired expiry dates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowExpiredDate,
                        onCheckedChange = {
                            allowExpiredDate = it
                            setParamSafely(ValidationParameter.ALLOW_EXPIRED_DATE, it)
                        },
                    )
                }
            }

            // 2-Digit Fields Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { useTwoDigitFields = !useTwoDigitFields },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2-Digit Date Fields",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                        Text(
                            text = "Use 2-digit month (MM) and 2-digit year (YY) fields",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = useTwoDigitFields,
                        onCheckedChange = { useTwoDigitFields = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    SPLTextField(
                        label = "Card Number",
                        formFieldType = FormFieldType.CARD(true),
                        config = CustomFieldsConfig.Default,
                        value = sdk.paymentState.value.cardNumber.value,
                        onChange = { sdk.callbacks.onCardNumberChange(it, true) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Conditional Expiry Fields
                    val dateRequired = !allowBlankDate

                    if (useSeparateFields) {
                        if (useTwoDigitFields) {
                            // 2-Digit Month and 2-Digit Year Fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SPLTextField(
                                    label = "Month (MM)",
                                    formFieldType = FormFieldType.MONTH(dateRequired),
                                    config = CustomFieldsConfig.Default,
                                    value = sdk.paymentState.value.month.value,
                                    onChange = { sdk.callbacks.onMonthChange(it, dateRequired) },
                                    modifier = Modifier.weight(1f),
                                )

                                SPLTextField(
                                    label = "Year (YY)",
                                    formFieldType = FormFieldType.YEAR_SECONDARY(dateRequired),
                                    config = CustomFieldsConfig.Default,
                                    value = sdk.paymentState.value.yearSecondary.value,
                                    onChange = { sdk.callbacks.onYearSecondaryChange(it, dateRequired) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            // Regular Month and 4-Digit Year Fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SPLTextField(
                                    label = "Month",
                                    formFieldType = FormFieldType.MONTH(dateRequired),
                                    config = CustomFieldsConfig.Default,
                                    value = sdk.paymentState.value.month.value,
                                    onChange = { sdk.callbacks.onMonthChange(it, dateRequired) },
                                    modifier = Modifier.weight(1f),
                                )

                                SPLTextField(
                                    label = "Year (YYYY)",
                                    formFieldType = FormFieldType.YEAR(dateRequired),
                                    config = CustomFieldsConfig.Default,
                                    value = sdk.paymentState.value.year.value,
                                    onChange = { sdk.callbacks.onYearChange(it, dateRequired) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        SPLTextField(
                            label = "Expiry Date (MM/YY)",
                            formFieldType = FormFieldType.EXPIRY_DATE(dateRequired),
                            config = CustomFieldsConfig.Default,
                            value = sdk.paymentState.value.expirationDate.value,
                            onChange = { sdk.callbacks.onExpirationDateChange(it, dateRequired) },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SPLTextField(
                        label = "Security Code",
                        formFieldType = FormFieldType.CVV(true),
                        config = CustomFieldsConfig.Default,
                        value = sdk.paymentState.value.securityCode.value,
                        onChange = { sdk.callbacks.onSecurityCodeChange(it) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val nameRequired = !allowBlankName

                    SPLTextField(
                        label = "Name",
                        formFieldType = FormFieldType.NAME(nameRequired),
                        config = CustomFieldsConfig.Default,
                        value = sdk.paymentState.value.nameOnCard.value,
                        onChange = { sdk.callbacks.onNameOnCardChange(it, nameRequired) },
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

                    CheckoutButton(
                        formFields =
                            when {
                                useSeparateFields && useTwoDigitFields -> {
                                    listOf(
                                        FormFieldType.CARD(true),
                                        FormFieldType.MONTH(dateRequired),
                                        FormFieldType.YEAR_SECONDARY(dateRequired),
                                        FormFieldType.CVV(true),
                                        FormFieldType.NAME(nameRequired),
                                    )
                                }

                                useSeparateFields -> {
                                    listOf(
                                        FormFieldType.CARD(true),
                                        FormFieldType.MONTH(dateRequired),
                                        FormFieldType.YEAR(dateRequired),
                                        FormFieldType.CVV(true),
                                        FormFieldType.NAME(nameRequired),
                                    )
                                }

                                else -> {
                                    listOf(
                                        FormFieldType.CARD(true),
                                        FormFieldType.EXPIRY_DATE(dateRequired),
                                        FormFieldType.CVV(true),
                                        FormFieldType.NAME(nameRequired),
                                    )
                                }
                            },
                        sdk = sdk,
                        isInitializing = isInitializing,
                        metadata =
                            mapOf(
                                "checkout_type" to "validation_params_demo",
                                "expiry_mode" to when {
                                    useSeparateFields && useTwoDigitFields -> "separate_2digit"
                                    useSeparateFields -> "separate_4digit"
                                    else -> "combined"
                                },
                                "allow_blank_name" to allowBlankName.toString(),
                                "allow_blank_date" to allowBlankDate.toString(),
                                "allow_expired_date" to allowExpiredDate.toString(),
                                "use_two_digit_fields" to useTwoDigitFields.toString(),
                                "timestamp" to "${System.currentTimeMillis()}",
                                "fields_count" to when {
                                    useSeparateFields && useTwoDigitFields -> "4"
                                    useSeparateFields -> "4"
                                    else -> "3"
                                },
                            ),
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
                    "Validation parameters and field configuration demo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
