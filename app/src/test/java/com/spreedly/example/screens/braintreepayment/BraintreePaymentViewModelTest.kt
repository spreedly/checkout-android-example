package com.spreedly.example.screens.braintreepayment

import android.app.Activity
import android.content.Context
import com.spreedly.braintree.BraintreeAPMPaymentType
import com.spreedly.braintree.SpreedlyBraintreeAPMCheckout

import com.spreedly.example.api.BraintreeConfirmState
import com.spreedly.example.api.BraintreeFields
import com.spreedly.example.api.BraintreeGatewaySpecificFields
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.api.SpreedlyPurchaseResponse
import com.spreedly.example.api.SpreedlyPurchaseTransaction
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BraintreePaymentViewModel.
 *
 * Tests the stage machine, payment type selection, product selection,
 * payment result handling (including nonce confirmation flow),
 * and validation logic for the Braintree payment flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BraintreePaymentViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var mockSdk: Spreedly
    private lateinit var mockPurchaseClient: SpreedlyPurchaseAPIClient

    private val paymentResultFlow = MutableStateFlow<PaymentResult>(PaymentResult.Initial)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockActivity = mockk(relaxed = true)
        mockSdk = mockk(relaxed = true)
        mockPurchaseClient = mockk(relaxed = true)

        every { mockSdk.paymentResultFlow } returns paymentResultFlow
        every { mockSdk.isInitialized } returns false
        every { mockContext.applicationContext } returns mockContext

        mockkObject(SpreedlyBraintreeAPMCheckout)
        every { SpreedlyBraintreeAPMCheckout.present(any(), any()) } returns Unit
        every { SpreedlyBraintreeAPMCheckout.finalizeIfActive() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): BraintreePaymentViewModel = BraintreePaymentViewModel(
            context = mockContext,
            sdk = mockSdk,
            purchaseClient = mockPurchaseClient,
        )

    // ========================================================================
    // Initial State Tests
    // ========================================================================

    @Test
    fun `should start in IDLE stage`() {
        val viewModel = createViewModel()
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should have no selected product initially`() {
        val viewModel = createViewModel()
        assertNull(viewModel.selectedProduct.value)
    }

    @Test
    fun `should have PAYPAL selected by default`() {
        val viewModel = createViewModel()
        assertEquals(BraintreeAPMPaymentType.PAYPAL, viewModel.selectedPaymentType.value)
    }

    @Test
    fun `should have no error message initially`() {
        val viewModel = createViewModel()
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should have no success message initially`() {
        val viewModel = createViewModel()
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should have available products list`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.products.isNotEmpty())
    }

    @Test
    fun `should have available payment types list`() {
        val viewModel = createViewModel()
        assertEquals(BraintreeAPMPaymentType.entries, viewModel.paymentTypes)
        assertTrue(viewModel.paymentTypes.contains(BraintreeAPMPaymentType.PAYPAL))
        assertTrue(viewModel.paymentTypes.contains(BraintreeAPMPaymentType.VENMO))
    }

    // ========================================================================
    // Product Selection Tests
    // ========================================================================

    @Test
    fun `should update selected product on selectProduct`() {
        val viewModel = createViewModel()
        val product = viewModel.products.first()

        viewModel.selectProduct(product)

        assertEquals(product, viewModel.selectedProduct.value)
    }

    @Test
    fun `should clear error message on selectProduct`() {
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Previous error")

        viewModel.selectProduct(viewModel.products.first())

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should clear success message on selectProduct`() {
        val viewModel = createViewModel()
        setSuccessMessage(viewModel, "Previous success")

        viewModel.selectProduct(viewModel.products.first())

        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Payment Type Selection Tests
    // ========================================================================

    @Test
    fun `should update selected payment type on selectPaymentType`() {
        val viewModel = createViewModel()

        viewModel.selectPaymentType(BraintreeAPMPaymentType.VENMO)

        assertEquals(BraintreeAPMPaymentType.VENMO, viewModel.selectedPaymentType.value)
    }

    @Test
    fun `should clear error message on selectPaymentType`() {
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Previous error")

        viewModel.selectPaymentType(BraintreeAPMPaymentType.VENMO)

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should clear success message on selectPaymentType`() {
        val viewModel = createViewModel()
        setSuccessMessage(viewModel, "Previous success")

        viewModel.selectPaymentType(BraintreeAPMPaymentType.VENMO)

        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // startPayment Validation Tests
    // ========================================================================

    @Test
    fun `startPayment should show error when no product selected`() {
        val viewModel = createViewModel()

        viewModel.startPayment(mockActivity)

        assertEquals("Please select a product", viewModel.errorMessage.value)
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `startPayment should show error when SDK not initialized`() {
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        every { mockSdk.isInitialized } returns false

        viewModel.startPayment(mockActivity)

        assertEquals("SDK is still initializing, please wait...", viewModel.errorMessage.value)
    }

    // ========================================================================
    // Payment Result Handling Tests
    // ========================================================================

    @Test
    fun `should trigger confirmation when payment completes with nonce`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(
                transactionToken = "txn_123",
                state = BraintreeConfirmState.SUCCESSFUL,
                nonce = "bt_nonce_abc",
                paymentMethodType = "paypal",
            )
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_123",
                state = "succeeded",
                succeeded = true,
            ),
        )

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_123",
            nonce = "bt_nonce_abc",
            deviceData = "{\"correlation\":\"123\"}",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment successful. The transaction has been completed successfully.",
            viewModel.successMessage.value,
        )
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show pending message when confirmation returns processing state`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_pending",
                state = "processing",
                succeeded = false,
            ),
        )

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_pending",
            nonce = "nonce_123",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment is being processed. Final confirmation may take a moment.",
            viewModel.successMessage.value,
        )
    }

    @Test
    fun `should show error when confirmation throws exception`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        } throws RuntimeException("Network error")

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_err",
            nonce = "nonce_err",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        assertEquals("Confirmation failed: Network error", viewModel.errorMessage.value)
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show success when payment completes without nonce and succeeded`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_no_nonce",
            state = "succeeded",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment successful. The transaction has been completed successfully.",
            viewModel.successMessage.value,
        )
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show pending message when payment completes without nonce and pending`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_pending_no_nonce",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment is being processed. Final confirmation may take a moment.",
            viewModel.successMessage.value,
        )
    }

    @Test
    fun `should show error message when payment fails`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_fail")
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "Braintree auth failed",
            state = null,
        )
        advanceUntilIdle()

        // Then
        assertEquals("Braintree auth failed", viewModel.errorMessage.value)
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show default error message when payment fails without message`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_fail_default")
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = null,
            state = null,
        )
        advanceUntilIdle()

        // Then - default payment type is PayPal
        assertEquals("PayPal payment failed.", viewModel.errorMessage.value)
    }

    @Test
    fun `should show canceled message when payment is canceled`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_cancel")
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Canceled
        advanceUntilIdle()

        // Then - default payment type is PayPal
        assertEquals("PayPal payment was canceled.", viewModel.errorMessage.value)
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should call braintreeConfirm with Failed state when payment fails`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_fail_confirm")
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "PayPal payment options are invalid",
            state = null,
        )
        advanceUntilIdle()

        // Then
        coVerify {
            mockPurchaseClient.braintreeConfirm(
                transactionToken = "txn_fail_confirm",
                state = BraintreeConfirmState.FAILED,
                message = "PayPal payment options are invalid",
                paymentMethodType = "paypal",
            )
        }
    }

    @Test
    fun `should call braintreeConfirm with Cancelled state when payment is canceled`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_cancel_confirm")
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Canceled
        advanceUntilIdle()

        // Then
        coVerify {
            mockPurchaseClient.braintreeConfirm(
                transactionToken = "txn_cancel_confirm",
                state = BraintreeConfirmState.CANCELLED,
                message = "PayPal payment was canceled.",
                paymentMethodType = "paypal",
            )
        }
    }

    @Test
    fun `should still show error message when confirm call fails for Failed state`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        setCurrentTransactionToken(viewModel, "txn_fail_net")
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        } throws RuntimeException("Network error")

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "Braintree auth failed",
            state = null,
        )
        advanceUntilIdle()

        // Then - error message still shown despite confirm call failure
        assertEquals("Braintree auth failed", viewModel.errorMessage.value)
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should skip confirm when currentTransactionToken is null on Failed`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When - no currentTransactionToken set
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "Some error",
            state = null,
        )
        advanceUntilIdle()

        // Then - confirm not called, but error message still shown
        coVerify(exactly = 0) {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        }
        assertEquals("Some error", viewModel.errorMessage.value)
    }

    @Test
    fun `should skip confirm when currentTransactionToken is null on Canceled`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When - no currentTransactionToken set
        paymentResultFlow.value = PaymentResult.Canceled
        advanceUntilIdle()

        // Then - confirm not called, but canceled message still shown
        coVerify(exactly = 0) {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        }
        assertEquals("PayPal payment was canceled.", viewModel.errorMessage.value)
    }

    @Test
    fun `should ignore payment result when not in CHECKOUT stage`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_ignored",
            state = "succeeded",
        )
        advanceUntilIdle()

        // Then
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should transition to CONFIRMING stage when nonce received`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_stage",
                state = "succeeded",
                succeeded = true,
            ),
        )

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_stage",
            nonce = "nonce_stage",
            state = "pending",
        )
        advanceUntilIdle()

        // Then - after confirmation completes, should be back to IDLE
        assertEquals(BraintreePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `confirmation should call braintreeConfirm with correct payment type`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        viewModel.selectPaymentType(BraintreeAPMPaymentType.VENMO)
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        coEvery {
            mockPurchaseClient.braintreeConfirm(any(), any(), any(), any(), any())
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_venmo",
                state = "succeeded",
                succeeded = true,
            ),
        )

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_venmo",
            nonce = "venmo_nonce",
            deviceData = "device_data_123",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        coVerify {
            mockPurchaseClient.braintreeConfirm(
                transactionToken = "txn_venmo",
                state = BraintreeConfirmState.SUCCESSFUL,
                nonce = "venmo_nonce",
                paymentMethodType = "venmo",
            )
        }
    }

    // ========================================================================
    // PayPal flow_type Tests
    // ========================================================================

    @Test
    fun `startPayment with PayPal should include paypal_flow_type checkout`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        viewModel.selectPaymentType(BraintreeAPMPaymentType.PAYPAL)

        coEvery {
            mockPurchaseClient.braintreePurchase(any(), any(), any(), any())
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_paypal",
                state = "pending",
                succeeded = false,
            ),
        )

        // When
        viewModel.startPayment(mockActivity)
        advanceUntilIdle()

        // Then
        coVerify {
            mockPurchaseClient.braintreePurchase(
                paymentMethodType = "paypal",
                amount = any(),
                currencyCode = any(),
                gatewaySpecificFields = BraintreeGatewaySpecificFields(
                    braintree = BraintreeFields(paypalFlowType = "checkout"),
                ),
            )
        }
    }

    @Test
    fun `startPayment with Venmo should include venmo_flow_type multi_use`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        viewModel.selectPaymentType(BraintreeAPMPaymentType.VENMO)

        coEvery {
            mockPurchaseClient.braintreePurchase(any(), any(), any(), any())
        } returns SpreedlyPurchaseResponse(
            transaction = SpreedlyPurchaseTransaction(
                token = "txn_venmo",
                state = "pending",
                succeeded = false,
            ),
        )

        // When
        viewModel.startPayment(mockActivity)
        advanceUntilIdle()

        // Then
        coVerify {
            mockPurchaseClient.braintreePurchase(
                paymentMethodType = "venmo",
                amount = any(),
                currencyCode = any(),
                gatewaySpecificFields = BraintreeGatewaySpecificFields(
                    braintree = BraintreeFields(
                        venmoFlowType = "multi_use",
                        venmoProfileId = "12345",
                    ),
                ),
            )
        }
    }

    // ========================================================================
    // onResumeFromCheckout Tests
    // ========================================================================

    @Test
    fun `onResumeFromCheckout should call finalizeIfActive when in CHECKOUT stage`() = runTest {
        // Given
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)

        // When
        viewModel.onResumeFromCheckout()

        // Then
        io.mockk.verify { SpreedlyBraintreeAPMCheckout.finalizeIfActive() }
    }

    @Test
    fun `onResumeFromCheckout should not call finalizeIfActive when in IDLE stage`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onResumeFromCheckout()

        // Then
        io.mockk.verify(exactly = 0) { SpreedlyBraintreeAPMCheckout.finalizeIfActive() }
    }

    // ========================================================================
    // clearMessages Tests
    // ========================================================================

    @Test
    fun `clearMessages should clear error message`() {
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Some error")

        viewModel.clearMessages()

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearMessages should clear success message`() {
        val viewModel = createViewModel()
        setSuccessMessage(viewModel, "Some success")

        viewModel.clearMessages()

        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    private fun setStageToCheckout(viewModel: BraintreePaymentViewModel) {
        val field = BraintreePaymentViewModel::class.java.getDeclaredField("_stage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stageFlow = field.get(viewModel) as MutableStateFlow<BraintreePaymentViewModel.Stage>
        stageFlow.value = BraintreePaymentViewModel.Stage.CHECKOUT
    }

    private fun startPaymentResultObserver(viewModel: BraintreePaymentViewModel) {
        val method =
            BraintreePaymentViewModel::class.java.getDeclaredMethod("startPaymentResultObserver")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    private fun setErrorMessage(viewModel: BraintreePaymentViewModel, message: String) {
        val field = BraintreePaymentViewModel::class.java.getDeclaredField("_errorMessage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<String?>
        flow.value = message
    }

    private fun setSuccessMessage(viewModel: BraintreePaymentViewModel, message: String) {
        val field = BraintreePaymentViewModel::class.java.getDeclaredField("_successMessage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<String?>
        flow.value = message
    }

    private fun setCurrentTransactionToken(viewModel: BraintreePaymentViewModel, token: String?) {
        val field =
            BraintreePaymentViewModel::class.java.getDeclaredField("currentTransactionToken")
        field.isAccessible = true
        field.set(viewModel, token)
    }
}
