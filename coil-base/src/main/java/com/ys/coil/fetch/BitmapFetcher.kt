package com.ys.coil.fetch

import android.graphics.Bitmap
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.request.Options
import com.ys.coil.util.toDrawable

internal class BitmapFetcher(
    private val data: Bitmap,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return DrawableResult(
            drawable = data.toDrawable(options.context),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory: Fetcher.Factory<Bitmap> {
        override fun create(data: Bitmap, options: Options, imageLoader: ImageLoader): Fetcher {
            return BitmapFetcher(data, options)
        }
    }
}