package com.develop.dayre.lymp

interface ILYMPView {
    fun createView()
    fun createControl()

    //Большую часть полей мы биндим. Со списком треков мне кажется проще вызывать руками, что он изменился.
    fun refreshList()
}
