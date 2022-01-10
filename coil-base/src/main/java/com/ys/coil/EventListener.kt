package com.ys.coil

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.ys.coil.EventListener.Factory
import com.ys.coil.decode.DecodeResult
import com.ys.coil.fetch.FetchResult
import com.ys.coil.fetch.Fetcher
import com.ys.coil.request.ErrorResult
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.Options
import com.ys.coil.request.SuccessResult
import com.ys.coil.size.Size
import com.ys.coil.transition.Transition

/**
 * 이미지 요청의 진행 상황을 추적하기 위한 리스너.
 * 이 클래스는 분석, 성능 또는 기타 메트릭 추적을 측정하는 데 유용합니다.
 *
 * @see ImageLoader.Builder.eventListenerFactory
 */
interface EventListener : ImageRequest.Listener {

	/**
	 * @see ImageRequest.Listener.onStart
	 */
	@MainThread
	override fun onStart(request: ImageRequest) {}

	/**
	 * [SizeResolver.size] 전에 호출됩니다.
	 */
	@MainThread
	fun resolveSizeStart(request: ImageRequest) {}

	/**
	 * [SizeResolver.size] 이후에 호출됩니다.
	 *
	 * @param size 이 요청에 대해 해결된 [Size]입니다.
	 */
	@MainThread
	fun resolveSizeEnd(request: ImageRequest, size: Size) {}

	/**
	 * [Mapper.map] 전에 호출됩니다.
	 *
	 * @param input 변환될 데이터입니다.
	 */
	@MainThread
	fun mapStart(request: ImageRequest, input: Any) {}

	/**
	 * [Mapper.map] 이후에 호출됩니다.
	 *
	 * @param output 변환된 후의 데이터입니다. 적용 가능한 매퍼가 없는 경우 [output]은 [ImageRequest.data]와 동일합니다.
	 */
	@MainThread
	fun mapEnd(request: ImageRequest, output: Any) {}

	/**
	 * [Keyer.key] 전에 호출됩니다.
	 *
	 * @param input 변환될 데이터입니다.
	 */
	@MainThread
	fun keyStart(request: ImageRequest, input: Any) {}

	/**
	 * [Keyer.key] 이후에 호출됩니다.
	 *
	 * @param output 문자열 키로 변환된 후의 데이터입니다.
	 * [output]이 'null'이면 메모리 캐시에 캐시되지 않습니다.
	 */
	@MainThread
	fun keyEnd(request: ImageRequest, output: Any) {}

	/**
	 * [Fetcher.fetch] 전에 호출됩니다.
	 *
	 * @param fetcher 요청을 처리하는 데 사용할 [Fetcher]입니다.
	 * @param options [Fetcher.fetch]에 전달할 [Options]입니다.
	 */
	@WorkerThread
	fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) {}

	/**
	 * [Fetcher.fetch] 이후에 호출됩니다.
	 *
	 * @param fetcher 요청을 처리하는 데 사용된 [Fetcher]입니다.
	 * @param options [Fetcher.fetch]에 전달된 [Options]입니다.
	 * @param result [Fetcher.fetch] 결과.
	 */
	@WorkerThread
	fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?) {}

	/**
	 * [Decoder.decode] 전에 호출됩니다.
	 *
	 * [Fetcher.fetch]가 [SourceResult]를 반환하지 않으면 건너뜁니다.
	 *
	 * @param decoder 요청을 처리하는 데 사용할 [Decoder]입니다.
	 * @param options [Decoder.decode]에 전달할 [Options]입니다.
	 */
	@WorkerThread
	fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {}

	/**
	 * [Decoder.decode] 이후에 호출됩니다.
	 *
	 * [Fetcher.fetch]가 [SourceResult]를 반환하지 않으면 건너뜁니다.
	 *
	 * @param decoder 요청을 처리하는 데 사용된 [Decoder].
	 * @param options [Decoder.decode]에 전달된 [Options]입니다.
	 * @param result [Decoder.decode]의 결과입니다.
	 */
	@WorkerThread
	fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?) {}

	/**
	 * [Transformation]이 적용되기 전에 호출됩니다.
	 *
	 * [ImageRequest.transformations]가 비어 있으면 건너뜁니다.
	 *
	 * @param input 변환될 [Bitmap]입니다.
	 */
	@WorkerThread
	fun transformStart(request: ImageRequest, input: Bitmap) {}

	/**
	 * [Transformation]이 적용된 후 호출됩니다.
	 *
	 * [ImageRequest.transformations]가 비어 있으면 건너뜁니다.
	 *
	 * @param output 변환된 [Bitmap]입니다.
	 */
	@WorkerThread
	fun transformEnd(request: ImageRequest, output: Bitmap) {}

	/**
	 * [Transition.transition] 전에 호출됩니다.
	 *
	 * [transition]이 [NoneTransition]인 경우 건너뜁니다.
	 * 또는 [ImageRequest.target]은 [TransitionTarget]을 구현하지 않습니다.
	 */
	@MainThread
	fun transitionStart(request: ImageRequest, transient: Transition) {}

	/**
	 * [Transition.transition] 이후에 호출됩니다.
	 *
	 * [transition]이 [NoneTransition]인 경우 건너뜁니다.
	 * 또는 [ImageRequest.target]은 [TransitionTarget]을 구현하지 않습니다.
	 */
	@MainThread
	fun transitionEnd(request: ImageRequest, transient: Transition) {}

	/**
	 * @see ImageRequest.Listener.onCancel
	 */
	@MainThread
	override fun onCancel(request: ImageRequest) {}

	/**
	 * @see ImageRequest.Listener.onError
	 */
	@MainThread
	override fun onError(request: ImageRequest, result: ErrorResult) {}

	/**
	 * @see ImageRequest.Listener.onSuccess
	 */
	@MainThread
	override fun onSuccess(request: ImageRequest, result: SuccessResult) {}

	fun interface Factory {

		fun create(request: ImageRequest): EventListener

		companion object {
			@JvmField val NONE = Factory { EventListener.NONE }
		}
	}

	companion object {
		@JvmField val NONE = object : EventListener {}
	}
}