package com.ys.coil.fetch

import android.graphics.drawable.Drawable
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.DrawableDecoderService
import com.ys.coil.decode.Options
import com.ys.coil.size.Size

internal class DrawableFetcher(
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<Drawable> {
    override suspend fun fetch(
        pool: BitmapPool,
        data: Drawable,
        size: Size,
        options: Options
    ): FetchResult {
        return DrawableResult(
            drawable = drawableDecoder.convertIfNecessary(
                drawable = data,
                size = size,
                config = options.config
            ),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}