package com.develop.dayre.lymp

import android.content.Context
import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

enum class RepeatState {All, One, Stop}
enum class PlayState {Play , Stop, Pause}
enum class SortState {ByName, ByDate, ByCount}

interface ILYMPObserver {
    //Надо подумать, что именно передавать.
    //Возможно, будет переменная что-то в духе allData либо несколько вариантов
    //update, если мы не хотм обновлять всегда все разом.
    fun update()
}

interface ILYMPObservable {
    var observersList: ArrayList<ILYMPObserver>

    fun addObserver(newObserver: ILYMPObserver) {
        observersList.add(newObserver)
    }

    fun removeObserver(remObserver: ILYMPObserver) {
        if (observersList.contains(remObserver)) observersList.remove(remObserver)
    }

    fun notifyObservers()
}

interface ILYMPModel {
    fun initialize()

    fun saveSongToDB(song: Song)
    fun nextSong()
    fun prevSong()
    fun changeShuffle()

    fun testAction()
    fun takeTestCounter() : Int
    fun getCurrentSong() : Song?
    fun getCurrentSongsList() : ArrayList<Song>
    fun getAllTags() : String
    fun getShuffleStatus() :Boolean
}

class LYMPModel(context: Context) : ILYMPModel, ILYMPObservable, BaseObservable(){
    override var observersList =  ArrayList<ILYMPObserver>()
    private val tag = "$APP_TAG/model"
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var helper : RealmHelper = RealmHelper(context)

    private var shuffleStatus : Boolean = false
        @Bindable set(value) {
            field = value
            notifyObservers()
        }

    private var testCounter: Int = 0
        @Bindable set(value) {
            field = value
            notifyObservers()
        }
    private var currentSongPositionInList : Int  = 0
        @Bindable set(value) {
            field = value
            notifyObservers()
        }

    //Методы для обсерверов
    override fun notifyObservers() {
        notifyChange()
        for (obs in observersList) {
            obs.update()
        }
    }
    override fun getShuffleStatus() : Boolean {
        return shuffleStatus
    }
    override fun getAllTags(): String {
       return "rock; pop; techno; jazz; "
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
        helper.writeSong(song)
        Log.i(tag, "Song with name ${song.name} and tags ${song.tags} saved to DB")
    }
    override fun nextSong() {
        currentSongPositionInList = getNextPositionInList(currentSongsShuffledListNumber,currentSongPositionInList,shuffleStatus)
        Log.i(tag, "Next track name ${currentSongsList[currentSongPositionInList].name}")
    }
    override fun prevSong() {
        currentSongPositionInList = getPrevPositionInList(currentSongsShuffledListNumber,currentSongPositionInList,shuffleStatus)
        Log.i(tag, "Next track name ${currentSongsList[currentSongPositionInList].name}")
    }
    override fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        Log.i(tag, "Now shuffle is $shuffleStatus")
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
        currentSongsList = helper.getAllSongs()
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)

//        currentSongsList.add(Song("1 name",100))
//        currentSongsList.add(Song("2 name",120))
//        currentSongsList.add(Song("3 name",130))
//        currentSongsList.add(Song("4 name",140))
//        currentSongsList.add(Song("5 name",150))
    }


}