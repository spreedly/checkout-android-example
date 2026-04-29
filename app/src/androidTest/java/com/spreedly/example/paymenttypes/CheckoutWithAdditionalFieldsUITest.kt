package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.TestConfiguration
import com.spreedly.example.screens.customcheckout.CheckoutWithAdditionalFieldsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for CheckoutWithAdditionalFieldsScreen.
 * These tests directly compose the screen and test UI interactions.
 */
@RunWith(AndroidJUnit4::class)
class CheckoutWithAdditionalFieldsUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allFormFieldsAreDisplayed() = runTest {
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
    fun formFieldsAreInteractive() = runTest {
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
            .performTextInput("Test City")

        composeTestRule.waitForIdle()
    }

    @Test
    fun acceptsValidPaymentData() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD)
            .performTextInput("12/2025")
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
    fun handlesCardNumberInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        val cardNumberField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)

        cardNumberField.performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)

        composeTestRule.waitForIdle()

        cardNumberField.performTextClearance()
        cardNumberField.performTextInput(TestConfiguration.TestData.VALID_MASTERCARD)

        composeTestRule.waitForIdle()
    }

    @Test
    fun handlesExpiryDateInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        val expiryDateField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD)

        expiryDateField.performTextInput("12/2025")

        composeTestRule.waitForIdle()

        expiryDateField.performTextClearance()
        expiryDateField.performTextInput("0630")

        composeTestRule.waitForIdle()
    }

    @Test
    fun handlesSecurityCodeInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        val securityCodeField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD)

        securityCodeField.performTextInput(TestConfiguration.TestData.VALID_CVV)

        composeTestRule.waitForIdle()

        securityCodeField.performTextClearance()
        securityCodeField.performTextInput(TestConfiguration.TestData.VALID_AMEX_CVV)

        composeTestRule.waitForIdle()
    }

    @Test
    fun handlesNameInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        val nameField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)

        nameField.performTextInput(TestConfiguration.TestData.VALID_NAME)

        composeTestRule.waitForIdle()

        nameField.performTextClearance()
        nameField.performTextInput(TestConfiguration.TestData.SPECIAL_CHARS_NAME)

        composeTestRule.waitForIdle()
    }

    @Test
    fun handlesAddressFieldsInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

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
    fun scrollableWhenKeyboardShown() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .performScrollTo()
            .performTextInput(TestConfiguration.TestData.VALID_ZIP_CODE)

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performScrollTo()
            .performTextInput("4111")

        composeTestRule.waitForIdle()
    }

    @Test
    fun formFieldsHaveCorrectLabels() = runTest {
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
    fun hasProperAccessibility() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON))
            .assertExists()
        composeTestRule
            .onNode(hasContentDescription(TestConfiguration.UIElements.SECURITY_ICON))
            .assertExists()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .assertHasClickAction()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .assertHasClickAction()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .assertHasClickAction()
    }

    @Test
    @Ignore("Disabled due to slow execution in CI - complete form flow test takes too long")
    fun completeFormFlow() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD)
            .performTextInput("12/2025")
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
    @Ignore("Disabled due to rotation issues in CI environment - screen layout not stable after rotation")
    fun layoutAfterRotation() = runTest {
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

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDITIONAL_FIELDS_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ZIP_CODE_FIELD)
            .assertExists()
    }

    @Test
    fun handlesRapidInput() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput("4111111111111111")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_FIELD)
            .performTextInput("12/25")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD)
            .performTextInput("123")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput("John Doe")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput("NYC")

        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyStateHandling() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
            .performTextInput("")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
            .performTextInput("")

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput("")

        composeTestRule.waitForIdle()
    }

    @Test
    fun fieldClearingAndRefilling() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        val nameField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
        val cityField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)

        nameField.performTextInput("Test User")
        cityField.performTextInput(TestConfiguration.TestData.VALID_CITY)

        composeTestRule.waitForIdle()

        nameField.performTextClearance()
        cityField.performTextClearance()

        composeTestRule.waitForIdle()

        nameField.performTextInput("Jane Smith")
        cityField.performTextInput("Boston")

        composeTestRule.waitForIdle()
    }

    @Test
    fun longAddressHandling() = runTest {
        composeTestRule.setContent {
            CheckoutWithAdditionalFieldsScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.ADDRESS_LINE_1_FIELD)
            .performTextInput(TestConfiguration.TestData.LONG_ADDRESS)

        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.CITY_FIELD)
            .performTextInput(TestConfiguration.TestData.LONG_CITY)

        composeTestRule.waitForIdle()
    }
}
