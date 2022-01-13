package com.ys.coil.request

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.ys.coil.ImageLoader
import com.ys.coil.size.Size
import com.ys.coil.target.ViewTarget
import com.ys.coil.util.HardwareBitmapService
import com.ys.coil.util.Logger
import com.ys.coil.util.SystemCallbacks
import com.ys.coil.util.VALID_TRANSFORMATION_CONFIGS
import com.ys.coil.util.allowInexactSize
import com.ys.coil.util.isHardware
import kotlinx.coroutines.Job

/** [ImageRequest]에서 작동하는 작업을 처리합니다. */
internal class RequestService(
	private val imageLoader: ImageLoader,
	private val systemCallbacks: SystemCallbacks,
	logger: Logger?
) {

	private val hardwareBitmapService = HardwareBitmapService(logger)

	/**
	 * 수명 주기에 따라 [ImageRequest]를 자동으로 폐기 및/또는 다시 시작하려면 [initialRequest]를 래핑합니다.
	 */
	fun requestDelegate(initialRequest: ImageRequest, job: Job): RequestDelegate{
		val lifecycle = initialRequest.lifecycle
		return when (val target = initialRequest.target) {
			is ViewTarget<*> ->
				ViewTargetRequestDelegate(imageLoader, initialRequest, target, lifecycle, job)
			else -> BaseRequestDelegate(lifecycle, job)
		}
	}

	fun errorResult(request: ImageRequest, throwable: Throwable): ErrorResult {
		return ErrorResult(
			drawable = if (throwable is NullRequestDataException) {
				request.fallback ?: request.error
			} else {
				request.error
			},
			request = request,
			throwable = throwable
		)
	}

	/**
	 * 요청 옵션을 반환합니다. 이 함수는 메인 스레드에서 호출되며 빨라야 합니다.
	 */
	fun options(request: ImageRequest, size: Size): Options {
		// 요청된 비트맵 구성이 검사를 통과하지 못하면 ARGB_8888로 폴백합니다.
		val isValidConfig = isConfigValidForTransformations(request) &&
			isConfigValidForHardwareAllocation(request, size)
		val config = if (isValidConfig) request.bitmapConfig else Bitmap.Config.ARGB_8888

		// 오프라인 상태임을 알면 네트워크에서 가져오기를 비활성화합니다.
		val networkCachePolicy = if (systemCallbacks.isOnline) {
			request.networkCachePolicy
		} else {
			CachePolicy.DISABLED
		}

		// 변환이 있거나 요청된 구성이 ALPHA_8이면 allowRgb565를 비활성화합니다.
		// ALPHA_8은 각 픽셀이 1바이트인 마스크 구성이므로 이 경우 최적화로 RGB_565를 사용하는 것은 의미가 없습니다.
		val allowRgb565 = request.allowRgb565 && request.transformations.isEmpty() &&
			config != Bitmap.Config.ALPHA_8

		return Options(
			context = request.context,
			config = config,
			colorSpace = request.colorSpace,
			size = size,
			scale = request.scale,
			allowInexactSize = request.allowInexactSize,
			allowRgb565 = allowRgb565,
			premultipliedAlpha = request.premultipliedAlpha,
			diskCacheKey = request.diskCacheKey,
			headers = request.headers,
			parameters = request.parameters,
			memoryCachePolicy = request.memoryCachePolicy,
			diskCachePolicy = request.diskCachePolicy,
			networkCachePolicy = networkCachePolicy
		)
	}

	/**
	 * [requestedConfig]가 [request]에 대해 유효한(즉, [Target]으로 반환될 수 있음) 구성이면 'true'를 반환합니다.
	 */
	fun isConfigValidForHardware(request: ImageRequest, requestedConfig: Bitmap.Config): Boolean {
		// 요청된 비트맵 구성이 소프트웨어인 경우 단락.
		if (!requestedConfig.isHardware) return true

		// 요청이 하드웨어 비트맵을 허용하는지 확인합니다.
		if (!request.allowHardware) return false

		// 비 하드웨어 가속 대상에 대한 하드웨어 비트맵을 방지합니다.
		val target = request.target
		if (target is ViewTarget<*> &&
			target.view.run { isAttachedToWindow && !isHardwareAccelerated }) return false

		return true
	}

	/** 하드웨어 비트맵을 할당할 수 있으면 'true'를 반환합니다. */
	@WorkerThread
	fun allowHardwareWorkerThread(options: Options): Boolean {
		return !options.config.isHardware || hardwareBitmapService.allowHardwareWorkerThread()
	}

	/**
	 * [request]의 요청된 비트맵 구성이 유효한 경우 'true'를 반환합니다(즉, [Target]으로 반환될 수 있음).
	 *
	 * 이 검사는 새 하드웨어 비트맵을 할당할 수 있는지도 검사한다는 점을 제외하고는 [isConfigValidForHardware]와 유사합니다.
	 */
	private fun isConfigValidForHardwareAllocation(request: ImageRequest, size: Size): Boolean {
		return isConfigValidForHardware(request, request.bitmapConfig) &&
			hardwareBitmapService.allowHardwareMainThread(size)
	}

	/** [ImageRequest.bitmapConfig]가 [Transformation]에 대해 유효한 경우 'true'를 반환합니다. */
	private fun isConfigValidForTransformations(request: ImageRequest): Boolean {
		return request.transformations.isEmpty() ||
			request.bitmapConfig in VALID_TRANSFORMATION_CONFIGS
	}
}