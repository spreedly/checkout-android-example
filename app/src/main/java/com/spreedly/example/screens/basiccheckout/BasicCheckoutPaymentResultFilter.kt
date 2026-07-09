package com.spreedly.example.screens.basiccheckout

import com.spreedly.sdk.ui.PaymentResult

internal const val CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE =
    "All required fields are not valid"

internal fun PaymentResult.Failed.isClientSideFormValidationFailure(): Boolean =
    message == CLIENT_SIDE_FORM_VALIDATION_FAILURE_MESSAGE
