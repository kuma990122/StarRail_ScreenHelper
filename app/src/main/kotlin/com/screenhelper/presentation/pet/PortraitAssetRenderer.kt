package com.screenhelper.presentation.pet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.screenhelper.model.Emotion
import java.io.File

/** Decodes user portraits off the UI thread and keeps animated frames local to the view. */
internal class PortraitAssetRenderer(
    private val invalidate: () -> Unit,
    private val maxDimension: Int,
) : Drawable.Callback {
    private val main = Handler(Looper.getMainLooper())
    private var currentPath: String? = null
    private var generation = 0
    private var bitmap: Bitmap? = null
    private var movie: Movie? = null
    private var drawable: Drawable? = null
    private var attached = false
    private var movieScheduled = false
    private var movieStartedAt = 0L
    private val movieFrame = object : Runnable {
        override fun run() {
            if (!attached || movie == null) return
            invalidate()
            main.postDelayed(this, 33)
            movieScheduled = true
        }
    }

    fun setImage(path: String?) {
        if (path == currentPath) return
        currentPath = path
        val request = ++generation
        clearDecoded()
        if (path == null) { invalidate(); return }
        Thread({
            val decoded = runCatching { decode(File(path)) }.getOrNull()
            main.post {
                if (request != generation) {
                    decoded?.bitmap?.recycle()
                    return@post
                }
                bitmap = decoded?.bitmap
                movie = decoded?.movie
                drawable = decoded?.drawable?.also { it.callback = this }
                movieStartedAt = SystemClock.uptimeMillis()
                startAnimation()
                invalidate()
            }
        }, "portrait-decode").start()
    }

    fun onAttached() {
        attached = true
        startAnimation()
        if (movie != null) {
            main.removeCallbacks(movieFrame)
            main.post(movieFrame)
            movieScheduled = true
        }
    }

    fun onDetached() {
        attached = false
        main.removeCallbacks(movieFrame)
        movieScheduled = false
        stopAnimation()
    }

    fun clear() {
        generation++
        currentPath = null
        onDetached()
        clearDecoded()
    }

    fun draw(canvas: Canvas, bounds: RectF, emotion: Emotion) {
        val image = bitmap
        val animation = movie
        val imageDrawable = drawable
        when {
            image != null && !image.isRecycled -> drawBitmap(canvas, image, bounds)
            animation != null -> drawMovie(canvas, animation, bounds)
            imageDrawable != null -> {
                val sourceWidth = imageDrawable.intrinsicWidth.coerceAtLeast(1).toFloat()
                val sourceHeight = imageDrawable.intrinsicHeight.coerceAtLeast(1).toFloat()
                val scale = minOf(bounds.width() / sourceWidth, bounds.height() / sourceHeight)
                val width = sourceWidth * scale
                val height = sourceHeight * scale
                val left = (bounds.centerX() - width / 2).toInt()
                val top = (bounds.centerY() - height / 2).toInt()
                imageDrawable.setBounds(left, top, (left + width).toInt(), (top + height).toInt())
                imageDrawable.draw(canvas)
            }
            else -> MascotArt.draw(canvas, bounds, emotion)
        }
    }

    override fun invalidateDrawable(who: Drawable) = invalidate()
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        if (attached) main.postAtTime(what, this, `when`)
    }
    override fun unscheduleDrawable(who: Drawable, what: Runnable) = main.removeCallbacks(what, this)

    private fun decode(file: File): Decoded? {
        if (!file.isFile) return null
        if (file.extension.equals("gif", true)) return Movie.decodeFile(file.path)?.let { Decoded(movie = it) }
        if (file.extension.equals("webp", true) && Build.VERSION.SDK_INT >= 28) {
            return decodeWithImageDecoder(file)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.let { Decoded(bitmap = it) }
    }

    @androidx.annotation.RequiresApi(28)
    private fun decodeWithImageDecoder(file: File): Decoded? {
        val source = android.graphics.ImageDecoder.createSource(file)
        val result = android.graphics.ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
            decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        }
        if (result is Animatable) return Decoded(drawable = result)
        if (result is BitmapDrawable) return Decoded(bitmap = result.bitmap)
        return Decoded(drawable = result)
    }

    private fun drawBitmap(canvas: Canvas, image: Bitmap, bounds: RectF) {
        val scale = minOf(bounds.width() / image.width, bounds.height() / image.height)
        val width = image.width * scale
        val height = image.height * scale
        val target = RectF(bounds.centerX() - width / 2, bounds.centerY() - height / 2, bounds.centerX() + width / 2, bounds.centerY() + height / 2)
        canvas.drawBitmap(image, null, target, null)
    }

    private fun drawMovie(canvas: Canvas, animation: Movie, bounds: RectF) {
        val duration = animation.duration().coerceAtLeast(1)
        animation.setTime(((SystemClock.uptimeMillis() - movieStartedAt) % duration).toInt())
        val scale = minOf(bounds.width() / animation.width(), bounds.height() / animation.height())
        canvas.save()
        canvas.translate(bounds.centerX() - animation.width() * scale / 2, bounds.centerY() - animation.height() * scale / 2)
        canvas.scale(scale, scale)
        animation.draw(canvas, 0f, 0f)
        canvas.restore()
        if (attached && duration > 1 && !movieScheduled) {
            main.post(movieFrame)
            movieScheduled = true
        }
    }

    private fun startAnimation() { (drawable as? Animatable)?.start() }
    private fun stopAnimation() { (drawable as? Animatable)?.stop() }
    private fun clearDecoded() {
        main.removeCallbacks(movieFrame)
        movieScheduled = false
        stopAnimation()
        drawable?.callback = null
        drawable = null
        movie = null
        bitmap?.recycle()
        bitmap = null
    }

    private data class Decoded(val bitmap: Bitmap? = null, val movie: Movie? = null, val drawable: Drawable? = null)
}

internal class AnimatedPortraitView(context: android.content.Context) : android.view.View(context) {
    private val renderer = PortraitAssetRenderer(::invalidate, 1024)
    private var emotion = Emotion.NEUTRAL

    fun show(path: String?, state: Emotion) {
        emotion = state
        renderer.setImage(path)
        contentDescription = "${state.label}动态立绘"
        invalidate()
    }

    fun clear() = renderer.clear()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, RectF(0f, 0f, width.toFloat(), height.toFloat()), emotion)
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); renderer.onAttached() }
    override fun onDetachedFromWindow() { renderer.onDetached(); super.onDetachedFromWindow() }
}

internal class OverlayPortraitRenderer(invalidate: () -> Unit) : AutoCloseable {
    private val renderer = PortraitAssetRenderer(invalidate, 512)
    fun show(path: String?, emotion: Emotion) { renderer.setImage(path); this.emotion = emotion }
    private var emotion = Emotion.NEUTRAL
    fun draw(canvas: Canvas, bounds: RectF) = renderer.draw(canvas, bounds, emotion)
    fun attach() = renderer.onAttached()
    fun detach() = renderer.onDetached()
    override fun close() = renderer.clear()
}
