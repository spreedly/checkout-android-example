package com.spreedly.example.screens.customtextfields

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.spreedly.hostedfields.models.HostedFieldEventType
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.models.CardScheme
import com.spreedly.sdk.models.FormFieldType
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

    @Test
    fun `onFieldStateUpdate routes CARD to inspector`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = CustomTextFieldsViewModel(context, skipAutoInitializeSdk = true)
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
    fun `performFullPaymentReset clears inspector`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = CustomTextFieldsViewModel(context, skipAutoInitializeSdk = true)
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
    }

    @Test
    fun `isFormValid requires hosted fields and custom address fields`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = CustomTextFieldsViewModel(context, skipAutoInitializeSdk = true)
        assertFalse(vm.isFormValid.value)
        vm.updateNameInput("Jane Doe")
        vm.updateAddressInput("123 Main St")
        vm.updateCityInput("Springfield")
        vm.updateStateInput("CA")
        vm.updateZipCodeInput("12345")
        assertFalse(vm.isFormValid.value)
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `custom field updates refresh aggregate`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = CustomTextFieldsViewModel(context, skipAutoInitializeSdk = true)
        vm.updateNameInput("Jane Doe")
        assertTrue(vm.inspectorUiState.value.onChangeReadout.contains("Full Name"))
    }
}
