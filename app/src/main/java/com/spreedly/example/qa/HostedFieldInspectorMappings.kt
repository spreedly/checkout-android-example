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
        is FormFieldType.NAME -> "Full name"
        is FormFieldType.FIRST_NAME -> "First name"
        is FormFieldType.LAST_NAME -> "Last name"
        is FormFieldType.EMAIL -> "Email"
        is FormFieldType.COMPANY -> "Company"
        is FormFieldType.COUNTRY -> "Country"
        is FormFieldType.PHONE_NUMBER -> "Phone number"
        is FormFieldType.ADDRESS_LINE_1 -> "Address line 1"
        is FormFieldType.ADDRESS_LINE_2 -> "Address line 2"
        is FormFieldType.CITY -> "City"
        is FormFieldType.STATE -> "State"
        is FormFieldType.ZIP -> "Postal code"
        is FormFieldType.SHIPPING_ADDRESS_LINE_1 -> "Shipping address line 1"
        is FormFieldType.SHIPPING_ADDRESS_LINE_2 -> "Shipping address line 2"
        is FormFieldType.SHIPPING_CITY -> "Shipping city"
        is FormFieldType.SHIPPING_STATE -> "Shipping state"
        is FormFieldType.SHIPPING_ZIP -> "Shipping postal code"
        is FormFieldType.SHIPPING_COUNTRY -> "Shipping country"
        is FormFieldType.SHIPPING_PHONE_NUMBER -> "Shipping phone number"
        is FormFieldType.HOUSE_NUMBER_OR_NAME -> "House number or name"
        is FormFieldType.STREET -> "Street"
        is FormFieldType.STREET_LINE_2 -> "Street line 2"
        is FormFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME -> "Shipping house number or name"
        is FormFieldType.SHIPPING_STREET -> "Shipping street"
        is FormFieldType.SHIPPING_STREET_LINE_2 -> "Shipping street line 2"
        is FormFieldType.PHONE_NUMBER_COUNTRY_CODE -> "Phone country code"
        is FormFieldType.PHONE_NUMBER_AREA_CODE -> "Phone area code"
        is FormFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE -> "Shipping phone country code"
        is FormFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE -> "Shipping phone area code"
        is FormFieldType.ROUTING_NUMBER -> "Routing number"
        is FormFieldType.ACCOUNT_NUMBER -> "Account number"
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
