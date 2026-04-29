package com.spreedly.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.sdk.ui.CustomFieldsConfig

@SuppressLint("ComposeModifierMissing")
@Composable
fun SectionHeader(
    title: String,
    config: CustomFieldsConfig,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(Spacing.xxs)
                    .background(
                        color = config.primaryColor,
                        shape = CircleShape,
                    ),
        )
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = config.primaryColor,
                ),
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}
