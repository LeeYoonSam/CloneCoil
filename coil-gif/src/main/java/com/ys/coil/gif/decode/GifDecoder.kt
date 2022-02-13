package com.ys.coil.gif.decode

import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.Movie
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DecodeResult
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.decode.Decoder
import com.ys.coil.decode.ImageSource
import com.ys.coil.fetch.SourceResult
import com.ys.coil.gif.drawable.MovieDrawable
import com.ys.coil.gif.request.animatedTransformation
import com.ys.coil.gif.request.animationEndCallback
import com.ys.coil.gif.request.animationStartCallback
import com.ys.coil.gif.request.repeatCount
import com.ys.coil.gif.util.animatable2CompatCallbackOf
import com.ys.coil.gif.util.isHardware
import com.ys.coil.request.Options
import kotlinx.coroutines.runInterruptible
import okio.buffer

/**
 * [Movie]을 사용하여 GIF를 디코딩하는 [Decoder]입니다.
 *
 * 참고: API 28 이상에서는 [ImageDecoderDecoder]를 사용하는 것을 선호합니다.
 *
 * @param enforceMinimumFrameDelay 참이면 GIF의 프레임 지연이 임계값 미만이면 기본값으로 다시 씁니다.
 * 자세한 내용은 https://github.com/coil-kt/coil/issues/540을 참조하세요
 */
class GifDecoder @JvmOverloads constructor(
	private val source: ImageSource,
	private val options: Options,
	private val enforceMinimumFrameDelay: Boolean = true
) : Decoder {

	override suspend fun decode() = runInterruptible {
		val bufferedSource = if (enforceMinimumFrameDelay) {
			FrameDelayRewritingSource(source.source()).buffer()
		} else {
			source.source()
		}
		val movie: Movie? = bufferedSource.use { Movie.decodeStream(it.inputStream()) }

		check(movie != null && movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

		val drawable = MovieDrawable(
			movie = movie,
			config = when {
				movie.isOpaque && options.allowRgb565 -> RGB_565
				options.config.isHardware -> ARGB_8888
				else -> options.config
			},
			scale = options.scale
		)

		drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

		// 요청을 통해 제공되는 경우 애니메이션 시작 및 종료 콜백을 설정합니다.
		val onStart = options.parameters.animationStartCallback()
		val onEnd = options.parameters.animationEndCallback()
		if (onStart != null || onEnd != null) {
			drawable.registerAnimationCallback(animatable2CompatCallbackOf(onStart, onEnd))
		}

		// 각 프레임에 적용할 애니메이션 변환을 설정합니다.
		drawable.setAnimatedTransformation(options.parameters.animatedTransformation())

		DecodeResult(
			drawable = drawable,
			isSampled = false
		)
	}

	class Factory @JvmOverloads constructor(
		private val enforceMinimumFrameDelay: Boolean = true
	) : Decoder.Factory {

		override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
			if (!DecodeUtils.isGif(result.source.source())) return null
			return GifDecoder(result.source, options, enforceMinimumFrameDelay)
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}

	companion object {
		const val REPEAT_COUNT_KEY = "coil#repeat_count"
		const val ANIMATED_TRANSFORMATION_KEY = "coil#animated_transformation"
		const val ANIMATION_START_CALLBACK_KEY = "coil#animation_start_callback"
		const val ANIMATION_END_CALLBACK_KEY = "coil#animation_end_callback"
	}
}
