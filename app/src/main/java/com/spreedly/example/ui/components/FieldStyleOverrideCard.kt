package com.spreedly.example.ui.components

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.ui.theme.SplFieldStyleOverrides
import com.spreedly.example.ui.theme.SplFieldTarget

private data class OverrideColorOption(
    val label: String,
    val color: Color?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldStyleOverrideCard(
    selectedTarget: SplFieldTarget,
    overrides: SplFieldStyleOverrides,
    onTargetSelected: (SplFieldTarget) -> Unit,
    onOverridesChange: (SplFieldStyleOverrides) -> Unit,
    onClearOverrides: () -> Unit,
    modifier: Modifier = Modifier,
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
                text = "Field-Level Style Override",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            Text(
                text = "Style one SPL field while others keep the global theme.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Target field",
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SplFieldTarget.entries.forEach { target ->
                    FilterChip(
                        selected = selectedTarget == target,
                        onClick = { onTargetSelected(target) },
                        label = { Text(target.label) },
                    )
                }
            }

            if (selectedTarget != SplFieldTarget.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                OverrideColorRow(
                    label = "Primary",
                    selectedColor = overrides.primaryColor,
                    onColorSelected = { onOverridesChange(overrides.copy(primaryColor = it)) },
                    defaultOption = OverrideColorOption("Default", null),
                    firstAccent = OverrideColorOption("Red", Color(0xFFE53935)),
                    secondAccent = OverrideColorOption("Teal", Color(0xFF00897B)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverrideColorRow(
                    label = "Background",
                    selectedColor = overrides.fieldBackgroundColor,
                    onColorSelected = { onOverridesChange(overrides.copy(fieldBackgroundColor = it)) },
                    defaultOption = OverrideColorOption("Default", null),
                    firstAccent = OverrideColorOption("Gray", Color(0xFFECEFF1)),
                    secondAccent = OverrideColorOption("Yellow", Color(0xFFFFF9C4)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverrideColorRow(
                    label = "Text",
                    selectedColor = overrides.textColor,
                    onColorSelected = { onOverridesChange(overrides.copy(textColor = it)) },
                    defaultOption = OverrideColorOption("Default", null),
                    firstAccent = OverrideColorOption("Navy", Color(0xFF1A237E)),
                    secondAccent = OverrideColorOption("Black", Color(0xFF212121)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverrideColorRow(
                    label = "Placeholder",
                    selectedColor = overrides.placeholderColor,
                    onColorSelected = { onOverridesChange(overrides.copy(placeholderColor = it)) },
                    defaultOption = OverrideColorOption("Default", null),
                    firstAccent = OverrideColorOption("Gray", Color(0xFF9E9E9E)),
                    secondAccent = OverrideColorOption("Blue", Color(0xFF5C6BC0)),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverrideColorRow(
                    label = "Border",
                    selectedColor = overrides.borderColor,
                    onColorSelected = { onOverridesChange(overrides.copy(borderColor = it)) },
                    defaultOption = OverrideColorOption("Default", null),
                    firstAccent = OverrideColorOption("Red", Color(0xFFE53935)),
                    secondAccent = OverrideColorOption("Orange", Color(0xFFFB8C00)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onClearOverrides) {
                    Text("Clear field override")
                }
            }
        }
    }
}

@Composable
private fun OverrideColorRow(
    label: String,
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    defaultOption: OverrideColorOption,
    firstAccent: OverrideColorOption,
    secondAccent: OverrideColorOption,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverrideColorChip(
                label = defaultOption.label,
                color = defaultOption.color,
                isSelected = selectedColor == defaultOption.color,
                onClick = { onColorSelected(defaultOption.color) },
            )
            OverrideColorChip(
                label = firstAccent.label,
                color = firstAccent.color,
                isSelected = selectedColor == firstAccent.color,
                onClick = { onColorSelected(firstAccent.color) },
            )
            OverrideColorChip(
                label = secondAccent.label,
                color = secondAccent.color,
                isSelected = selectedColor == secondAccent.color,
                onClick = { onColorSelected(secondAccent.color) },
            )
        }
    }
}

@Composable
private fun OverrideColorChip(
    label: String,
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .then(
                        if (color == null) {
                            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            Modifier.background(color)
                        },
                    ).border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            },
                        shape = CircleShape,
                    ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
