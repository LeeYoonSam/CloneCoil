package com.ys.coil.gif.decode

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build.VERSION
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DecodeResult
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.decode.Decoder
import com.ys.coil.decode.ImageSource
import com.ys.coil.fetch.SourceResult
import com.ys.coil.gif.drawable.ScaleDrawable
import com.ys.coil.gif.request.animatedTransformation
import com.ys.coil.gif.request.animationEndCallback
import com.ys.coil.gif.request.animationStartCallback
import com.ys.coil.gif.request.repeatCount
import com.ys.coil.gif.util.animatable2CallbackOf
import com.ys.coil.gif.util.asPostProcessor
import com.ys.coil.gif.util.isHardware
import com.ys.coil.request.Options
import com.ys.coil.size.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.buffer
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * [ImageDecoder]를 사용하여 GIF, 애니메이션 WebP 및 애니메이션 HEIF를 디코딩하는 [Decoder]입니다.
 *
 * 참고: 애니메이션 HEIF 파일은 API 30 이상에서만 지원됩니다.
 *
 * @param enforceMinimumFrameDelay 참이면 GIF의 프레임 지연이 임계값 미만이면 기본값으로 다시 씁니다. 자세한 내용은 https://github.com/coil-kt/coil/issues/540을 참조하세요.
 */
@RequiresApi(28)
class ImageDecoderDecoder @JvmOverloads constructor(
	private val source: ImageSource,
	private val options: Options,
	private val enforceMinimumFrameDelay: Boolean = true
) : Decoder {

	override suspend fun decode(): DecodeResult? {
		var isSampled = false
		val baseDrawable = runInterruptible {
			val imageSource = if (enforceMinimumFrameDelay && DecodeUtils.isGif(source.source())) {
				ImageSource(FrameDelayRewritingSource(source.source()).buffer(), options.context)
			} else {
				source
			}
			imageSource.use {
				val file = imageSource.fileOrNull()
				val decoderSource = when {
					file != null -> ImageDecoder.createSource(file)
					// https://issuetracker.google.com/issues/139371066
					VERSION.SDK_INT < 30 -> ImageDecoder.createSource(imageSource.file())
					else -> ImageDecoder.createSource(ByteBuffer.wrap(imageSource.source().readByteArray()))
				}

				decoderSource.decodeDrawable { info, _ ->
					val size = options.size
					if (size is PixelSize) {
						val (srcWidth, srcHeight) = info.size
						val multiplier = DecodeUtils.computeSizeMultiplier(
							srcWidth = srcWidth,
							srcHeight = srcHeight,
							dstWidth = size.width,
							dstHeight = size.height,
							scale = options.scale
						)

						// 이미지가 요청된 치수보다 크거나 요청에 정확한 치수가 필요한 경우 대상 크기를 설정하십시오.
						isSampled = multiplier < 1
						if (isSampled || !options.allowInexactSize) {
							val targetWidth = (multiplier * srcWidth).roundToInt()
							val targetHeight = (multiplier * srcHeight).roundToInt()
							setTargetSize(targetWidth, targetHeight)
						}
					}

					allocator = if (options.config.isHardware) {
						ImageDecoder.ALLOCATOR_HARDWARE
					} else {
						ImageDecoder.ALLOCATOR_SOFTWARE
					}

					memorySizePolicy = if (options.allowRgb565) {
						ImageDecoder.MEMORY_POLICY_LOW_RAM
					} else {
						ImageDecoder.MEMORY_POLICY_DEFAULT
					}

					if (options.colorSpace != null) {
						setTargetColorSpace(options.colorSpace)
					}

					isUnpremultipliedRequired = !options.premultipliedAlpha

					postProcessor = options.parameters.animatedTransformation()?.asPostProcessor()
				}
			}
		}

		val drawable = if (baseDrawable is AnimatedImageDrawable) {
			baseDrawable.repeatCount = options.parameters.repeatCount() ?: AnimatedImageDrawable.REPEAT_INFINITE

			// 요청을 통해 제공되는 경우 애니메이션 시작 및 종료 콜백을 설정합니다.
			val onStart = options.parameters.animationStartCallback()
			val onEnd = options.parameters.animationEndCallback()
			if (onStart != null || onEnd != null) {
				// 애니메이션 콜백은 메인 스레드에서 설정해야 합니다.
				withContext(Dispatchers.Main.immediate) {
					baseDrawable.registerAnimationCallback(animatable2CallbackOf(onStart, onEnd))
				}
			}

			// AnimatedImageDrawable을 ScaleDrawable에 넘치도록 겁을 들이고 있습니다.
			ScaleDrawable(baseDrawable, options.scale)
		} else {
			baseDrawable
		}

		return DecodeResult(
			drawable = drawable,
			isSampled = isSampled
		)
	}

	@RequiresApi(28)
	class Factory @JvmOverloads constructor(
		private val enforceMinimumFrameDelay: Boolean = true
	) : Decoder.Factory {

		override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
			if (!isApplicable(result.source.source())) return null
			return ImageDecoderDecoder(result.source, options, enforceMinimumFrameDelay)
		}

		private fun isApplicable(source: BufferedSource): Boolean {
			return DecodeUtils.isGif(source) ||
				DecodeUtils.isAnimatedWebP(source) ||
				(VERSION.SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}
}
