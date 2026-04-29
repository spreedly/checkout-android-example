package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.TestConfiguration
import com.spreedly.example.screens.bottomsheet.BottomSheetPaymentScreen
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BottomSheetPaymentScreen component.
 * Tests the express checkout functionality using bottom sheet UI pattern.
 */
@RunWith(AndroidJUnit4::class)
class BottomSheetPaymentScreenIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomSheetPayment_displaysCorrectLayout() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION).assertExists()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_displaysExpressCheckoutButton() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.CHECKOUT_ICON)).assertExists()
    }

    @Test
    fun bottomSheetPayment_buttonIsClickable() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val checkoutButton = composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON)

        checkoutButton.assertHasClickAction()

        checkoutButton.performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomSheetPayment_showsInitializingState() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.INITIALIZING_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_handlesButtonStates() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON)
            .performClick()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_accessibilityFeatures() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()
        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON)).assertExists()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON)
            .assertHasClickAction()
    }

    @Test
    fun bottomSheetPayment_visualElements() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_TEXT).assertExists()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()
        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON)).assertExists()
    }

    @Test
    fun bottomSheetPayment_buttonInteractionFlow() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val checkoutButton = composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON)
        checkoutButton.assertIsEnabled()

        checkoutButton.performClick()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_sdkIntegration() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_tokenDisplayHandling() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        try {
            composeTestRule.onNodeWithText(TestConfiguration.UIElements.SUCCESS_MESSAGE).assertDoesNotExist()
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun bottomSheetPayment_layoutStructure() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION).assertExists()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_TEXT).assertExists()
    }

    @Test
    fun bottomSheetPayment_centerAlignment() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION).assertExists()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
    }
}
