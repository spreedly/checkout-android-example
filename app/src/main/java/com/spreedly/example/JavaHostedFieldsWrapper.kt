package com.spreedly.example

import androidx.compose.ui.platform.ComposeView
import com.spreedly.example.viewmodel.ConfigurationChangeAwareViewModel
import com.spreedly.hostedfields.HostedFieldsJavaHelper
import com.spreedly.sdk.Spreedly

/**
 * App-specific wrapper that adds ViewModel-based configuration-change awareness
 * on top of the SDK's [HostedFieldsJavaHelper].
 *
 * The ViewModel parameter is retained so the Activity can manage SDK lifecycle
 * across configuration changes; the actual form layout delegates to the SDK.
 */
object JavaHostedFieldsWrapper {
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun setupHostedFieldsCompose(
        composeView: ComposeView,
        sdk: Spreedly,
        viewModel: ConfigurationChangeAwareViewModel,
    ) {
        HostedFieldsJavaHelper.setupContent(composeView, sdk)
    }
}
