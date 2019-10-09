package com.develop.dayre.lymp

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

interface ILYMPModel {
    fun initialize()

    fun testAction()
    fun takeTestCounter() : Int
}

class LYMPModel : ILYMPModel, BaseObservable(){
    private val tag = "$APP_TAG/model"

    private var testCounter: Int = 0
        @Bindable set(value) {
            field = value
            notifyChange()
        }

    override fun takeTestCounter() : Int {
        return testCounter
    }

    override fun testAction() {
        Log.i(tag,"testAction")
        testCounter++
    }

    override fun initialize() {}
}