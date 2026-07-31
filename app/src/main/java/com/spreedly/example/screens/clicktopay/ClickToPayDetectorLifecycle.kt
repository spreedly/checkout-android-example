package com.spreedly.example.screens.clicktopay

import com.spreedly.clicktopay.ClickToPaySavedCardsDetectorResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

internal fun deviceRecognitionForDetectorResult(
    result: ClickToPaySavedCardsDetectorResult,
): DeviceRecognitionState =
    when {
        result.hasSavedCards -> DeviceRecognitionState.Recognized
        result.failure != null -> DeviceRecognitionState.DetectionFailed
        else -> DeviceRecognitionState.NotRecognized
    }

internal const val CHECKOUT_INACTIVE_REMOUNT_TIMEOUT_MS = 10_000L
internal const val CHECKOUT_INACTIVE_REMOUNT_POLL_MS = 50L
internal const val SAVED_CARDS_DETECTOR_TEARDOWN_TIMEOUT_MS = 5_000L

internal fun shouldRemountSavedCardsDetector(
    deviceRecognition: DeviceRecognitionState,
    remountedDetectorForCheckoutGeneration: Int,
    checkoutSessionGeneration: Int,
): Boolean =
    deviceRecognition != DeviceRecognitionState.UsingDifferentEmail &&
        remountedDetectorForCheckoutGeneration != checkoutSessionGeneration

internal suspend fun awaitCheckoutInactiveForRemount(
    isCheckoutActive: () -> Boolean,
    delayMs: Long = CHECKOUT_INACTIVE_REMOUNT_POLL_MS,
    timeoutMs: Long = CHECKOUT_INACTIVE_REMOUNT_TIMEOUT_MS,
    delay: suspend (Long) -> Unit,
    nowMs: () -> Long = { System.currentTimeMillis() },
): Boolean {
    val deadline = nowMs() + timeoutMs
    while (isCheckoutActive()) {
        if (nowMs() >= deadline) {
            return false
        }
        delay(delayMs)
    }
    return true
}

internal suspend fun awaitCheckoutInactiveUntilRemount(
    isCheckoutActive: () -> Boolean,
    delayMs: Long = CHECKOUT_INACTIVE_REMOUNT_POLL_MS,
    delay: suspend (Long) -> Unit,
) {
    while (isCheckoutActive()) {
        delay(delayMs)
    }
}

internal class SavedCardsDetectorTearDownAwaiter {
    private var inFlight: CompletableDeferred<Unit>? = null
    private var disposeSignaledWithoutAwaiter = false

    fun begin(): CompletableDeferred<Unit> {
        if (disposeSignaledWithoutAwaiter) {
            disposeSignaledWithoutAwaiter = false
            return CompletableDeferred<Unit>().also { it.complete(Unit) }
        }
        val deferred = CompletableDeferred<Unit>()
        inFlight = deferred
        return deferred
    }

    fun signalDisposed() {
        val current = inFlight
        if (current != null) {
            current.complete(Unit)
            inFlight = null
        } else {
            disposeSignaledWithoutAwaiter = true
        }
    }

    fun clear() {
        inFlight = null
        disposeSignaledWithoutAwaiter = false
    }
}

internal suspend fun awaitSavedCardsDetectorTearDown(
    hadActiveDetector: Boolean,
    controllerAwaitTearDown: (suspend () -> Boolean)?,
    awaiter: SavedCardsDetectorTearDownAwaiter,
    timeoutMs: Long = SAVED_CARDS_DETECTOR_TEARDOWN_TIMEOUT_MS,
): Boolean {
    if (!hadActiveDetector) {
        return true
    }
    if (controllerAwaitTearDown != null) {
        return controllerAwaitTearDown()
    }
    val fallback = awaiter.begin()
    return try {
        withTimeoutOrNull(timeoutMs) {
            fallback.await()
        } != null
    } finally {
        awaiter.clear()
    }
}
