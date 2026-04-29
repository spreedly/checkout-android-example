package com.spreedly.example;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.spreedly.example.viewmodel.ConfigurationChangeAwareViewModel;
import com.spreedly.sdk.Spreedly;

/**
 * Helper class for Java activities to handle configuration changes properly.
 * This ensures that payment form data is preserved during orientation changes.
 */
public class ConfigurationChangeHelper {
    
    private static final String TAG = "ConfigurationChangeHelper";
    private static final String KEY_WAS_BOTTOM_SHEET_VISIBLE = "was_bottom_sheet_visible";
    
    private ConfigurationChangeAwareViewModel viewModel;
    private AppCompatActivity activity;
    
    public ConfigurationChangeHelper(AppCompatActivity activity) {
        this.activity = activity;
        
        // Create a factory that provides the Context to the ViewModel
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(ConfigurationChangeAwareViewModel.class)) {
                    @SuppressWarnings("unchecked")
                    T viewModel = (T) new ConfigurationChangeAwareViewModel(activity.getApplicationContext());
                    return viewModel;
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        };
        
        this.viewModel = new ViewModelProvider(activity, factory).get(ConfigurationChangeAwareViewModel.class);
    }
    
    /**
     * Get the SDK instance from the ViewModel.
     */
    public Spreedly getSdk() {
        return viewModel.getSdk();
    }
    
    /**
     * Get the ViewModel instance.
     */
    public ConfigurationChangeAwareViewModel getViewModel() {
        return viewModel;
    }
    
    /**
     * Call this method in Activity.onCreate() after super.onCreate().
     */
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called - restoring state if needed");
        
        // Check if we're restoring from a configuration change
        if (savedInstanceState != null) {
            boolean wasBottomSheetVisible = savedInstanceState.getBoolean(KEY_WAS_BOTTOM_SHEET_VISIBLE, false);
            if (wasBottomSheetVisible) {
                Log.d(TAG, "Bottom sheet was visible before config change - preserving state");
                // The ViewModel already handled state preservation
            }
        }
        
        // Signal that configuration change is complete
        viewModel.onConfigurationChangeComplete();
    }
    
    /**
     * Call this method in Activity.onSaveInstanceState().
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState called - saving current state");
        
        // Save whether bottom sheet is currently visible
        boolean isBottomSheetVisible = viewModel.getSdk().getShowBottomSheet().getValue();
        outState.putBoolean(KEY_WAS_BOTTOM_SHEET_VISIBLE, isBottomSheetVisible);
        
        if (isBottomSheetVisible) {
            Log.d(TAG, "Bottom sheet is visible - will preserve state");
            // Tell the ViewModel to preserve state for the next show
            viewModel.onConfigurationChanging();
        }
    }
    
    /**
     * Call this method in Activity.onConfigurationChanged().
     */
    public void onConfigurationChanged() {
        Log.d(TAG, "onConfigurationChanged called");
        
        // Check if bottom sheet is currently visible
        if (viewModel.getSdk().getShowBottomSheet().getValue()) {
            Log.d(TAG, "Bottom sheet visible during config change - preserving state");
            viewModel.onConfigurationChanging();
        }
    }
    
    /**
     * Start processing state.
     */
    public void setProcessing(boolean processing) {
        viewModel.setProcessing(processing);
    }
    
    /**
     * Clear payment token.
     */
    public void clearToken() {
        viewModel.clearToken();
    }
    
    /**
     * Start payment polling timeout.
     */
    public void startPaymentPolling() {
        viewModel.startPaymentPolling();
    }
    
    /**
     * Check if SDK is initializing.
     */
    public boolean isInitializing() {
        return viewModel.getInitializingState().getValue();
    }
    
    /**
     * Check if payment is processing.
     */
    public boolean isProcessing() {
        return viewModel.getProcessingState().getValue();
    }
    
    /**
     * Clear snackbar state and reset UI state when activity is finishing.
     * This prevents stale error messages from appearing when returning to form list.
     */
    public void clearStateOnFinish() {
        viewModel.clearStateOnFinish();
    }
    
    /**
     * Get the current payment token from the ViewModel.
     */
    public String getCurrentToken() {
        return viewModel.getTokenState().getValue();
    }
}