package com.jgm90.cloudmusic.feature.playlist.presentation.contract

interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean

    fun onItemDismiss(position: Int)
}
