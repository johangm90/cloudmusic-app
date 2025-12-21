package com.jgm90.cloudmusic.core.model

import com.jgm90.cloudmusic.core.util.SharedUtils

data class SongModel(
    val id: String?,
    val name: String,
    val artist: List<String> = emptyList(),
    val album: String,
    val pic_id: String?,
    val url_id: String?,
    val lyric_id: String?,
    val source: String?,
    var local_file: String? = null,
    var local_thumbnail: String? = null,
    var local_lyric: String? = null,
    var position: Int = 0,
    var position_date: String? = null,
    val playlist_id: Int = 0,
) {
    fun getCoverThumbnail(): String {
        return if (pic_id != null) {
            SharedUtils.server + "pic/" + pic_id
        } else {
            ""
        }
    }

    fun getAudioUrl(): String {
        return if (url_id != null) {
            SharedUtils.server + "play/" + id + "/160"
        } else {
            ""
        }
    }

    fun getFileName(): String {
        var filename = "$name.mp3"
        filename = filename.replace(
            "\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/".toRegex(),
            ""
        )
        return filename
    }
}
