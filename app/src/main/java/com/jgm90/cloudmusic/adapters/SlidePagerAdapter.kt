package com.jgm90.cloudmusic.adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager.widget.PagerAdapter
import com.jgm90.cloudmusic.GlideApp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.models.SongModel
import com.jgm90.cloudmusic.utils.SharedUtils

class SlidePagerAdapter(
    private val context: Context,
    private val items: List<SongModel>,
) : PagerAdapter() {
    private var song: SongModel? = null

    override fun getCount(): Int = items.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.slide, container, false)
        val albumArt: AppCompatImageView = view.findViewById(R.id.album_art)
        val lyricsView: RelativeLayout = view.findViewById(R.id.lyrics_view)
        val currentLine: TextView = view.findViewById(R.id.lbl_currect_line)
        val nextLine: TextView = view.findViewById(R.id.lbl_next_line)
        albumArt.setImageResource(R.drawable.default_cover)
        song = items[position]
        val currentSong = song ?: return view
        view.tag = "rootView_${currentSong.id}"
        lyricsView.tag = "lyricsView_${currentSong.id}"
        currentLine.tag = "currentView_${currentSong.id}"
        nextLine.tag = "nextView_${currentSong.id}"
        val picUrl = if (!TextUtils.isEmpty(currentSong.local_thumbnail)) {
            currentSong.local_thumbnail
        } else {
            SharedUtils.server + "pic/" + currentSong.pic_id
        }
        GlideApp
            .with(context)
            .load(picUrl)
            .centerCrop()
            .placeholder(R.drawable.default_cover)
            .into(albumArt)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }
}
