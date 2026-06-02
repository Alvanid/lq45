package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF050505)
val DarkSurface = Color(0xFF0F172A)
val DarkSurfacePressed = Color(0xFF1E293B)
val DarkBorder = Color(0x2294A3B8)

val PrimaryNeon = Color(0xFF3B82F6) // Electric blue
val AccentCyan = Color(0xFF60A5FA) // Hologram cyan
val ProfitGreen = Color(0xFF34D399) // Toxic Profit Green
val LossRed = Color(0xFFF43F5E) // Laser Loss Red
val NeutralOrange = Color(0xFFF97316) // Amber Warn

val TextPrimary = Color(0xFFE2E8F0)
val TextSecondary = Color(0xFF94A3B8)
val TextHint = Color(0xFF64748B)

// M3 Color Scheme compatibility definitions
val Purple80 = PrimaryNeon
val PurpleGrey80 = DarkSurface
val Pink80 = AccentCyan

val Purple40 = Color(0xFF2563EB)
val PurpleGrey40 = Color(0xFF1D4ED8)
val Pink40 = Color(0xFF3B82F6)

fun Modifier.immersiveBackground(): Modifier = this.drawBehind {
    // Top Left Blue glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1C2563EB), Color.Transparent),
            center = Offset(0f, 0f),
            radius = size.width * 0.8f
        ),
        radius = size.width * 0.8f,
        center = Offset(0f, 0f)
    )
    // Bottom Right Emerald glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1010B981), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width, size.height)
    )
}
