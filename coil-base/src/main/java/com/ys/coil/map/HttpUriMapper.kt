package com.ys.coil.map

import android.net.Uri
import androidx.collection.arraySetOf
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal class HttpUriMapper : Mapper<Uri, HttpUrl> {

    companion object {
        private val SUPPORTED_SCHEMES = arraySetOf("http", "https")
    }

    override fun handles(data: Uri) = SUPPORTED_SCHEMES.contains(data.scheme)

    override fun map(data: Uri): HttpUrl = data.toString().toHttpUrl()
}