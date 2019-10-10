package com.develop.dayre.lymp

import android.content.Context
import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

enum class RepeatState { All, One, Stop }
enum class PlayState { Play, Stop, Pause }
enum class SortState { ByName, ByAdded, ByListened }
enum class TagsFlag { Or, And }

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

    fun newSearch(tags: String)

    fun saveSongToDB(song: Song)
    fun nextSong()
    fun prevSong()
    fun changeShuffle()
    fun clearTag()

    fun testAction()
    fun takeTestCounter(): Int
    fun getCurrentSong(): Song?
    fun getCurrentSongsList(): ArrayList<Song>
    fun getAllTags(): String
    fun getShuffleStatus(): Boolean
    fun getCurrentSearchTags(): String
}

class LYMPModel(context: Context) : ILYMPModel, ILYMPObservable, BaseObservable() {
    override var observersList = ArrayList<ILYMPObserver>()
    private val tag = "$APP_TAG/model"
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var helper: RealmHelper = RealmHelper(context)
    private var searchTags = ";"


    private var shuffleStatus: Boolean = false
        @Bindable set(value) {
            field = value
            notifyObservers()
        }

    private var testCounter: Int = 0
        @Bindable set(value) {
            field = value
            notifyObservers()
        }
    private var currentSongPositionInList: Int = 0
        @Bindable set(value) {
            field = value
            notifyObservers()
        }

    //Методы для обсерверов
    override fun getCurrentSearchTags(): String {
        return searchTags
    }

    override fun notifyObservers() {
        notifyChange()
        for (obs in observersList) {
            obs.update()
        }
    }

    override fun getShuffleStatus(): Boolean {
        return shuffleStatus
    }

    override fun getAllTags(): String {
        return "rock; pop; techno; jazz; superjazz; technojazz"
    }

    override fun getCurrentSongsList(): ArrayList<Song> {
        return currentSongsList
    }

    override fun getCurrentSong(): Song? {
        return if (currentSongPositionInList < currentSongsList.size && currentSongPositionInList >= 0)
            currentSongsList[currentSongPositionInList]
        else null
    }

    override fun takeTestCounter(): Int {
        return testCounter
    }

    override fun clearTag() {
        val song = getCurrentSong()?.copy()
        song?.tags = ";"
        if (song!=null) helper.writeSong(song)
        notifyObservers()
        createCurrentList()
    }
    override fun newSearch(tags: String) {
        searchTags = tags
        createCurrentList()
    }

    override fun saveSongToDB(song: Song) {
        helper.writeSong(song)
        Log.i(tag, "Song with name ${song.name} and tags ${song.tags} saved to DB")
        notifyObservers()
    }

    override fun nextSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getNextPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(tag, "Next track ${currentSongsList[currentSongPositionInList].name} / " +
                    "${currentSongsList[currentSongPositionInList].tags}")
            notifyObservers()
        }
    }

    override fun prevSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getPrevPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(tag, "Next track name ${currentSongsList[currentSongPositionInList].name}")
            notifyObservers()
        }
    }

    override fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        Log.i(tag, "Now shuffle is $shuffleStatus")
    }
    //


    override fun testAction() {
        Log.i(tag, "testAction")
        testCounter++
        getCurrentSong()?.name = "new name"
    }

    override fun initialize() {
        Log.i(tag, "initialization")
    }

    init {
        helper.writeSong(Song("Американская мечта",250,2))
        helper.writeSong(Song("Русская мечта",250,2))
        helper.writeSong(Song("Песня-мечта",250,2))
        helper.writeSong(Song("Голова",250,2))
        helper.writeSong(Song("Песня для всех тегов",250,2))
        helper.writeSong(Song("Песня с очень длинным названием прям вообще",250,2))
        helper.writeSong(Song("99394",250,2))

        createCurrentList()
    }

    private fun createCurrentList() {
        currentSongsList =
            ArrayList(helper.getSongsFromDBToCurrentSongsList(getListFromString(searchTags)))
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        currentSongPositionInList = 0
    }

}