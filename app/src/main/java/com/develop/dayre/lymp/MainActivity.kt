package com.develop.dayre.lymp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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

const val APP_TAG = "lymp"
const val SPACE_IN_LINK = ';'

class MainActivity : AppCompatActivity() {
    private val TAG = "$APP_TAG/view"

    private lateinit var tagView: TagView
    private lateinit var searchTagView: TagView

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView

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

    init {
        instance = this
    }

    companion object {
        //Вызывает вопросы
        lateinit var viewModel: LYMPViewModel
        //Или передавать контектст при инициализации вьюмодели и модели.
        //По факту он нужен только для БД.
        var instance: MainActivity? = null
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    private fun createControl() {
        testbutton.setOnClickListener {
            Log.i(TAG, "test button pressed")
            viewModel.testPress()
        }
        nextbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            val intent = Intent(this@MainActivity, LYMPService::class.java)
            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Next)
            startService(intent)
        }
        prevbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            viewModel.prevPress()
        }
        shufflebutton.setOnClickListener {
            Log.i(TAG, "shuffle button pressed")
            viewModel.shufflePress()
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
        }
    }

    private fun createView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //Получаем инстанс, а не создаем новый - актуально при перезапуске приложения, повороте экрана и т.д.
        viewModel = ViewModelProviders.of(this).get(LYMPViewModel::class.java)
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
                if (viewModel.currentSongsList.value!=null && viewModel.currentSongsList.value!!.contains(viewModel.currentSong.value)) {
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

                if (viewModel.currentSongsList.value!=null && viewModel.currentSongsList.value!!.contains(viewModel.currentSong.value)) {
                    val n = viewModel.currentSongsList.value!!.indexOf(viewModel.currentSong.value)
                    Log.i(TAG, "current song index in list $n")
                    listView.setItemChecked(n, true)
                    listView.smoothScrollToPosition(n)
                }

                serv?.mBuilder?.setContentText(viewModel.currentSong.value?.name)
                serv?.startForeground(9595, serv?.notification)

                buildLinkField()
            })
        viewModel.currentSearchTags.observe(this,
            Observer<String> {
                Log.i(TAG, "current search tags updated")
                buildSearchField()
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createView()
        createControl()
        createObservers()

        val intent = Intent(this, LYMPService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

        viewModel.startModel()

        //From settings - только в таком порядке
//        val name = "Русская мечта"
//        viewModel.songSelect(name)
        val search = "rock"
        viewModel.newSearch(search)
    }

    private fun buildSearchField() {
        searchTagView.removeAllTags()
            for (word in getListFromString(viewModel.getAllTags())) {
                val tag = Tag(word)
                tag.tagTextColor = Color.BLACK
                tag.layoutBorderSize = 2f
                tag.layoutBorderColor = Color.BLACK
                tag.radius = 20f
                if (viewModel.currentSearchTags.value!=null && getListFromString(viewModel.currentSearchTags.value!!).contains(word))
                    tag.layoutColor = Color.YELLOW
                else tag.layoutColor = Color.TRANSPARENT
                searchTagView.addTag(tag)
            }
    }

    private fun buildLinkField() {
        tagView.removeAllTags()
        if (viewModel.currentSong.value!=null) {
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
}
