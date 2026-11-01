package com.screenhelper.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

val CanvasColor: Color @Composable get() = MaterialTheme.colorScheme.background
val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Green: Color @Composable get() = MaterialTheme.colorScheme.primary
val Mint: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
val Peach: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
val Line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val WarmWhite: Color @Composable get() = MaterialTheme.colorScheme.surface

fun isNightTheme(hour: Int): Boolean = hour >= 19 || hour < 6

@Composable
fun ScreenHelperTheme(hourOverride: Int? = null, content: @Composable () -> Unit) {
    val hour = produceState(LocalTime.now().hour) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            delay(Duration.between(now, nextMinute).toMillis().coerceAtLeast(250L))
            value = LocalTime.now().hour
        }
    }.value
    val night = isNightTheme(hourOverride ?: hour)
    val colors = if (night) darkColorScheme(
        primary = Color(0xFFE2A5CF), onPrimary = Color(0xFF39263A), primaryContainer = Color(0xFF553B55),
        secondary = Color(0xFFDFC0E1), secondaryContainer = Color(0xFF443348),
        tertiary = Color(0xFFEFACC0), tertiaryContainer = Color(0xFF4B3343),
        background = Color(0xFF211D2B), surface = Color(0xFF2B2536), surfaceVariant = Color(0xFF393143),
        onSurface = Color(0xFFF4EDF4), onSurfaceVariant = Color(0xFFC0B2C2), onBackground = Color(0xFFF4EDF4),
        outline = Color(0xFF76667B), outlineVariant = Color(0xFF4A4052), error = Color(0xFFFFB4AB),
    ) else lightColorScheme(
        primary = Color(0xFFAA6D8D), onPrimary = Color.White, primaryContainer = Color(0xFFF0DDE8),
        secondary = Color(0xFF9B7896), secondaryContainer = Color(0xFFF1E7F1),
        tertiary = Color(0xFFC67B91), tertiaryContainer = Color(0xFFF8E4E9),
        background = Color(0xFFFAF6F8), surface = Color(0xFFFFFBFD), surfaceVariant = Color(0xFFF3ECF1),
        onSurface = Color(0xFF493D49), onSurfaceVariant = Color(0xFF837583), onBackground = Color(0xFF493D49),
        outline = Color(0xFFCDBFCB), outlineVariant = Color(0xFFE6DDE5), error = Color(0xFFB3261E),
    )
    val view = LocalView.current
    SideEffect {
        var context = view.context
        while (context is android.content.ContextWrapper && context !is android.app.Activity) context = context.baseContext
        (context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars = !night
                isAppearanceLightNavigationBars = !night
            }
            if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
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
