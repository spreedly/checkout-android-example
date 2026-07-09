package com.spreedly.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.ui.theme.SampleThemePreset

enum class ThemeConfigurationStyle {
    SWATCH,
    BUTTON,
}

@Composable
fun ThemeConfigurationCard(
    useCustomTheme: Boolean,
    selectedPreset: SampleThemePreset,
    onUseCustomThemeChange: (Boolean) -> Unit,
    onPresetSelected: (SampleThemePreset) -> Unit,
    onResetTheme: () -> Unit,
    style: ThemeConfigurationStyle,
    modifier: Modifier = Modifier,
    showDarkPreset: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Theme Configuration",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Use Custom Theme",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = useCustomTheme,
                    onCheckedChange = onUseCustomThemeChange,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Current Theme:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                when (style) {
                    ThemeConfigurationStyle.SWATCH -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(selectedPreset.swatchColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape),
                        )
                        Text(
                            text = selectedPreset.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (useCustomTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    ThemeConfigurationStyle.BUTTON -> {
                        Text(
                            text = selectedPreset.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (useCustomTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            if (useCustomTheme) {
                Spacer(modifier = Modifier.height(12.dp))
                val presets =
                    buildList {
                        add(SampleThemePreset.BLUE)
                        add(SampleThemePreset.GREEN)
                        add(SampleThemePreset.PURPLE)
                        if (showDarkPreset) {
                            add(SampleThemePreset.DARK)
                        }
                    }

                when (style) {
                    ThemeConfigurationStyle.SWATCH -> {
                        Text(
                            text = "Pick a color:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            presets.forEach { preset ->
                                ThemeSwatchButton(
                                    preset = preset,
                                    isSelected = selectedPreset == preset,
                                    onClick = { onPresetSelected(preset) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onResetTheme) {
                            Text("Reset to Default")
                        }
                    }
                    ThemeConfigurationStyle.BUTTON -> {
                        Text(
                            text = "Custom Theme Colors:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            presets.forEach { preset ->
                                ThemeLabelButton(
                                    text = "${preset.displayName} Theme",
                                    accentColor = preset.swatchColor,
                                    isSelected = selectedPreset == preset,
                                    onClick = { onPresetSelected(preset) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchButton(
    preset: SampleThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(preset.swatchColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            )
        }
    }
}

@Composable
private fun ThemeLabelButton(
    text: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = if (isSelected) 0.2f else 0.1f))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
