package com.spreedly.example

import com.spreedly.example.paymenttypes.BottomSheetPaymentScreenIntegrationTest
import com.spreedly.example.paymenttypes.BottomSheetPaymentScreenUITest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for BottomSheetPaymentScreen.
 * Runs both integration tests and UI tests together.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    BottomSheetPaymentScreenIntegrationTest::class,
    BottomSheetPaymentScreenUITest::class,
)
class BottomSheetPaymentTestSuite
