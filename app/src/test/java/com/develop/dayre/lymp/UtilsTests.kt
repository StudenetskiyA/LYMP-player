package com.develop.dayre.lymp

import androidx.databinding.ObservableField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test

class UtilsTests {
    private val tag = "$APP_TAG/tests"

    val n = ObservableField<Int>(0)


    @Test
    fun getTimeFromSecondsTest() {
        var seconds = 85
        assertEquals("1:25",getTimeFromSeconds(seconds))
        seconds = 3600+85
        assertEquals("1:1:25",getTimeFromSeconds(seconds))
    }

    @Test
    fun changeVarInFunTest() {

        fun change(b:ObservableField<Int>) {
            b.set(7)
        }
        change(n)
        println ("CHANGETEST ${n.get()}")
    }

    @Test
    fun getNameFromPathTest() {
        assertEquals("name", "/path/path/name.mp3".getNameFromPath())
    }

    @Test
    fun getShuffledListOfIntTest() {
        val list = getShuffledListOfInt(10)
        for (l in list)
            println("$tag , $l")
    }

    @Test
    //Здесь сейчас нет assert, надо смотреть глазами - мы не знаем, как пошафлится лист
    //Можно, конечно, сделать симуляцию шафла с предсказуемым результатом, но пока не вижу смысла.
    fun getNextPositionInListTest() {
        val originalList = listOf(0,1,2,3,4,5)
        val shuffleList = getShuffledListOfInt(originalList.size)

        for (l in shuffleList) print("$l ")

        println("$tag , next ${getNextPositionInList(shuffleList, ArrayList(),2,true)}")
        println("$tag , next ${getNextPositionInList(shuffleList, ArrayList(),5,true)}")
        println("$tag , next ${getNextPositionInList(shuffleList, ArrayList(),0,true)}")
    }
    @Test
    fun getPrevPositionInListTest() {
        val originalList = listOf(0,1,2,3,4,5)
        val shuffleList = getShuffledListOfInt(originalList.size)

        for (l in shuffleList) print("$l ")

        println("$tag , next ${getPrevPositionInList(shuffleList,2,true)}")
        println("$tag , next ${getPrevPositionInList(shuffleList,5,true)}")
        println("$tag , next ${getPrevPositionInList(shuffleList,0,true)}")
    }

    @Test
    fun getListFromStringTest() {
        val result = getListFromString("rock; pop; techno; jazz; ")
        val expected = listOf("rock","pop","techno","jazz")
       // for (r in result) println ("$tag , $r")
        assertEquals(result, expected)
    }
}