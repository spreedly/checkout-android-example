package com.spreedly.example

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Test configuration and utilities for integration tests
 */
object TestConfiguration {
    /**
     * Get the test context
     */
    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test environment configuration
     */
    object Environment {
        const val TEST_ENVIRONMENT_KEY = "test_env_key_12345"
        const val TEST_TOKEN = "test_token_12345"
        const val TEST_NONCE = "test_nonce_12345"
        const val TEST_TIMESTAMP = "1234567890"
        const val TEST_CERTIFICATE_TOKEN = "test_cert_token_12345"
        const val TEST_SIGNATURE = "test_signature_12345"
    }

    /**
     * Test timeouts and timing
     */
    object Timing {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val ANIMATION_TIMEOUT_MS = 1000L
        const val NETWORK_TIMEOUT_MS = 10000L
    }

    /**
     * Test data constants
     */
    object TestData {
        const val VALID_VISA_CARD = "4111111111111111"
        const val VALID_MASTERCARD = "5555555555554444"
        const val VALID_AMEX = "378282246310005"
        const val VALID_DISCOVER = "6011111111111117"

        const val VALID_MONTH = "12"
        const val VALID_YEAR = "2025"
        const val VALID_CVV = "123"
        const val VALID_AMEX_CVV = "1234"
        const val VALID_NAME = "John Doe"

        const val INVALID_CARD = "123456" // Short invalid card number to avoid text formatting conflicts
        const val INVALID_MONTH_HIGH = "13"
        const val INVALID_MONTH_LOW = "00"
        const val INVALID_YEAR_PAST = "2020"
        const val INVALID_CVV_SHORT = "12"
        const val EMPTY_NAME = ""
        const val BLANK_NAME = "   " // Just spaces

        const val LONG_NAME = "Jean-Baptiste Emmanuel Zorg de la Fontaine Smith-Johnson III"
        const val SPECIAL_CHARS_NAME = "José María García-Hernández"

        // Expired date test data
        const val EXPIRED_MONTH = "01"
        const val EXPIRED_YEAR = "2020"
        const val EXPIRED_EXPIRY_DATE = "01/20"
        const val MIN_YEAR = "2024"
        const val MAX_YEAR = "2040"

        const val VALID_CITY = "New York"
        const val VALID_STATE = "NY"
        const val VALID_ADDRESS_LINE_1 = "123 Main Street"
        const val VALID_ADDRESS_LINE_2 = "Apartment 4B"
        const val VALID_ZIP_CODE = "10001"

        const val EMPTY_CITY = ""
        const val LONG_CITY = "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch"
        const val INVALID_STATE = "ZZ"
        const val LONG_ADDRESS =
            "This is an extremely long address line that exceeds the normal character limit for most address fields"
        const val INVALID_ZIP_SHORT = "123"
        const val INVALID_ZIP_LONG = "1234567890"
        const val INVALID_ZIP_ALPHA = "ABCDE"
    }

    /**
     * UI test selectors and identifiers
     */
    object UIElements {
        const val CARD_NUMBER_FIELD = "Card Number"
        const val MONTH_FIELD = "Month"
        const val YEAR_FIELD = "Year" // Basic checkout screen uses simple "Year"
        const val YEAR_YYYY_FIELD = "Year (YYYY)" // FlexibleExpiry screen uses this when not in 2-digit mode
        const val YEAR_YY_FIELD = "Year (YY)" // FlexibleExpiry screen uses this in 2-digit mode
        const val MONTH_MM_FIELD = "Month (MM)"
        const val SECURITY_CODE_FIELD = "Security Code"
        const val NAME_FIELD = "Name"

        // BasicCheckoutScreen specific fields
        const val BASIC_EXPIRY_FIELD = "MM/YY"
        const val BASIC_CVV_FIELD = "CVV"
        const val BASIC_FULL_NAME_FIELD = "Full Name"
        const val BASIC_EMAIL_FIELD = "Email"

        const val EXPIRY_DATE_FIELD = "Expiry Date"
        const val CITY_FIELD = "City"
        const val STATE_FIELD = "State"
        const val ADDRESS_LINE_1_FIELD = "Address Line 1"
        const val ADDRESS_LINE_2_FIELD = "Address Line 2"
        const val ZIP_CODE_FIELD = "Zip Code"

        const val EXPIRY_DATE_MM_YY_FIELD = "Expiry Date (MM/YY)"
        const val SEPARATE_FIELDS_TEXT = "Separate Month/Year Fields"
        const val COMBINED_FIELD_TEXT = "Combined Expiry Date Field"
        const val ALLOW_BLANK_NAME_TEXT = "Allow Blank Name"
        const val ALLOW_EXPIRED_DATE_TEXT = "Allow Expired Date"
        const val FLEXIBLE_EXPIRY_SECURITY_TEXT = "Validation parameters and field configuration demo"

        // TraditionalActivity specific elements
        const val TRADITIONAL_ACTIVITY_TITLE = "Payment SDK Demo"
        const val TRADITIONAL_ACTIVITY_DESCRIPTION = "Test your payment integration"
        const val EXPRESS_CHECKOUT_BUTTON = "Express Checkout"
        const val INITIALIZING_BUTTON_TEXT = "Initializing..."
        const val PROCESSING_BUTTON_TEXT = "Processing..."
        const val TOKEN_GENERATED_MESSAGE = "Payment Token Generated"

        const val SCREEN_TITLE = "Validation Parameters Demo"
        const val BASIC_CHECKOUT_TITLE = "Basic Checkout with Custom Fields"
        const val ADDITIONAL_FIELDS_TITLE = "Checkout with additional fields"
        const val BOTTOM_SHEET_TITLE = "Payment SDK Demo"
        const val FLEXIBLE_EXPIRY_TITLE = "Validation Parameters Demo"
        const val SCREEN_DESCRIPTION = "Configure validation behavior and field layout"
        const val BASIC_CHECKOUT_DESCRIPTION = "SPL fields for sensitive data, custom fields with custom button"
        const val BOTTOM_SHEET_DESCRIPTION = "Test your payment integration"
        const val FLEXIBLE_EXPIRY_DESCRIPTION = "Configure validation behavior and field layout"
        const val SECURITY_TEXT = "Secure tokenized payments"

        const val PAYMENT_SDK_ICON = "Payment SDK"
        const val SECURITY_ICON = "Secure"
        // FlexibleExpiryScreen doesn't have Secure icon, only BasicCheckoutScreen does

        const val SUCCESS_MESSAGE = "Payment Token Generated"
        const val CANCEL_MESSAGE = "Payment canceled"
        const val ERROR_MESSAGE = "Error creating payment method"
        const val INIT_SUCCESS_MESSAGE = "Payment method created successfully!"

        const val INITIALIZING_TEXT = "Initializing..."
        const val PROCESSING_TEXT = "Processing..."
        const val CHECKOUT_ICON = "Checkout"
        const val SUCCESS_ICON = "Success"
        const val PAYMENT_FAILED_MESSAGE = "Payment failed"
        const val PAYMENT_COMPLETED_MESSAGE = "Payment completed successfully!"

        const val JAVA_PAYMENT_ACTIVITY_TITLE = "Java Payment Activity"
        const val JAVA_PAYMENT_ACTIVITY_DESCRIPTION = "Secure checkout with Spreedly SDK"
        const val JAVA_EXPRESS_CHECKOUT_HEADER = "Express Checkout"
        const val JAVA_EXPRESS_CHECKOUT_DESC = "Quick and secure payment processing"
        const val JAVA_BANK_LEVEL_SECURITY = "Bank-Level Security"
        const val JAVA_PCI_DSS_COMPLIANCE = "PCI DSS compliant payment processing"
        const val JAVA_PAYMENT_TOKEN = "Payment Token"
        const val JAVA_REAL_SDK_INFO = "• Real SDK with Auto-Closing Bottom Sheet"
        const val JAVA_PURE_JAVA_INFO = "• Pure Java Implementation with XML"
        const val JAVA_REAL_TOKEN_INFO = "• Real Token Display on Success"
        const val JAVA_AUTO_ERROR_INFO = "• Automatic Error & Cancel Handling"
        const val JAVA_PAYMENT_ICON = "Payment Icon"
        const val JAVA_SHOPPING_CART = "Shopping Cart"
        const val JAVA_SECURITY_ICON = "Security"
        const val JAVA_SUCCESS_ICON = "Success"
    }

    /**
     * Expected payment results for testing
     */
    object PaymentResults {
        const val SUCCESS_TOKEN_PREFIX = "pm_test_"
        const val MOCK_SUCCESS_TOKEN = "pm_test_token_12345"
        const val MOCK_ERROR_MESSAGE = "Payment processing failed"
        const val MOCK_INIT_ERROR = "SDK initialization failed"
    }
}
