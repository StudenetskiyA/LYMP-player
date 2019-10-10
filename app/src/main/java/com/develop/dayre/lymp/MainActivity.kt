package com.develop.dayre.lymp

import com.develop.dayre.tagfield.TagView
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

var array = arrayOf("Melbourne", "Vienna", "Vancouver", "Toronto", "Calgary", "Adelaide", "Perth", "Auckland", "Helsinki", "Hamburg", "Munich", "New York", "Sydney", "Paris", "Cape Town", "Barcelona", "London", "Bangkok")

class MainActivity : AppCompatActivity(), ILYMPView, ILYMPObserver {
    private val tag = "$APP_TAG/view"

    private lateinit var tagView: TagView
    private lateinit var presenter: ILYMPPresenter
    private lateinit var model: LYMPModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongListAdapter
    private lateinit var listView: ListView

    override fun update() {
        Log.i(tag,"update")
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
            Log.i(tag, "test button pressed")
            presenter.testPress()
        }
        nextbutton.setOnClickListener {
            Log.i(tag, "next button pressed")
            presenter.nextPress()
        }
        prevbutton.setOnClickListener {
            Log.i(tag, "next button pressed")
            presenter.prevPress()
        }
        shufflebutton.setOnClickListener {
            Log.i(tag, "shuffle button pressed")
            presenter.shufflePress()
        }
        clear_tag.setOnClickListener{
            Log.i(tag, "clear tag button pressed")
            presenter.clearTagPress()
        }

        buildLinkField()
        buildSearchField()
    }

    override fun createView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.test = model
        binding.executePendingBindings()

        //Делай это после биндинга, иначе не видно.
        listView = findViewById(R.id.current_list)
        adapter = SongListAdapter(model.getCurrentSongsList(), this)
        listView.adapter = adapter

        tagView = this.findViewById(R.id.tagview)

        val tag = Tag("Rock")
        tag.tagTextColor = Color.BLACK
        tag.layoutBorderSize = 2f
        tag.layoutBorderColor = Color.BLACK
        tag.radius = 20f
        tag.layoutColor = Color.TRANSPARENT
        // tag.isDeletable = true;
        tagView.addTag(tag)

        tagView.setOnTagClickListener { position, tag ->
            Log.i("TESTTAG", "click $position")
            tagView.tags[position].layoutColor = Color.YELLOW
            tagView.drawTags()
        }
        tagView.setOnTagDeleteListener { position, tag ->
            Toast.makeText(
                this@MainActivity,
                "delete tag id = " + tag.id + " position =" + position,
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Потом будем передавать контекст сервиса, а не активити.
        model = LYMPModel(this)
        model.addObserver(this)
        presenter = LYMPPresenter(model, this)
    }

    private fun buildSearchField() {
        val definition = model.getAllTags().trim { it <= ' ' }
        search_track_tags.movementMethod = LinkMovementMethod.getInstance()
        search_track_tags.setText(definition, TextView.BufferType.SPANNABLE)

        val spans = search_track_tags.text as Spannable
        val indices = getSpaceIndices(model.getAllTags(), SPACE_IN_LINK)
        var start = 0
        var end: Int
        for (i in 0..indices.size) {
            val clickSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val tv = widget as TextView
                    val s = tv.text.subSequence(tv.selectionStart, tv.selectionEnd).toString()
                    Log.i(tag, "from search link clicked $s")
                    if (s != "") {
                        selectLinkForSearch(s)
                    }
                    buildSearchField()
                }

                override fun updateDrawState(ds: TextPaint) {}
            }
            //Для последнего/единственного слова
            end = if (i < indices.size) indices[i] else spans.length
            spans.setSpan(clickSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = end + 1
        }

        var compareText = search_track_tags.text.toString()
        compareText = "$SPACE_IN_LINK $compareText$SPACE_IN_LINK"
        val ls = model.getAllTags().split("$SPACE_IN_LINK ")
        for (i in ls.indices) {
            if (model.getCurrentSearchTags().contains(SPACE_IN_LINK+ ls[i] + SPACE_IN_LINK)) {
                //Log.i(tag, "link must be orange = " + ls[i])
                val startIndex = compareText.indexOf("$SPACE_IN_LINK " + ls[i] + SPACE_IN_LINK, 0)
                if (startIndex != -1) {
                    spans.setSpan(
                        ForegroundColorSpan(resources.getColor(R.color.selectLink)),
                        startIndex,
                        startIndex + ls[i].length,
                        Spannable.SPAN_PRIORITY_SHIFT
                    )
                }
            }

        }
        search_track_tags.text = spans
    }

    //Формирует кликабельный и выделенный текст из model.getAllTag()
    //Надо вызывать после изменений.
    private fun buildLinkField() {
        val definition = model.getAllTags().trim { it <= ' ' }
        current_track_tags.movementMethod = LinkMovementMethod.getInstance()
        current_track_tags.setText(definition, TextView.BufferType.SPANNABLE)

        val spans = current_track_tags.text as Spannable
        val indices = getSpaceIndices(model.getAllTags(), SPACE_IN_LINK)
        var start = 0
        var end: Int
        for (i in 0..indices.size) {
            val clickSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val tv = widget as TextView
                    val s = tv.text.subSequence(tv.selectionStart, tv.selectionEnd).toString()
                    Log.i(tag, "from link clicked $s")
                    if (s != "") {
                        selectLinkForTrack(s)
                    }
                    buildLinkField()
                }

                override fun updateDrawState(ds: TextPaint) {}
            }
            //Для последнего/единственного слова
            end = if (i < indices.size) indices[i] else spans.length
            spans.setSpan(clickSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = end + 1
        }

        var compareText = current_track_tags.text.toString()
        compareText = "$SPACE_IN_LINK $compareText$SPACE_IN_LINK"
        val ls = model.getAllTags().split("$SPACE_IN_LINK ")
        for (i in ls.indices) {
            if (model.getCurrentSong() != null) {
                if (model.getCurrentSong()!!.tags.contains(SPACE_IN_LINK+ls[i] + SPACE_IN_LINK)) {
                    //Log.i(tag, "link must be orange = " + ls[i])
                    val startIndex =
                        compareText.indexOf("$SPACE_IN_LINK " + ls[i] + SPACE_IN_LINK, 0)
                    if (startIndex != -1) {
                        spans.setSpan(
                            ForegroundColorSpan(resources.getColor(R.color.selectLink)),
                            startIndex,
                            startIndex + ls[i].length,
                            Spannable.SPAN_PRIORITY_SHIFT
                        )
                    }
                }
            }
        }
        current_track_tags.text = spans
    }

    //Обработка нажатий на отдельное слово в SearchField
    private fun selectLinkForSearch(linkSelected: String) {
        var txt = linkSelected
        // if (model.getCurrentSearchTags() == "") model. = ";"

        txt = txt.trim { it <= ' ' }

        val tags = if (model.getCurrentSearchTags().contains(SPACE_IN_LINK + txt + SPACE_IN_LINK)) {
            val end = model.getCurrentSearchTags().indexOf(txt + SPACE_IN_LINK)
            model.getCurrentSearchTags().substring(0, end) +
                    model.getCurrentSearchTags().substring(
                        end + txt.length + 1,
                        model.getCurrentSearchTags().length
                    )
        } else {
            model.getCurrentSearchTags() + txt + SPACE_IN_LINK
        }
        Log.i(tag, "Current search tags is $tags")
        presenter.newSearch(tags)

        //Нужно запускать после каждого изменения.
        buildSearchField()
    }

    //Обработка нажатий на отдельное слово в LinkField
    private fun selectLinkForTrack(linkSelected: String) {
        var txt = linkSelected
        if (model.getCurrentSong() != null && model.getCurrentSong()!!.tags == "") model.getCurrentSong()!!.tags = ";"

        txt = txt.trim { it <= ' ' }
        val cs = model.getCurrentSong()?.copy()
        Log.i(tag, cs?.tags)
        if (cs != null) {
            if (cs.tags.contains(SPACE_IN_LINK + txt + SPACE_IN_LINK)) {
                val end = cs.tags.indexOf(txt + SPACE_IN_LINK)
                cs.tags = cs.tags.substring(0, end) + cs.tags.substring(
                    end + txt.length + 1,
                    cs.tags.length
                )
            } else {
                cs.tags = cs.tags + txt + SPACE_IN_LINK
            }
            Log.i(tag, "Current track tags is ${cs.tags}")
            presenter.currentSongEdit(cs)
        }
        //Нужно запускать после каждого изменения.
        buildLinkField()
    }
}
