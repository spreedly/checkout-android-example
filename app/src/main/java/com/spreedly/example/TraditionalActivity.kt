package com.spreedly.example

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.spreedly.app.R
import com.spreedly.example.viewmodel.ConfigurationChangeAwareViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.spreedly.example.qa.ExpressDisplayConfigBar
import com.spreedly.paymentsheet.SpreedlyBottomSheet
import com.spreedly.sdk.ui.CardNumberFormat
import com.spreedly.sdk.ui.PaymentSheetDisplayConfig

import kotlinx.coroutines.launch

class TraditionalActivity : AppCompatActivity() {
    private lateinit var checkoutButton: Button
    private var progressOverlay: LinearLayout? = null
    private var progressBar: ProgressBar? = null
    private var buttonText: TextView? = null
    private lateinit var tokenCard: CardView
    private lateinit var tokenText: TextView
    private lateinit var composeBottomSheet: ComposeView

    // Use ViewModel to survive configuration changes
    private lateinit var viewModel: ConfigurationChangeAwareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_traditional_payment)

        // Initialize ViewModel with factory that provides context
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    Log.d(TAG, "create viewmodel")
                    return ConfigurationChangeAwareViewModel(applicationContext) as T
                }
            },
        )[ConfigurationChangeAwareViewModel::class.java]

        initializeViews()
        setupClickListeners()
        setupComposeBottomSheet()
        observeViewModelState()

        // Configuration change completed
        viewModel.onConfigurationChangeComplete()

        Log.d(TAG, "Activity created - ViewModel state preserved")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save whether bottom sheet is currently visible
        val isBottomSheetVisible = viewModel.sdk.showBottomSheet.value
        outState.putBoolean("was_bottom_sheet_visible", isBottomSheetVisible)

        if (isBottomSheetVisible) {
            Log.d(TAG, "Bottom sheet is visible - will preserve state")
            // Tell the ViewModel to preserve state for the next show
            viewModel.onConfigurationChanging()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changing - preserving state")

        // Notify ViewModel that configuration is changing
        if (viewModel.sdk.showBottomSheet.value) {
            viewModel.onConfigurationChanging()
        }
    }

    private fun initializeViews() {
        checkoutButton = findViewById(R.id.checkout_button)
        progressOverlay = findViewById(R.id.progress_overlay)
        progressBar = findViewById(R.id.progress_bar)
        buttonText = findViewById(R.id.button_text)
        tokenCard = findViewById(R.id.token_card)
        tokenText = findViewById(R.id.token_text)
        composeBottomSheet = findViewById(R.id.compose_bottom_sheet)

        // Initially hide progress and token
        progressOverlay?.visibility = View.GONE
        tokenCard.visibility = View.GONE
    }

    private fun setupClickListeners() {
        checkoutButton.setOnClickListener {
            val isProcessing = viewModel.isProcessing.value
            if (!isProcessing) {
                startCheckout()
            }
        }
    }

    private fun setupComposeBottomSheet() {
        composeBottomSheet.setContent {
            // Observe ViewModel state in Compose
            val isInitializing by viewModel.isInitializing.collectAsState()

            if (!isInitializing) {
                var sheetEnableAutofill by remember { mutableStateOf(true) }
                var sheetUseMaskedFormat by remember { mutableStateOf(false) }
                val displayConfig =
                    PaymentSheetDisplayConfig(
                        enableAutofill = sheetEnableAutofill,
                        cardNumberFormat =
                            if (sheetUseMaskedFormat) {
                                CardNumberFormat.MASKED
                            } else {
                                CardNumberFormat.PRETTY
                            },
                    )
                androidx.compose.foundation.layout.Column {
                    ExpressDisplayConfigBar(
                        enableAutofill = sheetEnableAutofill,
                        onEnableAutofillChange = { sheetEnableAutofill = it },
                        useMaskedFormat = sheetUseMaskedFormat,
                        onUseMaskedFormatChange = { sheetUseMaskedFormat = it },
                    )
                    SpreedlyBottomSheet(
                        sdk = viewModel.sdk,
                        displayConfig = displayConfig,
                    )
                }
            }
        }
    }

    private fun observeViewModelState() {
        // Observe initialization state
        lifecycleScope.launch {
            viewModel.isInitializing.collect { isInitializing ->
                updateButtonState(isInitializing, viewModel.isProcessing.value)

            }
        }

        // Observe processing state
        lifecycleScope.launch {
            viewModel.isProcessing.collect { isProcessing ->
                updateButtonState(viewModel.isInitializing.value, isProcessing)

                if (isProcessing) {
                    progressOverlay?.visibility = View.VISIBLE
                    tokenCard.visibility = View.GONE
                } else {
                    progressOverlay?.visibility = View.GONE
                }
            }
        }

        // Observe payment token from ViewModel (primary)
        lifecycleScope.launch {
            Log.d(TAG, "Starting to observe payment token...")
            viewModel.paymentToken.collect { token ->
                if (token.isNotEmpty()) {
                    tokenText.text = token
                    tokenCard.visibility = View.VISIBLE
                    Toast
                        .makeText(this@TraditionalActivity, "Payment completed successfully!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(TAG, "Token is empty, hiding token card")
                    tokenCard.visibility = View.GONE
                }
            }
        }
    }

    private fun updateButtonState(isInitializing: Boolean, isProcessing: Boolean) {
        val isButtonDisabled = isInitializing || isProcessing
        checkoutButton.isEnabled = !isButtonDisabled
        checkoutButton.text = when {
            isInitializing -> "Initializing..."
            isProcessing -> "Processing..."
            else -> "Express Checkout"
        }
    }

    private fun startCheckout() {
        viewModel.setProcessing(true)
        viewModel.clearToken() // Clear any previous token

        // Call actual SDK express checkout
        try {
            viewModel.sdk.expressCheckout()
            Log.d(
                TAG,
                "Express checkout initiated - form data will be preserved during orientation changes!",
            )

            // Start payment polling as backup
            viewModel.startPaymentPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Express checkout failed: ${e::class.simpleName}")
            viewModel.setProcessing(false)
            Toast.makeText(this, "Failed to start checkout", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
    }

    private companion object {
        private const val TAG = "TraditionalActivity"
    }
}
