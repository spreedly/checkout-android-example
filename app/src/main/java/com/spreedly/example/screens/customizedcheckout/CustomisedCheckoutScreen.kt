package com.spreedly.example.screens.customizedcheckout

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.spreedly.example.ui.components.SectionHeader
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.customisedCheckoutViewModel
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CheckoutButton
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.security.secureScreen

@Composable
@SuppressLint("ComposeModifierMissing")
fun CustomisedCheckoutScreen(
    viewModel: CustomisedCheckoutViewModel = customisedCheckoutViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val scrollState = rememberScrollState()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val paymentToken by viewModel.token.collectAsState()

    var shouldRetainPaymentMethod by remember { mutableStateOf(false) }

    // Initialize SDK only once when screen is first created
    LaunchedEffect(Unit) {
        if (!sdk.isInitialized) {
            viewModel.initializeSDKIfNeeded()
        }
    }

    // Custom configuration for this screen - Note: These colors are intentionally themed
    // to demonstrate customization capabilities. In production, use MaterialTheme colors
    val config = CustomFieldsConfig(
        primaryColor = MaterialTheme.colorScheme.primary,
        secondaryColor = MaterialTheme.colorScheme.secondary,
        formBorderColor = MaterialTheme.colorScheme.primary,
        formBackgroundColor = MaterialTheme.colorScheme.surface,
        fieldBackgroundColor = MaterialTheme.colorScheme.surface,
        fieldLabelColor = MaterialTheme.colorScheme.primary,
        borderRadius = 16.dp,
        fieldShape = RoundedCornerShape(12.dp),
    )

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
            // Header Section with custom colors
            Card(
                modifier =
                    Modifier
                        .size(80.dp)
                        .padding(bottom = Spacing.lg),
                shape = CircleShape,
                colors =
                    CardDefaults.cardColors(
                        containerColor = config.primaryColor,
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
                text = "Premium Checkout",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = config.primaryColor,
                    ),
                modifier = Modifier.padding(bottom = Spacing.xs),
            )

            Text(
                text = "Enhanced with premium card styling & layouts",
                style = MaterialTheme.typography.bodyLarge,
                color = config.fieldLabelColor,
                modifier = Modifier.padding(bottom = Spacing.xxxl),
            )

            // Main Card with custom styling
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                shape = RoundedCornerShape(config.borderRadius),
                colors =
                    CardDefaults.cardColors(
                        containerColor = config.formBackgroundColor,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, config.formBorderColor),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Payment Information Section
                    SectionHeader(
                        title = "Payment Information",
                        config = config,
                    )

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xxs),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(Spacing.md),
                        ) {
                            SPLTextField(
                                label = "Card Number",
                                formFieldType = FormFieldType.CARD(true),
                                config = config,
                                value = sdk.paymentState.value.cardNumber.value,
                                onChange = { sdk.callbacks.onCardNumberChange(it, true) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Expiry and Security Code Row with gradient background
                    Card(
                        modifier =
                            Modifier
                                .padding(vertical = Spacing.xxs)
                                .padding(end = Spacing.xs),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(Spacing.md),
                        ) {
                            SPLTextField(
                                label = "Expiry Date",
                                formFieldType = FormFieldType.EXPIRY_DATE(true),
                                config = config,
                                value = sdk.paymentState.value.expirationDate.value,
                                onChange = { sdk.callbacks.onExpirationDateChange(it, true) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Card(
                        modifier =
                            Modifier
                                .padding(vertical = Spacing.xxs)
                                .padding(end = Spacing.xs),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(Spacing.md),
                        ) {
                            SPLTextField(
                                label = "Security Code",
                                formFieldType = FormFieldType.CVV(true),
                                config = config,
                                value = sdk.paymentState.value.securityCode.value,
                                onChange = { sdk.callbacks.onSecurityCodeChange(it) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Billing Information Section
                    SectionHeader(
                        title = "Billing Information",
                        config = config,
                    )

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xxs),
                        shape = MaterialTheme.shapes.large,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(18.dp),
                        ) {
                            SPLTextField(
                                label = "Name",
                                formFieldType = FormFieldType.NAME(true),
                                config = config,
                                value = sdk.paymentState.value.nameOnCard.value,
                                onChange = { sdk.callbacks.onNameOnCardChange(it, true) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Address fields with grouped styling
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                        shape = RoundedCornerShape(Spacing.mlg),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.mlg),
                        ) {
                            Box(
                                modifier = Modifier.padding(bottom = Spacing.sm),
                            ) {
                                SPLTextField(
                                    label = "City",
                                    formFieldType = FormFieldType.CITY(true),
                                    config = config,
                                    value = sdk.paymentState.value.city.value,
                                    onChange = { sdk.callbacks.onCityChange(it, true) },
                                )
                            }

                            Box(
                                modifier = Modifier.padding(bottom = Spacing.sm),
                            ) {
                                SPLTextField(
                                    label = "State",
                                    formFieldType = FormFieldType.STATE(true),
                                    config = config,
                                    value = sdk.paymentState.value.state.value,
                                    onChange = { sdk.callbacks.onStateChange(it) },
                                )
                            }

                            SPLTextField(
                                label = "Zip Code",
                                formFieldType = FormFieldType.ZIP(true),
                                config = config,
                                value = sdk.paymentState.value.zipCode.value,
                                onChange = { sdk.callbacks.onZipCodeChange(it, true) },
                                imeAction = ImeAction.Done,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    // Premium styled checkout button container
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = Spacing.md),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Secure Payment",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    "Secure Payment Processing",
                                    style =
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        ),
                                )
                            }

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

                            CheckoutButton(
                                formFields =
                                    listOf(
                                        FormFieldType.CARD(true),
                                        FormFieldType.EXPIRY_DATE(true),
                                        FormFieldType.CVV(true),
                                        FormFieldType.NAME(true),
                                        FormFieldType.CITY(true),
                                        FormFieldType.STATE(true),
                                        FormFieldType.ZIP(true),
                                    ),
                                sdk = sdk,
                                isInitializing = isInitializing,
                                metadata =
                                    mapOf(
                                        "order_id" to "ORD-${System.currentTimeMillis()}",
                                        "user_id" to "user_12345",
                                        "checkout_type" to "premium",
                                        "currency" to "USD",
                                        "amount" to "99.99",
                                    ),
                                retainOnSuccess = shouldRetainPaymentMethod,
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

            // Info Section with custom colors
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
                    "Premium card layouts with enhanced visual styling & grouping",
                    style = MaterialTheme.typography.bodySmall,
                    color = config.fieldLabelColor,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
