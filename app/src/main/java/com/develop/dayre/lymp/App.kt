package com.develop.dayre.lymp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager

class App : Application() {
    companion object {
        val instance = App()
    }

    lateinit var viewModel : LYMPViewModel
    lateinit var context : Context
    lateinit var appSettings: SharedPreferences

    fun getAppViewModel() : LYMPViewModel {
        return viewModel
    }

    fun setViewModel(am : AudioManager) {
        viewModel= LYMPViewModel( am)
    }

    fun setAppContext(_context: Context) {
        context = _context
    }

    fun setSettings(sp : SharedPreferences) {
        appSettings = sp
    }
}