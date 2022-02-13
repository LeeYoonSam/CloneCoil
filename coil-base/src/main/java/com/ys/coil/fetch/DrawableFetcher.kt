package com.ys.coil.fetch

import android.graphics.drawable.Drawable
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.request.Options
import com.ys.coil.util.DrawableUtils
import com.ys.coil.util.isVector
import com.ys.coil.util.toDrawable

internal class DrawableFetcher(
    private val data: Drawable,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val isVector = data.isVector
        return DrawableResult(
            drawable = if (isVector) {
                DrawableUtils.convertToBitmap(
                    drawable = data,
                    config = options.config,
                    size = options.size,
                    scale = options.scale,
                    allowInexactSize = options.allowInexactSize
                ).toDrawable(options.context)
            } else {
                data
            },
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Drawable> {
        override fun create(data: Drawable, options: Options, imageLoader: ImageLoader): Fetcher {
            return DrawableFetcher(data, options)
        }
    }
}
