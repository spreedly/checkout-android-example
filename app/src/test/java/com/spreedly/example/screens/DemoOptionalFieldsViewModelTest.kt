package com.spreedly.example.screens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.spreedly.example.optionalfields.DemoAddressGroup
import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.example.screens.bottomsheet.BottomSheetPaymentViewModel
import com.spreedly.example.screens.customtextfields.CustomTextFieldsViewModel
import com.spreedly.example.validation.DemoCoreNameField
import com.spreedly.example.validation.DemoCustomValidationPreset
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.SpreedlySDKInitOptions
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.toFormFieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DemoOptionalFieldsViewModelTest {
    private fun initSdk(vm: CustomTextFieldsViewModel) {
        vm.sdk.init(
            SpreedlySDKInitOptions(
                environmentKey = "test_env_key",
                nonce = "test_nonce",
                timestamp = "1234567890",
                signature = "test_signature",
                certificateToken = "test_cert_token",
            ),
        )
    }

    private fun fillCustomTextFields(vm: CustomTextFieldsViewModel) {
        vm.updateNameInput("Jane Doe")
        vm.updateAddressInput("123 Main St")
        vm.updateCityInput("Springfield")
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        DemoOptionalFieldSelection.default().enabledFields.forEach { type ->
            vm.onHostedFieldValidation(type.toFormFieldType(isRequired = false), true)
        }
    }

    @Test
    fun `custom text fields screen does not register core name validators`() {
        val customTextVm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        // availableCoreNameFields was removed from CustomTextFieldsViewModel; the config card
        // always uses an empty layout so FULL_NAME custom validators are never registered.
        // runCustomValidationBeforeSubmit is used here as a probe of the validator registry —
        // CustomTextFieldsScreen no longer calls it directly; validation runs inside createCreditCard.

        initSdk(customTextVm)
        customTextVm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FULL_NAME,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                ),
        )
        customTextVm.updateNameInput("Al")
        // FULL_NAME validator is never registered (empty core name layout) — validation passes.
        assertFalse(
            customTextVm.sdk.runCustomValidationBeforeSubmit(
                formFields = customTextVm.hostedFormFields(),
                additionalFields = customTextVm.buildSubmitAdditionalFields(),
            ),
        )
    }

    @Test
    fun `bottom sheet core name sync and layout scoping`() {
        val bottomSheetVm = BottomSheetPaymentViewModel(ApplicationProvider.getApplicationContext())
        assertEquals(DemoCoreNameField.forBottomSheetScreen(), bottomSheetVm.availableCoreNameFields)

        bottomSheetVm.sdk.init(
            SpreedlySDKInitOptions(
                environmentKey = "test_env_key",
                nonce = "test_nonce",
                timestamp = "1234567890",
                signature = "test_signature",
                certificateToken = "test_cert_token",
            ),
        )
        bottomSheetVm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FIRST_NAME,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                ),
        )
        bottomSheetVm.sdk.callbacks.onFirstNameChange("Al", isRequired = true)
        bottomSheetVm.sdk.registerMountedCatalogueField(FormFieldType.FIRST_NAME(required = true))
        assertTrue(
            bottomSheetVm.sdk.runCustomValidationBeforeSubmit(
                formFields = emptyList(),
                additionalFields = emptyMap(),
            ),
        )

        bottomSheetVm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FULL_NAME,
                    DemoCustomValidationPreset.ALWAYS_INVALID,
                ),
        )
        bottomSheetVm.sdk.callbacks.onFirstNameChange("Jane", isRequired = true)
        assertFalse(
            bottomSheetVm.sdk.runCustomValidationBeforeSubmit(
                formFields = emptyList(),
                additionalFields = emptyMap(),
            ),
        )
    }

    @Test
    fun `toggling field to required disables button without a blur when field is unvalidated`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // ZIP was optional (default valid=true) — toggling it to required must reset validity to false
        // so the button disables immediately, without waiting for a blur.
        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true),
        )
        assertFalse(vm.isFormValid.value)

        // A real blur that passes re-enables the button.
        vm.onHostedFieldValidation(FormFieldType.ZIP(true), true)
        assertTrue(vm.isFormValid.value)

        // Toggling back to optional resets to valid-by-default.
        vm.updateFieldSelection(DemoOptionalFieldSelection.default())
        fillCustomTextFields(vm)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `toggling required off on a field with stale false validity re-enables button`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // Toggle ZIP to required — button disables immediately.
        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true),
        )
        assertFalse(vm.isFormValid.value)
        // Toggle back to optional — ZIP default is valid-by-default, button re-enables
        // (core fields are still valid from fillCustomTextFields).
        vm.updateFieldSelection(DemoOptionalFieldSelection.default())
        fillCustomTextFields(vm)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `all required optional fields must be valid before button enables`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // Make both ZIP and STATE required.
        val bothRequired = DemoOptionalFieldSelection.default()
            .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true)
            .withFieldRequired(OptionalFieldType.STATE, required = true)
        vm.updateFieldSelection(bothRequired)
        assertFalse(vm.isFormValid.value)

        // Validate ZIP only — still blocked by STATE.
        vm.onHostedFieldValidation(FormFieldType.ZIP(true), true)
        assertFalse(vm.isFormValid.value)

        // Validate STATE — now both are valid, button enables.
        vm.onHostedFieldValidation(FormFieldType.STATE(true), true)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `removing a required field from selection clears its validity and re-evaluates button`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // Enable and require SHIPPING_ZIP (not in default selection).
        val withShipping = DemoOptionalFieldSelection.default()
            .withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = true)
            .withFieldRequired(OptionalFieldType.SHIPPING_ZIP, required = true)
        vm.updateFieldSelection(withShipping)
        assertFalse(vm.isFormValid.value)

        // Remove the shipping group — SHIPPING_ZIP is gone, no required fields remain unsatisfied.
        vm.updateFieldSelection(DemoOptionalFieldSelection.default())
        fillCustomTextFields(vm)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `performFullPaymentReset resets required field validity and disables button`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // Make ZIP required and mark it valid via a blur signal.
        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true),
        )
        vm.onHostedFieldValidation(FormFieldType.ZIP(true), true)
        assertTrue(vm.isFormValid.value)

        // Reset clears card/CVV/expiry validity; ZIP required field defaults back to false.
        vm.performFullPaymentReset()
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `optional field with active blur error disables button`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)
        // ZIP is not required, but a custom-validator blur error means an inline error is visible.
        // The button must be disabled while that error is showing.
        vm.onHostedFieldValidation(FormFieldType.ZIP(false), false)
        assertFalse(vm.isFormValid.value)
        // Once the user fixes the value the SDK fires isValid=true and the button re-enables.
        vm.onHostedFieldValidation(FormFieldType.ZIP(false), true)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `custom text fields optional hosted validity and submit map`() {
        val vm =
            CustomTextFieldsViewModel(ApplicationProvider.getApplicationContext(), skipAutoInitializeSdk = true)
        fillCustomTextFields(vm)

        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true),
        )
        vm.onHostedFieldValidation(FormFieldType.ZIP(false), false)
        assertFalse(vm.isFormValid.value)

        vm.updateFieldSelection(DemoOptionalFieldSelection.default())
        fillCustomTextFields(vm)
        // ZIP is back to optional, but the field is still reporting an error — button stays disabled.
        vm.onHostedFieldValidation(FormFieldType.ZIP(false), false)
        assertFalse(vm.isFormValid.value)
        // Clear the error — button re-enables.
        vm.onHostedFieldValidation(FormFieldType.ZIP(false), true)
        assertTrue(vm.isFormValid.value)

        vm.updateFieldSelection(
            DemoOptionalFieldSelection.default()
                .withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = true),
        )
        assertTrue(vm.hostedFormFields().any { it is FormFieldType.SHIPPING_ZIP })

        vm.updateNameInput("Jane Doe")
        vm.sdk.callbacks.onZipCodeChange("94107", false)
        vm.sdk.optionalCallbacks.onCountryChange("US", false)
        val map = vm.buildSubmitAdditionalFields()
        assertEquals("Jane Doe", map[AdditionalField.FULL_NAME])
        assertEquals("94107", map[AdditionalField.ZIP_CODE])
    }
}
