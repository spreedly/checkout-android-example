package com.spreedly.example.utils

import android.content.Context
import com.spreedly.sdk.Spreedly
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Demo helper: fetch fresh signed auth via [SdkSessionManager] without racing concurrent inits.
 *
 * Used by sample screens that perform multiple tokenizations in one ViewModel lifetime.
 */
class DemoPaymentAuthCoordinator(
    private val sdkSessionManager: SdkSessionManager,
) {
    private val initMutex = Mutex()
    private var authConsumed = false

    fun markAuthConsumed() {
        authConsumed = true
    }

    fun requireRefreshOnNextPayment() {
        authConsumed = true
    }

    internal fun isAuthConsumedForTests(): Boolean = authConsumed

    fun shouldRefresh(isInitialized: Boolean): Boolean =
        shouldRefreshAuthBeforePayment(isInitialized, authConsumed)

    suspend fun refreshAuthIfNeeded(
        sdk: Spreedly,
        context: Context,
        environmentKey: String,
        onInitialized: () -> Unit = {},
    ): Result<Unit> =
        initMutex.withLock {
            if (!shouldRefresh(sdk.isInitialized)) {
                return@withLock if (sdk.isInitialized) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("SDK not initialized"))
                }
            }
            sdkSessionManager.initializeSdk(sdk, context.applicationContext, environmentKey)
                .onSuccess {
                    authConsumed = false
                    onInitialized()
                }
        }
}
