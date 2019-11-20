package com.develop.dayre.lymp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import com.develop.dayre.tagfield.TagView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
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
import android.os.*
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.develop.dayre.lymp.App.Companion.exoPlayer
import com.develop.dayre.lymp.service.PlayerService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val TAG = "$APP_TAG/view"

    private lateinit var tagView: TagView
    private lateinit var searchTagView: TagView

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView
    private lateinit var seekBar: SeekBar
    private lateinit var searchByNameEnterField: EditText
    private lateinit var footerBar: RelativeLayout


    private lateinit var settings: SharedPreferences
    private var mLastPlaybackStatePosition: Int = 0
    private var mLastPlaybackStatePositionTime: Int = 0

    //Service
    private var statePlaying = PlaybackStateCompat.STATE_STOPPED
    private var playerServiceBinder: PlayerService.PlayerServiceBinder? = null
    private var mediaController: MediaControllerCompat? = null
    val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) return
            statePlaying = state.state
            if (statePlaying == PlaybackStateCompat.STATE_PLAYING) {
                playbutton.setImageResource(R.drawable.pause_bn)
                mLastPlaybackStatePosition = exoPlayer.currentPosition.toInt()
                mLastPlaybackStatePositionTime = SystemClock.elapsedRealtime().toInt()
                scheduleSeekbarUpdate()
            } else {
                playbutton.setImageResource(R.drawable.play_bn)
                stopSeekbarUpdate()
                if (statePlaying == PlaybackStateCompat.STATE_STOPPED) {
                    seekBar.progress = 0
                }
            }
        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG + "/service", "service connected")
            playerServiceBinder = service as PlayerService.PlayerServiceBinder
            try {
                mediaController = MediaControllerCompat(
                    this@MainActivity,
                    playerServiceBinder!!.mediaSessionToken
                )
                mediaController?.registerCallback(callback)
                callback.onPlaybackStateChanged(mediaController?.playbackState)
            } catch (e: RemoteException) {
                mediaController = null
            }

        }

        override fun onServiceDisconnected(name: ComponentName) {
            playerServiceBinder = null
            if (mediaController != null) {
                mediaController!!.unregisterCallback(callback)
                mediaController = null
            }
        }
    }

    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0

    lateinit var viewModel: LYMPViewModel

    private fun createControl(context: Context) {
        Log.i(TAG, "createControl")

        settings_bn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
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
            if (mediaController != null) {
                mediaController!!.transportControls.skipToNext()
                if (statePlaying != PlaybackStateCompat.STATE_PLAYING)
                    mediaController!!.transportControls.pause()
            }
        }
        prevbutton.setOnClickListener {
            Log.i(TAG, "prev button pressed")
            if (mediaController != null) {
                mediaController!!.transportControls.skipToPrevious()
                if (statePlaying != PlaybackStateCompat.STATE_PLAYING)
                    mediaController!!.transportControls.pause()
            }
        }
        playbutton.setOnClickListener {
            Log.i(TAG, "play button pressed")
            if (mediaController != null) {
                if (statePlaying == PlaybackStateCompat.STATE_PLAYING)
                    mediaController!!.transportControls.pause()
                else mediaController!!.transportControls.play()
            }
        }
        stopbutton.setOnClickListener {
            mediaController!!.transportControls.stop()
        }
        clear_search_button.setOnClickListener {
            Log.i(TAG, "clear search button pressed")
            ratingBarInSearch.rating = 0f
            search_by_name_enter_field.setText("")
            settings.edit()
                .putString(APP_PREFERENCES_CURRENT_SEARCH_NAME, "")
                .apply()
            viewModel.setSearchRating(0, true)
            viewModel.newSearch(clearSearch = true, searchName = "")

            context.toast("Clear search")
        }
        shufflebutton.setOnClickListener {
            Log.i(TAG, "shuffle button pressed")
            viewModel.shufflePress()
        }
        sort_search_button.setOnClickListener {
            Log.i(TAG, "sort button pressed")
            viewModel.sortPress()
            context.toast("Sort status set ${viewModel.sort.get()}")
        }
        andor_search_button.setOnClickListener {
            Log.i(TAG, "andOr button pressed")
            viewModel.andOrPress()
            context.toast("AndOr status set ${viewModel.andOr.get()}")
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
                val tagClicked =
                    getListFromString(viewModel.getAllTags(), ignoreSuperTags = true)[position]
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
            var list = ArrayList(getListFromString(cs))
            val csAnti = viewModel.currentAntiSearchTags.value!!
            var listAnti = ArrayList(getListFromString(csAnti))

            //Supertags
            val copyList = ArrayList(list)
            if (tagClicked.startsWith("#")) {
                for (item in list) {
                    if (!item.startsWith("#")) copyList.remove(item)
                }
            } else {
                //Remove all supertags
                for (item in list) {
                    if (item.startsWith("#")) copyList.remove(item)
                }
            }
            list = copyList
            val copyAntiList = ArrayList(listAnti)
            if (tagClicked.startsWith("#")) {
                for (item in listAnti) {
                    if (!item.startsWith("#")) copyAntiList.remove(item)
                }
            } else {
                //Remove all supertags
                for (item in listAnti) {
                    if (item.startsWith("#")) copyAntiList.remove(item)
                }
            }
            listAnti = copyAntiList


            if (listAnti.contains(tagClicked)) listAnti.remove(tagClicked)

            if (list.contains(tagClicked)) {
                list.remove(tagClicked)
            } else {
                list.add(tagClicked)
            }
            Log.i(
                TAG,
                "Current search tags is ${getStringFromList(list)}, antitags is ${getStringFromList(
                    listAnti
                )} "
            )
            viewModel.newSearch(getStringFromList(list), getStringFromList(listAnti))
        }
        searchTagView.setOnTagLongClickListener { position, _ ->
            Log.d(TAG, "long click on search tag")
            val cs = viewModel.currentSearchTags.value!!
            var list = ArrayList(getListFromString(cs))
            val csAnti = viewModel.currentAntiSearchTags.value!!
            val tagClicked = getListFromString(viewModel.getAllTags())[position]
            var listAnti = ArrayList(getListFromString(csAnti))

            //Supertags
            val copyList = ArrayList(list)
            if (tagClicked.startsWith("#")) {
                for (item in list) {
                    if (!item.startsWith("#")) copyList.remove(item)
                }
            } else {
                //Remove all supertags
                for (item in list) {
                    if (item.startsWith("#")) copyList.remove(item)
                }
            }
            list = copyList
            val copyAntiList = ArrayList(listAnti)
            if (tagClicked.startsWith("#")) {
                for (item in listAnti) {
                    if (!item.startsWith("#")) copyAntiList.remove(item)
                }
            } else {
                //Remove all supertags
                for (item in listAnti) {
                    if (item.startsWith("#")) copyAntiList.remove(item)
                }
            }
            listAnti = copyAntiList

            if (list.contains(tagClicked)) list.remove(tagClicked)
            if (listAnti.contains(tagClicked)) {
                listAnti.remove(tagClicked)
            } else {
                listAnti.add(tagClicked)
            }
            Log.i(
                TAG,
                "Current search tags is ${getStringFromList(list)}, antitags is ${getStringFromList(
                    listAnti
                )} "
            )
            viewModel.newSearch(getStringFromList(list), getStringFromList(listAnti))
        }
        current_list.setOnItemClickListener { _, _, position, _ ->
            if (mediaController != null) {
                mediaController!!.transportControls.stop()
                viewModel.songInListPress(position)
                if (statePlaying == PlaybackStateCompat.STATE_PLAYING)
                    mediaController!!.transportControls.play()
            }
        }
        current_list.setOnItemLongClickListener { parent, view, position, id ->
            Log.i(TAG, "Long click on list element")
            viewModel.songInListLongPress(position)
            true
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.i(TAG, "seekBar onProgressChanged by user, progress = $progress")
                    seekBar.max = (App.exoPlayer.duration / 1000).toInt()
                    exoPlayer.seekTo((progress * 1000).toLong())
                    //Вот так не работает, приходится держать плеер глобальной переменной
                    //mediaController!!.transportControls.seekTo((progress*1000).toLong())

                    mLastPlaybackStatePosition = progress*1000
                    mLastPlaybackStatePositionTime = SystemClock.elapsedRealtime().toInt()
                }
            }
        })
        ratingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, _ ->
            val cs = viewModel.currentSong.value?.copy()
            if (cs != null) {
                cs.rating = rating.toInt()
                viewModel.currentSongEdit(cs)
            }
        }
        ratingBarInSearch.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                viewModel.setSearchRating(rating.toInt())
            }
        searchByNameEnterField.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                viewModel.newSearch(searchName = search_by_name_enter_field.text.toString())
                hideKeyboard(v)
                settings.edit()
                    .putString(
                        APP_PREFERENCES_CURRENT_SEARCH_NAME,
                        search_by_name_enter_field.text.toString()
                    )
                    .apply()
                return@OnKeyListener true
            }
            false
        })
    }

    private fun createView(context: Context) {
        Log.i(TAG, "createView")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //Получаем инстанс, а не создаем новый - актуально при перезапуске приложения, повороте экрана и т.д.

        // App.instance.setViewModel(getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        viewModel = App.viewModel
        viewModel.startModel(context)
        binding.viewmodel = viewModel
        binding.executePendingBindings()

        listView = findViewById(R.id.current_list)
        footerBar = findViewById(R.id.footer)
        searchByNameEnterField = findViewById(R.id.search_by_name_enter_field)
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
                ratingBar.rating =
                    if (viewModel.currentSong.value?.rating != null) viewModel.currentSong.value?.rating!!.toFloat() else 0f
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
        viewModel.currentAntiSearchTags.observe(this,
            Observer<String> {
                Log.i(
                    TAG,
                    "current search antitags updated, ${viewModel.currentAntiSearchTags.value}"
                )
                settings.edit()
                    .putString(
                        APP_PREFERENCES_CURRENT_SEARCH_ANTITAGS,
                        viewModel.currentAntiSearchTags.value
                    )
                    .apply()
                buildSearchField()
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
            }
        } else {
            Log.i(TAG, "Permission has already been granted")
            viewModel.startBrowseFolderForFiles()
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
                    viewModel.startBrowseFolderForFiles()
                } else {
                    //TODO
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
        Log.i(TAG, "onCreate")
        App.instance.setApp(
            applicationContext,
            this,
            getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        )

        settings = App.appSettings
        createView(applicationContext)
        createControl(applicationContext)
        createObservers()
        grantPermission()
        //continue after grant/not grant permission
        readSettings()

        val intent = Intent(this@MainActivity, PlayerService::class.java)
        bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
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
            else if (viewModel.currentAntiSearchTags.value != null && getListFromString(viewModel.currentAntiSearchTags.value!!).contains(
                    word
                )
            ) tag.layoutColor = Color.RED
            else tag.layoutColor = Color.TRANSPARENT
            searchTagView.addTag(tag)
        }
    }

    private fun buildLinkField() {
        tagView.removeAllTags()
        if (viewModel.currentSong.value != null) {
            for (word in getListFromString(viewModel.getAllTags(), ignoreSuperTags = true)) {
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
            var list = getListFromString(t)
            list = list.sorted()
            viewModel.setAllTagsFromSettings(getStringFromList(ArrayList(list)))
        } else {
            var list = listOf("#недавние", "#без_тегов", "блюз")
            list = list.sorted()
            viewModel.setAllTagsFromSettings(getStringFromList(ArrayList(list)))
        }

        if (settings.contains(APP_PREFERENCES_SELECT_SONG)) {
            val t = settings.getString(APP_PREFERENCES_SELECT_SONG, "0")
            viewModel.songSelectByID(t)
        }
        var search = ""
        if (settings.contains(APP_PREFERENCES_CURRENT_SEARCH)) {
            search = settings.getString(APP_PREFERENCES_CURRENT_SEARCH, "")!!
        }
        var antiSearch = ""
        if (settings.contains(APP_PREFERENCES_CURRENT_SEARCH_ANTITAGS)) {
            antiSearch = settings.getString(APP_PREFERENCES_CURRENT_SEARCH_ANTITAGS, "")!!
        }
        var searchName = ""
        if (settings.contains(APP_PREFERENCES_CURRENT_SEARCH_NAME)) {
            searchName = settings.getString(APP_PREFERENCES_CURRENT_SEARCH_NAME, "")!!
            search_by_name_enter_field.setText(searchName)
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
        if (settings.contains(APP_PREFERENCES_ANDOR)) {
            when (settings.getInt(APP_PREFERENCES_ANDOR, 0)) {
                0 -> viewModel.setAndOr(AndOrState.Or)
                1 -> viewModel.setAndOr(AndOrState.And)
            }
        }
        if (settings.contains(APP_PREFERENCES_SEARCH_MIN_RATING)) {
            val r = settings.getInt(APP_PREFERENCES_SEARCH_MIN_RATING, 0)
            viewModel.setSearchRating(r)
            ratingBarInSearch.rating = r.toFloat()
        }
        viewModel.newSearch(search, antiSearch, searchName)
    }


    private fun scheduleSeekbarUpdate() {
        stopSeekbarUpdate()
        if (!mExecutorService.isShutdown) {
            Log.i(TAG, "sheduleSeekbarUpdate")
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                {
                    mHandler.post {
                        var currentPosition: Int = mLastPlaybackStatePosition
                        val timeDelta =
                            SystemClock.elapsedRealtime() - mLastPlaybackStatePositionTime
                        currentPosition += (timeDelta * 1).toInt()
                        seekBar.max = (App.exoPlayer.duration / 1000).toInt()
                        seekBar.progress = currentPosition/1000
                    }
                }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS
            )
        }
    }

    private fun stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture!!.cancel(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerServiceBinder = null
        if (mediaController != null) {
            mediaController!!.unregisterCallback(callback)
            mediaController = null
        }
        unbindService(serviceConnection)
    }

    private val mHandler = Handler()

    private val mExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private var mScheduleFuture: ScheduledFuture<*>? = null

    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        searchByNameEnterField.isCursorVisible = false
        footerBar.visibility = View.VISIBLE
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val v = currentFocus
        if (v != null &&
            (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) &&
            v is EditText
        ) {
            hideKeyboard(this.window.decorView)
        }
        return super.dispatchTouchEvent(ev)
    }
}
