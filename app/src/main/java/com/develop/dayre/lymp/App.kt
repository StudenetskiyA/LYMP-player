package com.develop.dayre.lymp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

class App : Application() {
    companion object {
        val instance = App()
        lateinit var viewModel : LYMPViewModel
        lateinit var realmHelper : RealmHelper
        lateinit var appSettings: SharedPreferences
    }

    fun setApp(context: Context, activity : AppCompatActivity, audioManager: AudioManager, sharedPreferences: SharedPreferences) {
        setRealmHelper(context)
        setSettings(sharedPreferences)
        setViewModel(audioManager, activity)
    }

    class MyViewModelFactory(private val audioManager: AudioManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(AudioManager::class.java).newInstance(audioManager)
        }
    }

    private fun setViewModel(am : AudioManager, activity : AppCompatActivity) {
        val viewModelFactory = MyViewModelFactory(am)
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(LYMPViewModel::class.java)
    }

    private fun setRealmHelper (context: Context) {
        realmHelper = RealmHelper(context)
    }

    private fun setSettings(sp : SharedPreferences) {
        appSettings = sp
    }
}