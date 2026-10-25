package com.screenhelper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Movie
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.data.local.credentials.CredentialVault
import com.screenhelper.data.local.database.AppDatabase
import com.screenhelper.data.local.files.AssetStore
import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.*
import com.screenhelper.presentation.MainActivity
import com.screenhelper.presentation.pet.PetService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.appContainer
    @Before fun demoOnly() = runBlocking { app.ready.await(); app.settings.save(AppSettings(touchTalk = false)); app.settings.selectCharacter("default"); app.conversation.cancel(); delay(200) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "test-screenshots").apply { mkdirs() }
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(directory, "$name.png")))
    }
    @Test fun pagesAndLocalChatWork() {
        screenshot("01-home")
        compose.onNodeWithTag("nav_chat").performClick()
        compose.onNodeWithTag("chat_input").performTextInput("今天很开心")
        compose.onNodeWithTag("send_message").performClick()
        compose.waitUntil(12000) { !app.conversation.state.value.busy }
        compose.onAllNodesWithTag("assistant_message").onLast().assertTextContains("星灯", substring = true)
        screenshot("02-chat")
        compose.onNodeWithTag("nav_memo").performClick()
        compose.onNodeWithTag("add_memo").performClick()
        compose.onNodeWithTag("memo_title").performTextInput("给自己倒杯水")
        screenshot("03-memo-editor")
        compose.onNodeWithTag("save_memo").performClick()
        compose.waitUntil(5000) { runBlocking { app.local.pendingMemos().any { it.title == "给自己倒杯水" } } }
        screenshot("04-memo")
        compose.onNodeWithTag("nav_character").performClick(); screenshot("05-character")
        compose.onNodeWithTag("nav_settings").performClick(); screenshot("06-settings")
    }
    @Test fun databaseDeduplicatesEventsAndRecoversInterruptedMessages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val local = LocalRepository(database); local.initialize()
            val first = local.updateRelationship("event", 1000, "default") { it.copy(points = it.points + 2) }
            val second = local.updateRelationship("event", 1000, "default") { it.copy(points = it.points + 2) }
            assertEquals(first.points, second.points)
            local.saveMemo(Memo("atomic", "drink", 1000, version = 2))
            assertNull(local.claimReminder("atomic", 1, 1000))
            val claims = coroutineScope { (1..8).map { async(Dispatchers.IO) { local.claimReminder("atomic", 2, 1000) } }.awaitAll() }
            assertEquals(1, claims.count { it != null })
            local.saveMessage(ChatMessage("interrupted", "default", "assistant", "partial", 1, status = "streaming"))
            local.initialize()
            assertEquals("interrupted", local.messages("default").first().single().status)
        } finally { database.close() }
    }
    @Test fun keystoreRoundTripAndImageImport() = runBlocking {
        val vault = CredentialVault(); val encrypted = vault.encrypt("test-key-no-network")
        assertFalse(encrypted.contains("test-key-no-network")); assertEquals("test-key-no-network", vault.decrypt(encrypted))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = File(context.cacheDir, "test-source.png")
        val bitmap = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        val path = AssetStore(context).importImage(Uri.fromFile(image))
        val decoded = android.graphics.BitmapFactory.decodeFile(path)
        assertTrue(maxOf(decoded.width, decoded.height) <= 1536)
        decoded.recycle(); image.delete(); File(path).delete(); Unit
    }

    @Test fun animatedGifImportPreservesItsFrames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = File(context.cacheDir, "test-animation.gif")
        val gif = """
            47 49 46 38 39 61 01 00 01 00 80 00 00 00 00 00 ff ff ff
            21 ff 0b 4e 45 54 53 43 41 50 45 32 2e 30 03 01 00 00 00
            21 f9 04 00 0a 00 00 00 2c 00 00 00 00 01 00 01 00 00 02 02 44 01 00
            21 f9 04 00 0a 00 00 00 2c 00 00 00 00 01 00 01 00 00 02 02 49 01 00 3b
        """.trimIndent().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()
        image.writeBytes(gif)
        val path = AssetStore(context).importPortrait(Uri.fromFile(image))
        val saved = File(path)
        val movie = Movie.decodeFile(path)
        try {
            assertEquals("gif", saved.extension)
            assertArrayEquals(gif, saved.readBytes())
            assertNotNull("The imported animation must remain decodable", movie)
            assertTrue("Both frames and their timing must be retained", movie!!.duration() >= 100)
        } finally {
            image.delete()
            saved.delete()
        }
    }
}
