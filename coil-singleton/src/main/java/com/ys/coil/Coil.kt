package com.ys.coil

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.ys.coil.Coil.imageLoader
import com.ys.coil.Coil.setImageLoader
import com.ys.coil.request.Disposable
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult

/**
 * 싱글톤 [ImageLoader] 인스턴스를 보유하는 클래스.
 *
 * - 싱글톤 [ImageLoader]를 얻으려면 [Context.imageLoader](선호) 또는 [imageLoader]를 사용합니다.
 * - 싱글톤 [ImageLoader]를 설정하려면 [ImageLoaderFactory](선호) 또는 [setImageLoader]를 사용합니다.
 */
object Coil {

	private var imageLoader: ImageLoader? = null
	private var imageLoaderFactory: ImageLoaderFactory? = null

	/**
	 * 싱글톤 [ImageLoader]를 가져옵니다.
	 */
	@JvmStatic
	fun imageLoader(context: Context) = imageLoader ?: newImageLoader(context)

	/**
	 * 싱글톤 [ImageLoader]를 설정합니다.
	 * [ImageLoader]를 느리게 생성하려면 `setImageLoader(ImageLoaderFactory)`를 사용하는 것이 좋습니다.
	 */
	@JvmStatic
	@Synchronized
	fun setImageLoader(imageLoader: ImageLoader) {
		this.imageLoaderFactory = null
		this.imageLoader = imageLoader
	}

	/**
	 * 싱글톤 [ImageLoader]를 생성하는 데 사용할 [ImageLoaderFactory]를 설정합니다.
	 * [factory]는 최대 한 번만 호출되도록 보장됩니다.
	 *
	 * 참고: [Factory]는 [ImageLoaderFactory]를 구현하는 [Application]보다 우선합니다.
	 */
	@JvmStatic
	@Synchronized
	fun setImageLoader(factory: ImageLoaderFactory) {
		imageLoaderFactory = factory
		imageLoader = null
	}

	/** 새 싱글톤 [ImageLoader]를 만들고 설정합니다. */
	@Synchronized
	private fun newImageLoader(context: Context): ImageLoader {
		// imageLoader가 방금 설정되었는지 다시 확인합니다.
		imageLoader?.let { return it }

		// 새로운 ImageLoader를 생성합니다.
		val newImageLoader = imageLoaderFactory?.newImageLoader()
			?: (context.applicationContext as? ImageLoaderFactory)?.newImageLoader()
			?: ImageLoader(context)

		imageLoaderFactory = null
		imageLoader = newImageLoader
		return newImageLoader
	}

	/** 내부 상태를 재설정합니다. */
	@VisibleForTesting
	@Synchronized
	internal fun reset() {
		imageLoader = null
		imageLoaderFactory = null
	}

	@Deprecated(
		message = "Replace with 'context.imageLoader.enqueue(request)'.",
		replaceWith = ReplaceWith(
			expression = "request.context.imageLoader.enqueue(request)",
			imports = ["coil.imageLoader"]
		),
		level = DeprecationLevel.ERROR // Temporary migration aid.
	)
	@JvmStatic
	fun enqueue(request: ImageRequest): Disposable = error("Unsupported")

	@Deprecated(
		message = "Replace with 'context.imageLoader.execute(request)'.",
		replaceWith = ReplaceWith(
			expression = "request.context.imageLoader.execute(request)",
			imports = ["coil.imageLoader"]
		),
		level = DeprecationLevel.ERROR // Temporary migration aid.
	)
	@JvmStatic
	@Suppress("RedundantSuspendModifier")
	suspend fun execute(request: ImageRequest): ImageResult = error("Unsupported")
}
