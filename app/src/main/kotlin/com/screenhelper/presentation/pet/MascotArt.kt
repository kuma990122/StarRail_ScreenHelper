package com.screenhelper.presentation.pet

import android.graphics.*
import com.screenhelper.model.Emotion

/** Original procedural illustration; shared by Compose and the lightweight overlay. */
object MascotArt {
    fun draw(canvas: Canvas, bounds: RectF, emotion: Emotion = Emotion.NEUTRAL) {
        val save = canvas.save()
        val scale = minOf(bounds.width() / 240f, bounds.height() / 280f)
        canvas.translate(bounds.centerX() - 120 * scale, bounds.centerY() - 140 * scale)
        canvas.scale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun color(hex: String) { paint.color = Color.parseColor(hex); paint.style = Paint.Style.FILL }
        fun oval(l: Float, t: Float, r: Float, b: Float, hex: String) { color(hex); canvas.drawOval(l,t,r,b,paint) }
        fun path(hex: String, block: Path.() -> Unit) { color(hex); canvas.drawPath(Path().apply(block),paint) }
        fun stroke(hex: String, width: Float = 3f, block: Path.() -> Unit) {
            color(hex); paint.style = Paint.Style.STROKE; paint.strokeWidth = width; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(Path().apply(block), paint); paint.style = Paint.Style.FILL
        }
        oval(44f,245f,204f,263f,"#D5DFD1")
        path("#F1D897") { moveTo(169f,198f); cubicTo(231f,177f,224f,254f,164f,242f); cubicTo(201f,235f,202f,215f,169f,218f); close() }
        path("#F5EFE0") { moveTo(84f,163f); cubicTo(60f,186f,66f,243f,97f,248f); lineTo(155f,248f); cubicTo(184f,232f,177f,187f,157f,166f); close() }
        oval(76f,230f,116f,254f,"#F8F4E9"); oval(132f,230f,172f,254f,"#F8F4E9")
        path("#F4EBD6") { moveTo(64f,99f); cubicTo(53f,62f,65f,23f,78f,22f); cubicTo(91f,25f,104f,52f,108f,63f); lineTo(141f,65f); cubicTo(153f,40f,170f,26f,180f,29f); cubicTo(195f,34f,187f,80f,179f,102f); close() }
        path("#E9B7A2") { moveTo(72f,77f); quadTo(66f,49f,77f,38f); quadTo(87f,46f,94f,68f); close() }
        path("#E9B7A2") { moveTo(155f,72f); quadTo(170f,45f,178f,44f); quadTo(183f,61f,174f,82f); close() }
        path("#FCF8EA") { moveTo(66f,87f); cubicTo(84f,61f,149f,57f,176f,85f); cubicTo(197f,106f,194f,146f,173f,168f); cubicTo(150f,188f,90f,186f,66f,163f); cubicTo(44f,143f,46f,108f,66f,87f); close() }
        path("#D6E3CF") { moveTo(101f,70f); quadTo(117f,48f,139f,57f); quadTo(130f,60f,130f,73f); quadTo(118f,66f,111f,82f); close() }
        oval(65f,132f,87f,144f,"#F0CBBA"); oval(157f,132f,179f,144f,"#F0CBBA")
        when (emotion) {
            Emotion.HAPPY -> {
                stroke("#344C40",4f) { moveTo(88f,121f); quadTo(95f,110f,102f,121f) }
                stroke("#344C40",4f) { moveTo(144f,121f); quadTo(151f,110f,158f,121f) }
            }
            Emotion.ANGRY -> {
                stroke("#344C40",4f) { moveTo(85f,106f); lineTo(104f,115f) }
                stroke("#344C40",4f) { moveTo(142f,115f); lineTo(161f,106f) }
                oval(93f,118f,100f,129f,"#344C40"); oval(147f,118f,154f,129f,"#344C40")
            }
            Emotion.SAD -> {
                stroke("#344C40",3f) { moveTo(85f,109f); lineTo(103f,103f) }
                stroke("#344C40",3f) { moveTo(143f,103f); lineTo(161f,109f) }
                oval(93f,118f,101f,130f,"#344C40"); oval(147f,118f,155f,130f,"#344C40")
                path("#B6D3D5") { moveTo(160f,136f); quadTo(147f,151f,160f,154f); quadTo(172f,151f,160f,136f); close() }
            }
            Emotion.THINKING -> {
                oval(92f,114f,100f,127f,"#344C40")
                stroke("#344C40",3.5f) { moveTo(145f,121f); lineTo(159f,118f) }
            }
            else -> { oval(92f,114f,100f,127f,"#344C40"); oval(147f,114f,155f,127f,"#344C40") }
        }
        path("#C58776") { moveTo(117f,134f); quadTo(123f,131f,129f,134f); lineTo(123f,140f); close() }
        if (emotion == Emotion.SAD) stroke("#725E4B",2.2f) { moveTo(117f,148f); quadTo(123f,143f,130f,148f) }
        else stroke("#725E4B",2.2f) { moveTo(114f,144f); quadTo(119f,150f,123f,144f); quadTo(128f,150f,133f,144f) }
        path("#477562") { moveTo(78f,165f); quadTo(121f,186f,165f,164f); lineTo(169f,180f); quadTo(124f,200f,78f,181f); close() }
        path("#365F50") { moveTo(147f,184f); lineTo(158f,213f); lineTo(141f,210f); lineTo(132f,188f); close() }
        oval(112f,176f,132f,196f,"#E5C879")
        stroke("#F8E8AA",1.8f) { moveTo(122f,180f); lineTo(122f,190f); moveTo(117f,185f); lineTo(127f,185f) }
        oval(71f,194f,92f,228f,"#FBF6E7"); oval(154f,194f,175f,228f,"#FBF6E7")
        fun star(x: Float, y: Float, size: Float) { path("#D6BC72") { moveTo(x,y-size); quadTo(x+2,y-2,x+size,y); quadTo(x+2,y+2,x,y+size); quadTo(x-2,y+2,x-size,y); quadTo(x-2,y-2,x,y-size); close() } }
        star(40f,101f,8f); star(205f,63f,10f); star(195f,159f,5f)
        oval(33f,162f,38f,167f,"#99B49B"); oval(208f,112f,214f,118f,"#99B49B")
        canvas.restoreToCount(save)
    }
}
