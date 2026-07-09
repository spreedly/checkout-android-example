package com.spreedly.example.screens.basiccheckout

import com.spreedly.sdk.models.FormFieldType

data class SubmitCheckoutParams(
    val formFields: List<FormFieldType>,
    val fullName: String,
    val email: String,
    val shouldRetainPaymentMethod: Boolean,
    val eligibleForCardUpdater: Boolean?,
)
