package com.develop.dayre.lymp

import android.app.*
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


const val SERVICE_COMMAND = "LYMP_SERVICE_COMMAND"
enum class ServiceCommand { Prev, Play, Stop, Next, Init }

class LYMPService : LifecycleService() {
    private val NOTIFICATION_ID = 9999
    private val TAG = "$APP_TAG/service"
    //private val viewModel = MainActivity.instance?.viewModel!!
    private lateinit var viewModel : LYMPViewModel

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
        Log.i(TAG, "Service started")
        viewModel.setMediaSessionCallback(mediaSessionCallback)

        if (intent.hasExtra(SERVICE_COMMAND)) {
            val command = intent.getSerializableExtra(SERVICE_COMMAND) as ServiceCommand
            Log.i(TAG, "onStartCommand $command")
            when (command) {
                ServiceCommand.Play -> viewModel.playPress()
                ServiceCommand.Next -> {
                    viewModel.nextPress()
                    viewModel.playPress()
                }
                ServiceCommand.Prev -> {
                    viewModel.prevPress()
                    viewModel.playPress()
                }
            }
        }
        createObservers()
        return Service.START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "service created")

        viewModel = App.viewModel
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
//                // На паузе мы перестаем быть foreground, однако оставляем уведомление,
//                // чтобы пользователь мог play нажать
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
                createNotificationChannel("lymp_service", "LYMP Background Service")
            } else {
                ""
            }

        val builder = MediaStyleHelperFrom(this, viewModel.getMediaSession())

        // Добавляем кнопки
        // ...на предыдущий трек
        val intentPrev = Intent(this, LYMPService::class.java)
        intentPrev.putExtra(SERVICE_COMMAND, ServiceCommand.Prev)
        val pendingIntentPrev = PendingIntent.getService(
            applicationContext, NOTIFICATION_ID, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous, getString(R.string.previous),
                pendingIntentPrev
            )
        )

        // ...play/pause. Интент у нас один, модель разберется.
        val intentPlay = Intent(this, LYMPService::class.java)
        intentPlay.putExtra(SERVICE_COMMAND, ServiceCommand.Play)
        val pendingIntentPlay = PendingIntent.getService(
            applicationContext, NOTIFICATION_ID+1, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause, getString(R.string.pause),
                    pendingIntentPlay
                )
            )
        else
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play, getString(R.string.play),
                    pendingIntentPlay
                )
            )

        // ...на следующий трек
        val intentNext = Intent(this, LYMPService::class.java)
        intentNext.putExtra(SERVICE_COMMAND, ServiceCommand.Next)
        val pendingIntentNext = PendingIntent.getService(
            applicationContext, NOTIFICATION_ID+2, intentNext, PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next, getString(R.string.next),
              pendingIntentNext
            )
        )

        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
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
            //model.callBackAwaited - ужасный костыль, но иначе приходится либо размещать здесь логику
            //Либо происходят MediaButton не будут обрабатываться.
            override fun onPlay() {
                Log.i(TAG, "callback onPlay")
                if (!viewModel.getCallBackAwaited()) viewModel.playPress()
                else viewModel.setCallBackAwaited(false)
                if (viewModel.getIsPlaying()==PlayState.Play)
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
                else  refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onPause() {
                Log.i(TAG, "callback onPause")
                if (!viewModel.getCallBackAwaited()) viewModel.playPress()
                else viewModel.setCallBackAwaited(false)
                if (viewModel.getIsPlaying()==PlayState.Play)
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
                else  refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onStop() {
                Log.i(TAG, "callback onStop")
                if (!viewModel.getCallBackAwaited()) viewModel.stopPress()
                else viewModel.setCallBackAwaited(false)
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_STOPPED)
            }

            override fun onSkipToNext() {
                Log.i(TAG, "callback onNext")
                if (!viewModel.getCallBackAwaited()) {
                    viewModel.nextPress()
                    viewModel.playPress()
                }
                else viewModel.setCallBackAwaited(false)
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onSkipToPrevious() {
                Log.i(TAG, "callback onPrev")
                if (!viewModel.getCallBackAwaited()) {
                    viewModel.prevPress()
                    viewModel.playPress()
                }
                else viewModel.setCallBackAwaited(false)
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
            }
        }

}