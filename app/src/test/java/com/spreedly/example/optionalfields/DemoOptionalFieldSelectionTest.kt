package com.spreedly.example.optionalfields

import com.spreedly.example.validation.DemoCoreNameField
import com.spreedly.example.validation.DemoCustomValidationPreset
import com.spreedly.example.validation.DemoExpiryManifestTest
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.PaymentSheetState
import com.spreedly.validation.validators.LimitedTextFormValidator
import com.spreedly.validation.validators.RequiredValidator
import com.spreedly.validation.validators.ZipCodeValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoOptionalFieldSelectionTest {
    @Test
    fun `group and field selection matrix`() {
        val defaultSelection = DemoOptionalFieldSelection.default()
        assertTrue(defaultSelection.isGroupFullyEnabled(DemoAddressGroup.BILLING))
        assertFalse(defaultSelection.isGroupFullyEnabled(DemoAddressGroup.SHIPPING))

        val withShipping =
            defaultSelection.withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = true)
        assertTrue(withShipping.isFieldEnabled(OptionalFieldType.SHIPPING_ZIP))
        assertTrue(withShipping.isFieldEnabled(OptionalFieldType.COUNTRY))

        val withoutShipping = withShipping.withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = false)
        assertFalse(withoutShipping.isFieldEnabled(OptionalFieldType.SHIPPING_ZIP))
        assertTrue(withoutShipping.isFieldEnabled(OptionalFieldType.ZIP_CODE))

        val clearedBilling =
            defaultSelection.withGroupEnabled(DemoAddressGroup.BILLING, enabled = false)
        assertTrue(clearedBilling.enabledFields.isEmpty())

        val requiredThenDisabled =
            defaultSelection
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true)
                .withFieldEnabled(OptionalFieldType.ZIP_CODE, enabled = false)
        assertFalse(requiredThenDisabled.isFieldRequired(OptionalFieldType.ZIP_CODE))
    }

    @Test
    fun `hosted mapping and additional fields matrix`() {
        val requiredState =
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.STATE, required = true)
        assertTrue(
            requiredState.toConfigurableFormFields().first { it.type == OptionalFieldType.STATE }.isRequired,
        )

        val hosted =
            DemoOptionalFieldSelection.default()
                .withFieldRequired(OptionalFieldType.ZIP_CODE, required = true)
                .toHostedFormFields(includeCoreCardFields = true)
        assertTrue(hosted.any { it is FormFieldType.CARD })
        assertTrue(hosted.filterIsInstance<FormFieldType.ZIP>().single().required)

        val defaultExpiry =
            DemoOptionalFieldSelection.default()
                .toHostedFormFields(demoInvalidExpiryManifest = false)
        assertTrue(defaultExpiry.any { it is FormFieldType.EXPIRY_DATE })
        assertFalse(defaultExpiry.any { it is FormFieldType.MONTH })
        assertFalse(DemoExpiryManifestTest.isInvalidDualManifest(defaultExpiry))

        val invalidDualManifest =
            DemoOptionalFieldSelection.default()
                .toHostedFormFields(demoInvalidExpiryManifest = true)
        assertTrue(invalidDualManifest.any { it is FormFieldType.EXPIRY_DATE })
        assertTrue(invalidDualManifest.any { it is FormFieldType.MONTH })
        assertTrue(invalidDualManifest.any { it is FormFieldType.YEAR })
        assertTrue(DemoExpiryManifestTest.isInvalidDualManifest(invalidDualManifest))

        val state =
            PaymentSheetState(
                country = LimitedTextFormValidator("US", maxLength = 100),
                zipCode = ZipCodeValidator("94107", false),
                shippingZip = ZipCodeValidator("10001", false),
            )
        val map =
            DemoOptionalFieldSelection.default()
                .withGroupEnabled(DemoAddressGroup.SHIPPING, enabled = true)
                .buildAdditionalFieldsMap(
                    paymentState = state,
                    merchantOwned =
                        mapOf(
                            AdditionalField.FULL_NAME to "Jane Doe",
                            AdditionalField.ADDRESS_LINE_1 to "123 Main",
                        ),
                )
        assertEquals("Jane Doe", map[AdditionalField.FULL_NAME])
        assertEquals("94107", map[AdditionalField.ZIP_CODE])
        assertEquals("10001", map[AdditionalField.SHIPPING_ZIP])

        val blankHosted =
            DemoOptionalFieldSelection.default()
                .buildAdditionalFieldsMap(
                    paymentState = PaymentSheetState(),
                    merchantOwned = mapOf(AdditionalField.FULL_NAME to ""),
                )
        assertFalse(blankHosted.containsKey(AdditionalField.FULL_NAME))
        assertFalse(blankHosted.containsKey(AdditionalField.ZIP_CODE))

        assertEquals(
            OptionalFieldType.SHIPPING_ZIP,
            DemoOptionalFieldSelection.optionalFieldFrom(FormFieldType.SHIPPING_ZIP(false)),
        )
        assertEquals(null, DemoOptionalFieldSelection.optionalFieldFrom(FormFieldType.CARD(true)))
    }

    @Test
    fun `custom validation preset storage matrix`() {
        val withCoreName =
            DemoOptionalFieldSelection.default()
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FULL_NAME,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                )
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FULL_NAME,
                    DemoCustomValidationPreset.NONE,
                )
        assertEquals(
            DemoCustomValidationPreset.NONE,
            withCoreName.coreNameCustomValidationPreset(DemoCoreNameField.FULL_NAME),
        )

        val withOptional =
            DemoOptionalFieldSelection.default()
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                )
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.NONE,
                )
        assertEquals(DemoCustomValidationPreset.NONE, withOptional.customValidationPreset(OptionalFieldType.CITY))
    }

    @Test
    fun `catalog allowlist and defaults`() {
        assertEquals(
            OptionalFieldType.entries.size,
            DemoOptionalFieldSelection.advancedCatalog().size,
        )
        DemoOptionalFieldSelection.default().let { selection ->
            DemoOptionalFieldSelection.advancedCatalog().forEach { type ->
                assertEquals(DemoCustomValidationPreset.NONE, selection.customValidationPreset(type))
            }
            DemoOptionalFieldSelection.coreNameValidationCatalog().forEach { field ->
                assertEquals(DemoCustomValidationPreset.NONE, selection.coreNameCustomValidationPreset(field))
            }
        }
        assertEquals(29, DemoOptionalFieldSelection.allCustomValidationWireKeys().size)
        assertFalse(
            DemoOptionalFieldSelection.defaultValidityForField(OptionalFieldType.ZIP_CODE, required = true),
        )
        assertTrue(
            DemoOptionalFieldSelection.defaultValidityForField(OptionalFieldType.ZIP_CODE, required = false),
        )
        assertEquals("city", DemoOptionalFieldSelection.wireKeyFor(OptionalFieldType.CITY))
    }
}
