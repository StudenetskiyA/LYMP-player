package com.develop.dayre.lymp

import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.lang.Integer.min

class SongOrNull(var song:Song, var isNull:Boolean=false)

@RealmClass
open class Song(
    @PrimaryKey
    var ID : String = "0", //md5-hash from name without extension and size
    var path : String = "", //Полный путь, включая имя и расширение
    var name : String = "no name",
    var lenght : Int = 0,
    var rating : Int = 0,
    var tags : String = "",
    var added : String = "01.01.1000 00:00",
    var listenedTimes : Double = 0.0,
    var isFileExist: Boolean = true,
    var order: Int = 0
)  : RealmModel {


    companion object {
        fun getSongFromString(songString: String): Song {
            val splited = songString.split("/")
            return Song(splited[0],"",splited[1],splited[2].toInt(),splited[3].toInt(),splited[4],"",
                0.0, false, 0
            )
        }
    }

    fun setTagsFromList(list : ArrayList<String>) {
        var result = "$SPACE_IN_LINK"
        for (i in list)
            result = result + i + SPACE_IN_LINK
        tags = result
    }

    fun copy() : Song {
        return Song(this.ID, this.path, this.name,this.lenght,this.rating,this.tags,this.added,this.listenedTimes,this.isFileExist,this.order)
    }

    override fun toString(): String {
        return "$ID/$name/${this.lenght}/$rating/$tags/$added/$listenedTimes/$isFileExist"
    }

}