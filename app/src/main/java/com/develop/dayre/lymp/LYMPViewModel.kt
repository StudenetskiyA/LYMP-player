package com.develop.dayre.lymp

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

interface ILYMPViewModel {
    fun newSearch(tags: String = "")
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

class LYMPViewModel : ILYMPViewModel, ViewModel() {
    private val TAG = "$APP_TAG/viewmodel"

    private var model = LYMPModel()

    var currentSongsList = MutableLiveData<ArrayList<Song>>() //Локальный список, потом будет урезанная версия.
    val isLoadingSongsList = ObservableField<Boolean>(true)
    var currentSong = MutableLiveData<Song>()
    var currentSearchTags = MutableLiveData<String>()
    val shuffle = ObservableField<Boolean>()

    fun startModel() {
        model.initialize()
    }

    fun getAllTags(): String {
        return model.getAllTags()
    }

    override fun testPress() {
        Log.i(TAG, "testPress")
        model.testAction()
    }

    override fun currentSongEdit(song: Song) {
        model.saveSongToDB(song)
        setCurrentSong()
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

    override fun playPress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shufflePress() {
        model.changeShuffle()
        shuffle.set(model.getShuffleStatus())
    }

    //Вызывается вью при новом поиске. TODO Добавить сюда остальные критерии поиска.
    override fun newSearch(tags: String) {
        Log.i(TAG, "load ongs")
        isLoadingSongsList.set(true)

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
                    Log.i(TAG, "ongs list updated complite")
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
    fun songSelect(songName: String) {
        model.setCurrentSong(songName)
        setCurrentSong()
    }

    fun songSelectByID(songID: Long) {
        model.setCurrentSongByID(songID)
        setCurrentSong()
    }

    override fun repeatPress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    private fun setCurrentSong() {
        model.getCurrentSong()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Song?> {
                override fun onSubscribe(d: Disposable) {
                    //todo
                }

                override fun onError(e: Throwable) {
                    //todo
                }

                override fun onNext(data: Song) {
                    Log.i(TAG, "current song update")
                    currentSong.value = data
                }

                override fun onComplete() {
                }
            })
    }
}