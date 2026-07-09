package com.spreedly.example.utils

import com.spreedly.sdk.utils.CardNumberContext
import com.spreedly.sdk.utils.isCvvOptionalForCardScheme

internal fun isCvvFormRequirementMet(cvvFieldValid: Boolean): Boolean =
    cvvFieldValid || isCvvOptionalForCardScheme(CardNumberContext.getCardScheme())
