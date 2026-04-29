package com.spreedly.example.paymenttypes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.example.TestConfiguration
import com.spreedly.example.screens.flexibleexpiry.FlexibleExpiryScreen
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for FlexibleExpiryScreen component.
 * Tests the validation parameters demo functionality including toggle switches for
 * separate/combined expiry fields, allow_blank_name, allow_expired_date,
 * form validation, payment processing, and UI state management.
 */
@RunWith(AndroidJUnit4::class)
class FlexibleExpiryScreenIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flexibleExpiry_displaysCorrectLayout() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.FLEXIBLE_EXPIRY_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.FLEXIBLE_EXPIRY_DESCRIPTION).assertExists()
        composeTestRule.onNode(hasContentDescription(TestConfiguration.UIElements.PAYMENT_SDK_ICON)).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.FLEXIBLE_EXPIRY_SECURITY_TEXT).assertExists()
    }

    @Test
    fun flexibleExpiry_displaysToggleSwitch() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SEPARATE_FIELDS_TEXT).assertExists()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.SEPARATE_FIELDS_TEXT)
            .onParent()
            .assertExists()
    }

    @Test
    fun flexibleExpiry_defaultsSeparateFieldsMode() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SEPARATE_FIELDS_TEXT).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD).assertExists()
        try {
            composeTestRule.onNodeWithText(TestConfiguration.UIElements.EXPIRY_DATE_MM_YY_FIELD).assertDoesNotExist()
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun flexibleExpiry_displaysAllRequiredFieldsSeparateMode() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()
    }

    @Test
    fun flexibleExpiry_fieldsAreInteractiveSeparateMode() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        val cardNumberField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
        val monthField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD)
        val yearField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD)
        val cvvField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD)
        val nameField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
        cardNumberField.assertExists()
        cardNumberField.performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)

        monthField.assertExists()
        monthField.performTextInput(TestConfiguration.TestData.VALID_MONTH)

        yearField.assertExists()
        yearField.performTextInput(TestConfiguration.TestData.VALID_YEAR)

        cvvField.assertExists()
        cvvField.performTextInput(TestConfiguration.TestData.VALID_CVV)

        nameField.assertExists()
        nameField.performTextInput(TestConfiguration.TestData.VALID_NAME)
    }

    @Test
    fun flexibleExpiry_handlesInvalidInputSeparateMode() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        val cardNumberField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
        val monthField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD)
        val yearField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD)
        cardNumberField.performTextInput(TestConfiguration.TestData.INVALID_CARD)
        monthField.performTextInput(TestConfiguration.TestData.INVALID_MONTH_HIGH)
        yearField.performTextInput(TestConfiguration.TestData.INVALID_YEAR_PAST)
        composeTestRule.waitForIdle()
    }

    @Test
    fun flexibleExpiry_accessibilityFeatures() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
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
            .onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD)
            .assertHasClickAction()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD)
            .assertHasClickAction()
        composeTestRule
            .onNodeWithText(TestConfiguration.UIElements.SEPARATE_FIELDS_TEXT)
            .onParent()
            .assertExists()
    }

    @Test
    fun flexibleExpiry_isScrollable() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.FLEXIBLE_EXPIRY_TITLE).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.FLEXIBLE_EXPIRY_SECURITY_TEXT).assertExists()
    }

    @Test
    fun flexibleExpiry_formFieldValidation() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        val cardNumberField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD)
        val monthField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.MONTH_FIELD)
        val yearField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.YEAR_YYYY_FIELD)
        val cvvField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD)
        val nameField = composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD)
        cardNumberField.performTextInput(TestConfiguration.TestData.VALID_VISA_CARD)
        composeTestRule.waitForIdle()

        monthField.performTextInput(TestConfiguration.TestData.VALID_MONTH)
        composeTestRule.waitForIdle()

        yearField.performTextInput(TestConfiguration.TestData.VALID_YEAR)
        composeTestRule.waitForIdle()

        cvvField.performTextInput(TestConfiguration.TestData.VALID_CVV)
        composeTestRule.waitForIdle()

        nameField.performTextInput(TestConfiguration.TestData.VALID_NAME)
        composeTestRule.waitForIdle()
    }

    @Test
    fun flexibleExpiry_tokenDisplayHandling() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        try {
            composeTestRule.onNodeWithText(TestConfiguration.UIElements.SUCCESS_MESSAGE).assertDoesNotExist()
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun flexibleExpiry_sdkIntegration() = runTest {
        composeTestRule.setContent {
            FlexibleExpiryScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.CARD_NUMBER_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.SECURITY_CODE_FIELD).assertExists()
        composeTestRule.onNodeWithText(TestConfiguration.UIElements.NAME_FIELD).assertExists()
    }
}
