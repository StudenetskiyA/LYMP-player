package com.develop.dayre.lymp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.exoplayer2.SimpleExoPlayer

class App : Application() {
    companion object {
        val instance = App()
        lateinit var viewModel : LYMPViewModel
        lateinit var realmHelper : RealmHelper
        lateinit var appSettings: SharedPreferences
        lateinit var exoPlayer: SimpleExoPlayer
    }

    fun setApp(context: Context, activity : AppCompatActivity, sharedPreferences: SharedPreferences) {
        setRealmHelper(context)
        setSettings(sharedPreferences)
        setViewModel(activity)
    }

    private fun setViewModel(activity : AppCompatActivity) {
        viewModel = ViewModelProviders.of(activity).get(LYMPViewModel::class.java)
    }

    private fun setRealmHelper (context: Context) {
        realmHelper = RealmHelper(context)
    }

    private fun setSettings(sp : SharedPreferences) {
        appSettings = sp
    }
}