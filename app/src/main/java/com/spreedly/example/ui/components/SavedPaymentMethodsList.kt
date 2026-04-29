package com.spreedly.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.models.SavedPaymentMethod
import com.spreedly.example.ui.theme.Spacing

/**
 * List of saved payment methods with section title.
 *
 * @param savedPaymentMethods List of saved payment methods to display
 * @param onCardClick Callback when a card is clicked (to trigger recaching)
 * @param onDeleteClick Callback when the delete button is clicked
 * @param modifier Modifier for the list
 */
@SuppressLint("ComposeUnstableCollections")
@Composable
fun SavedPaymentMethodsList(
    savedPaymentMethods: List<SavedPaymentMethod>,
    onCardClick: (SavedPaymentMethod) -> Unit,
    onDeleteClick: (SavedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (savedPaymentMethods.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Section header
        Text(
            text = "Saved Payment Methods",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        // Help text
        Text(
            text = "Tap a card to use it for this payment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        // Cards list
        savedPaymentMethods.forEach { savedMethod ->
            SavedPaymentMethodCard(
                savedPaymentMethod = savedMethod,
                onCardClick = onCardClick,
                onDeleteClick = onDeleteClick,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}
