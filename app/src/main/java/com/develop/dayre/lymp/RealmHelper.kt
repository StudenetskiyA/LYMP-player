package com.develop.dayre.lymp

import android.content.Context
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration

import com.develop.dayre.lymp.TagsFlag.*
import com.develop.dayre.lymp.SortState.*

class RealmHelper(context: Context) {
    private val tag = "$APP_TAG/realm"
    var realm: Realm = initRealm(context)

    fun clearDataBase() {
        realm.executeTransaction { realm.deleteAll() }
    }

    fun writeSong(song: Song) {
        realm.executeTransaction {
            realm.insertOrUpdate(song)
        }
    }

    fun getSongsFromDBToCurrentSongsList(
        tags: List<String>,
        tagsFlag: TagsFlag = Or,
        sort: SortState = ByName,
        searchName: String = ""
    ): ArrayList<Song> {
        var result: List<Song>? = if (tags.isNotEmpty()) {
            if (tagsFlag == Or)
                getAllSongs().filter {
                    it.tags.split(",").intersect(tags.asIterable()).isNotEmpty()
                }
            else
                getAllSongs().filter { it.tags.split(",").toMutableList().containsAll(tags) }
        } else getAllSongs()

        result = result?.filter { it.name.contains(searchName) }

        result = when (sort) {
            ByName -> result?.sortedBy { it.name }
            ByListened -> result?.sortedBy { it.listenedTimes }
            else -> {
                result?.sortedBy { it.added }
            }
        }

        if (result != null) {
            Log.i(tag, "Songs found ${result.size}")
            return ArrayList(result)
        }
        //Возвращаем пустой лист
        return ArrayList<Song>()
    }

     fun getAllSongs(): ArrayList<Song> {
        val result = ArrayList<Song>()

        val resultRealm = realm.where(Song::class.java).findAll()

        for (r in resultRealm) {
            result.add(r)
        }
        return result
    }

    fun getSongByName(songName: String): Song? {
        return if (realm.where(Song::class.java).equalTo("name", songName).count() > 0)
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

