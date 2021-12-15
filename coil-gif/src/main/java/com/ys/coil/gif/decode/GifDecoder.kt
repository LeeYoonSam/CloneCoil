package com.ys.coil.gif.decode

import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.HARDWARE
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.Movie
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.decode.DecodeResult
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.decode.Decoder
import com.ys.coil.decode.Options
import com.ys.coil.gif.drawable.MovieDrawable
import com.ys.coil.size.Size
import okio.BufferedSource

/**
 * [Movie]을 사용하여 GIF를 로드하는 [Decoder]입니다.
 *
 * 참고: Android P 이상에서는 [ImageDecoderDecoder]를 사용하는 것을 선호합니다.
 */
class GifDecoder : Decoder {
	override fun handles(source: BufferedSource, mimeType: String?): Boolean {
		return DecodeUtils.isGif(source)
	}

	override suspend fun decode(
		pool: BitmapPool,
		source: BufferedSource,
		size: Size,
		options: Options
	): DecodeResult {
		return DecodeResult(
			drawable = MovieDrawable(
				movie = source.use { checkNotNull(Movie.decodeStream(it.inputStream())) },
				config = when {
					options.allowRgb565 -> RGB_565
					VERSION.SDK_INT >= VERSION_CODES.O && options.config == HARDWARE -> ARGB_8888
					else -> options.config
				},
				scale = options.scale,
				pool = pool
			),
			isSampled = false
		)
	}
}