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
import android.text.Spannable
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.activity_main.*
import com.develop.dayre.lymp.databinding.ActivityMainBinding
import com.develop.dayre.tagfield.OnTagClickListener
import com.develop.dayre.tagfield.OnTagDeleteListener
import com.develop.dayre.tagfield.Tag

const val APP_TAG = "lymp"
const val SPACE_IN_LINK = ';'

class MainActivity : AppCompatActivity(), ILYMPView, ILYMPObserver {
    private val TAG = "$APP_TAG/view"

    private lateinit var tagView: TagView
    private lateinit var searchTagView: TagView

    private lateinit var presenter: ILYMPPresenter

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView

    var serv: LYMPService? = null
    var isBound = false
    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
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
        var instance: MainActivity? = null
        lateinit var model: LYMPModel
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun update() {
        Log.i(TAG, "update")
        buildLinkField()
        buildSearchField()

        //adapter.notifyDataSetChanged() - не работает!
        adapter = SongListAdapter(model.getCurrentSongsList(), this)
        listView.adapter = adapter
    }

    override fun refreshList() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createControl() {
        testbutton.setOnClickListener {
            Log.i(TAG, "test button pressed")
            presenter.testPress()
        }
        nextbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            val intent = Intent(this@MainActivity, LYMPService::class.java)
            intent.putExtra(EXTRA_COMMAND, ServiceCommand.Next)
            startService(intent)
            presenter.nextPress()
        }
        prevbutton.setOnClickListener {
            Log.i(TAG, "next button pressed")
            presenter.prevPress()
        }
        shufflebutton.setOnClickListener {
            Log.i(TAG, "shuffle button pressed")
            presenter.shufflePress()
        }
        clear_tag.setOnClickListener {
            Log.i(TAG, "clear tag button pressed")
            presenter.clearTagPress()
        }
    }

    override fun createView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.test = model
        binding.executePendingBindings()

        //Делай это после биндинга, иначе не видно.
        listView = findViewById(R.id.current_list)
        adapter = SongListAdapter(model.getCurrentSongsList(), this)
        listView.adapter = adapter

        tagView = this.findViewById(R.id.current_track_tags)
        searchTagView = this.findViewById(R.id.search_track_tags)

        tagView.setOnTagClickListener { position, _ ->
            val cs = model.getCurrentSong()?.copy()
            if (cs != null) {
                val tagClicked = getListFromString(model.getAllTags())[position]
                val list = ArrayList(getListFromString(model.getCurrentSong()!!.tags))
                if (list.contains(tagClicked)) {
                    list.remove(tagClicked)
                } else {
                    list.add(tagClicked)
                }
                cs.setTagsFromList(list)
                Log.i(TAG, "Current song tags is ${cs.tags}")
                presenter.currentSongEdit(cs)
            }
     //       buildLinkField()
        }
        tagView.setOnTagDeleteListener { position, tag ->
            Toast.makeText(
                this@MainActivity,
                "delete tag id = " + tag.id + " position =" + position,
                Toast.LENGTH_SHORT
            ).show()
        }
        searchTagView.setOnTagClickListener { position, _ ->
            val cs = model.getCurrentSearchTags()
            val tagClicked = getListFromString(model.getAllTags())[position]
            val list = ArrayList(getListFromString(cs))
            if (list.contains(tagClicked)) {
                list.remove(tagClicked)
            } else {
                list.add(tagClicked)
            }
            Log.i(TAG, "Current search tags is ${getStringFromList(list)}")
            presenter.newSearch(getStringFromList(list))
            //buildSearchField()
        }

        buildLinkField()
        buildSearchField()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, LYMPService::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

//        val intent = Intent(this@MainActivity, LYMPService::class.java)


        //Потом будем передавать контекст сервиса, а не активити.
        model = LYMPModel.instance
        model.addObserver(this)
        presenter = LYMPPresenter(model, this)
    }

    private fun buildSearchField() {
        searchTagView.removeAllTags()
        for (word in getListFromString(model.getAllTags())) {
            val tag = Tag(word)
            tag.tagTextColor = Color.BLACK
            tag.layoutBorderSize = 2f
            tag.layoutBorderColor = Color.BLACK
            tag.radius = 20f
            if (getListFromString(model.getCurrentSearchTags()).contains(word))
                tag.layoutColor = Color.YELLOW
            else tag.layoutColor = Color.TRANSPARENT
            searchTagView.addTag(tag)
        }
    }

    //Формирует кликабельный и выделенный текст из model.getAllTag()
    //Надо вызывать после изменений.
    private fun buildLinkField() {
        tagView.removeAllTags()
        for (word in getListFromString(model.getAllTags())) {
            val tag = Tag(word)
            tag.tagTextColor = Color.BLACK
            tag.layoutBorderSize = 2f
            tag.layoutBorderColor = Color.BLACK
            tag.radius = 20f
            if (model.getCurrentSong()!=null && getListFromString(model.getCurrentSong()!!.tags).contains(word))
                tag.layoutColor = Color.YELLOW
            else tag.layoutColor = Color.TRANSPARENT
            tagView.addTag(tag)
        }
    }
}
