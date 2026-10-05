package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.*

class AudioPlayer {
    private var toneGenerator: ToneGenerator? = null
    private var alarmJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 85)
        } catch (_: Throwable) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Plays continuous alarm loop until explicitly stopped via stopAlarm()
     */
    fun startAlarmLoop() {
        stopAlarm()
        alarmJob = scope.launch {
            if (toneGenerator == null) {
                try {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                } catch (_: Throwable) {}
            }
            while (isActive) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    delay(300)
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    delay(700)
                } catch (_: Throwable) {
                    delay(1000)
                }
            }
        }
    }

    /**
     * Stops any currently playing alarm loop
     */
    fun stopAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        try {
            toneGenerator?.stopTone()
        } catch (_: Throwable) {}
    }

    /**
     * Plays a pleasant double chime for overdue habit reminder
     */
    fun playOverdueChime() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                delay(300)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Plays celebratory tone when focus session finishes or alarm is correctly typed
     */
    fun playCompletionFanfare() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                delay(250)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                delay(250)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (_: Throwable) {}
        }
    }

    fun release() {
        stopAlarm()
        try {
            toneGenerator?.release()
        } catch (_: Throwable) {}
        toneGenerator = null
    }
}
