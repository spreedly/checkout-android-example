package com.spreedly.example.screens.headlessbankaccount

import com.spreedly.sdk.ui.isAchNameFieldValid
import com.spreedly.sdk.ui.isAchSeparateNamePairValid

internal data class HeadlessAchNameValidity(
    val fullNameIsValid: Boolean,
    val firstNameIsValid: Boolean,
    val lastNameIsValid: Boolean,
    val bankNameIsValid: Boolean,
)

internal fun headlessAchNameValidity(
    accountHolderName: String,
    firstName: String,
    lastName: String,
    bankName: String,
    showBankName: Boolean,
    bankNameRequired: Boolean,
    allowBlankName: Boolean,
): HeadlessAchNameValidity {
    val pairValid = isAchSeparateNamePairValid(
        firstName = firstName,
        lastName = lastName,
        allowBlankName = allowBlankName,
    )
    val bankNameIsValid = when {
        !showBankName -> true
        !bankNameRequired -> true
        else -> isAchNameFieldValid(bankName, allowBlankName = false)
    }
    return HeadlessAchNameValidity(
        fullNameIsValid = isAchNameFieldValid(accountHolderName, allowBlankName),
        firstNameIsValid = pairValid,
        lastNameIsValid = pairValid,
        bankNameIsValid = bankNameIsValid,
    )
}
