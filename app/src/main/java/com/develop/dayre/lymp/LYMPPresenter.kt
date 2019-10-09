package com.develop.dayre.lymp

import android.util.Log

interface IObservable {
    fun notifyObservers()
    
}

interface ILYMPPresenter {
    var model : ILYMPModel
    var view : ILYMPView

    fun nextPress()
    fun prevPress()
    fun playPress()
    fun shufflePress()
    fun repeatPress()
    fun onTrackInListPress(nTrack : Int)
    fun onTrackInListLongPress(nTrack : Int)
    fun tagInSearchPress(tag : String)
    fun tagInEditPress(tag : String)
    fun ratingInSearchPress(rating : Int)
    fun ratingInEditPress(rating : Int)
    fun currentSongEdit(song : Song)


    fun testPress()
}

class LYMPPresenter(override var model: ILYMPModel, override var view: ILYMPView) : ILYMPPresenter{
    private val tag = "$APP_TAG/presenter"

    init {
        view.createView()
        view.createControl()
        model.initialize()
    }

    override fun testPress(){
        Log.i(tag,"testPress")
        model.testAction()
    }

    override fun currentSongEdit(song : Song) {
       model.saveSongToDB(song)
    }

    override fun nextPress() {
        Log.i(tag,"nextPress")
        model.nextSong()
    }

    override fun prevPress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun playPress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shufflePress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}