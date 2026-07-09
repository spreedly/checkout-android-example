package com.spreedly.example.screens.stripeapmpayment

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@SuppressLint("ComposeModifierMissing")
@Composable
fun StripeAPMAppearanceSection(
    useCustomAppearance: Boolean,
    onUseCustomAppearanceChange: (Boolean) -> Unit,
    primaryColor: Color,
    onPrimaryColorChange: (Color) -> Unit,
    backgroundColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    buttonBackgroundColor: Color,
    onButtonBackgroundColorChange: (Color) -> Unit,
    buttonTextColor: Color,
    onButtonTextColorChange: (Color) -> Unit,
    cornerRadiusDp: Float,
    onCornerRadiusDpChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "PaymentSheet appearance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(StripeApmPaymentTestTags.APPEARANCE_SECTION_TITLE),
            )
            Text(
                text = "Colors map to StripeAPMAppearanceConfig and are applied when PaymentSheet opens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Customize PaymentSheet appearance",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = useCustomAppearance,
                    onCheckedChange = onUseCustomAppearanceChange,
                    enabled = enabled,
                    modifier = Modifier.testTag(StripeApmPaymentTestTags.USE_CUSTOM_APPEARANCE_TOGGLE),
                )
            }
            AnimatedVisibility(visible = useCustomAppearance) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StripeAppearanceColorPickerRow(
                        title = "Primary",
                        color = primaryColor,
                        onColorChange = onPrimaryColorChange,
                        enabled = enabled,
                        testTag = StripeApmPaymentTestTags.PRIMARY_COLOR_PICKER,
                    )
                    StripeAppearanceColorPickerRow(
                        title = "Background",
                        color = backgroundColor,
                        onColorChange = onBackgroundColorChange,
                        enabled = enabled,
                        testTag = StripeApmPaymentTestTags.BACKGROUND_COLOR_PICKER,
                    )
                    StripeAppearanceColorPickerRow(
                        title = "Pay button background",
                        color = buttonBackgroundColor,
                        onColorChange = onButtonBackgroundColorChange,
                        enabled = enabled,
                        testTag = StripeApmPaymentTestTags.BUTTON_BACKGROUND_COLOR_PICKER,
                    )
                    StripeAppearanceColorPickerRow(
                        title = "Pay button text",
                        color = buttonTextColor,
                        onColorChange = onButtonTextColorChange,
                        enabled = enabled,
                        testTag = StripeApmPaymentTestTags.BUTTON_TEXT_COLOR_PICKER,
                    )
                    StripeAppearanceCornerRadiusRow(
                        cornerRadiusDp = cornerRadiusDp,
                        onCornerRadiusDpChange = onCornerRadiusDpChange,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun StripeAppearanceColorPickerRow(
    title: String,
    color: Color,
    onColorChange: (Color) -> Unit,
    enabled: Boolean,
    testTag: String,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Pick a color for ${title.lowercase()} in the Stripe PaymentSheet" }
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { showDialog = true },
        )
    }

    if (showDialog) {
        StripeAppearanceColorDialog(
            title = title,
            selectedColor = color,
            onColorSelected = {
                onColorChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StripeAppearanceColorDialog(
    title: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select $title") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StripeApmAppearancePresetColors.forEach { preset ->
                    val isSelected = preset.toArgb() == selectedColor.toArgb()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .clickable { onColorSelected(preset) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun StripeAppearanceCornerRadiusRow(
    cornerRadiusDp: Float,
    onCornerRadiusDpChange: (Float) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StripeApmPaymentTestTags.CORNER_RADIUS_STEPPER)
            .semantics { contentDescription = "Corner radius for Stripe PaymentSheet" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Corner radius", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${cornerRadiusDp.toInt()} pt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            IconButton(
                onClick = { onCornerRadiusDpChange((cornerRadiusDp - 1f).coerceAtLeast(0f)) },
                enabled = enabled && cornerRadiusDp > 0f,
            ) {
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(
                onClick = { onCornerRadiusDpChange((cornerRadiusDp + 1f).coerceAtMost(24f)) },
                enabled = enabled && cornerRadiusDp < 24f,
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private val StripeApmAppearancePresetColors: List<Color> = listOf(
    DefaultStripeApmPrimaryColor,
    Color(0xFF6750A4),
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFFF3B30),
    Color(0xFF000000),
    Color(0xFFFFFFFF),
    Color(0xFF8E8E93),
    Color(0xFF5AC8FA),
    Color(0xFFFF2D55),
    Color(0xFF30B0C7),
)
