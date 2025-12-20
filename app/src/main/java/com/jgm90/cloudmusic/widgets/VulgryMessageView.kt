package com.jgm90.cloudmusic.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jgm90.cloudmusic.R

class VulgryMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {
    private val imageView: ImageView
    private val textView: TextView

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VulgryMessageView, 0, 0)
        val img = typedArray.getDrawable(R.styleable.VulgryMessageView_imageDrawable)
        val text = typedArray.getString(R.styleable.VulgryMessageView_messageText)
        typedArray.recycle()
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.vulgry_message_view, this)
        imageView = findViewById(R.id.message_img)
        img?.let { imageView.setImageDrawable(it) }
        textView = findViewById(R.id.message_text)
        text?.let { textView.text = it }
    }

    fun setDrawable(drawable: Drawable) {
        imageView.setImageDrawable(drawable)
    }

    fun setDrawable(drawable: Int) {
        val img = ContextCompat.getDrawable(context, drawable)
        imageView.setImageDrawable(img)
    }

    fun setText(message: String) {
        textView.text = message
    }

    fun setText(message: Int) {
        textView.setText(message)
    }
}
