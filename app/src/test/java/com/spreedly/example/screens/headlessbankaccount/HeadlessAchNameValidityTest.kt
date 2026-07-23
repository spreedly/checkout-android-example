package com.spreedly.example.screens.headlessbankaccount

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadlessAchNameValidityTest {

    @Test
    fun `headlessAchNameValidity allows blank single name when allowBlankName true`() {
        val result = headlessAchNameValidity(
            accountHolderName = "",
            firstName = "",
            lastName = "",
            bankName = "",
            showBankName = false,
            bankNameRequired = false,
            allowBlankName = true,
        )
        assertTrue(result.fullNameIsValid)
    }

    @Test
    fun `headlessAchNameValidity allows blank separate pair when allowBlankName true`() {
        val result = headlessAchNameValidity(
            accountHolderName = "",
            firstName = "",
            lastName = "",
            bankName = "",
            showBankName = false,
            bankNameRequired = false,
            allowBlankName = true,
        )
        assertTrue(result.firstNameIsValid)
        assertTrue(result.lastNameIsValid)
    }

    @Test
    fun `headlessAchNameValidity rejects blank when allowBlankName false`() {
        val result = headlessAchNameValidity(
            accountHolderName = "",
            firstName = "",
            lastName = "",
            bankName = "",
            showBankName = false,
            bankNameRequired = false,
            allowBlankName = false,
        )
        assertFalse(result.fullNameIsValid)
        assertFalse(result.firstNameIsValid)
        assertFalse(result.lastNameIsValid)
    }

    @Test
    fun `required bank name stays invalid when blank regardless of allowBlankName`() {
        val whenAllowBlank = headlessAchNameValidity(
            accountHolderName = "Jon Doe",
            firstName = "Jon",
            lastName = "Doe",
            bankName = "",
            showBankName = true,
            bankNameRequired = true,
            allowBlankName = true,
        )
        val whenDisallowBlank = headlessAchNameValidity(
            accountHolderName = "Jon Doe",
            firstName = "Jon",
            lastName = "Doe",
            bankName = "",
            showBankName = true,
            bankNameRequired = true,
            allowBlankName = false,
        )
        assertFalse(whenAllowBlank.bankNameIsValid)
        assertFalse(whenDisallowBlank.bankNameIsValid)
    }
}
