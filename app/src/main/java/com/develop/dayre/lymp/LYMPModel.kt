package com.develop.dayre.lymp

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

interface ILYMPModel {
    fun initialize()

    fun saveSongToDB(song: Song)

    fun testAction()
    fun takeTestCounter() : Int
    fun getCurrentSong() : Song?
    fun getCurrentSongsList() : ArrayList<Song>
    fun getAllTags() : String
}

class LYMPModel : ILYMPModel, BaseObservable(){
    private val tag = "$APP_TAG/model"
    private var currentSongsList = ArrayList<Song>()
    private var currentSongPositionInList  = 0

    private var testCounter: Int = 0
        @Bindable set(value) {
            field = value
            notifyChange()
        }

    //Методы для обсерверов
    override fun getAllTags(): String {
       return "rock; pop; techno"
    }
    override fun getCurrentSongsList(): ArrayList<Song> {
        return currentSongsList
    }

    override fun getCurrentSong(): Song? {
        return if (currentSongPositionInList<currentSongsList.size && currentSongPositionInList>=0)
            currentSongsList[currentSongPositionInList]
        else null
    }

    override fun takeTestCounter() : Int {
        return testCounter
    }
    //
    override fun saveSongToDB(song: Song) {
        Log.i(tag, "Song with name ${song.name} and tags ${song.tags} saved to DB")
    }
    //


    override fun testAction() {
        Log.i(tag,"testAction")
        testCounter++
        getCurrentSong()?.name = "new name"
    }

    override fun initialize() {

    }

    init {
        currentSongsList.add(Song("list name"))
    }
}