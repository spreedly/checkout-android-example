package com.spreedly.example

/**
 * Fast test configuration with reduced timeouts for quicker feedback
 * Use this for development and CI optimization
 */
object FastTestConfiguration {
    /**
     * Optimized timeout values for faster test execution
     */
    object Timeouts {
        const val SHORT_WAIT_MS = 500L
        const val MEDIUM_WAIT_MS = 2000L
        const val LONG_WAIT_MS = 3000L
        const val API_INITIALIZATION_MS = 8000L // Reduced from 12s to 8s
        const val BUTTON_TRANSITION_MS = 6000L // Reduced from 10s to 6s
        const val UI_ELEMENT_LOAD_MS = 2000L // Reduced from 4s to 2s
        const val RETRY_INTERVAL_MS = 250L // Faster retry checks
    }

    /**
     * Test data optimized for speed (shorter strings, simpler validation)
     */
    object TestData {
        const val FAST_VISA_CARD = "4111111111111111"
        const val FAST_MONTH = "12"
        const val FAST_YEAR = "2025"
        const val FAST_CVV = "123"
        const val FAST_NAME = "Test User"
    }

    /**
     * Critical UI elements that must be tested
     */
    object CriticalTests {
        val BUTTON_TESTS = listOf(
            "buttonTransitionsToReadyState",
            "buttonClickTriggersProcessingVisuals",
        )

        val COMPOSE_TESTS = listOf(
            "fieldValidationInCombinedMode",
            "formFieldLabelsCombinedMode",
        )

        val ACCESSIBILITY_TESTS = listOf(
            "checkoutWithAdditionalFields_meetsAccessibilityStandards",
        )
    }
}
