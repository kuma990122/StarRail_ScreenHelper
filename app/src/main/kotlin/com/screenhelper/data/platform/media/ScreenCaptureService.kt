package com.screenhelper.data.platform.media

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.Image
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Base64
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.business.conversation.ContextBuilder
import com.screenhelper.business.pet.FrameWindow
import com.screenhelper.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

/** A user-authorized session. Streaming capture never persists frames beyond its three-slot cache. */
class ScreenCaptureService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var continuous = false
    @Volatile private var completed = false
    @Volatile private var lastPixels: ByteArray? = null
    private val frames = FrameWindow<ByteArray>()
    private var inference: Job? = null
    private var pendingImage: Image? = null
    private var encoding = false
    @Volatile private var screenOn = true
    @Volatile private var captureGeneration = 0L
    private var lastBubbleId = 0L
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            screenOn = intent.action != Intent.ACTION_SCREEN_OFF
            if (!screenOn) {
                captureGeneration++
                inference?.cancel(); frames.clear(); lastPixels = null
                pendingImage?.close(); pendingImage = null
                appContainer.screenWatch.update { it.copy(frames=0,message="锁屏暂停，画面缓存已清空") }
            } else if(continuous) appContainer.screenWatch.update { it.copy(message="等待三帧画面；每 5 秒采样") }
        }
    }
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() { if (!completed) fail("屏幕分享已结束") }
    }
    private val timeout = Runnable { if (lastPixels == null && !completed) fail("未能获取屏幕画面，请重新授权") }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projection != null) return START_NOT_STICKY
        continuous = intent?.getBooleanExtra("continuous", false) == true
        try {
            val notification = appContainer.notifications.capture(continuous)
            if (Build.VERSION.SDK_INT >= 29) startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(1002, notification)
            val code = intent?.getIntExtra("result_code", 0) ?: 0
            @Suppress("DEPRECATION")
            val consent = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("result_data", Intent::class.java) else intent?.getParcelableExtra<Intent>("result_data")
            require(code == Activity.RESULT_OK && consent != null)
            val config = appContainer
            screenOn = getSystemService(PowerManager::class.java).isInteractive
            androidx.core.content.ContextCompat.registerReceiver(this, screenReceiver,
                android.content.IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            val metrics = resources.displayMetrics
            val scale = minOf(1f, (if (continuous) 720f else 1280f) / maxOf(metrics.widthPixels, metrics.heightPixels))
            val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(1)
            val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(1)
            val next = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, consent)
            projection = next
            next.registerCallback(callback, handler)
            fun encode(image: Image) {
                encoding = true
                val generation = captureGeneration
                scope.launch {
                        try {
                            val plane = image.planes[0]
                            val paddedWidth = width + (plane.rowStride - plane.pixelStride * width) / plane.pixelStride
                            val padded = Bitmap.createBitmap(paddedWidth,height,Bitmap.Config.ARGB_8888)
                            try {
                                padded.copyPixelsFromBuffer(plane.buffer)
                                val cropped = Bitmap.createBitmap(padded,0,0,width,height)
                                try {
                                    val bytes = java.io.ByteArrayOutputStream().use { out ->
                                        check(cropped.compress(Bitmap.CompressFormat.JPEG,75,out)); out.toByteArray()
                                    }
                                    if(completed || !screenOn || generation != captureGeneration) return@launch
                                    lastPixels = bytes
                                    if(continuous) {
                                        frames.add(bytes)
                                        appContainer.screenWatch.update { it.copy(frames=frames.snapshot().size) }
                                    }
                                    if (!continuous) {
                                        val file = File(filesDir,"images/capture-${UUID.randomUUID()}.jpg").apply { parentFile!!.mkdirs(); writeBytes(bytes) }
                                        appContainer.captureResult.value = CaptureResult(path=file.path)
                                        completed = true
                                        withContext(Dispatchers.Main) { stopSelf() }
                                    }
                                } finally { if (cropped !== padded) cropped.recycle() }
                            } finally { padded.recycle() }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            withContext(Dispatchers.Main) { fail("屏幕画面处理失败，请重新开启") }
                        } finally { image.close(); withContext(NonCancellable + Dispatchers.Main) { encoding = false } }
                }
            }
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3).also { imageReader ->
                imageReader.setOnImageAvailableListener({ source ->
                    val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                    if (completed || !screenOn) { image.close(); return@setOnImageAvailableListener }
                    if (continuous) {
                        // Keep only the newest unencoded surface buffer; encode on the five-second clock.
                        pendingImage?.close(); pendingImage=image
                    } else if(!encoding) encode(image) else image.close()
                },handler)
            }
            display = next.createVirtualDisplay("ScreenHelperCapture",width,height,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,handler)
            handler.postDelayed(timeout,10_000)
            if (continuous) {
                val sample = object : Runnable {
                    override fun run() {
                        if(completed) return
                        if(screenOn && !encoding) {
                            val image=pendingImage; pendingImage=null
                            if(image!=null) encode(image)
                            else lastPixels?.let { bytes ->
                                // A static display need not submit a new surface buffer. It is still
                                // the current picture at this sample time; share its immutable JPEG.
                                frames.add(bytes)
                                config.screenWatch.update { it.copy(frames=frames.snapshot().size) }
                            }
                        }
                        handler.postDelayed(this,5000)
                    }
                }
                handler.postDelayed(sample,5000)
                config.screenWatch.value = ScreenWatchState(running=true,message="等待三帧画面；每 5 秒采样")
                scope.launch {
                    config.ready.await()
                    while (isActive) {
                        delay(5000)
                        if (!screenOn) continue
                        if (frames.snapshot().size == 3 && inference?.isActive != true && !config.conversation.state.value.busy) {
                            // Encoding snapshot now makes eviction independent from the in-flight request.
                            val urls = frames.snapshot().map { "data:image/jpeg;base64," + Base64.encodeToString(it,Base64.NO_WRAP) }
                            inference = launch {
                                config.screenWatch.update { it.copy(busy=true,message="正在看最近三帧画面") }
                                lastBubbleId = config.petState.value.bubbleId
                                try {
                                    val result = config.screenInteraction.comment(urls)
                                    // A newer reminder or chat owns the bubble; never overwrite it.
                                    if (screenOn && config.petState.value.bubbleId == lastBubbleId && config.petState.value.reminderId == null && result.isNotBlank())
                                        config.showBubble(result)
                                    config.screenWatch.update { it.copy(message="屏幕互动中 · 每 5 秒采样") }
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) {
                                    config.screenWatch.update { it.copy(message="屏幕互动已停止：${e.message?.take(100)}") }
                                    withContext(Dispatchers.Main) { stopSelf() }
                                } finally { config.screenWatch.update { it.copy(busy=false) } }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { fail("无法采集屏幕，请重新授权或检查设备限制") }
        return START_NOT_STICKY
    }
    private fun fail(message: String) {
        if (continuous) appContainer.screenWatch.update { it.copy(message=message) }
        else appContainer.captureResult.value = CaptureResult(error=message)
        stopSelf()
    }
    override fun onDestroy() {
        completed = true
        handler.removeCallbacksAndMessages(null)
        pendingImage?.close(); pendingImage=null
        runCatching { unregisterReceiver(screenReceiver) }
        val closingReader = reader
        reader = null
        scope.coroutineContext[Job]?.invokeOnCompletion { closingReader?.close() }
        scope.cancel()
        display?.release(); display = null
        projection?.unregisterCallback(callback); projection?.stop(); projection = null
        frames.clear(); lastPixels = null
        if (continuous) appContainer.screenWatch.update { it.copy(running=false,frames=0,busy=false) }
        super.onDestroy()
    }
}
