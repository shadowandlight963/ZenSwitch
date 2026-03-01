package com.example.ourmajor.ui.quickgames

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator

class QuickGameAudio(private val context: Context) {

    private val soundPool: SoundPool
    private val tone: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)

    private val ids: MutableMap<String, Int> = LinkedHashMap()

    private val tileNotes = listOf(
        "piano_c",
        "piano_d",
        "piano_e",
        "piano_f",
        "piano_g",
        "piano_a",
        "piano_b",
        "piano_c2",
        "piano_d2"
    )

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        tileNotes.forEach { ensureLoaded(it) }
        ensureLoaded("pop")
        ensureLoaded("click")
    }

    fun playTileNote(tileIndex: Int) {
        val key = tileNotes.getOrNull(tileIndex) ?: return
        if (!play(key, rate = 1f)) {
            val toneId = when (tileIndex % 9) {
                0 -> ToneGenerator.TONE_DTMF_1
                1 -> ToneGenerator.TONE_DTMF_2
                2 -> ToneGenerator.TONE_DTMF_3
                3 -> ToneGenerator.TONE_DTMF_4
                4 -> ToneGenerator.TONE_DTMF_5
                5 -> ToneGenerator.TONE_DTMF_6
                6 -> ToneGenerator.TONE_DTMF_7
                7 -> ToneGenerator.TONE_DTMF_8
                else -> ToneGenerator.TONE_DTMF_9
            }
            tone.startTone(toneId, 90)
        }
    }

    fun playPop() {
        if (!play("pop", rate = 1f)) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
        }
    }

    fun playClick() {
        if (!play("click", rate = 1f)) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        }
    }

    fun playDing() {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 110)
    }

    fun release() {
        soundPool.release()
        tone.release()
    }

    private fun play(key: String, rate: Float): Boolean {
        val id = ids[key] ?: return false
        if (id == 0) return false
        soundPool.play(id, 1f, 1f, 1, 0, rate)
        return true
    }

    private fun ensureLoaded(key: String) {
        if (ids.containsKey(key)) return
        val resId = context.resources.getIdentifier(key, "raw", context.packageName)
        if (resId == 0) {
            ids[key] = 0
            return
        }
        ids[key] = soundPool.load(context, resId, 1)
    }
}
