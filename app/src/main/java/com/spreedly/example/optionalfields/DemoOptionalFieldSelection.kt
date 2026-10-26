package com.spreedly.example.optionalfields

import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.ConfigurableFormField
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentSheetState
import com.spreedly.sdk.ui.toFormFieldType
import com.spreedly.example.validation.DemoCoreNameField
import com.spreedly.example.validation.DemoCustomValidationPreset
import com.spreedly.example.validation.DemoExpiryManifestTest

/**
 * Demo-only model for merchant-chosen optional catalogue fields (billing, shipping, contact).
 *
 * Group toggles enable sensible presets; advanced mode exposes every [OptionalFieldType].
 */
enum class DemoAddressGroup {
    BILLING,
    SHIPPING,
    CONTACT,
}

data class DemoOptionalFieldSelection(
    val enabledFields: Set<OptionalFieldType> = defaultEnabledFields(),
    val requiredFields: Set<OptionalFieldType> = emptySet(),
    val fieldCustomValidationPresets: Map<OptionalFieldType, DemoCustomValidationPreset> = defaultPresets(),
    val coreNameCustomValidationPresets: Map<DemoCoreNameField, DemoCustomValidationPreset> = defaultCoreNamePresets(),
) {
    fun isGroupFullyEnabled(group: DemoAddressGroup): Boolean =
        fieldsForGroup(group).all { it in enabledFields }

    fun withGroupEnabled(group: DemoAddressGroup, enabled: Boolean): DemoOptionalFieldSelection {
        val groupFields = fieldsForGroup(group)
        val newEnabled =
            if (enabled) {
                enabledFields + groupFields
            } else {
                enabledFields - groupFields
            }
        return copy(
            enabledFields = newEnabled,
            requiredFields = requiredFields.intersect(newEnabled),
        )
    }

    fun withFieldEnabled(type: OptionalFieldType, enabled: Boolean): DemoOptionalFieldSelection {
        val newEnabled =
            if (enabled) {
                enabledFields + type
            } else {
                enabledFields - type
            }
        return copy(
            enabledFields = newEnabled,
            requiredFields = requiredFields.intersect(newEnabled),
        )
    }

    fun withFieldRequired(type: OptionalFieldType, required: Boolean): DemoOptionalFieldSelection {
        if (type !in enabledFields) return this
        val newRequired =
            if (required) {
                requiredFields + type
            } else {
                requiredFields - type
            }
        return copy(requiredFields = newRequired)
    }

    fun withFieldCustomValidation(
        type: OptionalFieldType,
        preset: DemoCustomValidationPreset,
    ): DemoOptionalFieldSelection {
        val newPresets = fieldCustomValidationPresets.toMutableMap()
        if (preset == DemoCustomValidationPreset.NONE) {
            newPresets.remove(type)
        } else {
            newPresets[type] = preset
        }
        return copy(fieldCustomValidationPresets = newPresets)
    }

    fun customValidationPreset(type: OptionalFieldType): DemoCustomValidationPreset =
        fieldCustomValidationPresets[type] ?: DemoCustomValidationPreset.NONE

    fun withCoreNameCustomValidation(
        field: DemoCoreNameField,
        preset: DemoCustomValidationPreset,
    ): DemoOptionalFieldSelection {
        val newPresets = coreNameCustomValidationPresets.toMutableMap()
        if (preset == DemoCustomValidationPreset.NONE) {
            newPresets.remove(field)
        } else {
            newPresets[field] = preset
        }
        return copy(coreNameCustomValidationPresets = newPresets)
    }

    fun coreNameCustomValidationPreset(field: DemoCoreNameField): DemoCustomValidationPreset =
        coreNameCustomValidationPresets[field] ?: DemoCustomValidationPreset.NONE

    fun isFieldEnabled(type: OptionalFieldType): Boolean = type in enabledFields

    fun isFieldRequired(type: OptionalFieldType): Boolean = type in requiredFields

    fun toConfigurableFormFields(): List<ConfigurableFormField> =
        enabledFields
            .sortedBy { catalogIndex(it) }
            .map { type ->
                ConfigurableFormField(
                    type = type,
                    isRequired = type in requiredFields,
                )
            }

    fun toHostedFormFields(
        includeCoreCardFields: Boolean = true,
        demoInvalidExpiryManifest: Boolean = false,
    ): List<FormFieldType> {
        val core =
            if (includeCoreCardFields) {
                listOf(FormFieldType.CARD(true)) +
                    DemoExpiryManifestTest.coreExpiryFields(demoInvalidExpiryManifest, required = true) +
                    listOf(FormFieldType.CVV(true))
            } else {
                emptyList()
            }
        val optional =
            enabledFields
                .sortedBy { catalogIndex(it) }
                .map { it.toFormFieldType(it in requiredFields) }
        return core + optional
    }

    fun buildAdditionalFieldsMap(
        paymentState: PaymentSheetState,
        merchantOwned: Map<AdditionalField, String> = emptyMap(),
    ): Map<AdditionalField, String> {
        val result = linkedMapOf<AdditionalField, String>()
        merchantOwned.forEach { (field, value) ->
            if (value.isNotBlank()) {
                result[field] = value
            }
        }
        enabledFields.forEach { type ->
            val additionalField = optionalFieldToAdditionalField(type) ?: return@forEach
            val value = DemoOptionalFieldValues.read(paymentState, type)
            if (value.isNotBlank()) {
                result[additionalField] = value
            }
        }
        return result
    }

    companion object {
        fun default(): DemoOptionalFieldSelection = DemoOptionalFieldSelection()

        fun defaultEnabledFields(): Set<OptionalFieldType> = fieldsForGroup(DemoAddressGroup.BILLING)

        fun defaultPresets(): Map<OptionalFieldType, DemoCustomValidationPreset> = emptyMap()

        fun defaultCoreNamePresets(): Map<DemoCoreNameField, DemoCustomValidationPreset> = emptyMap()

        fun coreNameValidationCatalog(): List<DemoCoreNameField> = DemoCoreNameField.entries

        fun allCustomValidationWireKeys(): Set<String> =
            advancedCatalog()
                .mapNotNull { wireKeyFor(it) }
                .toSet() + DemoCoreNameField.wireKeys

        fun wireKeyFor(type: OptionalFieldType): String? = optionalFieldToAdditionalField(type)?.fieldName

        fun fieldsForGroup(group: DemoAddressGroup): Set<OptionalFieldType> =
            when (group) {
                DemoAddressGroup.BILLING ->
                    setOf(
                        OptionalFieldType.COUNTRY,
                        OptionalFieldType.STATE,
                        OptionalFieldType.ZIP_CODE,
                    )
                DemoAddressGroup.SHIPPING ->
                    setOf(
                        OptionalFieldType.SHIPPING_COUNTRY,
                        OptionalFieldType.SHIPPING_STATE,
                        OptionalFieldType.SHIPPING_ZIP,
                    )
                DemoAddressGroup.CONTACT -> setOf(OptionalFieldType.EMAIL)
            }

        fun advancedCatalog(): List<OptionalFieldType> =
            listOf(
                OptionalFieldType.ADDRESS_LINE_1,
                OptionalFieldType.ADDRESS_LINE_2,
                OptionalFieldType.CITY,
                OptionalFieldType.STATE,
                OptionalFieldType.ZIP_CODE,
                OptionalFieldType.COUNTRY,
                OptionalFieldType.EMAIL,
                OptionalFieldType.COMPANY,
                OptionalFieldType.PHONE_NUMBER,
                OptionalFieldType.HOUSE_NUMBER_OR_NAME,
                OptionalFieldType.STREET,
                OptionalFieldType.STREET_LINE_2,
                OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE,
                OptionalFieldType.PHONE_NUMBER_AREA_CODE,
                OptionalFieldType.SHIPPING_ADDRESS_1,
                OptionalFieldType.SHIPPING_ADDRESS_2,
                OptionalFieldType.SHIPPING_CITY,
                OptionalFieldType.SHIPPING_STATE,
                OptionalFieldType.SHIPPING_ZIP,
                OptionalFieldType.SHIPPING_COUNTRY,
                OptionalFieldType.SHIPPING_PHONE_NUMBER,
                OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME,
                OptionalFieldType.SHIPPING_STREET,
                OptionalFieldType.SHIPPING_STREET_LINE_2,
                OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE,
                OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE,
            )

        fun optionalFieldFrom(formFieldType: FormFieldType): OptionalFieldType? =
            when (formFieldType) {
                is FormFieldType.ADDRESS_LINE_1 -> OptionalFieldType.ADDRESS_LINE_1
                is FormFieldType.ADDRESS_LINE_2 -> OptionalFieldType.ADDRESS_LINE_2
                is FormFieldType.CITY -> OptionalFieldType.CITY
                is FormFieldType.STATE -> OptionalFieldType.STATE
                is FormFieldType.ZIP -> OptionalFieldType.ZIP_CODE
                is FormFieldType.COUNTRY -> OptionalFieldType.COUNTRY
                is FormFieldType.EMAIL -> OptionalFieldType.EMAIL
                is FormFieldType.COMPANY -> OptionalFieldType.COMPANY
                is FormFieldType.PHONE_NUMBER -> OptionalFieldType.PHONE_NUMBER
                is FormFieldType.SHIPPING_ADDRESS_LINE_1 -> OptionalFieldType.SHIPPING_ADDRESS_1
                is FormFieldType.SHIPPING_ADDRESS_LINE_2 -> OptionalFieldType.SHIPPING_ADDRESS_2
                is FormFieldType.SHIPPING_CITY -> OptionalFieldType.SHIPPING_CITY
                is FormFieldType.SHIPPING_STATE -> OptionalFieldType.SHIPPING_STATE
                is FormFieldType.SHIPPING_ZIP -> OptionalFieldType.SHIPPING_ZIP
                is FormFieldType.SHIPPING_COUNTRY -> OptionalFieldType.SHIPPING_COUNTRY
                is FormFieldType.SHIPPING_PHONE_NUMBER -> OptionalFieldType.SHIPPING_PHONE_NUMBER
                is FormFieldType.HOUSE_NUMBER_OR_NAME -> OptionalFieldType.HOUSE_NUMBER_OR_NAME
                is FormFieldType.STREET -> OptionalFieldType.STREET
                is FormFieldType.STREET_LINE_2 -> OptionalFieldType.STREET_LINE_2
                is FormFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME -> OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME
                is FormFieldType.SHIPPING_STREET -> OptionalFieldType.SHIPPING_STREET
                is FormFieldType.SHIPPING_STREET_LINE_2 -> OptionalFieldType.SHIPPING_STREET_LINE_2
                is FormFieldType.PHONE_NUMBER_COUNTRY_CODE -> OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE
                is FormFieldType.PHONE_NUMBER_AREA_CODE -> OptionalFieldType.PHONE_NUMBER_AREA_CODE
                is FormFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE ->
                    OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE
                is FormFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE ->
                    OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE
                else -> null
            }

        fun optionalFieldToAdditionalField(type: OptionalFieldType): AdditionalField? =
            when (type) {
                OptionalFieldType.ADDRESS_LINE_1 -> AdditionalField.ADDRESS_LINE_1
                OptionalFieldType.ADDRESS_LINE_2 -> AdditionalField.ADDRESS_LINE_2
                OptionalFieldType.CITY -> AdditionalField.CITY
                OptionalFieldType.STATE -> AdditionalField.STATE
                OptionalFieldType.ZIP_CODE -> AdditionalField.ZIP_CODE
                OptionalFieldType.COUNTRY -> AdditionalField.COUNTRY
                OptionalFieldType.EMAIL -> AdditionalField.EMAIL
                OptionalFieldType.COMPANY -> AdditionalField.COMPANY
                OptionalFieldType.PHONE_NUMBER -> AdditionalField.PHONE_NUMBER
                OptionalFieldType.SHIPPING_ADDRESS_1 -> AdditionalField.SHIPPING_ADDRESS_1
                OptionalFieldType.SHIPPING_ADDRESS_2 -> AdditionalField.SHIPPING_ADDRESS_2
                OptionalFieldType.SHIPPING_CITY -> AdditionalField.SHIPPING_CITY
                OptionalFieldType.SHIPPING_STATE -> AdditionalField.SHIPPING_STATE
                OptionalFieldType.SHIPPING_ZIP -> AdditionalField.SHIPPING_ZIP
                OptionalFieldType.SHIPPING_COUNTRY -> AdditionalField.SHIPPING_COUNTRY
                OptionalFieldType.SHIPPING_PHONE_NUMBER -> AdditionalField.SHIPPING_PHONE_NUMBER
                OptionalFieldType.HOUSE_NUMBER_OR_NAME -> AdditionalField.HOUSE_NUMBER_OR_NAME
                OptionalFieldType.STREET -> AdditionalField.STREET
                OptionalFieldType.STREET_LINE_2 -> AdditionalField.STREET_LINE_2
                OptionalFieldType.SHIPPING_HOUSE_NUMBER_OR_NAME -> AdditionalField.SHIPPING_HOUSE_NUMBER_OR_NAME
                OptionalFieldType.SHIPPING_STREET -> AdditionalField.SHIPPING_STREET
                OptionalFieldType.SHIPPING_STREET_LINE_2 -> AdditionalField.SHIPPING_STREET_LINE_2
                OptionalFieldType.PHONE_NUMBER_COUNTRY_CODE -> AdditionalField.PHONE_NUMBER_COUNTRY_CODE
                OptionalFieldType.PHONE_NUMBER_AREA_CODE -> AdditionalField.PHONE_NUMBER_AREA_CODE
                OptionalFieldType.SHIPPING_PHONE_NUMBER_COUNTRY_CODE ->
                    AdditionalField.SHIPPING_PHONE_NUMBER_COUNTRY_CODE
                OptionalFieldType.SHIPPING_PHONE_NUMBER_AREA_CODE ->
                    AdditionalField.SHIPPING_PHONE_NUMBER_AREA_CODE
            }

        fun defaultValidityForField(type: OptionalFieldType, required: Boolean): Boolean = !required

        private fun catalogIndex(type: OptionalFieldType): Int = advancedCatalog().indexOf(type)
    }
}
