package com.spreedly.example.screens.bankaccount

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.spreedly.sdk.ui.CustomFieldsConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BankAccountViewModelThemeTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `useCustomTheme defaults to false with default uiConfig`() = runTest {
        val viewModel = BankAccountViewModel(mockContext)
        advanceUntilIdle()

        assertFalse(viewModel.useCustomTheme.value)
        assertEquals(CustomFieldsConfig(), viewModel.uiConfig.value)
    }

    @Test
    fun `useCustomTheme true with swatch selection updates uiConfig primaryColor`() = runTest {
        val viewModel = BankAccountViewModel(mockContext)
        advanceUntilIdle()

        viewModel.updateUseCustomTheme(true)
        viewModel.updateUiConfig(CustomFieldsConfig(primaryColor = Color.Red))

        assertEquals(Color.Red, viewModel.uiConfig.value.primaryColor)
        assertNotEquals(Color.Unspecified, viewModel.uiConfig.value.primaryColor)
    }

    @Test
    fun `toggle off after customization resets uiConfig to defaults`() = runTest {
        val viewModel = BankAccountViewModel(mockContext)
        advanceUntilIdle()

        viewModel.updateUseCustomTheme(true)
        viewModel.updateUiConfig(CustomFieldsConfig(primaryColor = Color.Red))
        viewModel.updateUseCustomTheme(false)

        assertFalse(viewModel.useCustomTheme.value)
        assertEquals(CustomFieldsConfig(), viewModel.uiConfig.value)
    }
}
