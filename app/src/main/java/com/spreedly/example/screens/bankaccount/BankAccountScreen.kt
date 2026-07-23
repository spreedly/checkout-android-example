package com.spreedly.example.screens.bankaccount

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.spreedly.example.viewmodel.findActivityOrNull
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spreedly.app.R
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.bankAccountViewModel
import com.spreedly.example.screens.common.BankAccountConfigPanel
import com.spreedly.paymentsheet.SpreedlyBankAccountBottomSheet
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.BankAccountFieldConfig
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.security.secureScreen
import kotlinx.coroutines.launch

@SuppressLint("ComposeModifierMissing")
@Composable
fun BankAccountScreen(
    viewModel: BankAccountViewModel = bankAccountViewModel(),
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val coroutineScope = rememberCoroutineScope()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    val showSheet by viewModel.showSheet.collectAsState()
    val fieldConfig by viewModel.fieldConfig.collectAsState()
    val uiConfig by viewModel.uiConfig.collectAsState()
    val useCustomTheme by viewModel.useCustomTheme.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current.findActivityOrNull()
    DisposableEffect(lifecycleOwner, showSheet) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (showSheet && activity?.isChangingConfigurations == true) {
                        viewModel.onConfigurationChanging()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    viewModel.onConfigurationChangeComplete()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isError = data.visuals.withDismissAction
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        SnackbarDefaults.color
                    },
                    contentColor = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        SnackbarDefaults.contentColor
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .secureScreen()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = Spacing.lg),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = stringResource(R.string.bank_account_screen_title),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = stringResource(R.string.bank_account_screen_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.padding(bottom = Spacing.xs),
            )

            Text(
                text = stringResource(R.string.bank_account_screen_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xxxl),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BankAccountConfigPanel(
                        fieldConfig = fieldConfig,
                        onFieldConfigChange = { viewModel.updateFieldConfig(it) },
                        uiConfig = uiConfig,
                        onUiConfigChange = { viewModel.updateUiConfig(it) },
                        useCustomTheme = useCustomTheme,
                        onUseCustomThemeChange = { viewModel.updateUseCustomTheme(it) },
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    val isButtonDisabled = isInitializing || isProcessing
                    Button(
                        onClick = { viewModel.showBankAccountSheet() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                        enabled = !isButtonDisabled,
                    ) {
                        if (isButtonDisabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isInitializing -> stringResource(R.string.bank_account_initializing)
                                        isProcessing -> stringResource(R.string.bank_account_processing)
                                        else -> stringResource(R.string.bank_account_add_button)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.bank_account_add_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (paymentToken.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.lg))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = Spacing.xs),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.success),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.payment_token_generated),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }

                                SelectionContainer {
                                    Text(
                                        text = paymentToken,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.sm),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = stringResource(R.string.secure),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.bank_account_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val paymentFailedMsg = stringResource(R.string.bank_account_error)
        SpreedlyBankAccountBottomSheet(
            sdk = sdk,
            show = showSheet,
            onDismiss = { viewModel.dismissSheet() },
            fieldConfig = fieldConfig,
            customFieldsConfig = uiConfig,
            formFields = listOf(
                FormFieldType.ROUTING_NUMBER(required = true),
                FormFieldType.ACCOUNT_NUMBER(required = true),
            ),
            onPaymentResult = { result ->
                if (result is PaymentResult.Failed) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = paymentFailedMsg,
                            withDismissAction = true,
                        )
                    }
                }
            },
        )
    }
}
