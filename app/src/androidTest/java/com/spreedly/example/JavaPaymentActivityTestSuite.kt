package com.spreedly.example

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for JavaPaymentActivity.
 * Combines integration tests and UI tests to provide complete coverage
 * of the Java-based XML Android Activity with Spreedly SDK integration.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    JavaPaymentActivityIntegrationTest::class,
    JavaPaymentActivityUITest::class,
)
class JavaPaymentActivityTestSuite
