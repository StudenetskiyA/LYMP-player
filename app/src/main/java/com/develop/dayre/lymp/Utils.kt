package com.develop.dayre.lymp

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import java.io.File
import org.apache.commons.codec.digest.DigestUtils

fun getHashFromNameAndSize(name: String, size : Long) : String{
    return DigestUtils.md5Hex("$name , $size")
}

fun getFileSize(path : String) : Long {
    val file = File(path)
    return file.length()
}

// This is an extension method for easy Toast call
fun Context.toast(message: CharSequence) {
    val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.BOTTOM, 0, 325)
    toast.show()
}

fun String.getNameFromPath() : String  {
    val nd = this.substring(this.lastIndexOf("/"),this.length)
    return nd.substring(1,nd.lastIndexOf("."))
}

//Возвращает лист из всех файлов в папке и подпапках в парах имя/путь(включая имя)
fun getFilesListInFolderAndSubFolder(path : File,endWith : String) : ArrayList<String>{
    val find = ArrayList<String>()
    File(path.canonicalPath).walk().forEach {
        if ( it.name.endsWith(".$endWith")) //TODO Change to supported music
            find.add(it.canonicalPath)
    }
    return find
}

////Возвращает лист из всех файлов в папке и подпапках в парах имя/путь(включая имя)
//fun getFilesListInFolderAndSubFolder(path : File,endWith : String) : ArrayList<Pair<String,String>>{
//    val find = ArrayList<Pair<String,String>>()
//    File(path.canonicalPath).walk().forEach {
//        if ( it.name.endsWith(".$endWith")) //TODO Change to supported music
//            find.add(Pair(it.name,it.canonicalPath))
//    }
//    return find
//}

fun getStringFromList(list : ArrayList<String>) : String{
    var result = "$SPACE_IN_LINK"
    for (i in list)
        result = result + i + SPACE_IN_LINK
    return result
}

fun getListFromString(text:String) : List<String> {
    return text.split(SPACE_IN_LINK).map { it.trim() }.filter { it != "" }
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