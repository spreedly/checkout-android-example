package com.spreedly.example.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spreedly.example.screens.bankaccount.BankAccountViewModel
import com.spreedly.example.screens.basiccheckout.BasicCheckoutViewModel
import com.spreedly.example.screens.bottomsheet.BottomSheetPaymentViewModel
import com.spreedly.example.screens.braintreepayment.BraintreePaymentViewModel
import com.spreedly.example.screens.customcheckout.CheckoutWithAdditionalFieldsViewModel
import com.spreedly.example.screens.customizedcheckout.CustomisedCheckoutViewModel
import com.spreedly.example.screens.customtextfields.CustomTextFieldsViewModel
import com.spreedly.example.screens.ebanxpayment.EbanxPaymentViewModel
import com.spreedly.example.screens.flexibleexpiry.FlexibleExpiryViewModel
import com.spreedly.example.screens.offsitepayment.OffsitePaymentViewModel
import com.spreedly.example.screens.recachingshowcase.RecachingShowcaseViewModel
import com.spreedly.example.screens.reusablebottomsheet.ReusableBottomSheetPaymentViewModel
import com.spreedly.example.screens.stripeapmpayment.StripeAPMPaymentViewModel
import com.spreedly.example.screens.three3dsglobal.ThreeDSGlobalExampleViewModel
import com.spreedly.example.screens.threedschallenge.ThreeDSExampleViewModel

/**
 * Helper functions to create ViewModels with context parameter.
 * These factories pass the application context to ViewModels that need it
 * for SDK initialization and Datadog logging.
 */

@Composable
inline fun <reified VM : ViewModel> viewModelWithContext(
    crossinline create: (Context) -> VM,
): VM {
    val context = LocalContext.current
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(context.applicationContext) as T
        },
    )
}

@Composable
inline fun <reified VM : ViewModel> viewModelWithContext(
    viewModelStoreOwner: ViewModelStoreOwner,
    crossinline create: (Context) -> VM,
): VM {
    val context = LocalContext.current
    return viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(context.applicationContext) as T
        },
    )
}

@Composable
inline fun <reified VM : ViewModel> viewModelWithContextAndActivity(
    crossinline create: (Context, Activity) -> VM,
): VM {
    val context = LocalContext.current
    val activity = context.findActivity()
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(
                context.applicationContext,
                activity,
            ) as T
        },
    )
}

/**
 * Helper to find the Activity from a Context.
 * Walks up the Context chain until an Activity is found.
 */
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Could not find Activity in Context chain")
}

@Composable
fun bankAccountViewModel(): BankAccountViewModel = viewModelWithContext { context ->
    BankAccountViewModel(context)
}

@Composable
fun bottomSheetPaymentViewModel(): BottomSheetPaymentViewModel = viewModelWithContext { context ->
    BottomSheetPaymentViewModel(context)
}

@Composable
fun reusableBottomSheetPaymentViewModel(): ReusableBottomSheetPaymentViewModel = viewModelWithContext { context ->
    ReusableBottomSheetPaymentViewModel(context)
}

@Composable
fun basicCheckoutViewModel(): BasicCheckoutViewModel = viewModelWithContext { context ->
    BasicCheckoutViewModel(context)
}

@Composable
fun checkoutWithAdditionalFieldsViewModel(): CheckoutWithAdditionalFieldsViewModel = viewModelWithContext { context ->
    CheckoutWithAdditionalFieldsViewModel(context)
}

@Composable
fun customisedCheckoutViewModel(): CustomisedCheckoutViewModel = viewModelWithContext { context ->
    CustomisedCheckoutViewModel(context)
}

@Composable
fun flexibleExpiryViewModel(): FlexibleExpiryViewModel = viewModelWithContext { context ->
    FlexibleExpiryViewModel(context)
}

@Composable
fun customTextFieldsViewModel(): CustomTextFieldsViewModel = viewModelWithContext { context ->
    CustomTextFieldsViewModel(context)
}

@Composable
fun configurationChangeAwareViewModel(): ConfigurationChangeAwareViewModel = viewModelWithContext { context ->
    ConfigurationChangeAwareViewModel(context)
}

@Composable
fun recachingShowcaseViewModel(): RecachingShowcaseViewModel = viewModelWithContext { context ->
    RecachingShowcaseViewModel(context)
}

@Composable
fun threeDSExampleViewModel(): ThreeDSExampleViewModel = viewModelWithContextAndActivity { context, activity ->
    ThreeDSExampleViewModel(context, activity)
}

@Composable
fun threeDSGlobalExampleViewModel(): ThreeDSGlobalExampleViewModel = viewModelWithContext { context ->
    ThreeDSGlobalExampleViewModel(context)
}

@Composable
fun offsitePaymentViewModel(): OffsitePaymentViewModel = viewModelWithContext { context ->
    OffsitePaymentViewModel(context)
}

@Composable
fun ebanxPaymentViewModel(): EbanxPaymentViewModel = viewModelWithContext { context ->
    EbanxPaymentViewModel(context)
}

@Composable
fun stripeAPMPaymentViewModel(): StripeAPMPaymentViewModel = viewModelWithContext { context ->
    StripeAPMPaymentViewModel(context)
}

@Composable
fun braintreePaymentViewModel(): BraintreePaymentViewModel = viewModelWithContext { context ->
    BraintreePaymentViewModel(context)
}
