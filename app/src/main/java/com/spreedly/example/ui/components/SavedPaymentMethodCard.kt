package com.spreedly.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.models.SavedPaymentMethod
import com.spreedly.example.ui.theme.Spacing

/**
 * Card displaying a saved payment method with tap-to-recache functionality.
 * Design inspired by the 3DS Example screen PaymentMethodCard.
 *
 * @param savedPaymentMethod The saved payment method to display
 * @param onCardClick Callback when the card is clicked (to trigger recaching)
 * @param onDeleteClick Callback when the delete button is clicked
 * @param modifier Modifier for the card
 * @param isSelected Whether this card is currently selected (optional, for selection UI)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPaymentMethodCard(
    savedPaymentMethod: SavedPaymentMethod,
    onCardClick: (SavedPaymentMethod) -> Unit,
    onDeleteClick: (SavedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Card(
        onClick = { onCardClick(savedPaymentMethod) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Card icon
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = "Card",
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // Card details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                // Card type and masked number combined
                Text(
                    text = "${savedPaymentMethod.getFormattedCardType()} •••• ${savedPaymentMethod.lastFourDigits}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                // Expiry date
                savedPaymentMethod.getFormattedExpiry()?.let { expiry ->
                    Text(
                        text = "Expires: $expiry",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                    )
                }
            }

            // Check icon for selected state
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
