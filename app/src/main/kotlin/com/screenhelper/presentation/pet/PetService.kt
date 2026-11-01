package com.screenhelper.presentation.pet

import android.app.Service
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.*
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.business.pet.*
import com.screenhelper.model.*
import com.screenhelper.data.platform.media.PetSound
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.util.UUID
import kotlin.math.abs

class PetService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var petView: PetView? = null
    private var params: WindowManager.LayoutParams? = null
    private var bubbleView: BubbleView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var host = PetHostState()
    private var desktopX = 0
    private var desktopY = 0
    private var desktopDock = PetAction.IDLE
    private lateinit var windowManager: WindowManager
    private lateinit var overlayContext: Context
    private lateinit var displayManager: DisplayManager
    private var displayGeneration = 0
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                displayGeneration++
                handler.post { updateGeometry() }
            }
        }
    }
    private val windowConfiguration = object : ComponentCallbacks {
        override fun onConfigurationChanged(config: android.content.res.Configuration) { handler.post { updateGeometry() } }
        override fun onLowMemory() {}
    }
    private var tapCount = 0
    private var lastTapAt = 0L
    private var lastInteraction = SystemClock.elapsedRealtime()
    private var lastGreeting: Long? = null
    private var action = PetAction.IDLE
    private var dock = PetAction.IDLE
    private var actionUntil = 0L
    private var dragging = false
    private var reminder = false
    private var screenOn = true
    private var lastProfile = ""
    private val tapAction = Runnable { tapCount = 0; touched() }
    private val hideBubble = Runnable { bubbleView?.bubble = ""; if(reminder) appContainer.petState.update { it.copy(reminderId=null) }; reminder = false; actionUntil = 0; restore(); positionBubble() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                appContainer.sounds.stop()
                screenOn = false; petView?.pause(); appContainer.presence(pet=false)
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                screenOn = true; petView?.resume(); appContainer.presence(pet=true)
                val now = SystemClock.elapsedRealtime()
                if (!reminder && PetPolicy.greetingAllowed(now,lastGreeting)) {
                    lastGreeting = now; lastInteraction = now
                    appContainer.showBubble(listOf("又见面啦，今天也一起写下故事吧♪", "你回来啦，人家一直在这里。", "亮起来的，还有我们的新故事♪").random())
                    play(PetAction.WAKE,5000)
                    appContainer.sounds.play(PetSound.GREETING)
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        startForeground(1001,appContainer.notifications.ongoing("迷迷正在陪伴你","单击回应 · 双击主页 · 提醒时双击待办"))
        displayManager = getSystemService(DisplayManager::class.java)
        overlayContext = if(Build.VERSION.SDK_INT>=30) {
            createDisplayContext(displayManager.getDisplay(Display.DEFAULT_DISPLAY))
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null)
        } else this
        windowManager = overlayContext.getSystemService(WindowManager::class.java)
        displayManager.registerDisplayListener(displayListener,handler)
        if(overlayContext !== this) overlayContext.registerComponentCallbacks(windowConfiguration)
        screenOn = getSystemService(PowerManager::class.java).isInteractive
        androidx.core.content.ContextCompat.registerReceiver(this,receiver,IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        },androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        val screen = screenSize()
        params = windowParams(PetPolicy.windowSize(screen.width,screen.height,false),false).apply { x=screen.width-width; y=screen.height/3 }
        desktopX=params!!.x; desktopY=params!!.y
        bubbleParams=windowParams(PetPolicy.bubbleSize(screen.width),true)
        val view = PetView(overlayContext)
        petView = view
        bubbleView=BubbleView(overlayContext).apply { visibility=View.GONE }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { updateGeometry() }
            override fun onViewDetachedFromWindow(v: View) {}
        })
        installTouch(view)
        try { windowManager.addView(view,params); windowManager.addView(bubbleView,bubbleParams) } catch (_: Exception) { stopSelf(); return }
        running.value = true
        appContainer.presence(pet=screenOn)
        serviceScope.launch {
            PetHost.state.collect { next ->
                if(next.appVisible != host.appVisible) {
                    handler.removeCallbacks(tapAction); tapCount=0; dragging=false
                    if(next.appVisible) {
                        desktopX=params!!.x; desktopY=params!!.y; desktopDock=dock; dock=PetAction.IDLE
                    } else { params!!.x=desktopX; params!!.y=desktopY; dock=desktopDock }
                    actionUntil=0
                }
                host=next; updateGeometry(); restore()
            }
        }
        serviceScope.launch {
            appContainer.ready.await()
            var lastBubble = Long.MIN_VALUE
            appContainer.petState.collect { state ->
                val profileKey = state.character.id + state.character.assets.toString()
                if (profileKey != lastProfile) { lastProfile = profileKey; render() }
                if (state.bubbleId != lastBubble) {
                    lastBubble = state.bubbleId
                    bubbleView?.bubble = state.bubble
                    reminder = state.reminderId != null
                    if (reminder) play(PetAction.REMINDER,10_000)
                    handler.removeCallbacks(hideBubble)
                    handler.postDelayed(hideBubble,10_000)
                    positionBubble()
                }
                view.invalidate()
            }
        }
        serviceScope.launch {
            appContainer.ready.await()
            render()
            while (isActive) {
                delay(1000)
                if (!screenOn || dragging || actionUntil > SystemClock.elapsedRealtime()) continue
                restore()
            }
        }
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        displayGeneration++
        updateGeometry()
    }
    @Suppress("DEPRECATION")
    private fun screenSize(): PetSize {
        if(Build.VERSION.SDK_INT>=30) return windowManager.maximumWindowMetrics.bounds.let { PetSize(it.width(),it.height()) }
        val metrics=android.util.DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return PetSize(metrics.widthPixels,metrics.heightPixels)
    }
    @Suppress("RtlHardcoded") // Overlay drag coordinates refer to physical screen edges in every locale.
    private fun windowParams(size: PetSize,bubble: Boolean)=WindowManager.LayoutParams(size.width,size.height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if(bubble) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0),PixelFormat.TRANSLUCENT).apply {
        gravity=Gravity.TOP or Gravity.LEFT
        if(bubble) alpha=.8f
        if(Build.VERSION.SDK_INT>=30) {
            layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
    }
    private fun maxX() = maxOf(0,screenSize().width-(params?.width ?: 0))
    private fun maxY() = maxOf(0,screenSize().height-(params?.height ?: 0))
    private fun updateGeometry() {
        val p=params ?: return
        val screen=screenSize()
        val size=PetPolicy.windowSize(screen.width,screen.height,host.appVisible,dock==PetAction.BOTTOM)
        p.width=size.width; p.height=size.height
        val anchor=host.homeAnchor.takeIf { host.appVisible }
        if(anchor!=null) {
            p.x=(anchor.centerX-p.width/2f).toInt(); p.y=(anchor.centerY-p.height/2f).toInt()
        } else when(dock) {
            PetAction.LEFT -> p.x=0
            PetAction.RIGHT -> p.x=maxX()
            PetAction.BOTTOM -> p.y=maxY()
            else -> {}
        }
        p.x=p.x.coerceIn(0,maxX()); p.y=p.y.coerceIn(0,maxY())
        petView?.apply {
            edgeX=when(dock) { PetAction.LEFT -> -1; PetAction.RIGHT -> 1; else -> 0 }
            edgeY=when { dock==PetAction.BOTTOM -> 1; p.y==0 -> -1; else -> 0 }
            // addView has a ViewRoot before the first attachment traversal completes.
            if(parent!=null) windowManager.updateViewLayout(this,p)
            invalidate()
        }
        positionBubble()
    }
    private fun positionBubble() {
        val view=bubbleView ?: return
        val p=params ?: return
        val bp=bubbleParams ?: return
        val screen=screenSize()
        val size=PetPolicy.bubbleSize(screen.width,!host.appVisible && dock==PetAction.BOTTOM)
        bp.width=size.width; bp.height=size.height
        bp.x=(p.x+p.width/2-bp.width/2).coerceIn(0,(screen.width-bp.width).coerceAtLeast(0))
        bp.y=(if(p.y>=bp.height) p.y-bp.height else p.y+p.height).coerceIn(0,(screen.height-bp.height).coerceAtLeast(0))
        view.visibility=if(view.bubble.isBlank()) View.GONE else View.VISIBLE
        if(view.parent!=null) windowManager.updateViewLayout(view,bp)
        view.invalidate()
    }
    private fun play(next: PetAction, duration: Long = 0) {
        if(next==PetAction.SLEEP && action!=PetAction.SLEEP) appContainer.sounds.play(PetSound.SLEEP)
        action = next; actionUntil = SystemClock.elapsedRealtime()+duration; render()
    }
    private fun restore() {
        val next = when {
            reminder -> PetAction.REMINDER
            PetPolicy.sleeping(LocalTime.now().hour,SystemClock.elapsedRealtime()-lastInteraction) -> PetAction.SLEEP
            else -> dock
        }
        if (next != action) play(next)
    }
    private fun render() {
        val state = appContainer.petState.value
        val path = if (state.character.id == "default") appContainer.petAssets.path(action)
            else state.character.assets[Emotion.NEUTRAL]
        petView?.showPortrait(path,Emotion.NEUTRAL)
        petView?.contentDescription = "迷迷桌宠，单击回应，双击主页，提醒时双击待办，状态：${action.name}"
    }
    private fun installTouch(view: PetView) {
        var startX=0f; var startY=0f; var originX=0; var originY=0; var moved=false
        var touchGeneration=0
        val slop=ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _,event ->
            val layout=params ?: return@setOnTouchListener false
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX=event.rawX; startY=event.rawY; originX=layout.x; originY=layout.y; moved=false
                    touchGeneration=displayGeneration
                    lastInteraction=SystemClock.elapsedRealtime()
                }
                MotionEvent.ACTION_MOVE -> {
                    if(touchGeneration!=displayGeneration) {
                        touchGeneration=displayGeneration
                        startX=event.rawX; startY=event.rawY; originX=layout.x; originY=layout.y
                    }
                    val dx=event.rawX-startX; val dy=event.rawY-startY
                    if (!moved && (abs(dx)>slop || abs(dy)>slop)) {
                        moved=true; dragging=true; play(PetAction.DRAG); appContainer.sounds.play(PetSound.DRAG)
                    }
                    if (moved) {
                        handler.removeCallbacks(tapAction); tapCount=0
                        if(!(host.appVisible && host.homeAnchor!=null)) {
                            layout.x=(originX+dx.toInt()).coerceIn(0,maxX()); layout.y=(originY+dy.toInt()).coerceIn(0,maxY())
                            windowManager.updateViewLayout(view,layout); positionBubble()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick(); lastInteraction=SystemClock.elapsedRealtime()
                    if (!moved) {
                        val now=SystemClock.uptimeMillis()
                        tapCount=if(now-lastTapAt<ViewConfiguration.getDoubleTapTimeout()) tapCount+1 else 1
                        lastTapAt=now; handler.removeCallbacks(tapAction)
                        if(tapCount>=2) {
                            tapCount=0
                            appContainer.notifications.launchIntent(if(reminder) "memo" else "home").send()
                        } else handler.postDelayed(tapAction,ViewConfiguration.getDoubleTapTimeout().toLong()+30)
                    } else {
                        dragging=false
                        dock=if(host.appVisible) PetAction.IDLE else PetPolicy.dock(layout.x,layout.y,maxX(),maxY(),(layout.width*.15f).toInt())
                        updateGeometry()
                        play(if(reminder) PetAction.REMINDER else dock)
                        if(dock==PetAction.LEFT || dock==PetAction.RIGHT) appContainer.sounds.play(PetSound.DOCK_SIDE)
                        appContainer.scope.launch { appContainer.ready.await(); appContainer.relationship.interact(appContainer.petState.value.character.id,"drag:${UUID.randomUUID()}",1) }
                    }
                }
                MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(tapAction); tapCount=0; dragging=false; restore() }
            }
            true
        }
    }
    private fun touched() {
        lastInteraction=SystemClock.elapsedRealtime()
        val wasSleeping=action==PetAction.SLEEP
        appContainer.sounds.play(listOf(PetSound.TOUCH_1,PetSound.TOUCH_2).random())
        // Preserve reminder content and its double-tap destination until it expires.
        if(!reminder) {
            play(if(wasSleeping) PetAction.IDLE else PetAction.CLICK,5000)
            appContainer.showBubble(TouchReplyPolicy.replies(appContainer.petState.value.character,wasSleeping).random())
        }
        appContainer.scope.launch { appContainer.ready.await(); appContainer.relationship.interact(appContainer.petState.value.character.id,"touch:${UUID.randomUUID()}",1) }
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int)=START_NOT_STICKY
    override fun onBind(intent: Intent?)=null
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); serviceScope.cancel()
        if(::displayManager.isInitialized) displayManager.unregisterDisplayListener(displayListener)
        if(::overlayContext.isInitialized && overlayContext !== this) overlayContext.unregisterComponentCallbacks(windowConfiguration)
        appContainer.sounds.stop()
        runCatching { unregisterReceiver(receiver) }
        petView?.let { if(it.isAttachedToWindow) windowManager.removeView(it); it.clearPortrait() }
        bubbleView?.let { if(it.isAttachedToWindow) windowManager.removeView(it) }
        bubbleView=null
        petView=null; running.value=false; appContainer.presence(pet=false)
        super.onDestroy()
    }
    companion object { val running=MutableStateFlow(false); val isRunning get()=running.value }
}

private class PetView(context: android.content.Context): View(context) {
    var edgeX=0
    var edgeY=0
    private val portraitRect=RectF()
    private val renderer=OverlayPortraitRenderer(::invalidate)
    init { setLayerType(LAYER_TYPE_SOFTWARE,null) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        portraitRect.set(0f,0f,width.toFloat(),height.toFloat())
        renderer.draw(canvas,portraitRect,edgeX,edgeY)
    }
    fun showPortrait(path: String?,state: Emotion) { renderer.show(path,state); invalidate() }
    fun clearPortrait()=renderer.close()
    fun pause()=renderer.detach()
    fun resume()=renderer.attach()
    override fun onAttachedToWindow() { super.onAttachedToWindow(); renderer.attach() }
    override fun onDetachedFromWindow() { renderer.detach(); super.onDetachedFromWindow() }
    override fun performClick(): Boolean { super.performClick(); return true }
}

private class BubbleView(context: android.content.Context): View(context) {
    var bubble=""
        set(value) { field=value; contentDescription=value; invalidate() }
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var cachedText=""
    private var cachedWidth=0
    private var cachedLayout: StaticLayout?=null
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val margin=width*.055f
        val bubbleHeight=height.toFloat()
        val night=com.screenhelper.presentation.theme.isNightTheme(LocalTime.now().hour)
        if(bubble.isNotBlank()) {
            paint.color=if(night) Color.rgb(53,43,65) else Color.rgb(255,249,252)
            canvas.drawRoundRect(margin,margin,width-margin,bubbleHeight-margin,margin,margin,paint)
            textPaint.color=if(night) Color.rgb(246,233,246) else Color.rgb(85,62,79)
            textPaint.textSize=width*.067f
            val textWidth=(width-margin*4).toInt().coerceAtLeast(1)
            if(cachedLayout==null || bubble!=cachedText || textWidth!=cachedWidth) {
                cachedText=bubble;cachedWidth=textWidth
                cachedLayout=StaticLayout.Builder.obtain(bubble,0,bubble.length,textPaint,textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
            }
            canvas.save(); canvas.clipRect(margin,margin,width-margin,bubbleHeight-margin)
            canvas.translate(margin*2,margin*1.5f); cachedLayout?.draw(canvas); canvas.restore()
        }
    }
}
