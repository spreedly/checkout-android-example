package com.spreedly.example.validation

import androidx.compose.runtime.Immutable
import com.spreedly.sdk.AdditionalField
import com.spreedly.sdk.ui.NameFieldDisplayMode

/**
 * Core cardholder name fields in the custom-validator allowlist but outside [com.spreedly.sdk.ui.OptionalFieldType].
 *
 * Demo exposes custom validation presets only — no enable/required toggles.
 */
enum class DemoCoreNameField(val displayLabel: String) {
    FIRST_NAME("First Name"),
    LAST_NAME("Last Name"),
    FULL_NAME("Full Name"),
    ;

    fun toAdditionalField(): AdditionalField =
        when (this) {
            FIRST_NAME -> AdditionalField.FIRST_NAME
            LAST_NAME -> AdditionalField.LAST_NAME
            FULL_NAME -> AdditionalField.FULL_NAME
        }

    companion object {
        val wireKeys: Set<String> = entries.map { it.toAdditionalField().fieldName }.toSet()

        fun forNameDisplayMode(mode: NameFieldDisplayMode): List<DemoCoreNameField> =
            when (mode) {
                NameFieldDisplayMode.SINGLE_FIELD -> listOf(FULL_NAME)
                NameFieldDisplayMode.SEPARATE_FIELDS -> listOf(FIRST_NAME, LAST_NAME)
            }

        fun forCustomTextFieldsScreen(): DemoCoreNameFieldLayout =
            DemoCoreNameFieldLayout(listOf(FULL_NAME))

        fun forBottomSheetScreen(): DemoCoreNameFieldLayout =
            DemoCoreNameFieldLayout(forNameDisplayMode(NameFieldDisplayMode.SEPARATE_FIELDS))
    }
}

/** Stable Compose parameter for the name fields mounted on a demo screen. */
@Immutable
data class DemoCoreNameFieldLayout(val fields: List<DemoCoreNameField>) {
    val isEmpty: Boolean get() = fields.isEmpty()
}
