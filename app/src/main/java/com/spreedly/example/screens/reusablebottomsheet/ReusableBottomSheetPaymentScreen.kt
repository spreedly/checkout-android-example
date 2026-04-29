package com.spreedly.example.screens.reusablebottomsheet

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.reusableBottomSheetPaymentViewModel
import com.spreedly.paymentsheet.SpreedlyBottomSheet
import com.spreedly.sdk.ui.ConfigurableFormField
import com.spreedly.sdk.ui.NameFieldDisplayMode
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentSheetConfig
import com.spreedly.sdk.ui.YearFormat
import com.spreedly.validation.ValidationParameter

private const val TAG = "ReusableBottomSheetPaymentScreen"

@SuppressLint("ComposeModifierMissing")
@Composable
fun ReusableBottomSheetPaymentScreen(
    viewModel: ReusableBottomSheetPaymentViewModel = reusableBottomSheetPaymentViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState

    val isInitializing by viewModel.isInitializing.collectAsState()
    val token by viewModel.token.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentCount by viewModel.paymentCount.collectAsState()
    val tokenHistory by viewModel.tokenHistory.collectAsState()

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

    // Detect configuration changes and preserve payment state
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sdk) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // Activity is being stopped (likely for configuration change)
                    // Preserve payment state if bottom sheet is open
                    if (sdk.showBottomSheet.value) {
                        viewModel.onConfigurationChanging()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    // Activity restarted after configuration change
                    viewModel.onConfigurationChangeComplete()
                }
                else -> { /* Ignore other events */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reusable Payment",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = "Reusable Payment Sheet",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Make multiple payments with fresh initialization",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Payment Counter
            if (paymentCount > 0) {
                Card(
                    modifier = Modifier.padding(bottom = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Completed Payments: $paymentCount",
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

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
                    // New Payment Button
                    val isButtonDisabled = isInitializing || isProcessing

                    if (isProcessing) {
                        // Show processing state with cancel option
                        Column {
                            Button(
                                onClick = { /* Processing, disabled */ },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    ),
                                enabled = false,
                            ) {
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
                                        text = "Processing Payment...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cancel button when processing
                            OutlinedButton(
                                onClick = { viewModel.cancelPayment() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Text(
                                    "Cancel Payment",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        // Normal payment button
                        Button(
                            onClick = {
                                if (!isButtonDisabled) {
                                    Log.d(
                                        TAG,
                                        "Starting new payment, SDK state: ${viewModel.getSDKState()}",
                                    )
                                    viewModel.startNewPayment()
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.4f),
                                ),
                            enabled = !isButtonDisabled,
                        ) {
                            if (isInitializing) {
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
                                        text = "Initializing...",
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
                                        contentDescription = "New Payment",
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (paymentCount == 0) "Start Payment" else "Start New Payment",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    // Reset Counter Button
                    if (paymentCount > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetPaymentCounter() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Counter",
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Reset Counter",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.forceReset() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            ) {
                                Text(
                                    "Force Reset",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        // Show Force Reset button even when no payments completed, in case of stuck state
                        Spacer(modifier = Modifier.height(16.dp))
                        Column {
                            OutlinedButton(
                                onClick = { viewModel.forceReset() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            ) {
                                Text(
                                    "Force Reset (if stuck)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Debug buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.testManualPaymentSuccess() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.tertiary,
                                        ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                                ) {
                                    Text(
                                        "Test Success",
                                        fontSize = 11.sp,
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.testManualCloseBottomSheet() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                                ) {
                                    Text(
                                        "Test Close",
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Token Display
                    if (token.isNotEmpty() && token != "Spreedly") {
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                CardDefaults.cardColors(
                                    // Light green background
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary), // Green border
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Payment Token Generated",
                                        style =
                                            MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            ),
                                    )
                                }

                                Text(
                                    "Payment #$paymentCount successful! Token ready for processing:",
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )

                                SelectionContainer {
                                    Text(
                                        text = token,
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                            ),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(8.dp),
                                                ).padding(16.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "💡 Tap the token above to copy it",
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        ),
                                )
                            }
                        }
                    }

                    // Token History
                    if (tokenHistory.size > 1) {
                        Spacer(modifier = Modifier.height(16.dp))

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
                                Text(
                                    "Payment History (${tokenHistory.size} tokens)",
                                    style =
                                        MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )

                                tokenHistory.forEachIndexed { index, historyToken ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "#${index + 1}",
                                            style =
                                                MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        .copy(alpha = 0.7f),
                                                ),
                                            modifier = Modifier.width(24.dp),
                                        )

                                        SelectionContainer {
                                            Text(
                                                text = historyToken,
                                                style =
                                                    MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 11.sp,
                                                    ),
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .background(
                                                            MaterialTheme.colorScheme.surface,
                                                            RoundedCornerShape(4.dp),
                                                        ).padding(8.dp),
                                            )
                                        }
                                    }
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

            // Emergency close button if bottom sheet is stuck open
            if (sdk.showBottomSheet.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "⚠️ Bottom Sheet Open",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                            ),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.testManualCloseBottomSheet() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "Force Close Bottom Sheet",
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Info Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Secure",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Fresh authentication for each payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reusable",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Reusable for multiple payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Bottom Sheet with custom configuration
        val customFields = listOf(
            ConfigurableFormField(type = OptionalFieldType.ADDRESS_LINE_1, isRequired = true),
            ConfigurableFormField(type = OptionalFieldType.CITY, isRequired = false),
            ConfigurableFormField(type = OptionalFieldType.STATE, isRequired = true),
        )

        SpreedlyBottomSheet(
            sdk = sdk,
            config = PaymentSheetConfig(
                primaryColor = MaterialTheme.colorScheme.tertiary,
                fieldBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                formBackgroundColor = MaterialTheme.colorScheme.surface,
            ),
            nameFieldDisplayMode = NameFieldDisplayMode.SEPARATE_FIELDS,
            allowBlankName = allowBlankName,
            allowBlankDate = allowBlankDate,
            allowExpiredDate = allowExpiredDate,
            yearFormat = YearFormat.MM_YY,
            additionalFields = customFields,
        )
    }
}
