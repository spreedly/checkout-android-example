package com.spreedly.example.screens.stripeapmpayment

import android.app.Activity
import android.content.Context

import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StripeAPMPaymentViewModel.
 *
 * Tests the stage machine, APM type selection, product selection,
 * and payment result handling for Stripe APM payment flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StripeAPMPaymentViewModelTest {
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.unmockkAll()
    }

    private fun createViewModel(): StripeAPMPaymentViewModel = StripeAPMPaymentViewModel(
            context = mockContext,
            sdk = mockSdk,
            purchaseClient = mockPurchaseClient,
        )

    // ========================================================================
    // Initial State Tests
    // ========================================================================

    @Test
    fun `should start in IDLE stage`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should have no selected product initially`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertNull(viewModel.selectedProduct.value)
    }

    @Test
    fun `should have first APM type selected by default`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(1, viewModel.selectedApmTypes.value.size)
        assertEquals(
            "ideal",
            viewModel.selectedApmTypes.value
            .first()
            .id,
        )
    }

    @Test
    fun `should have no error message initially`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should have no success message initially`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should have available APM types list`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.apmTypesList.isNotEmpty())
        assertTrue(viewModel.apmTypesList.any { it.id == "ideal" })
        assertTrue(viewModel.apmTypesList.any { it.id == "bancontact" })
    }

    @Test
    fun `should have available products list`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.products.isNotEmpty())
    }

    // ========================================================================
    // Product Selection Tests
    // ========================================================================

    @Test
    fun `should update selected product on selectProduct`() {
        // Given
        val viewModel = createViewModel()
        val product = viewModel.products.first()

        // When
        viewModel.selectProduct(product)

        // Then
        assertEquals(product, viewModel.selectedProduct.value)
    }

    @Test
    fun `should clear error message on selectProduct`() {
        // Given
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Previous error")

        // When
        viewModel.selectProduct(viewModel.products.first())

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `should clear success message on selectProduct`() {
        // Given
        val viewModel = createViewModel()
        setSuccessMessage(viewModel, "Previous success")

        // When
        viewModel.selectProduct(viewModel.products.first())

        // Then
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // APM Type Selection Tests (Multi-select)
    // ========================================================================

    @Test
    fun `should add APM type when toggling unselected type`() {
        // Given
        val viewModel = createViewModel()
        val bancontact = viewModel.apmTypesList.find { it.id == "bancontact" }!!

        // When
        viewModel.toggleApmType(bancontact)

        // Then
        assertTrue(viewModel.selectedApmTypes.value.contains(bancontact))
        assertEquals(2, viewModel.selectedApmTypes.value.size)
    }

    @Test
    fun `should remove APM type when toggling already selected type`() {
        // Given
        val viewModel = createViewModel()
        val ideal = viewModel.apmTypesList.find { it.id == "ideal" }!!

        // When
        viewModel.toggleApmType(ideal)

        // Then
        assertFalse(viewModel.selectedApmTypes.value.contains(ideal))
        assertEquals(0, viewModel.selectedApmTypes.value.size)
    }

    @Test
    fun `should allow selecting multiple APM types`() {
        // Given
        val viewModel = createViewModel()
        val bancontact = viewModel.apmTypesList.find { it.id == "bancontact" }!!

        // When
        viewModel.toggleApmType(bancontact)

        // Then
        assertEquals(2, viewModel.selectedApmTypes.value.size)
        assertTrue(viewModel.selectedApmTypes.value.any { it.id == "ideal" })
        assertTrue(viewModel.selectedApmTypes.value.any { it.id == "bancontact" })
    }

    @Test
    fun `should allow deselecting all APM types`() {
        // Given
        val viewModel = createViewModel()
        val ideal = viewModel.apmTypesList.find { it.id == "ideal" }!!

        // When
        viewModel.toggleApmType(ideal)

        // Then
        assertTrue(viewModel.selectedApmTypes.value.isEmpty())
    }

    @Test
    fun `should clear error message on toggleApmType`() {
        // Given
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Previous error")
        val bancontact = viewModel.apmTypesList.find { it.id == "bancontact" }!!

        // When
        viewModel.toggleApmType(bancontact)

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    // ========================================================================
    // startPayment Validation Tests
    // ========================================================================

    @Test
    fun `startPayment should show error when no product selected`() {
        // Given
        val viewModel = createViewModel()
        // No product selected

        // When
        viewModel.startPayment(mockActivity)

        // Then
        assertEquals("Please select a product", viewModel.errorMessage.value)
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `startPayment should show error when no APM types selected`() {
        // Given
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        val ideal = viewModel.apmTypesList.find { it.id == "ideal" }!!
        viewModel.toggleApmType(ideal) // Deselect the default

        // When
        viewModel.startPayment(mockActivity)

        // Then
        assertEquals("Please select at least one payment method", viewModel.errorMessage.value)
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `startPayment should show error when SDK not initialized`() {
        // Given
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        every { mockSdk.isInitialized } returns false

        // When
        viewModel.startPayment(mockActivity)

        // Then
        assertEquals("SDK is still initializing, please wait...", viewModel.errorMessage.value)
    }

    // ========================================================================
    // Payment Result Handling Tests
    // ========================================================================

    @Test
    fun `should show success message when payment completes with succeeded state`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_123",
            state = "succeeded",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment successful. The transaction has been completed successfully.",
            viewModel.successMessage.value,
        )
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show pending message when payment completes with pending state`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_123",
            state = "pending",
        )
        advanceUntilIdle()

        // Then
        assertEquals(
            "Payment submitted. Awaiting final confirmation from the payment provider.",
            viewModel.successMessage.value,
        )
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show error message when payment fails`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "Card declined",
            state = null,
        )
        advanceUntilIdle()

        // Then
        assertEquals("Card declined", viewModel.errorMessage.value)
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should show default error message when payment fails without message`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = null,
            state = null,
        )
        advanceUntilIdle()

        // Then - default selected APM is iDEAL
        assertEquals("iDEAL payment failed.", viewModel.errorMessage.value)
    }

    @Test
    fun `should show canceled message when payment is canceled`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Canceled
        advanceUntilIdle()

        // Then - default selected APM is iDEAL
        assertEquals("iDEAL payment was canceled.", viewModel.errorMessage.value)
        assertEquals(StripeAPMPaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should ignore payment result when not in CHECKOUT stage`() = runTest {
        // Given
        every { mockSdk.isInitialized } returns true
        val viewModel = createViewModel()
        // Stage is IDLE, not CHECKOUT
        startPaymentResultObserver(viewModel)

        // When
        paymentResultFlow.value = PaymentResult.Completed(
            token = "txn_123",
            state = "succeeded",
        )
        advanceUntilIdle()

        // Then - should be ignored, no success message
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // clearMessages Tests
    // ========================================================================

    @Test
    fun `clearMessages should clear error message`() {
        // Given
        val viewModel = createViewModel()
        setErrorMessage(viewModel, "Some error")

        // When
        viewModel.clearMessages()

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearMessages should clear success message`() {
        // Given
        val viewModel = createViewModel()
        setSuccessMessage(viewModel, "Some success")

        // When
        viewModel.clearMessages()

        // Then
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    private fun setStageToCheckout(viewModel: StripeAPMPaymentViewModel) {
        val field = StripeAPMPaymentViewModel::class.java.getDeclaredField("_stage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stageFlow = field.get(viewModel) as MutableStateFlow<StripeAPMPaymentViewModel.Stage>
        stageFlow.value = StripeAPMPaymentViewModel.Stage.CHECKOUT
    }

    private fun startPaymentResultObserver(viewModel: StripeAPMPaymentViewModel) {
        val method = StripeAPMPaymentViewModel::class.java.getDeclaredMethod("startPaymentResultObserver")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    private fun setErrorMessage(viewModel: StripeAPMPaymentViewModel, message: String) {
        val field = StripeAPMPaymentViewModel::class.java.getDeclaredField("_errorMessage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<String?>
        flow.value = message
    }

    private fun setSuccessMessage(viewModel: StripeAPMPaymentViewModel, message: String) {
        val field = StripeAPMPaymentViewModel::class.java.getDeclaredField("_successMessage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<String?>
        flow.value = message
    }
}
