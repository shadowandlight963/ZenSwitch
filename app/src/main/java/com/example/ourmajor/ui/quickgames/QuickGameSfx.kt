package com.example.ourmajor.ui.quickgames

import android.media.AudioManager
import android.media.ToneGenerator

class QuickGameSfx {

    private var tone: ToneGenerator? = null

    fun playPop() {
        ensure()
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
    }

    fun playDing() {
        ensure()
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 110)
    }

    fun release() {
        tone?.release()
        tone = null
    }

    private fun ensure() {
        if (tone != null) return
        tone = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    }
}
