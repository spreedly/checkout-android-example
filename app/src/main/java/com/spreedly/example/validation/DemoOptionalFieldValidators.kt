package com.spreedly.example.validation

import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.addValidation
import com.spreedly.sdk.removeValidation
import com.spreedly.sdk.ui.OptionalFieldType
import java.util.IdentityHashMap

/**
 * Registers demo [sdk.addValidation] rules from [DemoOptionalFieldSelection] preset choices.
 *
 * Tracks registrations per [Spreedly] instance so Custom Text Fields and Bottom Sheet demos
 * (each with their own [Spreedly]) do not cross-remove each other's validators.
 */
internal object DemoOptionalFieldValidators {
    private val registeredFieldsBySdk = IdentityHashMap<Spreedly, LinkedHashSet<AdditionalField>>()

    fun sync(
        sdk: Spreedly,
        selection: DemoOptionalFieldSelection,
        availableCoreNameFields: DemoCoreNameFieldLayout = DemoCoreNameFieldLayout(emptyList()),
    ) {
        val registeredFields = registeredFieldsBySdk.getOrPut(sdk) { linkedSetOf() }
        registeredFields.forEach { field ->
            sdk.removeValidation(field)
        }
        registeredFields.clear()

        selection.enabledFields.forEach { type ->
            val preset = selection.customValidationPreset(type)
            if (!DemoPresetFieldValidators.shouldRegisterPreset(preset, type)) {
                return@forEach
            }
            val additionalField = DemoOptionalFieldSelection.optionalFieldToAdditionalField(type) ?: return@forEach
            sdk.addValidation(additionalField) { value, fields ->
                DemoPresetFieldValidators.validate(
                    preset = preset,
                    fieldType = type,
                    value = value,
                    fields = fields,
                )
            }
            registeredFields.add(additionalField)
        }

        availableCoreNameFields.fields.forEach { coreField ->
            val preset = selection.coreNameCustomValidationPreset(coreField)
            if (!DemoPresetFieldValidators.shouldRegisterPreset(preset, coreField)) {
                return@forEach
            }
            val additionalField = coreField.toAdditionalField()
            sdk.addValidation(additionalField) { value, fields ->
                DemoPresetFieldValidators.validate(
                    preset = preset,
                    field = coreField,
                    value = value,
                    fields = fields,
                )
            }
            registeredFields.add(additionalField)
        }
    }

    /**
     * Removes all demo validators registered on [sdk] and drops tracking for that instance.
     *
     * Safe to call when [sdk] was never synced (no-op) or after a previous [clear] (idempotent).
     * Demo ViewModels invoke this from [androidx.lifecycle.ViewModel.onCleared].
     */
    fun clear(sdk: Spreedly) {
        registeredFieldsBySdk.remove(sdk)?.forEach { field ->
            sdk.removeValidation(field)
        }
    }
}
