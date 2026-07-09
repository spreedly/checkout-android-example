package com.spreedly.example.screens.stripeapmpayment

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreedly.example.screens.common.PaymentErrorCard
import com.spreedly.example.screens.common.PaymentProductGrid
import com.spreedly.example.screens.common.PaymentProviderMultiSelector
import com.spreedly.example.screens.common.PaymentStageIndicator
import com.spreedly.example.screens.common.PaymentSuccessCard
import com.spreedly.example.viewmodel.stripeAPMPaymentViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Screen demonstrating the Stripe APM offsite payment flow (iDEAL, Bancontact, SEPA).
 *
 * Uses a 3-stage flow: Idle -> Purchase -> Checkout (no tokenization).
 * Presents the Stripe PaymentSheet with the client_secret from a pending purchase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun StripeAPMPaymentScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: StripeAPMPaymentViewModel = stripeAPMPaymentViewModel()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedApmTypes by viewModel.selectedApmTypes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val radarEnabled by viewModel.radarEnabled.collectAsStateWithLifecycle()
    val radarState by viewModel.radarState.collectAsStateWithLifecycle()
    val radarSessionId by viewModel.radarSessionId.collectAsStateWithLifecycle()

    val controlsEnabled = stage == StripeAPMPaymentViewModel.Stage.IDLE

    var useCustomAppearance by rememberSaveable { mutableStateOf(true) }
    var appearancePrimaryArgb by rememberSaveable {
        mutableIntStateOf(DefaultStripeApmPrimaryColor.toArgb())
    }
    var appearanceBackgroundArgb by rememberSaveable {
        mutableIntStateOf(DefaultStripeApmBackgroundColor.toArgb())
    }
    var appearanceButtonBackgroundArgb by rememberSaveable {
        mutableIntStateOf(DefaultStripeApmPrimaryColor.toArgb())
    }
    var appearanceButtonTextArgb by rememberSaveable {
        mutableIntStateOf(Color.White.toArgb())
    }
    var appearanceCornerRadius by rememberSaveable { mutableFloatStateOf(10f) }

    // onResume handling for Stripe APM checkout is done in MainActivity.onResume() via
    // SpreedlyStripeAPMCheckout.finalizeIfActive() to avoid LifecycleOwner registration
    // when the Activity is already RESUMED (which can occur when navigating to this screen).

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stripe APM Payment") },
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
                        stageLabels = listOf("Idle", "Purchase", "Checkout"),
                        currentIndex = when (stage) {
                            StripeAPMPaymentViewModel.Stage.IDLE -> 0
                            StripeAPMPaymentViewModel.Stage.PURCHASING -> 1
                            StripeAPMPaymentViewModel.Stage.CHECKOUT -> 2
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RadarToggleRow(
                        enabled = radarEnabled,
                        radarState = radarState,
                        radarSessionId = radarSessionId,
                        onToggle = viewModel::toggleRadar,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Payment Methods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentProviderMultiSelector(
                        providers = viewModel.apmTypesList,
                        selectedProviders = selectedApmTypes,
                        getLabel = { it.displayName },
                        onProviderToggled = viewModel::toggleApmType,
                        enabled = controlsEnabled,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    StripeAPMAppearanceSection(
                        useCustomAppearance = useCustomAppearance,
                        onUseCustomAppearanceChange = { useCustomAppearance = it },
                        primaryColor = Color(appearancePrimaryArgb),
                        onPrimaryColorChange = { appearancePrimaryArgb = it.toArgb() },
                        backgroundColor = Color(appearanceBackgroundArgb),
                        onBackgroundColorChange = { appearanceBackgroundArgb = it.toArgb() },
                        buttonBackgroundColor = Color(appearanceButtonBackgroundArgb),
                        onButtonBackgroundColorChange = { appearanceButtonBackgroundArgb = it.toArgb() },
                        buttonTextColor = Color(appearanceButtonTextArgb),
                        onButtonTextColorChange = { appearanceButtonTextArgb = it.toArgb() },
                        cornerRadiusDp = appearanceCornerRadius,
                        onCornerRadiusDpChange = { appearanceCornerRadius = it },
                        enabled = controlsEnabled,
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
                        enabled = stage == StripeAPMPaymentViewModel.Stage.IDLE,
                        formatPrice = { cents -> formatEurPrice(cents) },
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
                                val appearance = buildStripeAppearanceConfig(
                                    useCustomAppearance = useCustomAppearance,
                                    primaryColor = Color(appearancePrimaryArgb),
                                    backgroundColor = Color(appearanceBackgroundArgb),
                                    buttonBackgroundColor = Color(appearanceButtonBackgroundArgb),
                                    buttonTextColor = Color(appearanceButtonTextArgb),
                                    cornerRadiusDp = appearanceCornerRadius,
                                )
                                viewModel.startPayment(activity, appearance)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isInitializing &&
                            selectedProduct != null &&
                            selectedApmTypes.isNotEmpty() &&
                            stage == StripeAPMPaymentViewModel.Stage.IDLE,
                    ) {
                        when (stage) {
                            StripeAPMPaymentViewModel.Stage.IDLE -> {
                                val product = selectedProduct
                                Text(
                                    text = when {
                                        isInitializing -> "Initializing..."
                                        product != null -> "Pay ${formatEurPrice(product.price)}"
                                        else -> "Select a Product"
                                    },
                                    fontSize = 18.sp,
                                )
                            }

                            StripeAPMPaymentViewModel.Stage.PURCHASING -> {
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

                            StripeAPMPaymentViewModel.Stage.CHECKOUT -> {
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

@Composable
private fun RadarToggleRow(
    enabled: Boolean,
    radarState: StripeAPMPaymentViewModel.RadarState,
    radarSessionId: String?,
    onToggle: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Stripe Radar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }

        if (enabled) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (radarState) {
                    StripeAPMPaymentViewModel.RadarState.IDLE -> {}
                    StripeAPMPaymentViewModel.RadarState.COLLECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Collecting device data...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StripeAPMPaymentViewModel.RadarState.SUCCESS -> {
                        Text(
                            text = radarSessionId ?: "",
                            style = MaterialTheme.typography.labelSmall
                                .copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StripeAPMPaymentViewModel.RadarState.FAILED -> {
                        Text(
                            text = "Collection failed -- payment will proceed without Radar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatEurPrice(cents: Int): String {
    val amount = cents / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    format.currency = Currency.getInstance("EUR")
    return format.format(amount)
}
