package com.spreedly.example.screens.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spreedly.example.ui.theme.Blue200
import com.spreedly.example.ui.theme.Blue400
import com.spreedly.example.ui.theme.Blue50
import com.spreedly.example.ui.theme.Blue600
import com.spreedly.example.ui.theme.Blue800
import com.spreedly.example.ui.theme.CustomShapes
import com.spreedly.example.ui.theme.Gray200
import com.spreedly.example.ui.theme.Gray400
import com.spreedly.example.ui.theme.Gray600
import com.spreedly.example.ui.theme.Gray800
import com.spreedly.example.ui.theme.Green500
import com.spreedly.example.ui.theme.Orange500
import com.spreedly.example.ui.theme.Red600
import com.spreedly.example.ui.theme.Spacing
import com.spreedly.example.ui.theme.SpreedlyExampleTheme
import com.spreedly.example.ui.theme.SpreedlyYellow
import com.spreedly.example.ui.theme.Teal500
import com.spreedly.example.ui.theme.info
import com.spreedly.example.ui.theme.success
import com.spreedly.example.ui.theme.successLight
import com.spreedly.example.ui.theme.warning
import com.spreedly.example.ui.theme.warningLight

/**
 * A comprehensive showcase of the Spreedly design system
 */
@Composable
fun DesignSystemShowcaseScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.sectionSpacing),
    ) {
        // Header
        Text(
            text = "Design System Showcase",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Colors Section
        ColorShowcaseSection()

        Divider()

        // Typography Section
        TypographyShowcaseSection()

        Divider()

        // Buttons Section
        ButtonShowcaseSection()

        Divider()

        // Cards Section
        CardShowcaseSection()

        Divider()

        // Spacing Section
        SpacingShowcaseSection()

        Divider()

        // Semantic Colors Section
        SemanticColorsSection()

        // Bottom spacing
        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Composable
private fun ColorShowcaseSection() {
    Column {
        SectionTitle("Colors")

        // Primary Colors
        SubsectionTitle("Primary (Blue)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ColorSwatch(color = Blue50, label = "50")
            ColorSwatch(color = Blue200, label = "200")
            ColorSwatch(color = Blue400, label = "400")
            ColorSwatch(color = Blue600, label = "600")
            ColorSwatch(color = Blue800, label = "800")
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Secondary Colors
        SubsectionTitle("Secondary (Teal)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ColorSwatch(color = Teal500, label = "Teal")
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Neutral Colors
        SubsectionTitle("Neutral (Gray)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ColorSwatch(color = Gray200, label = "200")
            ColorSwatch(color = Gray400, label = "400")
            ColorSwatch(color = Gray600, label = "600")
            ColorSwatch(color = Gray800, label = "800")
        }
    }
}

@Composable
private fun TypographyShowcaseSection() {
    Column {
        SectionTitle("Typography")

        TypographyExample("Display Large", MaterialTheme.typography.displayLarge)
        TypographyExample("Display Medium", MaterialTheme.typography.displayMedium)
        TypographyExample("Display Small", MaterialTheme.typography.displaySmall)

        Spacer(modifier = Modifier.height(Spacing.xs))

        TypographyExample("Headline Large", MaterialTheme.typography.headlineLarge)
        TypographyExample("Headline Medium", MaterialTheme.typography.headlineMedium)
        TypographyExample("Headline Small", MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(Spacing.xs))

        TypographyExample("Title Large", MaterialTheme.typography.titleLarge)
        TypographyExample("Title Medium", MaterialTheme.typography.titleMedium)
        TypographyExample("Title Small", MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(Spacing.xs))

        TypographyExample("Body Large", MaterialTheme.typography.bodyLarge)
        TypographyExample("Body Medium", MaterialTheme.typography.bodyMedium)
        TypographyExample("Body Small", MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(Spacing.xs))

        TypographyExample("Label Large", MaterialTheme.typography.labelLarge)
        TypographyExample("Label Medium", MaterialTheme.typography.labelMedium)
        TypographyExample("Label Small", MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ButtonShowcaseSection() {
    Column {
        SectionTitle("Buttons")

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Primary Button")
        }

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Outlined Button")
        }

        TextButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Text Button")
        }

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.success,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Success Button")
        }

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Error Button")
        }
        }
    }
}

@Composable
private fun CardShowcaseSection() {
    Column {
        SectionTitle("Cards")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Card Title",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "This is a sample card demonstrating the card component with proper spacing and typography.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Highlighted Card",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "This card uses the primary container color for emphasis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SpacingShowcaseSection() {
    Column {
        SectionTitle("Spacing")

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SpacingExample("XXS (4dp)", Spacing.xxs)
            SpacingExample("XS (8dp)", Spacing.xs)
            SpacingExample("SM (12dp)", Spacing.sm)
            SpacingExample("MD (16dp)", Spacing.md)
            SpacingExample("LG (24dp)", Spacing.lg)
            SpacingExample("XL (32dp)", Spacing.xl)
        }
    }
}

@Composable
private fun SemanticColorsSection() {
    Column {
        SectionTitle("Semantic Colors")

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
        SemanticColorCard(
            title = "Success",
            icon = Icons.Default.Check,
            backgroundColor = MaterialTheme.colorScheme.successLight,
            foregroundColor = Green500,
            description = "Used for positive actions and confirmations",
        )

        SemanticColorCard(
            title = "Warning",
            icon = Icons.Default.Warning,
            backgroundColor = MaterialTheme.colorScheme.warningLight,
            foregroundColor = Orange500,
            description = "Used for cautionary messages",
        )

        SemanticColorCard(
            title = "Error",
            icon = Icons.Default.Close,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            foregroundColor = Red600,
            description = "Used for error messages and destructive actions",
        )

        SemanticColorCard(
            title = "Info",
            icon = Icons.Default.Info,
            backgroundColor = MaterialTheme.colorScheme.info,
            foregroundColor = Blue600,
            description = "Used for informational messages",
        )

        // Spreedly Brand Colors
        SubsectionTitle("Spreedly Brand Colors")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ColorSwatch(color = Red600, label = "Red")
            ColorSwatch(color = Orange500, label = "Orange")
            ColorSwatch(color = SpreedlyYellow, label = "Yellow")
        }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

@Composable
private fun SubsectionTitle(text: String) {
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CustomShapes.rounded)
                    .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TypographyExample(
    label: String,
    style: TextStyle,
) {
    Column(
        modifier = Modifier.padding(vertical = Spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Text(
            text = "The quick brown fox",
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SpacingExample(
    label: String,
    spacing: androidx.compose.ui.unit.Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(120.dp),
        )
        Box(
            modifier =
                Modifier
                    .height(24.dp)
                    .width(spacing)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.extraSmall,
                    ),
        )
    }
}

@Composable
private fun SemanticColorCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    foregroundColor: Color,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = backgroundColor,
            ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foregroundColor,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = foregroundColor,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DesignSystemShowcaseScreenPreview() {
    SpreedlyExampleTheme {
        Surface {
            DesignSystemShowcaseScreen()
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DesignSystemShowcaseScreenDarkPreview() {
    SpreedlyExampleTheme(darkTheme = true) {
        Surface {
            DesignSystemShowcaseScreen()
        }
    }
}
