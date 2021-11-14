package com.ys.coil.map

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal class StringMapper : Mapper<String, HttpUrl> {
    override fun map(data: String): HttpUrl = data.toHttpUrl()
}