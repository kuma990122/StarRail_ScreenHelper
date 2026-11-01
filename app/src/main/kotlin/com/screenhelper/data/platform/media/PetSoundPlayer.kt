package com.screenhelper.data.platform.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.screenhelper.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PetSound(val resource: Int) {
    GREETING(R.raw.pet_greeting), TODO(R.raw.pet_todo), SLEEP(R.raw.pet_sleep),
    TOUCH_1(R.raw.pet_touch_1), TOUCH_2(R.raw.pet_touch_2),
    DRAG(R.raw.pet_happy), DOCK_SIDE(R.raw.pet_dock_side)
}

enum class SoundPhase { IDLE, PREPARING, PLAYING, ERROR }
data class SoundPlayback(val enabled: Boolean = false, val cue: PetSound? = null,
    val phase: SoundPhase = SoundPhase.IDLE, val sequence: Long = 0)

/** All player transitions run on Main; replacing a clip also cancels pending preparation. */
class PetSoundPlayer(context: Context, enabled: Flow<Boolean>, private val scope: CoroutineScope) {
    private val resources = context.applicationContext.resources
    private var player: MediaPlayer? = null
    private val mutable = MutableStateFlow(SoundPlayback())
    val playback = mutable.asStateFlow()

    init {
        scope.launch(Dispatchers.Main.immediate) {
            enabled.collect { value ->
                mutable.update { it.copy(enabled = value) }
                if (!value) release()
            }
        }
    }

    fun play(cue: PetSound) {
        scope.launch(Dispatchers.Main.immediate) {
            release()
            if (!mutable.value.enabled) return@launch
            val next = MediaPlayer()
            player = next
            mutable.update { it.copy(cue = cue, phase = SoundPhase.PREPARING, sequence = it.sequence + 1) }
            try {
                next.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                resources.openRawResourceFd(cue.resource).use { source ->
                    next.setDataSource(source.fileDescriptor, source.startOffset, source.length)
                }
                next.setOnPreparedListener {
                    if (player === it && mutable.value.enabled) {
                        it.start()
                        mutable.update { state -> state.copy(phase = SoundPhase.PLAYING) }
                    }
                }
                next.setOnCompletionListener { if (player === it) release() }
                next.setOnErrorListener { failed, _, _ ->
                    if (player === failed) release(SoundPhase.ERROR)
                    true
                }
                next.prepareAsync()
            } catch (_: Exception) {
                if (player === next) release(SoundPhase.ERROR)
            }
        }
    }

    fun stop() { scope.launch(Dispatchers.Main.immediate) { release() } }

    private fun release(phase: SoundPhase = SoundPhase.IDLE) {
        val previous = player
        player = null
        previous?.apply {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            release()
        }
        mutable.update { it.copy(phase = phase) }
    }
}
