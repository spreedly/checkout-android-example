package com.spreedly.example

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.spreedly.example.utils.SdkSessionManager
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SpreedlyHelper {
    private val sdkSessionManager = SdkSessionManager(AuthService())

    @JvmStatic
    fun initializeSdkWithRemoteAuth(
        lifecycleOwner: LifecycleOwner,
        sdk: Spreedly,
        context: Context,
        environmentKey: String,
        onSuccess: Runnable,
        onError: java.util.function.Consumer<Exception>,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            sdkSessionManager.initializeSdk(sdk, context, environmentKey).fold(
                onSuccess = { onSuccess.run() },
                onFailure = { onError.accept(Exception(it.message, it)) },
            )
        }
    }

    @JvmStatic
    fun startPaymentResultMonitoring(
        lifecycleOwner: LifecycleOwner,
        sdk: Spreedly,
        onCompleted: java.util.function.Consumer<String>,
        onFailed: java.util.function.Consumer<String>,
        onCanceled: Runnable,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            while (!sdk.isInitialized) {
                delay(100)
            }

            try {
                sdk.paymentResultFlow.collect { result ->
                    when (result) {
                        is PaymentResult.Initial -> { /* no-op */ }
                        is PaymentResult.Canceled -> {
                            sdk.hideBottomSheet()
                            onCanceled.run()
                        }
                        is PaymentResult.Failed -> {
                            sdk.hideBottomSheet()
                            onFailed.accept(result.message ?: "Unknown error")
                        }
                        is PaymentResult.Completed -> {
                            sdk.hideBottomSheet()
                            onCompleted.accept(result.token)
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                onFailed.accept("SDK initialization error")
            }
        }
    }
}
