package com.ys.coil.util

import com.ys.coil.ImageLoader
import com.ys.coil.RealImageLoader
import com.ys.coil.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM

/**
 * [RealImageLoader]에서 사용하는 개인 구성 옵션입니다.
 *
 * @see ImageLoader.Builder
 */
internal class ImageLoaderOptions(
	val addLastModifiedToFileCacheKey: Boolean = true,
	val networkObserverEnabled: Boolean = true,
	val respectCacheHeaders: Boolean = true,
	val bitmapFactoryMaxParallelism: Int = DEFAULT_MAX_PARALLELISM
) {
	fun copy(
		addLastModifiedToFileCacheKey: Boolean = this.addLastModifiedToFileCacheKey,
		networkObserverEnabled: Boolean = this.networkObserverEnabled,
		respectCacheHeaders: Boolean = this.respectCacheHeaders,
		bitmapFactoryMaxParallelism: Int = this.bitmapFactoryMaxParallelism,
	) = ImageLoaderOptions(
		addLastModifiedToFileCacheKey,
		networkObserverEnabled,
		respectCacheHeaders,
		bitmapFactoryMaxParallelism,
	)
}