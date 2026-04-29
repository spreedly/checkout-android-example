package com.spreedly.example.screens.three3dsglobal

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spreedly.example.models.Product
import com.spreedly.example.models.SavedCard
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.viewmodel.threeDSGlobalExampleViewModel
import com.spreedly.threeds.ui.ThreeDSChallengeSheet

/**
 * 3DS Payment Flow Screen
 *
 * Matches iOS implementation: ThreeDSPaymentFlowView.swift
 *
 * Features:
 * - Product selection (6 products matching iOS)
 * - Payment method selection (loaded from API)
 * - Pay button (enabled when both selected)
 * - Automatic 3DS challenge flow
 * - Success/failure handling
 */
@Composable
fun ThreeDSGlobalExampleScreen(
    modifier: Modifier = Modifier,
    viewModel: ThreeDSGlobalExampleViewModel = threeDSGlobalExampleViewModel(),
) {
    val sdk = viewModel.sdk
    val selectedProduct by viewModel.selectedProduct
    val selectedCard by viewModel.selectedCard
    val isLoading by viewModel.isLoading
    val isLoadingCards by viewModel.isLoadingCards
    val savedCards by viewModel.savedCards
    val successMessage by viewModel.successMessage
    val errorMessage by viewModel.errorMessage
    val isPayButtonEnabled = viewModel.isPayButtonEnabled

    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Header
            HeaderSection()

            // Product Selection
            ProductSelectionSection(
                products = viewModel.products,
                selectedProduct = selectedProduct,
                onProductSelected = { viewModel.selectProduct(it) },
            )

            // Payment Method Selection
            if (isLoadingCards) {
                LoadingPaymentMethodsSection()
            } else {
                PaymentMethodSelectionSection(
                    cards = savedCards,
                    selectedCard = selectedCard,
                    onCardSelected = { viewModel.selectCard(it) },
                )
            }

            // Pay Button
            PayButton(
                isEnabled = isPayButtonEnabled,
                isLoading = isLoading,
                onClick = { viewModel.handlePayButtonTap() },
            )

            // Success Message
            successMessage?.let { message ->
                SuccessMessageCard(
                    message = message,
                    onDismiss = { viewModel.clearSuccess() },
                )
            }

            // Error Message
            errorMessage?.let { message ->
                ErrorMessageCard(
                    message = message,
                    onDismiss = { viewModel.clearError() },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        // Unified 3DS Challenge UI (works for both Global and Gateway-Specific flows)
        ThreeDSChallengeSheet(sdk = sdk)
    }
}

@Composable
private fun HeaderSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            Text(
                text = "3DS Challenge Flow",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Select a product and payment method, then proceed with the 3DS challenge flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionSection(
    @SuppressLint("ComposeUnstableCollections") products: List<Product>,
    selectedProduct: Product?,
    onProductSelected: (Product) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Select Product",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            maxItemsInEachRow = 2,
        ) {
            products.forEach { product ->
                ProductChip(
                    product = product,
                    isSelected = selectedProduct?.id == product.id,
                    onClick = { onProductSelected(product) },
                    modifier = Modifier.weight(0.5f),
                )
            }
        }

        // Selected product details
        selectedProduct?.let { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = getProductIcon(product.iconName),
                        contentDescription = product.name,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                    Text(
                        text = product.formattedPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductChip(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.sm),
            ) {
                Icon(
                    imageVector = getProductIcon(product.iconName),
                    contentDescription = product.name,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = product.formattedPrice(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        leadingIcon = null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = if (isSelected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = MaterialTheme.colorScheme.primary,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 2.dp,
                selectedBorderWidth = 2.dp,
            )
        } else {
            FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
        },
    )
}

@Composable
private fun LoadingPaymentMethodsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loading payment methods...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PaymentMethodSelectionSection(
    @SuppressLint("ComposeUnstableCollections") cards: List<SavedCard>,
    selectedCard: SavedCard?,
    onCardSelected: (SavedCard) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Select Payment Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        if (cards.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = "No saved payment methods. Please add cards first.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cards.forEach { card ->
                    PaymentMethodCard(
                        card = card,
                        isSelected = selectedCard?.id == card.id,
                        onClick = { onCardSelected(card) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodCard(
    card: SavedCard,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                card.displayExpiry?.let { expiry ->
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
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PayButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = isEnabled && !isLoading,
        contentPadding = PaddingValues(16.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Processing...",
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                text = "Pay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SuccessMessageCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Success!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/**
 * Map product icon name to Material Icon
 */
private fun getProductIcon(iconName: String): ImageVector = when (iconName) {
    "headphones" -> Icons.Default.Headphones
    "watch" -> Icons.Default.Watch
    "tablet" -> Icons.Default.Tablet
    "laptop" -> Icons.Default.Laptop
    "speaker" -> Icons.Default.Speaker
    "videogame_asset" -> Icons.Default.Videocam
    else -> Icons.Default.CreditCard
}
