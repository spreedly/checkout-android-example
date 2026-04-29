package com.spreedly.example.utils

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.spreedly.app.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Test utilities for integration tests
 */
object TestUtils {
    /**
     * Fills out all payment form fields with valid test data
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.fillPaymentForm(
        cardNumber: String = "4111111111111111",
        month: String = "12",
        year: String = "2025",
        securityCode: String = "123",
        name: String = "John Doe",
    ) {
        waitForIdle()

        onNodeWithText("Card Number").performTextInput(cardNumber)

        onNodeWithText("Month").performTextInput(month)

        onNodeWithText("Year").performTextInput(year)

        onNodeWithText("Security Code").performTextInput(securityCode)

        onNodeWithText("Name").performTextInput(name)

        waitForIdle()
    }

    /**
     * Validates that all required form fields are displayed
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.assertAllFieldsDisplayed() {
        onNodeWithText("Card Number").assertIsDisplayed()
        onNodeWithText("Month").assertIsDisplayed()
        onNodeWithText("Year").assertIsDisplayed()
        onNodeWithText("Security Code").assertIsDisplayed()
        onNodeWithText("Name").assertIsDisplayed()
    }

    /**
     * Fills out payment form with blank name (for testing allow_blank_name parameter)
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.fillPaymentFormWithBlankName(
        cardNumber: String = "4111111111111111",
        month: String = "12",
        year: String = "2025",
        securityCode: String = "123",
    ) {
        waitForIdle()

        onNodeWithText("Card Number").performTextInput(cardNumber)
        onNodeWithText("Month").performTextInput(month)
        onNodeWithText("Year").performTextInput(year)
        onNodeWithText("Security Code").performTextInput(securityCode)
        onNodeWithText("Name").performTextInput("") // Blank name

        waitForIdle()
    }

    /**
     * Fills out payment form with expired date (for testing allow_expired_date parameter)
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.fillPaymentFormWithExpiredDate(
        cardNumber: String = "4111111111111111",
        month: String = "01",
        year: String = "2020", // Expired year
        securityCode: String = "123",
        name: String = "John Doe",
    ) {
        waitForIdle()

        onNodeWithText("Card Number").performTextInput(cardNumber)
        onNodeWithText("Month").performTextInput(month)
        onNodeWithText("Year").performTextInput(year)
        onNodeWithText("Security Code").performTextInput(securityCode)
        onNodeWithText("Name").performTextInput(name)

        waitForIdle()
    }

    /**
     * Toggles validation parameter switches
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.toggleValidationParameters(
        allowBlankName: Boolean = false,
        allowExpiredDate: Boolean = false,
    ) {
        waitForIdle()

        if (allowBlankName) {
            onNodeWithText("Allow Blank Name").onParent().performClick()
            waitForIdle()
        }

        if (allowExpiredDate) {
            onNodeWithText("Allow Expired Date").onParent().performClick()
            waitForIdle()
        }
    }

    /**
     * Validates the basic screen layout and elements
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.assertBasicScreenLayout() {
        onNodeWithText("Validation Parameters Demo").assertIsDisplayed()
        onNodeWithText("Configure validation behavior and field layout").assertIsDisplayed()

        onNodeWithText("Secure tokenized payments").assertIsDisplayed()

        onNode(hasContentDescription("Payment SDK")).assertIsDisplayed()
        onNode(hasContentDescription("Secure")).assertIsDisplayed()
    }

    /**
     * Clears all form fields
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.clearAllFields() {
        onNodeWithText("Card Number").performTextClearance()
        onNodeWithText("Month").performTextClearance()
        onNodeWithText("Year").performTextClearance()
        onNodeWithText("Security Code").performTextClearance()
        onNodeWithText("Name").performTextClearance()
    }

    /**
     * Fills all payment form fields including additional address fields
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.fillPaymentFormWithAdditionalFields(
        cardNumber: String = "4111111111111111",
        expiryDate: String = "12/2025",
        securityCode: String = "123",
        name: String = "John Doe",
        city: String = "New York",
        state: String = "NY",
        addressLine1: String = "123 Main Street",
        addressLine2: String = "Apartment 4B",
        zipCode: String = "10001",
    ) {
        waitForIdle()

        onNodeWithText("Card Number").performTextInput(cardNumber)
        onNodeWithText("Expiry Date").performTextInput(expiryDate)
        onNodeWithText("Security Code").performTextInput(securityCode)
        onNodeWithText("Name").performTextInput(name)

        onNodeWithText("City").performTextInput(city)
        onNodeWithText("State").performTextInput(state)
        onNodeWithText("Address Line 1").performTextInput(addressLine1)
        onNodeWithText("Address Line 2").performTextInput(addressLine2)
        onNodeWithText("Zip Code").performTextInput(zipCode)

        waitForIdle()
    }

    /**
     * Validates that all additional form fields are displayed
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.assertAllAdditionalFieldsDisplayed() {
        onNodeWithText("Card Number").assertIsDisplayed()
        onNodeWithText("Expiry Date").assertIsDisplayed()
        onNodeWithText("Security Code").assertIsDisplayed()
        onNodeWithText("Name").assertIsDisplayed()

        onNodeWithText("City").assertIsDisplayed()
        onNodeWithText("State").assertIsDisplayed()
        onNodeWithText("Address Line 1").assertIsDisplayed()
        onNodeWithText("Address Line 2").assertIsDisplayed()
        onNodeWithText("Zip Code").assertIsDisplayed()
    }

    /**
     * Validates the additional fields screen layout and elements
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.assertAdditionalFieldsScreenLayout() {
        onNodeWithText("Checkout with additional fields").assertIsDisplayed()
        onNodeWithText("Configure validation behavior and field layout").assertIsDisplayed()

        onNodeWithText("Secure tokenized payments").assertIsDisplayed()

        onNode(hasContentDescription("Payment SDK")).assertIsDisplayed()
        onNode(hasContentDescription("Secure")).assertIsDisplayed()
    }

    /**
     * Clears all form fields including additional fields
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.clearAllAdditionalFields() {
        onNodeWithText("Card Number").performTextClearance()
        onNodeWithText("Expiry Date").performTextClearance()
        onNodeWithText("Security Code").performTextClearance()
        onNodeWithText("Name").performTextClearance()
        onNodeWithText("City").performTextClearance()
        onNodeWithText("State").performTextClearance()
        onNodeWithText("Address Line 1").performTextClearance()
        onNodeWithText("Address Line 2").performTextClearance()
        onNodeWithText("Zip Code").performTextClearance()
    }

    /**
     * Waits for toggle switch to complete its action and UI to update
     */
    fun ComposeTestRule.waitForToggleComplete(timeoutMs: Long = 1000L) {
        runBlocking { delay(200L) }
        waitForIdle()
        runBlocking { delay(100L) }
        waitForIdle()
    }

    /**
     * Safely performs toggle switch action with proper wait
     */
    fun ComposeTestRule.performToggleWithWait(toggleText: String) {
        onNodeWithText(toggleText).onParent().performClick()
        waitForToggleComplete()
    }

    /**
     * Waits for Compose node to exist with retry logic
     */
    fun ComposeTestRule.waitForNodeWithText(
        text: String,
        timeoutMs: Long = 3000L,
        retryIntervalMs: Long = 300L,
    ) {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                onNodeWithText(text).assertExists()
                return
            } catch (e: Exception) {
                lastException = e
                runBlocking { delay(retryIntervalMs) }
                waitForIdle()
            }
        }

        throw AssertionError("Node with text '$text' was not found within ${timeoutMs}ms", lastException)
    }

    /**
     * Test data for valid payment information
     */
    object TestData {
        const val VALID_VISA_CARD = "4111111111111111"
        const val VALID_MASTERCARD = "5555555555554444"
        const val VALID_AMEX = "378282246310005"
        const val VALID_MONTH = "12"
        const val VALID_YEAR = "2025"
        const val VALID_CVV = "123"
        const val VALID_AMEX_CVV = "1234"
        const val VALID_NAME = "John Doe"

        const val INVALID_CARD = "1234567890123456"
        const val INVALID_MONTH = "13"
        const val INVALID_YEAR = "2020"
        const val INVALID_CVV = "12"
    }
}
