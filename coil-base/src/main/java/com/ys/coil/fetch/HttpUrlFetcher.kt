package com.ys.coil.fetch

import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.Options
import com.ys.coil.network.HttpException
import com.ys.coil.size.Size
import com.ys.coil.util.await
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpUrlFetcher(
    private val okHttp: OkHttpClient
) : Fetcher<HttpUrl> {

    companion object {
        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE = CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }

    override fun key(data: HttpUrl): String = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: HttpUrl,
        size: Size,
        options: Options
    ): FetchResult {
        val request = Request.Builder().url(data)

        val networkRead = options.networkCachePolicy.readEnabled
        val diskRead = options.diskCachePolicy.readEnabled

        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }

            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }

            !networkRead && !diskRead -> {
                // 이로 인해 요청이 504 Unsatisfiable Request와 함께 실패합니다.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        val response = okHttp.newCall(request.build()).await()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = checkNotNull(response.body) { "Null response body!" }

        return SourceResult(
            source = body.source(),
            mimeType = body.contentType()?.toString(),
            dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK
        )
    }
}