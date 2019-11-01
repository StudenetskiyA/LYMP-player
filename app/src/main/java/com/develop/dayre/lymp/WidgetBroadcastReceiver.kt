package com.develop.dayre.lymp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProviders

//Widget receiver
val WIDGET_ACTION_PLAY_PAUSE = "com.zaycevnet.skyfolk.zwidget.playpause"
 val WIDGET_ACTION_NEXT = "com.zaycevnet.skyfolk.zwidget.next"
 val WIDGET_ACTION_PREV = "com.zaycevnet.skyfolk.zwidget.prev"
 val WIDGET_ACTION_LIKE = "com.zaycevnet.skyfolk.zwidget.like"

class WidgetBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "$APP_TAG/view"
    private lateinit var viewModel : LYMPViewModel

    override fun onReceive(contxt: Context?, intent: Intent?) {
        Log.i(TAG, "Broadcast something.")
        viewModel  = App.instance.getAppViewModel()
        when (intent?.action) {
            WIDGET_ACTION_PLAY_PAUSE -> {
                Log.i(TAG, "Broadcast from widget play/pause.")
                viewModel.playPress()
            }
        }
    }
}
