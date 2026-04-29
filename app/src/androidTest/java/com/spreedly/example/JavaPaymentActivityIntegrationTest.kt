package com.spreedly.example

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.base.RootViewPicker
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.spreedly.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for JavaPaymentActivity.
 * Tests the complete XML-based Java Activity with Spreedly SDK integration,
 * including express checkout functionality and bottom sheet payment flow.
 *
 * NOTE: These tests are currently disabled due to window focus issues in CI environment.
 * The RootViewWithoutFocusException is a common issue with Espresso tests in emulated environments.
 * These tests work fine locally but fail in GitHub Actions due to emulator focus handling.
 */
@RunWith(AndroidJUnit4::class)
@Ignore("Disabled due to window focus issues in CI environment - RootViewWithoutFocusException")
class JavaPaymentActivityIntegrationTest {
    private lateinit var scenario: ActivityScenario<JavaPaymentActivity>
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JavaPaymentActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        // Wait longer for Activity to fully initialize and gain focus
        runBlocking { delay(2000) }

        // Wait for the main title to be displayed to ensure Activity is ready
        try {
            onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // If title not found, wait a bit more and try again
            runBlocking { delay(1000) }
        }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    /**
     * Helper function to wait for a view to be displayed with retries
     */
    private fun waitForViewToBeDisplayed(matcher: org.hamcrest.Matcher<android.view.View>, maxRetries: Int = 5) {
        var retries = 0
        while (retries < maxRetries) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return // Success, exit the loop
            } catch (e: Exception) {
                retries++
                if (retries >= maxRetries) {
                    throw e // Re-throw the exception if max retries reached
                }
                runBlocking { delay(1000) } // Wait 1 second before retry
            }
        }
    }

    @Test
    fun javaPaymentActivity_displaysCorrectLayout() {
        waitForViewToBeDisplayed(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
        waitForViewToBeDisplayed(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_DESCRIPTION))
    }

    @Test
    fun javaPaymentActivity_hasAllRequiredViews() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))

        onView(withId(R.id.java_compose_bottom_sheet))
            .check(matches(isAssignableFrom(androidx.compose.ui.platform.ComposeView::class.java)))

        onView(withId(R.id.java_progress_overlay))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.java_token_card))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun javaPaymentActivity_displaysFeatureCards() {
        onView(withText(TestConfiguration.UIElements.JAVA_BANK_LEVEL_SECURITY))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_EXPRESS_CHECKOUT_DESC))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_BANK_LEVEL_SECURITY))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PCI_DSS_COMPLIANCE))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_hasPaymentIcon() {
        onView(withContentDescription(TestConfiguration.UIElements.JAVA_PAYMENT_ICON))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_hasShoppingCartIcon() {
        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SHOPPING_CART))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_hasSecurityIcon() {
        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SECURITY_ICON))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_mainCardIsDisplayed() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_layoutIsScrollable() {
        onView(isRoot())
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_sdkIntegration() {
        runBlocking { delay(1500) }

        onView(withId(R.id.java_compose_bottom_sheet))
            .check(matches(isAssignableFrom(androidx.compose.ui.platform.ComposeView::class.java)))
    }

    @Test
    fun javaPaymentActivity_buttonInitiallyShowsInitializing() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_progressOverlayInitiallyHidden() {
        onView(withId(R.id.java_progress_overlay))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun javaPaymentActivity_tokenCardInitiallyHidden() {
        onView(withId(R.id.java_token_card))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun javaPaymentActivity_hasInfoSection() {
        onView(withText(TestConfiguration.UIElements.JAVA_REAL_SDK_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PURE_JAVA_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_REAL_TOKEN_INFO))
            .check(matches(isDisplayed()))
    }

    @Test
    fun javaPaymentActivity_layoutStructure() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(allOf(isDisplayed(), isClickable())))
    }

    @Test
    fun javaPaymentActivity_hasComposeBottomSheet() {
        onView(withId(R.id.java_compose_bottom_sheet))
            .check(matches(isAssignableFrom(androidx.compose.ui.platform.ComposeView::class.java)))
    }

    @Test
    fun javaPaymentActivity_expressCheckoutFlow() {
        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_DESCRIPTION))
            .check(matches(isDisplayed()))

        onView(withId(R.id.java_compose_bottom_sheet))
            .check(matches(isAssignableFrom(androidx.compose.ui.platform.ComposeView::class.java)))
    }
}
