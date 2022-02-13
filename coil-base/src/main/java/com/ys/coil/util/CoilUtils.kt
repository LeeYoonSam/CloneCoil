package com.ys.coil.util

import android.content.Context
import android.view.View
import com.ys.coil.request.ImageResult
import okhttp3.Cache

/** 코일의 공공 유틸리티 메소드. */
object CoilUtils {

	/**
	 * 이 보기에 첨부된 요청을 삭제합니다(있는 경우).
	 *
	 * 참고: 일반적으로 요청을 취소하고 리소스를 지우려면 [Disposable.dispose]를 사용해야 하지만 이 방법은 편의를 위해 제공됩니다.
	 *
	 * @see Disposable.dispose
	 */
	@JvmStatic
	fun dispose(view: View) {
		view.requestManager.dispose()
	}

	/**
	 * 이 보기에 첨부된 가장 최근에 실행된 이미지 요청의 [ImageResult]를 가져옵니다.
	 */
	fun result(view: View): ImageResult? {
		return view.requestManager.getResult()
	}

	@Deprecated(
		message = "ImageLoaders no longer (and should not) use OkHttp's disk cache. " +
			"Use 'ImageLoader.Builder.diskCache' to configure a custom disk cache.",
		level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("unsupported()") // Temporary migration aid.
	)
	@JvmStatic
	fun createDefaultCache(context: Context): Cache = unsupported()
}
