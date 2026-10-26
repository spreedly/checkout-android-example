package com.spreedly.example.utils

import android.content.Context
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Demo session helper for screens that tokenize more than once per [Spreedly] instance.
 *
 * Handles mutex-protected auth refresh, observer registration once per session, and
 * single-use auth consumption rules.
 */
class DemoSdkAuthSession(
    sdkSessionManager: SdkSessionManager,
) {
    private val coordinator = DemoPaymentAuthCoordinator(sdkSessionManager)
    private var observersRegistered = false

    internal fun isAuthConsumedForTests(): Boolean = coordinator.isAuthConsumedForTests()

    internal fun shouldRefreshForTests(isInitialized: Boolean): Boolean = coordinator.shouldRefresh(isInitialized)

    fun resetObserverRegistration() {
        observersRegistered = false
    }

    suspend fun ensureFreshAuth(
        sdk: Spreedly,
        context: Context,
        environmentKey: String,
        isInitializing: MutableStateFlow<Boolean>,
        registerObserversIfNeeded: () -> Unit,
        onFailure: suspend () -> Unit = {},
    ): Boolean {
        isInitializing.value = true
        return try {
            val result =
                coordinator.refreshAuthIfNeeded(
                    sdk = sdk,
                    context = context.applicationContext,
                    environmentKey = environmentKey,
                    onInitialized = {
                        if (!observersRegistered) {
                            registerObserversIfNeeded()
                            observersRegistered = true
                        }
                    },
                )
            if (result.isFailure) {
                onFailure()
            }
            result.isSuccess && sdk.isInitialized
        } finally {
            isInitializing.value = false
        }
    }

    fun prepareForManualReinitialize() {
        observersRegistered = false
        coordinator.requireRefreshOnNextPayment()
    }

    fun onPaymentSubmitResult(result: PaymentProcessingResult) {
        if (shouldMarkAuthConsumedAfterSubmit(result)) {
            coordinator.markAuthConsumed()
        }
    }

    fun onTerminalPaymentResult(result: PaymentResult) {
        if (shouldMarkAuthConsumedAfterPaymentResult(result)) {
            coordinator.markAuthConsumed()
        }
    }
}
