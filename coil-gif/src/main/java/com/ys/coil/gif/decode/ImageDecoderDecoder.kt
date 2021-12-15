package com.ys.coil.gif.decode

import android.graphics.ImageDecoder
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DecodeResult
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.decode.Decoder
import com.ys.coil.decode.Options
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import okio.BufferedSource
import java.nio.ByteBuffer

/**
 * [ImageDecoderDecoder]를 사용하는 [Decoder]. Android P 이상에서 GIF 및 애니메이션 WEBP 이미지를 로드하는 데만 사용됩니다.
 */
@RequiresApi(VERSION_CODES.P)
class ImageDecoderDecoder: Decoder {

	override fun handles(source: BufferedSource, mimeType: String?): Boolean {
		return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
	}

	override suspend fun decode(
		pool: BitmapPool,
		source: BufferedSource,
		size: Size,
		options: Options
	): DecodeResult {
		var isSampled = false
		val decoderSource = source.use {
			ImageDecoder.createSource(ByteBuffer.wrap(it.readByteArray()))
		}
		
		val drawable = decoderSource.decodeDrawable { info, _ ->
			// 원본 이미지가 대상보다 큰 경우 대상 크기를 설정합니다.
			if (size is PixelSize && (info.size.width > size.width || info.size.height > size.height)) {
				isSampled = true
				setTargetSize(size.width, size.height)
			}

			if (options.colorSpace != null) {
				setTargetColorSpace(options.colorSpace)
			}

			memorySizePolicy = if (options.allowRgb565) {
				ImageDecoder.MEMORY_POLICY_LOW_RAM
			} else {
				ImageDecoder.MEMORY_POLICY_DEFAULT
			}
		}

		return DecodeResult(
			drawable = drawable,
			isSampled = isSampled
		)
	}
}