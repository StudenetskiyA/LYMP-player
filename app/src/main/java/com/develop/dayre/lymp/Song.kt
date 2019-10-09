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
    var tags : String = ""
)  : RealmModel {

    override fun toString(): String {
        return "$name / ${this.lenght} / $rating / $tags"
    }
}