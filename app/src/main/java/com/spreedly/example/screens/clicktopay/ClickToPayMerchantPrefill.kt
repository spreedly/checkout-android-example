package com.spreedly.example.screens.clicktopay

import com.spreedly.clicktopay.ClickToPayTokenizeBilling

data class ClickToPayMerchantPrefill(
    val firstName: String = "",
    val lastName: String = "",
    val phoneCountryCode: String = "",
    val phoneNumber: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "",
    val copyBillingToShipping: Boolean = false,
) {
    fun makeTokenizeBilling(email: String): ClickToPayTokenizeBilling {
        val trimmedEmail = email.trim()
        val trimmedPhone = phoneNumber.trim()
        val billing =
            ClickToPayTokenizeBilling(
                email = trimmedEmail.ifBlank { null },
                firstName = firstName.nilIfBlank(),
                lastName = lastName.nilIfBlank(),
                phoneNumber = trimmedPhone.ifBlank { null },
                addressLine1 = addressLine1.nilIfBlank(),
                addressLine2 = addressLine2.nilIfBlank(),
                city = city.nilIfBlank(),
                state = state.nilIfBlank(),
                zip = zip.nilIfBlank(),
                country = country.nilIfBlank(),
            )
        if (!copyBillingToShipping) {
            return billing
        }
        return billing.copy(
            shippingAddressLine1 = billing.addressLine1,
            shippingAddressLine2 = billing.addressLine2,
            shippingCity = billing.city,
            shippingState = billing.state,
            shippingZip = billing.zip,
            shippingCountry = billing.country,
            shippingPhoneNumber = billing.phoneNumber,
        )
    }
}

private fun String.nilIfBlank(): String? {
    val trimmed = trim()
    return trimmed.ifBlank { null }
}
