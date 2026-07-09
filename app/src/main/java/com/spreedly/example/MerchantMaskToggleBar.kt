package com.spreedly.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spreedly.app.R
import com.spreedly.sdk.Spreedly
import com.spreedly.sdk.ui.CardNumberFormat
import com.spreedly.sdk.ui.HostedCardDisplayState

@Composable
fun MerchantMaskToggleBar(
    sdk: Spreedly,
    hostedCardDisplayState: HostedCardDisplayState,
    modifier: Modifier = Modifier,
) {
    val formats =
        listOf(
            CardNumberFormat.PRETTY to stringResource(R.string.sample_format_pretty),
            CardNumberFormat.PLAIN to stringResource(R.string.sample_format_plain),
            CardNumberFormat.MASKED to stringResource(R.string.sample_format_masked),
        )
    val selectedIndex = formats.indexOfFirst { it.first == hostedCardDisplayState.cardNumberFormat }
        .coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sample_card_display_format_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            formats.forEachIndexed { index, (format, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
                    selected = selectedIndex == index,
                    onClick = { sdk.setNumberFormat(format) },
                ) {
                    Text(label)
                }
            }
        }
        Text(
            text = stringResource(R.string.sample_mask_toggle_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        TextButton(onClick = { sdk.toggleMask() }) {
            Text(stringResource(R.string.sample_toggle_mask_button))
        }
        Text(
            text =
                stringResource(
                    R.string.sample_pan_masked_policy,
                    hostedCardDisplayState.panMasked.toString(),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
