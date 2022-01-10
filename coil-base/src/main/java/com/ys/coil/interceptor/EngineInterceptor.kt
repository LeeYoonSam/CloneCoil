package com.ys.coil.interceptor

import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.interceptor.Interceptor.Chain
import com.ys.coil.memory.RequestService
import com.ys.coil.request.ImageResult
import com.ys.coil.util.Logger

internal class EngineInterceptor(
	private val imageLoader: ImageLoader,
	private val requestService: RequestService,
	private val logger: Logger?
) : Interceptor {

	override suspend fun interceptor(chain: Chain): ImageResult {
		TODO("Not yet implemented")
	}

	@VisibleForTesting
	internal class ExecuteResult(
		val drawable: Drawable,
		val isSampled: Boolean,
		val dataSource: DataSource,
		val diskCacheKey: String?
	) {
		fun copy(
			drawable: Drawable = this.drawable,
			isSampled: Boolean = this.isSampled,
			dataSource: DataSource = this.dataSource,
			diskCacheKey: String? = this.diskCacheKey
		) = ExecuteResult(drawable, isSampled, dataSource, diskCacheKey)
	}

	companion object {
		private const val TAG = "EngineInterceptor"
		@VisibleForTesting internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
		@VisibleForTesting internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_WIDTH = "coil#width"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_HEIGHT = "coil#height"
		@VisibleForTesting internal const val MEMORY_CACHE_KEY_TRANSFORMATIONS = "coil#transformations"
		@VisibleForTesting internal const val TRANSFORMATIONS_DELIMITER = '~'
	}
}