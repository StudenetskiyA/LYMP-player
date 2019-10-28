package com.develop.dayre.lymp

import android.os.Environment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileWorkTests {

    private val TAG = "$APP_TAG/filetests"

    @Test
    fun getFilesListInFolderAndSubfolderTest() {
        val f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        Log.i(TAG, "browse folder ${f.canonicalPath}, exist = ${f.exists()}, canRead = ${f.canRead()}")

        if (f.exists() && f.isDirectory && f.canRead()) {
            val find = getFilesListInFolderAndSubFolder(f,"mp3")
            for (i in find)
                Log.i(TAG, "$i")
        } else {
            Log.i(TAG, "Нет доступа к директории.")
        }
    }

    @Test
    fun getFileSizeTest() {
        val f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        Log.i(TAG, "browse folder ${f.canonicalPath}, exist = ${f.exists()}, canRead = ${f.canRead()}")

        if (f.exists() && f.isDirectory && f.canRead()) {
            val find = getFilesListInFolderAndSubFolder(f,"mp3")
            for (i in find)
                Log.i(TAG, "$i , size = ${getFileSize(i)}")
        } else {
            Log.i(TAG, "Нет доступа к директории.")
        }
    }

    @Test
    fun getHashFromNameAndSizeTest() {
        val f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        Log.i(TAG, "browse folder ${f.canonicalPath}, exist = ${f.exists()}, canRead = ${f.canRead()}")

        if (f.exists() && f.isDirectory && f.canRead()) {
            val find = getFilesListInFolderAndSubFolder(f,"mp3")
            for (i in find)
                Log.i(TAG, "$i , hash = ${getHashFromNameAndSize(i,getFileSize(i))}")
        } else {
            Log.i(TAG, "Нет доступа к директории.")
        }
    }
}