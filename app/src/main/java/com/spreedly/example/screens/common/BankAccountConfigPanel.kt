package com.spreedly.example.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spreedly.app.R
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.ui.theme.SpreedlyExampleTheme
import com.spreedly.sdk.ui.BankAccountFieldConfig
import com.spreedly.sdk.ui.CustomFieldsConfig
import com.spreedly.sdk.ui.NameFieldDisplayMode

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primaryColor else outlineColor,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
fun BankAccountConfigPanel(
    fieldConfig: BankAccountFieldConfig,
    onFieldConfigChange: (BankAccountFieldConfig) -> Unit,
    uiConfig: CustomFieldsConfig,
    onUiConfigChange: (CustomFieldsConfig) -> Unit,
    useCustomTheme: Boolean,
    onUseCustomThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val primaryColors =
        remember(isDark) {
            if (isDark) {
                listOf(
                    Color(0xFF64B5F6),
                    Color(0xFF81C784),
                    Color(0xFFBA68C8),
                    Color(0xFFE57373),
                    Color(0xFF4DB6AC),
                    Color(0xFFFF8A65),
                )
            } else {
                listOf(
                    Color(0xFF1976D2),
                    Color(0xFF388E3C),
                    Color(0xFF7B1FA2),
                    Color(0xFFD32F2F),
                    Color(0xFF00897B),
                    Color(0xFFE64A19),
                )
            }
        }
    val fieldBackgroundColors =
        remember(isDark) {
            if (isDark) {
                listOf(
                    Color.Transparent,
                    Color(0xFF2C2C2C),
                    Color(0xFF1B3A2A),
                    Color(0xFF1A2C3D),
                    Color(0xFF3D2E1A),
                    Color(0xFF2E1A3D),
                )
            } else {
                listOf(
                    Color.Transparent,
                    Color(0xFFF5F5F5),
                    Color(0xFFE8F5E9),
                    Color(0xFFE3F2FD),
                    Color(0xFFFFF3E0),
                    Color(0xFFF3E5F5),
                )
            }
        }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.config_field_configuration),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        Text(
            text = stringResource(R.string.config_name_display_mode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Spacing.xxs),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                stringResource(R.string.config_name_full) to NameFieldDisplayMode.SINGLE_FIELD,
                stringResource(R.string.config_name_separate) to NameFieldDisplayMode.SEPARATE_FIELDS,
            ).forEachIndexed { index, (label, mode) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = 2,
                    ),
                    onClick = { onFieldConfigChange(fieldConfig.copy(nameDisplayMode = mode)) },
                    selected = fieldConfig.nameDisplayMode == mode,
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.config_show_bank_name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = fieldConfig.showBankName,
                onCheckedChange = { onFieldConfigChange(fieldConfig.copy(showBankName = it)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.config_show_account_type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = fieldConfig.showAccountType,
                onCheckedChange = { onFieldConfigChange(fieldConfig.copy(showAccountType = it)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.config_show_account_holder_type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = fieldConfig.showAccountHolderType,
                onCheckedChange = { onFieldConfigChange(fieldConfig.copy(showAccountHolderType = it)) },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.config_ui_customization),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.config_use_custom_theme),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = useCustomTheme,
                onCheckedChange = onUseCustomThemeChange,
            )
        }

        if (useCustomTheme) {
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.config_primary_color),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                primaryColors.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = uiConfig.primaryColor == color,
                        onClick = { onUiConfigChange(uiConfig.copy(primaryColor = color)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.config_field_background),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                fieldBackgroundColors.forEach { color ->
                    val fieldBackgroundValue =
                        if (color == Color.Transparent) Color.Unspecified else color
                    val isDefaultSwatch = color == Color.Transparent
                    ColorSwatch(
                        color = color,
                        selected =
                            if (isDefaultSwatch) {
                                uiConfig.fieldBackgroundColor == Color.Unspecified
                            } else {
                                uiConfig.fieldBackgroundColor == fieldBackgroundValue
                            },
                        onClick = {
                            val hasCustomBg = !isDefaultSwatch
                            onUiConfigChange(
                                uiConfig.copy(
                                    fieldBackgroundColor = fieldBackgroundValue,
                                    textColor = if (hasCustomBg && isDark) {
                                        Color.White
                                    } else if (hasCustomBg) {
                                        Color.Black
                                    } else {
                                        Color.Unspecified
                                    },
                                    fieldLabelColor = if (hasCustomBg && isDark) {
                                        Color.LightGray
                                    } else if (hasCustomBg) {
                                        Color.DarkGray
                                    } else {
                                        Color.Unspecified
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.config_border_radius),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slider(
                    modifier = Modifier.weight(1f),
                    value = uiConfig.borderRadius.value,
                    onValueChange = {
                        onUiConfigChange(
                            uiConfig.copy(
                                borderRadius = it.dp,
                                fieldShape = RoundedCornerShape(it.dp),
                            ),
                        )
                    },
                    valueRange = 0f..24f,
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(
                    text = "${uiConfig.borderRadius.value.toInt()}dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BankAccountConfigPanelPreview() {
    SpreedlyExampleTheme {
        Surface {
            Column(modifier = Modifier.padding(Spacing.md)) {
                BankAccountConfigPanel(
                    fieldConfig = BankAccountFieldConfig.Default,
                    onFieldConfigChange = {},
                    uiConfig = CustomFieldsConfig.Default,
                    onUiConfigChange = {},
                    useCustomTheme = false,
                    onUseCustomThemeChange = {},
                )
            }
        }
    }
}
