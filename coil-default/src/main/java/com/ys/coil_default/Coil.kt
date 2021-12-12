@file:JvmName("Coil")
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.ys.coil_default

import com.ys.coil.ImageLoader
import com.ys.coil_default.util.CoilContentProvider

/**
 * 기본 [ImageLoader] 인스턴스를 보유하는 싱글톤입니다.
 */
object Coil {

	private var imageLoader: ImageLoader? = null
	private var imageLoaderFactory: (() -> ImageLoader)? = null

	/**
	 * 기본 [ImageLoader] 인스턴스를 가져옵니다. 아무것도 설정되지 않은 경우 새 인스턴스를 만듭니다.
	 */
	@JvmStatic
	fun loader(): ImageLoader = imageLoader ?: buildDefaultImageLoader()

	/**
	 * 기본 [ImageLoader] 인스턴스를 설정합니다. 현재 인스턴스를 종료합니다.
	 */
	@JvmStatic
	fun setDefaultImageLoader(loader: ImageLoader) {
		imageLoader?.shutdown()
		imageLoader = loader
		imageLoaderFactory = null
	}

	/**
	 * 기본 [ImageLoader] 인스턴스의 factory를 설정합니다. 현재 인스턴스를 종료합니다.
	 *
	 * [factory]는 한 번만 호출되도록 보장됩니다. 이것은 기본 [ImageLoader]의 지연 인스턴스화를 가능하게 합니다.
	 */
	@JvmStatic
	fun setDefaultImageLoader(factory: () -> ImageLoader) {
		imageLoader?.shutdown()
		imageLoaderFactory = factory
		imageLoader = null
	}

	@Synchronized
	private fun buildDefaultImageLoader(): ImageLoader {
		// imageLoader가 방금 설정되었는지 다시 확인하십시오.
		return imageLoader ?: run {
			val loader = imageLoaderFactory?.invoke() ?: ImageLoader(CoilContentProvider.context)
			imageLoaderFactory = null
			setDefaultImageLoader(loader)
			loader
		}
	}
}