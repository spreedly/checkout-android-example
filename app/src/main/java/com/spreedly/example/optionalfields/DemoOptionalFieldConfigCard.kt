package com.spreedly.example.optionalfields

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.validation.DemoCoreNameField
import com.spreedly.example.validation.DemoCoreNameFieldLayout
import com.spreedly.example.validation.DemoCustomValidationPreset
import com.spreedly.example.validation.DemoPresetFieldValidators
import com.spreedly.sdk.ui.OptionalFieldType

@Composable
fun DemoOptionalFieldConfigCard(
    selection: DemoOptionalFieldSelection,
    onSelectionChange: (DemoOptionalFieldSelection) -> Unit,
    availableCoreNameFields: DemoCoreNameFieldLayout,
    modifier: Modifier = Modifier,
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Field configuration",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )

            if (!availableCoreNameFields.isEmpty) {
                Text(
                    text = "Core name fields",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = coreNameSectionSubtitle(availableCoreNameFields),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                availableCoreNameFields.fields.forEach { field ->
                    CoreNameValidationRow(
                        field = field,
                        customValidation = selection.coreNameCustomValidationPreset(field),
                        onCustomValidationChange = { preset ->
                            onSelectionChange(selection.withCoreNameCustomValidation(field, preset))
                        },
                    )
                }
            }

            Text(
                text = "Optional address fields",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Turn on sections or pick individual fields, then assign a custom validation preset per enabled row.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GroupSwitchRow(
                title = "Billing address",
                subtitle = "Country, state, zip",
                checked = selection.isGroupFullyEnabled(DemoAddressGroup.BILLING),
                onCheckedChange = { enabled ->
                    onSelectionChange(selection.withGroupEnabled(DemoAddressGroup.BILLING, enabled))
                },
            )
            GroupSwitchRow(
                title = "Shipping address",
                subtitle = "Shipping country, state, zip",
                checked = selection.isGroupFullyEnabled(DemoAddressGroup.SHIPPING),
                onCheckedChange = { enabled ->
                    onSelectionChange(selection.withGroupEnabled(DemoAddressGroup.SHIPPING, enabled))
                },
            )
            GroupSwitchRow(
                title = "Email",
                subtitle = "Contact email field",
                checked = selection.isGroupFullyEnabled(DemoAddressGroup.CONTACT),
                onCheckedChange = { enabled ->
                    onSelectionChange(selection.withGroupEnabled(DemoAddressGroup.CONTACT, enabled))
                },
            )

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide individual fields" else "Choose individual fields…")
            }
            if (!showAdvanced) {
                Text(
                    text = "Custom validation presets are configured per field in individual mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showAdvanced) {
                DemoOptionalFieldSelection.advancedCatalog().forEach { type ->
                    AdvancedFieldRow(
                        type = type,
                        enabled = selection.isFieldEnabled(type),
                        required = selection.isFieldRequired(type),
                        customValidation = selection.customValidationPreset(type),
                        onEnabledChange = { enabled ->
                            onSelectionChange(selection.withFieldEnabled(type, enabled))
                        },
                        onRequiredChange = { required ->
                            onSelectionChange(selection.withFieldRequired(type, required))
                        },
                        onCustomValidationChange = { preset ->
                            onSelectionChange(selection.withFieldCustomValidation(type, preset))
                        },
                    )
                }
            }
        }
    }
}

private fun coreNameSectionSubtitle(layout: DemoCoreNameFieldLayout): String {
    val fields = layout.fields
    return when {
        fields == listOf(DemoCoreNameField.FULL_NAME) ->
            "Merchant-owned full name on this screen — presets run at Pay preflight only (not blur)."
        fields.contains(DemoCoreNameField.FIRST_NAME) && fields.contains(DemoCoreNameField.LAST_NAME) ->
            "First and last name in the payment sheet — custom validation on blur and Pay."
        else -> "Name fields on this screen — custom validation only (no enable toggle)."
    }
}

@Composable
private fun GroupSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoreNameValidationRow(
    field: DemoCoreNameField,
    customValidation: DemoCustomValidationPreset,
    onCustomValidationChange: (DemoCustomValidationPreset) -> Unit,
) {
    CustomValidationPresetDropdown(
        fieldLabel = field.displayLabel,
        dropdownSaveKey = "core_name_${field.name}",
        customValidation = customValidation,
        showOnlyWhenUs = DemoPresetFieldValidators.onlyWhenUsAppliesTo(field),
        enabled = true,
        onCustomValidationChange = onCustomValidationChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFieldRow(
    type: OptionalFieldType,
    enabled: Boolean,
    required: Boolean,
    customValidation: DemoCustomValidationPreset,
    onEnabledChange: (Boolean) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onCustomValidationChange: (DemoCustomValidationPreset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onEnabledChange(!enabled) },
            ) {
                Text(
                    text = DemoOptionalFieldActions.displayLabel(type),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "Req",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 4.dp),
            )
            Switch(
                checked = required,
                onCheckedChange = onRequiredChange,
                enabled = enabled,
            )
        }
        CustomValidationPresetDropdown(
            fieldLabel = null,
            dropdownSaveKey = "optional_${type.name}",
            customValidation = customValidation,
            showOnlyWhenUs = DemoPresetFieldValidators.onlyWhenUsAppliesTo(type),
            enabled = enabled,
            onCustomValidationChange = onCustomValidationChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomValidationPresetDropdown(
    fieldLabel: String?,
    dropdownSaveKey: String,
    customValidation: DemoCustomValidationPreset,
    showOnlyWhenUs: Boolean,
    enabled: Boolean,
    onCustomValidationChange: (DemoCustomValidationPreset) -> Unit,
) {
    var presetExpanded by rememberSaveable(dropdownSaveKey) { mutableStateOf(false) }

    if (fieldLabel != null) {
        Text(
            text = fieldLabel,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    ExposedDropdownMenuBox(
        expanded = presetExpanded && enabled,
        onExpandedChange = { expanded ->
            if (enabled) {
                presetExpanded = expanded
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = customValidation.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Custom validation") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded && enabled) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = enabled)
                    .fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = presetExpanded && enabled,
            onDismissRequest = { presetExpanded = false },
        ) {
            DemoCustomValidationPreset.entries
                .filter { preset ->
                    preset != DemoCustomValidationPreset.ONLY_WHEN_US || showOnlyWhenUs
                }
                .forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = {
                            presetExpanded = false
                            onCustomValidationChange(preset)
                        },
                    )
                }
        }
    }
}
