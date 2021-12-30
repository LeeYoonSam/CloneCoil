package com.ys.coil.map

import android.net.Uri
import androidx.core.net.toUri
import com.ys.coil.request.Options

internal class StringMapper : Mapper<String, Uri> {
    override fun map(data: String, options: Options) = data.toUri()
}