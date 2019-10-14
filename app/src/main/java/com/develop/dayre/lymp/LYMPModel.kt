package com.develop.dayre.lymp

import android.util.Log
import androidx.databinding.BaseObservable
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

enum class RepeatState { All, One, Stop }
enum class PlayState { Play, Stop, Pause }
enum class SortState { ByName, ByAdded, ByListened }
enum class TagsFlag { Or, And }

interface ILYMPModel {
    fun initialize()

    fun newSearch(tags: String): Observable<ArrayList<Song>>

    fun saveSongToDB(song: Song)
    fun nextSong()
    fun prevSong()
    fun changeShuffle()
    fun clearTag()

    fun testAction()
    fun getCurrentSong(): Observable<Song?>
    fun getCurrentSongsList(): ArrayList<Song>
    fun getAllTags(): String
    fun getShuffleStatus(): Boolean
    fun getCurrentSearchTags(): Observable<String>
}

class LYMPModel : ILYMPModel, BaseObservable() {
    private val TAG = "$APP_TAG/model"
    private var currentSong: Song? =
        Song() //У нас бывают ситуации, когда текущий трек не в текущем листе.
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var helper: RealmHelper = RealmHelper(MainActivity.applicationContext())
    private var searchTags = ";"
    private var shuffleStatus: Boolean = false
    private var currentSongPositionInList: Int = 0
        set(value) {
            if (value < currentSongsList.size) {
                field = value
                currentSong = currentSongsList[currentSongPositionInList]
            }
        }

    //Методы для обсерверов
    override fun getShuffleStatus(): Boolean {
        return shuffleStatus
    }

    override fun getAllTags(): String {
        return "rock; pop; techno; jazz; superjazz; technojazz; вскрытие души; оптимально для суицида; осень; дорога"
    }

    override fun getCurrentSongsList(): ArrayList<Song> {
        return currentSongsList
    }

    override fun getCurrentSong(): Observable<Song?> {
        return if (currentSongPositionInList < currentSongsList.size && currentSongPositionInList >= 0)
            Observable.just(currentSongsList[currentSongPositionInList])
        else Observable.just(null)
    }

    override fun getCurrentSearchTags(): Observable<String> {
        return Observable.just(searchTags)
    }

    //Действия
    fun setCurrentSong(songName: String) {
        val s = helper.getSongByName(songName)
        if (s!=null && currentSongsList.contains(s)) {
            currentSong = s
            currentSongPositionInList = currentSongsList.indexOf(s)
        }
    }

    fun setPositionInList(position: Int) {
        currentSongPositionInList = position
    }

    override fun clearTag() {
        val song = currentSong?.copy()
        if (song != null) {
            song.tags = ";"
            helper.writeSong(song)
            createCurrentList() //После удаления тегов трека он может пропасть/появиться в листе.
        }
    }

    override fun newSearch(tags: String): Observable<ArrayList<Song>> {
        Log.i(TAG, "new search")
        searchTags = tags
        createCurrentList()
        //  return Observable.just(currentSongsList).delay(5, TimeUnit.SECONDS)
        return Observable.just(currentSongsList)
    }

    override fun saveSongToDB(song: Song) {
        helper.writeSong(song)
        Log.i(TAG, "Song with name ${song.name} and tags ${song.tags} saved to DB")
    }

    override fun nextSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getNextPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(
                TAG, "Next track ${currentSong?.name} / " +
                        "${currentSong?.tags}"
            )
        }
    }

    override fun prevSong() {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getPrevPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            Log.i(TAG, "Next track name ${currentSong?.name}")
        }
    }

    override fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        Log.i(TAG, "Now shuffle is $shuffleStatus")
    }

    override fun testAction() {
        Log.i(TAG, "testAction")
//        testCounter++
//        getCurrentSong()?.name = "new name"
    }

    override fun initialize() {
        Log.i(TAG, "initialization")
        createCurrentList()
    }

    //Private
    private fun createCurrentList() {
        currentSongsList =
            ArrayList(helper.getSongsFromDBToCurrentSongsList(getListFromString(searchTags)))
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)

        //currentSongPositionInList = 0 - при поиске мы не меняем трек, играет/редактируется тот же, что и был.
    }


}