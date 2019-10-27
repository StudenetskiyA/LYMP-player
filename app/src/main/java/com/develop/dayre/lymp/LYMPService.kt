package com.develop.dayre.lymp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver

const val EXTRA_COMMAND = "EXTRA_COMMAND"

enum class ServiceCommand { Start, Stop, Next, Prev, Init }

class LYMPService : LifecycleService() {
    private val NOTIFICATION_ID = 9999
    private val TAG = "$APP_TAG/service"
    private val viewModel = MainActivity.instance?.viewModel!!

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return MyLocalBinder()
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): LYMPService {
            return this@LYMPService
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "Init service")
        viewModel.setMediaSessonCallback(mediaSessionCallback)
        createObservers()
        return START_STICKY
    }

    private fun createObservers() {
        viewModel.currentSong.observe(this,
            Observer<Song> {
                Log.i(TAG, "current song updated")
                val settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                settings.edit().putString(
                    APP_PREFERENCES_SELECT_SONG,
                    viewModel.currentSong.value?.ID.toString()
                ).apply()
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    internal fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                // На паузе мы перестаем быть foreground, однако оставляем уведомление,
                // чтобы пользователь мог play нажать
//                NotificationManagerCompat.from(this@LYMPService)
//                    .notify(NOTIFICATION_ID, getNotification(playbackState))
//                stopForeground(false)
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            else -> {
                // Все, можно прятать уведомление
                stopForeground(true)
            }
        }
    }

    private fun getNotification(playbackState: Int): Notification {
        // MediaStyleHelper заполняет уведомление метаданными трека.
        // Хелпер любезно написал Ian Lake / Android Framework Developer at Google
        // и выложил здесь: https://gist.github.com/ianhanniballake/47617ec3488e0257325c
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                ""
            }

        val builder = MediaStyleHelperFrom(this, viewModel.getMediaSession())

        // Добавляем кнопки
        // ...на предыдущий трек
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous, getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )

        // ...play/pause
        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
        else
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play, getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

        // ...на следующий трек
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next, getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )

        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                // В компактном варианте показывать Action с данным порядковым номером.
                // В нашем случае это play/pause.
                .setShowActionsInCompactView(1)
                // Отображать крестик в углу уведомления для его закрытия.
                // Это связано с тем, что для API < 21 из-за ошибки во фреймворке
                // пользователь не мог смахнуть уведомление foreground-сервиса
                // даже после вызова stopForeground(false).
                // Так что это костыль.
                // На API >= 21 крестик не отображается, там просто смахиваем уведомление.
                .setShowCancelButton(true)
                // Указываем, что делать при нажатии на крестик или смахивании
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                // Передаем токен. Это важно для Android Wear. Если токен не передать,
                // кнопка на Android Wear будет отображаться, но не будет ничего делать
                .setMediaSession(viewModel.getMediaSessionToken())
        )

        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setChannelId(channelId)
        builder.color = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        // Не отображать время создания уведомления. В нашем случае это не имеет смысла
        builder.setShowWhen(false)

        // Это важно. Без этой строчки уведомления не отображаются на Android Wear
        // и криво отображаются на самом телефоне.
        builder.priority = NotificationCompat.PRIORITY_HIGH

        // Не надо каждый раз вываливать уведомление на пользователя
        builder.setOnlyAlertOnce(true)

        return builder.build()
    }

    private var mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.i(TAG, "callback onPlay")
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onPause() {
                Log.i(TAG, "callback onPause")
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onStop() {
                Log.i(TAG, "callback onStop")
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_STOPPED)
            }

            override fun onSkipToNext() {
                Log.i(TAG, "callback onNext")
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onSkipToPrevious() {
                Log.i(TAG, "callback onPrev")
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
            }
        }

}