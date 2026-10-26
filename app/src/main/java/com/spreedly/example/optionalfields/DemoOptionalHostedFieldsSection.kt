package com.spreedly.example.optionalfields

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.spreedly.hostedfields.ui.SPLTextField
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.OptionalFieldType
import com.spreedly.sdk.ui.toFormFieldType

/**
 * Optional hosted catalogue fields driven by [DemoOptionalFieldActions.dispatchChange] callbacks.
 * [writeCatalogueValueToSdk] is `false` so values are not double-written via
 * [com.spreedly.sdk.syncHostedCatalogueFieldValue].
 */
@Composable
fun DemoOptionalHostedFieldsSection(
    sdk: Spreedly,
    selection: DemoOptionalFieldSelection,
    fieldConfigFor: (FormFieldType) -> CustomFieldsConfig?,
    onValidationChange: (FormFieldType, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sectionTitle: String = "Additional Hosted Fields",
) {
    val enabledSorted =
        selection.enabledFields.sortedBy { DemoOptionalFieldSelection.advancedCatalog().indexOf(it) }
    if (enabledSorted.isEmpty()) return

    androidx.compose.material3.Text(
        text = sectionTitle,
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        modifier = modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    enabledSorted.forEach { type ->
        val isRequired = selection.isFieldRequired(type)
        val formFieldType = type.toFormFieldType(isRequired)
        val value = DemoOptionalFieldValues.read(sdk.paymentState.value, type)
        key(type, isRequired) {
            SPLTextField(
                label = DemoOptionalFieldActions.displayLabel(type),
                formFieldType = formFieldType,
                config = fieldConfigFor(formFieldType),
                value = value,
                onChange = { newValue ->
                    DemoOptionalFieldActions.dispatchChange(sdk, type, newValue, isRequired)
                },
                onValidationChange = { isValid ->
                    onValidationChange(formFieldType, isValid)
                },
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                sdk = sdk,
                writeCatalogueValueToSdk = false,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
