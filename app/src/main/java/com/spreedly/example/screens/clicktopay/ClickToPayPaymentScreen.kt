package com.spreedly.example.screens.clicktopay

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreedly.clicktopay.ClickToPayCheckoutConfig
import com.spreedly.clicktopay.ClickToPayButtonConfig
import com.spreedly.clicktopay.ui.ClickToPayBrandColors
import com.spreedly.clicktopay.ui.ClickToPaySavedCardsDetector
import com.spreedly.clicktopay.ui.SpreedlyClickToPayButton
import com.spreedly.example.screens.basiccheckout.SimpleInputField
import com.spreedly.example.screens.common.PaymentErrorCard
import com.spreedly.example.screens.common.PaymentProductGrid
import com.spreedly.example.screens.common.PaymentStageIndicator
import com.spreedly.example.screens.common.PaymentSuccessCard
import com.spreedly.example.ui.components.ThemeConfigurationCard
import com.spreedly.example.ui.components.ThemeConfigurationStyle
import com.spreedly.example.viewmodel.clickToPayPaymentViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun ClickToPayPaymentScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ClickToPayPaymentViewModel = clickToPayPaymentViewModel()

    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val doLookup by viewModel.doLookup.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val emailError by viewModel.emailError.collectAsStateWithLifecycle()
    val phoneError by viewModel.phoneError.collectAsStateWithLifecycle()
    val deviceRecognition by viewModel.deviceRecognition.collectAsStateWithLifecycle()
    val recognizedCardLabels by viewModel.recognizedCardLabels.collectAsStateWithLifecycle()
    val savedCardsDetectorKey by viewModel.savedCardsDetectorKey.collectAsStateWithLifecycle()
    val merchantPrefill by viewModel.merchantPrefill.collectAsStateWithLifecycle()
    val firstNameError by viewModel.firstNameError.collectAsStateWithLifecycle()
    val lastNameError by viewModel.lastNameError.collectAsStateWithLifecycle()
    val flowPhase by viewModel.flowPhase.collectAsStateWithLifecycle()
    val eventLog by viewModel.eventLog.collectAsStateWithLifecycle()
    val useCustomTheme by viewModel.useCustomTheme.collectAsStateWithLifecycle()
    val selectedThemePreset by viewModel.selectedThemePreset.collectAsStateWithLifecycle()
    val isDarkMode = isSystemInDarkTheme()

    LaunchedEffect(useCustomTheme, selectedThemePreset, isDarkMode) {
        viewModel.applyThemeToSdk(isDarkMode)
    }

    LaunchedEffect(isInitializing) {
        if (!isInitializing) {
            viewModel.onMerchantScreenDisplayed()
        }
    }

    var devOptionsExpanded by remember { mutableStateOf(false) }
    var emailHasEverChanged by remember { mutableStateOf(false) }
    var emailWasFocused by remember { mutableStateOf(false) }

    val checkoutEnabled = stage == ClickToPayPaymentViewModel.Stage.IDLE
    val canStartPayment =
        ClickToPayPaymentViewModel.canStartPayment(
            stage = stage,
            selectedProduct = selectedProduct,
            deviceRecognition = deviceRecognition,
        )
    val contactHint =
        when (deviceRecognition) {
            DeviceRecognitionState.Checking ->
                "Checking whether this device has saved Click to Pay cards…"
            DeviceRecognitionState.Recognized ->
                "Your saved cards are ready. Tap Click to Pay to continue."
            DeviceRecognitionState.DetectionFailed ->
                "We could not check for saved cards on this device. Enter your email or phone to continue."
            DeviceRecognitionState.UsingDifferentEmail ->
                ClickToPaySandboxCatalog.MERCHANT_PREFILL_HINT
            DeviceRecognitionState.NotRecognized ->
                ClickToPaySandboxCatalog.MERCHANT_PREFILL_HINT
        }

    val showContactIdentityFields =
        deviceRecognition == DeviceRecognitionState.NotRecognized ||
            deviceRecognition == DeviceRecognitionState.DetectionFailed ||
            deviceRecognition == DeviceRecognitionState.UsingDifferentEmail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Click to Pay") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
    ) { paddingValues ->
        if (isInitializing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (savedCardsDetectorKey >= 0) {
                ClickToPaySavedCardsDetector(
                    config = viewModel.savedCardsDetectorConfig,
                    detectorKey = savedCardsDetectorKey,
                    modifier = Modifier.size(0.dp),
                    onResult = viewModel::onSavedCardsDetectorResult,
                    onControllerReady = viewModel::onSavedCardsDetectorControllerReady,
                    onDetectorDisposed = viewModel::onSavedCardsDetectorDisposed,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                PaymentStageIndicator(
                    stageLabels = C2P_STAGE_LABELS,
                    currentIndex = c2pStageToIndex(stage),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SDK phase: $flowPhase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. Select product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                PaymentProductGrid(
                    products = viewModel.products,
                    selectedProduct = selectedProduct,
                    onProductSelected = viewModel::selectProduct,
                    enabled = checkoutEnabled,
                    formatPrice = ::formatPrice,
                )

                Spacer(modifier = Modifier.height(20.dp))

                ThemeConfigurationCard(
                    useCustomTheme = useCustomTheme,
                    selectedPreset = selectedThemePreset,
                    onUseCustomThemeChange = viewModel::setUseCustomTheme,
                    onPresetSelected = viewModel::setThemePreset,
                    onResetTheme = viewModel::resetThemeConfiguration,
                    style = ThemeConfigurationStyle.SWATCH,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "2. Contact information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = contactHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ClickToPayContactSection(
                    email = email,
                    emailError = emailError,
                    phoneError = phoneError,
                    deviceRecognition = deviceRecognition,
                    recognizedCardLabels = recognizedCardLabels,
                    phoneCountryCode = merchantPrefill.phoneCountryCode,
                    phoneNumber = merchantPrefill.phoneNumber,
                    enabled = checkoutEnabled,
                    showIdentityFields = showContactIdentityFields,
                    onEmailChange = { value ->
                        if (value.isNotEmpty()) emailHasEverChanged = true
                        viewModel.updateEmail(value)
                        if (emailHasEverChanged) {
                            viewModel.validateCustomerIdentity()
                        } else {
                            viewModel.clearEmailError()
                            viewModel.clearPhoneError()
                        }
                    },
                    onEmailFocus = { focused ->
                        if (focused) {
                            emailWasFocused = true
                        } else if (emailWasFocused) {
                            viewModel.validateCustomerIdentity()
                        }
                    },
                    onUseDifferentEmail = viewModel::useDifferentEmail,
                    onPhoneCountryCodeChange = viewModel::updatePhoneCountryCode,
                    onPhoneNumberChange = viewModel::updatePhoneNumber,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "3. Billing / shipping address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "First and last name are required for tokenize and prefilled as cardholder name in the Click to Pay sheet. Other address fields are optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ClickToPayBillingPrefillSection(
                    prefill = merchantPrefill,
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    enabled = checkoutEnabled,
                    onFirstNameChange = viewModel::updateFirstName,
                    onLastNameChange = viewModel::updateLastName,
                    onAddressLine1Change = viewModel::updateAddressLine1,
                    onAddressLine2Change = viewModel::updateAddressLine2,
                    onCountryChange = viewModel::updateCountry,
                    onCityChange = viewModel::updateCity,
                    onStateChange = viewModel::updateState,
                    onZipChange = viewModel::updateZip,
                    onCopyBillingToShippingChange = viewModel::setCopyBillingToShipping,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Card details are collected inside Click to Pay checkout (not on this screen).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { devOptionsExpanded = !devOptionsExpanded }) {
                    Text(if (devOptionsExpanded) "Hide developer options" else "Show developer options")
                }

                if (devOptionsExpanded) {
                    ClickToPayDeveloperOptionsSection(
                        doLookup = doLookup,
                        eventLog = eventLog,
                        enabled = checkoutEnabled,
                        onDoLookupChange = viewModel::setDoLookup,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let { error ->
                    PaymentErrorCard(message = error)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                successMessage?.let { success ->
                    PaymentSuccessCard(message = success)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (stage) {
                    ClickToPayPaymentViewModel.Stage.IDLE -> {
                        val checkoutConfig: ClickToPayCheckoutConfig? =
                            remember(
                                selectedProduct,
                                email,
                                doLookup,
                                merchantPrefill,
                            ) {
                                viewModel.merchantCheckoutConfig()
                            }
                        if (checkoutConfig != null) {
                            SpreedlyClickToPayButton(
                                checkoutConfig = checkoutConfig,
                                buttonConfig =
                                    ClickToPayButtonConfig(
                                        isEnabled = canStartPayment,
                                        isDark = isDarkMode,
                                    ),
                                prepareForPresentation = { viewModel.prepareForCheckout(isDarkMode) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (selectedProduct == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select a product to enable Click to Pay",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ClickToPayPaymentViewModel.Stage.CHECKOUT,
                    ClickToPayPaymentViewModel.Stage.TOKENIZING,
                    -> {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text =
                                    if (stage == ClickToPayPaymentViewModel.Stage.CHECKOUT) {
                                        "Click to Pay checkout open…"
                                    } else {
                                        "Tokenizing…"
                                    },
                                fontSize = 16.sp,
                            )
                        }
                    }
                }

                if (stage == ClickToPayPaymentViewModel.Stage.CHECKOUT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::cancelCheckout,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancel checkout")
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickToPayContactSection(
    email: String,
    emailError: String?,
    phoneError: String?,
    deviceRecognition: DeviceRecognitionState,
    recognizedCardLabels: RecognizedCardLabelList,
    phoneCountryCode: String,
    phoneNumber: String,
    enabled: Boolean,
    showIdentityFields: Boolean,
    onEmailChange: (String) -> Unit,
    onEmailFocus: (Boolean) -> Unit,
    onUseDifferentEmail: () -> Unit,
    onPhoneCountryCodeChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (deviceRecognition) {
            DeviceRecognitionState.Checking -> {
                ClickToPayMcRecognizedDevicePanel(
                    title = "Checking this device",
                    body = "Looking for saved Click to Pay cards on this device…",
                    showProgress = true,
                )
            }
            DeviceRecognitionState.Recognized -> {
                ClickToPayMcRecognizedDevicePanel(
                    title = "Welcome back",
                    body =
                        if (recognizedCardLabels.items.isNotEmpty()) {
                            "Use your saved cards to check out faster."
                        } else {
                            "This device has saved Click to Pay cards."
                        },
                    cardLabels = recognizedCardLabels,
                    linkText = "Not you? Use a different email",
                    onLinkClick = onUseDifferentEmail,
                    linkEnabled = enabled,
                )
            }
            DeviceRecognitionState.DetectionFailed -> {
                ClickToPayMcRecognizedDevicePanel(
                    title = "Could not check saved cards",
                    body = "Saved-card detection did not finish. You can still pay with Click to Pay using your email or phone.",
                )
            }
            DeviceRecognitionState.UsingDifferentEmail,
            DeviceRecognitionState.NotRecognized,
            -> Unit
        }

        if (showIdentityFields) {
            SimpleInputField(
                label = "Email",
                value = email,
                onValueChange = onEmailChange,
                isRequired = false,
                isError = emailError != null,
                errorMessage = emailError,
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next,
                onFocusChanged = onEmailFocus,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SimpleInputField(
                    label = "Country code",
                    value = phoneCountryCode,
                    onValueChange = onPhoneCountryCodeChange,
                    isRequired = false,
                    placeholder = "Country code",
                    showLabel = false,
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.width(120.dp),
                )
                SimpleInputField(
                    label = "Mobile number",
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    isRequired = false,
                    placeholder = "Mobile number",
                    showLabel = false,
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }

            if (phoneError != null) {
                Text(
                    text = phoneError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ClickToPayMcRecognizedDevicePanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    cardLabels: RecognizedCardLabelList = RecognizedCardLabelList(),
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null,
    linkEnabled: Boolean = true,
    showProgress: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, ClickToPayBrandColors.panelBorder, shape),
        shape = shape,
        color = ClickToPayBrandColors.panelBackground,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ClickToPayBrandColors.primaryText,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ClickToPayBrandColors.primaryText,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = ClickToPayBrandColors.secondaryText,
            )
            if (cardLabels.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                cardLabels.items.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClickToPayBrandColors.primaryText,
                    )
                }
            }
            if (linkText != null && onLinkClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = linkText,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = ClickToPayBrandColors.primaryText,
                            textDecoration = TextDecoration.Underline,
                        ),
                    modifier =
                        Modifier.clickable(enabled = linkEnabled) {
                            onLinkClick()
                        },
                )
            }
        }
    }
}

@Composable
private fun ClickToPayBillingPrefillSection(
    prefill: ClickToPayMerchantPrefill,
    firstNameError: String?,
    lastNameError: String?,
    enabled: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onAddressLine1Change: (String) -> Unit,
    onAddressLine2Change: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onZipChange: (String) -> Unit,
    onCopyBillingToShippingChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SimpleInputField(
                label = "First name",
                value = prefill.firstName,
                onValueChange = onFirstNameChange,
                isRequired = true,
                isError = firstNameError != null,
                errorMessage = firstNameError,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
            SimpleInputField(
                label = "Last name",
                value = prefill.lastName,
                onValueChange = onLastNameChange,
                isRequired = true,
                isError = lastNameError != null,
                errorMessage = lastNameError,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
        }

        SimpleInputField(
            label = "Street address 1",
            value = prefill.addressLine1,
            onValueChange = onAddressLine1Change,
            isRequired = false,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        )

        SimpleInputField(
            label = "Street address 2 (optional)",
            value = prefill.addressLine2,
            onValueChange = onAddressLine2Change,
            isRequired = false,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        )

        SimpleInputField(
            label = "Country (ISO)",
            value = prefill.country,
            onValueChange = onCountryChange,
            isRequired = false,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next,
        )

        SimpleInputField(
            label = "City",
            value = prefill.city,
            onValueChange = onCityChange,
            isRequired = false,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SimpleInputField(
                label = "State",
                value = prefill.state,
                onValueChange = onStateChange,
                isRequired = false,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
            SimpleInputField(
                label = "ZIP",
                value = prefill.zip,
                onValueChange = onZipChange,
                isRequired = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = prefill.copyBillingToShipping,
                onCheckedChange = onCopyBillingToShippingChange,
                enabled = enabled,
            )
            Text(
                text = "Copy billing to shipping on tokenize",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ClickToPayDeveloperOptionsSection(
    doLookup: Boolean,
    eventLog: ClickToPayEventLog,
    enabled: Boolean,
    onDoLookupChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Auto lookup", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = doLookup, onCheckedChange = onDoLookupChange, enabled = enabled)
        }

        HorizontalDivider()

        if (eventLog.lines.isNotEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SpreedlyClickToPayCheckout.events",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    eventLog.lines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

private val C2P_STAGE_LABELS = listOf("Idle", "Checkout", "Tokenize")

private fun c2pStageToIndex(stage: ClickToPayPaymentViewModel.Stage): Int = when (stage) {
    ClickToPayPaymentViewModel.Stage.IDLE -> 0
    ClickToPayPaymentViewModel.Stage.CHECKOUT -> 1
    ClickToPayPaymentViewModel.Stage.TOKENIZING -> 2
}

private fun formatPrice(cents: Int): String {
    val dollars = cents / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(dollars)
}
