package com.spreedly.example.utils

import android.util.Log
import com.spreedly.example.repository.PaymentMethodRepository
import com.spreedly.sdk.ui.PaymentResult
import com.spreedly.sdk.Spreedly
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Encapsulates payment result observation and retention logic.
 *
 * Collects from [Spreedly.paymentResultFlow] and dispatches to caller-provided
 * callbacks so ViewModels only specify their own UI reactions (snackbar, token display).
 *
 * @param repository Used to retain payment methods when [PaymentResult.Completed.shouldRetain] is true.
 */
class PaymentResultHandler(
    private val repository: PaymentMethodRepository,
) {
    /**
     * Start collecting from [Spreedly.paymentResultFlow].
     *
     * @return [Job] that the caller can cancel when the scope is no longer active.
     */
    fun observeResults(
        sdk: Spreedly,
        scope: CoroutineScope,
        onCompleted: suspend (PaymentResult.Completed) -> Unit,
        onFailed: suspend (PaymentResult.Failed) -> Unit,
        onCanceled: suspend () -> Unit,
    ): Job = scope.launch {
        try {
            sdk.paymentResultFlow.collect { result ->
                when (result) {
                    is PaymentResult.Initial -> { /* no-op */ }
                    is PaymentResult.Canceled -> scope.launch { onCanceled() }
                    is PaymentResult.Failed -> scope.launch { onFailed(result) }
                    is PaymentResult.Completed -> scope.launch { onCompleted(result) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error collecting payment results", e)
        }
    }

    /**
     * Retain the payment method if [PaymentResult.Completed.shouldRetain] is true.
     *
     * Prefers the token from the full payment method response when available.
     * Falls back to [PaymentResult.Completed.token] for flows where the full
     * response is absent (e.g. offsite payments), so retention still works.
     *
     * **Behavior change:** Previously, offsite flows with a null
     * `paymentMethodResponse` skipped retention entirely. This now uses the
     * top-level token as a fallback so that offsite-flow cards can be retained.
     *
     * @return [Result.success] with `true` if retained, `false` if retention was
     *         not requested, or [Result.failure] on API error.
     */
    suspend fun retainIfNeeded(result: PaymentResult.Completed): kotlin.Result<Boolean> {
        if (!result.shouldRetain) return kotlin.Result.success(false)

        val token = result.paymentMethodResponse?.transaction?.paymentMethod?.token
            ?: result.token
        return repository.retainPaymentMethod(token)
    }

    private companion object {
        const val TAG = "PaymentResultHandler"
    }
}
