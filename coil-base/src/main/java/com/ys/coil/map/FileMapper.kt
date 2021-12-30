package com.ys.coil.map

import android.net.Uri
import androidx.core.net.toUri
import com.ys.coil.request.Options
import java.io.File

internal class FileMapper : Mapper<File, Uri> {
    override fun map(data: File, options: Options) = data.toUri()
}