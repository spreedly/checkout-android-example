package com.spreedly.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.screens.flexibleexpiry.FlexibleExpiryScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleBasicCheckoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun basicCheckoutScreen_displaysTitle() {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Validation Parameters Demo").assertIsDisplayed()
    }
}
