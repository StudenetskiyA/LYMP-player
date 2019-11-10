package com.develop.dayre.lymp

import android.Manifest
import android.app.AlertDialog
import com.develop.dayre.tagfield.TagView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.activity_main.*
import com.develop.dayre.lymp.databinding.ActivityMainBinding
import com.develop.dayre.tagfield.Tag
import androidx.lifecycle.Observer
import android.content.*
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.support.v4.media.session.MediaControllerCompat
import android.media.AudioManager
import android.support.v4.media.session.PlaybackStateCompat
import android.content.Intent
import android.os.Handler
import android.os.SystemClock
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private val TAG = "$APP_TAG/view"

    var serv: LYMPService? = null
    private lateinit var tagView: TagView
    private lateinit var searchTagView: TagView

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView
    private lateinit var seekBar: SeekBar


    private lateinit var settings: SharedPreferences
    private var mLastPlaybackStatePosition : Int = 0
    private var mLastPlaybackStatePositionTime : Int = 0
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

    lateinit var viewModel: LYMPViewModel

    private fun createControl() {
        Log.i(TAG, "createControl")
        addnewtagbutton.setOnClickListener {
            //Show enter field
            val input = EditText(this)
            val alert = AlertDialog.Builder(this)
            with(alert) {
                setTitle(resources.getText(R.string.enter_new_tag))
                setMessage(resources.getText(R.string.enter_new_tag_message))
                setView(input)
                setPositiveButton("ОК") { _, _ ->
                    val result = input.text.toString()
                    Log.i(TAG, "New tag name entered $result")
                    UIHelper.hideSoftKeyboard(input)
                    //TODO Check input
                    val newTags = "${viewModel.getAllTags()}; $result"
                    settings.edit()
                        .putString(APP_PREFERENCES_ALL_TAGS, newTags)
                        .apply()
                    viewModel.setAllTagsFromSettings(newTags)
                    buildLinkField()
                    buildSearchField()
                }
                setNegativeButton("Отмена") { _, _ ->
                    UIHelper.hideSoftKeyboard(input)
                }
            }
            val dialog = alert.create()
            dialog.setOnShowListener {
                UIHelper.showKeyboard(this, input)
            }

            dialog.show()
        }
        nextbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            viewModel.nextPress()
        }
        prevbutton.setOnClickListener {
            Log.i(TAG, "prev button pressed")
            viewModel.prevPress()
        }
        playbutton.setOnClickListener {
            Log.i(TAG, "play button pressed")
            viewModel.playPress()
        }
        stopbutton.setOnClickListener {
            Log.i(TAG, "stop button pressed")
            viewModel.stopPress()
        }

        shufflebutton.setOnClickListener {
            Log.i(TAG, "shuffle button pressed")
            viewModel.shufflePress()
        }
        sort_search_button.setOnClickListener {
            Log.i(TAG, "sort button pressed")
            viewModel.sortPress()
            viewModel.newSearch(viewModel.currentSearchTags.value!!)
            App.instance.context
                .toast("Sort status set ${viewModel.sort.get()}")

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
        tagView.setOnTagLongClickListener { position, tag ->
            //Show enter field
            val alert = AlertDialog.Builder(this)
            with(alert) {
                val tagClicked = getListFromString(viewModel.getAllTags())[position]
                setTitle(resources.getText(R.string.delete_tag_alert_title))
                setMessage("${resources.getText(R.string.delete_tag_alert)} $tagClicked ?")
                setPositiveButton("ОК") { _, _ ->
                    val list = ArrayList(getListFromString(viewModel.getAllTags()))
                    list.remove(tagClicked)
                    val newTags = getStringFromList(list)
                    settings.edit()
                        .putString(APP_PREFERENCES_ALL_TAGS, newTags)
                        .apply()
                    viewModel.setAllTagsFromSettings(newTags)
                    buildLinkField()
                    buildSearchField()
                }
                setNegativeButton("Отмена") { _, _ ->
                }
            }
            val dialog = alert.create()

            dialog.show()
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
        }
        seekBar.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.i(TAG,"seekBar onStartTrackingTouch")
            }

            override fun onProgressChanged(seekBar:SeekBar,progress:Int,fromUser: Boolean) {
                if (fromUser) {
                    Log.i(TAG,"seekBar onProgressChanged by user")
                    //seek here
                    mLastPlaybackStatePosition = progress
                    mLastPlaybackStatePositionTime = SystemClock.elapsedRealtime().toInt()
                    viewModel.jumpToPosition(progress)
                }
            }
        })

        ratingBar.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                val cs = viewModel.currentSong.value?.copy()
                if (cs != null) {
                    cs.rating = rating.toInt()
                    viewModel.currentSongEdit(cs)
                }
            }
        ratingBarInSearch.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                viewModel.setSearchRating(rating.toInt())
                viewModel.newSearch()
            }
    }

    private fun createView() {
        Log.i(TAG, "createView")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //Получаем инстанс, а не создаем новый - актуально при перезапуске приложения, повороте экрана и т.д.
        val application = application
        if (application !is App) {
            throw RuntimeException("Application in not implemented IModulePlayer.Application")
        }

        App.instance.setAppContext(this)
        App.instance.setViewModel(getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        viewModel = App.instance.getAppViewModel()
        viewModel.startModel()
        binding.viewmodel = viewModel
        binding.executePendingBindings()

        listView = findViewById(R.id.current_list)
        tagView = findViewById(R.id.current_track_tags)
        searchTagView = findViewById(R.id.search_track_tags)
        seekBar = findViewById(R.id.seek_bar)
    }

    private fun createObservers() {
        Log.i(TAG, "createObservers")
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
                track_info_added_at.text =
                    "${resources.getText(R.string.added_at)} ${viewModel.currentSong.value?.added}"
                track_info_listened_times.text =
                    "${resources.getText(R.string.listened_times)} ${viewModel.currentSong.value?.listenedTimes}"
                ratingBar.rating = if (viewModel.currentSong.value?.rating!=null) viewModel.currentSong.value?.rating!!.toFloat() else 0f
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

        viewModel.setMediaControllerCallback(object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                Log.i(TAG, "PlayState changed.")
                mLastPlaybackStatePosition = state.position.toInt()
                mLastPlaybackStatePositionTime =  SystemClock.elapsedRealtime().toInt()
                scheduleSeekbarUpdate()
                if (state.state == PlaybackStateCompat.STATE_PLAYING)
                    playbutton.setImageResource(R.drawable.pause_inbar)
                else
                    playbutton.setImageResource(R.drawable.playbutton)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        readSettings()
    }

    private fun grantPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

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
            Log.i(TAG, "Permission has already been granted")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
        Log.i(TAG,"onCreate")
        App.instance.setSettings(getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE))
        settings = App.instance.appSettings
        createView()
        createControl()
        createObservers()
        grantPermission()

        readSettings()

        val intent = Intent(this@MainActivity, LYMPService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
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
        //Я решил хранить все теги в настроках, не в БД. Пока так кажется проще, может потом передумаю.
        if (settings.contains(APP_PREFERENCES_ALL_TAGS)) {
            val t = settings.getString(APP_PREFERENCES_ALL_TAGS, "")
            viewModel.setAllTagsFromSettings(t)
        }

        if (settings.contains(APP_PREFERENCES_SELECT_SONG)) {
            val t = settings.getString(APP_PREFERENCES_SELECT_SONG, "0")
            viewModel.songSelectByID(t)
        }
        var search = ""
        if (settings.contains(APP_PREFERENCES_CURRENT_SEARCH)) {
            search = settings.getString(APP_PREFERENCES_CURRENT_SEARCH, "")!!
        }

        if (settings.contains(APP_PREFERENCES_SHUFFLE)) {
            val shuffle = settings.getBoolean(APP_PREFERENCES_SHUFFLE, false)
            if (shuffle) viewModel.setShuffle(true)
        }
        if (settings.contains(APP_PREFERENCES_REPEAT)) {
            when (settings.getInt(APP_PREFERENCES_REPEAT, 0)) {
                0 -> viewModel.setRepeat(RepeatState.All)
               1 -> viewModel.setRepeat(RepeatState.One)
               2 -> viewModel.setRepeat(RepeatState.Stop)
           }
        }
        if (settings.contains(APP_PREFERENCES_SORT)) {
            when (settings.getInt(APP_PREFERENCES_SORT, 0)) {
                0 -> viewModel.setSort(SortState.ByName)
                1 -> viewModel.setSort(SortState.ByListened)
                2 -> viewModel.setSort(SortState.ByAdded)
            }
        }

        viewModel.newSearch(search)
    }

    //For SeekBar
    private fun updateProgress() {
        var currentPosition:Int = mLastPlaybackStatePosition
        if (viewModel.getIsPlaying()==PlayState.Play) {
            val timeDelta = SystemClock.elapsedRealtime() -  mLastPlaybackStatePositionTime
            currentPosition +=  (timeDelta * 1).toInt()
        }
        seekBar.max = viewModel.getCurrentTrackDuration()
        seekBar.progress =  currentPosition
        Log.i(TAG, "Update seekbar $currentPosition / ${viewModel.getCurrentTrackDuration()}")
    }

    fun scheduleSeekbarUpdate() {
        stopSeekbarUpdate()
        if (!mExecutorService.isShutdown) {
            Log.i(TAG,"sheduleSeekbarUpdate")
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                {
                        mHandler.post {updateProgress()}
                }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS)
        }
    }

     private fun stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture!!.cancel(false)
        }
    }

    private val mHandler = Handler()

    private val mExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private var mScheduleFuture:ScheduledFuture<*>? = null

}
