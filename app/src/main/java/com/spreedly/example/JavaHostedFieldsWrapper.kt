package com.spreedly.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.util.function.Consumer
import kotlinx.coroutines.launch
import com.spreedly.example.qa.FieldStateInspectorCard
import com.spreedly.example.qa.JavaHostedFieldsSampleConfig
import com.spreedly.example.viewmodel.ConfigurationChangeAwareViewModel
import com.spreedly.hostedfields.HostedFieldsJavaHelper
import com.spreedly.hostedfields.models.HostedFieldStateListener
import com.spreedly.sdk.Spreedly

object JavaHostedFieldsWrapper {
    @JvmStatic
    fun setupMerchantMaskToggle(composeView: ComposeView, sdk: Spreedly, onAutofillChanged: Runnable) {
        composeView.setContent {
            val hostedCardDisplayState by sdk.hostedCardDisplayState
            Column(modifier = Modifier.fillMaxWidth()) {
                MerchantMaskToggleBar(
                    sdk = sdk,
                    hostedCardDisplayState = hostedCardDisplayState,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "enableAutofill",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    androidx.compose.material3.Switch(
                        checked = JavaHostedFieldsSampleConfig.enableAutofill,
                        onCheckedChange = {
                            JavaHostedFieldsSampleConfig.enableAutofill = it
                            onAutofillChanged.run()
                        },
                    )
                }
            }
        }
    }

    @JvmStatic
    fun setupHostedFieldsCompose(
        composeView: ComposeView,
        sdk: Spreedly,
        viewModel: ConfigurationChangeAwareViewModel,
    ) {
        val listener =
            HostedFieldStateListener { state ->
                viewModel.onHostedFieldState(state)
            }
        HostedFieldsJavaHelper.setupContentWithInspector(
            composeView,
            sdk,
            listener,
            {
                HostedFieldsInspectorSlot(sdk = sdk, viewModel = viewModel)
            },
            onFieldValidated = { type, valid -> viewModel.onHostedFieldValidation(type, valid) },
            enableAutofill = JavaHostedFieldsSampleConfig.enableAutofill,
        )
    }

    @JvmStatic
    fun observePaymentToken(
        lifecycleOwner: LifecycleOwner,
        viewModel: ConfigurationChangeAwareViewModel,
        onNonEmptyToken: Consumer<String>,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentToken.collect { token ->
                    if (token.isNotEmpty()) {
                        onNonEmptyToken.accept(token)
                    }
                }
            }
        }
    }

    @Composable
    private fun HostedFieldsInspectorSlot(
        sdk: Spreedly,
        viewModel: ConfigurationChangeAwareViewModel,
    ) {
        val hostedCardDisplayState by sdk.hostedCardDisplayState
        val inspectorUiState by viewModel.inspectorUiState.collectAsState()
        LaunchedEffect(hostedCardDisplayState) {
            viewModel.fieldStateInspector.refreshMismatch(hostedCardDisplayState)
        }
        FieldStateInspectorCard(
            uiState = inspectorUiState,
            hostedCardDisplayState = hostedCardDisplayState,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
