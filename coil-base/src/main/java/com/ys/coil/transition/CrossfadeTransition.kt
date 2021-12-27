package com.ys.coil.transition

import android.widget.ImageView
import com.ys.coil.decode.DataSource
import com.ys.coil.drawable.CrossfadeDrawable
import com.ys.coil.request.ErrorResult
import com.ys.coil.request.ImageResult
import com.ys.coil.request.SuccessResult
import com.ys.coil.size.Scale
import com.ys.coil.util.scale

/**
 * 현재 드로어블에서 새 드로어블로 크로스페이드하는 [전환]입니다.
 *
 * @param durationMillis 애니메이션의 지속 시간(밀리초)입니다.
 * @param preferExactIntrinsicSize [CrossfadeDrawable.preferExactIntrinsicSize]를 참조하십시오.
 */
class CrossfadeTransition @JvmOverloads constructor(
	private val target: TransitionTarget,
	private val result: ImageResult,
	val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION,
	val preferExactIntrinsicSize: Boolean = false
) : Transition {

	init {
		require(durationMillis > 0) { "durationMillis must be > 0." }
	}

	override fun transition() {
		val drawable = CrossfadeDrawable(
			start = target.drawable,
			end = result.drawable,
			scale = (target.view as? ImageView)?.scale ?: Scale.FIT,
			durationMillis = durationMillis,
			fadeStart = !(result is SuccessResult && result.isPlaceholderCached),
			preferExactIntrinsicSize = preferExactIntrinsicSize
		)

		when (result) {
			is SuccessResult -> target.onSuccess(drawable)
			is ErrorResult -> target.onError(drawable)
		}
	}

	class Factory @JvmOverloads constructor(
		val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION,
		val preferExactIntrinsicSize: Boolean = false
	) : Transition.Factory {

		init {
			require(durationMillis > 0) { "durationMillis must be > 0." }
		}

		override fun create(target: TransitionTarget, result: ImageResult): Transition {
			// 성공한 요청만 애니메이션합니다.
			if (result !is SuccessResult) {
				return Transition.Factory.NONE.create(target, result)
			}

			// 요청이 메모리 캐시에 의해 이행된 경우 애니메이션을 적용하지 마십시오.
			if (result.dataSource == DataSource.MEMORY_CACHE) {
				return Transition.Factory.NONE.create(target, result)
			}

			return CrossfadeTransition(target, result, durationMillis, preferExactIntrinsicSize)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			return other is Factory &&
				durationMillis == other.durationMillis &&
				preferExactIntrinsicSize == other.preferExactIntrinsicSize
		}

		override fun hashCode(): Int {
			var result = durationMillis
			result = 31 * result + preferExactIntrinsicSize.hashCode()
			return result
		}
	}
}