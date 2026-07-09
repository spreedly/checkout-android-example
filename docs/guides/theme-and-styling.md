# Theme and Styling

A guide to the Spreedly Android SDK design system, built with Jetpack Compose and Material Design 3. It covers the color palette, typography scale, spacing, shapes, and dark mode support used across SDK components.

## Overview

The design system provides a consistent visual language for building accessible and maintainable payment UIs. All values are defined in theme files and accessed through `MaterialTheme`, so components adapt automatically to light and dark modes.

For initial project setup, see [Getting Started](getting-started.md). For building payment forms with these styles, see [Custom Payment Forms](custom-payment-forms.md).

## Theme Configuration

Wrap your app (or the relevant subtree) with the Spreedly theme:

```kotlin
import androidx.compose.material3.MaterialTheme

MaterialTheme(
    colorScheme = yourCustomColorScheme,  // or use default
    typography = yourCustomTypography,     // or use default
) {
    // Your app content with Spreedly SDK components
}
```

Access design tokens through `MaterialTheme`:

```kotlin
@Composable
fun MyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenPadding)
    ) {
        Text(
            text = "Title",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

The SDK theme is defined in `payments-core/src/main/java/com/spreedly/ui/theme/`:

| File | Purpose |
|------|---------|
| `Colors.kt` | Color definitions |
| `AppTheme.kt` | Theme configuration |
| `AppTypography.kt` | Typography scale |
| `SpreedlyTheme.kt` | Public theme composable |
| `GlobalThemeManager.kt` | Runtime theme overrides |

The sample app extends the theme in `app/src/main/java/com/spreedly/example/ui/theme/`:

| File | Purpose |
|------|---------|
| `Spacing.kt` | Spacing system |
| `Shape.kt` | Shape definitions |
| `Extensions.kt` | Helper extensions |

## Color Palette

### Primary - Blue

The primary palette is used for main actions, links, and brand elements.

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Blue50 | `#F2F4F8` | `242, 244, 248` | Lightest background |
| Blue100 | `#F3F8FC` | `243, 248, 252` | Light background |
| Blue200 | `#DDEDFC` | `221, 237, 252` | Info background |
| Blue300 | `#B8D9F0` | `184, 217, 240` | Hover states |
| Blue400 | `#8BC0E6` | `139, 192, 230` | Secondary actions |
| Blue500 | `#4DA0D9` | `77, 160, 217` | Active states |
| Blue600 | `#0077C8` | `0, 119, 200` | **Primary brand color** |
| Blue700 | `#005FAD` | `0, 95, 173` | Pressed states |
| Blue800 | `#00253E` | `0, 37, 62` | Dark accents |

### Secondary - Teal

Accents and complementary actions.

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Teal200 | `#96EAE2` | `150, 234, 226` | Light accents |
| Teal500 | `#2CD5C4` | `44, 213, 196` | Secondary actions |

### Neutral - Gray

Text, backgrounds, borders, and structural elements.

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Gray50 | `#FBFCFF` | `251, 252, 255` | Light backgrounds |
| Gray100 | `#F5F5F3` | `245, 245, 243` | Subtle backgrounds |
| Gray200 | `#EFEDEA` | `239, 237, 234` | Dividers (light) |
| Gray300 | `#D7D2CB` | `215, 210, 203` | Borders |
| Gray400 | `#AFB4B5` | `175, 180, 181` | Disabled text |
| Gray500 | `#8B9192` | `139, 145, 146` | Secondary text |
| Gray600 | `#545859` | `84, 88, 89` | Body text |
| Gray700 | `#363A3A` | `54, 58, 58` | Emphasized text |
| Gray800 | `#27272A` | `39, 39, 42` | Headers |
| Gray900 | `#18181B` | `24, 24, 27` | Primary text |
| Gray950 | `#09090B` | `9, 9, 11` | Darkest backgrounds |

### Success - Green

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Green50 | `#EBFFF7` | `235, 255, 247` | Success backgrounds |
| Green100 | `#C8E6C9` | `200, 230, 201` | Success messages |
| Green200 | `#9AE6B4` | `154, 230, 180` | Light success |
| Green300 | `#68D391` | `104, 211, 145` | Medium success |
| Green400 | `#48BB78` | `72, 187, 120` | Active success |
| Green500 | `#24844E` | `36, 132, 78` | **Success primary** |
| Green600 | `#2F855A` | `47, 133, 90` | Success dark |
| Green700 | `#276749` | `39, 103, 73` | Success darker |
| Green800 | `#22543D` | `34, 84, 61` | Success darkest |
| Green900 | `#1C4532` | `28, 69, 50` | Success extreme |

### Warning - Orange

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Orange50 | `#FFF8F1` | `255, 248, 241` | Warning backgrounds |
| Orange100 | `#FFE7D6` | `255, 231, 214` | Warning light |
| Orange200 | `#FCD9BD` | `252, 217, 189` | Light warning |
| Orange300 | `#FAB38B` | `250, 179, 139` | Medium warning |
| Orange400 | `#F29D5D` | `242, 157, 93` | Active warning |
| Orange500 | `#E3660E` | `227, 102, 14` | **Warning / Spreedly orange** |
| Orange600 | `#BA5C18` | `186, 92, 24` | Warning dark |
| Orange700 | `#9C4D14` | `156, 77, 20` | Warning darker |
| Orange800 | `#7C2D12` | `124, 45, 18` | Warning darkest |
| Orange900 | `#6C2C12` | `108, 44, 18` | Warning extreme |

### Error - Red

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Red50 | `#FDF2F2` | `253, 242, 242` | Error backgrounds |
| Red100 | `#FDE8E8` | `253, 232, 232` | Light error |
| Red200 | `#FBD5D5` | `251, 213, 213` | Error messages |
| Red300 | `#F8B4B4` | `248, 180, 180` | Medium error |
| Red400 | `#F98080` | `249, 128, 128` | Active error |
| Red500 | `#F05252` | `240, 82, 82` | Error primary |
| Red600 | `#E02424` | `224, 36, 36` | **Error main** |
| Red700 | `#C70039` | `199, 0, 57` | **Spreedly red** |
| Red800 | `#911C1C` | `145, 28, 28` | **Spreedly dark red** |
| Red900 | `#771D1D` | `119, 29, 29` | Error darkest |

### Spreedly Brand Colors

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| SpreedlyRed | `#C70039` | `199, 0, 57` | Brand primary |
| SpreedlyOrange | `#E3660E` | `227, 102, 14` | Brand secondary |
| SpreedlyYellow | `#FFA23A` | `255, 162, 58` | Brand accent |

### Semantic Color Mapping

| Semantic Name | Maps To | Usage |
|---------------|---------|-------|
| `SuccessGreenLight` | Green50 | Success backgrounds |
| `SuccessMessageGreen` | Green100 | Success messages |
| `SuccessGreen` | Green500 | Success actions |
| `SpreedlyRed` | Red700 | Brand primary red |
| `SpreedlyDarkRed` | Red800 | Brand dark red |
| `SpreedlyOrange` | Orange500 | Brand primary orange |
| `WarningOrange` | Orange500 | Warning messages |
| `WarningOrangeLight` | Orange100 | Warning backgrounds |
| `DefaultBlue` | Blue600 | Default primary |
| `InfoBlue` | Blue200 | Info backgrounds |

## Typography

### Display Styles

Large, attention-grabbing text for hero sections and splash screens.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `displayLarge` | 57sp | Bold | Hero text |
| `displayMedium` | 45sp | Bold | Marketing content |
| `displaySmall` | 36sp | Bold | Section headers |

### Headline Styles

High-emphasis text for titles and headers.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `headlineLarge` | 32sp | SemiBold | Page titles |
| `headlineMedium` | 28sp | SemiBold | Section titles |
| `headlineSmall` | 24sp | SemiBold | Card titles |

### Title Styles

Medium-emphasis text for component titles.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `titleLarge` | 22sp | SemiBold | App bar titles |
| `titleMedium` | 16sp | Medium | List items |
| `titleSmall` | 14sp | Medium | Dialog titles |

### Body Styles

Primary content text.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `bodyLarge` | 16sp | Normal | Primary content |
| `bodyMedium` | 14sp | Normal | Secondary content |
| `bodySmall` | 12sp | Normal | Captions |

### Label Styles

Text for UI elements like buttons and labels.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `labelLarge` | 14sp | Medium | Buttons, tabs |
| `labelMedium` | 12sp | Medium | Input labels |
| `labelSmall` | 11sp | Medium | Helper text |

## Component Styling

### Spacing

All spacing is based on a 4dp base unit.

| Name | Value | Usage |
|------|-------|-------|
| `none` | 0dp | No spacing |
| `xxxs` | 2dp | Minimal spacing |
| `xxs` | 4dp | Extra extra small |
| `xs` | 8dp | Extra small |
| `sm` | 12dp | Small |
| `md` | 16dp | Medium (default) |
| `mlg` | 20dp | Medium-large |
| `lg` | 24dp | Large |
| `xl` | 32dp | Extra large |
| `xxl` | 40dp | Extra extra large |
| `xxxl` | 48dp | Extra extra extra large |
| `massive` | 64dp | Massive |

Semantic spacing tokens for common layout patterns:

| Name | Value | Usage |
|------|-------|-------|
| `screenPadding` | 16dp | Screen edges |
| `elementSpacing` | 8dp | Between elements |
| `sectionSpacing` | 24dp | Between sections |
| `cardPadding` | 12dp | Inside cards |
| `buttonPaddingHorizontal` | 8dp | Button horizontal |
| `buttonPaddingVertical` | 12dp | Button vertical |
| `iconSpacing` | 4dp | Icon to text |

### Shapes

| Shape | Radius | Usage |
|-------|--------|-------|
| `extraSmall` | 4dp | Chips, small buttons |
| `small` | 8dp | Buttons, text fields |
| `medium` | 12dp | Cards, dialogs |
| `large` | 16dp | Bottom sheets |
| `extraLarge` | 24dp | Modal dialogs |

Custom shapes:

- `circle` — Fully rounded
- `rounded` — 8dp corners
- `roundedLarge` — 16dp corners
- `topRounded` — Top corners only (16dp)
- `bottomRounded` — Bottom corners only (16dp)

## Dark Mode

The theme ships with light and dark color schemes. Always reference colors, typography, and shapes through `MaterialTheme` so components adapt automatically.

### Light Theme

| Role | Token |
|------|-------|
| Background | Gray50 |
| Surface | Gray50 |
| Primary | Blue600 |
| On Background | Gray900 |

### Dark Theme

| Role | Token |
|------|-------|
| Background | Gray900 |
| Surface | Gray800 |
| Primary | Blue600 |
| On Background | Gray100 |

Never hardcode colors. Use theme accessors:

```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onBackground
```

## Examples

### Colors

```kotlin
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.primary
)

Box(modifier = Modifier.background(MaterialTheme.colorScheme.error))
```

### Typography

```kotlin
Text(
    text = "Section Title",
    style = MaterialTheme.typography.headlineLarge
)
```

### Spacing

```kotlin
Column(
    modifier = Modifier.padding(Spacing.screenPadding),
    verticalArrangement = Arrangement.spacedBy(Spacing.elementSpacing)
) {
    // Content
}
```

### Shapes

```kotlin
Card(
    shape = MaterialTheme.shapes.medium
) {
    // Content
}
```

## Accessibility

All color combinations meet WCAG AA standards:

- Normal text: **4.5:1** minimum contrast ratio
- Large text (18sp+): **3:1** minimum contrast ratio

Provide `contentDescription` for icons and use semantic colors that work in both themes.

## Best Practices

1. **Always use theme tokens** — never hardcode colors or sizes.
2. **Follow Material Design 3** — stay consistent with platform conventions.
3. **Use semantic colors** — `success`, `warning`, `error` for state feedback.
4. **Test in both themes** — verify dark mode compatibility.
5. **Maintain spacing consistency** — use `Spacing.*` tokens throughout.
6. **Follow accessibility guidelines** — check contrast ratios.
7. **Use preview functions** — test components in isolation.

## Additional Resources

- [Material Design 3 Guidelines](https://m3.material.io/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3)
