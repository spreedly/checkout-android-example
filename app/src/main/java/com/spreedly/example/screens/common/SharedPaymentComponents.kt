package com.spreedly.example.screens.common

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared stage indicator for payment flows (Offsite, EBANX, Stripe APM).
 *
 * @param stageLabels Labels for each stage (e.g. listOf("Idle", "Tokenize", "Purchase", "Checkout") or listOf("Idle", "Purchase", "Checkout"))
 * @param currentIndex Zero-based index of the current stage
 */
@SuppressLint("ComposeUnstableCollections")
@Composable
fun PaymentStageIndicator(
    stageLabels: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for ((index, name) in stageLabels.withIndex()) {
            val isActive = index <= currentIndex
            val isCurrent = index == currentIndex

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}

/**
 * Shared provider selector (chips) for payment methods.
 *
 * @param T Type of provider (e.g. OffsitePaymentMethodType or String for APM type names)
 * @param getLabel Maps provider to display label
 */
@SuppressLint("ComposeUnstableCollections")
@Composable
fun <T> PaymentProviderSelector(
    providers: List<T>,
    selectedProvider: T,
    getLabel: (T) -> String,
    onProviderSelected: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        providers.forEach { provider ->
            FilterChip(
                selected = selectedProvider == provider,
                onClick = { if (enabled) onProviderSelected(provider) },
                label = { Text(getLabel(provider)) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

/**
 * Multi-select provider selector (chips) for payment methods (e.g. Stripe APM types).
 *
 * @param T Type of provider
 * @param getLabel Maps provider to display label
 */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("ComposeUnstableCollections")
@Composable
fun <T> PaymentProviderMultiSelector(
    providers: List<T>,
    selectedProviders: Set<T>,
    getLabel: (T) -> String,
    onProviderToggled: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        providers.forEach { provider ->
            FilterChip(
                selected = selectedProviders.contains(provider),
                onClick = { if (enabled) onProviderToggled(provider) },
                label = { Text(getLabel(provider)) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

/**
 * Shared product grid for payment screens.
 */
@SuppressLint("ComposeUnstableCollections")
@Composable
fun PaymentProductGrid(
    products: List<Product>,
    selectedProduct: Product?,
    onProductSelected: (Product) -> Unit,
    enabled: Boolean,
    formatPrice: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.height(320.dp),
    ) {
        items(products) { product ->
            PaymentProductCard(
                product = product,
                isSelected = selectedProduct == product,
                onClick = { if (enabled) onProductSelected(product) },
                enabled = enabled,
                priceText = formatPrice(product.price),
            )
        }
    }
}

/**
 * Shared product card for payment screens.
 */
@Composable
fun PaymentProductCard(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    priceText: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = product.emoji, fontSize = 24.sp)
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Shared error message card (red container).
 */
@Composable
fun PaymentErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/**
 * Shared success message card (tertiary container).
 */
@Composable
fun PaymentSuccessCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}
