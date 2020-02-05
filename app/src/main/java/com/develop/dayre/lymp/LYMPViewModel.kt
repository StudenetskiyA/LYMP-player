package com.develop.dayre.lymp

import android.content.Context
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class LYMPViewModel : ViewModel() {
    private val TAG = "$APP_TAG/viewmodel"

    private var model = LYMPModel()

    var currentSongsList =
        MutableLiveData<ArrayList<Song>>() //Локальный список, потом будет урезанная версия.
    val isLoadingSongsList = ObservableField<Boolean>(false)
    val isLoadingFilesList = ObservableField<Boolean>(false)
    var currentSong = MutableLiveData<Song>()
    var currentSearchTags = MutableLiveData<String>()
    var currentAntiSearchTags = MutableLiveData<String>()
    val shuffle = ObservableField<Boolean>()
    val repeat = ObservableField<RepeatState>()
    val sort = ObservableField<SortState>()
    val andOr = ObservableField<AndOrState>()
    var isShowMore = ObservableField<Boolean>(false)

    var mLastPlaybackStatePosition: Int = 0
    var mLastPlaybackStatePositionTime: Int = 0

    fun startModel(context: Context) {
        model.initialize(context)
        startBrowseFolderForFiles()
    }

    fun getAllTags(): String {
        return model.allTags
    }

    fun currentSongEdit(song: Song) {
        model.saveSongToDB(song)
        setCurrentSong()
        //newSearch()
    }

    fun showMorePress() {
        Log.i(TAG, "showMorePress")
        if (isShowMore.get() == false) {
            if (currentSong.value != null) {
                isShowMore.set(true)
            }
        } else isShowMore.set(false)
    }

    fun getAfterSong(): Song? {
        return model.getAfterSong()
    }
    fun getNextSong(): Song? {
        return model.getNextSong()
    }
    fun getPreviousSong(): Song? {
        return model.getPreviousSong()
    }

    fun increaseListenedTimeOfCurrentTrack(n: Double) {
        model.increaseListenedTimeOfCurrentTrack(n)
    }

    //Press
    fun songInListLongPress(position: Int) {
        model.addSongToAdditionList(position)
        newSearch(fromAdditionList = true)
    }
    fun clearTagPress() {
        Log.i(TAG, "clearTagPress")
        model.clearTag()
    }
    fun shufflePress() {
        model.changeShuffle()
        shuffle.set(model.getShuffleStatus())
    }
    fun repeatPress() {
        model.changeRepeat()
        repeat.set(model.getRepeatStatus())
    }
    fun sortPress() {
        model.changeSort()
        sort.set(model.getSortStatus())
        newSearch()
    }
    fun andOrPress() {
        model.changeAndOr()
        andOr.set(model.getAndOrStatus())
        newSearch()
    }
    fun setSearchRating(rating: Int, withoutNewSearch: Boolean = false) {
        model.setSearchRating(rating, withoutNewSearch)
        if (!withoutNewSearch) newSearch()
    }

    //Для восстановления из настроек
    fun setShuffle(newShuffleStatus: Boolean) {
        model.setShuffleStatus(newShuffleStatus)
        shuffle.set(model.getShuffleStatus())
    }
    fun setRepeat(newRepeatStatus: RepeatState) {
        model.setRepeatStatus(newRepeatStatus)
        repeat.set(model.getRepeatStatus())
    }
    fun setSort(newSortStatus: SortState) {
        model.setSortStatus(newSortStatus)
        sort.set(model.getSortStatus())
    }
    fun setAndOr(newAndOrStatus: AndOrState) {
        model.setAndOrStatus(newAndOrStatus)
        andOr.set(model.getAndOrStatus())
    }
    fun setAllTagsFromSettings(t: String) {
        model.setAllTagsFromSettings(t)
    }
    fun setSelectedSongById(songID: String) {
        model.setCurrentSongByID(songID)
        setCurrentSong()
    }

    //Вызывается вью при новом поиске.
    fun newSearch(
        searchTags: String = "",
        antiSearchTags: String = "",
        searchName: String = "",
        clearSearch: Boolean = false,
        fromAdditionList: Boolean = false
    ) {
        Log.i(TAG, "load songs, $searchTags , ${currentSearchTags.value}")
        isLoadingSongsList.set(true)
        var tags = if (searchTags != "") searchTags
        else currentSearchTags.value
        var antiTags = if (antiSearchTags != "") antiSearchTags
        else currentAntiSearchTags.value

        if (clearSearch) {
            tags = ";"
            antiTags = ";"
        }

        if (tags != null && antiTags != null)
            model.newSearch(tags, antiTags, searchName, fromAdditionList)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<ArrayList<Song>> {
                    override fun onSubscribe(d: Disposable) {
                        //todo
                    }

                    override fun onError(e: Throwable) {
                        //todo
                    }

                    override fun onNext(data: ArrayList<Song>) {
                        currentSongsList.value = data
                    }

                    override fun onComplete() {
                        Log.i(TAG, "songs list updated complite")
                        isLoadingSongsList.set(false)
                    }
                })
        setCurrentSearchTags()
    }

    //Выбор песни из текущего листа по номеру в листе. Например, при нажатии на трек в списке.
    fun songInListPress(positionInList: Int) {
        model.setPositionInList(positionInList)
        setCurrentSong()
    }

    //Локальный метод. Вызывается при обновлении текущих тегов поиска.
    private fun setCurrentSearchTags() {
        model.getCurrentSearchTags()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                    //todo
                }

                override fun onError(e: Throwable) {
                    //todo
                }

                override fun onNext(data: String) {
                    Log.i(TAG, "current song update")
                    currentSearchTags.value = data
                }

                override fun onComplete() {
                }
            })
        model.getCurrentAntiSearchTags()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                    //todo
                }

                override fun onError(e: Throwable) {
                    //todo
                }

                override fun onNext(data: String) {
                    Log.i(TAG, "current song update")
                    currentAntiSearchTags.value = data
                }

                override fun onComplete() {
                }
            })
    }

    fun getCurrentSong(): Song? {
        return model.getCurrentSongNew()
    }

    //Используется, когда у нас будет меняться текущий выбранный трек и надо подгрузить информацию о новом.
    fun setCurrentSong() {
        model.getCurrentSong()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<SongOrNull> {
                override fun onSubscribe(d: Disposable) {
                    //todo
                }

                override fun onError(e: Throwable) {
                    //todo
                }

                override fun onNext(data: SongOrNull) {
                    Log.i(TAG, "current song update")
                    if (!data.isNull)
                        currentSong.value = data.song
                }

                override fun onComplete() {
                }
            })
    }

    private fun startBrowseFolderForFiles() {
        Log.i(TAG, "load files list")
        isLoadingFilesList.set(true)
        model.browseFolderForFiles()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Boolean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {

                }

                override fun onNext(data: Boolean) {
                }

                override fun onComplete() {
                    isLoadingFilesList.set(false)
                    Log.i(TAG, "initial load complete.")
                }
            })
    }
}