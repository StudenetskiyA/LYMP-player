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

const val EXTRA_COMMAND = "EXTRA_COMMAND"
enum class ServiceCommand {Start , Stop, Next, Prev}

class LYMPService: Service()  {
    private val TAG = "$APP_TAG/service"

    private val myBinder = MyLocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : LYMPService {
            return this@LYMPService
        }

    }

    override fun onStartCommand(intent :Intent, flags :Int, startId :Int) : Int {
        Log.i(TAG, "Try to start service")

        val command = intent.getSerializableExtra(EXTRA_COMMAND) as ServiceCommand

        val callIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        val title = if ( MainActivity.model.getCurrentSong()!=null)  MainActivity.model.getCurrentSong()!!.name
        else "no title"

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                  ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )

        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.notification_icon_background)
            .setWhen(0)
            .setContentIntent(callIntent)
            .setAutoCancel(false)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentTitle("LYM-player")
            .setContentText(title)
            .build()

        //Proceed command
        Log.i(APP_TAG, "command = $command")
        when (command) {
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

        bindUI()

        startForeground(9595, notification)
        return START_STICKY
    }

    private fun next() {

    }

    private fun prev() {

    }

    private fun play() {

    }

    private fun stop() {

    }

    private fun bindUI() {

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