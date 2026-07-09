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

    private ComposeView merchantMaskToggleComposeView;
    private ComposeView hostedFieldsComposeView;
    private ComposeView recacheComposeView;
    private ComposeView savedCardsComposeView;
    private CardView tokenCard;
    private TextView tokenText;
    private Button retokenizeButton;

    private ConfigurationChangeHelper configHelper;
    private Spreedly sdk;
    private PaymentMethodRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_hosted_fields);

        configHelper = new ConfigurationChangeHelper(this);
        sdk = configHelper.getSdk();

        repository = new PaymentMethodRepository(getApplicationContext());

        initializeViews();
        setupHostedFieldsCompose();
        setupRecacheUI();
        loadSavedPaymentMethods();
        observePaymentToken();

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
        merchantMaskToggleComposeView = findViewById(R.id.merchant_mask_toggle_compose_view);
        hostedFieldsComposeView = findViewById(R.id.hosted_fields_compose_view);
        recacheComposeView = findViewById(R.id.recache_compose_view);
        savedCardsComposeView = findViewById(R.id.saved_cards_compose_view);
        tokenCard = findViewById(R.id.token_card);
        tokenText = findViewById(R.id.token_text);
        retokenizeButton = findViewById(R.id.retokenize_button);

        tokenCard.setVisibility(View.GONE);

        retokenizeButton.setOnClickListener(v -> {
            tokenCard.setVisibility(View.GONE);
            configHelper.getViewModel().reinitialize();
        });
    }

    private void setupHostedFieldsCompose() {
        try {
            JavaHostedFieldsWrapper.setupMerchantMaskToggle(
                merchantMaskToggleComposeView,
                sdk,
                this::refreshHostedFieldsCompose
            );
            refreshHostedFieldsCompose();
            Log.d(TAG, "Hosted Fields ComposeView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup hosted fields ComposeView", e);
            Toast.makeText(this, "Failed to setup hosted fields: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void refreshHostedFieldsCompose() {
        JavaHostedFieldsWrapper.setupHostedFieldsCompose(
            hostedFieldsComposeView,
            sdk,
            configHelper.getViewModel()
        );
    }

    private void observePaymentToken() {
        JavaHostedFieldsWrapper.observePaymentToken(
            this,
            configHelper.getViewModel(),
            this::showPaymentToken
        );
    }

    private void showPaymentToken(String token) {
        runOnUiThread(() -> {
            tokenText.setText(token);
            tokenCard.setVisibility(View.VISIBLE);
            Toast.makeText(
                JavaHostedFieldsActivity.this,
                "Payment method created successfully!",
                Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupRecacheUI() {
        RecacheJavaHelper.setupRecacheUI(recacheComposeView, sdk);
        Log.d(TAG, "Recache UI setup completed");
    }

    private void loadSavedPaymentMethods() {
        repository.initializeMockDataIfEmpty();
        List<SavedPaymentMethod> savedMethods = repository.getSavedPaymentMethods();
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
            savedCard -> handleRecacheClick(savedCard),
            savedCard -> {
                repository.deletePaymentMethod(savedCard.getToken());
                loadSavedPaymentMethods();
                Toast.makeText(this, "Card removed", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void handleRecacheClick(SavedPaymentMethod savedCard) {
        Log.d(TAG, "Recaching card: " + savedCard.getCardType() + " **** " + savedCard.getLastFourDigits());

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

        RecacheJavaHelper.recachePaymentMethod(
            this,
            sdk,
            savedCard.getToken(),
            config,
            updatedToken -> runOnUiThread(() -> {
                tokenText.setText(updatedToken);
                tokenCard.setVisibility(View.VISIBLE);
                Toast.makeText(this, "CVV recached successfully!", Toast.LENGTH_LONG).show();
            }),
            errorMessage -> runOnUiThread(() -> {
                Toast.makeText(this, "Recaching failed: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Recaching failed: " + errorMessage);
            })
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (configHelper != null && isFinishing()) {
            configHelper.clearStateOnFinish();
            Log.d(TAG, "Cleared state on activity finish");
        }
        Log.d(TAG, "Hosted fields activity destroyed");
    }
}
