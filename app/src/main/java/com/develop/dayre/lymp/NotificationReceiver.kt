package com.develop.dayre.lymp


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("LYMP/", intent.action.toString())
        val action = intent.action

        if ("NEXT_ACTION" == action) {
            Log.i(APPLICATION_TAG, "Pressed NEXT")
            val intent = Intent(MainActivity.applicationContext(), LYMPService::class.java)
            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Next)
            MainActivity.applicationContext().startService(intent)
        }
        if ("PREV_ACTION" == action) {
            Log.i(APPLICATION_TAG, "Pressed PREV")
            //amf.lma.playMedia(31)
        }
        if ("PLAY_ACTION" == action) {
            Log.i(APPLICATION_TAG, "Pressed PLAY")
            //amf.lma.playMedia(1)
        }

    }

    companion object {
        val APPLICATION_TAG = "LYMP/notification"
    }
}