package hu.infokristaly.homework4timersonandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import hu.infokristaly.homework4timersonandroid.MainActivity
import hu.infokristaly.homework4timersonandroid.R
import java.util.Locale

class TimerService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "homework4timers_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_PLAY_ALARM = "ACTION_PLAY_ALARM"
        const val ACTION_SPEAK = "ACTION_SPEAK"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_BODY = "EXTRA_BODY"
        const val EXTRA_SPEAK_TEXT = "EXTRA_SPEAK_TEXT"
        const val EXTRA_SPEAK_LANG = "EXTRA_SPEAK_LANG"

        fun startService(
            context: Context,
            title: String,
            body: String,
            speakText: String? = null,
            speakLang: String = "hu"
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                speakText?.let { putExtra(EXTRA_SPEAK_TEXT, it) }
                putExtra(EXTRA_SPEAK_LANG, speakLang)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateNotification(context: Context, title: String, body: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            context.startService(intent)
        }

        fun speakSectionLabel(context: Context, speakText: String, speakLang: String = "hu") {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_SPEAK
                putExtra(EXTRA_SPEAK_TEXT, speakText)
                putExtra(EXTRA_SPEAK_LANG, speakLang)
            }
            context.startService(intent)
        }

        fun playAlarm(
            context: Context,
            title: String,
            body: String,
            speakText: String? = null,
            speakLang: String = "hu"
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PLAY_ALARM
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                speakText?.let { putExtra(EXTRA_SPEAK_TEXT, it) }
                putExtra(EXTRA_SPEAK_LANG, speakLang)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeakText: String? = null
    private var pendingSpeakLang: String = "hu"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            val pending = pendingSpeakText
            if (pending != null) {
                speakTextInternal(pending, pendingSpeakLang)
                pendingSpeakText = null
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "Homework4Timers:TimerWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(30 * 60 * 1000L) // 30 mins max timeout
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                acquireWakeLock()
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Intervallum időzítő"
                val body = intent.getStringExtra(EXTRA_BODY) ?: "Futás..."
                val textToSpeak = intent.getStringExtra(EXTRA_SPEAK_TEXT)
                val langToSpeak = intent.getStringExtra(EXTRA_SPEAK_LANG) ?: "hu"

                startForeground(NOTIFICATION_ID, buildNotification(title, body))

                if (!textToSpeak.isNullOrEmpty()) {
                    speakText(textToSpeak, langToSpeak)
                }
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Intervallum időzítő"
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(title, body))
            }
            ACTION_SPEAK -> {
                val textToSpeak = intent.getStringExtra(EXTRA_SPEAK_TEXT)
                val langToSpeak = intent.getStringExtra(EXTRA_SPEAK_LANG) ?: "hu"
                if (!textToSpeak.isNullOrEmpty()) {
                    speakText(textToSpeak, langToSpeak)
                }
            }
            ACTION_PLAY_ALARM -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Intervallum időzítő"
                val body = intent.getStringExtra(EXTRA_BODY) ?: "Az időzítő lejárt!"
                val textToSpeak = intent.getStringExtra(EXTRA_SPEAK_TEXT)
                val langToSpeak = intent.getStringExtra(EXTRA_SPEAK_LANG) ?: "hu"

                if (!textToSpeak.isNullOrEmpty()) {
                    speakText(textToSpeak, langToSpeak)
                } else {
                    speakText(body, langToSpeak)
                }

                triggerVibration()

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(title, body))
            }
            ACTION_STOP -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun speakText(text: String, langCode: String) {
        if (isTtsInitialized) {
            speakTextInternal(text, langCode)
        } else {
            pendingSpeakText = text
            pendingSpeakLang = langCode
        }
    }

    private fun speakTextInternal(text: String, langCode: String) {
        val locale = Locale.forLanguageTag(langCode)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.getDefault()
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TimerUtterance")
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val timings = longArrayOf(0, 500, 200, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Intervallum időzítő",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Az intervallum időzítő értesítései"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, body: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
