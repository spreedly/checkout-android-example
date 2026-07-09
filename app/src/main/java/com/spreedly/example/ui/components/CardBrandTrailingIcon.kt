package com.spreedly.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.app.R
import com.spreedly.sdk.models.CardScheme

@Composable
fun CardBrandTrailingIcon(
    scheme: CardScheme?,
    modifier: Modifier = Modifier,
) {
    when (scheme) {
        CardScheme.VISA ->
            Image(
                painter = painterResource(R.drawable.ic_card_brand_visa),
                contentDescription = "Visa",
                modifier = modifier.size(36.dp),
            )

        null, CardScheme.UNKNOWN -> Unit

        else ->
            Text(
                text = scheme.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = modifier,
            )
    }
}
