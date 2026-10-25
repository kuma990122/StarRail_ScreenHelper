package com.screenhelper

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.model.AppSettings
import com.screenhelper.presentation.MainActivity
import com.screenhelper.presentation.pet.PetService
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Cross-task navigation uses UiAutomator, without owning a Compose ActivityScenario. */
@RunWith(AndroidJUnit4::class)
class OverlayFlowTest {
    @Test fun alarmAndOverlayTripleTap() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.appContainer
        app.ready.await()
        app.settings.save(AppSettings(touchTalk = false))
        app.settings.selectCharacter("default")
        val device = UiDevice.getInstance(instrumentation)
        val screenshots = File(context.getExternalFilesDir(null), "test-screenshots").apply { mkdirs() }
        val metrics = File(context.getExternalFilesDir(null), "test-metrics").apply { mkdirs() }
        fun metric(name: String, command: String) { File(metrics, name).writeText(device.executeShellCommand(command)) }
        device.executeShellCommand("appops set com.screenhelper SYSTEM_ALERT_WINDOW allow")
        device.executeShellCommand("appops set com.screenhelper SCHEDULE_EXACT_ALARM allow")
        device.executeShellCommand("pm grant com.screenhelper android.permission.POST_NOTIFICATIONS")
        context.startActivity(Intent(context, MainActivity::class.java).putExtra("tab", "home").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 8000))
        delay(2000)
        metric("instrumented-foreground-memory.txt", "dumpsys meminfo com.screenhelper")
        try {
            context.startForegroundService(Intent(context, PetService::class.java))
            withTimeout(8000) { while (!PetService.isRunning) delay(100) }
            device.pressHome()
            val memo = app.memo.save("该休息一下了", System.currentTimeMillis() + 2500, "alarm-ui-test")
            withTimeout(25000) { while (app.local.memo(memo.id)!!.deliveredVersion != memo.version) delay(250) }
            assertTrue(app.petState.value.bubble.contains(memo.title))
            delay(200)
            assertTrue(device.takeScreenshot(File(screenshots, "07-overlay-reminder.png")))
            delay(8000)
            metric("instrumented-overlay-memory.txt", "dumpsys meminfo com.screenhelper")
            metric("instrumented-overlay-cpu.txt", "top -b -n 1 -p ${android.os.Process.myPid()}")
            val density = context.resources.displayMetrics.density
            val x = device.displayWidth - (90 * density).toInt()
            val y = device.displayHeight / 3 + (165 * density).toInt()
            repeat(3) { device.click(x, y); delay(90) }
            assertTrue(device.wait(Until.hasObject(By.textContains("新待办")), 8000))
            assertTrue(device.takeScreenshot(File(screenshots, "08-triple-tap-memo.png")))
        } finally {
            context.stopService(Intent(context, PetService::class.java))
            app.memo.delete("alarm-ui-test")
        }
    }
}
