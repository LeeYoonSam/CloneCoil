package com.ys.coil.util

import android.content.Context
import android.graphics.Bitmap
import com.ys.coil.DefaultRequestOptions
import com.ys.coil.decode.Options
import com.ys.coil.request.*
import com.ys.coil.size.Scale
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.source

fun createMockWebServer(context: Context, vararg images: String): MockWebServer {
    return MockWebServer().apply {
        images.forEach { image ->
            val buffer = Buffer()
            context.assets.open(image).source().buffer().readAll(buffer)
            enqueue(MockResponse().setBody(buffer))
        }
        start()
    }
}

fun createOptions(): Options {
    return Options(
        config = Bitmap.Config.ARGB_8888,
        colorSpace = null,
        scale = Scale.FILL,
        allowRgb565 = false,
        networkCachePolicy = CachePolicy.ENABLED,
        diskCachePolicy = CachePolicy.ENABLED
    )
}

inline fun createGetRequest(
    builder: GetRequestBuilder.() -> Unit = {}
): GetRequest = GetRequestBuilder(DefaultRequestOptions()).data(Any()).apply(builder).build()

inline fun createLoadRequest(
    context: Context,
    builder: LoadRequestBuilder.() -> Unit = {}
): LoadRequest = LoadRequestBuilder(context, DefaultRequestOptions()).data(Any()).apply(builder).build()