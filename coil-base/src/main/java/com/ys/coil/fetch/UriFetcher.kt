package com.ys.coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.collection.arraySetOf
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.Options
import com.ys.coil.size.Size
import okio.buffer
import okio.source

class UriFetcher(
    private val context: Context
) : Fetcher<Uri> {

    companion object {
        private val SUPPORTED_SCHEMES = arraySetOf(
            ContentResolver.SCHEME_ANDROID_RESOURCE,
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE
        )
    }

    override fun handles(data: Uri) = SUPPORTED_SCHEMES.contains(data.scheme)

    override fun key(data: Uri) = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        return SourceResult(
            source = checkNotNull(context.contentResolver.openInputStream(data)).source().buffer(),
            mimeType = context.contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }
}