package com.develop.dayre.lymp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.activity_main.*
import com.develop.dayre.lymp.databinding.ActivityMainBinding

const val APP_TAG = "lymp"
const val SPACE_IN_LINK = ';'

class MainActivity : AppCompatActivity(), ILYMPView {
    private val tag = "$APP_TAG/view"

    private lateinit var presenter : ILYMPPresenter
    private lateinit var model : LYMPModel
    private lateinit var binding: ActivityMainBinding

    override fun refreshList() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createControl() {
        testbutton.setOnClickListener {
            Log.i(tag, "test button pressed")
            presenter.testPress()
        }
    }

    override fun createView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.test = model
        binding.executePendingBindings()

        buildLinkField()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        model = LYMPModel()
        presenter = LYMPPresenter(model, this)
    }


    //Формирует кликабельный и выделенный текст из model.getAllTag()
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
                if (model.getCurrentSong()!!.tags.contains(ls[i] + SPACE_IN_LINK)) {
                    //Log.i(tag, "link must be orange = " + ls[i])
                    val startIndex = compareText.indexOf("$SPACE_IN_LINK " + ls[i] + SPACE_IN_LINK, 0)
                    if (startIndex != -1) {
                        spans.setSpan(ForegroundColorSpan(resources.getColor(R.color.selectLink)), startIndex, startIndex + ls[i].length, Spannable.SPAN_PRIORITY_SHIFT)
                    }
                }
            }
        }
        current_track_tags.text = spans
    }

    //Обработка нажатий на отдельное слово в LinkField
    private fun selectLinkForTrack(linkSelected: String) {
        var txt = linkSelected
        if (model.getCurrentSong() != null && model.getCurrentSong()!!.tags == "") model.getCurrentSong()!!.tags = ";"

        txt = txt.trim { it <= ' ' }
        val cs = model.getCurrentSong()
        if (cs != null) {
            cs.tags = if (cs.tags.contains(txt + SPACE_IN_LINK)) {
                val end = cs.tags.indexOf(txt + SPACE_IN_LINK)
                cs.tags.substring(0, end) + cs.tags.substring(end + txt.length + 1, cs.tags.length)
            } else {
                cs.tags + txt + SPACE_IN_LINK
            }
            Log.d(tag, "Current track tags is ${cs.tags}")
            presenter.currentSongEdit(cs)
        }
        //Нужно запускать после каждого изменения.
        buildLinkField()
    }
}
