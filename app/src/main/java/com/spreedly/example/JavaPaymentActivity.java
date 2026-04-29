package com.spreedly.example;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.compose.ui.platform.ComposeView;

import com.spreedly.app.R;
import com.spreedly.example.models.SavedPaymentMethod;
import com.spreedly.example.repository.PaymentMethodRepository;
import com.spreedly.sdk.ui.PaymentSheetConfig;
import com.spreedly.sdk.Spreedly;
import com.spreedly.sdk.models.ScreenPresentationMode;
import com.spreedly.paymentsheet.PaymentSheetJavaHelper;
import com.spreedly.paymentsheet.recache.RecacheJavaHelper;

import java.util.List;

public class JavaPaymentActivity extends AppCompatActivity {

    private static final String TAG = "JavaPaymentActivity";

    private Button checkoutButton;
    private LinearLayout progressOverlay;
    private ProgressBar progressBar;
    private TextView buttonText;
    private CardView tokenCard;
    private TextView tokenText;
    private ComposeView composeBottomSheet;
    private ComposeView recacheComposeView;
    private ComposeView savedCardsComposeView;
    
    // Configuration change helper
    private ConfigurationChangeHelper configHelper;
    private Spreedly sdk;
    private PaymentMethodRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_payment);

        // Initialize configuration change helper
        configHelper = new ConfigurationChangeHelper(this);
        sdk = configHelper.getSdk();
        
        // Initialize Payment Method Repository
        repository = new PaymentMethodRepository(getApplicationContext());
        
        initializeViews();
        setupClickListeners();
        setupComposeBottomSheet();
        setupRecacheUI();
        loadSavedPaymentMethods();
        observeViewModelState();
        
        // Handle configuration change restoration
        configHelper.onCreate(savedInstanceState);
        
        Log.d(TAG, "Activity created with configuration change support");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        configHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configHelper.onConfigurationChanged();
    }


    private void initializeViews() {
        checkoutButton = findViewById(R.id.java_checkout_button);
        progressOverlay = findViewById(R.id.java_progress_overlay);
        progressBar = findViewById(R.id.java_progress_bar);
        buttonText = findViewById(R.id.java_button_text);
        tokenCard = findViewById(R.id.java_token_card);
        tokenText = findViewById(R.id.java_token_text);
        composeBottomSheet = findViewById(R.id.java_compose_bottom_sheet);
        recacheComposeView = findViewById(R.id.recache_compose_view);
        savedCardsComposeView = findViewById(R.id.saved_cards_compose_view);

        // Initially hide progress and token
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
        tokenCard.setVisibility(View.GONE);
    }

    private void setupComposeBottomSheet() {
        try {
            // Use the Kotlin helper to set up the ComposeView properly
            PaymentSheetJavaHelper.setupContent(composeBottomSheet, sdk);
            Log.d(TAG, "ComposeView setup completed with SpreedlyBottomSheet");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup ComposeView", e);
            Toast.makeText(this, "Failed to setup bottom sheet: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void observeViewModelState() {
        // Monitor ViewModel state changes instead of using SpreedlyHelper to avoid
        // multiple observers on the same SDK paymentResultFlow which can cause race conditions
        
        // Use a simple background thread to check for token changes
        // The ViewModel already handles all payment result events properly
        new Thread(() -> {
            String lastToken = "";
            while (!isFinishing() && !isDestroyed()) {
                try {
                    Thread.sleep(500); // Check every 500ms
                    
                    String currentToken = configHelper.getCurrentToken();
                    if (currentToken != null && !currentToken.isEmpty() && !currentToken.equals(lastToken)) {
                        // New token detected - payment completed successfully
                        lastToken = currentToken;
                        runOnUiThread(() -> {
                            configHelper.setProcessing(false);
                            updateButtonStateFromViewModel();

                            if (progressOverlay != null) {
                                progressOverlay.setVisibility(View.GONE);
                            }

                            // Show the real token from the payment
                            tokenText.setText(currentToken);
                            tokenCard.setVisibility(View.VISIBLE);

                            Toast.makeText(JavaPaymentActivity.this, "Payment completed successfully!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Payment completed - form data preserved during orientation changes!");
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Continue monitoring
                }
            }
        }).start();
        
        Log.d(TAG, "ViewModel state observation setup complete");
    }

    @SuppressLint("SetTextI18n")
    private void updateButtonStateFromViewModel() {
        boolean isInitializing = configHelper.isInitializing();
        boolean isProcessing = configHelper.isProcessing();
        updateButtonState(isInitializing, isProcessing);
    }

    @SuppressLint("SetTextI18n")
    private void updateButtonState(boolean isInitializing, boolean isProcessing) {
        boolean isButtonDisabled = isInitializing || isProcessing;
        checkoutButton.setEnabled(!isButtonDisabled);

        if (isInitializing) {
            buttonText.setText("Initializing...");
        } else if (isProcessing) {
            buttonText.setText("Processing...");
        } else {
            buttonText.setText("Express Checkout");
        }
    }

    private void setupClickListeners() {
        checkoutButton.setOnClickListener(v -> {
            // Use ViewModel state for checking
            boolean isInitializing = configHelper.isInitializing();
            boolean isProcessing = configHelper.isProcessing();
            
            if (!isInitializing && !isProcessing) {
                startCheckout();
            }
        });
    }

    private void startCheckout() {
        configHelper.setProcessing(true);
        configHelper.clearToken(); // Clear any previous token
        updateButtonStateFromViewModel();

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
        tokenCard.setVisibility(View.GONE);

        try {
            sdk.expressCheckout();
            Log.d(TAG, "Express checkout initiated - form data will be preserved during orientation changes!");
            
            // Start payment polling as backup
            configHelper.startPaymentPolling();

        } catch (Exception e) {
            Log.e(TAG, "Express checkout failed", e);
            configHelper.setProcessing(false);
            updateButtonStateFromViewModel();
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Failed to start checkout: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecacheUI() {
        // Set up the recache UI composable
        RecacheJavaHelper.setupRecacheUI(recacheComposeView, sdk);
        Log.d(TAG, "Recache UI setup completed");
    }
    
    private void loadSavedPaymentMethods() {
        // Initialize with mock data if empty
        repository.initializeMockDataIfEmpty();
        
        // Get saved payment methods from repository
        List<SavedPaymentMethod> savedMethods = repository.getSavedPaymentMethods();
        
        // Display saved cards using Compose
        displaySavedCards(savedMethods);
        
        Log.d(TAG, "Loaded " + savedMethods.size() + " saved payment methods");
    }
    
    private void displaySavedCards(List<SavedPaymentMethod> savedMethods) {
        if (savedMethods.isEmpty()) {
            return;
        }
        
        SpreedlyRecacheWrapper.setupSavedPaymentMethodsList(
            savedCardsComposeView,
            savedMethods,
            // On card click - trigger recaching
            savedCard -> handleRecacheClick(savedCard),
            // On delete click - remove from repository
            savedCard -> {
                repository.deletePaymentMethod(savedCard.getToken());
                loadSavedPaymentMethods(); // Reload list
                Toast.makeText(this, "Card removed", Toast.LENGTH_SHORT).show();
            }
        );
    }
    
    private void handleRecacheClick(SavedPaymentMethod savedCard) {
        Log.d(TAG, "Recaching card: " + savedCard.getCardType() + " **** " + savedCard.getLastFourDigits());
        
        // Create recache config - SDK automatically calculates CVV length from card type
        var config = RecacheJavaHelper.createRecacheConfigFull(
            savedCard.getLastFourDigits(),
            savedCard.getCardType(),
            savedCard.getCardholderName(),
            ScreenPresentationMode.bottomSheet,
            "CVV",
            "123",
            "Confirm",
            "Cancel"
        );
        
        // Trigger recaching
        RecacheJavaHelper.recachePaymentMethod(
            this, // LifecycleOwner
            sdk,
            savedCard.getToken(),
            config,
            // Success callback
            updatedToken -> {
                runOnUiThread(() -> {
                    // Display the updated token
                    tokenText.setText(updatedToken);
                    tokenCard.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(this, 
                        "CVV recached successfully!", 
                        Toast.LENGTH_LONG).show();
                });
            },
            // Error callback
            errorMessage -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Recaching failed: " + errorMessage, 
                        Toast.LENGTH_LONG).show();
                    
                    Log.e(TAG, "Recaching failed: " + errorMessage);
                });
            }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear state when activity is finishing to prevent stale messages
        // when navigating back to form list
        if (configHelper != null && isFinishing()) {
            configHelper.clearStateOnFinish();
            Log.d(TAG, "Cleared state on activity finish");
        }
        Log.d(TAG, "Activity destroyed");
    }
}