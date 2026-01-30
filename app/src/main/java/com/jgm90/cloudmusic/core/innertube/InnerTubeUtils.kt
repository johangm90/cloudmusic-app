package com.jgm90.cloudmusic.core.innertube

import android.util.Base64
import java.security.MessageDigest

data class InnerTubeLocale(
    val gl: String,
    val hl: String
)

fun parseCookieString(cookie: String): Map<String, String> =
    cookie
        .split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }

fun sha1(str: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(str.toByteArray())
    return digest.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

fun String.encodeBase64(): String =
    Base64.encodeToString(toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
