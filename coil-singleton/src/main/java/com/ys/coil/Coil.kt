package com.ys.coil

import android.content.Context
import com.ys.coil.Coil.imageLoader

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
}
