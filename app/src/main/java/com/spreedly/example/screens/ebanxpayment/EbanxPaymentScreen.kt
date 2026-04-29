package com.spreedly.example.screens.ebanxpayment

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreedly.example.screens.common.PaymentErrorCard
import com.spreedly.example.screens.common.PaymentProductGrid
import com.spreedly.example.screens.common.PaymentProviderSelector
import com.spreedly.example.screens.common.PaymentStageIndicator
import com.spreedly.example.screens.common.PaymentSuccessCard
import com.spreedly.example.screens.offsitepayment.BaseOffsitePaymentViewModel
import com.spreedly.example.ui.theme.SpreedlyExampleTheme
import com.spreedly.example.viewmodel.ebanxPaymentViewModel
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Screen demonstrating the EBANX payment flow (Pix, Boleto, OXXO, NuPay).
 *
 * This screen allows users to:
 * 1. Select a product from a grid
 * 2. Choose an EBANX payment provider
 * 3. Start the offsite payment flow
 * 4. See success/error messages (including pending state for offline payments)
 *
 * ## EBANX-Specific Behavior
 *
 * - **Currency**: OXXO uses MXN (Mexican Peso), all others use BRL (Brazilian Real)
 * - **Pending state**: Shown as success for offline payment methods (Boleto, OXXO)
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun EbanxPaymentScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: EbanxPaymentViewModel = ebanxPaymentViewModel()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    // onResume handling for EBANX checkout is done in MainActivity.onResume() via
    // SpreedlyOffsiteCheckout.finalizeIfActive() to avoid LifecycleOwner registration
    // when the Activity is already RESUMED.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EBANX Payment") },
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
                        stageLabels = EBANX_STAGE_LABELS,
                        currentIndex = ebanxStageToIndex(stage),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                                OffsitePaymentMethodType.PIX -> "Pix"
                                OffsitePaymentMethodType.BOLETO_BANCARIO -> "Boleto"
                                OffsitePaymentMethodType.OXXO -> "OXXO"
                                OffsitePaymentMethodType.NUPAY -> "NuPay"
                                else -> provider.rawValue
                            }
                        },
                        onProviderSelected = viewModel::selectProvider,
                        enabled = stage == BaseOffsitePaymentViewModel.Stage.IDLE,
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
                        enabled = stage == BaseOffsitePaymentViewModel.Stage.IDLE,
                        formatPrice = { formatPriceByCurrency(it, EbanxConfigBuilder.currencyCode(selectedProvider)) },
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
                        enabled = selectedProduct != null && stage == BaseOffsitePaymentViewModel.Stage.IDLE,
                    ) {
                        when (stage) {
                            BaseOffsitePaymentViewModel.Stage.IDLE -> {
                                Text(
                                    text = selectedProduct?.let { product ->
                                        "Pay ${formatEbanxPrice(product.price, selectedProvider)}"
                                    } ?: "Select a Product",
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

private val EBANX_STAGE_LABELS = listOf("Idle", "Tokenize", "Purchase", "Checkout")

private fun ebanxStageToIndex(stage: BaseOffsitePaymentViewModel.Stage): Int = when (stage) {
    BaseOffsitePaymentViewModel.Stage.IDLE -> 0
    BaseOffsitePaymentViewModel.Stage.CREATING_PAYMENT_METHOD -> 1
    BaseOffsitePaymentViewModel.Stage.PURCHASING -> 2
    BaseOffsitePaymentViewModel.Stage.CHECKOUT -> 3
}

/** Format price for EBANX payment methods with appropriate currency. */
private fun formatEbanxPrice(cents: Int, provider: OffsitePaymentMethodType): String {
    val currencyCode = EbanxConfigBuilder.currencyCode(provider)
    return formatPriceByCurrency(cents, currencyCode)
}

/**
 * Format price with the specified currency code.
 *
 * MXN special handling: the `es-MX` locale formats MXN with a bare "$" symbol,
 * identical to USD. We prefix with "MX" so it renders as "MX$" to avoid ambiguity
 * in this international-audience demo app.
 */
private fun formatPriceByCurrency(cents: Int, currencyCode: String): String {
    val amount = cents / 100.0
    return try {
        val currency = Currency.getInstance(currencyCode)
        val locale = when (currencyCode) {
            "BRL" -> Locale.forLanguageTag("pt-BR")
            "MXN" -> Locale.forLanguageTag("es-MX")
            else -> Locale.US
        }
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = currency
        val formatted = format.format(amount)
        if (currencyCode == "MXN" && formatted.startsWith("$")) {
            "MX$formatted"
        } else {
            formatted
        }
    } catch (e: Exception) {
        "$currencyCode ${String.format("%.2f", amount)}"
    }
}

@Preview(showBackground = true)
@Composable
private fun EbanxPaymentScreenPreview() {
    SpreedlyExampleTheme {
        Surface {
            EbanxPaymentScreen(onBackClick = {})
        }
    }
}
