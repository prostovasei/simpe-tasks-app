package com.prostovasei.simpletasks.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * Озвучка плана дня (итерация 30): голос телефона (Samsung или Google), русский, без интернета. Читает по фразам и
 * при выключенном экране, как музыкальный плеер; в шторке — «Пауза»/«Дальше» и «Стоп». Музыка на время чтения
 * становится тише. Нет русского голоса — в шторке подсказка, как его скачать.
 */
@AndroidEntryPoint
class SpeechService : Service(), TextToSpeech.OnInitListener {
    @Inject lateinit var summary: DaySummary

    @Inject lateinit var time: TimeProvider

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var lines: List<String>? = null

    /** Какую фразу читать дальше: с неё продолжает «Дальше» после паузы. */
    private var next = 0
    private var paused = false
    private var focus: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SPEAK -> {
                foreground(R.string.speech_playing)
                lines = null
                next = 0
                paused = false
                appScope.launch {
                    val text = summary.speech(summary.plan(), time.now().atZone(time.zone()).hour)
                    withContext(Dispatchers.Main) {
                        lines = text
                        startIfReady()
                    }
                }
                if (tts == null) tts = TextToSpeech(this, this) else startIfReady()
            }
            ACTION_PAUSE -> {
                paused = true
                tts?.stop()
                foreground(R.string.speech_paused)
            }
            ACTION_RESUME -> {
                // Сервис успел закрыться — продолжать нечего.
                if (lines == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                paused = false
                foreground(R.string.speech_playing)
                speakFrom(next)
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        val language = if (status == TextToSpeech.SUCCESS) engine.setLanguage(Locale.forLanguageTag("ru-RU")) else TextToSpeech.LANG_NOT_SUPPORTED
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            noVoice()
            return
        }
        engine.setAudioAttributes(ATTRIBUTES)
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    next = utteranceId.toInt()
                }

                override fun onDone(utteranceId: String) {
                    val done = utteranceId.toInt()
                    next = done + 1
                    if (done == (lines?.lastIndex ?: -1)) main.post { stopSelf() }
                }

                @Deprecated("Устарело в Android, но обязательно к реализации")
                override fun onError(utteranceId: String) {
                    main.post { stopSelf() }
                }
            },
        )
        ready = true
        startIfReady()
    }

    private fun startIfReady() {
        if (ready && lines != null && !paused) speakFrom(next)
    }

    private fun speakFrom(index: Int) {
        val engine = tts ?: return
        val text = lines ?: return
        if (index > text.lastIndex) {
            stopSelf()
            return
        }
        requestFocus()
        for (i in index..text.lastIndex) {
            engine.speak(text[i], if (i == index) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, i.toString())
        }
    }

    private fun requestFocus() {
        if (focus != null) return
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(ATTRIBUTES).build().also {
            getSystemService(AudioManager::class.java).requestAudioFocus(it)
        }
    }

    /** Русского голоса нет — подсказка в шторке вместо чтения. */
    private fun noVoice() {
        ensureChannel()
        val install = PendingIntent.getActivity(
            this,
            0,
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_tasks)
            .setContentTitle(getString(R.string.speech_no_voice))
            .setContentText(getString(R.string.speech_no_voice_hint))
            .setContentIntent(install)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, notification)
        stopSelf()
    }

    private fun foreground(status: Int) {
        ensureChannel()
        val playing = status == R.string.speech_playing
        val toggle = if (playing) {
            NotificationCompat.Action(0, getString(R.string.speech_pause), service(ACTION_PAUSE))
        } else {
            NotificationCompat.Action(0, getString(R.string.speech_resume), service(ACTION_RESUME))
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_tasks)
            .setContentTitle(getString(R.string.speech_title))
            .setContentText(getString(status))
            .setOngoing(playing)
            .setSilent(true)
            .addAction(toggle)
            .addAction(0, getString(R.string.speech_stop), service(ACTION_STOP))
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun service(action: String): PendingIntent =
        PendingIntent.getService(this, action.hashCode(), Intent(this, SpeechService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.speech_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SPEAK = "com.prostovasei.simpletasks.SPEAK_PLAN"
        const val ACTION_PAUSE = "com.prostovasei.simpletasks.SPEAK_PAUSE"
        const val ACTION_RESUME = "com.prostovasei.simpletasks.SPEAK_RESUME"
        const val ACTION_STOP = "com.prostovasei.simpletasks.SPEAK_STOP"
        private const val CHANNEL_ID = "day_speech"
        private const val NOTIFICATION_ID = Int.MAX_VALUE - 3
        private val ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
