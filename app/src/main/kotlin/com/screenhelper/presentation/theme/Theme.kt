package com.screenhelper.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CanvasColor = Color(0xFFF6F7F1)
val Ink = Color(0xFF263F36)
val Muted = Color(0xFF7B8B80)
val Green = Color(0xFF356B55)
val Mint = Color(0xFFE4EDE0)
val Peach = Color(0xFFF3E0D2)
val Line = Color(0xFFE5E9DF)
val WarmWhite = Color(0xFFFFFEF9)

@Composable
fun ScreenHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, primaryContainer = Mint,
            background = CanvasColor, surface = WarmWhite, onSurface = Ink, onBackground = Ink,
            secondary = Color(0xFFB08C5B), outline = Line, error = Color(0xFFAA5146)),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
            labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
        ), content = content,
    )
}
