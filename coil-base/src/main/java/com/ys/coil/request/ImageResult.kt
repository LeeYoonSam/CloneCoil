package com.ys.coil.request

import android.graphics.drawable.Drawable
import com.ys.coil.decode.DataSource
import com.ys.coil.memory.MemoryCache

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
	val dataSource: DataSource,

	/**
	 * 메모리 캐시에 있는 이미지의 캐시 키입니다.
	 * 이미지가 메모리 캐시에 기록되지 않은 경우 'null'입니다.
	 */
	val memoryCacheKey: MemoryCache.Key?,

	/**
	 * 디스크 캐시에 있는 이미지의 캐시 키입니다.
	 * 이미지가 디스크 캐시에 기록되지 않은 경우 'null'입니다.
	 */
	val diskCacheKey: String?,

	/**
	 * 이미지가 샘플링되면 'true'입니다(즉, 원래 크기보다 작게 메모리에 로드됨).
	 */
	val isSampled: Boolean,

	/**
	 * [ImageRequest.placeholderMemoryCacheKey]가 메모리 캐시에 있으면 'true'입니다.
	 */
	val isPlaceholderCached: Boolean
) : ImageResult() {

	fun copy(
		drawable: Drawable = this.drawable,
		request: ImageRequest = this.request,
		dataSource: DataSource = this.dataSource,
		memoryCacheKey: MemoryCache.Key? = this.memoryCacheKey,
		diskCacheKey: String? = this.diskCacheKey,
		isSampled: Boolean = this.isSampled,
		isPlaceholderCached: Boolean = this.isPlaceholderCached
	) = SuccessResult(
		drawable = drawable,
		request = request,
		dataSource = dataSource,
		memoryCacheKey = memoryCacheKey,
		diskCacheKey = diskCacheKey,
		isSampled = isSampled,
		isPlaceholderCached = isPlaceholderCached,
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return other is SuccessResult &&
			drawable == other.drawable &&
			request == other.request &&
			dataSource == other.dataSource &&
			memoryCacheKey == other.memoryCacheKey &&
			diskCacheKey == other.diskCacheKey &&
			isSampled == other.isSampled &&
			isPlaceholderCached == other.isPlaceholderCached
	}

	override fun hashCode(): Int {
		var result = drawable.hashCode()
		result = 31 * result + request.hashCode()
		result = 31 * result + dataSource.hashCode()
		result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
		result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
		result = 31 * result + isSampled.hashCode()
		result = 31 * result + isPlaceholderCached.hashCode()
		return result
	}
}

class ErrorResult(
	/**
	 * 에러 드로어블.
	 */
	override val drawable: Drawable?,

	/**
	 * 이 결과를 생성하기 위해 실행된 요청입니다.
	 */
	override val request: ImageRequest,

	/**
	 * 요청에 실패한 오류입니다.
	 */
	val throwable: Throwable
) : ImageResult() {

	fun copy(
		drawable: Drawable? = this.drawable,
		request: ImageRequest = this.request,
		throwable: Throwable = this.throwable
	) = ErrorResult(
		drawable = drawable,
		request = request,
		throwable = throwable
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return other is ErrorResult &&
			drawable == other.drawable &&
			request == other.request &&
			throwable == other.throwable
	}

	override fun hashCode(): Int {
		var result = drawable?.hashCode() ?: 0
		result = 31 * result + request.hashCode()
		result = 31 * result + throwable.hashCode()
		return result
	}
}