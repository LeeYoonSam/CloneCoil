package com.ys.coil.fetch

import android.content.Context
import android.graphics.Bitmap
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.Options
import com.ys.coil.size.Size
import com.ys.coil.util.toDrawable

internal class BitmapFetcher(
    private val context: Context
) : Fetcher<Bitmap> {

    override suspend fun fetch(
        pool: BitmapPool,
        data: Bitmap,
        size: Size,
        options: Options
    ): FetchResult {
        return DrawableResult(
            drawable = data.toDrawable(context),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}