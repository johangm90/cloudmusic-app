package com.jgm90.cloudmusic.feature.playlist.presentation.contract

import androidx.recyclerview.widget.RecyclerView

fun interface OnStartDragListener {
    fun onDrag(viewHolder: RecyclerView.ViewHolder)
}
