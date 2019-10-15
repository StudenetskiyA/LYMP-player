package com.develop.dayre.lymp

import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

class OpenFolderDialog(context: Context) : AlertDialog.Builder(context) {
    val APPLICATION_TAG = "LYMP/OpenDialog"
    private var currentPath = Environment.getExternalStorageDirectory().path
    private val files = ArrayList<File>()
    private val title: TextView
    private val listView: ListView
    private val filenameFilter: FilenameFilter? = null
    private var selectedIndex = -1
    private var listener: OpenDialogListener? = null
    private var folderIcon: Drawable? = null
    private var fileIcon: Drawable? = null
    private var accessDeniedMessage: String? = null

    interface OpenDialogListener {
        fun OnSelectedFile(fileName: String)
    }

    private inner class FileAdapter(context: Context, files: List<File>) :
        ArrayAdapter<File>(context, android.R.layout.simple_list_item_1, files) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val file = getItem(position)
            if (view != null) {
                view.text = file!!.name
                setDrawable(view, fileIcon)
                if (selectedIndex == position)
                    view.setBackgroundColor(context.resources.getColor(android.R.color.holo_blue_dark))
                else
                    view.setBackgroundColor(context.resources.getColor(android.R.color.transparent))
                //                }
            }
            return view
        }

        private fun setDrawable(view: TextView?, drawable: Drawable?) {
            if (view != null) {
                if (drawable != null) {
                    drawable.setBounds(0, 0, 60, 60)
                    view.setCompoundDrawables(drawable, null, null, null)
                } else {
                    view.setCompoundDrawables(null, null, null, null)
                }
            }
        }
    }

    init {
        title = createTitle(context)
        changeTitle()
        val linearLayout = createMainLayout(context)
        linearLayout.addView(createBackItem(context))
        listView = createListView(context)
        linearLayout.addView(listView)
        setCustomTitle(title)
            .setView(linearLayout)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                //My
                Log.i(APPLICATION_TAG, "New folder choice : $currentPath")
//                amf.editor.putString(amf.lma.APP_PREFERENCES_MUSIC_FOLDER, currentPath)
//                // specialSearch="";
//                amf.editor.putString(amf.lma.APP_PREFERENCES_SEARCH_SPECIAL, "")
//                amf.editor.apply()
//                amf.editor.apply()
//                amf.lma.recreate()
                if (selectedIndex > -1 && listener != null) {
                    listener!!.OnSelectedFile(listView.getItemAtPosition(selectedIndex).toString())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun show(): AlertDialog {
        files.addAll(getFiles(currentPath))
        listView.adapter = FileAdapter(context, files)
        return super.show()
    }

    fun setOpenDialogListener(listener: OpenDialogListener): OpenFolderDialog {
        this.listener = listener
        return this
    }

    fun setFolderIcon(drawable: Drawable): OpenFolderDialog {
        this.folderIcon = drawable
        return this
    }

    fun setFileIcon(drawable: Drawable): OpenFolderDialog {
        this.fileIcon = drawable
        return this
    }

    fun setAccessDeniedMessage(message: String): OpenFolderDialog {
        this.accessDeniedMessage = message
        return this
    }

    private fun createMainLayout(context: Context): LinearLayout {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.minimumHeight = getLinearLayoutMinHeight(context)
        return linearLayout
    }

    private fun getItemHeight(context: Context): Int {
        val value = TypedValue()
        val metrics = DisplayMetrics()
        context.theme.resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true)
        getDefaultDisplay(context).getMetrics(metrics)
        return TypedValue.complexToDimension(value.data, metrics).toInt()
    }

    private fun createTextView(context: Context, style: Int): TextView {
        val textView = TextView(context)
        textView.setTextAppearance(context, style)
        val itemHeight = getItemHeight(context)
        textView.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        textView.minHeight = itemHeight
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.setPadding(15, 0, 0, 0)
        return textView
    }

    private fun createTitle(context: Context): TextView {
        return createTextView(context, R.style.TextAppearance_DeviceDefault_DialogWindowTitle)
    }

    private fun createBackItem(context: Context): TextView {
        val textView = createTextView(context, android.R.style.TextAppearance_DeviceDefault_Small)
        val drawable = getContext().resources.getDrawable(android.R.drawable.ic_menu_directions)
        drawable.setBounds(0, 0, 60, 60)
        textView.setCompoundDrawables(drawable, null, null, null)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.setOnClickListener {
            //TODO
            val file = File(currentPath)
            val parentDirectory = file.parentFile
            if (parentDirectory != null) {
                //Log.i(APPLICATION_TAG,"DIRECTORY = "+parentDirectory.getPath());
                currentPath = parentDirectory.path
                RebuildFiles(listView.adapter as FileAdapter)
            }
        }
        return textView
    }

    fun getTextWidth(text: String, paint: Paint): Int {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.left + bounds.width() + 80
    }

    private fun changeTitle() {
        var titleText = currentPath
        val screenWidth = getScreenSize(context).x
        val maxWidth = (screenWidth * 0.99).toInt()
        if (getTextWidth(titleText, title.paint) > maxWidth) {
            while (getTextWidth("...$titleText", title.paint) > maxWidth) {
                val start = titleText.indexOf("/", 2)
                if (start > 0)
                    titleText = titleText.substring(start)
                else
                    titleText = titleText.substring(2)
            }
            title.text = "...$titleText"
        } else {
            title.text = titleText
        }
    }

    private fun getFiles(directoryPath: String): ArrayList<File> {
        val directory = File(directoryPath)

        val files = directory.listFiles(FileFilter { file ->
            //If a file or directory is hidden, or unreadable, don't show it in the list.</div>
            if (file.isHidden)
                return@FileFilter false
            if (!file.canRead())
                return@FileFilter false
            //Show all directories in the list.
            if (file.isDirectory) true else true

            //TODO
        })

        val filelist = ArrayList(Arrays.asList(*files))
        Collections.sort(filelist)

        return filelist
    }

    private fun RebuildFiles(adapter: ArrayAdapter<File>) {
        try {
            val fileList = getFiles(currentPath)
            files.clear()
            selectedIndex = -1
            files.addAll(fileList)
            adapter.notifyDataSetChanged()
            changeTitle()
        } catch (e: NullPointerException) {
            var message = context.resources.getString(android.R.string.unknownName)
            if (accessDeniedMessage != "")
                message = accessDeniedMessage
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun createListView(context: Context): ListView {
        val listView = ListView(context)
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        listView.requestFocusFromTouch()
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, index, l ->
                val adapter = adapterView.adapter as FileAdapter
                val file = adapter.getItem(index)
                if (file!!.isDirectory) {
                    Log.i(APPLICATION_TAG, "FOLDER = " + file.name)
                    //                    listView.setItemChecked(index,true);
                    //                    if (index != selectedIndex)
                    //                        selectedIndex = index;
                    //                    else
                    //                        selectedIndex = -1;
                    ////                    adapter.notifyDataSetChanged();
                    //                    selectedIndex=-1;
                    //                    adapter.notifyDataSetChanged();
                    currentPath = file.path
                    RebuildFiles(adapter)
                } else {
                    //                    if (index != selectedIndex)
                    //                        selectedIndex = index;
                    //                    else
                    //                        selectedIndex = -1;
                    //                    adapter.notifyDataSetChanged();
                }
            }
        return listView
    }

    companion object {


        val APPLICATION_TAG = "LYM-tag-open"

        private fun getDefaultDisplay(context: Context): Display {
            return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }

        private fun getScreenSize(context: Context): Point {
            val screeSize = Point()
            getDefaultDisplay(context).getSize(screeSize)
            return screeSize
        }

        private fun getLinearLayoutMinHeight(context: Context): Int {
            return getScreenSize(context).y
        }
    }
}