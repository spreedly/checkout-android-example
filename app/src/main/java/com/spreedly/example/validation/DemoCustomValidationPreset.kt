package com.spreedly.example.validation

/**
 * Demo-only preset rules merchants can assign per optional hosted field.
 */
enum class DemoCustomValidationPreset(val label: String) {
    NONE("None"),
    MIN_LENGTH_3("Min length 3"),
    NO_DIGITS("No digits"),
    DIGITS_ONLY("Digits only"),
    NO_SPECIAL_CHARACTERS("No special characters"),
    ONLY_WHEN_US("Only when country = US"),
    ALWAYS_INVALID("Always invalid"),
}
