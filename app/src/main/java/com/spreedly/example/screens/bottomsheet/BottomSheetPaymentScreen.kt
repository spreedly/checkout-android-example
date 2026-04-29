package com.spreedly.example.screens.bottomsheet

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.bottomSheetPaymentViewModel
import com.spreedly.paymentsheet.SpreedlyBottomSheet
import com.spreedly.sdk.ui.ConfigurableFormField
import com.spreedly.sdk.ui.NameFieldDisplayMode
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentSheetConfig
import com.spreedly.sdk.ui.YearFormat
import com.spreedly.validation.ValidationParameter

@SuppressLint("ComposeModifierMissing")
@Composable
fun BottomSheetPaymentScreen(
    viewModel: BottomSheetPaymentViewModel = bottomSheetPaymentViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState

    val isInitializing by viewModel.isInitializing.collectAsState()
    val token by viewModel.token.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var allowBlankName by rememberSaveable { mutableStateOf(false) }
    var allowBlankDate by rememberSaveable { mutableStateOf(false) }
    var allowExpiredDate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(allowBlankName) {
        if (!isInitializing) {
            try {
                sdk.setParam(ValidationParameter.ALLOW_BLANK_NAME, allowBlankName)
            } catch (e: IllegalStateException) {
                // SDK not ready yet
            }
        }
    }

    LaunchedEffect(allowBlankDate) {
        if (!isInitializing) {
            try {
                sdk.setParam(ValidationParameter.ALLOW_BLANK_DATE, allowBlankDate)
            } catch (e: IllegalStateException) {
                // SDK not ready yet
            }
        }
    }

    LaunchedEffect(allowExpiredDate) {
        if (!isInitializing) {
            try {
                sdk.setParam(ValidationParameter.ALLOW_EXPIRED_DATE, allowExpiredDate)
            } catch (e: IllegalStateException) {
                // SDK not ready yet
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.Top,
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
                text = "Payment SDK Demo",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Test your payment integration",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 48.dp),
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
                    // Checkout Button
                    val isButtonDisabled = isInitializing || isProcessing
                    Button(
                        onClick = {
                            if (!isButtonDisabled) {
                                viewModel.expressCheckout()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            ),
                        enabled = !isButtonDisabled,
                    ) {
                        if (isButtonDisabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text =
                                        when {
                                            isInitializing -> "Initializing..."
                                            isProcessing -> "Processing..."
                                            else -> "Express Checkout"
                                        },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Checkout",
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Express Checkout",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Token Display
                    if (token.isNotEmpty() && token != "Spreedly") {
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Payment Token Generated",
                                        style =
                                            MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                    )
                                }

                                SelectionContainer {
                                    Text(
                                        text = token,
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(4.dp),
                                                ).padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Validation Toggles Section
            Text(
                text = "Validation Options",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Allow Blank Name Toggle
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { allowBlankName = !allowBlankName },
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
                            text = "Skip name field validation when empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowBlankName,
                        onCheckedChange = { allowBlankName = it },
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { allowBlankDate = !allowBlankDate },
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
                        onCheckedChange = { allowBlankDate = it },
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { allowExpiredDate = !allowExpiredDate },
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
                            text = "Allow expired card dates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowExpiredDate,
                        onCheckedChange = { allowExpiredDate = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

        val customFields = listOf(
            ConfigurableFormField(type = OptionalFieldType.ADDRESS_LINE_1, isRequired = true),
            ConfigurableFormField(type = OptionalFieldType.CITY, isRequired = false),
            ConfigurableFormField(type = OptionalFieldType.STATE, isRequired = true),
        )

        SpreedlyBottomSheet(
            sdk = sdk,
            config = PaymentSheetConfig.Default,
            nameFieldDisplayMode = NameFieldDisplayMode.SEPARATE_FIELDS,
            allowBlankName = allowBlankName,
            allowBlankDate = allowBlankDate,
            allowExpiredDate = allowExpiredDate,
            yearFormat = YearFormat.MM_YY,
            additionalFields = customFields,
        )
    }
}
