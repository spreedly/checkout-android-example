package com.spreedly.example.qa

internal const val INSPECTOR_TITLE = "Field state inspector"
internal const val INSPECTOR_CAPTION =
    "Updates from onFieldStateChange. Use snapshot fields — not hostedCardDisplayState in the callback."
internal const val INSPECTOR_WIRING_CAPTION =
    "PAN + CVC follow setNumberFormat / toggleMask (iframe parity)"
internal const val GLOBAL_DISPLAY_TITLE = "Global hostedCardDisplayState"
internal const val EVENT_LOG_TITLE = "Event log (last 5)"
internal const val CARD_PANEL_TITLE = "Card number"
internal const val CVV_PANEL_TITLE = "CVC"
internal const val CARD_EMPTY_HINT = "Type a card number or change the format picker above."
internal const val CVV_EMPTY_HINT = "Type a security code. isPanMasked is not used on CVC events."
