package com.develop.dayre.lymp


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.develop.dayre.lymp.ServiceCommand.*
import com.develop.dayre.lymp.PlayState.*
import android.app.NotificationManager
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*


const val EXTRA_COMMAND = "EXTRA_COMMAND"
enum class ServiceCommand {Start , Stop, Next, Prev, Init}

class LYMPService: LifecycleService()  {
     val requestCode = 9999
    private val TAG = "$APP_TAG/service"

    var notification: Notification? = null
    lateinit var notificationView : RemoteViews
    lateinit var  mBuilder: NotificationCompat.Builder

    private val myBinder = MyLocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : LYMPService {
            return this@LYMPService
        }

    }

    private fun initService() {
        Log.i(TAG, "Try to init service")
        notificationView = RemoteViews(packageName,R.layout.notification)

        //Нотификация
        val title = if ( MainActivity.viewModel.currentSong.value!=null)  MainActivity.viewModel.currentSong!!.value!!.name
        else "no title"
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                ""
            }
        //Кнопки на нотификации
        val callIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        val nextReceive = Intent()
        nextReceive.action = "NEXT_ACTION"
        val pendingIntentNext =
            PendingIntent.getBroadcast(this, requestCode, nextReceive, PendingIntent.FLAG_UPDATE_CURRENT)

        val prevReceive = Intent()
        prevReceive.action = "PREV_ACTION"
        val pendingIntentPrev =
            PendingIntent.getBroadcast(this, requestCode, nextReceive, PendingIntent.FLAG_UPDATE_CURRENT)

        val playReceive = Intent()
        playReceive.action = "PLAY_ACTION"
        val pendingIntentPlay =
            PendingIntent.getBroadcast(this, requestCode, nextReceive, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationView.setOnClickPendingIntent(R.id.next_in_notification,  pendingIntentNext)
        notificationView.setOnClickPendingIntent(R.id.prev_in_notification,  pendingIntentPrev)
        notificationView.setOnClickPendingIntent(R.id.play_in_notification,  pendingIntentPlay)

        notificationView.setTextViewText(R.id.track_name,  title)
        mBuilder = NotificationCompat.Builder(this, channelId )
        notification = mBuilder.setOngoing(true)
            .setCustomContentView(notificationView)
            .setSmallIcon(R.drawable.notification_icon_background)
            .setWhen(0)
            .setContentIntent(callIntent)
            .setAutoCancel(false)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(VISIBILITY_PRIVATE)
            .setContentTitle("LYM-player")
            .setContentText(title)
            .build()
        createObservers()
    }

    override fun onStartCommand(intent :Intent, flags :Int, startId :Int) : Int {
        super.onStartCommand(intent, flags, startId)

        val command = intent.getSerializableExtra(EXTRA_COMMAND) as ServiceCommand

        //Proceed command
        Log.i(APP_TAG, "command = $command")
        when (command) {
            Init -> { initService()}
            ServiceCommand.Stop -> {
                stop()
            }
            Start -> {
                play()
            }
            Next -> {
                next()
            }
            Prev -> {
                prev()
            }
        }

        startForeground(requestCode, notification)

        return START_STICKY
    }

    private fun next() {
        MainActivity.viewModel.nextPress()
    }

    private fun prev() {

    }

    private fun play() {

    }

    private fun stop() {

    }

    private fun createObservers() {
        //TODO Observe play status to change play/pause icon
        MainActivity.viewModel.currentSong.observe(this,
            Observer<Song> {
                Log.i(TAG, "current song updated")
                val settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                settings.edit().putString(APP_PREFERENCES_SELECT_SONG, MainActivity.viewModel.currentSong.value?.ID.toString()).apply()

                notificationView?.setTextViewText(R.id.track_name,  MainActivity.viewModel.currentSong.value?.name)
                startForeground(requestCode, notification)
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}