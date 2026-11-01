package com.screenhelper.data.local.files

import android.content.Context
import com.screenhelper.business.pet.PetAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PetAssets(private val context: Context) {
    private val directory = File(context.filesDir, "mimi-motion-v1")
    suspend fun install() = withContext(Dispatchers.IO) {
        directory.mkdirs()
        context.assets.list("mimi")!!.filter { it.endsWith(".webp") || it.endsWith(".bounds") }.forEach { name ->
            val target = File(directory, name)
            if (!target.isFile) {
                val temp = File(directory, "$name.tmp")
                context.assets.open("mimi/$name").use { input -> temp.outputStream().use { input.copyTo(it) } }
                check(temp.renameTo(target))
            }
        }
    }
    fun path(action: PetAction): String {
        val prefix = when (action) {
            PetAction.IDLE -> "idle"; PetAction.WAKE -> "wake"; PetAction.SLEEP -> "sleep"
            PetAction.CLICK -> "click"; PetAction.DRAG -> "drag"; PetAction.REMINDER -> "reminder"
            PetAction.LEFT -> "left"; PetAction.RIGHT -> "right"; PetAction.BOTTOM -> "bottom"
        }
        return directory.listFiles()?.filter { it.name.startsWith(prefix) && it.extension == "webp" }?.randomOrNull()?.path
            ?: File(directory, "idle.webp").path
    }
}
