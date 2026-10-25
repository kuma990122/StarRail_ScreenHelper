package com.screenhelper.presentation.pet

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.*
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.*
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import kotlin.math.abs

class PetService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var petView: PetView? = null
    private var params: WindowManager.LayoutParams? = null
    private lateinit var windowManager: WindowManager
    private var tapCount = 0
    private var lastTapAt = 0L
    private val tapAction = Runnable { tapCount = 0; touched() }
    private val hideBubble = Runnable { petView?.bubble = ""; petView?.invalidate() }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        val app = appContainer
        startForeground(1001, app.notifications.ongoing("栖伴正在陪伴你", "拖动调整位置 · 连续点三下打开备忘录"))
        windowManager = getSystemService(WindowManager::class.java)
        val density = resources.displayMetrics.density
        val width = (180 * density).toInt()
        val height = (240 * density).toInt()
        val display = resources.displayMetrics
        params = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = maxOf(0, display.widthPixels - width)
            y = display.heightPixels / 3
        }
        val view = PetView(this)
        petView = view
        installTouch(view)
        try { windowManager.addView(view, params) }
        catch (_: Exception) { stopSelf(); return }
        running.value = true
        app.presence(pet = true)
        serviceScope.launch {
            var lastBubble = Long.MIN_VALUE
            app.petState.collect { state ->
                val path = state.character.assets[state.emotion] ?: state.character.assets[Emotion.NEUTRAL]
                view.showPortrait(path, state.emotion)
                if (state.bubbleId != lastBubble) {
                    lastBubble = state.bubbleId
                    view.bubble = state.bubble
                    handler.removeCallbacks(hideBubble)
                    handler.postDelayed(hideBubble, 8000)
                }
                view.invalidate()
            }
        }
    }

    private fun installTouch(view: PetView) {
        var startX = 0f; var startY = 0f; var originX = 0; var originY = 0; var moved = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            val layout = params ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; startY = event.rawY; originX = layout.x; originY = layout.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX; val dy = event.rawY - startY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        handler.removeCallbacks(tapAction); tapCount = 0
                        layout.x = (originX + dx.toInt()).coerceIn(0, maxOf(0, resources.displayMetrics.widthPixels - layout.width))
                        layout.y = (originY + dy.toInt()).coerceIn(0, maxOf(0, resources.displayMetrics.heightPixels - layout.height))
                        windowManager.updateViewLayout(view, layout)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    if (!moved) {
                        val now = SystemClock.uptimeMillis()
                        tapCount = if (now - lastTapAt < 420) tapCount + 1 else 1
                        lastTapAt = now
                        handler.removeCallbacks(tapAction)
                        if (tapCount >= 3) {
                            tapCount = 0
                            appContainer.notifications.launchIntent("memo").send()
                        } else handler.postDelayed(tapAction, 440)
                    } else appContainer.scope.launch { appContainer.relationship.interact(appContainer.petState.value.character.id, "drag:${UUID.randomUUID()}", 1) }
                }
                MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(tapAction); tapCount = 0 }
            }
            true
        }
    }
    private fun touched() {
        val app = appContainer
        app.showBubble("被你发现啦，今天也请多关照。", Emotion.HAPPY)
        app.scope.launch {
            app.ready.await()
            val id = app.petState.value.character.id
            app.relationship.interact(id, "touch:${UUID.randomUUID()}", 1)
            if (app.settings.current().touchTalk) app.conversation.send(id, "（用户轻轻摸了摸你的头，请简短回应。）")
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        petView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
            it.clearPortrait()
        }
        petView = null
        running.value = false
        appContainer.presence(pet = false)
        super.onDestroy()
    }
    companion object {
        val running = MutableStateFlow(false)
        val isRunning get() = running.value
    }
}

private class PetView(context: android.content.Context) : View(context) {
    var emotion = Emotion.NEUTRAL
    var bubble = ""
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleTail = Path()
    private val portraitRect = RectF()
    private val portraitRenderer = OverlayPortraitRenderer(::invalidate)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(38,63,54); textSize = 12 * resources.displayMetrics.density }
    init { setLayerType(LAYER_TYPE_SOFTWARE, null); contentDescription = "栖伴桌宠，连续点三下打开备忘录" }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        if (bubble.isNotBlank()) {
            paint.color = Color.rgb(255,254,247); paint.setShadowLayer(6*d,0f,2*d,0x18344937)
            canvas.drawRoundRect(5*d,5*d,width-5*d,75*d,14*d,14*d,paint)
            paint.clearShadowLayer()
            bubbleTail.reset()
            bubbleTail.moveTo(110*d,74*d); bubbleTail.lineTo(122*d,84*d); bubbleTail.lineTo(127*d,74*d); bubbleTail.close()
            canvas.drawPath(bubbleTail,paint)
            val layout = StaticLayout.Builder.obtain(bubble,0,bubble.length,textPaint,(width-30*d).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
            canvas.save(); canvas.translate(15*d,13*d); layout.draw(canvas); canvas.restore()
        }
        portraitRect.set(12*d,79*d,width-12*d,height.toFloat())
        val rect = portraitRect
        portraitRenderer.draw(canvas, rect)
    }
    fun showPortrait(path: String?, state: Emotion) { emotion = state; portraitRenderer.show(path, state); invalidate() }
    fun clearPortrait() { portraitRenderer.close() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); portraitRenderer.attach() }
    override fun onDetachedFromWindow() { portraitRenderer.detach(); super.onDetachedFromWindow() }
    override fun performClick(): Boolean { super.performClick(); return true }
}
