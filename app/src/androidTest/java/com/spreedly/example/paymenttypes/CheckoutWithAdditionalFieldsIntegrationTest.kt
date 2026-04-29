package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.TestConfiguration
import com.spreedly.example.screens.customcheckout.CheckoutWithAdditionalFieldsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for CheckoutWithAdditionalFieldsScreen component.
 * Tests the complete form including all additional address fields.
 */
@RunWith(AndroidJUnit4::class)
class CheckoutWithAdditionalFieldsIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkoutWithAdditionalFields_displaysAllRequiredFields() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CITY_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.STATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_2_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD).assertExists()
    }

    @Test
    fun checkoutWithAdditionalFields_fieldsAreInteractive() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput("4111")
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput("Test User")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_CITY)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.STATE_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_STATE)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_ADDRESS_LINE_1)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_ZIP_CODE)

        composeTestRule.waitForIdle()
    }

    @Test
    fun checkoutWithAdditionalFields_acceptsValidData() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD)
            .performTextInput("${TestConfiguration.TestData.VALID_MONTH}/${TestConfiguration.TestData.VALID_YEAR}")
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_CVV)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_NAME)

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_CITY)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.STATE_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_STATE)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_ADDRESS_LINE_1)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_2_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_ADDRESS_LINE_2)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_ZIP_CODE)

        composeTestRule.waitForIdle()
    }

    @Test
    fun checkoutWithAdditionalFields_handlesInvalidInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CITY_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.STATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_2_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD).assertExists()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput(TestConfiguration.TestData.EMPTY_NAME)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.EMPTY_CITY)

        composeTestRule.waitForIdle()
    }

    @Test
    fun checkoutWithAdditionalFields_handlesLongInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput(TestConfiguration.TestData.LONG_NAME)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.LONG_CITY)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD)
            .performTextInput(TestConfiguration.TestData.LONG_ADDRESS)

        composeTestRule.waitForIdle()
    }

    @Test
    fun checkoutWithAdditionalFields_accessibilityFeatures() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()
        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON)).assertExists()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CITY_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.STATE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_2_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD).assertExists()
    }

    @Test
    fun checkoutWithAdditionalFields_isScrollable() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .assertExists()

        composeTestRule.onRoot().performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 500)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
    }

    @Test
    fun checkoutWithAdditionalFields_formStateManagement() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput("Test User")
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_CITY)

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 300)
        }
        composeTestRule.onRoot().performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 300)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CITY_FIELD).assertExists()
    }

    @Test
    fun checkoutWithAdditionalFields_handlesSpecialCharacters() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput(TestConfiguration.TestData.SPECIAL_CHARS_NAME)

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD)
            .performTextInput("123 Main St. #A-1")
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput("New York")

        composeTestRule.waitForIdle()
    }
}
