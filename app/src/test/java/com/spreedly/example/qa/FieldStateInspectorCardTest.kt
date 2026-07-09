package com.spreedly.example.qa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.spreedly.sdk.ui.CardNumberFormat
import com.spreedly.sdk.ui.HostedCardDisplayState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FieldStateInspectorCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders inspector card and wiring readout test tags`() {
        composeRule.setContent {
            FieldStateInspectorCard(
                uiState = FieldStateInspectorUiState(),
                hostedCardDisplayState =
                    HostedCardDisplayState(
                        cardNumberFormat = CardNumberFormat.MASKED,
                        panMasked = true,
                    ),
            )
        }
        composeRule.onNodeWithTag("field-state-inspector-card").assertIsDisplayed()
        composeRule.onNodeWithTag("custom-form-wiring-readout").assertIsDisplayed()
        composeRule.onNodeWithTag("custom-form-event-log-toggle").assertExists()
    }
}
