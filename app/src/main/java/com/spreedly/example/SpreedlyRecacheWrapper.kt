package com.spreedly.example

import androidx.compose.ui.platform.ComposeView
import com.spreedly.example.models.SavedPaymentMethod
import com.spreedly.example.ui.components.SavedPaymentMethodsList

/**
 * App-specific wrapper for saved payment methods list UI (Java interop).
 *
 * Generic recache functionality (setupRecacheUI, createRecacheConfig,
 * recachePaymentMethod) has moved to [com.spreedly.paymentsheet.recache.RecacheJavaHelper]
 * in the payments-core SDK module.
 */
object SpreedlyRecacheWrapper {
    /**
     * Setup saved payment methods list for Java interop.
     */
    @JvmStatic
    fun setupSavedPaymentMethodsList(
        composeView: ComposeView,
        savedPaymentMethods: List<SavedPaymentMethod>,
        onCardClick: CardClickCallback,
        onDeleteClick: CardClickCallback,
    ) {
        composeView.setContent {
            SavedPaymentMethodsList(
                savedPaymentMethods = savedPaymentMethods,
                onCardClick = { savedCard -> onCardClick.onClick(savedCard) },
                onDeleteClick = { savedCard -> onDeleteClick.onClick(savedCard) },
            )
        }
    }

    /**
     * Java-friendly callback interface for card clicks.
     */
    interface CardClickCallback {
        fun onClick(savedPaymentMethod: SavedPaymentMethod)
    }
}
