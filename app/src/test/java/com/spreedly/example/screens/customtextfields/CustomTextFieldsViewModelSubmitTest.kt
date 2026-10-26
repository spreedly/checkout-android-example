package com.spreedly.example.screens.customtextfields

import androidx.test.core.app.ApplicationProvider
import com.spreedly.example.validation.DemoExpiryManifestTest
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.PaymentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit coverage for the FA10 demo submit path on [CustomTextFieldsViewModel].
 *
 * Verifies that:
 * - the SDK manifest rejection is correctly classified as a checkout-blocked snackbar,
 * - an unrelated UNKNOWN_ERROR while the switch is on does NOT produce a false-positive blocked message,
 * - the gate bypass is only active when the demo switch is on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomTextFieldsViewModelSubmitTest {

    private fun vm(): CustomTextFieldsViewModel =
        CustomTextFieldsViewModel(
            ApplicationProvider.getApplicationContext(),
            skipAutoInitializeSdk = true,
        )

    // --- skipPreSubmitFieldValidityGateForDemo ---

    @Test
    fun `skipPreSubmitFieldValidityGateForDemo is false when switch is off`() {
        val vm = vm()
        assertFalse(vm.skipPreSubmitFieldValidityGateForDemo())
    }

    @Test
    fun `skipPreSubmitFieldValidityGateForDemo is true when switch is on`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)
        assertTrue(vm.skipPreSubmitFieldValidityGateForDemo())
    }

    // --- resolvePaymentFailedSnackbar — FA10 manifest rejection path ---

    @Test
    fun `resolvePaymentFailedSnackbar shows checkout blocked when FA10 switch on and manifest mismatch`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        // The result the SDK emits when ExpiryFieldConfigurationValidator rejects the manifest
        val sdkManifestRejection =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )

        val snackbar = vm.resolvePaymentFailedSnackbar(sdkManifestRejection)

        assertNotNull("Expected checkout-blocked snackbar for FA10 manifest rejection", snackbar)
        assertEquals(DemoExpiryManifestTest.CHECKOUT_BLOCKED_MESSAGE, snackbar)
    }

    @Test
    fun `resolvePaymentFailedSnackbar shows generic message when FA10 switch is off`() {
        val vm = vm()
        // switch is off by default

        val failed =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )

        val snackbar = vm.resolvePaymentFailedSnackbar(failed)

        assertNotNull(snackbar)
        assertTrue(
            "Expected generic payment failure message, got: $snackbar",
            snackbar!!.startsWith("Payment failed:"),
        )
    }

    // --- F-01 edge case: unrelated UNKNOWN_ERROR must not be misclassified ---

    @Test
    fun `resolvePaymentFailedSnackbar does NOT show checkout blocked for unrelated UNKNOWN_ERROR`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        // Unrelated failure — different message, same error type
        val unrelatedFailure =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "Connection reset by peer",
            )

        val snackbar = vm.resolvePaymentFailedSnackbar(unrelatedFailure)

        assertNotNull(snackbar)
        assertFalse(
            "Unrelated UNKNOWN_ERROR must not produce checkout-blocked copy, got: $snackbar",
            snackbar!!.contains("Checkout blocked"),
        )
        assertTrue(snackbar.startsWith("Payment failed:"))
    }

    @Test
    fun `resolvePaymentFailedSnackbar does NOT show checkout blocked for UNKNOWN_ERROR with null message`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        val nullMessageFailure =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = null,
            )

        val snackbar = vm.resolvePaymentFailedSnackbar(nullMessageFailure)

        assertNotNull(snackbar)
        assertFalse(
            "Null-message UNKNOWN_ERROR must not produce checkout-blocked copy, got: $snackbar",
            snackbar!!.contains("Checkout blocked"),
        )
    }

    @Test
    fun `resolvePaymentFailedSnackbar does NOT show checkout blocked for API_ERROR`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        val apiError =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.API_ERROR,
                message = "card_number is invalid",
            )

        val snackbar = vm.resolvePaymentFailedSnackbar(apiError)

        assertNotNull(snackbar)
        assertFalse(snackbar!!.contains("Checkout blocked"))
    }

    // --- hostedFormFields reflects the FA10 switch state ---

    @Test
    fun `hostedFormFields includes invalid dual manifest when FA10 switch is on`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        val fields = vm.hostedFormFields()

        assertTrue(DemoExpiryManifestTest.isInvalidDualManifest(fields))
        assertTrue(fields.any { it is FormFieldType.EXPIRY_DATE })
        assertTrue(fields.any { it is FormFieldType.MONTH })
        assertTrue(fields.any { it is FormFieldType.YEAR })
    }

    @Test
    fun `hostedFormFields uses valid expiry manifest when FA10 switch is off`() {
        val vm = vm()

        val fields = vm.hostedFormFields()

        assertFalse(DemoExpiryManifestTest.isInvalidDualManifest(fields))
        assertTrue(fields.any { it is FormFieldType.EXPIRY_DATE })
        assertFalse(fields.any { it is FormFieldType.MONTH })
        assertFalse(fields.any { it is FormFieldType.YEAR })
    }

    // --- snackbar dedup guard ---

    @Test
    fun `resolvePaymentFailedSnackbar deduplicates identical consecutive failures`() {
        val vm = vm()
        vm.updateDemoInvalidExpiryManifest(true)

        val sdkRejection =
            PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = DemoExpiryManifestTest.SDK_CLIENT_VALIDATION_FAILED_MESSAGE,
            )

        val first = vm.resolvePaymentFailedSnackbar(sdkRejection)
        val second = vm.resolvePaymentFailedSnackbar(sdkRejection)

        assertNotNull("First call must return the snackbar", first)
        assertNull("Second identical call must be deduped to null", second)
    }
}
