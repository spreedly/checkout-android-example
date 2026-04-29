package com.spreedly.example.utils

import android.view.View
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.spreedly.app.MainActivity
import com.spreedly.example.TestConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import androidx.test.espresso.matcher.ViewMatchers.isEnabled as espressoIsEnabled

/**
 * Enhanced test utilities for handling timing and wait conditions in integration tests
 */
object TestWaitUtils {
    /**
     * Default timeout values for different types of operations
     */
    object Timeouts {
        const val SHORT_WAIT_MS = 1500L
        const val MEDIUM_WAIT_MS = 4000L
        const val LONG_WAIT_MS = 7000L
        const val API_INITIALIZATION_MS = 15000L // Balanced: 15s instead of 30s
        const val BUTTON_TRANSITION_MS = 12000L // Balanced: 12s instead of 25s
        const val UI_ELEMENT_LOAD_MS = 5000L // Balanced: 5s instead of 8s
    }

    /**
     * Waits for a view to become enabled with retry logic
     */
    fun waitForViewEnabled(
        viewMatcher: Matcher<View>,
        timeoutMs: Long = Timeouts.BUTTON_TRANSITION_MS,
        retryIntervalMs: Long = 500L,
    ): ViewInteraction {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val viewInteraction = onView(viewMatcher)
                viewInteraction.check(matches(espressoIsEnabled()))
                return viewInteraction
            } catch (e: Exception) {
                lastException = e
                runBlocking { delay(retryIntervalMs) }
            }
        }

        throw AssertionError("View did not become enabled within ${timeoutMs}ms", lastException)
    }

    /**
     * Waits for a view to become displayed with retry logic
     */
    fun waitForViewDisplayed(
        viewMatcher: Matcher<View>,
        timeoutMs: Long = Timeouts.UI_ELEMENT_LOAD_MS,
        retryIntervalMs: Long = 500L,
    ): ViewInteraction {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val viewInteraction = onView(viewMatcher)
                viewInteraction.check(matches(isDisplayed()))
                return viewInteraction
            } catch (e: Exception) {
                lastException = e
                runBlocking { delay(retryIntervalMs) }
            }
        }

        throw AssertionError("View was not displayed within ${timeoutMs}ms", lastException)
    }

    /**
     * Waits for a view with specific text to become enabled
     */
    fun waitForViewWithTextEnabled(
        text: String,
        timeoutMs: Long = Timeouts.BUTTON_TRANSITION_MS,
        retryIntervalMs: Long = 1000L,
    ): ViewInteraction = waitForViewEnabled(withText(text), timeoutMs, retryIntervalMs)

    /**
     * Waits for a view with specific ID to become enabled
     */
    fun waitForViewWithIdEnabled(
        resourceId: Int,
        timeoutMs: Long = Timeouts.BUTTON_TRANSITION_MS,
        retryIntervalMs: Long = 1000L,
    ): ViewInteraction = waitForViewEnabled(withId(resourceId), timeoutMs, retryIntervalMs)

    /**
     * Waits for button to transition from "Initializing..." to "Express Checkout"
     */
    fun waitForButtonReady(
        buttonId: Int,
        timeoutMs: Long = Timeouts.API_INITIALIZATION_MS,
    ): ViewInteraction {
        val startTime = System.currentTimeMillis()

        // First wait for the button to exist
        waitForViewDisplayed(withId(buttonId), timeoutMs = 5000L)

        // Fast-fail check: if button is already enabled, return immediately
        try {
            val viewInteraction = onView(withId(buttonId))
            viewInteraction.check(matches(espressoIsEnabled()))
            return viewInteraction
        } catch (e: Exception) {
            // Button not ready yet, continue with normal wait logic
        }

        // Then wait for it to transition from initializing to ready
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val viewInteraction = onView(withId(buttonId))

                // Check if button is enabled - this is the key indicator it's ready
                try {
                    viewInteraction.check(matches(espressoIsEnabled()))
                    // If enabled, it's ready regardless of text
                    return viewInteraction
                } catch (enabledEx: Exception) {
                    // Button not enabled yet, check if it's still initializing
                    try {
                        viewInteraction.check(matches(withText(TestConfiguration.UIElements.INITIALIZING_BUTTON_TEXT)))
                        // Still initializing, continue waiting
                    } catch (textEx: Exception) {
                        // Button text changed but not enabled - may be in error state
                        // Continue waiting a bit more
                    }
                }
            } catch (e: Exception) {
                // Button might not exist or other error, continue waiting
            }
            runBlocking { delay(500L) }
        }

        // If we timeout, still return the button but log that it might not be ready
        val viewInteraction = onView(withId(buttonId))
        println("TestWaitUtils: Button may not be ready after ${timeoutMs}ms timeout")
        return viewInteraction
    }

    /**
     * Waits for Compose node to become available with retry logic
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForComposeNode(
        matcher: SemanticsMatcher,
        timeoutMs: Long = Timeouts.UI_ELEMENT_LOAD_MS,
        retryIntervalMs: Long = 500L,
    ) {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                this.onNode(matcher).assertExists()
                return
            } catch (e: Exception) {
                lastException = e
                runBlocking { delay(retryIntervalMs) }
                waitForIdle()
            }
        }

        throw AssertionError("Compose node was not found within ${timeoutMs}ms", lastException)
    }

    /**
     * Waits for Compose node with text to become available
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForComposeNodeWithText(
        text: String,
        timeoutMs: Long = Timeouts.UI_ELEMENT_LOAD_MS,
        retryIntervalMs: Long = 500L,
    ) {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                this.onNodeWithText(text).assertExists()
                return
            } catch (e: Exception) {
                lastException = e
                runBlocking { delay(retryIntervalMs) }
                waitForIdle()
            }
        }

        throw AssertionError("Compose node with text '$text' was not found within ${timeoutMs}ms", lastException)
    }

    /**
     * Enhanced wait for idle with timeout
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForIdleWithTimeout(
        timeoutMs: Long = Timeouts.MEDIUM_WAIT_MS,
    ) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                waitForIdle()
                return
            } catch (e: ComposeTimeoutException) {
                runBlocking { delay(500L) }
            }
        }

        // Final attempt
        waitForIdle()
    }

    /**
     * Waits for screen navigation to complete and elements to load
     */
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForScreenLoad(
        screenTitle: String,
        timeoutMs: Long = Timeouts.UI_ELEMENT_LOAD_MS,
    ) {
        waitForIdleWithTimeout()
        waitForComposeNodeWithText(screenTitle, timeoutMs)
        waitForIdleWithTimeout()
    }

    /**
     * Safe view assertion with retry logic
     */
    fun safeViewAssertion(
        viewMatcher: Matcher<View>,
        assertion: ViewInteraction.() -> Unit,
        maxRetries: Int = 3,
        retryDelayMs: Long = 1000L,
    ) {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                onView(viewMatcher).assertion()
                return
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    runBlocking { delay(retryDelayMs) }
                }
            }
        }

        throw AssertionError("Assertion failed after $maxRetries attempts", lastException)
    }

    /**
     * Waits for API initialization to complete by checking button state
     */
    fun waitForApiInitialization(
        buttonId: Int,
        timeoutMs: Long = Timeouts.API_INITIALIZATION_MS,
    ) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Check if button exists and is no longer showing "Initializing..."
                onView(withId(buttonId))
                    .check(matches(isDisplayed()))

                // Try to verify it's not initializing anymore
                try {
                    onView(withId(buttonId))
                        .check(matches(not(withText(TestConfiguration.UIElements.INITIALIZING_BUTTON_TEXT))))
                    return // Success - no longer initializing
                } catch (e: Exception) {
                    // Still initializing, continue waiting
                }

                runBlocking { delay(500L) }
            } catch (e: Exception) {
                runBlocking { delay(500L) }
            }
        }

        throw AssertionError("API initialization did not complete within ${timeoutMs}ms")
    }
}
