package com.spreedly.example.qa

import com.spreedly.hostedfields.models.HostedFieldState

data class FieldStateInspectorUiState(
    val cardSnapshot: HostedFieldState? = null,
    val cvvSnapshot: HostedFieldState? = null,
    val lastEventSummary: String = "Last event: —",
    val eventLog: List<String> = emptyList(),
    val aggregateReadout: String = "",
    val onChangeReadout: String = DEFAULT_ON_CHANGE_READOUT,
    val mismatchMessage: String? = null,
) {
    companion object {
        const val DEFAULT_ON_CHANGE_READOUT =
            "onChange: edit a field to see values (card/CVC stay opaque)."
    }
}
