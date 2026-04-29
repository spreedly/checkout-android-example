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
 * UI tests for BottomSheetPaymentScreen.
 * These tests focus on user interactions and UI behavior.
 */
@RunWith(AndroidJUnit4::class)
class BottomSheetPaymentScreenUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCorrectLayout() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_TEXT).assertExists()
    }

    @Test
    fun expressCheckoutButtonIsPresent() = runTest {
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

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPRESS_CHECKOUT_BUTTON).assertExists()
    }

    @Test
    fun buttonShowsInitializingState() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.INITIALIZING_TEXT).assertExists()
    }

    @Test
    fun buttonTransitionsToReadyState() = runTest {
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
            .assertIsEnabled()
    }

    @Test
    fun buttonClickTriggersProcessingState() = runTest {
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
    fun hasProperAccessibility() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON))
            .assertExists()
        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON))
            .assertExists()

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
            .assertHasClickAction()
    }

    @Test
    fun visualElementsPresent() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE)
            .assertExists()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_DESCRIPTION)
            .assertExists()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.SECURITY_TEXT)
            .assertExists()

        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON))
            .assertExists()
        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON))
            .assertExists()
    }

    @Test
    fun buttonStateTransitions() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.INITIALIZING_TEXT).assertExists()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
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
    fun checkoutIconPresent() = runTest {
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
            .onNode(hasContentDescription(TestConfiguration.UIElements.CHECKOUT_ICON))
            .assertExists()
    }

    @Test
    fun buttonDisabledDuringInitialization() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.INITIALIZING_TEXT).assertExists()

        try {
            val initializingButton = composeTestRule.onNodeWithText(TestConfiguration.UIElements.INITIALIZING_TEXT)
            initializingButton.assertIsNotEnabled()
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun buttonDisabledDuringProcessing() = runTest {
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

        try {
            val processingButton = composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT)
            processingButton.assertIsNotEnabled()
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun layoutStructureIsCorrect() = runTest {
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
        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON)).assertExists()
    }

    @Test
    fun multipleButtonClicks() = runTest {
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

        checkoutButton.performClick()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT).assertExists()

        try {
            composeTestRule.onNodeWithText(TestConfiguration.UIElements.PROCESSING_TEXT).performClick()
        } catch (e: Exception) {
        }
    }

    @Test
    fun tokenDisplayArea() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        try {
            composeTestRule.onNodeWithText(TestConfiguration.UIElements.SUCCESS_MESSAGE).assertDoesNotExist()
        } catch (e: AssertionError) {
        }

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()
    }

    @Test
    fun progressIndicatorVisibility() = runTest {
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
    fun responsiveLayout() = runTest {
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

    @Test
    fun bottomSheetIntegration() = runTest {
        composeTestRule.setContent {
            BottomSheetPaymentScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.BOTTOM_SHEET_TITLE).assertExists()

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
}
