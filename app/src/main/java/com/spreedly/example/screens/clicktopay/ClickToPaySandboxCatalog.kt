package com.spreedly.example.screens.clicktopay

import com.spreedly.clicktopay.ClickToPayCheckoutConfig
import com.spreedly.clicktopay.ClickToPayCustomer
import com.spreedly.clicktopay.ClickToPayInitConfig
import com.spreedly.clicktopay.ClickToPayMaskedCard

object ClickToPaySandboxCatalog {
    const val SANDBOX_SRC_DPA_ID = "83f255e3-7f82-4441-8782-b17737fa6e29"
    const val LOCALE = "en_US"
    const val DPA_PRESENTATION_NAME = "Spreedly C2P Sandbox"
    const val DPA_NAME = "SpreedlyC2PSandbox"

    const val MERCHANT_PREFILL_HINT =
        "Enter email or mobile with country code for lookup. " +
            "Billing name is required on Pay; other address fields are optional and sent on tokenize when provided."

    fun labelForMaskedCard(card: ClickToPayMaskedCard): String {
        val brand = card.brand.replace("-", " ").replaceFirstChar { it.uppercase() }
        return "$brand •••• ${card.lastFour}"
    }

    fun buildCheckoutConfig(
        email: String,
        doLookup: Boolean,
        transactionAmount: Double = 100.0,
        merchantPrefill: ClickToPayMerchantPrefill = ClickToPayMerchantPrefill(),
        currencyCode: String = "USD",
    ): ClickToPayCheckoutConfig {
        val trimmedEmail = email.trim()
        val trimmedPhone = merchantPrefill.phoneNumber.trim()
        val countryCode = merchantPrefill.phoneCountryCode.trim()
        return ClickToPayCheckoutConfig(
            initConfig =
                ClickToPayInitConfig(
                    transactionAmount = transactionAmount,
                    transactionCurrencyCode = currencyCode,
                ),
            srcDpaId = SANDBOX_SRC_DPA_ID,
            locale = LOCALE,
            isSandbox = true,
            customer =
                ClickToPayCustomer(
                    email = trimmedEmail.ifBlank { null },
                    phoneNumber = trimmedPhone.ifBlank { null },
                    countryCode = countryCode.ifBlank { null },
                ),
            doLookup = doLookup,
            tokenizeBilling = merchantPrefill.makeTokenizeBilling(trimmedEmail),
            dpaPresentationName = DPA_PRESENTATION_NAME,
            dpaName = DPA_NAME,
        )
    }

    fun buildSavedCardsDetectorConfig(transactionAmount: Double = 100.0): ClickToPayCheckoutConfig =
        ClickToPayCheckoutConfig(
            initConfig =
                ClickToPayInitConfig(
                    transactionAmount = transactionAmount,
                    transactionCurrencyCode = "USD",
                ),
            srcDpaId = SANDBOX_SRC_DPA_ID,
            locale = LOCALE,
            isSandbox = true,
            customer = null,
            doLookup = true,
            merchantHostedCardList = true,
            dpaPresentationName = DPA_PRESENTATION_NAME,
            dpaName = DPA_NAME,
        )
}
