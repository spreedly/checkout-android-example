package com.spreedly.example.utils

import com.spreedly.sdk.utils.CardNumberContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CvvFormValidityTest {

    @Before
    fun setUp() {
        CardNumberContext.clear()
    }

    @After
    fun tearDown() {
        CardNumberContext.clear()
    }

    @Test
    fun `should treat CVV as met when field invalid but card scheme has optional CVV`() {
        CardNumberContext.setCardScheme("6018280000000006")

        assertTrue(isCvvFormRequirementMet(cvvFieldValid = false))
    }

    @Test
    fun `should require valid CVV when card scheme requires CVV`() {
        CardNumberContext.setCardScheme("4111111111111111")

        assertFalse(isCvvFormRequirementMet(cvvFieldValid = false))
        assertTrue(isCvvFormRequirementMet(cvvFieldValid = true))
    }
}
