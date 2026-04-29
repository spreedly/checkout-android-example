package com.spreedly.example;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.cardview.widget.CardView;
import android.widget.TextView;

import com.spreedly.app.R;
import com.spreedly.example.models.SavedPaymentMethod;
import com.spreedly.example.repository.PaymentMethodRepository;
import com.spreedly.sdk.Spreedly;
import com.spreedly.sdk.models.ScreenPresentationMode;
import com.spreedly.paymentsheet.recache.RecacheJavaHelper;

import android.widget.Button;

import java.util.List;

public class JavaHostedFieldsActivity extends AppCompatActivity {

    private static final String TAG = "JavaHostedFieldsActivity";

    private ComposeView hostedFieldsComposeView;
    private ComposeView recacheComposeView;
    private ComposeView savedCardsComposeView;
    private CardView tokenCard;
    private TextView tokenText;
    private Button retokenizeButton;
    
    // Configuration change helper
    private ConfigurationChangeHelper configHelper;
    private Spreedly sdk;
    private PaymentMethodRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_hosted_fields);

        // Initialize configuration change helper
        configHelper = new ConfigurationChangeHelper(this);
        sdk = configHelper.getSdk();
        
        // Initialize Payment Method Repository
        repository = new PaymentMethodRepository(getApplicationContext());

        initializeViews();
        setupHostedFieldsCompose();
        setupRecacheUI();
        loadSavedPaymentMethods();
        observeViewModelState();
        
        // Handle configuration change restoration
        configHelper.onCreate(savedInstanceState);
        
        Log.d(TAG, "Activity created with configuration change support for hosted fields");
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
        hostedFieldsComposeView = findViewById(R.id.hosted_fields_compose_view);
        recacheComposeView = findViewById(R.id.recache_compose_view);
        savedCardsComposeView = findViewById(R.id.saved_cards_compose_view);
        tokenCard = findViewById(R.id.token_card);
        tokenText = findViewById(R.id.token_text);
        retokenizeButton = findViewById(R.id.retokenize_button);

        // Initially hide token card
        tokenCard.setVisibility(View.GONE);

        retokenizeButton.setOnClickListener(v -> {
            tokenCard.setVisibility(View.GONE);
            configHelper.getViewModel().reinitialize();
        });
    }

    private void setupHostedFieldsCompose() {
        try {
            // Use the helper to set up the hosted fields form with reactive state observation
            JavaHostedFieldsWrapper.setupHostedFieldsCompose(hostedFieldsComposeView, sdk, configHelper.getViewModel());
            Log.d(TAG, "Hosted Fields ComposeView setup completed with reactive state observation");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup hosted fields ComposeView", e);
            Toast.makeText(this, "Failed to setup hosted fields: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
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
                            // Show the real token from the payment
                            tokenText.setText(currentToken);
                            tokenCard.setVisibility(View.VISIBLE);

                            Toast.makeText(JavaHostedFieldsActivity.this, 
                                "Payment method created successfully!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Payment method created - form data preserved during orientation changes");
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
        
        Log.d(TAG, "ViewModel state observation setup complete for hosted fields");
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
        Log.d(TAG, "Hosted fields activity destroyed");
    }
}