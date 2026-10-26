package com.spreedly.example.screens.customtextfields

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.spreedly.example.optionalfields.DemoAddressGroup
import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.example.validation.DemoExpiryManifestTest
import com.spreedly.hostedfields.models.HostedFieldEventType
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.models.CardScheme
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.sdk.ui.toFormFieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomTextFieldsViewModelFieldStateTest {

    private fun fullyValidVm(): CustomTextFieldsViewModel {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateNameInput("Jane Doe")
        vm.updateAddressInput("123 Main St")
        vm.updateCityInput("Springfield")
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        DemoOptionalFieldSelection.default().enabledFields.forEach { type ->
            vm.onHostedFieldValidation(type.toFormFieldType(isRequired = false), true)
        }
        return vm
    }

    @Test
    fun `onFieldStateUpdate routes CARD to inspector`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.onFieldStateUpdate(
            HostedFieldState(
                fieldType = FormFieldType.CARD(true),
                eventType = HostedFieldEventType.INPUT,
                isFocused = true,
                isValid = true,
                isEmpty = false,
                cardScheme = CardScheme.VISA,
                numberLength = 16,
                cvvLength = null,
                isPanMasked = true,
                iin = "411111",
            ),
        )
        assertEquals(16, vm.inspectorUiState.value.cardSnapshot?.numberLength)
    }

    @Test
    fun `performFullPaymentReset clears inspector and hosted field validity`() {
        val vm = fullyValidVm()
        vm.onFieldStateUpdate(
            HostedFieldState(
                fieldType = FormFieldType.CVV(true),
                eventType = HostedFieldEventType.BLUR,
                isFocused = false,
                isValid = true,
                isEmpty = false,
                cardScheme = null,
                numberLength = null,
                cvvLength = 3,
                isPanMasked = true,
                iin = null,
            ),
        )
        vm.performFullPaymentReset()
        assertNull(vm.inspectorUiState.value.cvvSnapshot)
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid requires hosted fields and merchant custom fields`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        assertFalse(vm.isFormValid.value)
        vm.updateNameInput("Jane Doe")
        vm.updateAddressInput("123 Main St")
        vm.updateCityInput("Springfield")
        assertFalse(vm.isFormValid.value)
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        DemoOptionalFieldSelection.default().enabledFields.forEach { type ->
            vm.onHostedFieldValidation(type.toFormFieldType(isRequired = false), true)
        }
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid false when required hosted zip invalid`() {
        val vm = fullyValidVm()
        val requiredZip =
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true)
        vm.updateFieldSelection(requiredZip)
        vm.onHostedFieldValidation(FormFieldType.ZIP(true), false)
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid false when core card fields invalid`() {
        val vm = fullyValidVm()
        vm.onHostedFieldValidation(FormFieldType.CARD(true), false)
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `hostedFormFields includes invalid dual manifest when demo switch enabled`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val formFields = vm.hostedFormFields()
        assertTrue(DemoExpiryManifestTest.isInvalidDualManifest(formFields))
    }

    @Test
    fun `isFormValid does not require month and year when invalid dual manifest enabled`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        vm.updateNameInput("Jane Doe")
        vm.updateAddressInput("123 Main St")
        vm.updateCityInput("Springfield")
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        assertTrue(vm.isFormValid.value)
        assertTrue(vm.skipPreSubmitFieldValidityGateForDemo())
    }

    @Test
    fun `resolveCheckoutValidationSnackbar dedupes companion validation messages`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val message = DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE
        assertEquals(message, vm.resolveCheckoutValidationSnackbar(message))
        assertNull(vm.resolveCheckoutValidationSnackbar(message))
        vm.beginCheckoutAttempt()
        assertEquals(message, vm.resolveCheckoutValidationSnackbar(message))
    }

    @Test
    fun `resolvePaymentFailedSnackbar maps typed async expiry manifest failure`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )
        assertEquals(
            DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE,
            vm.resolvePaymentFailedSnackbar(failed),
        )
    }

    @Test
    fun `resolvePaymentFailedSnackbar does not map custom validator failure with validationErrors`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
                validationErrors =
                    listOf(
                        PaymentResult.Failed.ValidationError(
                            fieldName = "zip",
                            errorKey = "invalid",
                            errorMessage = null,
                        ),
                    ),
            )
        assertEquals("Payment failed: Payment form validation failed", vm.resolvePaymentFailedSnackbar(failed))
    }

    @Test
    fun `resolvePaymentFailedSnackbar keeps network style copy for non validation failures`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "Gateway timeout",
            )
        assertEquals("Payment failed: Gateway timeout", vm.resolvePaymentFailedSnackbar(failed))
    }

    @Test
    fun `formatValidationFailedMessage returns FA10 copy for dual manifest failure`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val message =
            vm.formatValidationFailedMessage(
                PaymentProcessingResult.ValidationFailed(
                    invalidFields = DemoExpiryManifestTest.invalidDualManifestFields(),
                ),
            )
        assertEquals(DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE, message)
    }

    @Test
    fun `validation failed path does not enter processing state`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        assertFalse(vm.isProcessing.value)
        vm.resolveValidationFailedSnackbar(
            PaymentProcessingResult.ValidationFailed(
                invalidFields = DemoExpiryManifestTest.invalidDualManifestFields(),
            ),
        )
        assertFalse(vm.isProcessing.value)
    }

    @Test
    fun `hostedFormFields core manifest matches FA10 fixture when demo switch enabled`() {
        val vm = CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        vm.updateDemoInvalidExpiryManifest(true)
        val formFields = vm.hostedFormFields()
        val coreFields =
            formFields.filter {
                it is FormFieldType.CARD ||
                    it is FormFieldType.CVV ||
                    it is FormFieldType.EXPIRY_DATE ||
                    it is FormFieldType.MONTH ||
                    it is FormFieldType.YEAR
            }
        assertEquals(DemoExpiryManifestTest.demoSubmitFormFields(), coreFields)
    }

    @Test
    fun `isFormValid false when merchant name invalid`() {
        val vm = fullyValidVm()
        vm.updateNameInput("J")
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid false when merchant address invalid`() {
        val vm = fullyValidVm()
        vm.updateAddressInput("123")
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid false when merchant city invalid`() {
        val vm = fullyValidVm()
        vm.updateCityInput("S")
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `onCheckoutFieldsClearedBySdk resets card hosted validity only`() {
        val vm = fullyValidVm()
        vm.onCheckoutFieldsClearedBySdk()
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `resetFormFields clears merchant custom inputs and invalidates form`() {
        val vm = fullyValidVm()
        vm.resetFormFields()
        assertEquals("", vm.nameInput.value.value)
        assertEquals("", vm.addressInput.value.value)
        assertEquals("", vm.cityInput.value.value)
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `updateFieldSelection shipping enables shipping validity tracking`() {
        val vm = fullyValidVm()
        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = true),
        )
        // SHIPPING_ZIP is optional but once it has a blur error the button must be disabled.
        vm.onHostedFieldValidation(FormFieldType.SHIPPING_ZIP(false), false)
        assertFalse(vm.isFormValid.value)
        // After the user corrects it the button re-enables.
        vm.onHostedFieldValidation(FormFieldType.SHIPPING_ZIP(false), true)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `NameInput rejects invalid characters`() {
        val input = NameInput("Jane123")
        assertFalse(input.isValid)
        assertEquals(NameError.InvalidCharacters, input.error)
    }

    @Test
    fun `AddressInput rejects too short value`() {
        val input = AddressInput("1234")
        assertFalse(input.isValid)
        assertEquals(AddressError.TooShort, input.error)
    }

    @Test
    fun `CityInput rejects blank value`() {
        val input = CityInput("")
        assertFalse(input.isValid)
        assertEquals(CityError.Empty, input.error)
    }
}
