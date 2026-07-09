package com.spreedly.example.qa

import com.spreedly.hostedfields.models.HostedFieldEventType
import com.spreedly.sdk.models.CardScheme
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CardNumberFormat

internal fun hostedFieldDisplayName(fieldType: FormFieldType): String =
    when (fieldType) {
        is FormFieldType.CARD -> "Card number"
        is FormFieldType.CVV -> "Security code"
        is FormFieldType.EXPIRY_DATE -> "MM/YY"
        is FormFieldType.MONTH -> "MM"
        is FormFieldType.YEAR, is FormFieldType.YEAR_SECONDARY -> "YY"
        is FormFieldType.NAME -> "Full Name"
        else -> fieldType.toString()
    }

internal fun hostedFieldEventLabel(eventType: HostedFieldEventType): String =
    when (eventType) {
        HostedFieldEventType.INPUT -> "INPUT"
        HostedFieldEventType.FOCUS -> "FOCUS"
        HostedFieldEventType.BLUR -> "BLUR"
        HostedFieldEventType.VALIDATION -> "VALIDATION"
        HostedFieldEventType.PAN_MASK_CHANGED -> "PAN_MASK_CHANGED"
    }

internal fun cardNumberFormatLabel(format: CardNumberFormat): String =
    when (format) {
        CardNumberFormat.PRETTY -> "Pretty"
        CardNumberFormat.PLAIN -> "Plain"
        CardNumberFormat.MASKED -> "Masked"
    }

internal fun logYesNo(value: Boolean): String = if (value) "yes" else "no"

internal fun cardSchemeRawValue(scheme: CardScheme?): String? =
    scheme?.takeIf { it != CardScheme.UNKNOWN }?.name?.lowercase()

internal fun globalDisplayMismatchMessage(
    card: com.spreedly.hostedfields.models.HostedFieldState?,
    globalFormat: CardNumberFormat,
    globalPanMasked: Boolean,
): String? {
    if (card == null) return null
    card.panDisplayFormat?.let { snapFormat ->
        if (snapFormat != globalFormat) {
            return "Snapshot format (${cardNumberFormatLabel(snapFormat)}) ≠ global (${cardNumberFormatLabel(globalFormat)})"
        }
    }
    card.panDisplayPolicyMasked?.let { policy ->
        if (policy != globalPanMasked) {
            return "Snapshot policy masked (${logYesNo(policy)}) ≠ global panMasked (${logYesNo(globalPanMasked)})"
        }
    }
    return null
}
