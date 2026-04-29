package com.spreedly.example.screens.braintreepayment

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
import com.spreedly.braintree.BraintreeAPMPaymentType
import com.spreedly.example.screens.common.PaymentErrorCard
import com.spreedly.example.screens.common.PaymentProductGrid
import com.spreedly.example.screens.common.PaymentProviderSelector
import com.spreedly.example.screens.common.PaymentStageIndicator
import com.spreedly.example.screens.common.PaymentSuccessCard
import com.spreedly.example.viewmodel.braintreePaymentViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen demonstrating the Braintree PayPal/Venmo payment flow.
 *
 * This screen allows users to:
 * 1. Select a product from a grid
 * 2. Choose a payment type (PayPal or Venmo)
 * 3. Start the Braintree payment flow
 * 4. See success/error messages after nonce confirmation
 *
 * ## Flow
 *
 * When the user taps "Start Payment":
 * 1. App creates a pending purchase on the Braintree gateway
 * 2. SDK fetches client_token from transaction status
 * 3. SDK launches native PayPal/Venmo flow via BraintreeActivity
 * 4. User completes payment in PayPal/Venmo
 * 5. App receives nonce via paymentResultFlow
 * 6. App confirms transaction by sending nonce to backend
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun BraintreePaymentScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BraintreePaymentViewModel = braintreePaymentViewModel()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedPaymentType by viewModel.selectedPaymentType.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Braintree Payment") },
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
                    PaymentStageIndicator(
                        stageLabels = BRAINTREE_STAGE_LABELS,
                        currentIndex = braintreeStageToIndex(stage),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Payment Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentProviderSelector(
                        providers = viewModel.paymentTypes.toList(),
                        selectedProvider = selectedPaymentType,
                        getLabel = { type ->
                            when (type) {
                                BraintreeAPMPaymentType.PAYPAL -> "PayPal"
                                BraintreeAPMPaymentType.VENMO -> "Venmo"
                            }
                        },
                        onProviderSelected = viewModel::selectPaymentType,
                        enabled = stage == BraintreePaymentViewModel.Stage.IDLE,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
                        enabled = stage == BraintreePaymentViewModel.Stage.IDLE,
                        formatPrice = ::formatPrice,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    errorMessage?.let { error ->
                        PaymentErrorCard(message = error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    successMessage?.let { success ->
                        PaymentSuccessCard(message = success)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

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
                        enabled = selectedProduct != null &&
                            stage == BraintreePaymentViewModel.Stage.IDLE,
                    ) {
                        when (stage) {
                            BraintreePaymentViewModel.Stage.IDLE -> {
                                Text(
                                    text = if (selectedProduct != null) {
                                        "Pay ${formatPrice(selectedProduct!!.price)}"
                                    } else {
                                        "Select a Product"
                                    },
                                    fontSize = 18.sp,
                                )
                            }

                            BraintreePaymentViewModel.Stage.PURCHASING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "  Creating purchase...",
                                    fontSize = 16.sp,
                                )
                            }

                            BraintreePaymentViewModel.Stage.CHECKOUT -> {
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

                            BraintreePaymentViewModel.Stage.CONFIRMING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "  Confirming payment...",
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

private val BRAINTREE_STAGE_LABELS = listOf("Idle", "Purchase", "Checkout", "Confirm")

private fun braintreeStageToIndex(stage: BraintreePaymentViewModel.Stage): Int = when (stage) {
    BraintreePaymentViewModel.Stage.IDLE -> 0
    BraintreePaymentViewModel.Stage.PURCHASING -> 1
    BraintreePaymentViewModel.Stage.CHECKOUT -> 2
    BraintreePaymentViewModel.Stage.CONFIRMING -> 3
}

private fun formatPrice(cents: Int): String {
    val dollars = cents / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(dollars)
}
