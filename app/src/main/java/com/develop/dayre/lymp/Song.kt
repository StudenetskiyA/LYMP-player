package com.develop.dayre.lymp

import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

class FileSong(fileName: String, path: String)

@RealmClass
open class Song(
    @PrimaryKey
    var ID : Long = 0,
    var path : String = "",
    var name : String = "no name",
    var lenght : Int = 0,
    var rating : Int = 0,
    var tags : String = "",
    var added : String = "01.01.1000 00:00",
    var listenedTimes : Double = 0.0
)  : RealmModel {

    fun setTagsFromList(list : ArrayList<String>) {
        var result = "$SPACE_IN_LINK"
        for (i in list)
            result = result + i + SPACE_IN_LINK
        tags = result
    }

    fun copy() : Song {
        return Song(this.ID, this.path, this.name,this.lenght,this.rating,this.tags,this.added,this.listenedTimes)
    }

    override fun toString(): String {
        return "$ID / $path / $name / ${this.lenght} / $rating / $tags / $added / $listenedTimes"
    }
}