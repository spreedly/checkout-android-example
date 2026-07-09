package com.spreedly.example.qa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.ui.HostedCardDisplayState

@Composable
fun HostedFieldStateInspector(
    uiState: FieldStateInspectorUiState,
    hostedCardDisplayState: HostedCardDisplayState,
    modifier: Modifier = Modifier,
) {
    FieldStateInspectorCard(
        uiState = uiState,
        hostedCardDisplayState = hostedCardDisplayState,
        modifier = modifier,
    )
}

@Deprecated("Use HostedFieldStateInspector(uiState, hostedCardDisplayState)")
@Composable
fun HostedFieldStateInspector(
    cardState: HostedFieldState?,
    cvvState: HostedFieldState?,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    FieldStateInspectorCard(
        uiState =
            FieldStateInspectorUiState(
                cardSnapshot = cardState,
                cvvSnapshot = cvvState,
            ),
        hostedCardDisplayState = HostedCardDisplayState(),
        modifier = modifier,
    )
}

@Composable
fun HeadlessHostedFieldsConfigCard(
    enableAutofill: Boolean,
    onEnableAutofillChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CARD/CVV read display via sdk on SPLTextField",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "enableAutofill (SPLTextField)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = enableAutofill,
                onCheckedChange = onEnableAutofillChange,
            )
        }
    }
}

@Deprecated("Use HeadlessHostedFieldsConfigCard")
@Composable
fun HeadlessHostedFieldsConfigBar(
    enableAutofill: Boolean,
    onEnableAutofillChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HeadlessHostedFieldsConfigCard(
        enableAutofill = enableAutofill,
        onEnableAutofillChange = onEnableAutofillChange,
        modifier = modifier,
    )
}

@Composable
fun ExpressDisplayConfigBar(
    enableAutofill: Boolean,
    onEnableAutofillChange: (Boolean) -> Unit,
    useMaskedFormat: Boolean,
    onUseMaskedFormatChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PaymentSheetDisplayConfig (sheet seed; global setNumberFormat overrides after open)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "enableAutofill",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = enableAutofill,
                onCheckedChange = onEnableAutofillChange,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "cardNumberFormat MASKED seed",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = useMaskedFormat,
                onCheckedChange = onUseMaskedFormatChange,
            )
        }
    }
}
