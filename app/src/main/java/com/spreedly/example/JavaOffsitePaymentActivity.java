package com.spreedly.example;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.spreedly.app.BuildConfig;
import com.spreedly.app.R;
import com.spreedly.example.api.SpreedlyPurchaseAPIClient;
import com.spreedly.example.api.SpreedlyPurchaseResponse;
import com.spreedly.sdk.Spreedly;
import com.spreedly.sdk.models.offsite.OffsitePaymentConfig;
import com.spreedly.sdk.ui.offsite.OffsitePaymentJavaHelper;
import com.spreedly.sdk.ui.offsite.SpreedlyOffsiteCheckout;
import com.spreedly.sdk.models.offsite.OffsitePaymentMethodType;
import com.spreedly.sdk.ui.PaymentResult;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import androidx.appcompat.app.AppCompatActivity;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Java example demonstrating offsite payment integration.
 *
 * Optional in the app UI: the main menu shows this entry only when
 * enableJavaOffsitePayment=true (e.g. -PenableJavaOffsitePayment=true or in gradle.properties).
 * By default the menu entry is hidden.
 *
 * This activity shows how to:
 * 1. Initialize the Spreedly SDK
 * 2. Submit an offsite payment method for tokenization
 * 3. Create a purchase via the merchant backend API
 * 4. Present the checkout in a Chrome Custom Tab
 * 5. Handle the payment result
 *
 * ## Important Notes
 *
 * - The SDK uses a custom URL scheme ({applicationId}.spreedlyoffsite://) to receive redirects.
 * - Handle payment results via OffsitePaymentJavaHelper.startPaymentResultMonitoring()
 */
public class JavaOffsitePaymentActivity extends AppCompatActivity {

    private static final String TAG = "JavaOffsitePayment";

    private String redirectUrl;

    // UI Components - Toolbar
    private Toolbar toolbar;

    // UI Components - Stage Indicator (circles and labels)
    private FrameLayout stageIdleCircle;
    private TextView stageIdleNumber;
    private TextView stageIdleLabel;
    private FrameLayout stageTokenizeCircle;
    private TextView stageTokenizeNumber;
    private TextView stageTokenizeLabel;
    private FrameLayout stagePurchaseCircle;
    private TextView stagePurchaseNumber;
    private TextView stagePurchaseLabel;
    private FrameLayout stageCheckoutCircle;
    private TextView stageCheckoutNumber;
    private TextView stageCheckoutLabel;

    // UI Components - Provider Selection (Buttons)
    private Button btnSprel;
    private Button btnPaypal;

    // UI Components - Products
    private CardView productSunglasses;
    private CardView productWatch;
    private CardView productHeadphones;
    private CardView productCamera;
    private CardView productLaptop;
    private CardView productPhone;

    // UI Components - Messages
    private CardView errorCard;
    private TextView errorText;
    private CardView successCard;
    private TextView successText;

    // UI Components - Pay Button
    private Button payButton;

    // SDK Components
    private Spreedly sdk;
    private SpreedlyPurchaseAPIClient purchaseClient;

    // State
    private Stage currentStage = Stage.IDLE;
    private String currentPaymentMethodToken = null;
    private String currentTransactionToken = null;

    // Selection state
    private OffsitePaymentMethodType selectedProvider = OffsitePaymentMethodType.SPREL;
    private Product selectedProduct = null;

    // Product data class
    private static class Product {
        final String id;
        final String name;
        final String emoji;
        final int priceInCents;

        Product(String id, String name, String emoji, int priceInCents) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.priceInCents = priceInCents;
        }
    }

    // Products matching Kotlin implementation
    private final Product[] products = {
        new Product("sunglasses", "Sunglasses", "🕶️", 4400),
        new Product("watch", "Watch", "⌚", 19900),
        new Product("headphones", "Headphones", "🎧", 29900),
        new Product("camera", "Camera", "📷", 89900),
        new Product("laptop", "Laptop", "💻", 129900),
        new Product("phone", "Phone", "📱", 99900)
    };

    /**
     * Payment flow stages matching the Kotlin implementation.
     */
    private enum Stage {
        IDLE,
        CREATING_PAYMENT_METHOD,
        PURCHASING,
        CHECKOUT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_offsite_payment);

        redirectUrl = SpreedlyOffsiteCheckout.INSTANCE.redirectUrl(this, "sprel/checkout");

        initializeViews();
        initializeSDK();
        setupToolbar();
        setupProviderButtons();
        setupProductCards();
        setupPayButton();
        startPaymentResultMonitoring();

        Log.d(TAG, "JavaOffsitePaymentActivity created");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Handle return from Chrome Custom Tab
        if (currentStage == Stage.CHECKOUT) {
            Log.d(TAG, "Resuming from checkout - finalizing if active");
            OffsitePaymentJavaHelper.finalizeIfActive();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (purchaseClient != null) {
            purchaseClient.close();
        }
        Log.d(TAG, "JavaOffsitePaymentActivity destroyed");
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Stage indicators - circles
        stageIdleCircle = findViewById(R.id.stage_idle_circle);
        stageIdleNumber = findViewById(R.id.stage_idle_number);
        stageIdleLabel = findViewById(R.id.stage_idle_label);
        stageTokenizeCircle = findViewById(R.id.stage_tokenize_circle);
        stageTokenizeNumber = findViewById(R.id.stage_tokenize_number);
        stageTokenizeLabel = findViewById(R.id.stage_tokenize_label);
        stagePurchaseCircle = findViewById(R.id.stage_purchase_circle);
        stagePurchaseNumber = findViewById(R.id.stage_purchase_number);
        stagePurchaseLabel = findViewById(R.id.stage_purchase_label);
        stageCheckoutCircle = findViewById(R.id.stage_checkout_circle);
        stageCheckoutNumber = findViewById(R.id.stage_checkout_number);
        stageCheckoutLabel = findViewById(R.id.stage_checkout_label);

        // Provider buttons
        btnSprel = findViewById(R.id.btn_sprel);
        btnPaypal = findViewById(R.id.btn_paypal);

        // Product cards
        productSunglasses = findViewById(R.id.product_sunglasses);
        productWatch = findViewById(R.id.product_watch);
        productHeadphones = findViewById(R.id.product_headphones);
        productCamera = findViewById(R.id.product_camera);
        productLaptop = findViewById(R.id.product_laptop);
        productPhone = findViewById(R.id.product_phone);

        // Message cards
        errorCard = findViewById(R.id.error_card);
        errorText = findViewById(R.id.error_text);
        successCard = findViewById(R.id.success_card);
        successText = findViewById(R.id.success_text);

        // Pay button
        payButton = findViewById(R.id.pay_button);

        // Initially hide message cards
        errorCard.setVisibility(View.GONE);
        successCard.setVisibility(View.GONE);
    }

    private void initializeSDK() {
        // Create SDK instance - actual initialization with auth params happens before each payment
        // because enhanced iframe security params are single-use
        sdk = new Spreedly();

        purchaseClient = new SpreedlyPurchaseAPIClient();

        Log.d(TAG, "SDK instance created, ready for payment");
        updateUIForStage();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupProviderButtons() {
        // Select Sprel by default
        selectProvider(OffsitePaymentMethodType.SPREL);

        btnSprel.setOnClickListener(v -> selectProvider(OffsitePaymentMethodType.SPREL));
        btnPaypal.setOnClickListener(v -> selectProvider(OffsitePaymentMethodType.PAYPAL));
    }

    private void selectProvider(OffsitePaymentMethodType provider) {
        selectedProvider = provider;

        boolean isSprel = provider == OffsitePaymentMethodType.SPREL;

        // Update button backgrounds and text colors
        btnSprel.setBackgroundResource(isSprel ? R.drawable.provider_button_selected : R.drawable.provider_button_unselected);
        btnSprel.setTextColor(isSprel ? Color.WHITE : getColor(R.color.purple_500));

        btnPaypal.setBackgroundResource(!isSprel ? R.drawable.provider_button_selected : R.drawable.provider_button_unselected);
        btnPaypal.setTextColor(!isSprel ? Color.WHITE : getColor(R.color.purple_500));
    }

    private void setupProductCards() {
        CardView[] cards = {
            productSunglasses, productWatch, productHeadphones,
            productCamera, productLaptop, productPhone
        };

        for (int i = 0; i < cards.length; i++) {
            final Product product = products[i];
            final CardView card = cards[i];

            card.setOnClickListener(v -> selectProduct(product, card));
        }
    }

    private void selectProduct(Product product, CardView selectedCard) {
        if (currentStage != Stage.IDLE) return;

        selectedProduct = product;

        // Reset all cards to default state (use theme-aware color)
        CardView[] cards = {
            productSunglasses, productWatch, productHeadphones,
            productCamera, productLaptop, productPhone
        };

        int defaultColor = ContextCompat.getColor(this, R.color.card_background);
        int selectedColor = ContextCompat.getColor(this, R.color.card_selected_background);

        for (CardView card : cards) {
            card.setCardBackgroundColor(defaultColor);
            card.setCardElevation(4f);
        }

        // Highlight selected card
        selectedCard.setCardBackgroundColor(selectedColor);
        selectedCard.setCardElevation(8f);

        // Update pay button
        updatePayButton();
    }

    private void setupPayButton() {
        payButton.setOnClickListener(v -> {
            if (currentStage == Stage.IDLE && selectedProduct != null) {
                // Clear previous messages
                errorCard.setVisibility(View.GONE);
                successCard.setVisibility(View.GONE);
                startPayment();
            }
        });

        updatePayButton();
    }

    @SuppressLint("SetTextI18n")
    private void updatePayButton() {
        if (selectedProduct == null) {
            payButton.setText("Select a Product");
            payButton.setEnabled(false);
        } else {
            payButton.setText("Pay " + formatPrice(selectedProduct.priceInCents));
            payButton.setEnabled(currentStage == Stage.IDLE);
        }
    }

    private String formatPrice(int cents) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setCurrency(Currency.getInstance("USD"));
        return format.format(cents / 100.0);
    }

    /**
     * Initialize SDK with fresh auth params.
     * This is called before each payment because enhanced iframe security params are single-use.
     *
     * @param onSuccess Called when SDK is initialized
     * @param onError Called on initialization error
     */
    private void initializeSDKWithFreshAuth(Runnable onSuccess, java.util.function.Consumer<Exception> onError) {
        Log.d(TAG, "Fetching fresh auth params for payment");

        SpreedlyHelper.initializeSdkWithRemoteAuth(
            this,
            sdk,
            getApplicationContext(),
            BuildConfig.ENVIRONMENT_KEY,
            () -> {
                Log.d(TAG, "SDK initialized with fresh auth");
                onSuccess.run();
            },
            error -> {
                Log.e(TAG, "SDK initialization failed", error);
                onError.accept(error);
            }
        );
    }

    private void startPaymentResultMonitoring() {
        OffsitePaymentJavaHelper.startPaymentResultMonitoring(
            this,
            sdk,
            // On completed
            result -> {
                Log.d(TAG, "Payment completed");
                runOnUiThread(() -> handlePaymentCompleted(result));
            },
            // On failed
            result -> {
                Log.e(TAG, "Payment failed");
                runOnUiThread(() -> handlePaymentFailed(result));
            },
            // On canceled
            () -> {
                Log.d(TAG, "Payment canceled");
                runOnUiThread(this::handlePaymentCanceled);
            }
        );
    }

    private void startPayment() {
        if (selectedProduct == null) {
            showError("Please select a product first");
            return;
        }

        Log.d(TAG, "Starting payment with provider: " + selectedProvider.name() +
              ", product: " + selectedProduct.name);

        // Update state
        setStage(Stage.CREATING_PAYMENT_METHOD);

        // Step 1: Fetch fresh auth params and re-initialize SDK
        // This is required because enhanced iframe security params are single-use
        initializeSDKWithFreshAuth(
            () -> {
                // SDK initialized with fresh auth - proceed with payment
                runOnUiThread(() -> proceedWithPayment(selectedProvider));
            },
            error -> {
                Log.e(TAG, "Failed to initialize SDK: " + error.getClass().getSimpleName());
                runOnUiThread(() -> {
                    setStage(Stage.IDLE);
                    showError("Failed to initialize SDK");
                });
            }
        );
    }

    /**
     * Proceed with the payment after SDK is initialized with fresh auth.
     */
    private void proceedWithPayment(OffsitePaymentMethodType paymentType) {
        // Build config
        OffsitePaymentConfig config = new OffsitePaymentConfig(
            paymentType,
            redirectUrl,
            "test@example.com",  // email
            "Test User",         // fullName
            null, null, null,    // firstName, lastName, documentId
            "US",                // country
            null, null,          // countryCode, phoneNumber
            "123 Main St",       // address1
            null,                // address2
            "San Francisco",     // city
            "CA",                // state
            "94105"              // zip
        );

        // Submit offsite payment
        OffsitePaymentJavaHelper.submitOffsitePayment(
            this,
            sdk,
            config,
            // Success - proceed to purchase
            paymentMethodToken -> {
                Log.d(TAG, "Payment method created");
                runOnUiThread(() -> proceedToPurchase(paymentMethodToken, paymentType));
            },
            // Failed
            error -> {
                Log.e(TAG, "Failed to create payment method: " + error);
                runOnUiThread(() -> {
                    setStage(Stage.IDLE);
                    showError("Failed to create payment method: " + error);
                });
            }
        );
    }

    private void proceedToPurchase(String paymentMethodToken, OffsitePaymentMethodType paymentType) {
        currentPaymentMethodToken = paymentMethodToken;
        setStage(Stage.PURCHASING);

        String gateway = paymentType == OffsitePaymentMethodType.PAYPAL
            ? SpreedlyPurchaseAPIClient.GATEWAY_PAYPAL
            : null;

        Log.d(TAG, "Creating purchase");

        new Thread(() -> {
            try {
                SpreedlyPurchaseResponse response = executePurchaseBlocking(
                    gateway,
                    paymentMethodToken,
                    selectedProduct.priceInCents,
                    "USD",
                    redirectUrl
                );

                if (response.getTransaction() != null) {
                    String transactionToken = response.getTransaction().getToken();
                    Log.d(TAG, "Purchase created");
                    runOnUiThread(() -> proceedToCheckout(transactionToken));
                } else {
                    String errorMsg = response.getErrors() != null && !response.getErrors().isEmpty()
                        ? response.getErrors().get(0).getMessage()
                        : "Unknown error";
                    Log.e(TAG, "Purchase failed: " + errorMsg);
                    runOnUiThread(() -> {
                        setStage(Stage.IDLE);
                        showError("Purchase failed: " + errorMsg);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Purchase error: " + e.getClass().getSimpleName());
                runOnUiThread(() -> {
                    setStage(Stage.IDLE);
                    showError("Purchase error");
                });
            }
        }).start();
    }

    private SpreedlyPurchaseResponse executePurchaseBlocking(
            String gateway,
            String paymentMethodToken,
            int amount,
            String currencyCode,
            String redirectUrl) throws Exception {

        AtomicReference<SpreedlyPurchaseResponse> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        Thread purchaseThread = new Thread(() -> {
            try {
                result.set(kotlinx.coroutines.BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> purchaseClient.purchase(
                        gateway,
                        paymentMethodToken,
                        amount,
                        currencyCode,
                        redirectUrl,
                        SpreedlyPurchaseAPIClient.DEFAULT_CALLBACK_URL,
                        continuation
                    )
                ));
            } catch (Exception e) {
                error.set(e);
            }
        });

        purchaseThread.start();
        purchaseThread.join();

        if (error.get() != null) {
            throw error.get();
        }

        return result.get();
    }

    private void proceedToCheckout(String transactionToken) {
        currentTransactionToken = transactionToken;
        setStage(Stage.CHECKOUT);

        Log.d(TAG, "Presenting checkout");

        // Present checkout in Chrome Custom Tab
        OffsitePaymentJavaHelper.presentCheckout(transactionToken, this);
    }

    private void handlePaymentCompleted(PaymentResult.Completed result) {
        Log.d(TAG, "Payment completed handler called, currentStage=" + currentStage);

        if (currentStage == Stage.CREATING_PAYMENT_METHOD) {
            // This is the tokenization result - proceed to purchase
            // Note: The actual flow uses the callback in submitOffsitePayment
            Log.d(TAG, "Ignoring tokenization result (handled via callback)");
            return;
        }

        // This is the final checkout result
        Log.d(TAG, "Final checkout result - showing success");
        setStage(Stage.IDLE);
        showSuccess("Payment completed!\n\nToken: " + result.getToken() + "\n\nState: " + result.getState());
    }

    private void handlePaymentFailed(PaymentResult.Failed result) {
        Log.d(TAG, "Payment failed handler called");
        setStage(Stage.IDLE);
        showError("Payment failed: " + result.getDescription() + "\n\nState: " + result.getState());
    }

    private void handlePaymentCanceled() {
        Log.d(TAG, "Payment canceled handler called");
        setStage(Stage.IDLE);
        showError("Payment was canceled");
    }

    @SuppressLint("SetTextI18n")
    private void setStage(Stage stage) {
        Log.d(TAG, "Setting stage: " + stage);
        currentStage = stage;
        updateUIForStage();
    }

    @SuppressLint("SetTextI18n")
    private void updateUIForStage() {
        boolean isIdle = currentStage == Stage.IDLE;

        // Update stage indicators
        updateStageIndicator();

        // Enable/disable provider buttons
        btnSprel.setEnabled(isIdle);
        btnPaypal.setEnabled(isIdle);

        // Enable/disable product cards
        float alpha = isIdle ? 1.0f : 0.5f;
        productSunglasses.setAlpha(alpha);
        productWatch.setAlpha(alpha);
        productHeadphones.setAlpha(alpha);
        productCamera.setAlpha(alpha);
        productLaptop.setAlpha(alpha);
        productPhone.setAlpha(alpha);

        // Update pay button
        switch (currentStage) {
            case IDLE:
                updatePayButton();
                break;
            case CREATING_PAYMENT_METHOD:
                payButton.setText("Creating payment method...");
                payButton.setEnabled(false);
                break;
            case PURCHASING:
                payButton.setText("Processing purchase...");
                payButton.setEnabled(false);
                break;
            case CHECKOUT:
                payButton.setText("Waiting for checkout...");
                payButton.setEnabled(false);
                break;
        }
    }

    private void updateStageIndicator() {
        int primaryColor = ContextCompat.getColor(this, R.color.purple_500);
        int inactiveColor = ContextCompat.getColor(this, R.color.stage_inactive_color);

        // Determine current index
        int currentIndex = 0;
        switch (currentStage) {
            case IDLE: currentIndex = 0; break;
            case CREATING_PAYMENT_METHOD: currentIndex = 1; break;
            case PURCHASING: currentIndex = 2; break;
            case CHECKOUT: currentIndex = 3; break;
        }

        // Update stage 1 (Idle) - always active when we're in a valid stage
        boolean stage1Active = true;
        stageIdleCircle.setBackgroundResource(stage1Active ? R.drawable.stage_circle_active : R.drawable.stage_circle_inactive);
        stageIdleNumber.setTextColor(stage1Active ? Color.WHITE : inactiveColor);
        stageIdleLabel.setTextColor(stage1Active ? primaryColor : inactiveColor);

        // Update stage 2 (Tokenize)
        boolean stage2Active = currentIndex >= 1;
        stageTokenizeCircle.setBackgroundResource(stage2Active ? R.drawable.stage_circle_active : R.drawable.stage_circle_inactive);
        stageTokenizeNumber.setTextColor(stage2Active ? Color.WHITE : inactiveColor);
        stageTokenizeLabel.setTextColor(stage2Active ? primaryColor : inactiveColor);

        // Update stage 3 (Purchase)
        boolean stage3Active = currentIndex >= 2;
        stagePurchaseCircle.setBackgroundResource(stage3Active ? R.drawable.stage_circle_active : R.drawable.stage_circle_inactive);
        stagePurchaseNumber.setTextColor(stage3Active ? Color.WHITE : inactiveColor);
        stagePurchaseLabel.setTextColor(stage3Active ? primaryColor : inactiveColor);

        // Update stage 4 (Checkout)
        boolean stage4Active = currentIndex >= 3;
        stageCheckoutCircle.setBackgroundResource(stage4Active ? R.drawable.stage_circle_active : R.drawable.stage_circle_inactive);
        stageCheckoutNumber.setTextColor(stage4Active ? Color.WHITE : inactiveColor);
        stageCheckoutLabel.setTextColor(stage4Active ? primaryColor : inactiveColor);
    }

    private void showSuccess(String message) {
        Log.d(TAG, "showSuccess");
        errorCard.setVisibility(View.GONE);
        successText.setText(message);
        successCard.setVisibility(View.VISIBLE);

        // Scroll to show the success card
        successCard.post(() -> successCard.getParent().requestChildFocus(successCard, successCard));
    }

    private void showError(String message) {
        Log.d(TAG, "showError");
        successCard.setVisibility(View.GONE);
        errorText.setText(message);
        errorCard.setVisibility(View.VISIBLE);

        // Scroll to show the error card
        errorCard.post(() -> errorCard.getParent().requestChildFocus(errorCard, errorCard));
    }
}
