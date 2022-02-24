package com.ys.coil.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.drawablepainter.DrawablePainter
import com.ys.coil.ImageLoader
import com.ys.coil.compose.ImagePainter.ExecuteCallback
import com.ys.coil.compose.ImagePainter.State
import com.ys.coil.compose.ImagePainter.State.Empty
import com.ys.coil.decode.DataSource
import com.ys.coil.request.ErrorResult
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.request.SuccessResult
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.Precision
import com.ys.coil.size.Scale
import com.ys.coil.transition.CrossfadeTransition
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * [imageLoader]를 사용하여 [ImageRequest]를 실행할 [ImagePainter]를 반환합니다.
 *
 * @param data 로드할 [ImageRequest.data]입니다.
 * @param imageLoader 요청을 실행하는 데 사용할 [ImageLoader]입니다.
 * @param onExecute [ImagePainter]가 이미지 요청을 시작하기 직전에 호출됩니다.
 * 요청을 계속하려면 'true'를 반환하십시오. 요청 실행을 건너뛰려면 '거짓'을 반환합니다.
 * @param builder 요청을 구성하기 위한 선택적 람다.
 */
@Composable
inline fun rememberImagePainter(
	data: Any?,
	imageLoader: ImageLoader,
	onExecute: ExecuteCallback = ExecuteCallback.Lazy,
	builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter {
	val request = ImageRequest.Builder(LocalContext.current)
		.apply(builder)
		.data(data)
		.build()
	return rememberImagePainter(request, imageLoader, onExecute)
}

/**
 * [imageLoader]를 사용하여 [request]을 실행할 [ImagePainter]를 반환합니다.
 *
 * @param request 실행할 [ImageRequest]입니다.
 * @param imageLoader [request]을 실행하는 데 사용할 [ImageLoader]입니다.
 * @param onExecute [ImagePainter]가 이미지 요청을 시작하기 직전에 호출됩니다.
 * 요청을 계속하려면 'true'를 반환하십시오. 요청 실행을 건너뛰려면 '거짓'을 반환합니다.
 */
@Composable
fun rememberImagePainter(
	request: ImageRequest,
	imageLoader: ImageLoader,
	onExecute: ExecuteCallback = ExecuteCallback.Lazy,
): ImagePainter {
	requireSupportedData(request.data)
	require(request.target == null) { "request.target must be null." }

	val scope = rememberCoroutineScope { Dispatchers.Main.immediate + EMPTY_COROUTINE_EXCEPTION_HANDLER }
	val imagePainter = remember(scope) { ImagePainter(scope, request, imageLoader) }
	imagePainter.request = request
	imagePainter.imageLoader = imageLoader
	imagePainter.onExecute = onExecute
	imagePainter.isPreview = LocalInspectionMode.current
	updatePainter(imagePainter, request, imageLoader)
	return imagePainter
}

/**
 * [ImageRequest]를 비동기적으로 실행하고 결과를 그리는 [Painter].
 * 인스턴스는 [rememberImagePainter]로만 생성할 수 있습니다.
 */
@Stable
class ImagePainter internal constructor(
	private val parentScope: CoroutineScope,
	request: ImageRequest,
	imageLoader: ImageLoader
) : Painter(), RememberObserver {

	private var rememberScope: CoroutineScope? = null
	private var requestJob: Job? = null
	private var drawSize: Size by mutableStateOf(Size.Zero)

	private var alpha: Float by mutableStateOf(1f)
	private var colorFilter: ColorFilter? by mutableStateOf(null)

	internal var painter: Painter? by mutableStateOf(null)
	internal var onExecute = ExecuteCallback.Lazy
	internal var isPreview = false

	/** The current [ImagePainter.State]. */
	var state: State by mutableStateOf(Empty)
		private set

	/** The current [ImageRequest]. */
	var request: ImageRequest by mutableStateOf(request)
		internal set

	/** The current [ImageLoader]. */
	var imageLoader: ImageLoader by mutableStateOf(imageLoader)
		internal set

	override val intrinsicSize: Size
		get() = painter?.intrinsicSize ?: Size.Unspecified

	override fun DrawScope.onDraw() {
		// 그리기 범위의 현재 크기를 업데이트합니다.
		drawSize = size

		// 현재 painter를 그립니다.
		painter?.apply { draw(size, alpha, colorFilter) }
	}

	override fun applyAlpha(alpha: Float): Boolean {
		this.alpha = alpha
		return true
	}

	override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
		this.colorFilter = colorFilter
		return true
	}

	override fun onRemembered() {
		if (isPreview) return

		// 상태를 관찰하고 기억하는 동안 요청을 실행하는 새 범위를 만듭니다.
		rememberScope?.cancel()
		val context = parentScope.coroutineContext
		val scope = CoroutineScope(context + SupervisorJob(context.job))
		rememberScope = scope

		// 현재 요청 + 요청 크기를 관찰하고 필요에 따라 새 요청을 시작합니다.
		scope.launch {
			var snapshot: Snapshot? = null
			combine(
				snapshotFlow { request },
				snapshotFlow { drawSize },
				transform = ::Pair
			).collect { (request, size) ->
				val previous = snapshot
				val current = Snapshot(state, request, size)
				snapshot = current

				// 필요한 경우 새 이미지 요청을 시작합니다.
				if (onExecute(previous, current)) {
					requestJob?.cancel()
					requestJob = launch {
						state = imageLoader
							.execute(updateRequest(current.request, current.size))
							.toState()
					}
				}
			}
		}
	}

	override fun onForgotten() {
		rememberScope?.cancel()
		rememberScope = null
		requestJob?.cancel()
		requestJob = null
	}

	override fun onAbandoned() = onForgotten()

	/** [ImagePainter]와 함께 작동하도록 [request]을 업데이트합니다. */
	private fun updateRequest(request: ImageRequest, size: Size): ImageRequest {
		return request.newBuilder()
			.target(
				onStart = { placeholder ->
					state = State.Loading(painter = placeholder?.toPainter())
				}
			)
			.apply {
				// 명시적으로 설정되지 않은 경우 크기를 설정합니다.
				if (request.defined.sizeResolver == null) {
					if (size.isSpecified) {
						val (width, height) = size
						if (width >= 0.5f && height >= 0.5f) {
							size(size.width.roundToInt(), size.height.roundToInt())
						} else {
							size(OriginalSize)
						}
					} else {
						size(OriginalSize)
					}
				}

				// 명시적으로 설정되지 않은 경우 채우기로 스케일을 설정합니다.
				// `ImageView`와 같이 스케일 유형을 자동 감지할 수 없기 때문에 이 작업을 수행합니다.
				if (request.defined.scale == null) {
					scale(Scale.FILL)
				}

				// 정확한 정밀도가 명시적으로 설정되지 않은 경우 정확하지 않은 정밀도를 설정합니다.
				if (request.defined.precision != Precision.EXACT) {
					precision(Precision.INEXACT)
				}
			}
			.build()
	}


	/**
	 * [ImagePainter]가 새 이미지 요청을 실행하기 직전에 호출됩니다.
	 * 요청을 계속하려면 'true'를 반환하십시오. 요청 실행을 건너뛰려면 '거짓'을 반환합니다.
	 */
	fun interface ExecuteCallback {

		operator fun invoke(previous: Snapshot?, current: Snapshot): Boolean

		companion object {

			/**
			 * painter가 비어있거나 요청이 변경된 경우 요청을 진행합니다.
			 *
			 * 또한 이 콜백은 이미지 요청에 명시적 크기가 있거나 그리기 캔버스의 크기로 [ImagePainter.onDraw]가 호출된 경우에만 진행됩니다.
			 */
			@JvmField val Lazy = ExecuteCallback { previous, current ->
				(current.state == State.Empty || previous?.request != current.request) &&
					(current.request.defined.sizeResolver != null ||
						current.size.isUnspecified ||
						(current.size.width >= 0.5f && current.size.height >= 0.5f))
			}

			/** painter가 비어있거나 요청이 변경된 경우 요청을 진행합니다.
			 *
			 * [Lazy]와 달리 이 콜백은 요청을 즉시 실행합니다. 일반적으로,
			 * [ImageRequest.Builder.size]가 설정되어 있지 않으면 이미지를 원래 크기로 로드합니다.
			 */
			@JvmField val immediate = ExecuteCallback { previous, current ->
				current.state == State.Empty || previous?.request != current.request
			}

			@Deprecated(
				message = "Migrate to `Lazy`.",
				replaceWith = ReplaceWith(
					expression = "ExecuteCallback.Lazy",
					imports = ["coil.compose.ImagePainter.ExecuteCallback"]
				),
				level = DeprecationLevel.ERROR // Temporary migration aid.
			)

			@JvmField val Default = Lazy
		}
	}

	/**
	 * [ImagePainter] 속성의 스냅샷.
	 */
	data class Snapshot(
		val state: State,
		val request: ImageRequest,
		val size: Size
	)

	/**
	 * [ImagePainter]의 현재 상태입니다.
	 */
	sealed class State {
		/** [ImagePainter]가 그리는 현재 painter. */
		abstract val painter: Painter?

		/** 요청이 시작되지 않았습니다. */
		object Empty : State() {
			override val painter: Painter? get() = null
		}

		/** 요청이 진행 중입니다. */
		data class Loading(
			override val painter: Painter?
		) : State()

		/** 요청이 성공했습니다. */
		data class Success(
			override val painter: Painter,
			val result: SuccessResult
		) : State()

		/** [ErrorResult.throwable]로 인해 요청이 실패했습니다. */
		data class Error(
			override val painter: Painter?,
			val result: ErrorResult
		) : State()
	}
}

/**
 * 현재 [ImagePainter.painter]를 관찰할 수 있습니다.
 * 이 기능을 사용하면 [ImagePainter.state]가 변경될 때만 이 기능을 다시 시작하면 되도록 필요한 재구성의 양을 최소화할 수 있습니다.
 */
@Composable
private fun updatePainter(
	imagePainter: ImagePainter,
	request: ImageRequest,
	imageLoader: ImageLoader
) {
	// 검사 모드(미리 보기)에 있고 자리 표시자가 있는 경우 이미지 요청을 실행하지 않고 그냥 그립니다.
	if (imagePainter.isPreview) {
		imagePainter.painter = request.placeholder?.toPainter()
		return
	}

	// 이것은 쓸모없는 remember 처럼 보일 수 있지만 이것은 모든 Painter 인스턴스가 remember 이벤트를 수신할 수 있도록 합니다(RememberObserver를 구현하는 경우).
	// 제거하지 마십시오.
	val state = imagePainter.state
	val painter = remember(state) { state.painter }

	// 크로스페이드 전환이 설정되지 않은 경우 단락.
	// 요청이 실행될 때까지 기본값이 설정되지 않으므로 `imageLoader.defaults.transitionFactory`를 구체적으로 확인합니다.
	val transition = request.defined.transitionFactory ?: imageLoader.defaults.transitionFactory
	if (transition !is CrossfadeTransition.Factory) {
		imagePainter.painter = painter
		return
	}

	// 가장 최근의 로딩 페인터를 추적하여 크로스페이드하십시오.
	val loading = remember(request) { ValueHolder<Painter?>(null) }
	if (state is State.Loading) loading.value = state.painter

	// 요청이 성공하지 못했거나 메모리 캐시에서 반환된 경우 단락입니다.
	if (state !is State.Success || state.result.dataSource == DataSource.MEMORY_CACHE) {
		imagePainter.painter = painter
		return
	}

	// 크로스페이드 페인터를 설정합니다.
	imagePainter.painter = rememberCrossfadePainter(
		key = state,
		start = loading.value,
		end = painter,
		scale = request.scale,
		durationMillis = transition.durationMillis,
		fadeStart = !state.result.isPlaceholderCached
	)
}

private fun requireSupportedData(data: Any?) = when (data) {
	is ImageBitmap -> unsupportedData("ImageBitmap")
	is ImageVector -> unsupportedData("ImageVector")
	is Painter -> unsupportedData("Painter")
	else -> data
}

private fun unsupportedData(name: String): Nothing {
	throw IllegalArgumentException(
		"Unsupported type: $name. If you wish to display this $name, " +
			"use androidx.compose.foundation.Image."
	)
}

private fun ImageResult.toState() = when (this) {
	is SuccessResult -> State.Success(
		painter = drawable.toPainter(),
		result = this
	)
	is ErrorResult -> State.Error(
		painter = drawable?.toPainter(),
		result = this
	)
}

/** 가능하면 Compose 프리미티브를 사용하여 이 [Drawable]을 [Painter]로 변환하십시오. */
private fun Drawable.toPainter(): Painter {
	return when (this) {
		is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
		is ColorDrawable -> ColorPainter(Color(color))
		else -> DrawablePainter(mutate())
	}
}

/** 재구성을 피하는 단순한 가변 값 보유자. */
private class ValueHolder<T>(@JvmField var value: T)

/** 잡히지 않은 예외를 무시하는 예외 처리기. */
private val EMPTY_COROUTINE_EXCEPTION_HANDLER = CoroutineExceptionHandler { _, _ -> }
