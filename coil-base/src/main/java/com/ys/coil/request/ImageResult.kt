package com.ys.coil.request

import android.graphics.drawable.Drawable
import com.ys.coil.decode.DataSource

/**
 * 실행된 [ImageRequest]의 결과를 나타냅니다.
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
sealed class ImageResult {
	abstract val drawable: Drawable?
	abstract val request: ImageRequest
}

class SuccessResult(
	/**
	 * 성공 드로어블.
	 */
	override val drawable: Drawable,

	/**
	 * 이 결과를 생성하기 위해 실행된 요청입니다.
	 */
	override val request: ImageRequest,

	/**
	 * 이미지가 로드된 데이터 소스입니다.
	 */
	val dataSource: DataSource
) : ImageResult()

class ErrorResult(
	/**
	 * 성공 드로어블.
	 */
	override val drawable: Drawable,

	/**
	 * 이 결과를 생성하기 위해 실행된 요청입니다.
	 */
	override val request: ImageRequest,

	/**
	 * 요청에 실패한 오류입니다.
	 */
	val throwable: Throwable
) : ImageResult()