package com.develop.dayre.lymp


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    val TAG = "$APP_TAG/notification"

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("LYMP/", intent.action.toString())
        val action = intent.action

        if ("NEXT_ACTION" == action) {
            Log.i(TAG, "Pressed NEXT")
            val intent = Intent(MainActivity.applicationContext(), LYMPService::class.java)
            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Next)
            MainActivity.applicationContext().startService(intent)
        }
        if ("PREV_ACTION" == action) {
            Log.i(TAG, "Pressed PREV")
            //amf.lma.playMedia(31)
        }
        if ("PLAY_ACTION" == action) {
            Log.i(TAG, "Pressed PLAY")
            //amf.lma.playMedia(1)
        }

    }

}