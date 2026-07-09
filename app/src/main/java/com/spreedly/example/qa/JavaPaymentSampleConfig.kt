package com.spreedly.example.qa

import com.spreedly.sdk.ui.CardNumberFormat
import com.spreedly.sdk.ui.PaymentSheetDisplayConfig

object JavaPaymentSampleConfig {
    @JvmField
    var enableAutofill: Boolean = true

    @JvmField
    var useMaskedFormat: Boolean = false

    @JvmStatic
    fun displayConfig(): PaymentSheetDisplayConfig =
        PaymentSheetDisplayConfig(
            enableAutofill = enableAutofill,
            cardNumberFormat =
                if (useMaskedFormat) {
                    CardNumberFormat.MASKED
                } else {
                    CardNumberFormat.PRETTY
                },
        )
}
