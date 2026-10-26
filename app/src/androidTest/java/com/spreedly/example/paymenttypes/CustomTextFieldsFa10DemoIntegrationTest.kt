package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.screens.customtextfields.CustomTextFieldsScreen
import com.spreedly.example.validation.DemoExpiryManifestTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation coverage for the FA10 demo switch on Custom Text Fields.
 * Verifies UI toggling only — submit/tokenize is covered by payments-core integration tests.
 */
@RunWith(AndroidJUnit4::class)
class CustomTextFieldsFa10DemoIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun customTextFields_displaysFa10DemoSwitch() {
        composeTestRule.setContent {
            CustomTextFieldsScreen()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(DemoExpiryManifestTest.SWITCH_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DemoExpiryManifestTest.SWITCH_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun customTextFields_fa10SwitchOnShowsMonthAndYearFields() {
        composeTestRule.setContent {
            CustomTextFieldsScreen()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DemoExpiryManifestTest.SWITCH_TEST_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MM/YY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Year").assertIsDisplayed()
    }

    @Test
    fun customTextFields_fa10SwitchOffHidesMonthAndYearFields() {
        composeTestRule.setContent {
            CustomTextFieldsScreen()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DemoExpiryManifestTest.SWITCH_TEST_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DemoExpiryManifestTest.SWITCH_TEST_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MM/YY").assertIsDisplayed()
    }
}
