package com.spreedly.example.paymenttypes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.TestConfiguration
import com.spreedly.example.screens.basiccheckout.BasicCheckoutScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicCheckoutRotationInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun basicCheckout_recreate_preserves_screen() {
        composeTestRule.setContent {
            BasicCheckoutScreen()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BASIC_CHECKOUT_TITLE).assertIsDisplayed()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BASIC_CHECKOUT_TITLE).assertIsDisplayed()
    }
}
