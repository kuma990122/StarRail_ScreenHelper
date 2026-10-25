package com.screenhelper.presentation.theme

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.screenhelper.model.*
import com.screenhelper.presentation.pet.AnimatedPortraitView

@Composable
fun Portrait(profile: CharacterProfile, modifier: Modifier = Modifier, emotion: Emotion = Emotion.NEUTRAL) {
    val path = profile.assets[emotion] ?: profile.assets[Emotion.NEUTRAL]
    AndroidView(
        modifier = modifier,
        factory = { AnimatedPortraitView(it) },
        update = { view -> view.show(path, emotion); view.contentDescription = "${profile.name}的${emotion.label}动态立绘" },
    )
}

@Composable
fun Glyph(name: String, modifier: Modifier = Modifier.size(22.dp), color: Color = Ink) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        drawIntoCanvas { composeCanvas ->
            val c = composeCanvas.nativeCanvas
            val save = c.save()
            c.scale(size.width / 24f, size.height / 24f)
            paint.color = color.toArgb(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.7f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
            fun line(x: Float,y: Float,x2: Float,y2: Float) = c.drawLine(x,y,x2,y2,paint)
            fun path(block: Path.() -> Unit) = c.drawPath(Path().apply(block),paint)
            when (name) {
                "home" -> { path { moveTo(3f,11f); lineTo(12f,3f); lineTo(21f,11f); moveTo(5f,10f); lineTo(5f,21f); lineTo(19f,21f); lineTo(19f,10f) }; c.drawRoundRect(10f,15f,14f,21f,1f,1f,paint) }
                "chat" -> path { moveTo(6f,4f); lineTo(18f,4f); quadTo(21f,4f,21f,7f); lineTo(21f,15f); quadTo(21f,18f,18f,18f); lineTo(9f,18f); lineTo(4f,21f); lineTo(4f,7f); quadTo(4f,4f,6f,4f); moveTo(8f,10f); lineTo(16f,10f); moveTo(8f,14f); lineTo(13f,14f) }
                "memo" -> { c.drawRoundRect(5f,4f,19f,21f,3f,3f,paint); line(9f,3f,15f,3f); line(9f,10f,15f,10f); line(9f,14f,15f,14f) }
                "user" -> { c.drawCircle(12f,8f,4f,paint); path { moveTo(4f,21f); cubicTo(4f,13f,20f,13f,20f,21f) } }
                "settings" -> { c.drawCircle(12f,12f,3.5f,paint); c.drawCircle(12f,12f,8f,paint); line(12f,1f,12f,4f); line(12f,20f,12f,23f); line(1f,12f,4f,12f); line(20f,12f,23f,12f) }
                "plus" -> { line(12f,5f,12f,19f); line(5f,12f,19f,12f) }
                "close" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
                "send" -> path { moveTo(3f,3f); lineTo(22f,12f); lineTo(3f,21f); lineTo(7f,12f); close(); moveTo(7f,12f); lineTo(16f,12f) }
                "image" -> { c.drawRoundRect(3f,4f,21f,20f,3f,3f,paint); c.drawCircle(8f,9f,1.5f,paint); path { moveTo(4f,18f); lineTo(10f,12f); lineTo(14f,16f); lineTo(17f,13f); lineTo(21f,18f) } }
                "mic" -> { c.drawRoundRect(9f,3f,15f,15f,3f,3f,paint); path { moveTo(6f,11f); cubicTo(6f,21f,18f,21f,18f,11f) }; line(12f,19f,12f,22f); line(9f,22f,15f,22f) }
                "screen" -> { c.drawRoundRect(3f,4f,21f,18f,2f,2f,paint); line(12f,18f,12f,22f); line(8f,22f,16f,22f); line(7f,8f,11f,8f); line(7f,8f,7f,12f) }
                "heart" -> path { moveTo(12f,21f); cubicTo(-3f,12f,4f,-1f,12f,7f); cubicTo(20f,-1f,27f,12f,12f,21f) }
                "check" -> path { moveTo(5f,12f); lineTo(10f,17f); lineTo(20f,6f) }
                "arrow" -> path { moveTo(5f,12f); lineTo(19f,12f); moveTo(14f,7f); lineTo(19f,12f); lineTo(14f,17f) }
                else -> { c.drawCircle(12f,12f,8f,paint); line(12f,7f,12f,12f); line(12f,12f,16f,14f) }
            }
            c.restoreToCount(save)
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = WarmWhite, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = color) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable
fun Eyebrow(text: String) { Text(text, fontSize = 10.sp, letterSpacing = 2.sp, color = Muted, fontWeight = FontWeight.SemiBold) }

@Composable
fun PageHeading(eyebrow: String, title: String, subtitle: String) {
    Column(Modifier.padding(top = 10.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Tag(text: String, color: Color = Mint) {
    Text(text, color = Green, fontSize = 11.sp, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 5.dp))
}

@Composable
fun FormField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, minLines: Int = 1) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, minLines = minLines,
        shape = RoundedCornerShape(14.dp), singleLine = minLines == 1)
}
