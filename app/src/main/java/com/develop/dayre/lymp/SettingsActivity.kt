package com.develop.dayre.lymp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.settings.*
import java.io.File
import java.io.PrintWriter
import android.widget.Toast
import java.nio.file.Files
import java.nio.file.Paths


class SettingsActivity : AppCompatActivity() {
    val TAG = "lymp/settings"
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

    private fun initControlls() {
        import_bn.setOnClickListener {
            Log.d(TAG, "import click")
            importFromFile()
        }
        export_bn.setOnClickListener {
            Log.d(TAG, "export click")
            exportToFile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        initControlls()
    }

    private fun importFromFile() {
        grantPermission()
        val fileDialog = OpenFileDialog(this)
            .setFilter(".*\\.lma")
            .setOpenDialogListener { fileName ->
                val allSongs = ArrayList<Song>()
                var songsImport = 0
                Files.lines(Paths.get(fileName))
                    .use { stream -> stream.forEach(({
                        songsImport++
                        allSongs.add(Song.getSongFromString(it))
                        Log.i(TAG,it)
                    })) }
                Toast.makeText(
                    applicationContext,
                    "Songs found $songsImport",
                    Toast.LENGTH_LONG
                ).show()
            }
        fileDialog.show()
    }

    private fun exportToFile() {
        grantPermission()
        val sdMain = File(getExternalStorageDirectory().toString() + "/Lymp")
        var success = true
        if (!sdMain.exists()) {
            success = sdMain.mkdir()
        }
        if (success) {
            // directory exists or already created
            val file = File(sdMain, "export.lma")
            try {
                val allSongs = App.realmHelper.getAllSongs()
                Log.d(TAG, "size = ${allSongs.size}")
                PrintWriter(file).use { out ->
                    for (song in allSongs) out.println(song.toString())
                }
            } catch (e: Exception) {
            }
        } else {
            Log.d(TAG, "directory not created")
        }
    }

    private fun grantPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission not granted")
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                Log.i(TAG, "Permission request")
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        } else {
            Log.i(TAG, "Permission has already been granted")
        }
    }

    fun importSongs (songs: ArrayList<Song>) {

    }
}