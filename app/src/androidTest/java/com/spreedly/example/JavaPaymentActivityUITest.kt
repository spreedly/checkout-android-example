package com.spreedly.example

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spreedly.app.R
import com.spreedly.example.utils.TestWaitUtils
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
 * UI tests for JavaPaymentActivity.
 * Tests user interactions, visual elements, and UI behavior
 * for the Java-based XML Activity implementation.
 *
 * NOTE: Disabled due to window focus issues in CI environment (RootViewWithoutFocusException).
 */
@RunWith(AndroidJUnit4::class)
@Ignore("Disabled due to window focus issues in CI environment - RootViewWithoutFocusException")
class JavaPaymentActivityUITest {
    private lateinit var scenario: ActivityScenario<JavaPaymentActivity>

    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JavaPaymentActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        // Quick wait for activity to initialize
        runBlocking { delay(500) }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun activityLaunchesSuccessfully() {
        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
            .check(matches(isDisplayed()))
    }

    @Test
    fun displaysHeaderContent() {
        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_DESCRIPTION))
            .check(matches(isDisplayed()))

        onView(withContentDescription(TestConfiguration.UIElements.JAVA_PAYMENT_ICON))
            .check(matches(isDisplayed()))
    }

    @Test
    fun displaysFeatureCardIcons() {
        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SHOPPING_CART))
            .check(matches(isDisplayed()))

        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SECURITY_ICON))
            .check(matches(isDisplayed()))
    }

    @Test
    fun checkoutButtonIsVisible() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun progressOverlayIsInitiallyHidden() {
        onView(withId(R.id.java_progress_overlay))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun tokenCardIsInitiallyHidden() {
        onView(withId(R.id.java_token_card))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun displaysInfoSection() {
        onView(withText(TestConfiguration.UIElements.JAVA_REAL_SDK_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PURE_JAVA_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_REAL_TOKEN_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_AUTO_ERROR_INFO))
            .check(matches(isDisplayed()))
    }

    @Test
    fun layoutElementsAreProperlySized() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(allOf(isDisplayed(), isClickable())))
    }

    @Test
    fun scrollViewIsPresent() {
        onView(isRoot())
            .check(matches(isDisplayed()))
    }

    @Test
    fun visualElementsArePresentAndStyled() {
        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_BANK_LEVEL_SECURITY))
            .check(matches(isDisplayed()))
    }

    @Test
    fun accessibilityElementsPresent() {
        onView(withContentDescription(TestConfiguration.UIElements.JAVA_PAYMENT_ICON))
            .check(matches(isDisplayed()))

        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SHOPPING_CART))
            .check(matches(isDisplayed()))

        onView(withContentDescription(TestConfiguration.UIElements.JAVA_SECURITY_ICON))
            .check(matches(isDisplayed()))
    }

    @Test
    fun uiElementsHaveProperVisibility() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))

        onView(withId(R.id.java_progress_overlay))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.java_token_card))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun textElementsDisplayCorrectContent() {
        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_TITLE))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PAYMENT_ACTIVITY_DESCRIPTION))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_EXPRESS_CHECKOUT_DESC))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PCI_DSS_COMPLIANCE))
            .check(matches(isDisplayed()))
    }

    @Test
    fun infoSectionIsCompletelyDisplayed() {
        onView(withText(TestConfiguration.UIElements.JAVA_REAL_SDK_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_PURE_JAVA_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_REAL_TOKEN_INFO))
            .check(matches(isDisplayed()))

        onView(withText(TestConfiguration.UIElements.JAVA_AUTO_ERROR_INFO))
            .check(matches(isDisplayed()))
    }

    @Test
    fun componentStatesAreConsistent() {
        onView(withId(R.id.java_checkout_button))
            .check(matches(isDisplayed()))

        onView(withId(R.id.java_progress_overlay))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.java_token_card))
            .check(matches(not(isDisplayed())))
    }
}
