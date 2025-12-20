package com.jgm90.cloudmusic.interfaces

import androidx.recyclerview.widget.RecyclerView

fun interface OnStartDragListener {
    fun onDrag(viewHolder: RecyclerView.ViewHolder)
}
