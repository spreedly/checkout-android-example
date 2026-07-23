package com.spreedly.example.screens.headlessbankaccount

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.app.R
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.findActivityOrNull
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.models.BankAccountHolderType
import com.spreedly.sdk.models.BankAccountType
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.BankAccountFieldConfig
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.NameFieldDisplayMode
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.achCombinedNameLengthErrorMessage
import com.spreedly.sdk.ui.isAchAccountNumberFieldValid
import com.spreedly.sdk.ui.resolveAchTextFieldImeAction
import com.spreedly.sdk.ui.resolveBankAccountNameErrorMessage
import com.spreedly.security.secureScreen
import com.spreedly.ui.molecules.AppTextField
import com.spreedly.ui.theme.resolveColor
import com.spreedly.ui.theme.resolveEffectiveCustomFieldsConfig
import com.spreedly.ui.theme.spreedlySegmentedButtonColors
import com.spreedly.validation.SpreedlyParamsManager
import com.spreedly.validation.validators.NameValidator
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
@SuppressLint("ComposeModifierMissing")
fun HeadlessBankAccountScreen(
    viewModel: HeadlessBankAccountViewModel,
    navController: NavController,
) {
    val sdk = viewModel.sdk
    val snackbarHostState = viewModel.snackbarHostState
    val coroutineScope = rememberCoroutineScope()

    val isInitializing by viewModel.isInitializing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val paymentToken by viewModel.paymentToken.collectAsState()
    val paymentFinished by viewModel.paymentFinished.collectAsState()
    val fieldConfig by viewModel.fieldConfig.collectAsState()
    val uiConfig by viewModel.uiConfig.collectAsState()
    val bankAccountState by sdk.bankAccountState
    val resolvedConfig = resolveEffectiveCustomFieldsConfig(uiConfig)

    var accountHolderName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var accountType by remember { mutableStateOf(BankAccountType.CHECKING) }
    var accountHolderType by remember { mutableStateOf(BankAccountHolderType.PERSONAL) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var bankName by remember { mutableStateOf("") }
    var bankNameError by remember { mutableStateOf<String?>(null) }

    var routingNumberIsValid by remember { mutableStateOf(false) }
    var accountNumberIsValid by remember { mutableStateOf(false) }
    var fullNameIsValid by remember { mutableStateOf(false) }
    var firstNameIsValid by remember { mutableStateOf(false) }
    var lastNameIsValid by remember { mutableStateOf(false) }
    var bankNameIsValid by remember { mutableStateOf(true) }

    fun clearLocalBankAccountFields() {
        accountHolderName = ""
        nameError = null
        firstName = ""
        lastName = ""
        firstNameError = null
        lastNameError = null
        bankName = ""
        bankNameError = null
        accountType = BankAccountType.CHECKING
        accountHolderType = BankAccountHolderType.PERSONAL
        routingNumberIsValid = false
        accountNumberIsValid = false
        fullNameIsValid = false
        firstNameIsValid = false
        lastNameIsValid = false
        bankNameIsValid = true
    }

    val allowBlankName = SpreedlyParamsManager.allowBlankName

    val lastSyncedFieldConfig = remember { HeadlessFieldConfigSyncHolder() }
    SideEffect {
        if (lastSyncedFieldConfig.config != fieldConfig) {
            sdk.setBankAccountFieldConfig(fieldConfig)
            lastSyncedFieldConfig.config = fieldConfig
        }
    }

    val activity = LocalContext.current.findActivityOrNull()
    DisposableEffect(Unit) {
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                clearLocalBankAccountFields()
                viewModel.markLocalFieldsClearPending()
                sdk.resetBankAccountState()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.consumeLocalFieldsClearPending()) {
            clearLocalBankAccountFields()
        } else {
            accountHolderName = bankAccountState.accountHolderName.value
            firstName = bankAccountState.firstName.value
            lastName = bankAccountState.lastName.value
            bankName = bankAccountState.bankName.value
            accountType = bankAccountState.accountType
            accountHolderType = bankAccountState.accountHolderType
        }
    }

    LaunchedEffect(
        accountHolderName,
        firstName,
        lastName,
        bankName,
        fieldConfig,
        allowBlankName,
        bankAccountState.routingNumber.value,
        bankAccountState.accountNumber.value,
    ) {
        val nameValidity = headlessAchNameValidity(
            accountHolderName = accountHolderName,
            firstName = firstName,
            lastName = lastName,
            bankName = bankName,
            showBankName = fieldConfig.showBankName,
            bankNameRequired = fieldConfig.bankNameRequired,
            allowBlankName = allowBlankName,
        )
        fullNameIsValid = nameValidity.fullNameIsValid
        firstNameIsValid = nameValidity.firstNameIsValid
        lastNameIsValid = nameValidity.lastNameIsValid
        bankNameIsValid = nameValidity.bankNameIsValid
        routingNumberIsValid = bankAccountState.routingNumber.isValid
        accountNumberIsValid = isAchAccountNumberFieldValid(bankAccountState.accountNumber.value)
    }

    LaunchedEffect(paymentFinished) {
        if (paymentFinished) {
            clearLocalBankAccountFields()
            sdk.resetBankAccountState()
            viewModel.resetPaymentFinished()
            navController.popBackStack()
        }
    }

    val formFields = listOf(
        FormFieldType.ROUTING_NUMBER(required = true),
        FormFieldType.ACCOUNT_NUMBER(required = true),
    )

    val nameRequiredError = stringResource(R.string.headless_bank_account_holder_name_required)
    val nameMinLengthError = stringResource(R.string.headless_bank_account_holder_name_min_length)
    val nameTooLongError = stringResource(R.string.headless_bank_account_holder_name_too_long)
    val validationFailedMsg = stringResource(R.string.headless_bank_account_validation_failed)
    val processingErrorMsg = stringResource(R.string.headless_bank_account_error)

    fun hasFieldsAfterName(): Boolean {
        return fieldConfig.showBankName || fieldConfig.showAccountType || fieldConfig.showAccountHolderType
    }

    fun nameValidationError(validator: NameValidator): String? {
        return validator.resolveBankAccountNameErrorMessage(
            isValidationForced = true,
            requiredMessage = nameRequiredError,
            tooShortMessage = nameMinLengthError,
            tooLongMessage = nameTooLongError,
        )
    }

    fun updateAccountHolderName(value: String) {
        accountHolderName = value
        sdk.bankAccountCallbacks.onAccountHolderNameChange(value)
        val validator = NameValidator(value, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH)
        nameError = nameValidationError(validator)
        fullNameIsValid = headlessAchNameValidity(
            accountHolderName = value,
            firstName = firstName,
            lastName = lastName,
            bankName = bankName,
            showBankName = fieldConfig.showBankName,
            bankNameRequired = fieldConfig.bankNameRequired,
            allowBlankName = allowBlankName,
        ).fullNameIsValid
    }

    fun updateFirstName(value: String) {
        firstName = value
        sdk.bankAccountCallbacks.onFirstNameChange(value)
        val validator = NameValidator(value, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH)
        val combinedError = achCombinedNameLengthErrorMessage()
        firstNameError = combinedError ?: nameValidationError(validator)
        lastNameError = combinedError
            ?: nameValidationError(NameValidator(lastName, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH))
        val nameValidity = headlessAchNameValidity(
            accountHolderName = accountHolderName,
            firstName = value,
            lastName = lastName,
            bankName = bankName,
            showBankName = fieldConfig.showBankName,
            bankNameRequired = fieldConfig.bankNameRequired,
            allowBlankName = allowBlankName,
        )
        firstNameIsValid = nameValidity.firstNameIsValid
        lastNameIsValid = nameValidity.lastNameIsValid
    }

    fun updateLastName(value: String) {
        lastName = value
        sdk.bankAccountCallbacks.onLastNameChange(value)
        val validator = NameValidator(value, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH)
        val combinedError = achCombinedNameLengthErrorMessage()
        lastNameError = combinedError ?: nameValidationError(validator)
        firstNameError = combinedError
            ?: nameValidationError(NameValidator(firstName, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH))
        val nameValidity = headlessAchNameValidity(
            accountHolderName = accountHolderName,
            firstName = firstName,
            lastName = value,
            bankName = bankName,
            showBankName = fieldConfig.showBankName,
            bankNameRequired = fieldConfig.bankNameRequired,
            allowBlankName = allowBlankName,
        )
        firstNameIsValid = nameValidity.firstNameIsValid
        lastNameIsValid = nameValidity.lastNameIsValid
    }

    fun updateBankName(value: String) {
        bankName = value
        sdk.bankAccountCallbacks.onBankNameChange(value, fieldConfig.bankNameRequired)
        if (!fieldConfig.showBankName) {
            bankNameError = null
            bankNameIsValid = true
            return
        }
        if (!fieldConfig.bankNameRequired) {
            bankNameError = null
            bankNameIsValid = true
            return
        }
        val validator = NameValidator(value, isPure = false, minLength = NameValidator.ACH_MIN_LENGTH)
        bankNameError = validator.resolveBankAccountNameErrorMessage(
            isValidationForced = true,
            requiredMessage = nameRequiredError,
            tooShortMessage = nameMinLengthError,
            tooLongMessage = nameTooLongError,
            requireNonBlank = true,
        )
        bankNameIsValid = headlessAchNameValidity(
            accountHolderName = accountHolderName,
            firstName = firstName,
            lastName = lastName,
            bankName = value,
            showBankName = fieldConfig.showBankName,
            bankNameRequired = fieldConfig.bankNameRequired,
            allowBlankName = allowBlankName,
        ).bankNameIsValid
    }

    val isFormValid = routingNumberIsValid &&
        accountNumberIsValid &&
        when (fieldConfig.nameDisplayMode) {
            NameFieldDisplayMode.SINGLE_FIELD -> fullNameIsValid
            NameFieldDisplayMode.SEPARATE_FIELDS -> firstNameIsValid && lastNameIsValid
        } &&
        if (fieldConfig.showBankName && fieldConfig.bankNameRequired) {
            bankNameIsValid
        } else {
            true
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
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
                        contentDescription = stringResource(R.string.headless_bank_account_title),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Text(
                text = stringResource(R.string.headless_bank_account_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.padding(bottom = Spacing.xs),
            )

            Text(
                text = stringResource(R.string.headless_bank_account_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xxxl),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                shape = MaterialTheme.shapes.large,
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
                    Text(
                        text = stringResource(R.string.headless_bank_account_section_secure),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                    )

                    SPLTextField(
                        label = stringResource(com.spreedly.paymentsheet.R.string.bank_account_routing_number_label),
                        formFieldType = FormFieldType.ROUTING_NUMBER(required = true),
                        config = uiConfig,
                        value = bankAccountState.routingNumber.value,
                        onChange = { sdk.bankAccountCallbacks.onRoutingNumberChange(it, true) },
                        onValidationChange = { routingNumberIsValid = it },
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    SPLTextField(
                        label = stringResource(com.spreedly.paymentsheet.R.string.bank_account_account_number_label),
                        formFieldType = FormFieldType.ACCOUNT_NUMBER(required = true),
                        config = uiConfig,
                        value = bankAccountState.accountNumber.value,
                        onChange = { sdk.bankAccountCallbacks.onAccountNumberChange(it, true) },
                        onValidationChange = { accountNumberIsValid = it },
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    Text(
                        text = stringResource(R.string.headless_bank_account_section_personal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                    )

                    val fieldShape = resolvedConfig.fieldShape

                    when (fieldConfig.nameDisplayMode) {
                        NameFieldDisplayMode.SINGLE_FIELD -> {
                            AppTextField(
                                label = stringResource(R.string.headless_bank_account_holder_name_label),
                                value = accountHolderName,
                                onValueChange = { updateAccountHolderName(it) },
                                hint = "",
                                isRequired = true,
                                error = nameError,
                                backgroundColor = resolvedConfig.fieldBackgroundColor,
                                focusedBorderColor = resolvedConfig.primaryColor,
                                unfocusedBorderColor = resolvedConfig.formBorderColor,
                                labelColor = resolvedConfig.fieldLabelColor,
                                placeholderColor = resolvedConfig.placeholderColor,
                                textColor = resolvedConfig.textColor,
                                shape = fieldShape,
                                imeAction = resolveAchTextFieldImeAction(hasFieldsAfterName()),
                                maxLength = NameValidator.MAX_LENGTH,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        NameFieldDisplayMode.SEPARATE_FIELDS -> {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                AppTextField(
                                    label = stringResource(R.string.headless_bank_account_first_name_label),
                                    value = firstName,
                                    onValueChange = { updateFirstName(it) },
                                    hint = "",
                                    isRequired = true,
                                    error = firstNameError,
                                    backgroundColor = resolvedConfig.fieldBackgroundColor,
                                    focusedBorderColor = resolvedConfig.primaryColor,
                                    unfocusedBorderColor = resolvedConfig.formBorderColor,
                                    labelColor = resolvedConfig.fieldLabelColor,
                                    placeholderColor = resolvedConfig.placeholderColor,
                                    textColor = resolvedConfig.textColor,
                                    shape = fieldShape,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                AppTextField(
                                    label = stringResource(R.string.headless_bank_account_last_name_label),
                                    value = lastName,
                                    onValueChange = { updateLastName(it) },
                                    hint = "",
                                    isRequired = true,
                                    error = lastNameError,
                                    backgroundColor = resolvedConfig.fieldBackgroundColor,
                                    focusedBorderColor = resolvedConfig.primaryColor,
                                    unfocusedBorderColor = resolvedConfig.formBorderColor,
                                    labelColor = resolvedConfig.fieldLabelColor,
                                    placeholderColor = resolvedConfig.placeholderColor,
                                    textColor = resolvedConfig.textColor,
                                    shape = fieldShape,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (fieldConfig.showBankName) {
                        Spacer(modifier = Modifier.height(Spacing.md))

                        val bankNameLabel = fieldConfig.bankNameLabel
                            ?: stringResource(R.string.headless_bank_account_bank_name_label)
                        AppTextField(
                            label = bankNameLabel,
                            value = bankName,
                            onValueChange = { updateBankName(it) },
                            hint = "",
                            isRequired = fieldConfig.bankNameRequired,
                            error = bankNameError,
                            backgroundColor = resolvedConfig.fieldBackgroundColor,
                            focusedBorderColor = resolvedConfig.primaryColor,
                            unfocusedBorderColor = resolvedConfig.formBorderColor,
                            labelColor = resolvedConfig.fieldLabelColor,
                            placeholderColor = resolvedConfig.placeholderColor,
                            textColor = resolvedConfig.textColor,
                            shape = fieldShape,
                            imeAction = resolveAchTextFieldImeAction(
                                fieldConfig.showAccountType || fieldConfig.showAccountHolderType,
                            ),
                            maxLength = NameValidator.MAX_LENGTH,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (fieldConfig.showAccountType) {
                        Spacer(modifier = Modifier.height(Spacing.md))

                        Text(
                            text = stringResource(R.string.headless_bank_account_account_type),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xxs),
                        )
                        val accountTypes = remember { BankAccountType.entries.toList() }
                        val accountTypeLabels = mapOf(
                            BankAccountType.CHECKING to stringResource(R.string.headless_bank_account_checking),
                            BankAccountType.SAVINGS to stringResource(R.string.headless_bank_account_savings),
                        )
                        val segmentedColors = spreedlySegmentedButtonColors(resolvedConfig.primaryColor)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            accountTypes.forEachIndexed { index, type ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = accountTypes.size,
                                    ),
                                    onClick = { accountType = type },
                                    selected = accountType == type,
                                    colors = segmentedColors,
                                ) {
                                    Text(accountTypeLabels[type] ?: type.rawValue)
                                }
                            }
                        }
                    }

                    if (fieldConfig.showAccountHolderType) {
                        Spacer(modifier = Modifier.height(Spacing.md))

                        Text(
                            text = stringResource(R.string.headless_bank_account_holder_type),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xxs),
                        )
                        val holderTypes = remember { BankAccountHolderType.entries.toList() }
                        val holderTypeLabels = mapOf(
                            BankAccountHolderType.PERSONAL to stringResource(R.string.headless_bank_account_personal),
                            BankAccountHolderType.BUSINESS to stringResource(R.string.headless_bank_account_business),
                        )
                        val holderSegmentedColors = spreedlySegmentedButtonColors(resolvedConfig.primaryColor)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            holderTypes.forEachIndexed { index, type ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = holderTypes.size,
                                    ),
                                    onClick = { accountHolderType = type },
                                    selected = accountHolderType == type,
                                    colors = holderSegmentedColors,
                                ) {
                                    Text(holderTypeLabels[type] ?: type.rawValue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    val isButtonDisabled = isInitializing || isProcessing || !isFormValid

                    Button(
                        onClick = {
                            if (!isFormValid) return@Button
                            coroutineScope.launch {
                                    try {
                                        if (!viewModel.initializeForPayment()) return@launch

                                        sdk.bankAccountCallbacks.onAccountTypeChange(accountType)
                                        sdk.bankAccountCallbacks.onAccountHolderTypeChange(accountHolderType)

                                        val additionalFields = buildMap {
                                            when (fieldConfig.nameDisplayMode) {
                                                NameFieldDisplayMode.SINGLE_FIELD -> {
                                                    put(AdditionalField.FULL_NAME, accountHolderName)
                                                }
                                                NameFieldDisplayMode.SEPARATE_FIELDS -> {
                                                    put(AdditionalField.FIRST_NAME, firstName)
                                                    put(AdditionalField.LAST_NAME, lastName)
                                                }
                                            }
                                            if (fieldConfig.showBankName && bankName.isNotBlank()) {
                                                put(AdditionalField.BANK_NAME, bankName)
                                            }
                                        }

                                        val result = sdk.createBankAccount(
                                            formFields = formFields,
                                            additionalFields = additionalFields,
                                            metadata = mapOf(
                                                "checkout_type" to "headless_bank_account",
                                                "timestamp" to "${System.currentTimeMillis()}",
                                            ),
                                        )

                                        when (result) {
                                            is PaymentProcessingResult.Processing -> {
                                                viewModel.setProcessing(true)
                                                viewModel.startPaymentPolling()
                                            }
                                            is PaymentProcessingResult.ValidationFailed -> {
                                                viewModel.setProcessing(false)
                                                snackbarHostState.showSnackbar(
                                                    message = validationFailedMsg,
                                                    withDismissAction = true,
                                                )
                                            }
                                            is PaymentProcessingResult.Rejected -> {
                                                viewModel.setProcessing(false)
                                            }
                                            is PaymentProcessingResult.Failed -> {
                                                viewModel.setProcessing(true)
                                                viewModel.startPaymentPolling()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        viewModel.setProcessing(false)
                                        snackbarHostState.showSnackbar(
                                            message = processingErrorMsg,
                                            withDismissAction = true,
                                        )
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isButtonDisabled,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = resolveColor(
                                resolvedConfig.primaryColor,
                                MaterialTheme.colorScheme.primary,
                            ),
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isInitializing || isProcessing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                            }
                            Text(
                                text = when {
                                    isInitializing -> stringResource(R.string.headless_bank_account_initializing)
                                    isProcessing -> stringResource(R.string.headless_bank_account_processing)
                                    else -> stringResource(R.string.headless_bank_account_submit)
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = stringResource(R.string.secure),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    stringResource(R.string.headless_bank_account_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private class HeadlessFieldConfigSyncHolder {
    var config: BankAccountFieldConfig? = null
}

