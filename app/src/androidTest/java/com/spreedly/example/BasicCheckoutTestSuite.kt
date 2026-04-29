package com.spreedly.example

import com.spreedly.example.paymenttypes.BasicCheckoutScreenIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for BasicCheckoutScreen integration tests
 *
 * This suite includes both UI interaction tests and integration tests
 * that cover the complete functionality of the BasicCheckoutScreen.
 *
 * Run this suite to execute all BasicCheckoutScreen tests at once:
 * ./gradlew app:connectedAndroidTest --tests "com.spreedly.example.BasicCheckoutTestSuite"
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    BasicCheckoutScreenIntegrationTest::class,
)
class BasicCheckoutTestSuite
