package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Rotina Viva Brand Colors (Material Design 3 Palette)
val SkyPrimary = Color(0xFF0284C7) // Sky 600
val SkyPrimaryContainer = Color(0xFFE0F2FE) // Sky 100
val SkyOnPrimary = Color(0xFFFFFFFF)
val SkyOnPrimaryContainer = Color(0xFF0369A1)

val SkyPrimaryDark = Color(0xFF38BDF8) // Sky 400
val SkyPrimaryContainerDark = Color(0xFF0369A1) // Sky 700
val SkyOnPrimaryDark = Color(0xFF082F49) // Sky 950

// Success, Warning, Danger (Strictly adhere to Rotina Viva semantics)
val StatusDone = Color(0xFF16A34A) // Pure Green 600 (Sucesso / Feito)
val StatusDoneDark = Color(0xFF22C55E) // Green 500 in dark mode
val StatusPartial = Color(0xFFF59E0B) // Amber 500 (Parcial)
val StatusPartialDark = Color(0xFFFBBF24) // Amber 400
val StatusSkipped = Color(0xFFEF4444) // Rose 500 (Pular / Falta)
val StatusSkippedDark = Color(0xFFF87171) // Rose 400

// Neutral Backgrounds & Surfaces (Glare-free light mode with light-gray cards and high text contrast)
val LightBackground = Color(0xFFE2E7ED) // Softer, non-aggressive background (avoids blinding white)
val LightSurface = Color(0xFFF1F5F9) // Light-gray card background
val LightSurfaceVariant = Color(0xFFDEE5ED) // Subtle inner card / container tone
val LightTextMain = Color(0xFF0F172A) // Deep charcoal navy (WCAG AAA contrast > 12:1)
val LightTextSec = Color(0xFF334155) // Deep Slate 700 for subtitles and secondary text (> 7:1 contrast)
val LightOutline = Color(0xFFCBD5E1) // Slate 300 card and divider border

val DarkBackground = Color(0xFF0B0F19) // Deep Night Blue
val DarkSurface = Color(0xFF1E293B) // Slate 800
val DarkSurfaceVariant = Color(0xFF111827) // Slate 900
val DarkSurfaceCardStep1 = Color(0xFF334155) // Slate 700 (Alternating depth steps)
val DarkTextMain = Color(0xFFF8FAFC) // Ice White
val DarkTextSec = Color(0xFF94A3B8) // Slate 400
