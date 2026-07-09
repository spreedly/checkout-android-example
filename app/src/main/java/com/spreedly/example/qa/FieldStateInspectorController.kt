package com.spreedly.example.qa

import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.HostedCardDisplayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class FieldStateInspectorController(
    private val sdk: Spreedly,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val _uiState = MutableStateFlow(FieldStateInspectorUiState())
    val uiState: StateFlow<FieldStateInspectorUiState> = _uiState.asStateFlow()

    private var aggregateFields: List<Pair<String, () -> Boolean>> = emptyList()
    private var isFormValidProvider: () -> Boolean = { false }

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US).withZone(ZoneId.systemDefault())

    fun configureAggregate(
        fields: List<Pair<String, () -> Boolean>>,
        isFormValid: () -> Boolean,
    ) {
        this.aggregateFields = fields
        this.isFormValidProvider = isFormValid
        refreshAggregate()
    }

    fun onFieldStateChanged(state: HostedFieldState) {
        when (state.fieldType) {
            is FormFieldType.CARD -> {
                _uiState.update { it.copy(cardSnapshot = state) }
                appendEventLog(state)
                refreshMismatchFromSnapshots()
            }
            is FormFieldType.CVV -> {
                _uiState.update { it.copy(cvvSnapshot = state) }
                appendEventLog(state)
            }
            else -> Unit
        }
    }

    fun logOpaqueFieldChange(fieldType: FormFieldType) {
        when (fieldType) {
            is FormFieldType.CARD ->
                _uiState.update {
                    it.copy(onChangeReadout = "onChange: card number (opaque — not logged)")
                }
            is FormFieldType.CVV ->
                _uiState.update {
                    it.copy(onChangeReadout = "onChange: CVC (opaque — not logged)")
                }
            else -> Unit
        }
    }

    fun logNonSensitiveFieldChange(fieldType: FormFieldType, value: String) {
        logOnChangeReadout(hostedFieldDisplayName(fieldType), value)
    }

    fun logOnChangeReadout(label: String, value: String) {
        val display = if (value.isEmpty()) "(empty)" else value
        _uiState.update {
            it.copy(onChangeReadout = "onChange: $label = \"$display\"")
        }
    }

    fun refreshMismatch(global: HostedCardDisplayState) {
        val mismatch =
            globalDisplayMismatchMessage(
                card = _uiState.value.cardSnapshot,
                globalFormat = global.cardNumberFormat,
                globalPanMasked = global.panMasked,
            )
        _uiState.update { it.copy(mismatchMessage = mismatch) }
    }

    fun resetInspector() {
        _uiState.value = FieldStateInspectorUiState()
        refreshAggregate()
    }

    private fun refreshMismatchFromSnapshots() {
        refreshMismatch(sdk.hostedCardDisplayState.value)
    }

    private fun appendEventLog(state: HostedFieldState) {
        val fieldLabel = hostedFieldDisplayName(state.fieldType)
        val event = hostedFieldEventLabel(state.eventType)
        val time = timeFormatter.format(clock.instant())
        val line = "$event · $fieldLabel · $time"
        _uiState.update { current ->
            val log = (listOf(line) + current.eventLog).take(5)
            current.copy(
                lastEventSummary = "Last event: $line",
                eventLog = log,
            )
        }
    }

    private fun refreshAggregate() {
        if (aggregateFields.isEmpty()) return
        val allValid = isFormValidProvider()
        val invalid =
            aggregateFields.filter { (_, isValid) -> !isValid() }.map { (name, _) -> name }
        val invalidText =
            if (invalid.isEmpty()) {
                "none"
            } else {
                invalid.joinToString(", ")
            }
        val registered = sdk.getRegisteredFieldCount()
        val readout =
            "Form valid: ${logYesNo(allValid)} · invalid: $invalidText · registered: $registered"
        _uiState.update { it.copy(aggregateReadout = readout) }
    }
}
