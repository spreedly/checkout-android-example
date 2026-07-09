package com.spreedly.example.qa

import com.spreedly.hostedfields.models.HostedFieldEventType
import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.models.CardScheme
import com.spreedly.sdk.models.FormFieldType
import com.spreedly.sdk.ui.CardNumberFormat
import com.spreedly.sdk.ui.HostedCardDisplayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FieldStateInspectorControllerTest {

    private fun cardState(
        eventType: HostedFieldEventType = HostedFieldEventType.INPUT,
        panDisplayFormat: CardNumberFormat? = CardNumberFormat.MASKED,
        panDisplayPolicyMasked: Boolean? = true,
    ) = HostedFieldState(
        fieldType = FormFieldType.CARD(true),
        eventType = eventType,
        isFocused = true,
        isValid = false,
        isEmpty = false,
        cardScheme = CardScheme.VISA,
        numberLength = 4,
        cvvLength = null,
        isPanMasked = true,
        iin = null,
        panDisplayFormat = panDisplayFormat,
        panDisplayPolicyMasked = panDisplayPolicyMasked,
    )

    @Test
    fun `onFieldStateChanged stores CARD snapshot and appends event log`() {
        val controller = FieldStateInspectorController(Spreedly())
        controller.onFieldStateChanged(cardState(eventType = HostedFieldEventType.FOCUS))
        val ui = controller.uiState.value
        assertEquals(CardScheme.VISA, ui.cardSnapshot?.cardScheme)
        assertTrue(ui.lastEventSummary.contains("FOCUS"))
        assertEquals(1, ui.eventLog.size)
    }

    @Test
    fun `configureAggregate reflects invalid fields and registered count`() {
        val controller = FieldStateInspectorController(Spreedly())
        controller.configureAggregate(
            fields =
                listOf(
                    "Card number" to { false },
                    "Security code (CVC)" to { true },
                ),
            isFormValid = { false },
        )
        val readout = controller.uiState.value.aggregateReadout
        assertTrue(readout.contains("Form valid: no"))
        assertTrue(readout.contains("Card number"))
        assertTrue(readout.contains("registered:"))
    }

    @Test
    fun `refreshMismatch reports format drift`() {
        val controller = FieldStateInspectorController(Spreedly())
        controller.onFieldStateChanged(
            cardState(
                panDisplayFormat = CardNumberFormat.PRETTY,
                panDisplayPolicyMasked = false,
            ),
        )
        controller.refreshMismatch(
            HostedCardDisplayState(
                cardNumberFormat = CardNumberFormat.MASKED,
                panMasked = true,
            ),
        )
        assertNotNull(controller.uiState.value.mismatchMessage)
        assertTrue(controller.uiState.value.mismatchMessage!!.contains("Snapshot format"))
    }

    @Test
    fun `resetInspector clears snapshots and event log`() {
        val controller = FieldStateInspectorController(Spreedly())
        controller.onFieldStateChanged(cardState())
        controller.resetInspector()
        val ui = controller.uiState.value
        assertNull(ui.cardSnapshot)
        assertNull(ui.cvvSnapshot)
        assertEquals("Last event: —", ui.lastEventSummary)
        assertTrue(ui.eventLog.isEmpty())
    }
}
