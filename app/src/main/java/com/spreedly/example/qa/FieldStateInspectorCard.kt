package com.spreedly.example.qa

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.ui.HostedCardDisplayState

@Composable
fun FieldStateInspectorCard(
    uiState: FieldStateInspectorUiState,
    hostedCardDisplayState: HostedCardDisplayState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp),
                )
                .padding(16.dp)
                .testTag("field-state-inspector-card"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = INSPECTOR_TITLE,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = INSPECTOR_CAPTION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = INSPECTOR_WIRING_CAPTION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("custom-form-wiring-readout"),
        )
        GlobalDisplayPanel(hostedCardDisplayState = hostedCardDisplayState)
        uiState.mismatchMessage?.let { mismatch ->
            Text(
                text = mismatch,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag("custom-form-snapshot-global-mismatch"),
            )
        }
        Text(
            text = uiState.lastEventSummary,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (uiState.lastEventSummary.contains("PAN_MASK_CHANGED")) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.testTag("custom-form-last-event-readout"),
        )
        EventLogSection(eventLog = uiState.eventLog)
        CardSnapshotPanel(state = uiState.cardSnapshot)
        CvvSnapshotPanel(state = uiState.cvvSnapshot)
        if (uiState.aggregateReadout.isNotEmpty()) {
            Text(
                text = uiState.aggregateReadout,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("custom-form-aggregate-validation-readout"),
            )
        }
        Text(
            text = uiState.onChangeReadout,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("custom-form-onchange-readout"),
        )
    }
}

@Composable
@SuppressLint("ComposeUnstableCollections")
private fun EventLogSection(eventLog: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.testTag("custom-form-event-log")) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("custom-form-event-log-toggle"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = EVENT_LOG_TITLE,
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse event log" else "Expand event log",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(
                modifier =
                    Modifier
                        .padding(top = 6.dp)
                        .testTag("custom-form-event-log-entries"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (eventLog.isEmpty()) {
                    Text(
                        text = "No events yet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    eventLog.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalDisplayPanel(hostedCardDisplayState: HostedCardDisplayState) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                    RoundedCornerShape(8.dp),
                )
                .padding(10.dp)
                .testTag("custom-form-global-display-readout"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = GLOBAL_DISPLAY_TITLE,
            style = MaterialTheme.typography.titleSmall,
        )
        InspectorRow("Format", cardNumberFormatLabel(hostedCardDisplayState.cardNumberFormat))
        InspectorRow("panMasked", logYesNo(hostedCardDisplayState.panMasked))
        InspectorRow("cvvDisplayMasked", logYesNo(hostedCardDisplayState.cvvDisplayMasked))
    }
}

@Composable
private fun CardSnapshotPanel(state: HostedFieldState?) {
    SnapshotPanelContainer(title = CARD_PANEL_TITLE) {
        if (state == null) {
            InspectorEmptyHint(CARD_EMPTY_HINT)
        } else {
            SnapshotPanelContent(state = state, isCardNumber = true)
        }
    }
}

@Composable
private fun CvvSnapshotPanel(state: HostedFieldState?) {
    SnapshotPanelContainer(
        title = CVV_PANEL_TITLE,
        testTag = "custom-form-cvc-field-state-inspector",
    ) {
        if (state == null) {
            InspectorEmptyHint(CVV_EMPTY_HINT)
        } else {
            SnapshotPanelContent(state = state, isCardNumber = false)
        }
    }
}

@Composable
private fun SnapshotPanelContainer(
    title: String,
    testTag: String = "custom-form-field-state-inspector",
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    RoundedCornerShape(8.dp),
                )
                .padding(10.dp)
                .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun SnapshotPanelContent(
    state: HostedFieldState,
    isCardNumber: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InspectorRow("Event", hostedFieldEventLabel(state.eventType))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InspectorRow("Valid", logYesNo(state.isValid))
            InspectorRow("Focused", logYesNo(state.isFocused))
            InspectorRow("Empty", logYesNo(state.isEmpty))
        }
        if (isCardNumber) {
            HorizontalDivider()
            Text(
                text = "PAN display (from snapshot)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InspectorRow(
                    "Format",
                    state.panDisplayFormat?.let { cardNumberFormatLabel(it) } ?: "—",
                )
                InspectorRow(
                    "Policy masked",
                    state.panDisplayPolicyMasked?.let { logYesNo(it) } ?: "—",
                )
                InspectorRow("Digits hidden", logYesNo(state.isPanMasked))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InspectorRow(
                    "Brand",
                    cardSchemeRawValue(state.cardScheme) ?: "—",
                )
                InspectorRow(
                    "PAN digit count",
                    state.numberLength?.toString() ?: "0",
                )
                InspectorRow("IIN", state.iin ?: "—")
            }
        } else {
            InspectorRow("CVV digit count", state.cvvLength?.toString() ?: "0")
        }
    }
}

@Composable
private fun InspectorRow(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InspectorEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
