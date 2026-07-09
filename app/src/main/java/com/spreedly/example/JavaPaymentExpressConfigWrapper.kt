package com.spreedly.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.spreedly.example.qa.ExpressDisplayConfigBar
import com.spreedly.example.qa.JavaPaymentSampleConfig
import com.spreedly.paymentsheet.PaymentSheetJavaHelper
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.PaymentSheetConfig

object JavaPaymentExpressConfigWrapper {
    @JvmStatic
    fun setupExpressConfigBar(composeView: ComposeView, onConfigChanged: Runnable) {
        composeView.setContent {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                ExpressDisplayConfigBar(
                    enableAutofill = JavaPaymentSampleConfig.enableAutofill,
                    onEnableAutofillChange = {
                        JavaPaymentSampleConfig.enableAutofill = it
                        onConfigChanged.run()
                    },
                    useMaskedFormat = JavaPaymentSampleConfig.useMaskedFormat,
                    onUseMaskedFormatChange = {
                        JavaPaymentSampleConfig.useMaskedFormat = it
                        onConfigChanged.run()
                    },
                )
            }
        }
    }

    @JvmStatic
    fun setupBottomSheet(composeView: ComposeView, sdk: Spreedly) {
        PaymentSheetJavaHelper.setupContent(
            composeView,
            sdk,
            PaymentSheetConfig.Default,
            JavaPaymentSampleConfig.displayConfig(),
        )
    }
}
