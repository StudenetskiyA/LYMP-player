package com.develop.dayre.lymp

import android.Manifest
import com.develop.dayre.tagfield.TagView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import com.develop.dayre.lymp.databinding.ActivityMainBinding
import com.develop.dayre.tagfield.Tag
import androidx.lifecycle.Observer
import android.content.*
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.media.AudioManager
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View


class MainActivity : AppCompatActivity() {
    private val TAG = "$APP_TAG/view"

    private lateinit var tagView: TagView
    private lateinit var searchTagView: TagView

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView

    private lateinit var settings: SharedPreferences
    var serv: LYMPService? = null
    var isBound = false

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as LYMPService.MyLocalBinder
            serv = binder.getService()
            isBound = true
            Log.i(TAG, "Service binded.")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            Log.i(TAG, "Service unbinded.")
        }
    }

    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0

    init {
        instance = this
    }

    companion object {
        //Вызывает вопросы
        lateinit var viewModel: LYMPViewModel
        //Или передавать контекст при инициализации вьюмодели и модели.
        //По факту он нужен только для БД.
        var instance: MainActivity? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    private var receiversRegistered = false

  //  var mediaController: MediaControllerCompat? = null

    private fun registerReceivers(contextIn: Context) {
        if (receiversRegistered) return

        val context = contextIn.applicationContext
       // val receiver = NotificationReceiver()

//        val providerChanged = IntentFilter()
//        providerChanged.addAction("NEXT_ACTION")
//        context.registerReceiver(receiver, providerChanged)
//
//        val userPresent = IntentFilter()
//        userPresent.addAction("android.intent.action.USER_PRESENT")
//        context.registerReceiver(receiver, userPresent)

        receiversRegistered = true
    }

    private fun createControl() {
        nextbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            viewModel.nextPress()
//            val intent = Intent(this@MainActivity, LYMPService::class.java)
//            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Next)
//            startService(intent)
//            if (viewModel.isPlaying) {
//                mediaController?.transportControls?.stop()
//                mediaController?.transportControls?.play()
//            }
        }
        prevbutton.setOnClickListener {
            Log.i(TAG, "prev button pressed")
            val intent = Intent(this@MainActivity, LYMPService::class.java)
            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Prev)
            startService(intent)
//            if (viewModel.isPlaying) {
//                mediaController?.transportControls?.stop()
//                mediaController?.transportControls?.play()
//            }
        }
        playbutton.setOnClickListener {
            viewModel.playPress()
//            if (!viewModel.isPlaying) {
//            Log.i(TAG, "play button pressed")
//            mediaController?.transportControls?.play()}
//            else {
//                Log.i(TAG, "pause button pressed")
//                mediaController?.transportControls?.pause()
//            }
        }
        stopbutton.setOnClickListener {
            Log.i(TAG, "stop button pressed")
//            if (viewModel.isPlaying)
//                mediaController?.transportControls?.stop()
        }
        shufflebutton.setOnClickListener {
            Log.i(TAG, "shuffle button pressed")
            viewModel.shufflePress()
        }
        showmorebutton.setOnClickListener {
            Log.i(TAG, "showmore button pressed")
            viewModel.showMorePress()
        }
        repeatbutton.setOnClickListener {
            Log.i(TAG, "repeat button pressed")
            viewModel.repeatPress()
        }
        clear_tag.setOnClickListener {
            Log.i(TAG, "clear tag button pressed")
            viewModel.clearTagPress()
        }
        tagView.setOnTagClickListener { position, _ ->
            val cs = viewModel.currentSong.value?.copy()
            if (cs != null) {
                val tagClicked = getListFromString(viewModel.getAllTags())[position]
                val list = ArrayList(getListFromString(cs.tags))
                if (list.contains(tagClicked)) {
                    list.remove(tagClicked)
                } else {
                    list.add(tagClicked)
                }
                cs.setTagsFromList(list)
                Log.i(TAG, "Current song tags is ${cs.tags}")
                viewModel.currentSongEdit(cs)
            }
        }
        tagView.setOnTagDeleteListener { position, tag ->
            Toast.makeText(
                this@MainActivity,
                "delete tag id = " + tag.id + " position =" + position,
                Toast.LENGTH_SHORT
            ).show()
        }
        searchTagView.setOnTagClickListener { position, _ ->
            val cs = viewModel.currentSearchTags.value!!
            val tagClicked = getListFromString(viewModel.getAllTags())[position]
            val list = ArrayList(getListFromString(cs))
            if (list.contains(tagClicked)) {
                list.remove(tagClicked)
            } else {
                list.add(tagClicked)
            }
            Log.i(TAG, "Current search tags is ${getStringFromList(list)}")
            viewModel.newSearch(getStringFromList(list))
        }
        current_list.setOnItemClickListener { parent, view, position, id ->
            viewModel.songInListPress(position)
//            if (viewModel.isPlaying)
//            mediaController?.transportControls?.play()
        }
    }

    private fun createView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //Получаем инстанс, а не создаем новый - актуально при перезапуске приложения, повороте экрана и т.д.
        val viewModelFactory = MyViewModelFactory(getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LYMPViewModel::class.java)
        binding.viewmodel = viewModel
        binding.executePendingBindings()

        listView = findViewById(R.id.current_list)
        tagView = findViewById(R.id.current_track_tags)
        searchTagView = findViewById(R.id.search_track_tags)
    }

    private fun createObservers() {
        adapter = SongListAdapter(ArrayList(listOf(Song())), this)
        binding.currentList.adapter = adapter

        viewModel.currentSongsList.observe(this,
            Observer<ArrayList<Song>> {
                Log.i(TAG, "songs list updated")
                adapter = SongListAdapter(it, this)
                listView.adapter = adapter
                val s = "${resources.getText(R.string.tracks_found)}  ${it.size}"
                tracks_found.text = s
                if (viewModel.currentSongsList.value != null && viewModel.currentSongsList.value!!.contains(
                        viewModel.currentSong.value
                    )
                ) {
                    val n = viewModel.currentSongsList.value!!.indexOf(viewModel.currentSong.value)
                    listView.setItemChecked(n, true)
                    listView.smoothScrollToPosition(n)
                } else {
                    listView.setItemChecked(current_list.checkedItemPosition, false)
                    listView.smoothScrollToPosition(0)
                }
            })

        viewModel.currentSong.observe(this,
            Observer<Song> {
                Log.i(TAG, "current song updated")
                track_info_current_song.text = viewModel.currentSong.value?.name
                track_info_added_at.text = viewModel.currentSong.value?.added
                track_info_listened_times.text =
                    viewModel.currentSong.value?.listenedTimes.toString()

                if (viewModel.currentSongsList.value != null && viewModel.currentSongsList.value!!.contains(
                        viewModel.currentSong.value
                    )
                ) {
                    val n = viewModel.currentSongsList.value!!.indexOf(viewModel.currentSong.value)
                    Log.i(TAG, "current song index in list $n")
                    listView.setItemChecked(n, true)
                    listView.smoothScrollToPosition(n)
                }
                buildLinkField()
            })
        viewModel.currentSearchTags.observe(this,
            Observer<String> {
                Log.i(TAG, "current search tags updated, ${viewModel.currentSearchTags.value}")
                settings.edit()
                    .putString(APP_PREFERENCES_CURRENT_SEARCH, viewModel.currentSearchTags.value)
                    .apply()
                buildSearchField()
            })

        viewModel.setMediaControllerCallback( object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state : PlaybackStateCompat) {
                Log.i(TAG,"PlayState changed.")
                if (state.state==PlaybackStateCompat.STATE_PLAYING)
                    playbutton.setImageResource(R.drawable.pause_inbar)
                else
                    playbutton.setImageResource(R.drawable.playbutton)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        registerReceivers(this)
        readSettings()
    }

    private fun grantPermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Log.i(TAG,"Permission has already been granted")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        createView()
        viewModel.startModel()
        createControl()
        createObservers()
        grantPermission()

        //Надо подумать, хотим мы обновлять список файлов на диске при запуске, или при resume тоже.
       // viewModel.startModel()

        //Нужно в Андроид 7+, запись в манифесте больше не работает.
        registerReceivers(this)

        readSettings()

        val intent = Intent(this@MainActivity, LYMPService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        intent.putExtra(EXTRA_COMMAND, ServiceCommand.Init)
        startService(intent)
    }

    private fun buildSearchField() {
        searchTagView.removeAllTags()
        for (word in getListFromString(viewModel.getAllTags())) {
            val tag = Tag(word)
            tag.tagTextColor = Color.BLACK
            tag.layoutBorderSize = 2f
            tag.layoutBorderColor = Color.BLACK
            tag.radius = 20f
            if (viewModel.currentSearchTags.value != null && getListFromString(viewModel.currentSearchTags.value!!).contains(
                    word
                )
            )
                tag.layoutColor = Color.YELLOW
            else tag.layoutColor = Color.TRANSPARENT
            searchTagView.addTag(tag)
        }
    }

    private fun buildLinkField() {
        tagView.removeAllTags()
        if (viewModel.currentSong.value != null) {
            for (word in getListFromString(viewModel.getAllTags())) {
                val tag = Tag(word)
                tag.tagTextColor = Color.BLACK
                tag.layoutBorderSize = 2f
                tag.layoutBorderColor = Color.BLACK
                tag.radius = 20f
                if (viewModel.currentSong.value != null && getListFromString(viewModel.currentSong.value!!.tags).contains(
                        word
                    )
                )
                    tag.layoutColor = Color.YELLOW
                else tag.layoutColor = Color.TRANSPARENT
                tagView.addTag(tag)
            }
        }
    }

    private fun readSettings() {
        if (settings.contains(APP_PREFERENCES_SELECT_SONG)) {
            val t = settings.getString(APP_PREFERENCES_SELECT_SONG, "0")
            viewModel.songSelectByID(t.toLong())
        }
        var search = ""
        if (settings.contains(APP_PREFERENCES_CURRENT_SEARCH)) {
            search = settings.getString(APP_PREFERENCES_CURRENT_SEARCH, "")!!
        }
        viewModel.newSearch(search)
    }
}
