package com.invatech.pockit.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Pockit Cozy Pastel Brand Palette ----

// Warm Cream Canvas
val WarmCream = Color(0xFFFFFBEF)             // Main screen light canvas
val WarmCreamSurface = Color(0xFFFAF4EA)      // Cards, sheets, and elevated elements
val WarmCreamMuted = Color(0xFFF3ECE0)        // Subtle containers, chips, input backgrounds
val WarmBorder = Color(0xFFEFE6DA)            // Soft warm border
val WarmDivider = Color(0xFFE8DFD3)           // Divider hairlines

// Cocoa & Espresso Typography & Dark Accents
val CocoaEspresso = Color(0xFF282A30)         // Primary text & dark accents
val CocoaSoft = Color(0xFF4E413B)             // Secondary text
val CocoaMuted = Color(0xFF93867F)            // Tertiary muted labels & timestamps
val CocoaFab = Color(0xFF5E4B45)              // Floating action button cocoa
val CocoaPill = Color(0xFF3F3530)             // Date chip pill background

// Backward-compatible aliases mapped to the warm brand palette
val Ink = CocoaEspresso
val InkSoft = CocoaSoft
val InkMuted = CocoaMuted
val Cloud = WarmCream
val CloudDeep = WarmCreamMuted

// Category Pastel Card Tints (as seen in the design mockup)
val PastelPeach = Color(0xFFFFE7D1)           // Food / Dining
val PastelMint = Color(0xFFDCF5E8)            // Greens / Groceries / Health
val PastelPink = Color(0xFFFFE3E8)            // Desserts / Sweets / Treats
val PastelSky = Color(0xFFDEF1FE)             // Car / Transport / Travel
val PastelApricot = Color(0xFFFFF0CE)         // Games / Leisure / Entertainment
val PastelLilac = Color(0xFFF5E3F7)           // Cosmetics / Shopping / Beauty
val PastelCyan = Color(0xFFDCF5F4)            // Sports / Fitness
val PastelYellow = Color(0xFFFFF6D6)          // Bills / Utilities

// Brand pastel aliases
val Mint = PockitTeal
val MintDeep = Color(0xFF52B69A)
val Lavender = PockitLavender
val LavenderDeep = Color(0xFF9E86C8)
val Peach = PastelPeach
val PeachDeep = Color(0xFFEAA65D)
val Rose = PastelPink

// Organic decorative background shapes
val BlobCoral = Color(0xFFF6A38E)
val BlobTeal = Color(0xFF8CD4E2)
val BlobHoney = Color(0xFFF7D58C)

// Semantic Financial & Transaction Colors
val IncomeGreen = Color(0xFF2E9B82)
val IncomeGreenContainer = Color(0xFFDCF5E8)
val ExpenseRed = Color(0xFFD64545)
val ExpenseRedDark = Color(0xFFD64545)
val ExpenseRedContainer = Color(0xFFFFE3E8)
val ErrorRed = Color(0xFFD64545)
val ErrorSoft = Color(0xFFFFE3E8)
val OwedColor = Color(0xFF2E9B82)

// Shimmer Palette for Skeleton Loaders
val ShimmerLightBase = Color(0xFFEFE8DD)
val ShimmerLightHighlight = Color(0xFFF9F5EE)
val ShimmerDarkBase = ShimmerLightBase
val ShimmerDarkHighlight = ShimmerLightHighlight

// Deprecated dark aliases mapped to warm brand colors for clean compilation
val DarkBackground = WarmCream
val DarkSurface = WarmCreamSurface
val DarkSurfaceElevated = WarmCreamMuted
val DarkSurfaceVariant = WarmCreamMuted
val DarkOutline = WarmBorder
val DarkOutlineVariant = WarmDivider
val DarkTextPrimary = CocoaEspresso
val DarkTextSecondary = CocoaSoft
val DarkTextMuted = CocoaMuted
val MintDark = MintDeep
val MintContainerDark = PastelMint
val LavenderDark = LavenderDeep
val LavenderContainerDark = PastelLilac
val PeachDark = PeachDeep
val PeachContainerDark = PastelPeach
val RoseDark = Color(0xFFE26D82)
val RoseContainerDark = PastelPink
