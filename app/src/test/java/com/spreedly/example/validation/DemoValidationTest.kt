package com.spreedly.example.validation

import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.ui.NameFieldDisplayMode
import com.spreedly.sdk.ui.OptionalFieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Preset rules, hosted cross-field validators, and core name layout helpers. */
class DemoValidationTest {
    @Test
    fun `core name field layouts match screen entry points`() {
        assertEquals(
            listOf(DemoCoreNameField.FULL_NAME),
            DemoCoreNameField.forNameDisplayMode(NameFieldDisplayMode.SINGLE_FIELD),
        )
        assertEquals(
            listOf(DemoCoreNameField.FIRST_NAME, DemoCoreNameField.LAST_NAME),
            DemoCoreNameField.forNameDisplayMode(NameFieldDisplayMode.SEPARATE_FIELDS),
        )
        assertEquals(
            DemoCoreNameFieldLayout(listOf(DemoCoreNameField.FULL_NAME)),
            DemoCoreNameField.forCustomTextFieldsScreen(),
        )
        assertEquals(
            DemoCoreNameFieldLayout(listOf(DemoCoreNameField.FIRST_NAME, DemoCoreNameField.LAST_NAME)),
            DemoCoreNameField.forBottomSheetScreen(),
        )
    }

    @Test
    fun `preset validation matrix`() {
        data class Case(
            val preset: DemoCustomValidationPreset,
            val fieldType: OptionalFieldType,
            val value: String,
            val fields: Map<AdditionalField, String>,
            val expectedValid: Boolean,
        )

        val cases =
            listOf(
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "NY", emptyMap(), false),
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "NYC", emptyMap(), true),
                // HC-1901: whitespace-only input is not "untouched" — it must still fail the
                // min-length check rather than being waved through as vacuously valid.
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "   ", emptyMap(), false),
                // Single space, and non-space whitespace (tab/newline) — same "no real content" case.
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, " ", emptyMap(), false),
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "\t\n", emptyMap(), false),
                // A genuinely empty (untouched) value is still skipped — required-ness is a
                // separate, built-in SDK concern, not this preset's job.
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "", emptyMap(), true),
                // Leading/trailing whitespace around real content is trimmed before measuring length.
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "  NY", emptyMap(), false),
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "NYC  ", emptyMap(), true),
                // Exactly at the boundary (min = 3): length 3 passes.
                Case(DemoCustomValidationPreset.MIN_LENGTH_3, OptionalFieldType.CITY, "ABC", emptyMap(), true),
                Case(DemoCustomValidationPreset.ALWAYS_INVALID, OptionalFieldType.CITY, "anything", emptyMap(), false),
                Case(DemoCustomValidationPreset.NO_DIGITS, OptionalFieldType.CITY, "NYC1", emptyMap(), false),
                Case(DemoCustomValidationPreset.DIGITS_ONLY, OptionalFieldType.PHONE_NUMBER_AREA_CODE, "12A", emptyMap(), false),
                Case(DemoCustomValidationPreset.NO_SPECIAL_CHARACTERS, OptionalFieldType.STREET, "Main St.", emptyMap(), false),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.ZIP_CODE, "bad", mapOf(AdditionalField.COUNTRY to "US"), false),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.ZIP_CODE, "94107", mapOf(AdditionalField.COUNTRY to "US"), true),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.ZIP_CODE, "bad", mapOf(AdditionalField.COUNTRY to "CA"), true),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.SHIPPING_ZIP, "94107", mapOf(AdditionalField.SHIPPING_COUNTRY to "US"), true),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.STATE, "C", mapOf(AdditionalField.COUNTRY to "US"), false),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.STATE, "CA", mapOf(AdditionalField.COUNTRY to "US"), true),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.SHIPPING_STATE, "C", mapOf(AdditionalField.SHIPPING_COUNTRY to "US"), false),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.SHIPPING_STATE, "NY", mapOf(AdditionalField.SHIPPING_COUNTRY to "US"), true),
                Case(DemoCustomValidationPreset.ONLY_WHEN_US, OptionalFieldType.CITY, "X", mapOf(AdditionalField.COUNTRY to "US"), true),
            )

        cases.forEach { case ->
            val result =
                DemoPresetFieldValidators.validate(
                    preset = case.preset,
                    fieldType = case.fieldType,
                    value = case.value,
                    fields = case.fields,
                )
            assertEquals(
                "${case.preset} on ${case.fieldType} value=${case.value}",
                case.expectedValid,
                result.isValid,
            )
        }
    }

    @Test
    fun `hosted zip and state validator matrix`() {
        data class ZipCase(val zip: String, val country: String, val expectedValid: Boolean)
        listOf(
            ZipCase("94107", "US", true),
            ZipCase("94107-1234", "US", true),
            ZipCase("411037", "US", false),
            ZipCase("", "US", false),
            ZipCase("invalid", "CA", true),
        ).forEach { case ->
            val result = DemoHostedFieldValidators.validateZip(case.zip, mapOf(AdditionalField.COUNTRY to case.country))
            assertEquals("zip=${case.zip} country=${case.country}", case.expectedValid, result.isValid)
        }

        data class StateCase(val state: String, val country: String, val expectedValid: Boolean)
        listOf(
            StateCase("CA", "US", true),
            StateCase("", "US", false),
            StateCase("CAL", "US", false),
            StateCase("X", "CA", true),
        ).forEach { case ->
            val result = DemoHostedFieldValidators.validateState(case.state, mapOf(AdditionalField.COUNTRY to case.country))
            assertEquals("state=${case.state} country=${case.country}", case.expectedValid, result.isValid)
        }

        assertTrue(
            DemoHostedFieldValidators.validateZip("  94107  ", mapOf(AdditionalField.COUNTRY to "US")).isValid,
        )
    }

    @Test
    fun `shouldRegisterPreset and core name preset rules`() {
        assertFalse(
            DemoPresetFieldValidators.shouldRegisterPreset(
                DemoCustomValidationPreset.ONLY_WHEN_US,
                OptionalFieldType.CITY,
            ),
        )
        assertTrue(
            DemoPresetFieldValidators.shouldRegisterPreset(
                DemoCustomValidationPreset.ONLY_WHEN_US,
                OptionalFieldType.ZIP_CODE,
            ),
        )
        assertFalse(
            DemoPresetFieldValidators.shouldRegisterPreset(
                DemoCustomValidationPreset.ONLY_WHEN_US,
                DemoCoreNameField.FIRST_NAME,
            ),
        )
        assertFalse(
            DemoPresetFieldValidators.validate(
                preset = DemoCustomValidationPreset.MIN_LENGTH_3,
                field = DemoCoreNameField.FULL_NAME,
                value = "Al",
                fields = emptyMap(),
            ).isValid,
        )
        listOf(
            DemoCustomValidationPreset.MIN_LENGTH_3,
            DemoCustomValidationPreset.NO_DIGITS,
            DemoCustomValidationPreset.DIGITS_ONLY,
            DemoCustomValidationPreset.NO_SPECIAL_CHARACTERS,
        ).forEach { preset ->
            assertTrue(
                DemoPresetFieldValidators.validate(
                    preset = preset,
                    fieldType = OptionalFieldType.CITY,
                    value = "",
                    fields = emptyMap(),
                ).isValid,
            )
        }
    }
}
