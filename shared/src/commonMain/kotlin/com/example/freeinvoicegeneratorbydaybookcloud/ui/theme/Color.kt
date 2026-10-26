package com.example.freeinvoicegeneratorbydaybookcloud.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — deeper ocean teal
val Primary        = Color(0xFF0D7A8E)
val PrimaryDark    = Color(0xFF005F70)
val OnPrimary      = Color(0xFFFFFFFF)
val PrimaryContainer    = Color(0xFFB2EBF8)
val OnPrimaryContainer  = Color(0xFF001F26)

// Neutral warm surface
val BackgroundLight = Color(0xFFF4F6FA)
val SurfaceLight    = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFECF0F5)

// Containers for depth hierarchy
val SurfaceContainerLowest  = Color(0xFFFFFFFF)
val SurfaceContainerLow     = Color(0xFFF4F6FA)
val SurfaceContainerDefault = Color(0xFFEEF1F6)
val SurfaceContainerHigh    = Color(0xFFE5E9EF)
val SurfaceContainerHighest = Color(0xFFDEE2EA)

val OnSurfaceLight   = Color(0xFF111827)
val OnSurfaceVariant = Color(0xFF5A6A7E)
val OutlineLight     = Color(0xFFD1D9E0)

// Template cards intentionally keep their light pastel backgrounds in both
// themes, so their foregrounds must not inherit dark-mode light text.
val TemplateCardTitle       = Color(0xFF102A43)
val TemplateCardDescription = Color(0xFF486581)

// Dark mode surfaces
val BackgroundDark   = Color(0xFF0D1117)
val SurfaceDark      = Color(0xFF161B22)
val SurfaceVariantDark = Color(0xFF21262D)
val OnSurfaceDark    = Color(0xFFE6EDF3)

// Status colors — light theme
val StatusPaidBg    = Color(0xFFD1FADF)
val StatusPaidText  = Color(0xFF027A48)
val StatusSentBg    = Color(0xFFD1E9FF)
val StatusSentText  = Color(0xFF175CD3)
val StatusDraftBg   = Color(0xFFF2F4F7)
val StatusDraftText = Color(0xFF344054)
val StatusOverdueBg = Color(0xFFFFE4E4)
val StatusOverdueText = Color(0xFFB91C1C)

// Status colors — dark theme
val StatusPaidBgDark    = Color(0xFF053321)
val StatusPaidTextDark  = Color(0xFF6CE9A6)
val StatusSentBgDark    = Color(0xFF102A4C)
val StatusSentTextDark  = Color(0xFF84CAFF)
val StatusDraftBgDark   = Color(0xFF1F242F)
val StatusDraftTextDark = Color(0xFF98A2B3)
val StatusOverdueBgDark = Color(0xFF491212)
val StatusOverdueTextDark = Color(0xFFFDA29B)

// Aliases kept for backward compat
val PrimaryTeal = Primary
val TealLight   = PrimaryContainer
val DarkNavy    = OnSurfaceLight
val MutedText   = OnSurfaceVariant
val BackgroundGray  = BackgroundLight
val SurfaceWhite    = SurfaceLight
val BorderGray      = OutlineLight
