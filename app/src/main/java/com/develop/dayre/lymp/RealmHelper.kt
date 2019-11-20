package com.develop.dayre.lymp

import android.content.Context
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration

import com.develop.dayre.lymp.SortState.*


class RealmHelper(context: Context) {
    private val tag = "$APP_TAG/realm"
    var realm: Realm = initRealm(context)

    fun close() {
        realm.close()
    }

    fun clearDataBase() {
        realm.executeTransaction { realm.deleteAll() }
    }

    fun writeSong(song: Song) {
        realm.executeTransaction {
            realm.insertOrUpdate(song)
        }
    }

    fun getSongsFromDBToCurrentSongsList(
        tags: List<String>, antiTags: List<String>,
        andOrFlag: AndOrState = AndOrState.Or,
        sort: SortState = ByName,
        searchName: String = "",
        minRating: Int = 0
    ): ArrayList<Song> {
        var result: List<Song>? = if (tags.isNotEmpty() && !tags[0].startsWith("#")) {
                if (andOrFlag == AndOrState.Or)
                    getAllSongsFileExist().filter {
                        it.tags.split(SPACE_IN_LINK).intersect(tags.asIterable()).isNotEmpty()
                    }
                else
                    getAllSongsFileExist().filter {
                        it.tags.split(SPACE_IN_LINK).toMutableList().containsAll(tags)
                    }
        } else if (tags.isEmpty()) getAllSongsFileExist()
        else { //supertags
            var result:List<Song> = getAllSongsFileExist()
            if (tags.contains("#без_тегов")) {
                    result = result.filter {
                        it.tags.length < 2
                    }
                }
            if (tags.contains("#недавние")) {
                val lastDate = getLastDate()
                result = result.filter {
                    it.added == lastDate
                }
            }
            result
        }

        if (antiTags.isNotEmpty()) result = result?.filter{  it.tags.split(SPACE_IN_LINK).intersect(antiTags.asIterable()).isEmpty() }

        result = result?.filter { it.name.toLowerCase().contains(searchName.toLowerCase()) }
        result = result?.filter { it.rating >= minRating }


        result = when (sort) {
            ByName -> result?.sortedBy { it.name }
            ByListened -> result?.sortedByDescending { it.listenedTimes }
            else -> {
                result?.sortedBy { it.added }
            }
        }

        if (result != null) {
            Log.i(tag, "Songs found ${result.size}")
            return ArrayList(result)
        }
        //Возвращаем пустой лист
        return ArrayList()
    }

    fun getAllSongs() : ArrayList<Song> {
        val result = ArrayList<Song>()
        for (r in realm.where(Song::class.java).findAll()) {
            result.add(r)
        }
        return result
    }

    fun getLastDate(): String {
        val list = getAllSongsFileExist().sortedByDescending { it.added }
        return if (list.isEmpty()) "" else list[0].added
    }

    fun getAllSongsFileExist(): ArrayList<Song> {
        val result = ArrayList<Song>()

        val resultRealm = realm.where(Song::class.java).equalTo("isFileExist", true).findAll()

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

    fun getSongByID(songID: String): Song? {
        return if (realm.where(Song::class.java).equalTo("ID", songID).count() > 0)
            realm.where(Song::class.java).equalTo("ID", songID).findFirst()
        else null
    }


    private fun initRealm(context: Context): Realm {
        //Инициализируем движок Realm
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("lymp.realm")
            .schemaVersion(5)
            .deleteRealmIfMigrationNeeded() // todo remove for production
            .build()
        Realm.setDefaultConfiguration(config)
        return Realm.getDefaultInstance()
    }
}

