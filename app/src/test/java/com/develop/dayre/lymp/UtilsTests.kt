package com.develop.dayre.lymp

import org.junit.Test

class UtilsTests {
    private val tag = "$APP_TAG/tests"

    @Test
    fun getShuffledListOfIntTest() {
        val list = getShuffledListOfInt(10)
        for (l in list)
            println("$tag , $l")
    }

    @Test
    fun getNextPositionInListTest() {
        val originalList = listOf(0,1,2,3,4,5)
        val shuffleList = getShuffledListOfInt(originalList.size)

        for (l in shuffleList) print("$l ")

        println("$tag , next ${getNextPositionInList(shuffleList,2,true)}")
        println("$tag , next ${getNextPositionInList(shuffleList,5,true)}")
        println("$tag , next ${getNextPositionInList(shuffleList,0,true)}")
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
}