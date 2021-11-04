package com.ys.coil

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import com.ys.coil.request.GetRequest
import com.ys.coil.request.LoadRequest
import com.ys.coil.request.RequestDisposable
import okhttp3.OkHttpClient

internal class RealImageLoader(
    private val context: Context,
    override val defaults: DefaultRequestOptions,
    bitmapPoolSize: Long,
    memoryCacheSize: Int,
    okHttpClient: OkHttpClient,
    registry: ComponentRegistry
) : ImageLoader, ComponentCallbacks {
    override fun load(request: LoadRequest): RequestDisposable {
        TODO("Not yet implemented")
    }

    override suspend fun get(request: GetRequest): Drawable {
        TODO("Not yet implemented")
    }

    override fun clearMemory() {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }

    override fun onConfigurationChanged(p0: Configuration) {
        TODO("Not yet implemented")
    }

    override fun onLowMemory() {
        TODO("Not yet implemented")
    }
}