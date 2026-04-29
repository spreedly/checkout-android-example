package com.spreedly.example.screens.offsitepayment

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreedly.example.screens.common.PaymentErrorCard
import com.spreedly.example.screens.common.PaymentProductGrid
import com.spreedly.example.screens.common.PaymentProviderSelector
import com.spreedly.example.screens.common.PaymentStageIndicator
import com.spreedly.example.screens.common.PaymentSuccessCard
import com.spreedly.example.viewmodel.offsitePaymentViewModel
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen demonstrating the offsite payment flow (PayPal, Sprel, etc.).
 *
 * This screen allows users to:
 * 1. Select a product from a grid
 * 2. Choose a payment provider (Sprel or PayPal)
 * 3. Start the offsite payment flow
 * 4. See success/error messages
 *
 * ## Flow
 *
 * When the user taps "Start Payment":
 * 1. SDK creates an offsite payment method token
 * 2. App calls purchase API with the token
 * 3. Checkout presenter launches Chrome Custom Tab
 * 4. User completes payment on provider's page
 * 5. App receives result via paymentResultFlow
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun OffsitePaymentScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: OffsitePaymentViewModel = offsitePaymentViewModel()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    // onResume handling for offsite checkout is done in MainActivity.onResume() via
    // SpreedlyOffsiteCheckout.finalizeIfActive() to avoid LifecycleOwner registration
    // when the Activity is already RESUMED.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offsite Payment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(viewModel.snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isInitializing) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Initializing SDK...")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Stage indicator
                    PaymentStageIndicator(
                        stageLabels = OFFSITE_STAGE_LABELS,
                        currentIndex = offsiteStageToIndex(stage),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Provider selection
                    Text(
                        text = "Select Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentProviderSelector(
                        providers = viewModel.providers,
                        selectedProvider = selectedProvider,
                        getLabel = { provider ->
                            when (provider) {
                                OffsitePaymentMethodType.SPREL -> "Sprel (Test)"
                                OffsitePaymentMethodType.PAYPAL -> "PayPal"
                                else -> provider.rawValue
                            }
                        },
                        onProviderSelected = viewModel::selectProvider,
                        enabled = stage == BaseOffsitePaymentViewModel.Stage.IDLE,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Product selection
                    Text(
                        text = "Select Product",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentProductGrid(
                        products = viewModel.products,
                        selectedProduct = selectedProduct,
                        onProductSelected = viewModel::selectProduct,
                        enabled = stage == BaseOffsitePaymentViewModel.Stage.IDLE,
                        formatPrice = ::formatPrice,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error message
                    errorMessage?.let { error ->
                        PaymentErrorCard(message = error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Success message
                    successMessage?.let { success ->
                        PaymentSuccessCard(message = success)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Start button
                    Button(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null) {
                                viewModel.startPayment(activity)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedProduct != null && stage == BaseOffsitePaymentViewModel.Stage.IDLE,
                    ) {
                        when (stage) {
                            BaseOffsitePaymentViewModel.Stage.IDLE -> {
                                Text(
                                    text = if (selectedProduct != null) {
                                        "Pay ${formatPrice(selectedProduct!!.price)}"
                                    } else {
                                        "Select a Product"
                                    },
                                    fontSize = 18.sp,
                                )
                            }

                            BaseOffsitePaymentViewModel.Stage.CREATING_PAYMENT_METHOD -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "  Creating payment method...",
                                    fontSize = 16.sp,
                                )
                            }

                            BaseOffsitePaymentViewModel.Stage.PURCHASING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "  Processing purchase...",
                                    fontSize = 16.sp,
                                )
                            }

                            BaseOffsitePaymentViewModel.Stage.CHECKOUT -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "  Waiting for checkout...",
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val OFFSITE_STAGE_LABELS = listOf("Idle", "Tokenize", "Purchase", "Checkout")

private fun offsiteStageToIndex(stage: BaseOffsitePaymentViewModel.Stage): Int = when (stage) {
    BaseOffsitePaymentViewModel.Stage.IDLE -> 0
    BaseOffsitePaymentViewModel.Stage.CREATING_PAYMENT_METHOD -> 1
    BaseOffsitePaymentViewModel.Stage.PURCHASING -> 2
    BaseOffsitePaymentViewModel.Stage.CHECKOUT -> 3
}

private fun formatPrice(cents: Int): String {
    val dollars = cents / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(dollars)
}
