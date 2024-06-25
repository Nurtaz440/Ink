package com.nurtaz.dev.inkrecognation

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class StatusTextView : AppCompatTextView, StrokeManager.StatusChangedListener {
    private lateinit var strokeManager : StrokeManager
    constructor(context: Context) : super(context){}
    constructor(context: Context?, attributeSet: AttributeSet?) : super(
            context!!,
            attributeSet
            ){}
    override fun onStatusChanged(){
        this.text = strokeManager!!.status
    }
    fun setStrokeManager(strokeManager:StrokeManager){
        this.strokeManager = strokeManager
    }
}