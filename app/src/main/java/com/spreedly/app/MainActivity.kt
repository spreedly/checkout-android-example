package com.spreedly.app

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.spreedly.braintree.SpreedlyBraintreeAPMCheckout
import com.spreedly.example.screens.bankaccount.BankAccountScreen
import com.spreedly.example.screens.basiccheckout.BasicCheckoutScreen
import com.spreedly.example.screens.headlessbankaccount.HeadlessBankAccountConfigScreen
import com.spreedly.example.screens.headlessbankaccount.HeadlessBankAccountScreen
import com.spreedly.example.screens.headlessbankaccount.HeadlessBankAccountViewModel
import com.spreedly.example.viewmodel.viewModelWithContext
import com.spreedly.example.screens.bottomsheet.BottomSheetPaymentScreen
import com.spreedly.example.screens.bottomsheet.BottomSheetPaymentViewModel
import com.spreedly.example.screens.braintreepayment.BraintreePaymentScreen
import com.spreedly.example.screens.customcheckout.CheckoutWithAdditionalFieldsScreen
import com.spreedly.example.screens.customizedcheckout.CustomisedCheckoutScreen
import com.spreedly.example.screens.customtextfields.CustomTextFieldsScreen
import com.spreedly.example.screens.designsystem.DesignSystemShowcaseScreen
import com.spreedly.example.screens.ebanxpayment.EbanxPaymentScreen
import com.spreedly.example.screens.flexibleexpiry.FlexibleExpiryScreen
import com.spreedly.example.screens.mainmenu.MainMenuScreen
import com.spreedly.example.screens.offsitepayment.OffsitePaymentScreen
import com.spreedly.example.screens.recachingshowcase.RecachingShowcaseScreen
import com.spreedly.example.screens.reusablebottomsheet.ReusableBottomSheetPaymentScreen
import com.spreedly.example.screens.stripeapmpayment.StripeAPMPaymentScreen
import com.spreedly.example.screens.three3dsglobal.ThreeDSGlobalExampleScreen
import com.spreedly.example.screens.threedschallenge.ThreeDSExampleScreen
import com.spreedly.example.ui.theme.SpreedlyExampleTheme
import com.spreedly.sdk.ui.offsite.SpreedlyOffsiteCheckout
import com.spreedly.stripe.SpreedlyStripeAPMCheckout

class MainActivity : ComponentActivity() {
    private lateinit var bottomSheetViewModel: BottomSheetPaymentViewModel

    private companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel with factory that provides context
        bottomSheetViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    Log.d(TAG, "create viewmodel")
                    return BottomSheetPaymentViewModel(applicationContext) as T
                }
            },
        )[BottomSheetPaymentViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            SpreedlyExampleTheme {
                MainNavHost(bottomSheetViewModel)
            }
        }

        // Signal configuration change complete
        bottomSheetViewModel.onConfigurationChangeComplete()

        Log.d(TAG, "Created with orientation change support")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save whether bottom sheet is currently visible
        val isBottomSheetVisible = bottomSheetViewModel.sdk.showBottomSheet.value
        outState.putBoolean("was_bottom_sheet_visible", isBottomSheetVisible)

        if (isBottomSheetVisible) {
            Log.d(TAG, "Bottom sheet is visible - will preserve state")
            // Tell the ViewModel to preserve state for the next show
            bottomSheetViewModel.onConfigurationChanging()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changing - preserving payment state")

        // Notify ViewModel that configuration is changing
        if (bottomSheetViewModel.sdk.showBottomSheet.value) {
            bottomSheetViewModel.onConfigurationChanging()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed - clearing any stale snackbar messages")

        // Clear any stale snackbar messages when returning from child activities
        // This prevents error messages from Java activities from persisting
        bottomSheetViewModel.clearSnackbarState()

        // If user returned from offsite checkout (Chrome Custom Tab or Stripe PaymentSheet),
        // finalize so the flow can emit result and clean up. Avoids registering
        // lifecycle observers from Compose when Activity is already RESUMED.
        SpreedlyOffsiteCheckout.finalizeIfActive()
        SpreedlyStripeAPMCheckout.finalizeIfActive()
        SpreedlyBraintreeAPMCheckout.finalizeIfActive()
    }
}

@Composable
fun MainNavHost(bottomSheetViewModel: BottomSheetPaymentViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(navController)
        }
        composable("home_screen") {
            BottomSheetPaymentScreen(viewModel = bottomSheetViewModel)
        }
        composable("reusable_bottom_sheet") {
            ReusableBottomSheetPaymentScreen()
        }
        composable("custom_checkout") {
            CheckoutWithAdditionalFieldsScreen()
        }
        composable("basic_checkout") {
            BasicCheckoutScreen()
        }
        composable("bank_account") {
            BankAccountScreen()
        }
        navigation(startDestination = "config", route = "headless_bank_account_flow") {
            composable("config") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("headless_bank_account_flow")
                }
                val vm: HeadlessBankAccountViewModel = viewModelWithContext(
                    viewModelStoreOwner = parentEntry,
                ) { ctx -> HeadlessBankAccountViewModel(ctx) }
                HeadlessBankAccountConfigScreen(viewModel = vm, navController = navController)
            }
            composable("form") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("headless_bank_account_flow")
                }
                val vm: HeadlessBankAccountViewModel = viewModelWithContext(
                    viewModelStoreOwner = parentEntry,
                ) { ctx -> HeadlessBankAccountViewModel(ctx) }
                HeadlessBankAccountScreen(viewModel = vm, navController = navController)
            }
        }
        composable("customized_checkout") {
            CustomisedCheckoutScreen()
        }
        composable("flexible_expiry") {
            FlexibleExpiryScreen()
        }

        composable("custom_text_fields") {
            CustomTextFieldsScreen()
        }

        composable("recaching_showcase") {
            RecachingShowcaseScreen()
        }

        composable("threeds_challenge") {
            ThreeDSExampleScreen()
        }

        composable("threeds_global_challenge") {
            ThreeDSGlobalExampleScreen()
        }

        composable("design_system") {
            DesignSystemShowcaseScreen()
        }

        composable("offsite_payment") {
            OffsitePaymentScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable("ebanx_payment") {
            EbanxPaymentScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable("stripe_apm_payment") {
            StripeAPMPaymentScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable("braintree_payment") {
            BraintreePaymentScreen(
                onBackClick = { navController.popBackStack() },
            )
        }


    }
}
