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
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.model.CaptureResult
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

/** One user-authorized frame; no continuous screen monitoring. */
class ScreenCaptureService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var captured = false
    private val callback = object : MediaProjection.Callback() { override fun onStop() { if (!captured) fail("屏幕分享已结束") } }
    private val timeout = Runnable { if (!captured) fail("未能获取屏幕画面，请重新授权") }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projection != null) return START_NOT_STICKY
        try {
            val notification = appContainer.notifications.ongoing("正在获取一张屏幕画面", "画面将作为待发送图片，你可以确认后再发送", true)
            if (Build.VERSION.SDK_INT >= 29) startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1002, notification)
            val code = intent?.getIntExtra("result_code", 0) ?: 0
            @Suppress("DEPRECATION")
            val consent = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("result_data", Intent::class.java) else intent?.getParcelableExtra<Intent>("result_data")
            require(code == Activity.RESULT_OK && consent != null) { "需要先授权屏幕分享" }
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            val next = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, consent)
            projection = next
            next.registerCallback(callback, handler)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { imageReader ->
                imageReader.setOnImageAvailableListener({ source ->
                    val frame = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                    if (captured) { frame.close(); return@setOnImageAvailableListener }
                    captured = true
                    handler.removeCallbacks(timeout)
                    scope.launch {
                        try {
                            val plane = frame.planes[0]
                            val paddedWidth = width + (plane.rowStride - plane.pixelStride * width) / plane.pixelStride
                            val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                            padded.copyPixelsFromBuffer(plane.buffer)
                            val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                            val scale = minOf(1f, 1280f / maxOf(width, height))
                            val outputBitmap = if (scale < 1f) Bitmap.createScaledBitmap(cropped, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true) else cropped
                            val directory = File(filesDir, "images").apply { mkdirs() }
                            val output = File(directory, "capture-${UUID.randomUUID()}.png")
                            output.outputStream().use { check(outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                            if (outputBitmap !== cropped) outputBitmap.recycle()
                            if (cropped !== padded) cropped.recycle()
                            padded.recycle()
                            appContainer.captureResult.value = CaptureResult(path = output.path)
                        } catch (_: Exception) { appContainer.captureResult.value = CaptureResult(error = "屏幕画面保存失败") }
                        finally { frame.close(); withContext(Dispatchers.Main) { stopSelf() } }
                    }
                }, handler)
            }
            display = next.createVirtualDisplay("ScreenHelperCapture", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
            handler.postDelayed(timeout, 10_000)
        } catch (_: Exception) { fail("无法采集屏幕，请重新授权或检查设备限制") }
        return START_NOT_STICKY
    }
    private fun fail(message: String) { appContainer.captureResult.value = CaptureResult(error = message); stopSelf() }
    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        display?.release(); display = null
        reader?.close(); reader = null
        projection?.unregisterCallback(callback); projection?.stop(); projection = null
        scope.cancel()
        super.onDestroy()
    }
}
