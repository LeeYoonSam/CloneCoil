package com.ys.coil.transition

import androidx.annotation.MainThread
import com.ys.coil.request.ImageResult

/**
 * [Target]의 현재 드로어블과 이미지 요청 결과 사이에 애니메이션을 적용할 클래스입니다.
 *
 * 참고:
 * [Target]은 [Transition] 적용을 지원하기 위해 [TransitionTarget]을 구현해야 합니다.
 * [Target]이 [TransitionTarget]을 구현하지 않으면 모든 [Transition]이 무시됩니다.
 */
interface Transition {

	/**
	 * 전환 애니메이션을 시작합니다.
	 *
	 * 구현은 올바른 [Target] 수명 주기 콜백을 호출할 책임이 있습니다.
	 * 예는 [CrossfadeTransition]을 참조하십시오.
	 */
	@MainThread
	fun transition()

	fun interface Factory {
		fun create(target: TransitionTarget, result: ImageResult): Transition

		companion object {
			@JvmField val NONE: Factory = NoneTransition.Factory()
		}
	}
}