package com.develop.dayre.lymp

import android.app.Application
import android.content.Context
import android.media.AudioManager

class App : Application() {
    companion object {
        val instance = App()
    }

    //val viewModel : LYMPViewModel by lazy {LYMPViewModel(this, getSystemService(Context.AUDIO_SERVICE) as AudioManager)}
    lateinit var viewModel : LYMPViewModel
    lateinit var context : Context

    fun getAppViewModel() : LYMPViewModel {
        return viewModel
    }

    fun setViewModel(am : AudioManager) {
        viewModel= LYMPViewModel(this, am)
    }

    fun setAppContext(_context: Context) {
        context = _context
    }
}