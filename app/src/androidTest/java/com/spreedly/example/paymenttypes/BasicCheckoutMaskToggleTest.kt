package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.screens.basiccheckout.BasicCheckoutScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicCheckoutMaskToggleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun basicCheckout_hasMerchantMaskToggle_andNoSdkPanEye() {
        composeTestRule.setContent {
            BasicCheckoutScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pretty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plain").assertIsDisplayed()
        composeTestRule.onNodeWithText("Masked").assertIsDisplayed()
        composeTestRule.onNodeWithText("toggleMask()").assertIsDisplayed()

        composeTestRule.onNode(hasContentDescription("Show card number")).assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Hide card number")).assertDoesNotExist()
    }

    @Test
    fun basicCheckout_formatSegmentsAreClickable() {
        composeTestRule.setContent {
            BasicCheckoutScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Plain").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Masked").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Card Number").assertIsDisplayed()
    }
}
