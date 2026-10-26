package com.spreedly.example.validation

import com.spreedly.example.optionalfields.DemoOptionalFieldSelection
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.addValidation
import com.spreedly.sdk.removeValidation
import com.spreedly.sdk.ui.OptionalFieldType
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DemoOptionalFieldValidatorsTest {
    @Test
    fun `sync registration matrix`() {
        data class Case(
            val selection: DemoOptionalFieldSelection,
            val layout: DemoCoreNameFieldLayout = DemoCoreNameFieldLayout(emptyList()),
            val register: Set<AdditionalField> = emptySet(),
            val mustNotRegister: Set<AdditionalField> = emptySet(),
        )

        val defaultMustNot =
            setOf(
                AdditionalField.CITY,
                AdditionalField.ZIP_CODE,
                AdditionalField.EMAIL,
                AdditionalField.FULL_NAME,
                AdditionalField.FIRST_NAME,
                AdditionalField.LAST_NAME,
            )

        val cases =
            listOf(
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                            .withFieldCustomValidation(
                                OptionalFieldType.CITY,
                                DemoCustomValidationPreset.MIN_LENGTH_3,
                            ),
                    register = setOf(AdditionalField.CITY),
                    mustNotRegister = defaultMustNot - AdditionalField.CITY,
                ),
                Case(
                    selection = DemoOptionalFieldSelection.default(),
                    mustNotRegister = defaultMustNot,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                            .withFieldCustomValidation(
                                OptionalFieldType.CITY,
                                DemoCustomValidationPreset.ONLY_WHEN_US,
                            ),
                    mustNotRegister = defaultMustNot,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withFieldCustomValidation(
                                OptionalFieldType.ZIP_CODE,
                                DemoCustomValidationPreset.ONLY_WHEN_US,
                            ),
                    register = setOf(AdditionalField.ZIP_CODE),
                    mustNotRegister = defaultMustNot - AdditionalField.ZIP_CODE,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withFieldCustomValidation(
                                OptionalFieldType.EMAIL,
                                DemoCustomValidationPreset.ALWAYS_INVALID,
                            ),
                    mustNotRegister = defaultMustNot,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withCoreNameCustomValidation(
                                DemoCoreNameField.FULL_NAME,
                                DemoCustomValidationPreset.ALWAYS_INVALID,
                            ),
                    layout = DemoCoreNameField.forCustomTextFieldsScreen(),
                    register = setOf(AdditionalField.FULL_NAME),
                    mustNotRegister = defaultMustNot - AdditionalField.FULL_NAME,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withCoreNameCustomValidation(
                                DemoCoreNameField.FIRST_NAME,
                                DemoCustomValidationPreset.MIN_LENGTH_3,
                            )
                            .withCoreNameCustomValidation(
                                DemoCoreNameField.LAST_NAME,
                                DemoCustomValidationPreset.NO_DIGITS,
                            ),
                    layout = DemoCoreNameField.forBottomSheetScreen(),
                    register = setOf(AdditionalField.FIRST_NAME, AdditionalField.LAST_NAME),
                    mustNotRegister =
                        defaultMustNot - AdditionalField.FIRST_NAME - AdditionalField.LAST_NAME,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withCoreNameCustomValidation(
                                DemoCoreNameField.FULL_NAME,
                                DemoCustomValidationPreset.ALWAYS_INVALID,
                            ),
                    layout = DemoCoreNameField.forBottomSheetScreen(),
                    mustNotRegister = defaultMustNot,
                ),
                Case(
                    selection =
                        DemoOptionalFieldSelection.default()
                            .withCoreNameCustomValidation(
                                DemoCoreNameField.FULL_NAME,
                                DemoCustomValidationPreset.ALWAYS_INVALID,
                            ),
                    mustNotRegister = defaultMustNot,
                ),
            )

        cases.forEach { case ->
            val sdk = mockk<Spreedly>(relaxed = true)
            DemoOptionalFieldValidators.sync(sdk, case.selection, case.layout)
            case.register.forEach { field ->
                verify(exactly = 1) { sdk.addValidation(field, any()) }
            }
            case.mustNotRegister.forEach { field ->
                verify(exactly = 0) { sdk.addValidation(field, any()) }
            }
        }
    }

    @Test
    fun `sync removes stale validators on resync`() {
        val sdk = mockk<Spreedly>(relaxed = true)
        val withValidators =
            DemoOptionalFieldSelection.default()
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.ALWAYS_INVALID,
                )
                .withCoreNameCustomValidation(
                    DemoCoreNameField.LAST_NAME,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                )

        DemoOptionalFieldValidators.sync(sdk, withValidators, DemoCoreNameField.forBottomSheetScreen())
        DemoOptionalFieldValidators.sync(
            sdk,
            DemoOptionalFieldSelection.default(),
            DemoCoreNameField.forBottomSheetScreen(),
        )

        verify(atLeast = 1) { sdk.removeValidation(AdditionalField.CITY) }
        verify(atLeast = 1) { sdk.removeValidation(AdditionalField.LAST_NAME) }
    }

    @Test
    fun `sync with no layout never registers core name validators even when preset configured`() {
        // CustomTextFieldsViewModel calls sync without a layout (empty default).
        // Core name validators must not be registered regardless of what the selection holds.
        val sdk = mockk<Spreedly>(relaxed = true)
        val selectionWithFullName =
            DemoOptionalFieldSelection.default()
                .withCoreNameCustomValidation(
                    DemoCoreNameField.FULL_NAME,
                    DemoCustomValidationPreset.ALWAYS_INVALID,
                )

        DemoOptionalFieldValidators.sync(sdk, selectionWithFullName)

        verify(exactly = 0) { sdk.addValidation(AdditionalField.FULL_NAME, any()) }
        verify(exactly = 0) { sdk.addValidation(AdditionalField.FIRST_NAME, any()) }
        verify(exactly = 0) { sdk.addValidation(AdditionalField.LAST_NAME, any()) }
    }

    @Test
    fun `sync does not cross-remove validators on another Spreedly instance`() {
        val sdkA = mockk<Spreedly>(relaxed = true)
        val sdkB = mockk<Spreedly>(relaxed = true)
        val withCityValidator =
            DemoOptionalFieldSelection.default()
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                )

        DemoOptionalFieldValidators.sync(sdkA, withCityValidator)
        DemoOptionalFieldValidators.sync(sdkB, DemoOptionalFieldSelection.default())

        verify(exactly = 1) { sdkA.addValidation(AdditionalField.CITY, any()) }
        verify(exactly = 0) { sdkA.removeValidation(AdditionalField.CITY) }
        verify(exactly = 0) { sdkB.addValidation(AdditionalField.CITY, any()) }
    }

    @Test
    fun `clear removes validators only for the given Spreedly instance`() {
        val sdkA = mockk<Spreedly>(relaxed = true)
        val sdkB = mockk<Spreedly>(relaxed = true)
        val withCityValidator =
            DemoOptionalFieldSelection.default()
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.ALWAYS_INVALID,
                )

        DemoOptionalFieldValidators.sync(sdkA, withCityValidator)
        DemoOptionalFieldValidators.sync(sdkB, withCityValidator)

        DemoOptionalFieldValidators.clear(sdkA)

        verify(atLeast = 1) { sdkA.removeValidation(AdditionalField.CITY) }
        verify(exactly = 0) { sdkB.removeValidation(AdditionalField.CITY) }
    }

    @Test
    fun `clear on sdk with no prior sync is a no-op`() {
        val sdk = mockk<Spreedly>(relaxed = true)
        DemoOptionalFieldValidators.clear(sdk)
        listOf(
            AdditionalField.CITY,
            AdditionalField.ZIP_CODE,
            AdditionalField.EMAIL,
            AdditionalField.FULL_NAME,
        ).forEach { field ->
            verify(exactly = 0) { sdk.removeValidation(field) }
        }
    }

    @Test
    fun `sync after clear registers validators again`() {
        val sdk = mockk<Spreedly>(relaxed = true)
        val withCityValidator =
            DemoOptionalFieldSelection.default()
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.MIN_LENGTH_3,
                )

        DemoOptionalFieldValidators.sync(sdk, withCityValidator)
        DemoOptionalFieldValidators.clear(sdk)
        DemoOptionalFieldValidators.sync(sdk, withCityValidator)

        verify(exactly = 2) { sdk.addValidation(AdditionalField.CITY, any()) }
    }

    @Test
    fun `second clear on same sdk is a no-op`() {
        val sdk = mockk<Spreedly>(relaxed = true)
        val withCityValidator =
            DemoOptionalFieldSelection.default()
                .withFieldEnabled(OptionalFieldType.CITY, enabled = true)
                .withFieldCustomValidation(
                    OptionalFieldType.CITY,
                    DemoCustomValidationPreset.ALWAYS_INVALID,
                )

        DemoOptionalFieldValidators.sync(sdk, withCityValidator)
        DemoOptionalFieldValidators.clear(sdk)
        DemoOptionalFieldValidators.clear(sdk)

        verify(exactly = 1) { sdk.removeValidation(AdditionalField.CITY) }
    }
}
