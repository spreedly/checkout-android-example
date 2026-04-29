package com.spreedly.example

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for TraditionalActivity.
 * Combines integration tests and UI tests to provide complete coverage
 * of the traditional XML-based Android Activity with Spreedly SDK integration.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    TraditionalActivityIntegrationTest::class,
    TraditionalActivityUITest::class,
)
class TraditionalActivityTestSuite
