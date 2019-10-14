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
        if (getSongByNameAndPath(song.name,song.path)!=null)
        realm.executeTransaction {
            realm.insertOrUpdate(song)
        }
        else {
            val lastID = getLastID()
            song.ID=lastID+1
            realm.executeTransaction {
                realm.insertOrUpdate(song)
            }
        }
    }

    private fun getLastID():Long {
        return if ( realm.where(Song::class.java).count()!=0.toLong()) {
            realm.where(Song::class.java).max("ID").toLong()
        } else 0
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
                    it.tags.split(SPACE_IN_LINK).intersect(tags.asIterable()).isNotEmpty()
                }
            else
                getAllSongs().filter { it.tags.split(SPACE_IN_LINK).toMutableList().containsAll(tags) }
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

     private fun getAllSongs(): ArrayList<Song> {
        val result = ArrayList<Song>()

        val resultRealm = realm.where(Song::class.java).findAll()

        for (r in resultRealm) {
            result.add(r)
        }
        return result
    }

    fun getSongByNameAndPath(songName: String, path: String): Song? {
        return if (realm.where(Song::class.java).equalTo("name", songName).equalTo("path", path).count() > 0)
            realm.where(Song::class.java).equalTo("name", songName).equalTo("path", path).findFirst()
        else null
    }

    fun getSongByName(songName: String): Song? {
        return if (realm.where(Song::class.java).equalTo("name", songName).count() > 0)
            realm.where(Song::class.java).equalTo("name", songName).findFirst()
        else null
    }

    fun getSongByID(songID: Long): Song? {
        return if (realm.where(Song::class.java).equalTo("ID", songID).count() > 0)
            realm.where(Song::class.java).equalTo("ID", songID).findFirst()
        else null
    }


    fun initRealm(context: Context): Realm {
        //Инициализируем движок Realm
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("lymp.realm")
            .schemaVersion(3)
            .deleteRealmIfMigrationNeeded() // todo remove for production
            .build()
        Realm.setDefaultConfiguration(config)
        return Realm.getDefaultInstance()
    }
}

