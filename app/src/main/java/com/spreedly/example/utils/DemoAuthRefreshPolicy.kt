package com.spreedly.example.utils

import com.spreedly.sdk.ui.PaymentProcessingResult
import com.spreedly.sdk.ui.PaymentResult

/**
 * When the demo should fetch fresh signed auth before starting another tokenization attempt.
 *
 * Spreedly auth params are single-use per tokenization attempt. Skip refresh while auth is still
 * unused so screen-load init is not duplicated on the first Pay click.
 */
internal fun shouldRefreshAuthBeforePayment(
    isInitialized: Boolean,
    authConsumed: Boolean,
): Boolean = !isInitialized || authConsumed

/** Auth is consumed once tokenization reaches the API ([PaymentProcessingResult.Processing]). */
internal fun shouldMarkAuthConsumedAfterSubmit(result: PaymentProcessingResult): Boolean =
    result is PaymentProcessingResult.Processing

/** Auth is consumed after terminal payment results from [com.spreedly.sdk.Spreedly.paymentResultFlow]. */
internal fun shouldMarkAuthConsumedAfterPaymentResult(result: PaymentResult): Boolean =
    when (result) {
        is PaymentResult.Completed -> true
        is PaymentResult.Failed ->
            result.errorType == PaymentResult.Failed.ErrorType.API_ERROR ||
                result.errorType == PaymentResult.Failed.ErrorType.NETWORK_ERROR
        else -> false
    }
