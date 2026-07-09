package com.spreedly.example.screens.basiccheckout

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.spreedly.hostedfields.models.HostedFieldEventType
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.CardScheme
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.utils.CardNumberContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BasicCheckoutViewModelFieldStateTest {

    @Before
    fun setUp() {
        CardNumberContext.clear()
    }

    @After
    fun tearDown() {
        CardNumberContext.clear()
    }

    private fun cardState() =
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
            iin = null,
        )

    @Test
    fun `onFieldStateUpdate routes CARD to inspector`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        vm.onFieldStateUpdate(cardState())
        assertEquals(CardScheme.VISA, vm.inspectorUiState.value.cardSnapshot?.cardScheme)
    }

    @Test
    fun `performFullPaymentReset clears inspector snapshots`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        vm.onFieldStateUpdate(cardState())
        vm.performFullPaymentReset()
        assertNull(vm.inspectorUiState.value.cardSnapshot)
    }

    @Test
    fun `isFormValid is false until all hosted and custom fields are valid`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        assertFalse(vm.isFormValid.value)
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        assertFalse(vm.isFormValid.value)
        vm.updateNameValidity("Jane Doe")
        vm.updateEmailValidity("jane@example.com")
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `performFullPaymentReset clears isFormValid`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), true)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        vm.updateNameValidity("Jane Doe")
        vm.updateEmailValidity("jane@example.com")
        assertTrue(vm.isFormValid.value)
        vm.performFullPaymentReset()
        assertFalse(vm.isFormValid.value)
    }

    @Test
    fun `validateName sets error when cleared after entry`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        assertTrue(vm.validateName("Jo"))
        assertNull(vm.nameError.value)
        assertFalse(vm.validateName(""))
        assertEquals("Name is required", vm.nameError.value)
    }

    @Test
    fun `validateEmail sets error when cleared after entry`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        assertTrue(vm.validateEmail("jane@example.com"))
        assertNull(vm.emailError.value)
        assertFalse(vm.validateEmail(""))
        assertEquals("Email is required", vm.emailError.value)
    }

    @Test
    fun `isFormValid true when CVV untouched and card scheme has optional CVV`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        CardNumberContext.setCardScheme("117515279008103")
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), false)
        vm.onHostedFieldValidation(FormFieldType.EXPIRY_DATE(true), true)
        vm.updateNameValidity("Jane Doe")
        vm.updateEmailValidity("jane@example.com")
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `onHostedFieldValidation updates aggregate readout`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = BasicCheckoutViewModel(context, Spreedly(), skipAutoInitializeSdk = true)
        vm.onHostedFieldValidation(FormFieldType.CARD(true), true)
        vm.onHostedFieldValidation(FormFieldType.CVV(true), false)
        val readout = vm.inspectorUiState.value.aggregateReadout
        assertTrue(readout.contains("Security code (CVC)"))
        assertNotNull(readout)
    }
}
