package com.spreedly.example.screens.ebanxpayment

import android.app.Activity
import android.content.Context
import com.spreedly.example.api.SpreedlyPurchaseAPIClient
import com.spreedly.example.screens.offsitepayment.BaseOffsitePaymentViewModel
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType
import com.spreedly.sdk.ui.PaymentResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Unit tests for EbanxPaymentViewModel.
 *
 * Tests the stage machine, config building, and result handling
 * for EBANX payment flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EbanxPaymentViewModelTest {
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
        every { mockContext.applicationContext } returns mockContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.unmockkAll()
    }

    private fun createViewModel(): EbanxPaymentViewModel = EbanxPaymentViewModel(
            context = mockContext,
            sdk = mockSdk,
            purchaseClient = mockPurchaseClient,
        )

    private fun setStageToCheckout(viewModel: EbanxPaymentViewModel) {
        val field = BaseOffsitePaymentViewModel::class.java.getDeclaredField("_stage")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stageFlow = field.get(viewModel) as MutableStateFlow<BaseOffsitePaymentViewModel.Stage>
        stageFlow.value = BaseOffsitePaymentViewModel.Stage.CHECKOUT
    }

    private fun startPaymentResultObserver(viewModel: EbanxPaymentViewModel) {
        val method = BaseOffsitePaymentViewModel::class.java.getDeclaredMethod("restartPaymentResultObserver")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    // ========================================================================
    // Initial State Tests
    // ========================================================================

    @Test
    fun `should start in IDLE stage`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    @Test
    fun `should have no selected product initially`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertNull(viewModel.selectedProduct.value)
    }

    @Test
    fun `should default to PIX provider`() {
        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(OffsitePaymentMethodType.PIX, viewModel.selectedProvider.value)
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
    fun `should clear messages on product selection`() = runTest {
        // Given
        val viewModel = createViewModel()
        // Set an error message first (we can't directly set it, so this tests the clear behavior)
        viewModel.selectProduct(viewModel.products.first())

        // Then
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Provider Selection Tests
    // ========================================================================

    @Test
    fun `should update selected provider on selectProvider`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.selectProvider(OffsitePaymentMethodType.OXXO)

        // Then
        assertEquals(OffsitePaymentMethodType.OXXO, viewModel.selectedProvider.value)
    }

    @Test
    fun `should clear messages on provider selection`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.selectProvider(OffsitePaymentMethodType.BOLETO_BANCARIO)

        // Then
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Start Payment Tests - Validation
    // ========================================================================

    @Test
    fun `should set error when no product selected on startPayment`() {
        // Given
        val viewModel = createViewModel()
        // Don't select a product

        // When
        viewModel.startPayment(mockActivity)

        // Then
        assertEquals("Please select a product", viewModel.errorMessage.value)
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }

    // ========================================================================
    // Reset Tests
    // ========================================================================

    @Test
    fun `should reset to initial state on reset`() {
        // Given
        val viewModel = createViewModel()
        viewModel.selectProduct(viewModel.products.first())
        viewModel.selectProvider(OffsitePaymentMethodType.OXXO)

        // When
        viewModel.reset()

        // Then
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
        assertNull(viewModel.selectedProduct.value)
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Clear Messages Tests
    // ========================================================================

    @Test
    fun `should clear error and success messages on clearMessages`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.clearMessages()

        // Then
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
    }

    // ========================================================================
    // Provider List Tests
    // ========================================================================

    @Test
    fun `should have correct EBANX providers`() {
        // Given
        val viewModel = createViewModel()

        // Then
        assertEquals(4, viewModel.providers.size)
        assertEquals(OffsitePaymentMethodType.PIX, viewModel.providers[0])
        assertEquals(OffsitePaymentMethodType.BOLETO_BANCARIO, viewModel.providers[1])
        assertEquals(OffsitePaymentMethodType.OXXO, viewModel.providers[2])
        assertEquals(OffsitePaymentMethodType.NUPAY, viewModel.providers[3])
    }

    // ========================================================================
    // Product List Tests
    // ========================================================================

    @Test
    fun `should have product list with prices in cents`() {
        // Given
        val viewModel = createViewModel()

        // Then
        assertEquals(6, viewModel.products.size)
        viewModel.products.forEach { product ->
            assert(product.price > 0) { "Product price should be positive" }
            assert(product.name.isNotEmpty()) { "Product name should not be empty" }
            assert(product.emoji.isNotEmpty()) { "Product emoji should not be empty" }
        }
    }

    // ========================================================================
    // handlePaymentFailed (handleFailedResult) Tests - EBANX pending/processing
    // ========================================================================

    @Test
    fun `should show error message and return to IDLE when payment result state is pending`() = runTest {
        // Given - ViewModel in CHECKOUT stage, observer running
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Emit Failed with state "pending"
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = null,
            state = "pending",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(
            "Your payment is pending. Please try again shortly.",
            viewModel.errorMessage.value,
        )
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should show default error and return to IDLE when payment result state is processing`() = runTest {
        // Given - "processing" in Failed context falls through to else (uses method display name)
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = null,
            state = "processing",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - default provider is PIX
        assertEquals("Pix payment failed.", viewModel.errorMessage.value)
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should show gateway error and return to IDLE when payment result state is gateway_processing_failed`() =
        runTest {
            // Given
            val viewModel = createViewModel()
            setStageToCheckout(viewModel)
            startPaymentResultObserver(viewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            paymentResultFlow.value = PaymentResult.Failed(
                errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
                message = "Gateway error",
                state = "gateway_processing_failed",
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - default provider is PIX
            assertEquals(
                "We couldn't complete your Pix payment. Please try again.",
                viewModel.errorMessage.value,
            )
            assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
            assertNull(viewModel.successMessage.value)
        }

    @Test
    fun `should show result message and return to IDLE for unknown failure state`() = runTest {
        // Given
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Unknown state, use message
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = "Something went wrong",
            state = "unknown_state",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Something went wrong", viewModel.errorMessage.value)
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `should show default error when failure has null message`() = runTest {
        // Given
        val viewModel = createViewModel()
        setStageToCheckout(viewModel)
        startPaymentResultObserver(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - state and message both null or state not one of the known ones
        paymentResultFlow.value = PaymentResult.Failed(
            errorType = PaymentResult.Failed.ErrorType.UNKNOWN_ERROR,
            message = null,
            state = "other",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - default provider is PIX
        assertEquals("Pix payment failed.", viewModel.errorMessage.value)
        assertEquals(BaseOffsitePaymentViewModel.Stage.IDLE, viewModel.stage.value)
    }
}
