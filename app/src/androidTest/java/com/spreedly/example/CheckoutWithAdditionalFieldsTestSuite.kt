package com.spreedly.example

import com.spreedly.example.paymenttypes.CheckoutWithAdditionalFieldsIntegrationTest
import com.spreedly.example.paymenttypes.CheckoutWithAdditionalFieldsUITest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for CheckoutWithAdditionalFieldsScreen.
 * Runs both integration tests and UI tests together.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CheckoutWithAdditionalFieldsIntegrationTest::class,
    CheckoutWithAdditionalFieldsUITest::class,
)
class CheckoutWithAdditionalFieldsTestSuite
