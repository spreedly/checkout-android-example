package com.spreedly.example.optionalfields

import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentSheetState

internal object DemoOptionalFieldValues {
    fun read(state: PaymentSheetState, type: OptionalFieldType): String =
        when (type) {
            OptionalFieldType.ADDRESS_LINE_1 -> state.addressLine1.value
            OptionalFieldType.ADDRESS_LINE_2 -> state.addressLine2.value
            OptionalFieldType.CITY -> state.city.value
            OptionalFieldType.STATE -> state.state.value
            OptionalFieldType.ZIP_CODE -> state.zipCode.value
            OptionalFieldType.EMAIL -> state.email.value
            OptionalFieldType.COMPANY -> state.company.value
            OptionalFieldType.COUNTRY -> state.country.value
            OptionalFieldType.PHONE_NUMBER -> state.phoneNumber.value
            OptionalFieldType.SHIPPING_ADDRESS_1 -> state.shippingAddressLine1.value
            OptionalFieldType.SHIPPING_ADDRESS_2 -> state.shippingAddressLine2.value
            OptionalFieldType.SHIPPING_CITY -> state.shippingCity.value
            OptionalFieldType.SHIPPING_STATE -> state.shippingState.value
            OptionalFieldType.SHIPPING_ZIP -> state.shippingZip.value
            OptionalFieldType.SHIPPING_COUNTRY -> state.shippingCountry.value
            OptionalFieldType.SHIPPING_PHONE_NUMBER -> state.shippingPhoneNumber.value
            OptionalFieldType.HOUSE_NUMBER_OR_NAME -> state.houseNumberOrName.value
            OptionalFieldType.STREET -> state.street.value
            OptionalFieldType.STREET_LINE_2 -> state.streetLine2.value
            OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME -> state.shippingHouseNumberOrName.value
            OptionalFieldType.SHIPPING_STREET -> state.shippingStreet.value
            OptionalFieldType.SHIPPING_STREET_LINE_2 -> state.shippingStreetLine2.value
            OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE -> state.phoneNumberCountryCode.value
            OptionalFieldType.PHONE_NUMBER_AREA_CODE -> state.phoneNumberAreaCode.value
            OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE -> state.shippingPhoneNumberCountryCode.value
            OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE -> state.shippingPhoneNumberAreaCode.value
        }
}

object DemoOptionalFieldActions {
    fun dispatchChange(
        sdk: Spreedly,
        type: OptionalFieldType,
        value: String,
        isRequired: Boolean,
    ) {
        val callbacks = sdk.callbacks
        val optionalCallbacks = sdk.optionalCallbacks
        when (type) {
            OptionalFieldType.ADDRESS_LINE_1 -> callbacks.onAddressLine1Change(value, isRequired)
            OptionalFieldType.ADDRESS_LINE_2 -> callbacks.onAddressLine2Change(value, isRequired)
            OptionalFieldType.CITY -> callbacks.onCityChange(value, isRequired)
            OptionalFieldType.STATE -> callbacks.onStateChange(value, isRequired)
            OptionalFieldType.ZIP_CODE -> callbacks.onZipCodeChange(value, isRequired)
            OptionalFieldType.EMAIL -> optionalCallbacks.onEmailChange(value, isRequired)
            OptionalFieldType.COMPANY -> optionalCallbacks.onCompanyChange(value, isRequired)
            OptionalFieldType.COUNTRY -> optionalCallbacks.onCountryChange(value, isRequired)
            OptionalFieldType.PHONE_NUMBER -> optionalCallbacks.onPhoneNumberChange(value, isRequired)
            OptionalFieldType.SHIPPING_ADDRESS_1 ->
                optionalCallbacks.onShippingAddressLine1Change(value, isRequired)
            OptionalFieldType.SHIPPING_ADDRESS_2 ->
                optionalCallbacks.onShippingAddressLine2Change(value, isRequired)
            OptionalFieldType.SHIPPING_CITY -> optionalCallbacks.onShippingCityChange(value, isRequired)
            OptionalFieldType.SHIPPING_STATE -> optionalCallbacks.onShippingStateChange(value, isRequired)
            OptionalFieldType.SHIPPING_ZIP -> optionalCallbacks.onShippingZipChange(value, isRequired)
            OptionalFieldType.SHIPPING_COUNTRY -> optionalCallbacks.onShippingCountryChange(value, isRequired)
            OptionalFieldType.SHIPPING_PHONE_NUMBER ->
                optionalCallbacks.onShippingPhoneNumberChange(value, isRequired)
            OptionalFieldType.HOUSE_NUMBER_OR_NAME ->
                optionalCallbacks.onHouseNumberOrNameChange(value, isRequired)
            OptionalFieldType.STREET -> optionalCallbacks.onStreetChange(value, isRequired)
            OptionalFieldType.STREET_LINE_2 -> optionalCallbacks.onStreetLine2Change(value, isRequired)
            OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME ->
                optionalCallbacks.onShippingHouseNumberOrNameChange(value, isRequired)
            OptionalFieldType.SHIPPING_STREET -> optionalCallbacks.onShippingStreetChange(value, isRequired)
            OptionalFieldType.SHIPPING_STREET_LINE_2 ->
                optionalCallbacks.onShippingStreetLine2Change(value, isRequired)
            OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE ->
                optionalCallbacks.onPhoneNumberCountryCodeChange(value, isRequired)
            OptionalFieldType.PHONE_NUMBER_AREA_CODE ->
                optionalCallbacks.onPhoneNumberAreaCodeChange(value, isRequired)
            OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE ->
                optionalCallbacks.onShippingPhoneNumberCountryCodeChange(value, isRequired)
            OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE ->
                optionalCallbacks.onShippingPhoneNumberAreaCodeChange(value, isRequired)
        }
    }

    fun clearDisabledFields(
        sdk: Spreedly,
        previous: DemoOptionalFieldSelection,
        current: DemoOptionalFieldSelection,
    ) {
        val removed = previous.enabledFields - current.enabledFields
        removed.forEach { type ->
            dispatchChange(sdk, type, value = "", isRequired = false)
        }
    }

    fun displayLabel(type: OptionalFieldType): String =
        when (type) {
            OptionalFieldType.ADDRESS_LINE_1 -> "Address Line 1"
            OptionalFieldType.ADDRESS_LINE_2 -> "Address Line 2"
            OptionalFieldType.CITY -> "City"
            OptionalFieldType.STATE -> "State"
            OptionalFieldType.ZIP_CODE -> "Zip Code"
            OptionalFieldType.EMAIL -> "Email"
            OptionalFieldType.COMPANY -> "Company"
            OptionalFieldType.COUNTRY -> "Country"
            OptionalFieldType.PHONE_NUMBER -> "Phone Number"
            OptionalFieldType.SHIPPING_ADDRESS_1 -> "Shipping Address Line 1"
            OptionalFieldType.SHIPPING_ADDRESS_2 -> "Shipping Address Line 2"
            OptionalFieldType.SHIPPING_CITY -> "Shipping City"
            OptionalFieldType.SHIPPING_STATE -> "Shipping State"
            OptionalFieldType.SHIPPING_ZIP -> "Shipping Zip"
            OptionalFieldType.SHIPPING_COUNTRY -> "Shipping Country"
            OptionalFieldType.SHIPPING_PHONE_NUMBER -> "Shipping Phone"
            OptionalFieldType.HOUSE_NUMBER_OR_NAME -> "House Number or Name"
            OptionalFieldType.STREET -> "Street"
            OptionalFieldType.STREET_LINE_2 -> "Street Line 2"
            OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME -> "Shipping House # or Name"
            OptionalFieldType.SHIPPING_STREET -> "Shipping Street"
            OptionalFieldType.SHIPPING_STREET_LINE_2 -> "Shipping Street Line 2"
            OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE -> "Phone Country Code"
            OptionalFieldType.PHONE_NUMBER_AREA_CODE -> "Phone Area Code"
            OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE -> "Shipping Phone Country Code"
            OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE -> "Shipping Phone Area Code"
        }
}
