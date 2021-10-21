package com.ys.coil.fetch

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.DrawableRes
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.DrawableDecoderService
import com.ys.coil.decode.Options
import com.ys.coil.size.Size
import com.ys.coil.util.getDrawableCompat
import okio.buffer
import okio.source

internal class ResourceFetcher(
    private val context: Context,
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<@DrawableRes Int> {

    override fun handles(@DrawableRes data: Int) = try {
        context.resources.getResourceName(data) != null
    } catch (e: Resources.NotFoundException) {
        false
    }

    override fun key(@DrawableRes data: Int) = "res_$data"

    @SuppressLint("ResourceType")
    override suspend fun fetch(
        pool: BitmapPool,
        @DrawableRes data: Int,
        size: Size,
        options: Options
    ): FetchResult {
        return if (context.resources.isXmlResource(data)) {
            DrawableResult(
                drawable = drawableDecoder.convertIfNecessary(
                    drawable = context.getDrawableCompat(data),
                    size = size,
                    config = options.config
                ),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        } else {
            SourceResult(
                source = context.resources.openRawResource(data).source().buffer(),
                mimeType = context.getType(data),
                dataSource = DataSource.MEMORY
            )
        }
    }

    private fun Context.getType(@DrawableRes resId: Int): String? {
        return contentResolver.getType(resources.resIdToUri(resId))
    }

    private fun Resources.resIdToUri(@DrawableRes resId: Int): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getResourcePackageName(resId))
            .appendPath(getResourceTypeName(resId))
            .appendPath(getResourceEntryName(resId))
            .build()
    }

    private fun Resources.isXmlResource(@DrawableRes resId: Int): Boolean {
        val fileName = TypedValue().apply { getValue(resId, this, true) }.string
        return fileName?.endsWith(".xml") == true
    }
}