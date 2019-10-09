package com.develop.dayre.lymp

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmHelper(context : Context) {
    var realm : Realm = initRealm(context)

    fun clearDataBase() {
        realm.executeTransaction { realm.deleteAll() }
    }

    fun writeSong(song : Song) {
        realm.executeTransaction { realm.insertOrUpdate(song)
        }
    }

    fun getSongByName (songName : String) : Song? {
        return if (realm.where(Song::class.java).equalTo("name", songName).count()>0)
            realm.where(Song::class.java).equalTo("name", songName).findFirst()
        else null
    }

    fun initRealm(context: Context): Realm {
        //Инициализируем движок Realm
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("lymp.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded() // todo remove for production
            .build()
        Realm.setDefaultConfiguration(config)
        return Realm.getDefaultInstance()
    }
}

