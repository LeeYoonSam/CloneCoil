package com.ys.coil.transition

import com.ys.coil.request.ErrorResult
import com.ys.coil.request.ImageResult
import com.ys.coil.request.SuccessResult

/**
 * 애니메이션 없이 [TransitionTarget]에 [ImageResult]를 적용하는 전환입니다.
 */
internal class NoneTransition(
	private val target: TransitionTarget,
	private val result: ImageResult
) : Transition {

	override fun transition() {
		when (result) {
			is SuccessResult -> target.onSuccess(result.drawable)
			is ErrorResult -> target.onError(result.drawable)
		}
	}

	class Factory : Transition.Factory {

		override fun create(target: TransitionTarget, result: ImageResult): Transition {
			return NoneTransition(target, result)
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}
}
