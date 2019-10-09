package com.develop.dayre.lymp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.activity_main.*
import com.develop.dayre.lymp.databinding.ActivityMainBinding

const val APP_TAG = "lymp"

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        model = LYMPModel()
        presenter = LYMPPresenter(model, this)
    }
}
