package com.develop.dayre.ormtests

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.develop.dayre.lymp.*
import io.realm.Realm

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class RealmTests {
    private val tag = "$APP_TAG/tests"
    private var realm : Realm
    private var helper : RealmHelper
    private val data = Song("1","", "TestName", 100, 2, "rock" )

    init {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        helper = RealmHelper(appContext)
        realm =  helper.realm
    }

    @Test
    fun writeAndRead() {
        realm.executeTransaction { realm.insertOrUpdate(data) }

        val dataRead = realm.where(Song::class.java).equalTo("name","TestName").findFirst()

        assertEquals(dataRead.name,"TestName")
        assertEquals(dataRead.lenght,100)
        assertEquals(dataRead.rating,2)
        assertEquals(dataRead.tags,"rock")
    }

    @Test
    fun writeSomeName() {
        realm.executeTransaction { realm.insertOrUpdate(data) }
        data.tags = "pop"
        data.rating = 5
        realm.executeTransaction { realm.insertOrUpdate(data) }
        val dataRead = realm.where(Song::class.java).equalTo("name","TestName").findFirst()

        assertEquals(dataRead.name,"TestName")
        assertEquals(dataRead.tags,"pop")
        assertEquals(dataRead.rating,5)
    }

    @Test
    fun getSongByNameTest() {
        //Сотрет базу, закомментируй, когда в ней что-то появится, требующее хранения.
        //Или используй другую базу.
        //helper.clearDataBase()
        helper.writeSong(data)
        val result = helper.getSongByName(data.name)

        assertEquals(result?.name,data.name)
        assertEquals(result?.tags,data.tags)
        assertEquals(result?.rating,data.rating)
        assertEquals(result?.lenght,data.lenght)
    }

    @Test
    fun antiTagsTest() {
        val tagsString = ";"
        val antiTagsString = ";блюз;"
        val tags = getListFromString(tagsString)
        val antiTags = getListFromString(antiTagsString)
        val resultAll: List<Song> = helper.getAllSongsFileExist()
//            .filter { it.tags.split(SPACE_IN_LINK).intersect(tags.asIterable()).isNotEmpty() }
        val result = resultAll
            .filter { it.tags.split(SPACE_IN_LINK).intersect(tags.asIterable()).isNotEmpty() }
           // .filter { it.tags.split(SPACE_IN_LINK).intersect(antiTags.asIterable()).isEmpty() }

//        var item = resultAll[0]
//        val inter = item.tags.split(SPACE_IN_LINK).intersect(antiTags.asIterable())
//
//        Log.d("lymp/tests" , item.toString())
//        Log.d("lymp/tests" , inter.toString())
        for (item in result) {
            Log.d("lymp/tests", item.toString())
        }
    }

    @Test
    fun getLastDateTest() {
       // Log.d("lymp/tests",helper.getLastDate())
    }

    @Test
    fun getSongByNameNoResultTest() {
        val result = helper.getSongByName("jsgdfhsgfhgrhgrgrhtb")

        assertNull(result)
    }

    @Test
    fun printAllBase() {
        val dataRead = realm.where(Song::class.java).findAll()
        for (data in  dataRead) {
         //   Log.i(tag, data.toString())
        }
    }
}
