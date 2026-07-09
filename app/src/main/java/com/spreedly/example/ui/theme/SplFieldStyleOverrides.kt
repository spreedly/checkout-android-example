package com.spreedly.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.spreedly.sdk.models.FormFieldType

enum class SplFieldTarget(val label: String) {
    NONE("None"),
    CARD_NUMBER("Card Number"),
    EXPIRY("Expiry"),
    CVV("CVV"),
    ;

    val formFieldType: FormFieldType?
        get() =
            when (this) {
                NONE -> null
                CARD_NUMBER -> FormFieldType.CARD(true)
                EXPIRY -> FormFieldType.EXPIRY_DATE(true)
                CVV -> FormFieldType.CVV(true)
            }
}

data class SplFieldStyleOverrides(
    val primaryColor: Color? = null,
    val fieldBackgroundColor: Color? = null,
    val textColor: Color? = null,
    val placeholderColor: Color? = null,
    val borderColor: Color? = null,
)

fun FormFieldType.matchesSplFieldTarget(target: FormFieldType): Boolean =
    when {
        this is FormFieldType.CARD && target is FormFieldType.CARD -> true
        this is FormFieldType.CVV && target is FormFieldType.CVV -> true
        this is FormFieldType.EXPIRY_DATE && target is FormFieldType.EXPIRY_DATE -> true
        else -> false
    }
