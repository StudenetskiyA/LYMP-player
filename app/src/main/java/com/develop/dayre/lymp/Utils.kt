package com.develop.dayre.lymp

//Необходима для текста-ссылок
fun getSpaceIndices(s: String, c: Char): Array<Int> {
    var pos = s.indexOf(c, 0)
    val indices = ArrayList<Int>()
    while (pos != -1) {
        indices.add(pos)
        pos = s.indexOf(c, pos + 1)
    }
    return indices.toTypedArray()
}

fun getShuffledListOfInt (size: Int) : ArrayList<Int> {
    val result = ArrayList<Int>()
    for (i in 0 until size) {
        result.add(i)
    }
    result.shuffle()
    return result
}

fun getNextPositionInList(shuffledList: ArrayList<Int>, currentPosition: Int, shuffleStatus: Boolean):Int {
    return if (!shuffleStatus) {
        if (currentPosition+1<shuffledList.size){
            currentPosition+1
        } else {
            0
        }
    } else {
        val cp = shuffledList.indexOfFirst {it == currentPosition}
        if (cp+1<shuffledList.size){
            shuffledList[cp+1]
        } else {
            shuffledList[0]
        }
    }
}

fun getPrevPositionInList(shuffledList: ArrayList<Int>, currentPosition: Int, shuffleStatus: Boolean):Int {
    return if (!shuffleStatus) {
        if (currentPosition==0){
            shuffledList.size-1
        } else {
            currentPosition-1
        }
    } else {
        val cp = shuffledList.indexOfFirst {it == currentPosition}
        if (cp==0){
            shuffledList[ shuffledList.size-1]
        } else {
            shuffledList[cp-1]
        }
    }
}