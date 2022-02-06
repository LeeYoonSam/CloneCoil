package com.ys.coil

import android.app.Application

/**
 * 새로운 [ImageLoader] 인스턴스를 생성하는 팩토리.
 *
 * 싱글톤 [ImageLoader] 생성 방법을 구성하려면 **둘 중 하나**:
 * - [Application]에서 [ImageLoaderFactory]를 구현합니다.
 * - **또는** [ImageLoaderFactory]로 [Coil.setImageLoader]를 호출합니다.
 */
interface ImageLoaderFactory {

	/**
	 * 새 [ImageLoader]를 반환합니다.
	 */
	fun newImageLoader(): ImageLoader
}
