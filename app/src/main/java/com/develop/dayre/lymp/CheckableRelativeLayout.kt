package com.develop.dayre.lymp

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.RelativeLayout

class CheckableRelativeLayout(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), Checkable {
    private var isChecked = false

    override fun isChecked(): Boolean {
        return isChecked
    }

    override fun setChecked(isChecked: Boolean) {
        this.isChecked = isChecked
        changeColor(isChecked)
    }

    override fun toggle() {
        this.isChecked = !this.isChecked
        changeColor(this.isChecked)
    }

    private fun changeColor(isChecked: Boolean) {
        if (isChecked) {
            setBackgroundColor(resources.getColor(R.color.select_list_item))
        } else {
            setBackgroundColor(resources.getColor(android.R.color.transparent))
        }
    }
}