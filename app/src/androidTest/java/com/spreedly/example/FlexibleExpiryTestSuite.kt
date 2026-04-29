package com.spreedly.example

import com.spreedly.example.paymenttypes.FlexibleExpiryScreenIntegrationTest
import com.spreedly.example.paymenttypes.FlexibleExpiryScreenUITest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for FlexibleExpiryScreen.
 * Runs both integration tests and UI tests together.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    FlexibleExpiryScreenIntegrationTest::class,
    FlexibleExpiryScreenUITest::class,
)
class FlexibleExpiryTestSuite
