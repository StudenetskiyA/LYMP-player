package com.develop.dayre.lymp

import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

interface ILYMPViewModel {
    fun nextPress()
    fun prevPress()
    fun playPress()
    fun shufflePress()
    fun repeatPress()
    fun clearTagPress()

    fun onTrackInListPress(nTrack: Int)
    fun onTrackInListLongPress(nTrack: Int)
    fun tagInSearchPress(tag: String)
    fun tagInEditPress(tag: String)
    fun ratingInSearchPress(rating: Int)
    fun ratingInEditPress(rating: Int)
    fun currentSongEdit(song: Song)

    fun testPress()
}

class LYMPViewModel(audioManager: AudioManager) : ILYMPViewModel, ViewModel() {
    private val TAG = "$APP_TAG/viewmodel"

    private var model = LYMPModel(audioManager)

    var currentSongsList =
        MutableLiveData<ArrayList<Song>>() //Локальный список, потом будет урезанная версия.
    val isLoadingSongsList = ObservableField<Boolean>(false)
    val isLoadingFilesList = ObservableField<Boolean>(false)
    var currentSong = MutableLiveData<Song>()
    var currentSearchTags = MutableLiveData<String>()
    val shuffle = ObservableField<Boolean>()
    val repeat = ObservableField<RepeatState>()
    val sort = ObservableField<SortState>()
    val andOr = ObservableField<AndOrState>()
    var isShowMore = ObservableField<Boolean>(false)

    fun startModel() {
        model.initialize()
        startBrowseFolderForFiles()
    }

    fun getAllTags(): String {
        return model.allTags
    }

    override fun testPress() {
        Log.i(TAG, "testPress")
        model.testAction()
    }

    fun getCurrentTrackDuration() : Int{
        return model.duration.get()!!
    }

    override fun currentSongEdit(song: Song) {
        model.saveSongToDB(song)
        setCurrentSong()
        newSearch()
    }

    fun showMorePress() {
        Log.i(TAG, "showMorePress")
        if (isShowMore.get()==false) {
            if (currentSong.value != null) {
                isShowMore.set(true)
            }
        } else  isShowMore.set(false)
    }

    fun jumpToPosition(position : Int){
        model.jumpToPosition(position)
    }

    override fun nextPress() {
        Log.i(TAG, "nextPress")
        model.nextSong()
        setCurrentSong()
    }

    override fun prevPress() {
        Log.i(TAG, "prevPress")
        model.prevSong()
        setCurrentSong()
    }

    override fun clearTagPress() {
        Log.i(TAG, "clearTagPress")
        model.clearTag()
    }

    fun getIsPlaying() : PlayState {
        return model.isPlaying
    }
    fun getCallBackAwaited() : Boolean {
        return model.callBackAwaited
    }
    fun setCallBackAwaited(value :Boolean) {
        model.callBackAwaited = value
    }
    override fun playPress() {
        Log.i(TAG, "playPress")
            model.play()
    }

    fun stopPress() {
        Log.i(TAG, "stopPress")
        model.stop()
    }

    fun setMediaSessonCallback(mediaSessionCallback : MediaSessionCompat.Callback) {
        model.setMediaSessonCallback(mediaSessionCallback)
    }
    fun setMediaControllerCallback(mediaControllerCallback: MediaControllerCompat.Callback) {
        model.setMediaControllerCallback(mediaControllerCallback)
    }

    override fun shufflePress() {
        model.changeShuffle()
        shuffle.set(model.getShuffleStatus())
    }
    override fun repeatPress() {
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
    fun setShuffle(newShuffleStatus : Boolean) {
        model.setShuffleStatus(newShuffleStatus)
        shuffle.set(model.getShuffleStatus())
    }
    fun setRepeat(newRepeatStatus : RepeatState) {
        model.setRepeatStatus(newRepeatStatus)
        repeat.set(model.getRepeatStatus())
    }
    fun setSort(newSortStatus : SortState) {
        model.setSortStatus(newSortStatus)
        sort.set(model.getSortStatus())
    }
    fun setAndOr(newAndOrStatus : AndOrState) {
        model.setAndOrStatus(newAndOrStatus)
        andOr.set(model.getAndOrStatus())
    }

    //Вызывается вью при новом поиске. TODO Добавить сюда остальные критерии поиска.
    fun newSearch(searchTags: String = "", clearSearch: Boolean = false) {
        Log.i(TAG, "load songs, $searchTags , ${currentSearchTags.value}")
        isLoadingSongsList.set(true)
        var tags = if (searchTags!="") searchTags
        else currentSearchTags.value
        if (clearSearch) {
            tags = ";"
        }

        if (tags!=null)
        model.newSearch(tags)
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

    //Используется для выбора песни не из текущего листа. Например, при загрузке из настроек.
    fun songSelectByID(songID: String) {
        model.setCurrentSongByID(songID)
        setCurrentSong()
    }

    override fun onTrackInListPress(nTrack: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTrackInListLongPress(nTrack: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun tagInSearchPress(tag: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun tagInEditPress(tag: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ratingInSearchPress(rating: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ratingInEditPress(rating: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    fun startBrowseFolderForFiles() {
        Log.i(TAG, "load files list")
        isLoadingFilesList.set(true)
        model. browseFolderForFiles()
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

    fun getMediaSessionToken() : MediaSessionCompat.Token{
        return model.getMediaSessionToken()
    }
    fun getMediaSession() : MediaSessionCompat{
        return model.mediaSession
    }

    fun setAllTagsFromSettings(t: String) {
        model.setAllTagsFromSettings(t)
    }
}