package com.develop.dayre.lymp

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.databinding.BaseObservable
import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

enum class RepeatState { All, One, Stop }
enum class SortState { ByName, ByAdded, ByListened }
enum class AndOrState { Or, And }

class LYMPModel : BaseObservable() {
    private val TAG = "$APP_TAG/model"
    private var currentSong: Song? =
        Song() //У нас бывают ситуации, когда текущий трек не в текущем листе.
    private var currentSongsList = ArrayList<Song>()
    private var currentSongsShuffledListNumber = ArrayList<Int>()
    private var currentSongsAdditionListNumber = ArrayList<Int>()

    var helper: RealmHelper = App.realmHelper
    private var searchTags = ";"
    private var antiSearchTags = ";"
    private var searchName = ""
    private var shuffleStatus: Boolean = false
    private var repeatStatus = RepeatState.All
    private var sortStatus = SortState.ByName
    private var andOrStatus = AndOrState.Or

    private var searchMinRating = 0

    private var currentSongPositionInList: Int = 0
        set(value) {
            if (value < currentSongsList.size) {
                field = value
                currentSong = currentSongsList[currentSongPositionInList]
            }
        }

    var allTags = ""
    lateinit var context: Context

    //Методы для обсерверов
    fun getShuffleStatus(): Boolean {
        return shuffleStatus
    }
    fun getRepeatStatus(): RepeatState {
        return repeatStatus
    }
    fun getSortStatus(): SortState {
        return sortStatus
    }
    fun getAndOrStatus(): AndOrState {
        return andOrStatus
    }

    fun getCurrentSong(): Observable<SongOrNull> {
        return if (currentSong!=null)
            Observable.just(SongOrNull(currentSong!!))
        //In Rx2 I can't do Observable.just(null)!
        else Observable.just(SongOrNull(Song(), true))
    }

    fun getCurrentSongNew(): Song? {
        return currentSong
    }

    fun getCurrentSearchTags(): Observable<String> {
        return Observable.just(searchTags)
    }
    fun getCurrentAntiSearchTags(): Observable<String> {
        return Observable.just(antiSearchTags)
    }

    //Действия
    fun browseFolderForFiles(): Observable<Boolean> {
        var newSongFound = 0
        var songsRestored = 0
        var deletedSong = 0
        val dateTime = SimpleDateFormat("dd/M/yyyy HH:mm").format(Date())
        //Получаем список всех файлов в папке и подпапках
        //TODO Поменять путь на недефолтный при загрузке из настроек.
        val allFiles = getFilesListInFolderAndSubFolder(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "mp3"
        )
        Log.i(TAG, "/browse - File found ${allFiles.size}")
        val allRecord = helper.getAllSongsFileExist()
        Log.i(TAG, "/browse - Record with isExist found ${allRecord.size}")
        //Проверяем, каких файлов больше физически нет и помечаем их как отсутствующие, из базы не удаляем.
        for (r in allRecord) {
            if (!allFiles.contains(r.path)) {
                deletedSong++
                val w = r.copy()
                w.isFileExist = false
                helper.writeSong(w)
            }
        }
        //Проверяем, каких нет в базе или они помечены isFileExist=false и добавляем их.
        for (f in allFiles) {
            val s = helper.getSongByID(getHashFromNameAndSize(f.getNameFromPath(), getFileSize(f)))
            if (s == null) {
                newSongFound++
                helper.writeSong(
                    Song(
                        ID = getHashFromNameAndSize(
                            f.getNameFromPath(),
                            getFileSize(f)
                        ),
                        name = f.getNameFromPath(),
                        path = f,
                        isFileExist = true,
                        added = dateTime,
                        lenght = getAudioFileDuration(f)
                    )
                )
            }
            if (s != null && !s.isFileExist) {
                songsRestored++
                s.isFileExist = true
                s.path = f
                s.added = dateTime
                helper.writeSong(s)
            }
        }
        //И уведомление с результатами
        if (songsRestored!=0 || newSongFound!=0 || deletedSong!=0)
            context
                .toast("Новых песен найдено - $newSongFound \r\nУдалено песен - $deletedSong \r\nВосстановленно удаленных - $songsRestored")
        Log.i(
            TAG,
            "Новых песен найдено - $newSongFound \r\nУдалено песен - $deletedSong \r\nВосстановленно удаленных - $songsRestored"
        )
        return Observable.just(true)
    }

    fun setCurrentSongByID(songID: String) {
        val s = helper.getSongByID(songID)
        if (s != null && currentSongsList.contains(s)) {
            currentSong = s
            currentSongPositionInList = currentSongsList.indexOf(s)
        }
    }

    fun setPositionInList(position: Int) {
        currentSongPositionInList = position
        App.appSettings.edit().putString(
            APP_PREFERENCES_SELECT_SONG,
            currentSong?.ID
        ).apply()
    }

    fun clearTag() {
        val song = currentSong?.copy()
        if (song != null) {
            song.tags = ";"
            helper.writeSong(song)
            createCurrentList() //После удаления тегов трека он может пропасть/появиться в листе.
        }
    }

    fun newSearch(tags: String = "", antiTags: String = "", searchName: String = "", fromAdditionList:Boolean = false): Observable<ArrayList<Song>> {
        Log.i(TAG, "new search")
        searchTags = tags
        antiSearchTags = antiTags
        this.searchName = searchName
        if (!fromAdditionList) clearAdditionList()
        createCurrentList()
        return Observable.just(currentSongsList)
    }

    fun saveSongToDB(song: Song) {
        helper.writeSong(song)
        Log.i(TAG, "Song with name ${song.name}, tags ${song.tags} and rating ${song.rating} saved to DB")
    }

    fun getAfterSong(): Song? {
        if (repeatStatus==RepeatState.Stop) return null
        else if (repeatStatus==RepeatState.One) return currentSong
        else {
            if (currentSongsList.isNotEmpty()) {
                currentSongPositionInList = getNextPositionInList(
                    currentSongsShuffledListNumber, currentSongsAdditionListNumber,
                    currentSongPositionInList,
                    shuffleStatus
                )
                if (currentSongsAdditionListNumber.isNotEmpty()) {
                    for (position in currentSongsAdditionListNumber) {
                        val s = currentSongsList[position].copy()
                        s.order = s.order - 1
                        helper.writeSong(s)
                    }
                    currentSongsAdditionListNumber.removeAt(0)
                }

                return currentSong
            }
        }
        return null
    }

    fun getNextSong(): Song? {
            if (currentSongsList.isNotEmpty()) {
                currentSongPositionInList = getNextPositionInList(
                    currentSongsShuffledListNumber, currentSongsAdditionListNumber,
                    currentSongPositionInList,
                    shuffleStatus
                )
                if (currentSongsAdditionListNumber.isNotEmpty()) {
                    for (position in currentSongsAdditionListNumber) {
                        val s = currentSongsList[position].copy()
                        s.order = s.order - 1
                        helper.writeSong(s)
                    }
                    currentSongsAdditionListNumber.removeAt(0)
                }

                return currentSong
            }
        return null
    }

    fun getPreviousSong(): Song? {
        if (currentSongsList.isNotEmpty()) {
            currentSongPositionInList = getPrevPositionInList(
                currentSongsShuffledListNumber,
                currentSongPositionInList,
                shuffleStatus
            )
            if (currentSongsAdditionListNumber.isNotEmpty()) {
                for (position in currentSongsAdditionListNumber) {
                    val s = currentSongsList[position].copy()
                    s.order = s.order - 1
                    helper.writeSong(s)
                }
                currentSongsAdditionListNumber.removeAt(0)
            }

            return currentSong
        }
        return null
    }

    fun increaseListenedTimeOfCurrentTrack(n: Double) {
        val cs = currentSong!!.copy()
        cs.listenedTimes += n
        helper.writeSong(cs)
    }

    fun changeShuffle() {
        shuffleStatus = !shuffleStatus
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
        App.appSettings.edit()
            .putBoolean(APP_PREFERENCES_SHUFFLE, shuffleStatus)
            .apply()
        Log.i(TAG, "Now shuffle is $shuffleStatus")
    }
    fun changeRepeat() {
        //TODO
        var rs = 0
        when (repeatStatus) {
            RepeatState.All -> {
                repeatStatus = RepeatState.One
                rs = 1
            }
            RepeatState.One -> {
                repeatStatus = RepeatState.Stop
                rs = 2
            }
            RepeatState.Stop -> {
                repeatStatus = RepeatState.All
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_REPEAT, rs)
            .apply()
        Log.i(TAG, "Now repeat is $repeatStatus")
    }
    fun changeSort() {
        //TODO
        var ss = 0
        when (sortStatus) {
            SortState.ByName -> {
                sortStatus = SortState.ByListened
                ss = 1
            }
            SortState.ByListened -> {
                sortStatus = SortState.ByAdded
                ss = 2
            }
            SortState.ByAdded -> {
                sortStatus = SortState.ByName
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_SORT, ss)
            .apply()
        createCurrentList()
        Log.i(TAG, "Now sortStatus is $sortStatus")
    }
    fun changeAndOr() {
        andOrStatus =
        when (andOrStatus) {
            AndOrState.Or -> {
                 AndOrState.And
            }
            AndOrState.And -> {
                AndOrState.Or
            }
        }
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_ANDOR, andOrStatus.ordinal)
            .apply()
        createCurrentList()
        Log.i(TAG, "Now andOrStatus is $andOrStatus")
    }
    fun setShuffleStatus(newShuffleStatus: Boolean) {
        shuffleStatus = newShuffleStatus
    }
    fun setRepeatStatus(newRepeatStatus: RepeatState) {
        repeatStatus = newRepeatStatus
    }
    fun setSortStatus(newSortStatus: SortState) {
        sortStatus = newSortStatus
        createCurrentList()
    }
    fun setAndOrStatus(newAndOrStatus: AndOrState) {
        andOrStatus = newAndOrStatus
        createCurrentList()
    }
    fun setSearchRating(rating: Int, withoutNewSearch: Boolean = false) {
        searchMinRating = rating
        App.appSettings.edit()
            .putInt(APP_PREFERENCES_SEARCH_MIN_RATING, searchMinRating)
            .apply()
        if (!withoutNewSearch) createCurrentList()
    }

    fun initialize(context: Context) {
        Log.i(TAG, "initialization")
        this.context = context
    }

    private fun clearAdditionList() {
        for (position in currentSongsAdditionListNumber) {
            val s = currentSongsList[position].copy()
            s.order=0
            helper.writeSong(s)
        }
        currentSongsAdditionListNumber = ArrayList()
    }

    //Private
    private fun createCurrentList() {
        currentSongsList =
            ArrayList(
                helper.getSongsFromDBToCurrentSongsList(
                    getListFromString(searchTags), getListFromString(antiSearchTags), searchName = searchName,
                    sort = sortStatus, minRating = searchMinRating, andOrFlag = andOrStatus
                )
            )
        currentSongsShuffledListNumber = getShuffledListOfInt(currentSongsList.size)
    }

    fun setAllTagsFromSettings(t: String) {
        allTags = t
    }

    fun addSongToAdditionList(position: Int) {
        if (currentSongPositionInList!=position) {
            val s = currentSongsList[position].copy()
            if (!currentSongsAdditionListNumber.contains(position)) {
                currentSongsAdditionListNumber.add(position)
                s.order = currentSongsAdditionListNumber.size
            } else {
                currentSongsAdditionListNumber.remove(position)
                s.order = 0
            }
            helper.writeSong(s)
        }
    }

}