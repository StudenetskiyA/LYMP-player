package com.develop.dayre.lymp.service

import com.develop.dayre.lymp.App
import com.develop.dayre.lymp.Song

class MusicRepository {
    fun getNextSong(): Song? {
        return App.viewModel.getNextSong()
    }

    fun getAfterSong(): Song? {
        return App.viewModel.getAfterSong()
    }

    fun getPreviousSong(): Song? {
        return App.viewModel.getPreviousSong()
    }

    fun getCurrentSong(): Song? {
        return App.viewModel.getCurrentSong()
    }
}
