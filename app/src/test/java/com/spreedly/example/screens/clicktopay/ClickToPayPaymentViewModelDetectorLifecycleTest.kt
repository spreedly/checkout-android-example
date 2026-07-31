package com.spreedly.example.screens.clicktopay

import com.spreedly.clicktopay.ClickToPaySavedCardsDetectorFailure
import com.spreedly.clicktopay.ClickToPaySavedCardsDetectorResult
import com.spreedly.example.screens.common.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClickToPayPaymentViewModelDetectorLifecycleTest {
    private val sampleProduct =
        Product(
            name = "Test",
            description = "Test",
            price = 100,
            emoji = "🧪",
        )

    @Test
    fun `canStartPayment returns true for DetectionFailed when idle and product selected`() {
        assertTrue(
            ClickToPayPaymentViewModel.canStartPayment(
                stage = ClickToPayPaymentViewModel.Stage.IDLE,
                selectedProduct = sampleProduct,
                deviceRecognition = DeviceRecognitionState.DetectionFailed,
            ),
        )
    }

    @Test
    fun `canStartPayment returns false while device recognition is checking`() {
        assertFalse(
            ClickToPayPaymentViewModel.canStartPayment(
                stage = ClickToPayPaymentViewModel.Stage.IDLE,
                selectedProduct = sampleProduct,
                deviceRecognition = DeviceRecognitionState.Checking,
            ),
        )
    }

    @Test
    fun `hasValidCustomerIdentity for DetectionFailed requires email or phone like not recognized`() {
        assertFalse(
            ClickToPayPaymentViewModel.hasValidCustomerIdentity(
                deviceRecognition = DeviceRecognitionState.DetectionFailed,
                email = "",
                phoneNumber = "",
                phoneCountryCode = "",
            ),
        )
        assertTrue(
            ClickToPayPaymentViewModel.hasValidCustomerIdentity(
                deviceRecognition = DeviceRecognitionState.DetectionFailed,
                email = "shopper@example.com",
                phoneNumber = "",
                phoneCountryCode = "",
            ),
        )
    }

    @Test
    fun `hasValidCustomerIdentity for Recognized allows blank contact fields`() {
        assertTrue(
            ClickToPayPaymentViewModel.hasValidCustomerIdentity(
                deviceRecognition = DeviceRecognitionState.Recognized,
                email = "",
                phoneNumber = "",
                phoneCountryCode = "",
            ),
        )
    }

    @Test
    fun `awaitSavedCardsDetectorTearDown blocks until dispose when no controller`() = runTest {
        val awaiter = SavedCardsDetectorTearDownAwaiter()
        val tearDown =
            async {
                awaitSavedCardsDetectorTearDown(
                    hadActiveDetector = true,
                    controllerAwaitTearDown = null,
                    awaiter = awaiter,
                )
            }
        testScheduler.runCurrent()
        assertFalse(tearDown.isCompleted)

        awaiter.signalDisposed()
        assertTrue(tearDown.await())
    }

    @Test
    fun `awaitSavedCardsDetectorTearDown completes when controller tear down finishes first`() = runTest {
        val awaiter = SavedCardsDetectorTearDownAwaiter()
        var controllerTearDownDone = false

        assertTrue(
            awaitSavedCardsDetectorTearDown(
                hadActiveDetector = true,
                controllerAwaitTearDown = {
                    controllerTearDownDone = true
                    true
                },
                awaiter = awaiter,
            ),
        )

        assertTrue(controllerTearDownDone)
    }

    @Test
    fun `deviceRecognitionForDetectorResult maps saved cards to recognized`() {
        assertEquals(
            DeviceRecognitionState.Recognized,
            deviceRecognitionForDetectorResult(
                ClickToPaySavedCardsDetectorResult(hasSavedCards = true),
            ),
        )
    }

    @Test
    fun `deviceRecognitionForDetectorResult maps empty success to not recognized`() {
        assertEquals(
            DeviceRecognitionState.NotRecognized,
            deviceRecognitionForDetectorResult(
                ClickToPaySavedCardsDetectorResult(hasSavedCards = false),
            ),
        )
    }

    @Test
    fun `deviceRecognitionForDetectorResult maps failure to detection failed`() {
        assertEquals(
            DeviceRecognitionState.DetectionFailed,
            deviceRecognitionForDetectorResult(
                ClickToPaySavedCardsDetectorResult(
                    hasSavedCards = false,
                    failure = ClickToPaySavedCardsDetectorFailure.Timeout,
                ),
            ),
        )
        assertEquals(
            DeviceRecognitionState.DetectionFailed,
            deviceRecognitionForDetectorResult(
                ClickToPaySavedCardsDetectorResult(
                    hasSavedCards = false,
                    failure = ClickToPaySavedCardsDetectorFailure.InitFailed,
                ),
            ),
        )
        assertEquals(
            DeviceRecognitionState.DetectionFailed,
            deviceRecognitionForDetectorResult(
                ClickToPaySavedCardsDetectorResult(
                    hasSavedCards = false,
                    failure = ClickToPaySavedCardsDetectorFailure.Error,
                ),
            ),
        )
    }

    @Test
    fun `shouldRemountSavedCardsDetector allows remount when detection failed`() {
        assertTrue(
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.DetectionFailed,
                remountedDetectorForCheckoutGeneration = -1,
                checkoutSessionGeneration = 1,
            ),
        )
    }

    @Test
    fun `shouldRemountSavedCardsDetector returns false when using different email`() {
        assertFalse(
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.UsingDifferentEmail,
                remountedDetectorForCheckoutGeneration = -1,
                checkoutSessionGeneration = 1,
            ),
        )
    }

    @Test
    fun `shouldRemountSavedCardsDetector returns false when already remounted for session`() {
        assertFalse(
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.Recognized,
                remountedDetectorForCheckoutGeneration = 2,
                checkoutSessionGeneration = 2,
            ),
        )
    }

    @Test
    fun `shouldRemountSavedCardsDetector allows at most one remount per checkout session generation`() {
        val generation = 3
        val first =
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.Recognized,
                remountedDetectorForCheckoutGeneration = -1,
                checkoutSessionGeneration = generation,
            )
        assertTrue(first)

        val secondAfterAcquire =
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.Recognized,
                remountedDetectorForCheckoutGeneration = generation,
                checkoutSessionGeneration = generation,
            )
        assertFalse(secondAfterAcquire)
    }

    @Test
    fun `awaitCheckoutInactiveForRemount polls until checkout inactive`() = runTest {
        var checks = 0
        val inactive =
            async {
                awaitCheckoutInactiveForRemount(
                    isCheckoutActive = {
                        ++checks <= 2
                    },
                    delayMs = 50,
                    timeoutMs = 10_000,
                    delay = { kotlinx.coroutines.delay(it) },
                    nowMs = { 0L },
                )
            }
        testScheduler.runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(50)
        testScheduler.runCurrent()
        assertEquals(2, checks)
        advanceTimeBy(50)
        testScheduler.runCurrent()
        assertTrue(inactive.await())
        assertEquals(3, checks)
    }

    @Test
    fun `awaitCheckoutInactiveForRemount returns false when checkout still active after timeout`() =
        runTest {
            val inactive =
                awaitCheckoutInactiveForRemount(
                    isCheckoutActive = { true },
                    delayMs = 50,
                    timeoutMs = 200,
                    delay = { kotlinx.coroutines.delay(it) },
                    nowMs = { testScheduler.currentTime },
                )
            assertFalse(inactive)
        }

    @Test
    fun `awaitCheckoutInactiveUntilRemount waits until checkout inactive`() = runTest {
        var checks = 0
        val inactive =
            async {
                awaitCheckoutInactiveUntilRemount(
                    isCheckoutActive = {
                        ++checks <= 2
                    },
                    delayMs = 50,
                    delay = { kotlinx.coroutines.delay(it) },
                )
            }
        testScheduler.runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(50)
        testScheduler.runCurrent()
        assertEquals(2, checks)
        advanceTimeBy(50)
        testScheduler.runCurrent()
        inactive.await()
        assertEquals(3, checks)
    }

    @Test
    fun `awaitCheckoutInactiveUntilRemount keeps waiting past remount timeout window`() = runTest {
        var checkoutActive = true
        val inactive =
            async {
                awaitCheckoutInactiveUntilRemount(
                    isCheckoutActive = { checkoutActive },
                    delayMs = 50,
                    delay = { kotlinx.coroutines.delay(it) },
                )
            }
        testScheduler.runCurrent()
        advanceTimeBy(CHECKOUT_INACTIVE_REMOUNT_TIMEOUT_MS + 200)
        testScheduler.runCurrent()
        assertFalse(inactive.isCompleted)
        checkoutActive = false
        advanceTimeBy(50)
        testScheduler.runCurrent()
        inactive.await()
    }

    @Test
    fun `awaitCheckoutInactiveUntilRemount stops when coroutine cancelled`() = runTest {
        val inactive =
            async {
                awaitCheckoutInactiveUntilRemount(
                    isCheckoutActive = { true },
                    delayMs = 50,
                    delay = { kotlinx.coroutines.delay(it) },
                )
            }
        testScheduler.runCurrent()
        inactive.cancel()
        try {
            inactive.await()
        } catch (_: CancellationException) {
        }
        assertTrue(inactive.isCancelled)
    }

    @Test
    fun `awaitSavedCardsDetectorTearDown completes after timeout when dispose never signals`() = runTest {
        val awaiter = SavedCardsDetectorTearDownAwaiter()
        val tearDown =
            async {
                awaitSavedCardsDetectorTearDown(
                    hadActiveDetector = true,
                    controllerAwaitTearDown = null,
                    awaiter = awaiter,
                    timeoutMs = 5_000,
                )
            }
        testScheduler.runCurrent()
        assertFalse(tearDown.isCompleted)
        advanceTimeBy(5_000)
        assertFalse(tearDown.await())
    }

    @Test
    fun `awaitSavedCardsDetectorTearDown returns false when controller reports tear down timeout`() =
        runTest {
            val awaiter = SavedCardsDetectorTearDownAwaiter()

            assertFalse(
                awaitSavedCardsDetectorTearDown(
                    hadActiveDetector = true,
                    controllerAwaitTearDown = { false },
                    awaiter = awaiter,
                ),
            )
        }

    @Test
    fun `SavedCardsDetectorTearDownAwaiter begin completes immediately when dispose signaled first`() =
        runTest {
            val awaiter = SavedCardsDetectorTearDownAwaiter()
            awaiter.signalDisposed()
            val deferred = awaiter.begin()
            assertTrue(deferred.isCompleted)
            deferred.await()
        }

    @Test
    fun `awaitSavedCardsDetectorTearDown does not hang when dispose signaled before begin`() = runTest {
        val awaiter = SavedCardsDetectorTearDownAwaiter()
        awaiter.signalDisposed()
        assertTrue(
            awaitSavedCardsDetectorTearDown(
                hadActiveDetector = true,
                controllerAwaitTearDown = null,
                awaiter = awaiter,
                timeoutMs = 5_000,
            ),
        )
    }

    @Test
    fun `shouldRemountSavedCardsDetector allows remount again after new checkout session generation`() {
        val priorGeneration = 4
        val newGeneration = 5
        assertFalse(
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.Recognized,
                remountedDetectorForCheckoutGeneration = priorGeneration,
                checkoutSessionGeneration = priorGeneration,
            ),
        )
        assertTrue(
            shouldRemountSavedCardsDetector(
                deviceRecognition = DeviceRecognitionState.Recognized,
                remountedDetectorForCheckoutGeneration = priorGeneration,
                checkoutSessionGeneration = newGeneration,
            ),
        )
    }
}
