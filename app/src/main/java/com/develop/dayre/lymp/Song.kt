package com.develop.dayre.lymp

import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class Song(
    @PrimaryKey
    var name : String = "no name",
    var lenght : Int = 0,
    var rating : Int = 0,
    var tags : String = "",
    var added : String = "01.01.1000 00:00",
    var listenedTimes : Double = 0.0
)  : RealmModel {

    fun copy() : Song {
        return Song(this.name,this.lenght,this.rating,this.tags,this.added,this.listenedTimes)
    }

    override fun toString(): String {
        return "$name / ${this.lenght} / $rating / $tags / $added / $listenedTimes"
    }
}